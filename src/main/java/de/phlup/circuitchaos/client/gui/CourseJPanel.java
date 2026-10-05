package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Floor;
import org.springframework.validation.annotation.Validated;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

@Validated
public class CourseJPanel extends JPanel {

    private final ImageSupplier imageSupplier;

    public CourseJPanel(ImageSupplier imageSupplier) {
        super(true);
        setBackground(Color.black);
        this.imageSupplier = imageSupplier;
    }

    public void arrangeElements(BaseGui gui, Step step, Integer phase, Integer subPhase, int animationSteps, int nextCP) {
        GridBagLayout layout      = new GridBagLayout();
        JPanel        contentPane = new JPanel(layout, true);
        contentPane.setBackground(Color.black);
        contentPane.setOpaque(true);
        redrawAllElements(gui, layout, contentPane, step, phase, subPhase, animationSteps, nextCP);
        removeAll();
        add(contentPane);
        validate();
        gui.getMainFrame().validate();
    }

    private void redrawAllElements(BaseGui gui, GridBagLayout layout, JPanel contentPane, Step step, Integer phase, Integer subPhase, int animationSteps, int nextCP) {
        for (Floor floor : gui.getCourse().getFloor()) {
            createCourseElementLabel(gui, layout, floor, contentPane);
            imageSupplier.redrawFloor(floor, gui, step, phase, subPhase, animationSteps, nextCP);
        }
    }

    private void createCourseElementLabel(BaseGui gui, GridBagLayout layout, Floor floor, JPanel contentPane) {
        if (floor != null) {
            JLabel             label = createJLabel(gui, floor);
            GridBagConstraints gbc   = createGridBagConstraints(gui, floor);
            layout.setConstraints(label, gbc);
            label.print(label.getGraphics());
            contentPane.add(label);
        }
    }

    private JLabel createJLabel(BaseGui gui, Floor floor) {
        JLabel label = new JLabel(imageSupplier.getImageIcon(gui, floor));
        label.setToolTipText(ToolTipTexts.getToolTipText(gui.getCourse(), floor));
        label.setOpaque(false);
        label.getInsets().set(0, 0, 0, 0);
        if (gui.isNotStartedYet() && gui instanceof GameGui gameGui) { // TODO remove me
            label.addMouseListener(new CheckpointSettingMouseAdapter(gameGui, floor));
        }
        return label;
    }

    private GridBagConstraints createGridBagConstraints(BaseGui gui, Floor floor) {
        return new GridBagConstraints(floor.getPosition().x() - gui.getCourse().getRange().minX(),
                                      floor.getPosition().y() - gui.getCourse().getRange().minY(),
                                      1, 1, 0, 0,
                                      GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                      new Insets(0, 0, 0, 0), 0, 0);
    }

}
