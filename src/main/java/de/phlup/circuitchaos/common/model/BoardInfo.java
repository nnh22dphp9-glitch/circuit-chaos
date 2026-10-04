package de.phlup.circuitchaos.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.awt.Image;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class BoardInfo {

    private final String name;
    private       Image  image;
    private       Board  board;

    public String toString() {
        return name;
    }

}
