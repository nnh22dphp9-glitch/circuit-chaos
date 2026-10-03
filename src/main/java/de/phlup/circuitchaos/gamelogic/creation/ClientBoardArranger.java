package de.phlup.circuitchaos.gamelogic.creation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.enums.ComputerType;
import de.phlup.circuitchaos.gamelogic.GameGui;
import de.phlup.circuitchaos.gamelogic.GameOptions;
import de.phlup.circuitchaos.gamelogic.utils.BoardHandler;
import de.phlup.circuitchaos.gamelogic.utils.GuiHelper;
import de.phlup.circuitchaos.gamelogic.utils.PictureConstants;
import de.phlup.circuitchaos.model.Board;
import de.phlup.circuitchaos.model.BoardInfo;
import de.phlup.circuitchaos.model.BoardPosition;
import de.phlup.circuitchaos.service.ImageSupplier;
import de.phlup.circuitchaos.settings.ClientSettings;
import de.phlup.circuitchaos.settings.GameSettings;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Objects;

@Slf4j
public class ClientBoardArranger implements GuiHelper {

    private final ImageSupplier imageSupplier;
    private final JFrame        frame = new JFrame("CC");

    // arrangement

    private final JComboBox<BoardInfo> boardList = new JComboBox<>();

    private final JButton   rotateCCW;
    private final JButton   rotateCW;
    private final JButton   addButton    = new JButton("add");
    @Getter
    private final JButton   removeButton = new JButton("remove");
    @Getter
    private final JButton   playButton   = new JButton("Play");
    private final JPanel    chooserPanel = new JPanel(new BorderLayout());
    private final JTextArea posx         = new JTextArea(1, 3);
    private final JTextArea posy         = new JTextArea(1, 3);
    private final JLabel    regPlayers   = new JLabel(" 1 ");
    private final JLabel    regWatchers  = new JLabel(" 0 ");

    @SuppressWarnings("unchecked")
    private final JComboBox<String>[]   computerTypes = new JComboBox[7];
    private final Collection<BoardInfo> boardInfos;
    private final ObjectMapper          objectMapper;
    private final ClientSettings        clientSettings;

    private int    rotation     = 0;
    private JLabel boardPreview = new JLabel();


    public ClientBoardArranger(GameGui game, GameSettings gameSettings, Collection<BoardInfo> boards, ObjectMapper objectMapper, ClientSettings clientSettings) {
        imageSupplier = game.getImageSupplier();
        rotateCCW = new JButton(imageSupplier.getImageIconPlain(PictureConstants.GFX_ROTATE_LEFT_ARROW));
        rotateCW = new JButton(imageSupplier.getImageIconPlain(PictureConstants.GFX_ROTATE_RIGHT_ARROW));
        boardInfos = boards;
        this.objectMapper = objectMapper;
        this.clientSettings = clientSettings;

        JPanel borderPanel = new JPanel(new BorderLayout());
        JPanel mainPanel   = new JPanel(new BorderLayout());
        mainPanel.add(createBoardChooser(game), BorderLayout.NORTH);
        mainPanel.add(createPlayersDisplay(), BorderLayout.CENTER);
        mainPanel.add(createComputerPlayerChooser(gameSettings), BorderLayout.SOUTH);
        borderPanel.add(mainPanel, BorderLayout.CENTER);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.SOUTH);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB))), BorderLayout.EAST);
        borderPanel.add(new JLabel(new ImageIcon(new BufferedImage(10, 5, BufferedImage.TYPE_INT_ARGB))), BorderLayout.WEST);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (game.isNotStartedYet()) {
                    game.getMainFrame().dispose();
                }
            }
        });
        frame.setContentPane(borderPanel);
        frame.setResizable(false);
        frame.setLocationRelativeTo(game.getMainFrame());
        frame.setLocation(frame.getX() + game.getMainFrame().getWidth() / 2,
                          frame.getY() - game.getMainFrame().getHeight() / 2 + 20);
        frame.pack();
        frame.setVisible(true);
        frame.requestFocus();
    }

    private JPanel createPlayersDisplay() {
        GridBagLayout gbl   = new GridBagLayout();
        JPanel        panel = new JPanel(gbl);
        addComponentToPanel(new JLabel(" "), panel, gbl, 1, 1, 1, GridBagConstraints.WEST);
        addComponentToPanel(new JLabel(" registered Players:     "), panel, gbl, 1, 2, 1, GridBagConstraints.WEST);
        addComponentToPanel(regPlayers, panel, gbl, 2, 2, 1, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(" registered Watchers:    "), panel, gbl, 1, 3, 1, GridBagConstraints.WEST);
        addComponentToPanel(regWatchers, panel, gbl, 2, 3, 1, GridBagConstraints.EAST);
        addComponentToPanel(new JLabel(" "), panel, gbl, 1, 4, 1, GridBagConstraints.WEST);
        return panel;
    }

    private JPanel createComputerPlayerChooser(GameSettings gameSettings) {
        GridBagLayout layout      = new GridBagLayout();
        JPanel        compPlayers = new JPanel(layout);
        compPlayers.setBorder(new TitledBorder(new LineBorder(Color.black), " Computer players "));
        for (int i = 0; i < 7; i++) {
            addComponentToPanel(new JLabel("#%s: ".formatted(i + 1)), compPlayers, layout, 1, 6 + i, 1, GridBagConstraints.WEST);
            computerTypes[i] = new JComboBox<>();
            for (ComputerType ct : ComputerType.values()) {
                computerTypes[i].addItem(ct.getName());
            }
            computerTypes[i].setSelectedIndex(i + 1 <= gameSettings.getMaxNumberOfComputerPlayers() ? 1 : 0);
            addComponentToPanel(computerTypes[i], compPlayers, layout, 2, 6 + i, 1, GridBagConstraints.WEST);
        }
        return compPlayers;
    }

    private JPanel createBoardChooser(GameGui game) {
        JPanel boardChooser = new JPanel(new BorderLayout());
        boardList.setEditable(false);
        BoardInfo item = new BoardInfo("Choose a board!");
        item.setImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_PICTURE).getImage());
        boardList.addItem(item);
        for (BoardInfo bi : boardInfos) {
            boardList.addItem(bi);
        }
        boardList.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotation = 0;
                rotateCW.setEnabled(boardList.getSelectedIndex() > 0);
                rotateCCW.setEnabled(boardList.getSelectedIndex() > 0);
                addButton.setEnabled(boardList.getSelectedIndex() > 0);
                redrawPreview();
            }
        });
        rotation = 0;
        boardChooser.add(boardList, BorderLayout.CENTER);

        rotateCCW.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotation--;
                if (rotation == -1) {
                    rotation = 3;
                }
                redrawPreview();
            }
        });
        rotateCCW.setEnabled(false);

        rotateCW.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotation++;
                if (rotation == 4) {
                    rotation = 0;
                }
                redrawPreview();
            }
        });
        rotateCW.setEnabled(false);

        posx.setText("0");
        posx.setBorder(new SoftBevelBorder(1));
        posy.setText("0");
        posy.setBorder(new SoftBevelBorder(1));

        addButton.addActionListener(new AbstractAction() {
            @Override
            @SneakyThrows
            public void actionPerformed(ActionEvent e) {
                if (boardList.getSelectedIndex() > 0) {
                    BoardInfo selectedItem = (BoardInfo) boardList.getSelectedItem();
                    if (selectedItem != null && selectedItem.getBoard() != null) {
                        Board board = copy(selectedItem.getBoard());
                        while (rotation > 0) {
                            BoardHandler.rotateBoard(board);
                            rotation--;
                        }
                        BoardPosition position = new BoardPosition(board, Integer.parseInt(posx.getText()), Integer.parseInt(posy.getText()));
                        game.getClientToServerConnection().addBoard(game.getGameAttributes().getGameUrl(), position);
                        removeButton.setEnabled(true);
                    }
                }
            }
        });
        addButton.setEnabled(true);

        removeButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.getClientToServerConnection().removeBoard(game.getGameAttributes().getGameUrl(), Integer.parseInt(posx.getText()), Integer.parseInt(posy.getText()));
            }
        });
        removeButton.setEnabled(false);

        playButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.setNotStartedYet(false);
                frame.dispose();
                GameOptions gameOptions = new GameOptions();
                for (int i = 0; i < 7; i++) {
                    String type = (String) computerTypes[i].getSelectedItem();
                    if (!ComputerType.NONE.getName().equals(type)) {
                        gameOptions.getComputerType().addLast(i + "-" + type);
                    }
                }
                gameOptions.setDefaultModules(clientSettings.getDefaultModules()); // TODO im Dialog konfigurierbar machen

                game.play(gameOptions);
            }
        });
        playButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel flowPanel1  = new JPanel(new FlowLayout());
        flowPanel1.add(rotateCCW);
        flowPanel1.add(rotateCW);
        JPanel flowPanel2 = new JPanel(new FlowLayout());
        flowPanel2.add(addButton);
        flowPanel2.add(removeButton);
        JPanel flowPanel3 = new JPanel(new FlowLayout());
        flowPanel3.add(new JLabel("Position:  x: "));
        flowPanel3.add(posx);
        flowPanel3.add(new JLabel("   y: "));
        flowPanel3.add(posy);
        boardChooser.add(flowPanel1, BorderLayout.NORTH);
        buttonPanel.add(flowPanel3, BorderLayout.NORTH);
        buttonPanel.add(flowPanel2, BorderLayout.CENTER);
        buttonPanel.add(playButton, BorderLayout.SOUTH);
        boardChooser.add(buttonPanel, BorderLayout.SOUTH);

        chooserPanel.add(boardChooser, BorderLayout.SOUTH);
        redrawPreview();
        return chooserPanel;
    }

    public Board copy(Board board) {
        if (board == null) {
            return null;
        }
        return objectMapper.convertValue(board, Board.class);
    }

    private void redrawPreview() {
        chooserPanel.remove(boardPreview);
        BufferedImage bi = new BufferedImage(150, 231, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    gr = bi.createGraphics();
        if (boardList.getSelectedIndex() > 0) {
            BoardInfo       boardInfo = (BoardInfo) boardList.getSelectedItem();
            String          bild      = PictureConstants.GFX_LOGO_SMALL;
            AffineTransform af        = AffineTransform.getRotateInstance(rotation * Math.PI / 2, 75, 75);
            gr.drawImage(imageSupplier.getImageIconPlain(bild).getImage(), 0, 0, null);
            gr.translate(0, 81);
            gr.drawImage(Objects.requireNonNull(boardInfo).getImage(), af, null);
        } else {
            gr.drawImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_LOGO_SMALL).getImage(), 0, 0, null);
            gr.translate(0, 81);
            gr.drawImage(imageSupplier.getImageIconPlain(PictureConstants.GFX_PICTURE).getImage(), 0, 0, null);
        }
        boardPreview = new JLabel(imageSupplier.getImageIconPlain(bi));
        chooserPanel.add(boardPreview, BorderLayout.CENTER);
        chooserPanel.validate();
    }

    public void updatePlayersAndWatchers(int players, int watchers) {
        regPlayers.setText(" " + players + " ");
        regWatchers.setText(" " + watchers + " ");
    }

    public void dispose() {
        frame.dispose();
    }

}
