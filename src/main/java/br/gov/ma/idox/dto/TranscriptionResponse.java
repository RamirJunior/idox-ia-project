package br.gov.ma.idox.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranscriptionResponse {
    private String textFileLink;
    private boolean summarize;
    private String summary;
}
