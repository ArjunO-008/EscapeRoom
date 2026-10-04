package interaction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import puzzle.PuzzleManager;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * A wall panel / lock sitting in the world. Same pattern as Safe:
 * no puzzle logic of its own - hands that off to PuzzleManager/Puzzle2.
 * On first solve, drops a Note into the room instead of a key.
 */
public class SymbolLock implements Interactable {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 40;
    private static final int RANGE = 16;

    private final int x;
    private final int y;
    private final int puzzleId;
    private final PuzzleManager puzzleManager;
    private final InteractionSystem interactionSystem;

    private final int noteX;
    private final int noteY;
    private final String noteMessage;

    private BufferedImage sprite;

    private boolean noteDropped = false;

    public SymbolLock(
            int x,
            int y,
            int puzzleId,
            PuzzleManager puzzleManager,
            InteractionSystem interactionSystem,
            int noteX,
            int noteY,
            String noteMessage) {

        this.x = x;
        this.y = y;
        this.puzzleId = puzzleId;
        this.puzzleManager = puzzleManager;
        this.interactionSystem = interactionSystem;
        this.noteX = noteX;
        this.noteY = noteY;
        this.noteMessage = noteMessage;

        sprite = loadSprite();
    }

    private BufferedImage loadSprite() {
    try {
        return ImageIO.read(new File("src/map/resources/SymbolLock.png"));
    } catch (Exception e) {
        System.out.println("Failed to load symbol lock sprite, using placeholder.");
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

        if (!wasSolvedBefore && isSolvedNow && !noteDropped) {
            interactionSystem.addInteractable(new Note(noteX, noteY, noteMessage));
            noteDropped = true;
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void draw(Graphics2D g) {
        if (sprite != null) {
            g.drawImage(sprite, x, y, WIDTH, HEIGHT, null);
        } else {
            g.setColor(Color.CYAN.darker());
            g.fillRect(x, y, WIDTH, HEIGHT);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, WIDTH, HEIGHT);
        }
    }
}