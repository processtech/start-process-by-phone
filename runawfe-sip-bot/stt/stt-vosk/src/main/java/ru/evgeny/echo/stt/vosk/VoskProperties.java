package ru.evgeny.echo.stt.vosk;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "vosk.model")
public class VoskProperties {
    private String dir;
    private long silenceLag = 789;
}
