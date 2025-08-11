package br.gov.ma.idox.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Getter
public class UploadDirectoryService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    //@Scheduled(cron = "0 * * * * ?") <-- limpeza a cada 1 min
    @Scheduled(cron = "0 0 3 ? * TUE-SAT")
    public void cleanUploadDirectory() {
        File folder = new File(uploadDir);
        System.out.println("Iniciando rotina de limpeza programada...");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("Arquivo deletado: " + file.getName());
                    } else {
                        System.err.println("Falha ao deletar arquivo: " + file.getName());
                    }
                }
            }
        } else {
            System.out.println("Nenhum arquivo encontrado para limpeza na pasta: " + uploadDir);
        }
    }
}
