package de.phlup.circuitchaos.common.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "circuit-chaos.game.waiting-times-in-milliseconds")
public class TimeSettings {

    private int timeBetweenSteps;
    private int timeToShowExplosions;
    private int questionTimeout;
    private int sleepTimeBetweenAnswerPolling;
    private int animationSteps;

}
