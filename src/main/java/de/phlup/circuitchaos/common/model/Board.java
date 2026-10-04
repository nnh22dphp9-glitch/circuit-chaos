package de.phlup.circuitchaos.common.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Board {

    private final List<Floor>              factoryFloor = new ArrayList<>();
    private final List<CircuitChaosObject> objects      = new ArrayList<>();
    private final List<Robot>              robots       = new ArrayList<>();
    private final List<Checkpoint>         checkpoints  = new ArrayList<>();

    private final List<CircuitChaosObject> objectsFallingIntoAbyss = new ArrayList<>();
    private final List<Robot>              robotsFallingIntoAbyss  = new ArrayList<>();

    @NotNull
    private Range range = new Range(0, 0, 0, 0);

    @NotNull
    private BoardProperties properties = new BoardProperties();

}
