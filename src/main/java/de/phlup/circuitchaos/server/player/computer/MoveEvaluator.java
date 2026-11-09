package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.model.course.Checkpoint;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Range;
import de.phlup.circuitchaos.server.game.DistanceCalculator;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Evaluates simulated moves for the FINISH goal.
 */
public class MoveEvaluator {

    private static final int CHECKPOINT_SCORE = 1_000;
    private static final int WIN_SCORE = 1_000_000;
    private static final int MAX_PATH_DISTANCE = 8;

    private final EvaluationWeights weights;

    public MoveEvaluator() {
        weights = EvaluationWeights.createRandomized();
    }

    public double evaluate(MoveInfo move, Course course) {
        if (move.isWon()) {
            return WIN_SCORE;
        }

        double score = move.getCheckpointsReached() * CHECKPOINT_SCORE;
        score += move.getCheckpointPhaseBonus() * weights.checkpointPhaseScore();

        score -= calculateDistance(move, course) * weights.distanceScore();
        score -= move.getDamage() * weights.damageScore();

        if (move.isWillDie()) {
            score -= weights.deathPenalty();
        }
        if (move.isReachesPitStop()) {
            score += weights.pitStopScore();
        }
        if (move.isCopiesArchive()) {
            score += weights.archiveScore();
        }
        if (move.isFacesTowardsCheckpoint()) {
            score += weights.facingCheckpointScore();
        }
        if (move.isStandsOnOpenFloor()) {
            score += weights.openFloorScore();
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

    private record EvaluationWeights(
            double checkpointPhaseScore,
            double deathPenalty,
            double damageScore,
            double pitStopScore,
            double archiveScore,
            double distanceScore,
            double facingCheckpointScore,
            double openFloorScore) {

        private static EvaluationWeights createRandomized() {
            return new EvaluationWeights(
                    vary(10, 0.40),
                    vary(400, 0.20),
                    vary(40, 0.30),
                    vary(20, 0.50),
                    vary(10, 0.50),
                    vary(10, 0.30),
                    vary(2, 0.50),
                    vary(1, 0.50));
        }

        private static double vary(double baseValue, double variation) {
            return baseValue * ThreadLocalRandom.current().nextDouble(1.0 - variation, 1.0 + variation);
        }
    }
}
