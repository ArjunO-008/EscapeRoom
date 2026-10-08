package interaction;

import inventory.InventoryManager;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import ui.InfoUI;

public class Door implements Interactable {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 64;
    private static final int RANGE = 16;

    private final int x;
    private final int y;
    private final InventoryManager inventoryManager;

    private boolean open = false;
    private final InfoUI infoUI;
    private final Runnable onWin;

    public Door(
        int x,
        int y,
        InventoryManager inventoryManager,
        InfoUI infoUI,
        Runnable onWin) {
        this.x = x;
        this.y = y;
        this.inventoryManager = inventoryManager;
        this.infoUI = infoUI;
        this.onWin = onWin;
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
            infoUI.show("The door is already open.");
            return;
        }

        if (inventoryManager.hasItem(Key.ITEM_ID)) {
            open = true;
            infoUI.show("The door unlocks... You escaped!");
        } else {
            infoUI.show("The door is locked. You need a key.");
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