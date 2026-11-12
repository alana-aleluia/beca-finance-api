package com.becafinance.msusers.adapter.out.csv;

import com.becafinance.msusers.application.dto.CsvRow;
import com.becafinance.msusers.application.exception.InvalidCsvException;
import com.becafinance.msusers.application.port.out.UserCsvReaderPort;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class CommonsUserCsvReader implements UserCsvReaderPort {
    private static final int MAX_RECORDS = 10000;

    @Override
    public List<CsvRow> read(InputStream input) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try (var reader = new PushbackReader(new InputStreamReader(input, decoder), 1)) {
            int first = reader.read();
            if (first != -1 && first != '\uFEFF') reader.unread(first);
            try (var parser = CSVFormat.RFC4180.builder().setIgnoreEmptyLines(false).get().parse(reader)) {
                var iterator = parser.iterator();
                if (!iterator.hasNext()) throw new InvalidCsvException("Arquivo CSV vazio");
                var header = iterator.next();
                if (!header.toList().equals(List.of("name", "email"))) {
                    throw new InvalidCsvException("Cabecalho deve ser exatamente name,email");
                }
                var rows = new ArrayList<CsvRow>();
                while (iterator.hasNext()) {
                    var record = iterator.next();
                    if (rows.size() >= MAX_RECORDS) {
                        throw new InvalidCsvException("Limite de 10000 registros excedido");
                    }
                    rows.add(new CsvRow(record.getRecordNumber(), record.toList()));
                }
                if (rows.isEmpty()) throw new InvalidCsvException("CSV sem registros de usuarios");
                return rows;
            }
        } catch (IOException | UncheckedIOException | IllegalArgumentException exception) {
            throw new InvalidCsvException("CSV malformado ou codificacao UTF-8 invalida");
        }
    }
}
