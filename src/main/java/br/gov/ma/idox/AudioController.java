package br.gov.ma.idox;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public ResponseEntity<String> transcribe(@RequestParam("audioFile") MultipartFile audioFile) {
        try {
            CompletableFuture<String> resultFuture = transcriptionService.processAudio(audioFile);

            String result = resultFuture.get();
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro: " + e.getMessage());
        }

    }

    @PostMapping("/summarize")
    public DeferredResult<ResponseEntity<String>> summarizeTxt(@RequestParam("file") MultipartFile file) throws IOException {
        DeferredResult<ResponseEntity<String>> output = new DeferredResult<>(180_000L); // 60 segundos

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
}
