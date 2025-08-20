package br.gov.ma.idox.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@AllArgsConstructor
public class FileService {

    private final UploadDirectoryService uploadDirectoryService;

    public File saveMultipartFile(TaskService taskService, MultipartFile multipartFile, String taskId) throws IOException {

        Path uploadPath = getAbsoluteFilePath();
        String fileName = getHashedFileName(multipartFile.getOriginalFilename(), taskId);
        Path filePath = uploadPath.resolve(fileName);

        taskService.updateStatus(taskId, "PROCESSANDO", "iDox está salvando o arquivo");
        multipartFile.transferTo(filePath.toFile());
        log.info("Arquivo salvo em: {}", filePath);
        return filePath.toFile();
    }

    private Path getAbsoluteFilePath() throws IOException {
        Path uploadPath = Paths.get(uploadDirectoryService.getUploadDir()).toAbsolutePath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Pasta de uploads criada em: {}", uploadPath);
        }
        return uploadPath;
    }

    private static String getHashedFileName(String originalFileName, String taskId) {
        String baseName = (originalFileName != null) ? originalFileName.replaceAll("\\.[^.]+$", "") : "audio";
        return taskId + baseName + ".wav";
    }
}
