package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.CourseElement;
import de.phlup.circuitchaos.common.model.CourseObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Teleporter {

    private boolean hasBeenTeleported;
    private int     oldMovement;

    public static Teleporter teleport(CourseElement ce, int movement, Game game, int phase) {
        return new Teleporter().teleportInternal(ce, movement, game, phase);
    }

    private Teleporter teleportInternal(CourseElement ce, int movement, Game game, int phase) {
        hasBeenTeleported = false;
        oldMovement = movement;
        for (CourseObject co : CourseHandler.getObjects(game.getCourse(), ce.getPosition())) {
            if (co.getType() == ObjectType.TELEPORTER && movement != 0 && !ce.isFlying()) {
                if (movement < 0) {
                    movement = 1 - movement;
                } else {
                    movement = 2 + movement;
                }
                Floor targetFloor = CourseHandler.getFloor(game.getCourse(), switch (ce.getDirection()) {
                    case NORTH -> new Position(ce.getPosition().x(), ce.getPosition().y() - movement);
                    case EAST -> new Position(ce.getPosition().x() + movement, ce.getPosition().y());
                    case SOUTH -> new Position(ce.getPosition().x(), ce.getPosition().y() + movement);
                    case WEST -> new Position(ce.getPosition().x() - movement, ce.getPosition().y());
                });
                if (CourseHandler.getObjectsAndRobots(game.getCourse(), targetFloor.getPosition()).isEmpty()) {
                    game.setPosition(ce, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.ROBOTS_AND_OBJECTS_MOVE, phase, null, false);
                    game.notifyCourseMayHaveChanged(Step.ROBOTS_AND_OBJECTS_MOVE, phase, "%s was teleported".formatted(ce.getName()), null);
                    hasBeenTeleported = true;
                }
            }
        }
        return this;
    }

}
