package br.gov.ma.idox.controller;


import br.gov.ma.idox.dto.TaskIdResponse;
import br.gov.ma.idox.dto.TaskStatusResponse;
import br.gov.ma.idox.service.TaskService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/idox")
@RequiredArgsConstructor
public class AudioController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/index")
    public String openIndex() {
        return "index";
    }

    @PostMapping("/process")
    public ResponseEntity<TaskIdResponse> transcribe(
            @NotNull @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("summarize") Boolean summarize) throws Exception {
        var taskIdResponse = taskService.startTask(audioFile, summarize);
        return ResponseEntity.status(HttpStatus.OK).body(taskIdResponse);
    }

//    @PostMapping("/summarize")
//    public DeferredResult<ResponseEntity<String>> summarizeTxt(@RequestParam("file") MultipartFile file) throws IOException {
//        DeferredResult<ResponseEntity<String>> output = new DeferredResult<>(360_000L);
//
//        File tempFile = File.createTempFile("upload-", ".txt");
//        file.transferTo(tempFile);
//
//        summarizationService.summarizeFile(tempFile)
//                .thenAccept(summary -> {
//                    tempFile.delete();
//                    output.setResult(ResponseEntity.ok(summary));
//                })
//                .exceptionally(ex -> {
//                    tempFile.delete();
//                    output.setErrorResult(ResponseEntity.status(500).body("Erro: " + ex.getMessage()));
//                    return null;
//                });
//
//        return output;
//    }

    @DeleteMapping("/cancel/{taskId}")
    public ResponseEntity<TaskStatusResponse> cancelProcess(@PathVariable String taskId) {
        var cancelledTask = taskService.cancelTask(taskId);
        return ResponseEntity.status(HttpStatus.OK).body(cancelledTask);
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<TaskStatusResponse> getStatus(@PathVariable String taskId) {
        TaskStatusResponse status = taskService.getStatus(taskId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(status);
    }
}
