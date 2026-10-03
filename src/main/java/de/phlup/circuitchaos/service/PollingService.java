package de.phlup.circuitchaos.service;

import de.phlup.circuitchaos.enums.PullType;
import de.phlup.circuitchaos.enums.Step;
import de.phlup.circuitchaos.gamelogic.Game;
import de.phlup.circuitchaos.gamelogic.GlobalServerAttributes;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.NetworkRequest;
import de.phlup.circuitchaos.model.PullingData;
import de.phlup.circuitchaos.model.Registration;
import de.phlup.circuitchaos.model.RevealProgrammeResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Component
public class PollingService {

    private final Map<String, List<PullingData>> data = new HashMap<>();

    public List<PullingData> getDataByRegistrationId(String registrationId) {
        synchronized (data) {
            List<PullingData> dataList = data.get(registrationId);
            data.remove(registrationId);
            return dataList;
        }
    }

    public void notifyOfBoardChange(Registration registration, Board board, String reason, Step step, Integer phase, Integer subPhase, int animationSteps) {
        NetworkRequest programRequest = new NetworkRequest();
        programRequest.setBoard(board);
        programRequest.setReasonForBoardChange(reason);
        programRequest.setStep(step);
        programRequest.setPhase(phase);
        programRequest.setSubPhase(subPhase);
        programRequest.setAnimationSteps(animationSteps);
        PullingData pullingData = new PullingData();
        pullingData.setType(PullType.BOARD_CHANGE);
        pullingData.setRequest(programRequest);
        synchronized (data) {
            data.computeIfAbsent(registration.getId(), k -> new ArrayList<>()).add(pullingData);
        }
    }

    public void showMessage(Registration registration, String message) {
        PullingData pullingData = new PullingData();
        pullingData.setType(PullType.SHOW_MESSAGE);
        pullingData.setMessage(message);
        synchronized (data) {
            data.computeIfAbsent(registration.getId(), k -> new ArrayList<>()).add(pullingData);
        }
    }

    public void revealProgrammes(Registration registration, RevealProgrammeResponse revealProgrammeResponse) {
        PullingData pullingData = new PullingData();
        pullingData.setType(PullType.REVEAL_PROGRAMME);
        pullingData.setRevealProgrammeResponse(revealProgrammeResponse);
        synchronized (data) {
            data.computeIfAbsent(registration.getId(), k -> new ArrayList<>()).add(pullingData);
        }
    }

    public void programQuestion(Registration registration, String purposeId, String requestId, NetworkRequest request) {
        PullingData pullingData = new PullingData();
        pullingData.setType(PullType.PROGRAMME_QUESTION);
        pullingData.setPurposeId(purposeId);
        pullingData.setRequestId(requestId);
        pullingData.setRequest(request);
        synchronized (data) {
            data.computeIfAbsent(registration.getId(), k -> new ArrayList<>()).add(pullingData);
        }
    }

    public void notifyOfNumberOfPlayersChange(Registration registration, int numberOfPlayers, int numberOfWatchers) {
        PullingData pullingData = new PullingData();
        pullingData.setType(PullType.NUMBER_OF_PLAYERS_CHANGE);
        pullingData.setNumberOfPlayers(numberOfPlayers);
        pullingData.setNumberOfWatchers(numberOfWatchers - numberOfPlayers);
        synchronized (data) {
            data.computeIfAbsent(registration.getId(), k -> new ArrayList<>()).add(pullingData);
        }
    }

    public void cleanupRegistrations() {
        List<String> registrationsToCheck = data.keySet().stream().toList(); // make a copy
        for (String registrationId : registrationsToCheck) {
            if (isNotFound(registrationId, GlobalServerAttributes.PREPARING_GAMES.values())
                    && isNotFound(registrationId, GlobalServerAttributes.RUNNING_GAMES.values())) {
                data.remove(registrationId);
            }
        }
    }

    private static boolean isNotFound(String registrationId, Collection<Game> games) {
        boolean notFound = true;
        for (Game game : games) {
            for (Registration reg : game.getNetworkPlayers()) {
                if (registrationId.equals(reg.getId())) {
                    notFound = false;
                    break;
                }
            }
            for (Registration reg : game.getWatchers()) {
                if (registrationId.equals(reg.getId())) {
                    notFound = false;
                    break;
                }
            }
        }
        return notFound;
    }

}
