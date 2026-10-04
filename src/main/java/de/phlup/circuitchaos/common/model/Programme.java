package de.phlup.circuitchaos.common.model;

import de.phlup.circuitchaos.common.enums.ProgramType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Programme {

    private static final SecureRandom RANDOM = new SecureRandom();

    private int         priority;
    private ProgramType type;

    public static Programme create(boolean overdrive, boolean reverseDrive) {
        Programme programme = new Programme();
        programme.setType(getRandomType(overdrive, reverseDrive));
        if (programme.getType().isMovement()) {
            programme.setPriority(RANDOM.nextInt(10000));
        } else {
            programme.setPriority(0);
        }
        return programme;
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
