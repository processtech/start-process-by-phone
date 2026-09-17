package ru.evgeny.echo.stt.api;

import java.io.Closeable;

public interface Transformator extends Closeable {
    void pushAudioChunk(byte[] data);

    boolean hasValue();

    TextPart popText();

    boolean isListeningNow();
}
