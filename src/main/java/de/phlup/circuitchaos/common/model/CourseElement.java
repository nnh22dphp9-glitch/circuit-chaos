package de.phlup.circuitchaos.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.common.enums.Direction;
import lombok.Data;

@Data
public abstract class CourseElement {

    private String      name;
    private Position    position      = new Position(0, 0);
    private Position    prevPosition  = new Position(0, 0);
    private int         level         = 0;
    private Direction   direction     = Direction.NORTH;
    private Direction   prevDirection = Direction.NORTH;
    private boolean     onCourse      = false;

    @JsonIgnore
    private int conflictingMark = 0;

    @JsonIgnore
    public abstract boolean isFlying();

}
