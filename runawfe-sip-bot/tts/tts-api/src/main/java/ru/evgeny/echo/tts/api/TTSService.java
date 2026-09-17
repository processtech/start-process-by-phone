package ru.evgeny.echo.tts.api;

public interface TTSService {
    byte[] synthesize(String text);
}
