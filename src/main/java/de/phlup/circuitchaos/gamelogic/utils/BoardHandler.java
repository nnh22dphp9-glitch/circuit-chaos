package de.phlup.circuitchaos.gamelogic.utils;

import de.phlup.circuitchaos.enums.Direction;
import de.phlup.circuitchaos.enums.Floortype;
import de.phlup.circuitchaos.enums.ObjectType;
import de.phlup.circuitchaos.enums.Step;
import de.phlup.circuitchaos.enums.WallType;
import de.phlup.circuitchaos.gamelogic.Game;
import de.phlup.circuitchaos.gamelogic.creation.ServerBoardArranger;
import de.phlup.circuitchaos.gamelogic.player.Player;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.BoardElement;
import de.phlup.circuitchaos.model.BoardElementStub;
import de.phlup.circuitchaos.model.Checkpoint;
import de.phlup.circuitchaos.model.CircuitChaosObject;
import de.phlup.circuitchaos.model.Floor;
import de.phlup.circuitchaos.model.Position;
import de.phlup.circuitchaos.model.Range;
import de.phlup.circuitchaos.model.Robot;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static de.phlup.circuitchaos.enums.ModuleType.EXCHANGE_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.MAIN_LASER;
import static de.phlup.circuitchaos.enums.ModuleType.PRESSURE_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.SPIN_LEFT_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.SPIN_RIGHT_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.TRACTOR_BEAM;
import static de.phlup.circuitchaos.gamelogic.GlobalServerAttributes.RANDOM;

public class BoardHandler {

    public static void add(Board board, BoardElement be) {
        if (be != null) {
            be.setOnBoard(true);
            if (be instanceof Robot) {
                board.getRobots().add((Robot) be);
            } else if (be instanceof CircuitChaosObject) {
                board.getObjects().add((CircuitChaosObject) be);
            }
        }
    }

    public static void add(Board board, Floor floor) {
        if (floor != null) {
            board.getFactoryFloor().add(floor);
        }
    }

    public static void remove(Board board, BoardElement be) {
        if (be != null) {
            be.setOnBoard(false);
            if (be instanceof Robot) {
                board.getRobots().remove(be);
            } else if (be instanceof CircuitChaosObject) {
                board.getObjects().remove(be);
            }
        }
    }

    public static void remove(Board board, Checkpoint checkpoint) {
        if (checkpoint != null) {
            board.getCheckpoints().remove(checkpoint);
        }
    }

    public static void remove(Board board, Floor floor) {
        if (floor != null) {
            board.getFactoryFloor().remove(floor);
        }
    }

    /**
     * Liefert im Zweifelsfall ein ABYSS zurück, nicht null. Ist so im Spielverlauf sinnvoll.
     */
    @NotNull
    public static Floor getFloor(Board board, Position position) {
        for (Floor floor : board.getFactoryFloor()) {
            if (floor.getPosition().x() == position.x() && floor.getPosition().y() == position.y()) {
                return floor;
            }
        }
        Floor floor = new Floor();
        floor.setFloortype(Floortype.ABYSS);
        floor.setPosition(position);
        return floor;
    }

    public static List<BoardElement> getObjectsAndRobots(Board board) {
        List<BoardElement> list = new ArrayList<>();
        list.addAll(board.getRobots());
        list.addAll(board.getObjects());
        return list;
    }

    public static List<Robot> getRobots(Board board) {
        return board.getRobots();
    }

    public static List<CircuitChaosObject> getObjects(Board board, Position position) {
        List<CircuitChaosObject> elements2 = new ArrayList<>();
        for (CircuitChaosObject be : board.getObjects()) {
            if (position.x() == be.getPosition().x() && position.y() == be.getPosition().y()) {
                elements2.add(be);
            }
        }
        return elements2;
    }

    public static List<CircuitChaosObject> getLeavingObjects(Board board, Position position) {
        List<CircuitChaosObject> elements2 = new ArrayList<>();
        for (CircuitChaosObject be : board.getObjects()) {
            if (!be.getPosition().equals(be.getPrevPosition())) {
                if (position.x() == be.getPrevPosition().x() && position.y() == be.getPrevPosition().y()) {
                    elements2.add(be);
                }
            }
        }
        return elements2;
    }

    public static List<Robot> getRobots(Board board, Position position) {
        List<Robot> elements2 = new ArrayList<>();
        for (Robot be : board.getRobots()) {
            if (position.x() == be.getPosition().x() && position.y() == be.getPosition().y()) {
                elements2.add(be);
            }
        }
        return elements2;
    }

    public static List<Robot> getLeavingRobots(Board board, Position position) {
        List<Robot> elements2 = new ArrayList<>();
        for (Robot be : board.getRobots()) {
            if (!be.getPosition().equals(be.getPrevPosition())) {
                if (position.x() == be.getPrevPosition().x() && position.y() == be.getPrevPosition().y()) {
                    elements2.add(be);
                }
            }
        }
        return elements2;
    }

    public static List<BoardElement> getFallingIntoAbyss(Board board, Position position) {
        List<BoardElement> elements2 = new ArrayList<>();
        for (BoardElement be : board.getRobotsFallingIntoAbyss()) {
            if (position.x() == be.getPosition().x() && position.y() == be.getPosition().y()) {
                elements2.add(be);
            }
        }
        for (BoardElement be : board.getObjectsFallingIntoAbyss()) {
            if (position.x() == be.getPosition().x() && position.y() == be.getPosition().y()) {
                elements2.add(be);
            }
        }
        return elements2;
    }

    public static List<BoardElement> getLeavingFallingIntoAbyss(Board board, Position position) {
        List<BoardElement> elements2 = new ArrayList<>();
        for (BoardElement be : board.getRobotsFallingIntoAbyss()) {
            if (!be.getPosition().equals(be.getPrevPosition())) {
                if (position.x() == be.getPrevPosition().x() && position.y() == be.getPrevPosition().y()) {
                    elements2.add(be);
                }
            }
        }
        for (BoardElement be : board.getObjectsFallingIntoAbyss()) {
            if (!be.getPosition().equals(be.getPrevPosition())) {
                if (position.x() == be.getPrevPosition().x() && position.y() == be.getPrevPosition().y()) {
                    elements2.add(be);
                }
            }
        }
        return elements2;
    }

    public static Checkpoint getCheckpoint(Board board, Position position) {
        for (Checkpoint cp : board.getCheckpoints()) {
            if (cp.getPosition().x() == position.x() && cp.getPosition().y() == position.y()) {
                return cp;
            }
        }
        return null;
    }

    public static void rotateFloor(Floor floor) {
        WallType tmp = floor.getWallNorth();
        floor.setWallNorth(floor.getWallWest());
        floor.setWallWest(floor.getWallSouth());
        floor.setWallSouth(floor.getWallEast());
        floor.setWallEast(tmp);
        int tmp2 = floor.getLasers()[0];
        floor.getLasers()[0] = floor.getLasers()[3];
        floor.getLasers()[3] = floor.getLasers()[2];
        floor.getLasers()[2] = floor.getLasers()[1];
        floor.getLasers()[1] = tmp2;
        tmp2 = floor.getBoardMountedLaserBeamsWE();
        floor.setBoardMountedLaserBeamsWE(floor.getBoardMountedLaserBeamsNS());
        floor.setBoardMountedLaserBeamsNS(tmp2);
        boolean tmp3 = floor.getPressureBeam()[0];
        floor.getPressureBeam()[0] = floor.getPressureBeam()[3];
        floor.getPressureBeam()[3] = floor.getPressureBeam()[2];
        floor.getPressureBeam()[2] = floor.getPressureBeam()[1];
        floor.getPressureBeam()[1] = tmp3;
        tmp3 = floor.isBoardMountedPressureBeamsWE();
        floor.setBoardMountedPressureBeamsWE(floor.isBoardMountedPressureBeamsNS());
        floor.setBoardMountedPressureBeamsNS(tmp3);
        tmp3 = floor.getTractorBeam()[0];
        floor.getTractorBeam()[0] = floor.getTractorBeam()[3];
        floor.getTractorBeam()[3] = floor.getTractorBeam()[2];
        floor.getTractorBeam()[2] = floor.getTractorBeam()[1];
        floor.getTractorBeam()[1] = tmp3;
        tmp3 = floor.isBoardMountedTractorBeamsWE();
        floor.setBoardMountedTractorBeamsWE(floor.isBoardMountedTractorBeamsNS());
        floor.setBoardMountedTractorBeamsNS(tmp3);
        floor.setFacingDirection(floor.getFacingDirection().add(Direction.EAST));
        floor.setPusherDirection(floor.getPusherDirection().add(Direction.EAST));
    }

    public static CircuitChaosObject getPortal(Board board, Position position) {
        for (CircuitChaosObject obj : getObjects(board, position)) {
            if (obj.getType().isPortal()) {
                return obj;
            }
        }
        return null;
    }

    public static void createCircuitChaosObject(Board board, ObjectType objectType, Player creator) {
        CircuitChaosObject newRRO = createCircuitChaosObject(board, objectType, creator.getRobot().getPosition());
        newRRO.setLevel(creator.getRobot().getLevel());
        newRRO.setDirection(creator.getRobot().getDirection());
        newRRO.setPrevDirection(newRRO.getDirection());
    }

    public static CircuitChaosObject createCircuitChaosObject(Board board, ObjectType objectType, Position position) {
        CircuitChaosObject cco = new CircuitChaosObject();
        cco.setName(objectType.getName());
        cco.setType(objectType);
        cco.setFlying(objectType.isFlying());
        cco.setPosition(position);
        cco.setPrevPosition(position);
        add(board, cco);
        return cco;
    }

    public static void createGlue(Board board, Position position) {
        CircuitChaosObject glue = new CircuitChaosObject();
        glue.setName(ObjectType.GLUE.getName());
        glue.setType(ObjectType.GLUE);
        glue.setFlying(false);
        glue.setPosition(position);
        glue.setPrevPosition(position);
        glue.setDirection(Direction.random());
        glue.setVariantSeed(RANDOM.nextDouble());
        add(board, glue);
    }

    public static void createOil(Board board, Position position) {
        CircuitChaosObject oil = new CircuitChaosObject();
        oil.setName(ObjectType.OIL.getName());
        oil.setType(ObjectType.OIL);
        oil.setFlying(false);
        oil.setPosition(position);
        oil.setPrevPosition(position);
        oil.setDirection(Direction.random());
        oil.setVariantSeed(RANDOM.nextDouble());
        add(board, oil);
    }

    public static void readjustBoardMinMaxValues(Board board) {
        boolean firstElement = true;
        int     maxx         = 0;
        int     maxy         = 0;
        int     minx         = 0;
        int     miny         = 0;
        for (Floor element : board.getFactoryFloor()) {
            if (firstElement) {
                maxx = element.getPosition().x();
                maxy = element.getPosition().y();
                minx = element.getPosition().x();
                miny = element.getPosition().y();
                firstElement = false;
            } else {
                if (element.getPosition().x() > maxx) {
                    maxx = element.getPosition().x();
                }
                if (element.getPosition().y() > maxy) {
                    maxy = element.getPosition().y();
                }
                if (element.getPosition().x() < minx) {
                    minx = element.getPosition().x();
                }
                if (element.getPosition().y() < miny) {
                    miny = element.getPosition().y();
                }
            }
        }
        for (BoardElement element : getObjectsAndRobots(board)) {
            if (element.isOnBoard()) {
                if (element.getPosition().x() > maxx) {
                    maxx = element.getPosition().x();
                }
                if (element.getPosition().y() > maxy) {
                    maxy = element.getPosition().y();
                }
                if (element.getPosition().x() < minx) {
                    minx = element.getPosition().x();
                }
                if (element.getPosition().y() < miny) {
                    miny = element.getPosition().y();
                }
            }
        }
        board.setRange(new Range(minx, miny, maxx, maxy));
    }

    public static void rotateBoard(Board board) {
        int maxy = board.getFactoryFloor().stream().max(Comparator.comparingInt((a) -> a.getPosition().y())).orElseThrow().getPosition().y();
        for (Floor floor : board.getFactoryFloor()) {
            rotateFloor(floor);
            Position oldPosition = floor.getPosition();
            floor.setPosition(new Position(maxy - floor.getPosition().y(), oldPosition.x()));
        }
        for (BoardElement be : getObjectsAndRobots(board)) {
            be.setDirection(be.getDirection().add(Direction.EAST));
            be.setPrevDirection(be.getDirection());
            Position oldPosition = be.getPosition();
            be.setPosition(new Position(maxy - be.getPosition().y(), oldPosition.x()));
            be.setPrevPosition(be.getPosition());
        }
        for (Checkpoint checkpoint : board.getCheckpoints()) {
            Position oldPosition = checkpoint.getPosition();
            checkpoint.setPosition(new Position(maxy - checkpoint.getPosition().y(), oldPosition.x()));
        }
    }

    public static void removeBoardFromGameBoard(Board board, int posx, int posy, ServerBoardArranger boardArranger) {
        if (!isBoardLaidOutOnPostion(board, posx, posy)) {
            return;
        }
        for (Floor o : new ArrayList<>(board.getFactoryFloor())) {
            if (o.getPosition().x() >= 12 * posx && o.getPosition().x() < 12 * (posx + 1) && o.getPosition().y() >= 12 * posy && o.getPosition().y() < 12 * (posy + 1)) {
                remove(board, o);
            }
        }
        for (BoardElement o : new ArrayList<>(getObjectsAndRobots(board))) {
            if (o.getPosition().x() >= 12 * posx && o.getPosition().x() < 12 * (posx + 1) && o.getPosition().y() >= 12 * posy && o.getPosition().y() < 12 * (posy + 1)) {
                remove(board, o);
            }
        }
        for (Checkpoint o : new ArrayList<>(board.getCheckpoints())) {
            if (o.getPosition().x() >= 12 * posx && o.getPosition().x() < 12 * (posx + 1) && o.getPosition().y() >= 12 * posy && o.getPosition().y() < 12 * (posy + 1)) {
                remove(board, o);
            }
        }
        boardArranger.adjustCheckpointNumbers(board.getCheckpoints());
        readjustBoardMinMaxValues(board);
    }

    public static boolean isBoardLaidOutOnPostion(Board board, int posx, int posy) {
        boolean erg = false;
        if (posx * 12 <= board.getRange().maxX()
                && posx * 12 >= board.getRange().minY()
                && posy * 12 <= board.getRange().maxY()
                && posy * 12 >= board.getRange().minY()) {
            for (int x = 0; x < 12 && !erg; x++) {
                for (int y = 0; y < 12 && !erg; y++) {
                    if (getFloor(board, new Position(posx * 12 + x, posy * 12 + y)).getFloortype() != Floortype.ABYSS) {
                        erg = true;
                    }
                }
            }
        }
        return erg;
    }

    public static void addBoardToGameBoard(Board board, Board boardToAdd, int posx, int posy, Game game, ServerBoardArranger boardArranger) {
        for (int x = boardToAdd.getRange().minX(); x <= boardToAdd.getRange().maxX(); x++) {
            for (int y = boardToAdd.getRange().minY(); y <= boardToAdd.getRange().maxY(); y++) {
                if (x % 12 == 0 && y % 12 == 0 && isBoardLaidOutOnPostion(board, x / 12, y / 12)) {
                    removeBoardFromGameBoard(board, posx + x / 12, posy + y / 12, boardArranger);
                }
            }
        }
        for (Floor be : boardToAdd.getFactoryFloor()) {
            be.setPosition(new Position(be.getPosition().x() + 12 * posx, be.getPosition().y() + 12 * posy));
            add(board, be);
        }
        for (BoardElement be : getObjectsAndRobots(boardToAdd)) {
            be.setPosition(new Position(be.getPosition().x() + 12 * posx, be.getPosition().y() + 12 * posy));
            be.setPrevPosition(be.getPosition());
            add(board, be);
        }
        readjustBoardMinMaxValues(board);
        game.notifyBoardMayHaveChanged(Step.SETUP, null, "Board was added");
    }

    public static List<BoardElementStub> createBoardElementStubList(Board board) {
        List<BoardElementStub> list = new ArrayList<>();
        for (Floor floor : board.getFactoryFloor()) {
            if (floor.isWater() && !floor.getFloortype().isConveyorBelt()) {
                // ignore those facing directions, it slows down the process
                // see Game.randomlySetFloorOrientationForDisplay()
                list.add(new BoardElementStub(floor.getStubListName(), floor.getPosition(), Direction.NORTH));
            } else {
                list.add(new BoardElementStub(floor.getStubListName(), floor.getPosition(), floor.getFacingDirection()));
            }
        }
        for (CircuitChaosObject cco : board.getObjects()) {
            if (cco.isOnBoard()) {
                list.add(new BoardElementStub(cco.getName() + cco.getType(), cco.getPosition(), cco.getDirection()));
            }
        }
        for (Robot robot : board.getRobots()) {
            if (robot.isOnBoard()) {
                list.add(new BoardElementStub(robot.getName(), robot.getPosition(), robot.getDirection()));
            }
        }
        for (Checkpoint checkpoint : board.getCheckpoints()) {
            list.add(new BoardElementStub(String.valueOf(checkpoint.getNumber()), null, null));
        }
        for (Robot robot : board.getRobotsFallingIntoAbyss()) {
            list.add(new BoardElementStub(robot.getName(), robot.getPosition(), robot.getDirection()));
        }
        for (CircuitChaosObject cco : board.getObjectsFallingIntoAbyss()) {
            list.add(new BoardElementStub(cco.getName(), cco.getPosition(), cco.getDirection()));
        }
        return list.stream()
                   .sorted(Comparator.comparing(BoardElementStub::name)
                                     .thenComparingInt(a -> a.position() == null ? 0 : a.position().x())
                                     .thenComparingInt(a -> a.position() == null ? 0 : a.position().y())
                                     .thenComparing(BoardElementStub::direction))
                   .toList();
    }

    public static void setBeam(Board board, Position position, Direction direction, Step step) {
        Floor floor = getFloor(board, position);
        switch (direction) {
            case NORTH, SOUTH -> floor.setBeamsNS(true);
            case EAST, WEST -> floor.setBeamsWE(true);
        }
        floor.setBeamType(switch (step) {
            case ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE -> EXCHANGE_BEAM;
            case ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE -> SPIN_LEFT_BEAM;
            case ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE -> SPIN_RIGHT_BEAM;
            case ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE, ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE_NO_WAIT,
                 BOARD_MOUNTED_PRESSURE_BEAMS_FIRE, BOARD_MOUNTED_PRESSURE_BEAMS_FIRE_NO_WAIT -> PRESSURE_BEAM;
            case ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE, ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE_NO_WAIT,
                 BOARD_MOUNTED_TRACTOR_BEAMS_FIRE, BOARD_MOUNTED_TRACTOR_BEAMS_FIRE_NO_WAIT -> TRACTOR_BEAM;
            default -> MAIN_LASER;
        });
    }

    public static void clearBeams(Board board) {
        for (Floor f : board.getFactoryFloor()) {
            f.setBeamsNS(false);
            f.setBeamsWE(false);
        }
    }

}
