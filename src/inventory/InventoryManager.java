package inventory;

import java.util.HashSet;
import java.util.Set;

import ui.InfoUI;

public class InventoryManager {

    private final Set<Integer> items = new HashSet<>();
    private final InfoUI infoUI;

    public InventoryManager(InfoUI infoUI) {
        this.infoUI = infoUI;
    }

    public void addItem(int itemId) {
        items.add(itemId);

        infoUI.show("Inventory: added item " + itemId);
    }

    public boolean hasItem(int itemId) {
        return items.contains(itemId);
    }
}