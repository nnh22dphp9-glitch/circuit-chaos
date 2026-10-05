package de.phlup.circuitchaos.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.client.ClientToServerConnection;
import de.phlup.circuitchaos.client.gui.ClientCourseArranger;
import de.phlup.circuitchaos.client.gui.GameGui;
import de.phlup.circuitchaos.client.gui.GuiHelper;
import de.phlup.circuitchaos.client.gui.PictureConstants;
import de.phlup.circuitchaos.client.gui.editor.EditorGui;
import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.model.CourseInfo;
import de.phlup.circuitchaos.common.model.GameItem;
import de.phlup.circuitchaos.common.model.Registration;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import de.phlup.circuitchaos.common.settings.GameSettings;
import de.phlup.circuitchaos.server.game.GameAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Profile("!headless")
@RequiredArgsConstructor
public class CircuitChaosGui implements GuiHelper {

    private static final String TEXT_NO_GAME_FOUND = "  ---   no preparing game found on server   ---  ";

    public static String lastRequestedNewGame;

    private final ResourceLoader           resourceLoader;
    private final ClientSettings           clientSettings;
    private final ClientToServerConnection clientToServerConnection;
    private final GameSettings             gameSettings;
    private final AudioSupplier            audioSupplier;
    private final CourseLoader             courseLoader;
    private final ObjectMapper             objectMapper;


    private ImageSupplier          imageSupplier;
    private JTextArea              serverAddressInput;
    private JComboBox<GameItem>    gameChooser;
    private JButton                watchButton;
    private JButton                playButton;
    private Collection<CourseInfo> courses;

    @SuppressWarnings("unused")
    @EventListener(ApplicationReadyEvent.class)
    public void initClientGUI() {
        ClientSettings.Theme theme = determineTheme();
        this.imageSupplier = new ImageSupplier(theme, resourceLoader);
        courses = courseLoader.loadCourses(imageSupplier.getImageIconPlain(PictureConstants.GFX_PICTURE).getImage());
        JFrame frame = new JFrame("Circuit Chaos - " + clientSettings.getOwnUrl());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(getStartPanel());
        frame.setResizable(false);
        addMenuBar(frame);
        frame.pack();
        frame.setVisible(true);
        refreshServerGames();
    }

    private ClientSettings.Theme determineTheme() {
        Collection<ClientSettings.Theme> themes = clientSettings.getThemeCollection();
        if (themes.isEmpty()) {
            log.error("No theme found");
        }
        for (ClientSettings.Theme t : themes) {
            if (t.getName().equals(clientSettings.getDefaultTheme())) {
                return t;
            }
        }
        //noinspection OptionalGetWithoutIsPresent
        ClientSettings.Theme theme = themes.stream().findFirst().get();
        log.info("No default theme set - using '{}'", theme.getName());
        return theme;
    }

    private void addMenuBar(JFrame frame) {
        JMenuBar menuBar    = new JMenuBar();
        JMenu    editorMenu = new JMenu("Editor");
        menuBar.add(editorMenu);
        JMenuItem showEditor = new JMenuItem("Show editor");
        showEditor.setEnabled(true);
        showEditor.addActionListener(e -> new EditorGui(resourceLoader, clientSettings, courseLoader).open());
        editorMenu.add(showEditor);
        // TODO build new help menu
        //        JMenu    helpMenu = new JMenu("Help");
        //        menuBar.add(helpMenu);
        //        new HelpMenu(imageSupplier).createHelpMenu(helpMenu);
        frame.setJMenuBar(menuBar);
    }

    private Container getStartPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        JPanel        mainPanel     = new JPanel(gridBagLayout);
        JCheckBox     pollingBox    = new JCheckBox(" polling ");
        addComponentToPanel(createNewGamePanel(pollingBox), mainPanel, gridBagLayout, 1, 1, 1, GridBagConstraints.WEST);
        addComponentToPanel(new JLabel(" "), mainPanel, gridBagLayout, 1, 2, 1, GridBagConstraints.WEST);
        addComponentToPanel(createConnectToPreparingGamePanel(pollingBox), mainPanel, gridBagLayout, 1, 3, 1, GridBagConstraints.WEST);
        addComponentToPanel(new JLabel(" "), mainPanel, gridBagLayout, 1, 4, 1, GridBagConstraints.WEST);
        addComponentToPanel(new JLabel(" "), mainPanel, gridBagLayout, 1, 5, 1, GridBagConstraints.WEST);

        JPanel headerPanel = getHeaderPanel();
        headerPanel.add(mainPanel, BorderLayout.SOUTH);
        return getSidePanel(headerPanel);
    }

    private JPanel createConnectToPreparingGamePanel(JCheckBox pollingBox) {
        JPanel connectPanel = new JPanel(new BorderLayout());
        connectPanel.setBorder(new TitledBorder(new LineBorder(Color.black), " Connect to a preparing game "));
        connectPanel.add(createServerRow(), BorderLayout.NORTH);
        connectPanel.add(createGameChooserRow(), BorderLayout.CENTER);
        watchButton = new JButton("Watch");
        watchButton.addActionListener(e -> playOrWatch(true, pollingBox.isSelected()));
        watchButton.setEnabled(false);
        playButton = new JButton("Play");
        playButton.addActionListener(e -> playOrWatch(false, pollingBox.isSelected()));
        playButton.setEnabled(false);
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(pollingBox);
        panel.add(watchButton);
        panel.add(playButton);
        connectPanel.add(panel, BorderLayout.SOUTH);
        return connectPanel;
    }

    private JPanel createNewGamePanel(JCheckBox pollingBox) {
        JPanel newGamePanel = new JPanel(new BorderLayout());
        newGamePanel.setBorder(new TitledBorder(new LineBorder(Color.black), " Create a new game "));
        JPanel newGameNamePanel = new JPanel(new BorderLayout());
        newGameNamePanel.add(new JLabel(" Name:   "), BorderLayout.WEST);
        JTextArea nameOfNewGame = new JTextArea("new game", 1, 50);
        nameOfNewGame.setLineWrap(false);
        newGameNamePanel.add(nameOfNewGame, BorderLayout.EAST);
        newGameNamePanel.add(new JLabel(" "), BorderLayout.SOUTH);
        newGamePanel.add(newGameNamePanel, BorderLayout.WEST);
        JButton newGameButton = new JButton("Create");
        newGameButton.addActionListener(e -> startNewGame(nameOfNewGame.getText(), pollingBox.isSelected()));
        newGameButton.setEnabled(true);
        newGamePanel.add(newGameButton, BorderLayout.EAST);
        return newGamePanel;
    }

    private JPanel getSidePanel(JPanel content) {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.NORTH);
        sidePanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.WEST);
        sidePanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.EAST);
        sidePanel.add(content, BorderLayout.CENTER);
        return sidePanel;
    }

    private JPanel getHeaderPanel() {
        JLabel headerImageLabel = new JLabel(createNewHeaderImageIcon());
        JPanel borderPanel      = new JPanel(new BorderLayout());
        borderPanel.add(headerImageLabel, BorderLayout.CENTER);
        return borderPanel;
    }

    private ImageIcon createNewHeaderImageIcon() {
        String        logoFile = PictureConstants.GFX_LOGO;
        BufferedImage bi       = new BufferedImage(450, 225, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr       = bi.createGraphics();
        gr.drawImage(imageSupplier.getImageIconPlain(logoFile).getImage(), 0, 0, null);
        gr.dispose();
        return new ImageIcon(bi);
    }

    private void startNewGame(String gameName, boolean pull) {
        GameAttributes gameAttributes = null;
        try {
            lastRequestedNewGame = UUID.randomUUID().toString();

            Registration clientRegistration = new Registration();
            clientRegistration.setGameName(gameName);
            clientRegistration.setId(lastRequestedNewGame);
            clientRegistration.setUrl(clientSettings.getOwnUrl() + "/client/" + clientRegistration.getId());
            clientRegistration.setWatchOnly(false);
            clientRegistration.setPull(pull);

            String gameUrl = serverAddressInput.getText().trim() + "/game";
            String gameId  = clientToServerConnection.createGame(gameUrl, clientRegistration);

            gameAttributes = new GameAttributes(gameId, clientRegistration, gameUrl + "/" + gameId, false);

            GameGui gui = new GameGui(resourceLoader, audioSupplier, clientToServerConnection, gameAttributes, clientSettings);
            gameAttributes.setGameGui(gui);
            gui.setCourseArranger(new ClientCourseArranger(gui, gameSettings, courses, objectMapper, clientSettings));
        } catch (Exception exc) {
            log.error("Exception occurred - shutting game down", exc);
            if (gameAttributes != null) {
                GameGui gameGui = gameAttributes.getGameGui();
                if (gameGui != null) {
                    gameGui.end();
                }
            }
        }
    }

    private void playOrWatch(boolean watchOnly, boolean pull) {
        GameItem selectedGame = (GameItem) gameChooser.getSelectedItem();
        if (selectedGame == null || StringUtils.hasText(selectedGame.getErrorMessage())) {
            throw new CircuitChaosException("no game selected");
        }
        GameAttributes gameAttributes = null;
        try {
            Registration clientRegistration = new Registration();
            clientRegistration.setId(UUID.randomUUID().toString());
            if (!pull) {
                clientRegistration.setUrl(clientSettings.getOwnUrl() + "/client/" + clientRegistration.getId());
            }
            clientRegistration.setWatchOnly(watchOnly);
            clientRegistration.setGameName(selectedGame.getGameName());
            clientRegistration.setPull(pull);

            String gameId  = selectedGame.getGameId();
            String gameUrl = serverAddressInput.getText().trim() + "/game/" + gameId;
            clientToServerConnection.registerAsWatcher(gameUrl, clientRegistration);

            gameAttributes = new GameAttributes(gameId, clientRegistration, gameUrl, watchOnly);
            GameGui gameGui = new GameGui(resourceLoader, audioSupplier, clientToServerConnection, gameAttributes, clientSettings);
            gameAttributes.setGameGui(gameGui);
        } catch (Exception exc) {
            log.error("Exception occurred - shutting game down", exc);
            if (gameAttributes != null) {
                GameGui gameGui = gameAttributes.getGameGui();
                if (gameGui != null) {
                    gameGui.end();
                }
            }
        }
    }

    private JPanel createGameChooserRow() {
        JPanel gameRow   = new JPanel(new BorderLayout());
        JLabel gameLabel = new JLabel("   Game:   ");
        gameRow.add(gameLabel, BorderLayout.WEST);
        gameChooser = new JComboBox<>();
        gameChooser.setEditable(false);
        gameChooser.addItem(new GameItem(TEXT_NO_GAME_FOUND));
        gameRow.add(gameChooser, BorderLayout.CENTER);
        return gameRow;
    }

    private JPanel createServerRow() {
        JPanel serverRow   = new JPanel(new FlowLayout());
        JLabel serverLabel = new JLabel("Server:   ");
        serverRow.add(serverLabel);
        serverAddressInput = new JTextArea(clientSettings.getServerUrl(), 1, 50);
        serverRow.add(serverAddressInput);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshServerGames());
        serverRow.add(refreshButton);
        return serverRow;
    }

    private void refreshServerGames() {
        String serverAddress = serverAddressInput.getText();
        try {
            Set<GameItem> allPreparingGames = clientToServerConnection.getAllPreparingGames(serverAddress);
            gameChooser.removeAllItems();
            if (allPreparingGames.isEmpty()) {
                gameChooser.addItem(new GameItem(TEXT_NO_GAME_FOUND));
                watchButton.setEnabled(false);
                playButton.setEnabled(false);
            } else {
                for (GameItem game : allPreparingGames) {
                    gameChooser.addItem(game);
                }
                watchButton.setEnabled(true);
                playButton.setEnabled(true);
            }
        } catch (Exception exc) {
            gameChooser.removeAllItems();
            gameChooser.addItem(new GameItem("ERROR: %s - %s".formatted(exc.getClass().getSimpleName(), exc.getMessage())));
            log.info(exc.getMessage(), exc);
            watchButton.setEnabled(false);
            playButton.setEnabled(false);
        }
    }

}
