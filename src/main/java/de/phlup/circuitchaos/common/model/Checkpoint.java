package de.phlup.circuitchaos.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {

    private Position position = new Position(0, 0);
    private int      number   = 0;

}
