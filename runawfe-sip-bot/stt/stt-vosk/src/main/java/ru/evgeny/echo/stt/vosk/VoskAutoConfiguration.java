package ru.evgeny.echo.stt.vosk;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.evgeny.echo.stt.api.STTService;

@AutoConfiguration
@EnableConfigurationProperties(VoskProperties.class)
@ConditionalOnClass(VoskSTTService.class)
@ConditionalOnProperty(
        prefix = "stt",
        name = "type",
        havingValue = "vosk"
)
@ConditionalOnProperty({"vosk.model.dir"})
public class VoskAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public STTService voskSTTService(VoskProperties properties) {
        return new VoskSTTService(
                properties.getDir(),
                properties.getSilenceLag()
        );
    }
}
