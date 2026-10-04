package de.phlup.circuitchaos.common.model;

import lombok.Data;

@Data
public class Checkpoint {

    private Position position = new Position(0, 0);
    private int      number   = 0;

}
