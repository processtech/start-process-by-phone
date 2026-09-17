package ru.evgeny.echo.tts.rhvoice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rhvoice")
public class RhVoiceProperties {
    private String baseUrl;
    private Voice voice = Voice.aleksandr;
    private Format format = Format.opus;
}
