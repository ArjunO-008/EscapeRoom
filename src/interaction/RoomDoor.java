package interaction;

import java.awt.Graphics2D;
import java.awt.Rectangle;


public class RoomDoor implements Interactable {

    private final int x;
    private final int y;

    private final int width;
    private final int height;

    private final int range;

    private final RoomSelectorCallback callback;

    public RoomDoor(
            int x,
            int y,
            int width,
            int height,
            int range,
            RoomSelectorCallback callback
    ) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.range = range;
        this.callback = callback;
    }

    @Override
    public Rectangle getInteractionBounds() {

        return new Rectangle(
                x - range,
                y - range,
                width + range * 2,
                height + range * 2
        );
    }

    @Override
    public void interact() {

        callback.openRoomSelector();
    }

    @Override
    public boolean isActive() {

        return true;
    }

    @Override
    public void draw(Graphics2D g) {
        /*
         * The door is already drawn by the map.
         */
    }

    public interface RoomSelectorCallback {

        void openRoomSelector();
    }
}