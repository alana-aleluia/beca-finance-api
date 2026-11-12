package com.becafinance.msusers.application.port.in;

import com.becafinance.msusers.application.dto.ImportResult;
import java.io.InputStream;

public interface ImportUsersPort {
    ImportResult execute(InputStream input);
}
