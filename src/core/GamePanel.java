
package core;

import input.InputSystem;
import interaction.Door;
import interaction.Interactable;
import interaction.InteractionSystem;
import interaction.RoomDoor;
import interaction.Safe;
import interaction.Note;
import interaction.Key;
import interaction.SymbolLock;
import inventory.InventoryManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import map.MapManager;
import player.Player;
import puzzle.PuzzleManager;
import ui.*;

public class GamePanel extends JPanel {

    private Timer gameTimer;

    // UI screens
    private LoadingScreen loadingScreen;
    private MainMenu mainMenu;
    private WinScreen winScreen;
    private InfoUI infoUI;
    private IntroUI introUI;
    private RoomSelector roomSelector;

    // Puzzle and inventory systems
    private PuzzleManager puzzleManager;
    private InventoryUI inventoryUI;
    private InventoryManager inventoryManager;

    // Player and input
    private Player player;
    private InputSystem inputSystem;

    // Map and interaction systems
    private MapManager mapManager;
    private InteractionSystem interactionSystem;

    // Current game state
    private GameState gameState;

    public GamePanel() {

        setPreferredSize(new Dimension(960, 640));
        setBackground(Color.BLACK);

        // Initialize UI screens
        loadingScreen = new LoadingScreen();
        mainMenu = new MainMenu();
        winScreen = new WinScreen();
        infoUI = new InfoUI();
        introUI = new IntroUI();
        roomSelector = new RoomSelector();

        gameState = GameState.MENU;
        mainMenu.show();

        // Inventory and interaction systems
        inventoryManager = new InventoryManager(infoUI);
        interactionSystem = new InteractionSystem();
        inventoryUI = new InventoryUI(inventoryManager);

        // Puzzle system
        puzzleManager = new PuzzleManager(infoUI);

        // Player
        player = new Player(0, 0);

        // Map manager
        mapManager = new MapManager();

        // Input handling
        inputSystem = new InputSystem(this);

        setFocusable(true);
        requestFocusInWindow();

        // Keyboard handling
        addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {

                if (gameState == GameState.MENU
                        && e.getKeyCode()
                        == java.awt.event.KeyEvent.VK_ENTER) {

                    startNewGame();
                }
            }
        });

        // Mouse handling
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {

                // Handle the introduction screen first.
                if (gameState == GameState.INTRO) {

                    if (introUI.isBeginButtonClicked(
                            e.getX(),
                            e.getY())) {

                        gameState = GameState.PLAYING;

                        requestFocusInWindow();
                        repaint();
                    }

                    return;
                }

                // Ignore clicks outside gameplay.
                if (gameState != GameState.PLAYING) {
                    return;
                }

                // Handle room selector clicks.
                if (roomSelector.isVisible()) {

                    roomSelector.handleClick(
                            e.getX(),
                            e.getY(),
                            getWidth(),
                            getHeight()
                    );

                    repaint();
                }
            }
        });

        // Room selector hover handling
        addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {

                if (gameState != GameState.PLAYING) {
                    return;
                }

                if (roomSelector.isVisible()) {

                    roomSelector.handleMouseMove(
                            e.getX(),
                            e.getY(),
                            getWidth(),
                            getHeight()
                    );

                    repaint();
                }
            }
        });

        // Game loop: approximately 60 FPS
        gameTimer = new Timer(16, e -> {
            update();
            repaint();
        });

        gameTimer.start();
    }

    // --------------------------------------------------
    // Start a new game
    // --------------------------------------------------

    private void startNewGame() {

        System.out.println("Starting new game...");

        gameState = GameState.LOADING;

        loadingScreen.show();

        mainMenu.startNewGame();

        puzzleManager.resetPuzzles();

        // Load the starting room.
        loadRoom(3);

        // Show the story before allowing movement.
        gameState = GameState.INTRO;

        requestFocusInWindow();
        repaint();

        System.out.println("Introduction displayed.");
    }

    // --------------------------------------------------
    // Load a room
    // --------------------------------------------------

    public void loadRoom(int roomId) {

        String mapName = switch (roomId) {

            case 1 -> "bedroom";
            case 2 -> "kitchen";
            case 3 -> "living_room";

            default -> throw new IllegalArgumentException(
                    "Unknown room ID: " + roomId
            );
        };

        changeMap(mapName);

        interactionSystem.setInteractables(
                getInteractablesForRoom(roomId)
        );
    }

    // --------------------------------------------------
    // Win condition
    // --------------------------------------------------

    private void winGame() {

        gameState = GameState.WIN;

        winScreen.show();

        repaint();

        System.out.println("Game completed!");
    }

    // --------------------------------------------------
    // Create puzzle note
    // --------------------------------------------------

    private Note createLivingRoomNote() {

        return new Note(
                370,
                200,
                "Digit 3 of 4: 1",
                "third_digit_1.png"
        );
    }

    // --------------------------------------------------
    // Room interactables
    // --------------------------------------------------

    private List<Interactable> getInteractablesForRoom(
            int roomId) {

        List<Interactable> list = new ArrayList<>();

        // ==============================================
        // LIVING ROOM
        // ==============================================

        if (roomId == 3) {

            list.add(
                    new RoomDoor(
                            528,
                            193,
                            32,
                            64,
                            20,
                            () -> roomSelector.show(3)
                    )
            );

            list.add(
                    new Door(
                            430,
                            450,
                            inventoryManager,
                            infoUI,
                            this::winGame
                    )
            );

            list.add(
                    new SymbolLock(
                            610,
                            320,
                            2,
                            puzzleManager,
                            () -> interactionSystem.addInteractable(
                                    createLivingRoomNote()
                            )
                    )
            );

            if (puzzleManager.isPuzzleSolved(2)) {

                list.add(
                        new Note(
                                370,
                                200,
                                "Digit 3 of 4: 1",
                                "third_digit_1.png"
                        )
                );

                list.add(createLivingRoomNote());
            }
        }

        // ==============================================
        // BEDROOM
        // ==============================================

        if (roomId == 1) {

            list.add(
                    new Safe(
                            450,
                            300,
                            1,
                            puzzleManager,
                            inventoryManager,
                            interactionSystem,
                            infoUI
                    )
            );

            list.add(
                    new RoomDoor(
                            401,
                            490,
                            110,
                            65,
                            30,
                            () -> loadRoom(3)
                    )
            );

            if (puzzleManager.isPuzzleSolved(1)
                    && !inventoryManager.hasItem(Key.ITEM_ID)) {

                list.add(
                        new Key(
                                484,
                                300,
                                inventoryManager,
                                infoUI
                        )
                );
            }

            if (puzzleManager.isPuzzleSolved(2)) {

                list.add(
                        new Note(
                                630,
                                320,
                                "Digit 1 of 4: 4",
                                "first_digit_4.png"
                        )
                );
            }
        }

        // ==============================================
        // KITCHEN
        // ==============================================

        if (roomId == 2) {

            list.add(
                    new RoomDoor(
                            609,
                            381,
                            38,
                            95,
                            30,
                            () -> loadRoom(3)
                    )
            );

            if (puzzleManager.isPuzzleSolved(2)) {

                list.add(
                        new Note(
                                400,
                                350,
                                "Digit 2 of 4: 7",
                                "second_digit_7.png"
                        )
                );

                list.add(
                        new Note(
                                430,
                                230,
                                "Digit 4 of 4: 9",
                                "fourth_digit_9.png"
                        )
                );
            }
        }

        return list;
    }

    // --------------------------------------------------
    // Change map
    // --------------------------------------------------

    private void changeMap(String mapName) {

        System.out.println("Changing room to: " + mapName);

        mapManager.switchMap(mapName);

        map.RoomConfig roomConfig =
                mapManager.getCurrentRoomConfig();

        player.setX(roomConfig.getSpawnX());
        player.setY(roomConfig.getSpawnY());

        repaint();
    }

    // --------------------------------------------------
    // Update game
    // --------------------------------------------------

    private void update() {

        // Do not update gameplay during the intro,
        // loading screen, menu, or victory screen.
        if (gameState != GameState.PLAYING) {
            return;
        }

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
                mapManager.getCurrentRoomConfig(),
                mapManager.getCurrentMap()
        );

        player.update();

        interactionSystem.update(player);

        if (inputSystem.consumeInteract()) {
            interactionSystem.tryInteract();
        }

        int selectedRoomId =
                roomSelector.getSelectedRoomId();

        if (selectedRoomId != -1) {
            loadRoom(selectedRoomId);
        }

        infoUI.update();
    }

    // --------------------------------------------------
    // Loading screen fallback
    // --------------------------------------------------

    private void drawLoadingFallback(Graphics2D g) {

        g.setColor(new Color(25, 18, 14));

        g.fillRect(
                0,
                0,
                getWidth(),
                getHeight()
        );

        g.setColor(new Color(235, 218, 180));

        g.setFont(
                new java.awt.Font(
                        "Serif",
                        java.awt.Font.BOLD,
                        36
                )
        );

        String text = "LOADING...";

        int textWidth =
                g.getFontMetrics().stringWidth(text);

        g.drawString(
                text,
                (getWidth() - textWidth) / 2,
                getHeight() / 2
        );
    }

    // --------------------------------------------------
    // Victory screen fallback
    // --------------------------------------------------

    private void drawWinFallback(Graphics2D g) {

        g.setColor(new Color(25, 18, 14));

        g.fillRect(
                0,
                0,
                getWidth(),
                getHeight()
        );

        g.setColor(new Color(235, 218, 180));

        g.setFont(
                new java.awt.Font(
                        "Serif",
                        java.awt.Font.BOLD,
                        50
                )
        );

        String text = "YOU ESCAPED!";

        int textWidth =
                g.getFontMetrics().stringWidth(text);

        g.drawString(
                text,
                (getWidth() - textWidth) / 2,
                getHeight() / 2
        );
    }

    // --------------------------------------------------
    // Render current screen
    // --------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        try {

            switch (gameState) {

                case MENU:

                    mainMenu.draw(
                            g2,
                            getWidth(),
                            getHeight()
                    );

                    break;

                case LOADING:

                    drawLoadingFallback(g2);

                    break;

                case INTRO:

                    // Render the room behind the introduction.
                    mapManager.draw(g2);

                    interactionSystem.drawObjects(g2);

                    player.draw(g2);

                    // Render the story overlay.
                    introUI.draw(
                            g2,
                            getWidth(),
                            getHeight()
                    );

                    break;

                case PLAYING:

                    mapManager.draw(g2);

                    roomSelector.draw(
                            g2,
                            getWidth(),
                            getHeight()
                    );

                    interactionSystem.drawObjects(g2);

                    player.draw(g2);

                    interactionSystem.drawPrompt(
                            g2,
                            getWidth(),
                            getHeight()
                    );

                    infoUI.draw(
                            g2,
                            getWidth(),
                            getHeight()
                    );

                    inventoryUI.draw(
                            g2,
                            getHeight()
                    );

                    break;

                case WIN:

                    winScreen.draw(
                            g2,
                            getWidth(),
                            getHeight()
                    );

                    break;
            }

        } finally {
            g2.dispose();
        }
    }
}
