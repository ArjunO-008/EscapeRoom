package core;

import input.InputSystem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import map.MapManager;
import player.Player;

public class GamePanel extends JPanel {

    private Timer gameTimer;

    private Player player;
    private InputSystem inputSystem;

    private MapManager mapManager;

    public GamePanel() {

        /*
         * Game window size
         */
        setPreferredSize(
            new Dimension(960, 640)
        );

        setBackground(Color.BLACK);

        /*
         * Load all maps
         */
        mapManager = new MapManager();

        /*
         * Create player.
         *
         * This is the initial position
         * inside the living room.
         */
        player = new Player(
            400,
            300
        );

        /*
         * Keyboard input
         */
        inputSystem = new InputSystem(this);

        setFocusable(true);

        setupMapControls();

        requestFocusInWindow();

        /*
         * Game loop
         *
         * 16 ms ~= 60 FPS
         */
        gameTimer = new Timer(
            16,
            e -> {

                update();

                repaint();
            }
        );

        gameTimer.start();
    }

    /*
     * Map switching controls.
     *
     * 1 = Bedroom
     * 2 = Kitchen
     * 3 = Living Room
     * 4 = Study
     */
    private void setupMapControls() {

        /*
         * 1 -> Bedroom
         */
        getInputMap().put(
            KeyStroke.getKeyStroke("1"),
            "map-bedroom"
        );

        getActionMap().put(
            "map-bedroom",
            new AbstractAction() {

                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {

                    changeMap(
                        "bedroom",
                        400,
                        300
                    );
                }
            }
        );

        /*
         * 2 -> Kitchen
         */
        getInputMap().put(
            KeyStroke.getKeyStroke("2"),
            "map-kitchen"
        );

        getActionMap().put(
            "map-kitchen",
            new AbstractAction() {

                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {

                    changeMap(
                        "kitchen",
                        400,
                        300
                    );
                }
            }
        );

        /*
         * 3 -> Living Room
         */
        getInputMap().put(
            KeyStroke.getKeyStroke("3"),
            "map-living-room"
        );

        getActionMap().put(
            "map-living-room",
            new AbstractAction() {

                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {

                    changeMap(
                        "living_room",
                        400,
                        300
                    );
                }
            }
        );

        /*
         * 4 -> Study
         */
        getInputMap().put(
            KeyStroke.getKeyStroke("4"),
            "map-study"
        );

        getActionMap().put(
            "map-study",
            new AbstractAction() {

                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {

                    changeMap(
                        "study",
                        400,
                        300
                    );
                }
            }
        );
    }

    /*
     * Change the current map
     * and reposition the player.
     */
    private void changeMap(
        String mapName,
        int playerX,
        int playerY
    ) {

        mapManager.switchMap(
            mapName
        );

        player.setX(playerX);
        player.setY(playerY);
    }

    /*
     * Game update
     */
    private void update() {

        final int speed = 4;

        int dx = 0;
        int dy = 0;

        if (inputSystem.isUp()) {
            dy -= speed;
        }

        if (inputSystem.isDown()) {
            dy += speed;
        }

        if (inputSystem.isLeft()) {
            dx -= speed;
        }

        if (inputSystem.isRight()) {
            dx += speed;
        }

        player.moveBy(
            dx,
            dy,
            getWidth(),
            getHeight()
        );
    }

    /*
     * Rendering
     */
    @Override
    protected void paintComponent(
        Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2 =
            (Graphics2D) g.create();

        /*
         * Draw current map
         */
        mapManager.draw(g2);

        /*
         * Draw player
         */
        g2.setColor(
            player.getColor()
        );

        g2.fillRect(
            player.getX(),
            player.getY(),
            player.getWidth(),
            player.getHeight()
        );

        g2.dispose();
    }
}