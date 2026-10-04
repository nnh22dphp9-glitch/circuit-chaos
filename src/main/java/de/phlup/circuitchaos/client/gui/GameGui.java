package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.client.ClientToServerConnection;
import de.phlup.circuitchaos.client.service.AudioSupplier;
import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Board;
import de.phlup.circuitchaos.common.model.BoardElement;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.RevealProgrammeListItem;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import de.phlup.circuitchaos.common.settings.ClientSettings.Theme;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.GameAttributes;
import de.phlup.circuitchaos.server.game.GameOptions;
import de.phlup.circuitchaos.server.player.network.ProgramRobotAnswerWindow;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Getter
public class GameGui {

    private final String id = UUID.randomUUID().toString();

    private final ClientToServerConnection clientToServerConnection;
    private final GameAttributes           gameAttributes;
    private final ClientSettings           clientSettings;

    private String myRobotsName = null;

    private final AudioSupplier     audioSupplier;
    private final ImageSupplier     imageSupplier;
    private final ResourceLoader    resourceLoader;
    private final JFrame            mainFrame       = new JFrame();
    private       JFrame            lastRevealFrame = null;
    private final JPanel            revealPanel     = new JPanel(new FlowLayout());
    private final JCheckBoxMenuItem soundMenuItem   = new JCheckBoxMenuItem("Sound", true);

    private       Board       board;
    private final BoardJPanel boardJPanel;

    @Setter
    private boolean notStartedYet = true;
    private boolean gameEnded     = false;

    @Setter
    private       ClientBoardArranger          boardArranger = null;
    private final Map<Position, BufferedImage> images        = new HashMap<>();
    private       float                        zoomFactor;

    public GameGui(@NotNull ResourceLoader resourceLoader,
                   @NotNull AudioSupplier audioSupplier,
                   @NotNull ClientToServerConnection clientToServerConnection,
                   @NotNull GameAttributes gameAttributes,
                   @NotNull ClientSettings clientSettings) {
        this.resourceLoader = resourceLoader;
        this.clientSettings = clientSettings;
        this.audioSupplier = audioSupplier;
        Theme theme = determineTheme();
        this.audioSupplier.setThemePath(theme.getPath());
        this.imageSupplier = new ImageSupplier(theme, resourceLoader);
        boardJPanel = new BoardJPanel(imageSupplier);
        this.clientToServerConnection = clientToServerConnection;
        this.gameAttributes = gameAttributes;
        this.zoomFactor = clientSettings.getDefaultZoom();
        GlobalServerAttributes.CLIENT_GAMES.put(gameAttributes.getRegistration().getId(), gameAttributes);
        board = clientToServerConnection.getBoard(gameAttributes.getGameUrl());
        synchronized (boardJPanel) {
            boardJPanel.arrangeElements(this, Step.GIVE_UP, null, null, 1);
        }
        JScrollPane boardPane = new JScrollPane(boardJPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        boardPane.setPreferredSize(new Dimension(clientSettings.getDefaultBoardSizeX(), clientSettings.getDefaultBoardSizeY()));
        mainFrame.setTitle("Circuit Chaos Board - %s - Game has not been started yet - %s".formatted(gameAttributes.getRegistration().getGameName(), Step.SETUP.getName()));
        mainFrame.setContentPane(boardPane);
        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.setResizable(true);
        addMenuBar(mainFrame, clientSettings.isSound());
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                end();
            }
        });
        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.requestFocus();
    }

    private Theme determineTheme() {
        Collection<Theme> themes = clientSettings.getThemeCollection();
        if (themes.isEmpty()) {
            log.error("No theme found");
        }
        for (Theme t : themes) {
            if (t.getName().equals(clientSettings.getDefaultTheme())) {
                return t;
            }
        }
        //noinspection OptionalGetWithoutIsPresent
        Theme theme = themes.stream().findFirst().get();
        log.info("No default theme set - using '{}'", theme.getName());
        return theme;
    }

    private void addMenuBar(JFrame frame, boolean sound) {
        JMenuBar menuBar  = new JMenuBar();
        JMenu    gameMenu = new JMenu("Game");
        menuBar.add(gameMenu);
        // TODO build new help menu
        //  JMenu helpMenu = new JMenu("Help");
        //  menuBar.add(helpMenu);
        //  new HelpMenu(imageSupplier).createHelpMenu(helpMenu);
        createGameMenu(gameMenu, sound);
        frame.setJMenuBar(menuBar);
    }

    private void createGameMenu(JMenu gameMenu, boolean sound) {
        soundMenuItem.setSelected(sound);
        soundMenuItem.addActionListener(e -> {
            if (!soundMenuItem.isSelected()) {
                audioSupplier.stop();
            }
        });
        gameMenu.add(soundMenuItem);
        gameMenu.addSeparator();
        gameMenu.add(createZoomMenu());
        gameMenu.addSeparator();
        JMenuItem giveUpAndCloseMenuItem = new JMenuItem("Give up and Close");
        giveUpAndCloseMenuItem.addActionListener(e -> end());
        gameMenu.add(giveUpAndCloseMenuItem);
    }

    private JMenu createZoomMenu() {
        JMenu       zoom      = new JMenu("Zoom");
        ButtonGroup zoomGroup = new ButtonGroup();
        createZoomButton("25 %", zoomGroup, zoom);
        createZoomButton("50 %", zoomGroup, zoom);
        createZoomButton("75 %", zoomGroup, zoom);
        createZoomButton("100 %", zoomGroup, zoom);
        createZoomButton("125 %", zoomGroup, zoom);
        createZoomButton("150 %", zoomGroup, zoom);
        createZoomButton("175 %", zoomGroup, zoom);
        createZoomButton("200 %", zoomGroup, zoom);
        return zoom;
    }

    private void createZoomButton(String title, ButtonGroup zoomGroup, JMenuItem zoom) {
        JRadioButtonMenuItem zoomButton = new JRadioButtonMenuItem(title);
        if (title.equals(((int) (zoomFactor * 100)) + " %")) {
            zoomButton.setSelected(true);
        }
        zoomButton.addActionListener(this::zoomButtonAction);
        zoomGroup.add(zoomButton);
        zoom.add(zoomButton);
    }

    private void zoomButtonAction(ActionEvent e) {
        JRadioButtonMenuItem button    = (JRadioButtonMenuItem) e.getSource();
        float                newFactor = Float.parseFloat(button.getText().substring(0, button.getText().indexOf(" "))) / 100;
        if (newFactor != zoomFactor) {
            Graphics2D gr = (Graphics2D) boardJPanel.getGraphics();
            gr.setBackground(Color.black);
            gr.clearRect(0, 0, boardJPanel.getWidth(), boardJPanel.getHeight());
            gr.dispose();
            zoomFactor = newFactor;
            refreshBoard(board, "zoom changed", Step.SETUP, null, null, 1, null);
        }
    }

    public void end() {
        gameAttributes.setGameGui(null);
        GlobalServerAttributes.CLIENT_GAMES.remove(gameAttributes.getRegistration().getId());
        if (!gameEnded) {
            clientToServerConnection.deregister(gameAttributes.getGameUrl(), gameAttributes.getRegistration().getId());
        }
        mainFrame.dispose();
        if (boardArranger != null) {
            boardArranger.dispose();
            boardArranger = null;
        }
        gameHasEnded();
    }

    public void gameHasEnded() {
        gameEnded = true;
        if (lastRevealFrame != null) {
            lastRevealFrame.setVisible(false);
        }
    }

    public void refreshBoard(Board board, String reasonForBoardChange, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        this.board = board;
        if (getBoard().getFactoryFloor().isEmpty()) {
            Graphics2D gr = (Graphics2D) boardJPanel.getGraphics();
            gr.setBackground(Color.black);
            gr.clearRect(0, 0, boardJPanel.getWidth(), boardJPanel.getHeight());
            gr.dispose();
        }
        mainFrame.setTitle("Circuit Chaos Board - %s - %s - %s".formatted(gameAttributes.getRegistration().getGameName(),
                                                                          StringUtils.hasText(myRobotsName) ? myRobotsName : "Game has not been started yet",
                                                                          StringUtils.hasText(reasonForBoardChange) ? reasonForBoardChange : ""));
        synchronized (boardJPanel) {
            boardJPanel.arrangeElements(this, step, phase, subPhase, animationSteps);
            if (subPhase == null || gameEnded) {
                audioSupplier.stop();
            } else if (subPhase == 0) {
                boolean hover = step == Step.ROBOTS_AND_OBJECTS_MOVE && isHover(movingRobotName);
                playSound(step, board, hover);
            }
        }
        if (boardArranger != null) {
            boardArranger.getPlayButton().setEnabled(board.getCheckpoints().size() > 1);
        }
    }

    private boolean isHover(String movingRobotName) {
        return board.getRobots().stream()
                    .filter(r -> r.getName().equals(movingRobotName))
                    .filter(BoardElement::isOnBoard)
                    .anyMatch(BoardElement::isFlying);
    }

    private void playSound(Step step, Board board, boolean hover) {
        if (!soundMenuItem.isSelected()) {
            audioSupplier.stop();
            return;
        }
        switch (step) {
            case ROBOT_MOUNTED_LASER_FIRE, BOARD_MOUNTED_LASER_FIRE,
                 ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE, BOARD_MOUNTED_PRESSURE_BEAMS_FIRE,
                 ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE, BOARD_MOUNTED_TRACTOR_BEAMS_FIRE,
                 ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE, ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE,
                 ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE -> audioSupplier.playSound(AudioConstants.LASER_BEAM);
            case EXPRESS_CONVEYOR_BELTS_MOVE, CONVEYOR_BELTS_MOVE ->
                    audioSupplier.playSound(AudioConstants.CONVEYOR_BELT);
            case PUSHERS_PUSH -> audioSupplier.playSound(AudioConstants.PUSHER);
            case GEARS_ROTATE -> audioSupplier.playSound(AudioConstants.GEARS);
            case OPEN_TRAPDOORS -> {
                if (board.getProperties().isTrapdoor()) {
                    audioSupplier.playSound(AudioConstants.TRAPDOOR);
                }
            }
            case ROBOTS_AND_OBJECTS_MOVE -> {
                if (hover) {
                    audioSupplier.playSound(AudioConstants.ROBOTS_HOVER);
                } else {
                    audioSupplier.playSound(AudioConstants.ROBOTS_MOVE);
                }
            }
            default -> audioSupplier.stop();
        }
    }

    public void perform(String answerUrl, NetworkRequest request) {
        if (gameEnded) {
            return;
        }
        myRobotsName = request.getMyRobot().getName();
        mainFrame.setTitle("Circuit Chaos Board - %s - %s - Waiting for all to program their robots"
                                   .formatted(gameAttributes.getRegistration().getGameName(), myRobotsName));
        if (lastRevealFrame != null) {
            lastRevealFrame.setVisible(false);
        }
        new ProgramRobotAnswerWindow(gameAttributes, answerUrl, request).apply();
    }

    public void revealProgramme(RevealProgrammeResponse revealProgrammeResponse) {
        if (gameEnded) {
            return;
        }
        synchronized (revealPanel) {
            revealPanel.removeAll();
            revealPanel.setBackground(Color.WHITE);
            revealPanel.add(new JLabel(" Phase: %s ".formatted(revealProgrammeResponse.getPhase() + 1)));
            for (RevealProgrammeListItem item : revealProgrammeResponse.getProgramme()) {
                JPanel p = new JPanel(new BorderLayout());
                p.add(new JLabel(imageSupplier.getImageIconPlain(item.imageIconPath())), BorderLayout.NORTH);
                p.add(new JLabel(imageSupplier.getImageIcon(item.programme(), true, false)), BorderLayout.CENTER);
                p.setBackground(Color.WHITE);
                JPanel p2 = new JPanel(new BorderLayout());
                p2.add(new JLabel(item.name()), BorderLayout.NORTH);
                p2.add(new JLabel(item.programme().getType().getCommand() + " (" + item.programme().getPriority() + ")"), BorderLayout.SOUTH);
                p2.setBackground(Color.WHITE);
                p.add(p2, BorderLayout.SOUTH);
                revealPanel.add(p);
            }
            if (lastRevealFrame == null) {
                lastRevealFrame = new JFrame("Circuit Chaos - %s - %s".formatted(gameAttributes.getRegistration().getGameName(), myRobotsName));
                lastRevealFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
                lastRevealFrame.setLocationRelativeTo(mainFrame);
                lastRevealFrame.setLocation(lastRevealFrame.getLocation().x - mainFrame.getWidth() / 2 + 50,
                                            lastRevealFrame.getLocation().y + 120);
                lastRevealFrame.setContentPane(new JScrollPane(revealPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
            }
            lastRevealFrame.pack();
            lastRevealFrame.setVisible(true);
            lastRevealFrame.requestFocus();
        }
    }

    public BufferedImage getImageByFloor(Floor f) {
        return images.get(f.getPosition());
    }

    public void putImageOfFloor(Floor f, BufferedImage bi) {
        images.put(f.getPosition(), bi);
    }

    public void play(GameOptions gameOptions) {
        boardArranger = null;
        clientToServerConnection.play(gameAttributes.getGameUrl(), gameOptions);
    }

}
