package de.phlup.circuitchaos.server.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class GameOptions {

    private List<String> computerType   = new ArrayList<>();
    private List<String> defaultModules = new ArrayList<>();

    @JsonIgnore
    public int getMaxNumberOfComputerPlayers() {
        return computerType.size();
    }

}
