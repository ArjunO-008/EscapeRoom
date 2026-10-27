package map;

import java.awt.image.BufferedImage;

/**
 * The source rectangle within a tileset's image for a given,
 * already flip-bit-stripped GID. Returns {@code null} when the GID
 * falls outside the tileset's tile count, matching the original
 * bounds check.
 */
record TileSource(BufferedImage image, int sourceX, int sourceY, int sourceWidth, int sourceHeight) {

    static TileSource of(Tileset tileset, long gid) {
        int localId = (int) (gid - tileset.getFirstGid());

        if (localId < 0 || localId >= tileset.getTileCount()) {
            return null;
        }

        int columns = tileset.getColumns();
        int sourceColumn = localId % columns;
        int sourceRow = localId / columns;

        return new TileSource(
            tileset.getImage(),
            sourceColumn * tileset.getTileWidth(),
            sourceRow * tileset.getTileHeight(),
            tileset.getTileWidth(),
            tileset.getTileHeight()
        );
    }
}
