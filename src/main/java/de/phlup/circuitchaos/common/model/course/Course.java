package de.phlup.circuitchaos.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.common.records.Range;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class Course {

    private final List<Floor>        floor       = new ArrayList<>();
    private final List<CourseObject> objects     = new ArrayList<>();
    private final List<Robot>        robots      = new ArrayList<>();
    private final List<Checkpoint>   checkpoints = new ArrayList<>();

    private final List<CourseObject> objectsFallingIntoAbyss = new ArrayList<>();
    private final List<Robot>        robotsFallingIntoAbyss  = new ArrayList<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean considerGui = false;

    @NotNull
    private Range range = new Range(0, 0, 0, 0);

    @NotNull
    private CourseProperties properties = new CourseProperties();

    @NotNull
    @JsonIgnore
    public Course copyForSimulation() {
        Course course = new Course();
        course.getFloor().addAll(floor.stream().map(Floor::copyForSimulation).toList());
        course.getObjects().addAll(objects.stream().map(CourseObject::copyForSimulation).toList());
        course.getRobots().addAll(robots.stream().map(Robot::copyForSimulation).toList());
        course.getCheckpoints().addAll(checkpoints.stream().map(Checkpoint::copyForSimulation).toList());
        course.setRange(range);
        course.setProperties(properties.copyForSimulation());
        course.considerGui = false;
        return course;
    }

}
