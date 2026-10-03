package de.phlup.circuitchaos.enums;

import static de.phlup.circuitchaos.gamelogic.GlobalServerAttributes.RANDOM;

public enum Direction {

    NORTH, // aka FORWARD
    EAST, // aka TURN_RIGHT
    SOUTH, // aka BACKWARD
    WEST, // aka TURN_LEFT
    ;

    public Direction add(Direction direction) {
        return switch (direction) {
            case null -> this;
            case NORTH -> this;
            case SOUTH -> switch (this) {
                case NORTH -> SOUTH;
                case EAST -> WEST;
                case SOUTH -> NORTH;
                case WEST -> EAST;
            };
            case EAST -> switch (this) {
                case NORTH -> EAST;
                case EAST -> SOUTH;
                case SOUTH -> WEST;
                case WEST -> NORTH;
            };
            case WEST -> switch (this) {
                case NORTH -> WEST;
                case EAST -> NORTH;
                case SOUTH -> EAST;
                case WEST -> SOUTH;
            };
        };
    }

    public Direction minus(Direction direction) {
        return switch (direction) {
            case null -> this;
            case NORTH -> this;
            case SOUTH -> switch (this) {
                case NORTH -> SOUTH;
                case EAST -> WEST;
                case SOUTH -> NORTH;
                case WEST -> EAST;
            };
            case EAST -> switch (this) {
                case NORTH -> WEST;
                case EAST -> NORTH;
                case SOUTH -> EAST;
                case WEST -> SOUTH;
            };
            case WEST -> switch (this) {
                case NORTH -> EAST;
                case EAST -> SOUTH;
                case SOUTH -> WEST;
                case WEST -> NORTH;
            };
        };
    }

    public Direction reverse() {
        return add(SOUTH);
    }

    public static Direction random() {
        return switch (RANDOM.nextInt(4)) {
            case 0 -> NORTH;
            case 1 -> EAST;
            case 2 -> SOUTH;
            default -> WEST;
        };
    }

}
