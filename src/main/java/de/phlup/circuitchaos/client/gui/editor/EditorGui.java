package de.phlup.circuitchaos.client.gui.editor;

import de.phlup.circuitchaos.client.gui.BaseGui;
import de.phlup.circuitchaos.client.gui.GuiHelper;
import de.phlup.circuitchaos.client.service.CourseLoader;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LOGO_SMALL;

@Slf4j
public class EditorGui extends BaseGui implements GuiHelper {

    private final CourseLoader courseLoader;
    private final JFrame       editorFrame;

    private File currentFileDirectory;

    private JComboBox<Floortype> floortypeBox;

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
        course = new Course();
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
        addComponentToPanel(new JLabel(imageSupplier.getImageIconPlain(GFX_LOGO_SMALL)), courseEditorPanel, layout,
                            0, y, 2, GridBagConstraints.EAST);

        y++;
        floortypeBox = new JComboBox<>();
        for (Floortype ft : Floortype.values()) {
            floortypeBox.addItem(ft);
        }
        floortypeBox.setEnabled(false);
        floortypeBox.addActionListener(floorChangedEvent);
        ;
        addComponentToPanel(floortypeBox, courseEditorPanel, layout, 0, y, 2, GridBagConstraints.EAST);

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

        if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
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

        if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            currentFileDirectory = path.toFile();
            if (!path.toString().toLowerCase().endsWith(".json")) {
                path = path.resolveSibling(path.getFileName() + ".json");
            }
            courseLoader.saveCourse(course, path); // TODO auch die preview speichern
        }
    }

    public void adjustValuesToSelectedFloor() {
        floortypeBox.setSelectedItem(selectedFloor.getFloortype());
        floortypeBox.setEnabled(true);
    }

    private class FloorChangedEvent extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            selectedFloor.setFloortype((Floortype) floortypeBox.getSelectedItem());
            redrawAll();
        }
    }

}
