package br.gov.ma.idox.exception;

import br.gov.ma.idox.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WhisperProcessingException.class)
    public ResponseEntity<ErrorResponse> handleWhisper(WhisperProcessingException err) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_WHISPER_FAIL", err.getMessage());
    }

    @ExceptionHandler(LlamaProcessingException.class)
    public ResponseEntity<ErrorResponse> handleLlama(LlamaProcessingException err) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_LLAMA_FAIL", err.getMessage());
    }

    @ExceptionHandler(UnsupportedAudioFormatException.class)
    public ResponseEntity<ErrorResponse> handleFormat(UnsupportedAudioFormatException err) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "ERR_UNSUPPORTED_FORMAT", err.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooMany(TooManyRequestsException err) {
        return buildError(HttpStatus.TOO_MANY_REQUESTS, "ERR_TOO_MANY_REQUESTS", err.getMessage());
    }

    @ExceptionHandler(AudioProcessingTimeOutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(AudioProcessingTimeOutException err) {
        return buildError(HttpStatus.GATEWAY_TIMEOUT, "ERR_AUDIO_TIMEOUT", err.getMessage());
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
