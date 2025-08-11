package br.gov.ma.idox.config.environment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("linux")
public class LinuxAiPathProvider implements AiPathProvider {
    @Override
    public String getWhisperBinPath() {
        return "/home/junior/Documents/projects/idox-ia-project/whisper/whisper.cpp/build/bin/whisper-cli";
    }

    @Override
    public String getWhisperModelPath() {
        return "/home/junior/Documents/projects/idox-ia-project/whisper/whisper.cpp/models/ggml-medium.bin";
    }

    @Override
    public String getLlamaBinPath() {
        return "/home/junior/Documents/projects/idox-ia-project/llama/llama.cpp/build/bin/llama-cli";
    }

    @Override
    public String getLlamaModelPath() {
        return "/home/junior/Documents/projects/idox-ia-project/llama/llama.cpp/models/Nous-Hermes-2-Mistral-7B-DPO.Q4_K_M.gguf";
    }
}
