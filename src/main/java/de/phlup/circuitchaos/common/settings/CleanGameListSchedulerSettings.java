package de.phlup.circuitchaos.common.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("circuit-chaos.server.clean-game-list-scheduler")
public class CleanGameListSchedulerSettings {

    @SuppressWarnings("unused")
    private int startDelaySeconds;
    @SuppressWarnings("unused")
    private int fixedDelaySeconds;
    private int evictTimePreparingMinutes;
    private int evictTimeRunningMinutes;

}
