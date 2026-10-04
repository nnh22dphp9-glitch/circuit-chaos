package de.phlup.circuitchaos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.phlup.circuitchaos.enums.Step;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkRequest {

    private Board   board;
    private Robot   myRobot;
    private Step    step;
    private Integer phase;
    private Integer subPhase;
    private Integer animationSteps;
    private String  movingRobotName;
    private String  reasonForBoardChange;

}
