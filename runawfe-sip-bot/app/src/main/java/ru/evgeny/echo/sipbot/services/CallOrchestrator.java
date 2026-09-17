package ru.evgeny.echo.sipbot.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.evgeny.echo.sipbot.services.dialog.Dialog;
import ru.evgeny.echo.sipbot.services.dialog.DialogFactory;
import ru.evgeny.echo.sipbot.services.sockets.SipSocket;
import ru.evgeny.echo.stt.api.STTService;
import ru.evgeny.echo.stt.api.TextPart;
import ru.evgeny.echo.stt.api.Transformator;
import ru.evgeny.echo.tts.api.TTSService;

@Slf4j
@Service
public class CallOrchestrator {
    private final String sockDir;
    private final String nameSockControl;

    private final STTService sttService;
    private final TTSService ttsService;

    private final DialogFactory dialogFactory;

    private int ttsRequestCount = 0;

    private final CrossBridge bridge = new CrossBridge();

    public CallOrchestrator(@Value("${app.sock.dir}") String sockDir,
                            @Value("${app.sock.control}") String nameSockControl,
                            STTService sttService,
                            TTSService ttsService,
                            DialogFactory dialogFactory) {
        this.sockDir = sockDir;
        this.nameSockControl = nameSockControl.trim();
        this.sttService = sttService;
        this.ttsService = ttsService;
        this.dialogFactory = dialogFactory;
    }


    @PostConstruct
    private void init() {
        SipSocket sipSocket = new SipSocket(sockDir, nameSockControl);
        bridge.setSipSocket(sipSocket);

        sipSocket.onRtpSocketCreated = rtpSocket -> {
            // по сути ставится 1 раз
            if (null == bridge.getTransformator()) {
                bridge.setTransformator(sttService.createTransformator(rtpSocket.getMediaInfo().getSampleRate()));
            }

            bridge.setRtpSocket(rtpSocket);

            Dialog dialog = dialogFactory.createSimpleDialog(rtpSocket.getCurrentClientSip(),
                    answer -> {
                        boolean needhang = false;
                        if (answer.contains(Dialog.FINISH_TAG)) {
                            needhang = true;
                            answer = answer.replace(Dialog.FINISH_TAG, "");
                        }

                        log.info("Answer: {}", answer);

                        log.warn("!!! ВЫЗОВ TTS (Количество запросов к RhVoice увеличивается) для фразы: {}", answer);

                        byte[] sound = ttsService.synthesize(answer);
                        log.info("Sound {} bytes", sound.length);
                        bridge.sendAudio(sound);

                        if (needhang) {
                            rtpSocket.setAfterDataSentCallback(() -> {
                                sipSocket.sendData("HANGUP|" + rtpSocket.getMediaInfo().getSockId());
                            });
                        }
                    });
            bridge.setDialog(dialog);
            //
            DialogCash.CACHE.put(dialog.getUuid(), dialog);
        };

        sipSocket.onRtpData = bytes -> {
            Transformator transformator = bridge.getTransformator();
            if (null != transformator) {
                transformator.pushAudioChunk(bytes);

                if (transformator.hasValue()) {
                    TextPart textPart = transformator.popText();
                    if (textPart.finished() && StringUtils.isNotBlank(textPart.text())) {
                        bridge.userSay(textPart.text());
                    }
                }
            } else {
                log.info("Transformator is NULL");
            }
        };
    }

    @SneakyThrows
    @PreDestroy
    private void destroy() {
        bridge.closeTransformator();
    }

}
