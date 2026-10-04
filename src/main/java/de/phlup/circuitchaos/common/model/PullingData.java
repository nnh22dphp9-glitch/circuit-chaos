package de.phlup.circuitchaos.common.model;

import de.phlup.circuitchaos.common.enums.PullType;
import lombok.Data;

@Data
public class PullingData {

    private PullType type;

    // refresh board / action
    private NetworkRequest request;

    // show message
    private String message;

    // reveal programme
    private RevealProgrammeResponse revealProgrammeResponse;

    // action
    private String purposeId;
    private String requestId;

    // number of players changed
    private int numberOfPlayers;
    private int numberOfWatchers;

}
