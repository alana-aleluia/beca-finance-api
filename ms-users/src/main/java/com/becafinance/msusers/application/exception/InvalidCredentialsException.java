package com.becafinance.msusers.application.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("Credenciais invalidas"); }
}
