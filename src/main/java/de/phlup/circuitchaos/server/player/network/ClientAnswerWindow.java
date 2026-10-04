package de.phlup.circuitchaos.server.player.network;

import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.NetworkResponse;
import de.phlup.circuitchaos.server.game.GameAttributes;
import lombok.Data;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@Data
public abstract class ClientAnswerWindow {

    protected final String          answerUrl;
    protected final NetworkRequest  request;
    protected final NetworkResponse response = new NetworkResponse();
    protected final JFrame          mainFrame;

    protected GameAttributes gameAttributes;

    public ClientAnswerWindow(GameAttributes gameAttributes, String answerUrl, NetworkRequest request, String title) {
        this.answerUrl = answerUrl;
        this.request = request;
        this.gameAttributes = gameAttributes;
        response.setFilled(true);
        mainFrame = new JFrame("Circuit Chaos - %s - %s - %s".formatted(gameAttributes.getRegistration().getGameName(), request.getMyRobot().getName(), title));
        mainFrame.setVisible(false);
        JFrame courseFrame = gameAttributes.getGameGui().getMainFrame();
        mainFrame.setLocationRelativeTo(courseFrame);
        mainFrame.setLocation(mainFrame.getLocation().x - courseFrame.getWidth() / 2 + 10,
                              mainFrame.getLocation().y + 120);
    }

    public void apply() {
        JScrollPane contentPane = new JScrollPane(createContentPane(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainFrame.setContentPane(contentPane);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                sendAnswer();
            }
        });
        mainFrame.setResizable(true);
        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.requestFocus();
    }

    protected abstract JPanel createContentPane();

    public void sendAnswer() {
        if (gameAttributes != null) {
            gameAttributes.getGameGui().getClientToServerConnection().postAnswer(answerUrl, response);
        }
        mainFrame.dispose();
        gameAttributes = null;
    }

}
