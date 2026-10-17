package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Goal {

    FINISH("reach finish first"),
    KILL_ALL("kill all other robots");

    private final String name;

    public String toString() {
        return name;
    }

}
