package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Goal;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class GameOptions {

    private Goal         goal                       = Goal.FINISH;
    private List<String> defaultModules             = new ArrayList<>();
    private int          maxNumberOfComputerPlayers = 0;

}
