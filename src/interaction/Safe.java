package interaction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import inventory.InventoryManager;
import puzzle.PuzzleManager;
import ui.InfoUI;

/**
 * The safe sitting in the world. Doesn't hold any code-checking logic itself -
 * it hands that off to PuzzleManager/Puzzle1. What it DOES own: dropping a
 * physical Key into the room the moment the puzzle becomes solved, so the
 * player has to walk over and pick it up rather than receiving it instantly.
 */
public class Safe implements Interactable {

    private static final int WIDTH = 35;
    private static final int HEIGHT = 40;
    private static final int RANGE = 16;

    private final int x;
    private final int y;
    private final int puzzleId;
    private final PuzzleManager puzzleManager;
    private final InventoryManager inventoryManager;
    private final InteractionSystem interactionSystem;

    private boolean keyDropped = false;
    private BufferedImage sprite;

    private final InfoUI infoUI;

    public Safe(
            int x,
            int y,
            int puzzleId,
            PuzzleManager puzzleManager,
            InventoryManager inventoryManager,
            InteractionSystem interactionSystem,
            InfoUI infoUI) {

        this.x = x;
        this.y = y;
        this.puzzleId = puzzleId;
        this.puzzleManager = puzzleManager;
        this.inventoryManager = inventoryManager;
        this.interactionSystem = interactionSystem;
        this.infoUI = infoUI;

        sprite = loadSprite();
    }

    private BufferedImage loadSprite() {
        try {
            return ImageIO.read(new File("src/map/resources/Safe.png"));
        } catch (Exception e) {
            System.out.println("Failed to load safe sprite, using placeholder.");
            return null;
        }
    }

    @Override
    public Rectangle getInteractionBounds() {
        return new Rectangle(
                x - RANGE,
                y - RANGE,
                WIDTH + RANGE * 2,
                HEIGHT + RANGE * 2);
    }

    @Override
    public void interact() {
        boolean wasSolvedBefore = puzzleManager.isPuzzleSolved(puzzleId);

        puzzleManager.openPuzzle(puzzleId);

        boolean isSolvedNow = puzzleManager.isPuzzleSolved(puzzleId);

        // Only drop the key the moment it FIRST becomes solved,
        // not every time someone presses E on an already-solved safe.
        if (!wasSolvedBefore && isSolvedNow && !keyDropped) {
            interactionSystem.addInteractable(
                    new Key(
                            x + WIDTH + 10,
                            y,
                            inventoryManager,
                            infoUI));
            keyDropped = true;
        }
    }

    @Override
    public boolean isActive() {
        return true; // stays in the world whether solved or not
    }

    @Override
    public void draw(Graphics2D g) {
        if (sprite != null) {
            g.drawImage(sprite, x, y, WIDTH, HEIGHT, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, y, WIDTH, HEIGHT);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, WIDTH, HEIGHT);
        }
    }
}