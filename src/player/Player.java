package player;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Player {

    private int x;
    private int y;

    private final int width = 32;
    private final int height = 48;

    // 0 = Down, 1 = Left, 2 = Right, 3 = Up
    private int direction = 0;

    private BufferedImage[] idleSprites = new BufferedImage[4];

    public Player(int x, int y) {
        this.x = x;
        this.y = y;

        loadSprites();
    }

    private void loadSprites() {

        idleSprites[0] = loadSprite("SpriteSheet_001.png"); // Down
        idleSprites[1] = loadSprite("SpriteSheet_003.png"); // Left
        idleSprites[2] = loadSprite("SpriteSheet_005.png"); // Right
        idleSprites[3] = loadSprite("SpriteSheet_011.png"); // Up
    }

    private BufferedImage loadSprite(String name) {

        try {
            return ImageIO.read(
                getClass().getResource("/player/SpriteSheet/" + name)
            );
        } catch (Exception e) {
            System.out.println("Failed to load: " + name);
            e.printStackTrace();
            return null;
        }
    }

    public void draw(Graphics2D g) {

        BufferedImage sprite = idleSprites[direction];

        if (sprite == null)
            return;

        g.drawImage(
            sprite,
            x,
            y,
            width,
            height,
            null
        );
    }

    public void moveBy(int dx, int dy, int screenWidth, int screenHeight) {

        x += dx;
        y += dy;

        if (dx < 0)
            direction = 1;
        else if (dx > 0)
            direction = 2;
        else if (dy < 0)
            direction = 3;
        else if (dy > 0)
            direction = 0;

        if (x < 0)
            x = 0;

        if (y < 0)
            y = 0;

        if (x + width > screenWidth)
            x = screenWidth - width;

        if (y + height > screenHeight)
            y = screenHeight - height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}