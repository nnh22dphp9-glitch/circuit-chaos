package de.phlup.circuitchaos.gamelogic;

import de.phlup.circuitchaos.model.Registration;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class GameAttributes {

    private final String       gameId;
    private final Registration registration;
    private final String       gameUrl;
    private final boolean      watchOnly;

    private GameGui gameGui;

}
