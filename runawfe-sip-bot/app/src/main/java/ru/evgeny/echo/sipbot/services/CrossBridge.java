package ru.evgeny.echo.sipbot.services;

import lombok.Data;
import lombok.SneakyThrows;
import ru.evgeny.echo.sipbot.services.dialog.Dialog;
import ru.evgeny.echo.sipbot.services.sockets.RtpSocket;
import ru.evgeny.echo.sipbot.services.sockets.SipSocket;
import ru.evgeny.echo.stt.api.Transformator;

@Data
public class CrossBridge {
    Dialog dialog;
    RtpSocket rtpSocket;
    SipSocket sipSocket;
    Transformator transformator;

    public void userSay(String s) {
        dialog.userSay(s);
    }

    public void sendAudio(byte[] bytes) {
        rtpSocket.sendData(bytes, true);
    }

    @SneakyThrows
    public void closeTransformator() {
        if (null != transformator)
            transformator.close();
    }
}