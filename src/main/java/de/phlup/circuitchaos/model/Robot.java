package de.phlup.circuitchaos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.phlup.circuitchaos.enums.ModuleType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Robot extends BoardElement {

    private String          id;
    private int             damage                 = 0;
    private Position        archivePosition;
    private int             archiveLevel;
    private List<Programme> potentialProgramme     = new ArrayList<>();
    private List<Module>    modules                = new ArrayList<>();
    private boolean[]       blocked                = new boolean[]{false, false, false, false, false};
    private boolean         giveUp                 = false;
    private boolean         virtual                = true;
    private boolean         poweredDown            = false;
    private boolean         powerDownAnnounced     = false;
    private boolean         mayChooseDirection     = true;
    private int             nextCheckpoint         = 1;
    private boolean         reactivateRammingArmor = false;

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
    public void adjustToNetworkRobot(Robot networkRobot) {
        setPosition(networkRobot.getPosition());
        setPrevPosition(networkRobot.getPrevPosition());
        setLevel(networkRobot.getLevel());
        setFlying(networkRobot.isFlying());
        setDirection(networkRobot.getDirection());
        setPrevDirection(networkRobot.getPrevDirection());
        System.arraycopy(networkRobot.getProgram(), 0, getProgram(), 0,
                         Math.min(networkRobot.getProgram().length, getProgram().length));
        setOnBoard(networkRobot.isOnBoard());
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
                serverModule.setDirection(networkModule.getDirection());
            }
        }

        giveUp = networkRobot.isGiveUp();
        virtual = networkRobot.isVirtual();
        poweredDown = networkRobot.isPoweredDown();
        powerDownAnnounced = networkRobot.isPowerDownAnnounced();
        mayChooseDirection = networkRobot.isMayChooseDirection();
        nextCheckpoint = networkRobot.getNextCheckpoint();
        reactivateRammingArmor = networkRobot.isReactivateRammingArmor();
    }

}
