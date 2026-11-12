package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.Module;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Programme;
import de.phlup.circuitchaos.server.game.GameLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Selects the weapon to use in one program phase.
 *
 * Beam weapons are compared by running the real beam-resolution code on
 * independent copies of the current simulated course. Ramming Armor must be
 * selected before movement, so its selection is based on the planned movement
 * path instead.
 */
final class WeaponEvaluator {

    private static final double DAMAGE_VALUE = 100.0;
    private static final double DEATH_VALUE = 1_000.0;
    private static final double OWN_DAMAGE_COST = 150.0;
    private static final double OWN_DEATH_COST = 10_000.0;

    private static final List<ModuleType> BEAM_WEAPONS = List.of(
            ModuleType.MAIN_LASER,
            ModuleType.DOUBLE_BARREL_LASER,
            ModuleType.HIGH_POWER_LASER,
            ModuleType.REAR_LASER,
            ModuleType.EXCHANGE_BEAM,
            ModuleType.PRESSURE_BEAM,
            ModuleType.TRACTOR_BEAM,
            ModuleType.SPIN_LEFT_BEAM,
            ModuleType.SPIN_RIGHT_BEAM
    );

    private WeaponEvaluator() {
    }

    /**
     * Ramming Armor has to be active during movement. If another robot lies
     * directly on the planned movement path, it takes precedence over a beam
     * weapon for this phase.
     *
     * @return RAMMING_ARMOR if it should be selected, otherwise null
     */
    static ModuleType selectRammingArmor(Course course, Robot robot, int phase, Programme program) {
        if (!robot.hasModule(ModuleType.RAMMING_ARMOR)) {
            return null;
        }

        boolean hasBeamWeapon = BEAM_WEAPONS.stream().anyMatch(robot::hasModule);
        if (!hasBeamWeapon) {
            return ModuleType.RAMMING_ARMOR;
        }

        if (program == null) {
            return null;
        }

        int movement = CourseHandler.determineMovementAmount(program, robot.uses(ModuleType.BRAKES, phase));
        if (movement == 0) {
            return null;
        }

        Direction direction = movement > 0 ? robot.getDirection() : robot.getDirection().reverse();
        Position position = robot.getPosition();
        for (int step = 0; step < Math.abs(movement); step++) {
            position = position.neighbour(direction);
            for (Robot other : course.getRobots()) {
                if (other != robot && other.isOnCourse()
                        && other.getLevel() == robot.getLevel()
                        && Objects.equals(other.getPosition(), position)) {
                    return ModuleType.RAMMING_ARMOR;
                }
            }
        }
        return null;
    }

    /**
     * Chooses one owned beam weapon by comparing the actual simulated outcome
     * of each candidate. The fallback is the first owned beam weapon, ensuring
     * that a robot with a beam weapon never deliberately leaves the phase
     * without a weapon selected.
     */
    static ModuleType selectBeamWeapon(GameLogic currentGame, Robot robot, int phase) {
        List<ModuleType> available = BEAM_WEAPONS.stream()
                .filter(robot::hasModule)
                .toList();
        if (available.isEmpty()) {
            return robot.hasModule(ModuleType.RAMMING_ARMOR) ? ModuleType.RAMMING_ARMOR : null;
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        ModuleType bestWeapon = available.get(0);
        for (ModuleType candidate : available) {
            double score = evaluateWeapon(currentGame, robot, phase, candidate);
            if (score > bestScore) {
                bestScore = score;
                bestWeapon = candidate;
            }
        }
        return bestWeapon;
    }

    private static double evaluateWeapon(GameLogic currentGame, Robot robot, int phase, ModuleType weapon) {
        Course simulation = currentGame.getCourse().copyForSimulation();
        Robot simulatedRobot = findRobot(simulation, robot.getId());
        if (simulatedRobot == null) {
            return Double.NEGATIVE_INFINITY;
        }

        // Robot.copyForSimulation intentionally shares read-only modules.
        // The weapon activation flags are changed here, so this simulation
        // needs private module instances for the selected robot.
        simulatedRobot.setModules(simulatedRobot.getModules().stream()
                .map(Module::copyForSimulation)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));

        for (Module module : simulatedRobot.getModules()) {
            if (module.getType().isWeapon()) {
                module.getActiveInPhase()[phase] = module.getType() == weapon;
            }
        }

        GameLogic simulationLogic = new GameLogic(simulation, (step, currentPhase, detail, movingRobotName) -> {
        });
        simulationLogic.stepResolveBeamsAndLaserFire(phase, simulatedRobot);
        return evaluateOutcome(simulation, robot.getId(), currentGame.getCourse());
    }

    private static double evaluateOutcome(Course result, String ownRobotId, Course initialCourse) {
        Robot ownBefore = findRobot(initialCourse, ownRobotId);
        Robot ownAfter = findRobot(result, ownRobotId);
        if (ownBefore == null || ownAfter == null) {
            return -OWN_DEATH_COST;
        }

        double score = (ownBefore.getDamage() - ownAfter.getDamage()) * OWN_DAMAGE_COST;
        if (!ownAfter.isOnCourse() || ownAfter.getDieCounter() > ownBefore.getDieCounter()) {
            score -= OWN_DEATH_COST;
        }

        for (Robot before : initialCourse.getRobots()) {
            if (Objects.equals(before.getId(), ownRobotId)) {
                continue;
            }
            Robot after = findRobot(result, before.getId());
            if (after == null) {
                continue;
            }

            score += (after.getDamage() - before.getDamage()) * DAMAGE_VALUE;
            if (after.getDieCounter() > before.getDieCounter() || !after.isOnCourse()) {
                score += DEATH_VALUE;
            }

            // A pull that brings an opponent closer can create an opportunity
            // in a later phase. A push that opens distance has the opposite
            // (smaller) value. Damage and elimination remain dominant.
            int oldDistance = manhattanDistance(ownBefore, before);
            int newDistance = manhattanDistance(ownAfter, after);
            score += (oldDistance - newDistance) * 2.0;

            if (before.getDirection() != after.getDirection()) {
                boolean wasFacingUs = isFacing(before, ownBefore);
                boolean nowFacingUs = isFacing(after, ownAfter);
                if (wasFacingUs && !nowFacingUs) {
                    score += 8.0;
                } else if (!wasFacingUs && nowFacingUs) {
                    score -= 8.0;
                }
            }
        }
        return score;
    }

    private static Robot findRobot(Course course, String id) {
        return course.getRobots().stream()
                .filter(robot -> Objects.equals(robot.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private static int manhattanDistance(Robot first, Robot second) {
        return Math.abs(first.getPosition().x() - second.getPosition().x())
                + Math.abs(first.getPosition().y() - second.getPosition().y());
    }

    private static boolean isFacing(Robot robot, Robot target) {
        return switch (robot.getDirection()) {
            case NORTH -> target.getPosition().y() < robot.getPosition().y();
            case EAST -> target.getPosition().x() > robot.getPosition().x();
            case SOUTH -> target.getPosition().y() > robot.getPosition().y();
            case WEST -> target.getPosition().x() < robot.getPosition().x();
        };
    }

}
