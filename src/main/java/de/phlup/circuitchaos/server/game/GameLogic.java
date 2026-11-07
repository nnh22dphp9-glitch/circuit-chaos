package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.ProgramType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.course.Checkpoint;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.CourseElement;
import de.phlup.circuitchaos.common.model.course.CourseObject;
import de.phlup.circuitchaos.common.model.course.CourseProperties;
import de.phlup.circuitchaos.common.model.course.Floor;
import de.phlup.circuitchaos.common.model.course.Module;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Programme;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static de.phlup.circuitchaos.common.enums.Direction.EAST;
import static de.phlup.circuitchaos.common.enums.Direction.NORTH;
import static de.phlup.circuitchaos.common.enums.Direction.SOUTH;
import static de.phlup.circuitchaos.common.enums.Direction.WEST;
import static de.phlup.circuitchaos.common.enums.ModuleType.MAIN_LASER;

@RequiredArgsConstructor
public class GameLogic {

    private static final int MINE_INITIAL_DAMAGE = 4;

    @Getter
    private final Course               course;
    private final CourseChangeListener listener;

    @Setter
    private boolean gameAborted = false;

    public void notifyCourseMayHaveChanged(Step step, Integer phase, String detail, String movingRobotName) {
        listener.courseChanged(step, phase, detail, movingRobotName);
    }

    public void singleRobotMove(int phase, Robot robot, Programme pc, int movement) {
        if (movement > 0) {
            moveInit(robot, robot.getDirection(), movement, movement, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true, true, robot.getName());
        } else if (movement < 0) {
            moveInit(robot, SOUTH.add(robot.getDirection()), movement, -movement, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true, true, robot.getName());
        }
        if (pc.type() == ProgramType.ROTATE_RIGHT) {
            rotate(robot, EAST, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
        } else if (pc.type() == ProgramType.ROTATE_LEFT) {
            rotate(robot, WEST, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
        } else if (pc.type() == ProgramType.U_TURN) {
            rotate(robot, SOUTH, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
        }
    }

    public void moveInit(CourseElement ce, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot, boolean wait, String movingRobotName) {
        move(ce, direction, strength, amount, step, phase, notBlockedByMovableRobot, wait, movingRobotName);
    }

    private boolean move(CourseElement ce, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot, boolean wait, String movingRobotName) {
        Position oldPosition    = ce.getPosition();
        Floor    floor1         = CourseHandler.getFloor(course, oldPosition);
        Position targetPosition = ce.getPosition().neighbour(direction);
        Floor    floor2         = CourseHandler.getFloor(course, targetPosition);
        if (!ce.isFlying()) {
            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, floor1.getPosition()))) {
                if (co.getType() == ObjectType.GLUE && co.isActive()) {
                    while (co.getGlue() > 0 && amount > 0) {
                        deGlue(co, phase);
                        amount--;
                    }
                }
            }
        }
        Direction direction2 = direction.reverse();
        if (!ce.getPosition().inRange(course.getRange())) {
            amount = 0;
            die(ce, step, phase);
        }
        wait = wait && (movingRobotName == null || movingRobotName.equals(ce.getName()));
        if (amount > 0) {
            WallType sourceWall = floor1.wall(direction);
            WallType targetWall = floor2.wall(direction2);
            boolean rampPassage = sourceWall == WallType.RAMP_UP || sourceWall == WallType.RAMP_DOWN
                    || targetWall == WallType.RAMP_UP || targetWall == WallType.RAMP_DOWN;
            boolean uphill   = floor2.getLevel() > floor1.getLevel() && !rampPassage;
            boolean downhill = floor2.getLevel() < floor1.getLevel() && !rampPassage;
            boolean wallPassage = ((sourceWall == WallType.NONE
                    || sourceWall == WallType.ONE_WAY_GREEN
                    || sourceWall == WallType.RAMP_DOWN
                    || sourceWall == WallType.RAMP_UP)
                    && (targetWall == WallType.NONE
                    || targetWall == WallType.ONE_WAY_GREEN
                    || targetWall == WallType.RAMP_UP
                    || targetWall == WallType.RAMP_DOWN
                    || targetWall == WallType.LEDGE))
                    || (sourceWall == WallType.ONE_WAY_GREEN && targetWall == WallType.ONE_WAY_RED);
            if (ce.isFlying() || (!uphill && wallPassage)) {
                CourseElement conflictingElement = getConflictingElement(ce, direction);
                if (conflictingElement == null) {
                    ce.setPosition(targetPosition);
                    if (ce instanceof Robot r && !r.isFlying()) {
                        ce.setLevel(floor2.getLevel());
                        if (downhill) {
                            r.takeDamage(course, phase, null, false);
                        }
                    }
                } else if (notBlockedByMovableRobot) {
                    if (ce instanceof Robot r && r.uses(ModuleType.RAMMING_ARMOR, phase)) {
                        r.getModule(ModuleType.RAMMING_ARMOR).getActiveInPhase()[phase] = false;
                        if (conflictingElement instanceof Robot rob) {
                            rob.takeDamage(course, phase, null, false);
                        }
                    }
                    if (move(conflictingElement, direction, strength, 1, step, phase, true, wait, movingRobotName)) {
                        ce.setPosition(targetPosition);
                        if (!ce.isFlying()) {
                            ce.setLevel(floor2.getLevel());
                            if (downhill && ce instanceof Robot r) {
                                r.takeDamage(course, phase, null, false);
                            }
                        }
                    }
                }
            } else if (sourceWall == WallType.REPULSOR_FIELD
                    || ((floor1.wall(direction) == WallType.NONE
                    || floor1.wall(direction) == WallType.ONE_WAY_GREEN
                    || floor1.wall(direction) == WallType.RAMP_DOWN)
                    && floor2.wall(direction2) == WallType.REPULSOR_FIELD)) {
                move(ce, direction2, strength, strength, step, phase, true, wait, movingRobotName);
            }
        }
        if (amount > 0 && wait) {
            notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
        }
        Floor floor3 = CourseHandler.getFloor(course, ce.getPosition());
        if (ce instanceof Robot r && !r.isFlying()) {
            minesGoOff(step, phase, floor3);
            CourseObject portal = CourseHandler.getPortal(course, floor3.getPosition());
            if (portal != null) {
                if (portal.getTargetPosition() != null) {
                    if (CourseHandler.getRobots(course, portal.getTargetPosition()).isEmpty()) {
                        floor3 = CourseHandler.getFloor(course, portal.getTargetPosition());
                        ce.setPosition(portal.getTargetPosition());
                        ce.setLevel(floor3.getLevel());
                        if (wait) {
                            notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
                        }
                        minesGoOff(step, phase, floor3);
                    }
                }
            }
        }
        if (!ce.isFlying()) {
            if (floor3.getFloortype() == Floortype.ABYSS || (floor3.getFloortype() == Floortype.TRAPDOOR && floor3.getActiveInPhase()[phase])) {
                amount = 0;
                die(ce, step, phase);
                if (ce instanceof Robot r) {
                    course.getRobotsFallingIntoAbyss().add(r);
                } else if (ce instanceof CourseObject co) {
                    course.getObjectsFallingIntoAbyss().add(co);
                }
            }
        }
        if (amount > 1) {
            move(ce, direction, strength, amount - 1, step, phase, true, wait, movingRobotName);
        }
        if (!oldPosition.equals(ce.getPosition())) {
            boolean notBlocked = true;
            if (!ce.isFlying()) {
                while (notBlocked && CourseHandler.getObjects(course, ce.getPosition()).stream().anyMatch(co -> co.getType() == ObjectType.OIL)) {
                    notBlocked = move(ce, direction, 0, 1, step, phase, false, wait, movingRobotName);
                }
            }
        }
        if (wait) {
            notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
        }
        return !oldPosition.equals(ce.getPosition());
    }

    private void minesGoOff(Step step, int phase, Floor floor) {
        minesGoesOff(floor, null, step, phase);
        minesGoesOff(floor, NORTH, step, phase);
        minesGoesOff(floor, EAST, step, phase);
        minesGoesOff(floor, SOUTH, step, phase);
        minesGoesOff(floor, WEST, step, phase);
    }

    private void minesGoesOff(Floor floor, Direction dir, Step step, int phase) {
        Position minePosition = dir == null ? floor.getPosition() : floor.getPosition().neighbour(dir);
        if (dir == null || !CourseHandler.cannotMoveAcross(CourseHandler.getFloor(course, minePosition), floor, dir.reverse(), false)) {
            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, minePosition))) {
                if (co.isActive() && (co.getType() == ObjectType.PROXIMITY_MINE
                        || (dir == null && co.getType() == ObjectType.MINE))) {
                    goOff(step, phase, co);
                }
            }
        }
    }

    public void touchRobotCheckpoints(int phase) {
        for (Robot robot : course.getRobots()) {
            if (gameAborted) {
                break;
            }
            if (!robot.isOnCourse()) {
                continue;
            }
            Checkpoint checkpoint = CourseHandler.getCheckpoint(course, robot.getPosition());
            Floor      floor      = CourseHandler.getFloor(course, robot.getPosition());
            if (robot.uses(ModuleType.MECHANICAL_ARM, phase)) {
                if (!(checkpoint != null || floor.getFloortype() == Floortype.PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x(), robot.getPosition().y() - 1));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x(), robot.getPosition().y() - 1));
                }
                if (!(checkpoint != null || floor.getFloortype() == Floortype.PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x() + 1, robot.getPosition().y()));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x() + 1, robot.getPosition().y()));
                }
                if (!(checkpoint != null || floor.getFloortype() == Floortype.PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x(), robot.getPosition().y() + 1));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x(), robot.getPosition().y() + 1));
                }
                if (!(checkpoint != null || floor.getFloortype() == Floortype.PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x() - 2, robot.getPosition().y()));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x() - 1, robot.getPosition().y()));
                }
            }
            if (checkpoint != null || floor.getFloortype() == Floortype.PIT_STOP) {
                robot.setArchivePosition(floor.getPosition());
                robot.setArchiveLevel(floor.getLevel());
                if (checkpoint != null && checkpoint.getNumber() == robot.getNextCheckpoint()) {
                    robot.setNextCheckpoint(robot.getNextCheckpoint() + 1);
                }
            }
        }
    }

    public void activateMines(int phase) {
        for (CourseObject object : new ArrayList<>(course.getObjects())) {
            if (gameAborted) {
                break;
            }
            object.setActive(true);
            switch (object.getType()) {
                case MINE -> {
                    if (!CourseHandler.getRobots(course, object.getPosition()).isEmpty()) {
                        goOff(Step.TOUCH_CHECKPOINTS, phase, object);
                    }
                }
                case PROXIMITY_MINE -> {
                    Position position = object.getPosition();
                    if (!(CourseHandler.getRobots(course, position).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x() + 1, position.y())).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x(), position.y() + 1)).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x() - 1, position.y())).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x(), position.y() - 1)).isEmpty())) {
                        goOff(Step.TOUCH_CHECKPOINTS, phase, object);
                    }
                }
                default -> {
                }
            }
        }
    }

    public void land(Robot robot, Step step, int phase) {
        Floor f = CourseHandler.getFloor(course, robot.getPosition());
        while (isRobotConflict(robot)) {
            moveInit(robot, robot.getDirection(), 0, 1, step, phase, true, true, null);
        }
        robot.setFlying(false);
        robot.setLevel(f.getLevel());
        if (f.getFloortype() == Floortype.ABYSS || (f.getFloortype() == Floortype.TRAPDOOR && f.getActiveInPhase()[phase])) {
            die(robot, step, phase);
            course.getRobotsFallingIntoAbyss().add(robot);
        }
    }

    private boolean isRobotConflict(Robot r) {
        for (Robot r2 : CourseHandler.getRobots(course, r.getPosition())) {
            if (!r2.equals(r)) {
                return true;
            }
        }
        return false;
    }

    private void deGlue(CourseObject courseObject, int phase) {
        if (courseObject.getGlue() > 0) {
            courseObject.setGlue(courseObject.getGlue() - 1);
            if (courseObject.getGlue() == 0) {
                die(courseObject, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
            }
        }
    }

    public void goOff(Step step, Integer phase, CourseObject courseObject) {
        if (courseObject.getType() == ObjectType.MINE || courseObject.getType() == ObjectType.PROXIMITY_MINE) {
            CourseHandler.remove(course, courseObject);
            explode(step, phase, courseObject.getPosition(), MINE_INITIAL_DAMAGE);
        }
    }

    public void explode(Step step, Integer phase, Position position, int initialDamage) {
        if (initialDamage > 0) {
            DistanceCalculator.calculateDistance(course, position, 7);
            for (Floor f : new ArrayList<>(course.getFloor())) {
                if (f.getDistanceCounter() < 7) {
                    int damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                    if (damage > 0) {
                        Floor floor = CourseHandler.getFloor(course, f.getPosition());
                        floor.setExplosiveDamage(damage);
                    }
                }
            }
            for (Robot robot : new ArrayList<>(course.getRobots())) {
                Floor f      = CourseHandler.getFloor(course, robot.getPosition());
                int   damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                for (int damagePoints = 0; damagePoints < damage; damagePoints++) {
                    robot.takeDamage(course);
                }
            }
            notifyCourseMayHaveChanged(step, phase, "explosion", null);
        }
    }

    private static int calculateEffectiveDamage(int initialDamage, int distanceCounter) {
        return switch (distanceCounter) {
            case 0 -> initialDamage;
            case 1 -> initialDamage / 2;
            case 2 -> initialDamage / 4;
            default -> 0;
        };
    }

    public void die(CourseElement ce, Step reason, Integer phase) {
        if (ce instanceof Robot r) {
            r.setDamage(10);
            r.takeDamage(course);
        } else {
            CourseHandler.remove(course, ce);
            notifyCourseMayHaveChanged(reason, phase, "%s died".formatted(ce.getName()), null);
        }
    }

    public void setPosition(CourseElement ce, int x, int y, Step step, int phase, Direction direction, boolean wait) {
        Floor previousFloor = CourseHandler.getFloor(course, ce.getPosition());
        ce.setPrevPosition(ce.getPosition());
        ce.setPosition(new Position(x, y));
        Floor floor3 = CourseHandler.getFloor(course, ce.getPosition());
        if (!ce.isFlying()) {
            ce.setLevel(floor3.getLevel());
            boolean rampPassage = false;
            if (direction != null) {
                WallType sourceWall = previousFloor.wall(direction);
                WallType targetWall = floor3.wall(direction.reverse());
                rampPassage = sourceWall == WallType.RAMP_UP || sourceWall == WallType.RAMP_DOWN
                        || targetWall == WallType.RAMP_UP || targetWall == WallType.RAMP_DOWN;
            }
            if (ce instanceof Robot r && floor3.getLevel() < previousFloor.getLevel() && !rampPassage) {
                r.takeDamage(course, phase, null, false);
            }
        }
        if (floor3.getFloortype() == Floortype.ABYSS || (floor3.getFloortype() == Floortype.TRAPDOOR && floor3.getActiveInPhase()[phase])) {
            die(ce, step, phase);
            if (ce instanceof Robot r) {
                course.getRobotsFallingIntoAbyss().add(r);
            } else if (ce instanceof CourseObject co) {
                course.getObjectsFallingIntoAbyss().add(co);
            }
        }
        if (direction != null) {
            if (CourseHandler.getObjects(course, floor3.getPosition()).stream().anyMatch(co -> co.getType() == ObjectType.OIL)) {
                moveInit(ce, direction, 0, 1, step, phase, false, wait, null);
            }
        }
        moveInit(ce, NORTH, 0, 0, step, phase, false, wait, null);
    }

    private CourseElement getConflictingElement(CourseElement courseElement, Direction direction) {
        if (!courseElement.isOnCourse() || courseElement.isFlying()) {
            return null;
        }
        Position targetPosition = courseElement.getPosition().neighbour(direction);
        for (Robot ce : CourseHandler.getRobots(course, targetPosition)) {
            if (!ce.equals(courseElement) && !ce.isFlying()) {
                return ce;
            }
        }
        return null;
    }

    private int getCBConflictingElementMark(CourseElement be2, Direction direction) {
        int           erg = 1;
        CourseElement ce  = getConflictingElement(be2, direction);
        if (ce != null) {
            erg = ce.getConflictingMark();
        }
        return erg;
    }


    public void rotate(CourseElement ce, Direction direction, Step step, int phase, boolean wait) {
        ce.setDirection(ce.getDirection().add(direction));
        if (wait) {
            notifyCourseMayHaveChanged(step, phase, "%s was rotated".formatted(ce.getName()), null);
        }
    }

    public Robot getLaserTarget(Position position, Direction direction, int level, boolean highPowerLaser, Step step) {
        return getLaserTarget(position, direction, level, highPowerLaser, false, false, step);
    }

    public Robot getLaserTarget(Position position, Direction direction, int level, boolean highPowerLaser, boolean isCourseMounted, boolean firstFloorGetsHit, Step step) {
        Robot target = null;
        if (firstFloorGetsHit) {
            for (Robot r : CourseHandler.getRobots(course, position)) {
                if (r.isOnCourse()) {
                    if (r.getLevel() == level && !highPowerLaser) {
                        target = r;
                        break;
                    } else if (r.getLevel() == level) {
                        highPowerLaser = false;
                    }
                }
            }
        }
        if (target == null) {
            Floor     sourceFloor    = CourseHandler.getFloor(course, position);
            Position  targetPosition = position.neighbour(direction);
            Floor     targetFloor    = CourseHandler.getFloor(course, targetPosition);
            Direction direction2     = direction.reverse();
            if (targetPosition.inRange(course.getRange())) {
                if (!(sourceFloor.wall(direction) == WallType.SOLID
                        || sourceFloor.wall(direction) == WallType.ONE_WAY_RED
                        || sourceFloor.wall(direction) == WallType.REPULSOR_FIELD
                        || targetFloor.wall(direction2) == WallType.SOLID
                        || targetFloor.wall(direction2) == WallType.REPULSOR_FIELD)) {
                    if (!isCourseMounted) {
                        CourseHandler.setBeam(course, targetPosition, direction, step);
                    }
                    return getLaserTarget(targetPosition, direction, level, highPowerLaser, isCourseMounted, true, step);
                }
            }
        }
        return target;
    }

    public void stepCourseElementsMove(int phase) {
        if (course.getProperties().isExpressConveyorBelts()) {
            expressConveyorBeltsMove(phase);
        }
        if (course.getProperties().isConveyorBelts() || course.getProperties().isExpressConveyorBelts()) {
            conveyorBeltsMove(phase);
        }
        if (course.getProperties().isPushers()) {
            pushersPush(phase);
        }
        if (course.getProperties().isGears()) {
            gearsRotate(phase);
        }
    }

    private void pushersPush(int phase) {
        for (Floor f : new ArrayList<>(course.getFloor())) {
            if (gameAborted) {
                break;
            }
            if (f.isHasPusher() && f.getActiveInPhase()[phase]) {
                for (CourseElement ce : CourseHandler.getObjectsAndRobots(course)) {
                    if (ce.isOnCourse() && ce.getPosition().equals(f.getPosition()) && willBePushed(ce)) {
                        moveInit(ce, f.getPusherDirection(), 1, 1, Step.PUSHERS_PUSH, phase, true, false, null);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.PUSHERS_PUSH, phase, null, null);
        }
    }

    private boolean willBePushed(CourseElement ce) {
        if (ce instanceof CourseObject co) {
            return co.getType() != ObjectType.RANDOMIZER
                    && co.getType() != ObjectType.TELEPORTER
                    && co.getType() != ObjectType.GLUE
                    && co.getType() != ObjectType.OIL
                    && co.getType() != ObjectType.PORTAL_RED
                    && co.getType() != ObjectType.PORTAL_BLUE
                    && co.getType() != ObjectType.PORTAL_YELLOW
                    && co.getType() != ObjectType.PORTAL_PURPLE
                    && co.getType() != ObjectType.PORTAL_GREEN;
        } else {
            return true;
        }
    }

    private void gearsRotate(int phase) {
        for (Floor f : course.getFloor()) {
            if (gameAborted) {
                break;
            }
            if (f.getFloortype() == Floortype.GEARS_CCW || f.getFloortype() == Floortype.GEARS_CW) {
                for (CourseElement ce : CourseHandler.getObjectsAndRobots(course)) {
                    if (ce.isOnCourse()
                            && ce.getPosition().equals(f.getPosition())
                            && !ce.isFlying()
                            && !(ce instanceof Robot r && r.uses(ModuleType.GYROSCOPIC_STABILIZER, phase))) {
                        rotate(ce, f.getFloortype() == Floortype.GEARS_CCW ? WEST : EAST, Step.GEARS_ROTATE, phase, false);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.GEARS_ROTATE, phase, null, null);
        }
    }

    private void conveyorBeltsMove(int phase) {
        boolean needToGoAgain = false;
        int     iteration     = 0;
        for (CourseElement obj : CourseHandler.getObjectsAndRobots(course)) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse()) {
                Floor f = CourseHandler.getFloor(course, obj.getPosition());
                obj.setConflictingMark((f.getFloortype().isConveyorBelt() && !obj.isFlying()) ? 0 : -1);
                if (f.getFloortype().isConveyorBelt()) {
                    needToGoAgain = true;
                }
            }
        }
        while (needToGoAgain && iteration < 20 && !gameAborted) {
            needToGoAgain = false;
            iteration++;
            for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
                if (obj.isOnCourse() && obj.getConflictingMark() == 0) {
                    Floor f           = CourseHandler.getFloor(course, obj.getPosition());
                    Floor targetFloor = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                    if (CourseHandler.cannotMoveAcross(f, targetFloor, f.getFacingDirection(), obj.isFlying())
                            || getCBConflictingElementMark(obj, f.getFacingDirection()) == -1) {
                        obj.setConflictingMark(-1);
                    } else if (getCBConflictingElementMark(obj, f.getFacingDirection()) == 1) {
                        obj.setConflictingMark(-1);
                        rotateObjectsOnConveyorBelts(Step.CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                        setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                    } else {
                        needToGoAgain = true;
                    }
                }
            }
        }
        for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse() && obj.getConflictingMark() == 0) {
                Floor f           = CourseHandler.getFloor(course, obj.getPosition());
                Floor targetFloor = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                rotateObjectsOnConveyorBelts(Step.CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.CONVEYOR_BELTS_MOVE, phase, null, null);
        }
    }

    private void expressConveyorBeltsMove(int phase) {
        boolean needToGoAgain = false;
        int     iteration     = 0;
        for (CourseElement obj : CourseHandler.getObjectsAndRobots(course)) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse()) {
                Floor f = CourseHandler.getFloor(course, obj.getPosition());
                obj.setConflictingMark((f.getFloortype().isExpressConveyorBelt() && !obj.isFlying()) ? 0 : -1);
                if (f.getFloortype().isExpressConveyorBelt()) {
                    needToGoAgain = true;
                }
            }
        }
        while (needToGoAgain && iteration < 20 && !gameAborted) {
            needToGoAgain = false;
            iteration++;
            for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
                if (gameAborted) {
                    break;
                }
                if (obj.isOnCourse() && obj.getConflictingMark() == 0) {
                    Floor f           = CourseHandler.getFloor(course, obj.getPosition());
                    Floor targetFloor = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                    if (CourseHandler.cannotMoveAcross(f, targetFloor, f.getFacingDirection(), obj.isFlying())
                            || getCBConflictingElementMark(obj, f.getFacingDirection()) == -1) {
                        obj.setConflictingMark(-1);
                    } else if (getCBConflictingElementMark(obj, f.getFacingDirection()) == 1) {
                        obj.setConflictingMark(-1);
                        rotateObjectsOnConveyorBelts(Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                        setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                    } else {
                        needToGoAgain = true;
                    }
                }
            }
        }
        for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse()) {
                if (obj.getConflictingMark() == 0) {
                    Floor f           = CourseHandler.getFloor(course, obj.getPosition());
                    Floor targetFloor = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                    rotateObjectsOnConveyorBelts(Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                    setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, null, null);
        }
    }

    private void rotateObjectsOnConveyorBelts(Step step, int phase, CourseElement obj, Floor targetFloor, Floor sourceFloor) {
        if (!(obj instanceof Robot r && r.uses(ModuleType.GYROSCOPIC_STABILIZER, phase))) {
            if ((targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CCW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(WEST)
                    || (targetFloor.getFacingDirection() == WEST && sourceFloor.getFacingDirection() == NORTH))) {
                rotate(obj, WEST, step, phase, false);
            } else if ((targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(EAST)
                    || (sourceFloor.getFacingDirection() == WEST && targetFloor.getFacingDirection() == NORTH))) {
                rotate(obj, EAST, step, phase, false);
            } else if (targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW) {
                if (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(WEST)
                        || (targetFloor.getFacingDirection() == WEST && sourceFloor.getFacingDirection() == NORTH)) {
                    rotate(obj, WEST, step, phase, false);
                } else if ((targetFloor.getFacingDirection() == NORTH && sourceFloor.getFacingDirection() == WEST)
                        || targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(EAST)) {
                    rotate(obj, EAST, step, phase, false);
                }
            }
        }
    }

    public int applyWaterAndOilSlick(CourseElement ce, Floor f, int movement) {
        if (!ce.isFlying() && (f.isWater()
                || CourseHandler.getObjects(course, f.getPosition()).stream().anyMatch(co -> co.getType() == ObjectType.OIL))) {
            if (movement > 0) {
                movement--;
            }
            if (movement < 0) {
                movement++;
            }
        }
        return movement;
    }

    private void reactivateRammingArmor(int phase, Robot robot) {
        if (robot.isReactivateRammingArmor() && robot.getModule(ModuleType.RAMMING_ARMOR) != null) {
            robot.setReactivateRammingArmor(false);
            robot.getModule(ModuleType.RAMMING_ARMOR).getActiveInPhase()[phase] = true;
        }
    }

    private void deactivateRammingArmor(int phase, boolean reactivate) {
        for (Robot robot : course.getRobots()) {
            if (robot.uses(ModuleType.RAMMING_ARMOR, phase)) {
                robot.setReactivateRammingArmor(reactivate);
                robot.getModule(ModuleType.RAMMING_ARMOR).getActiveInPhase()[phase] = false;
            }
        }
    }

    private void openTrapdoors(int phase, List<Robot> robots) {
        for (Robot ce : robots) {
            if (gameAborted) {
                break;
            }
            Floor f = CourseHandler.getFloor(course, ce.getPosition());
            if (f.getFloortype() == Floortype.TRAPDOOR && f.getActiveInPhase()[phase]) {
                die(ce, Step.OPEN_TRAPDOORS, phase);
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.OPEN_TRAPDOORS, phase, null, null);
        }
    }

    public void stepRobotsMove(int phase, List<Robot> robots) {
        if (course.getProperties().isTrapdoor()) {
            openTrapdoors(phase, robots);
        }
        deactivateRammingArmor(phase, true);
        robotsMove(phase, robots);
        deactivateRammingArmor(phase, false);
    }

    private void robotsMove(int phase, List<Robot> robots) {
        for (Robot robot : robots) {
            if (gameAborted) {
                break;
            }
            Programme program = robot.getProgram()[phase];
            Floor     f       = CourseHandler.getFloor(course, robot.getPosition());
            reactivateRammingArmor(phase, robot);
            releaseDevices(phase, robot);
            while (!gameAborted && program != null) {
                int movement = CourseHandler.determineMovementAmount(program, robot.uses(ModuleType.BRAKES, phase));
                if (robot.uses(ModuleType.HOVERCRAFT, phase)) {
                    robot.setFlying(movement != 0);
                }

                Teleporter teleportResult = Teleporter.teleport(robot, movement, this, phase);
                if (teleportResult.isHasBeenTeleported()) {
                    break;
                } else {
                    movement = teleportResult.getOldMovement();
                }

                movement = applyWaterAndOilSlick(robot, f, movement);
                singleRobotMove(phase, robot, program, movement);
                if (robot.isFlying()) {
                    land(robot, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
                }
                program = null;
            }
        }
    }

    private void releaseDevices(int phase, Robot robot) {
        Floor standingOn = CourseHandler.getFloor(course, robot.getPosition());
        if (robot.uses(ModuleType.MOBILE_TELEPORTER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
            de.phlup.circuitchaos.common.model.course.Module module = robot.getModule(ModuleType.MOBILE_TELEPORTER);
            if (module.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, ObjectType.TELEPORTER, robot);
                module.setAmmunition(module.getAmmunition() - 1);
            }
        }
        if (robot.uses(ModuleType.MOBILE_RANDOMIZER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
            de.phlup.circuitchaos.common.model.course.Module module = robot.getModule(ModuleType.MOBILE_RANDOMIZER);
            if (module.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, ObjectType.RANDOMIZER, robot);
                module.setAmmunition(module.getAmmunition() - 1);
            }
        }
        if (robot.uses(ModuleType.GLUE_DISPENSER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
            de.phlup.circuitchaos.common.model.course.Module glueDispenser = robot.getModule(ModuleType.GLUE_DISPENSER);
            if (glueDispenser.getAmmunition() > 0) {
                CourseHandler.createGlue(course, robot.getPosition());
                glueDispenser.setAmmunition(glueDispenser.getAmmunition() - 1);
            }
        }
        if (robot.uses(ModuleType.OIL_DISPENSER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
            de.phlup.circuitchaos.common.model.course.Module oilDispenser = robot.getModule(ModuleType.OIL_DISPENSER);
            if (oilDispenser.getAmmunition() > 0) {
                CourseHandler.createOil(course, robot.getPosition());
                oilDispenser.setAmmunition(oilDispenser.getAmmunition() - 1);
            }
        }
        if (robot.uses(ModuleType.MINE_LAYER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
            de.phlup.circuitchaos.common.model.course.Module mineLayer = robot.getModule(ModuleType.MINE_LAYER);
            if (mineLayer.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, ObjectType.MINE, robot);
                mineLayer.setAmmunition(mineLayer.getAmmunition() - 1);
            }
        }
        if (robot.uses(ModuleType.PROXIMITY_MINE_LAYER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
            de.phlup.circuitchaos.common.model.course.Module mineLayer = robot.getModule(ModuleType.PROXIMITY_MINE_LAYER);
            if (mineLayer.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, ObjectType.PROXIMITY_MINE, robot);
                mineLayer.setAmmunition(mineLayer.getAmmunition() - 1);
            }
        }
        if (robot.uses(ModuleType.BRIDGE_PROJECTOR, phase)) {
            Module bridgeProjector = robot.getModule(ModuleType.BRIDGE_PROJECTOR);
            if (bridgeProjector.getAmmunition() > 0) {
                Floor f2 = CourseHandler.getFloor(course, robot.getPosition().neighbour(robot.getDirection()));
                if (f2.getFloortype() == Floortype.ABYSS || f2.getFloortype() == Floortype.TRAPDOOR) {
                    bridgeProjector.setAmmunition(bridgeProjector.getAmmunition() - 1);
                    f2.setFloortype(Floortype.OPEN_FLOOR);
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.RELEASE_DEVICES, phase, null, null);
        }
    }

    public void stepResolveBeamsAndLaserFire(int phase) {
        stepResolveBeamsAndLaserFire(phase, null);
    }

    public void stepResolveBeamsAndLaserFire(int phase, Robot onlyRobot) {
        List.of(Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_LASER_FIRE).forEach(step ->
                                                               robotMountedBeamsAndLasersShoot(phase, getCurrentShooters(phase, step, onlyRobot), step));
        List.of(Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE,
                Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE,
                Step.COURSE_MOUNTED_LASER_FIRE)
            .forEach(step -> courseMountedLasersShoot(phase, step));
    }

    private void courseMountedLasersShoot(int phase, Step step) {
        CourseProperties properties = course.getProperties();
        boolean hasBeams = switch (step) {
            case COURSE_MOUNTED_LASER_FIRE -> properties.isLasers();
            case COURSE_MOUNTED_PRESSURE_BEAMS_FIRE -> properties.isPressureBeams();
            case COURSE_MOUNTED_TRACTOR_BEAMS_FIRE -> properties.isTractorBeams();
            default -> true;
        };
        if (hasBeams) {
            for (Floor f : new ArrayList<>(course.getFloor())) {
                for (Direction dir : Direction.values()) {
                    Direction dir2 = dir.reverse();
                    int amount = switch (step) {
                        case COURSE_MOUNTED_PRESSURE_BEAMS_FIRE -> f.getPressureBeam()[dir.ordinal()] ? 1 : 0;
                        case COURSE_MOUNTED_TRACTOR_BEAMS_FIRE -> f.getTractorBeam()[dir.ordinal()] ? 1 : 0;
                        default -> f.getLasers()[dir.ordinal()];
                    };
                    for (int cnt = 0; cnt < amount; cnt++) {
                        Robot target = getLaserTarget(f.getPosition(), dir, f.getLevel(), false, true, true, step);
                        if (target != null) {
                            if (step == Step.COURSE_MOUNTED_LASER_FIRE) {
                                target.takeDamage(course, phase, dir2, true);
                            } else if (step == Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE) {
                                moveInit(target, dir, 0, 1, Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE, phase, true, false, null);
                            } else if (step == Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE) {
                                moveInit(target, dir.reverse(), 0, 1, Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE, phase, false, false, null);
                            }
                        }
                    }
                }
            }
            if (!gameAborted) {
                notifyCourseMayHaveChanged(step, phase, null, null);
            }
            CourseHandler.clearBeams(course);
        }
    }

    private void robotMountedBeamsAndLasersShoot(int phase, List<Shooter> shooters, Step step) {
        if (gameAborted) {
            return;
        }
        for (Shooter shooter : shooters) {
            Module weapon = shooter.getWeapon();
            if (weapon != null) {
                Robot      robot           = shooter.getRobot();
                Direction  dir             = robot.getDirection();
                Robot      targetRobot     = shooter.getTarget();
                Robot      highPowerTarget = shooter.getTarget2();
                ModuleType weaponType      = weapon.getType();
                if (targetRobot != null && targetRobot.isOnCourse()) {
                    if (step == Step.ROBOT_MOUNTED_LASER_FIRE
                            && (weaponType == MAIN_LASER
                            || weaponType == ModuleType.DOUBLE_BARREL_LASER
                            || weaponType == ModuleType.HIGH_POWER_LASER
                            || weaponType == ModuleType.REAR_LASER)) {
                        targetRobot.takeDamage(course, phase, dir, true);
                        if (weaponType == ModuleType.DOUBLE_BARREL_LASER) {
                            targetRobot.takeDamage(course, phase, dir, true);
                        }
                    } else if (step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && weaponType == ModuleType.PRESSURE_BEAM) {
                        moveInit(targetRobot, dir, 0, 1, step, phase, true, false, null);
                    } else if (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && weaponType == ModuleType.TRACTOR_BEAM) {
                        moveInit(targetRobot, dir.reverse(), 0, 1, step, phase, false, false, null);
                    } else if (step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && weaponType == ModuleType.EXCHANGE_BEAM) {
                        // Dreiecks-Tausch
                        Position position = targetRobot.getPosition();
                        int      level    = targetRobot.getLevel();
                        targetRobot.setPosition(robot.getPosition());
                        targetRobot.setLevel(robot.getLevel());
                        robot.setPosition(position);
                        robot.setLevel(level);
                    } else if (step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && weaponType == ModuleType.SPIN_LEFT_BEAM) {
                        targetRobot.setDirection(targetRobot.getDirection().add(WEST));
                    } else if (step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && weaponType == ModuleType.SPIN_RIGHT_BEAM) {
                        targetRobot.setDirection(targetRobot.getDirection().add(EAST));
                    }
                }
                if (highPowerTarget != null && step == Step.ROBOT_MOUNTED_LASER_FIRE && highPowerTarget.isOnCourse()) {
                    highPowerTarget.takeDamage(course, phase, dir, true);
                }
            }
        }
        if (!gameAborted && shooters.stream().anyMatch(s -> s.getWeapon() != null)) {
            notifyCourseMayHaveChanged(step, phase, null, null);
        }
        if (shooters.stream().anyMatch(s -> s.getWeapon() != null)) {
            CourseHandler.clearBeams(course);
        }
    }

    private List<Shooter> getCurrentShooters(int phase, Step step, Robot onlyRobot) {
        List<Shooter> shooters = new ArrayList<>();
        for (Robot robot : course.getRobots()) {
            if (gameAborted) {
                break;
            }
            if (onlyRobot != null && robot != onlyRobot) {
                continue;
            }
            if (robot.isOnCourse() && !robot.isPoweredDown()) {
                shooters.add(new Shooter(phase, robot, this, step));
            }
        }
        return shooters;
    }

}
