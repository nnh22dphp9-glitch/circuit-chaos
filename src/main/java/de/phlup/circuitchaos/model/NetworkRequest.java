package de.phlup.circuitchaos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.phlup.circuitchaos.enums.Step;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkRequest {

    // ALWAYS
    private Board board;
    private Robot activeRobot;

    // REFRESH_BOARD
    private Step    step;
    private Integer subPhase;
    private Integer animationSteps;
    // REFRESH_BOARD, PROGRAM_PHASE_MODULES & CHOOSE_WEAPON
    private Integer phase;
    // REFRESH_BOARD
    private String  reasonForBoardChange;
    // CHOOSE_WEAPON
    private Robot   target;
    private Robot   highPowerLaserTarget;
    // REPAIR
    private Integer wrenches;

}
