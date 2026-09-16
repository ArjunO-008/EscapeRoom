package map;

import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapManager {

    private final Map<String, TileMap> maps = new LinkedHashMap<>();
    private String currentMapName;

    public MapManager() {

        loadMap("living_room", "src/map/Living_room.tmx");
        loadMap("kitchen", "src/map/Kitchen.tmx");
        loadMap("bedroom", "src/map/Bedroom.tmx");
    }

    private void loadMap(String name, String path) {

        TileMap map = new TileMap(path);

        maps.put(name, map);

        if (currentMapName == null) {
            currentMapName = name;
        }
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
            "Switched to map: " + currentMapName
        );
    }

    public TileMap getCurrentMap() {

        return maps.get(currentMapName);
    }

    public String getCurrentMapName() {

        return currentMapName;
    }

    public void draw(Graphics2D g2) {

        TileMap currentMap = getCurrentMap();

        if (currentMap != null) {
            currentMap.draw(g2);
        }
    }
}