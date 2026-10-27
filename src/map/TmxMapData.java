package map;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of everything parsed from a TMX file.
 */
final class TmxMapData {

    final int mapWidth;
    final int mapHeight;

    final int mapTileWidth;
    final int mapTileHeight;

    final int[][] tileData;

    final List<Tileset> tilesets;

    final List<MapObject> objects;

    final List<CollisionBox> collisionBoxes;

    final boolean loadedSuccessfully;

    TmxMapData(
            int mapWidth,
            int mapHeight,
            int mapTileWidth,
            int mapTileHeight,
            int[][] tileData,
            List<Tileset> tilesets,
            List<MapObject> objects,
            List<CollisionBox> collisionBoxes,
            boolean loadedSuccessfully
    ) {

        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;

        this.mapTileWidth = mapTileWidth;
        this.mapTileHeight = mapTileHeight;

        this.tileData = tileData;

        this.tilesets = tilesets;

        this.objects = objects;

        this.collisionBoxes = collisionBoxes;

        this.loadedSuccessfully =
                loadedSuccessfully;
    }

    static TmxMapData failed() {

        return new TmxMapData(
                0,
                0,
                0,
                0,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                false
        );
    }
}