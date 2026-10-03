package de.phlup.circuitchaos.gamelogic;

import de.phlup.circuitchaos.enums.ComputerType;
import de.phlup.circuitchaos.enums.Direction;
import de.phlup.circuitchaos.enums.Floortype;
import de.phlup.circuitchaos.enums.ModuleType;
import de.phlup.circuitchaos.enums.ObjectType;
import de.phlup.circuitchaos.enums.ProgramType;
import de.phlup.circuitchaos.enums.RobotType;
import de.phlup.circuitchaos.enums.Step;
import de.phlup.circuitchaos.enums.WallType;
import de.phlup.circuitchaos.exception.CircuitChaosException;
import de.phlup.circuitchaos.gamelogic.creation.ServerBoardArranger;
import de.phlup.circuitchaos.gamelogic.player.Player;
import de.phlup.circuitchaos.gamelogic.player.computer.ComputerPlayer;
import de.phlup.circuitchaos.gamelogic.player.network.NetworkPlayer;
import de.phlup.circuitchaos.gamelogic.utils.BoardHandler;
import de.phlup.circuitchaos.gamelogic.utils.DistanceCalculator;
import de.phlup.circuitchaos.gamelogic.utils.Shooter;
import de.phlup.circuitchaos.gamelogic.utils.Sleep;
import de.phlup.circuitchaos.gamelogic.utils.Teleporter;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.BoardElement;
import de.phlup.circuitchaos.model.BoardElementStub;
import de.phlup.circuitchaos.model.BoardProperties;
import de.phlup.circuitchaos.model.Checkpoint;
import de.phlup.circuitchaos.model.CircuitChaosObject;
import de.phlup.circuitchaos.model.Floor;
import de.phlup.circuitchaos.model.Module;
import de.phlup.circuitchaos.model.NetworkRequest;
import de.phlup.circuitchaos.model.NetworkResponse;
import de.phlup.circuitchaos.model.Position;
import de.phlup.circuitchaos.model.Programme;
import de.phlup.circuitchaos.model.Range;
import de.phlup.circuitchaos.model.Registration;
import de.phlup.circuitchaos.model.RevealProgrammeListItem;
import de.phlup.circuitchaos.model.RevealProgrammeResponse;
import de.phlup.circuitchaos.model.Robot;
import de.phlup.circuitchaos.service.ImageSupplier;
import de.phlup.circuitchaos.service.PollOrPushSwitchService;
import de.phlup.circuitchaos.settings.TimeSettings;
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

import static de.phlup.circuitchaos.enums.Direction.EAST;
import static de.phlup.circuitchaos.enums.Direction.NORTH;
import static de.phlup.circuitchaos.enums.Direction.SOUTH;
import static de.phlup.circuitchaos.enums.Direction.WEST;
import static de.phlup.circuitchaos.enums.ModuleType.DOUBLE_BARREL_LASER;
import static de.phlup.circuitchaos.enums.ModuleType.EXCHANGE_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.HIGH_POWER_LASER;
import static de.phlup.circuitchaos.enums.ModuleType.MAIN_LASER;
import static de.phlup.circuitchaos.enums.ModuleType.PRESSURE_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.REAR_LASER;
import static de.phlup.circuitchaos.enums.ModuleType.SPIN_LEFT_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.SPIN_RIGHT_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.TRACTOR_BEAM;
import static de.phlup.circuitchaos.gamelogic.GlobalServerAttributes.RANDOM;

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

    private List<BoardElementStub> elementStubs = null;

    private boolean gameAborted = false;

    private Thread loopThread;

    private final Board               board         = new Board();
    @Getter
    private final ServerBoardArranger boardArranger = new ServerBoardArranger();
    @Setter
    private       boolean             notStartedYet = true;

    private final Map<String, Map<String, NetworkResponse>> awaitingResponses = new ConcurrentHashMap<>();

    private int animationSteps;

    public void notifyBoardMayHaveChanged(Step step, Integer phase, String detail) {
        String reason = (phase == null ? "" : (phase + 1) + ": ") + step.getName() + (StringUtils.hasText(detail) ? " - " + detail : "");
        log.debug("Board may have changed: {}: {}", step.name(), reason);
        if (step != Step.CONVEYOR_BELTS_MOVE_NO_WAIT
                && step != Step.EXPRESS_CONVEYOR_BELTS_MOVE_NO_WAIT
                && step != Step.GEARS_ROTATE_NO_WAIT
                && step != Step.PUSHERS_PUSH_NO_WAIT
                && step != Step.BOARD_MOUNTED_PRESSURE_BEAMS_FIRE_NO_WAIT
                && step != Step.BOARD_MOUNTED_TRACTOR_BEAMS_FIRE_NO_WAIT
                && step != Step.ROBOT_MOUNTED_LASER_FIRE_NO_WAIT
                && step != Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE_NO_WAIT
                && step != Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE_NO_WAIT) {
            List<BoardElementStub> stubList = BoardHandler.createBoardElementStubList(board);
            boolean wait = (elementStubs != null && !elementStubs.equals(stubList))
                    || (step == Step.OPEN_TRAPDOORS && board.getProperties().isTrapdoor())
                    || (step == Step.ROBOT_MOUNTED_LASER_FIRE && phase != null
                    && board.getRobots().stream().filter(BoardElement::isOnBoard).anyMatch(
                    r -> (r.hasModule(MAIN_LASER) && r.getModule(MAIN_LASER).getActiveInPhase()[phase])
                            || (r.hasModule(DOUBLE_BARREL_LASER) && r.getModule(DOUBLE_BARREL_LASER).getActiveInPhase()[phase])
                            || (r.hasModule(HIGH_POWER_LASER) && r.getModule(HIGH_POWER_LASER).getActiveInPhase()[phase])
                            || (r.hasModule(REAR_LASER) && r.getModule(REAR_LASER).getActiveInPhase()[phase])))
                    || (step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE && phase != null
                    && board.getRobots().stream().filter(BoardElement::isOnBoard).anyMatch(
                    r -> r.hasModule(PRESSURE_BEAM) && r.getModule(PRESSURE_BEAM).getActiveInPhase()[phase]))
                    || (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && phase != null
                    && board.getRobots().stream().filter(BoardElement::isOnBoard).anyMatch(
                    r -> r.hasModule(TRACTOR_BEAM) && r.getModule(TRACTOR_BEAM).getActiveInPhase()[phase]))
                    || (step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE && phase != null
                    && board.getRobots().stream().filter(BoardElement::isOnBoard).anyMatch(
                    r -> r.hasModule(SPIN_LEFT_BEAM) && r.getModule(SPIN_LEFT_BEAM).getActiveInPhase()[phase]))
                    || (step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE && phase != null
                    && board.getRobots().stream().filter(BoardElement::isOnBoard).anyMatch(
                    r -> r.hasModule(SPIN_RIGHT_BEAM) && r.getModule(SPIN_RIGHT_BEAM).getActiveInPhase()[phase]))
                    || (step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE && phase != null
                    && board.getRobots().stream().filter(BoardElement::isOnBoard).anyMatch(
                    r -> r.hasModule(EXCHANGE_BEAM) && r.getModule(EXCHANGE_BEAM).getActiveInPhase()[phase]))
                    || (step == Step.BOARD_MOUNTED_LASER_FIRE && board.getProperties().isLasers())
                    || (step == Step.BOARD_MOUNTED_PRESSURE_BEAMS_FIRE && board.getProperties().isPressureBeams())
                    || (step == Step.BOARD_MOUNTED_TRACTOR_BEAMS_FIRE && board.getProperties().isTractorBeams())
                    || (step == Step.CONVEYOR_BELTS_MOVE && board.getProperties().isConveyorBelts())
                    || (step == Step.EXPRESS_CONVEYOR_BELTS_MOVE && board.getProperties().isExpressConveyorBelts())
                    || (step == Step.GEARS_ROTATE && board.getProperties().isGears())
                    || (step == Step.PUSHERS_PUSH && board.getProperties().isPushers());
            if (elementStubs == null || wait) {
                if (step == Step.SETUP || !wait) {
                    for (Registration reg : watchers) {
                        // Please note: "reason" will become part of the window title
                        pollOrPushSwitchService.notifyOfBoardChange(reg, board, reason, step, phase, null, animationSteps);
                    }
                } else {
                    randomlySetFloorOrientationForDisplay();
                    for (int subPhase = 0; subPhase < animationSteps; subPhase++) {
                        for (Registration reg : watchers) {
                            pollOrPushSwitchService.notifyOfBoardChange(reg, board, reason, step, phase, subPhase, animationSteps);
                        }
                        Sleep.sleepForMillis(timeSettings.getTimeBetweenSteps() / animationSteps);
                    }
                    resetPreviousPositionsAndDirections();
                }
            }
            // 2nd Phase for falling down robots/objects
            if (!board.getRobotsFallingIntoAbyss().isEmpty() || !board.getObjectsFallingIntoAbyss().isEmpty()) {
                randomlySetFloorOrientationForDisplay();
                for (int subPhase = animationSteps; subPhase < 2 * animationSteps; subPhase++) {
                    for (Registration reg : watchers) {
                        pollOrPushSwitchService.notifyOfBoardChange(reg, board, reason, step, phase, subPhase, animationSteps);
                    }
                    Sleep.sleepForMillis(timeSettings.getTimeBetweenSteps() / animationSteps);
                }
                board.getRobotsFallingIntoAbyss().clear();
                board.getObjectsFallingIntoAbyss().clear();
            }
            elementStubs = stubList;
        }
    }

    private void randomlySetFloorOrientationForDisplay() {
        for (Floor floor : board.getFactoryFloor()) {
            // also see BoardEditor.createBoardElementStubList(Board)
            if (floor.isWater() && !floor.getFloortype().isConveyorBelt()) {
                floor.setFacingDirection(Direction.values()[RANDOM.nextInt(4)]);
            }
        }
    }

    private void resetPreviousPositionsAndDirections() {
        for (BoardElement robot : board.getRobots()) {
            robot.setPrevPosition(robot.getPosition());
            robot.setPrevDirection(robot.getDirection());
        }
        for (BoardElement rrObject : board.getObjects()) {
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
        int numberOfTargetCheckpoint = board.getCheckpoints().size();
        if (numberOfTargetCheckpoint < 2) {
            throw new CircuitChaosException("Not enough checkpoints set.");
        }
        Position startPosition = board.getCheckpoints().stream()
                                      .min(Comparator.comparingInt(Checkpoint::getNumber))
                                      .orElseThrow().getPosition();
        notStartedYet = false;
        determineBoardProperties(board);
        fillMissingBoardElementsWithAbyss();
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
                BoardHandler.add(board, robot);
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
            BoardHandler.add(board, robot);
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

    private void fillMissingBoardElementsWithAbyss() {
        Range range = board.getRange().wide();
        board.setRange(range);
        List<Floor> factoryFloors = board.getFactoryFloor();
        for (int i = range.minX(); i <= range.maxX(); i++) {
            for (int j = range.minY(); j <= range.maxY(); j++) {
                boolean notFound = true;
                for (Floor factoryFloor : factoryFloors) {
                    if (factoryFloor.getPosition().x() == i && factoryFloor.getPosition().y() == j) {
                        notFound = false;
                        break;
                    }
                }
                if (notFound) {
                    Floor floor = new Floor();
                    floor.setFloortype(Floortype.ABYSS);
                    floor.setPosition(new Position(i, j));
                    factoryFloors.add(floor);
                }
            }
        }
    }

    private void determineBoardProperties(Board board) {
        BoardProperties boardProperties = board.getProperties();
        for (Floor floor : board.getFactoryFloor()) {
            if (floor.isHasPusher()) {
                boardProperties.setPushers(true);
            }
            switch (floor.getFloortype()) {
                case GEARS_CCW, GEARS_CW -> boardProperties.setGears(true);
                case TRAPDOOR -> boardProperties.setTrapdoor(true);
                case CONVEYOR_BELT -> boardProperties.setConveyorBelts(true);
                case EXPRESS_CONVEYOR_BELT -> {
                    boardProperties.setConveyorBelts(true);
                    boardProperties.setExpressConveyorBelts(true);
                }
            }
            if (Math.max(Math.max(floor.getLasers()[0], floor.getLasers()[1]), Math.max(floor.getLasers()[2], floor.getLasers()[3])) > 0) {
                boardProperties.setLasers(true);
            }
            if (floor.getPressureBeam()[0] || floor.getPressureBeam()[1] || floor.getPressureBeam()[2] || floor.getPressureBeam()[3]) {
                boardProperties.setPressureBeams(true);
            }
            if (floor.getTractorBeam()[0] || floor.getTractorBeam()[1] || floor.getTractorBeam()[2] || floor.getTractorBeam()[3]) {
                boardProperties.setTractorBeams(true);
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
        robot.setLevel(BoardHandler.getFloor(board, startPosition).getLevel());
        return robot;
    }

    @AllArgsConstructor
    private class LoopThread extends Thread {

        private final String id;

        public void run() {
            try {
                Player winner = null;
                while (winner == null && !players.isEmpty() && !isGameAborted()) {
                    notifyBoardMayHaveChanged(Step.SETUP, null, "start of new turn");

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
                    winner = endOfTurnBoardEffects(winner);
                }
                notifyBoardMayHaveChanged(Step.SETUP, null, "end of turn");
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
            notifyBoardMayHaveChanged(Step.START_OF_TURN_BOARD_EFFECTS, phase, "begin of phase %s".formatted(phase + 1));

            List<Robot> objectsWithPriority = stepRevealProgrammes(phase);
            stepRobotsMove(phase, objectsWithPriority);
            stepBoardElementsMove(phase);
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

    private void robotsMove(int phase, List<Robot> objectsWithPriority) {
        for (Robot robot : objectsWithPriority) {
            if (gameAborted) {
                break;
            }
            Programme program = getRobotsProgram(robot, phase);
            Floor     f       = BoardHandler.getFloor(board, robot.getPosition());
            reactivateRammingArmor(phase, robot);
            Player player = getPlayerOf(robot);
            if (player != null) {
                releaseDevices(phase, player);
                while (!gameAborted && program != null) {
                    int movement = determineMovementAmount(phase, robot, program);
                    useHovercraft(phase, player, movement);

                    Teleporter teleportResult = Teleporter.teleport(robot, movement, this, phase);
                    if (teleportResult.isHasBeenTeleported()) {
                        break;
                    } else {
                        movement = teleportResult.getOldMovement();
                    }

                    movement = applyWaterAndOilSlick(robot, f, movement);
                    boardElementMoves(phase, robot, program, movement);
                    program = null;
                }
            }
        }
    }

    private Player getPlayerOf(BoardElement be) {
        if (be instanceof Robot) {
            for (Player player : players) {
                if (player.getRobot().equals(be)) {
                    return player;
                }
            }
        }
        return null;
    }

    private void discardVirtualStateWhenPossible() {
        for (Player player : players) {
            if (!gameAborted) {
                if (player.getRobot().isOnBoard() && player.getRobot().isVirtual()) {
                    player.getRobot().setVirtual(false);
                    for (Robot be : BoardHandler.getRobots(board, player.getRobot().getPosition())) {
                        if (be != player.getRobot()) {
                            player.getRobot().setVirtual(true);
                            break;
                        }
                    }
                    notifyBoardMayHaveChanged(Step.END_OF_TURN_BOARD_EFFECTS, null, "discard virtual state");
                }
            }
        }
    }

    private Player endOfTurnBoardEffects(Player winner) {
        for (Player player : List.copyOf(players)) {
            if (winner == null && !gameAborted) {
                if (player.getRobot().isFlying()) {
                    land(player, Step.END_OF_TURN_BOARD_EFFECTS, 4);
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
                notifyBoardMayHaveChanged(Step.END_OF_TURN_BOARD_EFFECTS, null, "%s was destroyed".formatted(robot.getName()));
                BoardHandler.add(board, robot);
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
        for (BoardElement o : new ArrayList<>(BoardHandler.getObjectsAndRobots(board))) {
            if (!gameAborted) {
                if (o.isOnBoard()) {
                    Player player = getPlayerOf(o);
                    if (player != null) {
                        Checkpoint cp = BoardHandler.getCheckpoint(board, player.getRobot().getPosition());
                        Floor      f  = BoardHandler.getFloor(board, player.getRobot().getPosition());
                        Floor      f2 = f;
                        if (player.uses(ModuleType.MECHANICAL_ARM, phase)) {
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = BoardHandler.getFloor(board, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() - 1));
                                cp = BoardHandler.getCheckpoint(board, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() - 1));
                            }
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = BoardHandler.getFloor(board, new Position(player.getRobot().getPosition().x() + 1, player.getRobot().getPosition().y()));
                                cp = BoardHandler.getCheckpoint(board, new Position(player.getRobot().getPosition().x() + 1, player.getRobot().getPosition().y()));
                            }
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = BoardHandler.getFloor(board, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() + 1));
                                cp = BoardHandler.getCheckpoint(board, new Position(player.getRobot().getPosition().x(), player.getRobot().getPosition().y() + 1));
                            }
                            if (!(cp != null || f.getFloortype() == Floortype.PIT_STOP)) {
                                f = BoardHandler.getFloor(board, new Position(player.getRobot().getPosition().x() - 2, player.getRobot().getPosition().y()));
                                cp = BoardHandler.getCheckpoint(board, new Position(player.getRobot().getPosition().x() - 1, player.getRobot().getPosition().y()));
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
                    } else if (o instanceof CircuitChaosObject cco) {
                        cco.setActive(true);
                        switch (cco.getType()) {
                            case MINE:
                                if (hasNonVirtualRobot(cco.getPosition().x(), cco.getPosition().y())) {
                                    goOff(Step.TOUCH_CHECKPOINTS, phase, cco);
                                }
                                break;
                            case PROXIMITY_MINE:
                                if (hasNonVirtualRobot(cco.getPosition().x(), cco.getPosition().y())
                                        || hasNonVirtualRobot(cco.getPosition().x() + 1, cco.getPosition().y())
                                        || hasNonVirtualRobot(cco.getPosition().x(), cco.getPosition().y() + 1)
                                        || hasNonVirtualRobot(cco.getPosition().x() - 1, cco.getPosition().y())
                                        || hasNonVirtualRobot(cco.getPosition().x(), cco.getPosition().y() - 1)) {
                                    goOff(Step.TOUCH_CHECKPOINTS, phase, cco);
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
        for (Robot be : board.getRobots()) {
            if (be.getPosition().x() == posx && be.getPosition().y() == posy && !be.isVirtual()) {
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
        List.of(Step.BOARD_MOUNTED_TRACTOR_BEAMS_FIRE,
                Step.BOARD_MOUNTED_PRESSURE_BEAMS_FIRE,
                Step.BOARD_MOUNTED_LASER_FIRE)
            .forEach(step -> boardMountedLasersShoot(phase, step));
    }

    private void boardMountedLasersShoot(int phase, Step step) {
        for (Floor f : new ArrayList<>(board.getFactoryFloor())) {
            for (Direction dir : Direction.values()) {
                Direction dir2 = dir.reverse();
                int amount = switch (step) {
                    case BOARD_MOUNTED_PRESSURE_BEAMS_FIRE -> f.getPressureBeam()[dir.ordinal()] ? 1 : 0;
                    case BOARD_MOUNTED_TRACTOR_BEAMS_FIRE -> f.getTractorBeam()[dir.ordinal()] ? 1 : 0;
                    default -> f.getLasers()[dir.ordinal()];
                };
                for (int cnt = 0; cnt < amount; cnt++) {
                    Robot target = getLaserTarget(f.getPosition(), dir, f.getLevel(), false, true, true, step);
                    if (target != null) {
                        if (step == Step.BOARD_MOUNTED_LASER_FIRE) {
                            Player playerOfRobot = getPlayerOf(target);
                            if (playerOfRobot != null) {
                                playerOfRobot.takeDamage(phase, null, dir2, true);
                            }
                        } else if (step == Step.BOARD_MOUNTED_PRESSURE_BEAMS_FIRE) {
                            moveInit(target, dir, 0, 1, Step.BOARD_MOUNTED_PRESSURE_BEAMS_FIRE_NO_WAIT, phase, true);
                        } else if (step == Step.BOARD_MOUNTED_TRACTOR_BEAMS_FIRE) {
                            moveInit(target, dir.reverse(), 0, 1, Step.BOARD_MOUNTED_TRACTOR_BEAMS_FIRE_NO_WAIT, phase, false);
                        }
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(step, phase, null);
        }
        BoardHandler.clearBeams(board);
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
                if (targetRobot != null && targetRobot.isOnBoard()) {
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
                        moveInit(targetRobot, dir, 0, 1, Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE_NO_WAIT, phase, true);
                    } else if (step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE && weaponType == ModuleType.TRACTOR_BEAM) {
                        moveInit(targetRobot, dir.reverse(), 0, 1, Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE_NO_WAIT, phase, false);
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
                if (targetRobot2 != null && step == Step.ROBOT_MOUNTED_LASER_FIRE && targetRobot2.isOnBoard()) { // kann nur bei High Power Laser der Fall sein
                    Player playerOfTargetRobot = getPlayerOf(targetRobot2);
                    if (playerOfTargetRobot != null) {
                        playerOfTargetRobot.takeDamage(phase, player, dir, true);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(step, phase, null);
        }
        BoardHandler.clearBeams(board);
    }

    private List<Shooter> rememberPositionsOfRobotMountedLasers(int phase, Step step) {
        List<Shooter> shooters = new ArrayList<>();
        for (Player player : players) {
            if (gameAborted) {
                break;
            }
            if (player.getRobot().isOnBoard()) {
                shooters.add(new Shooter(phase, player, this, step));
            }
        }
        return shooters;
    }

    private void stepBoardElementsMove(int phase) {
        expressConveyorBeltsMove(phase);
        conveyorBeltsMove(phase);
        pushersPush(phase);
        gearsRotate(phase);
    }

    private void gearsRotate(int phase) {
        for (Floor f : board.getFactoryFloor()) {
            if (gameAborted) {
                break;
            }
            if (f.getFloortype() == Floortype.GEARS_CCW || f.getFloortype() == Floortype.GEARS_CW) {
                for (BoardElement be : BoardHandler.getObjectsAndRobots(board)) {
                    if (be.isOnBoard()
                            && be.getPosition().equals(f.getPosition())
                            && !be.isFlying()
                            && !(be instanceof Robot && getPlayerOf(be).uses(ModuleType.GYROSCOPIC_STABILIZER, phase))) {
                        rotate(be, f.getFloortype() == Floortype.GEARS_CCW ? WEST : EAST, Step.GEARS_ROTATE_NO_WAIT, phase);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(Step.GEARS_ROTATE, phase, null);
        }
    }

    private void pushersPush(int phase) {
        for (Floor f : new ArrayList<>(board.getFactoryFloor())) {
            if (gameAborted) {
                break;
            }
            if (f.isHasPusher() && f.getActiveInPhase()[phase]) {
                for (BoardElement be : BoardHandler.getObjectsAndRobots(board)) {
                    if (be.isOnBoard() && be.getPosition().equals(f.getPosition())) {
                        if (willBePushed(be)) {
                            moveInit(be, f.getPusherDirection(), 1, 1, Step.PUSHERS_PUSH_NO_WAIT, phase, true);
                        }
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(Step.PUSHERS_PUSH, phase, null);
        }
    }

    private boolean willBePushed(BoardElement be) {
        if (be instanceof CircuitChaosObject cco) {
            return cco.getType() != ObjectType.RANDOMIZER
                    && cco.getType() != ObjectType.TELEPORTER
                    && cco.getType() != ObjectType.GLUE
                    && cco.getType() != ObjectType.OIL
                    && cco.getType() != ObjectType.PORTAL_RED
                    && cco.getType() != ObjectType.PORTAL_BLUE
                    && cco.getType() != ObjectType.PORTAL_YELLOW
                    && cco.getType() != ObjectType.PORTAL_PURPLE
                    && cco.getType() != ObjectType.PORTAL_GREEN;
        } else {
            return true;
        }
    }

    private void conveyorBeltsMove(int phase) {
        boolean needToGoAgain = false;
        int     iteration     = 0;
        for (BoardElement obj : BoardHandler.getObjectsAndRobots(board)) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnBoard()) {
                Floor f = BoardHandler.getFloor(board, obj.getPosition());
                obj.setConflictingMark((f.getFloortype().isConveyorBelt() && (!f.isWater()) && !obj.isFlying()) ? 0 : -1);
                if (f.getFloortype().isConveyorBelt() && !f.isWater()) {
                    needToGoAgain = true;
                }
            }
        }
        while (needToGoAgain && iteration < 20 && !gameAborted) {
            needToGoAgain = false;
            iteration++;
            for (BoardElement obj : new ArrayList<>(BoardHandler.getObjectsAndRobots(board))) {
                if (obj.isOnBoard()) {
                    if (obj.getConflictingMark() == 0) {
                        Floor     f                = BoardHandler.getFloor(board, obj.getPosition());
                        Floor     targetFloor      = BoardHandler.getFloor(board, f.getPosition().neighbour(f.getFacingDirection()));
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
                            rotateObjectsOnConveyorBelts(Step.CONVEYOR_BELTS_MOVE_NO_WAIT, phase, obj, targetFloor, f);
                            setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.CONVEYOR_BELTS_MOVE_NO_WAIT, phase, f.getFacingDirection());
                        } else {
                            needToGoAgain = true;
                        }
                    }
                }
            }
        }
        for (BoardElement obj : new ArrayList<>(BoardHandler.getObjectsAndRobots(board))) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnBoard()) {
                if (obj.getConflictingMark() == 0) {
                    Floor f           = BoardHandler.getFloor(board, obj.getPosition());
                    Floor targetFloor = BoardHandler.getFloor(board, f.getPosition().neighbour(f.getFacingDirection()));
                    rotateObjectsOnConveyorBelts(Step.CONVEYOR_BELTS_MOVE_NO_WAIT, phase, obj, targetFloor, f);
                    setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.CONVEYOR_BELTS_MOVE_NO_WAIT, phase, f.getFacingDirection());
                }
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(Step.CONVEYOR_BELTS_MOVE, phase, null);
        }
    }

    private void expressConveyorBeltsMove(int phase) {
        boolean needToGoAgain = false;
        int     iteration     = 0;
        for (BoardElement obj : BoardHandler.getObjectsAndRobots(board)) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnBoard()) {
                Floor f = BoardHandler.getFloor(board, obj.getPosition());
                obj.setConflictingMark((f.getFloortype().isExpressConveyorBelt() && !obj.isFlying()) ? 0 : -1);
                if (f.getFloortype().isExpressConveyorBelt()) {
                    needToGoAgain = true;
                }
            }
        }
        while (needToGoAgain && iteration < 20 && !gameAborted) {
            needToGoAgain = false;
            iteration++;
            for (BoardElement obj : new ArrayList<>(BoardHandler.getObjectsAndRobots(board))) {
                if (gameAborted) {
                    break;
                }
                if (obj.isOnBoard()) {
                    if (obj.getConflictingMark() == 0) {
                        Floor     f                = BoardHandler.getFloor(board, obj.getPosition());
                        Floor     targetFloor      = BoardHandler.getFloor(board, f.getPosition().neighbour(f.getFacingDirection()));
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
                            rotateObjectsOnConveyorBelts(Step.EXPRESS_CONVEYOR_BELTS_MOVE_NO_WAIT, phase, obj, targetFloor, f);
                            setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.EXPRESS_CONVEYOR_BELTS_MOVE_NO_WAIT, phase, f.getFacingDirection());
                        } else {
                            needToGoAgain = true;
                        }
                    }
                }
            }
        }
        for (BoardElement obj : new ArrayList<>(BoardHandler.getObjectsAndRobots(board))) {
            if (gameAborted) {
                break;
            }
            if (obj.isOnBoard()) {
                if (obj.getConflictingMark() == 0) {
                    Floor f           = BoardHandler.getFloor(board, obj.getPosition());
                    Floor targetFloor = BoardHandler.getFloor(board, f.getPosition().neighbour(f.getFacingDirection()));
                    rotateObjectsOnConveyorBelts(Step.EXPRESS_CONVEYOR_BELTS_MOVE_NO_WAIT, phase, obj, targetFloor, f);
                    setPosition(obj, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.EXPRESS_CONVEYOR_BELTS_MOVE_NO_WAIT, phase, f.getFacingDirection());
                }
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(Step.EXPRESS_CONVEYOR_BELTS_MOVE, phase, null);
        }
    }

    private void boardElementMoves(int phase, BoardElement be, Programme pc, int movement) {
        if (be instanceof Robot) {
            if (movement > 0) {
                moveInit(be, be.getDirection(), movement, movement, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
            } else if (movement < 0) {
                moveInit(be, SOUTH.add(be.getDirection()), movement, -movement, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
            }
            if (pc.getType() == ProgramType.ROTATE_RIGHT) {
                rotate(be, EAST, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
            } else if (pc.getType() == ProgramType.ROTATE_LEFT) {
                rotate(be, WEST, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
            } else if (pc.getType() == ProgramType.U_TURN) {
                rotate(be, SOUTH, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
            }
            Player player = getPlayerOf(be);
            if (player.uses(ModuleType.HOVERCRAFT, phase)) {
                land(player, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
            }
        } else {
            moveInit(be, (pc.getType() == ProgramType.ROTATE_LEFT ? WEST : EAST).add(be.getDirection()), 1, 1, Step.ROBOTS_AND_OBJECTS_MOVE, phase, true);
        }
    }

    private void rotateObjectsOnConveyorBelts(Step step, int phase, BoardElement obj, Floor targetFloor, Floor sourceFloor) {
        if (!(obj instanceof Robot && getPlayerOf(obj).uses(ModuleType.GYROSCOPIC_STABILIZER, phase))) {
            if ((targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CCW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(WEST)
                    || (targetFloor.getFacingDirection() == WEST && sourceFloor.getFacingDirection() == NORTH))) {
                rotate(obj, WEST, step, phase);
            } else if ((targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW)
                    && (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(EAST)
                    || (sourceFloor.getFacingDirection() == WEST && targetFloor.getFacingDirection() == NORTH))) {
                rotate(obj, EAST, step, phase);
            } else if (targetFloor.getFloortype() == Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW
                    || targetFloor.getFloortype() == Floortype.TURNING_CONVEYOR_BELT_CW_CCW) {
                if (targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(WEST)
                        || (targetFloor.getFacingDirection() == WEST && sourceFloor.getFacingDirection() == NORTH)) {
                    rotate(obj, WEST, step, phase);
                } else if ((targetFloor.getFacingDirection() == NORTH && sourceFloor.getFacingDirection() == WEST)
                        || targetFloor.getFacingDirection() == sourceFloor.getFacingDirection().add(EAST)) {
                    rotate(obj, EAST, step, phase);
                }
            }
        }
    }

    private int applyWaterAndOilSlick(BoardElement be, Floor f, int movement) {
        if ((f.isWater() || BoardHandler.getObjects(board, f.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) && !be.isFlying()) {
            if (movement > 0) {
                movement--;
            }
            if (movement < 0) {
                movement++;
            }
        }
        return movement;
    }

    private void useHovercraft(int phase, Player r, int movement) {
        if (r.uses(ModuleType.HOVERCRAFT, phase)) {
            r.setFlying(movement != 0);
        }
    }

    private int determineMovementAmount(int phase, BoardElement be, Programme pc) {
        int movement;
        if (be instanceof Robot) {
            movement = pc.getType().getMovement();
            if (movement != 0 && getPlayerOf(be).uses(ModuleType.BRAKES, phase)) {
                movement = movement > 0 ? movement - 1 : movement + 1;
            }
        } else {
            movement = pc.getType().getMovement();
        }
        return movement;
    }

    private void releaseDevices(int phase, Player player) {
        Robot robot      = player.getRobot();
        Floor standingOn = BoardHandler.getFloor(board, robot.getPosition());
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.MOBILE_TELEPORTER, phase)) {
                Module module = player.getRobot().getModule(ModuleType.MOBILE_TELEPORTER);
                if (module.getAmmunition() > 0) {
                    BoardHandler.createCircuitChaosObject(board, ObjectType.TELEPORTER, player);
                    module.setAmmunition(module.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.GLUE_DISPENSER, phase)) {
                if (!standingOn.isWater() && (standingOn.getFloortype() == Floortype.OPEN_FLOOR //
                        || standingOn.getFloortype() == Floortype.PIT_STOP //
                        || standingOn.getFloortype() == Floortype.GEARS_CW //
                        || standingOn.getFloortype() == Floortype.GEARS_CCW)) {
                    Module glueDispenser = player.getRobot().getModule(ModuleType.GLUE_DISPENSER);
                    if (glueDispenser.getAmmunition() > 0) {
                        BoardHandler.createGlue(board, player.getRobot().getPosition());
                        glueDispenser.setAmmunition(glueDispenser.getAmmunition() - 1);
                    }
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.OIL_DISPENSER, phase)) {
                if (!standingOn.isWater() && (standingOn.getFloortype() == Floortype.OPEN_FLOOR //
                        || standingOn.getFloortype() == Floortype.PIT_STOP //
                        || standingOn.getFloortype() == Floortype.GEARS_CW //
                        || standingOn.getFloortype() == Floortype.GEARS_CCW)) {
                    Module oilDispenser = player.getRobot().getModule(ModuleType.OIL_DISPENSER);
                    if (oilDispenser.getAmmunition() > 0) {
                        BoardHandler.createOil(board, player.getRobot().getPosition());
                        oilDispenser.setAmmunition(oilDispenser.getAmmunition() - 1);
                    }
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.MINE_LAYER, phase)) {
                Module mineLayer = player.getRobot().getModule(ModuleType.MINE_LAYER);
                if (mineLayer.getAmmunition() > 0) {
                    BoardHandler.createCircuitChaosObject(board, ObjectType.MINE, player);
                    mineLayer.setAmmunition(mineLayer.getAmmunition() - 1);
                }
            }
        }
        if (!robot.isVirtual()) {
            if (player.uses(ModuleType.PROXIMITY_MINE_LAYER, phase)) {
                Module mineLayer = player.getRobot().getModule(ModuleType.PROXIMITY_MINE_LAYER);
                if (mineLayer.getAmmunition() > 0) {
                    BoardHandler.createCircuitChaosObject(board, ObjectType.PROXIMITY_MINE, player);
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
                                BoardHandler.getFloor(board, new Position(robot.getPosition().x(), robot.getPosition().y() - 1));
                        case EAST ->
                                BoardHandler.getFloor(board, new Position(robot.getPosition().x() + 1, robot.getPosition().y()));
                        case SOUTH ->
                                BoardHandler.getFloor(board, new Position(robot.getPosition().x(), robot.getPosition().y() + 1));
                        case WEST ->
                                BoardHandler.getFloor(board, new Position(robot.getPosition().x() - 1, robot.getPosition().y()));
                    };
                    if (f2.getFloortype() == Floortype.ABYSS || f2.getFloortype() == Floortype.TRAPDOOR) {
                        bridgeProjector.setAmmunition(bridgeProjector.getAmmunition() - 1);
                        f2.setFloortype(Floortype.OPEN_FLOOR);
                    }
                }
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(Step.RELEASE_DEVICES, phase, null);
        }
    }

    private void reactivateRammingArmor(int phase, BoardElement be) {
        if (be instanceof Robot robot) {
            Player player = getPlayerOf(be);
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
        for (Robot be : robots) {
            if (gameAborted) {
                break;
            }
            Floor f = BoardHandler.getFloor(board, be.getPosition());
            if (f.getFloortype() == Floortype.TRAPDOOR && f.getActiveInPhase()[phase]) {
                die(be, Step.OPEN_TRAPDOORS, phase);
            }
        }
        if (!gameAborted) {
            notifyBoardMayHaveChanged(Step.OPEN_TRAPDOORS, phase, null);
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
            Programme program = getRobotsProgram(r, phase);
            if (program != null && !r.isPoweredDown() && r.isOnBoard()) {
                revealProgrammeResponse.getProgramme().add(new RevealProgrammeListItem(r.getName(), ImageSupplier.getRobotImagePathAndName(r), program));
            }
        }
        for (Registration reg : watchers) {
            pollOrPushSwitchService.revealProgrammes(reg, revealProgrammeResponse);
        }
    }

    private List<Robot> revealProgrammesOrderExecutorByPriority(int phase) {
        return BoardHandler.getRobots(board)
                           .stream()
                           .filter(Objects::nonNull)
                           .filter(be -> getPriority(be, phase) >= 0)
                           .sorted((a, b) -> Integer.compare(getPriority(b, phase), getPriority(a, phase)))
                           .toList();
    }

    private void revealProgrammesRandomizers(int phase) {
        for (CircuitChaosObject cco : board.getObjects()) {
            if (gameAborted) {
                break;
            }
            if (cco.getType() == ObjectType.RANDOMIZER) {
                for (Robot r : BoardHandler.getRobots(board, cco.getPosition())) {
                    r.getForcedProgram()[phase] = Programme.create(r.hasModule(ModuleType.OVERDRIVE), r.hasModule(ModuleType.REVERSE_DRIVE));
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

    public void moveInit(BoardElement be, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot) {
        move(be, direction, strength, amount, step, phase, notBlockedByMovableRobot);
    }

    private boolean move(BoardElement be, Direction direction, int strength, int amount, Step step, int phase, boolean notBlockedByMovableRobot) {
        Position oldPosition    = be.getPosition();
        Floor    floor1         = BoardHandler.getFloor(board, oldPosition);
        Position targetPosition = be.getPosition().neighbour(direction);
        Floor    floor2         = BoardHandler.getFloor(board, targetPosition);
        if (!be.isFlying()) {
            for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, floor1.getPosition()))) {
                if (cco.getType() == ObjectType.GLUE && cco.isActive()) {
                    while (cco.getGlue() > 0 && amount > 0) {
                        deGlue(cco, phase);
                        amount--;
                    }
                }
            }
        }
        Direction direction2 = direction.reverse();
        if (floor1.wall(direction) == WallType.RAMP_UP && floor2.wall(direction2) == WallType.RAMP_DOWN && !be.isFlying()) {
            amount--;
        }
        if (!be.getPosition().inRange(board.getRange())) {
            amount = 0;
            die(be, step, phase);
        }
        Player player = getPlayerOf(be);
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
                BoardElement conflictingElement = getConflictingElement(be, direction);
                if (conflictingElement == null) {
                    be.setPosition(targetPosition);
                    if (!be.isFlying()) {
                        be.setLevel(BoardHandler.getFloor(board, be.getPosition()).getLevel());
                    }
                } else if (notBlockedByMovableRobot) {
                    if (player != null && player.uses(ModuleType.RAMMING_ARMOR, phase)) {
                        player.getRobot().getModule(ModuleType.RAMMING_ARMOR).getActiveInPhase()[phase] = false;
                        if (conflictingElement instanceof Robot) {
                            getPlayerOf(conflictingElement).takeDamage(phase, player, null, false);
                        }
                    }
                    if (move(conflictingElement, direction, strength, 1, step, phase, true)) {
                        be.setPosition(targetPosition);
                        if (!be.isFlying()) {
                            be.setLevel(BoardHandler.getFloor(board, be.getPosition()).getLevel());
                        }
                    }
                }
            } else if ((floor1.wall(direction) == WallType.NONE || floor1.wall(direction) == WallType.ONE_WAY_GREEN)
                    && (floor2.wall(direction2) == WallType.LEDGE)) {
                if (!be.isFlying() && player != null) {
                    player.takeDamage(phase, null, null, false);
                    player.takeDamage(phase, null, null, false);
                }
                BoardElement conflictingElement = getConflictingElement(be, direction);
                if (conflictingElement == null) {
                    be.setPosition(targetPosition);
                    if (!be.isFlying()) {
                        be.setLevel(BoardHandler.getFloor(board, be.getPosition()).getLevel());
                    }
                } else if (notBlockedByMovableRobot) {
                    if (move(conflictingElement, direction, strength, 1, step, phase, true)) {
                        move(be, direction, strength, amount, step, phase, true);
                    }
                }
            } else if (floor1.wall(direction) == WallType.REPULSOR_FIELD
                    || ((floor1.wall(direction) == WallType.NONE
                    || floor1.wall(direction) == WallType.ONE_WAY_GREEN
                    || floor1.wall(direction) == WallType.RAMP_DOWN)
                    && floor2.wall(direction2) == WallType.REPULSOR_FIELD)) {
                move(be, direction2, strength, strength, step, phase, true);
            }
        }
        if (amount > 0) {
            notifyBoardMayHaveChanged(step, phase, null);
        }
        Floor floor3 = BoardHandler.getFloor(board, be.getPosition());
        if (player != null && !be.isFlying()) {
            if (!player.getRobot().isVirtual()) {
                for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, floor3.getPosition()))) {
                    if ((cco.getType() == ObjectType.MINE || cco.getType() == ObjectType.PROXIMITY_MINE)
                            && cco.isActive()) {
                        goOff(step, phase, cco);
                    }
                }
                for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x() - 1, floor3.getPosition().y())))) {
                    if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                        goOff(step, phase, cco);
                    }
                }
                for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x() + 1, floor3.getPosition().y())))) {
                    if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                        goOff(step, phase, cco);
                    }
                }
                for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x(), floor3.getPosition().y() - 1)))) {
                    if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                        goOff(step, phase, cco);
                    }
                }
                for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x(), floor3.getPosition().y() + 1)))) {
                    if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                        goOff(step, phase, cco);
                    }
                }
            }
            CircuitChaosObject portal = BoardHandler.getPortal(board, floor3.getPosition());
            if (portal != null) {
                if (portal.getTargetPosition() != null) {
                    boolean noConflict = true;
                    for (CircuitChaosObject circuitChaosObject : BoardHandler.getObjects(board, portal.getTargetPosition())) {
                        if (!circuitChaosObject.getType().isFlat() && !circuitChaosObject.isFlying()) {
                            noConflict = false;
                            break;
                        }
                    }
                    if (noConflict) {
                        for (Robot boardRobot : BoardHandler.getRobots(board, portal.getTargetPosition())) {
                            if (!boardRobot.isVirtual()) {
                                noConflict = false;
                                break;
                            }
                        }
                    }
                    if (noConflict) {
                        floor3 = BoardHandler.getFloor(board, portal.getTargetPosition());
                        be.setPosition(portal.getTargetPosition());
                        be.setLevel(floor3.getLevel());
                        notifyBoardMayHaveChanged(step, phase, null);
                        if (!player.getRobot().isVirtual()) {
                            for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, floor3.getPosition()))) {
                                if ((cco.getType() == ObjectType.MINE || cco.getType() == ObjectType.PROXIMITY_MINE)
                                        && cco.isActive()) {
                                    goOff(step, phase, cco);
                                }
                            }
                            for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x() - 1, floor3.getPosition().y())))) {
                                if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                                    goOff(step, phase, cco);
                                }
                            }
                            for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x() + 1, floor3.getPosition().y())))) {
                                if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                                    goOff(step, phase, cco);
                                }
                            }
                            for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x(), floor3.getPosition().y() - 1)))) {
                                if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                                    goOff(step, phase, cco);
                                }
                            }
                            for (CircuitChaosObject cco : new ArrayList<>(BoardHandler.getObjects(board, new Position(floor3.getPosition().x(), floor3.getPosition().y() + 1)))) {
                                if (cco.getType() == ObjectType.PROXIMITY_MINE && cco.isActive()) {
                                    goOff(step, phase, cco);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!be.isFlying()) {
            if (floor3.getFloortype() == Floortype.ABYSS || (floor3.getFloortype() == Floortype.TRAPDOOR && floor3.getActiveInPhase()[phase])) {
                amount = 0;
                die(be, step, phase);
                if (be instanceof Robot r) {
                    board.getRobotsFallingIntoAbyss().add(r);
                } else if (be instanceof CircuitChaosObject cco) {
                    board.getObjectsFallingIntoAbyss().add(cco);
                }
            }
        }
        if (amount > 1) {
            move(be, direction, strength, amount - 1, step, phase, true);
        }
        if (!oldPosition.equals(be.getPosition())) {
            boolean notBlocked = true;
            if (!be.isFlying()) {
                while (notBlocked && BoardHandler.getObjects(board, be.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                    notBlocked = move(be, direction, 0, 1, step, phase, false);
                }
            }
        }
        notifyBoardMayHaveChanged(step, phase, null);
        return !oldPosition.equals(be.getPosition());
    }

    private void land(Player player, Step step, int phase) {
        Robot robot = player.getRobot();
        Floor f     = BoardHandler.getFloor(board, robot.getPosition());
        while (isRobotConflict(robot) && !player.getRobot().isVirtual()) {
            moveInit(robot, robot.getDirection(), 0, 1, step, phase, true);
        }
        player.setFlying(false);
        if (f.getFloortype() == Floortype.ABYSS || (f.getFloortype() == Floortype.TRAPDOOR && f.getActiveInPhase()[phase])) {
            die(robot, step, phase);
            board.getRobotsFallingIntoAbyss().add(robot);
        } else {
            while (f.getLevel() < player.getRobot().getLevel()) {
                player.getRobot().setLevel(player.getRobot().getLevel() - 1);
                player.takeDamage(phase, null, null, false);
                player.takeDamage(phase, null, null, false);
            }
            if (player.getRobot().isTakesDamageOnLanding()) {
                player.takeDamage(phase, null, null, false);
                player.takeDamage(phase, null, null, false);
            }
        }
    }

    private boolean isRobotConflict(Robot r) {
        for (Robot r2 : BoardHandler.getRobots(board, r.getPosition())) {
            if (!r2.isVirtual() && !r2.equals(r)) {
                return true;
            }
        }
        return false;
    }

    private BoardElement getConflictingElement(BoardElement boardElement, Direction direction) {
        if (!boardElement.isOnBoard()
                || (boardElement instanceof Robot && ((Robot) boardElement).isVirtual())
                || boardElement.isFlying()) {
            return null;
        }
        Position targetPosition = boardElement.getPosition().neighbour(direction);
        for (CircuitChaosObject be : BoardHandler.getObjects(board, targetPosition)) {
            if (!be.getType().isFlat() && !be.equals(boardElement) && !be.isFlying()) {
                return be;
            }
        }
        for (Robot be : BoardHandler.getRobots(board, targetPosition)) {
            if (!be.isVirtual() && !be.equals(boardElement) && !be.isFlying()) {
                return be;
            }
        }
        return null;
    }

    private int getCBConflictingElementMark(BoardElement be2, Direction direction) {
        int          erg = 1;
        BoardElement be  = getConflictingElement(be2, direction);
        if (be != null) {
            erg = be.getConflictingMark();
        }
        return erg;
    }

    public Robot getLaserTarget(Position position, Direction direction, int level, boolean highPowerLaser, Step step) {
        return getLaserTarget(position, direction, level, highPowerLaser, false, false, step);
    }

    private Robot getLaserTarget(Position position, Direction direction, int level, boolean highPowerLaser, boolean isBoardMounted, boolean firstFloorGetsHit, Step step) {
        Robot target = null;
        if (firstFloorGetsHit) {
            for (Robot ps : BoardHandler.getRobots(board, position)) {
                if (ps.getLevel() == level && (isBoardMounted || !ps.isVirtual()) && !highPowerLaser) {
                    target = ps;
                    break;
                } else if (ps.getLevel() == level && (isBoardMounted || !ps.isVirtual())) {
                    highPowerLaser = false;
                }
            }
        }
        if (target == null) {
            Floor     sourceFloor    = BoardHandler.getFloor(board, position);
            Position  targetPosition = position.neighbour(direction);
            Floor     targetFloor    = BoardHandler.getFloor(board, targetPosition);
            Direction direction2     = direction.reverse();
            if (targetPosition.inRange(board.getRange())) {
                if (!(sourceFloor.wall(direction) == WallType.SOLID
                        || sourceFloor.wall(direction) == WallType.ONE_WAY_RED
                        || sourceFloor.wall(direction) == WallType.REPULSOR_FIELD
                        || sourceFloor.wall(direction) == WallType.LEDGE
                        || sourceFloor.wall(direction) == WallType.RAMP_UP
                        || targetFloor.wall(direction2) == WallType.SOLID
                        || targetFloor.wall(direction2) == WallType.REPULSOR_FIELD)) {
                    if (!isBoardMounted) {
                        BoardHandler.setBeam(board, targetPosition, direction, step);
                    }
                    return getLaserTarget(targetPosition, direction, level, highPowerLaser, isBoardMounted, true, step);
                } else if (step == Step.ROBOT_MOUNTED_LASER_FIRE && highPowerLaser) {
                    if (!isBoardMounted) {
                        BoardHandler.setBeam(board, targetPosition, direction, step);
                    }
                    return getLaserTarget(targetPosition, direction, level, false, isBoardMounted, true, step);
                }
            }
        }
        return target;
    }

    public void rotate(BoardElement be, Direction direction, Step step, int phase) {
        be.setDirection(be.getDirection().add(direction));
        notifyBoardMayHaveChanged(step, phase, "%s was rotated".formatted(be.getName()));
    }

    public void setPosition(BoardElement be, int x, int y, Step step, int phase, Direction direction) {
        be.setPrevPosition(be.getPosition());
        be.setPosition(new Position(x, y));
        Floor floor3 = BoardHandler.getFloor(board, be.getPosition());
        if (floor3.getFloortype() == Floortype.ABYSS || (floor3.getFloortype() == Floortype.TRAPDOOR && floor3.getActiveInPhase()[phase])) {
            die(be, step, phase);
            if (be instanceof Robot r) {
                board.getRobotsFallingIntoAbyss().add(r);
            } else if (be instanceof CircuitChaosObject cco) {
                board.getObjectsFallingIntoAbyss().add(cco);
            }
        }
        if (direction != null) {
            if (BoardHandler.getObjects(board, floor3.getPosition()).stream().anyMatch(cco -> cco.getType() == ObjectType.OIL)) {
                moveInit(be, direction, 0, 1, step, phase, false);
            }
        }
        moveInit(be, NORTH, 0, 0, step, phase, false);
    }

    public void die(BoardElement be, Step reason, Integer phase) {
        Player player = getPlayerOf(be);
        if (player != null) {
            player.getRobot().setDamage(10);
            player.takeNormalDamage(board);
        } else {
            BoardHandler.remove(board, be);
            notifyBoardMayHaveChanged(reason, phase, "%s died".formatted(be.getName()));
        }
    }

    public void giveUp(Robot r, String registrationId) {
        r.setGiveUp(true);
        r.setDamage(10);
        r.setOnBoard(false);
        BoardHandler.remove(board, r);
        if (StringUtils.hasText(registrationId)) {
            for (Registration reg : List.copyOf(networkPlayers)) {
                if (registrationId.equals(reg.getId())) {
                    networkPlayers.remove(reg);
                }
            }
        }
        notifyBoardMayHaveChanged(Step.SETUP, null, "%s gave up".formatted(r.getName()));
    }

    public Programme getRobotsProgram(Robot robot, int phase) {
        Programme program = robot.getForcedProgram()[phase];
        if (program == null && robot.getProgram()[phase] != null) {
            program = robot.getProgram()[phase];
        }
        return program;
    }

    private void deGlue(CircuitChaosObject circuitChaosObject, int phase) {
        if (circuitChaosObject.getGlue() > 0) {
            circuitChaosObject.setGlue(circuitChaosObject.getGlue() - 1);
            if (circuitChaosObject.getGlue() == 0) {
                die(circuitChaosObject, Step.ROBOTS_AND_OBJECTS_MOVE, phase);
            }
        }
    }

    private void goOff(Step step, Integer phase, CircuitChaosObject circuitChaosObject) {
        if (circuitChaosObject.getType().getInitialDamage() > 0) {
            explode(step, phase, circuitChaosObject.getPosition(), circuitChaosObject.getType().getInitialDamage());
        }
        die(circuitChaosObject, step, phase);
    }

    private int getPriority(Robot robot, int phase) {
        if (robot.getForcedProgram()[phase] != null) {
            return robot.getForcedProgram()[phase].getPriority();
        } else if (robot.getProgram()[phase] != null) {
            return robot.getProgram()[phase].getPriority();
        } else {
            return -1;
        }
    }

    public void explode(Step step, Integer phase, Position position, int initialDamage) {
        if (initialDamage > 0) {
            DistanceCalculator.calculateDistance(board, position, 7);
            List<Floor> markers = new ArrayList<>();
            for (Floor f : new ArrayList<>(board.getFactoryFloor())) {
                if (f.getDistanceCounter() < 7) {
                    int damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                    if (damage > 0) {
                        Floor floor = BoardHandler.getFloor(board, f.getPosition());
                        floor.setExplosiveDamage(damage);
                        markers.add(floor);
                    }
                }
            }
            for (Robot obj : new ArrayList<>(board.getRobots())) {
                Floor f      = BoardHandler.getFloor(board, obj.getPosition());
                int   damage = calculateEffectiveDamage(initialDamage, f.getDistanceCounter());
                for (int damagePoints = 0; damagePoints < damage; damagePoints++) {
                    this.getPlayerOf(obj).takeNormalDamage(board);
                }
            }
            notifyBoardMayHaveChanged(step, phase, "explosion");
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
