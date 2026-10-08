package map;

import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapManager {

    private final Map<String, TileMap> maps =
            new LinkedHashMap<>();

    private final Map<String, RoomConfig> roomConfigs =
            new LinkedHashMap<>();

    private String currentMapName;

    public MapManager() {

        loadMap(
                "bedroom",
                "src/map/Bedroom.tmx"
        );

        loadMap(
                "kitchen",
                "src/map/Kitchen.tmx"
        );

        loadMap(
                "living_room",
                "src/map/Living_room.tmx"
        );

        /*
         * Load room configurations.
         */
        roomConfigs.put(
                "bedroom",
                RoomConfig.forMap("bedroom")
        );

        roomConfigs.put(
                "kitchen",
                RoomConfig.forMap("kitchen")
        );

        roomConfigs.put(
                "living_room",
                RoomConfig.forMap("living_room")
        );

        /*
         * Start in Living Room.
         */
        currentMapName = "living_room";

        System.out.println(
                "Starting map: " + currentMapName
        );
    }

    private void loadMap(
            String name,
            String path
    ) {

        TileMap map = new TileMap(path);

        maps.put(name, map);

        System.out.println(
                "Map loaded: " + name
        );
    }

    public void switchMap(String name) {

        if (!maps.containsKey(name)) {

            System.err.println(
                    "Map not found: " + name
            );

            return;
        }

        if (!roomConfigs.containsKey(name)) {

            System.err.println(
                    "Room configuration not found: " + name
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

    public RoomConfig getCurrentRoomConfig() {

        return roomConfigs.get(currentMapName);
    }

    public void draw(Graphics2D g2) {

        TileMap currentMap = getCurrentMap();

        if (currentMap != null) {

            currentMap.draw(g2);
        }
    }
}