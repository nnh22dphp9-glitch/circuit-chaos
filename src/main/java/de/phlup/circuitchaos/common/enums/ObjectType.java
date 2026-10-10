package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ObjectType {

    OIL("Oil"),
    GLUE("Glue"),
    MINE("Mine"),
    PROXIMITY_MINE("Proximity Mine"),
    RANDOMIZER("Randomizer"),
    TELEPORTER("Teleporter"),
    PORTAL_BLUE("Blue Portal"),
    PORTAL_PURPLE("Purple Portal"),
    PORTAL_RED("Red Portal"),
    PORTAL_YELLOW("Yellow Portal"),
    PORTAL_GREEN("Green Portal"),
    ;

    private final String name;

    public boolean isPortal() {
        return this == PORTAL_RED || this == PORTAL_BLUE || this == PORTAL_YELLOW || this == PORTAL_PURPLE || this == PORTAL_GREEN;
    }

}
