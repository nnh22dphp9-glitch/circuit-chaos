package de.phlup.circuitchaos.client.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.enums.ComputerType;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseInfo;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import de.phlup.circuitchaos.common.settings.GameSettings;
import de.phlup.circuitchaos.server.game.GameOptions;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Objects;

@Slf4j
public class ClientCourseArranger implements GuiHelper {

    private final ImageSupplier imageSupplier;
    private final JFrame        frame = new JFrame("CC");

    // arrangement

    private final JComboBox<CourseInfo> courseList = new JComboBox<>();

    private final JButton chooseCourseButton = new JButton("Choose");
    @Getter
    private final JButton playButton         = new JButton("Play");
    private final JPanel  chooserPanel       = new JPanel(new BorderLayout());
    private final JLabel  regPlayers         = new JLabel(" 1 ");
    private final JLabel  regWatchers        = new JLabel(" 0 ");

    @SuppressWarnings("unchecked")
    private final JComboBox<String>[]    computerTypes = new JComboBox[7];
    private final Collection<CourseInfo> courseInfos;
    private final ObjectMapper           objectMapper;
    private final ClientSettings         clientSettings;

    private JLabel coursePreview = new JLabel();


    public ClientCourseArranger(GameGui game, GameSettings gameSettings, Collection<CourseInfo> courses, ObjectMapper objectMapper, ClientSettings clientSettings) {
        imageSupplier = game.getImageSupplier();
        courseInfos = courses;
        this.objectMapper = objectMapper;
        this.clientSettings = clientSettings;

        JPanel borderPanel = new JPanel(new BorderLayout());
        JPanel mainPanel   = new JPanel(new BorderLayout());
        mainPanel.add(createCourseChooser(game), BorderLayout.NORTH);
        mainPanel.add(createPlayersDisplay(), BorderLayout.CENTER);
        mainPanel.add(createComputerPlayerChooser(gameSettings), BorderLayout.SOUTH);
        borderPanel.add(mainPanel, BorderLayout.CENTER);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.SOUTH);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.EAST);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 5, BufferedImage.TYPE_INT_ARGB))), BorderLayout.WEST);

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

    private JPanel createComputerPlayerChooser(GameSettings gameSettings) {
        GridBagLayout layout      = new GridBagLayout();
        JPanel        compPlayers = new JPanel(layout);
        compPlayers.setBorder(new TitledBorder(new LineBorder(Color.black), " Computer players "));
        for (int i = 0; i < 7; i++) {
            addComponentToPanel(new JLabel("#%s: ".formatted(i + 1)), compPlayers, layout, 1, 6 + i, 1, GridBagConstraints.WEST);
            computerTypes[i] = new JComboBox<>();
            for (ComputerType ct : ComputerType.values()) {
                computerTypes[i].addItem(ct.getName());
            }
            computerTypes[i].setSelectedIndex(i + 1 <= gameSettings.getMaxNumberOfComputerPlayers() ? 1 : 0);
            addComponentToPanel(computerTypes[i], compPlayers, layout, 2, 6 + i, 1, GridBagConstraints.WEST);
        }
        return compPlayers;
    }

    private JPanel createCourseChooser(GameGui game) {
        JPanel courseChooser = new JPanel(new BorderLayout());
        courseList.setEditable(false);
        CourseInfo item = new CourseInfo("Choose a course!");
        item.setImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_PICTURE).getImage());
        courseList.addItem(item);
        for (CourseInfo bi : courseInfos) {
            courseList.addItem(bi);
        }
        courseList.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseCourseButton.setEnabled(courseList.getSelectedIndex() > 0);
                redrawPreview();
            }
        });
        courseChooser.add(courseList, BorderLayout.CENTER);

        chooseCourseButton.addActionListener(new AbstractAction() {
            @Override
            @SneakyThrows
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
        chooseCourseButton.setEnabled(false);

        playButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.setState(CourseState.GAME_RUNNING);
                frame.dispose();
                GameOptions gameOptions = new GameOptions();
                for (int i = 0; i < 7; i++) {
                    String type = (String) computerTypes[i].getSelectedItem();
                    if (!ComputerType.NONE.getName().equals(type)) {
                        gameOptions.getComputerType().addLast(i + "-" + type);
                    }
                }
                gameOptions.setDefaultModules(clientSettings.getDefaultModules()); // TODO im Dialog konfigurierbar machen

                game.play(gameOptions);
            }
        });
        playButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(chooseCourseButton, BorderLayout.NORTH);
        buttonPanel.add(new JLabel(" "), BorderLayout.CENTER);
        buttonPanel.add(playButton, BorderLayout.SOUTH);
        courseChooser.add(buttonPanel, BorderLayout.SOUTH);

        chooserPanel.add(courseChooser, BorderLayout.SOUTH);
        redrawPreview();
        return chooserPanel;
    }

    public Course copy(Course course) {
        if (course == null) {
            return null;
        }
        return objectMapper.convertValue(course, Course.class);
    }

    private void redrawPreview() {
        chooserPanel.remove(coursePreview);
        BufferedImage bi = new BufferedImage(150, 231, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr = bi.createGraphics();
        if (courseList.getSelectedIndex() > 0) {
            CourseInfo courseInfo = (CourseInfo) courseList.getSelectedItem();
            String     bild       = PictureConstants.GFX_LOGO_SMALL;
            gr.drawImage(imageSupplier.getImageIconPlain(bild).getImage(), 0, 0, null);
            gr.translate(0, 81);
            gr.drawImage(Objects.requireNonNull(courseInfo).getImage(), 0, 0, null);
        } else {
            gr.drawImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_LOGO_SMALL).getImage(), 0, 0, null);
            gr.translate(0, 81);
            gr.drawImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_PICTURE).getImage(), 0, 0, null);
        }
        coursePreview = new JLabel(imageSupplier.getImageIconPlain(bi));
        chooserPanel.add(coursePreview, BorderLayout.CENTER);
        chooserPanel.validate();
    }

    public void updatePlayersAndWatchers(int players, int watchers) {
        regPlayers.setText(" " + players + " ");
        regWatchers.setText(" " + watchers + " ");
    }

    public void dispose() {
        frame.dispose();
    }

}
