package map;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A single loaded Tiled map.
 *
 * Stores:
 * - Map dimensions
 * - Tile data
 * - Tilesets
 * - Map objects
 * - Pre-sorted map objects for rendering
 */
public class TileMap {

    private final int mapWidth;
    private final int mapHeight;
    private final int mapTileWidth;
    private final int mapTileHeight;

    private final int[][] tileData;

    private final List<Tileset> tilesets;
    private final List<MapObject> objects;
    private final List<MapObject> orderedObjects;

    private final boolean loadedSuccessfully;

    public TileMap(String tmxPath) {

        TmxMapData data =
                TmxMapLoader.load(tmxPath);

        this.mapWidth =
                data.mapWidth;

        this.mapHeight =
                data.mapHeight;

        this.mapTileWidth =
                data.mapTileWidth;

        this.mapTileHeight =
                data.mapTileHeight;

        this.tileData =
                data.tileData;

        this.tilesets =
                data.tilesets;

        this.objects =
                data.objects;

        /*
         * Create the sorted object list ONCE.
         */
        this.orderedObjects =
                new ArrayList<>(this.objects);

        this.orderedObjects.sort(
                Comparator
                        .comparingDouble(
                                MapObject::getY
                        )
                        .thenComparingInt(
                                MapObject::getId
                        )
        );

        this.loadedSuccessfully =
                data.loadedSuccessfully;
    }

    public void draw(Graphics2D g2) {

        MapRenderer.draw(
                this,
                g2
        );
    }

    Tileset findTileset(long gid) {

        Tileset selected = null;

        for (Tileset tileset : tilesets) {

            if (gid >= tileset.getFirstGid()) {

                if (selected == null
                        || tileset.getFirstGid()
                                > selected.getFirstGid()) {

                    selected = tileset;
                }
            }
        }

        return selected;
    }

    int[][] getTileData() {
        return tileData;
    }

    List<MapObject> getObjects() {
        return objects;
    }

    /**
     * Returns the objects already sorted for rendering.
     *
     * This list is created and sorted once when the map loads.
     */
    public List<MapObject> getOrderedObjects() {
        return orderedObjects;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public int getMapTileWidth() {
        return mapTileWidth;
    }

    public int getMapTileHeight() {
        return mapTileHeight;
    }

    public boolean isLoadedSuccessfully() {
        return loadedSuccessfully;
    }
}