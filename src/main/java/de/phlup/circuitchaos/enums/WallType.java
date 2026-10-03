package de.phlup.circuitchaos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WallType {

    NONE,
    SOLID,
    ONE_WAY_GREEN,
    ONE_WAY_RED,
    REPULSOR_FIELD,
    LEDGE,
    RAMP_UP,
    RAMP_DOWN,

}
