package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.client.ClientToServerConnection;
import de.phlup.circuitchaos.client.service.AudioSupplier;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.GameAttributes;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.CourseElement;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.RevealProgrammeListItem;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.GameOptions;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.UUID;

import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_EXPLOSION;

@Slf4j
@Getter
public class GameGui extends BaseGui implements GuiHelper {

    private final String id = UUID.randomUUID().toString();

    private final ClientToServerConnection clientToServerConnection;
    private final GameAttributes           gameAttributes;

    private String myRobotsName = null;

    private       JFrame            lastRevealFrame = null;
    private final JPanel            revealPanel     = new JPanel(new FlowLayout());
    private final JCheckBoxMenuItem soundMenuItem   = new JCheckBoxMenuItem("Sound", true);

    @Setter
    private CourseArranger courseArranger = null;

    public GameGui(@NotNull ResourceLoader resourceLoader,
                   @NotNull AudioSupplier audioSupplier,
                   @NotNull ClientToServerConnection clientToServerConnection,
                   @NotNull GameAttributes gameAttributes,
                   @NotNull ClientSettings clientSettings) {
        super(resourceLoader, clientSettings, audioSupplier);

        this.clientToServerConnection = clientToServerConnection;
        this.gameAttributes = gameAttributes;
        GlobalServerAttributes.CLIENT_GAMES.put(gameAttributes.getRegistration().getId(), gameAttributes);
        course = clientToServerConnection.getCourse(gameAttributes.getGameUrl());
        synchronized (courseJPanel) {
            courseJPanel.arrangeElements(this, Step.GIVE_UP, null, null, 1, -1);
        }
        addMenuBar(mainFrame, clientSettings.isSound());
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                end();
            }
        });
        configureMainFrame("Circuit Chaos Course - %s - Game has not been started yet - %s"
                                   .formatted(gameAttributes.getRegistration().getGameName(), Step.SETUP.getName()));
        state = CourseState.GAME_INIT;
    }

    private void addMenuBar(JFrame frame, boolean sound) {
        JMenuBar menuBar  = new JMenuBar();
        JMenu    gameMenu = new JMenu("Game");
        menuBar.add(gameMenu);
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

    public void end() {
        gameAttributes.setGameGui(null);
        GlobalServerAttributes.CLIENT_GAMES.remove(gameAttributes.getRegistration().getId());
        if (state != CourseState.GAME_ENDED) {
            clientToServerConnection.deregister(gameAttributes.getGameUrl(), gameAttributes.getRegistration().getId());
        }
        mainFrame.dispose();
        if (courseArranger != null) {
            courseArranger.dispose();
            courseArranger = null;
        }
        gameHasEnded();
    }

    public void gameHasEnded() {
        state = CourseState.GAME_ENDED;
        if (lastRevealFrame != null) {
            lastRevealFrame.setVisible(false);
        }
    }

    @Override
    public void refreshCourse(Course course, String reasonForCourseChange, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        setAndClearIfEmpty(course);
        log.debug("Received new course because '{}': {}", reasonForCourseChange, course);
        mainFrame.setTitle("Circuit Chaos Course - %s - %s - %s".formatted(gameAttributes.getRegistration().getGameName(),
                                                                           StringUtils.hasText(myRobotsName) ? myRobotsName : "Game has not been started yet",
                                                                           StringUtils.hasText(reasonForCourseChange) ? reasonForCourseChange : ""));
        synchronized (courseJPanel) {
            courseJPanel.arrangeElements(this, step, phase, subPhase, animationSteps, myNextCheckpoint(course));
            if (subPhase == null || state == CourseState.GAME_ENDED || step == Step.SETUP) {
                audioSupplier.stop();
            } else if (subPhase == 0) {
                boolean hover   = step == Step.ROBOTS_AND_OBJECTS_MOVE && isHover(movingRobotName);
                boolean explode = step == Step.ROBOTS_AND_OBJECTS_MOVE && course.getFloor().stream().anyMatch(f -> f.getExplosiveDamage() > 0);
                playSound(step, course, hover, explode);
            }
        }
        if (courseArranger != null) {
            courseArranger.getPlayButton().setEnabled(course.getCheckpoints().size() > 1);
        }
    }

    private int myNextCheckpoint(Course course) {
        if (StringUtils.hasText(myRobotsName)) {
            Optional<Robot> myself = course.getRobots().stream().filter(r -> myRobotsName.equals(r.getName())).findFirst();
            if (myself.isPresent()) {
                return myself.get().getNextCheckpoint();
            }
        }
        return -1;
    }

    private boolean isHover(String movingRobotName) {
        return course.getRobots().stream()
                     .filter(r -> r.getName().equals(movingRobotName))
                     .filter(CourseElement::isOnCourse)
                     .anyMatch(Robot::isFlying);
    }

    private void playSound(Step step, Course course, boolean hover, boolean explode) {
        if (!soundMenuItem.isSelected()) {
            audioSupplier.stop();
            return;
        }
        switch (step) {
            case ROBOT_MOUNTED_LASER_FIRE, COURSE_MOUNTED_LASER_FIRE,
                 ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE, COURSE_MOUNTED_PRESSURE_BEAMS_FIRE,
                 ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE, COURSE_MOUNTED_TRACTOR_BEAMS_FIRE,
                 ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE, ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE,
                 ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE -> audioSupplier.playSound(AudioConstants.LASER_BEAM);
            case EXPRESS_CONVEYOR_BELTS_MOVE, CONVEYOR_BELTS_MOVE ->
                    audioSupplier.playSound(AudioConstants.CONVEYOR_BELT);
            case PUSHERS_PUSH -> audioSupplier.playSound(AudioConstants.PUSHER);
            case GEARS_ROTATE -> audioSupplier.playSound(AudioConstants.GEARS);
            case OPEN_TRAPDOORS -> {
                if (course.getProperties().isTrapdoor()) {
                    audioSupplier.playSound(AudioConstants.TRAPDOOR);
                }
            }
            case ROBOTS_AND_OBJECTS_MOVE -> {
                if (explode) {
                    audioSupplier.playSound(AudioConstants.EXPLOSION);
                } else if (hover) {
                    audioSupplier.playSound(AudioConstants.ROBOTS_HOVER);
                } else {
                    audioSupplier.playSound(AudioConstants.ROBOTS_MOVE);
                }
            }
            default -> audioSupplier.stop();
        }
    }

    public void perform(String answerUrl, NetworkRequest request) {
        if (state == CourseState.GAME_ENDED) {
            return;
        }
        myRobotsName = request.getMyRobot().getName();
        mainFrame.setTitle("Circuit Chaos Course - %s - %s - Waiting for all to program their robots"
                                   .formatted(gameAttributes.getRegistration().getGameName(), myRobotsName));
        if (lastRevealFrame != null) {
            lastRevealFrame.setVisible(false);
        }
        new ProgramRobotGui(gameAttributes, answerUrl, request).apply();
    }

    public void revealProgramme(RevealProgrammeResponse revealProgrammeResponse) {
        if (state == CourseState.GAME_ENDED) {
            return;
        }
        synchronized (revealPanel) {
            revealPanel.removeAll();
            revealPanel.setBackground(Color.WHITE);
            GridBagLayout displayLayout = new GridBagLayout();
            JPanel        p             = new JPanel(displayLayout);
            addComponentToPanel(new JLabel(" Phase: %s ".formatted(revealProgrammeResponse.getPhase() + 1)),
                                p, displayLayout, 1, 2, 1, GridBagConstraints.CENTER);
            Image scaledExplosion = imageSupplier.getImageIconPlain(GFX_EXPLOSION + "0").getImage()
                                                 .getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            for (int i = 0; i < revealProgrammeResponse.getProgramme().size(); i++) {
                int                     y    = 1;
                RevealProgrammeListItem item = revealProgrammeResponse.getProgramme().get(i);
                addComponentToPanel(new JLabel(item.name()), p, displayLayout, 2 + i, y++, 1, GridBagConstraints.CENTER);
                ImageIcon originalIcon = imageSupplier.getImageIconPlain(item.imageIconPath());
                Image     scaledImage  = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                addComponentToPanel(new JLabel(new ImageIcon(scaledImage)),
                                    p, displayLayout, 2 + i, y++, 1, GridBagConstraints.CENTER);
                addComponentToPanel(new JLabel(" "), p, displayLayout, 2 + i, y++, 1, GridBagConstraints.CENTER);
                addComponentToPanel(new JLabel(imageSupplier.getImageIcon(item.programme(), true, false)),
                                    p, displayLayout, 2 + i, y++, 1, GridBagConstraints.CENTER);
                addComponentToPanel(new JLabel(" "), p, displayLayout, 2 + i, y++, 1, GridBagConstraints.CENTER);
                if (item.weapon() != null) {
                    addComponentToPanel(new JLabel(imageSupplier.getImageIcon(item.weapon(), 0.75)),
                                        p, displayLayout, 2 + i, y++, 1, GridBagConstraints.CENTER);
                }
                for (int d = 0; d < item.damage(); d++) {
                    addComponentToPanel(new JLabel(new ImageIcon(scaledExplosion)),
                                        p, displayLayout, 2 + i, y++, 1, GridBagConstraints.CENTER);
                }
            }
            revealPanel.add(p);
            if (lastRevealFrame == null) {
                lastRevealFrame = new JFrame("Circuit Chaos - %s - %s".formatted(gameAttributes.getRegistration().getGameName(), myRobotsName));
                lastRevealFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
                lastRevealFrame.setLocationRelativeTo(mainFrame);
                lastRevealFrame.setLocation(lastRevealFrame.getX() + mainFrame.getWidth() / 2,
                                            lastRevealFrame.getY() - mainFrame.getHeight() / 2 + 20);
                lastRevealFrame.setContentPane(new JScrollPane(revealPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
            }
            lastRevealFrame.pack();
            lastRevealFrame.setVisible(true);
            lastRevealFrame.requestFocus();
        }
    }

    public void play(GameOptions gameOptions) {
        courseArranger = null;
        state = CourseState.GAME_RUNNING;
        clientToServerConnection.play(gameAttributes.getGameUrl(), gameOptions);
    }

}
