package br.gov.ma.idox.config.environment;

public interface AiPathProvider {
    String getWhisperBinPath();

    String getWhisperModelPath();

    String getLlamaBinPath();

    String getLlamaModelPath();
}
