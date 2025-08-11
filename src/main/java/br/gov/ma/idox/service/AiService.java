package br.gov.ma.idox.service;

import br.gov.ma.idox.config.environment.AiPathProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    @Autowired
    private AiPathProvider aiPathProvider;

    public String getWhisperExecutor() {
        return aiPathProvider.getWhisperBinPath();
    }

    public String getWhisperModel() {
        return aiPathProvider.getWhisperModelPath();
    }

    public String getLlamaExecutor() {
        return aiPathProvider.getLlamaBinPath();
    }

    public String getLlamaModel() {
        return aiPathProvider.getLlamaModelPath();
    }
}
