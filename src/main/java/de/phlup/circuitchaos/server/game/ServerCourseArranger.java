package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.Checkpoint;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.Position;

import java.util.Comparator;
import java.util.List;

public class ServerCourseArranger {

    public void setCourse(Game game, Course course) {
        CourseHandler.replaceGameCourse(course, game.getCourse());
        game.notifyCourseMayHaveChanged(Step.SETUP, null, "set course", null);
    }

    public void addCheckpoint(Game game, Position position) {
        Checkpoint checkpoint = CourseHandler.getCheckpoint(game.getCourse(), position);
        if (checkpoint != null) {
            throw new CircuitChaosException("Checkpoint already on position.");
        }
        List<Checkpoint> checkpoints = game.getCourse().getCheckpoints();
        int numberOfNewCheckpoint = checkpoints.isEmpty()
                ? 0
                : checkpoints.stream()
                             .max(Comparator.comparingInt(Checkpoint::getNumber))
                             .orElseThrow().getNumber() + 1;
        checkpoint = new Checkpoint();
        checkpoint.setNumber(numberOfNewCheckpoint);
        checkpoint.setPosition(position);
        checkpoints.add(checkpoint);
        game.notifyCourseMayHaveChanged(Step.SETUP, null, "checkpoint added", null);
    }

    public void removeCheckpoint(Game game, Position position) {
        Checkpoint checkpoint = CourseHandler.getCheckpoint(game.getCourse(), position);
        if (checkpoint == null) {
            throw new CircuitChaosException("Checkpoint not found.");
        }
        List<Checkpoint> checkpoints = game.getCourse().getCheckpoints();
        checkpoints.remove(checkpoint);
        adjustCheckpointNumbers(checkpoints);
        game.notifyCourseMayHaveChanged(Step.SETUP, null, "checkpoint removed", null);
    }

    public void adjustCheckpointNumbers(List<Checkpoint> checkpoints) {
        if (!checkpoints.isEmpty()) {
            int              number      = 0;
            List<Checkpoint> orderedList = checkpoints.stream().sorted(Comparator.comparingInt(Checkpoint::getNumber)).toList();
            for (Checkpoint cp : orderedList) {
                cp.setNumber(number++);
            }
        }
    }

}
