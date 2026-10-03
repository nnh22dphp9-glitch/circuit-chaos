package de.phlup.circuitchaos.gamelogic.utils;

import de.phlup.circuitchaos.enums.ModuleType;
import de.phlup.circuitchaos.enums.ObjectType;
import de.phlup.circuitchaos.enums.ProgramType;
import de.phlup.circuitchaos.enums.WallType;

import java.util.Map;

public interface PictureConstants {

    String PREFIX = "/gfx/";

    String FOLDER_ARROWS    = PREFIX + "arrows/";
    String FOLDER_BEAMS     = PREFIX + "beams/";
    String FOLDER_FLOOR     = PREFIX + "floor/";
    String FOLDER_OBJECTS   = PREFIX + "objects/";
    String FOLDER_MODULES   = PREFIX + "modules/";
    String FOLDER_PICTURES  = PREFIX + "pictures/";
    String FOLDER_PROGRAMME = PREFIX + "programme/";
    String FOLDER_ROBOTS    = PREFIX + "robots/";

    String FOLDER_FLOOR_CONVEYOR_BELTS = FOLDER_FLOOR + "conveyor-belts/";
    String FOLDER_FLOOR_ABYSS          = FOLDER_FLOOR + "abyss/";
    String FOLDER_FLOOR_WALLS          = FOLDER_FLOOR + "walls/";

    String GFX_START      = PREFIX + "start";
    String GFX_CHECKPOINT = PREFIX + "checkpoint";
    String GFX_FINISH     = PREFIX + "finish";

    String GFX_EXPLOSION = PREFIX + "explosion/explosion-";
    String GFX_PUSHER    = PREFIX + "pusher/pusher-";
    String GFX_WATER     = FOLDER_FLOOR + "water/water-";

    String GFX_MOVE_NORTH_ARROW   = FOLDER_ARROWS + "move-arrow-north";
    String GFX_MOVE_EAST_ARROW    = FOLDER_ARROWS + "move-arrow-east";
    String GFX_MOVE_SOUTH_ARROW   = FOLDER_ARROWS + "move-arrow-south";
    String GFX_MOVE_WEST_ARROW    = FOLDER_ARROWS + "move-arrow-west";
    String GFX_ROTATE_LEFT_ARROW  = FOLDER_ARROWS + "rotate-arrow-left";
    String GFX_ROTATE_RIGHT_ARROW = FOLDER_ARROWS + "rotate-arrow-right";

    String GFX_EXCHANGE_BEAM   = FOLDER_BEAMS + "exchange-beam";
    String GFX_LASER_BEAM_1    = FOLDER_BEAMS + "laser-beam-1";
    String GFX_LASER_BEAM_2    = FOLDER_BEAMS + "laser-beam-2";
    String GFX_LASER_BEAM_3    = FOLDER_BEAMS + "laser-beam-3";
    String GFX_PRESSURE_BEAM   = FOLDER_BEAMS + "pressure-beam";
    String GFX_SPIN_LEFT_BEAM  = FOLDER_BEAMS + "spin-left-beam";
    String GFX_SPIN_RIGHT_BEAM = FOLDER_BEAMS + "spin-right-beam";
    String GFX_TRACTOR_BEAM    = FOLDER_BEAMS + "tractor-beam";

    String GFX_OPEN_FLOOR   = FOLDER_FLOOR + "open-floor/open-floor-";
    String GFX_GEARS_SOCKET = FOLDER_FLOOR + "gears-socket";
    String GFX_GEARS_CCW    = FOLDER_FLOOR + "gears-ccw";
    String GFX_GEARS_CW     = FOLDER_FLOOR + "gears-cw";
    String GFX_LASER_1      = FOLDER_FLOOR + "laser-single";
    String GFX_LASER_2      = FOLDER_FLOOR + "laser-double";
    String GFX_LASER_3      = FOLDER_FLOOR + "laser-triple";
    String GFX_PIT_STOP     = FOLDER_FLOOR + "pit-stop";

    String GFX_ABYSS_BG  = FOLDER_FLOOR_ABYSS + "abyss-background";
    String GFX_ABYSS_E   = FOLDER_FLOOR_ABYSS + "abyss_e";
    String GFX_ABYSS_E_N = FOLDER_FLOOR_ABYSS + "abyss_e_n";
    String GFX_ABYSS_E_S = FOLDER_FLOOR_ABYSS + "abyss_e_s";
    String GFX_ABYSS_N   = FOLDER_FLOOR_ABYSS + "abyss_n";
    String GFX_ABYSS_N_E = FOLDER_FLOOR_ABYSS + "abyss_n_e";
    String GFX_ABYSS_N_W = FOLDER_FLOOR_ABYSS + "abyss_n_w";
    String GFX_ABYSS_NE  = FOLDER_FLOOR_ABYSS + "abyss_ne";
    String GFX_ABYSS_NW  = FOLDER_FLOOR_ABYSS + "abyss_nw";
    String GFX_ABYSS_S   = FOLDER_FLOOR_ABYSS + "abyss_s";
    String GFX_ABYSS_S_E = FOLDER_FLOOR_ABYSS + "abyss_s_e";
    String GFX_ABYSS_S_W = FOLDER_FLOOR_ABYSS + "abyss_s_w";
    String GFX_ABYSS_SE  = FOLDER_FLOOR_ABYSS + "abyss_se";
    String GFX_ABYSS_SW  = FOLDER_FLOOR_ABYSS + "abyss_sw";
    String GFX_ABYSS_W   = FOLDER_FLOOR_ABYSS + "abyss_w";
    String GFX_ABYSS_W_N = FOLDER_FLOOR_ABYSS + "abyss_w_n";
    String GFX_ABYSS_W_S = FOLDER_FLOOR_ABYSS + "abyss_w_s";

    String GFX_TRAPDOOR_1 = FOLDER_FLOOR_ABYSS + "trapdoor-1";
    String GFX_TRAPDOOR_2 = FOLDER_FLOOR_ABYSS + "trapdoor-2";
    String GFX_TRAPDOOR_3 = FOLDER_FLOOR_ABYSS + "trapdoor-3";
    String GFX_TRAPDOOR_4 = FOLDER_FLOOR_ABYSS + "trapdoor-4";
    String GFX_TRAPDOOR_5 = FOLDER_FLOOR_ABYSS + "trapdoor-5";
    String GFX_TRAPDOOR_6 = FOLDER_FLOOR_ABYSS + "trapdoor-6";

    String GFX_CONVEYOR_BELT_BACKGROUND         = FOLDER_FLOOR_CONVEYOR_BELTS + "background";
    String GFX_CONVEYOR_BELT_STRAIGHT           = FOLDER_FLOOR_CONVEYOR_BELTS + "straight";
    String GFX_CONVEYOR_BELT_CCW                = FOLDER_FLOOR_CONVEYOR_BELTS + "ccw";
    String GFX_CONVEYOR_BELT_CW                 = FOLDER_FLOOR_CONVEYOR_BELTS + "cw";
    String GFX_CONVEYOR_BELT_CW_CCW             = FOLDER_FLOOR_CONVEYOR_BELTS + "cw-ccw";
    String GFX_CONVEYOR_BELT_EXPRESS_BACKGROUND = FOLDER_FLOOR_CONVEYOR_BELTS + "express-background";
    String GFX_CONVEYOR_BELT_EXPRESS_STRAIGHT   = FOLDER_FLOOR_CONVEYOR_BELTS + "express-straight";
    String GFX_CONVEYOR_BELT_EXPRESS_CCW        = FOLDER_FLOOR_CONVEYOR_BELTS + "express-ccw";
    String GFX_CONVEYOR_BELT_EXPRESS_CW         = FOLDER_FLOOR_CONVEYOR_BELTS + "express-cw";
    String GFX_CONVEYOR_BELT_EXPRESS_CW_CCW     = FOLDER_FLOOR_CONVEYOR_BELTS + "express-cw-ccw";

    Map<WallType, String> GFX_WALL_TYPE = Map.of(
            WallType.NONE, "",
            WallType.SOLID, FOLDER_FLOOR_WALLS + "wall",
            WallType.ONE_WAY_GREEN, FOLDER_FLOOR_WALLS + "oneway-green",
            WallType.ONE_WAY_RED, FOLDER_FLOOR_WALLS + "oneway-red",
            WallType.REPULSOR_FIELD, FOLDER_FLOOR_WALLS + "repulsor",
            WallType.LEDGE, FOLDER_FLOOR_WALLS + "ledge",
            WallType.RAMP_UP, FOLDER_FLOOR_WALLS + "ramp-up",
            WallType.RAMP_DOWN, FOLDER_FLOOR_WALLS + "ramp-down"
    );

    static String getGfxOfObjectType(ObjectType objectType) {
        return FOLDER_OBJECTS + switch (objectType) {
            case GLUE -> "glue/glue-";
            case MINE -> "mine";
            case OIL -> "oil/oil-";
            case PORTAL_BLUE -> "portal/portal-blue";
            case PORTAL_PURPLE -> "portal/portal-purple";
            case PORTAL_RED -> "portal/portal-red";
            case PORTAL_YELLOW -> "portal/portal-yellow";
            case PORTAL_GREEN -> "portal/portal-green";
            case PROXIMITY_MINE -> "proximity-mine";
            case RANDOMIZER -> "randomizer";
            case TELEPORTER -> "teleporter";
        };
    }

    static String getGfxOfModuleType(ModuleType moduleType) {
        return FOLDER_MODULES + moduleType.getPicture();
    }

    Map<ProgramType, String> GFX_PROGRAMME = Map.of(
            ProgramType.MOVE_1, FOLDER_PROGRAMME + "move-1",
            ProgramType.MOVE_2, FOLDER_PROGRAMME + "move-2",
            ProgramType.MOVE_3, FOLDER_PROGRAMME + "move-3",
            ProgramType.MOVE_4, FOLDER_PROGRAMME + "move-4",
            ProgramType.BACKUP, FOLDER_PROGRAMME + "backup",
            ProgramType.BACKUP_2, FOLDER_PROGRAMME + "backup-2",
            ProgramType.ROTATE_LEFT, FOLDER_PROGRAMME + "rotate-left",
            ProgramType.ROTATE_RIGHT, FOLDER_PROGRAMME + "rotate-right",
            ProgramType.U_TURN, FOLDER_PROGRAMME + "u-turn"
    );

    String GFX_MODULE_BACKGROUND        = PREFIX + "module-background";
    String GFX_PROGRAMME_NOT_SET        = FOLDER_PROGRAMME + "not-set";
    String GFX_PROGRAMME_SLOT_SUFFIX    = "-slot";
    String GFX_PROGRAMME_BLOCKED_SUFFIX = "-blocked";

    String ROBOT_POSTFIX_VIRTUAL = "-virtual";
    String ROBOT_POSTFIX_FLYING  = "-flying";

    String GFX_LOGO       = FOLDER_PICTURES + "logo";
    String GFX_LOGO_SMALL = FOLDER_PICTURES + "logo-small";
    String GFX_PICTURE    = FOLDER_PICTURES + "picture";

}
