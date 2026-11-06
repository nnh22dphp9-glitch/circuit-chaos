package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.Goal;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.model.course.Checkpoint;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.CourseElement;
import de.phlup.circuitchaos.common.model.course.CourseObject;
import de.phlup.circuitchaos.common.model.course.Floor;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Programme;
import de.phlup.circuitchaos.server.game.DistanceCalculator;
import de.phlup.circuitchaos.server.game.GameLogic;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class MoveInfo {

    private final Goal       goal;
    private final GameLogic  simulationTemplate;
    private final Checkpoint nextCP;
    private Course    course;
    private GameLogic gameLogic;
    private Robot     myself;
    private final String robotId;

    private Programme[] program;
    private Direction   startingDirection;
    private boolean     willDie;
    private int         reachesCheckpoint;
    private int         damage;
    private boolean     reachesPitStop;
    private boolean     copiesArchive;
    private boolean     standsOnOpenFloor;
    private boolean     keepsStaying;
    private boolean     facesTowardsCheckpoint;
    private int         x;
    private int         y;
    private int         distance;
    private double      distance2;
    private Direction   endingDirection;
    private int         probableTargets;

    private final int initialX;
    private final int initialY;
    private final int initialDamage;
    private final int initialDieCounter;

    public MoveInfo(GameLogic gameLogic, Goal goal, Checkpoint nextCP, Robot originalRobot) {
        this.simulationTemplate = gameLogic;
        this.goal = goal;
        this.nextCP = nextCP;
        this.robotId = originalRobot.getId();
        this.initialX = originalRobot.getPosition().x();
        this.initialY = originalRobot.getPosition().y();
        this.initialDamage = originalRobot.getDamage();
        this.initialDieCounter = originalRobot.getDieCounter();
        this.x = initialX;
        this.y = initialY;
    }

    public void simulateCourse() {
        gameLogic = simulationTemplate.createSimulationCopy();
        course = gameLogic.getCourse();
        myself = course.getRobots().stream()
                .filter(robot -> Objects.equals(robot.getId(), robotId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Robot not found in simulation course: " + robotId));
        distance = Integer.MAX_VALUE;
        endingDirection = startingDirection;
        reachesCheckpoint = 5;
        reachesPitStop = false;
        copiesArchive = false;
        standsOnOpenFloor = false;
        keepsStaying = false;
        facesTowardsCheckpoint = false;
        probableTargets = 0;

        myself.setDirection(startingDirection);
        myself.setProgram(program.clone());

        for (int phase = 0; phase < 5 && myself.isOnCourse(); phase++) {
            gameLogic.stepRobotsMove(phase, List.of(myself));
            gameLogic.stepCourseElementsMove(phase);
            gameLogic.stepResolveBeamsAndLaserFire(phase);
            touchCourseElements(phase);
            calculateProbableTargets();
        }

        Floor finalFloor = CourseHandler.getFloor(course, myself.getPosition());
        x = myself.getPosition().x();
        y = myself.getPosition().y();
        endingDirection = myself.getDirection();
        willDie = myself.getDieCounter() > initialDieCounter || !myself.isOnCourse();
        damage = myself.getDamage() - initialDamage;

        if (myself.isOnCourse()) {
            standsOnOpenFloor = !finalFloor.getFloortype().isConveyorBelt()
                    && finalFloor.getFloortype() != Floortype.GEARS_CW
                    && finalFloor.getFloortype() != Floortype.GEARS_CCW
                    && !finalFloor.isHasPusher();
        }
        keepsStaying = x == initialX && y == initialY;

        if (nextCP != null) {
            switch (endingDirection) {
                case NORTH -> facesTowardsCheckpoint = nextCP.getPosition().y() <= y;
                case EAST -> facesTowardsCheckpoint = nextCP.getPosition().x() >= x;
                case SOUTH -> facesTowardsCheckpoint = nextCP.getPosition().y() >= y;
                case WEST -> facesTowardsCheckpoint = nextCP.getPosition().x() <= x;
            }
            calculateDistance2();
        }

        // A MoveInfo retains only its evaluation result, not a full copied course.
        myself = null;
        gameLogic = null;
        course = null;
    }

    private void touchCourseElements(int phase) {
        if (!myself.isOnCourse()) {
            return;
        }

        Position position = myself.getPosition();
        Floor standingFloor = CourseHandler.getFloor(course, position);
        Floor checkpointFloor = standingFloor;
        Checkpoint checkpoint = CourseHandler.getCheckpoint(course, position);

        if (myself.uses(ModuleType.MECHANICAL_ARM, phase)) {
            if (checkpoint == null && standingFloor.getFloortype() != Floortype.PIT_STOP) {
                checkpointFloor = CourseHandler.getFloor(course, new Position(position.x(), position.y() - 1));
                checkpoint = CourseHandler.getCheckpoint(course, checkpointFloor.getPosition());
            }
            if (checkpoint == null && checkpointFloor.getFloortype() != Floortype.PIT_STOP) {
                checkpointFloor = CourseHandler.getFloor(course, new Position(position.x() + 1, position.y()));
                checkpoint = CourseHandler.getCheckpoint(course, checkpointFloor.getPosition());
            }
            if (checkpoint == null && checkpointFloor.getFloortype() != Floortype.PIT_STOP) {
                checkpointFloor = CourseHandler.getFloor(course, new Position(position.x(), position.y() + 1));
                checkpoint = CourseHandler.getCheckpoint(course, checkpointFloor.getPosition());
            }
            if (checkpoint == null && checkpointFloor.getFloortype() != Floortype.PIT_STOP) {
                checkpointFloor = CourseHandler.getFloor(course, new Position(position.x() - 2, position.y()));
                checkpoint = CourseHandler.getCheckpoint(course, new Position(position.x() - 1, position.y()));
            }
        }

        if (checkpoint != null || checkpointFloor.getFloortype() == Floortype.PIT_STOP) {
            myself.setArchivePosition(checkpointFloor.getPosition());
            myself.setArchiveLevel(checkpointFloor.getLevel());
            copiesArchive = true;
            if (checkpoint != null && checkpoint.getNumber() == myself.getNextCheckpoint()) {
                if (nextCP != null && checkpoint.getNumber() == nextCP.getNumber() && reachesCheckpoint == 5) {
                    reachesCheckpoint = phase;
                }
                myself.setNextCheckpoint(myself.getNextCheckpoint() + 1);
            }
        }

        if (phase == 4 && standingFloor.getFloortype() == Floortype.PIT_STOP) {
            reachesPitStop = true;
            copiesArchive = true;
            if (myself.getDamage() > 0) {
                myself.setDamage(myself.getDamage() - 1);
            }
        }

        for (CourseObject object : new ArrayList<>(course.getObjects())) {
            if (object.getType() != ObjectType.MINE && object.getType() != ObjectType.PROXIMITY_MINE) {
                continue;
            }
            object.setActive(true);
            boolean triggered = CourseHandler.getRobots(course, object.getPosition()).stream().anyMatch(CourseElement::isOnCourse);
            if (!triggered && object.getType() == ObjectType.PROXIMITY_MINE) {
                Position mine = object.getPosition();
                triggered = List.of(
                        new Position(mine.x() + 1, mine.y()),
                        new Position(mine.x() - 1, mine.y()),
                        new Position(mine.x(), mine.y() + 1),
                        new Position(mine.x(), mine.y() - 1))
                        .stream()
                        .anyMatch(p -> CourseHandler.getRobots(course, p).stream().anyMatch(CourseElement::isOnCourse));
            }
            if (triggered) {
                gameLogic.goOff(de.phlup.circuitchaos.common.enums.Step.TOUCH_CHECKPOINTS, phase, object);
            }
        }
    }

    private void calculateProbableTargets() {
        for (Robot targetRobot : course.getRobots()) {
            if (!Objects.equals(targetRobot.getId(), myself.getId()) && targetRobot.isOnCourse()) {
                int maxDist = 4;
                int goalMultiplication = goal == Goal.KILL_ALL ? 1 : targetRobot.getNextCheckpoint();
                Position targetPos = targetRobot.getPosition();
                switch (myself.getDirection()) {
                    case NORTH -> {
                        if (targetPos.y() < y && Math.abs(targetPos.y() - y) <= maxDist
                                && Math.abs(targetPos.x() - x) <= Math.abs(targetPos.y() - y)) {
                            probableTargets += (maxDist - Math.abs(targetPos.x() - x)) * goalMultiplication;
                        }
                    }
                    case EAST -> {
                        if (targetPos.x() > x && Math.abs(targetPos.x() - x) <= maxDist
                                && Math.abs(targetPos.x() - x) >= Math.abs(targetPos.y() - y)) {
                            probableTargets += (maxDist - Math.abs(targetPos.y() - y)) * goalMultiplication;
                        }
                    }
                    case SOUTH -> {
                        if (targetPos.y() > y && Math.abs(targetPos.y() - y) <= maxDist
                                && Math.abs(targetPos.x() - x) <= Math.abs(targetPos.y() - y)) {
                            probableTargets += (maxDist - Math.abs(targetPos.x() - x)) * goalMultiplication;
                        }
                    }
                    case WEST -> {
                        if (targetPos.x() < x && Math.abs(targetPos.x() - x) <= maxDist
                                && Math.abs(targetPos.x() - x) >= Math.abs(targetPos.y() - y)) {
                            probableTargets += (maxDist - Math.abs(targetPos.y() - y)) * goalMultiplication;
                        }
                    }
                }
            }
        }
    }

    private void calculateDistance2() {
        int minX = course.getRange().minX();
        int minY = course.getRange().minY();
        int deltaX = x - nextCP.getPosition().x();
        int deltaY = y - nextCP.getPosition().y();
        if (deltaY == 0) {
            distance2 = deltaX;
        } else if (deltaX == 0) {
            distance2 = deltaY;
        } else {
            distance2 = deltaY / Math.sin(Math.atan(deltaY / (double) deltaX));
        }
        distance2 = Math.abs(distance2);
    }

}
