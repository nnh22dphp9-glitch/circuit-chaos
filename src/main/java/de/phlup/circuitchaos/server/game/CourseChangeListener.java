package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.Step;

import java.util.UUID;

public interface CourseChangeListener {

    void courseChanged(Step step, Integer phase, String detail, UUID movingRobotId);

}
