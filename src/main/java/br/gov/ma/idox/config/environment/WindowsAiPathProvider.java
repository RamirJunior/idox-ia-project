package br.gov.ma.idox.config.environment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("windows")
public class WindowsAiPathProvider implements AiPathProvider {
    @Override
    public String getWhisperBinPath() {
        return "C:\\Users\\User\\Documents\\projeto\\idox\\whisper\\whisper.cpp\\build\\bin\\Release\\whisper-cli.exe";
    }

    @Override
    public String getWhisperModelPath() {
        return "C:\\Users\\User\\Documents\\projeto\\idox\\whisper\\whisper.cpp\\models\\ggml-small.bin";
    }

    @Override
    public String getLlamaBinPath() {
        return "C:\\Users\\User\\Documents\\projeto\\idox\\llama\\llama.cpp\\build\\bin\\Release\\llama-cli.exe";
    }

    @Override
    public String getLlamaModelPath() {
        return "C:\\Users\\User\\Documents\\projeto\\idox\\llama\\llama.cpp\\models\\nous-hermes-2-mistral-7b-dpo.Q4_K_M.gguf";
    }
}
