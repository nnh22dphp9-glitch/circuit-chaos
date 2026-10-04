package de.phlup.circuitchaos.gamelogic.utils;

import de.phlup.circuitchaos.enums.Direction;
import de.phlup.circuitchaos.enums.Floortype;
import de.phlup.circuitchaos.enums.ObjectType;
import de.phlup.circuitchaos.enums.Step;
import de.phlup.circuitchaos.gamelogic.Game;
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

    public static void replaceBoardOnGameBoard(Board board, Board gameBoard) {
        gameBoard.getFactoryFloor().clear();
        gameBoard.getObjects().forEach(be -> be.setOnBoard(false));
        gameBoard.getObjects().clear();
        gameBoard.getCheckpoints().clear();
        gameBoard.getFactoryFloor().addAll(board.getFactoryFloor());
        gameBoard.getCheckpoints().addAll(board.getCheckpoints());
        gameBoard.getObjects().addAll(board.getObjects());
        gameBoard.getObjects().forEach(be -> be.setOnBoard(true));
        gameBoard.setRange(board.getRange());
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
            case ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE, BOARD_MOUNTED_PRESSURE_BEAMS_FIRE -> PRESSURE_BEAM;
            case ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE, BOARD_MOUNTED_TRACTOR_BEAMS_FIRE -> TRACTOR_BEAM;
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
