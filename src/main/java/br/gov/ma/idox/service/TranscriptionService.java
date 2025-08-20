package br.gov.ma.idox.service;

import br.gov.ma.idox.dto.TranscriptionResponse;
import br.gov.ma.idox.exception.ProcessInterruptedByUserException;
import br.gov.ma.idox.exception.TranscriptionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final int CODE_REQUESTED_BY_USER = 143;

    @Async
    public CompletableFuture<TranscriptionResponse> transcribe(TaskService taskService, File audioFile, boolean summarize, String taskId) {

        try {
            taskService.updateStatus(taskId, "AGUARDANDO", "iDox está salvando arquivo.");
            ensureUploadDirectoryExists(uploadService.getUploadDir());

            taskService.updateStatus(taskId, "PROCESSANDO", "Analisando áudio com Whisper.");
            Process process = startWhisperProcess(audioFile);
            processMap.put(taskId, process);

            log.info("Iniciando tarefa com taskId: {}", taskId);
            readProcessOutput(process);

            int exitCode = process.waitFor();
            processMap.remove(taskId);

            if (exitCode == CODE_REQUESTED_BY_USER) {
                taskService.updateStatus(taskId, "CANCELADO", "Processo cancelado pelo usuário");
                throw new ProcessInterruptedByUserException("Processo cancelado pelo usuário.");
            }

            if (exitCode != 0) {
                taskService.updateStatus(taskId, "FALHA", "Falha durante serviço Whisper.");
                throw new TranscriptionException("Processo Whisper falhou com código: " + exitCode);
            }

            taskService.updateStatus(taskId, "PROCESSANDO", "Gerando arquivo de texto transcrito.");
            TranscriptionResponse response = buildResponse(taskService, audioFile.toPath(), summarize, taskId);
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

    private Process startWhisperProcess(File file) throws IOException {
        ProcessBuilder builder = runWhisperCommand(file);
        log.info("Comando Whisper: {}", String.join(" ", builder.command()));
        return builder.start();
    }

    private void readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(log::info);
        }
    }

    private TranscriptionResponse buildResponse(TaskService taskService, Path savedPath, boolean summarize, String taskId) throws Exception {
        File txtFile = new File(savedPath.toString().replace(".wav", ".txt"));
        if (!txtFile.exists()) {
            taskService.updateStatus(taskId, "FALHA", "Arquivo de texto não encontrado.");
            throw new TranscriptionException("Arquivo de transcrição não encontrado em: " + txtFile.getAbsolutePath());
        }

        TranscriptionResponse response = new TranscriptionResponse();
        response.setTextFileLink("/uploads/" + txtFile.getName());
        response.setSummarize(summarize);
        response.setTaskId(taskId);
        taskService.updateLink(taskId, response.getTextFileLink());

        if (summarize) {
            taskService.updateStatus(taskId, "PROCESSANDO", "Iniciando sumarização com Llama.");
            String summary = summarizationService.summarizeFile(taskService, txtFile, taskId).get();
            response.setSummary(summary);
            log.info("Resumo gerado para a taskId: {}", taskId);
        }
        response.setSituation("Processamento iDox finalizado.");
        response.setStatus("FINALIZADO");
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
}
