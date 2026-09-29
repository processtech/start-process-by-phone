package ru.evgeny.echo.sipbot.services.dialog.simple;

import java.util.concurrent.locks.LockSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.evgeny.echo.sipbot.services.clients.CommonWfeRestClient;
import ru.evgeny.echo.sipbot.services.dialog.Dialog;
import ru.evgeny.echo.sipbot.services.dialog.simple.sm.FinishState;
import ru.evgeny.echo.sipbot.services.dialog.simple.sm.StartState;
import ru.evgeny.echo.sipbot.services.dialog.simple.sm.State;
import ru.evgeny.echo.sipbot.services.dialog.simple.sm.StateContext;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class SimpleDialog implements Dialog {
    public static final int IMMUNITY_INTERVAL_MS = 789;

    private final Consumer<String> systemAnswerConsumer;
    private final Thread worker;
    private final List<String> phrases = Collections.synchronizedList(new ArrayList<>());
    private volatile long lastPhraseTime = System.currentTimeMillis();

    @Getter
    private final UUID uuid = UUID.randomUUID();
    private final StateContext context = new StateContext();
    private State state = new StartState(context);

    public SimpleDialog(String currentClientSip,
            CommonWfeRestClient wfeRestClient,
            Consumer<String> systemAnswerConsumer) {

        this.context.setPhone(currentClientSip);
        this.systemAnswerConsumer = systemAnswerConsumer;
        this.worker = new Thread(() -> {
            log.info("Dialog started");
            systemAnswerConsumer.accept(state.initWords()); //Приветствие StartState
            log.info("Speech started");
            while (!Thread.interrupted()) {
                if (!phrases.isEmpty() && (lastPhraseTime + IMMUNITY_INTERVAL_MS < System.currentTimeMillis())) {
                    String speech = String.join("\n", phrases);
                    log.info("User: {}", speech);
                    state = state.calcNext(speech);
                    //StartState ->InputTargetAddressState->FinishState
                    systemAnswerConsumer.accept(state.initWords()); //InputTargetAddressState->FinishState

                    if (state instanceof FinishState) {
                        String phone = context.getPhone() != null ? context.getPhone() : "Неизвестный номер";
                        String from = context.getFrom() != null ? context.getFrom() : "Неизвестно";
                        String to = context.getTo() != null ? context.getTo() : "Неизвестно";

                        log.info("Taxi: {}, {} -> {}", phone, from, to);
                        boolean isSuccess = false;
                        try {
                            isSuccess = wfeRestClient.startTaxiProcess("Такси", Map.of(
                                    "Телефон", phone,
                                    "Адрес", "%s -=-> %s".formatted(context.getFrom(), context.getTo())
                            ));
                            log.info("Запрос был отправлен в RunaWFE");
                        } catch (Exception e) {
                            log.error("Ошибка при отправке запроса в RunaWFE", e);
                        }

                        try {
                            Thread.sleep(8500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        // Формируем итоговую фразу в зависимости от результата
                        if (isSuccess) {
                            log.info("Запрос успешно отправлен в RunaWFE.");
                            systemAnswerConsumer.accept("Спасибо за обращение, ожидайте машину. " + Dialog.FINISH_TAG);
                        } else {
                            log.info("! Запрос неуспешно отправлен в RunaWFE");
                            systemAnswerConsumer.accept("Извините, возникли технические проблемы. Пожалуйста, попробуйте позвонить позже. " + Dialog.FINISH_TAG);
                        }

                        break;
                    } else {
                        phrases.clear();
                    }

                } else {
                    // небольшая пауза, чтобы не нагружать процессор
                    LockSupport.parkNanos(10_000_000); // 10 мс
                }
            }
        });
        this.worker.start();
    }

    public void stop() {
        if (null != worker)
            worker.interrupt();
    }

    @Override
    public Consumer<String> systemAnswerConsumer() {
        return systemAnswerConsumer;
    }

    @Override
    public void userSay(String text) {
        lastPhraseTime = System.currentTimeMillis();
        phrases.add(text);
        log.info("userSay: {}", text);
    }
}
