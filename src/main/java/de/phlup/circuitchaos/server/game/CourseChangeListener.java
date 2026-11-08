package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Step;

public interface CourseChangeListener {

    void courseChanged(Step step, Integer phase, String detail, String movingRobotName);

}
