package de.phlup.circuitchaos.client.gui.editor;

import de.phlup.circuitchaos.client.gui.BaseGui;
import de.phlup.circuitchaos.client.gui.GuiHelper;
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
import java.util.List;

import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LOGO_SMALL;
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
    private final JLabel               activeInPhaseLabel = new JLabel("Trapdoor and Pusher phases: ");
    private final JCheckBox[]          activeInPhase      = new JCheckBox[]{new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox()};
    private final JLabel               waterLabel         = new JLabel("Water: ");
    private final JCheckBox            waterBox           = new JCheckBox();
    private final JLabel               wallsLabel         = new JLabel("Walls: ");
    private final JComboBox<WallType>  wallNorth          = new JComboBox<>();
    private final JComboBox<WallType>  wallEast           = new JComboBox<>();
    private final JComboBox<WallType>  wallSouth          = new JComboBox<>();
    private final JComboBox<WallType>  wallWest           = new JComboBox<>();

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
        adjustValuesToSelectedFloor();
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
            if (wt == WallType.NONE || wt == WallType.SOLID || wt == WallType.REPULSOR_FIELD) {
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

    public void adjustValuesToSelectedFloor() {
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

        // TODO add more attributes
        updatingFields = false;
    }

    private boolean wallTypeAllowed(WallType wt, int levelSecondFlor) {
        return (wt == WallType.SOLID || wt == WallType.REPULSOR_FIELD)
                || (levelSecondFlor == selectedFloor.getLevel() && (wt == WallType.NONE || wt == WallType.ONE_WAY_GREEN || wt == WallType.ONE_WAY_RED))
                || (levelSecondFlor == selectedFloor.getLevel() - 1 && wt == WallType.RAMP_DOWN)
                || (levelSecondFlor > selectedFloor.getLevel() && wt == WallType.LEDGE)
                || (levelSecondFlor == selectedFloor.getLevel() + 1 && wt == WallType.RAMP_UP);
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
