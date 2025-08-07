package br.gov.ma.idox.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private String timestamp;
}
