package core;

import audio.AudioConfig;
import audio.SoundManager;
import audio.SoundManager.Sfx;
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
import save.SaveData;
import save.SaveManager;
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

    // True once ENTER / SPACE was pressed during the intro (acted on at release).
    private boolean introKeyArmed = false;

    // True while a modal popup (safe keypad, symbol lock, note) is open.
    // Gameplay is paused so the player can't keep walking underneath it.
    private boolean modalActive = false;

    // Footsteps: one step every STEP_INTERVAL_TICKS game ticks (16 ms each)
    // while the player is actually moving.
    private static final int STEP_INTERVAL_TICKS = 14;
    private int stepCooldown = 0;
    private boolean stepAlternate = false;

    // True while Continue is restoring a save, so loadRoom() doesn't
    // overwrite the saved position with the spawn point.
    private boolean restoring = false;

    // Room the player is currently in (saved for Continue).
    private int currentRoomId = 3;

    // Fixed world positions used in more than one place.
    private static final int SAFE_X = 450;
    private static final int SAFE_Y = 300;

    public GamePanel() {

        setPreferredSize(new Dimension(960, 640));
        setBackground(Color.BLACK);

        // Synthesizes the sound effects in the background.
        SoundManager.init();

        // Music plays from the moment the game opens (menu, intro, gameplay).
        SoundManager.startMusic();

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
        inventoryManager.setOnChange(this::saveProgress);
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

        // Main menu: owns its own mouse + keyboard input (click, hover,
        // arrows / W / S, Enter, Esc).
        mainMenu.setListener(new MainMenu.Listener() {
            @Override
            public void onNewGame() {
                // The menu already created the new save file.
                javax.swing.SwingUtilities.invokeLater(() -> startNewGame());
            }

            @Override
            public void onContinue() {
                javax.swing.SwingUtilities.invokeLater(() -> continueGame());
            }

            @Override
            public void onExit() {
                System.exit(0);
            }
        });
        mainMenu.attachTo(this);

        // Intro + victory screen keyboard handling.
        // ENTER is handled on key RELEASE so that holding the key down
        // can never "fall through" into the next screen.
        addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {

                if (gameState == GameState.INTRO
                        && introUI.isAcceptingInput()
                        && (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER
                        || e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE)) {

                    introKeyArmed = true;
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {

                int key = e.getKeyCode();

                // ESC closes the room selector.
                if (key == java.awt.event.KeyEvent.VK_ESCAPE
                        && gameState == GameState.PLAYING
                        && roomSelector.isVisible()) {
                    roomSelector.hide();
                    setCursor(java.awt.Cursor.getDefaultCursor());
                    repaint();
                    return;
                }

                // M toggles all sound.
                if (key == java.awt.event.KeyEvent.VK_M) {
                    SoundManager.setMuted(!SoundManager.isMuted());
                    return;
                }

                boolean confirm = key == java.awt.event.KeyEvent.VK_ENTER
                        || key == java.awt.event.KeyEvent.VK_SPACE;

                if (gameState == GameState.INTRO && introKeyArmed && confirm) {

                    introKeyArmed = false;

                    if (introUI.isFinished()) {
                        beginPlaying();
                    } else {
                        introUI.skipTyping();
                    }

                    return;
                }

                if (gameState == GameState.WIN
                        && key == java.awt.event.KeyEvent.VK_ENTER) {

                    if (winScreen.isReady()) {
                        returnToMenu();
                    } else {
                        // Skip the door cinematic.
                        winScreen.skip();
                    }
                }
            }
        });

        // Mouse handling
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {

                // Victory screen: clicking the button returns to the menu.
                if (gameState == GameState.WIN) {

                    if (winScreen.isButtonClicked(
                            e.getX(),
                            e.getY(),
                            getWidth())) {

                        returnToMenu();

                    } else if (!winScreen.isReady()) {

                        // Click skips the door cinematic.
                        winScreen.skip();
                    }

                    return;
                }

                // Handle the introduction screen first.
                if (gameState == GameState.INTRO) {

                    if (introUI.isBeginButtonClicked(
                            e.getX(),
                            e.getY())) {

                        beginPlaying();

                    } else if (!introUI.isFinished()
                            && introUI.isAcceptingInput()) {

                        // Click skips the typing.
                        introUI.skipTyping();
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

                    if (!roomSelector.isVisible()) {
                        setCursor(java.awt.Cursor.getDefaultCursor());
                    }

                    repaint();
                }
            }
        });

        // Hover handling (victory button + room selector)
        addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {

                // Intro screen button hover.
                if (gameState == GameState.INTRO) {

                    introUI.handleMouseMove(e.getX(), e.getY());

                    setCursor(introUI.isHovering()
                            ? java.awt.Cursor.getPredefinedCursor(
                                    java.awt.Cursor.HAND_CURSOR)
                            : java.awt.Cursor.getDefaultCursor());

                    return;
                }

                // Victory screen button hover.
                if (gameState == GameState.WIN) {

                    winScreen.handleMouseMove(
                            e.getX(),
                            e.getY(),
                            getWidth());

                    setCursor(winScreen.isHovering()
                            ? java.awt.Cursor.getPredefinedCursor(
                                    java.awt.Cursor.HAND_CURSOR)
                            : java.awt.Cursor.getDefaultCursor());

                    return;
                }

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

                    setCursor(roomSelector.isHovering()
                            ? java.awt.Cursor.getPredefinedCursor(
                                    java.awt.Cursor.HAND_CURSOR)
                            : java.awt.Cursor.getDefaultCursor());

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

        // Fresh run: no puzzle progress and no items carried over.
        inventoryManager.clear();
        puzzleManager.resetPuzzles();

        // Load the starting room (also writes the fresh save).
        loadRoom(3);

        // Show the story before allowing movement.
        introKeyArmed = false;
        introUI.start();
        gameState = GameState.INTRO;

        requestFocusInWindow();
        repaint();

        System.out.println("Introduction displayed.");
    }

    // --------------------------------------------------
    // Continue a saved game
    // --------------------------------------------------

    private void continueGame() {

        System.out.println("Continuing game...");

        SaveData data = SaveManager.load();

        // The menu already checks this, but never trust a file blindly.
        if (data == null) {
            System.out.println("Continue failed: save could not be read.");
            gameState = GameState.MENU;
            mainMenu.show();
            repaint();
            return;
        }

        gameState = GameState.LOADING;

        loadingScreen.show();

        // Restore progress BEFORE building the room, so the key is not
        // spawned again if the player already picked it up.
        puzzleManager.reloadFromSave();
        inventoryManager.setItems(data.getInventory());

        int roomId = data.getCurrentRoomId();

        if (roomId < 1 || roomId > 3) {
            roomId = 3;
        }

        restoring = true;

        try {
            loadRoom(roomId);
        } finally {
            restoring = false;
        }

        // Put the player back where they were, if that spot is valid.
        int savedX = Math.round(data.getPlayerX());
        int savedY = Math.round(data.getPlayerY());

        if (player.canStandAt(
                savedX,
                savedY,
                mapManager.getCurrentRoomConfig(),
                mapManager.getCurrentMap())) {

            player.setX(savedX);
            player.setY(savedY);
        }

        inputSystem.reset();

        gameState = GameState.PLAYING;

        SoundManager.startMusic();

        requestFocusInWindow();
        repaint();
    }

    // --------------------------------------------------
    // Save progress (room, position, inventory)
    // --------------------------------------------------

    // Puzzle progress is written separately by PuzzleManager; this keeps
    // those values and updates everything else.
    public void saveProgress() {

        SaveData data = SaveManager.load();

        // No save file (e.g. it was deleted when the game was won).
        if (data == null) {
            return;
        }

        data.setCurrentRoomId(currentRoomId);
        data.setPlayerX(player.getX());
        data.setPlayerY(player.getY());
        data.setInventory(inventoryManager.getItemIds());

        SaveManager.save(data);
    }

    // Called when the window is closing.
    public void saveOnExit() {

        if (gameState == GameState.PLAYING) {
            saveProgress();
        }
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

        currentRoomId = roomId;

        interactionSystem.setInteractables(
                getInteractablesForRoom(roomId)
        );

        if (!restoring) {
            saveProgress();
        }
    }

    // --------------------------------------------------
    // Win condition
    // --------------------------------------------------

    private void winGame() {

        gameState = GameState.WIN;

        // Let the victory cinematic have the soundscape to itself.
        SoundManager.stopMusic(AudioConfig.MUSIC_FADE_OUT_MS);

        // Starts the door-opening cinematic + status window and deletes the save file.
        winScreen.show();

        repaint();

        System.out.println("Game completed!");
    }

    // --------------------------------------------------
    // Leave the intro and start playing
    // --------------------------------------------------

    private void beginPlaying() {

        introUI.finish();

        setCursor(java.awt.Cursor.getDefaultCursor());

        gameState = GameState.PLAYING;

        SoundManager.startMusic();

        requestFocusInWindow();
        repaint();
    }

    // --------------------------------------------------
    // Return to the main menu (from the victory screen)
    // --------------------------------------------------

    private void returnToMenu() {

        System.out.println("Returning to main menu...");

        SoundManager.play(Sfx.CLICK);
        SoundManager.fadeOut(Sfx.OUTDOORS, 600);

        // Back on the menu: the music comes back (it faded out for the win scene).
        SoundManager.startMusic();

        setCursor(java.awt.Cursor.getDefaultCursor());

        gameState = GameState.MENU;

        // Re-checks the save file (it was deleted when the game was won),
        // so "Continue" is no longer offered.
        mainMenu.show();

        requestFocusInWindow();
        repaint();
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

                list.add(createLivingRoomNote());
            }
        }

        // ==============================================
        // BEDROOM
        // ==============================================

        if (roomId == 1) {

            list.add(
                    new Safe(
                            SAFE_X,
                            SAFE_Y,
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
                            () -> goThroughDoor(3)
                    )
            );

            if (puzzleManager.isPuzzleSolved(1)
                    && !inventoryManager.hasItem(Key.ITEM_ID)) {

                list.add(
                        new Key(
                                Safe.keyDropX(SAFE_X),
                                SAFE_Y,
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
                            () -> goThroughDoor(3)
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

        if (mapName.equals("living_room") && currentRoomId != 3) {

            // Returning to living room from another room
            player.setX(528);
            player.setY(230);

        } else {

            map.RoomConfig roomConfig =
                    mapManager.getCurrentRoomConfig();

            player.setX(roomConfig.getSpawnX());
            player.setY(roomConfig.getSpawnY());
        }

        repaint();
    }

    // --------------------------------------------------
    // Update game
    // --------------------------------------------------

    // --------------------------------------------------
    // Sound helpers
    // --------------------------------------------------

    // Plays a footstep every STEP_INTERVAL_TICKS while walking, alternating
    // the level slightly so it sounds like left / right feet.
    private void updateFootsteps() {

        if (!player.isMoving()) {
            stepCooldown = 0; // the next step sounds right when walking starts
            return;
        }

        if (--stepCooldown <= 0) {
            SoundManager.play(Sfx.WALK, stepAlternate ? 0.85 : 1.0);
            stepAlternate = !stepAlternate;
            stepCooldown = STEP_INTERVAL_TICKS;
        }
    }

    // Leaves through a door that goes straight to another room: the door
    // opened (RoomDoor plays that), and a moment later it shuts behind us.
    private void goThroughDoor(int roomId) {

        loadRoom(roomId);

        Timer closeTimer = new Timer(600, e -> SoundManager.play(Sfx.SHUT));
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    private void update() {

        // Do not update gameplay during the intro,
        // loading screen, menu, or victory screen.
        if (gameState != GameState.PLAYING) {
            return;
        }

        // A popup is open: the timer keeps ticking inside the popup's own
        // event loop, so freeze the game until it closes.
        if (modalActive) {
            return;
        }

        boolean selectorOpen = roomSelector.isVisible();

        boolean horizontal = inputSystem.isLeft() != inputSystem.isRight();
        boolean vertical = inputSystem.isUp() != inputSystem.isDown();

        // 4 px straight, 3 px per axis on a diagonal (3*sqrt2 = 4.2),
        // so diagonal movement isn't ~41% faster.
        final int speed = (horizontal && vertical) ? 2 : 2;

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

        // Standing still while the room selector is open.
        if (selectorOpen) {
            dx = 0;
            dy = 0;
        }

        player.moveBy(
                dx,
                dy,
                mapManager.getCurrentRoomConfig(),
                mapManager.getCurrentMap()
        );

        player.update();

        updateFootsteps();

        interactionSystem.update(player);

        if (inputSystem.consumeInteract() && !selectorOpen) {

            modalActive = true;

            try {
                // May open a modal popup; blocks here until it closes.
                interactionSystem.tryInteract();
            } finally {
                modalActive = false;

                // Key releases that happened inside the popup never reached
                // us, so forget any "held" keys.
                inputSystem.reset();
            }

            if (gameState == GameState.PLAYING) {
                saveProgress();
            }
        }

        int selectedRoomId =
                roomSelector.getSelectedRoomId();

        if (selectedRoomId != -1) {
            loadRoom(selectedRoomId);
            SoundManager.play(Sfx.SHUT); // door closes behind the player
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

                    if (!roomSelector.isVisible()) {
                        interactionSystem.drawPrompt(
                                g2,
                                getWidth(),
                                getHeight()
                        );
                    }

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
