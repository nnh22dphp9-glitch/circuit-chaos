package de.phlup.circuitchaos.service;

import de.phlup.circuitchaos.gamelogic.GameAttributes;
import de.phlup.circuitchaos.gamelogic.GameGui;
import de.phlup.circuitchaos.gamelogic.GlobalServerAttributes;
import de.phlup.circuitchaos.model.NetworkRequest;
import de.phlup.circuitchaos.model.RevealProgrammeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.swing.JOptionPane;
import java.awt.Component;

import static de.phlup.circuitchaos.gamelogic.Game.AND_THE_WINNER_IS;
import static de.phlup.circuitchaos.gamelogic.Game.THIS_GAME_IS_A_TIE;

@Slf4j
@Service
public class AsyncService {

    // Nicht Async - Animationen könnten kaputtgehen
    public void refreshBoard(GameGui gameGui, NetworkRequest request) {
        if (request.getSubPhase() == null || request.getSubPhase() == 0) {
            log.info("{}: {} ({})", gameGui.getGameAttributes().getGameId(), request.getReasonForBoardChange(), request.getStep());
        }
        gameGui.refreshBoard(request.getBoard(), request.getReasonForBoardChange(), request.getStep(), request.getPhase(), request.getSubPhase(), request.getAnimationSteps());
    }

    @Async
    public void perform(GameGui gameGui, String answerUrl, NetworkRequest request) {
        gameGui.perform(answerUrl, request);
    }

    @Async
    public void showMessage(String registrationId, String message) {
        Component      mainFrame      = null;
        GameAttributes gameAttributes = GlobalServerAttributes.CLIENT_GAMES.get(registrationId);
        if (gameAttributes != null) {
            GameGui game = gameAttributes.getGameGui();
            if (game != null) {
                mainFrame = game.getMainFrame();
                if (message.startsWith(AND_THE_WINNER_IS) || THIS_GAME_IS_A_TIE.equals(message)) {
                    game.gameHasEnded();
                }
            }
        }
        JOptionPane.showMessageDialog(mainFrame, message);
    }

    @Async
    public void revealProgramme(GameGui game, RevealProgrammeResponse revealProgrammeResponse) {
        game.revealProgramme(revealProgrammeResponse);
    }

}
