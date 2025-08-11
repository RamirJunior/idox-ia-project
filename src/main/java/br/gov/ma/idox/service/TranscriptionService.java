package br.gov.ma.idox.service;

import br.gov.ma.idox.dto.TranscriptionResponse;
import br.gov.ma.idox.exception.TranscriptionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptionService {

    private final AiService aiService;
    private final UploadDirectoryService uploadService;
    private final SummarizationService summarizationService;

    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<TranscriptionResponse> transcribe(MultipartFile audioFile, boolean summarize) {
        try {
            ensureUploadDirectoryExists(uploadService.getUploadDir());
            String taskId = UUID.randomUUID().toString();

            File savedFile = saveFile(audioFile);
            Process process = startWhisperProcess(savedFile);

            processMap.put(taskId, process);
            log.info("Iniciando tarefa com taskId: {}", taskId);

            readProcessOutput(process);

            int exitCode = process.waitFor();
            processMap.remove(taskId);

            if (exitCode != 0) {
                throw new TranscriptionException("Processo Whisper falhou com código: " + exitCode);
            }

            TranscriptionResponse response = buildResponse(savedFile.toPath(), summarize, taskId);

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("Erro durante transcrição", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean cancelProcess(String taskId) {
        Process process = processMap.get(taskId);
        if (process != null && process.isAlive()) {
            process.destroy();
            processMap.remove(taskId);
            log.info("Processo com taskId cancelado: {}", taskId);
            return true;
        }
        return false;
    }

    private File saveFile(MultipartFile audioFile) throws IOException {
        String fileName = generateUniqueFileName(audioFile.getOriginalFilename());
        Path path = Paths.get(uploadService.getUploadDir(), fileName);
        audioFile.transferTo(path.toFile());
        log.info("Arquivo salvo em: {}", path);
        return path.toFile();
    }

    private Process startWhisperProcess(File file) throws IOException {
        ProcessBuilder builder = runWhisperCommand(file);
        log.info("Iniciando Whisper com comando: {}", String.join(" ", builder.command()));
        return builder.start();
    }

    private void readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(log::info);
        }
    }

    private TranscriptionResponse buildResponse(Path savedPath, boolean summarize, String taskId) throws Exception {
        File txtFile = new File(savedPath.toString().replace(".wav", ".txt"));
        if (!txtFile.exists()) {
            throw new TranscriptionException("Arquivo de transcrição não encontrado em: " + txtFile.getAbsolutePath());
        }

        TranscriptionResponse response = new TranscriptionResponse();
        response.setTextFileLink("/uploads/" + txtFile.getName());
        response.setSummarize(summarize);
        response.setTaskId(taskId);

        if (summarize) {
            String summary = summarizationService.summarizeFile(txtFile).get();
            response.setSummary(summary);
            log.info("Resumo gerado para a taskId: {}", taskId);
        }
        return response;
    }

    private void ensureUploadDirectoryExists(String uploadPath) throws IOException {
        Path path = Paths.get(uploadPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Pasta Uploads criada em: {}", uploadPath);
        }
    }

    private ProcessBuilder runWhisperCommand(File file) {
        String whisperBinPath = aiService.getWhisperExecutor();
        String whisperModelPath = aiService.getWhisperModel();

        return new ProcessBuilder(
                whisperBinPath,
                "-m", whisperModelPath,
                "-f", file.getAbsolutePath(),
                "-l", "pt",
                "-pp",
                "-nt",
                "-of", file.getAbsolutePath().replace(".wav", ""),
                "-otxt"
        );
    }

    private static String generateUniqueFileName(String originalFilename) {
        String baseName = (originalFilename != null) ?
                originalFilename.replaceAll("\\.[^.]+$", "") : "audio";
        return baseName + "-" + UUID.randomUUID() + ".wav";
    }
}
