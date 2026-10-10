package de.phlup.circuitchaos.common;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Checkpoint;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseElement;
import de.phlup.circuitchaos.common.model.CourseElementStub;
import de.phlup.circuitchaos.common.model.CourseObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Robot;
import de.phlup.circuitchaos.server.player.Player;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static de.phlup.circuitchaos.common.enums.ModuleType.EXCHANGE_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.MAIN_LASER;
import static de.phlup.circuitchaos.common.enums.ModuleType.PRESSURE_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.SPIN_LEFT_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.SPIN_RIGHT_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.TRACTOR_BEAM;
import static de.phlup.circuitchaos.server.GlobalServerAttributes.RANDOM;

public class CourseHandler {

    public static void add(Course course, CourseElement ce) {
        if (ce != null) {
            ce.setOnCourse(true);
            if (ce instanceof Robot r) {
                course.getRobots().add(r);
            } else if (ce instanceof CourseObject co) {
                course.getObjects().add(co);
            }
        }
    }

    public static void remove(Course course, CourseElement ce) {
        if (ce != null) {
            ce.setOnCourse(false);
            if (ce instanceof Robot) {
                course.getRobots().remove(ce);
            } else if (ce instanceof CourseObject) {
                course.getObjects().remove(ce);
            }
        }
    }

    /**
     * Liefert im Zweifelsfall ein ABYSS zurück, nicht null. Ist so im Spielverlauf sinnvoll.
     */
    @NotNull
    public static Floor getFloor(Course course, Position position) {
        for (Floor floor : course.getFloor()) {
            if (floor.getPosition().x() == position.x() && floor.getPosition().y() == position.y()) {
                return floor;
            }
        }
        Floor floor = new Floor();
        floor.setFloortype(Floortype.ABYSS);
        floor.setPosition(position);
        return floor;
    }

    public static List<CourseElement> getObjectsAndRobots(Course course) {
        List<CourseElement> list = new ArrayList<>();
        list.addAll(course.getRobots());
        list.addAll(course.getObjects());
        return list;
    }

    public static List<Robot> getRobots(Course course) {
        return course.getRobots();
    }

    public static List<CourseObject> getObjects(Course course, Position position) {
        List<CourseObject> elements2 = new ArrayList<>();
        for (CourseObject ce : course.getObjects()) {
            if (position.x() == ce.getPosition().x() && position.y() == ce.getPosition().y()) {
                elements2.add(ce);
            }
        }
        return elements2;
    }

    public static List<CourseObject> getLeavingObjects(Course course, Position position) {
        List<CourseObject> elements2 = new ArrayList<>();
        for (CourseObject ce : course.getObjects()) {
            if (!ce.getPosition().equals(ce.getPrevPosition())) {
                if (position.x() == ce.getPrevPosition().x() && position.y() == ce.getPrevPosition().y()) {
                    elements2.add(ce);
                }
            }
        }
        return elements2;
    }

    public static List<Robot> getRobots(Course course, Position position) {
        List<Robot> elements2 = new ArrayList<>();
        for (Robot ce : course.getRobots()) {
            if (position.x() == ce.getPosition().x() && position.y() == ce.getPosition().y()) {
                elements2.add(ce);
            }
        }
        return elements2;
    }

    public static List<Robot> getLeavingRobots(Course course, Position position) {
        List<Robot> elements2 = new ArrayList<>();
        for (Robot ce : course.getRobots()) {
            if (!ce.getPosition().equals(ce.getPrevPosition())) {
                if (position.x() == ce.getPrevPosition().x() && position.y() == ce.getPrevPosition().y()) {
                    elements2.add(ce);
                }
            }
        }
        return elements2;
    }

    public static List<CourseElement> getFallingIntoAbyss(Course course, Position position) {
        List<CourseElement> elements2 = new ArrayList<>();
        for (CourseElement ce : course.getRobotsFallingIntoAbyss()) {
            if (position.x() == ce.getPosition().x() && position.y() == ce.getPosition().y()) {
                elements2.add(ce);
            }
        }
        for (CourseElement ce : course.getObjectsFallingIntoAbyss()) {
            if (position.x() == ce.getPosition().x() && position.y() == ce.getPosition().y()) {
                elements2.add(ce);
            }
        }
        return elements2;
    }

    public static List<CourseElement> getLeavingFallingIntoAbyss(Course course, Position position) {
        List<CourseElement> elements2 = new ArrayList<>();
        for (CourseElement ce : course.getRobotsFallingIntoAbyss()) {
            if (!ce.getPosition().equals(ce.getPrevPosition())) {
                if (position.x() == ce.getPrevPosition().x() && position.y() == ce.getPrevPosition().y()) {
                    elements2.add(ce);
                }
            }
        }
        for (CourseElement ce : course.getObjectsFallingIntoAbyss()) {
            if (!ce.getPosition().equals(ce.getPrevPosition())) {
                if (position.x() == ce.getPrevPosition().x() && position.y() == ce.getPrevPosition().y()) {
                    elements2.add(ce);
                }
            }
        }
        return elements2;
    }

    public static Checkpoint getCheckpoint(Course course, Position position) {
        for (Checkpoint cp : course.getCheckpoints()) {
            if (cp.getPosition().x() == position.x() && cp.getPosition().y() == position.y()) {
                return cp;
            }
        }
        return null;
    }

    public static CourseObject getPortal(Course course, Position position) {
        for (CourseObject obj : getObjects(course, position)) {
            if (obj.getType().isPortal()) {
                return obj;
            }
        }
        return null;
    }

    public static void createCircuitChaosObject(Course course, ObjectType objectType, Player creator) {
        CourseObject newRRO = createCircuitChaosObject(course, objectType, creator.getRobot().getPosition());
        newRRO.setLevel(creator.getRobot().getLevel());
        newRRO.setDirection(creator.getRobot().getDirection());
        newRRO.setPrevDirection(newRRO.getDirection());
    }

    public static CourseObject createCircuitChaosObject(Course course, ObjectType objectType, Position position) {
        CourseObject co = new CourseObject();
        co.setName(objectType.getName());
        co.setType(objectType);
        co.setPosition(position);
        co.setLevel(getFloor(course, position).getLevel());
        co.setPrevPosition(position);
        add(course, co);
        return co;
    }

    public static CourseObject createGlue(Course course, Position position) {
        CourseObject glue = new CourseObject();
        glue.setName(ObjectType.GLUE.getName());
        glue.setType(ObjectType.GLUE);
        glue.setFlying(false);
        glue.setPosition(position);
        glue.setLevel(getFloor(course, position).getLevel());
        glue.setPrevPosition(position);
        glue.setDirection(Direction.random());
        glue.setVariantSeed(RANDOM.nextDouble());
        glue.setGlue(CourseObject.GLUE_INITIAL_AMOUNT);
        glue.setActive(false);
        add(course, glue);
        return glue;
    }

    public static CourseObject createOil(Course course, Position position) {
        CourseObject oil = new CourseObject();
        oil.setName(ObjectType.OIL.getName());
        oil.setType(ObjectType.OIL);
        oil.setFlying(false);
        oil.setPosition(position);
        oil.setLevel(getFloor(course, position).getLevel());
        oil.setPrevPosition(position);
        oil.setDirection(Direction.random());
        oil.setVariantSeed(RANDOM.nextDouble());
        oil.setActive(false);
        add(course, oil);
        return oil;
    }

    public static void replaceGameCourse(Course newCourse, Course currentCourse) {
        currentCourse.getFloor().clear();
        currentCourse.getObjects().forEach(ce -> ce.setOnCourse(false));
        currentCourse.getObjects().clear();
        currentCourse.getCheckpoints().clear();
        currentCourse.getFloor().addAll(newCourse.getFloor());
        currentCourse.getCheckpoints().addAll(newCourse.getCheckpoints());
        currentCourse.getObjects().addAll(newCourse.getObjects());
        currentCourse.getObjects().forEach(ce -> ce.setOnCourse(true));
        currentCourse.setRange(newCourse.getRange());
    }

    public static List<CourseElementStub> createCourseElementStubList(Course course) {
        List<CourseElementStub> elements = new ArrayList<>();
        for (Floor floor : course.getFloor()) {
            if (floor.isWater() && !floor.getFloortype().isConveyorBelt()) {
                // ignore those facing directions, it slows down the process
                // see Game.randomlySetFloorOrientationForDisplay()
                elements.add(new CourseElementStub(floor.getStubListName(), floor.getPosition(), Direction.NORTH));
            } else {
                elements.add(new CourseElementStub(floor.getStubListName(), floor.getPosition(), floor.getFacingDirection()));
            }
        }
        for (CourseObject co : course.getObjects()) {
            if (co.isOnCourse()) {
                elements.add(new CourseElementStub(co.getName() + co.getType(), co.getPosition(), co.getDirection()));
            }
        }
        for (Robot robot : course.getRobots()) {
            if (robot.isOnCourse()) {
                elements.add(new CourseElementStub(robot.getName(), robot.getPosition(), robot.getDirection()));
            }
        }
        for (Checkpoint checkpoint : course.getCheckpoints()) {
            elements.add(new CourseElementStub(String.valueOf(checkpoint.getNumber()), null, null));
        }
        for (Robot robot : course.getRobotsFallingIntoAbyss()) {
            elements.add(new CourseElementStub(robot.getName(), robot.getPosition(), robot.getDirection()));
        }
        for (CourseObject co : course.getObjectsFallingIntoAbyss()) {
            elements.add(new CourseElementStub(co.getName(), co.getPosition(), co.getDirection()));
        }
        return elements.stream()
                       .sorted(Comparator.comparing(CourseElementStub::name)
                                         .thenComparingInt(a -> a.position() == null ? 0 : a.position().x())
                                         .thenComparingInt(a -> a.position() == null ? 0 : a.position().y())
                                         .thenComparing(CourseElementStub::direction))
                       .toList();
    }

    public static void setBeam(Course course, Position position, Direction direction, Step step) {
        Floor floor = getFloor(course, position);
        switch (direction) {
            case NORTH, SOUTH -> floor.setBeamsNS(true);
            case EAST, WEST -> floor.setBeamsWE(true);
        }
        floor.setBeamType(switch (step) {
            case ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE -> EXCHANGE_BEAM;
            case ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE -> SPIN_LEFT_BEAM;
            case ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE -> SPIN_RIGHT_BEAM;
            case ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE, COURSE_MOUNTED_PRESSURE_BEAMS_FIRE -> PRESSURE_BEAM;
            case ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE, COURSE_MOUNTED_TRACTOR_BEAMS_FIRE -> TRACTOR_BEAM;
            default -> MAIN_LASER;
        });
    }

    public static void clearBeams(Course course) {
        for (Floor f : course.getFloor()) {
            f.setBeamsNS(false);
            f.setBeamsWE(false);
        }
    }

}
