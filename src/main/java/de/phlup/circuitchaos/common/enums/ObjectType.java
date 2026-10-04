package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ObjectType {

    GLUE("Glue", true, false, false, 0),
    MINE("Mine", true, false, false, 4),
    OIL("Oil", true, false, false, 0),
    PORTAL_BLUE("Blue Portal", true, false, false, 0),
    PORTAL_PURPLE("Purple Portal", true, false, false, 0),
    PORTAL_RED("Red Portal", true, false, false, 0),
    PORTAL_YELLOW("Yellow Portal", true, false, false, 0),
    PORTAL_GREEN("Green Portal", true, false, false, 0),
    PROXIMITY_MINE("Proximity Mine", true, false, false, 4),
    RANDOMIZER("Randomizer", true, false, false, 0),
    TELEPORTER("Teleporter", true, false, false, 0),
    ;

    private final String  name;
    private final boolean flat;
    private final boolean programmable;
    private final boolean flying;
    private final int     initialDamage;

    public boolean isPortal() {
        return this == PORTAL_RED || this == PORTAL_BLUE || this == PORTAL_YELLOW || this == PORTAL_PURPLE || this == PORTAL_GREEN;
    }

}
