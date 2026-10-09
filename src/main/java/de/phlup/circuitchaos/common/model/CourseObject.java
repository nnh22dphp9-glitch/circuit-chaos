package de.phlup.circuitchaos.common.model;

import de.phlup.circuitchaos.common.enums.ObjectType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CourseObject extends CourseElement {

    public final static int GLUE_INITIAL_AMOUNT = 3;

    private ObjectType type;
    private Position   targetPosition;
    private int        glue        = 0;
    private double     variantSeed = 0;
    private boolean    active      = false;

}
