package map;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TileMap {

    private int mapWidth;
    private int mapHeight;

    private int mapTileWidth;
    private int mapTileHeight;

    private int[][] tileData;

    private final List<Tileset> tilesets = new ArrayList<>();
    private final List<MapObject> objects = new ArrayList<>();

    private boolean loadedSuccessfully = false;

    public TileMap(String tmxPath) {

        loadTMX(tmxPath);
    }

    private void loadTMX(String tmxPath) {

        try {

            File tmxFile = new File(tmxPath);

            System.out.println();
            System.out.println(
                "========================================"
            );

            System.out.println(
                "Loading TMX: " +
                tmxFile.getAbsolutePath()
            );

            System.out.println(
                "Exists: " +
                tmxFile.exists()
            );

            if (!tmxFile.exists()) {

                throw new Exception(
                    "TMX file does not exist: " +
                    tmxFile.getAbsolutePath()
                );
            }

            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

            DocumentBuilder builder =
                factory.newDocumentBuilder();

            Document document =
                builder.parse(tmxFile);

            Element map =
                document.getDocumentElement();

            mapWidth =
                Integer.parseInt(
                    map.getAttribute("width")
                );

            mapHeight =
                Integer.parseInt(
                    map.getAttribute("height")
                );

            mapTileWidth =
                Integer.parseInt(
                    map.getAttribute("tilewidth")
                );

            mapTileHeight =
                Integer.parseInt(
                    map.getAttribute("tileheight")
                );

            System.out.println(
                "Map size: " +
                mapWidth +
                " x " +
                mapHeight
            );

            System.out.println(
                "Tile size: " +
                mapTileWidth +
                " x " +
                mapTileHeight
            );

            loadTilesets(
                map,
                tmxFile
            );

            loadTileLayer(map);

            loadObjectLayer(map);

            tilesets.sort(
                Comparator.comparingInt(
                    Tileset::getFirstGid
                )
            );

            loadedSuccessfully = true;

            System.out.println(
                "TMX loaded successfully!"
            );

            System.out.println(
                "Tilesets loaded: " +
                tilesets.size()
            );

            System.out.println(
                "Objects loaded: " +
                objects.size()
            );

            System.out.println(
                "========================================"
            );
            System.out.println();

        } catch (Exception e) {

            loadedSuccessfully = false;

            System.err.println();
            System.err.println(
                "FAILED TO LOAD TMX"
            );

            e.printStackTrace();

            System.err.println();
        }
    }

    private void loadTilesets(
        Element map,
        File tmxFile
    ) throws Exception {

        NodeList tilesetNodes =
            map.getElementsByTagName("tileset");

        for (
            int i = 0;
            i < tilesetNodes.getLength();
            i++
        ) {

            Element tilesetElement =
                (Element) tilesetNodes.item(i);

            int firstGid =
                Integer.parseInt(
                    tilesetElement.getAttribute(
                        "firstgid"
                    )
                );

            int tileWidth =
                Integer.parseInt(
                    tilesetElement.getAttribute(
                        "tilewidth"
                    )
                );

            int tileHeight =
                Integer.parseInt(
                    tilesetElement.getAttribute(
                        "tileheight"
                    )
                );

            int tileCount =
                Integer.parseInt(
                    tilesetElement.getAttribute(
                        "tilecount"
                    )
                );

            int columns =
                Integer.parseInt(
                    tilesetElement.getAttribute(
                        "columns"
                    )
                );

            String objectAlignment =
                tilesetElement.getAttribute(
                    "objectalignment"
                );

            if (objectAlignment.isEmpty()) {

                objectAlignment =
                    "bottomleft";
            }

            NodeList imageNodes =
                tilesetElement.getElementsByTagName(
                    "image"
                );

            if (imageNodes.getLength() == 0) {

                System.err.println(
                    "No image found for tileset: " +
                    tilesetElement.getAttribute("name")
                );

                continue;
            }

            Element imageElement =
                (Element) imageNodes.item(0);

            String imageSource =
                imageElement.getAttribute(
                    "source"
                );

            File imageFile =
                new File(
                    tmxFile.getParentFile(),
                    imageSource
                );

            System.out.println(
                "Tileset: " +
                tilesetElement.getAttribute("name")
            );

            System.out.println(
                "Image: " +
                imageFile.getAbsolutePath()
            );

            System.out.println(
                "Exists: " +
                imageFile.exists()
            );

            if (!imageFile.exists()) {

                throw new Exception(
                    "Missing tileset image: " +
                    imageFile.getAbsolutePath()
                );
            }

            BufferedImage image =
                ImageIO.read(imageFile);

            if (image == null) {

                throw new Exception(
                    "Could not decode image: " +
                    imageFile.getAbsolutePath()
                );
            }

            Tileset tileset =
                new Tileset(
                    firstGid,
                    tileWidth,
                    tileHeight,
                    tileCount,
                    columns,
                    objectAlignment,
                    image
                );

            tilesets.add(tileset);
        }
    }

    private void loadTileLayer(
        Element map
    ) throws Exception {

        NodeList layerNodes =
            map.getElementsByTagName("layer");

        if (layerNodes.getLength() == 0) {

            throw new Exception(
                "No tile layer found."
            );
        }

        Element layer =
            (Element) layerNodes.item(0);

        NodeList dataNodes =
            layer.getElementsByTagName("data");

        if (dataNodes.getLength() == 0) {

            throw new Exception(
                "No tile data found."
            );
        }

        String csv =
            dataNodes.item(0)
                .getTextContent()
                .trim();

        String[] values =
            csv.split(",");

        int requiredValues =
            mapWidth * mapHeight;

        if (values.length < requiredValues) {

            throw new Exception(
                "Tile data is incomplete. " +
                "Expected " +
                requiredValues +
                " values, got " +
                values.length
            );
        }

        tileData =
            new int[
                mapHeight
            ][
                mapWidth
            ];

        int index = 0;

        for (
            int row = 0;
            row < mapHeight;
            row++
        ) {

            for (
                int col = 0;
                col < mapWidth;
                col++
            ) {

                String value =
                    values[index++].trim();

                /*
                 * Tiled can store GIDs with the high
                 * flip bits set. Integer.parseUnsignedInt
                 * correctly handles values greater than
                 * Integer.MAX_VALUE.
                 */
                long unsignedValue =
                    Long.parseUnsignedLong(value);

                tileData[row][col] =
                    (int) unsignedValue;
            }
        }
    }

    private Tileset findTileset(
        long gid
    ) {

        Tileset selected =
            null;

        for (
            Tileset tileset : tilesets
        ) {

            if (
                gid >=
                tileset.getFirstGid()
            ) {

                if (
                    selected == null ||
                    tileset.getFirstGid() >
                    selected.getFirstGid()
                ) {

                    selected = tileset;
                }
            }
        }

        return selected;
    }

    private void loadObjectLayer(
        Element map
    ) throws Exception {

        NodeList objectGroups =
            map.getElementsByTagName(
                "objectgroup"
            );

        if (
            objectGroups.getLength() == 0
        ) {

            System.out.println(
                "No object layer found."
            );

            return;
        }

        Element objectGroup =
            (Element) objectGroups.item(0);

        NodeList objectNodes =
            objectGroup.getElementsByTagName(
                "object"
            );

        for (
            int i = 0;
            i < objectNodes.getLength();
            i++
        ) {

            Element object =
                (Element) objectNodes.item(i);

            String gidText =
                object.getAttribute("gid");

            /*
             * Ignore non-tile objects.
             */
            if (
                gidText == null ||
                gidText.isEmpty()
            ) {

                continue;
            }

            long gid =
                Long.parseUnsignedLong(
                    gidText
                );

            int id =
                Integer.parseInt(
                    object.getAttribute("id")
                );

            double x =
                Double.parseDouble(
                    object.getAttribute("x")
                );

            double y =
                Double.parseDouble(
                    object.getAttribute("y")
                );

            double width =
                Double.parseDouble(
                    object.getAttribute("width")
                );

            double height =
                Double.parseDouble(
                    object.getAttribute("height")
                );

            double rotation = 0;

            String rotationText =
                object.getAttribute("rotation");

            if (
                rotationText != null &&
                !rotationText.isEmpty()
            ) {

                rotation =
                    Double.parseDouble(
                        rotationText
                    );
            }

            MapObject mapObject =
                new MapObject(
                    id,
                    gid,
                    x,
                    y,
                    width,
                    height,
                    rotation
                );

            objects.add(mapObject);
        }
    }

    public void draw(
        Graphics2D g2
    ) {

        if (
            !loadedSuccessfully ||
            tileData == null
        ) {

            return;
        }

        for (
            int row = 0;
            row < mapHeight;
            row++
        ) {

            for (
                int col = 0;
                col < mapWidth;
                col++
            ) {

                long rawGid =
                    Integer.toUnsignedLong(
                        tileData[row][col]
                    );

                if (rawGid == 0) {

                    continue;
                }

                drawTile(
                    g2,
                    rawGid,
                    col * mapTileWidth,
                    row * mapTileHeight
                );
            }
        }

        drawObjects(g2);
    }

    private void drawTile(
        Graphics2D g2,
        long rawGid,
        int screenX,
        int screenY
    ) {

        boolean flipHorizontal =
            (rawGid & 0x80000000L) != 0;

        boolean flipVertical =
            (rawGid & 0x40000000L) != 0;

        boolean flipDiagonal =
            (rawGid & 0x20000000L) != 0;

        long gid =
            rawGid & 0x1FFFFFFFL;

        Tileset tileset =
            findTileset(gid);

        if (tileset == null) {

            System.err.println(
                "No tileset found for GID: " +
                gid
            );

            return;
        }

        int localId =
            (int) (
                gid -
                tileset.getFirstGid()
            );

        if (
            localId < 0 ||
            localId >= tileset.getTileCount()
        ) {

            return;
        }

        int columns =
            tileset.getColumns();

        int sourceColumn =
            localId % columns;

        int sourceRow =
            localId / columns;

        int sourceX =
            sourceColumn *
            tileset.getTileWidth();

        int sourceY =
            sourceRow *
            tileset.getTileHeight();

        int sourceWidth =
            tileset.getTileWidth();

        int sourceHeight =
            tileset.getTileHeight();

        BufferedImage image =
            tileset.getImage();

        AffineTransform oldTransform =
            g2.getTransform();

        if (
            !flipHorizontal &&
            !flipVertical &&
            !flipDiagonal
        ) {

            g2.drawImage(
                image,

                screenX,
                screenY,

                screenX + mapTileWidth,
                screenY + mapTileHeight,

                sourceX,
                sourceY,

                sourceX + sourceWidth,
                sourceY + sourceHeight,

                null
            );

            return;
        }

        AffineTransform transform =
            new AffineTransform();

        transform.translate(
            screenX +
            mapTileWidth / 2.0,

            screenY +
            mapTileHeight / 2.0
        );

        if (flipDiagonal) {

            transform.rotate(
                Math.PI / 2
            );
        }

        double scaleX =
            flipHorizontal ? -1 : 1;

        double scaleY =
            flipVertical ? -1 : 1;

        transform.scale(
            scaleX,
            scaleY
        );

        transform.translate(
            -mapTileWidth / 2.0,
            -mapTileHeight / 2.0
        );

        g2.setTransform(
            transform
        );

        g2.drawImage(
            image,

            0,
            0,

            mapTileWidth,
            mapTileHeight,

            sourceX,
            sourceY,

            sourceX + sourceWidth,
            sourceY + sourceHeight,

            null
        );

        g2.setTransform(
            oldTransform
        );
    }

    private void drawObjects(
        Graphics2D g2
    ) {

        for (
            MapObject object : objects
        ) {

            long rawGid =
                object.getGid();

            if (rawGid == 0) {

                continue;
            }

            drawObject(
                g2,
                object,
                rawGid
            );
        }
    }

    private void drawObject(
        Graphics2D g2,
        MapObject object,
        long rawGid
    ) {

        boolean flipHorizontal =
            (rawGid & 0x80000000L) != 0;

        boolean flipVertical =
            (rawGid & 0x40000000L) != 0;

        boolean flipDiagonal =
            (rawGid & 0x20000000L) != 0;

        long gid =
            rawGid & 0x1FFFFFFFL;

        Tileset tileset =
            findTileset(gid);

        if (tileset == null) {

            return;
        }

        int localId =
            (int) (
                gid -
                tileset.getFirstGid()
            );

        if (
            localId < 0 ||
            localId >= tileset.getTileCount()
        ) {

            return;
        }

        int columns =
            tileset.getColumns();

        int sourceColumn =
            localId % columns;

        int sourceRow =
            localId / columns;

        int sourceX =
            sourceColumn *
            tileset.getTileWidth();

        int sourceY =
            sourceRow *
            tileset.getTileHeight();

        int sourceWidth =
            tileset.getTileWidth();

        int sourceHeight =
            tileset.getTileHeight();

        BufferedImage image =
            tileset.getImage();

        double width =
            object.getWidth();

        double height =
            object.getHeight();

        double[] position =
            getObjectPosition(
                object,
                tileset
            );

        double drawX =
            position[0];

        double drawY =
            position[1];

        AffineTransform oldTransform =
            g2.getTransform();

        AffineTransform transform =
            new AffineTransform();

        transform.translate(
            object.getX(),
            object.getY()
        );

        if (
            object.getRotation() != 0
        ) {

            transform.rotate(
                Math.toRadians(
                    object.getRotation()
                )
            );
        }

        double localX;
        double localY;

        String alignment =
            tileset.getObjectAlignment();

        switch (alignment) {

            case "topleft" -> {

                localX = 0;
                localY = 0;
            }

            case "top" -> {

                localX = -width / 2.0;
                localY = 0;
            }

            case "topright" -> {

                localX = -width;
                localY = 0;
            }

            case "left" -> {

                localX = 0;
                localY = -height / 2.0;
            }

            case "center" -> {

                localX = -width / 2.0;
                localY = -height / 2.0;
            }

            case "right" -> {

                localX = -width;
                localY = -height / 2.0;
            }

            case "bottomleft" -> {

                localX = 0;
                localY = -height;
            }

            case "bottom" -> {

                localX = -width / 2.0;
                localY = -height;
            }

            case "bottomright" -> {

                localX = -width;
                localY = -height;
            }

            default -> {

                localX = 0;
                localY = -height;
            }
        }

        /*
         * Keep these values because the
         * existing object renderer uses
         * this coordinate convention.
         */
        localX += drawX - object.getX();
        localY += drawY - object.getY();

        transform.translate(
            localX,
            localY
        );

        if (
            flipDiagonal ||
            flipHorizontal ||
            flipVertical
        ) {

            if (flipDiagonal) {

                transform.rotate(
                    Math.PI / 2
                );
            }

            transform.scale(
                flipHorizontal ? -1 : 1,
                flipVertical ? -1 : 1
            );
        }

        g2.setTransform(
            transform
        );

        g2.drawImage(
            image,

            0,
            0,

            (int) width,
            (int) height,

            sourceX,
            sourceY,

            sourceX + sourceWidth,
            sourceY + sourceHeight,

            null
        );

        g2.setTransform(
            oldTransform
        );
    }

    private double[] getObjectPosition(
        MapObject object,
        Tileset tileset
    ) {

        double x =
            object.getX();

        double y =
            object.getY();

        double width =
            object.getWidth();

        double height =
            object.getHeight();

        String alignment =
            tileset.getObjectAlignment();

        switch (alignment) {

            case "topleft" -> {
                return new double[] {
                    x,
                    y
                };
            }

            case "top" -> {
                return new double[] {
                    x - width / 2.0,
                    y
                };
            }

            case "topright" -> {
                return new double[] {
                    x - width,
                    y
                };
            }

            case "left" -> {
                return new double[] {
                    x,
                    y - height / 2.0
                };
            }

            case "center" -> {
                return new double[] {
                    x - width / 2.0,
                    y - height / 2.0
                };
            }

            case "right" -> {
                return new double[] {
                    x - width,
                    y - height / 2.0
                };
            }

            case "bottomleft" -> {
                return new double[] {
                    x,
                    y - height
                };
            }

            case "bottom" -> {
                return new double[] {
                    x - width / 2.0,
                    y - height
                };
            }

            case "bottomright" -> {
                return new double[] {
                    x - width,
                    y - height
                };
            }

            default -> {
                return new double[] {
                    x,
                    y - height
                };
            }
        }
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