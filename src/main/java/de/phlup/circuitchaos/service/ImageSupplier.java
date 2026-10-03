package de.phlup.circuitchaos.service;

import de.phlup.circuitchaos.enums.Direction;
import de.phlup.circuitchaos.enums.Floortype;
import de.phlup.circuitchaos.enums.ModuleType;
import de.phlup.circuitchaos.enums.ObjectType;
import de.phlup.circuitchaos.enums.RobotType;
import de.phlup.circuitchaos.enums.Step;
import de.phlup.circuitchaos.enums.WallType;
import de.phlup.circuitchaos.gamelogic.GameGui;
import de.phlup.circuitchaos.gamelogic.utils.BoardHandler;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.BoardElement;
import de.phlup.circuitchaos.model.Checkpoint;
import de.phlup.circuitchaos.model.CircuitChaosObject;
import de.phlup.circuitchaos.model.Floor;
import de.phlup.circuitchaos.model.Module;
import de.phlup.circuitchaos.model.Position;
import de.phlup.circuitchaos.model.Programme;
import de.phlup.circuitchaos.model.Robot;
import de.phlup.circuitchaos.settings.ClientSettings.Theme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.phlup.circuitchaos.enums.Direction.EAST;
import static de.phlup.circuitchaos.enums.Direction.NORTH;
import static de.phlup.circuitchaos.enums.Direction.SOUTH;
import static de.phlup.circuitchaos.enums.Direction.WEST;
import static de.phlup.circuitchaos.enums.ModuleType.MAIN_LASER;
import static de.phlup.circuitchaos.enums.ModuleType.PRESSURE_BEAM;
import static de.phlup.circuitchaos.enums.ModuleType.TRACTOR_BEAM;
import static de.phlup.circuitchaos.enums.Step.CONVEYOR_BELTS_MOVE;
import static de.phlup.circuitchaos.enums.Step.EXPRESS_CONVEYOR_BELTS_MOVE;
import static de.phlup.circuitchaos.enums.Step.GEARS_ROTATE;
import static de.phlup.circuitchaos.gamelogic.GlobalServerAttributes.RANDOM;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.FOLDER_ROBOTS;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_BG;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_E;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_E_N;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_E_S;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_N;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_NE;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_NW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_N_E;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_N_W;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_S;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_SE;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_SW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_S_E;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_S_W;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_W;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_W_N;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_ABYSS_W_S;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CHECKPOINT;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_BACKGROUND;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_CCW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_CW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_CW_CCW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_BACKGROUND;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_CCW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_CW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_CW_CCW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_STRAIGHT;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_CONVEYOR_BELT_STRAIGHT;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_EXCHANGE_BEAM;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_EXPLOSION;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_FINISH;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_GEARS_CCW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_GEARS_CW;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_GEARS_SOCKET;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_LASER_1;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_LASER_2;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_LASER_3;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_LASER_BEAM_1;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_LASER_BEAM_2;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_LASER_BEAM_3;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_OPEN_FLOOR;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_PIT_STOP;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_PRESSURE_BEAM;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_PROGRAMME;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_PROGRAMME_BLOCKED_SUFFIX;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_PROGRAMME_NOT_SET;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_PROGRAMME_SLOT_SUFFIX;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_PUSHER;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_SPIN_LEFT_BEAM;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_SPIN_RIGHT_BEAM;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_START;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_TRACTOR_BEAM;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_TRAPDOOR_1;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_TRAPDOOR_2;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_TRAPDOOR_3;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_TRAPDOOR_4;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_TRAPDOOR_5;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_TRAPDOOR_6;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_WALL_TYPE;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.GFX_WATER;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.ROBOT_POSTFIX_FLYING;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.ROBOT_POSTFIX_VIRTUAL;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.getGfxOfModuleType;
import static de.phlup.circuitchaos.gamelogic.utils.PictureConstants.getGfxOfObjectType;

@Slf4j
public class ImageSupplier {

    private final Map<String, ImageIcon> imageIcons = new HashMap<>();
    private final ResourceLoader         resourceLoader;
    private final Theme                  theme;
    private final int                    imageSize;
    private final ImageIcon              emptyImage;

    public ImageSupplier(Theme theme, ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.theme = theme;
        imageSize = theme.getImageSize();
        emptyImage = new ImageIcon(new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB));
    }

    public ImageIcon getImageIconPlain(String name) {
        if (name == null) {
            log.warn("getImageIcon(name) called with null");
            return emptyImage;
        }
        name = theme.getPath() + name;
        if (imageIcons.containsKey(name)) {
            return imageIcons.get(name);
        }
        Resource resource = resourceLoader.getResource(name + ".png");
        if (resource.exists() && resource.isReadable()) {
            try (InputStream imageStream = resource.getInputStream()) {
                ImageIcon imageIcon = new ImageIcon(ImageIO.read(imageStream));
                imageIcons.put(name, imageIcon);
                return imageIcon;
            } catch (IOException e) {
                log.error("IOException while loading image {}", name, e);
                imageIcons.put(name, emptyImage);
            }
        } else {
            log.error("File not found {}.png", name);
            imageIcons.put(name, emptyImage);
        }
        return emptyImage;
    }

    public ImageIcon getImageIcon(GameGui game, Floor floor) {
        BufferedImage i = getImagePlain(game, floor);
        return new ImageIcon(scaleImage(i, game.getZoomFactor() * theme.getZoomFactor()));
    }

    public ImageIcon getImageIconPlain(BoardElement boardElement) {
        if (boardElement instanceof Robot r) {
            return getImageIconPlain(getRobotImagePathAndName(r));
        } else if (boardElement instanceof CircuitChaosObject be) {
            return getImageIconPlain(getGfxOfObjectType(be.getType()));
        } else {
            return new ImageIcon();
        }
    }

    private BufferedImage getImagePlain(GameGui game, Floor f) {
        BufferedImage i = game.getImageByFloor(f);
        if (i == null) {
            i = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
            game.putImageOfFloor(f, i);
        }
        return i;
    }

    public ImageIcon getImageIconPlain(Image i) {
        if (i == null) {
            i = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        }
        return new ImageIcon(i);
    }

    public void redrawFloor(Floor floor, GameGui game, Step step, Integer phase, Integer subPhase, int animationSteps) {
        Robot activeRobot = null;
        if (game instanceof GameGui cg) {
            Optional<Robot> activeRobotOptional = game.getBoard().getRobots().stream()
                                                      .filter((r) -> r.getName().equals(cg.getActiveRobotName()))
                                                      .filter(BoardElement::isOnBoard)
                                                      .filter((r) -> r.getPosition().equals(floor.getPosition()))
                                                      .findFirst();
            if (activeRobotOptional.isPresent()) {
                activeRobot = activeRobotOptional.get();
            }
        }
        redrawFloor(floor.getPosition(), floor, game, activeRobot, step, phase, subPhase, animationSteps);
    }

    private void redrawFloor(Position position, Floor floor, GameGui game, Robot activeRobot, Step step, Integer phase, Integer subPhase, int animationSteps) {
        Board         board  = game.getBoard();
        BufferedImage image  = getImagePlain(game, floor);
        int           damage = floor.getExplosiveDamage();
        Graphics2D    gr     = image.createGraphics();
        try {
            drawBasicFloor(floor, game, step, phase, subPhase, board, gr, animationSteps);
            drawCheckpoint(position, board, gr);
            drawFlatObjects(position, subPhase, board, gr, animationSteps);
            if (subPhase != null) {
                drawFlatObjectsMoving(position, subPhase, board, gr, animationSteps);
                drawBeams(floor, step, subPhase, image, animationSteps);
            }
            drawNonFlatObjects(position, subPhase, board, gr, animationSteps);
            if (subPhase != null && subPhase < animationSteps) {
                drawNonFlatObjectsMoving(position, subPhase, board, gr, animationSteps);
            }
            drawRobots(position, subPhase, board, gr, animationSteps);
            if (subPhase != null) {
                drawFallingRobotsAndObjects(position, subPhase, board, gr, animationSteps);
            }
            if (subPhase == null || subPhase < animationSteps) {
                drawRobotsMovingAndActiveRobot(position, activeRobot, step, subPhase, board, gr, animationSteps);
                // TODO auch explosion malen, wenn ein Robotor getroffen wird oder stirbt
                drawExplosion(damage, gr, subPhase, animationSteps, getExplosionVariant(position, step));
            }
        } finally {
            gr.dispose();
        }
    }

    private int getExplosionVariant(Position position, Step step) {
        return (position.x() + position.y() + step.ordinal()) % theme.getExplosionVariants();
    }

    private void drawRobotsMovingAndActiveRobot(Position position, Robot activeRobot, Step step, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        for (Robot r : BoardHandler.getLeavingRobots(board, position)) {
            drawRobot(r, gr, subPhase, true, animationSteps);
        }
        if (activeRobot != null && step == Step.SETUP) {
            drawRobot(activeRobot, gr, subPhase, false, animationSteps);
        }
    }

    private void drawFallingRobotsAndObjects(Position position, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        for (BoardElement be : BoardHandler.getFallingIntoAbyss(board, position)) {
            drawFalling(be, gr, subPhase, false, animationSteps);
        }
        for (BoardElement be : BoardHandler.getLeavingFallingIntoAbyss(board, position)) {
            drawFalling(be, gr, subPhase, true, animationSteps);
        }
    }

    private void drawRobots(Position position, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        for (Robot r : BoardHandler.getRobots(board, position)) {
            drawRobot(r, gr, subPhase, false, animationSteps);
        }
    }

    private void drawNonFlatObjectsMoving(Position position, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        for (CircuitChaosObject cco : BoardHandler.getLeavingObjects(board, position)) {
            if (!cco.getType().isFlat()) {
                drawCircuitChaosObject(cco, gr, subPhase, true, animationSteps);
            }
        }
    }

    private void drawNonFlatObjects(Position position, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        for (CircuitChaosObject cco : BoardHandler.getObjects(board, position)) {
            if (!cco.getType().isFlat()) {
                drawCircuitChaosObject(cco, gr, subPhase, false, animationSteps);
            }
        }
    }

    private void drawBeams(Floor floor, Step step, Integer subPhase, BufferedImage image, int animationSteps) {
        if (subPhase < animationSteps) {
            if (step == Step.ROBOT_MOUNTED_LASER_FIRE
                    || step == Step.ROBOT_MOUNTED_PRESSURE_BEAMS_FIRE
                    || step == Step.ROBOT_MOUNTED_TRACTOR_BEAMS_FIRE
                    || step == Step.ROBOT_MOUNTED_SPIN_LEFT_BEAMS_FIRE
                    || step == Step.ROBOT_MOUNTED_SPIN_RIGHT_BEAMS_FIRE
                    || step == Step.ROBOT_MOUNTED_EXCHANGE_BEAMS_FIRE) {
                Graphics2D gra = image.createGraphics();
                try {
                    gra.translate(imageSize / 2, imageSize / 2);
                    drawBeams(floor.isBeamsNS() ? 1 : 0, gra, false, subPhase, animationSteps, floor.getBeamType());
                    drawBeams(floor.isBeamsWE() ? 1 : 0, gra, true, subPhase, animationSteps, floor.getBeamType());
                } finally {
                    gra.dispose();
                }
            }
        }
    }

    private void drawFlatObjectsMoving(Position position, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        for (CircuitChaosObject cco : BoardHandler.getLeavingObjects(board, position)) {
            if (cco.getType().isFlat()) {
                drawCircuitChaosObject(cco, gr, subPhase, true, animationSteps);
            }
        }
    }

    private void drawFlatObjects(Position position, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        for (CircuitChaosObject cco : BoardHandler.getObjects(board, position)) {
            if (cco.getType().isFlat()) {
                drawCircuitChaosObject(cco, gr, subPhase, false, animationSteps);
            }
        }
    }

    private void drawBasicFloor(Floor floor, GameGui game, Step step, Integer phase, Integer subPhase, Board board, Graphics2D gr, int animationSteps) {
        BufferedImage bi = getFloorImage(floor, board, game.isNotStartedYet(), step, phase, subPhase, animationSteps);
        gr.drawImage(bi, new AffineTransform(), null);
    }

    private void drawCheckpoint(Position position, Board board, Graphics2D gr) {
        Checkpoint cp = BoardHandler.getCheckpoint(board, position);
        if (cp != null) {
            int maxCheckpoint = board.getCheckpoints().size() - 1;
            drawCheckpoint(cp, gr, maxCheckpoint);
        }
    }

    private void drawRobot(Robot robot, Graphics2D gr, Integer subPhase, boolean leaving, int animationSteps) {
        String picture = getRobotImagePathAndName(robot);
        drawBoardElement(robot, gr, subPhase, leaving, picture, animationSteps);
    }

    private void drawFalling(BoardElement be, Graphics2D gr, int subPhase, boolean leaving, int animationSteps) {
        String picture = be instanceof Robot r ? getRobotImagePathAndName(r) :
                be instanceof CircuitChaosObject cco ? getGfxOfObjectType(cco.getType()) : null;
        if (picture != null) {
            if (subPhase < animationSteps) {
                drawBoardElement(be, gr, subPhase, leaving, picture, animationSteps);
            } else if (!leaving) {
                drawFalling(be, gr, subPhase - animationSteps, picture, animationSteps);
            }
        }
    }

    private void drawFalling(BoardElement be, Graphics2D gr, int subPhase, String picture, int animationSteps) {
        Image         image  = getImageIconPlain(picture).getImage();
        BufferedImage image2 = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr2    = image2.createGraphics();
        try {
            gr2.drawImage(image, AffineTransform.getQuadrantRotateInstance(be.getDirection().ordinal(), imageSize / 2.0, imageSize / 2.0), null);
        } finally {
            gr2.dispose();
        }
        double factor = 1 - subPhase / (double) animationSteps;
        double offset = imageSize / 2.0 - imageSize / 2.0 * factor;
        gr.translate(offset, offset);
        gr.drawImage(image2, AffineTransform.getScaleInstance(factor, factor), null);
        gr.translate(-offset, -offset);
    }

    private void drawBoardElement(BoardElement be, Graphics2D gr, Integer subPhase, boolean leaving, String picture, int animationSteps) {
        Image pictureImage = getImageIconPlain(picture).getImage();
        if (subPhase == null || subPhase >= animationSteps || be.getPosition().equals(be.getPrevPosition())) {
            if (!leaving) {
                AffineTransform affineTransform = AffineTransform.getQuadrantRotateInstance(be.getPrevDirection().ordinal(), imageSize / 2.0, imageSize / 2.0);
                affineTransform.rotate(calculateRotation(be, subPhase, animationSteps), imageSize / 2.0, imageSize / 2.0);
                gr.drawImage(pictureImage, affineTransform, null);
            }
        } else {
            if (!be.getPosition().isNeighbor(be.getPrevPosition())) {
                float alpha = subPhase / (animationSteps + 0.0f);
                if (leaving) {
                    alpha = 1 - alpha;
                }
                Composite oldComposite = gr.getComposite();
                gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                AffineTransform affineTransform = AffineTransform.getQuadrantRotateInstance(be.getPrevDirection().ordinal(), imageSize / 2.0, imageSize / 2.0);
                affineTransform.rotate(calculateRotation(be, subPhase, animationSteps), imageSize / 2.0, imageSize / 2.0);
                gr.drawImage(pictureImage, affineTransform, null);
                gr.setComposite(oldComposite);
            } else if (be.getDirection() == be.getPrevDirection()) {
                gr.translate(imageSize / 2, imageSize / 2);
                addMovingAnimation(pictureImage, gr, be.getDirection(), calculateMovingDirectionAccordingToBE(be), subPhase, leaving, !leaving, animationSteps);
                gr.translate(-imageSize / 2, -imageSize / 2);
            } else {
                AffineTransform affineTransform = AffineTransform.getRotateInstance(calculateRotation(be, subPhase, animationSteps), imageSize / 2.0, imageSize / 2.0);
                affineTransform.quadrantRotate(be.getPrevDirection().minus(be.getDirection()).ordinal(), imageSize / 2.0, imageSize / 2.0);
                BufferedImage bi2 = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D    gr2 = bi2.createGraphics();
                try {
                    gr2.drawImage(pictureImage, affineTransform, null);
                } finally {
                    gr2.dispose();
                }

                gr.translate(imageSize / 2, imageSize / 2);
                addMovingAnimation(bi2, gr, be.getDirection(), calculateMovingDirectionAccordingToBE(be), subPhase, leaving, !leaving, animationSteps);
                gr.translate(-imageSize / 2, -imageSize / 2);
            }
        }
    }

    private double calculateRotation(BoardElement be, Integer subPhase, int animationSteps) {
        if (subPhase == null) {
            return 0.0;
        }
        float target = switch (be.getPrevDirection().minus(be.getDirection())) {
            case SOUTH -> 1.0f;
            case EAST -> -0.5f;
            case WEST -> 0.5f;
            case NORTH -> 0.0f;
        };
        return target * subPhase * Math.PI / animationSteps;
    }

    private Direction calculateMovingDirectionAccordingToBE(BoardElement be) {
        if (be.getPosition().y() < be.getPrevPosition().y()) {
            return be.getDirection().add(NORTH);
        } else if (be.getPosition().y() > be.getPrevPosition().y()) {
            return be.getDirection().add(SOUTH);
        } else if (be.getPosition().x() < be.getPrevPosition().x()) {
            return be.getDirection().add(EAST);
        } else {
            return be.getDirection().add(WEST);
        }
    }

    public static String getRobotImagePathAndName(Robot robot) {
        String picture = "";
        for (RobotType rt : RobotType.values()) {
            if (rt.getRobotName().equals(robot.getName())) {
                picture = rt.getNormalizedName();
            }
        }
        if (robot.isFlying()) {
            picture = picture + ROBOT_POSTFIX_FLYING;
        }
        if (robot.isVirtual()) {
            picture = picture + ROBOT_POSTFIX_VIRTUAL;
        }
        return FOLDER_ROBOTS + picture;
    }

    private void drawCheckpoint(Checkpoint cp, Graphics2D gr, int maxCheckpoint) {
        String imageName;
        if (cp.getNumber() == 0) {
            imageName = GFX_START;
        } else if (cp.getNumber() >= maxCheckpoint) {
            imageName = GFX_FINISH;
        } else {
            imageName = GFX_CHECKPOINT;
        }
        Image i = getImageIconPlain(imageName).getImage();
        gr.drawImage(i, 0, 0, null);
    }

    private void drawCircuitChaosObject(CircuitChaosObject cco, Graphics2D gr, Integer subPhase, boolean leaving, int animationSteps) {
        String picture = getGfxOfObjectType(cco.getType());
        if (cco.getType() == ObjectType.GLUE) {
            picture = picture + ((int) (cco.getVariantSeed() * theme.getGlueVariants()));
        }
        if (cco.getType() == ObjectType.OIL) {
            picture = picture + ((int) (cco.getVariantSeed() * theme.getOilVariants()));
        }
        drawBoardElement(cco, gr, subPhase, leaving, picture, animationSteps);
    }

    private void drawBeams(int amount, Graphics2D gr, boolean westEastDirection, Integer subPhase, int animationSteps, ModuleType type) {
        if (amount <= 0) {
            return;
        }
        Composite oldComposite = gr.getComposite();
        if (subPhase != null) {
            gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, calculateAlpha(subPhase, 1.0f / animationSteps, animationSteps)));
        }
        addToImage(switch (type) {
            case EXCHANGE_BEAM -> GFX_EXCHANGE_BEAM;
            case PRESSURE_BEAM -> GFX_PRESSURE_BEAM;
            case TRACTOR_BEAM -> GFX_TRACTOR_BEAM;
            case SPIN_LEFT_BEAM -> GFX_SPIN_LEFT_BEAM;
            case SPIN_RIGHT_BEAM -> GFX_SPIN_RIGHT_BEAM;
            default -> switch (amount) {
                case 1 -> GFX_LASER_BEAM_1;
                case 2 -> GFX_LASER_BEAM_2;
                default -> GFX_LASER_BEAM_3;
            };
        }, westEastDirection ? EAST : NORTH, gr);
        gr.setComposite(oldComposite);
    }

    private void drawExplosion(int damage, Graphics2D gr, Integer subPhase, int animationSteps, int variant) {
        if (damage > 0) {
            Composite oldComposite = gr.getComposite();
            if (subPhase != null) {
                gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, calculateAlpha(subPhase, 1.0f / animationSteps, animationSteps)));
            }
            gr.drawImage(getImageIconPlain(GFX_EXPLOSION + variant).getImage(), 0, 0, null);
            gr.setComposite(oldComposite);
        }
    }

    private BufferedImage getFloorImage(Floor floor, Board board, boolean gameIsNotStartedYet,
                                        Step step, Integer phase, Integer subPhase, int animationSteps) {
        Direction     rotation = determineRotation(floor);
        BufferedImage bi       = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr       = bi.createGraphics();
        try {
            gr.translate(imageSize / 2, imageSize / 2);

            addGround(floor, rotation, gr, step, subPhase, animationSteps);
            addTrapdoor(floor, step, phase, subPhase, gr, rotation, animationSteps);
            modifyAbyssEdges(board, floor, gr);
            addPusher(floor, gr, gameIsNotStartedYet, step, phase, subPhase, animationSteps);
            if (step == Step.BOARD_MOUNTED_LASER_FIRE || gameIsNotStartedYet) {
                drawBeams(floor.getBoardMountedLaserBeamsNS(), gr, false, subPhase, animationSteps, MAIN_LASER);
                drawBeams(floor.getBoardMountedLaserBeamsWE(), gr, true, subPhase, animationSteps, MAIN_LASER);
            }
            if (step == Step.BOARD_MOUNTED_PRESSURE_BEAMS_FIRE || gameIsNotStartedYet) {
                if (floor.isBoardMountedPressureBeamsNS()) {
                    drawBeams(1, gr, false, subPhase, animationSteps, PRESSURE_BEAM);
                }
                if (floor.isBoardMountedPressureBeamsWE()) {
                    drawBeams(1, gr, true, subPhase, animationSteps, PRESSURE_BEAM);
                }
            }
            if (step == Step.BOARD_MOUNTED_TRACTOR_BEAMS_FIRE || gameIsNotStartedYet) {
                if (floor.isBoardMountedTractorBeamsNS()) {
                    drawBeams(1, gr, false, subPhase, animationSteps, TRACTOR_BEAM);
                }
                if (floor.isBoardMountedTractorBeamsWE()) {
                    drawBeams(1, gr, true, subPhase, animationSteps, TRACTOR_BEAM);
                }
            }
            addWalls(floor, gr);
            addLasers(floor, gr);

            for (int i = 0; i > floor.getLevel(); i--) {
                lowerLight(gr);
            }
        } finally {
            gr.dispose();
        }

        return bi;
    }

    private void addTrapdoor(Floor floor, Step step, Integer phase, Integer subPhase, Graphics2D gr, Direction rotation, int animationSteps) {
        if (floor.getFloortype() == Floortype.TRAPDOOR && subPhase != null) {
            if (phase != null && step == Step.OPEN_TRAPDOORS) {
                boolean currentOpen  = floor.getActiveInPhase()[phase];
                boolean previousOpen = phase == 0 ? floor.getActiveInPhase()[4] : floor.getActiveInPhase()[phase - 1];
                String  image        = getTrapdoorAnimatedImage(subPhase, currentOpen, previousOpen, animationSteps);
                addToImage(image, rotation, gr);
            } else {
                addToImage(phase != null && floor.getActiveInPhase()[phase] ? GFX_TRAPDOOR_6 : GFX_TRAPDOOR_1, rotation, gr);
            }
        }
    }

    private String getTrapdoorAnimatedImage(Integer subPhase, boolean currentOpen, boolean previousOpen, int animationSteps) {
        String image;
        if (currentOpen && previousOpen) { // keep open
            image = GFX_TRAPDOOR_6;
        } else if (!currentOpen && !previousOpen) { // keep closed
            image = GFX_TRAPDOOR_1;
        } else {
            float alpha = subPhase / (float) animationSteps;
            if (currentOpen) { // open
                if (alpha < 1 / 6.0) {
                    image = GFX_TRAPDOOR_1;
                } else if (alpha < 2 / 6.0) {
                    image = GFX_TRAPDOOR_2;
                } else if (alpha < 3 / 6.0) {
                    image = GFX_TRAPDOOR_3;
                } else if (alpha < 4 / 6.0) {
                    image = GFX_TRAPDOOR_4;
                } else if (alpha < 5 / 6.0) {
                    image = GFX_TRAPDOOR_5;
                } else {
                    image = GFX_TRAPDOOR_6;
                }
            } else { // close
                if (alpha < 1 / 6.0) {
                    image = GFX_TRAPDOOR_6;
                } else if (alpha < 2 / 6.0) {
                    image = GFX_TRAPDOOR_5;
                } else if (alpha < 3 / 6.0) {
                    image = GFX_TRAPDOOR_4;
                } else if (alpha < 4 / 6.0) {
                    image = GFX_TRAPDOOR_3;
                } else if (alpha < 5 / 6.0) {
                    image = GFX_TRAPDOOR_2;
                } else {
                    image = GFX_TRAPDOOR_1;
                }
            }
        }
        return image;
    }

    private void addGround(Floor floor, Direction rotation, Graphics2D gr, Step step, Integer subPhase, int animationSteps) {
        String picture = getGroundElementPicture(floor);
        addToImage(picture, rotation, gr);
        addAnimationToImage(floor.getFloortype(), gr, step, subPhase == null ? 1 : subPhase, rotation, animationSteps);
        if (floor.isWater()) {
            picture = GFX_WATER + RANDOM.nextInt(theme.getWaterVariants());
            addToImage(picture, NORTH, gr);
        }
    }

    private void addLasers(Floor floor, Graphics2D gr) {
        for (int counter = 0; counter < 4; counter++) {
            int laserCount = floor.getLasers()[counter];
            if (laserCount > 0) {
                addToImage(laserCount == 1 ? GFX_LASER_1 : laserCount == 2 ? GFX_LASER_2 : GFX_LASER_3, Direction.values()[counter], gr);
            }
        }
    }

    private void lowerLight(Graphics2D gr) {
        Composite oldComposite = gr.getComposite();
        gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        addToImage(GFX_ABYSS_BG, NORTH, gr);
        gr.setComposite(oldComposite);
    }

    private void addPusher(Floor floor, Graphics2D gr, boolean gameIsNotStartedYet, Step step, Integer phase, Integer subPhase, int animationSteps) {
        if (floor.isHasPusher()) {
            if (gameIsNotStartedYet) {
                addToImage(GFX_PUSHER + (theme.getPusherAnimationSteps() / 2), floor.getPusherDirection(), gr);
            } else if (phase == null || subPhase == null || !floor.getActiveInPhase()[phase] || step != Step.PUSHERS_PUSH) {
                addToImage(GFX_PUSHER + 0, floor.getPusherDirection(), gr);
            } else {
                double alpha       = calculateAlpha(subPhase, 0, animationSteps) / 2;
                int    imageNummer = (int) (alpha * theme.getPusherAnimationSteps());
                addToImage(GFX_PUSHER + imageNummer, floor.getPusherDirection(), gr);
            }
        }
    }

    private void addWalls(Floor floor, Graphics2D gr) {
        if (floor.getWallNorth() != WallType.NONE) {
            addSingleWall(floor.getWallNorth(), NORTH, gr);
        }
        if (floor.getWallEast() != WallType.NONE) {
            addSingleWall(floor.getWallEast(), EAST, gr);
        }
        if (floor.getWallSouth() != WallType.NONE) {
            addSingleWall(floor.getWallSouth(), SOUTH, gr);
        }
        if (floor.getWallWest() != WallType.NONE) {
            addSingleWall(floor.getWallWest(), WEST, gr);
        }
    }

    private void addSingleWall(WallType floor, Direction north, Graphics2D gr) {
        String suffix = "";
        if (floor == WallType.REPULSOR_FIELD) {
            suffix = "-" + RANDOM.nextInt(theme.getRepulsorAnimationSteps());
        }
        addToImage(GFX_WALL_TYPE.get(floor) + suffix, north, gr);
    }

    private void modifyAbyssEdges(Board board, Floor floor, Graphics2D gr) {
        Floortype floortype = floor.getFloortype();
        if (floortype == Floortype.ABYSS) {
            Floor floorWest  = BoardHandler.getFloor(board, floor.getPosition().neighbour(WEST));
            Floor floorSouth = BoardHandler.getFloor(board, floor.getPosition().neighbour(SOUTH));
            Floor floorEast  = BoardHandler.getFloor(board, floor.getPosition().neighbour(EAST));
            Floor floorNorth = BoardHandler.getFloor(board, floor.getPosition().neighbour(NORTH));
            addPlatformEdgesOnAbyss(board, gr, floorNorth, floorWest, floorEast, floorSouth);
        }
    }

    private void addPlatformEdgesOnAbyss(Board board, Graphics2D gr, Floor floorNorth, Floor floorWest, Floor floorEast, Floor floorSouth) {
        if (floorNorth.getFloortype() != Floortype.ABYSS || floorNorth.isWater()) {
            addToImage(GFX_ABYSS_N, NORTH, gr);
            if (floorWest.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_N_W, NORTH, gr);
            }
            if (floorEast.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_N_E, NORTH, gr);
            }
        }
        if (floorEast.getFloortype() != Floortype.ABYSS || floorEast.isWater()) {
            addToImage(GFX_ABYSS_E, NORTH, gr);
            if (floorNorth.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_E_N, NORTH, gr);
            }
            if (floorSouth.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_E_S, NORTH, gr);
            }
        }
        if (floorSouth.getFloortype() != Floortype.ABYSS || floorSouth.isWater()) {
            addToImage(GFX_ABYSS_S, NORTH, gr);
            if (floorWest.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_S_W, NORTH, gr);
            }
            if (floorEast.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_S_E, NORTH, gr);
            }
        }
        if (floorWest.getFloortype() != Floortype.ABYSS || floorWest.isWater()) {
            addToImage(GFX_ABYSS_W, NORTH, gr);
            if (floorNorth.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_W_N, NORTH, gr);
            }
            if (floorSouth.getFloortype() == Floortype.ABYSS) {
                addToImage(GFX_ABYSS_W_S, NORTH, gr);
            }
        }
        if (floorNorth.getFloortype() == Floortype.ABYSS && floorEast.getFloortype() == Floortype.ABYSS
                && BoardHandler.getFloor(board, floorNorth.getPosition().neighbour(EAST)).getFloortype() != Floortype.ABYSS) {
            addToImage(GFX_ABYSS_NE, NORTH, gr);
        }
        if (floorNorth.getFloortype() == Floortype.ABYSS && floorWest.getFloortype() == Floortype.ABYSS
                && BoardHandler.getFloor(board, floorNorth.getPosition().neighbour(WEST)).getFloortype() != Floortype.ABYSS) {
            addToImage(GFX_ABYSS_NW, NORTH, gr);
        }
        if (floorSouth.getFloortype() == Floortype.ABYSS && floorEast.getFloortype() == Floortype.ABYSS
                && BoardHandler.getFloor(board, floorSouth.getPosition().neighbour(EAST)).getFloortype() != Floortype.ABYSS) {
            addToImage(GFX_ABYSS_SE, NORTH, gr);
        }
        if (floorSouth.getFloortype() == Floortype.ABYSS && floorWest.getFloortype() == Floortype.ABYSS
                && BoardHandler.getFloor(board, floorSouth.getPosition().neighbour(WEST)).getFloortype() != Floortype.ABYSS) {
            addToImage(GFX_ABYSS_SW, NORTH, gr);
        }
    }

    private Direction determineRotation(Floor floor) {
        Floortype floortype = floor.getFloortype();
        Direction rotation  = NORTH;
        if (floortype.isConveyorBelt() ||
                floortype == Floortype.OPEN_FLOOR ||
                floortype == Floortype.ABYSS ||
                floortype == Floortype.TRAPDOOR) {
            rotation = floor.getFacingDirection();
        }
        return rotation;
    }

    private void addAnimationToImage(Floortype floortype, Graphics2D gr, Step step, Integer subPhase, Direction rotation, int animationSteps) {
        if (step == GEARS_ROTATE && floortype == Floortype.GEARS_CCW) {
            Image i = getImageIconPlain(GFX_GEARS_CCW).getImage();
            gr.translate(-imageSize / 2, -imageSize / 2);
            gr.drawImage(i, AffineTransform.getRotateInstance(-(2.0f * subPhase * Math.PI / animationSteps), imageSize / 2.0, imageSize / 2.0), null);
            gr.translate(imageSize / 2, imageSize / 2);
        } else if (step == GEARS_ROTATE && floortype == Floortype.GEARS_CW) {
            Image i = getImageIconPlain(GFX_GEARS_CW).getImage();
            gr.translate(-imageSize / 2, -imageSize / 2);
            gr.drawImage(i, AffineTransform.getRotateInstance(2.0f * subPhase * Math.PI / animationSteps, imageSize / 2.0, imageSize / 2.0), null);
            gr.translate(imageSize / 2, imageSize / 2);
        }
        if (floortype.isSlowConveyorBelt() && step == CONVEYOR_BELTS_MOVE) {
            addMovingAnimation(getImageIconPlain(getMovingPicture(floortype)).getImage(), gr, rotation, NORTH, subPhase, true, true, animationSteps);
        }
        if (floortype.isExpressConveyorBelt() && (step == CONVEYOR_BELTS_MOVE || step == EXPRESS_CONVEYOR_BELTS_MOVE)) {
            addMovingAnimation(getImageIconPlain(getMovingPicture(floortype)).getImage(), gr, rotation, NORTH, subPhase, true, true, animationSteps);
        }
        // Special: add not-moving pictures on gears and conveyor belts, if they don't move
        if (floortype.isSlowConveyorBelt() && step != CONVEYOR_BELTS_MOVE) {
            addToImage(getMovingPicture(floortype), rotation, gr);
        }
        if (floortype.isExpressConveyorBelt() && step != CONVEYOR_BELTS_MOVE && step != EXPRESS_CONVEYOR_BELTS_MOVE) {
            addToImage(getMovingPicture(floortype), rotation, gr);
        }
        if (floortype == Floortype.GEARS_CCW && step != GEARS_ROTATE) {
            addToImage(GFX_GEARS_CCW, rotation, gr);
        }
        if (floortype == Floortype.GEARS_CW && step != GEARS_ROTATE) {
            addToImage(GFX_GEARS_CW, rotation, gr);
        }
    }

    private void addMovingAnimation(Image gfxMoving, Graphics2D gr, Direction rotation, Direction movingDirection, Integer subPhase, boolean outgoing, boolean incoming, int animationSteps) {
        BufferedImage bi2    = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr2    = (Graphics2D) bi2.getGraphics();
        double        amount = -(subPhase * (double) imageSize / animationSteps);
        if (outgoing) {
            AffineTransform at = switch (movingDirection) {
                case NORTH -> AffineTransform.getTranslateInstance(0.0, amount);
                case SOUTH -> AffineTransform.getTranslateInstance(0.0, -amount);
                case EAST -> AffineTransform.getTranslateInstance(amount, 0.0);
                case WEST -> AffineTransform.getTranslateInstance(-amount, 0.0);
            };
            gr2.drawImage(gfxMoving, at, null);
        }
        if (incoming) {
            amount = amount + imageSize;
            AffineTransform at = switch (movingDirection) {
                case NORTH -> AffineTransform.getTranslateInstance(0.0, amount);
                case SOUTH -> AffineTransform.getTranslateInstance(0.0, -amount);
                case EAST -> AffineTransform.getTranslateInstance(amount, 0.0);
                case WEST -> AffineTransform.getTranslateInstance(-amount, 0.0);
            };
            gr2.drawImage(gfxMoving, at, null);
        }
        gr.translate(-imageSize / 2, -imageSize / 2);
        gr.drawImage(bi2, AffineTransform.getQuadrantRotateInstance(rotation.ordinal(), imageSize / 2.0, imageSize / 2.0), null);
        gr.translate(imageSize / 2, imageSize / 2);
    }

    private String getGroundElementPicture(Floor floor) {
        return switch (floor.getFloortype()) {
            case Floortype.OPEN_FLOOR -> getOpenFloorImageName(floor.getPosition());
            case Floortype.PIT_STOP -> GFX_PIT_STOP;
            case Floortype.ABYSS -> GFX_ABYSS_BG;
            case Floortype.TRAPDOOR -> GFX_TRAPDOOR_3;
            case Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CCW, Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW,
                 Floortype.TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW, Floortype.EXPRESS_CONVEYOR_BELT ->
                    GFX_CONVEYOR_BELT_EXPRESS_BACKGROUND;
            case Floortype.TURNING_CONVEYOR_BELT_CCW, Floortype.TURNING_CONVEYOR_BELT_CW,
                 Floortype.TURNING_CONVEYOR_BELT_CW_CCW, Floortype.CONVEYOR_BELT -> GFX_CONVEYOR_BELT_BACKGROUND;
            case Floortype.GEARS_CCW, Floortype.GEARS_CW -> GFX_GEARS_SOCKET;
        };
    }

    private String getOpenFloorImageName(Position position) {
        return GFX_OPEN_FLOOR + ((position.x() + position.y() * 6) % theme.getOpenFloorVariants());
    }

    private void addToImage(String picture, Direction rotation, Graphics2D gr) {
        if (StringUtils.hasText(picture)) {
            Image i = getImageIconPlain(picture).getImage();
            gr.translate(-imageSize / 2, -imageSize / 2);
            gr.drawImage(i, AffineTransform.getQuadrantRotateInstance(rotation.ordinal(), imageSize / 2.0, imageSize / 2.0), null);
            gr.translate(imageSize / 2, imageSize / 2);
        }
    }

    public ImageIcon getImageIcon(Module module, double scale) {
        String imagename = getGfxOfModuleType(module.getType());
        if (scale == 1.0) {
            return getImageIconPlain(imagename);
        }
        Image         fi = getImageIconPlain(imagename).getImage();
        BufferedImage bi = new BufferedImage((int) (fi.getWidth(null) * scale), (int) (fi.getHeight(null) * scale), BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr = bi.createGraphics();
        try {
            gr.drawImage(fi, AffineTransform.getScaleInstance(scale, scale), null);
        } finally {
            gr.dispose();
        }
        return getImageIconPlain(bi);
    }

    public ImageIcon getImageIcon(Programme pc, boolean slot, boolean blocked) {
        Image         front_i = getImageIconPlain(GFX_PROGRAMME.get(pc.getType()) + (slot ? (blocked ? GFX_PROGRAMME_BLOCKED_SUFFIX : GFX_PROGRAMME_SLOT_SUFFIX) : "")).getImage();
        BufferedImage bi      = new BufferedImage(front_i.getWidth(null), front_i.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr      = bi.createGraphics();
        try {
            gr.drawImage(front_i, 0, 0, null);
        } finally {
            gr.dispose();
        }
        return getImageIconPlain(bi);
    }

    public ImageIcon getProgrammeNotSetIcon(boolean slot) {
        Image         front_i = getImageIconPlain(GFX_PROGRAMME_NOT_SET + (slot ? GFX_PROGRAMME_SLOT_SUFFIX : "")).getImage();
        BufferedImage bi      = new BufferedImage(front_i.getWidth(null), front_i.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr      = bi.createGraphics();
        try {
            gr.drawImage(front_i, 0, 0, null);
        } finally {
            gr.dispose();
        }
        return getImageIconPlain(bi);
    }

    private float calculateAlpha(Integer subPhase, float offset, int animationSteps) {
        if (subPhase == null) {
            return 1;
        }
        float middle = (animationSteps + 1) / 2.0f - 1;
        if (middle == 0) {
            return 1;
        }
        float differenceToMiddle = middle > subPhase ? middle - subPhase : subPhase - middle;
        float factor             = 2 * (middle - differenceToMiddle) / middle;
        float alpha              = offset + (1 - offset) * factor;
        return Math.min(alpha, 1);
    }

    private BufferedImage scaleImage(BufferedImage original, double zoomFactor) {
        if (zoomFactor == 1f) {
            return original;
        }
        int           targetWidth  = (int) (original.getWidth() * zoomFactor);
        int           targetHeight = (int) (original.getHeight() * zoomFactor);
        BufferedImage scaled       = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g2d          = scaled.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }
        return scaled;
    }

    private String getMovingPicture(Floortype floor) {
        return switch (floor) {
            case CONVEYOR_BELT -> GFX_CONVEYOR_BELT_STRAIGHT;
            case TURNING_CONVEYOR_BELT_CCW -> GFX_CONVEYOR_BELT_CCW;
            case TURNING_CONVEYOR_BELT_CW -> GFX_CONVEYOR_BELT_CW;
            case TURNING_CONVEYOR_BELT_CW_CCW -> GFX_CONVEYOR_BELT_CW_CCW;
            case EXPRESS_CONVEYOR_BELT -> GFX_CONVEYOR_BELT_EXPRESS_STRAIGHT;
            case TURNING_EXPRESS_CONVEYOR_BELT_CCW -> GFX_CONVEYOR_BELT_EXPRESS_CCW;
            case TURNING_EXPRESS_CONVEYOR_BELT_CW -> GFX_CONVEYOR_BELT_EXPRESS_CW;
            case TURNING_EXPRESS_CONVEYOR_BELT_CW_CCW -> GFX_CONVEYOR_BELT_EXPRESS_CW_CCW;
            default -> "";
        };
    }

}
