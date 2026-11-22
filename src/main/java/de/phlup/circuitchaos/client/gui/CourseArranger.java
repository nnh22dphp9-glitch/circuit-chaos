package de.phlup.circuitchaos.client.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.client.service.CourseLoader;
import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.enums.Goal;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.model.CourseInfo;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import de.phlup.circuitchaos.common.settings.GameSettings;
import de.phlup.circuitchaos.server.game.GameOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
public class CourseArranger implements GuiHelper {

    private static final int GLOBAL_MAX_COMPUTER_PLAYERS = 20;

    private final ImageSupplier imageSupplier;
    private final JFrame        frame = new JFrame("CC");

    private final JComboBox<CourseInfo> courseList      = new JComboBox<>();
    private final JComboBox<Goal>       goal            = new JComboBox<>();
    private final JComboBox<Integer>    computerPlayers = new JComboBox<>();

    @Getter
    private final JButton playButton   = new JButton("Play");
    private final JPanel  chooserPanel = new JPanel(new BorderLayout());
    private final JLabel  regPlayers   = new JLabel(" 1 ");
    private final JLabel  regWatchers  = new JLabel(" 0 ");

    private final Collection<CourseInfo> courseInfos;
    private final ObjectMapper           objectMapper;
    private final ClientSettings         clientSettings;


    public CourseArranger(GameGui game, GameSettings gameSettings, CourseLoader courseLoader, ObjectMapper objectMapper, ClientSettings clientSettings) {
        imageSupplier = game.getImageSupplier();
        courseInfos = courseLoader.loadCourses();
        this.objectMapper = objectMapper;
        this.clientSettings = clientSettings;

        JPanel borderPanel = new JPanel(new BorderLayout());
        JPanel mainPanel   = new JPanel(new BorderLayout());
        mainPanel.add(createCourseChooser(game), BorderLayout.NORTH);
        JPanel centerPanel = new JPanel(new BorderLayout());
        mainPanel.add(centerPanel);
        centerPanel.add(createGoalChooser(), BorderLayout.NORTH);
        centerPanel.add(createComputerPlayerChooser(gameSettings.getDefaultNumberOfComputerPlayers()), BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(createPlayersDisplay(), BorderLayout.SOUTH);

        borderPanel.add(mainPanel, BorderLayout.CENTER);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.SOUTH);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 5, BufferedImage.TYPE_INT_ARGB))), BorderLayout.WEST);

        JPanel defaultModulePanel = new JPanel(new BorderLayout());
        defaultModulePanel.add(new JLabel("Default Modules:"), BorderLayout.NORTH);
        JPanel defaultModulePanel2 = new JPanel(new BorderLayout());
        defaultModulePanel2.add(createDefaultModulePanel(clientSettings, true, false), BorderLayout.NORTH);
        defaultModulePanel2.add(createDefaultModulePanel(clientSettings, false, true), BorderLayout.CENTER);
        defaultModulePanel.add(new JLabel("  "), BorderLayout.WEST);
        defaultModulePanel.add(defaultModulePanel2, BorderLayout.CENTER);
        defaultModulePanel.add(createDefaultModulePanel(clientSettings, false, false), BorderLayout.EAST);
        borderPanel.add(defaultModulePanel, BorderLayout.EAST);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (game.getState() == CourseState.GAME_INIT) {
                    game.getMainFrame().dispose();
                }
            }
        });
        frame.setContentPane(borderPanel);
        frame.setResizable(false);
        frame.setLocationRelativeTo(game.getMainFrame());
        frame.setLocation(frame.getX() + game.getMainFrame().getWidth() / 2,
                          frame.getY() - game.getMainFrame().getHeight() / 2 + 20);
        frame.pack();
        frame.setVisible(true);
        frame.requestFocus();
    }

    private static JPanel createDefaultModulePanel(ClientSettings clientSettings, boolean isWeapon, boolean isDispenser) {
        JPanel defaultModulesPanel = new JPanel();
        defaultModulesPanel.setLayout(new BoxLayout(defaultModulesPanel, BoxLayout.Y_AXIS));
        defaultModulesPanel.add(new JLabel(" "));
        if (isWeapon) {
            defaultModulesPanel.add(new JLabel("Weapons:"));
        } else if (isDispenser) {
            defaultModulesPanel.add(new JLabel("Dispensers:"));
        } else {
            defaultModulesPanel.add(new JLabel("Other Modules:"));
        }
        Stream.of(ModuleType.values())
              .filter(mt -> isWeapon == mt.isWeapon())
              .filter(mt -> isDispenser == (mt.getAmmunition() > 0 && mt != ModuleType.EXTRA_PROGRAM && mt != ModuleType.LAYERED_ARMOR))
              .sorted(Comparator.comparing(ModuleType::getModuleName))
              .forEach(mt -> {
                  JCheckBox moduleCheckBox = new JCheckBox(mt.getModuleName(), clientSettings.getDefaultModules().contains(mt.name()));
                  defaultModulesPanel.add(moduleCheckBox);
                  moduleCheckBox.addActionListener(actionEvent -> {
                      if (!moduleCheckBox.isSelected()) {
                          clientSettings.getDefaultModules().remove(mt.name());
                      } else if (!clientSettings.getDefaultModules().contains(mt.name())) {
                          clientSettings.getDefaultModules().add(mt.name());
                      }
                  });
              });
        return defaultModulesPanel;
    }

    private JPanel createGoalChooser() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(" "), BorderLayout.NORTH);
        for (Goal g : Goal.values()) {
            goal.addItem(g);
        }
        panel.add(goal, BorderLayout.CENTER);
        panel.add(new JLabel(" "), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createComputerPlayerChooser(int numberOfComputerPlayers) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Number of computer players:"), BorderLayout.CENTER);
        for (int i = 0; i <= GLOBAL_MAX_COMPUTER_PLAYERS; i++) {
            computerPlayers.addItem(i);
        }
        computerPlayers.setSelectedItem(numberOfComputerPlayers);
        panel.add(computerPlayers, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createPlayersDisplay() {
        GridBagLayout gbl   = new GridBagLayout();
        JPanel        panel = new JPanel(gbl);
        addComponentToPanel(new JLabel(" "), panel, gbl, 1, 1, 1, GridBagConstraints.WEST);
        addComponentToPanel(new JLabel(" registered Players:     "), panel, gbl, 1, 2, 1, GridBagConstraints.WEST);
        addComponentToPanel(regPlayers, panel, gbl, 2, 2, 1, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(" registered Watchers:    "), panel, gbl, 1, 3, 1, GridBagConstraints.WEST);
        addComponentToPanel(regWatchers, panel, gbl, 2, 3, 1, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(" "), panel, gbl, 1, 4, 1, GridBagConstraints.WEST);
        return panel;
    }

    private JPanel createCourseChooser(GameGui game) {
        JPanel courseChooser = new JPanel(new BorderLayout());
        courseList.setEditable(false);
        CourseInfo item = new CourseInfo("Choose a course!");
        courseList.addItem(item);
        for (CourseInfo bi : courseInfos) {
            courseList.addItem(bi);
        }
        courseList.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (courseList.getSelectedIndex() > 0) {
                    CourseInfo selectedItem = (CourseInfo) courseList.getSelectedItem();
                    if (selectedItem != null && selectedItem.getCourse() != null) {
                        Course course = copy(selectedItem.getCourse());
                        game.getClientToServerConnection().setCourse(game.getGameAttributes().getGameUrl(), course);
                    }
                }
            }
        });
        courseChooser.add(courseList, BorderLayout.CENTER);

        playButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.setState(CourseState.GAME_RUNNING);
                frame.dispose();
                GameOptions gameOptions = new GameOptions();
                gameOptions.setGoal((Goal) goal.getSelectedItem());
                if (computerPlayers.getSelectedItem() != null) {
                    gameOptions.setNumberOfComputerPlayers((Integer) computerPlayers.getSelectedItem());
                } else {
                    gameOptions.setNumberOfComputerPlayers(0);
                }
                gameOptions.setDefaultModules(clientSettings.getDefaultModules());
                game.play(gameOptions);
            }
        });
        playButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(new JLabel(" "), BorderLayout.CENTER);
        buttonPanel.add(playButton, BorderLayout.SOUTH);
        courseChooser.add(buttonPanel, BorderLayout.SOUTH);

        BufferedImage bi = new BufferedImage(150, 231, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr = bi.createGraphics();
        gr.drawImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_LOGO_SMALL).getImage(), 0, 0, null);
        gr.translate(0, 81);
        gr.drawImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_PICTURE).getImage(), 0, 0, null);
        JLabel coursePreview = new JLabel(imageSupplier.getImageIconPlain(bi));
        chooserPanel.add(coursePreview, BorderLayout.NORTH);
        chooserPanel.add(new JLabel(" "), BorderLayout.CENTER);
        chooserPanel.add(courseChooser, BorderLayout.SOUTH);

        return chooserPanel;
    }

    public Course copy(Course course) {
        if (course == null) {
            return null;
        }
        return objectMapper.convertValue(course, Course.class);
    }

    public void updatePlayersAndWatchers(int players, int watchers) {
        regPlayers.setText(" " + players + " ");
        regWatchers.setText(" " + watchers + " ");
    }

    public void dispose() {
        frame.dispose();
    }

}
