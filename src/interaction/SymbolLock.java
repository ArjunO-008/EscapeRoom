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

    private static final int WIDTH = 16;
    private static final int HEIGHT = 20;
    private static final int RANGE = 16;

        private final int x;
    private final int y;
    private final int puzzleId;
    private final PuzzleManager puzzleManager;
    private final Runnable onSolved;

    private BufferedImage sprite;

    public SymbolLock(
        int x,
        int y,
        int puzzleId,
        PuzzleManager puzzleManager,
        Runnable onSolved) {

    this.x = x;
    this.y = y;
    this.puzzleId = puzzleId;
    this.puzzleManager = puzzleManager;
    this.onSolved = onSolved;

    sprite = loadSprite();
}
    

    private BufferedImage loadSprite() {
    try {
        return core.Assets.image("src/map/resources/SymbolLock.png");
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

        // Only fire the moment it first becomes solved.
        if (!wasSolvedBefore && puzzleManager.isPuzzleSolved(puzzleId)) {
            onSolved.run();
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