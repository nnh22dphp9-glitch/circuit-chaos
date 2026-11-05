package de.phlup.circuitchaos.common.records;

import de.phlup.circuitchaos.common.enums.ProgramType;

import java.util.ArrayList;
import java.util.List;

import static de.phlup.circuitchaos.server.GlobalServerAttributes.RANDOM;

public record Programme(int priority, ProgramType type) {

    public static Programme create(boolean overdrive, boolean reverseDrive) {
        ProgramType type = getRandomType(overdrive, reverseDrive);
        return new Programme(type.isMovement() ? RANDOM.nextInt(10000) : 0, type);
    }

    private static ProgramType getRandomType(boolean overdrive, boolean reverseDrive) {
        List<ProgramType> list = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            list.add(ProgramType.U_TURN);
            list.add(ProgramType.BACKUP);
            list.add(ProgramType.MOVE_3);
        }
        for (int i = 0; i < 4; i++) {
            list.add(ProgramType.MOVE_2);
        }
        for (int i = 0; i < 6; i++) {
            list.add(ProgramType.ROTATE_LEFT);
            list.add(ProgramType.ROTATE_RIGHT);
            list.add(ProgramType.MOVE_1);
        }
        if (overdrive) {
            list.add(ProgramType.MOVE_4);
        }
        if (reverseDrive) {
            list.add(ProgramType.BACKUP_2);
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

}
