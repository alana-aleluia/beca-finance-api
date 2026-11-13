package com.becafinance.mstransactions.application.dto;
import java.security.Principal;
public record UserIdentity(long id, String email) implements Principal {
    @Override public String getName() { return Long.toString(id); }
}
