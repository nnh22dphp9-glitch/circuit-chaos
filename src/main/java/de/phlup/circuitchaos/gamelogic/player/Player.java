package de.phlup.circuitchaos.gamelogic.player;

import de.phlup.circuitchaos.enums.Direction;
import de.phlup.circuitchaos.enums.ModuleType;
import de.phlup.circuitchaos.gamelogic.Game;
import de.phlup.circuitchaos.gamelogic.GlobalServerAttributes;
import de.phlup.circuitchaos.gamelogic.utils.BoardHandler;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.Module;
import de.phlup.circuitchaos.model.Robot;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import static de.phlup.circuitchaos.enums.ModuleType.LAYERED_ARMOR;
import static de.phlup.circuitchaos.enums.ModuleType.POWER_DOWN_SHIELD;
import static de.phlup.circuitchaos.enums.ModuleType.SHIELD;

@Data
public abstract class Player {

    protected final Robot  robot;
    protected final String gameId;

    protected Player(@NotNull Robot robot, @NotNull String gameId) {
        this.robot = robot;
        this.gameId = gameId;
    }

    public boolean hasLost() {
        return robot.isGiveUp();
    }

    public boolean hasWon() {
        return !hasLost() && robot.getNextCheckpoint() > GlobalServerAttributes.getGame(gameId).getBoard().getCheckpoints().size() - 1;
    }

    protected void initTurn() {
        for (Module module : robot.getModules()) {
            module.setPowerDownShield(new boolean[][]{{true, true, true, true}, {true, true, true, true}, {true, true, true, true}, {true, true, true, true}, {true, true, true, true}});
            module.setAlreadyUsed(false);
            for (int cnt = 0; cnt < 5; cnt++) {
                if (module.getType() == POWER_DOWN_SHIELD) {
                    module.getActiveInPhase()[cnt] = robot.isPoweredDown();
                } else {
                    module.getActiveInPhase()[cnt] = module.getType().isDefaultActiveInPhase();
                }
            }
        }
    }

    public void takeDamage(Integer phase, Player source, Direction direction, boolean considerShields) {
        Game game = GlobalServerAttributes.getGame(gameId);
        if (robot.isOnBoard() && (source == null || !robot.isVirtual())) {
            boolean takesDamage = true;
            if (considerShields) {
                if (uses(POWER_DOWN_SHIELD, phase)) {
                    Module module = robot.getModule(POWER_DOWN_SHIELD);
                    if (module.getActiveInPhase()[phase] && (module.getPowerDownShield()[phase][0]
                            || module.getPowerDownShield()[phase][1]
                            || module.getPowerDownShield()[phase][2]
                            || module.getPowerDownShield()[phase][3])) {
                        if (direction != null) {
                            if (module.getPowerDownShield()[phase][direction.reverse().ordinal()]) {
                                module.getPowerDownShield()[phase][direction.reverse().ordinal()] = false;
                                takesDamage = false;
                            }
                        }
                    }
                }
                if (direction != null) {
                    if (uses(SHIELD, phase)) {
                        Module module = robot.getModule(SHIELD);
                        if (module.getActiveInPhase()[phase] && module.getDirection() == direction.reverse()) {
                            module.getActiveInPhase()[phase] = false;
                            takesDamage = false;
                        }
                    }
                }
                Module module = robot.getModule(LAYERED_ARMOR);
                if (module != null && module.getAmmunition() > 0) {
                    module.setAmmunition(module.getAmmunition() - 1);
                    takesDamage = false;
                }
            }
            if (takesDamage) {
                takeNormalDamage(game.getBoard());
            }
        }
    }

    public void takeNormalDamage(Board board) {
        if (robot.isOnBoard()) {
            robot.setDamage(robot.getDamage() + 1);
            if (robot.getDamage() - countBlockedRegisters() > 4) {
                blockRegister();
            }
            if (robot.isOnBoard()) {
                if (robot.getDamage() >= 10) {
                    BoardHandler.remove(board, this.robot);
                }
            }
        }
    }

    public boolean uses(ModuleType moduleType, Integer phase) {
        if (phase == null || phase < 0) {
            return false;
        }
        boolean erg    = false;
        Module  module = robot.getModule(moduleType);
        if (module != null) {
            erg = module.getActiveInPhase()[phase];
        }
        return erg;
    }

    protected int countBlockedRegisters() {
        int erg = 0;
        for (int i = 0; i < 5; i++) {
            if (robot.getBlocked()[i]) {
                erg++;
            }
        }
        return erg;
    }

    private void blockRegister() {
        int index = 4;
        while (robot.getBlocked()[index]) {
            index--;
            if (index == -1) {
                break;
            }
        }
        if (index > -1) {
            robot.getBlocked()[index] = true;
        }
    }

    public void arriveAtPitStop() {
        Game game = GlobalServerAttributes.getGame(gameId);
        game.addNewModuleToRobot(robot);
        if (robot.getDamage() > 0) {
            robot.setDamage(robot.getDamage() - 1);
        }
    }

}
