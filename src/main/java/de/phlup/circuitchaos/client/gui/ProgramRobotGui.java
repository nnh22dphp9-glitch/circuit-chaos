package de.phlup.circuitchaos.client.gui;

import de.phlup.circuitchaos.client.service.ImageSupplier;
import de.phlup.circuitchaos.common.enums.Direction;
import de.phlup.circuitchaos.common.enums.ModuleType;
import de.phlup.circuitchaos.common.model.GameAttributes;
import de.phlup.circuitchaos.common.model.Module;
import de.phlup.circuitchaos.common.model.NetworkRequest;
import de.phlup.circuitchaos.common.model.NetworkResponse;
import de.phlup.circuitchaos.common.model.Programme;
import de.phlup.circuitchaos.common.model.Robot;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ProgramRobotGui implements GuiHelper {

    private final String          answerUrl;
    private final NetworkRequest  request;
    private final NetworkResponse response = new NetworkResponse();
    private final JFrame          mainFrame;

    private GameAttributes gameAttributes;

    private final JTextField[] programIds              = new JTextField[]{new JTextField(2), new JTextField(2), new JTextField(2), new JTextField(2), new JTextField(2)};
    private final JButton[]    weaponButton            = new JButton[]{new JButton(), new JButton(), new JButton(), new JButton(), new JButton()};
    private final Module[]     weaponModule            = new Module[5];
    private final JCheckBox[]  brakesPhase             = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  bridgePhase             = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  gluePhase               = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  minePhase               = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  oilPhase                = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  teleporterPhase         = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  proxyPhase              = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  hovercraftPhase         = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  mechanicalArm           = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox[]  gyroscope               = new JCheckBox[]{new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false), new JCheckBox("", false)};
    private final JCheckBox    powerdown               = new JCheckBox("", false);
    private final JButton      okButton                = new JButton("OK");
    private final JPanel       potentialProgrammePanel = new JPanel(new FlowLayout());
    private final JPanel[]     programmePanel          = new JPanel[5];

    private final Robot             robot;
    private final ImageSupplier     imageSupplier;
    private final ConstraintChecker constraintChecker = new ConstraintChecker();

    private int       potentialProgrammeCount;
    private boolean[] used;

    public void sendAnswer() {
        if (gameAttributes != null) {
            gameAttributes.getGameGui().getClientToServerConnection().postAnswer(answerUrl, response);
        }
        mainFrame.dispose();
        gameAttributes = null;
    }


    public ProgramRobotGui(GameAttributes gameAttributes, String answerUrl, NetworkRequest request) {
        this.answerUrl = answerUrl;
        this.request = request;
        this.gameAttributes = gameAttributes;
        response.setFilled(true);
        mainFrame = new JFrame("Circuit Chaos - %s - %s - Program".formatted(gameAttributes.getRegistration().getGameName(), request.getMyRobot().getName()));
        mainFrame.setVisible(false);
        JFrame courseFrame = gameAttributes.getGameGui().getMainFrame();
        mainFrame.setLocationRelativeTo(courseFrame);
        mainFrame.setLocation(mainFrame.getLocation().x - courseFrame.getWidth() / 2 + 10,
                              mainFrame.getLocation().y + 120);

        robot = request.getMyRobot();
        imageSupplier = gameAttributes.getGameGui().getImageSupplier();
        robot.setPotentialProgramme(new ArrayList<>(robot.getPotentialProgramme()
                                                         .stream()
                                                         .sorted((a, b) -> a == null || b == null ? 0 : Integer.compare(a.getPriority(), b.getPriority()))
                                                         .toList()));
    }

    public void apply() {
        JScrollPane contentPane = new JScrollPane(createContentPane(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainFrame.setContentPane(contentPane);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                sendAnswer();
            }
        });
        mainFrame.setResizable(true);
        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.requestFocus();
    }

    protected JPanel createContentPane() {
        initProgramPanels();
        potentialProgrammeCount = robot.getPotentialProgramme().size();
        used = new boolean[robot.getPotentialProgramme().size()];
        Arrays.fill(used, false);

        JPanel        contentPane   = new JPanel(new BorderLayout());
        GridBagLayout programLayout = new GridBagLayout();
        GridBagLayout potProgLayout = new GridBagLayout();
        JPanel        infoPanel     = new JPanel(new FlowLayout());
        JPanel        programP      = new JPanel(programLayout);
        JPanel        potProgP      = new JPanel(potProgLayout);
        JPanel        modulePanel   = new JPanel(new FlowLayout());
        JPanel        leftSidePanel = new JPanel(new BorderLayout());

        leftSidePanel.add(infoPanel, BorderLayout.NORTH);
        leftSidePanel.add(potProgP, BorderLayout.CENTER);
        leftSidePanel.add(modulePanel, BorderLayout.SOUTH);
        contentPane.add(leftSidePanel, BorderLayout.WEST);
        contentPane.add(programP, BorderLayout.CENTER);
        contentPane.setMaximumSize(new Dimension(200, 200));

        fillInfoPanel(infoPanel);
        fillPotProgPanel(potProgP, potProgLayout);
        fillModulePanel(modulePanel);
        fillProgramPanel(programP, programLayout);

        for (int placeID = 0; placeID < 5; placeID++) {
            if (!robot.getBlocked()[placeID]) {
                updateProgramPlaceID(placeID, -1);
            }
        }

        return contentPane;
    }

    private void fillInfoPanel(JPanel panel) {
        JLabel robotLabel = new JLabel(imageSupplier.getImageIconPlain(robot));
        robotLabel.setToolTipText(robot.getName());
        panel.add(robotLabel);
        panel.add(new JLabel("   "));

        String template = "<html><font face=\"sans-serif\">&nbsp;<br>&nbsp;<b style=\"color:Navy\">%s</b>&nbsp;&nbsp;%s&nbsp;<br>&nbsp;<br></font></html>";
        panel.add(new JLabel(template.formatted("Damage:", robot.getDamage())));
        panel.add(new JLabel(template.formatted("Next Checkpoint:", robot.getNextCheckpoint())));

        if (robot.isVirtual()) {
            panel.add(new JLabel(template.formatted("VIRTUAL", "")));
        }

        if (robot.isPoweredDown()) {
            panel.add(new JLabel(template.formatted("POWERED DOWN", "")));
        }
    }

    private void fillModulePanel(JPanel panel) {
        for (Module module : robot.getModules()) {
            JLabel p = new JLabel(imageSupplier.getImageIcon(module, 1));
            p.setToolTipText(module.getType().getModuleName());
            panel.add(p);
        }
    }

    private void fillPotProgPanel(JPanel panel, GridBagLayout layout) {
        addComponentToPanel(potentialProgrammePanel, panel, layout, 1, 2, 11, GridBagConstraints.CENTER);
        updatePotProgPanel();
    }

    private void fillProgramPanel(JPanel panel, GridBagLayout layout) {
        int y = 1;
        if (robot.isMayChooseDirection()) {
            addComponentToPanel(new JLabel("Starting Direction: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            robot.setDirection(Direction.NORTH);
            robot.setPrevDirection(robot.getDirection());
            JButton directionButton = new JButton(imageSupplier.getImageIconPlain(Direction.NORTH));
            directionButton.setMinimumSize(new Dimension(60, 60));
            directionButton.setPreferredSize(new Dimension(60, 60));
            directionButton.setMaximumSize(new Dimension(60, 60));
            directionButton.addActionListener(new DirectionListener(directionButton, null, imageSupplier));
            addComponentToPanel(directionButton, panel, layout, 2, y, 5, GridBagConstraints.WEST);
        }

        y++;
        addComponentToPanel(new JLabel(" "), panel, layout, 1, y, 1, GridBagConstraints.EAST);

        y++;
        addComponentToPanel(new JLabel(" "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
        for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
            addComponentToPanel(new JLabel(" %s ".formatted(phaseCounter + 1)), panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
        }

        y++;
        addComponentToPanel(new JLabel(" "), panel, layout, 1, y, 1, GridBagConstraints.EAST);

        y++;
        addComponentToPanel(new JLabel(" "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
        for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
            programIds[phaseCounter].setBorder(new SoftBevelBorder(1));
            JPanel panel2 = new JPanel(new BorderLayout());
            panel2.add(new JLabel(" "), BorderLayout.WEST);
            panel2.add(programmePanel[phaseCounter], BorderLayout.CENTER);
            panel2.add(new JLabel(" "), "East");
            addComponentToPanel(panel2, panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
        }

        y++;
        addComponentToPanel(new JLabel("Power Down: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
        powerdown.setSelected(robot.getDamage() > 7);
        addComponentToPanel(powerdown, panel, layout, 9, y, 1, GridBagConstraints.CENTER);

        List<Module> weapons = robot.getModules().stream().filter(m -> m.getType().isWeapon()).toList();
        if (weapons.size() == 1) {
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                weaponModule[phaseCounter] = weapons.getFirst();
            }
        } else if (weapons.size() > 1) {
            y++;
            addComponentToPanel(new JLabel("Weapon: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(weaponButton[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
                Iterator<Module> iterator    = weapons.iterator();
                Module           firstWeapon = iterator.next();
                weaponButton[phaseCounter].setIcon(imageSupplier.getImageIcon(firstWeapon, .5));
                weaponButton[phaseCounter].setBorderPainted(false);
                weaponButton[phaseCounter].setBackground(new Color(0, 0, 0, 0));
                weaponButton[phaseCounter].addActionListener(new WeaponListener(weaponButton[phaseCounter], phaseCounter, imageSupplier, weapons, iterator));
                weaponModule[phaseCounter] = firstWeapon;
                int phase = phaseCounter;
                weapons.forEach(m -> m.getActiveInPhase()[phase] = m.equals(firstWeapon));
            }
        }

        if (robot.hasModule(ModuleType.BRAKES)) {
            y++;
            addComponentToPanel(new JLabel("Brakes: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(brakesPhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.BRIDGE_PROJECTOR)) {
            y++;
            addComponentToPanel(new JLabel("Bridge Projector: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(bridgePhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.GLUE_DISPENSER)) {
            y++;
            addComponentToPanel(new JLabel("Glue Dispenser: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(gluePhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.GYROSCOPIC_STABILIZER)) {
            y++;
            addComponentToPanel(new JLabel("Gyroscopic Stabilizer: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(gyroscope[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.HOVERCRAFT)) {
            y++;
            addComponentToPanel(new JLabel("Hovercraft: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(hovercraftPhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.MECHANICAL_ARM)) {
            y++;
            addComponentToPanel(new JLabel("Mechanical Arm: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(mechanicalArm[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.MINE_LAYER)) {
            y++;
            addComponentToPanel(new JLabel("Mine Layer: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(minePhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.MOBILE_TELEPORTER)) {
            y++;
            addComponentToPanel(new JLabel("Mobile Teleporter: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(teleporterPhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.OIL_DISPENSER)) {
            y++;
            addComponentToPanel(new JLabel("Oil Dispenser: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(oilPhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        if (robot.hasModule(ModuleType.PROXIMITY_MINE_LAYER)) {
            y++;
            addComponentToPanel(new JLabel("Proximity Mine: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            for (int phaseCounter = 0; phaseCounter < 5; phaseCounter++) {
                addComponentToPanel(proxyPhase[phaseCounter], panel, layout, 3 + phaseCounter, y, 1, GridBagConstraints.CENTER);
            }
        }

        y++;
        addComponentToPanel(new JLabel(" "), panel, layout, 1, y, 1, GridBagConstraints.EAST);

        Module shield = robot.getModule(ModuleType.SHIELD);
        if (shield != null) {
            y++;
            addComponentToPanel(new JLabel("Shield Direction: "), panel, layout, 1, y, 1, GridBagConstraints.EAST);
            shield.setDirection(Direction.NORTH);
            JButton directionButton = new JButton(imageSupplier.getImageIconPlain(PictureConstants.GFX_MOVE_NORTH_ARROW));
            directionButton.setMinimumSize(new Dimension(60, 60));
            directionButton.setPreferredSize(new Dimension(60, 60));
            directionButton.setMaximumSize(new Dimension(60, 60));
            directionButton.addActionListener(new DirectionListener(directionButton, shield, imageSupplier));
            addComponentToPanel(directionButton, panel, layout, 2, y, 5, GridBagConstraints.CENTER);
        }

        okButton.addActionListener(new OkButtonActionListener());
        okButton.setEnabled(false);
        addComponentToPanel(okButton, panel, layout, 9, 4, 1, GridBagConstraints.CENTER);

        for (int cnt = 0; cnt < 5; cnt++) {
            programIds[cnt].getDocument().addDocumentListener(constraintChecker);
            weaponButton[cnt].addChangeListener(constraintChecker);
            brakesPhase[cnt].addChangeListener(constraintChecker);
            bridgePhase[cnt].addChangeListener(constraintChecker);
            gluePhase[cnt].addChangeListener(constraintChecker);
            minePhase[cnt].addChangeListener(constraintChecker);
            oilPhase[cnt].addChangeListener(constraintChecker);
            teleporterPhase[cnt].addChangeListener(constraintChecker);
            proxyPhase[cnt].addChangeListener(constraintChecker);
            hovercraftPhase[cnt].addChangeListener(constraintChecker);
        }
    }

    private void initProgramPanels() {
        for (int cnt = 0; cnt < 5; cnt++) {
            programmePanel[cnt] = new JPanel(new BorderLayout());
            if (robot.getBlocked()[cnt]) {
                Programme programme  = robot.getProgram()[cnt];
                JLabel    progPanel  = new JLabel(imageSupplier.getImageIcon(programme, true, true));
                JPanel    innerPanel = new JPanel(new BorderLayout());
                innerPanel.add(progPanel, BorderLayout.NORTH);
                if (programme.getPriority() > 0) {
                    innerPanel.add(new JLabel(" Prio %s ".formatted(programme.getPriority())), BorderLayout.CENTER);
                } else {
                    innerPanel.add(new JLabel(" "), BorderLayout.CENTER);
                }
                innerPanel.add(new JLabel(" - BLOCKED - "), BorderLayout.SOUTH);
                programIds[cnt].setText("BLOCKED");
                programmePanel[cnt].add(innerPanel);
            } else {
                programmePanel[cnt].add(programIds[cnt]);
            }
        }
    }

    public void updateProgramPlaceID(int placeID, int id) {
        programmePanel[placeID].removeAll();
        JPanel    innerPanel = new JPanel(new BorderLayout());
        Programme programme  = id >= 0 ? robot.getPotentialProgramme().get(id) : null;
        JLabel    progPanel  = new JLabel(programme == null ? imageSupplier.getProgrammeNotSetIcon(true) : imageSupplier.getImageIcon(programme, true, false));
        if (programme != null) {
            ProgramMouseAdapter ma = new ProgramMouseAdapter(id);
            progPanel.addMouseListener(ma);
            innerPanel.addMouseListener(ma);
            if (programme.getPriority() > 0) {
                innerPanel.add(new JLabel(" Prio %s ".formatted(programme.getPriority()), SwingConstants.CENTER), BorderLayout.CENTER);
            } else {
                innerPanel.add(new JLabel(" "), BorderLayout.CENTER);
            }
            used[id] = true;
        } else {
            innerPanel.add(new JLabel(" "), BorderLayout.CENTER);
            if (id >= 0) {
                used[id] = false;
            }
        }
        innerPanel.add(progPanel, BorderLayout.NORTH);
        // innerPanel.add(programIds[placeID], BorderLayout.SOUTH); // if mouse adapter fails again
        programmePanel[placeID].add(innerPanel);
        updatePotProgPanel();
    }

    private void updatePotProgPanel() {
        potentialProgrammePanel.removeAll();
        int counter = 0;
        for (Programme pr : robot.getPotentialProgramme()) {
            JLabel progPanel;
            JPanel innerPanel = new JPanel(new BorderLayout());
            if (pr.getPriority() > 0) {
                innerPanel.add(new JLabel(" Prio %s ".formatted(pr.getPriority()), SwingConstants.CENTER), BorderLayout.CENTER);
            } else {
                innerPanel.add(new JLabel(" "), BorderLayout.CENTER);
            }
            if (used[counter]) {
                progPanel = new JLabel(imageSupplier.getProgrammeNotSetIcon(false));
            } else {
                progPanel = new JLabel(imageSupplier.getImageIcon(pr, false, false));
                ProgramMouseAdapter ma = new ProgramMouseAdapter(counter);
                progPanel.addMouseListener(ma);
                innerPanel.addMouseListener(ma);
            }
            innerPanel.add(progPanel, BorderLayout.NORTH);
            potentialProgrammePanel.add(innerPanel);
            counter++;
        }
    }

    @AllArgsConstructor
    private class WeaponListener extends AbstractAction {

        private final JButton          button;
        private final int              phaseCounter;
        private final ImageSupplier    imageSupplier;
        private final List<Module>     weapons;
        private       Iterator<Module> weaponIterator;

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (weaponIterator.hasNext()) {
                Module next = weaponIterator.next();
                weaponModule[phaseCounter] = next;
            } else {
                weaponIterator = weapons.iterator();
                Module next = weaponIterator.next();
                weaponModule[phaseCounter] = next;
            }
            weapons.forEach(m -> m.getActiveInPhase()[phaseCounter] = m.equals(weaponModule[phaseCounter]));
            button.setIcon(imageSupplier.getImageIcon(weaponModule[phaseCounter], .5));
        }
    }

    @AllArgsConstructor
    private class DirectionListener extends AbstractAction {

        private final JButton       button;
        private final Module        module;
        private final ImageSupplier imageSupplier;

        @Override
        public void actionPerformed(ActionEvent ae) {
            Direction newDirection;
            if (module == null) {
                newDirection = setDirectionOfRobot();
            } else {
                newDirection = setDirectionOfOption();
            }
            button.setIcon(imageSupplier.getImageIconPlain(newDirection));
        }

        private Direction setDirectionOfRobot() {
            robot.setPrevDirection(robot.getDirection());
            robot.setDirection(robot.getDirection().add(Direction.EAST));
            return robot.getDirection();
        }

        private Direction setDirectionOfOption() {
            module.setDirection(module.getDirection().add(Direction.EAST));
            return module.getDirection();
        }
    }

    private class ConstraintChecker implements DocumentListener, ChangeListener {

        @Override
        public void changedUpdate(DocumentEvent e) {
            action();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            action();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            action();
        }

        public void action() {
            for (int cnt = 0; cnt < request.getMyRobot().getPotentialProgramme().size(); cnt++) {
                used[cnt] = false;
            }
            okButton.setEnabled(checkProgram());
        }

        private boolean checkProgram() {
            boolean erg = true;
            for (int cnt = 0; cnt < 5; cnt++) {
                if (!StringUtils.hasText(programIds[cnt].getText())) {
                    erg = request.getMyRobot().getBlocked()[cnt];
                } else {
                    try {
                        int c = Integer.parseInt(programIds[cnt].getText());
                        if (c > potentialProgrammeCount || c > used.length) {
                            erg = false;
                            updateProgramPlaceID(cnt, -1);
                        } else {
                            if (used[c - 1]) {
                                erg = false;
                                updateProgramPlaceID(cnt, -1);
                            } else {
                                used[c - 1] = true;
                                updateProgramPlaceID(cnt, c - 1);
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException | NumberFormatException nfe) {
                        erg = request.getMyRobot().getBlocked()[cnt];
                    }
                }
            }
            return erg;
        }
    }

    @AllArgsConstructor
    private class ProgramMouseAdapter extends MouseAdapter {

        private final int programmeId;

        @Override
        public void mouseReleased(MouseEvent e) {
            constraintChecker.action();
            if (used[programmeId]) {
                handleDeleteFromProgram();
            } else {
                handleNextProgramStep();
            }
            potentialProgrammePanel.validate();
            for (JPanel panel : programmePanel) {
                panel.validate();
            }
        }

        private void handleDeleteFromProgram() {
            for (int cnt = 0; cnt < 5; cnt++) {
                if (Objects.equals("" + (programmeId + 1), programIds[cnt].getText())) {
                    programIds[cnt].setText("");
                    updateProgramPlaceID(cnt, -1);
                }
            }
            constraintChecker.action();
        }

        private void handleNextProgramStep() {
            int freePlace = -1;
            for (int cnt = 0; cnt < 5; cnt++) {
                if (!StringUtils.hasText(programIds[cnt].getText())) {
                    freePlace = cnt;
                    break;
                }
            }
            if (freePlace > -1) {
                updateProgramPlaceID(freePlace, programmeId);
                programIds[freePlace].setText("" + (programmeId + 1));
            }
            constraintChecker.action();
        }
    }

    private class OkButtonActionListener extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            Programme[] programme = new Programme[robot.getPotentialProgramme().size()];
            for (int cnt = 0; cnt < robot.getPotentialProgramme().size(); cnt++) {
                programme[cnt] = robot.getPotentialProgramme().get(cnt);
            }
            for (int cnt = 0; cnt < robot.getPotentialProgramme().size(); cnt++) {
                if (used[cnt]) {
                    robot.getPotentialProgramme().remove(programme[cnt]);
                }
            }
            for (int phase = 0; phase < 5; phase++) {
                if (!robot.getBlocked()[phase]) {
                    if (StringUtils.hasText(programIds[phase].getText())) {
                        robot.getProgram()[phase] = programme[Integer.parseInt(programIds[phase].getText()) - 1];
                    } else {
                        robot.getProgram()[phase] = null;
                    }
                }
                int phaseFinal = phase;
                robot.getModules().stream().filter(m -> m.getType().isWeapon())
                     .forEach(module -> module.getActiveInPhase()[phaseFinal] = weaponModule[phaseFinal].equals(module));
                if (robot.hasModule(ModuleType.BRAKES)) {
                    robot.getModule(ModuleType.BRAKES).getActiveInPhase()[phase] = brakesPhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.BRIDGE_PROJECTOR)) {
                    robot.getModule(ModuleType.BRIDGE_PROJECTOR).getActiveInPhase()[phase] = bridgePhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.GLUE_DISPENSER)) {
                    robot.getModule(ModuleType.GLUE_DISPENSER).getActiveInPhase()[phase] = gluePhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.HOVERCRAFT)) {
                    robot.getModule(ModuleType.HOVERCRAFT).getActiveInPhase()[phase] = hovercraftPhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.MECHANICAL_ARM)) {
                    robot.getModule(ModuleType.MECHANICAL_ARM).getActiveInPhase()[phase] = mechanicalArm[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.MINE_LAYER)) {
                    robot.getModule(ModuleType.MINE_LAYER).getActiveInPhase()[phase] = minePhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.MOBILE_TELEPORTER)) {
                    robot.getModule(ModuleType.MOBILE_TELEPORTER).getActiveInPhase()[phase] = teleporterPhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.OIL_DISPENSER)) {
                    robot.getModule(ModuleType.OIL_DISPENSER).getActiveInPhase()[phase] = oilPhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.PROXIMITY_MINE_LAYER)) {
                    robot.getModule(ModuleType.PROXIMITY_MINE_LAYER).getActiveInPhase()[phase] = proxyPhase[phase].isSelected();
                }
                if (robot.hasModule(ModuleType.GYROSCOPIC_STABILIZER)) {
                    robot.getModule(ModuleType.GYROSCOPIC_STABILIZER).getActiveInPhase()[phase] = gyroscope[phase].isSelected();
                }
            }
            robot.setPowerDownAnnounced(powerdown.isSelected());
            response.setProgrammedRobot(robot);
            sendAnswer();
        }

    }

}
