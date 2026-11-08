package de.phlup.circuitchaos.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.records.Position;
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

    @JsonIgnore
    public CourseObject copyForSimulation() {
        CourseObject object = copyForSimulation(new CourseObject());
        object.setType(type);
        object.setTargetPosition(targetPosition);
        object.setGlue(glue);
        object.setVariantSeed(variantSeed);
        object.setActive(active);
        return object;
    }

}
