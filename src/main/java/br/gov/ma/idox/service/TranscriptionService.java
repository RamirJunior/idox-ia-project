package br.gov.ma.idox.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static br.gov.ma.idox.integration.whisper.WhisperConstants.*;

@Service
public class TranscriptionService {

    @Async
    public CompletableFuture<String> processAudio(MultipartFile audioFile) throws IOException {

        ensureUploadDirectoryExists();

        String hashedFileName = hashFileName(audioFile.getOriginalFilename());

        Path savedPath = Path.of(UPLOAD_DIR, hashedFileName);
        File file = savedPath.toFile();
        audioFile.transferTo(file);

        ProcessBuilder builder = runWhisperCommand(file, savedPath);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(System.out::println);
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Erro ao executar Whisper (código " + exitCode + ")");
                }

                // Verifica se o .txt foi gerado
                File txtFile = new File(savedPath.toString().replace(".wav", ".txt"));
                if (txtFile.exists()) {
                    return "/uploads/" + txtFile.getName(); // Link ou path relativo
                } else {
                    throw new FileNotFoundException("Arquivo de transcrição não encontrado após execução.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Erro ao processar áudio: " + e.getMessage(), e);
            }
        });
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
