package ru.evgeny.echo.sipbot.services.dialog.simple.sm;

public interface State {
    String initWords();

    State calcNext(String speech);

    StateContext getContext();
}
