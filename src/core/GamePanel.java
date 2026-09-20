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
import puzzle.PuzzleManager;
import ui.*;

public class GamePanel extends JPanel {

    private Timer gameTimer;

    private LoadingScreen loadingScreen;
    private MainMenu mainMenu;
    private WinScreen winScreen;

    private PuzzleManager puzzleManager;

    private Player player;
    private InputSystem inputSystem;

    private MapManager mapManager;

    public GamePanel() {
        setPreferredSize(new Dimension(960, 640));
        setBackground(Color.BLACK);

        // UI screens
        loadingScreen = new LoadingScreen();
        mainMenu = new MainMenu();
        winScreen = new WinScreen();

        // Use Commands and Use For testing Out each UI Screens.
        loadingScreen.show();
        mainMenu.show();
        winScreen.show();

        // Puzzle setup
        puzzleManager = new PuzzleManager();
        puzzleManager.openPuzzle(1); // .openPuzzle(ID)

        // Player
        player = new Player(100, 100);

        // Maps
        mapManager = new MapManager();
        /*
         * loadRoom(ID);
         * ID:
         * 1 = Bedroom,
         * 2 = Kitchen,
         * 3 = Living Room.
         */
        loadRoom(1);

        

        // Input handling (movement only)
        inputSystem = new InputSystem(this);
        setFocusable(true);

        // Game loop (~60 FPS)
        gameTimer = new Timer(16, e -> {
            update();
            repaint();
        });
        gameTimer.start();
    }

    public void loadRoom(int roomId) {
        String mapName = switch (roomId) {
            case 1 -> "bedroom";
            case 2 -> "kitchen";
            case 3 -> "living_room";
            default -> throw new IllegalArgumentException("Unknown room ID: " + roomId);
        };

        changeMap(mapName, 100, 100);
    }

    /** Switches the active room and repositions the player. */
    private void changeMap(String mapName, int playerX, int playerY) {
        System.out.println("Changing room to: " + mapName);

        mapManager.switchMap(mapName);
        player.setX(playerX);
        player.setY(playerY);

        repaint();
    }

    /** Per-frame update: reads input and moves the player. */
    private void update() {
        final int speed = 4;

        int dx = 0;
        int dy = 0;

        if (inputSystem.isUp())
            dy -= speed;
        if (inputSystem.isDown())
            dy += speed;
        if (inputSystem.isLeft())
            dx -= speed;
        if (inputSystem.isRight())
            dx += speed;

        player.moveBy(dx, dy, getWidth(), getHeight());
    }

    /** Renders the current room and the player. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        mapManager.draw(g2);

        g2.setColor(player.getColor());
        g2.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        g2.dispose();
    }
}