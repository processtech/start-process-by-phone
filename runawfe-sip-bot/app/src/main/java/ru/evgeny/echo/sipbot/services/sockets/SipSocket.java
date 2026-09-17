package ru.evgeny.echo.sipbot.services.sockets;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.evgeny.echo.sipbot.struct.MediaInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

@Slf4j
public class SipSocket extends AbstractSocket {
    public static final String READY = "APP_ACCOUNT_READY";
    public static final String DOWN = "APP_ACCOUNT_DOWN";
    public static final String CALL_STATE = "onCallState:";
    public static final String CALL_MEDIA_STATE = "onCallMediaState:";

    @Getter
    private volatile boolean ready;
    @Getter
    private volatile MediaInfo mediaInfo;
    @Getter
    private volatile RtpSocket rtpSocket;

    private final String sockDir;
    private volatile String currentClientSip;

    public Consumer<byte[]> onRtpData = bytes -> {
    };

    public Consumer<RtpSocket> onRtpSocketCreated = rtpSocket -> {
    };

    private final Thread rtpSocketUpdater;

    public SipSocket(String sockDir,
            String sockName) {
        super(Path.of(sockDir).resolve(sockName));
        this.sockDir = sockDir;

        this.rtpSocketUpdater = new Thread(() -> {
            File sockDirFile = Path.of(sockDir).toFile();
            while (!Thread.interrupted()) {
                for (String filename : Objects.requireNonNull(sockDirFile.list())) {
                    if (sockName.equals(filename))
                        continue;

                    if ("_client".equals(filename))
                        continue;

                    if (tryUpdateRtpSocket(MediaInfo.fromSockFile(filename), filename))
                        break;
                }
                LockSupport.parkNanos(10_000_000);
            }
        });
        this.rtpSocketUpdater.start();
    }

    @SneakyThrows
    @Override
    public void stop() {
        super.stop();

        rtpSocketUpdater.interrupt();
        rtpSocketUpdater.join();
    }

    @Override
    public int frameDurationMs() {
        // не притормаживаем
        return 0;
    }

    @Override
    public int bufSize() {
        return 32 * 1024;
    }

    @Override
    public void receiveData(byte[] bytes) {
        String command = new String(bytes);


        if (command.equals(READY) && !ready) {
            log.info("SipControlSocket: {}", command);
            ready = true;
            return;
        }

        if (command.equals(DOWN) && ready) {
            log.info("SipControlSocket: {}", command);
            ready = false;
            return;
        }


        if (command.startsWith(CALL_STATE)) {
            log.info("SipControlSocket: {}", command);

            String data = command.substring(CALL_STATE.length()).trim();
            log.info("data {}", data);
            String[] parts = data.split("\\|");

            if ("INCOMING".equals(parts[1])) {
                currentClientSip = extractPhoneNumber(parts[0]);
                log.info("currentClientSip {}", currentClientSip);
            }

            if ("DISCONNECTED".equals(parts[1])) {
                currentClientSip = null;
                mediaInfo = null;
                if (null != rtpSocket) {
                    rtpSocket.stop();
                    rtpSocket = null;
                }
            }
        }

        /*
        if (command.startsWith(CALL_MEDIA_STATE)) {
            log.info("SipControlSocket: {}", command);
            tryUpdateRtpSocket(MediaInfo.fromCommand(command));
        }
        */
    }

    public void sendData(String command) {
        super.sendData(command.getBytes(StandardCharsets.UTF_8), false);
    }


    private boolean tryUpdateRtpSocket(MediaInfo candidate, String sockFile) {
        if (null == mediaInfo || !Objects.equals(mediaInfo.getSockId(), candidate.getSockId())) {
            mediaInfo = candidate;

            if (null != rtpSocket) {
                rtpSocket.stop();
                rtpSocket = null;
            }

            if (currentClientSip == null) {
                log.warn("currentClientSip is null, waiting briefly for INCOMING command...");
                LockSupport.parkNanos(50_000_000); // 50 мс
            }

            log.info("currentClientSip1 {}", currentClientSip);
            rtpSocket = new RtpSocket(
                    Path.of(sockDir).resolve(sockFile),
                    mediaInfo, onRtpData, currentClientSip
            );

            onRtpSocketCreated.accept(rtpSocket);

            return true;
        }

        return false;
    }

    /**
     * Извлекает чистый номер телефона из SIP URI.
     * Примеры входных данных:
     *   "79122223228" <sip:79122223228@266191.14.rt.ru>  -> 79122223228
     *   sip:79122223228@266191.14.rt.ru                 -> 79122223228
     *   79122223228                                     -> 79122223228
     */
    private String extractPhoneNumber(String sipUri) {
        if (sipUri == null || sipUri.isEmpty()) {
            return sipUri;
        }

        // Вариант 1: Ищем цифры в кавычках в начале строки
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"(\\d+)\"").matcher(sipUri);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Вариант 2: Ищем цифры после "sip:"
        matcher = java.util.regex.Pattern.compile("sip:(\\d+)@").matcher(sipUri);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Вариант 3: Если это уже просто цифры - возвращаем как есть
        String trimmed = sipUri.trim();
        if (trimmed.matches("\\+?\\d+")) {
            return trimmed.replaceAll("^\\+", ""); // убираем возможный "+"
        }

        // Фоллбэк: возвращаем обрезанную строку
        return trimmed;
    }
}
