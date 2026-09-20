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
import ui.LoadingScreen;
import ui.MainMenu;
import ui.WinScreen;

public class GamePanel extends JPanel {

    private Timer gameTimer;

    private LoadingScreen loadingScreen;
    private MainMenu mainMenu;
    private WinScreen winScreen;
       

    private Player player;
    private InputSystem inputSystem;

    private MapManager mapManager;

    public GamePanel() {

        setPreferredSize(
            new Dimension(960, 640)
        );

        setBackground(Color.BLACK);

        loadingScreen = new LoadingScreen();
        mainMenu = new MainMenu();
        winScreen = new WinScreen();

        loadingScreen.show();
        mainMenu.show();
        winScreen.show();

        /*
         * Load all maps
         */
        mapManager = new MapManager();

        /*
         * Create player
         */
        player = new Player(
            100,
            100
        );

        /*
         * Keyboard movement
         */
        inputSystem = new InputSystem(this);

        setFocusable(true);

        /*
         * Setup room switching
         */
        setupMapControls();

        /*
         * Start game loop
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
     * =========================================================
     * MAP CONTROLS
     * =========================================================
     *
     * 1 = Bedroom
     * 2 = Kitchen
     * 3 = Living Room
     *
     * WHEN_IN_FOCUSED_WINDOW means the keys work while
     * the game window is active, even if the JPanel itself
     * does not have keyboard focus.
     */
    private void setupMapControls() {

        /*
         * 1 -> Bedroom
         */
        getInputMap(
            WHEN_IN_FOCUSED_WINDOW
        ).put(
            KeyStroke.getKeyStroke("1"),
            "switch-bedroom"
        );

        getActionMap().put(
            "switch-bedroom",
            new AbstractAction() {

                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {

                    changeMap(
                        "bedroom",
                        100,
                        100
                    );
                }
            }
        );

        /*
         * 2 -> Kitchen
         */
        getInputMap(
            WHEN_IN_FOCUSED_WINDOW
        ).put(
            KeyStroke.getKeyStroke("2"),
            "switch-kitchen"
        );

        getActionMap().put(
            "switch-kitchen",
            new AbstractAction() {

                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {

                    changeMap(
                        "kitchen",
                        100,
                        100
                    );
                }
            }
        );

        /*
         * 3 -> Living Room
         */
        getInputMap(
            WHEN_IN_FOCUSED_WINDOW
        ).put(
            KeyStroke.getKeyStroke("3"),
            "switch-living-room"
        );

        getActionMap().put(
            "switch-living-room",
            new AbstractAction() {

                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {

                    changeMap(
                        "living_room",
                        100,
                        100
                    );
                }
            }
        );

    }

    /*
     * Change room and reposition player
     */
    private void changeMap(
        String mapName,
        int playerX,
        int playerY
    ) {

        System.out.println(
            "Changing room to: " + mapName
        );

        mapManager.switchMap(mapName);

        player.setX(playerX);
        player.setY(playerY);

        repaint();
    }

    /*
     * =========================================================
     * GAME UPDATE
     * =========================================================
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
     * =========================================================
     * DRAWING
     * =========================================================
     */
    @Override
    protected void paintComponent(
        Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2 =
            (Graphics2D) g.create();

        /*
         * Draw current room
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