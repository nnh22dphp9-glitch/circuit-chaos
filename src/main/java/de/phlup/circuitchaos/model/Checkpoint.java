package de.phlup.circuitchaos.model;

import lombok.Data;

@Data
public class Checkpoint {

    private Position position = new Position(0, 0);
    private int      number   = 0;

}
