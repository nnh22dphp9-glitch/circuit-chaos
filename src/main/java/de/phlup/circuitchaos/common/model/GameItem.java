package de.phlup.circuitchaos.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Data
@NoArgsConstructor
public class GameItem {

    private String gameName;
    private UUID   gameId;
    private String errorMessage;

    public GameItem(String gameName, UUID gameId) {
        this.gameName = gameName;
        this.gameId = gameId;
        errorMessage = null;
    }

    public GameItem(String errorMessage) {
        gameName = null;
        gameId = null;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return StringUtils.hasText(errorMessage) ? errorMessage
                : StringUtils.hasText(gameName) ? gameId + " - " + gameName
                : gameId.toString();
    }

    @Override
    public boolean equals(Object o) {
        return gameId != null && o instanceof GameItem i && gameId.equals(i.getGameId());
    }

    @Override
    public int hashCode() {
        return gameId.hashCode();
    }

}
