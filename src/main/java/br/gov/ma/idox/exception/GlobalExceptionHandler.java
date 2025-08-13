package br.gov.ma.idox.exception;

import br.gov.ma.idox.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TranscriptionException.class)
    public ResponseEntity<ErrorResponse> handleWhisper(TranscriptionException err) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_WHISPER_FAIL", err.getMessage());
    }

    @ExceptionHandler(ProcessInterruptedByUserException.class)
    public ResponseEntity<ErrorResponse> cancelledByUser(ProcessInterruptedByUserException err) {
        return buildError(HttpStatus.ACCEPTED, "CANCELLED_BY_USER", err.getMessage());
    }

    @ExceptionHandler(TaskCancellationException.class)
    public ResponseEntity<ErrorResponse> failToCancelTask(TaskCancellationException err) {
        return buildError(HttpStatus.GONE, "CANCELLATION_FAILED", err.getMessage());
    }

    @ExceptionHandler(UnsupportedAudioFormatException.class)
    public ResponseEntity<ErrorResponse> handleFormat(UnsupportedAudioFormatException err) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "ERR_UNSUPPORTED_FORMAT", err.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception err) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_INTERNAL", "Erro inesperado. Tente novamente.");
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String code, String message) {
        ErrorResponse error = new ErrorResponse(status.value(), code, message, Instant.now().toString());
        return new ResponseEntity<>(error, status);
    }
}
