package interaction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import inventory.InventoryManager;
import ui.InfoUI;

public class Key implements Interactable {
    public static final int ITEM_ID = 1;

    private static final int SIZE = 40;
    private static final int RANGE = 16;

    private final int x;
    private final int y;
    private final InventoryManager inventoryManager;

    private boolean collected = false;
    private BufferedImage sprite;

    private final InfoUI infoUI;

    public Key(
            int x,
            int y,
            InventoryManager inventoryManager,
            InfoUI infoUI) {
        this.x = x;
        this.y = y;
        this.inventoryManager = inventoryManager;
        this.infoUI = infoUI;

        sprite = loadSprite();
    }

    private BufferedImage loadSprite() {
        try {
            return ImageIO.read(new File("src/map/resources/Key.png"));
        } catch (Exception e) {
            System.out.println("Failed to load key sprite, using placeholder.");
            return null;
        }
    }

    @Override
    public Rectangle getInteractionBounds() {
        return new Rectangle(
                x - RANGE,
                y - RANGE,
                SIZE + RANGE * 2,
                SIZE + RANGE * 2);
    }

    @Override
    public void interact() {
        if (collected) {
            return;
        }

        inventoryManager.addItem(ITEM_ID);
        collected = true;

        infoUI.show("Picked up the key.");
    }

    @Override
    public boolean isActive() {
        return !collected;
    }

    @Override
    public void draw(Graphics2D g) {
        if (sprite != null) {
            g.drawImage(sprite, x, y, SIZE, SIZE, null);
        } else {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, SIZE, SIZE);
        }
    }

}
