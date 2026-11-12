package de.phlup.circuitchaos.server.player.computer;

import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Goal;
import de.phlup.circuitchaos.common.model.course.Checkpoint;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.Programme;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.Game;
import de.phlup.circuitchaos.server.game.GameLogic;
import de.phlup.circuitchaos.server.game.Sleep;
import de.phlup.circuitchaos.server.player.Player;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Slf4j
public class ComputerPlayer extends Player {

    private final Goal goal;
    private final MoveEvaluator moveEvaluator = new MoveEvaluator();

    private Checkpoint nextCP;
    private boolean    turnFinished;
    private GameLogic  gameLogic;

    public ComputerPlayer(@NotNull Robot robot, @NotNull String gameId, @NotNull Goal goal) {
        super(robot, gameId);
        this.goal = goal;
    }

    public void perform(int numberOfCheckpoints) {
        if (robot.isPoweredDown()) {
            return;
        }
        Game game = GlobalServerAttributes.getGame(gameId);
        if (!(robot.hasWon(goal, numberOfCheckpoints) || robot.hasLost(goal) || robot.isPoweredDown())) {
            if (!game.isGameAborted()) {
                this.nextTurn(game);
            }
            while (!turnFinished && !game.isGameAborted()) {
                Sleep.waitForWakeUp(game);
            }
        }
    }

    private void nextTurn(Game game) {
        this.setTurnFinished(false);
        initTurn();
        Course course = game.getCourse().copyForSimulation();
        gameLogic = new GameLogic(course, (step, phase, detail, movingRobotName) -> {
        });
        initMoveInfo(course);
        programRobot(course);
        this.setTurnFinished(true);
    }

    private void programRobot(Course course) {
        log.info("{}: evaluating programs", robot.getName());
        MoveInfo bestProgram = evaluateAllPrograms(course);
        log.info("{}: evaluating best program", robot.getName());
        setBestProgram(bestProgram);
        log.info("{}: program finished", robot.getName());
    }

    private void setBestProgram(MoveInfo chosenMove) {
        if (chosenMove != null) {
            robot.setDirection(chosenMove.getStartingDirection());
            robot.setPrevDirection(robot.getDirection());
            robot.setProgram(chosenMove.getProgram());
            for (var module : robot.getModules()) {
                if (module.getType().isWeapon()) {
                    for (int phase = 0; phase < 5; phase++) {
                        module.getActiveInPhase()[phase] = module.getType() == chosenMove.getSelectedWeapons()[phase];
                    }
                }
            }
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

    private MoveInfo evaluateAllPrograms(Course gameCourse) {
        List<MoveInfo> programList = generatePrograms();
        programList.forEach(MoveInfo::simulateCourse);
        MoveInfo bestProgram = null;
        for (MoveInfo programme : programList) {
            bestProgram = evaluateProgram(programme, bestProgram, gameCourse);
        }
        return bestProgram;
    }

    private List<MoveInfo> generatePrograms() {
        List<MoveInfo> programList = new ArrayList<>();
        Programme[]    program     = new Programme[5];
        // use blocked registers
        for (int slot = 0; slot < program.length; slot++) {
            if (robot.getBlocked()[slot]) {
                program[slot] = robot.getProgram()[slot];
                if (program[slot] == null) {
                    return programList; // illegal case: a blocked register without a program step
                }
            }
        }
        Set<Programme> usedPrograms = Collections.newSetFromMap(new IdentityHashMap<>());
        // blocked registers are already used
        for (int slot = 0; slot < program.length; slot++) {
            if (robot.getBlocked()[slot] && !usedPrograms.add(program[slot])) {
                return programList; // illegal: one program step used twice
            }
        }
        generatePrograms(goal, 0, program, usedPrograms, programList);
        return programList;
    }

    private void generatePrograms(Goal goal, int slot, Programme[] program, Set<Programme> usedPrograms, List<MoveInfo> programList) {
        if (slot == program.length) {
            programList.addAll(createProgrammeForEveryPossibleStartingDirection(goal, program));
            return;
        }
        // blocked registers are already set
        if (robot.getBlocked()[slot]) {
            generatePrograms(goal, slot + 1, program, usedPrograms, programList);
            return;
        }
        for (Programme candidate : robot.getPotentialProgramme()) {
            if (candidate != null && usedPrograms.add(candidate)) { // identity constraint: don't use a program step twice
                program[slot] = candidate;
                generatePrograms(goal, slot + 1, program, usedPrograms, programList);
                usedPrograms.remove(candidate); // backtracking: free the program card for the next use
                program[slot] = null;
            }
        }
    }

    private List<MoveInfo> createProgrammeForEveryPossibleStartingDirection(Goal goal, Programme[] program) {
        List<MoveInfo> programList = new ArrayList<>();
        if (robot.isMayChooseDirection()) {
            for (Direction dir : Direction.values()) {
                programList.add(createComputerMoveInfo(goal, program, dir));
            }
        } else {
            programList.add(createComputerMoveInfo(goal, program, robot.getDirection()));
        }
        return programList;
    }

    private MoveInfo createComputerMoveInfo(Goal goal, Programme[] program, Direction dir) {
        MoveInfo mi = new MoveInfo(gameLogic, goal, nextCP, robot);
        mi.setStartingDirection(dir);
        mi.setProgram(program.clone());
        return mi;
    }

    private MoveInfo evaluateProgram(MoveInfo mi, MoveInfo chosenMove, Course gameCourse) {
        if (chosenMove == null) {
            chosenMove = mi;
        } else {
            boolean newMove = true;
            if (chosenMove.getStartingDirection() == mi.getStartingDirection()
                    && chosenMove.getProgram()[0].type() == mi.getProgram()[0].type()
                    && chosenMove.getProgram()[1].type() == mi.getProgram()[1].type()
                    && chosenMove.getProgram()[2].type() == mi.getProgram()[2].type()
                    && chosenMove.getProgram()[3].type() == mi.getProgram()[3].type()
                    && chosenMove.getProgram()[4].type() == mi.getProgram()[4].type()) {
                // all steps are identical but for the priority
                if (chosenMove.getProgram()[0].priority() > mi.getProgram()[0].priority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[1].priority() > mi.getProgram()[1].priority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[2].priority() > mi.getProgram()[2].priority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[3].priority() > mi.getProgram()[3].priority()) {
                    newMove = false;
                } else if (chosenMove.getProgram()[4].priority() > mi.getProgram()[4].priority()) {
                    newMove = false;
                }
            } else if (goal == Goal.KILL_ALL) {
                newMove = evaluateForKillAllGoal(mi, chosenMove, gameCourse);
            } else {
                newMove = moveEvaluator.evaluate(mi, gameCourse) > moveEvaluator.evaluate(chosenMove, gameCourse);
            }

            if (newMove) {
                chosenMove = mi;
            }
        }
        return chosenMove;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private boolean evaluateForKillAllGoal(MoveInfo mi, MoveInfo chosenMove, @SuppressWarnings("unused") Course gameCourse) {
        boolean newMove = true;
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
        } else if (mi.isCopiesArchive() && !chosenMove.isCopiesArchive()) {
            // "new move reaches archive point";
        } else if (chosenMove.getDamage() < mi.getDamage()) {
            newMove = false;
            // "old move has less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
        } else if (chosenMove.getDamage() > mi.getDamage()) {
            // "new move has less damage (" + chosenMove.getDamage() + " <-> " + mi.getDamage() + ")";
        } else if (chosenMove.isStandsOnOpenFloor() && !mi.isStandsOnOpenFloor()) {
            newMove = false;
            // "old move will end on open floor";
        } else if (mi.isStandsOnOpenFloor() && !chosenMove.isStandsOnOpenFloor()) {
            // "new move will end on open floor";
        } else if (mi.isKeepsStaying() && !chosenMove.isKeepsStaying()) {
            newMove = false;
            // "old move won't result in doing nothing";
        } else if (chosenMove.isKeepsStaying() && !mi.isKeepsStaying()) {
            // "new move won't result in doing nothing";
        } // else  "new move makes no difference";
        return newMove;
    }

    private void initMoveInfo(Course course) {
        nextCP = null;
        for (Checkpoint cp : course.getCheckpoints()) {
            if (cp.getNumber() == robot.getNextCheckpoint()) {
                nextCP = cp;
                break;
            }
        }
    }

}
