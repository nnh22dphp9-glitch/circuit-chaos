package de.phlup.circuitchaos.client;

import de.phlup.circuitchaos.client.gui.ClientCourseArranger;
import de.phlup.circuitchaos.client.gui.GameGui;
import de.phlup.circuitchaos.client.service.AsyncService;
import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.GameAttributes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/client")
@AllArgsConstructor
@SuppressWarnings("unused")
public class ClientController {

    private final AsyncService asyncService;

    /**
     * @see de.phlup.circuitchaos.client.ServerToClientConnection notifyOfCourseChange(Registration, Course, String, Step, Integer)
     */
    @PostMapping("/{registrationId}/course")
    public ResponseEntity<Void> refreshCourse(@PathVariable("registrationId") String registrationId,
                                              @RequestBody NetworkRequest request) {
        if (!StringUtils.hasText(registrationId) || request == null || request.getCourse() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.debug("Received refreshCourse request");
        GameGui gameGui = determineGame(registrationId);
        asyncService.refreshCourse(gameGui, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * @see de.phlup.circuitchaos.client.ServerToClientConnection showMessage(Registration, String)
     */
    @PostMapping("/{registrationId}/message")
    public ResponseEntity<Void> showMessage(@PathVariable("registrationId") String registrationId,
                                            @RequestBody String message) {
        if (!StringUtils.hasText(registrationId) || message == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.debug("Received showMessage request");
        asyncService.showMessage(registrationId, message);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * @see de.phlup.circuitchaos.client.ServerToClientConnection revealProgramme(Registration, RevealProgrammeResponse)
     */
    @PostMapping("/{registrationId}/reveal")
    public ResponseEntity<Void> revealProgramme(@PathVariable("registrationId") String registrationId,
                                                @RequestBody RevealProgrammeResponse revealProgrammeResponse) {
        if (!StringUtils.hasText(registrationId) || revealProgrammeResponse == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.debug("Received revealProgramme request");
        GameGui gameGui = determineGame(registrationId);
        gameGui.setNotStartedYet(false);
        asyncService.revealProgramme(gameGui, revealProgrammeResponse);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * @see de.phlup.circuitchaos.client.ServerToClientConnection programQuestion(Registration, Action, String, String, NetworkRequest)
     */
    @PostMapping("/{registrationId}/answer/program/{purposeId}/{requestId}")
    public ResponseEntity<Void> programQuestion(@PathVariable("registrationId") String registrationId,
                                                @PathVariable("purposeId") String purposeId,
                                                @PathVariable("requestId") String requestId,
                                                @RequestBody NetworkRequest request) {
        if (!StringUtils.hasText(registrationId) || request == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.debug("Received programQuestion request");
        GameGui gameGui = determineGame(registrationId);
        gameGui.setNotStartedYet(false);
        String answerUrl = gameGui.getGameAttributes().getGameUrl() + "/answer/" + purposeId + "/" + requestId;
        asyncService.perform(gameGui, answerUrl, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * @see de.phlup.circuitchaos.client.ServerToClientConnection notifyOfNumberOfPlayersChange(Registration, int, int)
     */
    @GetMapping("/{registrationId}/numberOfPlayersChanged/{numberOfPlayers}/{numberOfWatchers}")
    public ResponseEntity<Void> numberOfPlayersChanged(@PathVariable("registrationId") String registrationId,
                                                       @PathVariable("numberOfPlayers") int numberOfPlayers,
                                                       @PathVariable("numberOfWatchers") int numberOfWatchers) {
        if (!StringUtils.hasText(registrationId)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.debug("Received numberOfPlayersChanged request");
        ClientCourseArranger courseArranger = determineGame(registrationId).getCourseArranger();
        if (courseArranger != null) {
            courseArranger.updatePlayersAndWatchers(numberOfPlayers, numberOfWatchers);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private GameGui determineGame(String registrationId) {
        GameAttributes gameAttributes = GlobalServerAttributes.CLIENT_GAMES.get(registrationId);
        if (gameAttributes == null) {
            throw new CircuitChaosException("Game not found");
        }
        GameGui gameGui = gameAttributes.getGameGui();
        if (gameGui == null) {
            throw new CircuitChaosException("Game not found");
        }
        return gameGui;
    }

}
