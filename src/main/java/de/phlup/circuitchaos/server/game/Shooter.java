package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Module;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Robot;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.PRIVATE)
public class Shooter {

    private Robot robot;
    private Robot target;
    private Robot  target2;
    private Module weapon;

    public Shooter(int phase, Robot robot, Game game, Step step) {
        this.robot = robot;
        Position  position = robot.getPosition();
        int       level    = robot.getLevel();
        Direction dir      = robot.getDirection();

        target = null;
        target2 = null;

        if (step == Step.ROBOT_MOUNTED_LASER_FIRE) {
            if (robot.uses(ModuleType.MAIN_LASER, phase)) {
                target = game.getLaserTarget(position, dir, level, false, step);
                weapon = robot.getModule(ModuleType.MAIN_LASER);
            } else if (robot.uses(ModuleType.HIGH_POWER_LASER, phase)) {
                target = game.getLaserTarget(position, dir, level, false, step);
                target2 = game.getLaserTarget(position, dir, level, true, step);
                weapon = robot.getModule(ModuleType.HIGH_POWER_LASER);
            } else if (robot.uses(ModuleType.DOUBLE_BARREL_LASER, phase)) {
                target = game.getLaserTarget(position, dir, level, false, step);
                weapon = robot.getModule(ModuleType.DOUBLE_BARREL_LASER);
            } else if (robot.uses(ModuleType.REAR_LASER, phase)) {
                target = game.getLaserTarget(position, dir.reverse(), level, false, step);
                weapon = robot.getModule(ModuleType.REAR_LASER);
            }
        } else if (step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && robot.uses(ModuleType.EXCHANGE_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = robot.getModule(ModuleType.EXCHANGE_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && robot.uses(ModuleType.SPIN_LEFT_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = robot.getModule(ModuleType.SPIN_LEFT_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && robot.uses(ModuleType.SPIN_RIGHT_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = robot.getModule(ModuleType.SPIN_RIGHT_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && robot.uses(ModuleType.PRESSURE_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = robot.getModule(ModuleType.PRESSURE_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && robot.uses(ModuleType.TRACTOR_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = robot.getModule(ModuleType.TRACTOR_BEAM);
        }
    }

}
