package de.phlup.circuitchaos.gamelogic;

import de.phlup.circuitchaos.exception.CircuitChaosException;
import de.phlup.circuitchaos.model.GameItem;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalServerAttributes {

    public static final SecureRandom                RANDOM          = new SecureRandom();
    public static final Map<GameItem, Game>         PREPARING_GAMES = new ConcurrentHashMap<>();
    public static final Map<String, Game>           RUNNING_GAMES   = new ConcurrentHashMap<>();
    public static final Map<String, GameAttributes> CLIENT_GAMES    = new ConcurrentHashMap<>();

    /**
     * WARNING: do not use on network stuff!
     */
    public static Game getGame(String id) {
        return determineGame(id, true, true, false);
    }

    public static Game getGame(String id, boolean considerRunning, boolean considerPreparing) {
        return determineGame(id, considerRunning, considerPreparing, true);
    }

    public static void setGameRunning(String gameId, Game game) {
        for (GameItem i : PREPARING_GAMES.keySet()) {
            if (gameId.equals(i.getGameId())) {
                PREPARING_GAMES.remove(i);
                break;
            }
        }
        RUNNING_GAMES.put(gameId, game);
    }

    public static void removePreparingGame(String gameId) {
        for (GameItem i : PREPARING_GAMES.keySet()) {
            if (gameId.equals(i.getGameId())) {
                PREPARING_GAMES.remove(i);
                break;
            }
        }
    }

    private static Game determineGame(String gameId,
                                      boolean considerRunning,
                                      boolean considerPreparing,
                                      boolean errorOnAborted) {
        Game game = considerRunning ? GlobalServerAttributes.RUNNING_GAMES.get(gameId) : null;
        if (considerPreparing && game == null) {
            for (Map.Entry<GameItem, Game> entry : GlobalServerAttributes.PREPARING_GAMES.entrySet()) {
                if (gameId.equals(entry.getKey().getGameId())) {
                    game = entry.getValue();
                }
            }
        }
        if (game == null) {
            throw new CircuitChaosException("Game not found");
        }
        if (errorOnAborted && game.isGameAborted()) {
            throw new CircuitChaosException("Game aborted");
        }
        game.setLastAccessed(ZonedDateTime.now());
        return game;
    }

}
