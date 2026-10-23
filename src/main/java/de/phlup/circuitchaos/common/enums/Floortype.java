package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Floortype {

    OPEN_FLOOR("open floor"),
    ABYSS("abyss"),
    TRAPDOOR("trapdoor"),
    PIT_STOP("pit stop"),
    GEARS_CW("gears (clockwise)"),
    GEARS_CCW("gears (counter clockwise)"),
    CONVEYOR_BELT("conveyor belt"),
    TURNING_CONVEYOR_BELT_CW_CCW("turning conveyor belt"),
    TURNING_CONVEYOR_BELT_CW("turning conveyor belt (clockwise)"),
    TURNING_CONVEYOR_BELT_CCW("turning conveyor belt (counter clockwise)"),
    EXPRESS_CONVEYOR_BELT("express conveyor belt"),
    TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW("turning express conveyor belt"),
    TURNING_EXPRESS_CONVEYOR_BELT_CW("turning express conveyor belt (clockwise)"),
    TURNING_EXPRESS_CONVEYOR_BELT_CCW("turning express conveyor belt (counter clockwise)"),
    ;

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

    public String toString() {
        return typeName;
    }
}

