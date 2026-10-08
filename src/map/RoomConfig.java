package map;

import java.awt.Rectangle;

/**
 * Configuration for one room.
 *
 * Contains:
 * - The playable boundary of the room
 * - The player spawn position
 */
public final class RoomConfig {

    // The actual playable area of the room.
    private final Rectangle bounds;

    // Player spawn position.
    private final int spawnX;
    private final int spawnY;

    /*
     * Player dimensions from Player.java.
     */
    private static final int PLAYER_WIDTH = 32;
    private static final int PLAYER_HEIGHT = 48;

    public RoomConfig(
            int x,
            int y,
            int width,
            int height
    ) {

        bounds = new Rectangle(
                x,
                y,
                width,
                height
        );
        spawnX = x + (width - PLAYER_WIDTH) / 2;
        spawnY = y + (height - PLAYER_HEIGHT) / 2;
    }

    public boolean canMoveTo(
            int playerX,
            int playerY,
            int playerWidth,
            int playerHeight
    ) {

        return playerX >= bounds.x
                && playerY >= bounds.y
                && playerX + playerWidth <= bounds.x + bounds.width
                && playerY + playerHeight <= bounds.y + bounds.height;
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

    /**
     * Room configurations.
     *
     * Current Living Room:
     *
     * x = 256
     * y = 128
     * width = 384
     * height = 320
     *
     * This corresponds to a 12 x 10 tile playable area
     * using your 32 x 32 Tiled grid.
     */
    public static RoomConfig forMap(String mapName) {

        return switch (mapName) {

            case "living_room" ->
                    new RoomConfig(
                            256,
                            230,
                            384,
                            320
                    );

            /*
             * Temporary values for the other rooms.
             *
             * We will replace these after defining their
             * actual boundaries.
             */
            case "bedroom" ->
                    new RoomConfig(
                            0,
                            0,
                            960,
                            640
                    );

            case "kitchen" ->
                    new RoomConfig(
                            0,
                            0,
                            960,
                            640
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unknown room: " + mapName
                    );
        };
    }
}