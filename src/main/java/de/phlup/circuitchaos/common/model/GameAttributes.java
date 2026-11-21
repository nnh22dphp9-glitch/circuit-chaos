package de.phlup.circuitchaos.common.model;

import de.phlup.circuitchaos.client.gui.GameGui;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@RequiredArgsConstructor
public class GameAttributes {

    private final UUID         gameId;
    private final Registration registration;
    private final String       gameUrl;
    private final boolean      watchOnly;

    private GameGui gameGui;

}
