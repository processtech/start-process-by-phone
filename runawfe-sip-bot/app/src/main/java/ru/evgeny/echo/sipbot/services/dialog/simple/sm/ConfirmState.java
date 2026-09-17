package ru.evgeny.echo.sipbot.services.dialog.simple.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfirmState implements State {
    private final String confirmMessage;
    private final State previousState;
    private final State nextState;
    @Getter
    private final StateContext context;

    @Override
    public String initWords() {
        return confirmMessage + " ... Ответьте да или нет.";
    }

    @Override
    public State calcNext(String speech) {
        // Приводим к нижнему регистру и убираем пробелы для надежного сравнения
        String cleanSpeech = speech.trim().toLowerCase();

        // Расширяем варианты положительного ответа (Vosk часто слышит "ага" или "да" с шумом)
        if (cleanSpeech.contains("да") || cleanSpeech.contains("ага") || cleanSpeech.contains("верно")) {
            return nextState;
        }

        // Расширяем варианты отрицательного ответа
        if (cleanSpeech.contains("нет") || cleanSpeech.contains("не")) {
            return previousState;
        }

        // ИСПРАВЛЕНИЕ: Если пользователь сказал что-то непонятное,
        // даем ему другой, более строгий запрос, чтобы не зацикливаться одним и тем же
        return new ConfirmState("Я не расслышал. Пожалуйста, ответьте четко: ДА или НЕТ", previousState, nextState, context);
    }
}