package de.phlup.circuitchaos.server.service;

import de.phlup.circuitchaos.common.enums.PullType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.PullingData;
import de.phlup.circuitchaos.common.model.Registration;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.Game;
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

    public void notifyOfCourseChange(Registration registration, Course course, String reason, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        NetworkRequest programRequest = new NetworkRequest();
        programRequest.setCourse(course);
        programRequest.setReasonForCourseChange(reason);
        programRequest.setStep(step);
        programRequest.setPhase(phase);
        programRequest.setSubPhase(subPhase);
        programRequest.setAnimationSteps(animationSteps);
        programRequest.setMovingRobotName(movingRobotName);
        PullingData pullingData = new PullingData();
        pullingData.setType(PullType.COURSE_CHANGE);
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
