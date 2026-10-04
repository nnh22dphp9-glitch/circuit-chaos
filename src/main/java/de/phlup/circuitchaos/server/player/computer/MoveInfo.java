package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
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
import de.phlup.circuitchaos.course.CourseHandler;
import de.phlup.circuitchaos.server.game.Game;
import de.phlup.circuitchaos.server.player.Player;
import lombok.Data;

@Data
public class MoveInfo {

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

    public MoveInfo(Game game, Programme[] programme, Direction sd, Checkpoint nextCP, Player player) {
        this.program = new Programme[5];
        Course course = game.getCourse();
        System.arraycopy(programme, 0, program, 0, 5);
        startingDirection = sd;
        distance = Integer.MAX_VALUE;

        endingDirection = sd;
        x = player.getRobot().getPosition().x();
        y = player.getRobot().getPosition().y();
        willDie = false;
        reachesCheckpoint = 5;
        reachesPitStop = false;
        standsOnOpenFloor = false;
        keepsStaying = false;
        probableTargets = 0;
        tf = CourseHandler.getFloor(course, player.getRobot().getPosition());
        for (int phase = 0; phase < 5; phase++) {
            checkForAbyss(phase);
            boolean flying = player.getRobot().isFlying();
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
                if ((tf.isWater() && !flying) || CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                    if (movement > 0) {
                        movement--;
                    }
                    if (movement < 0) {
                        movement++;
                    }
                }
                if (movement != 0 && !CourseHandler.getObjects(course, new Position(x, y)).isEmpty()) {
                    for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                        if (cco.getType() == ObjectType.TELEPORTER && !flying) {
                            if (movement < 0) {
                                movement = 1 - movement;
                            } else {
                                movement = 2 + movement;
                            }
                            switch (endingDirection) {
                                case NORTH:
                                    y = y - movement;
                                    break;
                                case EAST:
                                    x = x + movement;
                                    break;
                                case SOUTH:
                                    y = y + movement;
                                    break;
                                case WEST:
                                    x = x - movement;
                                    break;
                            }
                            movement = 0;
                            checkForAbyss(phase);
                            break;
                        }
                    }
                }
                if (movement < 0) {
                    while (movement < 0) {
                        if (notBlocked(course, true)) {
                            switch (endingDirection) {
                                case NORTH:
                                    y++;
                                    break;
                                case EAST:
                                    x--;
                                    break;
                                case SOUTH:
                                    y--;
                                    break;
                                case WEST:
                                    x++;
                                    break;
                            }
                        }
                        tf = CourseHandler.getFloor(course, new Position(x, y));
                        checkForAbyss(phase);
                        for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                            if (cco.getType().isPortal() && !flying) {
                                x = cco.getTargetPosition().x();
                                y = cco.getTargetPosition().y();
                                tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                                break;
                            }
                        }
                        movement++;
                    }
                    while (CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                        switch (endingDirection) {
                            case NORTH:
                                y++;
                                break;
                            case EAST:
                                x--;
                                break;
                            case SOUTH:
                                y--;
                                break;
                            case WEST:
                                x++;
                                break;
                        }
                        tf = CourseHandler.getFloor(course, new Position(x, y));
                        checkForAbyss(phase);
                        for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                            if (cco.getType().isPortal() && !flying) {
                                x = cco.getTargetPosition().x();
                                y = cco.getTargetPosition().y();
                                tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                                break;
                            }
                        }
                    }
                } else if (movement > 0) {
                    while (movement > 0) {
                        if (notBlocked(course, false)) {
                            switch (endingDirection) {
                                case NORTH:
                                    y--;
                                    break;
                                case EAST:
                                    x++;
                                    break;
                                case SOUTH:
                                    y++;
                                    break;
                                case WEST:
                                    x--;
                                    break;
                            }
                        }
                        tf = CourseHandler.getFloor(course, new Position(x, y));
                        checkForAbyss(phase);
                        for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                            if (cco.getType().isPortal() && !flying) {
                                x = cco.getTargetPosition().x();
                                y = cco.getTargetPosition().y();
                                tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                                break;
                            }
                        }
                        movement--;
                    }
                    while (CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                        switch (endingDirection) {
                            case NORTH:
                                y--;
                                break;
                            case EAST:
                                x++;
                                break;
                            case SOUTH:
                                y++;
                                break;
                            case WEST:
                                x--;
                                break;
                        }
                        tf = CourseHandler.getFloor(course, new Position(x, y));
                        checkForAbyss(phase);
                        for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                            if (cco.getType().isPortal() && !flying) {
                                x = cco.getTargetPosition().x();
                                y = cco.getTargetPosition().y();
                                tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                                break;
                            }
                        }
                    }
                }
            }
            Direction lastDirection = Direction.NORTH;
            // express conveyor belts
            if (tf.getFloortype().isExpressConveyorBelt()) {
                lastDirection = tf.getFacingDirection();
                switch (tf.getFacingDirection()) {
                    case NORTH:
                        y--;
                        break;
                    case EAST:
                        x++;
                        break;
                    case SOUTH:
                        y++;
                        break;
                    case WEST:
                        x--;
                        break;
                }
                tf = CourseHandler.getFloor(course, new Position(x, y));
                if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW ||
                        tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CCW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW) &&
                        (tf.getFacingDirection() == lastDirection.add(Direction.WEST) || (tf.getFacingDirection() == Direction.WEST && lastDirection == Direction.NORTH))) {
                    endingDirection = endingDirection.add(Direction.WEST);
                } else if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW ||
                        tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW) &&
                        (tf.getFacingDirection() == lastDirection.add(Direction.EAST) || (tf.getFacingDirection() == Direction.NORTH && lastDirection == Direction.WEST))) {
                    endingDirection = endingDirection.add(Direction.EAST);
                }
                for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                    if (cco.getType().isPortal() && !flying) {
                        x = cco.getTargetPosition().x();
                        y = cco.getTargetPosition().y();
                        tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                        break;
                    }
                }
            }
            checkForAbyss(phase);
            while (CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                switch (lastDirection) {
                    case NORTH:
                        y--;
                        break;
                    case EAST:
                        x++;
                        break;
                    case SOUTH:
                        y++;
                        break;
                    case WEST:
                        x--;
                        break;
                }
                tf = CourseHandler.getFloor(course, new Position(x, y));
                checkForAbyss(phase);
                for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                    if (cco.getType().isPortal() && !flying) {
                        x = cco.getTargetPosition().x();
                        y = cco.getTargetPosition().y();
                        tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                        break;
                    }
                }
            }
            // conveyor belts
            if (tf.getFloortype().isConveyorBelt() && !tf.isWater()) {
                lastDirection = tf.getFacingDirection();
                switch (tf.getFacingDirection()) {
                    case NORTH:
                        y--;
                        break;
                    case EAST:
                        x++;
                        break;
                    case SOUTH:
                        y++;
                        break;
                    case WEST:
                        x--;
                        break;
                }
                tf = CourseHandler.getFloor(course, new Position(x, y));
                if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW ||
                        tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CCW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW) &&
                        (tf.getFacingDirection() == lastDirection.add(Direction.WEST) || (tf.getFacingDirection() == Direction.WEST && lastDirection == Direction.NORTH))) {
                    endingDirection = endingDirection.add(Direction.WEST);
                } else if ((tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW ||
                        tf.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW ||
                        tf.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW) &&
                        (tf.getFacingDirection() == lastDirection.add(Direction.EAST) || (tf.getFacingDirection() == Direction.NORTH && lastDirection == Direction.WEST))) {
                    endingDirection = endingDirection.add(Direction.EAST);
                }
                for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                    if (cco.getType().isPortal() && !flying) {
                        x = cco.getTargetPosition().x();
                        y = cco.getTargetPosition().y();
                        tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                        break;
                    }
                }
            }
            checkForAbyss(phase);
            while (CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                switch (lastDirection) {
                    case NORTH:
                        y--;
                        break;
                    case EAST:
                        x++;
                        break;
                    case SOUTH:
                        y++;
                        break;
                    case WEST:
                        x--;
                        break;
                }
                tf = CourseHandler.getFloor(course, new Position(x, y));
                checkForAbyss(phase);
                for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                    if (cco.getType().isPortal() && !flying) {
                        x = cco.getTargetPosition().x();
                        y = cco.getTargetPosition().y();
                        tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                        break;
                    }
                }
            }
            checkForAbyss(phase);
            while (CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                switch (lastDirection) {
                    case NORTH:
                        y--;
                        break;
                    case EAST:
                        x++;
                        break;
                    case SOUTH:
                        y++;
                        break;
                    case WEST:
                        x--;
                        break;
                }
                tf = CourseHandler.getFloor(course, new Position(x, y));
                checkForAbyss(phase);
                for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                    if (cco.getType().isPortal() && !flying) {
                        x = cco.getTargetPosition().x();
                        y = cco.getTargetPosition().y();
                        tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                        break;
                    }
                }
            }
            // pushers
            if (tf.isHasPusher() && tf.getActiveInPhase()[phase]) {
                lastDirection = tf.getPusherDirection();
                switch (lastDirection) {
                    case NORTH:
                        y--;
                        break;
                    case EAST:
                        x++;
                        break;
                    case SOUTH:
                        y++;
                        break;
                    case WEST:
                        x--;
                        break;
                }
                tf = CourseHandler.getFloor(course, new Position(x, y));
                checkForAbyss(phase);
                for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                    if (cco.getType().isPortal() && !flying) {
                        x = cco.getTargetPosition().x();
                        y = cco.getTargetPosition().y();
                        tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                        break;
                    }
                }
                //noinspection WhileCanBeDoWhile
                while (CourseHandler.getObjects(course, tf.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                    switch (lastDirection) {
                        case NORTH:
                            y--;
                            break;
                        case EAST:
                            x++;
                            break;
                        case SOUTH:
                            y++;
                            break;
                        case WEST:
                            x--;
                            break;
                    }
                    tf = CourseHandler.getFloor(course, new Position(x, y));
                    checkForAbyss(phase);
                    for (CourseObject cco : CourseHandler.getObjects(course, new Position(x, y))) {
                        if (cco.getType().isPortal() && !flying) {
                            x = cco.getTargetPosition().x();
                            y = cco.getTargetPosition().y();
                            tf = CourseHandler.getFloor(course, cco.getTargetPosition());
                            break;
                        }
                    }
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
            if (!player.getRobot().isVirtual()) {
                for (Player player2 : game.getPlayers()) {
                    if (player2.getRobot().isOnCourse()) {
                        if (!player2.getRobot().equals(player.getRobot())) {
                            if (!player2.getRobot().isVirtual()) {
                                boolean willCheck = true;
                                if (player2 instanceof ComputerPlayer) {
                                    willCheck = !((ComputerPlayer) player2).isAggressive();
                                }
                                if (willCheck) {
                                    int maxDist = 4;
                                    switch (endingDirection) {
                                        case NORTH:
                                            if (player2.getRobot().getPosition().y() < y && Math.abs(player2.getRobot().getPosition().y() - y) <= maxDist && Math.abs(
                                                    player2.getRobot().getPosition().x() - x) <= Math
                                                    .abs(player2.getRobot()
                                                                .getPosition()
                                                                .y() - y)) {
                                                probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().x() - x)) * player2.getRobot().getNextCheckpoint();
                                            }
                                            break;
                                        case EAST:
                                            if (player2.getRobot().getPosition().x() > x && Math.abs(player2.getRobot().getPosition().x() - x) <= maxDist && Math.abs(
                                                    player2.getRobot().getPosition().x() - x) >= Math
                                                    .abs(player2.getRobot()
                                                                .getPosition()
                                                                .y() - y)) {
                                                probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().y() - y)) * player2.getRobot().getNextCheckpoint();
                                            }
                                            break;
                                        case SOUTH:
                                            if (player2.getRobot().getPosition().y() > y && Math.abs(player2.getRobot().getPosition().y() - y) <= maxDist && Math.abs(
                                                    player2.getRobot().getPosition().x() - x) <= Math
                                                    .abs(player2.getRobot()
                                                                .getPosition()
                                                                .y() - y)) {
                                                probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().x() - x)) * player2.getRobot().getNextCheckpoint();
                                            }
                                            break;
                                        case WEST:
                                            if (player2.getRobot().getPosition().x() < x && Math.abs(player2.getRobot().getPosition().x() - x) <= maxDist && Math.abs(
                                                    player2.getRobot().getPosition().x() - x) >= Math
                                                    .abs(player2.getRobot()
                                                                .getPosition()
                                                                .y() - y)) {
                                                probableTargets = probableTargets + (maxDist - Math.abs(player2.getRobot().getPosition().y() - y)) * player2.getRobot().getNextCheckpoint();
                                            }
                                            break;
                                    }
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
                        case NORTH:
                            facesTowardsCheckpoint = nextCP.getPosition().y() <= y;
                            break;
                        case EAST:
                            facesTowardsCheckpoint = nextCP.getPosition().x() >= x;
                            break;
                        case SOUTH:
                            facesTowardsCheckpoint = nextCP.getPosition().y() >= y;
                            break;
                        case WEST:
                            facesTowardsCheckpoint = nextCP.getPosition().x() <= x;
                            break;
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

    private void checkForAbyss(int phase) {
        if (tf.getFloortype() == Floortype.ABYSS || (tf.getFloortype() == Floortype.TRAPDOOR && tf.getActiveInPhase()[phase])) {
            willDie = true;
        }
    }

    private boolean notBlocked(Course course, boolean backup) {
        Floor f = CourseHandler.getFloor(course, new Position(x, y));
        Floor tf = switch (endingDirection) {
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
        Direction dir1 = backup ? endingDirection.reverse() : endingDirection;
        Direction dir2 = endingDirection.reverse();
        return (f.wall(dir1) == WallType.NONE
                || f.wall(dir1) == WallType.ONE_WAY_GREEN
                || f.wall(dir1) == WallType.RAMP_UP
                || f.wall(dir1) == WallType.RAMP_DOWN)
                && (tf.wall(dir2) == WallType.NONE
                || tf.wall(dir2) == WallType.ONE_WAY_GREEN
                || tf.wall(dir2) == WallType.ONE_WAY_RED
                || tf.wall(dir2) == WallType.LEDGE
                || tf.wall(dir2) == WallType.RAMP_UP
                || tf.wall(dir2) == WallType.RAMP_DOWN);
    }

}
