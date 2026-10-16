package de.phlup.circuitchaos.server.player;

import de.phlup.circuitchaos.common.enums.ComputerType;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.model.Checkpoint;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Programme;
import de.phlup.circuitchaos.common.model.Robot;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.DistanceCalculator;
import de.phlup.circuitchaos.server.game.Game;
import de.phlup.circuitchaos.server.game.Sleep;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Setter
@Getter
@Slf4j
public class ComputerPlayer extends Player {

    private final boolean    aggressive;
    private final boolean    makesGoodMoves;
    private       Checkpoint nextCP;

    private boolean turnFinished;

    public ComputerPlayer(@NotNull Robot robot, @NotNull String gameId, @NotNull ComputerType computerType) {
        super(robot, gameId);
        this.aggressive = computerType == ComputerType.AGGRESSIVE;
        this.makesGoodMoves = computerType != ComputerType.STUPID;
    }

    public void perform() {
        if (robot.isPoweredDown()) {
            return;
        }
        Game game = GlobalServerAttributes.getGame(gameId);
        if (!(this.hasWon() || this.hasLost() || robot.isPoweredDown())) {
            if (!game.isGameAborted()) {
                this.nextTurn();
            }
            while (!turnFinished && !game.isGameAborted()) {
                Sleep.waitForWakeUp(game);
            }
        }
    }

    private void nextTurn() {
        Game game = GlobalServerAttributes.getGame(gameId);
        this.setTurnFinished(false);
        initTurn();
        initMoveInfo();
        if (isMakesGoodMoves()) {
            programRobot(game);
        } else {
            programCompletelyRandomly();
        }
        this.setTurnFinished(true);
    }

    private void programRobot(Game game) {
        Course           gameCourse = game.getCourse();
        ComputerMoveInfo bestProgram = evaluateAllPrograms(game, gameCourse);
        setBestProgram(bestProgram);
    }

    private void setBestProgram(ComputerMoveInfo chosenMove) {
        if (chosenMove != null) {
            robot.setDirection(chosenMove.getStartingDirection());
            robot.setPrevDirection(robot.getDirection());
            robot.setProgram(chosenMove.getProgram());
        } else {
            Iterator<Programme> i2 = robot.getPotentialProgramme().iterator();
            if (i2.hasNext() && !robot.getBlocked()[0]) {
                robot.getProgram()[0] = i2.next();
            }
            if (i2.hasNext() && !robot.getBlocked()[1]) {
                robot.getProgram()[1] = i2.next();
            }
            if (i2.hasNext() && !robot.getBlocked()[2]) {
                robot.getProgram()[2] = i2.next();
            }
            if (i2.hasNext() && !robot.getBlocked()[3]) {
                robot.getProgram()[3] = i2.next();
            }
            if (i2.hasNext() && !robot.getBlocked()[4]) {
                robot.getProgram()[4] = i2.next();
            }
            robot.setPowerDownAnnounced(true);
        }
    }

    private ComputerMoveInfo evaluateAllPrograms(Game game, Course gameCourse) {
        List<ComputerMoveInfo> programList = generatePrograms(game);
        programList.forEach(ComputerMoveInfo::simulateCourse);
        ComputerMoveInfo bestProgram = null;
        for (ComputerMoveInfo programme : programList) {
            bestProgram = evaluateProgram(programme, bestProgram, gameCourse);
        }
        return bestProgram;
    }

    private List<ComputerMoveInfo> generatePrograms(Game game) {
        List<ComputerMoveInfo> programList = new ArrayList<>();
        Programme[]      program    = new Programme[5];
        for (int pr1 = 0; pr1 < robot.getPotentialProgramme().size(); pr1++) {
            if (robot.getBlocked()[4]) {
                program[4] = robot.getProgram()[4];
            } else {
                program[4] = robot.getPotentialProgramme().get(pr1);
            }
            for (int pr2 = 0; pr2 < robot.getPotentialProgramme().size(); pr2++) {
                if (robot.getBlocked()[3]) {
                    program[3] = robot.getProgram()[3];
                } else {
                    program[3] = robot.getPotentialProgramme().get(pr2);
                }
                for (int pr3 = 0; pr3 < robot.getPotentialProgramme().size(); pr3++) {
                    if (robot.getBlocked()[2]) {
                        program[2] = robot.getProgram()[2];
                    } else {
                        program[2] = robot.getPotentialProgramme().get(pr3);
                    }
                    for (int pr4 = 0; pr4 < robot.getPotentialProgramme().size(); pr4++) {
                        if (robot.getBlocked()[1]) {
                            program[1] = robot.getProgram()[1];
                        } else {
                            program[1] = robot.getPotentialProgramme().get(pr4);
                        }
                        for (int pr5 = 0; pr5 < robot.getPotentialProgramme().size(); pr5++) {
                            if (robot.getBlocked()[0]) {
                                program[0] = robot.getProgram()[0];
                            } else {
                                program[0] = robot.getPotentialProgramme().get(pr5);
                            }
                            if (program[0] != program[1] && program[0] != program[2] && program[0] != program[3] && program[0] != program[4]
                                    && program[1] != program[2] && program[1] != program[3] && program[1] != program[4]
                                    && program[2] != program[3] && program[2] != program[4] && program[3] != program[4]
                                    && program[0] != null && program[1] != null && program[2] != null && program[3] != null && program[4] != null) {
                                programList.addAll(createProgrammeForEveryPossibleStartingDirection(game, program));
                            }
                        }
                    }
                }
            }
        }
        return programList;
    }

    private List<ComputerMoveInfo> createProgrammeForEveryPossibleStartingDirection(Game game, Programme[] program) {
        List<ComputerMoveInfo> programList = new ArrayList<>();
        if (robot.isMayChooseDirection()) {
            for (Direction dir : Direction.values()) {
                programList.add(createComputerMoveInfo(game, program, dir));
            }
        } else {
            programList.add(createComputerMoveInfo(game, program, robot.getDirection()));
        }
        return programList;
    }

    private ComputerMoveInfo createComputerMoveInfo(Game game, Programme[] program, Direction dir) {
        ComputerMoveInfo mi = new ComputerMoveInfo(game, nextCP, this);
        mi.setStartingDirection(dir);
        mi.setProgram(program);
        return mi;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private ComputerMoveInfo evaluateProgram(ComputerMoveInfo mi, ComputerMoveInfo chosenMove, Course gameCourse) {
        if (chosenMove == null) {
            chosenMove = mi;
        } else {
            boolean newMove = true;
            if (chosenMove.getStartingDirection() == mi.getStartingDirection()
                    && chosenMove.getProgram()[0].getType() == mi.getProgram()[0].getType()
                    && chosenMove.getProgram()[1].getType() == mi.getProgram()[1].getType()
                    && chosenMove.getProgram()[2].getType() == mi.getProgram()[2].getType()
                    && chosenMove.getProgram()[3].getType() == mi.getProgram()[3].getType()
                    && chosenMove.getProgram()[4].getType() == mi.getProgram()[4].getType()) {
                if (chosenMove.getProgram()[0].getPriority() > mi.getProgram()[0].getPriority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[1].getPriority() > mi.getProgram()[1].getPriority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[2].getPriority() > mi.getProgram()[2].getPriority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[3].getPriority() > mi.getProgram()[3].getPriority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[4].getPriority() > mi.getProgram()[4].getPriority()) {
                    newMove = false;
                } // else  "new move has better priority";
            } else if (isAggressive()) {
                if (mi.isWillDie() && !chosenMove.isWillDie()) {
                    newMove = false; // "old move won't result in death";
                } else if (chosenMove.isWillDie() && !mi.isWillDie()) {
                    // "new move won't result in death";
                } else if (chosenMove.getProbableTargets() > mi.getProbableTargets()) {
                    newMove = false;
                    // "old move has by far more probable targets (" + chosenMove.getProbableTargets() + " <-> " + mi.getProbableTargets() + ")";
                } else if (chosenMove.getProbableTargets() < mi.getProbableTargets()) {
                    // "new move has by far more probable targets (" + chosenMove.getProbableTargets() + " <-> " + mi.getProbableTargets() + ")";
                } else if (chosenMove.getDamage() < mi.getDamage() - 3) {
                    newMove = false;
                    // "old move has by far less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.getDamage() > mi.getDamage() + 3) {
                    // "new move has by far less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.isReachesPitStop() && !mi.isReachesPitStop()) {
                    newMove = false;
                    // "old move reaches pit stop";
                } else if (mi.isReachesPitStop() && !chosenMove.isReachesPitStop()) {
                    // "new move reaches pit stop";
                } else if (chosenMove.getReachesCheckpoint() < mi.getReachesCheckpoint()) {
                    newMove = false;
                    // "old move reaches checkpoint earlier";
                } else if (chosenMove.getReachesCheckpoint() > mi.getReachesCheckpoint()) {
                    // "new move reaches checkpoint earlier";
                } else if (chosenMove.getDistance2() < mi.getDistance2() - 2) {
                    newMove = false;
                    // "old move has by far better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                } else if (chosenMove.getDistance2() > mi.getDistance2() + 2) {
                    // "new move has by far better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                } else if (chosenMove.isCopiesArchive() && !mi.isCopiesArchive()) {
                    newMove = false;
                    // "old move reaches archive point";
                } else if (mi.isCopiesArchive() && !chosenMove.isCopiesArchive()) {
                    // "new move reaches archive point";
                } else if (chosenMove.getDamage() < mi.getDamage()) {
                    newMove = false;
                    // "old move has less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.getDamage() > mi.getDamage()) {
                    // "new move has less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.getReachesCheckpoint() == 6) {
                    if (chosenMove.getDistance2() < 5 || mi.getDistance2() < 5) {
                        if (mi.getDistance() == Integer.MAX_VALUE) {
                            mi.setDistance(DistanceCalculator.calculateDistance(gameCourse, new Position(mi.getX(), mi.getY()), nextCP.getPosition(), 100));
                        }
                        if (chosenMove.getDistance() == Integer.MAX_VALUE) {
                            chosenMove.setDistance(
                                    DistanceCalculator
                                            .calculateDistance(gameCourse, new Position(chosenMove.getX(), chosenMove.getY()), nextCP.getPosition(), 100));
                        }
                    }
                    if ((chosenMove.getDistance2() < 5 || mi.getDistance2() < 5) && chosenMove.getDistance() < mi.getDistance()) {
                        newMove = false;
                        // "old move has better distance (" + chosenMove.getDistance() + " <-> " + mi.getDistance() + ")";
                    } else if ((chosenMove.getDistance2() < 5 || mi.getDistance2() < 5) && chosenMove.getDistance() > mi.getDistance()) {
                        // "new move has better distance (" + chosenMove.getDistance() + " <-> " + mi.getDistance() + ")";
                    } else if ((chosenMove.getDistance2() >= 5 && mi.getDistance2() >= 5) && chosenMove.getDistance2() < mi.getDistance2()) {
                        newMove = false;
                        // "old move has better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                    } else if ((chosenMove.getDistance2() >= 5 && mi.getDistance2() >= 5) && chosenMove.getDistance2() > mi.getDistance2()) {
                        // "new move has better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                    } else if (chosenMove.isStandsOnOpenFloor() && !mi.isStandsOnOpenFloor()) {
                        newMove = false;
                        // "old move will end on open floor";
                    } else if (mi.isStandsOnOpenFloor() && !chosenMove.isStandsOnOpenFloor()) {
                        // "new move will end on open floor";
                    } else if (chosenMove.isFacesTowardsCheckpoint() && !mi.isFacesTowardsCheckpoint()) {
                        newMove = false;
                        // "old move faces towards checkpoint";
                    } else if (mi.isFacesTowardsCheckpoint() && !chosenMove.isFacesTowardsCheckpoint()) {
                        // "new move faces towards checkpoint";
                    } else if (mi.isKeepsStaying() && !chosenMove.isKeepsStaying()) {
                        newMove = false;
                        // "old move won't result in doing nothing";
                    } else if (chosenMove.isKeepsStaying() && !mi.isKeepsStaying()) {
                        // "new move won't result in doing nothing";
                    } // else "new move makes no difference";
                } // else  "new move makes no difference";
            } else {
                if (chosenMove.getReachesCheckpoint() < mi.getReachesCheckpoint()) {
                    newMove = false;
                    // "old move reaches checkpoint earlier";
                } else if (chosenMove.getReachesCheckpoint() > mi.getReachesCheckpoint()) {
                    // "new move reaches checkpoint earlier";
                } else if (mi.isWillDie() && !chosenMove.isWillDie()) {
                    newMove = false;
                    // "old move won't result in death";
                } else if (chosenMove.isWillDie() && !mi.isWillDie()) {
                    // "new move won't result in death";
                } else if (chosenMove.getDistance2() < mi.getDistance2() - 2) {
                    newMove = false;
                    // "old move has by far better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                } else if (chosenMove.getDistance2() > mi.getDistance2() + 2) {
                    // "new move has by far better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                } else if (chosenMove.getDamage() < mi.getDamage() - 3) {
                    newMove = false;
                    // "old move has by far less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.getDamage() > mi.getDamage() + 3) {
                    // "new move has by far less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.isReachesPitStop() && !mi.isReachesPitStop()) {
                    newMove = false;
                    // "old move reaches pit stop";
                } else if (mi.isReachesPitStop() && !chosenMove.isReachesPitStop()) {
                    // "new move reaches pit stop";
                } else if (chosenMove.isCopiesArchive() && !mi.isCopiesArchive()) {
                    newMove = false;
                    // "old move reaches archive point";
                } else if (mi.isCopiesArchive() && !chosenMove.isCopiesArchive()) {
                    // "new move reaches archive point";
                } else if (chosenMove.getDamage() < mi.getDamage()) {
                    newMove = false;
                    // "old move has less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.getDamage() > mi.getDamage()) {
                    // "new move has less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
                } else if (chosenMove.getReachesCheckpoint() == 6) {
                    if (chosenMove.getDistance2() < 5 || mi.getDistance2() < 5) {
                        if (mi.getDistance() == Integer.MAX_VALUE) {
                            mi.setDistance(DistanceCalculator.calculateDistance(gameCourse, new Position(mi.getX(), mi.getY()), nextCP.getPosition(), 100));
                        }
                        if (chosenMove.getDistance() == Integer.MAX_VALUE) {
                            chosenMove.setDistance(
                                    DistanceCalculator
                                            .calculateDistance(gameCourse, new Position(chosenMove.getX(), chosenMove.getY()), nextCP.getPosition(), 100));
                        }
                    }
                    if ((chosenMove.getDistance2() < 5 || mi.getDistance2() < 5) && chosenMove.getDistance() < mi.getDistance()) {
                        newMove = false;
                        // "old move has better distance (" + chosenMove.getDistance() + " <-> " + mi.getDistance() + ")";
                    } else if ((chosenMove.getDistance2() < 5 || mi.getDistance2() < 5) && chosenMove.getDistance() > mi.getDistance()) {
                        // "new move has better distance (" + chosenMove.getDistance() + " <-> " + mi.getDistance() + ")";
                    } else if ((chosenMove.getDistance2() >= 5 && mi.getDistance2() >= 5) && chosenMove.getDistance2() < mi.getDistance2()) {
                        newMove = false;
                        // "old move has better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                    } else if ((chosenMove.getDistance2() >= 5 && mi.getDistance2() >= 5) && chosenMove.getDistance2() > mi.getDistance2()) {
                        // "new move has better distance (" + chosenMove.getDistance2() + " <-> " + mi.getDistance2() + ")";
                    } else if (chosenMove.getProbableTargets() > mi.getProbableTargets()) {
                        newMove = false;
                        // "old move has more probable targets (" + chosenMove.getProbableTargets() + " <-> " + mi.getProbableTargets() + ")";
                    } else if (chosenMove.getProbableTargets() < mi.getProbableTargets()) {
                        // "new move has more probable targets (" + chosenMove.getProbableTargets() + " <-> " + mi.getProbableTargets() + ")";
                    } else if (chosenMove.isStandsOnOpenFloor() && !mi.isStandsOnOpenFloor()) {
                        newMove = false;
                        // "old move will end on open floor";
                    } else if (mi.isStandsOnOpenFloor() && !chosenMove.isStandsOnOpenFloor()) {
                        // "new move will end on open floor";
                    } else if (chosenMove.isFacesTowardsCheckpoint() && !mi.isFacesTowardsCheckpoint()) {
                        newMove = false; // "old move faces towards checkpoint";
                    } else if (mi.isFacesTowardsCheckpoint() && !chosenMove.isFacesTowardsCheckpoint()) {
                        // "new move faces towards checkpoint";
                    } else if (mi.isKeepsStaying() && !chosenMove.isKeepsStaying()) {
                        newMove = false;
                        // "old move won't result in doing nothing";
                    } // else  "new move won't result in doing nothing"; || "new move makes no difference";
                } // else "new move makes no difference";
            }

            if (newMove) {
                chosenMove = mi;
            }
        }
        return chosenMove;
    }

    private void programCompletelyRandomly() {
        if (robot.isMayChooseDirection()) {
            robot.setDirection(Direction.values()[GlobalServerAttributes.RANDOM.nextInt(4)]);
            robot.setPrevDirection(robot.getDirection());
        }
        Iterator<Programme> i = robot.getPotentialProgramme().iterator();
        for (int j = 0; j < 5; j++) {
            if (!robot.getBlocked()[j]) {
                robot.getProgram()[j] = i.next();
            }
        }
    }

    private void initMoveInfo() {
        Game game = GlobalServerAttributes.getGame(gameId);
        nextCP = null;
        for (Checkpoint cp : game.getCourse().getCheckpoints()) {
            if (cp.getNumber() == robot.getNextCheckpoint()) {
                nextCP = cp;
                break;
            }
        }
    }

}
