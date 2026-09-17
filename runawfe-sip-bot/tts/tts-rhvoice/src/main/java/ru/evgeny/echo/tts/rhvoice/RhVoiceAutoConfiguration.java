package ru.evgeny.echo.tts.rhvoice;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.evgeny.echo.tts.api.TTSService;

@AutoConfiguration
@EnableConfigurationProperties(RhVoiceProperties.class)
@ConditionalOnClass(RhVoiceTTSService.class)
@ConditionalOnProperty(
        prefix = "tts",
        name = "type",
        havingValue = "rhvoice"
)
@ConditionalOnProperty({"rhvoice.base-url"})
public class RhVoiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TTSService rhVoiceTTSService(RhVoiceProperties properties) {
        return new RhVoiceTTSService(
                properties.getBaseUrl(),
                properties.getVoice(),
                properties.getFormat()
        );
    }
}
