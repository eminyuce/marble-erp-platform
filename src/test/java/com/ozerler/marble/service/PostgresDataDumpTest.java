package com.ozerler.marble.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresDataDumpTest {

    @Test
    void quotesTextAndIdentifiers() {
        assertThat(PostgresDataDump.quote("O'Brien")).isEqualTo("'O''Brien'");
        assertThat(PostgresDataDump.quoteIdentifier("order")).isEqualTo("\"order\"");
        assertThat(PostgresDataDump.byteaLiteral(new byte[] {0x0a, (byte) 0xff})).isEqualTo("decode('0aff', 'hex')");
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
            assertThat(rows).isGreaterThan(0);
            assertThat(sql).contains("Özerler Mermer ERP Database Backup");
            assertThat(sql).contains("INSERT INTO public.\"blocks\"");
            assertThat(sql).contains("INSERT INTO public.\"users\"");
            assertThat(sql).contains("-- Rows: " + rows);
            assertThat(sql).doesNotContain("-- Tablo Listesi:");
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
