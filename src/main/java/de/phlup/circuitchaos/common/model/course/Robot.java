package de.phlup.circuitchaos.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Goal;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Programme;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

import static de.phlup.circuitchaos.common.enums.ModuleType.LAYERED_ARMOR;
import static de.phlup.circuitchaos.common.enums.ModuleType.POWER_DOWN_SHIELD;
import static de.phlup.circuitchaos.common.enums.ModuleType.SHIELD;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Robot extends CourseElement {

    private String          id;
    private int             damage                 = 0;
    private Position        archivePosition;
    private int             archiveLevel;
    private Programme[]     program                = new Programme[5];
    private List<Programme> potentialProgramme     = new ArrayList<>();
    private List<Module>    modules                = new ArrayList<>();
    private boolean[]       blocked                = new boolean[]{false, false, false, false, false};
    private boolean         giveUp                 = false;
    private boolean         poweredDown            = false;
    private boolean         powerDownAnnounced     = false;
    private boolean         mayChooseDirection     = true;
    private int             nextCheckpoint         = 1;
    private boolean         reactivateRammingArmor = false;
    private boolean         flying                 = false;
    private int             dieCounter             = 0;

    @JsonIgnore
    public void adjustToNetworkRobot(Robot networkRobot) {
        setPosition(networkRobot.getPosition());
        setPrevPosition(networkRobot.getPrevPosition());
        setLevel(networkRobot.getLevel());
        setFlying(networkRobot.isFlying());
        setDirection(networkRobot.getDirection());
        setPrevDirection(networkRobot.getPrevDirection());
        System.arraycopy(networkRobot.getProgram(), 0, getProgram(), 0,
                         Math.min(networkRobot.getProgram().length, getProgram().length));
        setOnCourse(networkRobot.isOnCourse());
        damage = networkRobot.getDamage();
        archivePosition = networkRobot.getArchivePosition();
        archiveLevel = networkRobot.getArchiveLevel();
        System.arraycopy(networkRobot.getBlocked(), 0, blocked, 0,
                         Math.min(networkRobot.getBlocked().length, blocked.length));

        potentialProgramme.clear();
        potentialProgramme.addAll(networkRobot.getPotentialProgramme());

        for (Module serverModule : modules) {
            Module networkModule = networkRobot.getModule(serverModule.getType());
            if (networkModule != null) {
                serverModule.setActiveInPhase(networkModule.getActiveInPhase());
                serverModule.setShieldDirection(networkModule.getShieldDirection());
            }
        }

        giveUp = networkRobot.isGiveUp();
        poweredDown = networkRobot.isPoweredDown();
        powerDownAnnounced = networkRobot.isPowerDownAnnounced();
        mayChooseDirection = networkRobot.isMayChooseDirection();
        nextCheckpoint = networkRobot.getNextCheckpoint();
        reactivateRammingArmor = networkRobot.isReactivateRammingArmor();
    }

    @JsonIgnore
    public Module getModule(ModuleType moduleType) {
        if (moduleType == null || modules == null) {
            return null;
        }
        for (Module m : modules) {
            if (m != null && m.getType() == moduleType) {
                return m;
            }
        }
        return null;
    }

    @JsonIgnore
    public boolean hasModule(ModuleType moduleType) {
        return moduleType != null && getModule(moduleType) != null;
    }

    @JsonIgnore
    public boolean uses(ModuleType moduleType, Integer phase) {
        if (phase == null || phase < 0) {
            return false;
        }
        boolean erg    = false;
        Module  module = getModule(moduleType);
        if (module != null) {
            erg = module.getActiveInPhase()[phase];
        }
        return erg;
    }

    @JsonIgnore
    public void takeDamage(Course course, Integer phase, Direction direction, boolean considerShields) {
        if (isOnCourse()) {
            boolean takesDamage = true;
            if (considerShields) {
                if (uses(POWER_DOWN_SHIELD, phase)) {
                    Module module = getModule(POWER_DOWN_SHIELD);
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
                        Module module = getModule(SHIELD);
                        if (module.getActiveInPhase()[phase] && module.getShieldDirection()[phase] == direction.reverse()) {
                            module.getActiveInPhase()[phase] = false;
                            takesDamage = false;
                        }
                    }
                }
                Module module = getModule(LAYERED_ARMOR);
                if (module != null && module.getAmmunition() > 0) {
                    module.setAmmunition(module.getAmmunition() - 1);
                    takesDamage = false;
                }
            }
            if (takesDamage) {
                takeDamage(course);
            }
        }
    }

    @JsonIgnore
    public void takeDamage(Course course) {
        if (isOnCourse()) {
            damage++;
            if (damage - countBlockedRegisters() > 4) {
                blockRegister();
            }
            if (isOnCourse() && damage >= 10) {
                CourseHandler.remove(course, this);
                dieCounter++;
            }
        }
    }

    @JsonIgnore
    private void blockRegister() {
        int index = 4;
        while (blocked[index]) {
            index--;
            if (index == -1) {
                break;
            }
        }
        if (index > -1) {
            blocked[index] = true;
        }
    }

    @JsonIgnore
    private int countBlockedRegisters() {
        int erg = 0;
        for (int i = 0; i < 5; i++) {
            if (blocked[i]) {
                erg++;
            }
        }
        return erg;
    }

    @JsonIgnore
    public boolean hasLost(Goal goal) {
        return giveUp || (goal == Goal.KILL_ALL && dieCounter > 0);
    }

    @JsonIgnore
    public boolean hasWon(Goal goal, int numberOfCheckpoints) {
        return !hasLost(goal) && (goal == Goal.FINISH && nextCheckpoint > numberOfCheckpoints - 1);
    }

    /**
     * Creates a copy for an isolated game simulation. Modules whose state can
     * change during a turn are copied; all other modules are shared read-only.
     */
    @JsonIgnore
    public Robot copyForSimulation() {
        Robot robot = copyForSimulation(new Robot());
        robot.setId(id);
        robot.setDamage(damage);
        robot.setArchivePosition(archivePosition);
        robot.setArchiveLevel(archiveLevel);
        System.arraycopy(program, 0, robot.getProgram(), 0, 5);
        // robot.getPotentialProgramme().addAll(potentialProgramme); // Computer player should not know this
        robot.getModules().addAll(modules.stream()
                .map(module -> isModifiedDuringSimulation(module.getType())
                        ? module.copyForSimulation() : module)
                .toList());
        System.arraycopy(blocked, 0, robot.getBlocked(), 0, 5);
        robot.setGiveUp(giveUp);
        robot.setPoweredDown(poweredDown);
        robot.setPowerDownAnnounced(powerDownAnnounced);
        robot.setMayChooseDirection(mayChooseDirection);
        robot.setNextCheckpoint(nextCheckpoint);
        robot.setReactivateRammingArmor(reactivateRammingArmor);
        robot.setFlying(flying);
        robot.setDieCounter(dieCounter);
        return robot;
    }

    private boolean isModifiedDuringSimulation(ModuleType type) {
        return switch (type) {
            case POWER_DOWN_SHIELD, SHIELD, LAYERED_ARMOR, RAMMING_ARMOR,
                    MOBILE_TELEPORTER, MOBILE_RANDOMIZER, GLUE_DISPENSER,
                    OIL_DISPENSER, MINE_LAYER, PROXIMITY_MINE_LAYER,
                    BRIDGE_PROJECTOR -> true;
            default -> false;
        };
    }

}
