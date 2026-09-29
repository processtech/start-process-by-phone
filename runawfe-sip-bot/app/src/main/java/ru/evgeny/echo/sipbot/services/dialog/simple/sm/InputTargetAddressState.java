package ru.evgeny.echo.sipbot.services.dialog.simple.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InputTargetAddressState implements State {
    @Getter
    private final StateContext context;

    @Override
    public String initWords() {
        return "Введите адрес назначения";
    }

    @Override
    public State calcNext(String speech) {
        context.setTo(speech);

        return  new FinishState(context);
    }
}
