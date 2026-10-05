package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;

public class DistanceCalculator {

    public static int calculateDistance(Course course, Position src, Position target, int maxDist) {
        calculateDistance(course, src, maxDist);
        return CourseHandler.getFloor(course, target).getDistanceCounter();
    }

    public static void calculateDistance(Course course, Position position, int maxDist) {
        for (Floor f : course.getFloor()) {
            f.setDistanceCounter(maxDist);
        }
        countDistances(course, 0, CourseHandler.getFloor(course, position), maxDist);
    }

    private static void countDistances(Course course, int distance, Floor sourceFloor, int maxDist) {
        sourceFloor.setDistanceCounter(distance);
        if (course != null) {
            if (distance < maxDist
                    && sourceFloor.getPosition().x() >= course.getRange().minX() - 1
                    && sourceFloor.getPosition().y() >= course.getRange().minY() - 1
                    && sourceFloor.getPosition().x() <= course.getRange().maxX() + 1
                    && sourceFloor.getPosition().y() <= course.getRange().maxY() + 1) {

                Floor targetFloor = CourseHandler.getFloor(course, new Position(sourceFloor.getPosition().x(), sourceFloor.getPosition().y() - 1));
                increaseDistanceDependingOnWalls(course, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallNorth(), targetFloor.getWallSouth());

                targetFloor = CourseHandler.getFloor(course, new Position(sourceFloor.getPosition().x(), sourceFloor.getPosition().y() + 1));
                increaseDistanceDependingOnWalls(course, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallSouth(), targetFloor.getWallNorth());

                targetFloor = CourseHandler.getFloor(course, new Position(sourceFloor.getPosition().x() + 1, sourceFloor.getPosition().y()));
                increaseDistanceDependingOnWalls(course, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallEast(), targetFloor.getWallWest());

                targetFloor = CourseHandler.getFloor(course, new Position(sourceFloor.getPosition().x() - 1, sourceFloor.getPosition().y()));
                increaseDistanceDependingOnWalls(course, distance, maxDist, sourceFloor, targetFloor, sourceFloor.getWallWest(), targetFloor.getWallEast());
            }
        }
    }

    private static void increaseDistanceDependingOnWalls(Course course, int distance, int maxDist, Floor sourceFloor, Floor targetFloor, WallType ownWall, WallType targetWall) {
        if ((ownWall == WallType.RAMP_DOWN && targetWall == WallType.RAMP_UP)
                || (ownWall == WallType.RAMP_UP && targetWall == WallType.RAMP_DOWN)
                || (ownWall == WallType.LEDGE && (targetWall == WallType.NONE || targetWall == WallType.ONE_WAY_GREEN))
                || ((ownWall == WallType.NONE || ownWall == WallType.ONE_WAY_GREEN) && targetWall == WallType.LEDGE)) {
            if (targetFloor.getDistanceCounter() > (distance + 2)) {
                countDistances(course, distance + 2, sourceFloor, maxDist);
            }
        } else if (((ownWall == WallType.NONE || ownWall == WallType.ONE_WAY_GREEN || ownWall == WallType.RAMP_DOWN)
                && (targetWall == WallType.NONE || targetWall == WallType.ONE_WAY_GREEN || targetWall == WallType.RAMP_UP))
                || (ownWall == WallType.ONE_WAY_GREEN && targetWall == WallType.ONE_WAY_RED)) {
            if (targetFloor.getDistanceCounter() > (distance + 1)) {
                countDistances(course, distance + 1, sourceFloor, maxDist);
            }
        }
    }

}
