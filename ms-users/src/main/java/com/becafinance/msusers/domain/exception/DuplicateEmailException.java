package com.becafinance.msusers.domain.exception;
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() { super("E-mail ja cadastrado"); }
}
