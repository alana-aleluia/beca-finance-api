package com.becafinance.msusers.adapter.in.web;

import com.becafinance.msusers.application.dto.ImportResult;
import com.becafinance.msusers.application.exception.InvalidCsvException;
import com.becafinance.msusers.application.port.in.ImportUsersPort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/users")
public class UserImportController {
    private final ImportUsersPort importer;
    public UserImportController(ImportUsersPort importer) { this.importer = importer; }
    @PostMapping(value="/import", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importUsers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new InvalidCsvException("Arquivo CSV vazio");
        try (var input = file.getInputStream()) {
            return importer.execute(input);
        } catch (IOException exception) {
            throw new InvalidCsvException("Nao foi possivel ler o arquivo CSV");
        }
    }
}
