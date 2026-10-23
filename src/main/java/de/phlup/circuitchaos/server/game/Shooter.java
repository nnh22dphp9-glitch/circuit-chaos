package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Module;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Robot;
import de.phlup.circuitchaos.server.player.Player;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.PRIVATE)
public class Shooter {

    private Player player;
    private Robot  target;
    private Robot  target2;
    private Module weapon;

    public Shooter(int phase, Player player, Game game, Step step) {
        this.player = player;
        Robot     robot    = this.getPlayer().getRobot();
        Position  position = robot.getPosition();
        int       level    = robot.getLevel();
        Direction dir      = robot.getDirection();

        target = null;
        target2 = null;

        if (step == Step.ROBOT_MOUNTED_LASER_FIRE) {
            if (player.uses(ModuleType.MAIN_LASER, phase)) {
                target = game.getLaserTarget(position, dir, level, false, step);
                weapon = player.getRobot().getModule(ModuleType.MAIN_LASER);
            } else if (player.uses(ModuleType.HIGH_POWER_LASER, phase)) {
                target = game.getLaserTarget(position, dir, level, false, step);
                target2 = game.getLaserTarget(position, dir, level, true, step);
                weapon = player.getRobot().getModule(ModuleType.HIGH_POWER_LASER);
            } else if (player.uses(ModuleType.DOUBLE_BARREL_LASER, phase)) {
                target = game.getLaserTarget(position, dir, level, false, step);
                weapon = player.getRobot().getModule(ModuleType.DOUBLE_BARREL_LASER);
            } else if (player.uses(ModuleType.REAR_LASER, phase)) {
                target = game.getLaserTarget(position, dir.reverse(), level, false, step);
                weapon = player.getRobot().getModule(ModuleType.REAR_LASER);
            }
        } else if (step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && player.uses(ModuleType.EXCHANGE_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = player.getRobot().getModule(ModuleType.EXCHANGE_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && player.uses(ModuleType.SPIN_LEFT_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = player.getRobot().getModule(ModuleType.SPIN_LEFT_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && player.uses(ModuleType.SPIN_RIGHT_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = player.getRobot().getModule(ModuleType.SPIN_RIGHT_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && player.uses(ModuleType.PRESSURE_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = player.getRobot().getModule(ModuleType.PRESSURE_BEAM);
        } else if (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && player.uses(ModuleType.TRACTOR_BEAM, phase)) {
            target = game.getLaserTarget(position, dir, level, false, step);
            weapon = player.getRobot().getModule(ModuleType.TRACTOR_BEAM);
        }
    }

}
