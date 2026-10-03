package de.phlup.circuitchaos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.enums.Direction;
import lombok.Data;

@Data
public abstract class BoardElement {

    private String      name;
    private Position    position      = new Position(0, 0);
    private Position    prevPosition  = new Position(0, 0);
    private int         level         = 0;
    private boolean     flying        = false;
    private Direction   direction     = Direction.NORTH;
    private Direction   prevDirection = Direction.NORTH;
    private Programme[] program       = new Programme[5];
    private boolean     onBoard       = false;

    @JsonIgnore
    private int conflictingMark = 0;

}
