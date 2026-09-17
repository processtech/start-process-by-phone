package ru.evgeny.echo.sipbot.services.dialog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.evgeny.echo.sipbot.services.clients.CommonWfeRestClient;
import ru.evgeny.echo.sipbot.services.dialog.simple.SimpleDialog;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class DialogFactory {
    private final List<String> waitMessages;
    private final CommonWfeRestClient wfeRestClient;

    public DialogFactory(
            @Value("${app.wait-messages}") String waitMessagesStr,
            CommonWfeRestClient wfeTaxiRestClient
    ) {
        this.waitMessages = List.of(waitMessagesStr.split("\\|"));
        this.wfeRestClient = wfeTaxiRestClient;
    }

    public Dialog createSimpleDialog(String currentClientSip, Consumer<String> systemAnswerConsumer) {
        log.info("Create simple dialog for {}", currentClientSip);
        return new SimpleDialog(currentClientSip, wfeRestClient, systemAnswerConsumer);
    }

}
