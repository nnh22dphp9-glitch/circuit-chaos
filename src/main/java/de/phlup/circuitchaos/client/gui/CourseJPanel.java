package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Floor;
import org.springframework.validation.annotation.Validated;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

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
        if (gui instanceof EditorGui editorGui) {
            label.addMouseListener(new SettingFloorMouseAdapter(editorGui, floor));
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

    public BufferedImage createPreview(int size) {
        if (getComponentCount() == 0) {
            throw new IllegalStateException("CourseJPanel has no content");
        }
        JPanel    contentPane  = (JPanel) getComponent(0);
        Dimension originalSize = contentPane.getPreferredSize();
        if (originalSize.width <= 0 || originalSize.height <= 0) {
            throw new IllegalStateException("Course has no content that can be rendered.");
        }
        contentPane.setSize(originalSize);
        contentPane.doLayout();
        BufferedImage original = new BufferedImage(
                originalSize.width,
                originalSize.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = original.createGraphics();
        try {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, originalSize.width, originalSize.height);
            contentPane.printAll(g);
        } finally {
            g.dispose();
        }
        BufferedImage preview = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        g = preview.createGraphics();
        try {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, size, size);
            double scale  = Math.min((double) size / original.getWidth(), (double) size / original.getHeight());
            int    width  = (int) Math.round(original.getWidth() * scale);
            int    height = (int) Math.round(original.getHeight() * scale);
            int    x      = (size - width) / 2;
            int    y      = (size - height) / 2;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, x, y, width, height, null);
        } finally {
            g.dispose();
        }
        return preview;
    }

}
