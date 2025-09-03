package br.gov.ma.idox.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatusResponse {
    private String taskId;
    private String status;
    private String situation;
    private String link;
    private String summary;
}
