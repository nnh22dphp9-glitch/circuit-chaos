package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProgramType {

    MOVE_1(1),
    MOVE_2(2),
    MOVE_3(3),
    MOVE_4(4),
    BACKUP(-1),
    BACKUP_2(-2),
    ROTATE_LEFT(0),
    ROTATE_RIGHT(0),
    U_TURN(0);

    private final int movement;

    public boolean isMovement() {
        return movement != 0;
    }

}
