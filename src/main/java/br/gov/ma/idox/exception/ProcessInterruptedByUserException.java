package br.gov.ma.idox.exception;

public class ProcessInterruptedByUserException extends RuntimeException {
    public ProcessInterruptedByUserException(String message) {
        super(message);
    }
}
