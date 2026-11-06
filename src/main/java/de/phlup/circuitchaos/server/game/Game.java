package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.CircuitChaosException;
import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.Goal;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.RobotType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.NetworkResponse;
import de.phlup.circuitchaos.common.model.Registration;
import de.phlup.circuitchaos.common.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.common.model.course.Checkpoint;
import de.phlup.circuitchaos.common.model.course.Course;
import de.phlup.circuitchaos.common.model.course.CourseElement;
import de.phlup.circuitchaos.common.model.course.CourseObject;
import de.phlup.circuitchaos.common.model.course.CourseProperties;
import de.phlup.circuitchaos.common.model.course.Floor;
import de.phlup.circuitchaos.common.model.course.Module;
import de.phlup.circuitchaos.common.model.course.Robot;
import de.phlup.circuitchaos.common.records.CourseElementStub;
import de.phlup.circuitchaos.common.records.Position;
import de.phlup.circuitchaos.common.records.Programme;
import de.phlup.circuitchaos.common.records.RevealProgrammeListItem;
import de.phlup.circuitchaos.common.settings.TimeSettings;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.player.NetworkPlayer;
import de.phlup.circuitchaos.server.player.Player;
import de.phlup.circuitchaos.server.player.computer.ComputerPlayer;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public static final String AND_THE_WINNER_IS  = "And The Winner Is: ";
    public static final String THIS_GAME_IS_A_TIE = "This game is a tie.";

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

    private final Course    course    = new Course();
    private final GameLogic gameLogic = new GameLogic(course, this::notifyCourseMayHaveChanged);

    private       Goal                 goal           = Goal.FINISH;
    @Getter
    private final ServerCourseArranger courseArranger = new ServerCourseArranger();
    @Setter
    private       boolean              notStartedYet  = true;

    private final Map<String, Map<String, NetworkResponse>> awaitingResponses = new ConcurrentHashMap<>();

    private int animationSteps;

    public void notifyCourseMayHaveChanged(Step step, Integer phase, String detail, String movingRobotName) {
        String reason = (phase == null ? "" : (phase + 1) + ": ") + step.getName() + (StringUtils.hasText(detail) ? " - " + detail : "");
        log.debug("Course may have changed: {}: {}", step.name(), reason);
        List<CourseElementStub> stubList    = CourseHandler.createCourseElementStubList(course);
        CourseProperties        courseProps = course.getProperties();
        boolean wait = (elementStubs != null && !elementStubs.equals(stubList) && step != Step.SETUP)
                || (step == Step.OPEN_TRAPDOORS && courseProps.isTrapdoor())
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
                || (step == Step.COURSE_MOUNTED_LASER_FIRE && courseProps.isLasers())
                || (step == Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE && courseProps.isPressureBeams())
                || (step == Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE && courseProps.isTractorBeams())
                || (step == Step.CONVEYOR_BELTS_MOVE && courseProps.isConveyorBelts())
                || (step == Step.EXPRESS_CONVEYOR_BELTS_MOVE && courseProps.isExpressConveyorBelts())
                || (step == Step.GEARS_ROTATE && courseProps.isGears())
                || (step == Step.PUSHERS_PUSH && courseProps.isPushers());
        if (elementStubs == null || wait || step == Step.SETUP) {
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
        course.getFloor().forEach(f -> f.setExplosiveDamage(0));
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
        elementStubs = CourseHandler.createCourseElementStubList(course);
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

    private void end(String name) {
        if (name != null) {
            showMessage(AND_THE_WINNER_IS + name);
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
        goal = gameOptions.getGoal();
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
                Robot         robot         = createRobot(robotType);
                NetworkPlayer networkPlayer = new NetworkPlayer(robot, gameId, reg);
                players.addLast(networkPlayer);
                CourseHandler.add(course, robot);
            }
        }
        for (int i = 0; !availableRobotTypes.isEmpty() && i < gameOptions.getMaxNumberOfComputerPlayers(); i++) {
            RobotType robotType = availableRobotTypes.getFirst();
            availableRobotTypes.remove(robotType);
            Robot robot = createRobot(robotType);
            players.addLast(new ComputerPlayer(robot, gameId, goal));
            CourseHandler.add(course, robot);
        }
        int playersSize = players.size();
        if (playersSize > 0) {
            provideRobotsWithDefaultModules(gameOptions.getDefaultModules());
            if (goal == Goal.KILL_ALL) {
                course.getCheckpoints().forEach(cp -> cp.setNumber(0));
            }
            determineStartPositions();
            loopThread = new LoopThread(getGameId());
            loopThread.start();
        } else {
            setGameAborted(true);
            gameLogic.setGameAborted(true);
            end(null);
        }
    }

    private void determineStartPositions() {
        Iterator<Checkpoint> cp = course.getCheckpoints().stream().filter(c -> c.getNumber() == 0).iterator();
        for (Robot robot : course.getRobots()) {
            if (!cp.hasNext()) {
                cp = course.getCheckpoints().stream().filter(c -> c.getNumber() == 0).iterator();
            }
            Position position = cp.next().getPosition();
            robot.setPosition(position);
            robot.setPrevPosition(position);
            robot.setArchivePosition(position);
            robot.setLevel(CourseHandler.getFloor(course, position).getLevel());
        }
    }

    private void provideRobotsWithDefaultModules(List<String> defaultModules) {
        for (String defaultModule : defaultModules) {
            try {
                ModuleType moduleType = ModuleType.valueOf(defaultModule);
                for (Robot robot : course.getRobots()) {
                    robot.getModules().add(new Module(moduleType));
                }
            } catch (Exception e) {
                log.warn("Could not find module {}.", defaultModule);
            }
        }
    }

    private Robot createRobot(RobotType robotType) {
        Robot robot = new Robot();
        robot.setName(robotType.getRobotName());
        robot.setId(UUID.randomUUID().toString());
        return robot;
    }

    @AllArgsConstructor
    private class LoopThread extends Thread {

        private final String id;

        public void run() {
            try {
                String winner = null;
                while (winner == null && !players.isEmpty() && !isGameAborted()) {
                    notifyCourseMayHaveChanged(Step.SETUP, null, "start of new turn", null);

                    deal();
                    letAllPlayersProgramTheRobots(players);

                    if (!gameAborted && log.isDebugEnabled()) {
                        for (Robot r : course.getRobots()) {
                            if (r.isOnCourse()) {
                                Programme[] p = r.getProgram();
                                log.debug("Received program for {}: {} - {} - {} - {} - {}", r.getName(), p[0].type().name(),
                                          p[1].type().name(), p[2].type().name(), p[3].type().name(), p[4].type().name());
                            }
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

    private String loopThroughPhases() {
        String winner = null;
        for (int phase = 0; phase < 5 && winner == null && !players.isEmpty() && !gameAborted; phase++) {
            notifyCourseMayHaveChanged(Step.START_OF_TURN_COURSE_EFFECTS, phase, "begin of phase %s".formatted(phase + 1), null);
            List<Robot> robots = stepRevealProgrammes(phase);
            gameLogic.stepRobotsMove(phase, robots);
            gameLogic.stepCourseElementsMove(phase);
            gameLogic.stepResolveBeamsAndLaserFire(phase);
            winner = setTouchCheckpoints(phase);
            if (goal == Goal.KILL_ALL) {
                List.copyOf(players).stream().filter(p -> p.getRobot().hasLost(goal)).forEach(this::removeLostPlayer);
                winner = checkKillAllWinner();
            }
        }
        return winner;
    }

    private String endOfTurnCourseEffects(String winner) {
        // Important: don't use course.getRobots(), because it does not contain off-board robots
        for (Player player : players) {
            Robot robot = player.getRobot();
            if (winner == null && !gameAborted) {
                if (robot.isFlying()) {
                    gameLogic.land(robot, Step.END_OF_TURN_COURSE_EFFECTS, 4);
                }

                resetPoweredDownForDestroyedRobot(robot);
                robot.setMayChooseDirection(false);
                handleDestroyedRobots(robot);

                if (goal == Goal.FINISH && robot.hasWon(goal, course.getCheckpoints().size())) {
                    winner = robot.getName();
                }
            }
        }
        List.copyOf(players).stream().filter(p -> p.getRobot().hasLost(goal)).forEach(this::removeLostPlayer);
        if (goal == Goal.KILL_ALL) {
            winner = checkKillAllWinner();
        }
        return winner;
    }

    private void removeLostPlayer(Player player) {
        players.remove(player);
        if (player instanceof NetworkPlayer networkPlayer) {
            for (Registration reg : List.copyOf(networkPlayers)) {
                if (reg.getId().equals(networkPlayer.getRegistration().getId())) {
                    networkPlayers.remove(reg);
                }
            }
        }
    }

    private String checkKillAllWinner() {
        if (players.isEmpty() && !gameAborted) {
            showMessage(THIS_GAME_IS_A_TIE);
            setGameAborted(true);
            gameLogic.setGameAborted(true);
        } else if (players.size() == 1) {
            return players.getFirst().getRobot().getName();
        }
        return null;
    }

    private void handleDestroyedRobots(Robot robot) {
        if (robot.getDamage() >= 10 && !gameAborted) {
            if (goal == Goal.FINISH) {
                robot.setDamage((robot.hasModule(ModuleType.SUPERIOR_ARCHIVE_COPY) ? 0 : 2));
                if (!robot.hasLost(goal)) {
                    robot.setPosition(robot.getArchivePosition());
                    robot.setPrevPosition(robot.getArchivePosition());
                    robot.setLevel(robot.getArchiveLevel());
                    robot.setMayChooseDirection(true);
                    notifyCourseMayHaveChanged(Step.END_OF_TURN_COURSE_EFFECTS, null, "%s was destroyed".formatted(robot.getName()), null);
                    CourseHandler.add(course, robot);
                    for (int phase = 0; phase < 5; phase++) {
                        if (robot.getBlocked()[phase]) {
                            robot.getProgram()[phase] = null;
                        }
                    }
                    robot.setBlocked(new boolean[]{false, false, false, false, false});
                }
            }
        } else if (robot.isPowerDownAnnounced()) {
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

    private void resetPoweredDownForDestroyedRobot(Robot robot) {
        if (robot.isPoweredDown() || robot.getDamage() >= 10) {
            robot.setPoweredDown(false);
        }
    }

    private String setTouchCheckpoints(int phase) {
        gameLogic.touchCourseElements(phase);
        String winner = null;
        if (!gameAborted) {
            for (Player player : players) {
                Robot robot = player.getRobot();
                if (robot.isOnCourse()) {
                    Floor floor = CourseHandler.getFloor(course, robot.getPosition());
                    if (phase == 4 && floor.getFloortype() == Floortype.PIT_STOP) {
                        arriveAtPitStop(robot);
                    }
                    if (goal == Goal.FINISH && robot.hasWon(goal, course.getCheckpoints().size())) {
                        winner = robot.getName();
                    }
                }
            }
        }
        return winner;
    }

    public void arriveAtPitStop(Robot robot) {
        addNewModuleToRobot(robot);
        if (robot.getDamage() > 0) {
            robot.setDamage(robot.getDamage() - 1);
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

    private void letAllPlayersProgramTheRobots(List<Player> players) {
        String                       purposeId        = UUID.randomUUID().toString();
        Map<String, NetworkResponse> requestedAnswers = askNetworkPlayersToProgramTheirRobots(purposeId, players);
        // +++ computer players perform
        for (Player player : players) {
            if (player instanceof ComputerPlayer computerPlayer
                    && !(computerPlayer.getRobot().isPoweredDown() || gameAborted || player.getRobot().hasLost(goal))) {
                computerPlayer.perform(course.getCheckpoints().size());
            }
        }
        if (!requestedAnswers.isEmpty()) {
            waitForAllAnswers(requestedAnswers);
            for (Player player : players) {
                if (player instanceof NetworkPlayer networkPlayer && !gameAborted && !player.getRobot().hasLost(goal)) {
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
            if (player instanceof NetworkPlayer networkPlayer && !gameAborted && !player.getRobot().hasLost(goal)) {
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

    private int getPriority(Robot robot, int phase) {
        return robot.isPoweredDown() || robot.getProgram()[phase] == null ? -1 : robot.getProgram()[phase].priority();
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

}
