package de.phlup.circuitchaos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Floortype {

    OPEN_FLOOR("open floor"),
    ABYSS("abyss"),
    TRAPDOOR("trapdoor"),
    PIT_STOP("pit stop"),
    EXPRESS_CONVEYOR_BELT("express conveyor belt"),
    TURNING_EXPRESS_CONVEYOR_BELT_CCW("turning expr. conv. belt (ccw)"),
    TURNING_EXPRESS_CONVEYOR_BELT_CW("turning expr. conv. belt (cw)"),
    TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW("turning expr. conv. belt"),
    CONVEYOR_BELT("conveyor belt"),
    TURNING_CONVEYOR_BELT_CCW("turning conveyor belt (ccw)"),
    TURNING_CONVEYOR_BELT_CW("turning conveyor belt (cw)"),
    TURNING_CONVEYOR_BELT_CW_CCW("turning conveyor belt"),
    GEARS_CW("gears (cw)"),
    GEARS_CCW("gears (ccw)");

    private final String typeName;

    public boolean isConveyorBelt() {
        return this == EXPRESS_CONVEYOR_BELT ||
                this == TURNING_EXPRESS_CONVEYOR_BELT_CCW ||
                this == TURNING_EXPRESS_CONVEYOR_BELT_CW ||
                this == TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW ||
                this == CONVEYOR_BELT ||
                this == TURNING_CONVEYOR_BELT_CCW ||
                this == TURNING_CONVEYOR_BELT_CW ||
                this == TURNING_CONVEYOR_BELT_CW_CCW;
    }

    public boolean isSlowConveyorBelt() {
        return this == CONVEYOR_BELT ||
                this == TURNING_CONVEYOR_BELT_CCW ||
                this == TURNING_CONVEYOR_BELT_CW ||
                this == TURNING_CONVEYOR_BELT_CW_CCW;
    }

    public boolean isExpressConveyorBelt() {
        return this == EXPRESS_CONVEYOR_BELT ||
                this == TURNING_EXPRESS_CONVEYOR_BELT_CCW ||
                this == TURNING_EXPRESS_CONVEYOR_BELT_CW ||
                this == TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW;
    }
}

