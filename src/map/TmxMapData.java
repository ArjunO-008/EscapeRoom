package map;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of everything parsed from a TMX file: map
 * dimensions, the raw tile grid, tilesets and objects. Handed to
 * {@link TileMap} by {@link TmxMapLoader} so parsing and state are
 * separate from rendering.
 */
final class TmxMapData {

    final int mapWidth;
    final int mapHeight;
    final int mapTileWidth;
    final int mapTileHeight;
    final int[][] tileData;
    final List<Tileset> tilesets;
    final List<MapObject> objects;
    final boolean loadedSuccessfully;

    TmxMapData(
        int mapWidth,
        int mapHeight,
        int mapTileWidth,
        int mapTileHeight,
        int[][] tileData,
        List<Tileset> tilesets,
        List<MapObject> objects,
        boolean loadedSuccessfully
    ) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.mapTileWidth = mapTileWidth;
        this.mapTileHeight = mapTileHeight;
        this.tileData = tileData;
        this.tilesets = tilesets;
        this.objects = objects;
        this.loadedSuccessfully = loadedSuccessfully;
    }

    static TmxMapData failed() {
        return new TmxMapData(
            0, 0, 0, 0, null, Collections.emptyList(), Collections.emptyList(), false
        );
    }
}
