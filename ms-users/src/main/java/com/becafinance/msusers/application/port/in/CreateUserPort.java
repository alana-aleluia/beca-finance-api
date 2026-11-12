package com.becafinance.msusers.application.port.in;

import com.becafinance.msusers.domain.model.User;

public interface CreateUserPort {
    User execute(String name, String email);
    User execute(String name, String email, String password);
}
