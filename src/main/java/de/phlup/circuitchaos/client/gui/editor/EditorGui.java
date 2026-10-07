package de.phlup.circuitchaos.client.gui.editor;

import de.phlup.circuitchaos.client.gui.BaseGui;
import de.phlup.circuitchaos.client.gui.GuiHelper;
import de.phlup.circuitchaos.client.service.CourseLoader;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.Step;
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
import static de.phlup.circuitchaos.common.enums.Floortype.ABYSS;
import static de.phlup.circuitchaos.common.enums.Floortype.TRAPDOOR;
import static javax.swing.JFileChooser.APPROVE_OPTION;

@Slf4j
public class EditorGui extends BaseGui implements GuiHelper {

    private static final int MAX_WIDTH = 6;

    private final CourseLoader courseLoader;
    private final JFrame       editorFrame;

    private File currentFileDirectory;

    private final JComboBox<Floortype> floortypeBox       = new JComboBox<>();
    private final JLabel               directionLabel     = new JLabel("Conveyor Belt Direction: ");
    private final JButton              directionButton    = new JButton();
    private final JLabel               activeInPhaseLabel = new JLabel("Trapdoor and Pusher activate at ");
    private final JCheckBox[]          activeInPhase      = new JCheckBox[]{new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox()};
    private final JLabel               waterLabel         = new JLabel("Water: ");
    private final JCheckBox            waterBox           = new JCheckBox();

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
        addComponentToPanel(new JLabel(imageSupplier.getImageIconPlain(GFX_LOGO_SMALL)), courseEditorPanel, layout, 0, y++, MAX_WIDTH, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(" "), courseEditorPanel, layout, 0, y++, 6, GridBagConstraints.EAST);

        for (Floortype ft : Floortype.values()) {
            floortypeBox.addItem(ft);
        }
        floortypeBox.setEnabled(false);
        floortypeBox.addActionListener(floorChangedEvent);
        addComponentToPanel(floortypeBox, courseEditorPanel, layout, 0, y++, 6, GridBagConstraints.EAST);

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
        addComponentToPanel(directionButton, courseEditorPanel, layout, 1, y++, 5, GridBagConstraints.WEST);

        for (int i = 0; i < 5; i++) {
            activeInPhase[i].setSelected(false);
            activeInPhase[i].setEnabled(false);
            activeInPhase[i].addActionListener(floorChangedEvent);
        }
        activeInPhaseLabel.setEnabled(false);
        addComponentToPanel(activeInPhaseLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        addComponentToPanel(activeInPhase[0], courseEditorPanel, layout, 1, y, 1, GridBagConstraints.EAST);
        addComponentToPanel(activeInPhase[1], courseEditorPanel, layout, 2, y, 1, GridBagConstraints.EAST);
        addComponentToPanel(activeInPhase[2], courseEditorPanel, layout, 3, y, 1, GridBagConstraints.EAST);
        addComponentToPanel(activeInPhase[3], courseEditorPanel, layout, 4, y, 1, GridBagConstraints.EAST);
        addComponentToPanel(activeInPhase[4], courseEditorPanel, layout, 5, y++, 1, GridBagConstraints.EAST);

        waterLabel.setEnabled(false);
        addComponentToPanel(waterLabel, courseEditorPanel, layout, 0, y, 1, GridBagConstraints.EAST);
        waterBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                selectedFloor.setWater(waterBox.isSelected());
                redrawAll();
            }
        });
        waterBox.setEnabled(false);
        addComponentToPanel(waterBox, courseEditorPanel, layout, 1, y++, 1, GridBagConstraints.WEST);

        // TODO add more attributes

        addComponentToPanel(new JLabel(" "), courseEditorPanel, layout, 0, y++, 6, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(imageSupplier.getImageIconPlain(GFX_PICTURE)), courseEditorPanel, layout, 0, y, MAX_WIDTH, GridBagConstraints.EAST);
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
        Floortype newFloortype = selectedFloor.getFloortype();
        floortypeBox.setSelectedItem(newFloortype);
        floortypeBox.setEnabled(true);
        directionLabel.setEnabled(newFloortype.isConveyorBelt());
        directionButton.setIcon(imageSupplier.getImageIconPlain(selectedFloor.getFacingDirection()));
        directionButton.setEnabled(newFloortype.isConveyorBelt());
        waterLabel.setEnabled(true);
        waterBox.setEnabled(true);
        waterBox.setSelected(selectedFloor.isWater());
        for (int i = 0; i < 5; i++) {
            boolean activationValid = newFloortype == TRAPDOOR || selectedFloor.isHasPusher();
            activeInPhase[i].setSelected(activationValid && selectedFloor.getActiveInPhase()[i]);
            activeInPhase[i].setEnabled(activationValid);
            activeInPhaseLabel.setEnabled(activationValid);
        }
        // TODO add more attributes
    }

    private class FloorChangedEvent extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
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
                // TODO add more attributes
                course.setRange(course.getRange().wideRangeToPosition(selectedFloor.getPosition()));
                fillMissingCourseElementsWithAbyss();
                determineCourseProperties();
                redrawAll();
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
