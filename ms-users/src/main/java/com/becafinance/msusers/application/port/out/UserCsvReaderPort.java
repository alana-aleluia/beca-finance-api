package com.becafinance.msusers.application.port.out;
import com.becafinance.msusers.application.dto.CsvRow;
import java.io.InputStream;
import java.util.List;
public interface UserCsvReaderPort {
    List<CsvRow> read(InputStream input);
}
