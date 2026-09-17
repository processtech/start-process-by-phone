package ru.evgeny.echo.sipbot.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.evgeny.echo.sipbot.services.sockets.SipSocket;

@RequiredArgsConstructor
public class MediaInfo {
    @Getter
    private final int sampleRate;
    @Getter
    private final int channels;
    @Getter
    private final int duration;
    @Getter
    private final int bitDepth;
    @Getter
    private final int frameSize;
    @Getter
    private final String sockId;

    public static MediaInfo fromCommand(String cmsCmd) {
        String[] parts = cmsCmd.substring(SipSocket.CALL_MEDIA_STATE.length()).trim().split("\\|");

        int sampleRate = Integer.parseInt(parts[0]);
        int channels = Integer.parseInt(parts[1]);
        int duration = Integer.parseInt(parts[2]);
        int bitDepth = Integer.parseInt(parts[3]);
        String sockId = parts[4];

        int frameSize = (sampleRate / 1000) * channels * (bitDepth / 8) * (duration / 1000);

        return new MediaInfo(
                sampleRate, channels, duration, bitDepth, frameSize, sockId
        );
    }

    public static MediaInfo fromSockFile(String fileName) {
        String[] parts = fileName.split("_");

        String sockId = parts[0];
        int sampleRate = Integer.parseInt(parts[1]);
        int channels = Integer.parseInt(parts[2]);
        int duration = Integer.parseInt(parts[3]);
        int bitDepth = Integer.parseInt(parts[4]);


        int frameSize = (sampleRate / 1000) * channels * (bitDepth / 8) * (duration / 1000);

        return new MediaInfo(
                sampleRate, channels, duration, bitDepth, frameSize, sockId
        );
    }
}
