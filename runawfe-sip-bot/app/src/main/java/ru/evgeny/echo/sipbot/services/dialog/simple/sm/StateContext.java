package ru.evgeny.echo.sipbot.services.dialog.simple.sm;

import lombok.Data;

@Data
public class StateContext {
    private String phone;
    private String from;
    private String to;
}
