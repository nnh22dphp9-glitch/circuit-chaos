package de.phlup.circuitchaos.enums;

import de.phlup.circuitchaos.gamelogic.GlobalServerAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public enum RobotType {

    // Blaze, Redline, Crimson, Riot, Hotshot, Scarlet, Ember, Torque
    ROBOT_1("Inferno", "robot-1"),
    // Azure, Frost, Blizzard, Bluebolt, Neon, Cobalt, Icebreaker, Skyfire
    ROBOT_2("Volt", "robot-2"),
    // Sparky, Lemon, Goldrush, Flash, Hazard, Bumble, Voltage, Brightside
    ROBOT_3("Sunstrike", "robot-3"),
    // Toxic, Emerald, Jade, Moss, Gremlin, Acid, Green Machine, Overgrowth
    ROBOT_4("Venom", "robot-4"),
    // Violet, Vortex, Amethyst, Grape, Purple Haze, Ultraviolet, Nebula, Raspberry
    ROBOT_5("Nightshade", "robot-5"),
    // Blaze, Rusty, Inferno, Cinder, Flare, Tangerine, Fireball, Burnout
    ROBOT_6("Pumpkin", "robot-6"),
    // Aqua, Tide, Lagoon, Frostbyte, Arctic, Teal, Wave, Glacier
    ROBOT_7("Hydro", "robot-7"),
    // Ghost, Snow, Ivory, Frost, Specter, Whiteout, Chrome, Zero
    ROBOT_8("Phantom", "robot-8");

    private final String robotName;
    private final String normalizedName;

    public static List<RobotType> mixedValues() {
        List<RobotType> output = new ArrayList<>();
        for (RobotType type : values()) {
            output.add(GlobalServerAttributes.RANDOM.nextInt(output.size() + 1), type);
        }
        return output;
    }

}
