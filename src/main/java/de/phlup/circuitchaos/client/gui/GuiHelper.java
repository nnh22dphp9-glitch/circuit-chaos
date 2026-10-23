package de.phlup.circuitchaos.client.gui;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public interface GuiHelper {

    default void addComponentToPanel(Component component, JPanel panel, GridBagLayout layout, int x, int y, int width, int orientation) {
        layout.setConstraints(component,
                              new GridBagConstraints(x, y, width, 1, 0, 0, orientation,
                                                     orientation == GridBagConstraints.EAST ? GridBagConstraints.BOTH : GridBagConstraints.NONE,
                                                     new Insets(0, 0, 0, 0), 0, 0));
        panel.add(component);
    }

}
