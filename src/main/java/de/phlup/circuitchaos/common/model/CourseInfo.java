package de.phlup.circuitchaos.common.model;

import de.phlup.circuitchaos.common.model.course.Course;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class CourseInfo {

    private final String name;
    private       Course course;

    public String toString() {
        return name;
    }

}
