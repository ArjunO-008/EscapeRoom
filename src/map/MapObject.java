package map;

public class MapObject {

    private final int id;
    private final long gid;

    private final double x;
    private final double y;

    private final double width;
    private final double height;

    private final double rotation;

    public MapObject(
            int id,
            long gid,
            double x,
            double y,
            double width,
            double height,
            double rotation
    ) {
        this.id = id;
        this.gid = gid;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    public int getId() {
        return id;
    }

    public long getGid() {
        return gid;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getRotation() {
        return rotation;
    }
}