package br.gov.ma.idox.service;

import br.gov.ma.idox.dto.TranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static br.gov.ma.idox.integration.whisper.WhisperConstants.*;

@Service
public class TranscriptionService {

    @Autowired
    private SummarizationService summarizationService;

    // Map para guardar processos ativos e poder cancelar depois
    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<TranscriptionResponse> transcribe(MultipartFile audioFile, boolean summarize) {
        try {
            ensureUploadDirectoryExists();

            // Gera um ID único para essa tarefa/processo
            String taskId = UUID.randomUUID().toString();

            String hashedFileName = hashFileName(audioFile.getOriginalFilename());
            Path savedPath = Path.of(UPLOAD_DIR, hashedFileName);
            File file = savedPath.toFile();
            audioFile.transferTo(file);

            ProcessBuilder builder = runWhisperCommand(file, savedPath);
            Process process = builder.start();

            // Guarda o processo no mapa com o taskId
            processMap.put(taskId, process);
            System.out.println("taskId do processo: " + taskId);

            // Executa assincronamente o processamento e retorna o CompletableFuture
            return CompletableFuture.supplyAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    reader.lines().forEach(System.out::println);
                } catch (Exception e) {
                    throw new RuntimeException("Erro lendo output do processo: " + e.getMessage(), e);
                }

                try {
                    int exitCode = process.waitFor();

                    // Processo terminou, remove do mapa
                    processMap.remove(taskId);

                    if (exitCode != 0) {
                        throw new RuntimeException("Erro ao executar Whisper (código " + exitCode + ")");
                    }

                    File txtFile = new File(savedPath.toString().replace(".wav", ".txt"));
                    if (!txtFile.exists()) {
                        throw new RuntimeException("Arquivo de transcrição não encontrado.");
                    }

                    TranscriptionResponse response = new TranscriptionResponse();
                    response.setTextFileLink("/uploads/" + txtFile.getName());
                    response.setSummarize(summarize);
                    response.setTaskId(taskId);  // Inclui o taskId no DTO

                    if (summarize) {
                        String resumo = summarizationService.summarizeFile(txtFile).get();
                        response.setSummary(resumo);
                    }

                    return response;

                } catch (Exception e) {
                    throw new RuntimeException("Erro no processamento: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // Método para cancelar processo dado um taskId
    public boolean cancelProcess(String taskId) {
        Process process = processMap.get(taskId);
        if (process != null && process.isAlive()) {
            process.destroy();
            processMap.remove(taskId);
            return true;
        }
        return false;
    }

    private static ProcessBuilder runWhisperCommand(File file, Path savedPath) {
        return new ProcessBuilder(
                WHISPER_PATH,
                "-m", MODEL_PATH,
                "-f", file.getAbsolutePath(),
                "-l", "pt",
                "-pp",
                "-nt",
                "-of", savedPath.toString().replace(".wav", ""),
                "-otxt"
        );
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
