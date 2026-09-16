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

        public TileMap(String tmxPath) {

                loadTMX(tmxPath);
        }

        private void loadTMX(String tmxPath) {

                try {

                File tmxFile = new File(tmxPath);

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

                loadTilesets(map, tmxFile);

                loadTileLayer(map);
                loadObjectLayer(map);

                tilesets.sort(
                        Comparator.comparingInt(
                                Tileset::getFirstGid
                        )
                );

                System.out.println("TMX loaded successfully!");
                System.out.println(
                        "Map: " +
                        mapWidth +
                        " x " +
                        mapHeight
                );

                System.out.println(
                        "Tilesets loaded: " +
                        tilesets.size()
                );

                } catch (Exception e) {

                System.err.println(
                        "Failed to load TMX:"
                );

                e.printStackTrace();
                }
        }

        private void loadTilesets(
                Element map,
                File tmxFile
        ) throws Exception {

                NodeList tilesetNodes =
                        map.getElementsByTagName("tileset");

                for (int i = 0;
                i < tilesetNodes.getLength();
                i++) {

                Element tilesetElement =
                        (Element) tilesetNodes.item(i);

                int firstGid =
                        Integer.parseInt(
                                tilesetElement
                                .getAttribute(
                                        "firstgid"
                                )
                        );

                int tileWidth =
                        Integer.parseInt(
                                tilesetElement
                                .getAttribute(
                                        "tilewidth"
                                )
                        );

                int tileHeight =
                        Integer.parseInt(
                                tilesetElement
                                .getAttribute(
                                        "tileheight"
                                )
                        );

                int tileCount =
                        Integer.parseInt(
                                tilesetElement
                                .getAttribute(
                                        "tilecount"
                                )
                        );

                int columns =
                        Integer.parseInt(
                                tilesetElement
                                .getAttribute(
                                        "columns"
                                )
                        );
                String objectAlignment =
                        tilesetElement.getAttribute(
                                "objectalignment"
                        );

                if (objectAlignment.isEmpty()) {
                        objectAlignment = "bottomleft";
                }

                NodeList imageNodes =
                        tilesetElement
                                .getElementsByTagName(
                                        "image"
                                );

                if (imageNodes.getLength() == 0) {
                        continue;
                }

                Element imageElement =
                        (Element) imageNodes.item(0);

                String imageSource =
                        imageElement
                                .getAttribute("source");

                File imageFile =
                        new File(
                                tmxFile
                                        .getParentFile(),
                                imageSource
                        );

                BufferedImage image =
                        ImageIO.read(imageFile);

                if (image == null) {

                        System.err.println(
                                "Could not load image: "
                                + imageFile
                        );

                        continue;
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

                System.out.println(
                        "Loaded: " +
                        tilesetElement
                                .getAttribute("name")
                );
                }
        }

        private void loadTileLayer(Element map)
                throws Exception {

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
                        dataNodes
                                .item(0)
                                .getTextContent()
                                .trim();

                String[] values =
                        csv.split(",");

                tileData =
                        new int[mapHeight][mapWidth];

                int index = 0;

                for (int row = 0;
                row < mapHeight;
                row++) {

                for (int col = 0;
                        col < mapWidth;
                        col++) {

                        tileData[row][col] =
                                Integer.parseUnsignedInt(
                                        values[index++]
                                                .trim()
                                );
                }
                }
        }

        private Tileset findTileset(long gid) {

                Tileset selected = null;

                for (Tileset tileset : tilesets) {

                if (gid >= tileset.getFirstGid()) {

                        if (selected == null ||
                        tileset.getFirstGid()
                        > selected.getFirstGid()) {

                        selected = tileset;
                        }
                }
                }

                return selected;
        }

        private void loadObjectLayer(Element map)
                throws Exception {

        NodeList objectGroups =
                map.getElementsByTagName("objectgroup");

        if (objectGroups.getLength() == 0) {

                System.out.println(
                        "No object layer found."
                );

                return;
        }

        Element objectGroup =
                (Element) objectGroups.item(0);

        NodeList objectNodes =
                objectGroup.getElementsByTagName("object");

        for (int i = 0;
                i < objectNodes.getLength();
                i++) {

                Element object =
                        (Element) objectNodes.item(i);

                String gidText =
                        object.getAttribute("gid");

                // Ignore objects that are not tile objects
                if (gidText.isEmpty()) {
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
                        object.getAttribute(
                                "rotation"
                        );

                if (!rotationText.isEmpty()) {

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

        System.out.println(
                "Objects loaded: " +
                objects.size()
        );
        }

        public void draw(Graphics2D g2) {

                if (tileData == null) {
                        return;
                }

                for (int row = 0;
                        row < mapHeight;
                        row++) {

                        for (int col = 0;
                                col < mapWidth;
                                col++) {

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
                return;
                }

                int localId =
                        (int) (
                                gid -
                                tileset.getFirstGid()
                        );

                if (localId < 0 ||
                localId >= tileset.getTileCount()) {

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

                /*
                * Normal tile.
                */
                if (!flipHorizontal &&
                !flipVertical &&
                !flipDiagonal) {

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

                /*
                * Transform flipped tiles.
                */
                AffineTransform transform =
                        new AffineTransform();

                transform.translate(
                        screenX + mapTileWidth / 2.0,
                        screenY + mapTileHeight / 2.0
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

                g2.setTransform(transform);

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

                g2.setTransform(oldTransform);
        }

        private void drawObjects(Graphics2D g2) {

                for (MapObject object : objects) {

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

        if (localId < 0 ||
                localId >= tileset.getTileCount()) {

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

        /*
        * Tiled tile-object origin:
        *
        * x = left
        * y = bottom
        *
        * Java Graphics2D:
        *
        * x = left
        * y = top
        */

        double[] position = getObjectPosition(object,tileset);

        double drawX = position[0];
        double drawY = position[1];

        AffineTransform oldTransform =
        g2.getTransform();

        AffineTransform transform =
                new AffineTransform();

        transform.translate(
                object.getX(),
                object.getY()
        );

        if (object.getRotation() != 0) {

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
        * Apply diagonal flip.
        */
        if (flipDiagonal) {

                transform.rotate(
                        Math.PI / 2
                );
        }

        /*
        * Apply horizontal / vertical flip.
        */
        transform.scale(
                flipHorizontal ? -1 : 1,
                flipVertical ? -1 : 1
        );

        /*
        * Move image origin to center.
        */
        transform.translate(
                localX,
                localY
        );

        g2.setTransform(transform);

        g2.drawImage(
                image,
                0,
                0,
                (int) Math.round(width),
                (int) Math.round(height),

                sourceX,
                sourceY,
                sourceX + sourceWidth,
                sourceY + sourceHeight,

                null
        );

        g2.setTransform(oldTransform);
}

        private double[] getObjectPosition(
        MapObject object,
        Tileset tileset
) {

    double x = object.getX();
    double y = object.getY();

    double width = object.getWidth();
    double height = object.getHeight();

    String alignment =
            tileset.getObjectAlignment();

    double drawX;
    double drawY;

    switch (alignment) {

        case "topleft":
            drawX = x;
            drawY = y;
            break;

        case "top":
            drawX = x - width / 2.0;
            drawY = y;
            break;

        case "topright":
            drawX = x - width;
            drawY = y;
            break;

        case "left":
            drawX = x;
            drawY = y - height / 2.0;
            break;

        case "center":
            drawX = x - width / 2.0;
            drawY = y - height / 2.0;
            break;

        case "right":
            drawX = x - width;
            drawY = y - height / 2.0;
            break;

        case "bottomleft":
            drawX = x;
            drawY = y - height;
            break;

        case "bottom":
            drawX = x - width / 2.0;
            drawY = y - height;
            break;

        case "bottomright":
            drawX = x - width;
            drawY = y - height;
            break;

        default:
            drawX = x;
            drawY = y - height;
    }

    return new double[] {
            drawX,
            drawY
    };
}


        public int getWidth() {
                return mapWidth;
        }

        public int getHeight() {
                return mapHeight;
        }

        public int getTileWidth() {
                return mapTileWidth;
        }

        public int getTileHeight() {
                return mapTileHeight;
        }
        }