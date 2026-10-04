package de.phlup.circuitchaos.client;

import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.Registration;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Slf4j
@AllArgsConstructor
@Component
public class ServerToClientConnection {

    private final RestClient restClient;

    /**
     * @see ClientController refreshCourse(String  NetworkRequest)
     */
    // Nicht Async - Animationen könnten kaputtgehen
    public void notifyOfCourseChange(Registration registration, Course course, String reason, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        log.debug("Send notifyOfCourseChange request");
        try {
            NetworkRequest programRequest = new NetworkRequest();
            programRequest.setCourse(course);
            programRequest.setReasonForCourseChange(reason);
            programRequest.setStep(step);
            programRequest.setPhase(phase);
            programRequest.setSubPhase(subPhase);
            programRequest.setAnimationSteps(animationSteps);
            programRequest.setMovingRobotName(movingRobotName);
            restClient.post()
                      .uri(URI.create(registration.getUrl() + "/course"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(programRequest)
                      .retrieve()
                      .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            log.info("notifyOfCourseChange: Ignoring http exception: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.info("Ignoring exception", e);
        }
    }

    /**
     * @see ClientController showMessage(String, String)
     */
    @Async
    public void showMessage(Registration registration, String message) {
        log.debug("Send showMessage request");
        try {
            restClient.post()
                      .uri(URI.create(registration.getUrl() + "/message"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(message)
                      .retrieve()
                      .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            log.info("showMessage: Ignoring http exception: {}: {}}", e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.info("Ignoring exception", e);
        }
    }

    /**
     * @see ClientController revealProgramme(String, RevealProgrammesResponse)
     */
    @Async
    public void revealProgramme(Registration registration, RevealProgrammeResponse revealProgrammeResponse) {
        log.debug("Send revealProgramme request");
        try {
            restClient.post()
                      .uri(URI.create(registration.getUrl() + "/reveal"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(revealProgrammeResponse)
                      .retrieve()
                      .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            log.info("revealProgrammes: Ignoring http exception: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.info("Ignoring exception", e);
        }
    }

    /**
     * @see ClientController programQuestion(String, String, String, NetworkRequest)
     */
    @Async
    public void programQuestion(Registration registration, String purposeId, String requestId, NetworkRequest request) {
        log.debug("Send programQuestion request");
        try {
            restClient.post()
                      .uri(URI.create(registration.getUrl() + "/answer/program/" + purposeId + "/" + requestId))
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(request)
                      .retrieve()
                      .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            log.info("programQuestion: Ignoring http exception: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.info("Ignoring exception", e);
        }
    }

    /**
     * @see ClientController numberOfPlayersChanged(String, int, int)
     */
    @Async
    public void notifyOfNumberOfPlayersChange(Registration registration, int numberOfPlayers, int numberOfWatchers) {
        log.debug("Send notifyOfNumberOfPlayersChange request");
        try {
            restClient.get()
                      .uri(URI.create(registration.getUrl() + "/numberOfPlayersChanged/" + numberOfPlayers + "/" + (numberOfWatchers - numberOfPlayers)))
                      .retrieve()
                      .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() != 422) {
                log.info("notifyOfNumberOfPlayersChange: Ignoring http exception: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        } catch (Exception e) {
            log.info("Ignoring exception", e);
        }
    }

}
