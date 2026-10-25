package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.Goal;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.ProgramType;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.Checkpoint;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Programme;
import de.phlup.circuitchaos.server.game.Game;
import de.phlup.circuitchaos.server.player.Player;
import lombok.Data;

@Data
public class MoveInfo {

    private final Game       game;
    private final Course     course;
    private final Player     player;
    private final Checkpoint nextCP;

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

    private Floor tf;

    public MoveInfo(Game game, Checkpoint nextCP, Player player) {
        this.game = game;
        course = game.getCourse();
        this.nextCP = nextCP;
        this.player = player;
        x = player.getRobot().getPosition().x();
        y = player.getRobot().getPosition().y();
    }

    public void simulateCourse() {
        distance = Integer.MAX_VALUE;
        endingDirection = startingDirection;
        willDie = false;
        reachesCheckpoint = 5;
        reachesPitStop = false;
        standsOnOpenFloor = false;
        keepsStaying = false;
        probableTargets = 0;

        tf = CourseHandler.getFloor(course, player.getRobot().getPosition());
        for (int phase = 0; phase < 5; phase++) {
            boolean flyingDuringMovement = false;
            checkForAbyss(phase);
            if (program[phase].getType() == ProgramType.ROTATE_LEFT) {
                endingDirection = endingDirection.add(Direction.WEST);
            } else if (program[phase].getType() == ProgramType.ROTATE_RIGHT) {
                endingDirection = endingDirection.add(Direction.EAST);
            } else if (program[phase].getType() == ProgramType.U_TURN) {
                endingDirection = endingDirection.add(Direction.SOUTH);
            } else {
                int movement = program[phase].getType().getMovement();
                if (movement != 0 && player.uses(ModuleType.BRAKES, phase)) {
                    movement = movement > 0 ? movement - 1 : movement + 1;
                }
                // Hovercraft flight starts before water/oil effects are applied.
                flyingDuringMovement = player.uses(ModuleType.HOVERCRAFT, phase) && movement != 0;
                if (!flyingDuringMovement && (tf.isWater() || CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(co -> co.getType() == ObjectType.OIL))) {
                    if (movement > 0) {
                        movement--;
                    }
                    if (movement < 0) {
                        movement++;
                    }
                }
                if (movement != 0 && !CourseHandler.getObjects(course, new Position(x, y)).isEmpty()) {
                    for (CourseObject co : CourseHandler.getObjects(course, new Position(x, y))) {
                        if (co.getType() == ObjectType.TELEPORTER && !flyingDuringMovement) {
                            if (movement < 0) {
                                movement = 1 - movement;
                            } else {
                                movement = 2 + movement;
                            }
                            switch (endingDirection) {
                                case NORTH -> y = y - movement;
                                case EAST -> x = x + movement;
                                case SOUTH -> y = y + movement;
                                case WEST -> x = x - movement;
                            }
                            movement = 0;
                            checkForAbyss(phase);
                            break;
                        }
                    }
                }
                int movementStartX = x;
                int movementStartY = y;
                Direction movementDirection = movement < 0 ? endingDirection.reverse() : endingDirection;
                if (movement < 0) {
                    while (movement < 0) {
                        Floor previousFloor = CourseHandler.getFloor(course, new Position(x, y));
                        if (notBlocked(course, true, flyingDuringMovement)) {
                            switch (endingDirection) {
                                case NORTH -> y++;
                                case EAST -> x--;
                                case SOUTH -> y--;
                                case WEST -> x++;
                            }
                        }
                        tf = CourseHandler.getFloor(course, new Position(x, y));
                        applyHeightDamage(previousFloor, tf, flyingDuringMovement, endingDirection.reverse());
                        checkForAbyss(phase);
                        for (CourseObject co : CourseHandler.getObjects(course, new Position(x, y))) {
                            if (co.getType().isPortal() && !flyingDuringMovement) {
                                x = co.getTargetPosition().x();
                                y = co.getTargetPosition().y();
                                tf = CourseHandler.getFloor(course, co.getTargetPosition());
                                break;
                            }
                        }
                        movement++;
                    }
                } else if (movement > 0) {
                    while (movement > 0) {
                        Floor previousFloor = CourseHandler.getFloor(course, new Position(x, y));
                        if (notBlocked(course, false, flyingDuringMovement)) {
                            switch (endingDirection) {
                                case NORTH -> y--;
                                case EAST -> x++;
                                case SOUTH -> y++;
                                case WEST -> x--;
                            }
                        }
                        tf = CourseHandler.getFloor(course, new Position(x, y));
                        applyHeightDamage(previousFloor, tf, flyingDuringMovement, endingDirection);
                        checkForAbyss(phase);
                        for (CourseObject co : CourseHandler.getObjects(course, new Position(x, y))) {
                            if (co.getType().isPortal() && !flyingDuringMovement) {
                                x = co.getTargetPosition().x();
                                y = co.getTargetPosition().y();
                                tf = CourseHandler.getFloor(course, co.getTargetPosition());
                                break;
                            }
                        }
                        movement--;
                    }
                }
                if (x != movementStartX || y != movementStartY) {
                    slideOnOil(movementDirection, flyingDuringMovement, phase);
                }
            }
            Direction lastDirection = Direction.NORTH;
            // express conveyor belts
            if (!flyingDuringMovement && tf.getFloortype().isExpressConveyorBelt()) {
                lastDirection = tf.getFacingDirection();
                if (moveForcedOneTile(lastDirection, flyingDuringMovement, phase)) {
                    if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW
                            || tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CCW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW)
                            && (tf.getFacingDirection() == lastDirection.add(Direction.WEST)
                            || (tf.getFacingDirection() == Direction.WEST && lastDirection == Direction.NORTH))) {
                        endingDirection = endingDirection.add(Direction.WEST);
                    } else if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW
                            || tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW)
                            && (tf.getFacingDirection() == lastDirection.add(Direction.EAST)
                            || (tf.getFacingDirection() == Direction.NORTH && lastDirection == Direction.WEST))) {
                        endingDirection = endingDirection.add(Direction.EAST);
                    }
                    slideOnOil(lastDirection, flyingDuringMovement, phase);
                }
            }

            // conveyor belts
            if (!flyingDuringMovement && tf.getFloortype().isConveyorBelt() && !tf.isWater()) {
                lastDirection = tf.getFacingDirection();
                if (moveForcedOneTile(lastDirection, flyingDuringMovement, phase)) {
                    if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW
                            || tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CCW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW)
                            && (tf.getFacingDirection() == lastDirection.add(Direction.WEST)
                            || (tf.getFacingDirection() == Direction.WEST && lastDirection == Direction.NORTH))) {
                        endingDirection = endingDirection.add(Direction.WEST);
                    } else if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW
                            || tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW
                            || tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW)
                            && (tf.getFacingDirection() == lastDirection.add(Direction.EAST)
                            || (tf.getFacingDirection() == Direction.NORTH && lastDirection == Direction.WEST))) {
                        endingDirection = endingDirection.add(Direction.EAST);
                    }
                    slideOnOil(lastDirection, flyingDuringMovement, phase);
                }
            }

            // pushers
            if (tf.isHasPusher() && tf.getActiveInPhase()[phase]) {
                lastDirection = tf.getPusherDirection();
                if (moveForcedOneTile(lastDirection, flyingDuringMovement, phase)) {
                    slideOnOil(lastDirection, flyingDuringMovement, phase);
                }
            }
            // gears
            if (tf.getFloortype() == Floortype.GEARS_CW) {
                endingDirection = endingDirection.add(Direction.EAST);
            } else if (tf.getFloortype() == Floortype.GEARS_CCW) {
                endingDirection = endingDirection.add(Direction.WEST);
            }
            // course mounted lasers (resolve laser fire)
            damage = damage + tf.getLasers()[0] + tf.getLasers()[1] + tf.getLasers()[2] + tf.getLasers()[3];
            // probableTargets (resolve laser fire)
            for (Player player2 : game.getPlayers()) {
                if (player2.getRobot().isOnCourse()) {
                    if (!player2.getRobot().equals(player.getRobot())) {
                        int maxDist = 4;
                        switch (endingDirection) {
                            case NORTH -> {
                                if (player2.getRobot().getPosition().y() < y && Math.abs(player2.getRobot().getPosition().y() - y) <= maxDist && Math.abs(
                                        player2.getRobot().getPosition().x() - x) <= Math
                                        .abs(player2.getRobot()
                                                    .getPosition()
                                                    .y() - y)) {
                                    probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().x() - x))
                                            * (player.getGoal() == Goal.KILL_ALL ? 1 : player2.getRobot().getNextCheckpoint());
                                }
                            }
                            case EAST -> {
                                if (player2.getRobot().getPosition().x() > x && Math.abs(player2.getRobot().getPosition().x() - x) <= maxDist && Math.abs(
                                        player2.getRobot().getPosition().x() - x) >= Math
                                        .abs(player2.getRobot()
                                                    .getPosition()
                                                    .y() - y)) {
                                    probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().y() - y))
                                            * (player.getGoal() == Goal.KILL_ALL ? 1 : player2.getRobot().getNextCheckpoint());
                                }
                            }
                            case SOUTH -> {
                                if (player2.getRobot().getPosition().y() > y && Math.abs(player2.getRobot().getPosition().y() - y) <= maxDist && Math.abs(
                                        player2.getRobot().getPosition().x() - x) <= Math
                                        .abs(player2.getRobot()
                                                    .getPosition()
                                                    .y() - y)) {
                                    probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().x() - x))
                                            * (player.getGoal() == Goal.KILL_ALL ? 1 : player2.getRobot().getNextCheckpoint());
                                }
                            }
                            case WEST -> {
                                if (player2.getRobot().getPosition().x() < x && Math.abs(player2.getRobot().getPosition().x() - x) <= maxDist && Math.abs(
                                        player2.getRobot().getPosition().x() - x) >= Math
                                        .abs(player2.getRobot()
                                                    .getPosition()
                                                    .y() - y)) {
                                    probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().y() - y))
                                            * (player.getGoal() == Goal.KILL_ALL ? 1 : player2.getRobot().getNextCheckpoint());
                                }
                            }
                        }
                    }
                }
            }
            // end of phase/turn course effects
            if (phase == 4 &&
                    !tf.getFloortype().isConveyorBelt() &&
                    tf.getFloortype() != Floortype.GEARS_CW &&
                    tf.getFloortype() != Floortype.GEARS_CCW &&
                    !tf.isHasPusher()) {
                standsOnOpenFloor = true;
            }
            if (!willDie) {
                if (tf.getFloortype() == Floortype.PIT_STOP) {
                    reachesPitStop = phase == 4;
                    copiesArchive = true;
                }
                if (nextCP != null) { // case Goal = KILL_ALL
                    if (reachesCheckpoint == 5 && nextCP.getPosition().x() == x && nextCP.getPosition().y() == y) {
                        reachesCheckpoint = phase;
                        if (phase == 4) {
                            reachesPitStop = true;
                        }
                        copiesArchive = true;
                    }
                    if (phase == 4) {
                        keepsStaying = x == player.getRobot().getPosition().x() && y == player.getRobot().getPosition().y();
                        switch (endingDirection) {
                            case NORTH -> facesTowardsCheckpoint = nextCP.getPosition().y() <= y;
                            case EAST -> facesTowardsCheckpoint = nextCP.getPosition().x() >= x;
                            case SOUTH -> facesTowardsCheckpoint = nextCP.getPosition().y() >= y;
                            case WEST -> facesTowardsCheckpoint = nextCP.getPosition().x() <= x;
                        }
                        int minx = course.getRange().minX();
                        int miny = course.getRange().minY();
                        if ((y - miny) - (nextCP.getPosition().y() - miny) == 0) {
                            distance2 = (x - minx) - (nextCP.getPosition().x() - minx);
                        } else if ((x - minx) - (nextCP.getPosition().x() - minx) == 0) {
                            distance2 = (y - miny) - (nextCP.getPosition().y() - miny);
                        } else {
                            distance2 = Math.abs(((y - miny) - (nextCP.getPosition().y() - miny)) / Math
                                    .sin(Math.atan(((y - miny) - (nextCP.getPosition().y() - miny)) / (double) ((x - minx) - (nextCP.getPosition().x() - minx)))));
                        }
                        if (distance2 < 0) {
                            distance2 = -distance2;
                        }
                    }
                }
            }
        }
    }

    private boolean moveForcedOneTile(Direction direction, boolean flying, int phase) {
        Floor previousFloor = CourseHandler.getFloor(course, new Position(x, y));
        Position targetPosition = previousFloor.getPosition().neighbour(direction);
        Floor targetFloor = CourseHandler.getFloor(course, targetPosition);
        if (!canMoveAcross(previousFloor, targetFloor, direction, flying)) {
            return false;
        }
        x = targetPosition.x();
        y = targetPosition.y();
        tf = targetFloor;
        applyHeightDamage(previousFloor, targetFloor, flying, direction);
        checkForAbyss(phase);
        if (!flying) {
            for (CourseObject co : CourseHandler.getObjects(course, targetPosition)) {
                if (co.getType().isPortal() && co.getTargetPosition() != null) {
                    x = co.getTargetPosition().x();
                    y = co.getTargetPosition().y();
                    tf = CourseHandler.getFloor(course, co.getTargetPosition());
                    checkForAbyss(phase);
                    break;
                }
            }
        }
        return true;
    }

    private void slideOnOil(Direction direction, boolean flying, int phase) {
        while (!flying && CourseHandler.getObjects(course, tf.getPosition()).stream()
                .anyMatch(co -> co.getType() == ObjectType.OIL)) {
            if (!moveForcedOneTile(direction, false, phase)) {
                break;
            }
        }
    }

    private boolean canMoveAcross(Floor sourceFloor, Floor targetFloor, Direction direction, boolean flying) {
        if (flying) {
            return true;
        }
        WallType sourceWall = sourceFloor.wall(direction);
        WallType targetWall = targetFloor.wall(direction.reverse());
        boolean rampPassage = sourceWall == WallType.RAMP_UP || sourceWall == WallType.RAMP_DOWN
                || targetWall == WallType.RAMP_UP || targetWall == WallType.RAMP_DOWN;
        if (targetFloor.getLevel() > sourceFloor.getLevel() && !rampPassage) {
            return false;
        }
        return ((sourceWall == WallType.NONE
                || sourceWall == WallType.ONE_WAY_GREEN
                || sourceWall == WallType.RAMP_UP
                || sourceWall == WallType.RAMP_DOWN)
                && (targetWall == WallType.NONE
                || targetWall == WallType.ONE_WAY_GREEN
                || targetWall == WallType.RAMP_UP
                || targetWall == WallType.RAMP_DOWN
                || targetWall == WallType.LEDGE))
                || (sourceWall == WallType.ONE_WAY_GREEN && targetWall == WallType.ONE_WAY_RED);
    }

    private void applyHeightDamage(Floor previousFloor, Floor currentFloor, boolean flying, Direction direction) {
        if (flying || currentFloor.getLevel() >= previousFloor.getLevel()) {
            return;
        }
        WallType sourceWall = previousFloor.wall(direction);
        WallType targetWall = currentFloor.wall(direction.reverse());
        boolean rampPassage = sourceWall == WallType.RAMP_UP || sourceWall == WallType.RAMP_DOWN
                || targetWall == WallType.RAMP_UP || targetWall == WallType.RAMP_DOWN;
        if (!rampPassage) {
            damage++;
        }
    }

    private void checkForAbyss(int phase) {
        if (tf.getFloortype() == Floortype.ABYSS || (tf.getFloortype() == Floortype.TRAPDOOR && tf.getActiveInPhase()[phase])) {
            willDie = true;
        }
    }

    private boolean notBlocked(Course course, boolean backup, boolean flying) {
        Floor f = CourseHandler.getFloor(course, new Position(x, y));
        Floor targetFloor = switch (endingDirection) {
            case NORTH -> CourseHandler.getFloor(course, new Position(x, backup ? y + 1 : y - 1));
            case EAST -> CourseHandler.getFloor(course, new Position(backup ? x - 1 : x + 1, y));
            case SOUTH -> CourseHandler.getFloor(course, new Position(x, backup ? y - 1 : y + 1));
            case WEST -> CourseHandler.getFloor(course, new Position(backup ? x + 1 : x - 1, y));
            case null -> {
                Floor t = new Floor();
                t.setFloortype(Floortype.ABYSS);
                t.setPosition(new Position(x, y));
                yield t;
            }
        };
        Direction direction = backup ? endingDirection.reverse() : endingDirection;
        return canMoveAcross(f, targetFloor, direction, flying);
    }

}
