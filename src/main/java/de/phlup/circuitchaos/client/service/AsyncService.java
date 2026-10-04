package de.phlup.circuitchaos.client.service;

import de.phlup.circuitchaos.client.gui.GameGui;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.GameAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.swing.JOptionPane;
import java.awt.Component;

import static de.phlup.circuitchaos.server.game.Game.AND_THE_WINNER_IS;
import static de.phlup.circuitchaos.server.game.Game.THIS_GAME_IS_A_TIE;

@Slf4j
@Service
public class AsyncService {

    // Nicht Async - Animationen könnten kaputtgehen
    public void refreshCourse(GameGui gameGui, NetworkRequest request) {
        if (request.getSubPhase() == null || request.getSubPhase() == 0) {
            log.info("{}: {} ({})", gameGui.getGameAttributes().getGameId(), request.getReasonForCourseChange(), request.getStep());
        }
        gameGui.refreshCourse(request.getCourse(), request.getReasonForCourseChange(), request.getStep(), request.getPhase(),
                              request.getSubPhase(), request.getAnimationSteps(), request.getMovingRobotName());
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
