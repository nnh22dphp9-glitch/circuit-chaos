package de.phlup.circuitchaos.model;

import de.phlup.circuitchaos.enums.Direction;

public record Position(int x, int y) {

    public boolean inRange(Range range) {
        return x >= range.minX()
                && x <= range.maxY()
                && y >= range.minY()
                && y <= range.maxY();
    }

    public Position neighbour(Direction direction) {
        int targetX = x;
        int targetY = y;
        switch (direction) {
            case NORTH -> targetY = targetY - 1;
            case EAST -> targetX = targetX + 1;
            case SOUTH -> targetY = targetY + 1;
            case WEST -> targetX = targetX - 1;
        }
        return new Position(targetX, targetY);
    }

    public boolean isNeighbor(Position pos2) {
        return (pos2.x() == x && pos2.y() == y + 1)
                || (pos2.x() == x && pos2.y() == y - 1)
                || (pos2.y() == y && pos2.x() == x + 1)
                || (pos2.y() == y && pos2.x() == x - 1);
    }

}
