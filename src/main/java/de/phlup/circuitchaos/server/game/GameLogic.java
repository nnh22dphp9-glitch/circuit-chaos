package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.course.Course;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GameLogic {

    private final Course               course;
    private final CourseChangeListener listener;

    private void notifyCourseMayHaveChanged(Step step, Integer phase, String detail, String movingRobotName) {
        listener.courseChanged(step, phase, detail, movingRobotName);
    }

}
