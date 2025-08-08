package br.gov.ma.idox.controller;


import br.gov.ma.idox.dto.TranscriptionResponse;
import br.gov.ma.idox.service.SummarizationService;
import br.gov.ma.idox.service.TranscriptionService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("/idox")
@RequiredArgsConstructor
public class AudioController {

    @Autowired
    private final TranscriptionService transcriptionService;

    @Autowired
    private final SummarizationService summarizationService;

    @GetMapping("/index")
    public String openIndex() {
        return "index";
    }

    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<TranscriptionResponse>> transcribe(
            @NotNull @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("summarize") Boolean summarize) throws Exception {

        return transcriptionService.transcribe(audioFile, summarize)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/summarize")
    public DeferredResult<ResponseEntity<String>> summarizeTxt(@RequestParam("file") MultipartFile file) throws IOException {
        DeferredResult<ResponseEntity<String>> output = new DeferredResult<>(360_000L); // 60 segundos

        File tempFile = File.createTempFile("upload-", ".txt");
        file.transferTo(tempFile);

        summarizationService.summarizeFile(tempFile)
                .thenAccept(summary -> {
                    tempFile.delete();
                    output.setResult(ResponseEntity.ok(summary));
                })
                .exceptionally(ex -> {
                    tempFile.delete();
                    output.setErrorResult(ResponseEntity.status(500).body("Erro: " + ex.getMessage()));
                    return null;
                });

        return output;
    }

    @DeleteMapping("/cancel/{taskId}")
    public ResponseEntity<String> cancelProcess(@PathVariable String taskId) {
        boolean cancelled = transcriptionService.cancelProcess(taskId);

        if (cancelled) {
            return ResponseEntity.ok("Processo com taskId " + taskId + " cancelado com sucesso.");
        } else {
            return ResponseEntity.status(404).body("Nenhum processo ativo encontrado para o taskId: " + taskId);
        }
    }
}
