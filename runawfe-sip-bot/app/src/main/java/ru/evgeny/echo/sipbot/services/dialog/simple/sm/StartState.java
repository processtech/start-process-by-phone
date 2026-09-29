package ru.evgeny.echo.sipbot.services.dialog.simple.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StartState implements State {
    @Getter
    private final StateContext context;

    @Override
    public String initWords() {
        return "Здравствуйте, это Роботакси, откуда вас забрать?";
    }

    @Override
    public State calcNext(String speech) {
        context.setFrom(speech);
        return new InputTargetAddressState(context);
    }
}
