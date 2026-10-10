package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ObjectType {

    OIL("Oil", 0),
    GLUE("Glue", 0),
    MINE("Mine", 4),
    PROXIMITY_MINE("Proximity Mine", 4),
    RANDOMIZER("Randomizer", 0),
    TELEPORTER("Teleporter", 0),
    PORTAL_BLUE("Blue Portal", 0),
    PORTAL_PURPLE("Purple Portal", 0),
    PORTAL_RED("Red Portal", 0),
    PORTAL_YELLOW("Yellow Portal", 0),
    PORTAL_GREEN("Green Portal", 0),
    ;

    private final String name;
    private final int    initialDamage; // only 4 on MINE and PROXIMITY_MINE

    public boolean isPortal() {
        return this == PORTAL_RED || this == PORTAL_BLUE || this == PORTAL_YELLOW || this == PORTAL_PURPLE || this == PORTAL_GREEN;
    }

}
