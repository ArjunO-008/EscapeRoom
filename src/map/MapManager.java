package map;

import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapManager {

    private final Map<String, TileMap> maps = new LinkedHashMap<>();
    private String currentMapName;

    public MapManager() {

        /*
         * Load all game maps.
         *
         * All TMX files must be inside:
         * src/map/
         */

        loadMap(
            "bedroom",
            "src/map/Bedroom(3).tmx"
        );

        loadMap(
            "kitchen",
            "src/map/Kitchen(3).tmx"
        );

        loadMap(
            "living_room",
            "src/map/Living_room(4).tmx"
        );

        loadMap(
            "study",
            "src/map/untitled(3).tmx"
        );

        /*
         * Start the game in the living room.
         */
        currentMapName = "living_room";
    }

    private void loadMap(
        String name,
        String path
    ) {

        System.out.println(
            "Loading map: " + name
        );

        TileMap map = new TileMap(path);

        maps.put(name, map);
    }

    public void switchMap(String name) {

        if (!maps.containsKey(name)) {

            System.err.println(
                "Map not found: " + name
            );

            return;
        }

        currentMapName = name;

        System.out.println(
            "Current map: " +
            currentMapName
        );
    }

    public TileMap getCurrentMap() {

        return maps.get(
            currentMapName
        );
    }

    public String getCurrentMapName() {

        return currentMapName;
    }

    public void draw(Graphics2D g2) {

        TileMap currentMap =
            getCurrentMap();

        if (currentMap != null) {
            currentMap.draw(g2);
        }
    }
}