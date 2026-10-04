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
    private boolean    beamsNS                      = false;
    private boolean    beamsWE                      = false;
    private ModuleType beamType                     = MAIN_LASER; // defining the beam type
    private int        courseMountedLaserBeamsNS    = 0; // laser beams in preview
    private int        courseMountedLaserBeamsWE    = 0; // laser beams in preview
    private boolean    courseMountedPressureBeamsNS = false; // pressure beams in preview
    private boolean    courseMountedPressureBeamsWE = false; // pressure beams in preview
    private boolean    courseMountedTractorBeamsNS  = false; // tractor beams in preview
    private boolean    courseMountedTractorBeamsWE  = false; // tractor beams in preview

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
