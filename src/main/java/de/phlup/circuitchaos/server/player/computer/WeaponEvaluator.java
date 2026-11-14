package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.Goal;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.Floor;
import de.phlup.circuitchaos.common.model.course.Module;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Programme;
import de.phlup.circuitchaos.server.game.GameLogic;

import java.util.List;
import java.util.Objects;

/**
 * Selects the weapon to use in one program phase.
 * <p>
 * Beam target positions are obtained from GameLogic, so range, line of sight,
 * walls and level differences use the same rules as the actual game. Ramming
 * Armor must be selected before movement and is therefore evaluated separately.
 */
final class WeaponEvaluator {

    private static final double DAMAGE_VALUE               = 100.0;
    private static final double KILL_ALL_ELIMINATION_VALUE = 1_000.0;

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
     * Evaluates all owned beam weapons using the engine's actual beam-target
     * lookup. Only three ray lookups are needed: forward, the second target for
     * High Power Laser, and backward for Rear Laser.
     */
    static ModuleType selectBeamWeapon(GameLogic game, Robot robot, int phase, Goal goal) {
        List<ModuleType> available = BEAM_WEAPONS.stream()
                                                 .filter(robot::hasModule)
                                                 .toList();
        if (available.isEmpty()) {
            return null;
        }

        Direction direction          = robot.getDirection();
        boolean   needsForwardTarget = available.stream().anyMatch(type -> type != ModuleType.REAR_LASER);
        boolean   needsSecondTarget  = available.contains(ModuleType.HIGH_POWER_LASER);
        boolean   needsRearTarget    = available.contains(ModuleType.REAR_LASER);

        Robot forwardTarget = needsForwardTarget
                ? game.getLaserTarget(robot.getPosition(), direction, robot.getLevel(),
                                      false, 1, false, Step.ROBOT_MOUNTED_LASER_FIRE)
                : null;
        Robot secondTarget = needsSecondTarget
                ? game.getLaserTarget(robot.getPosition(), direction, robot.getLevel(),
                                      true, 1, false, Step.ROBOT_MOUNTED_LASER_FIRE)
                : null;
        Robot rearTarget = needsRearTarget
                ? game.getLaserTarget(robot.getPosition(), direction.reverse(), robot.getLevel(),
                                      false, 1, false, Step.ROBOT_MOUNTED_LASER_FIRE)
                : null;

        ModuleType bestWeapon = available.getFirst();
        double     bestScore  = Double.NEGATIVE_INFINITY;
        for (ModuleType candidate : available) {
            double score = switch (candidate) {
                case MAIN_LASER -> scoreDamage(forwardTarget, direction, phase, 1, goal);
                case DOUBLE_BARREL_LASER -> scoreDamage(forwardTarget, direction, phase, 2, goal);
                case HIGH_POWER_LASER -> scoreDamage(forwardTarget, direction, phase, 1, goal)
                        + scoreDamage(secondTarget, direction, phase, 1, goal);
                case REAR_LASER -> scoreDamage(rearTarget, direction, phase, 1, goal);
                case PRESSURE_BEAM -> scorePressureBeam(game.getCourse(), forwardTarget, direction, phase);
                case TRACTOR_BEAM -> scoreTractorBeam(robot, forwardTarget, direction);
                case EXCHANGE_BEAM -> scoreExchangeBeam(game.getCourse(), robot, forwardTarget);
                case SPIN_LEFT_BEAM -> scoreSpinBeam(robot, forwardTarget, Direction.WEST);
                case SPIN_RIGHT_BEAM -> scoreSpinBeam(robot, forwardTarget, Direction.EAST);
                default -> Double.NEGATIVE_INFINITY;
            };
            if (score > bestScore) {
                bestScore = score;
                bestWeapon = candidate;
            }
        }
        return bestWeapon;
    }

    private static double scoreDamage(Robot target, Direction shooterDirection, int phase, int hits, Goal goal) {
        if (target == null) {
            return 0.0;
        }

        int    damage = calculateDamage(target, shooterDirection, phase, hits);
        double score  = damage * DAMAGE_VALUE;
        if (goal == Goal.KILL_ALL && target.getDamage() + damage >= 10) {
            score += KILL_ALL_ELIMINATION_VALUE;
        }
        return score;
    }

    /**
     * Mirrors Robot.takeDamage's shield-consumption order for the expected
     * number of hits without modifying the simulated robot.
     */
    private static int calculateDamage(Robot target, Direction shooterDirection, int phase, int hits) {
        Module powerDownShield = target.getModule(ModuleType.POWER_DOWN_SHIELD);
        boolean[] remainingPowerDownShield = powerDownShield == null
                ? null : powerDownShield.getPowerDownShield()[phase].clone();

        Module    shield          = target.getModule(ModuleType.SHIELD);
        boolean   shieldActive    = shield != null && target.uses(ModuleType.SHIELD, phase);
        Direction shieldDirection = shield == null ? null : shield.getShieldDirection()[phase];

        Module layeredArmor           = target.getModule(ModuleType.LAYERED_ARMOR);
        int    layeredArmorAmmunition = layeredArmor == null ? 0 : layeredArmor.getAmmunition();

        Direction incomingDirection = shooterDirection.reverse();
        int       damage            = 0;
        for (int hit = 0; hit < hits; hit++) {
            boolean takesDamage = true;

            if (powerDownShield != null && target.uses(ModuleType.POWER_DOWN_SHIELD, phase)
                    && powerDownShield.getActiveInPhase()[phase]
                    && remainingPowerDownShield != null
                    && remainingPowerDownShield[incomingDirection.ordinal()]) {
                remainingPowerDownShield[incomingDirection.ordinal()] = false;
                takesDamage = false;
            }

            if (shieldActive && shieldDirection == incomingDirection) {
                shieldActive = false;
                takesDamage = false;
            }

            if (layeredArmorAmmunition > 0) {
                layeredArmorAmmunition--;
                takesDamage = false;
            }

            if (takesDamage) {
                damage++;
            }
        }
        return damage;
    }

    private static double scorePressureBeam(Course course, Robot target, Direction direction, int phase) {
        if (target == null) {
            return 0.0;
        }

        Position destination = target.getPosition().neighbour(direction);
        if (!destination.inRange(course.getRange())) {
            return 20.0;
        }

        Floor floor = CourseHandler.getFloor(course, destination);
        if (floor.getFloortype() == Floortype.ABYSS
                || (floor.getFloortype() == Floortype.TRAPDOOR && floor.getActiveInPhase()[phase])) {
            return 250.0;
        }
        return 5.0;
    }

    private static double scoreTractorBeam(Robot robot, Robot target, Direction direction) {
        if (target == null) {
            return 0.0;
        }
        Position destination = target.getPosition().neighbour(direction.reverse());
        return Objects.equals(destination, robot.getPosition()) ? 2.0 : 12.0;
    }

    private static double scoreExchangeBeam(Course course, Robot robot, Robot target) {
        if (target == null) {
            return 0.0;
        }

        int oldDistance = totalDistanceToOtherRobots(course, robot, target, robot.getPosition());
        int newDistance = totalDistanceToOtherRobots(course, robot, target, target.getPosition());
        return (oldDistance - newDistance) * 2.0 + 5.0;
    }

    private static int totalDistanceToOtherRobots(Course course, Robot robot, Robot excludedTarget, Position position) {
        int distance = 0;
        for (Robot other : course.getRobots()) {
            if (other != robot && other != excludedTarget && other.isOnCourse()) {
                distance += Math.abs(position.x() - other.getPosition().x())
                        + Math.abs(position.y() - other.getPosition().y());
            }
        }
        return distance;
    }

    private static double scoreSpinBeam(Robot robot, Robot target, Direction rotation) {
        if (target == null) {
            return 0.0;
        }

        Direction newDirection      = target.getDirection().add(rotation);
        boolean   currentlyFacingUs = isFacing(target.getDirection(), target.getPosition(), robot.getPosition());
        boolean   willFaceUs        = isFacing(newDirection, target.getPosition(), robot.getPosition());
        if (currentlyFacingUs && !willFaceUs) {
            return 18.0;
        }
        if (!currentlyFacingUs && willFaceUs) {
            return 2.0;
        }
        return 10.0;
    }

    private static boolean isFacing(Direction direction, Position from, Position target) {
        return switch (direction) {
            case NORTH -> target.y() < from.y();
            case EAST -> target.x() > from.x();
            case SOUTH -> target.y() > from.y();
            case WEST -> target.x() < from.x();
        };
    }

}
