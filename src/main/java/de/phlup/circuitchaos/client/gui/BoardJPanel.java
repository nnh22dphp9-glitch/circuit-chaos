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
public class BoardJPanel extends JPanel {

    private final ImageSupplier imageSupplier;

    public BoardJPanel(ImageSupplier imageSupplier) {
        super(true);
        setBackground(Color.black);
        this.imageSupplier = imageSupplier;
    }

    public void arrangeElements(GameGui game, Step step, Integer phase, Integer subPhase, int animationSteps) {
        GridBagLayout layout      = new GridBagLayout();
        JPanel        contentPane = new JPanel(layout, true);
        contentPane.setBackground(Color.black);
        contentPane.setOpaque(true);
        redrawAllElements(game, layout, contentPane, step, phase, subPhase, animationSteps);
        removeAll();
        add(contentPane);
        validate();
        game.getMainFrame().validate();
    }

    private void redrawAllElements(GameGui game, GridBagLayout layout, JPanel contentPane, Step step, Integer phase, Integer subPhase, int animationSteps) {
        for (Floor floor : game.getBoard().getFactoryFloor()) {
            createBoardElementLabel(game, layout, floor, contentPane);
            imageSupplier.redrawFloor(floor, game, step, phase, subPhase, animationSteps);
        }
    }

    private void createBoardElementLabel(GameGui game, GridBagLayout layout, Floor floor, JPanel contentPane) {
        if (floor != null) {
            JLabel             label = createJLabel(game, floor);
            GridBagConstraints gbc   = createGridBagConstraints(game, floor);
            layout.setConstraints(label, gbc);
            label.print(label.getGraphics());
            contentPane.add(label);
        }
    }

    private JLabel createJLabel(GameGui game, Floor floor) {
        JLabel label = new JLabel(imageSupplier.getImageIcon(game, floor));
        label.setToolTipText(ToolTipTexts.getToolTipText(game.getBoard(), floor));
        label.setOpaque(false);
        label.getInsets().set(0, 0, 0, 0);
        if (game.isNotStartedYet()) {
            label.addMouseListener(new CheckpointSettingMouseAdapter(game, floor));
        }
        return label;
    }

    private GridBagConstraints createGridBagConstraints(GameGui game, Floor floor) {
        return new GridBagConstraints(floor.getPosition().x() - game.getBoard().getRange().minX(),
                                      floor.getPosition().y() - game.getBoard().getRange().minY(),
                                      1, 1, 0, 0,
                                      GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                      new Insets(0, 0, 0, 0), 0, 0);
    }

}
