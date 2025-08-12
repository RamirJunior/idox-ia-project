package br.gov.ma.idox.exception;

public class TaskCancellationException extends RuntimeException {
    public TaskCancellationException(String message) {
        super(message);
    }
}
