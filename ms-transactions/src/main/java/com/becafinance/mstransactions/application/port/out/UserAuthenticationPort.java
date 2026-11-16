package com.becafinance.mstransactions.application.port.out;
import com.becafinance.mstransactions.application.dto.UserIdentity;
public interface UserAuthenticationPort { UserIdentity authenticate(String email, String password); }
