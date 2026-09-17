package ru.evgeny.echo.stt.vosk;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import ru.evgeny.echo.stt.api.STTService;

@Service
public class VoskSTTService implements STTService {
    private final Model model;
    private final long silenceLag;

    @SneakyThrows
    public VoskSTTService(String dir, long silenceLag) {
        this.model = new Model(dir);
        this.silenceLag = silenceLag;
    }

    @Override
    public VoskTransformator createTransformator(int sampleRate) {
        return new VoskTransformator(model, sampleRate, silenceLag);
    }
}
