package br.gov.ma.idox.service;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static br.gov.ma.idox.integration.llama.LlamaConstants.END_PROMPT;
import static br.gov.ma.idox.integration.llama.LlamaConstants.START_PROMPT;

@Service
@AllArgsConstructor
public class SummarizationService {

    private final AiService aiService;

    @Async
    public CompletableFuture<String> summarizeFile(TaskService taskService, File transcriptionTextFile, String taskId) {
        try {
            taskService.updateStatus(taskId, "PROCESSANDO", "Iniciando sumarização com Llama.");
            StringBuilder transcriptionText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(transcriptionTextFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    transcriptionText.append(line).append("\n");
                }
            }

            String mountedPrompt = START_PROMPT + transcriptionText + END_PROMPT;

            File tempPrompt = createTempPromptFile(mountedPrompt);
            System.out.println("Local do texto de transcrição: " + transcriptionTextFile.getAbsolutePath());
            System.out.println("Local temporário do Prompt Llama.cpp: " + tempPrompt.getAbsolutePath());

            ProcessBuilder builder = runLlamaCommand(tempPrompt, aiService.getLlamaExecutor(), aiService.getLlamaModel());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            taskService.updateStatus(taskId, "PROCESSANDO", "Rodando prompt de análise com Llama.");
            StringBuilder rawLlamaResponse = new StringBuilder();
            try (BufferedReader lineReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = lineReader.readLine()) != null) {
                    rawLlamaResponse.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                taskService.updateStatus(taskId, "FALHA", "Falha durante análise com Llama.");
                throw new RuntimeException("Erro ao executar LLaMA (exit code " + exitCode + ")");
            }

            String rawResponse = rawLlamaResponse.toString();
            int start = rawResponse.indexOf(END_PROMPT);
            if (start >= 0) {
                String sliceRawResponse = rawResponse.substring(start + END_PROMPT.length());
                String summaryResponse = Arrays.stream(sliceRawResponse.split("\n"))
                        .takeWhile(l -> !l.trim().startsWith("llama_perf_context_print"))
                        .filter(l -> !l.matches(".*(llama|load|sampler|print_info|context|kv_cache|model_loader).*"))
                        .map(l -> l
                                .replaceAll("^###\\s*RESPOSTA:\\s*", "")
                                .replaceAll("\\[end of text\\]$", "")
                                .trim()
                        )
                        .filter(l -> !l.isEmpty())
                        .collect(Collectors.joining("\n"));

                tempPrompt.delete();

                taskService.updateStatus(taskId, "FINALIZADO", "Sumarização finalizada com sucesso.");
                taskService.updateSummary(taskId, summaryResponse);
                return CompletableFuture.completedFuture(summaryResponse.trim());
            }

        } catch (Exception e) {
            taskService.updateStatus(taskId, "FAILED", "Erro ao resumir arquivo");
            throw new RuntimeException("Erro ao resumir arquivo: " + e.getMessage(), e);
        }
        return null;
    }

    private ProcessBuilder runLlamaCommand(File tempPrompt, String llamaClient, String llamaModelPath) {
        ProcessBuilder builder = new ProcessBuilder(
                llamaClient,
                "-m", llamaModelPath,
                "-f", tempPrompt.getAbsolutePath(),
                "--temp", "0.2",
                "--no-conversation",
                "--ctx-size", "8192",
                "--repeat_penalty", "1.1"
        );
        return builder;
    }

    private static File createTempPromptFile(String mountedPrompt) throws IOException {
        File tempPromptFile = File.createTempFile("prompt_", ".txt");
        Files.write(tempPromptFile.toPath(), mountedPrompt.getBytes());
        return tempPromptFile;
    }
}
