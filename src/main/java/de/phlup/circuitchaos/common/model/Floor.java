package de.phlup.circuitchaos.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.WallType;
import lombok.Data;

import static de.phlup.circuitchaos.common.enums.ModuleType.MAIN_LASER;

@Data
public class Floor {

    private Position position = new Position(0, 0);

    private Floortype floortype       = Floortype.OPEN_FLOOR;
    private boolean[] activeInPhase   = new boolean[]{false, false, false, false, false};
    private Direction facingDirection = Direction.NORTH;
    private boolean   water           = false;
    private int[]     lasers          = new int[]{0, 0, 0, 0};
    private boolean[] pressureBeam    = new boolean[]{false, false, false, false};
    private boolean[] tractorBeam     = new boolean[]{false, false, false, false};
    private boolean   hasPusher       = false;
    private Direction pusherDirection = Direction.NORTH;
    private int       level           = 0;
    private int       distanceCounter = 0;
    private WallType  wallNorth       = WallType.NONE;
    private WallType  wallEast        = WallType.NONE;
    private WallType  wallSouth       = WallType.NONE;
    private WallType  wallWest        = WallType.NONE;

    // for drawing only
    private int        explosiveDamage              = 0;
    private boolean    beamsNS                      = false; // robot mounted
    private boolean    beamsWE                      = false; // robot mounted
    private ModuleType beamType                     = MAIN_LASER; // robot mounted
    private int        courseMountedLaserBeamsNS    = 0; // currently firing laser beams
    private int        courseMountedLaserBeamsWE    = 0; // currently firing laser beams
    private boolean    courseMountedPressureBeamsNS = false; // currently firing pressure beams
    private boolean    courseMountedPressureBeamsWE = false; // currently firing pressure beams
    private boolean    courseMountedTractorBeamsNS  = false; // currently firing tractor beams
    private boolean    courseMountedTractorBeamsWE  = false; // currently firing tractor beams

    @JsonIgnore
    public WallType wall(Direction direction) {
        return switch (direction) {
            case NORTH -> getWallNorth();
            case EAST -> getWallEast();
            case SOUTH -> getWallSouth();
            case WEST -> getWallWest();
        };
    }

    @JsonIgnore
    public String getStubListName() {
        return floortype.toString() + water +
                activeInPhase[0] + activeInPhase[1] + activeInPhase[2] +
                activeInPhase[3] + activeInPhase[4] +
                lasers[0] + lasers[1] + lasers[2] + lasers[3] +
                pressureBeam[0] + pressureBeam[1] + pressureBeam[2] + pressureBeam[3] +
                tractorBeam[0] + tractorBeam[1] + tractorBeam[2] + tractorBeam[3] +
                explosiveDamage + hasPusher + pusherDirection + level +
                wallNorth + wallEast + wallWest + wallSouth;
    }

}
