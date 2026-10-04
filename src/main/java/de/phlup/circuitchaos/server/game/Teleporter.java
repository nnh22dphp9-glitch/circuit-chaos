package de.phlup.circuitchaos.server.game;

import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.model.CourseElement;
import de.phlup.circuitchaos.common.model.CourseObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Robot;
import de.phlup.circuitchaos.course.CourseHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Teleporter {

    private boolean hasBeenTeleported;
    private int     oldMovement;

    public static Teleporter teleport(CourseElement be, int movement, Game game, int phase) {
        return new Teleporter().teleportInternal(be, movement, game, phase);
    }

    private Teleporter teleportInternal(CourseElement be, int movement, Game game, int phase) {
        hasBeenTeleported = false;
        oldMovement = movement;
        for (CourseObject cco : CourseHandler.getObjects(game.getCourse(), be.getPosition())) {
            if (cco.getType() == ObjectType.TELEPORTER && movement != 0 && !be.isFlying()) {
                if (movement < 0) {
                    movement = 1 - movement;
                } else {
                    movement = 2 + movement;
                }
                Floor targetFloor = switch (be.getDirection()) {
                    case NORTH ->
                            CourseHandler.getFloor(game.getCourse(), new Position(be.getPosition().x(), be.getPosition().y() - movement));
                    case EAST ->
                            CourseHandler.getFloor(game.getCourse(), new Position(be.getPosition().x() + movement, be.getPosition().y()));
                    case SOUTH ->
                            CourseHandler.getFloor(game.getCourse(), new Position(be.getPosition().x(), be.getPosition().y() + movement));
                    case WEST ->
                            CourseHandler.getFloor(game.getCourse(), new Position(be.getPosition().x() - movement, be.getPosition().y()));
                };
                boolean willTeleport = be instanceof Robot r && r.isVirtual();
                if (willTeleport || !isTargetBlockedByObject(targetFloor, game)) {
                    willTeleport = true;
                    for (Robot r : CourseHandler.getRobots(game.getCourse(), targetFloor.getPosition())) {
                        willTeleport = willTeleport && r.isVirtual();
                    }
                }
                if (willTeleport) {
                    game.setPosition(be, targetFloor.getPosition().x(), targetFloor.getPosition().y(), Step.ROBOTS_AND_OBJECTS_MOVE, phase, null, false);
                    game.notifyCourseMayHaveChanged(Step.ROBOTS_AND_OBJECTS_MOVE, phase, "%s was teleported".formatted(be.getName()), null);
                    hasBeenTeleported = true;
                }
            }
        }
        return this;
    }

    private boolean isTargetBlockedByObject(Floor targetFloor, Game game) {
        for (CourseObject rro2 : CourseHandler.getObjects(game.getCourse(), targetFloor.getPosition())) {
            if (!rro2.getType().isFlat()) {
                return true;
            }
        }
        return false;
    }

}
