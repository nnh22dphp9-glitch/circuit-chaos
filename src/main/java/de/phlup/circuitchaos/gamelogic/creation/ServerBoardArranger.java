package de.phlup.circuitchaos.gamelogic.creation;

import de.phlup.circuitchaos.enums.Step;
import de.phlup.circuitchaos.exception.CircuitChaosException;
import de.phlup.circuitchaos.gamelogic.Game;
import de.phlup.circuitchaos.gamelogic.utils.BoardHandler;
import de.phlup.circuitchaos.model.BoardPosition;
import de.phlup.circuitchaos.model.Checkpoint;
import de.phlup.circuitchaos.model.Position;

import java.util.Comparator;
import java.util.List;

public class ServerBoardArranger {

    public void putOnBoard(Game game, BoardPosition boardPosition) {
        BoardHandler.addBoardToGameBoard(game.getBoard(), boardPosition.board(), boardPosition.posX(), boardPosition.posY(), game, this);
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "added board");
    }

    public void removeFromBoard(Game game, int posx, int posy) {
        BoardHandler.removeBoardFromGameBoard(game.getBoard(), posx, posy, this);
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "removed board");
    }

    public void addCheckpoint(Game game, Position position) {
        Checkpoint checkpoint = BoardHandler.getCheckpoint(game.getBoard(), position);
        if (checkpoint != null) {
            throw new CircuitChaosException("Checkpoint already on position.");
        }
        List<Checkpoint> checkpoints = game.getBoard().getCheckpoints();
        int numberOfNewCheckpoint = checkpoints.isEmpty()
                ? 0
                : checkpoints.stream()
                             .max(Comparator.comparingInt(Checkpoint::getNumber))
                             .orElseThrow().getNumber() + 1;
        checkpoint = new Checkpoint();
        checkpoint.setNumber(numberOfNewCheckpoint);
        checkpoint.setPosition(position);
        checkpoints.add(checkpoint);
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "checkpoint added");
    }

    public void removeCheckpoint(Game game, Position position) {
        Checkpoint checkpoint = BoardHandler.getCheckpoint(game.getBoard(), position);
        if (checkpoint == null) {
            throw new CircuitChaosException("Checkpoint not found.");
        }
        List<Checkpoint> checkpoints = game.getBoard().getCheckpoints();
        checkpoints.remove(checkpoint);
        adjustCheckpointNumbers(checkpoints);
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "checkpoint removed");
    }

    public void adjustCheckpointNumbers(List<Checkpoint> checkpoints) {
        if (!checkpoints.isEmpty()) {
            int              number      = 0;
            List<Checkpoint> orderedList = checkpoints.stream().sorted(Comparator.comparingInt(Checkpoint::getNumber)).toList();
            for (Checkpoint cp : orderedList) {
                cp.setNumber(number++);
            }
        }
    }

}
