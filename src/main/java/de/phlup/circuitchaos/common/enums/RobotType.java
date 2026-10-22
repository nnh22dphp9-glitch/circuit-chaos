package de.phlup.circuitchaos.common.enums;

import de.phlup.circuitchaos.server.GlobalServerAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public enum RobotType {

    ROBOT_1("Inferno", "robot-1"),
    ROBOT_2("Volt", "robot-2"),
    ROBOT_3("Sunstrike", "robot-3"),
    ROBOT_4("Venom", "robot-4"),
    ROBOT_5("Nightshade", "robot-5"),
    ROBOT_6("Pumpkin", "robot-6"),
    ROBOT_7("Hydro", "robot-7"),
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
