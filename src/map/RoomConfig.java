package map;

import java.awt.Rectangle;

public final class RoomConfig {

    private final Rectangle bounds;

    private final int spawnX;
    private final int spawnY;

    private static final int PLAYER_WIDTH = 32;
    private static final int PLAYER_HEIGHT = 48;

    public RoomConfig(
            int x,
            int y,
            int width,
            int height,
            int spawnX,
            int spawnY
    ) {

        bounds = new Rectangle(
                x,
                y,
                width,
                height
        );

        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    public boolean canMoveTo(
            java.awt.geom.Rectangle2D playerBounds,
            int playerX,
            int playerY,
            int playerWidth,
            int playerHeight
    ) {

        return playerBounds.getMinX() >= bounds.x
                && playerBounds.getMinY() >= bounds.y
                && playerBounds.getMaxX()
                        <= bounds.x + bounds.width
                && playerBounds.getMaxY()
                        <= bounds.y + bounds.height;
    }

    public int getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    public static RoomConfig forMap(String mapName) {

        return switch (mapName) {

            case "living_room" ->
                    new RoomConfig(
                            256,
                            255,
                            384,
                            195,

                            /*
                             * Living room starts at the center.
                             */
                            432,
                            316
                    );

            case "bedroom" ->
                    new RoomConfig(
                            218,
                            250,
                            465,
                            235,

                            /*
                             * Bedroom spawn near the bottom entrance.
                             *
                             * Adjust these two values after testing.
                             */
                            448,
                            435
                    );

            case "kitchen" ->
                    new RoomConfig(
                            255,
                            275,
                            390,
                            170,

                            /*
                             * Kitchen spawn near its entrance.
                             *
                             * Adjust these after testing.
                             */
                            600,
                            380
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unknown room: " + mapName
                    );
        };
    }
}