package de.phlup.circuitchaos.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.common.records.Position;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseProperties {

    private boolean lasers;
    private boolean pressureBeams;
    private boolean tractorBeams;
    private boolean pushers;
    private boolean gears;
    private boolean conveyorBelts;
    private boolean expressConveyorBelts;
    private boolean trapdoor;

    // for performance optimization: pre-calculated positions
    private final List<Position> laserPositions        = new ArrayList<>();
    private final List<Position> pressureBeamPositions = new ArrayList<>();
    private final List<Position> tractorBeamPositions  = new ArrayList<>();

    @JsonIgnore
    public CourseProperties copyForSimulation() {
        CourseProperties courseProperties = new CourseProperties();
        courseProperties.setLasers(lasers);
        courseProperties.setPressureBeams(pressureBeams);
        courseProperties.setTractorBeams(tractorBeams);
        courseProperties.setPushers(pushers);
        courseProperties.setGears(gears);
        courseProperties.setConveyorBelts(conveyorBelts);
        courseProperties.setExpressConveyorBelts(expressConveyorBelts);
        courseProperties.setTrapdoor(trapdoor);
        return courseProperties;
    }

}
