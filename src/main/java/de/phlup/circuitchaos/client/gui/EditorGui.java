package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.client.service.CourseLoader;
import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseProperties;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Range;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LOGO_SMALL;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_NO_ACTION;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PICTURE;
import static de.phlup.circuitchaos.common.enums.Direction.EAST;
import static de.phlup.circuitchaos.common.enums.Direction.NORTH;
import static de.phlup.circuitchaos.common.enums.Direction.SOUTH;
import static de.phlup.circuitchaos.common.enums.Direction.WEST;
import static de.phlup.circuitchaos.common.enums.Floortype.ABYSS;
import static de.phlup.circuitchaos.common.enums.Floortype.TRAPDOOR;
import static javax.swing.JFileChooser.APPROVE_OPTION;

@Slf4j
public class EditorGui extends BaseGui implements GuiHelper {

    private final CourseLoader courseLoader;
    private final JFrame       editorFrame;

    private File currentFileDirectory;

    private final JComboBox<Floortype> floortypeBox       = new JComboBox<>();
    private final JLabel               directionLabel     = new JLabel("Conveyor Belt Direction: ");
    private final JButton              directionButton    = new JButton();
    private final JLabel               pusherLabel        = new JLabel("Pusher: ");
    private final JButton              pusherButton       = new JButton();
    private final JLabel               activeInPhaseLabel = new JLabel("Trapdoor and Pusher phases: ");
    private final JCheckBox[]          activeInPhase      = new JCheckBox[]{new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox()};
    private final JLabel               waterLabel         = new JLabel("Water: ");
    private final JCheckBox            waterBox           = new JCheckBox();
    private final JLabel               wallsLabel         = new JLabel("Walls: ");
    private final JComboBox<WallType>  wallNorth          = new JComboBox<>();
    private final JComboBox<WallType>  wallEast           = new JComboBox<>();
    private final JComboBox<WallType>  wallSouth          = new JComboBox<>();
    private final JComboBox<WallType>  wallWest           = new JComboBox<>();
    private final JButton              levelMinus         = new JButton(" - ");
    private final JLabel               levelLabel         = new JLabel(" 0 ");
    private final JButton              levelPlus          = new JButton(" + ");
    private final JLabel               beamLabel          = new JLabel("Beams: ");
    private final JComboBox<String>    beamChooserNorth   = new JComboBox<>();
    private final JComboBox<String>    beamChooserEast    = new JComboBox<>();
    private final JComboBox<String>    beamChooserSouth   = new JComboBox<>();
    private final JComboBox<String>    beamChooserWest    = new JComboBox<>();

    private boolean updatingFields = true;

    @Getter
    @Setter
    private Floor selectedFloor;

    public EditorGui(@NotNull ResourceLoader resourceLoader,
                     @NotNull ClientSettings clientSettings,
                     @NotNull CourseLoader courseLoader) {
        super(resourceLoader, clientSettings, null);
        this.courseLoader = courseLoader;
        currentFileDirectory = new File(clientSettings.getCourseDirectories().stream()
                                                      .filter(s -> s.startsWith("file:")).findFirst()
                                                      .orElse("file:.").substring(5));
        state = CourseState.EDITOR;
        editorFrame = new JFrame("Circuit Chaos - Course Editor");
        log.info("Initialised currentFileDirectory with {}", currentFileDirectory);
    }

    public void open() {
        initCourse();
        synchronized (courseJPanel) {
            courseJPanel.arrangeElements(this, Step.SETUP, null, null, 1, -1);
        }
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                mainFrame.dispose();
            }
        });
        addMenuBar(mainFrame);
        configureMainFrame("Circuit Chaos - Course Editor");
        openEditorFrame();
    }

    private void initCourse() {
        course = new Course();
        selectedFloor = new Floor();
        course.getFloor().add(selectedFloor);
        course.setRange(new Range(0, 0, 0, 0).wide());
        fillMissingCourseElementsWithAbyss();
        redrawAll();
        adjustSelectorValuesToSelectedFloor();
    }

    private void openEditorFrame() {
        JPanel borderPanel = new JPanel(new BorderLayout());
        JPanel mainPanel   = new JPanel(new BorderLayout());
        mainPanel.add(createContent(), BorderLayout.NORTH);
        borderPanel.add(mainPanel, BorderLayout.CENTER);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.SOUTH);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.EAST);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 5, BufferedImage.TYPE_INT_ARGB))), BorderLayout.WEST);

        editorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        editorFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                mainFrame.dispose();
            }
        });
        editorFrame.setContentPane(borderPanel);
        editorFrame.setResizable(false);
        editorFrame.setLocationRelativeTo(mainFrame);
        editorFrame.setLocation(editorFrame.getX() + mainFrame.getWidth() / 2,
                                editorFrame.getY() - mainFrame.getHeight() / 2 + 20);
        editorFrame.pack();
        editorFrame.setVisible(true);
        editorFrame.requestFocus();
    }

    private JPanel createContent() {
        GridBagLayout     layout            = new GridBagLayout();
        JPanel            courseEditorPanel = new JPanel(layout);
        FloorChangedEvent floorChangedEvent = new FloorChangedEvent();
        PusherButtonEvent pusherButtonEvent = new PusherButtonEvent();

        int y = 0;
        addComponentToPanel(new JLabel(imageSupplier.getImageIconPlain(GFX_LOGO_SMALL)), courseEditorPanel, layout, 0, y++, 2, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(" "), courseEditorPanel, layout, 0, y++, 2, GridBagConstraints.EAST);

        for (Floortype ft : Floortype.values()) {
            floortypeBox.addItem(ft);
        }
        floortypeBox.setEnabled(true);
        floortypeBox.addActionListener(floorChangedEvent);
        addComponentToPanel(floortypeBox, courseEditorPanel, layout, 0, y++, 2, GridBagConstraints.EAST);

        addComponentToPanel(wallsLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(wallNorth, BorderLayout.NORTH);
        borderPanel.add(wallEast, BorderLayout.EAST);
        borderPanel.add(wallSouth, BorderLayout.SOUTH);
        borderPanel.add(wallWest, BorderLayout.WEST);
        borderPanel.add(new JLabel(" "), BorderLayout.CENTER);
        addComponentToPanel(borderPanel, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.EAST);
        wallNorth.removeAllItems();
        wallEast.removeAllItems();
        wallSouth.removeAllItems();
        wallWest.removeAllItems();
        for (WallType wt : WallType.values()) {
            if (wt == WallType.NONE || wt == WallType.SOLID || wt == WallType.REPULSOR_FIELD
                    || wt == WallType.ONE_WAY_GREEN || wt == WallType.ONE_WAY_RED) {
                wallNorth.addItem(wt);
                wallEast.addItem(wt);
                wallSouth.addItem(wt);
                wallWest.addItem(wt);
            }
        }
        wallNorth.addActionListener(floorChangedEvent);
        wallEast.addActionListener(floorChangedEvent);
        wallSouth.addActionListener(floorChangedEvent);
        wallWest.addActionListener(floorChangedEvent);

        directionLabel.setEnabled(false);
        addComponentToPanel(directionLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        directionButton.setIcon(imageSupplier.getImageIconPlain(Direction.NORTH));
        directionButton.setMinimumSize(new Dimension(60, 60));
        directionButton.setPreferredSize(new Dimension(60, 60));
        directionButton.setMaximumSize(new Dimension(60, 60));
        directionButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                selectedFloor.setFacingDirection(selectedFloor.getFacingDirection().add(Direction.EAST));
                directionButton.setIcon(imageSupplier.getImageIconPlain(selectedFloor.getFacingDirection()));
                redrawAll();
            }
        });
        directionButton.setEnabled(false);
        addComponentToPanel(directionButton, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.WEST);

        addComponentToPanel(pusherLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        pusherButton.setIcon(imageSupplier.getImageIconPlain(GFX_NO_ACTION));
        pusherButton.setMinimumSize(new Dimension(60, 60));
        pusherButton.setPreferredSize(new Dimension(60, 60));
        pusherButton.setMaximumSize(new Dimension(60, 60));
        pusherButton.addActionListener(pusherButtonEvent);
        pusherButton.setEnabled(true);
        addComponentToPanel(pusherButton, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.WEST);

        JPanel flowPanel = new JPanel(new FlowLayout());
        for (int i = 0; i < 5; i++) {
            activeInPhase[i].setSelected(false);
            activeInPhase[i].setEnabled(false);
            activeInPhase[i].addActionListener(floorChangedEvent);
            flowPanel.add(activeInPhase[i]);
        }
        activeInPhaseLabel.setEnabled(false);
        addComponentToPanel(activeInPhaseLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        addComponentToPanel(flowPanel, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.EAST);

        waterLabel.setEnabled(true);
        addComponentToPanel(waterLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        waterBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                selectedFloor.setWater(waterBox.isSelected());
                redrawAll();
            }
        });
        waterBox.setEnabled(true);
        addComponentToPanel(waterBox, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.WEST);

        addComponentToPanel(new JLabel("Level: "), courseEditorPanel, layout, 0, y, 1, GridBagConstraints.WEST);
        flowPanel = new JPanel(new FlowLayout());
        flowPanel.add(levelMinus);
        levelMinus.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedFloor.setLevel(selectedFloor.getLevel() - 1);
                levelAdjustments();
            }
        });
        flowPanel.add(levelLabel);
        flowPanel.add(levelPlus);
        levelPlus.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedFloor.setLevel(selectedFloor.getLevel() + 1);
                levelAdjustments();
            }
        });
        addComponentToPanel(flowPanel, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.EAST);

        addComponentToPanel(beamLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(beamChooserNorth, BorderLayout.NORTH);
        borderPanel.add(beamChooserEast, BorderLayout.EAST);
        borderPanel.add(beamChooserSouth, BorderLayout.SOUTH);
        borderPanel.add(beamChooserWest, BorderLayout.WEST);
        borderPanel.add(new JLabel(" "), BorderLayout.CENTER);
        addComponentToPanel(borderPanel, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.EAST);
        beamChooserNorth.addItem("None");
        beamChooserNorth.addItem("1 Laser Beam");
        beamChooserNorth.addItem("2 Laser Beams");
        beamChooserNorth.addItem("3 Laser Beams");
        beamChooserNorth.addItem("Pressure Beam");
        beamChooserNorth.addItem("Tractor Beam");
        beamChooserNorth.addActionListener(floorChangedEvent);
        beamChooserNorth.setEnabled(false);
        beamChooserEast.addItem("None");
        beamChooserEast.addItem("1 Laser Beam");
        beamChooserEast.addItem("2 Laser Beams");
        beamChooserEast.addItem("3 Laser Beams");
        beamChooserEast.addItem("Pressure Beam");
        beamChooserEast.addItem("Tractor Beam");
        beamChooserEast.addActionListener(floorChangedEvent);
        beamChooserEast.setEnabled(false);
        beamChooserSouth.addItem("None");
        beamChooserSouth.addItem("1 Laser Beam");
        beamChooserSouth.addItem("2 Laser Beams");
        beamChooserSouth.addItem("3 Laser Beams");
        beamChooserSouth.addItem("Pressure Beam");
        beamChooserSouth.addItem("Tractor Beam");
        beamChooserSouth.addActionListener(floorChangedEvent);
        beamChooserSouth.setEnabled(false);
        beamChooserWest.addItem("None");
        beamChooserWest.addItem("1 Laser Beam");
        beamChooserWest.addItem("2 Laser Beams");
        beamChooserWest.addItem("3 Laser Beams");
        beamChooserWest.addItem("Pressure Beam");
        beamChooserWest.addItem("Tractor Beam");
        beamChooserWest.addActionListener(floorChangedEvent);
        beamChooserWest.setEnabled(false);

        // TODO add more attributes

        addComponentToPanel(new JLabel(" "), courseEditorPanel, layout, 0, y++, 2, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(imageSupplier.getImageIconPlain(GFX_PICTURE)), courseEditorPanel, layout, 0, y, 2, GridBagConstraints.EAST);
        updatingFields = false;
        return courseEditorPanel;
    }

    private void addMenuBar(JFrame frame) {
        JMenuBar menuBar    = new JMenuBar();
        JMenu    editorMenu = new JMenu("Editor");
        menuBar.add(editorMenu);
        JMenuItem loadMenuItem = new JMenuItem("Load...");
        loadMenuItem.addActionListener(e -> loadCourse());
        editorMenu.add(loadMenuItem);
        JMenuItem saveMenuItem = new JMenuItem("Save...");
        saveMenuItem.addActionListener(e -> saveCourse());
        editorMenu.add(saveMenuItem);
        editorMenu.addSeparator();
        editorMenu.add(createZoomMenu());
        // TODO build new help menu
        //  JMenu helpMenu = new JMenu("Help");
        //  menuBar.add(helpMenu);
        //  new HelpMenu(imageSupplier).createHelpMenu(helpMenu);
        frame.setJMenuBar(menuBar);
    }

    @Override
    public void refreshCourse(Course course, String reasonForCourseChange, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        setAndClearIfEmpty(course);
        redrawAll();
    }

    public void redrawAll() {
        synchronized (courseJPanel) {
            courseJPanel.arrangeElements(this, Step.SETUP, null, null, 1, -1);
        }
    }

    public void loadCourse() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentFileDirectory);
        fileChooser.setDialogTitle("Choose course file to load");
        fileChooser.setFileFilter(new FileNameExtensionFilter("course files (*.json)", "json"));

        if (fileChooser.showOpenDialog(mainFrame) == APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            currentFileDirectory = path.toFile();
            Course loadedCourse = courseLoader.loadCourse(path);
            if (loadedCourse != null) {
                selectedFloor = loadedCourse.getFloor().getFirst();
                adjustSelectorValuesToSelectedFloor();
                refreshCourse(loadedCourse, null, Step.SETUP, null, null, 1, null);
            }
        }
    }

    public void saveCourse() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentFileDirectory);
        fileChooser.setDialogTitle("Choose the file to save your course to");
        fileChooser.setFileFilter(new FileNameExtensionFilter("course files (*.json)", "json"));

        if (fileChooser.showSaveDialog(mainFrame) == APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            currentFileDirectory = path.toFile();
            if (!path.toString().toLowerCase().endsWith(".json")) {
                path = path.resolveSibling(path.getFileName() + ".json");
            }
            courseLoader.saveCourse(course, path); // TODO auch die preview speichern
        }
    }

    public void adjustSelectorValuesToSelectedFloor() {
        updatingFields = true;
        Floortype newFloortype = selectedFloor.getFloortype();
        floortypeBox.setSelectedItem(newFloortype);
        directionLabel.setEnabled(newFloortype.isConveyorBelt());
        directionButton.setIcon(imageSupplier.getImageIconPlain(selectedFloor.getFacingDirection()));
        directionButton.setEnabled(newFloortype.isConveyorBelt());
        waterBox.setSelected(selectedFloor.isWater());
        for (int i = 0; i < 5; i++) {
            boolean activationValid = newFloortype == TRAPDOOR || selectedFloor.isHasPusher();
            activeInPhase[i].setSelected(activationValid && selectedFloor.getActiveInPhase()[i]);
            activeInPhase[i].setEnabled(activationValid);
            activeInPhaseLabel.setEnabled(activationValid);
        }
        adjustWallTypeSelection();
        if (selectedFloor.isHasPusher()) {
            pusherButton.setIcon(imageSupplier.getImageIconPlain(selectedFloor.getPusherDirection()));
        } else {
            pusherButton.setIcon(imageSupplier.getImageIconPlain(GFX_NO_ACTION));
        }
        levelLabel.setText(" " + selectedFloor.getLevel() + " ");
        levelPlus.setEnabled(levelPlusOrMinusAllowed(true));
        levelMinus.setEnabled(levelPlusOrMinusAllowed(false));
        if (isBeamAllowed(NORTH)) {
            beamChooserNorth.setEnabled(true);
            if (selectedFloor.getPressureBeam()[0]) {
                beamChooserNorth.setSelectedItem(4);
            } else if (selectedFloor.getTractorBeam()[0]) {
                beamChooserNorth.setSelectedItem(5);
            } else {
                beamChooserNorth.setSelectedItem(selectedFloor.getLasers()[0]);
            }
        } else {
            beamChooserNorth.setEnabled(false);
            beamChooserNorth.setSelectedItem(0);
        }
        if (isBeamAllowed(EAST)) {
            beamChooserEast.setEnabled(true);
            if (selectedFloor.getPressureBeam()[1]) {
                beamChooserEast.setSelectedItem(4);
            } else if (selectedFloor.getTractorBeam()[1]) {
                beamChooserEast.setSelectedItem(5);
            } else {
                beamChooserEast.setSelectedItem(selectedFloor.getLasers()[1]);
            }
        } else {
            beamChooserEast.setEnabled(false);
            beamChooserEast.setSelectedItem(0);
        }
        if (isBeamAllowed(SOUTH)) {
            beamChooserSouth.setEnabled(true);
            if (selectedFloor.getPressureBeam()[2]) {
                beamChooserSouth.setSelectedItem(4);
            } else if (selectedFloor.getTractorBeam()[2]) {
                beamChooserSouth.setSelectedItem(5);
            } else {
                beamChooserSouth.setSelectedItem(selectedFloor.getLasers()[2]);
            }
        } else {
            beamChooserSouth.setEnabled(false);
            beamChooserSouth.setSelectedItem(0);
        }
        if (isBeamAllowed(WEST)) {
            beamChooserWest.setEnabled(true);
            if (selectedFloor.getPressureBeam()[3]) {
                beamChooserWest.setSelectedItem(4);
            } else if (selectedFloor.getTractorBeam()[3]) {
                beamChooserWest.setSelectedItem(5);
            } else {
                beamChooserWest.setSelectedItem(selectedFloor.getLasers()[3]);
            }
        } else {
            beamChooserWest.setEnabled(false);
            beamChooserWest.setSelectedItem(0);
        }

        // TODO add more attributes
        updatingFields = false;
    }

    private boolean isBeamAllowed(Direction dir) {
        return !selectedFloor.isHasPusher() && switch (dir) {
            case NORTH -> selectedFloor.getWallSouth();
            case EAST -> selectedFloor.getWallWest();
            case SOUTH -> selectedFloor.getWallNorth();
            case WEST -> selectedFloor.getWallEast();
        } == WallType.SOLID;
    }

    private void adjustWallTypeSelection() {
        wallNorth.removeAllItems();
        int secondFloorLevel = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(NORTH)).getLevel();
        for (WallType wt : WallType.values()) {
            if (wallTypeAllowed(wt, secondFloorLevel)) {
                wallNorth.addItem(wt);
                if (selectedFloor.getWallNorth() == wt) {
                    wallNorth.setSelectedItem(selectedFloor.getWallNorth());
                }
            }
        }
        wallEast.removeAllItems();
        secondFloorLevel = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(EAST)).getLevel();
        for (WallType wt : WallType.values()) {
            if (wallTypeAllowed(wt, secondFloorLevel)) {
                wallEast.addItem(wt);
                if (selectedFloor.getWallEast() == wt) {
                    wallEast.setSelectedItem(selectedFloor.getWallEast());
                }
            }
        }
        wallSouth.removeAllItems();
        secondFloorLevel = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(SOUTH)).getLevel();
        for (WallType wt : WallType.values()) {
            if (wallTypeAllowed(wt, secondFloorLevel)) {
                wallSouth.addItem(wt);
                if (selectedFloor.getWallSouth() == wt) {
                    wallSouth.setSelectedItem(selectedFloor.getWallSouth());
                }
            }
        }
        wallWest.removeAllItems();
        secondFloorLevel = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(WEST)).getLevel();
        for (WallType wt : WallType.values()) {
            if (wallTypeAllowed(wt, secondFloorLevel)) {
                wallWest.addItem(wt);
                if (selectedFloor.getWallWest() == wt) {
                    wallWest.setSelectedItem(selectedFloor.getWallWest());
                }
            }
        }
    }

    private boolean wallTypeAllowed(WallType wt, int levelSecondFlor) {
        return (wt == WallType.SOLID || wt == WallType.REPULSOR_FIELD || wt == WallType.ONE_WAY_GREEN || wt == WallType.ONE_WAY_RED)
                || (levelSecondFlor <= selectedFloor.getLevel() && wt == WallType.NONE)
                || (levelSecondFlor > selectedFloor.getLevel() && wt == WallType.LEDGE)
                || (levelSecondFlor == selectedFloor.getLevel() - 1 && wt == WallType.RAMP_DOWN)
                || (levelSecondFlor == selectedFloor.getLevel() + 1 && wt == WallType.RAMP_UP);
    }

    private boolean levelPlusOrMinusAllowed(boolean isPlus) {
        Floor secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(NORTH));
        if (secondFloor.getLevel() != selectedFloor.getLevel()
                && (isPlus || secondFloor.getLevel() + 1 != selectedFloor.getLevel())
                && (!isPlus || secondFloor.getLevel() - 1 != selectedFloor.getLevel())
        ) {
            return false;
        }
        secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(EAST));
        if (secondFloor.getLevel() != selectedFloor.getLevel()
                && (isPlus || secondFloor.getLevel() + 1 != selectedFloor.getLevel())
                && (!isPlus || secondFloor.getLevel() - 1 != selectedFloor.getLevel())
        ) {
            return false;
        }
        secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(SOUTH));
        if (secondFloor.getLevel() != selectedFloor.getLevel()
                && (isPlus || secondFloor.getLevel() + 1 != selectedFloor.getLevel())
                && (!isPlus || secondFloor.getLevel() - 1 != selectedFloor.getLevel())
        ) {
            return false;
        }
        secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(WEST));
        return secondFloor.getLevel() == selectedFloor.getLevel()
                || (!isPlus && secondFloor.getLevel() + 1 == selectedFloor.getLevel())
                || (isPlus && secondFloor.getLevel() - 1 == selectedFloor.getLevel());
    }

    private void levelAdjustments() {
        updatingFields = true;
        levelLabel.setText(" " + selectedFloor.getLevel() + " ");
        levelPlus.setEnabled(levelPlusOrMinusAllowed(true));
        levelMinus.setEnabled(levelPlusOrMinusAllowed(false));
        Floor secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(NORTH));
        if (secondFloor.getLevel() == selectedFloor.getLevel()) {
            if (selectedFloor.getWallNorth() == WallType.RAMP_UP || selectedFloor.getWallNorth() == WallType.RAMP_DOWN || selectedFloor.getWallNorth() == WallType.LEDGE) {
                selectedFloor.setWallNorth(WallType.NONE);
            }
            if (secondFloor.getWallSouth() == WallType.RAMP_UP || secondFloor.getWallSouth() == WallType.RAMP_DOWN || secondFloor.getWallSouth() == WallType.LEDGE) {
                secondFloor.setWallSouth(WallType.NONE);
            }
        } else if (secondFloor.getLevel() + 1 == selectedFloor.getLevel() && secondFloor.getWallSouth() == WallType.NONE) {
            secondFloor.setWallSouth(WallType.LEDGE);
        } else if (secondFloor.getLevel() - 1 == selectedFloor.getLevel() && selectedFloor.getWallNorth() == WallType.NONE) {
            selectedFloor.setWallNorth(WallType.LEDGE);
        }
        secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(EAST));
        if (secondFloor.getLevel() == selectedFloor.getLevel()) {
            if (selectedFloor.getWallEast() == WallType.RAMP_UP || selectedFloor.getWallEast() == WallType.RAMP_DOWN || selectedFloor.getWallEast() == WallType.LEDGE) {
                selectedFloor.setWallEast(WallType.NONE);
            }
            if (secondFloor.getWallWest() == WallType.RAMP_UP || secondFloor.getWallWest() == WallType.RAMP_DOWN || secondFloor.getWallWest() == WallType.LEDGE) {
                secondFloor.setWallWest(WallType.NONE);
            }
        } else if (secondFloor.getLevel() + 1 == selectedFloor.getLevel() && secondFloor.getWallWest() == WallType.NONE) {
            secondFloor.setWallWest(WallType.LEDGE);
        } else if (secondFloor.getLevel() - 1 == selectedFloor.getLevel() && selectedFloor.getWallEast() == WallType.NONE) {
            selectedFloor.setWallEast(WallType.LEDGE);
        }
        secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(SOUTH));
        if (secondFloor.getLevel() == selectedFloor.getLevel()) {
            if (selectedFloor.getWallSouth() == WallType.RAMP_UP || selectedFloor.getWallSouth() == WallType.RAMP_DOWN || selectedFloor.getWallSouth() == WallType.LEDGE) {
                selectedFloor.setWallSouth(WallType.NONE);
            }
            if (secondFloor.getWallNorth() == WallType.RAMP_UP || secondFloor.getWallNorth() == WallType.RAMP_DOWN || secondFloor.getWallNorth() == WallType.LEDGE) {
                secondFloor.setWallNorth(WallType.NONE);
            }
        } else if (secondFloor.getLevel() + 1 == selectedFloor.getLevel() && secondFloor.getWallNorth() == WallType.NONE) {
            secondFloor.setWallNorth(WallType.LEDGE);
        } else if (secondFloor.getLevel() - 1 == selectedFloor.getLevel() && selectedFloor.getWallSouth() == WallType.NONE) {
            selectedFloor.setWallSouth(WallType.LEDGE);
        }
        secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(WEST));
        if (secondFloor.getLevel() == selectedFloor.getLevel()) {
            if (selectedFloor.getWallWest() == WallType.RAMP_UP || selectedFloor.getWallWest() == WallType.RAMP_DOWN || selectedFloor.getWallWest() == WallType.LEDGE) {
                selectedFloor.setWallWest(WallType.NONE);
            }
            if (secondFloor.getWallEast() == WallType.RAMP_UP || secondFloor.getWallEast() == WallType.RAMP_DOWN || secondFloor.getWallEast() == WallType.LEDGE) {
                secondFloor.setWallEast(WallType.NONE);
            }
        } else if (secondFloor.getLevel() + 1 == selectedFloor.getLevel() && secondFloor.getWallEast() == WallType.NONE) {
            secondFloor.setWallEast(WallType.LEDGE);
        } else if (secondFloor.getLevel() - 1 == selectedFloor.getLevel() && selectedFloor.getWallWest() == WallType.NONE) {
            selectedFloor.setWallWest(WallType.LEDGE);
        }
        adjustWallTypeSelection();
        updatingFields = false;
        redrawAll();
    }

    private class FloorChangedEvent extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (updatingFields) {
                return;
            }
            Floortype newFloortype = (Floortype) floortypeBox.getSelectedItem();
            if (newFloortype != null) {
                selectedFloor.setFloortype(newFloortype);
                directionLabel.setEnabled(newFloortype.isConveyorBelt());
                directionButton.setIcon(imageSupplier.getImageIconPlain(selectedFloor.getFacingDirection()));
                directionButton.setEnabled(newFloortype.isConveyorBelt());
                for (int i = 0; i < 5; i++) {
                    boolean activationValid = newFloortype == TRAPDOOR || selectedFloor.isHasPusher();
                    selectedFloor.getActiveInPhase()[i] = activationValid && activeInPhase[i].isSelected();
                    activeInPhase[i].setEnabled(activationValid);
                    activeInPhaseLabel.setEnabled(activationValid);
                }
                adjustWallTypes();
                if (isBeamAllowed(NORTH)) {
                    beamChooserNorth.setEnabled(true);
                    selectedFloor.getLasers()[0] = beamChooserNorth.getSelectedIndex() > 3 ? 0 : beamChooserNorth.getSelectedIndex();
                    selectedFloor.getPressureBeam()[0] = beamChooserNorth.getSelectedIndex() == 4;
                    selectedFloor.getTractorBeam()[0] = beamChooserNorth.getSelectedIndex() == 5;
                } else {
                    beamChooserNorth.setEnabled(false);
                    selectedFloor.getLasers()[0] = 0;
                    selectedFloor.getPressureBeam()[0] = false;
                    selectedFloor.getTractorBeam()[0] = false;
                }
                if (isBeamAllowed(EAST)) {
                    beamChooserEast.setEnabled(true);
                    selectedFloor.getLasers()[1] = beamChooserEast.getSelectedIndex() > 3 ? 0 : beamChooserEast.getSelectedIndex();
                    selectedFloor.getPressureBeam()[1] = beamChooserEast.getSelectedIndex() == 4;
                    selectedFloor.getTractorBeam()[1] = beamChooserEast.getSelectedIndex() == 5;
                } else {
                    beamChooserEast.setEnabled(false);
                    selectedFloor.getLasers()[1] = 0;
                    selectedFloor.getPressureBeam()[1] = false;
                    selectedFloor.getTractorBeam()[1] = false;
                }
                if (isBeamAllowed(SOUTH)) {
                    beamChooserSouth.setEnabled(true);
                    selectedFloor.getLasers()[2] = beamChooserSouth.getSelectedIndex() > 3 ? 0 : beamChooserSouth.getSelectedIndex();
                    selectedFloor.getPressureBeam()[2] = beamChooserSouth.getSelectedIndex() == 4;
                    selectedFloor.getTractorBeam()[2] = beamChooserSouth.getSelectedIndex() == 5;
                } else {
                    beamChooserSouth.setEnabled(false);
                    selectedFloor.getLasers()[2] = 0;
                    selectedFloor.getPressureBeam()[2] = false;
                    selectedFloor.getTractorBeam()[2] = false;
                }
                if (isBeamAllowed(WEST)) {
                    beamChooserWest.setEnabled(true);
                    selectedFloor.getLasers()[3] = beamChooserWest.getSelectedIndex() > 3 ? 0 : beamChooserWest.getSelectedIndex();
                    selectedFloor.getPressureBeam()[3] = beamChooserWest.getSelectedIndex() == 4;
                    selectedFloor.getTractorBeam()[3] = beamChooserWest.getSelectedIndex() == 5;
                } else {
                    beamChooserWest.setEnabled(false);
                    selectedFloor.getLasers()[3] = 0;
                    selectedFloor.getPressureBeam()[3] = false;
                    selectedFloor.getTractorBeam()[3] = false;
                }
                setBeams();

                // TODO add more attributes
                course.setRange(course.getRange().wideRangeToPosition(selectedFloor.getPosition()));
                fillMissingCourseElementsWithAbyss();
                determineCourseProperties();
                redrawAll();
            }
        }

        private void adjustWallTypes() {
            selectedFloor.setWallNorth((WallType) wallNorth.getSelectedItem());
            Floor secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(NORTH));
            if (selectedFloor.getWallNorth() == WallType.ONE_WAY_GREEN) {
                secondFloor.setWallSouth(WallType.ONE_WAY_RED);
            } else if (selectedFloor.getWallNorth() == WallType.ONE_WAY_RED) {
                secondFloor.setWallSouth(WallType.ONE_WAY_GREEN);
            } else if (selectedFloor.getWallNorth() == WallType.RAMP_UP) {
                secondFloor.setWallSouth(WallType.RAMP_DOWN);
            } else if (selectedFloor.getWallNorth() == WallType.RAMP_DOWN) {
                secondFloor.setWallSouth(WallType.RAMP_UP);
            } else if (secondFloor.getWallSouth() == WallType.ONE_WAY_GREEN
                    || secondFloor.getWallSouth() == WallType.ONE_WAY_RED
                    || secondFloor.getWallSouth() == WallType.RAMP_UP
                    || secondFloor.getWallSouth() == WallType.RAMP_DOWN) {
                secondFloor.setWallSouth(secondFloor.getLevel() < selectedFloor.getLevel() ? WallType.LEDGE : WallType.NONE);
            }
            selectedFloor.setWallEast((WallType) wallEast.getSelectedItem());
            secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(EAST));
            if (selectedFloor.getWallEast() == WallType.ONE_WAY_GREEN) {
                secondFloor.setWallWest(WallType.ONE_WAY_RED);
            } else if (selectedFloor.getWallEast() == WallType.ONE_WAY_RED) {
                secondFloor.setWallWest(WallType.ONE_WAY_GREEN);
            } else if (selectedFloor.getWallEast() == WallType.RAMP_UP) {
                secondFloor.setWallWest(WallType.RAMP_DOWN);
            } else if (selectedFloor.getWallEast() == WallType.RAMP_DOWN) {
                secondFloor.setWallWest(WallType.RAMP_UP);
            } else if (secondFloor.getWallWest() == WallType.ONE_WAY_GREEN
                    || secondFloor.getWallWest() == WallType.ONE_WAY_RED
                    || secondFloor.getWallWest() == WallType.RAMP_UP
                    || secondFloor.getWallWest() == WallType.RAMP_DOWN) {
                secondFloor.setWallWest(secondFloor.getLevel() < selectedFloor.getLevel() ? WallType.LEDGE : WallType.NONE);
            }
            selectedFloor.setWallSouth((WallType) wallSouth.getSelectedItem());
            secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(SOUTH));
            if (selectedFloor.getWallSouth() == WallType.ONE_WAY_GREEN) {
                secondFloor.setWallNorth(WallType.ONE_WAY_RED);
            } else if (selectedFloor.getWallSouth() == WallType.ONE_WAY_RED) {
                secondFloor.setWallNorth(WallType.ONE_WAY_GREEN);
            } else if (selectedFloor.getWallSouth() == WallType.RAMP_UP) {
                secondFloor.setWallNorth(WallType.RAMP_DOWN);
            } else if (selectedFloor.getWallSouth() == WallType.RAMP_DOWN) {
                secondFloor.setWallNorth(WallType.RAMP_UP);
            } else if (secondFloor.getWallNorth() == WallType.ONE_WAY_GREEN
                    || secondFloor.getWallNorth() == WallType.ONE_WAY_RED
                    || secondFloor.getWallNorth() == WallType.RAMP_UP
                    || secondFloor.getWallNorth() == WallType.RAMP_DOWN) {
                secondFloor.setWallNorth(secondFloor.getLevel() < selectedFloor.getLevel() ? WallType.LEDGE : WallType.NONE);
            }
            selectedFloor.setWallWest((WallType) wallWest.getSelectedItem());
            secondFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(WEST));
            if (selectedFloor.getWallWest() == WallType.ONE_WAY_GREEN) {
                secondFloor.setWallEast(WallType.ONE_WAY_RED);
            } else if (selectedFloor.getWallWest() == WallType.ONE_WAY_RED) {
                secondFloor.setWallEast(WallType.ONE_WAY_GREEN);
            } else if (selectedFloor.getWallWest() == WallType.RAMP_UP) {
                secondFloor.setWallEast(WallType.RAMP_DOWN);
            } else if (selectedFloor.getWallWest() == WallType.RAMP_DOWN) {
                secondFloor.setWallEast(WallType.RAMP_UP);
            } else if (secondFloor.getWallEast() == WallType.ONE_WAY_GREEN
                    || secondFloor.getWallEast() == WallType.ONE_WAY_RED
                    || secondFloor.getWallEast() == WallType.RAMP_UP
                    || secondFloor.getWallEast() == WallType.RAMP_DOWN) {
                secondFloor.setWallEast(secondFloor.getLevel() < selectedFloor.getLevel() ? WallType.LEDGE : WallType.NONE);
            }
            if (selectedFloor.isHasPusher()) {
                if (switch (selectedFloor.getPusherDirection()) {
                    case NORTH -> selectedFloor.getWallSouth();
                    case EAST -> selectedFloor.getWallWest();
                    case SOUTH -> selectedFloor.getWallNorth();
                    case WEST -> selectedFloor.getWallEast();
                } != WallType.SOLID) {
                    selectedFloor.setHasPusher(false);
                    pusherButton.setIcon(imageSupplier.getImageIconPlain(GFX_NO_ACTION));
                }
            }
        }
    }

    private void setBeams() {
        course.getFloor().forEach(f -> {
            f.setCourseMountedLaserBeamsNS(0);
            f.setCourseMountedLaserBeamsWE(0);
            f.setCourseMountedPressureBeamsNS(false);
            f.setCourseMountedPressureBeamsWE(false);
            f.setCourseMountedTractorBeamsNS(false);
            f.setCourseMountedTractorBeamsWE(false);
        });
        for (Floor f : new ArrayList<>(course.getFloor())) {
            for (Direction dir : Direction.values()) {
                int amount;
                if (f.getPressureBeam()[dir.ordinal()]) {
                    amount = 4;
                } else if (f.getTractorBeam()[dir.ordinal()]) {
                    amount = 5;
                } else {
                    amount = f.getLasers()[dir.ordinal()];
                }
                if (amount > 0) {
                    setBeam(f, dir, amount);
                }
            }
        }
    }

    private void setBeam(Floor floor, Direction direction, int amount) {
        if (direction == NORTH || direction == SOUTH) {
            if (amount == 5) {
                floor.setCourseMountedTractorBeamsNS(true);
            } else if (amount == 4) {
                floor.setCourseMountedTractorBeamsNS(true);
            } else {
                floor.setCourseMountedLaserBeamsNS(amount);
            }
        } else {
            if (amount == 5) {
                floor.setCourseMountedTractorBeamsWE(true);
            } else if (amount == 4) {
                floor.setCourseMountedTractorBeamsWE(true);
            } else {
                floor.setCourseMountedLaserBeamsWE(amount);
            }
        }
        Floor nextFloor = CourseHandler.getFloor(course, selectedFloor.getPosition().neighbour(direction));
        if (nextFloor.getPosition().inRange(course.getRange())) {
            WallType wallOutgoing = floor.wall(direction);
            WallType wallIncoming = nextFloor.wall(direction.reverse());
            if (wallOutgoing != WallType.SOLID && wallOutgoing != WallType.RAMP_UP && wallOutgoing != WallType.LEDGE && wallOutgoing != WallType.REPULSOR_FIELD
                    && wallOutgoing != WallType.ONE_WAY_RED && wallIncoming != WallType.SOLID && wallIncoming != WallType.REPULSOR_FIELD) {
                setBeam(nextFloor, direction, amount);
            }
        }
    }

    private class PusherButtonEvent extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Direction current = selectedFloor.isHasPusher() ? selectedFloor.getPusherDirection() : null;
            Direction next    = findNextPusherDirection(current);
            if (next == null) {
                selectedFloor.setHasPusher(false);
            } else {
                selectedFloor.setHasPusher(true);
                selectedFloor.setPusherDirection(next);
            }
            if (selectedFloor.isHasPusher()) {
                pusherButton.setIcon(imageSupplier.getImageIconPlain(selectedFloor.getPusherDirection()));
            } else {
                pusherButton.setIcon(imageSupplier.getImageIconPlain(GFX_NO_ACTION));
            }
            beamChooserNorth.setEnabled(isBeamAllowed(NORTH));
            beamChooserEast.setEnabled(isBeamAllowed(EAST));
            beamChooserSouth.setEnabled(isBeamAllowed(SOUTH));
            beamChooserWest.setEnabled(isBeamAllowed(WEST));
            redrawAll();
        }

        private Direction findNextPusherDirection(Direction current) {
            Direction[] states = {null, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            int         start  = 0;
            for (int i = 0; i < states.length; i++) {
                if (Objects.equals(states[i], current)) {
                    start = i;
                    break;
                }
            }
            for (int i = 1; i <= states.length; i++) {
                Direction candidate = states[(start + i) % states.length];
                if (candidate == null || isSolidWall(candidate)) {
                    return candidate;
                }
            }
            return null;
        }

        private boolean isSolidWall(Direction direction) {
            return switch (direction) {
                case NORTH -> selectedFloor.getWallSouth() == WallType.SOLID;
                case EAST -> selectedFloor.getWallWest() == WallType.SOLID;
                case SOUTH -> selectedFloor.getWallNorth() == WallType.SOLID;
                case WEST -> selectedFloor.getWallEast() == WallType.SOLID;
            };
        }
    }

    private void fillMissingCourseElementsWithAbyss() {
        Range       range         = course.getRange();
        List<Floor> factoryFloors = course.getFloor();
        for (int i = range.minX(); i <= range.maxX(); i++) {
            for (int j = range.minY(); j <= range.maxY(); j++) {
                boolean notFound = true;
                for (Floor factoryFloor : factoryFloors) {
                    if (factoryFloor.getPosition().x() == i && factoryFloor.getPosition().y() == j) {
                        notFound = false;
                        break;
                    }
                }
                if (notFound) {
                    Floor floor = new Floor();
                    floor.setFloortype(ABYSS);
                    floor.setPosition(new Position(i, j));
                    factoryFloors.add(floor);
                }
            }
        }
    }

    private void determineCourseProperties() {
        CourseProperties courseProperties = course.getProperties();
        for (Floor floor : course.getFloor()) {
            if (floor.isHasPusher()) {
                courseProperties.setPushers(true);
            }
            switch (floor.getFloortype()) {
                case GEARS_CCW, GEARS_CW -> courseProperties.setGears(true);
                case TRAPDOOR -> courseProperties.setTrapdoor(true);
                case CONVEYOR_BELT -> courseProperties.setConveyorBelts(true);
                case EXPRESS_CONVEYOR_BELT -> {
                    courseProperties.setConveyorBelts(true);
                    courseProperties.setExpressConveyorBelts(true);
                }
            }
            if (Math.max(Math.max(floor.getLasers()[0], floor.getLasers()[1]), Math.max(floor.getLasers()[2], floor.getLasers()[3])) > 0) {
                courseProperties.setLasers(true);
            }
            if (floor.getPressureBeam()[0] || floor.getPressureBeam()[1] || floor.getPressureBeam()[2] || floor.getPressureBeam()[3]) {
                courseProperties.setPressureBeams(true);
            }
            if (floor.getTractorBeam()[0] || floor.getTractorBeam()[1] || floor.getTractorBeam()[2] || floor.getTractorBeam()[3]) {
                courseProperties.setTractorBeams(true);
            }
        }
    }

}
