package interaction;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * A readable note lying on a table. Interacting with it opens a borderless
 * popup showing the note's full content image, closable only via the
 * window's own X button (no OK button). Used here to reveal the safe's code.
 */
public class Note implements Interactable {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 12;
    private static final int RANGE = 16;

    // How wide the popup's note image should appear on screen, regardless
    // of the source file's actual pixel dimensions. Height scales to match.
    private static final int POPUP_IMAGE_WIDTH = 220;

    private final int x;
    private final int y;
    private final String message; // fallback text, used only if the popup image fails to load
    private BufferedImage sprite;       // small icon drawn on the table in-world
    private BufferedImage contentImage; // bigger "readable" image shown in the popup

    public Note(int x, int y, String message) {
        this.x = x;
        this.y = y;
        this.message = message;

        sprite = loadSprite();
        contentImage = loadContentImage();
    }

    private BufferedImage loadSprite() {
        try {
            return ImageIO.read(new File("src/map/resources/note.png"));
        } catch (Exception e) {
            System.out.println("Failed to load note sprite, using placeholder.");
            return null;
        }
    }

    private BufferedImage loadContentImage() {
        try {
            return ImageIO.read(new File("src/map/resources/note_content.png"));
        } catch (Exception e) {
            System.out.println("Failed to load note content image, falling back to text popup.");
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
        if (contentImage == null) {
            // No image yet (or it failed to load) - fall back to the plain text popup.
            JOptionPane.showMessageDialog(null, message, "Note", JOptionPane.PLAIN_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) null, "Note", true); // true = modal

        // Scale to a fixed display width, keeping the image's own aspect
        // ratio so it doesn't look squashed or stretched.
        int displayWidth = POPUP_IMAGE_WIDTH;
        int displayHeight = (int) ((double) contentImage.getHeight()
                / contentImage.getWidth() * displayWidth);

        Image scaledImage = contentImage.getScaledInstance(
                displayWidth, displayHeight, Image.SCALE_SMOOTH);

        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        dialog.add(imageLabel);

        dialog.setResizable(false);
        dialog.pack(); // sizes the window to exactly fit the image label
        dialog.setLocationRelativeTo(null); // centers it on screen
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // No buttons added anywhere - only the window's own title bar X closes it.
        dialog.setVisible(true); // modal, so this blocks here until the player closes it
    }

    @Override
    public boolean isActive() {
        return true; // readable any number of times
    }

    @Override
    public void draw(Graphics2D g) {
        if (sprite != null) {
            g.drawImage(sprite, x, y, WIDTH, HEIGHT, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(x, y, WIDTH, HEIGHT);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, WIDTH, HEIGHT);
        }
    }
}