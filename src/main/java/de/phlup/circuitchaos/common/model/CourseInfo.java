package de.phlup.circuitchaos.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.awt.Image;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class CourseInfo {

    private final String name;
    private       Image  image;
    private       Course course;

    public String toString() {
        return name;
    }

}
