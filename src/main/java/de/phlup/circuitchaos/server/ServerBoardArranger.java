package de.phlup.circuitchaos.server;

import de.phlup.circuitchaos.board.BoardHandler;
import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Board;
import de.phlup.circuitchaos.common.model.Checkpoint;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.server.game.Game;

import java.util.Comparator;
import java.util.List;

public class ServerBoardArranger {

    public void setBoard(Game game, Board board) {
        BoardHandler.replaceBoardOnGameBoard(board, game.getBoard());
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "set board", null);
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
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "checkpoint added", null);
    }

    public void removeCheckpoint(Game game, Position position) {
        Checkpoint checkpoint = BoardHandler.getCheckpoint(game.getBoard(), position);
        if (checkpoint == null) {
            throw new CircuitChaosException("Checkpoint not found.");
        }
        List<Checkpoint> checkpoints = game.getBoard().getCheckpoints();
        checkpoints.remove(checkpoint);
        adjustCheckpointNumbers(checkpoints);
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "checkpoint removed", null);
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
