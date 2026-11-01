package de.phlup.circuitchaos.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.common.records.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {

    private Position position = new Position(0, 0);
    private int      number   = 0;

    @JsonIgnore
    public Checkpoint copy() {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setPosition(position);
        checkpoint.setNumber(number);
        return checkpoint;
    }

}
