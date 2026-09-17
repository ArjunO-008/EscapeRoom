package map;

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

/**
 * Loads the parts of a Tiled TMX map needed by the renderer.
 */
final class TmxMapLoader {

    private TmxMapLoader() {
    }

    static TmxMapData load(String tmxPath) {

        try {

            File tmxFile = new File(tmxPath);

            if (!tmxFile.exists()) {
                throw new Exception(
                        "TMX file does not exist: "
                                + tmxFile.getAbsolutePath());
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            /*
             * We only need normal XML parsing here.
             */
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(tmxFile);

            Element map = document.getDocumentElement();

            int mapWidth = parseIntAttribute(map, "width");

            int mapHeight = parseIntAttribute(map, "height");

            int mapTileWidth = parseIntAttribute(map, "tilewidth");

            int mapTileHeight = parseIntAttribute(map, "tileheight");

            List<Tileset> tilesets = loadTilesets(map, tmxFile);

            int[][] tileData = loadTileLayer(
                    map,
                    mapWidth,
                    mapHeight);

            List<MapObject> objects = loadObjectLayer(map);

            tilesets.sort(
                    Comparator.comparingInt(
                            Tileset::getFirstGid));

            return new TmxMapData(
                    mapWidth,
                    mapHeight,
                    mapTileWidth,
                    mapTileHeight,
                    tileData,
                    tilesets,
                    objects,
                    true);

        } catch (Exception e) {

            System.err.println(
                    "FAILED TO LOAD TMX");

            e.printStackTrace();

            return TmxMapData.failed();
        }
    }

    // ============================================================
    // TILESETS
    // ============================================================

    private static List<Tileset> loadTilesets(
            Element map,
            File tmxFile) throws Exception {

        List<Tileset> tilesets = new ArrayList<>();

        NodeList nodes = map.getElementsByTagName("tileset");

        for (int i = 0; i < nodes.getLength(); i++) {

            Element element = (Element) nodes.item(i);

            String firstGidText = element.getAttribute("firstgid");

            /*
             * firstgid belongs to the <tileset> element
             * in the TMX map.
             */
            if (firstGidText.isEmpty()) {
                continue;
            }

            int firstGid = Integer.parseInt(firstGidText);

            int tileWidth = parseIntAttribute(
                    element,
                    "tilewidth");

            int tileHeight = parseIntAttribute(
                    element,
                    "tileheight");

            int tileCount = parseIntAttribute(
                    element,
                    "tilecount");

            int columns = parseIntAttribute(
                    element,
                    "columns");

            /*
             * Tiled's default for an orthogonal map is
             * bottom-left when objectalignment isn't present.
             */
            String objectAlignment = element.getAttribute(
                    "objectalignment");

            if (objectAlignment == null
                    || objectAlignment.isBlank()) {

                objectAlignment = "bottomleft";
            }

            NodeList imageNodes = element.getElementsByTagName(
                    "image");

            if (imageNodes.getLength() == 0) {

                System.err.println(
                        "No image found for tileset: "
                                + element.getAttribute("name"));

                continue;
            }

            Element imageElement = (Element) imageNodes.item(0);

            String imageSource = imageElement.getAttribute(
                    "source");

            if (imageSource == null
                    || imageSource.isEmpty()) {

                throw new Exception(
                        "Tileset image has no source: "
                                + element.getAttribute("name"));
            }

            File imageFile = new File(
                    tmxFile.getParentFile(),
                    imageSource);

            if (!imageFile.exists()) {

                throw new Exception(
                        "Missing tileset image: "
                                + imageFile.getAbsolutePath());
            }

            BufferedImage image = ImageIO.read(imageFile);

            if (image == null) {

                throw new Exception(
                        "Could not decode tileset image: "
                                + imageFile.getAbsolutePath());
            }

            tilesets.add(
                    new Tileset(
                            firstGid,
                            tileWidth,
                            tileHeight,
                            tileCount,
                            columns,
                            objectAlignment,
                            image));
        }

        return tilesets;
    }

    // ============================================================
    // TILE LAYER
    // ============================================================

    private static int[][] loadTileLayer(
            Element map,
            int mapWidth,
            int mapHeight) throws Exception {

        NodeList layers = map.getElementsByTagName("layer");

        if (layers.getLength() == 0) {

            throw new Exception(
                    "No tile layer found.");
        }

        /*
         * The current project expects the first tile layer.
         */
        Element layer = (Element) layers.item(0);

        NodeList dataNodes = layer.getElementsByTagName("data");

        if (dataNodes.getLength() == 0) {

            throw new Exception(
                    "No tile data found.");
        }

        Element data = (Element) dataNodes.item(0);

        String encoding = data.getAttribute("encoding");

        if (!"csv".equalsIgnoreCase(encoding)) {

            throw new Exception(
                    "Only CSV encoded TMX layers are supported.");
        }

        String csv = data.getTextContent().trim();

        String[] values = csv.split(",");

        int expected = mapWidth * mapHeight;

        if (values.length < expected) {

            throw new Exception(
                    "Tile data is incomplete. Expected "
                            + expected
                            + " values, got "
                            + values.length);
        }

        int[][] tileData = new int[mapHeight][mapWidth];

        int index = 0;

        for (int row = 0; row < mapHeight; row++) {

            for (int col = 0; col < mapWidth; col++) {

                String value = values[index++].trim();

                /*
                 * Keep the full unsigned 32-bit GID.
                 *
                 * The Java int stores the bit pattern;
                 * MapRenderer converts it back to unsigned.
                 */
                long unsignedGid = Long.parseUnsignedLong(value);

                tileData[row][col] = (int) unsignedGid;
            }
        }

        return tileData;
    }

    // ============================================================
    // OBJECT LAYER
    // ============================================================

    private static List<MapObject> loadObjectLayer(Element map) {
        List<MapObject> objects = new ArrayList<>();

        NodeList objectGroups = map.getElementsByTagName("objectgroup");

        if (objectGroups.getLength() == 0) {
            System.out.println("No object layer found.");
            return objects;
        }

        Element objectGroup = (Element) objectGroups.item(0);
        NodeList objectNodes = objectGroup.getElementsByTagName("object");

        for (int i = 0; i < objectNodes.getLength(); i++) {
            Element object = (Element) objectNodes.item(i);

            String gidText = object.getAttribute("gid");

            // Ignore non-tile objects.
            if (gidText == null || gidText.isEmpty()) {
                continue;
            }

            long gid = Long.parseUnsignedLong(gidText);

            int id = Integer.parseInt(object.getAttribute("id"));

            double x = Double.parseDouble(object.getAttribute("x"));
            double y = Double.parseDouble(object.getAttribute("y"));

            double width = Double.parseDouble(object.getAttribute("width"));
            double height = Double.parseDouble(object.getAttribute("height"));

            double rotation = 0.0;

            String rotationText = object.getAttribute("rotation");

            if (rotationText != null && !rotationText.isEmpty()) {
                rotation = Double.parseDouble(rotationText);
            }

            objects.add(
                    new MapObject(
                            id,
                            gid,
                            x,
                            y,
                            width,
                            height,
                            rotation));
        }

        return objects;
    }
    // ============================================================
    // XML HELPERS
    // ============================================================

    private static int parseIntAttribute(
            Element element,
            String name) throws Exception {

        String value = element.getAttribute(name);

        if (value == null
                || value.isBlank()) {

            throw new Exception(
                    "Missing integer attribute '"
                            + name
                            + "' in <"
                            + element.getTagName()
                            + ">");
        }

        return Integer.parseInt(value);
    }

}