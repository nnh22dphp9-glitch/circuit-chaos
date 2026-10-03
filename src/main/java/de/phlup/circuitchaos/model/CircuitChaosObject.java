package de.phlup.circuitchaos.model;

import de.phlup.circuitchaos.enums.ObjectType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CircuitChaosObject extends BoardElement {

    private ObjectType type;
    private Position   targetPosition;
    private int        glue        = 0;
    private double     variantSeed = 0;
    private boolean    active      = false;

}
