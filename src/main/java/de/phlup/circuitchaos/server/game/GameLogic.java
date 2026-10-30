package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Programme;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GameLogic {

    public static int determineMovementAmount(Programme program, boolean usesBrakes) {
        int movement = program.getType().getMovement();
        if (movement != 0 && usesBrakes) {
            movement = movement > 0 ? movement - 1 : movement + 1;
        }
        return movement;
    }

    public static boolean cannotMoveAcross(Floor sourceFloor, Floor targetFloor, Direction direction, boolean flying) {
        if (flying) {
            return false;
        }
        WallType sourceWall = sourceFloor.wall(direction);
        WallType targetWall = targetFloor.wall(direction.reverse());
        boolean rampPassage = sourceWall == WallType.RAMP_UP || sourceWall == WallType.RAMP_DOWN
                || targetWall == WallType.RAMP_UP || targetWall == WallType.RAMP_DOWN;
        if (targetFloor.getLevel() > sourceFloor.getLevel() && !rampPassage) {
            return true;
        }
        return ((sourceWall != WallType.NONE
                && sourceWall != WallType.ONE_WAY_GREEN
                && sourceWall != WallType.RAMP_UP
                && sourceWall != WallType.RAMP_DOWN)
                || (targetWall != WallType.NONE
                && targetWall != WallType.ONE_WAY_GREEN
                && targetWall != WallType.RAMP_UP
                && targetWall != WallType.RAMP_DOWN
                && targetWall != WallType.LEDGE))
                && (sourceWall != WallType.ONE_WAY_GREEN || targetWall != WallType.ONE_WAY_RED);
    }

}
