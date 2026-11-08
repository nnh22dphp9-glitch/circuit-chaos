package de.phlup.circuitchaos.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

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
