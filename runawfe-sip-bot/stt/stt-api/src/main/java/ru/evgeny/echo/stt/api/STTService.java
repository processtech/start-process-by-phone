package ru.evgeny.echo.stt.api;

public interface STTService {
    Transformator createTransformator(int sampleRate);
}
