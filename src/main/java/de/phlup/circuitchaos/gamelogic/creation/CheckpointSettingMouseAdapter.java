package de.phlup.circuitchaos.gamelogic.creation;

import de.phlup.circuitchaos.enums.Floortype;
import de.phlup.circuitchaos.gamelogic.GameGui;
import de.phlup.circuitchaos.gamelogic.utils.BoardHandler;
import de.phlup.circuitchaos.model.Checkpoint;
import de.phlup.circuitchaos.model.Floor;
import lombok.RequiredArgsConstructor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@RequiredArgsConstructor
public class CheckpointSettingMouseAdapter extends MouseAdapter {

    private final GameGui game;
    private final Floor   floor;

    @Override
    public void mouseReleased(MouseEvent e) {
        if (game == null) {
            return;
        }
        Checkpoint checkpoint = BoardHandler.getCheckpoint(game.getBoard(), floor.getPosition());
        if (checkpoint != null) {
            handleMouseEventOnCheckpoint();
        } else {
            handleMouseEventOnFloor();
        }
    }

    private void handleMouseEventOnCheckpoint() {
        if (game != null && game.isNotStartedYet()) {
            game.getClientToServerConnection().removeCheckpoint(game.getGameAttributes().getGameUrl(), floor.getPosition());
        }
    }

    private void handleMouseEventOnFloor() {
        assert floor != null;
        if (game != null && game.isNotStartedYet()) {
            Floortype floortype = floor.getFloortype();
            if (floortype == Floortype.OPEN_FLOOR || floortype == Floortype.PIT_STOP) {
                game.getClientToServerConnection().addCheckpoint(game.getGameAttributes().getGameUrl(), floor.getPosition());
            }
        }
    }

}
