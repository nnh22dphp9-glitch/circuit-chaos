package de.phlup.circuitchaos.client.gui.editor;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.client.gui.BaseGui;
import de.phlup.circuitchaos.client.service.CourseLoader;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.core.io.ResourceLoader;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EditorGui extends BaseGui {

    private final CourseLoader courseLoader;
    private final ObjectMapper objectMapper;

    @Getter
    private final boolean notStartedYet = true;

    public EditorGui(@NotNull ResourceLoader resourceLoader,
                     @NotNull ClientSettings clientSettings,
                     @NotNull CourseLoader courseLoader,
                     @NotNull ObjectMapper objectMapper) {
        super(resourceLoader, clientSettings, null);
        this.courseLoader = courseLoader;
        this.objectMapper = objectMapper;
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
        editorMenu.add(createZoomMenu());
        // TODO build new help menu
        //  JMenu helpMenu = new JMenu("Help");
        //  menuBar.add(helpMenu);
        //  new HelpMenu(imageSupplier).createHelpMenu(helpMenu);
        frame.setJMenuBar(menuBar);
    }

    public void refreshCourse(Course course, String reasonForCourseChange, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName) {
        setAndClearIfEmpty(course);
        synchronized (courseJPanel) {
            courseJPanel.arrangeElements(this, Step.SETUP, null, null, 1, -1);
        }
    }

}
