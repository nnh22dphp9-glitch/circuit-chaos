package de.phlup.circuitchaos.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
public class GameItem {

    private String gameName;
    private String gameId;
    private String errorMessage;

    public GameItem(String gameName, String gameId) {
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
                : gameId;
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
