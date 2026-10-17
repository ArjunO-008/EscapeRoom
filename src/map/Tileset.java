package map;

import java.awt.image.BufferedImage;

public class Tileset {

    private final int firstGid;
    private final int tileWidth;
    private final int tileHeight;
    private final int tileCount;
    private final int columns;
    private final BufferedImage image;
    private final String objectAlignment;

    public Tileset(
        int firstGid,
        int tileWidth,
        int tileHeight,
        int tileCount,
        int columns,
        String objectAlignment,
        BufferedImage image
) {
    this.firstGid = firstGid;
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
    this.tileCount = tileCount;
    this.columns = columns;
    this.objectAlignment = objectAlignment;
    this.image = image;
}
    public int getFirstGid() {
        return firstGid;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getTileCount() {
        return tileCount;
    }

    public int getColumns() {
        return columns;
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getObjectAlignment() {
        return objectAlignment;
    }

}

