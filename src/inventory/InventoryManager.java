package inventory;

import java.util.HashSet;
import java.util.Set;

public class InventoryManager {
    private Set<Integer> items = new HashSet<>();

    public void addItem(int itemId) {
        items.add(itemId);
        System.out.println("Inventory: added item " + itemId);
    }

    public boolean hasItem(int itemId) {
        return items.contains(itemId);
    }
}
