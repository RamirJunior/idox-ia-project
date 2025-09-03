package br.gov.ma.idox.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TaskRequest {
    private MultipartFile audioFile;
    private boolean summarize;
}
