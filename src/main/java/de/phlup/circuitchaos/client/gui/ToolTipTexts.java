package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Module;
import de.phlup.circuitchaos.common.model.Robot;

import java.util.List;

public class ToolTipTexts {

    private static final String INCREMENT = "&nbsp;&nbsp;&nbsp;&nbsp;";
    private static final String NEW_LINE  = "<br>";

    public static String getToolTipText(Course course, Floor floor) {
        if (floor == null) {
            return "";
        }
        StringBuilder text = new StringBuilder("<html><font face=\"sans-serif\"><strong>");
        switch (floor.getFloortype()) {
            case Floortype.OPEN_FLOOR -> text.append("Open floor");
            case Floortype.ABYSS -> text.append("Abyss");
            case Floortype.TRAPDOOR -> text.append("Trapdoor");
            case Floortype.PIT_STOP -> text.append("Pit Stop");
            case Floortype.EXPRESS_CONVEYOR_BELT -> text.append("Express Conveyor Belt");
            case Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW -> text.append("Turning Express Conveyor Belt (ccw)");
            case Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW -> text.append("Turning Express Conveyor Belt (cw)");
            case Floortype.TURNING_CONVEYOR_BELT_CW_CCW -> text.append("Turning Express Conveyor Belt");
            case Floortype.CONVEYOR_BELT -> text.append("Conveyor Belt");
            case Floortype.TURNING_CONVEYOR_BELT_CCW -> text.append("Turning Conveyor Belt (ccw)");
            case Floortype.TURNING_CONVEYOR_BELT_CW -> text.append("Turning Conveyor Belt (cw)");
            case Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW -> text.append("Turning Conveyor Belt");
            case Floortype.GEARS_CW -> text.append("Gears (cw)");
            case Floortype.GEARS_CCW -> text.append("Gears (ccw)");
        }
        if (floor.isWater()) {
            text.append(" under water");
        }
        text.append("</strong>")
            .append(NEW_LINE);
        if (floor.getFloortype().isConveyorBelt()) {
            text.append("facing ")
                .append(floor.getFacingDirection().name())
                .append(NEW_LINE);
        }
        if (floor.isHasPusher()) {
            text.append("Pusher facing ")
                .append(floor.getPusherDirection().name())
                .append(NEW_LINE);
        }
        if (floor.getActiveInPhase()[0] || floor.getActiveInPhase()[1] || floor.getActiveInPhase()[2] || floor.getActiveInPhase()[3] || floor.getActiveInPhase()[4]) {
            text.append("Active Phases: ")
                .append(floor.getActiveInPhase()[0] ? " 1 " : "")
                .append(floor.getActiveInPhase()[1] ? " 2 " : "")
                .append(floor.getActiveInPhase()[2] ? " 3 " : "")
                .append(floor.getActiveInPhase()[3] ? " 4 " : "")
                .append(floor.getActiveInPhase()[4] ? " 5 " : "")
                .append(NEW_LINE);
        }
        for (CourseObject co : CourseHandler.getObjects(course, floor.getPosition())) {
            text.append(INCREMENT).append(NEW_LINE)
                .append("<strong>").append(co.getType().getName()).append("</strong>").append(NEW_LINE);
        }
        List<Robot> robots = CourseHandler.getRobots(course, floor.getPosition());
        if (!robots.isEmpty()) {
            for (Robot robot : robots) {
                text.append(getToolTipText(robot));
            }
        }
        text.append("</font></html>");
        return text.toString();
    }

    private static String getToolTipText(Robot r) {
        StringBuilder ttt = new StringBuilder()
                .append(INCREMENT).append(NEW_LINE)
                .append("<strong>").append(r.getName()).append("</strong>, Damage: ")
                .append(r.getDamage()).append(", next CP: ").append(r.getNextCheckpoint());
        if (r.isVirtual()) {
            ttt.append(" - VIRTUAL");
        }
        if (r.isPoweredDown()) {
            ttt.append(" - POWERED DOWN");
        }
        if (r.isPowerDownAnnounced()) {
            ttt.append(" - will power down");
        }
        ttt.append(NEW_LINE);
        for (Module module : r.getModules()) {
            ttt.append(getToolTipText(module));
        }
        return ttt.toString();
    }

    private static String getToolTipText(Module module) {
        StringBuilder ttt = new StringBuilder()
                .append(INCREMENT).append(INCREMENT).append("<strong>").append(module.getType().getModuleName()).append("</strong>");
        if (module.getType().getAmmunition() > 0) {
            ttt.append(", Ammunition: ").append(module.getAmmunition()).append(" / ").append(module.getType().getAmmunition());
        }
        if (!module.getActiveInPhase()[0] || !module.getActiveInPhase()[1] || !module.getActiveInPhase()[2] || !module.getActiveInPhase()[3] || !module.getActiveInPhase()[4]) {
            if (module.getActiveInPhase()[0] || module.getActiveInPhase()[1] || module.getActiveInPhase()[2] || module.getActiveInPhase()[3] || module.getActiveInPhase()[4]) {
                ttt.append(", Active Phases: ")
                   .append(module.getActiveInPhase()[0] ? " 1 " : "")
                   .append(module.getActiveInPhase()[1] ? " 2 " : "")
                   .append(module.getActiveInPhase()[2] ? " 3 " : "")
                   .append(module.getActiveInPhase()[3] ? " 4 " : "")
                   .append(module.getActiveInPhase()[4] ? " 5 " : "");
            }
        }
        if (module.getDirection() != null) {
            ttt.append(", Direction: ").append(switch (module.getDirection()) {
                case Direction.NORTH -> "front";
                case Direction.EAST -> "right";
                case Direction.SOUTH -> "back";
                case Direction.WEST -> "left";
            });
        }
        return ttt.append(NEW_LINE).toString();
    }

}
