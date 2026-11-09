package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.model.course.Checkpoint;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Range;
import de.phlup.circuitchaos.server.game.DistanceCalculator;

/**
 * Evaluates simulated moves for the FINISH goal.
 */
public class MoveEvaluator {

    private static final int CHECKPOINT_SCORE = 1_000;
    private static final int CHECKPOINT_PHASE_SCORE = 10;
    private static final int WIN_SCORE = 1_000_000;
    private static final int DEATH_PENALTY = 400;
    private static final int DAMAGE_SCORE = 40;
    private static final int PIT_STOP_SCORE = 20;
    private static final int ARCHIVE_SCORE = 10;
    private static final int DISTANCE_SCORE = 10;
    private static final int MAX_PATH_DISTANCE = 8;

    public double evaluate(MoveInfo move, Course course) {
        if (move.isWon()) {
            return WIN_SCORE;
        }

        double score = move.getCheckpointsReached() * CHECKPOINT_SCORE;
        score += move.getCheckpointPhaseBonus() * CHECKPOINT_PHASE_SCORE;

        score -= calculateDistance(move, course) * DISTANCE_SCORE;
        score -= move.getDamage() * DAMAGE_SCORE;

        if (move.isWillDie()) {
            score -= DEATH_PENALTY;
        }
        if (move.isReachesPitStop()) {
            score += PIT_STOP_SCORE;
        }
        if (move.isCopiesArchive()) {
            score += ARCHIVE_SCORE;
        }
        if (move.isFacesTowardsCheckpoint()) {
            score += 2;
        }
        if (move.isStandsOnOpenFloor()) {
            score += 1;
        }

        return score;
    }

    private double calculateDistance(MoveInfo move, Course course) {
        Checkpoint target = null;
        for (Checkpoint checkpoint : course.getCheckpoints()) {
            if (checkpoint.getNumber() == move.getNextCheckpointNumber()) {
                target = checkpoint;
                break;
            }
        }
        if (target == null) {
            return 0;
        }

        Position position = new Position(move.getX(), move.getY());
        double geometricDistance = move.getDistance2();
        Range range = Range.rangeAroundPosition(position, MAX_PATH_DISTANCE);
        if (!target.getPosition().inRange(range) || geometricDistance > MAX_PATH_DISTANCE) {
            return geometricDistance;
        }

        DistanceCalculator.calculateDistance(course, position, MAX_PATH_DISTANCE, range);
        return CourseHandler.getFloor(course, target.getPosition()).getDistanceCounter();
    }
}
