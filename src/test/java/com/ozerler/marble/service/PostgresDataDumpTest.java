package com.ozerler.marble.service;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class PostgresDataDumpTest {

    @Test
    void quotesTextAndIdentifiers() {
        Assertions.assertThat(PostgresDataDump.quote("O'Brien")).isEqualTo("'O''Brien'");
        Assertions.assertThat(PostgresDataDump.quoteIdentifier("order")).isEqualTo("\"order\"");
        Assertions.assertThat(PostgresDataDump.byteaLiteral(new byte[] {0x0a, (byte) 0xff})).isEqualTo("decode('0aff', 'hex')");
    }

    @Test
    void writesInsertsFromLocalPostgreSQL() throws Exception {
        Connection connection = openLocalDatabase();
        Assumptions.assumeTrue(connection != null, "Yerel PostgreSQL yok");
        try (connection) {
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            StringWriter writer = new StringWriter();
            long rows = new PostgresDataDump().write(connection, writer, "marble_erp");
            connection.rollback();

            String sql = writer.toString();
            Assertions.assertThat(rows).isGreaterThan(0);
            Assertions.assertThat(sql).contains("Özerler Mermer ERP Database Backup");
            Assertions.assertThat(sql).contains("INSERT INTO public.\"blocks\"");
            Assertions.assertThat(sql).contains("INSERT INTO public.\"users\"");
            Assertions.assertThat(sql).contains("-- Rows: " + rows);
            Assertions.assertThat(sql).doesNotContain("-- Tablo Listesi:");
        }
    }

    private static Connection openLocalDatabase() {
        try {
            return DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/marble_erp",
                    "marbleuser",
                    "marblepass");
        } catch (SQLException exception) {
            return null;
        }
    }
}
