package map;

/**
 * Tiled's "objectalignment" tileset values, and the pixel offset each
 * one applies to a tile object's declared (x, y) position given its
 * width/height. Previously this switch was written out twice (once
 * in drawObject, once in getObjectPosition) with identical cases;
 * it's centralised here so the two call sites can't drift apart.
 */
enum ObjectAlignment {

    TOP_LEFT("topleft"),
    TOP("top"),
    TOP_RIGHT("topright"),
    LEFT("left"),
    CENTER("center"),
    RIGHT("right"),
    BOTTOM_LEFT("bottomleft"),
    BOTTOM("bottom"),
    BOTTOM_RIGHT("bottomright");

    private final String tmxValue;

    ObjectAlignment(String tmxValue) {
        this.tmxValue = tmxValue;
    }

    static ObjectAlignment fromTmxValue(String value) {
        for (ObjectAlignment alignment : values()) {
            if (alignment.tmxValue.equals(value)) {
                return alignment;
            }
        }
        // Matches the loader's own fallback for a missing/unknown value.
        return BOTTOM_LEFT;
    }

    /** The (offsetX, offsetY) to add to an object's raw x/y for this alignment. */
    double[] offset(double width, double height) {
        return switch (this) {
            case TOP_LEFT -> new double[] { 0, 0 };
            case TOP -> new double[] { -width / 2.0, 0 };
            case TOP_RIGHT -> new double[] { -width, 0 };
            case LEFT -> new double[] { 0, -height / 2.0 };
            case CENTER -> new double[] { -width / 2.0, -height / 2.0 };
            case RIGHT -> new double[] { -width, -height / 2.0 };
            case BOTTOM_LEFT -> new double[] { 0, -height };
            case BOTTOM -> new double[] { -width / 2.0, -height };
            case BOTTOM_RIGHT -> new double[] { -width, -height };
        };
    }
}
