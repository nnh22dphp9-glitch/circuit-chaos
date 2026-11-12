package de.phlup.circuitchaos.common.model.course;

import de.phlup.circuitchaos.common.records.Position;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

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
    private final Set<Position> laserPositions        = new HashSet<>();
    private final Set<Position> pressureBeamPositions = new HashSet<>();
    private final Set<Position> tractorBeamPositions  = new HashSet<>();

}
