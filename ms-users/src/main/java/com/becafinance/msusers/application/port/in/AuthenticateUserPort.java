package com.becafinance.msusers.application.port.in;

import com.becafinance.msusers.application.dto.UserIdentity;

public interface AuthenticateUserPort {
    UserIdentity execute(String email, String password);
}
