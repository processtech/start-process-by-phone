package ru.evgeny.echo.sipbot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.evgeny.echo.sipbot.services.dialog.Dialog;
import ru.evgeny.echo.sipbot.struct.LRUCache;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class DialogCash {

    public static final Map<UUID, Dialog> CACHE = new LRUCache<>(10);

}
