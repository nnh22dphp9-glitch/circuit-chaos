package de.phlup.circuitchaos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.phlup.circuitchaos.enums.ModuleType;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkResponse {

    private boolean filled = false;

    // PROGRAM_ROBOT
    private Robot      programmedRobot;
    // PROGRAM_PHASE_MODULES
    private Boolean    brakes;
    // CHOOSE_WEAPON
    private ModuleType module;

}
