package de.phlup.circuitchaos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProgramType {

    MOVE_1("Move 1", 1),
    MOVE_2("Move 2", 2),
    MOVE_3("Move 3", 3),
    MOVE_4("Move 4", 4),
    BACKUP("Back-Up", -1),
    BACKUP_2("Back-Up 2", -2),
    ROTATE_LEFT("Turn Left", 0),
    ROTATE_RIGHT("Turn Right", 0),
    U_TURN("U-Turn", 0);

    private final String command;
    private final int    movement;

    public boolean isMovement() {
        return movement != 0;
    }

}
