package de.phlup.circuitchaos.client.gui.editor;

import de.phlup.circuitchaos.client.gui.BaseGui;
import de.phlup.circuitchaos.client.service.CourseLoader;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;

@Slf4j
public class EditorGui extends BaseGui {

    private final CourseLoader courseLoader;

    private File currentFileDirectory;

    public EditorGui(@NotNull ResourceLoader resourceLoader,
                     @NotNull ClientSettings clientSettings,
                     @NotNull CourseLoader courseLoader) {
        super(resourceLoader, clientSettings, null);
        this.courseLoader = courseLoader;
        currentFileDirectory = new File(clientSettings.getCourseDirectories().stream()
                                                      .filter(s -> s.startsWith("file:")).findFirst()
                                                      .orElse("file:.").substring(5));
        state = CourseState.EDITOR;
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
        configureMainFrame("Course Editor");
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

    public void refreshCourse(Course course) {
        refreshCourse(course, null, Step.SETUP, null, null, 1, null);
    }

    public void refreshCourse(Course course, String reasonForCourseChange, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        setAndClearIfEmpty(course);
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
                refreshCourse(loadedCourse);
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

}
