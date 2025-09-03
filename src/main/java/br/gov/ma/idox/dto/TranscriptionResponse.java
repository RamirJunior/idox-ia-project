package br.gov.ma.idox.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranscriptionResponse {
    private String taskId;
    private boolean summarize;
    private String status;
    private String situation;
    private String textFileLink;
    private String summary;
}
