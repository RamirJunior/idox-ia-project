package br.gov.ma.idox.service;

import br.gov.ma.idox.dto.TaskIdResponse;
import br.gov.ma.idox.dto.TaskStatusResponse;
import br.gov.ma.idox.exception.TaskCancellationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class TaskService {

    private final TranscriptionService transcriptionService;
    private final FileService fileService;
    private final Map<String, TaskStatusResponse> tasksList = new ConcurrentHashMap<>();

    public TaskIdResponse startTask(MultipartFile audioFile, Boolean summarize) throws IOException {
        String taskId = UUID.randomUUID().toString();
        TaskStatusResponse status = new TaskStatusResponse();
        status.setTaskId(taskId);
        status.setStatus("AGUARDANDO");
        status.setSituation("Arquivo recebido");
        tasksList.put(taskId, status);

        File audio = fileService.saveMultipartFile(this, audioFile, taskId);
        CompletableFuture.runAsync(() -> transcriptionService.transcribe(this, audio, summarize, taskId));
        return new TaskIdResponse(taskId);
    }

    public TaskStatusResponse getStatus(String taskId) {
        return tasksList.get(taskId);
    }

    public void updateStatus(String taskId, String status, String situation) {
        TaskStatusResponse taskStatus = tasksList.get(taskId);
        if (taskStatus != null) {
            taskStatus.setStatus(status);
            taskStatus.setSituation(situation);
        }
    }

    public void updateSummary(String taskId, String summary) {
        TaskStatusResponse taskStatus = tasksList.get(taskId);
        taskStatus.setSummary(summary);
    }

    public void updateLink(String taskId, String link) {
        TaskStatusResponse taskStatus = tasksList.get(taskId);
        taskStatus.setLink(link);
    }

    public TaskStatusResponse cancelTask(String taskId) throws TaskCancellationException {
        boolean canceled = transcriptionService.cancelProcess(taskId);
        if (canceled) {
            TaskStatusResponse taskStatus = tasksList.get(taskId);
            taskStatus.setStatus("CANCELADO");
            taskStatus.setSituation("Cancelado pelo usuário");
            return taskStatus;
        }
        throw new TaskCancellationException("Falha ao cancelar tarefa.");
    }

    // TODO: criar serviço de limpeza pra tasklist
    public boolean deleteTask(String taskId) {
        tasksList.remove(taskId);
        return !tasksList.containsKey(taskId);
    }
}
