package de.phlup.circuitchaos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ComputerType {

    NONE("none"),
    NORMAL("normal"),
    AGGRESSIVE("aggressive"),
    STUPID("stupid"),
    ;

    private final String name;

    public static ComputerType getByName(String name) {
        for (ComputerType ct : values()) {
            if (ct.getName().equals(name)) {
                return ct;
            }
        }
        return null;
    }

}
