package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.common.model.Floor;
import lombok.RequiredArgsConstructor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@RequiredArgsConstructor
public class SettingFloorMouseAdapter extends MouseAdapter {

    private final EditorGui gui;
    private final Floor     floor;

    @Override
    public void mouseReleased(MouseEvent e) {
        if (gui == null) {
            return;
        }
        gui.setSelectedFloor(floor);
        gui.redrawAll();
        gui.adjustSelectorValuesToSelectedFloor();
    }

}
