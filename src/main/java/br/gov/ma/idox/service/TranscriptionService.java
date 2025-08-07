package br.gov.ma.idox.service;

import br.gov.ma.idox.dto.TranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static br.gov.ma.idox.integration.whisper.WhisperConstants.*;

@Service
public class TranscriptionService {

    @Autowired
    private SummarizationService summarizationService;

    @Async
    public CompletableFuture<TranscriptionResponse> transcribe(MultipartFile audioFile, boolean summarize) {
        try {
            ensureUploadDirectoryExists();

            String hashedFileName = hashFileName(audioFile.getOriginalFilename());
            Path savedPath = Path.of(UPLOAD_DIR, hashedFileName);
            File file = savedPath.toFile();
            audioFile.transferTo(file);

            ProcessBuilder builder = runWhisperCommand(file, savedPath);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(System.out::println);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return CompletableFuture.failedFuture(new RuntimeException("Erro ao executar Whisper (código " + exitCode + ")"));
            }

            File txtFile = new File(savedPath.toString().replace(".wav", ".txt"));
            if (!txtFile.exists()) {
                return CompletableFuture.failedFuture(new RuntimeException("Arquivo de transcrição não encontrado."));
            }

            String link = "/uploads/" + txtFile.getName();

            TranscriptionResponse response = new TranscriptionResponse();
            response.setSummarize(summarize); // ou qualquer outra flag que você usa
            response.setTextFileLink("/uploads/" + txtFile.getName()); // já existente

            if (summarize) {
                try {
                    CompletableFuture<String> resumoFuture = summarizationService.summarizeFile(txtFile);
                    String resumo = resumoFuture.get(); // Aqui você espera a resposta
                    response.setSummary(resumo);        // Agora sim, uma string válida
                } catch (Exception e) {
                    response.setSummary("Erro ao gerar resumo: " + e.getMessage());
                }
            }
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static ProcessBuilder runWhisperCommand(File file, Path savedPath) {
        ProcessBuilder builder = new ProcessBuilder(
                WHISPER_PATH,
                "-m", MODEL_PATH,
                "-f", file.getAbsolutePath(),
                "-l", "pt",
                "-pp",
                "-nt",
                "-of", savedPath.toString().replace(".wav", ""),
                "-otxt"
        );
        return builder;
    }

    private static String hashFileName(String fileName) {
        String baseName = fileName != null ? fileName.replaceAll("\\.[^.]+$", "") : "audio";
        return UUID.randomUUID() + "-" + baseName + ".wav";
    }

    private static void ensureUploadDirectoryExists() {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) uploadDir.mkdirs();
    }
}
