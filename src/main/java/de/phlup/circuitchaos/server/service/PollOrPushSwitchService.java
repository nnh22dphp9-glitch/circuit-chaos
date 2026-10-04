package de.phlup.circuitchaos.server.service;

import de.phlup.circuitchaos.client.ServerToClientConnection;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Board;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.Registration;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class PollOrPushSwitchService {

    private final ServerToClientConnection serverToClientConnection;
    private final PollingService           pollingService;

    public void notifyOfBoardChange(Registration registration, Board board, String reason, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        if (registration.isPull()) {
            pollingService.notifyOfBoardChange(registration, board, reason, step, phase, subPhase, animationSteps, movingRobotName);
        } else {
            serverToClientConnection.notifyOfBoardChange(registration, board, reason, step, phase, subPhase, animationSteps, movingRobotName);
        }
    }

    public void showMessage(Registration registration, String message) {
        if (registration.isPull()) {
            pollingService.showMessage(registration, message);
        } else {
            serverToClientConnection.showMessage(registration, message);
        }
    }

    public void revealProgrammes(Registration registration, RevealProgrammeResponse revealProgrammeResponse) {
        if (registration.isPull()) {
            pollingService.revealProgrammes(registration, revealProgrammeResponse);
        } else {
            serverToClientConnection.revealProgramme(registration, revealProgrammeResponse);
        }
    }

    public void programQuestion(Registration registration, String purposeId, String requestId, NetworkRequest request) {
        if (registration.isPull()) {
            pollingService.programQuestion(registration, purposeId, requestId, request);
        } else {
            serverToClientConnection.programQuestion(registration, purposeId, requestId, request);
        }
    }

    public void notifyOfNumberOfPlayersChange(Registration registration, int numberOfPlayers, int numberOfWatchers) {
        if (registration.isPull()) {
            pollingService.notifyOfNumberOfPlayersChange(registration, numberOfPlayers, numberOfWatchers);
        } else {
            serverToClientConnection.notifyOfNumberOfPlayersChange(registration, numberOfPlayers, numberOfWatchers);
        }
    }

}
