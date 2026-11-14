package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.course.Module;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.Position;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.PRIVATE)
public class Shooter {

    private Robot  robot;
    private Robot  target;
    private Robot  target2;
    private Module weapon;

    public Shooter(int phase, Robot robot, GameLogic game, Step step) {
        this.robot = robot;
        Position  position = robot.getPosition();
        int       level    = robot.getLevel();
        Direction dir      = robot.getDirection();

        target = null;
        target2 = null;
        weapon = null;
        ModuleType weaponType = robot.getWeapon(phase);

        if (step == Step.ROBOT_MOUNTED_LASER_FIRE) {
            if (weaponType == ModuleType.MAIN_LASER || weaponType == ModuleType.DOUBLE_BARREL_LASER) {
                target = game.getLaserTarget(position, dir, level, false, 1, false, step);
                weapon = robot.getModule(weaponType);
            } else if (weaponType == ModuleType.HIGH_POWER_LASER) {
                target = game.getLaserTarget(position, dir, level, false, 1, false, step);
                target2 = game.getLaserTarget(position, dir, level, true, 1, false, step);
                weapon = robot.getModule(weaponType);
            } else if (weaponType == ModuleType.REAR_LASER) {
                target = game.getLaserTarget(position, dir.reverse(), level, false, 1, false, step);
                weapon = robot.getModule(weaponType);
            }
        } else if ((step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && weaponType == ModuleType.EXCHANGE_BEAM)
                || (step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && weaponType == ModuleType.SPIN_LEFT_BEAM)
                || (step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && weaponType == ModuleType.SPIN_RIGHT_BEAM)
                || (step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && weaponType == ModuleType.PRESSURE_BEAM)
                || (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && weaponType == ModuleType.TRACTOR_BEAM)) {
            target = game.getLaserTarget(position, dir, level, false, 1, false, step);
            weapon = robot.getModule(weaponType);
        }
    }

}
