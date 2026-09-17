package map;

/**
 * Decodes a raw Tiled GID into its flip bits and the underlying tile
 * GID. Tiled packs horizontal/vertical/diagonal flip flags into the
 * top three bits of each cell's GID; both the tile-layer renderer and
 * the object renderer need to unpack them the same way, so it lives
 * here instead of being duplicated in each.
 */
record GidFlags(long gid, boolean flipHorizontal, boolean flipVertical, boolean flipDiagonal) {

    private static final long FLIP_HORIZONTAL_BIT = 0x80000000L;
    private static final long FLIP_VERTICAL_BIT = 0x40000000L;
    private static final long FLIP_DIAGONAL_BIT = 0x20000000L;
    private static final long GID_MASK = 0x1FFFFFFFL;

    static GidFlags parse(long rawGid) {
        return new GidFlags(
            rawGid & GID_MASK,
            (rawGid & FLIP_HORIZONTAL_BIT) != 0,
            (rawGid & FLIP_VERTICAL_BIT) != 0,
            (rawGid & FLIP_DIAGONAL_BIT) != 0
        );
    }
}
