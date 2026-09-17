package ru.evgeny.echo.sipbot.services.dialog;

import java.util.UUID;
import java.util.function.Consumer;

public interface Dialog {
    public static final String FINISH_TAG = "<HANGUP/>";

    UUID getUuid();

    Consumer<String> systemAnswerConsumer();

    void userSay(String text);
}
