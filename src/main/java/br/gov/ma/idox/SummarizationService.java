package br.gov.ma.idox;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SummarizationService {

    private final String LLAMA_PATH = "C:\\Users\\User\\Documents\\projeto\\idox\\llama\\llama.cpp\\build\\bin\\Release\\llama-cli.exe"; // ou seu script wrapper
    private final String MODEL_LLAMA = "C:\\Users\\User\\Documents\\projeto\\idox\\llama\\llama.cpp\\models\\tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf";

    @Async
    public CompletableFuture<String> summarizeFile(File txtFile) {
        try {
            // Lê o conteúdo do arquivo
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String prompt = "Responda sempre em português do Brasil. Quero saber o(s) assunto(s) principal nesse texto. Sempre que possível use tópicos de forma objetiva:\n\n" + content + "\n\n### RESPOSTA:\n";


            ProcessBuilder builder = new ProcessBuilder(
                    LLAMA_PATH,
                    "-m", MODEL_LLAMA,
                    "-p", prompt,
                    "--no-conversation",
                    "--n-predict", "512"
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            StringBuilder rawOutput = new StringBuilder();
            try (BufferedReader processReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = processReader.readLine()) != null) {
                    rawOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Erro ao executar LLaMA (exit code " + exitCode + ")");
            }

            String raw = rawOutput.toString();
            int start = raw.indexOf("### RESPOSTA:");
            if (start >= 0) {
                String corte = raw.substring(start + "### RESPOSTA:".length());
                // remove logs extras após a resposta
                String respostaFinal = Arrays.stream(corte.split("\n"))
                        .takeWhile(l -> !l.trim().startsWith("llama_perf_context_print"))
                        .filter(l -> !l.matches(".*(llama|load|sampler|print_info|context|kv_cache|model_loader).*"))
                        .filter(l -> !l.trim().isEmpty())
                        .collect(Collectors.joining("\n"));

                return CompletableFuture.completedFuture(respostaFinal.trim());
            }


        } catch (Exception e) {
            throw new RuntimeException("Erro ao resumir arquivo: " + e.getMessage(), e);
        }
        return null;
    }
}
