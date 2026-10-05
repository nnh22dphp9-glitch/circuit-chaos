package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.client.service.AudioSupplier;
import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public abstract class BaseGui {

    protected final ResourceLoader resourceLoader;
    protected final ClientSettings clientSettings;
    protected final AudioSupplier  audioSupplier;
    protected final ImageSupplier  imageSupplier;

    protected final CourseJPanel courseJPanel;
    protected       Course       course;

    protected final JFrame mainFrame = new JFrame();

    protected       float                        zoomFactor;
    protected final Map<Position, BufferedImage> images = new HashMap<>();

    public BaseGui(@NotNull ResourceLoader resourceLoader,
                   @NotNull ClientSettings clientSettings,
                   @Nullable AudioSupplier audioSupplier) {
        this.resourceLoader = resourceLoader;
        this.clientSettings = clientSettings;
        this.audioSupplier = audioSupplier;
        ClientSettings.Theme theme = determineTheme();
        if (audioSupplier != null) {
            this.audioSupplier.setThemePath(theme.getPath());
        }
        this.imageSupplier = new ImageSupplier(theme, resourceLoader);
        courseJPanel = new CourseJPanel(imageSupplier);
        this.zoomFactor = clientSettings.getDefaultZoom();
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

    protected void configureMainFrame(String windowTitle) {
        JScrollPane coursePane = new JScrollPane(courseJPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        coursePane.setPreferredSize(new Dimension(clientSettings.getDefaultCourseSizeX(), clientSettings.getDefaultCourseSizeY()));
        mainFrame.setTitle(windowTitle);
        mainFrame.setContentPane(coursePane);
        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.setResizable(true);
        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.requestFocus();
    }

    protected JMenu createZoomMenu() {
        JMenu       zoom      = new JMenu("Zoom");
        ButtonGroup zoomGroup = new ButtonGroup();
        createZoomButton("25 %", zoomGroup, zoom);
        createZoomButton("50 %", zoomGroup, zoom);
        createZoomButton("75 %", zoomGroup, zoom);
        createZoomButton("100 %", zoomGroup, zoom);
        createZoomButton("125 %", zoomGroup, zoom);
        createZoomButton("150 %", zoomGroup, zoom);
        createZoomButton("175 %", zoomGroup, zoom);
        createZoomButton("200 %", zoomGroup, zoom);
        return zoom;
    }

    private void createZoomButton(String title, ButtonGroup zoomGroup, JMenuItem zoom) {
        JRadioButtonMenuItem zoomButton = new JRadioButtonMenuItem(title);
        if (title.equals(((int) (zoomFactor * 100)) + " %")) {
            zoomButton.setSelected(true);
        }
        zoomButton.addActionListener(this::zoomButtonAction);
        zoomGroup.add(zoomButton);
        zoom.add(zoomButton);
    }

    private void zoomButtonAction(ActionEvent e) {
        JRadioButtonMenuItem button    = (JRadioButtonMenuItem) e.getSource();
        float                newFactor = Float.parseFloat(button.getText().substring(0, button.getText().indexOf(" "))) / 100;
        if (newFactor != zoomFactor) {
            Graphics2D gr = (Graphics2D) courseJPanel.getGraphics();
            gr.setBackground(Color.black);
            gr.clearRect(0, 0, courseJPanel.getWidth(), courseJPanel.getHeight());
            gr.dispose();
            zoomFactor = newFactor;
            refreshCourse(course, "zoom changed", Step.SETUP, null, null, 1, null);
        }
    }

    protected void setAndClearIfEmpty(Course course) {
        this.course = course;
        if (course.getFloor().isEmpty()) {
            Graphics2D gr = (Graphics2D) courseJPanel.getGraphics();
            gr.setBackground(Color.black);
            gr.clearRect(0, 0, courseJPanel.getWidth(), courseJPanel.getHeight());
            gr.dispose();
        }
    }

    public BufferedImage getImageByFloor(Floor f) {
        return images.get(f.getPosition());
    }

    public void putImageOfFloor(Floor f, BufferedImage bi) {
        images.put(f.getPosition(), bi);
    }

    public abstract boolean isNotStartedYet();

    public abstract void refreshCourse(Course course, String reasonForCourseChange, Step step, Integer phase, Integer subPhase, int animationSteps, String movingRobotName);

}

