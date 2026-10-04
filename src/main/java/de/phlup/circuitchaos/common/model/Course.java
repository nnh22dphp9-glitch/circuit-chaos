package de.phlup.circuitchaos.common.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

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

    @NotNull
    private Range range = new Range(0, 0, 0, 0);

    @NotNull
    private CourseProperties properties = new CourseProperties();

}
