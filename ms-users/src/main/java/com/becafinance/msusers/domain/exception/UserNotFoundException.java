package com.becafinance.msusers.domain.exception;
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) { super("Usuario nao encontrado: " + id); }
}
