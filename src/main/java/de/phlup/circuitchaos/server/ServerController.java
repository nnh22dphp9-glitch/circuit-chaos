package de.phlup.circuitchaos.server;

import de.phlup.circuitchaos.client.service.CircuitChaosGui;
import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.GameItem;
import de.phlup.circuitchaos.common.model.GameItemsWrapper;
import de.phlup.circuitchaos.common.model.NetworkResponse;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.PullingData;
import de.phlup.circuitchaos.common.model.Registration;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import de.phlup.circuitchaos.common.settings.TimeSettings;
import de.phlup.circuitchaos.server.game.Game;
import de.phlup.circuitchaos.server.game.GameOptions;
import de.phlup.circuitchaos.server.game.ServerCourseArranger;
import de.phlup.circuitchaos.server.player.Player;
import de.phlup.circuitchaos.server.player.network.NetworkPlayer;
import de.phlup.circuitchaos.server.service.PollOrPushSwitchService;
import de.phlup.circuitchaos.server.service.PollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ServerController {

    private final PollOrPushSwitchService pollOrPushSwitchService;
    private final TimeSettings            timeSettings;
    private final ClientSettings          clientSettings;
    private final PollingService          pollingService;

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection getAllPreparingGames(String server)
     */
    @GetMapping
    public ResponseEntity<GameItemsWrapper> getGameIds() {
        GameItemsWrapper gameItemsWrapper = new GameItemsWrapper();
        gameItemsWrapper.setItems(GlobalServerAttributes.PREPARING_GAMES.keySet());
        return new ResponseEntity<>(gameItemsWrapper, HttpStatus.OK);
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection createGame(String gameUrl, Registration clientRegistration)
     */
    @PostMapping
    public ResponseEntity<String> createGame(@RequestBody Registration registration) {
        if (registration == null || (!registration.isPull() && !StringUtils.hasText(registration.getUrl()))) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.debug("Received createGame request");
        if (!clientSettings.isAllowOthersToStartAGame()
                && (!registration.getId().equals(CircuitChaosGui.lastRequestedNewGame)
                || !registration.getUrl().equals(clientSettings.getOwnUrl() + "/client/" + registration.getId()))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        Game game = new Game(pollOrPushSwitchService, timeSettings, registration);
        game.setAnimationSteps(timeSettings.getAnimationSteps());
        game.getWatchers().add(registration);
        game.getNetworkPlayers().add(registration);
        GlobalServerAttributes.PREPARING_GAMES.put(new GameItem(game.getInitiator().getGameName(), game.getGameId()), game);
        return ResponseEntity.ok(game.getGameId());
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection registerAsWatcher(String gameUrl, Registration clientRegistration)
     */
    @PostMapping("/{gameId}")
    public ResponseEntity<Void> register(@PathVariable("gameId") String gameId, @RequestBody Registration registration) {
        if (!StringUtils.hasText(gameId) || registration == null || (!registration.isPull() && !StringUtils.hasText(registration.getUrl()))) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.debug("Received register request");
        Game game = GlobalServerAttributes.getGame(gameId, false, true);
        if (!registration.isWatchOnly()) {
            if (game.getNetworkPlayers().size() > 7) {
                throw new CircuitChaosException("The game is full. Registered only as watcher.");
            }
            game.getNetworkPlayers().add(registration);
        }
        game.getWatchers().add(registration);
        pollOrPushSwitchService.notifyOfNumberOfPlayersChange(game.getInitiator(), game.getNetworkPlayers().size(), game.getWatchers().size());
        return ResponseEntity.noContent().build();
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection deregister(String gameUrl, String clientRegistration)
     */
    @DeleteMapping("/{gameId}/{registrationId}")
    public ResponseEntity<Course> deregister(@PathVariable("gameId") String gameId, @PathVariable("registrationId") String registrationId) {
        log.debug("Received deregister request");
        Game               game             = GlobalServerAttributes.getGame(gameId, true, true);
        List<Registration> registrationList = List.copyOf(game.getWatchers());
        for (Registration reg : registrationList) {
            if (reg.getId().equals(registrationId)) {
                game.getWatchers().remove(reg);
            }
        }
        for (Player player : List.copyOf(game.getPlayers())) {
            if (player instanceof NetworkPlayer networkPlayer && registrationId.equals(networkPlayer.getRegistration().getId())) {
                game.getPlayers().remove(player);
                game.giveUp(networkPlayer.getRobot(), networkPlayer.getRegistration().getId());
            }
        }
        for (Registration player : List.copyOf(game.getNetworkPlayers())) {
            if (registrationId.equals(player.getId())) {
                game.getNetworkPlayers().remove(player);
            }
        }
        if (game.getInitiator() != null && registrationId.equals(game.getInitiator().getId())) {
            GlobalServerAttributes.removePreparingGame(gameId);
        } else if (game.getInitiator() != null && game.isNotStartedYet()) {
            pollOrPushSwitchService.notifyOfNumberOfPlayersChange(game.getInitiator(), game.getNetworkPlayers().size(), game.getWatchers().size());
        }
        return new ResponseEntity<>(game.getCourse(), HttpStatus.NO_CONTENT);
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection getCourse(String gameUrl)
     */
    @GetMapping("/{gameId}/course")
    public Course getCourse(@PathVariable("gameId") String gameId) {
        log.debug("Received getCourse request");
        return GlobalServerAttributes.getGame(gameId, true, true).getCourse();
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection play(String gameUrl, GameOptions gameOptions)
     */
    @PostMapping("/{gameId}/play")
    public void play(@PathVariable("gameId") String gameId, @RequestBody GameOptions options) {
        log.debug("Received play request");
        GlobalServerAttributes.getGame(gameId, false, true).play(options);
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection replaceCourse(String gameUrl, Course course)
     */
    @PostMapping("/{gameId}/course")
    public void replaceCourse(@PathVariable("gameId") String gameId, @RequestBody Course course) {
        log.debug("Received replaceCourse request");
        Game                 game           = GlobalServerAttributes.getGame(gameId, false, true);
        ServerCourseArranger courseArranger = game.getCourseArranger();
        if (courseArranger == null || !game.isNotStartedYet()) {
            throw new CircuitChaosException("Game has already startet.");
        }
        try {
            courseArranger.setCourse(game, course);
        } catch (Exception e) {
            log.info("Exception!", e);
            throw new CircuitChaosException("Exception! - " + e.getMessage(), e);
        }
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection addCheckpoint(String gameUrl, Position position)
     */
    @PostMapping("/{gameId}/checkpoint")
    public void addCheckpoint(@PathVariable("gameId") String gameId, @RequestParam("x") int x, @RequestParam("y") int y) {
        log.debug("Received addCheckpoint request");
        Game                 game           = GlobalServerAttributes.getGame(gameId, false, true);
        ServerCourseArranger courseArranger = game.getCourseArranger();
        if (courseArranger == null || !game.isNotStartedYet()) {
            throw new CircuitChaosException("Game has already startet.");
        }
        try {
            courseArranger.addCheckpoint(game, new Position(x, y));
        } catch (Exception e) {
            log.info("Exception!", e);
            throw new CircuitChaosException("Exception! - " + e.getMessage(), e);
        }
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection removeCheckpoint(String gameUrl, Position position)
     */
    @DeleteMapping("/{gameId}/checkpoint")
    public void removeCheckpoint(@PathVariable("gameId") String gameId, @RequestParam("x") int x, @RequestParam("y") int y) {
        log.debug("Received removeCheckpoint request");
        Game                 game           = GlobalServerAttributes.getGame(gameId, false, true);
        ServerCourseArranger courseArranger = game.getCourseArranger();
        if (courseArranger == null || !game.isNotStartedYet()) {
            throw new CircuitChaosException("Game has already startet.");
        }
        try {
            courseArranger.removeCheckpoint(game, new Position(x, y));
        } catch (Exception e) {
            log.info("Exception!", e);
            throw new CircuitChaosException("Exception! - " + e.getMessage(), e);
        }
    }

    /**
     * @see de.phlup.circuitchaos.client.ClientToServerConnection postAnswer(String answerUrl, NetworkResponse response)
     */
    @PostMapping("/{gameId}/answer/{purposeId}/{requestId}")
    public void receiveAnswer(@PathVariable("gameId") String gameId,
                              @PathVariable("purposeId") String purposeId,
                              @PathVariable("requestId") String requestId,
                              @RequestBody NetworkResponse response) {
        log.info("Received receivedAnswer request: game {}, purpose {}, request {}, response {}",
                 gameId, purposeId, requestId, response);
        Game                                      game                   = GlobalServerAttributes.getGame(gameId, true, false);
        Map<String, Map<String, NetworkResponse>> awaitingResponses      = game.getAwaitingResponses();
        Map<String, NetworkResponse>              networkResponseHashMap = awaitingResponses.get(purposeId);
        if (networkResponseHashMap == null) {
            throw new CircuitChaosException("You´ve timed out.");
        } else {
            if (networkResponseHashMap.get(requestId) == null) {
                throw new CircuitChaosException("Illegal request ID");
            } else {
                networkResponseHashMap.put(requestId, response);
            }
        }
    }

    @GetMapping("/{gameId}/pull/{registrationId}")
    public List<PullingData> pullAction(@PathVariable("gameId") @SuppressWarnings("unused") String gameId,
                                        @PathVariable("registrationId") String registrationId) {
        log.debug("Received pullAction request");
        return pollingService.getDataByRegistrationId(registrationId);
    }

}
