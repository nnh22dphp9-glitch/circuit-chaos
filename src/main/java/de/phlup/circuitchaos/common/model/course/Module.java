package de.phlup.circuitchaos.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private boolean[]   activeInPhase   = new boolean[]{false, false, false, false, false};
    private Direction   direction       = null;   // in relation to robot!
    private boolean[][] powerDownShield = new boolean[][]{{false, false, false, false}, {false, false, false, false}, {false, false, false, false}, {false, false, false, false}, {false, false, false, false}};

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

    @JsonIgnore
    public Module copy() {
        Module module = new Module();
        module.setType(type);
        module.setAmmunition(ammunition);
        System.arraycopy(activeInPhase, 0, module.getActiveInPhase(), 0, 5);
        for (int i = 0; i < 5; i++) {
            System.arraycopy(powerDownShield[i], 0, module.getPowerDownShield()[i], 0, 4);
        }
        return module;
    }

}
