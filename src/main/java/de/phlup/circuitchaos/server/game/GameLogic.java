package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.ProgramType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.CourseElement;
import de.phlup.circuitchaos.common.model.course.CourseObject;
import de.phlup.circuitchaos.common.model.course.Floor;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Programme;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

import static de.phlup.circuitchaos.common.enums.Direction.EAST;
import static de.phlup.circuitchaos.common.enums.Direction.NORTH;
import static de.phlup.circuitchaos.common.enums.Direction.SOUTH;
import static de.phlup.circuitchaos.common.enums.Direction.WEST;

@RequiredArgsConstructor
public class GameLogic {

    private static final int MINE_INITIAL_DAMAGE = 4;

    private final Course               course;
    private final CourseChangeListener listener;

    private void notifyCourseMayHaveChanged(Step step, Integer phase, String detail, String movingRobotName) {
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

    private void minesGoOff(Step step, int phase, Floor floor3) {
        minesGoesOff(floor3, null, step, phase);
        minesGoesOff(floor3, NORTH, step, phase);
        minesGoesOff(floor3, EAST, step, phase);
        minesGoesOff(floor3, SOUTH, step, phase);
        minesGoesOff(floor3, WEST, step, phase);
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

    public CourseElement getConflictingElement(CourseElement courseElement, Direction direction) {
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

}
