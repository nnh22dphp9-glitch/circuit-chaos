package de.phlup.circuitchaos.client;

import de.phlup.circuitchaos.exception.CircuitChaosException;
import de.phlup.circuitchaos.gamelogic.GameOptions;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.BoardPosition;
import de.phlup.circuitchaos.model.GameItem;
import de.phlup.circuitchaos.model.GameItemsWrapper;
import de.phlup.circuitchaos.model.NetworkResponse;
import de.phlup.circuitchaos.model.Position;
import de.phlup.circuitchaos.model.PullingData;
import de.phlup.circuitchaos.model.Registration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import javax.swing.JOptionPane;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@AllArgsConstructor
@Component
public class ClientToServerConnection {

    private final RestClient restClient;

    /**
     * @see de.phlup.circuitchaos.controller.ServerController getGameIds()
     */
    public Set<GameItem> getAllPreparingGames(String server) {
        // an error message is shown in the drop-down list
        log.debug("Send getAllPreparingGames request");
        return Objects.requireNonNull(restClient.get()
                                                .uri(URI.create(getServerUrl(server) + "/game"))
                                                .retrieve()
                                                .body(GameItemsWrapper.class)).getItems();
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController register(String gameId, Registration registration)
     */
    public void registerAsWatcher(String gameUrl, Registration clientRegistration) {
        log.debug("Send registerAsWatcher request");
        showErrorMessage(() -> restClient.post()
                                         .uri(URI.create(gameUrl))
                                         .contentType(MediaType.APPLICATION_JSON)
                                         .body(clientRegistration)
                                         .retrieve()
                                         .toBodilessEntity());
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController createGame(Registration registration)
     */
    public String createGame(String gameUrl, Registration clientRegistration) {
        log.debug("Send createGame request");
        return showErrorMessage(() -> restClient.post()
                                                .uri(URI.create(gameUrl))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body(clientRegistration)
                                                .retrieve()
                                                .body(String.class));
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController deregister(String gameId, String registrationId)
     */
    @Async
    public void deregister(String gameUrl, String clientRegistration) {
        log.debug("Send deregister request");
        try {
            restClient.delete().uri(URI.create(gameUrl + "/" + clientRegistration)).retrieve().toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            log.info("deregister: Ignoring http exception: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.info("exception", e);
        }
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController play(String gameId, GameOptions options)
     */
    public void play(String gameUrl, GameOptions gameOptions) {
        log.debug("Send play request");
        showErrorMessage(() -> restClient.post()
                                         .uri(URI.create(gameUrl + "/play"))
                                         .contentType(MediaType.APPLICATION_JSON)
                                         .body(gameOptions)
                                         .retrieve()
                                         .toBodilessEntity());
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController addBoard(String gameId, BoardPosition boardPosition)
     */
    public void addBoard(String gameUrl, BoardPosition boardPosition) {
        log.debug("Send addBoard request");
        showErrorMessage(() -> restClient.post()
                                         .uri(URI.create(gameUrl + "/board"))
                                         .contentType(MediaType.APPLICATION_JSON)
                                         .body(boardPosition)
                                         .retrieve()
                                         .toBodilessEntity());
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController removeBoard(String gameId, int x, int y)
     */
    public void removeBoard(String gameUrl, int x, int y) {
        log.debug("Send removeBoard request");
        String uri = gameUrl + "/board?x=" + x + "&y=" + y;
        showErrorMessage(() -> restClient.delete().uri(URI.create(uri)).retrieve().toBodilessEntity());
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController addCheckpoint(String gameId, int x, int y)
     */
    public void addCheckpoint(String gameUrl, Position position) {
        log.debug("Send addCheckpoint request");
        String uri = gameUrl + "/checkpoint?x=" + position.x() + "&y=" + position.y();
        showErrorMessage(() -> restClient.post()
                                         .uri(URI.create(uri))
                                         .contentType(MediaType.APPLICATION_JSON)
                                         .retrieve()
                                         .toBodilessEntity());
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController removeCheckpoint(String gameId, int x, int y)
     */
    public void removeCheckpoint(String gameUrl, Position position) {
        log.debug("Send removeCheckpoint request");
        String uri = gameUrl + "/checkpoint?x=" + position.x() + "&y=" + position.y();
        showErrorMessage(() -> restClient.delete().uri(URI.create(uri)).retrieve().toBodilessEntity());
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController getBoard(String gameId)
     */
    public Board getBoard(String gameUrl) {
        log.debug("Send getBoard request");
        return showErrorMessage(() -> restClient.get()
                                                .uri(URI.create(gameUrl + "/board"))
                                                .accept(MediaType.APPLICATION_JSON)
                                                .retrieve()
                                                .body(Board.class));
    }

    /**
     * @see de.phlup.circuitchaos.controller.ServerController receiveAnswer(String gameId, String purposeId, String requestId, NetworkResponse response)
     */
    public void postAnswer(String answerUrl, NetworkResponse response) {
        log.debug("Send postAnswer request");
        try {
            restClient.post()
                      .uri(URI.create(answerUrl))
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(response)
                      .retrieve()
                      .toBodilessEntity();
        } catch (Exception ignored) {
        }
    }

    public PullingData[] pull(String gameUrl, String registrationId) {
        log.debug("Send pull request");
        return restClient.get()
                         .uri(URI.create(gameUrl + "/pull/" + registrationId))
                         .accept(MediaType.APPLICATION_JSON)
                         .retrieve()
                         .body(PullingData[].class);
    }

    private String getServerUrl(String server) {
        if (!StringUtils.hasText(server) || (!server.startsWith("http://") && !server.startsWith("https://"))) {
            throw new CircuitChaosException("invalid url");
        }
        if (server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }
        return server;
    }

    private <T> T showErrorMessage(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (HttpClientErrorException exc) {
            if (exc.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                JOptionPane.showMessageDialog(null, exc.getResponseBodyAsString());
            } else {
                JOptionPane.showMessageDialog(null, "ERROR: %s - %s".formatted(exc.getClass().getSimpleName(), exc.getMessage()));
            }
            throw exc;
        } catch (Exception exc) {
            JOptionPane.showMessageDialog(null, "ERROR: %s - %s".formatted(exc.getClass().getSimpleName(), exc.getMessage()));
            throw exc;
        }
    }

}
