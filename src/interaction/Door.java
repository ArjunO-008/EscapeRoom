package interaction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import inventory.InventoryManager;

public class Door implements Interactable {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 64;
    private static final int RANGE = 16;

    private final int x;
    private final int y;
    private final InventoryManager inventoryManager;

    private boolean open = false;

    public Door(int x, int y, InventoryManager inventoryManager) {
        this.x = x;
        this.y = y;
        this.inventoryManager = inventoryManager;
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
        if (open) {
            System.out.println("The door is already open.");
            return;
        }

        if (inventoryManager.hasItem(Key.ITEM_ID)) {
            open = true;
            System.out.println("The door unlocks... You escaped!");
        } else {
            System.out.println("The door is locked. You need a key.");
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void draw(Graphics2D g) {
        // No visual here on purpose — the map's own door tile is the art.
        // This object only exists to detect proximity + E presses.
        
    }
}