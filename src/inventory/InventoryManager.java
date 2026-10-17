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

    // Called whenever the contents change (used to auto-save progress).
    private Runnable onChange;

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void addItem(int itemId) {
        // Items announce themselves (e.g. Key says "Picked up the key."),
        // so no generic message is queued here.
        if (items.add(itemId) && onChange != null) {
            onChange.run();
        }
    }

    public boolean hasItem(int itemId) {
        return items.contains(itemId);
    }

    // Empties the inventory. Called when a new game starts.
    public void clear() {
        items.clear();
    }

    // Item ids as an array, for the save file.
    public int[] getItemIds() {
        int[] ids = new int[items.size()];
        int i = 0;

        for (int id : items) {
            ids[i++] = id;
        }

        return ids;
    }

    // Replaces the contents (used by Continue). Does not trigger onChange.
    public void setItems(int[] ids) {
        items.clear();

        if (ids == null) {
            return;
        }

        for (int id : ids) {
            items.add(id);
        }
    }
}