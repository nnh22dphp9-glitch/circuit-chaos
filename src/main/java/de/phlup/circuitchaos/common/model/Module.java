package de.phlup.circuitchaos.common.model;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.ModuleType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@Data
@NoArgsConstructor
public class Module {

    @NonNull
    private ModuleType  type;
    private int         ammunition      = 0;
    private int[]       fuelUseIn       = new int[]{1, 1, 1, 1, 1};
    private boolean[]   activeInPhase   = new boolean[]{false, false, false, false, false};
    private Direction   direction       = null;   // in relation to robot!
    private boolean     layer           = false;
    private boolean[][] powerDownShield = new boolean[][]{{false, false, false, false}, {false, false, false, false}, {false, false, false, false}, {false, false, false, false}, {false, false, false, false}};
    private String      targetRobotId   = null;
    private boolean     alreadyUsed     = false;

    public Module(@NonNull ModuleType type) {
        this.type = type;
        if (type == ModuleType.POWER_DOWN_SHIELD) {
            powerDownShield = new boolean[][]{{true, true, true, true}, {true, true, true, true}, {true, true, true, true}, {true, true, true, true}, {true, true, true, true}};
        }
        if (type == ModuleType.SHIELD) {
            direction = Direction.NORTH;
        } else if (type == ModuleType.REAR_LASER) {
            direction = Direction.SOUTH;
        }
    }

}
