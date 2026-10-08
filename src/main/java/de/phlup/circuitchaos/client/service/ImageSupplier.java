package de.phlup.circuitchaos.client.service;

import de.phlup.circuitchaos.client.gui.BaseGui;
import de.phlup.circuitchaos.client.gui.EditorGui;
import de.phlup.circuitchaos.client.gui.GameGui;
import de.phlup.circuitchaos.client.gui.PictureConstants;
import de.phlup.circuitchaos.common.CourseHandler;
import de.phlup.circuitchaos.common.enums.CourseState;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.Floortype;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.enums.ObjectType;
import de.phlup.circuitchaos.common.enums.RobotType;
import de.phlup.circuitchaos.common.enums.Step;
import de.phlup.circuitchaos.common.enums.WallType;
import de.phlup.circuitchaos.common.model.Checkpoint;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseElement;
import de.phlup.circuitchaos.common.model.CourseObject;
import de.phlup.circuitchaos.common.model.Floor;
import de.phlup.circuitchaos.common.model.Module;
import de.phlup.circuitchaos.common.model.Position;
import de.phlup.circuitchaos.common.model.Programme;
import de.phlup.circuitchaos.common.model.Robot;
import de.phlup.circuitchaos.common.settings.ClientSettings.Theme;
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

import static de.phlup.circuitchaos.client.gui.PictureConstants.FOLDER_ROBOTS;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_BG;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_E;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_E_N;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_E_S;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_N;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_NE;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_NW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_N_E;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_N_W;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_S;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_SE;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_SW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_S_E;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_S_W;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_W;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_W_N;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_ABYSS_W_S;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CHECKPOINT;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_BACKGROUND;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_CCW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_CW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_CW_CCW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_BACKGROUND;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_CCW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_CW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_CW_CCW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_EXPRESS_STRAIGHT;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_CONVEYOR_BELT_STRAIGHT;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_EXCHANGE_BEAM;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_EXPLOSION;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_FINISH;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_GEARS_CCW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_GEARS_CW;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_GEARS_SOCKET;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LASER_1;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LASER_2;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LASER_3;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LASER_BEAM_1;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LASER_BEAM_2;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_LASER_BEAM_3;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_OPEN_FLOOR;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PIT_STOP;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PRESSURE_BEAM;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PRESSURE_SOCKET;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PROGRAMME;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PROGRAMME_BLOCKED_SUFFIX;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PROGRAMME_NOT_SET;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PROGRAMME_SLOT_SUFFIX;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_PUSHER;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_SELECTION;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_SPIN_LEFT_BEAM;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_SPIN_RIGHT_BEAM;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_START;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRACTOR_BEAM;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRACTOR_SOCKET;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRAPDOOR_1;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRAPDOOR_2;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRAPDOOR_3;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRAPDOOR_4;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRAPDOOR_5;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_TRAPDOOR_6;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_WALL_TYPE;
import static de.phlup.circuitchaos.client.gui.PictureConstants.GFX_WATER;
import static de.phlup.circuitchaos.client.gui.PictureConstants.ROBOT_POSTFIX_FLYING;
import static de.phlup.circuitchaos.client.gui.PictureConstants.ROBOT_POSTFIX_VIRTUAL;
import static de.phlup.circuitchaos.client.gui.PictureConstants.getGfxOfModuleType;
import static de.phlup.circuitchaos.client.gui.PictureConstants.getGfxOfObjectType;
import static de.phlup.circuitchaos.common.enums.Direction.EAST;
import static de.phlup.circuitchaos.common.enums.Direction.NORTH;
import static de.phlup.circuitchaos.common.enums.Direction.SOUTH;
import static de.phlup.circuitchaos.common.enums.Direction.WEST;
import static de.phlup.circuitchaos.common.enums.ModuleType.MAIN_LASER;
import static de.phlup.circuitchaos.common.enums.ModuleType.PRESSURE_BEAM;
import static de.phlup.circuitchaos.common.enums.ModuleType.TRACTOR_BEAM;
import static de.phlup.circuitchaos.common.enums.Step.CONVEYOR_BELTS_MOVE;
import static de.phlup.circuitchaos.common.enums.Step.EXPRESS_CONVEYOR_BELTS_MOVE;
import static de.phlup.circuitchaos.common.enums.Step.GEARS_ROTATE;
import static de.phlup.circuitchaos.server.GlobalServerAttributes.RANDOM;

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

    public ImageIcon getImageIconPlain(Direction direction) {
        return getImageIconPlain(switch (direction) {
            case NORTH -> PictureConstants.GFX_MOVE_NORTH_ARROW;
            case EAST -> PictureConstants.GFX_MOVE_EAST_ARROW;
            case SOUTH -> PictureConstants.GFX_MOVE_SOUTH_ARROW;
            case WEST -> PictureConstants.GFX_MOVE_WEST_ARROW;
        });
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

    public ImageIcon getImageIcon(BaseGui gui, Floor floor) {
        BufferedImage i = getImagePlain(gui, floor);
        return new ImageIcon(scaleImage(i, gui.getZoomFactor() * theme.getZoomFactor()));
    }

    public ImageIcon getImageIconPlain(CourseElement courseElement) {
        if (courseElement instanceof Robot r) {
            return getImageIconPlain(getRobotImagePathAndName(r));
        } else if (courseElement instanceof CourseObject ce) {
            return getImageIconPlain(getGfxOfObjectType(ce.getType()));
        } else {
            return new ImageIcon();
        }
    }

    private BufferedImage getImagePlain(BaseGui gui, Floor f) {
        BufferedImage i = gui.getImageByFloor(f);
        if (i == null) {
            i = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
            gui.putImageOfFloor(f, i);
        }
        return i;
    }

    public ImageIcon getImageIconPlain(Image i) {
        if (i == null) {
            i = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        }
        return new ImageIcon(i);
    }

    public void redrawFloor(Floor floor, BaseGui gui, Step step, Integer phase, Integer subPhase, int animationSteps, int nextCP) {
        Robot activeRobot = null;
        if (gui instanceof GameGui gameGui) {
            Optional<Robot> activeRobotOptional = gui.getCourse().getRobots().stream()
                                                     .filter((r) -> r.getName().equals(gameGui.getMyRobotsName()))
                                                     .filter(CourseElement::isOnCourse)
                                                     .filter((r) -> r.getPosition().equals(floor.getPosition()))
                                                     .findFirst();
            if (activeRobotOptional.isPresent()) {
                activeRobot = activeRobotOptional.get();
            }
        }
        redrawFloor(floor.getPosition(), floor, gui, activeRobot, step, phase, subPhase, animationSteps, nextCP);
    }

    private void redrawFloor(Position position, Floor floor, BaseGui gui, Robot activeRobot, Step step, Integer phase, Integer subPhase, int animationSteps, int nextCP) {
        Course        course = gui.getCourse();
        BufferedImage image  = getImagePlain(gui, floor);
        int           damage = floor.getExplosiveDamage();
        Graphics2D    gr     = image.createGraphics();
        try {
            drawBasicFloor(floor, gui, step, phase, subPhase, course, gr, animationSteps);
            drawCheckpoint(position, course, gr, nextCP);
            drawFlatObjects(position, subPhase, course, gr, animationSteps);
            if (subPhase != null) {
                drawFlatObjectsMoving(position, subPhase, course, gr, animationSteps);
                drawBeams(floor, step, subPhase, image, animationSteps);
            }
            drawNonFlatObjects(position, subPhase, course, gr, animationSteps);
            if (subPhase != null && subPhase < animationSteps) {
                drawNonFlatObjectsMoving(position, subPhase, course, gr, animationSteps);
            }
            drawRobots(position, subPhase, course, gr, animationSteps);
            if (subPhase != null) {
                drawFallingRobotsAndObjects(position, subPhase, course, gr, animationSteps);
            }
            if (subPhase == null || subPhase < animationSteps) {
                drawRobotsMovingAndActiveRobot(position, activeRobot, step, subPhase, course, gr, animationSteps);
                // TODO auch explosion malen, wenn ein Robotor getroffen wird oder stirbt
                drawExplosion(damage, gr, subPhase, animationSteps, getExplosionVariant(position, step));
            }
            if (gui instanceof EditorGui editorGui && editorGui.getSelectedFloor() != null
                    && floor.getPosition().equals(editorGui.getSelectedFloor().getPosition())) {
                gr.drawImage(getImageIconPlain(GFX_SELECTION).getImage(), 0, 0, null);
            }
        } finally {
            gr.dispose();
        }
    }

    private int getExplosionVariant(Position position, Step step) {
        return (position.x() + position.y() + step.ordinal()) % theme.getExplosionVariants();
    }

    private void drawRobotsMovingAndActiveRobot(Position position, Robot activeRobot, Step step, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        for (Robot r : CourseHandler.getLeavingRobots(course, position)) {
            drawRobot(r, gr, subPhase, true, animationSteps);
        }
        if (activeRobot != null && step == Step.SETUP) {
            drawRobot(activeRobot, gr, subPhase, false, animationSteps);
        }
    }

    private void drawFallingRobotsAndObjects(Position position, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        for (CourseElement ce : CourseHandler.getFallingIntoAbyss(course, position)) {
            drawFalling(ce, gr, subPhase, false, animationSteps);
        }
        for (CourseElement ce : CourseHandler.getLeavingFallingIntoAbyss(course, position)) {
            drawFalling(ce, gr, subPhase, true, animationSteps);
        }
    }

    private void drawRobots(Position position, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        for (Robot r : CourseHandler.getRobots(course, position)) {
            drawRobot(r, gr, subPhase, false, animationSteps);
        }
    }

    private void drawNonFlatObjectsMoving(Position position, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        for (CourseObject co : CourseHandler.getLeavingObjects(course, position)) {
            if (!co.getType().isFlat()) {
                drawCircuitChaosObject(co, gr, subPhase, true, animationSteps);
            }
        }
    }

    private void drawNonFlatObjects(Position position, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        for (CourseObject co : CourseHandler.getObjects(course, position)) {
            if (!co.getType().isFlat()) {
                drawCircuitChaosObject(co, gr, subPhase, false, animationSteps);
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

    private void drawFlatObjectsMoving(Position position, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        for (CourseObject co : CourseHandler.getLeavingObjects(course, position)) {
            if (co.getType().isFlat()) {
                drawCircuitChaosObject(co, gr, subPhase, true, animationSteps);
            }
        }
    }

    private void drawFlatObjects(Position position, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        for (CourseObject co : CourseHandler.getObjects(course, position)) {
            if (co.getType().isFlat()) {
                drawCircuitChaosObject(co, gr, subPhase, false, animationSteps);
            }
        }
    }

    private void drawBasicFloor(Floor floor, BaseGui gui, Step step, Integer phase, Integer subPhase, Course course, Graphics2D gr, int animationSteps) {
        BufferedImage bi = getFloorImage(floor, course, gui.getState() != CourseState.GAME_RUNNING, step, phase, subPhase, animationSteps);
        gr.drawImage(bi, new AffineTransform(), null);
    }

    private void drawCheckpoint(Position position, Course course, Graphics2D gr, int nextCP) {
        Checkpoint cp = CourseHandler.getCheckpoint(course, position);
        if (cp != null) {
            int maxCheckpoint = course.getCheckpoints().size() - 1;
            drawCheckpoint(cp, gr, maxCheckpoint, nextCP);
        }
    }

    private void drawRobot(Robot robot, Graphics2D gr, Integer subPhase, boolean leaving, int animationSteps) {
        String picture = getRobotImagePathAndName(robot);
        drawCourseElement(robot, gr, subPhase, leaving, picture, animationSteps);
    }

    private void drawFalling(CourseElement ce, Graphics2D gr, int subPhase, boolean leaving, int animationSteps) {
        String picture = ce instanceof Robot r ? getRobotImagePathAndName(r) :
                ce instanceof CourseObject co ? getGfxOfObjectType(co.getType()) : null;
        if (picture != null) {
            if (subPhase < animationSteps) {
                drawCourseElement(ce, gr, subPhase, leaving, picture, animationSteps);
            } else if (!leaving) {
                drawFalling(ce, gr, subPhase - animationSteps, picture, animationSteps);
            }
        }
    }

    private void drawFalling(CourseElement ce, Graphics2D gr, int subPhase, String picture, int animationSteps) {
        Image         image  = getImageIconPlain(picture).getImage();
        BufferedImage image2 = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr2    = image2.createGraphics();
        try {
            gr2.drawImage(image, AffineTransform.getQuadrantRotateInstance(ce.getDirection().ordinal(), imageSize / 2.0, imageSize / 2.0), null);
        } finally {
            gr2.dispose();
        }
        double factor = 1 - subPhase / (double) animationSteps;
        double offset = imageSize / 2.0 - imageSize / 2.0 * factor;
        gr.translate(offset, offset);
        gr.drawImage(image2, AffineTransform.getScaleInstance(factor, factor), null);
        gr.translate(-offset, -offset);
    }

    private void drawCourseElement(CourseElement ce, Graphics2D gr, Integer subPhase, boolean leaving, String picture, int animationSteps) {
        Image pictureImage = getImageIconPlain(picture).getImage();
        if (subPhase == null || subPhase >= animationSteps || ce.getPosition().equals(ce.getPrevPosition())) {
            if (!leaving) {
                AffineTransform affineTransform = AffineTransform.getQuadrantRotateInstance(ce.getPrevDirection().ordinal(), imageSize / 2.0, imageSize / 2.0);
                affineTransform.rotate(calculateRotation(ce, subPhase, animationSteps), imageSize / 2.0, imageSize / 2.0);
                gr.drawImage(pictureImage, affineTransform, null);
            }
        } else {
            if (!ce.getPosition().isNeighbor(ce.getPrevPosition())) {
                float alpha = subPhase / (animationSteps + 0.0f);
                if (leaving) {
                    alpha = 1 - alpha;
                }
                Composite oldComposite = gr.getComposite();
                gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                AffineTransform affineTransform = AffineTransform.getQuadrantRotateInstance(ce.getPrevDirection().ordinal(), imageSize / 2.0, imageSize / 2.0);
                affineTransform.rotate(calculateRotation(ce, subPhase, animationSteps), imageSize / 2.0, imageSize / 2.0);
                gr.drawImage(pictureImage, affineTransform, null);
                gr.setComposite(oldComposite);
            } else if (ce.getDirection() == ce.getPrevDirection()) {
                gr.translate(imageSize / 2, imageSize / 2);
                addMovingAnimation(pictureImage, gr, ce.getDirection(), calculateMovingDirectionAccordingToBE(ce), subPhase, leaving, !leaving, animationSteps);
                gr.translate(-imageSize / 2, -imageSize / 2);
            } else {
                AffineTransform affineTransform = AffineTransform.getRotateInstance(calculateRotation(ce, subPhase, animationSteps), imageSize / 2.0, imageSize / 2.0);
                affineTransform.quadrantRotate(ce.getPrevDirection().minus(ce.getDirection()).ordinal(), imageSize / 2.0, imageSize / 2.0);
                BufferedImage bi2 = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D    gr2 = bi2.createGraphics();
                try {
                    gr2.drawImage(pictureImage, affineTransform, null);
                } finally {
                    gr2.dispose();
                }

                gr.translate(imageSize / 2, imageSize / 2);
                addMovingAnimation(bi2, gr, ce.getDirection(), calculateMovingDirectionAccordingToBE(ce), subPhase, leaving, !leaving, animationSteps);
                gr.translate(-imageSize / 2, -imageSize / 2);
            }
        }
    }

    private double calculateRotation(CourseElement ce, Integer subPhase, int animationSteps) {
        if (subPhase == null) {
            return 0.0;
        }
        float target = switch (ce.getPrevDirection().minus(ce.getDirection())) {
            case SOUTH -> 1.0f;
            case EAST -> -0.5f;
            case WEST -> 0.5f;
            case NORTH -> 0.0f;
        };
        return target * subPhase * Math.PI / animationSteps;
    }

    private Direction calculateMovingDirectionAccordingToBE(CourseElement ce) {
        if (ce.getPosition().y() < ce.getPrevPosition().y()) {
            return ce.getDirection().add(NORTH);
        } else if (ce.getPosition().y() > ce.getPrevPosition().y()) {
            return ce.getDirection().add(SOUTH);
        } else if (ce.getPosition().x() < ce.getPrevPosition().x()) {
            return ce.getDirection().add(EAST);
        } else {
            return ce.getDirection().add(WEST);
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

    private void drawCheckpoint(Checkpoint cp, Graphics2D gr, int maxCheckpoint, int nextCP) {
        String imageName;
        if (cp.getNumber() == 0) {
            imageName = GFX_START;
        } else if (cp.getNumber() >= maxCheckpoint) {
            imageName = GFX_FINISH;
        } else {
            imageName = GFX_CHECKPOINT;
        }
        Graphics2D checkpointGraphics = (Graphics2D) gr.create();
        try {
            Image i = getImageIconPlain(imageName).getImage();
            if (nextCP != -1 && nextCP != cp.getNumber()) {
                checkpointGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            }
            checkpointGraphics.drawImage(i, 0, 0, null);
        } finally {
            checkpointGraphics.dispose();
        }
    }

    private void drawCircuitChaosObject(CourseObject co, Graphics2D gr, Integer subPhase, boolean leaving, int animationSteps) {
        String picture = getGfxOfObjectType(co.getType());
        if (co.getType() == ObjectType.GLUE) {
            picture = picture + ((int) (co.getVariantSeed() * theme.getGlueVariants()));
        }
        if (co.getType() == ObjectType.OIL) {
            picture = picture + ((int) (co.getVariantSeed() * theme.getOilVariants()));
        }
        drawCourseElement(co, gr, subPhase, leaving, picture, animationSteps);
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

    private BufferedImage getFloorImage(Floor floor, Course course, boolean gameIsNotStartedYet,
                                        Step step, Integer phase, Integer subPhase, int animationSteps) {
        Direction     rotation = determineRotation(floor);
        BufferedImage bi       = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr       = bi.createGraphics();
        try {
            gr.translate(imageSize / 2, imageSize / 2);

            addGround(floor, rotation, gr, step, subPhase, animationSteps);
            addTrapdoor(floor, step, phase, subPhase, gr, rotation, animationSteps);
            modifyAbyssEdges(course, floor, gr);
            if (floor.isWater()) {
                addToImage(GFX_WATER + RANDOM.nextInt(theme.getWaterVariants()), NORTH, gr);
            }
            addPusher(floor, gr, gameIsNotStartedYet, step, phase, subPhase, animationSteps);
            if (step == Step.COURSE_MOUNTED_LASER_FIRE || gameIsNotStartedYet) {
                drawBeams(floor.getCourseMountedLaserBeamsNS(), gr, false, subPhase, animationSteps, MAIN_LASER);
                drawBeams(floor.getCourseMountedLaserBeamsWE(), gr, true, subPhase, animationSteps, MAIN_LASER);
            }
            if (step == Step.COURSE_MOUNTED_PRESSURE_BEAMS_FIRE || gameIsNotStartedYet) {
                if (floor.isCourseMountedPressureBeamsNS()) {
                    drawBeams(1, gr, false, subPhase, animationSteps, PRESSURE_BEAM);
                }
                if (floor.isCourseMountedPressureBeamsWE()) {
                    drawBeams(1, gr, true, subPhase, animationSteps, PRESSURE_BEAM);
                }
            }
            if (step == Step.COURSE_MOUNTED_TRACTOR_BEAMS_FIRE || gameIsNotStartedYet) {
                if (floor.isCourseMountedTractorBeamsNS()) {
                    drawBeams(1, gr, false, subPhase, animationSteps, TRACTOR_BEAM);
                }
                if (floor.isCourseMountedTractorBeamsWE()) {
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
    }

    private void addLasers(Floor floor, Graphics2D gr) {
        for (int counter = 0; counter < 4; counter++) {
            int laserCount = floor.getLasers()[counter];
            if (laserCount > 0) {
                addToImage(laserCount == 1 ? GFX_LASER_1 : laserCount == 2 ? GFX_LASER_2 : GFX_LASER_3, Direction.values()[counter], gr);
            }
            if (floor.getPressureBeam()[counter]) {
                addToImage(GFX_PRESSURE_SOCKET, Direction.values()[counter], gr);
            }
            if (floor.getTractorBeam()[counter]) {
                addToImage(GFX_TRACTOR_SOCKET, Direction.values()[counter], gr);
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

    private void modifyAbyssEdges(Course course, Floor floor, Graphics2D gr) {
        Floortype floortype = floor.getFloortype();
        if (floortype == Floortype.ABYSS) {
            Floor floorWest  = CourseHandler.getFloor(course, floor.getPosition().neighbour(WEST));
            Floor floorSouth = CourseHandler.getFloor(course, floor.getPosition().neighbour(SOUTH));
            Floor floorEast  = CourseHandler.getFloor(course, floor.getPosition().neighbour(EAST));
            Floor floorNorth = CourseHandler.getFloor(course, floor.getPosition().neighbour(NORTH));
            addPlatformEdgesOnAbyss(course, gr, floorNorth, floorWest, floorEast, floorSouth);
        }
    }

    private void addPlatformEdgesOnAbyss(Course course, Graphics2D gr, Floor floorNorth, Floor floorWest, Floor floorEast, Floor floorSouth) {
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
                && CourseHandler.getFloor(course, floorNorth.getPosition().neighbour(EAST)).getFloortype() != Floortype.ABYSS) {
            addToImage(GFX_ABYSS_NE, NORTH, gr);
        }
        if (floorNorth.getFloortype() == Floortype.ABYSS && floorWest.getFloortype() == Floortype.ABYSS
                && CourseHandler.getFloor(course, floorNorth.getPosition().neighbour(WEST)).getFloortype() != Floortype.ABYSS) {
            addToImage(GFX_ABYSS_NW, NORTH, gr);
        }
        if (floorSouth.getFloortype() == Floortype.ABYSS && floorEast.getFloortype() == Floortype.ABYSS
                && CourseHandler.getFloor(course, floorSouth.getPosition().neighbour(EAST)).getFloortype() != Floortype.ABYSS) {
            addToImage(GFX_ABYSS_SE, NORTH, gr);
        }
        if (floorSouth.getFloortype() == Floortype.ABYSS && floorWest.getFloortype() == Floortype.ABYSS
                && CourseHandler.getFloor(course, floorSouth.getPosition().neighbour(WEST)).getFloortype() != Floortype.ABYSS) {
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
        return GFX_OPEN_FLOOR + Math.floorMod(position.x() + position.y() * 6, theme.getOpenFloorVariants());
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
