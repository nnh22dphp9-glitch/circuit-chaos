package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.client.gui.GameGui;
import de.phlup.circuitchaos.common.model.Registration;
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
