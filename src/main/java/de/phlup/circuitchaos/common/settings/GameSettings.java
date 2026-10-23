package de.phlup.circuitchaos.common.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "circuit-chaos.game.options")
public class GameSettings {

    private int maxNumberOfComputerPlayers;

}
