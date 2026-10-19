package com.ozerler.marble.service;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes a restorable SQL script of the public schema when {@code pg_dump} is not installed.
 * Schema changes stay with Flyway; this script replaces table contents and resets sequences.
 */
final class PostgresDataDump {

    private static final String TABLE_SQL = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """;

    private static final String COLUMN_SQL = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ?
            ORDER BY ordinal_position
            """;

    private static final String SEQUENCE_SQL = """
            SELECT c.relname
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public' AND c.relkind = 'S'
            ORDER BY c.relname
            """;

    long write(Connection connection, Writer writer, String databaseName) throws SQLException, IOException {
        List<String> tables = tableNames(connection);
        if (tables.isEmpty()) {
            throw new SQLException("public şemasında yedeklenecek tablo yok.");
        }

        writer.write("-- ========================================================\n");
        writer.write("-- Özerler Mermer ERP Database Backup\n");
        writer.write("-- Timestamp: " + Instant.now() + "\n");
        writer.write("-- Database: " + (databaseName == null ? "marble_erp" : databaseName) + "\n");
        writer.write("-- Source: PostgreSQL data dump\n");
        writer.write("-- Tables: " + tables.size() + "\n");
        writer.write("-- ========================================================\n");
        writer.write("BEGIN;\n");
        writer.write("SET session_replication_role = replica;\n");
        writer.write("TRUNCATE TABLE ");
        for (int index = 0; index < tables.size(); index++) {
            if (index > 0) {
                writer.write(", ");
            }
            writer.write("public." + quoteIdentifier(tables.get(index)));
        }
        writer.write(" RESTART IDENTITY CASCADE;\n");

        long rowCount = 0;
        for (String table : tables) {
            rowCount += writeTable(connection, writer, table);
        }
        writeSequences(connection, writer);
        writer.write("COMMIT;\n");
        writer.write("-- Rows: " + rowCount + "\n");
        writer.flush();
        return rowCount;
    }

    private static List<String> tableNames(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(TABLE_SQL)) {
            while (rows.next()) {
                tables.add(rows.getString(1));
            }
        }
        return tables;
    }

    private long writeTable(Connection connection, Writer writer, String table) throws SQLException, IOException {
        List<String> columns = columnNames(connection, table);
        if (columns.isEmpty()) {
            return 0;
        }
        StringBuilder select = new StringBuilder("SELECT ");
        for (int index = 0; index < columns.size(); index++) {
            if (index > 0) {
                select.append(", ");
            }
            select.append(quoteIdentifier(columns.get(index)));
        }
        select.append(" FROM public.").append(quoteIdentifier(table));

        String columnList = quotedColumnList(columns);
        long rowsWritten = 0;
        try (Statement statement = connection.createStatement()) {
            statement.setFetchSize(500);
            try (ResultSet rows = statement.executeQuery(select.toString())) {
                ResultSetMetaData meta = rows.getMetaData();
                while (rows.next()) {
                    writer.write("INSERT INTO public.");
                    writer.write(quoteIdentifier(table));
                    writer.write(" (");
                    writer.write(columnList);
                    writer.write(") VALUES (");
                    for (int column = 1; column <= meta.getColumnCount(); column++) {
                        if (column > 1) {
                            writer.write(", ");
                        }
                        writer.write(sqlLiteral(rows, meta, column));
                    }
                    writer.write(");\n");
                    rowsWritten++;
                }
            }
        }
        return rowsWritten;
    }

    private static List<String> columnNames(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(COLUMN_SQL)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    columns.add(rows.getString(1));
                }
            }
        }
        return columns;
    }

    private static String quotedColumnList(List<String> columns) {
        StringBuilder list = new StringBuilder();
        for (int index = 0; index < columns.size(); index++) {
            if (index > 0) {
                list.append(", ");
            }
            list.append(quoteIdentifier(columns.get(index)));
        }
        return list.toString();
    }

    private static void writeSequences(Connection connection, Writer writer) throws SQLException, IOException {
        List<String> sequences = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(SEQUENCE_SQL)) {
            while (rows.next()) {
                sequences.add(rows.getString(1));
            }
        }
        for (String name : sequences) {
            String query = "SELECT last_value, is_called FROM public." + quoteIdentifier(name);
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(query)) {
                if (!rows.next()) {
                    continue;
                }
                long lastValue = rows.getLong(1);
                boolean lastValueMissing = rows.wasNull();
                boolean called = rows.getBoolean(2);
                String sequence = quote("public." + quoteIdentifier(name));
                if (lastValueMissing) {
                    writer.write("SELECT setval(" + sequence + ", 1, false);\n");
                    continue;
                }
                writer.write("SELECT setval(" + sequence + ", " + lastValue + ", " + (called ? "true" : "false") + ");\n");
            }
        }
    }

    static String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    static String sqlLiteral(ResultSet rows, ResultSetMetaData meta, int column) throws SQLException {
        Object value = rows.getObject(column);
        if (value == null || rows.wasNull()) {
            return "NULL";
        }
        if (value instanceof byte[] bytes) {
            return byteaLiteral(bytes);
        }
        if (value instanceof Array array) {
            String text = rows.getString(column);
            if (text == null) {
                return "NULL";
            }
            return quote(text) + "::" + array.getBaseTypeName() + "[]";
        }
        int sqlType = meta.getColumnType(column);
        if (sqlType == Types.BOOLEAN || sqlType == Types.BIT || value instanceof Boolean) {
            return rows.getBoolean(column) ? "TRUE" : "FALSE";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (sqlType == Types.TINYINT || sqlType == Types.SMALLINT || sqlType == Types.INTEGER || sqlType == Types.BIGINT) {
            return Long.toString(rows.getLong(column));
        }
        if (sqlType == Types.FLOAT || sqlType == Types.REAL || sqlType == Types.DOUBLE) {
            double number = rows.getDouble(column);
            if (Double.isFinite(number)) {
                return Double.toString(number);
            }
            return quote(Double.toString(number));
        }
        String typeName = meta.getColumnTypeName(column);
        String text = rows.getString(column);
        if (text == null) {
            return "NULL";
        }
        if (typeName != null && !typeName.isBlank() && isUserDefined(typeName, sqlType)) {
            return quote(text) + "::" + typeName;
        }
        return quote(text);
    }

    private static boolean isUserDefined(String typeName, int sqlType) {
        String normalized = typeName.toLowerCase(Locale.ROOT);
        return sqlType == Types.OTHER
                || normalized.equals("json")
                || normalized.equals("jsonb")
                || normalized.equals("uuid")
                || normalized.startsWith("_");
    }

    static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    static String byteaLiteral(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return "decode('" + hex + "', 'hex')";
    }
}
