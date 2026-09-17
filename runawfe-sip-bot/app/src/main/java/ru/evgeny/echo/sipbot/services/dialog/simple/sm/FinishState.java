package ru.evgeny.echo.sipbot.services.dialog.simple.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.evgeny.echo.sipbot.services.dialog.Dialog;

@RequiredArgsConstructor
public class FinishState implements State {
    @Getter
    private final StateContext context;

    @Override
    public String initWords() {
        return "Мы заберем вас по адресу: " + context.getFrom() + " и отвезем вас по адресу:" + context.getTo() + " Спасибо за обращение, ожидайте машину " + Dialog.FINISH_TAG;
    }

    @Override
    public State calcNext(String speech) {
        return this;
    }
}
