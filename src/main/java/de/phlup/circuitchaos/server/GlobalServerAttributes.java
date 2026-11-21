package de.phlup.circuitchaos.server;

import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.model.GameAttributes;
import de.phlup.circuitchaos.common.model.GameItem;
import de.phlup.circuitchaos.server.game.Game;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalServerAttributes {

    public static final SecureRandom              RANDOM          = new SecureRandom();
    public static final Map<GameItem, Game>       PREPARING_GAMES = new ConcurrentHashMap<>();
    public static final Map<UUID, Game>           RUNNING_GAMES   = new ConcurrentHashMap<>();
    public static final Map<UUID, GameAttributes> CLIENT_GAMES    = new ConcurrentHashMap<>();

    /**
     * WARNING: do not use on network stuff!
     */
    public static Game getGame(UUID gameId) {
        return determineGame(gameId, true, true, false);
    }

    public static Game getGame(UUID gameId, boolean considerRunning, boolean considerPreparing) {
        return determineGame(gameId, considerRunning, considerPreparing, true);
    }

    public static void setGameRunning(UUID gameId, Game game) {
        for (GameItem i : PREPARING_GAMES.keySet()) {
            if (gameId.equals(i.getGameId())) {
                PREPARING_GAMES.remove(i);
                break;
            }
        }
        RUNNING_GAMES.put(gameId, game);
    }

    public static void removePreparingGame(UUID gameId) {
        for (GameItem i : PREPARING_GAMES.keySet()) {
            if (gameId.equals(i.getGameId())) {
                PREPARING_GAMES.remove(i);
                break;
            }
        }
    }

    private static Game determineGame(UUID gameId,
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
