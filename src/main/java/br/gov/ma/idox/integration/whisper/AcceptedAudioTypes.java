package br.gov.ma.idox.integration.whisper;

import java.util.List;

public class AcceptedAudioTypes {

    public static final List<String> SUPPORTED_MIME_TYPES = List.of(
            "audio/mpeg",
            "audio/mp4",
            "audio/x-m4a",
            "audio/ogg",
            "audio/wav"
    );
}
