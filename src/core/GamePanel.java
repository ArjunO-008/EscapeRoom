package core;

import input.InputSystem;
import interaction.Door;
import interaction.Interactable;
import interaction.InteractionSystem;
import interaction.RoomDoor;
import interaction.Safe;
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

        private LoadingScreen loadingScreen;
        private MainMenu mainMenu;
        private WinScreen winScreen;

        private InfoUI infoUI;

        private RoomSelector roomSelector;

        private PuzzleManager puzzleManager;
        private InventoryUI inventoryUI;

        private Player player;
        private InputSystem inputSystem;

        private MapManager mapManager;

        private InventoryManager inventoryManager;
        private InteractionSystem interactionSystem;

        private GameState gameState;

        public GamePanel() {
                setPreferredSize(new Dimension(960, 640));
                setBackground(Color.BLACK);

                // System.out.println("========== NEW GAME TEST ==========");

                // SaveManager.createNewSave();

                // SaveData data = SaveManager.load();

                // System.out.println("Continuity: "
                // + data.isContinuity());

                // System.out.println("Player X: "
                // + data.getPlayerX());

                // System.out.println("Player Y: "
                // + data.getPlayerY());

                // System.out.println("Room ID: "
                // + data.getCurrentRoomId());

                // System.out.println("Inventory size: "
                // + data.getInventory().length);

                // System.out.println("Puzzle 1 solved: "
                // + data.getPuzzlesCompleted()[0]);

                // System.out.println("===================================");

                // UI screens
                loadingScreen = new LoadingScreen();
                mainMenu = new MainMenu();
                winScreen = new WinScreen();
                infoUI = new InfoUI();

                gameState = GameState.PLAYING;
                roomSelector = new RoomSelector();

                // Inventory + interactable objects (key, door, etc.)
                inventoryManager = new InventoryManager(infoUI);
                interactionSystem = new InteractionSystem();
                inventoryUI = new InventoryUI(inventoryManager);

                // Puzzle setup
                puzzleManager = new PuzzleManager(infoUI);
                // puzzleManager.openPuzzle(1); // .openPuzzle(ID)

                // Player
                player = new Player(0, 0);

                // Maps
                mapManager = new MapManager();
                loadRoom(3);

                /*
                 * loadRoom(ID);
                 * ID:
                 * 1 = Bedroom,
                 * 2 = Kitchen,
                 * 3 = Living Room.
                 */

                infoUI.show("Puzzle 2 completed.");

                // Test Room Selector
                // roomSelector.show(2);

                // Input handling (movement only)
                inputSystem = new InputSystem(this);
                setFocusable(true);

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

                addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                                if (roomSelector.isVisible()) {
                                        roomSelector.handleClick(e.getX(), e.getY(), getWidth(), getHeight());
                                        repaint();
                                }
                        }
                });
                addMouseMotionListener(new MouseAdapter() {

                        @Override
                        public void mouseMoved(MouseEvent e) {

                                if (roomSelector.isVisible()) {

                                        roomSelector.handleMouseMove(
                                                        e.getX(),
                                                        e.getY(),
                                                        getWidth(),
                                                        getHeight());

                                        repaint();
                                }
                        }
                });

                // Game loop (~60 FPS)
                gameTimer = new Timer(16, e -> {
                        update();
                        repaint();
                });
                gameTimer.start();
        }

        private void startGame() {

                System.out.println("Starting new game...");

                gameState = GameState.LOADING;

                loadingScreen.show();

                /*
                * Start the new game.
                */
                mainMenu.startNewGame();

                /*
                * Start in the Living Room.
                */
                loadRoom(3);
        }

        private void startNewGame() {

                System.out.println("Starting new game...");

                gameState = GameState.LOADING;

                loadingScreen.show();

                mainMenu.startNewGame();

                /*
                * Start in the Living Room.
                */
                loadRoom(3);

                gameState = GameState.PLAYING;

                System.out.println("Loading complete.");
        }

        public void loadRoom(int roomId) {

                String mapName = switch (roomId) {

                        case 1 -> "bedroom";
                        case 2 -> "kitchen";
                        case 3 -> "living_room";
                        default ->
                                throw new IllegalArgumentException("Unknown room ID: " + roomId);
                };

                changeMap(mapName);

                interactionSystem.setInteractables(
                        getInteractablesForRoom(roomId)
                );
        }

        private void winGame() {

                gameState = GameState.WIN;

                winScreen.show();

                System.out.println(
                        "Game completed!"
                );
                }

        private List<Interactable> getInteractablesForRoom(int roomId) {

                List<Interactable> list =
                        new ArrayList<>();

                /*
                * ========================================================
                * LIVING ROOM
                * ========================================================
                */

                if (roomId == 3) {
                        list.add(
                                new RoomDoor(
                                        528,
                                        193,
                                        32,
                                        64,
                                        20,
                                        () -> {

                                                roomSelector.show(3);

                                        }
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
                }

                if (roomId == 1) {

                        list.add(
                                new SymbolLock(
                                        400,
                                        370,
                                        2,
                                        puzzleManager,
                                        interactionSystem,
                                        630,
                                        330,
                                        "A note on the table reads: 4719"
                                )
                        );

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
                                        () -> {

                                                loadRoom(3);

                                        }
                                )
                        );
                }

                if (roomId == 2) {

                        list.add(
                                new RoomDoor(
                                        609,
                                        381,
                                        38,
                                        95,
                                        30,
                                        () -> {

                                                loadRoom(3);

                                        }
                                )
                        );
                }

                return list;
        }

        /** Switches the active room and repositions the player. */
        private void changeMap(String mapName) {

                System.out.println("Changing room to: " + mapName);

                mapManager.switchMap(mapName);

                /*
                * Get the configuration of the newly loaded room.
                */
                map.RoomConfig roomConfig = mapManager.getCurrentRoomConfig();

                /*
                * Spawn the player at the room's configured spawn point.
                */
                player.setX(roomConfig.getSpawnX());

                player.setY(roomConfig.getSpawnY());

                repaint();
        }

        /** Per-frame update: reads input and moves the player. */
        private void update() {

                if (gameState != GameState.PLAYING) {
                        return;
                }

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

        /** Renders the current room and the player. */
        @Override
        protected void paintComponent(Graphics g) {

        super.paintComponent(g);

                Graphics2D g2 =
                        (Graphics2D) g.create();

                if (gameState == GameState.PLAYING) {

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
                }

        g2.dispose();
        }

}