package de.phlup.circuitchaos.gamelogic.utils;

import de.phlup.circuitchaos.enums.WallType;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.Floor;
import de.phlup.circuitchaos.model.Position;

public class DistanceCalculator {

    public static int calculateDistance(Board board, Position src, Position target, int maxDist) {
        calculateDistance(board, src, maxDist);
        return BoardHandler.getFloor(board, target).getDistanceCounter();
    }

    public static void calculateDistance(Board board, Position position, int maxDist) {
        for (Floor f : board.getFactoryFloor()) {
            f.setDistanceCounter(maxDist);
        }
        countDistances(board, 0, BoardHandler.getFloor(board, position), maxDist);
    }

    private static void countDistances(Board board, int distance, Floor sourceFloor, int maxDist) {
        sourceFloor.setDistanceCounter(distance);
        if (board != null) {
            if (distance < maxDist
                    && sourceFloor.getPosition().x() >= board.getRange().minX() - 1
                    && sourceFloor.getPosition().y() >= board.getRange().minY() - 1
                    && sourceFloor.getPosition().x() <= board.getRange().maxX() + 1
                    && sourceFloor.getPosition().y() <= board.getRange().maxY() + 1) {

                Floor targetFloor = BoardHandler.getFloor(board, new Position(sourceFloor.getPosition().x(), sourceFloor.getPosition().y() - 1));
                increaseDistanceDependingOnWalls(board, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallNorth(), targetFloor.getWallSouth());

                targetFloor = BoardHandler.getFloor(board, new Position(sourceFloor.getPosition().x(), sourceFloor.getPosition().y() + 1));
                increaseDistanceDependingOnWalls(board, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallSouth(), targetFloor.getWallNorth());

                targetFloor = BoardHandler.getFloor(board, new Position(sourceFloor.getPosition().x() + 1, sourceFloor.getPosition().y()));
                increaseDistanceDependingOnWalls(board, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallEast(), targetFloor.getWallWest());

                targetFloor = BoardHandler.getFloor(board, new Position(sourceFloor.getPosition().x() - 1, sourceFloor.getPosition().y()));
                increaseDistanceDependingOnWalls(board, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallWest(), targetFloor.getWallEast());
            }
        }
    }

    private static void increaseDistanceDependingOnWalls(Board board, int distance, int maxDist, Floor sourceFloor, Floor targetFloor, WallType ownWall, WallType targetWall) {
        if ((ownWall == WallType.RAMP_DOWN && targetWall == WallType.RAMP_UP)
                || (ownWall == WallType.RAMP_UP && targetWall == WallType.RAMP_DOWN)
                || (ownWall == WallType.LEDGE && (targetWall == WallType.NONE || targetWall == WallType.ONE_WAY_GREEN))
                || ((ownWall == WallType.NONE || ownWall == WallType.ONE_WAY_GREEN) && targetWall == WallType.LEDGE)) {
            if (targetFloor.getDistanceCounter() > (distance + 2)) {
                countDistances(board, distance + 2, sourceFloor, maxDist);
            }
        } else if (((ownWall == WallType.NONE || ownWall == WallType.ONE_WAY_GREEN || ownWall == WallType.RAMP_DOWN)
                && (targetWall == WallType.NONE || targetWall == WallType.ONE_WAY_GREEN || targetWall == WallType.RAMP_UP))
                || (ownWall == WallType.ONE_WAY_GREEN && targetWall == WallType.ONE_WAY_RED)) {
            if (targetFloor.getDistanceCounter() > (distance + 1)) {
                countDistances(board, distance + 1, sourceFloor, maxDist);
            }
        }
    }

}
