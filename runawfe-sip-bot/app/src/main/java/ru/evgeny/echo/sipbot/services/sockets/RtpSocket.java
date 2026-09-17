package ru.evgeny.echo.sipbot.services.sockets;

import lombok.Getter;
import ru.evgeny.echo.sipbot.struct.MediaInfo;

import java.nio.file.Path;
import java.util.function.Consumer;

public class RtpSocket extends AbstractSocket {
    private final int bufSize;
    private final Consumer<byte[]> onData;

    @Getter
    private final MediaInfo mediaInfo;
    @Getter
    private final String currentClientSip;

    public RtpSocket(Path sockPath, MediaInfo mediaInfo,
                     Consumer<byte[]> onData, String currentClientSip) {
        super(sockPath);
        this.bufSize = mediaInfo.getFrameSize();
        this.mediaInfo = mediaInfo;
        this.onData = onData;
        this.currentClientSip = currentClientSip;
    }

    @Override
    public int frameDurationMs() {
        return mediaInfo.getDuration() / 1000;
    }

    @Override
    public int bufSize() {
        return bufSize;
    }

    @Override
    public void receiveData(byte[] data) {
        onData.accept(data);
    }


}
