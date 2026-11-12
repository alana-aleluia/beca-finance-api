package com.becafinance.msusers.adapter.out.csv;

import com.becafinance.msusers.application.exception.InvalidCsvException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class CommonsUserCsvReaderTest {
    final CommonsUserCsvReader reader = new CommonsUserCsvReader();

    @Test void readsBomUtf8AndQuotedFields() {
        var rows = reader.read(csv("\uFEFFname,email\r\n\"Ana, Silva\",ana@example.com\r\nJosé,jose@example.com\r\n"));
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().record()).isEqualTo(2);
        assertThat(rows.getFirst().values()).containsExactly("Ana, Silva", "ana@example.com");
        assertThat(rows.get(1).values()).containsExactly("José", "jose@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "name,email\n", "email,name\nana@example.com,Ana\n", "name;email\nAna;ana@example.com\n", "name,email,password\nAna,ana@example.com,secret\n", "name,email\n\"unclosed"})
    void rejectsEmptyInvalidHeaderOrMalformedCsv(String content) {
        assertThatThrownBy(() -> reader.read(csv(content))).isInstanceOf(InvalidCsvException.class);
    }

    @Test void rejectsInvalidUtf8() {
        byte[] header = "name,email\n".getBytes(StandardCharsets.UTF_8);
        byte[] bytes = java.util.Arrays.copyOf(header, header.length + 2);
        bytes[header.length] = (byte) 0xC3;
        bytes[header.length + 1] = (byte) 0x28;
        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(bytes))).isInstanceOf(InvalidCsvException.class);
    }

    @Test void preservesWrongColumnCountsForRowValidation() {
        var rows = reader.read(csv("name,email\nAna\nBia,bia@example.com,extra\n\n"));
        assertThat(rows).hasSize(3);
        assertThat(rows.getFirst().values()).containsExactly("Ana");
        assertThat(rows.get(1).values()).hasSize(3);
    }

    @Test void readsQuotedMultilineAndEscapedQuotes() {
        var rows = reader.read(csv("name,email\n\"Ana \"\"Silva\"\"\nSouza\",ana@example.com\n"));
        assertThat(rows.getFirst().values()).containsExactly("Ana \"Silva\"\nSouza", "ana@example.com");
    }

    @Test void acceptsExactlyTenThousandRecords() {
        assertThat(reader.read(csv("name,email\n" + "Ana,ana@example.com\n".repeat(10000)))).hasSize(10000);
    }

    @Test void rejectsMoreThanTenThousandRecords() {
        assertThatThrownBy(() -> reader.read(csv("name,email\n" + "Ana,ana@example.com\n".repeat(10001))))
            .isInstanceOf(InvalidCsvException.class).hasMessageContaining("10000");
    }

    @Test void translatesIoFailure() {
        var broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("broken"); }
        };
        assertThatThrownBy(() -> reader.read(broken)).isInstanceOf(InvalidCsvException.class);
    }

    private InputStream csv(String content) { return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)); }
}
