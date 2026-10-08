package map;

import java.awt.geom.Rectangle2D;

/**
 * A rectangular collision area loaded from Tiled.
 */
public final class CollisionBox {

    private final Rectangle2D.Double bounds;

    public CollisionBox(
            double x,
            double y,
            double width,
            double height
    ) {

        this.bounds = new Rectangle2D.Double(
                x,
                y,
                width,
                height
        );
    }

    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );
    }

    public boolean intersects(
            double x,
            double y,
            double width,
            double height
    ) {

        return bounds.intersects(
                x,
                y,
                width,
                height
        );
    }
}