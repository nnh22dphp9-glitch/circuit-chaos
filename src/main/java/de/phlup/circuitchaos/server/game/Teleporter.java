package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.board.BoardHandler;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.BoardElement;
import de.phlup.circuitchaos.common.model.CircuitChaosObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Robot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Teleporter {

    private boolean hasBeenTeleported;
    private int     oldMovement;

    public static Teleporter teleport(BoardElement be, int movement, Game game, int phase) {
        return new Teleporter().teleportInternal(be, movement, game, phase);
    }

    private Teleporter teleportInternal(BoardElement be, int movement, Game game, int phase) {
        hasBeenTeleported = false;
        oldMovement = movement;
        for (CircuitChaosObject cco : BoardHandler.getObjects(game.getBoard(), be.getPosition())) {
            if (cco.getType() == ObjectType.TELEPORTER && movement != 0 && !be.isFlying()) {
                if (movement < 0) {
                    movement = 1 - movement;
                } else {
                    movement = 2 + movement;
                }
                Floor targetFloor = switch (be.getDirection()) {
                    case NORTH ->
                            BoardHandler.getFloor(game.getBoard(), new Position(be.getPosition().x(), be.getPosition().y() - movement));
                    case EAST ->
                            BoardHandler.getFloor(game.getBoard(), new Position(be.getPosition().x() + movement, be.getPosition().y()));
                    case SOUTH ->
                            BoardHandler.getFloor(game.getBoard(), new Position(be.getPosition().x(), be.getPosition().y() + movement));
                    case WEST ->
                            BoardHandler.getFloor(game.getBoard(), new Position(be.getPosition().x() - movement, be.getPosition().y()));
                };
                boolean willTeleport = be instanceof Robot r && r.isVirtual();
                if (willTeleport || !isTargetBlockedByObject(targetFloor, game)) {
                    willTeleport = true;
                    for (Robot r : BoardHandler.getRobots(game.getBoard(), targetFloor.getPosition())) {
                        willTeleport = willTeleport && r.isVirtual();
                    }
                }
                if (willTeleport) {
                    game.setPosition(be, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.ROBOTS_AND_OBJECTS_MOVE, phase, null, false);
                    game.notifyBoardMayHaveChanged(Step.ROBOTS_AND_OBJECTS_MOVE, phase, "%s was teleported".formatted(be.getName()), null);
                    hasBeenTeleported = true;
                }
            }
        }
        return this;
    }

    private boolean isTargetBlockedByObject(Floor targetFloor, Game game) {
        for (CircuitChaosObject rro2 : BoardHandler.getObjects(game.getBoard(), targetFloor.getPosition())) {
            if (!rro2.getType().isFlat()) {
                return true;
            }
        }
        return false;
    }

}
