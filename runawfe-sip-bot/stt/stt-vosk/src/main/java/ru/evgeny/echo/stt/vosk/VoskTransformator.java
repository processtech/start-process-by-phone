package ru.evgeny.echo.stt.vosk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.vosk.Model;
import org.vosk.Recognizer;
import ru.evgeny.echo.stt.api.TextPart;
import ru.evgeny.echo.stt.api.Transformator;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class VoskTransformator implements Transformator {

    @Data
    private static class RecognizeResult {
        private String text;
        private String partial;
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean listening;
    private volatile Long lastListen = System.currentTimeMillis();

    @SneakyThrows
    private String extractText(String json, boolean finished) {
        RecognizeResult result = mapper.readValue(json, RecognizeResult.class);

        if (finished)
            return result.text;
        else
            return result.partial;
    }

    private final Queue<TextPart> textParts = new LinkedBlockingDeque<>();

    private final Recognizer recognizer;
    private final Long silenceLag;

    @SneakyThrows
    public VoskTransformator(Model model, int sampleRate, Long silenceLag) {
        // Важно: слова должны быть в кавычках внутри квадратных скобок
        // String grammar = "[\"налево\", \"направо\", \"вперед\", \"назад\", \"[unk]\"]";

        this.recognizer = new Recognizer(model, sampleRate);
        this.silenceLag = silenceLag;
    }

    @Override
    public void pushAudioChunk(byte[] chunk) {
        if (null == chunk)
            return;

        boolean finished = recognizer.acceptWaveForm(chunk, chunk.length);

        String text;
        if (finished) {
            text = extractText(recognizer.getResult(), true);
        } else {
            text = extractText(recognizer.getPartialResult(), false);
        }

        if (!text.isBlank()) {
            listening = true;
            lastListen = System.currentTimeMillis();

            textParts.offer(new TextPart(text, finished));
        } else {
            if (System.currentTimeMillis() - lastListen > silenceLag)
                listening = false;
        }
    }

    @Override
    public boolean hasValue() {
        return !textParts.isEmpty();
    }

    @Override
    public TextPart popText() {
        return textParts.poll();
    }

    @Override
    public boolean isListeningNow() {
        return listening;
    }

    @Override
    public void close() {
        this.recognizer.close();
    }
}
