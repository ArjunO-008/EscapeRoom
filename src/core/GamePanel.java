package core;

import input.InputSystem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import javax.swing.Timer;
import map.MapManager;
import player.Player;

public class GamePanel extends JPanel {

    private Timer gameTimer;

    private Player player;
    private InputSystem inputSystem;

    private MapManager mapManager;

    public GamePanel() {

        setPreferredSize(
            new Dimension(960, 640)
        );

        setBackground(Color.BLACK);

        /*
         * Load all available maps
         */
        mapManager = new MapManager();

        /*
         * Player starting position
         */
        player = new Player(400, 300);

        /*
         * Keyboard input
         */
        inputSystem = new InputSystem(this);

        setFocusable(true);

        requestFocusInWindow();

        /*
         * Game loop
         */
        gameTimer = new Timer(16, e -> {

            update();

            repaint();

        });

        gameTimer.start();
    }

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

    @Override
    protected void paintComponent(Graphics g) {

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