package map;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MapRenderer {

    private MapRenderer() {
    }


    // ============================================================
    // ENTRY
    // ============================================================

    static void draw(
            TileMap map,
            Graphics2D g2
    ) {

        if (!map.isLoadedSuccessfully()) {
            return;
        }

        if (map.getTileData() == null) {
            return;
        }

        drawTileLayer(map, g2);
        drawObjects(map, g2);
    }


    // ============================================================
    // TILE LAYER
    // ============================================================

    private static void drawTileLayer(
            TileMap map,
            Graphics2D g2
    ) {

        int[][] data =
                map.getTileData();

        int mapWidth =
                map.getMapWidth();

        int mapHeight =
                map.getMapHeight();

        int tileWidth =
                map.getMapTileWidth();

        int tileHeight =
                map.getMapTileHeight();


        for (int row = 0;
             row < mapHeight;
             row++) {

            for (int col = 0;
                 col < mapWidth;
                 col++) {

                long rawGid =
                        Integer.toUnsignedLong(
                                data[row][col]
                        );


                if (rawGid == 0) {
                    continue;
                }


                drawTile(
                        map,
                        g2,
                        rawGid,
                        col * tileWidth,
                        row * tileHeight,
                        tileWidth,
                        tileHeight
                );
            }
        }
    }


    // ============================================================
    // TILE
    // ============================================================

    private static void drawTile(
            TileMap map,
            Graphics2D g2,
            long rawGid,
            int x,
            int y,
            int width,
            int height
    ) {

        GidFlags flags =
                GidFlags.parse(rawGid);


        Tileset tileset =
                map.findTileset(flags.gid());


        if (tileset == null) {
            return;
        }


        TileSource source =
                TileSource.of(
                        tileset,
                        flags.gid()
                );


        if (source == null) {
            return;
        }


        /*
         * Normal tile.
         */
        if (!flags.flipHorizontal()
                && !flags.flipVertical()
                && !flags.flipDiagonal()) {

            g2.drawImage(
                    source.image(),

                    x,
                    y,
                    x + width,
                    y + height,

                    source.sourceX(),
                    source.sourceY(),
                    source.sourceX()
                            + source.sourceWidth(),
                    source.sourceY()
                            + source.sourceHeight(),

                    null
            );

            return;
        }


        /*
         * Flipped tile.
         */
        AffineTransform old =
                g2.getTransform();


        try {

            AffineTransform transform =
                    new AffineTransform(old);


            double centerX =
                    x + width / 2.0;

            double centerY =
                    y + height / 2.0;


            transform.translate(
                    centerX,
                    centerY
            );


            /*
             * Keep the existing tile-layer flip
             * behavior.
             */
            if (flags.flipDiagonal()) {

                transform.concatenate(
                        new AffineTransform(
                                0,
                                1,
                                1,
                                0,
                                0,
                                0
                        )
                );
            }


            transform.scale(
                    flags.flipHorizontal()
                            ? -1.0
                            : 1.0,

                    flags.flipVertical()
                            ? -1.0
                            : 1.0
            );


            transform.translate(
                    -width / 2.0,
                    -height / 2.0
            );


            g2.setTransform(transform);


            g2.drawImage(
                    source.image(),

                    0,
                    0,
                    width,
                    height,

                    source.sourceX(),
                    source.sourceY(),
                    source.sourceX()
                            + source.sourceWidth(),
                    source.sourceY()
                            + source.sourceHeight(),

                    null
            );

        } finally {

            g2.setTransform(old);
        }
    }


    // ============================================================
    // OBJECT LAYER
    // ============================================================

    private static void drawObjects(
            TileMap map,
            Graphics2D g2
    ) {

        List<MapObject> objects =
                map.getObjects();


        if (objects == null
                || objects.isEmpty()) {

            return;
        }


        /*
         * Tiled's top-down object ordering.
         *
         * Use a copy so we don't mutate the
         * map's stored object list.
         */
        List<MapObject> ordered =
                new ArrayList<>(objects);


        ordered.sort(
                Comparator
                        .comparingDouble(
                                MapObject::getY
                        )
                        .thenComparingInt(
                                MapObject::getId
                        )
        );


        for (MapObject object : ordered) {

            long rawGid =
                    object.getGid();


            if (rawGid == 0) {
                continue;
            }


            drawObject(
                    map,
                    g2,
                    object,
                    rawGid
            );
        }
    }


    // ============================================================
    // TILE OBJECT
    // ============================================================

    private static void drawObject(
            TileMap map,
            Graphics2D g2,
            MapObject object,
            long rawGid
    ) {

        GidFlags flags =
                GidFlags.parse(rawGid);


        Tileset tileset =
                map.findTileset(flags.gid());


        if (tileset == null) {
            return;
        }


        TileSource source =
                TileSource.of(
                        tileset,
                        flags.gid()
                );


        if (source == null) {
            return;
        }


        double width =
                object.getWidth();

        double height =
                object.getHeight();


        if (width <= 0.0
                || height <= 0.0) {

            return;
        }


        /*
         * --------------------------------------------------------
         * ALIGNMENT
         * --------------------------------------------------------
         *
         * The object position is the alignment point.
         *
         * Example:
         *
         * bottomleft:
         *
         *     (x,y)
         *       ┌─────────┐
         *       │  TILE   │
         *       └─────────┘
         *
         * bottom:
         *
         *          (x,y)
         *            │
         *       ┌────┴────┐
         *       │  TILE   │
         *       └─────────┘
         */
        ObjectAlignment alignment =
                ObjectAlignment.fromTmxValue(
                        tileset.getObjectAlignment()
                );


        double[] offset =
                alignment.offset(
                        width,
                        height
                );


        /*
         * Center of the displayed image relative
         * to the object's alignment origin.
         */
        double centerX =
                offset[0] + width / 2.0;

        double centerY =
                offset[1] + height / 2.0;


        AffineTransform old =
                g2.getTransform();


        try {

            AffineTransform transform =
                    new AffineTransform(old);


            /*
             * ----------------------------------------------------
             * 1. OBJECT ORIGIN
             * ----------------------------------------------------
             */
            transform.translate(
                    object.getX(),
                    object.getY()
            );


            /*
             * ----------------------------------------------------
             * 2. OBJECT ROTATION
             * ----------------------------------------------------
             *
             * Rotation happens around the object's alignment
             * point.
             */
            double rotation =
                    object.getRotation();


            if (rotation != 0.0) {

                transform.rotate(
                        Math.toRadians(rotation)
                );
            }


            /*
             * ----------------------------------------------------
             * 3. MOVE TO IMAGE CENTER
             * ----------------------------------------------------
             */
            transform.translate(
                    centerX,
                    centerY
            );


            /*
             * ----------------------------------------------------
             * 4. OBJECT FLIPS
             * ----------------------------------------------------
             *
             * Flips happen around the image center.
             */
            transform.scale(
                    flags.flipHorizontal()
                            ? -1.0
                            : 1.0,

                    flags.flipVertical()
                            ? -1.0
                            : 1.0
            );


            /*
             * ----------------------------------------------------
             * 5. SOURCE IMAGE
             * ----------------------------------------------------
             *
             * Tile source dimensions and object dimensions are
             * allowed to be different.
             */
            BufferedImage image =
                    source.image().getSubimage(
                            source.sourceX(),
                            source.sourceY(),
                            source.sourceWidth(),
                            source.sourceHeight()
                    );


            /*
             * Source → displayed object size.
             */
            double scaleX =
                    width / source.sourceWidth();

            double scaleY =
                    height / source.sourceHeight();


            transform.scale(
                    scaleX,
                    scaleY
            );


            /*
             * Put source image center at the transform origin.
             */
            transform.translate(
                    -source.sourceWidth() / 2.0,
                    -source.sourceHeight() / 2.0
            );


            g2.setTransform(transform);


            /*
             * No rounding here.
             *
             * All fractional TMX dimensions remain fractional
             * until Java2D performs the final rasterization.
             */
            g2.drawImage(
                    image,
                    0,
                    0,
                    null
            );

        } finally {

            g2.setTransform(old);
        }
    }
}