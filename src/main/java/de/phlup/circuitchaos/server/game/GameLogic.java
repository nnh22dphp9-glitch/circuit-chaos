package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.ModuleType;
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
import de.phlup.circuitchaos.common.records.Range;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.phlup.circuitchaos.common.enums.Direction.*;
import static de.phlup.circuitchaos.common.enums.Floortype.*;
import static de.phlup.circuitchaos.common.enums.ModuleType.*;
import static de.phlup.circuitchaos.common.enums.ObjectType.*;
import static de.phlup.circuitchaos.common.enums.ProgramType.*;
import static de.phlup.circuitchaos.common.enums.Step.*;
import static de.phlup.circuitchaos.common.enums.WallType.*;

@Slf4j
@RequiredArgsConstructor
public class GameLogic {

    private static final int MINE_INITIAL_DAMAGE = 4;
    private static final int MAX_DIST_FOR_MINES  = 3;

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
            moveInit(robot, robot.getDirection(), movement, movement, ROBOTS_AND_OBJECTS_MOVE, phase, true, true, robot.getName());
        } else if (movement < 0) {
            moveInit(robot, SOUTH.add(robot.getDirection()), movement, -movement, ROBOTS_AND_OBJECTS_MOVE, phase, true, true, robot.getName());
        }
        if (pc.type() == ROTATE_RIGHT) {
            rotate(robot, EAST, ROBOTS_AND_OBJECTS_MOVE, phase, true);
        } else if (pc.type() == ROTATE_LEFT) {
            rotate(robot, WEST, ROBOTS_AND_OBJECTS_MOVE, phase, true);
        } else if (pc.type() == U_TURN) {
            rotate(robot, SOUTH, ROBOTS_AND_OBJECTS_MOVE, phase, true);
        }
    }

    public void moveInit(CourseElement ce, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot, boolean wait, String movingRobotName) {
        course.getProperties().getSwitchPositions().forEach(pos -> {
            if (CourseHandler.getRobots(course, pos).isEmpty()) {
                CourseHandler.getFloor(course, pos).setSwitchActive(true);
            }
        });
        move(ce, direction, strength, amount, step, phase, notBlockedByMovableRobot, wait, movingRobotName, true);
    }

    private boolean move(CourseElement ce, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot, boolean wait, String movingRobotName, boolean considerRepulsor) {
        Position oldPosition    = ce.getPosition();
        Floor    floor1         = CourseHandler.getFloor(course, oldPosition);
        Position targetPosition = ce.getPosition().neighbour(direction);
        Floor    floor2         = CourseHandler.getFloor(course, targetPosition);
        if (!(ce instanceof Robot r) || !r.isFlying()) {
            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, floor1.getPosition()))) {
                if (co.getType() == GLUE && co.isActive()) {
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
            boolean rampPassage = sourceWall == RAMP_UP || sourceWall == RAMP_DOWN
                    || targetWall == RAMP_UP || targetWall == RAMP_DOWN;
            boolean uphill           = floor2.getLevel() > floor1.getLevel() && !rampPassage;
            boolean downhill         = floor2.getLevel() < floor1.getLevel() && !rampPassage;
            boolean repulsorPassage1 = sourceWall == REPULSOR_FIELD && !floor1.getActiveInPhase()[phase];
            boolean repulsorPassage2 = targetWall == REPULSOR_FIELD && !floor2.getActiveInPhase()[phase];
            boolean wallPassage = ((sourceWall == NONE
                    || sourceWall == ONE_WAY_GREEN
                    || sourceWall == RAMP_DOWN
                    || sourceWall == RAMP_UP
                    || repulsorPassage1)
                    && (targetWall == NONE
                    || targetWall == ONE_WAY_GREEN
                    || targetWall == RAMP_UP
                    || targetWall == RAMP_DOWN
                    || targetWall == LEDGE
                    || repulsorPassage2))
                    || (sourceWall == ONE_WAY_GREEN && targetWall == ONE_WAY_RED);
            if ((ce instanceof Robot r && r.isFlying()) || (!uphill && wallPassage)) {
                CourseElement conflictingElement = getConflictingElement(ce, direction);
                if (conflictingElement == null) {
                    ce.setPosition(targetPosition);
                    if (ce instanceof Robot r && !r.isFlying()) {
                        ce.setLevel(floor2.getLevel());
                        if (downhill) {
                            r.takeDamage(course, phase, null, false);
                        }
                    }
                } else if (notBlockedByMovableRobot && isNoRobotStack(targetPosition)) {
                    if (ce instanceof Robot r && r.uses(RAMMING_ARMOR, phase)) {
                        r.getModule(RAMMING_ARMOR).getActiveInPhase()[phase] = false;
                        if (conflictingElement instanceof Robot rob) {
                            rob.takeDamage(course, phase, null, false);
                        }
                    }
                    if (move(conflictingElement, direction, strength, 1, step, phase, true, wait, movingRobotName, considerRepulsor)) {
                        ce.setPosition(targetPosition);
                        if (!(ce instanceof Robot r) || !r.isFlying()) {
                            ce.setLevel(floor2.getLevel());
                            if (downhill && ce instanceof Robot r) {
                                r.takeDamage(course, phase, null, false);
                            }
                        }
                    }
                }
            } else if (considerRepulsor) {
                if ((floor1.wall(direction) == REPULSOR_FIELD && floor1.getActiveInPhase()[phase])
                        || ((floor1.wall(direction) == NONE
                        || floor1.wall(direction) == ONE_WAY_GREEN
                        || floor1.wall(direction) == RAMP_DOWN)
                        && floor2.wall(direction2) == REPULSOR_FIELD
                        && floor2.getActiveInPhase()[phase])) {
                    move(ce, direction2, strength, strength, step, phase, true, wait, movingRobotName, false);
                }
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
            if (floor3.getFloortype() == SWITCH && floor3.getSwitchTarget() != null && floor3.isSwitchActive()) {
                Floor floor4 = CourseHandler.getFloor(course, floor3.getSwitchTarget());
                for (int i = 0; i < 5; i++) {
                    floor4.getActiveInPhase()[i] = !floor4.getActiveInPhase()[i];
                }
                floor3.setSwitchActive(false);
                if (wait) {
                    notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
                }
            }
        }
        if ((!(ce instanceof Robot r) || !r.isFlying())
                && (floor3.getFloortype() == ABYSS || (floor3.getFloortype() == TRAPDOOR && floor3.getActiveInPhase()[phase]))) {
            amount = 0;
            die(ce, step, phase);
            if (ce instanceof Robot r) {
                course.getRobotsFallingIntoAbyss().add(r);
            } else if (ce instanceof CourseObject co) {
                course.getObjectsFallingIntoAbyss().add(co);
            }
        }
        if (amount > 1) {
            move(ce, direction, strength, amount - 1, step, phase, true, wait, movingRobotName, considerRepulsor);
        }
        if (!oldPosition.equals(ce.getPosition()) && !(ce instanceof CourseObject co && co.getType() == OIL)) {
            boolean notBlocked = true;
            if (!(ce instanceof Robot r) || !r.isFlying()) {
                while (notBlocked && CourseHandler.getObjects(course, ce.getPosition()).stream().anyMatch(co -> co.getType() == OIL)) {
                    notBlocked = move(ce, direction, 0, 1, step, phase, false, wait, movingRobotName, considerRepulsor);
                }
            }
        }
        if (wait) {
            notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
        }
        return !oldPosition.equals(ce.getPosition());
    }

    private boolean isNoRobotStack(Position position) {
        return CourseHandler.getRobots(course, position).stream().filter(r -> !r.isFlying()).count() < 2;
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
                if (co.isActive() && (co.getType() == PROXIMITY_MINE
                        || (dir == null && co.getType() == MINE))) {
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
            if (robot.uses(MECHANICAL_ARM, phase)) {
                if (!(checkpoint != null || floor.getFloortype() == PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x(), robot.getPosition().y() - 1));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x(), robot.getPosition().y() - 1));
                }
                if (!(checkpoint != null || floor.getFloortype() == PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x() + 1, robot.getPosition().y()));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x() + 1, robot.getPosition().y()));
                }
                if (!(checkpoint != null || floor.getFloortype() == PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x(), robot.getPosition().y() + 1));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x(), robot.getPosition().y() + 1));
                }
                if (!(checkpoint != null || floor.getFloortype() == PIT_STOP)) {
                    floor = CourseHandler.getFloor(course, new Position(robot.getPosition().x() - 2, robot.getPosition().y()));
                    checkpoint = CourseHandler.getCheckpoint(course, new Position(robot.getPosition().x() - 1, robot.getPosition().y()));
                }
            }
            if (checkpoint != null || floor.getFloortype() == PIT_STOP) {
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
                        goOff(TOUCH_CHECKPOINTS, phase, object);
                    }
                }
                case PROXIMITY_MINE -> {
                    Position position = object.getPosition();
                    if (!(CourseHandler.getRobots(course, position).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x() + 1, position.y())).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x(), position.y() + 1)).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x() - 1, position.y())).isEmpty()
                            && CourseHandler.getRobots(course, new Position(position.x(), position.y() - 1)).isEmpty())) {
                        goOff(TOUCH_CHECKPOINTS, phase, object);
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
        if (f.getFloortype() == ABYSS || (f.getFloortype() == TRAPDOOR && f.getActiveInPhase()[phase])) {
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
                die(courseObject, ROBOTS_AND_OBJECTS_MOVE, phase);
            }
        }
    }

    public void goOff(Step step, Integer phase, CourseObject courseObject) {
        if (courseObject.getType() == MINE || courseObject.getType() == PROXIMITY_MINE) {
            CourseHandler.remove(course, courseObject);
            explode(step, phase, courseObject.getPosition(), MINE_INITIAL_DAMAGE);
        }
    }

    public void explode(Step step, Integer phase, Position position, int initialDamage) {
        if (initialDamage > 0) {
            Range range = Range.rangeAroundPosition(position, MAX_DIST_FOR_MINES);
            DistanceCalculator.calculateDistance(course, position, MAX_DIST_FOR_MINES, range);
            if (course.isConsiderGui()) {
                for (Floor f : CourseHandler.getFloorsInRange(course, range)) {
                    if (f.getDistanceCounter() < MAX_DIST_FOR_MINES) {
                        int damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                        if (damage > 0) {
                            Floor floor = CourseHandler.getFloor(course, f.getPosition());
                            floor.setExplosiveDamage(damage);
                        }
                    }
                }
            }
            for (Robot robot : List.copyOf(course.getRobots())) {
                if (robot.getPosition().inRange(range)) {
                    Floor f      = CourseHandler.getFloor(course, robot.getPosition());
                    int   damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                    for (int damagePoints = 0; damagePoints < damage; damagePoints++) {
                        robot.takeDamage(course);
                    }
                }
            }
            if (step != CONVEYOR_BELTS_MOVE && step != EXPRESS_CONVEYOR_BELTS_MOVE) {
                notifyCourseMayHaveChanged(step, phase, "explosion", null);
            }
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
        if (!(ce instanceof Robot r) || !r.isFlying()) {
            ce.setLevel(floor3.getLevel());
            boolean rampPassage = false;
            if (direction != null) {
                WallType sourceWall = previousFloor.wall(direction);
                WallType targetWall = floor3.wall(direction.reverse());
                rampPassage = sourceWall == RAMP_UP || sourceWall == RAMP_DOWN
                        || targetWall == RAMP_UP || targetWall == RAMP_DOWN;
            }
            if (ce instanceof Robot r && floor3.getLevel() < previousFloor.getLevel() && !rampPassage) {
                r.takeDamage(course, phase, null, false);
            }
        }
        if (floor3.getFloortype() == ABYSS || (floor3.getFloortype() == TRAPDOOR && floor3.getActiveInPhase()[phase])) {
            die(ce, step, phase);
            if (ce instanceof Robot r) {
                course.getRobotsFallingIntoAbyss().add(r);
            } else if (ce instanceof CourseObject co) {
                course.getObjectsFallingIntoAbyss().add(co);
            }
        }
        if (direction != null && !(ce instanceof CourseObject co && co.getType() == OIL)) {
            if (CourseHandler.getObjects(course, floor3.getPosition()).stream().anyMatch(co -> co.getType() == OIL)) {
                moveInit(ce, direction, 0, 1, step, phase, false, wait, null);
            }
        }
        moveInit(ce, NORTH, 0, 0, step, phase, false, wait, null);
    }

    private CourseElement getConflictingElement(CourseElement courseElement, Direction direction) {
        if (!courseElement.isOnCourse() || (courseElement instanceof Robot r && r.isFlying())) {
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

    public Robot getLaserTarget(Position position, Direction direction, int level, boolean highPowerLaser, int amount, boolean firstFloorGetsHit, Step step, int phase) {
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
            if (course.isConsiderGui()) {
                CourseHandler.setBeam(course, position, direction, step, amount);
            }
        }
        if (target == null) {
            Floor     sourceFloor    = CourseHandler.getFloor(course, position);
            Position  targetPosition = position.neighbour(direction);
            Floor     targetFloor    = CourseHandler.getFloor(course, targetPosition);
            Direction direction2     = direction.reverse();
            if (targetPosition.inRange(course.getRange())) {
                if (!(sourceFloor.wall(direction) == SOLID
                        || sourceFloor.wall(direction) == ONE_WAY_RED
                        || (sourceFloor.wall(direction) == REPULSOR_FIELD && sourceFloor.getActiveInPhase()[phase])
                        || targetFloor.wall(direction2) == SOLID
                        || (targetFloor.wall(direction2) == REPULSOR_FIELD) && targetFloor.getActiveInPhase()[phase])) {
                    if (course.isConsiderGui()) {
                        CourseHandler.setBeam(course, targetPosition, direction, step, amount);
                    }
                    return getLaserTarget(targetPosition, direction, level, highPowerLaser, amount, true, step, phase);
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
        if (gameAborted) {
            return;
        }
        List<CourseElement> ceList = CourseHandler.getObjectsAndRobots(course).stream()
                                                  .filter(CourseElement::isOnCourse)
                                                  .filter(this::willBePushed)
                                                  .toList();
        for (CourseElement ce : ceList) {
            if (gameAborted) {
                return;
            }
            Position pos   = ce.getPosition();
            Floor    floor = CourseHandler.getFloor(course, pos);
            if (floor.isHasPusher() && floor.getActiveInPhase()[phase]) {
                moveInit(ce, floor.getPusherDirection(), 1, 1, PUSHERS_PUSH, phase, true, false, null);
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(PUSHERS_PUSH, phase, null, null);
        }
    }

    private boolean willBePushed(CourseElement ce) {
        if (ce instanceof CourseObject co) {
            return co.getType() == MINE || co.getType() == PROXIMITY_MINE;
        } else {
            return true;
        }
    }

    private void gearsRotate(int phase) {
        if (gameAborted) {
            return;
        }
        List<CourseElement> ceList = CourseHandler.getObjectsAndRobots(course).stream()
                                                  .filter(CourseElement::isOnCourse)
                                                  .filter(ce -> !(ce instanceof Robot r && r.isFlying()))
                                                  .filter(ce -> !(ce instanceof Robot r && r.uses(GYROSCOPIC_STABILIZER, phase)))
                                                  .toList();
        for (CourseElement ce : ceList) {
            if (gameAborted) {
                return;
            }
            Position pos   = ce.getPosition();
            Floor    floor = CourseHandler.getFloor(course, pos);
            if (floor.getFloortype() == GEARS_CCW || floor.getFloortype() == GEARS_CW) {
                rotate(ce, floor.getFloortype() == GEARS_CCW ? WEST : EAST, GEARS_ROTATE, phase, false);
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(GEARS_ROTATE, phase, null, null);
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
                obj.setConflictingMark((f.getFloortype().isConveyorBelt() && (!(obj instanceof Robot r) || !r.isFlying())) ? 0 : -1);
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
                    if (CourseHandler.cannotMoveAcross(f, targetFloor, f.getFacingDirection(), obj instanceof Robot r && r.isFlying())
                            || getCBConflictingElementMark(obj, f.getFacingDirection()) == -1) {
                        obj.setConflictingMark(-1);
                    } else if (getCBConflictingElementMark(obj, f.getFacingDirection()) == 1) {
                        obj.setConflictingMark(-1);
                        rotateObjectsOnConveyorBelts(CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                        setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
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
                rotateObjectsOnConveyorBelts(CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(CONVEYOR_BELTS_MOVE, phase, null, null);
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
                obj.setConflictingMark((f.getFloortype().isExpressConveyorBelt() && (!(obj instanceof Robot r) || !r.isFlying())) ? 0 : -1);
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
                    if (CourseHandler.cannotMoveAcross(f, targetFloor, f.getFacingDirection(), obj instanceof Robot r && r.isFlying())
                            || getCBConflictingElementMark(obj, f.getFacingDirection()) == -1) {
                        obj.setConflictingMark(-1);
                    } else if (getCBConflictingElementMark(obj, f.getFacingDirection()) == 1) {
                        obj.setConflictingMark(-1);
                        rotateObjectsOnConveyorBelts(EXPRESS_CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                        setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), EXPRESS_CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
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
                    rotateObjectsOnConveyorBelts(EXPRESS_CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                    setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), EXPRESS_CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(EXPRESS_CONVEYOR_BELTS_MOVE, phase, null, null);
        }
    }

    private void rotateObjectsOnConveyorBelts(Step step, int phase, CourseElement obj, Floor targetFloor, Floor sourceFloor) {
        if (!(obj instanceof Robot r && r.uses(GYROSCOPIC_STABILIZER, phase))) {
            if ((targetFloor.getFloortype() == TURNING_EXPRESS_CONVEYOR_BELT_CCW
                    || targetFloor.getFloortype() == TURNING_CONVEYOR_BELT_CCW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(WEST)
                    || (targetFloor.getFacingDirection() == WEST && sourceFloor.getFacingDirection() == NORTH))) {
                rotate(obj, WEST, step, phase, false);
            } else if ((targetFloor.getFloortype() == TURNING_EXPRESS_CONVEYOR_BELT_CW
                    || targetFloor.getFloortype() == TURNING_CONVEYOR_BELT_CW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(EAST)
                    || (sourceFloor.getFacingDirection() == WEST && targetFloor.getFacingDirection() == NORTH))) {
                rotate(obj, EAST, step, phase, false);
            } else if (targetFloor.getFloortype() == TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                    || targetFloor.getFloortype() == TURNING_CONVEYOR_BELT_CW_CCW) {
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
        if ((!(ce instanceof Robot r) || !r.isFlying()) && (f.isWater()
                || CourseHandler.getObjects(course, f.getPosition()).stream().anyMatch(co -> co.getType() == OIL))) {
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
        if (robot.isReactivateRammingArmor() && robot.getModule(RAMMING_ARMOR) != null) {
            robot.setReactivateRammingArmor(false);
            robot.getModule(RAMMING_ARMOR).getActiveInPhase()[phase] = true;
        }
    }

    private void deactivateRammingArmor(int phase, boolean reactivate) {
        for (Robot robot : course.getRobots()) {
            if (robot.uses(RAMMING_ARMOR, phase)) {
                robot.setReactivateRammingArmor(reactivate);
                robot.getModule(RAMMING_ARMOR).getActiveInPhase()[phase] = false;
            }
        }
    }

    private void openTrapdoors(int phase, List<Robot> robots) {
        for (Robot ce : robots) {
            if (gameAborted) {
                break;
            }
            Floor f = CourseHandler.getFloor(course, ce.getPosition());
            if (f.getFloortype() == TRAPDOOR && f.getActiveInPhase()[phase]) {
                die(ce, OPEN_TRAPDOORS, phase);
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(OPEN_TRAPDOORS, phase, null, null);
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
                int movement = CourseHandler.determineMovementAmount(program, robot.uses(BRAKES, phase));
                if (robot.uses(HOVERCRAFT, phase)) {
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
                    land(robot, ROBOTS_AND_OBJECTS_MOVE, phase);
                }
                program = null;
            }
        }
    }

    private void releaseDevices(int phase, Robot robot) {
        Floor standingOn = CourseHandler.getFloor(course, robot.getPosition());
        if (robot.uses(MOBILE_TELEPORTER, phase) && standingOn.getFloortype() == OPEN_FLOOR) {
            Module module = robot.getModule(MOBILE_TELEPORTER);
            if (module.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, TELEPORTER, robot);
                module.setAmmunition(module.getAmmunition() - 1);
            }
        }
        if (robot.uses(MOBILE_RANDOMIZER, phase) && standingOn.getFloortype() == OPEN_FLOOR) {
            Module module = robot.getModule(MOBILE_RANDOMIZER);
            if (module.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, RANDOMIZER, robot);
                module.setAmmunition(module.getAmmunition() - 1);
            }
        }
        if (robot.uses(GLUE_DISPENSER, phase) && standingOn.getFloortype() == OPEN_FLOOR) {
            Module glueDispenser = robot.getModule(GLUE_DISPENSER);
            if (glueDispenser.getAmmunition() > 0) {
                CourseHandler.createGlue(course, robot.getPosition());
                glueDispenser.setAmmunition(glueDispenser.getAmmunition() - 1);
            }
        }
        if (robot.uses(OIL_DISPENSER, phase) && standingOn.getFloortype() == OPEN_FLOOR) {
            Module oilDispenser = robot.getModule(OIL_DISPENSER);
            if (oilDispenser.getAmmunition() > 0) {
                CourseHandler.createOil(course, robot.getPosition());
                oilDispenser.setAmmunition(oilDispenser.getAmmunition() - 1);
            }
        }
        if (robot.uses(MINE_LAYER, phase) && standingOn.getFloortype() == OPEN_FLOOR) {
            Module mineLayer = robot.getModule(MINE_LAYER);
            if (mineLayer.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, MINE, robot);
                mineLayer.setAmmunition(mineLayer.getAmmunition() - 1);
            }
        }
        if (robot.uses(PROXIMITY_MINE_LAYER, phase) && standingOn.getFloortype() == OPEN_FLOOR) {
            Module mineLayer = robot.getModule(PROXIMITY_MINE_LAYER);
            if (mineLayer.getAmmunition() > 0) {
                CourseHandler.createCircuitChaosObject(course, PROXIMITY_MINE, robot);
                mineLayer.setAmmunition(mineLayer.getAmmunition() - 1);
            }
        }
        if (robot.uses(BRIDGE_PROJECTOR, phase)) {
            Module bridgeProjector = robot.getModule(BRIDGE_PROJECTOR);
            if (bridgeProjector.getAmmunition() > 0) {
                Floor f2 = CourseHandler.getFloor(course, robot.getPosition().neighbour(robot.getDirection()));
                if (f2.getFloortype() == ABYSS || f2.getFloortype() == TRAPDOOR) {
                    bridgeProjector.setAmmunition(bridgeProjector.getAmmunition() - 1);
                    f2.setFloortype(OPEN_FLOOR);
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(RELEASE_DEVICES, phase, null, null);
        }
    }

    public void stepResolveBeamsAndLaserFire(int phase) {
        stepResolveBeamsAndLaserFire(phase, null);
    }

    public void stepResolveBeamsAndLaserFire(int phase, Robot onlyRobot) {
        List.of(ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE,
                ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE,
                ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE,
                ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE,
                ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE,
                ROBOT_MOUNTED_LASER_FIRE).forEach(step ->
                                                               robotMountedBeamsAndLasersShoot(phase, getCurrentShooters(phase, step, onlyRobot), step));
        List.of(COURSE_MOUNTED_TRACTOR_BEAMS_FIRE,
                COURSE_MOUNTED_PRESSURE_BEAMS_FIRE,
                COURSE_MOUNTED_LASER_FIRE)
            .forEach(step -> courseMountedLasersShoot(phase, step));
    }

    private void courseMountedLasersShoot(int phase, Step step) {
        CourseProperties properties = course.getProperties();
        Set<Position> positions = switch (step) {
            case COURSE_MOUNTED_LASER_FIRE -> properties.getLaserPositions();
            case COURSE_MOUNTED_PRESSURE_BEAMS_FIRE -> properties.getPressureBeamPositions();
            case COURSE_MOUNTED_TRACTOR_BEAMS_FIRE -> properties.getTractorBeamPositions();
            default -> Set.of();
        };
        if (!positions.isEmpty()) {
            if (course.isConsiderGui()) {
                for (Floor f : course.getFloor()) {
                    f.setCourseMountedLaserBeamsNS(0);
                    f.setCourseMountedLaserBeamsWE(0);
                    f.setCourseMountedPressureBeamsNS(false);
                    f.setCourseMountedPressureBeamsWE(false);
                    f.setCourseMountedTractorBeamsNS(false);
                    f.setCourseMountedTractorBeamsWE(false);
                }
            }
            for (Position position : positions) {
                Floor f = CourseHandler.getFloor(course, position);
                for (Direction dir : Direction.values()) {
                    Direction dir2 = dir.reverse();
                    int amount = switch (step) {
                        case COURSE_MOUNTED_PRESSURE_BEAMS_FIRE -> f.getPressureBeam()[dir.ordinal()] ? 1 : 0;
                        case COURSE_MOUNTED_TRACTOR_BEAMS_FIRE -> f.getTractorBeam()[dir.ordinal()] ? 1 : 0;
                        default -> f.getLasers()[dir.ordinal()];
                    };
                    for (int cnt = 0; cnt < amount; cnt++) {
                        Robot target = getLaserTarget(f.getPosition(), dir, f.getLevel(), false, amount, true, step, phase);
                        if (target != null) {
                            if (step == COURSE_MOUNTED_LASER_FIRE) {
                                target.takeDamage(course, phase, dir2, true);
                            } else if (step == COURSE_MOUNTED_PRESSURE_BEAMS_FIRE) {
                                moveInit(target, dir, 0, 1, COURSE_MOUNTED_PRESSURE_BEAMS_FIRE, phase, true, false, null);
                            } else { // if (step == COURSE_MOUNTED_TRACTOR_BEAMS_FIRE) {
                                moveInit(target, dir.reverse(), 0, 1, COURSE_MOUNTED_TRACTOR_BEAMS_FIRE, phase, false, false, null);
                            }
                        }
                    }
                }
            }
            if (!gameAborted) {
                notifyCourseMayHaveChanged(step, phase, null, null);
            }
            if (course.isConsiderGui()) {
                CourseHandler.clearBeams(course);
            }
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
                    if (step == ROBOT_MOUNTED_LASER_FIRE
                            && (weaponType == MAIN_LASER
                            || weaponType == DOUBLE_BARREL_LASER
                            || weaponType == HIGH_POWER_LASER
                            || weaponType == REAR_LASER)) {
                        targetRobot.takeDamage(course, phase, dir, true);
                        if (weaponType == DOUBLE_BARREL_LASER) {
                            targetRobot.takeDamage(course, phase, dir, true);
                        }
                    } else if (step == ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && weaponType == PRESSURE_BEAM) {
                        moveInit(targetRobot, dir, 0, 1, step, phase, true, false, null);
                    } else if (step == ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && weaponType == TRACTOR_BEAM) {
                        moveInit(targetRobot, dir.reverse(), 0, 1, step, phase, false, false, null);
                    } else if (step == ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && weaponType == EXCHANGE_BEAM) {
                        // Dreiecks-Tausch
                        Position position = targetRobot.getPosition();
                        int      level    = targetRobot.getLevel();
                        targetRobot.setPosition(robot.getPosition());
                        targetRobot.setLevel(robot.getLevel());
                        robot.setPosition(position);
                        robot.setLevel(level);
                    } else if (step == ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && weaponType == SPIN_LEFT_BEAM) {
                        targetRobot.setDirection(targetRobot.getDirection().add(WEST));
                    } else if (step == ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && weaponType == SPIN_RIGHT_BEAM) {
                        targetRobot.setDirection(targetRobot.getDirection().add(EAST));
                    }
                }
                if (highPowerTarget != null && step == ROBOT_MOUNTED_LASER_FIRE && highPowerTarget.isOnCourse()) {
                    highPowerTarget.takeDamage(course, phase, dir, true);
                }
            }
        }
        if (!gameAborted && shooters.stream().anyMatch(s -> s.getWeapon() != null)) {
            notifyCourseMayHaveChanged(step, phase, null, null);
        }
        if (course.isConsiderGui() && shooters.stream().anyMatch(s -> s.getWeapon() != null)) {
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
            if (robot.isOnCourse() && !robot.isPoweredDown() &&
                    switch (step) {
                        case ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE -> robot.uses(TRACTOR_BEAM, phase);
                        case ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE -> robot.uses(PRESSURE_BEAM, phase);
                        case ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE -> robot.uses(SPIN_LEFT_BEAM, phase);
                        case ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE -> robot.uses(SPIN_RIGHT_BEAM, phase);
                        case ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE -> robot.uses(EXCHANGE_BEAM, phase);
                        default -> true;
                    }) {
                shooters.add(new Shooter(phase, robot, this, step));
            }
        }
        return shooters;
    }

}
