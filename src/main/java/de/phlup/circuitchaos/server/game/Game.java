package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.ComputerType;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.ProgramType;
import de.phlup.circuitchaos.common.enums.RobotType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.Checkpoint;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseElement;
import de.phlup.circuitchaos.common.model.CourseElementStub;
import de.phlup.circuitchaos.common.model.CourseObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Module;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.NetworkResponse;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Programme;
import de.phlup.circuitchaos.common.model.Registration;
import de.phlup.circuitchaos.common.model.RevealProgrammeListItem;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.common.model.Robot;
import de.phlup.circuitchaos.common.settings.TimeSettings;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.player.ComputerPlayer;
import de.phlup.circuitchaos.server.player.NetworkPlayer;
import de.phlup.circuitchaos.server.player.Player;
import de.phlup.circuitchaos.server.service.PollOrPushSwitchService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static de.phlup.circuitchaos.common.enums.Direction.EAST;
import static de.phlup.circuitchaos.common.enums.Direction.NORTH;
import static de.phlup.circuitchaos.common.enums.Direction.SOUTH;
import static de.phlup.circuitchaos.common.enums.Direction.WEST;
import static de.phlup.circuitchaos.common.enums.ModuleType.DOUBLE_BARREL_LASER;
import static de.phlup.circuitchaos.common.enums.ModuleType.EXCHANGE_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.HIGH_POWER_LASER;
import static de.phlup.circuitchaos.common.enums.ModuleType.MAIN_LASER;
import static de.phlup.circuitchaos.common.enums.ModuleType.PRESSURE_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.REAR_LASER;
import static de.phlup.circuitchaos.common.enums.ModuleType.SPIN_LEFT_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.SPIN_RIGHT_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.TRACTOR_BEAM;
import static de.phlup.circuitchaos.server.GlobalServerAttributes.RANDOM;

@Slf4j
@Validated
@Data
@RequiredArgsConstructor
public class Game {

    public static final String AND_THE_WINNER_IS   = "And The Winner Is: ";
    public static final String THIS_GAME_IS_A_TIE  = "This game is a tie.";
    public static final int    MINE_INITIAL_DAMAGE = 4;

    private final PollOrPushSwitchService pollOrPushSwitchService;
    private final TimeSettings            timeSettings;
    private final Registration            initiator;

    private final String gameId = UUID.randomUUID().toString();

    private ZonedDateTime lastAccessed = ZonedDateTime.now();

    private final List<Player>       players        = new ArrayList<>();
    private final List<Registration> watchers       = new ArrayList<>();
    private final List<Registration> networkPlayers = new ArrayList<>();

    private List<CourseElementStub> elementStubs = null;

    private boolean gameAborted = false;

    private Thread loopThread;

    private final Course               course         = new Course();
    @Getter
    private final ServerCourseArranger courseArranger = new ServerCourseArranger();
    @Setter
    private       boolean              notStartedYet  = true;

    private final Map<String, Map<String, NetworkResponse>> awaitingResponses = new ConcurrentHashMap<>();

    private int animationSteps;

    public void notifyCourseMayHaveChanged(Step step, Integer phase, String detail, String movingRobotName) {
        String reason = (phase == null ? "" : (phase + 1) + ": ") + step.getName() + (StringUtils.hasText(detail) ? " - " + detail : "");
        log.debug("Course may have changed: {}: {}", step.name(), reason);
        List<CourseElementStub> stubList = CourseHandler.createCourseElementStubList(course);
        boolean wait = (elementStubs != null && !elementStubs.equals(stubList))
                || (step == Step.OPEN_TRAPDOORS && course.getProperties().isTrapdoor())
                || (step == Step.ROBOT_MOUNTED_LASER_FIRE && phase != null
                && course.getRobots().stream().filter(CourseElement::isOnCourse).anyMatch(
                r -> (r.hasModule(MAIN_LASER) && r.getModule(MAIN_LASER).getActiveInPhase()[phase])
                        || (r.hasModule(DOUBLE_BARREL_LASER) && r.getModule(DOUBLE_BARREL_LASER).getActiveInPhase()[phase])
                        || (r.hasModule(HIGH_POWER_LASER) && r.getModule(HIGH_POWER_LASER).getActiveInPhase()[phase])
                        || (r.hasModule(REAR_LASER) && r.getModule(REAR_LASER).getActiveInPhase()[phase])))
                || (step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && phase != null
                && course.getRobots().stream().filter(CourseElement::isOnCourse).anyMatch(
                r -> r.hasModule(PRESSURE_BEAM) && r.getModule(PRESSURE_BEAM).getActiveInPhase()[phase]))
                || (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && phase != null
                && course.getRobots().stream().filter(CourseElement::isOnCourse).anyMatch(
                r -> r.hasModule(TRACTOR_BEAM) && r.getModule(TRACTOR_BEAM).getActiveInPhase()[phase]))
                || (step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && phase != null
                && course.getRobots().stream().filter(CourseElement::isOnCourse).anyMatch(
                r -> r.hasModule(SPIN_LEFT_BEAM) && r.getModule(SPIN_LEFT_BEAM).getActiveInPhase()[phase]))
                || (step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && phase != null
                && course.getRobots().stream().filter(CourseElement::isOnCourse).anyMatch(
                r -> r.hasModule(SPIN_RIGHT_BEAM) && r.getModule(SPIN_RIGHT_BEAM).getActiveInPhase()[phase]))
                || (step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && phase != null
                && course.getRobots().stream().filter(CourseElement::isOnCourse).anyMatch(
                r -> r.hasModule(EXCHANGE_BEAM) && r.getModule(EXCHANGE_BEAM).getActiveInPhase()[phase]))
                || (step == Step.COURSE_MOUNTED_LASER_FIRE && course.getProperties().isLasers())
                || (step == Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE && course.getProperties().isPressureBeams())
                || (step == Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE && course.getProperties().isTractorBeams())
                || (step == Step.CONVEYOR_BELTS_MOVE && course.getProperties().isConveyorBelts())
                || (step == Step.EXPRESS_CONVEYOR_BELTS_MOVE && course.getProperties().isExpressConveyorBelts())
                || (step == Step.GEARS_ROTATE && course.getProperties().isGears())
                || (step == Step.PUSHERS_PUSH && course.getProperties().isPushers());
        if (elementStubs == null || wait) {
            if (step == Step.SETUP || !wait) {
                for (Registration reg : watchers) {
                    // Please note: "reason" will become part of the window title
                    pollOrPushSwitchService.notifyOfCourseChange(reg, course, reason, step, phase, null, animationSteps, movingRobotName);
                }
            } else {
                randomlySetFloorOrientationForDisplay();
                for (int subPhase = 0; subPhase < animationSteps; subPhase++) {
                    for (Registration reg : watchers) {
                        pollOrPushSwitchService.notifyOfCourseChange(reg, course, reason, step, phase, subPhase, animationSteps, movingRobotName);
                    }
                    Sleep.sleepForMillis(timeSettings.getTimeBetweenSteps() / animationSteps);
                }
                resetPreviousPositionsAndDirections();
            }
        }
        // 2nd Phase for falling down robots/objects
        if (!course.getRobotsFallingIntoAbyss().isEmpty() || !course.getObjectsFallingIntoAbyss().isEmpty()) {
            randomlySetFloorOrientationForDisplay();
            for (int subPhase = animationSteps; subPhase < 2 * animationSteps; subPhase++) {
                for (Registration reg : watchers) {
                    pollOrPushSwitchService.notifyOfCourseChange(reg, course, reason, step, phase, subPhase, animationSteps, movingRobotName);
                }
                Sleep.sleepForMillis(timeSettings.getTimeBetweenSteps() / animationSteps);
            }
            course.getRobotsFallingIntoAbyss().clear();
            course.getObjectsFallingIntoAbyss().clear();
        }
        elementStubs = stubList;
    }

    private void randomlySetFloorOrientationForDisplay() {
        for (Floor floor : course.getFloor()) {
            if (floor.isWater() && !floor.getFloortype().isConveyorBelt()) {
                floor.setFacingDirection(Direction.values()[RANDOM.nextInt(4)]);
            }
        }
    }

    private void resetPreviousPositionsAndDirections() {
        for (CourseElement robot : course.getRobots()) {
            robot.setPrevPosition(robot.getPosition());
            robot.setPrevDirection(robot.getDirection());
        }
        for (CourseElement rrObject : course.getObjects()) {
            rrObject.setPrevPosition(rrObject.getPosition());
            rrObject.setPrevDirection(rrObject.getDirection());
        }
    }

    private void end(Player winner) {
        if (winner != null) {
            showMessage(AND_THE_WINNER_IS + winner.getRobot().getName());
        } else if (!gameAborted) {
            showMessage(THIS_GAME_IS_A_TIE);
        }
    }

    private void showMessage(String message) {
        for (Registration reg : watchers) {
            pollOrPushSwitchService.showMessage(reg, message);
        }
    }

    public void play(GameOptions gameOptions) {
        int numberOfTargetCheckpoint = course.getCheckpoints().size();
        if (numberOfTargetCheckpoint < 2) {
            throw new CircuitChaosException("Not enough checkpoints set.");
        }
        Position startPosition = course.getCheckpoints().stream()
                                       .min(Comparator.comparingInt(Checkpoint::getNumber))
                                       .orElseThrow().getPosition();
        notStartedYet = false;
        GlobalServerAttributes.setGameRunning(this.getGameId(), this);

        List<RobotType> availableRobotTypes = RobotType.mixedValues();
        for (Registration reg : List.copyOf(networkPlayers)) {
            if (availableRobotTypes.isEmpty()) {
                pollOrPushSwitchService.showMessage(reg, "Game is full. :-(");
                for (Registration registration : List.copyOf(networkPlayers)) {
                    if (reg.getId().equals(registration.getId())) {
                        networkPlayers.remove(reg);
                    }
                }
            } else {
                RobotType robotType = availableRobotTypes.getFirst();
                availableRobotTypes.remove(robotType);
                Robot         robot         = createRobot(robotType, startPosition);
                NetworkPlayer networkPlayer = new NetworkPlayer(robot, gameId, reg);
                players.addLast(networkPlayer);
                CourseHandler.add(course, robot);
            }
        }
        for (int i = 0; !availableRobotTypes.isEmpty() && i < gameOptions.getMaxNumberOfComputerPlayers(); i++) {
            RobotType robotType = availableRobotTypes.getFirst();
            availableRobotTypes.remove(robotType);
            Robot robot = createRobot(robotType, startPosition);
            ComputerType computerType = i < gameOptions.getComputerType().size()
                    ? ComputerType.getByName(gameOptions.getComputerType().get(i))
                    : ComputerType.NORMAL;
            players.addLast(new ComputerPlayer(robot, gameId, computerType));
            CourseHandler.add(course, robot);
        }
        int playersSize = players.size();
        if (playersSize > 0) {
            if (playersSize == 1) {
                players.getFirst().getRobot().setVirtual(false);
            }
            provideRobotsWithDefaultModules(gameOptions.getDefaultModules());
            loopThread = new LoopThread(getGameId());
            loopThread.start();
        } else {
            gameAborted = true;
            end(null);
        }
    }

    private void provideRobotsWithDefaultModules(List<String> defaultModules) {
        for (String defaultModule : defaultModules) {
            try {
                ModuleType moduleType = ModuleType.valueOf(defaultModule);
                for (Player player : players) {
                    player.getRobot().getModules().add(new Module(moduleType));
                }
            } catch (Exception e) {
                log.warn("Could not find module {}.", defaultModule);
            }
        }
    }

    private Robot createRobot(RobotType robotType, Position startPosition) {
        Robot robot = new Robot();
        robot.setName(robotType.getRobotName());
        robot.setId(UUID.randomUUID().toString());
        robot.setPosition(startPosition);
        robot.setPrevPosition(startPosition);
        robot.setArchivePosition(startPosition);
        robot.setLevel(CourseHandler.getFloor(course, startPosition).getLevel());
        return robot;
    }

    @AllArgsConstructor
    private class LoopThread extends Thread {

        private final String id;

        public void run() {
            try {
                Player winner = null;
                while (winner == null && !players.isEmpty() && !isGameAborted()) {
                    notifyCourseMayHaveChanged(Step.SETUP, null, "start of new turn", null);

                    deal();
                    interactWithAllPlayers(players);

                    if (!gameAborted) {
                        for (Player player : players) {
                            Robot       r = player.getRobot();
                            Programme[] p = r.getProgram();
                            log.debug("Received program for {}: {} - {} - {} - {} - {}", r.getName(), p[0].getType().getCommand(),
                                      p[1].getType().getCommand(), p[2].getType().getCommand(),
                                      p[3].getType().getCommand(), p[4].getType().getCommand());
                        }
                    }

                    winner = loopThroughPhases();
                    winner = endOfTurnCourseEffects(winner);
                }
                notifyCourseMayHaveChanged(Step.SETUP, null, "end of turn", null);
                if (!isGameAborted()) {
                    end(winner);
                }
            } finally {
                GlobalServerAttributes.RUNNING_GAMES.remove(id);
            }
        }

    }

    private Player loopThroughPhases() {
        Player winner = null;
        for (int phase = 0; phase < 5 && winner == null && !players.isEmpty() && !gameAborted; phase++) {
            notifyCourseMayHaveChanged(Step.START_OF_TURN_COURSE_EFFECTS, phase, "begin of phase %s".formatted(phase + 1), null);

            List<Robot> objectsWithPriority = stepRevealProgrammes(phase);
            stepRobotsMove(phase, objectsWithPriority);
            stepCourseElementsMove(phase);
            stepResolveBeamsAndLaserFire(phase);
            winner = setTouchCheckpoints(phase);
            discardVirtualStateWhenPossible();
        }
        return winner;
    }

    private void stepRobotsMove(int phase, List<Robot> robots) {
        openTrapdoors(phase, robots);
        deactivateRammingArmor(phase, true);
        robotsMove(phase, robots);
        deactivateRammingArmor(phase, false);
    }

    private void robotsMove(int phase, List<Robot> robots) {
        for (Robot robot : robots) {
            if (gameAborted) {
                break;
            }
            Programme program = robot.getProgram()[phase];
            Floor     f       = CourseHandler.getFloor(course, robot.getPosition());
            reactivateRammingArmor(phase, robot);
            Player player = getPlayerOf(robot);
            if (player != null) {
                releaseDevices(phase, player);
                while (!gameAborted && program != null) {
                    int movement = determineMovementAmount(phase, robot, program);
                    if (player.uses(ModuleType.HOVERCRAFT, phase)) {
                        robot.setFlying(movement != 0);
                    }

                    Teleporter teleportResult = Teleporter.teleport(robot, movement, this, phase);
                    if (teleportResult.isHasBeenTeleported()) {
                        break;
                    } else {
                        movement = teleportResult.getOldMovement();
                    }

                    movement = applyWaterAndOilSlick(robot, f, movement);
                    singleRobotMove(phase, robot, program, movement);
                    if (robot.isFlying()) {
                        land(getPlayerOf(robot), Step.ROBOTS_AND_OBJECTS_MOVE, phase);
                    }
                    program = null;
                }
            }
        }
    }

    private void singleRobotMove(int phase, Robot robot, Programme pc, int movement) {
        if (movement > 0) {
            moveInit(robot, robot.getDirection(), movement, movement, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true, true, robot.getName());
        } else if (movement < 0) {
            moveInit(robot, SOUTH.add(robot.getDirection()), movement, -movement, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true, true, robot.getName());
        }
        if (pc.getType() == ProgramType.ROTATE_RIGHT) {
            rotate(robot, EAST, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
        } else if (pc.getType() == ProgramType.ROTATE_LEFT) {
            rotate(robot, WEST, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
        } else if (pc.getType() == ProgramType.U_TURN) {
            rotate(robot, SOUTH, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
        }
    }

    private Player getPlayerOf(CourseElement ce) {
        if (ce instanceof Robot) {
            for (Player player : players) {
                if (player.getRobot().equals(ce)) {
                    return player;
                }
            }
        }
        return null;
    }

    private void discardVirtualStateWhenPossible() {
        for (Player player : players) {
            if (!gameAborted) {
                if (player.getRobot().isOnCourse() && player.getRobot().isVirtual()) {
                    player.getRobot().setVirtual(false);
                    for (Robot ce : CourseHandler.getRobots(course, player.getRobot().getPosition())) {
                        if (ce != player.getRobot()) {
                            player.getRobot().setVirtual(true);
                            break;
                        }
                    }
                    notifyCourseMayHaveChanged(Step.END_OF_TURN_COURSE_EFFECTS, null, "discard virtual state", null);
                }
            }
        }
    }

    private Player endOfTurnCourseEffects(Player winner) {
        for (Player player : List.copyOf(players)) {
            if (winner == null && !gameAborted) {
                if (player.getRobot().isFlying()) {
                    land(player, Step.END_OF_TURN_COURSE_EFFECTS, 4);
                }

                resetPoweredDownForDestroyedRobot(player);
                player.getRobot().setMayChooseDirection(false);
                handleDestroyedRobots(player);

                if (player.hasWon()) {
                    winner = player;
                } else if (player.hasLost()) {
                    players.remove(player);
                    if (player instanceof NetworkPlayer networkPlayer) {
                        for (Registration reg : List.copyOf(networkPlayers)) {
                            if (reg.getId().equals(networkPlayer.getRegistration().getId())) {
                                networkPlayers.remove(reg);
                            }
                        }
                    }
                }
            }
        }
        return winner;
    }

    private void handleDestroyedRobots(Player player) {
        Robot robot = player.getRobot();
        if (robot.getDamage() >= 10 && !gameAborted) {
            robot.setDamage((player.getRobot().hasModule(ModuleType.SUPERIOR_ARCHIVE_COPY) ? 0 : 2));
            if (!player.hasLost()) {
                robot.setPosition(robot.getArchivePosition());
                robot.setPrevPosition(robot.getArchivePosition());
                robot.setLevel(robot.getArchiveLevel());
                robot.setMayChooseDirection(true);
                robot.setVirtual(true);
                notifyCourseMayHaveChanged(Step.END_OF_TURN_COURSE_EFFECTS, null, "%s was destroyed".formatted(robot.getName()), null);
                CourseHandler.add(course, robot);
                for (int phase = 0; phase < 5; phase++) {
                    if (robot.getBlocked()[phase]) {
                        robot.getProgram()[phase] = null;
                    }
                }
                robot.setBlocked(new boolean[]{false, false, false, false, false});
            }
        } else {
            if (robot.isPowerDownAnnounced()) {
                robot.setPowerDownAnnounced(false);
                robot.setPoweredDown(true);
                robot.setDamage(0);
                for (int phase = 0; phase < 5; phase++) {
                    if (robot.getBlocked()[phase]) {
                        robot.getProgram()[phase] = null;
                    }
                }
                robot.setBlocked(new boolean[]{false, false, false, false, false});
            }
        }
    }

    private void resetPoweredDownForDestroyedRobot(Player player) {
        Robot robot = player.getRobot();
        if (robot.isPoweredDown() || robot.getDamage() >= 10) {
            robot.setPoweredDown(false);
        }
    }

    private Player setTouchCheckpoints(int phase) {
        Player winner = null;
        for (CourseElement o : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
            if (!gameAborted) {
                if (o.isOnCourse()) {
                    Player player = getPlayerOf(o);
                    if (player != null) {
                        Checkpoint cp = CourseHandler.getCheckpoint(course, player.getRobot().getPosition());
                        Floor      f  = CourseHandler.getFloor(course, player.getRobot().getPosition());
                        Floor      f2 = f;
                        if (player.uses(ModuleType.MECHANICAL_ARM, phase)) {
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = CourseHandler.getFloor(course, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() - 1));
                                cp = CourseHandler.getCheckpoint(course, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() - 1));
                            }
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = CourseHandler.getFloor(course, new Position(player.getRobot().getPosition().x() + 1, player.getRobot().getPosition().y()));
                                cp = CourseHandler.getCheckpoint(course, new Position(player.getRobot().getPosition().x() + 1, player.getRobot().getPosition().y()));
                            }
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = CourseHandler.getFloor(course, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() + 1));
                                cp = CourseHandler.getCheckpoint(course, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() + 1));
                            }
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = CourseHandler.getFloor(course, new Position(player.getRobot().getPosition().x() - 2, player.getRobot().getPosition().y()));
                                cp = CourseHandler.getCheckpoint(course, new Position(player.getRobot().getPosition().x() - 1, player.getRobot().getPosition().y()));
                            }
                        }
                        if (cp != null || f.getFloortype() == Floortype.PIT_STOP) {
                            player.getRobot().setArchivePosition(f.getPosition());
                            player.getRobot().setArchiveLevel(f.getLevel());
                            if (cp != null && cp.getNumber() == player.getRobot().getNextCheckpoint()) {
                                player.getRobot().setNextCheckpoint(player.getRobot().getNextCheckpoint() + 1);
                            }
                        }
                        if (phase == 4 && f2.getFloortype() == Floortype.PIT_STOP) {
                            player.arriveAtPitStop();
                        }
                        if (player.hasWon()) {
                            winner = player;
                        }
                    } else if (o instanceof CourseObject co) {
                        co.setActive(true);
                        switch (co.getType()) {
                            case MINE:
                                if (hasNonVirtualRobot(co.getPosition().x(), co.getPosition().y())) {
                                    goOff(Step.TOUCH_CHECKPOINTS, phase, co);
                                }
                                break;
                            case PROXIMITY_MINE:
                                if (hasNonVirtualRobot(co.getPosition().x(), co.getPosition().y())
                                        || hasNonVirtualRobot(co.getPosition().x() + 1, co.getPosition().y())
                                        || hasNonVirtualRobot(co.getPosition().x(), co.getPosition().y() + 1)
                                        || hasNonVirtualRobot(co.getPosition().x() - 1, co.getPosition().y())
                                        || hasNonVirtualRobot(co.getPosition().x(), co.getPosition().y() - 1)) {
                                    goOff(Step.TOUCH_CHECKPOINTS, phase, co);
                                }
                                break;
                        }
                    }
                }
            }
        }
        return winner;
    }

    private boolean hasNonVirtualRobot(int posx, int posy) {
        for (Robot ce : course.getRobots()) {
            if (ce.getPosition().x() == posx && ce.getPosition().y() == posy && !ce.isVirtual()) {
                return true;
            }
        }
        return false;
    }

    private void stepResolveBeamsAndLaserFire(int phase) {
        List.of(Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE,
                Step.ROBOT_MOUNTED_LASER_FIRE).forEach(step -> {
            List<Shooter> shooters = rememberPositionsOfRobotMountedLasers(phase, step);
            robotMountedBeamsAndLasersShoot(phase, shooters, step);
        });
        List.of(Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE,
                Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE,
                Step.COURSE_MOUNTED_LASER_FIRE)
            .forEach(step -> courseMountedLasersShoot(phase, step));
    }

    private void courseMountedLasersShoot(int phase, Step step) {
        for (Floor f : new ArrayList<>(course.getFloor())) {
            for (Direction dir : Direction.values()) {
                Direction dir2 = dir.reverse();
                int amount = switch (step) {
                    case COURSE_MOUNTED_PRESSURE_BEAMS_FIRE -> f.getPressureBeam()[dir.ordinal()] ? 1 : 0;
                    case COURSE_MOUNTED_TRACTOR_BEAMS_FIRE -> f.getTractorBeam()[dir.ordinal()] ? 1 : 0;
                    default -> f.getLasers()[dir.ordinal()];
                };
                for (int cnt = 0; cnt < amount; cnt++) {
                    Robot target = getLaserTarget(f.getPosition(), dir, f.getLevel(), false, true, true, step);
                    if (target != null) {
                        if (step == Step.COURSE_MOUNTED_LASER_FIRE) {
                            Player playerOfRobot = getPlayerOf(target);
                            if (playerOfRobot != null) {
                                playerOfRobot.takeDamage(phase, null, dir2, true);
                            }
                        } else if (step == Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE) {
                            moveInit(target, dir, 0, 1, Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE, phase, true, false, null);
                        } else if (step == Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE) {
                            moveInit(target, dir.reverse(), 0, 1, Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE, phase, false, false, null);
                        }
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(step, phase, null, null);
        }
        CourseHandler.clearBeams(course);
    }

    private void robotMountedBeamsAndLasersShoot(int phase, List<Shooter> shooters, Step step) {
        for (Shooter shooter : shooters) {
            if (gameAborted) {
                break;
            }
            Player    player       = shooter.getPlayer();
            Robot     robot        = player.getRobot();
            Direction dir          = robot.getDirection();
            Robot     targetRobot  = shooter.getTarget();
            Robot     targetRobot2 = shooter.getTarget2();
            Module    weapon       = shooter.getWeapon();
            if (!robot.isVirtual() && weapon != null) {
                ModuleType weaponType = weapon.getType();
                if (targetRobot != null && targetRobot.isOnCourse()) {
                    if (step == Step.ROBOT_MOUNTED_LASER_FIRE
                            && (weaponType == MAIN_LASER
                            || weaponType == ModuleType.DOUBLE_BARREL_LASER
                            || weaponType == ModuleType.HIGH_POWER_LASER
                            || weaponType == ModuleType.REAR_LASER)) {    // main laser
                        Player playerOfTargetRobot = getPlayerOf(targetRobot);
                        if (playerOfTargetRobot != null) {
                            playerOfTargetRobot.takeDamage(phase, player, dir, true);
                            if (weaponType == ModuleType.DOUBLE_BARREL_LASER) {
                                playerOfTargetRobot.takeDamage(phase, player, dir, true);
                            }
                        }
                    } else if (step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && weaponType == ModuleType.PRESSURE_BEAM) {
                        moveInit(targetRobot, dir, 0, 1, step, phase, true, false, null);
                    } else if (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && weaponType == ModuleType.TRACTOR_BEAM) {
                        moveInit(targetRobot, dir.reverse(), 0, 1, step, phase, false, false, null);
                    } else if (step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && weaponType == ModuleType.EXCHANGE_BEAM) {
                        // Dreiecks-Tausch
                        Position position = targetRobot.getPosition();
                        int      level    = targetRobot.getLevel();
                        targetRobot.setPosition(robot.getPosition());
                        targetRobot.setLevel(robot.getLevel());
                        robot.setPosition(position);
                        robot.setLevel(level);
                    } else if (step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && weaponType == ModuleType.SPIN_LEFT_BEAM) {
                        targetRobot.setDirection(targetRobot.getDirection().add(WEST));
                    } else if (step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && weaponType == ModuleType.SPIN_RIGHT_BEAM) {
                        targetRobot.setDirection(targetRobot.getDirection().add(EAST));
                    }
                }
                if (targetRobot2 != null && step == Step.ROBOT_MOUNTED_LASER_FIRE && targetRobot2.isOnCourse()) { // kann nur bei High Power Laser der Fall sein
                    Player playerOfTargetRobot = getPlayerOf(targetRobot2);
                    if (playerOfTargetRobot != null) {
                        playerOfTargetRobot.takeDamage(phase, player, dir, true);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(step, phase, null, null);
        }
        CourseHandler.clearBeams(course);
    }

    private List<Shooter> rememberPositionsOfRobotMountedLasers(int phase, Step step) {
        List<Shooter> shooters = new ArrayList<>();
        for (Player player : players) {
            if (gameAborted) {
                break;
            }
            if (player.getRobot().isOnCourse()) {
                shooters.add(new Shooter(phase, player, this, step));
            }
        }
        return shooters;
    }

    private void stepCourseElementsMove(int phase) {
        expressConveyorBeltsMove(phase);
        conveyorBeltsMove(phase);
        pushersPush(phase);
        gearsRotate(phase);
    }

    private void gearsRotate(int phase) {
        for (Floor f : course.getFloor()) {
            if (gameAborted) {
                break;
            }
            if (f.getFloortype() == Floortype.GEARS_CCW || f.getFloortype() == Floortype.GEARS_CW) {
                for (CourseElement ce : CourseHandler.getObjectsAndRobots(course)) {
                    if (ce.isOnCourse()
                            && ce.getPosition().equals(f.getPosition())
                            && !ce.isFlying()
                            && !(ce instanceof Robot && getPlayerOf(ce).uses(ModuleType.GYROSCOPIC_STABILIZER, phase))) {
                        rotate(ce, f.getFloortype() == Floortype.GEARS_CCW ? WEST : EAST, Step.GEARS_ROTATE, phase, false);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.GEARS_ROTATE, phase, null, null);
        }
    }

    private void pushersPush(int phase) {
        for (Floor f : new ArrayList<>(course.getFloor())) {
            if (gameAborted) {
                break;
            }
            if (f.isHasPusher() && f.getActiveInPhase()[phase]) {
                for (CourseElement ce : CourseHandler.getObjectsAndRobots(course)) {
                    if (ce.isOnCourse() && ce.getPosition().equals(f.getPosition())) {
                        if (willBePushed(ce)) {
                            moveInit(ce, f.getPusherDirection(), 1, 1, Step.PUSHERS_PUSH, phase, true, false, null);
                        }
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.PUSHERS_PUSH, phase, null, null);
        }
    }

    private boolean willBePushed(CourseElement ce) {
        if (ce instanceof CourseObject co) {
            return co.getType() != ObjectType.RANDOMIZER
                    && co.getType() != ObjectType.TELEPORTER
                    && co.getType() != ObjectType.GLUE
                    && co.getType() != ObjectType.OIL
                    && co.getType() != ObjectType.PORTAL_RED
                    && co.getType() != ObjectType.PORTAL_BLUE
                    && co.getType() != ObjectType.PORTAL_YELLOW
                    && co.getType() != ObjectType.PORTAL_PURPLE
                    && co.getType() != ObjectType.PORTAL_GREEN;
        } else {
            return true;
        }
    }

    private void conveyorBeltsMove(int phase) {
        boolean needToGoAgain = false;
        int     iteration     = 0;
        for (CourseElement obj : CourseHandler.getObjectsAndRobots(course)) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse()) {
                Floor f = CourseHandler.getFloor(course, obj.getPosition());
                obj.setConflictingMark((f.getFloortype().isConveyorBelt() && (!f.isWater()) && !obj.isFlying()) ? 0 : -1);
                if (f.getFloortype().isConveyorBelt() && !f.isWater()) {
                    needToGoAgain = true;
                }
            }
        }
        while (needToGoAgain && iteration < 20 && !gameAborted) {
            needToGoAgain = false;
            iteration++;
            for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
                if (obj.isOnCourse()) {
                    if (obj.getConflictingMark() == 0) {
                        Floor     f                = CourseHandler.getFloor(course, obj.getPosition());
                        Floor     targetFloor      = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                        Direction counterDirection = f.getFacingDirection().reverse();
                        if ((!(((f.wall(f.getFacingDirection()) == WallType.NONE
                                || f.wall(f.getFacingDirection()) == WallType.ONE_WAY_GREEN
                                || f.wall(f.getFacingDirection()) == WallType.RAMP_DOWN)
                                && (targetFloor.wall(counterDirection) == WallType.NONE
                                || targetFloor.wall(counterDirection) == WallType.LEDGE
                                || targetFloor.wall(counterDirection) == WallType.ONE_WAY_GREEN
                                || targetFloor.wall(counterDirection) == WallType.RAMP_UP))
                                || (f.wall(f.getFacingDirection()) == WallType.ONE_WAY_GREEN
                                && targetFloor.wall(counterDirection) == WallType.ONE_WAY_RED)))
                                || getCBConflictingElementMark(obj, f.getFacingDirection()) == -1) {
                            obj.setConflictingMark(-1);
                        } else if (getCBConflictingElementMark(obj, f.getFacingDirection()) == 1) {
                            obj.setConflictingMark(-1);
                            rotateObjectsOnConveyorBelts(Step.CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                            setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                        } else {
                            needToGoAgain = true;
                        }
                    }
                }
            }
        }
        for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse()) {
                if (obj.getConflictingMark() == 0) {
                    Floor f           = CourseHandler.getFloor(course, obj.getPosition());
                    Floor targetFloor = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                    rotateObjectsOnConveyorBelts(Step.CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                    setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.CONVEYOR_BELTS_MOVE, phase, null, null);
        }
    }

    private void expressConveyorBeltsMove(int phase) {
        boolean needToGoAgain = false;
        int     iteration     = 0;
        for (CourseElement obj : CourseHandler.getObjectsAndRobots(course)) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse()) {
                Floor f = CourseHandler.getFloor(course, obj.getPosition());
                obj.setConflictingMark((f.getFloortype().isExpressConveyorBelt() && !obj.isFlying()) ? 0 : -1);
                if (f.getFloortype().isExpressConveyorBelt()) {
                    needToGoAgain = true;
                }
            }
        }
        while (needToGoAgain && iteration < 20 && !gameAborted) {
            needToGoAgain = false;
            iteration++;
            for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
                if (gameAborted) {
                    break;
                }
                if (obj.isOnCourse()) {
                    if (obj.getConflictingMark() == 0) {
                        Floor     f                = CourseHandler.getFloor(course, obj.getPosition());
                        Floor     targetFloor      = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                        Direction counterDirection = f.getFacingDirection().reverse();
                        if ((!(((f.wall(f.getFacingDirection()) == WallType.NONE
                                || f.wall(f.getFacingDirection()) == WallType.ONE_WAY_GREEN
                                || f.wall(f.getFacingDirection()) == WallType.RAMP_DOWN)
                                && (targetFloor.wall(counterDirection) == WallType.NONE
                                || targetFloor.wall(counterDirection) == WallType.LEDGE
                                || targetFloor.wall(counterDirection) == WallType.ONE_WAY_GREEN
                                || targetFloor.wall(counterDirection) == WallType.RAMP_UP))
                                || (f.wall(f.getFacingDirection()) == WallType.ONE_WAY_GREEN
                                && targetFloor.wall(counterDirection) == WallType.ONE_WAY_RED)))
                                || getCBConflictingElementMark(obj, f.getFacingDirection()) == -1) {
                            obj.setConflictingMark(-1);
                        } else if (getCBConflictingElementMark(obj, f.getFacingDirection()) == 1) {
                            obj.setConflictingMark(-1);
                            rotateObjectsOnConveyorBelts(Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                            setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                        } else {
                            needToGoAgain = true;
                        }
                    }
                }
            }
        }
        for (CourseElement obj : new ArrayList<>(CourseHandler.getObjectsAndRobots(course))) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnCourse()) {
                if (obj.getConflictingMark() == 0) {
                    Floor f           = CourseHandler.getFloor(course, obj.getPosition());
                    Floor targetFloor = CourseHandler.getFloor(course, f.getPosition().neighbour(f.getFacingDirection()));
                    rotateObjectsOnConveyorBelts(Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, obj, targetFloor, f);
                    setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, f.getFacingDirection(), false);
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, null, null);
        }
    }

    private void rotateObjectsOnConveyorBelts(Step step, int phase, CourseElement obj, Floor targetFloor, Floor sourceFloor) {
        if (!(obj instanceof Robot && getPlayerOf(obj).uses(ModuleType.GYROSCOPIC_STABILIZER, phase))) {
            if ((targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CCW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(WEST)
                    || (targetFloor.getFacingDirection() == WEST && sourceFloor.getFacingDirection() == NORTH))) {
                rotate(obj, WEST, step, phase, false);
            } else if ((targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(EAST)
                    || (sourceFloor.getFacingDirection() == WEST && targetFloor.getFacingDirection() == NORTH))) {
                rotate(obj, EAST, step, phase, false);
            } else if (targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW) {
                if (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(WEST)
                        || (targetFloor.getFacingDirection() == WEST && sourceFloor.getFacingDirection() == NORTH)) {
                    rotate(obj, WEST, step, phase, false);
                } else if ((targetFloor.getFacingDirection() == NORTH && sourceFloor.getFacingDirection() == WEST)
                        || targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(EAST)) {
                    rotate(obj, EAST, step, phase, false);
                }
            }
        }
    }

    private int applyWaterAndOilSlick(CourseElement ce, Floor f, int movement) {
        if (!ce.isFlying() && (f.isWater()
                || CourseHandler.getObjects(course, f.getPosition()).stream().anyMatch(co -> co.getType() == ObjectType.OIL))) {
            if (movement > 0) {
                movement--;
            }
            if (movement < 0) {
                movement++;
            }
        }
        return movement;
    }

    private int determineMovementAmount(int phase, CourseElement ce, Programme pc) {
        int movement;
        if (ce instanceof Robot) {
            movement = pc.getType().getMovement();
            if (movement != 0 && getPlayerOf(ce).uses(ModuleType.BRAKES, phase)) {
                movement = movement > 0 ? movement - 1 : movement + 1;
            }
        } else {
            movement = pc.getType().getMovement();
        }
        return movement;
    }

    private void releaseDevices(int phase, Player player) {
        Robot robot      = player.getRobot();
        Floor standingOn = CourseHandler.getFloor(course, robot.getPosition());
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.MOBILE_TELEPORTER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
                Module module = player.getRobot().getModule(ModuleType.MOBILE_TELEPORTER);
                if (module.getAmmunition() > 0) {
                    CourseHandler.createCircuitChaosObject(course, ObjectType.TELEPORTER, player);
                    module.setAmmunition(module.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.MOBILE_RANDOMIZER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
                Module module = player.getRobot().getModule(ModuleType.MOBILE_RANDOMIZER);
                if (module.getAmmunition() > 0) {
                    CourseHandler.createCircuitChaosObject(course, ObjectType.RANDOMIZER, player);
                    module.setAmmunition(module.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.GLUE_DISPENSER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
                Module glueDispenser = player.getRobot().getModule(ModuleType.GLUE_DISPENSER);
                if (glueDispenser.getAmmunition() > 0) {
                    CourseHandler.createGlue(course, player.getRobot().getPosition());
                    glueDispenser.setAmmunition(glueDispenser.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.OIL_DISPENSER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
                Module oilDispenser = player.getRobot().getModule(ModuleType.OIL_DISPENSER);
                if (oilDispenser.getAmmunition() > 0) {
                    CourseHandler.createOil(course, player.getRobot().getPosition());
                    oilDispenser.setAmmunition(oilDispenser.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.MINE_LAYER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
                Module mineLayer = player.getRobot().getModule(ModuleType.MINE_LAYER);
                if (mineLayer.getAmmunition() > 0) {
                    CourseHandler.createCircuitChaosObject(course, ObjectType.MINE, player);
                    mineLayer.setAmmunition(mineLayer.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.PROXIMITY_MINE_LAYER, phase) && standingOn.getFloortype() == Floortype.OPEN_FLOOR) {
                Module mineLayer = player.getRobot().getModule(ModuleType.PROXIMITY_MINE_LAYER);
                if (mineLayer.getAmmunition() > 0) {
                    CourseHandler.createCircuitChaosObject(course, ObjectType.PROXIMITY_MINE, player);
                    mineLayer.setAmmunition(mineLayer.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.BRIDGE_PROJECTOR, phase)) {
                Module bridgeProjector = player.getRobot().getModule(ModuleType.BRIDGE_PROJECTOR);
                if (bridgeProjector.getAmmunition() > 0) {
                    Floor f2 = switch (robot.getDirection()) {
                        case NORTH ->
                                CourseHandler.getFloor(course, new Position(robot.getPosition().x(), robot.getPosition().y() - 1));
                        case EAST ->
                                CourseHandler.getFloor(course, new Position(robot.getPosition().x() + 1, robot.getPosition().y()));
                        case SOUTH ->
                                CourseHandler.getFloor(course, new Position(robot.getPosition().x(), robot.getPosition().y() + 1));
                        case WEST ->
                                CourseHandler.getFloor(course, new Position(robot.getPosition().x() - 1, robot.getPosition().y()));
                    };
                    if (f2.getFloortype() == Floortype.ABYSS || f2.getFloortype() == Floortype.TRAPDOOR) {
                        bridgeProjector.setAmmunition(bridgeProjector.getAmmunition() - 1);
                        f2.setFloortype(Floortype.OPEN_FLOOR);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.RELEASE_DEVICES, phase, null, null);
        }
    }

    private void reactivateRammingArmor(int phase, CourseElement ce) {
        if (ce instanceof Robot robot) {
            Player player = getPlayerOf(ce);
            if (robot.isReactivateRammingArmor()) {
                if (player.getRobot().getModule(ModuleType.RAMMING_ARMOR) != null) {
                    robot.setReactivateRammingArmor(false);
                    player.getRobot().getModule(ModuleType.RAMMING_ARMOR).getActiveInPhase()[phase] = true;
                }
            }
        }
    }

    private void deactivateRammingArmor(int phase, boolean reactivate) {
        for (Player player : players) {
            Robot robot = player.getRobot();
            if (player.uses(ModuleType.RAMMING_ARMOR, phase)) {
                robot.setReactivateRammingArmor(reactivate);
                robot.getModule(ModuleType.RAMMING_ARMOR).getActiveInPhase()[phase] = false;
            }
        }
    }

    private void openTrapdoors(int phase, List<Robot> robots) {
        for (Robot ce : robots) {
            if (gameAborted) {
                break;
            }
            Floor f = CourseHandler.getFloor(course, ce.getPosition());
            if (f.getFloortype() == Floortype.TRAPDOOR && f.getActiveInPhase()[phase]) {
                die(ce, Step.OPEN_TRAPDOORS, phase);
            }
        }
        if (!gameAborted) {
            notifyCourseMayHaveChanged(Step.OPEN_TRAPDOORS, phase, null, null);
        }
    }

    private List<Robot> stepRevealProgrammes(int phase) {
        revealProgrammesRandomizers(phase);
        List<Robot> robots = revealProgrammesOrderExecutorByPriority(phase);
        revealProgrammesShow(phase, robots);
        return robots;
    }

    private void revealProgrammesShow(int phase, List<Robot> robots) {
        RevealProgrammeResponse revealProgrammeResponse = new RevealProgrammeResponse();
        revealProgrammeResponse.setPhase(phase);
        for (Robot r : robots) {
            Programme program = r.getProgram()[phase];
            if (program != null && !r.isPoweredDown() && r.isOnCourse()) {
                revealProgrammeResponse.getProgramme().add(new RevealProgrammeListItem(r.getName(), ImageSupplier.getRobotImagePathAndName(r), program));
            }
        }
        for (Registration reg : watchers) {
            pollOrPushSwitchService.revealProgrammes(reg, revealProgrammeResponse);
        }
    }

    private List<Robot> revealProgrammesOrderExecutorByPriority(int phase) {
        return CourseHandler.getRobots(course)
                            .stream()
                            .filter(Objects::nonNull)
                            .filter(ce -> getPriority(ce, phase) >= 0)
                            .sorted((a, b) -> Integer.compare(getPriority(b, phase), getPriority(a, phase)))
                            .toList();
    }

    private void revealProgrammesRandomizers(int phase) {
        for (CourseObject co : course.getObjects()) {
            if (gameAborted) {
                break;
            }
            if (co.getType() == ObjectType.RANDOMIZER) {
                for (Robot r : CourseHandler.getRobots(course, co.getPosition())) {
                    r.getProgram()[phase] = Programme.create(r.hasModule(ModuleType.OVERDRIVE), r.hasModule(ModuleType.REVERSE_DRIVE));
                }
            }
        }
    }

    private void interactWithAllPlayers(List<Player> playersToInteractWith) {
        String                       purposeId        = UUID.randomUUID().toString();
        Map<String, NetworkResponse> requestedAnswers = askNetworkPlayersToProgramTheirRobots(purposeId, playersToInteractWith);
        // +++ computer players perform
        for (Player player : playersToInteractWith) {
            if (player instanceof ComputerPlayer computerPlayer
                    && !(computerPlayer.getRobot().isPoweredDown() || gameAborted || player.hasLost())) {
                computerPlayer.perform();
            }
        }
        if (!requestedAnswers.isEmpty()) {
            waitForAllAnswers(requestedAnswers);
            for (Player player : playersToInteractWith) {
                if (player instanceof NetworkPlayer networkPlayer && !gameAborted && !player.hasLost()) {
                    NetworkResponse networkResponse = requestedAnswers.get(networkPlayer.getId());
                    networkPlayer.handleResponse(networkResponse);
                }
            }
        }
        awaitingResponses.remove(purposeId);
    }

    private Map<String, NetworkResponse> askNetworkPlayersToProgramTheirRobots(String purposeId, List<Player> playersToInteractWith) {
        Map<String, NetworkResponse> requestedAnswers = new ConcurrentHashMap<>();
        awaitingResponses.put(purposeId, requestedAnswers);
        for (Player player : playersToInteractWith) {
            if (player instanceof NetworkPlayer networkPlayer && !gameAborted && !player.hasLost()) {
                NetworkRequest networkRequest = networkPlayer.createRequest(null);
                if (networkRequest != null) {
                    requestedAnswers.put(networkPlayer.getId(), new NetworkResponse());
                    pollOrPushSwitchService.programQuestion(networkPlayer.getRegistration(), purposeId, networkPlayer.getId(), networkRequest);
                }
            }
        }
        return requestedAnswers;
    }

    private void waitForAllAnswers(Map<String, NetworkResponse> requestedAnswers) {
        boolean stillWaitingOrTimeout = true;
        long    timestamp             = System.currentTimeMillis();
        while (stillWaitingOrTimeout) {
            if (timestamp < System.currentTimeMillis() - timeSettings.getQuestionTimeout()) {
                break;
            }
            stillWaitingOrTimeout = false;
            for (NetworkResponse answer : requestedAnswers.values()) {
                stillWaitingOrTimeout = stillWaitingOrTimeout || !answer.isFilled();
            }
            if (stillWaitingOrTimeout) {
                Sleep.sleepForMillis(timeSettings.getSleepTimeBetweenAnswerPolling());
            }
        }
    }

    private void deal() {
        for (Player activePlayer : players) {
            Robot r = activePlayer.getRobot();
            if (!(r.isPoweredDown() || gameAborted)) {
                r.getPotentialProgramme().clear();
                for (int counter = 9 - r.getDamage(); counter > 0; counter--) {
                    r.getPotentialProgramme().add(Programme.create(r.hasModule(ModuleType.OVERDRIVE), r.hasModule(ModuleType.REVERSE_DRIVE)));
                }
                if (r.hasModule(ModuleType.EXTRA_PROGRAM)) {
                    int amount = r.getModule(ModuleType.EXTRA_PROGRAM).getAmmunition();
                    for (int i = 0; i < amount; i++) {
                        r.getPotentialProgramme().add(Programme.create(r.hasModule(ModuleType.OVERDRIVE), r.hasModule(ModuleType.REVERSE_DRIVE)));
                    }
                }
            }
        }
    }

    public void moveInit(CourseElement ce, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot, boolean wait, String movingRobotName) {
        move(ce, direction, strength, amount, step, phase, notBlockedByMovableRobot, wait, movingRobotName);
    }

    private boolean move(CourseElement ce, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot, boolean wait, String movingRobotName) {
        Position oldPosition    = ce.getPosition();
        Floor    floor1         = CourseHandler.getFloor(course, oldPosition);
        Position targetPosition = ce.getPosition().neighbour(direction);
        Floor    floor2         = CourseHandler.getFloor(course, targetPosition);
        if (!ce.isFlying()) {
            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, floor1.getPosition()))) {
                if (co.getType() == ObjectType.GLUE && co.isActive()) {
                    while (co.getGlue() > 0 && amount > 0) {
                        deGlue(co, phase);
                        amount--;
                    }
                }
            }
        }
        Direction direction2 = direction.reverse();
        if (floor1.wall(direction) == WallType.RAMP_UP && floor2.wall(direction2) == WallType.RAMP_DOWN && !ce.isFlying()) {
            amount--;
        }
        if (!ce.getPosition().inRange(course.getRange())) {
            amount = 0;
            die(ce, step, phase);
        }
        wait = wait && (movingRobotName == null || movingRobotName.equals(ce.getName()));
        Player player = getPlayerOf(ce);
        if (amount > 0) {
            if (((floor1.wall(direction) == WallType.NONE
                    || floor1.wall(direction) == WallType.ONE_WAY_GREEN
                    || floor1.wall(direction) == WallType.RAMP_DOWN
                    || floor1.wall(direction) == WallType.RAMP_UP)
                    && (floor2.wall(direction2) == WallType.NONE
                    || floor2.wall(direction2) == WallType.ONE_WAY_GREEN
                    || floor2.wall(direction2) == WallType.RAMP_UP
                    || floor2.wall(direction2) == WallType.RAMP_DOWN))
                    || (floor1.wall(direction) == WallType.ONE_WAY_GREEN
                    && floor2.wall(direction2) == WallType.ONE_WAY_RED)) {
                CourseElement conflictingElement = getConflictingElement(ce, direction);
                if (conflictingElement == null) {
                    ce.setPosition(targetPosition);
                    if (!ce.isFlying()) {
                        ce.setLevel(CourseHandler.getFloor(course, ce.getPosition()).getLevel());
                    }
                } else if (notBlockedByMovableRobot) {
                    if (player != null && player.uses(ModuleType.RAMMING_ARMOR, phase)) {
                        player.getRobot().getModule(ModuleType.RAMMING_ARMOR).getActiveInPhase()[phase] = false;
                        if (conflictingElement instanceof Robot) {
                            getPlayerOf(conflictingElement).takeDamage(phase, player, null, false);
                        }
                    }
                    if (move(conflictingElement, direction, strength, 1, step, phase, true, wait, movingRobotName)) {
                        ce.setPosition(targetPosition);
                        if (!ce.isFlying()) {
                            ce.setLevel(CourseHandler.getFloor(course, ce.getPosition()).getLevel());
                        }
                    }
                }
            } else if ((floor1.wall(direction) == WallType.NONE || floor1.wall(direction) == WallType.ONE_WAY_GREEN)
                    && (floor2.wall(direction2) == WallType.LEDGE)) {
                if (!ce.isFlying() && player != null) {
                    player.takeDamage(phase, null, null, false);
                    player.takeDamage(phase, null, null, false);
                }
                CourseElement conflictingElement = getConflictingElement(ce, direction);
                if (conflictingElement == null) {
                    ce.setPosition(targetPosition);
                    if (!ce.isFlying()) {
                        ce.setLevel(CourseHandler.getFloor(course, ce.getPosition()).getLevel());
                    }
                } else if (notBlockedByMovableRobot) {
                    if (move(conflictingElement, direction, strength, 1, step, phase, true, wait, movingRobotName)) {
                        move(ce, direction, strength, amount, step, phase, true, wait, movingRobotName);
                    }
                }
            } else if (floor1.wall(direction) == WallType.REPULSOR_FIELD
                    || ((floor1.wall(direction) == WallType.NONE
                    || floor1.wall(direction) == WallType.ONE_WAY_GREEN
                    || floor1.wall(direction) == WallType.RAMP_DOWN)
                    && floor2.wall(direction2) == WallType.REPULSOR_FIELD)) {
                move(ce, direction2, strength, strength, step, phase, true, wait, movingRobotName);
            }
        }
        if (amount > 0 && wait) {
            notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
        }
        Floor floor3 = CourseHandler.getFloor(course, ce.getPosition());
        if (player != null && !ce.isFlying()) {
            if (!player.getRobot().isVirtual()) {
                for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, floor3.getPosition()))) {
                    if ((co.getType() == ObjectType.MINE || co.getType() == ObjectType.PROXIMITY_MINE)
                            && co.isActive()) {
                        goOff(step, phase, co);
                    }
                }
                for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x() - 1, floor3.getPosition().y())))) {
                    if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                        goOff(step, phase, co);
                    }
                }
                for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x() + 1, floor3.getPosition().y())))) {
                    if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                        goOff(step, phase, co);
                    }
                }
                for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x(), floor3.getPosition().y() - 1)))) {
                    if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                        goOff(step, phase, co);
                    }
                }
                for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x(), floor3.getPosition().y() + 1)))) {
                    if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                        goOff(step, phase, co);
                    }
                }
            }
            CourseObject portal = CourseHandler.getPortal(course, floor3.getPosition());
            if (portal != null) {
                if (portal.getTargetPosition() != null) {
                    boolean noConflict = true;
                    for (Robot courseRobot : CourseHandler.getRobots(course, portal.getTargetPosition())) {
                        if (!courseRobot.isVirtual()) {
                            noConflict = false;
                            break;
                        }
                    }
                    if (noConflict) {
                        floor3 = CourseHandler.getFloor(course, portal.getTargetPosition());
                        ce.setPosition(portal.getTargetPosition());
                        ce.setLevel(floor3.getLevel());
                        if (wait) {
                            notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
                        }
                        if (!player.getRobot().isVirtual()) {
                            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, floor3.getPosition()))) {
                                if ((co.getType() == ObjectType.MINE || co.getType() == ObjectType.PROXIMITY_MINE)
                                        && co.isActive()) {
                                    goOff(step, phase, co);
                                }
                            }
                            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x() - 1, floor3.getPosition().y())))) {
                                if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                                    goOff(step, phase, co);
                                }
                            }
                            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x() + 1, floor3.getPosition().y())))) {
                                if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                                    goOff(step, phase, co);
                                }
                            }
                            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x(), floor3.getPosition().y() - 1)))) {
                                if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                                    goOff(step, phase, co);
                                }
                            }
                            for (CourseObject co : new ArrayList<>(CourseHandler.getObjects(course, new Position(floor3.getPosition().x(), floor3.getPosition().y() + 1)))) {
                                if (co.getType() == ObjectType.PROXIMITY_MINE && co.isActive()) {
                                    goOff(step, phase, co);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!ce.isFlying()) {
            if (floor3.getFloortype() == Floortype.ABYSS || (floor3.getFloortype() == Floortype.TRAPDOOR && floor3.getActiveInPhase()[phase])) {
                amount = 0;
                die(ce, step, phase);
                if (ce instanceof Robot r) {
                    course.getRobotsFallingIntoAbyss().add(r);
                } else if (ce instanceof CourseObject co) {
                    course.getObjectsFallingIntoAbyss().add(co);
                }
            }
        }
        if (amount > 1) {
            move(ce, direction, strength, amount - 1, step, phase, true, wait, movingRobotName);
        }
        if (!oldPosition.equals(ce.getPosition())) {
            boolean notBlocked = true;
            if (!ce.isFlying()) {
                while (notBlocked && CourseHandler.getObjects(course, ce.getPosition()).stream().anyMatch(co -> co.getType() == ObjectType.OIL)) {
                    notBlocked = move(ce, direction, 0, 1, step, phase, false, wait, movingRobotName);
                }
            }
        }
        if (wait) {
            notifyCourseMayHaveChanged(step, phase, null, movingRobotName);
        }
        return !oldPosition.equals(ce.getPosition());
    }

    private void land(Player player, Step step, int phase) {
        Robot robot = player.getRobot();
        Floor f     = CourseHandler.getFloor(course, robot.getPosition());
        while (isRobotConflict(robot) && !player.getRobot().isVirtual()) {
            moveInit(robot, robot.getDirection(), 0, 1, step, phase, true, true, null);
        }
        robot.setFlying(false);
        if (f.getFloortype() == Floortype.ABYSS || (f.getFloortype() == Floortype.TRAPDOOR && f.getActiveInPhase()[phase])) {
            die(robot, step, phase);
            course.getRobotsFallingIntoAbyss().add(robot);
        } else {
            while (f.getLevel() < player.getRobot().getLevel()) {
                player.getRobot().setLevel(player.getRobot().getLevel() - 1);
                player.takeDamage(phase, null, null, false);
                player.takeDamage(phase, null, null, false);
            }
        }
    }

    private boolean isRobotConflict(Robot r) {
        for (Robot r2 : CourseHandler.getRobots(course, r.getPosition())) {
            if (!r2.isVirtual() && !r2.equals(r)) {
                return true;
            }
        }
        return false;
    }

    private CourseElement getConflictingElement(CourseElement courseElement, Direction direction) {
        if (!courseElement.isOnCourse()
                || (courseElement instanceof Robot && ((Robot) courseElement).isVirtual())
                || courseElement.isFlying()) {
            return null;
        }
        Position targetPosition = courseElement.getPosition().neighbour(direction);
        for (Robot ce : CourseHandler.getRobots(course, targetPosition)) {
            if (!ce.isVirtual() && !ce.equals(courseElement) && !ce.isFlying()) {
                return ce;
            }
        }
        return null;
    }

    private int getCBConflictingElementMark(CourseElement be2, Direction direction) {
        int           erg = 1;
        CourseElement ce  = getConflictingElement(be2, direction);
        if (ce != null) {
            erg = ce.getConflictingMark();
        }
        return erg;
    }

    public Robot getLaserTarget(Position position, Direction direction, int level, boolean highPowerLaser, Step step) {
        return getLaserTarget(position, direction, level, highPowerLaser, false, false, step);
    }

    private Robot getLaserTarget(Position position, Direction direction, int level, boolean highPowerLaser, boolean isCourseMounted, boolean firstFloorGetsHit, Step step) {
        Robot target = null;
        if (firstFloorGetsHit) {
            for (Robot ps : CourseHandler.getRobots(course, position)) {
                if (ps.getLevel() == level && (isCourseMounted || !ps.isVirtual()) && !highPowerLaser) {
                    target = ps;
                    break;
                } else if (ps.getLevel() == level && (isCourseMounted || !ps.isVirtual())) {
                    highPowerLaser = false;
                }
            }
        }
        if (target == null) {
            Floor     sourceFloor    = CourseHandler.getFloor(course, position);
            Position  targetPosition = position.neighbour(direction);
            Floor     targetFloor    = CourseHandler.getFloor(course, targetPosition);
            Direction direction2     = direction.reverse();
            if (targetPosition.inRange(course.getRange())) {
                if (!(sourceFloor.wall(direction) == WallType.SOLID
                        || sourceFloor.wall(direction) == WallType.ONE_WAY_RED
                        || sourceFloor.wall(direction) == WallType.REPULSOR_FIELD
                        || sourceFloor.wall(direction) == WallType.LEDGE
                        || sourceFloor.wall(direction) == WallType.RAMP_UP
                        || targetFloor.wall(direction2) == WallType.SOLID
                        || targetFloor.wall(direction2) == WallType.REPULSOR_FIELD)) {
                    if (!isCourseMounted) {
                        CourseHandler.setBeam(course, targetPosition, direction, step);
                    }
                    return getLaserTarget(targetPosition, direction, level, highPowerLaser, isCourseMounted, true, step);
                } else if (step == Step.ROBOT_MOUNTED_LASER_FIRE && highPowerLaser) {
                    if (!isCourseMounted) {
                        CourseHandler.setBeam(course, targetPosition, direction, step);
                    }
                    return getLaserTarget(targetPosition, direction, level, false, isCourseMounted, true, step);
                }
            }
        }
        return target;
    }

    public void rotate(CourseElement ce, Direction direction, Step step, int phase, boolean wait) {
        ce.setDirection(ce.getDirection().add(direction));
        if (wait) {
            notifyCourseMayHaveChanged(step, phase, "%s was rotated".formatted(ce.getName()), null);
        }
    }

    public void setPosition(CourseElement ce, int x, int y, Step step, int phase, Direction direction, boolean wait) {
        ce.setPrevPosition(ce.getPosition());
        ce.setPosition(new Position(x, y));
        Floor floor3 = CourseHandler.getFloor(course, ce.getPosition());
        if (floor3.getFloortype() == Floortype.ABYSS || (floor3.getFloortype() == Floortype.TRAPDOOR && floor3.getActiveInPhase()[phase])) {
            die(ce, step, phase);
            if (ce instanceof Robot r) {
                course.getRobotsFallingIntoAbyss().add(r);
            } else if (ce instanceof CourseObject co) {
                course.getObjectsFallingIntoAbyss().add(co);
            }
        }
        if (direction != null) {
            if (CourseHandler.getObjects(course, floor3.getPosition()).stream().anyMatch(co -> co.getType() == ObjectType.OIL)) {
                moveInit(ce, direction, 0, 1, step, phase, false, wait, null);
            }
        }
        moveInit(ce, NORTH, 0, 0, step, phase, false, wait, null);
    }

    public void die(CourseElement ce, Step reason, Integer phase) {
        Player player = getPlayerOf(ce);
        if (player != null) {
            player.getRobot().setDamage(10);
            player.takeNormalDamage(course);
        } else {
            CourseHandler.remove(course, ce);
            notifyCourseMayHaveChanged(reason, phase, "%s died".formatted(ce.getName()), null);
        }
    }

    public void giveUp(Robot r, String registrationId) {
        r.setGiveUp(true);
        r.setDamage(10);
        r.setOnCourse(false);
        CourseHandler.remove(course, r);
        if (StringUtils.hasText(registrationId)) {
            for (Registration reg : List.copyOf(networkPlayers)) {
                if (registrationId.equals(reg.getId())) {
                    networkPlayers.remove(reg);
                }
            }
        }
        notifyCourseMayHaveChanged(Step.SETUP, null, "%s gave up".formatted(r.getName()), null);
    }

    private void deGlue(CourseObject courseObject, int phase) {
        if (courseObject.getGlue() > 0) {
            courseObject.setGlue(courseObject.getGlue() - 1);
            if (courseObject.getGlue() == 0) {
                die(courseObject, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
            }
        }
    }

    private void goOff(Step step, Integer phase, CourseObject courseObject) {
        if (courseObject.getType() == ObjectType.MINE || courseObject.getType() == ObjectType.PROXIMITY_MINE) {
            explode(step, phase, courseObject.getPosition(), MINE_INITIAL_DAMAGE);
        }
        die(courseObject, step, phase);
    }

    private int getPriority(Robot robot, int phase) {
        return robot.getProgram()[phase] == null ? -1 : robot.getProgram()[phase].getPriority();
    }

    public void explode(Step step, Integer phase, Position position, int initialDamage) {
        if (initialDamage > 0) {
            DistanceCalculator.calculateDistance(course, position, 7);
            List<Floor> markers = new ArrayList<>();
            for (Floor f : new ArrayList<>(course.getFloor())) {
                if (f.getDistanceCounter() < 7) {
                    int damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                    if (damage > 0) {
                        Floor floor = CourseHandler.getFloor(course, f.getPosition());
                        floor.setExplosiveDamage(damage);
                        markers.add(floor);
                    }
                }
            }
            for (Robot obj : new ArrayList<>(course.getRobots())) {
                Floor f      = CourseHandler.getFloor(course, obj.getPosition());
                int   damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                for (int damagePoints = 0; damagePoints < damage; damagePoints++) {
                    this.getPlayerOf(obj).takeNormalDamage(course);
                }
            }
            notifyCourseMayHaveChanged(step, phase, "explosion", null);
            Sleep.sleepForMillis(timeSettings.getTimeToShowExplosions());
            for (Floor marker : markers) {
                marker.setExplosiveDamage(0);
            }
        }
    }

    public void addNewModuleToRobot(Robot robot) {
        boolean retry = true;
        while (retry) {
            ModuleType mt = ModuleType.values()[RANDOM.nextInt(ModuleType.values().length)];
            if (mt.getAmmunition() == 0) {
                if (!robot.hasModule(mt)) {
                    retry = false;
                    robot.getModules().add(new Module(mt));
                }
            } else {
                retry = false;
                if (robot.hasModule(mt)) {
                    Module module = robot.getModule(mt);
                    module.setAmmunition(module.getAmmunition() + mt.getAmmunition());
                } else {
                    robot.getModules().add(new Module(mt));
                }
            }
        }
    }

    private static int calculateEffectiveDamage(int initialDamage, int distanceCounter) {
        return switch (distanceCounter) {
            case 0 -> initialDamage;
            case 1 -> initialDamage / 2;
            case 2 -> initialDamage / 4;
            case 3 -> initialDamage / 8;
            case 4 -> initialDamage / 16;
            case 5 -> initialDamage / 32;
            case 6 -> initialDamage / 64;
            default -> 0;
        };
    }

}
