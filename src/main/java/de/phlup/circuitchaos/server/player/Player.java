package de.phlup.circuitchaos.server.player;

import de.phlup.circuitchaos.common.model.Module;
import de.phlup.circuitchaos.common.model.Robot;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import static de.phlup.circuitchaos.common.enums.ModuleType.POWER_DOWN_SHIELD;

@Data
public abstract class Player {

    protected final Robot  robot;
    protected final String gameId;

    protected Player(@NotNull Robot robot, @NotNull String gameId) {
        this.robot = robot;
        this.gameId = gameId;
    }

    protected void initTurn() {
        for (Module module : robot.getModules()) {
            module.setPowerDownShield(new boolean[][]{{true, true, true, true}, {true, true, true, true}, {true, true, true, true}, {true, true, true, true}, {true, true, true, true}});
            module.setAlreadyUsed(false);
            for (int cnt = 0; cnt < 5; cnt++) {
                if (module.getType() == POWER_DOWN_SHIELD) {
                    module.getActiveInPhase()[cnt] = robot.isPoweredDown();
                } else {
                    module.getActiveInPhase()[cnt] = module.getType().isDefaultActiveInPhase();
                }
            }
        }
    }

}
