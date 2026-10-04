package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Step {
    SETUP("Program robots"),
    START_OF_TURN_BOARD_EFFECTS("'Start of turn' board effects"),
    OPEN_TRAPDOORS("Open trapdoor pits and activate robots"),
    RELEASE_DEVICES("Release devices"),
    ROBOTS_AND_OBJECTS_MOVE("Robots and objects move"),
    EXPRESS_CONVEYOR_BELTS_MOVE("Express conveyor belts move"),
    CONVEYOR_BELTS_MOVE("Conveyor belts move"),
    PUSHERS_PUSH("Pushers push"),
    GEARS_ROTATE("Gears rotate"),
    BOARD_MOUNTED_LASER_FIRE("Board mounted lasers fire"),
    BOARD_MOUNTED_PRESSURE_BEAMS_FIRE("Board mounted pressure beams fire"),
    BOARD_MOUNTED_TRACTOR_BEAMS_FIRE("Board mounted tractor beams fire"),
    ROBOT_MOUNTED_LASER_FIRE("Robot mounted lasers fire"),
    ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE("Robot mounted pressure beams fire"),
    ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE("Robot mounted tractor beams fire"),
    ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE("Robot mounted spin left beams fire"),
    ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE("Robot mounted spin right beams fire"),
    ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE("Robot mounted exchange beams fire"),
    TOUCH_CHECKPOINTS("Touch checkpoints"),
    END_OF_TURN_BOARD_EFFECTS("'End of turn' board effects"),
    GIVE_UP("Remove robot");

    private final String name;

}
