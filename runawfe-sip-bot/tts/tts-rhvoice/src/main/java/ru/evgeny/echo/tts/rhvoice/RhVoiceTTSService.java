package ru.evgeny.echo.tts.rhvoice;

import lombok.SneakyThrows;
import ru.evgeny.echo.tts.api.TTSService;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class RhVoiceTTSService implements TTSService {
    private final String baseUrl;
    private final Voice voice;
    private final Format format;

    // Создаем один переиспользуемый клиент с явными настройками
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) // Таймаут на установку соединения
            .build();

    public RhVoiceTTSService(String baseUrl, Voice voice, Format format) {
        this.baseUrl = baseUrl;
        this.voice = voice;
        this.format = format;
    }

    @SneakyThrows
    @Override
    public byte[] synthesize(String text) {
        String target = String.format("%s/say?voice=%s&format=%s&text=%s",
                baseUrl, voice, format, URLEncoder.encode(text, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        long startTime = System.currentTimeMillis();

        HttpResponse<byte[]> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        long duration = System.currentTimeMillis() - startTime;

        if (response.statusCode() != 200) {
            throw new RuntimeException("RhVoice вернул ошибку: " + response.statusCode());
        }

        byte[] audio = response.body();
        return FfmpegUtils.convertToPCM(audio, format.name());
    }
}