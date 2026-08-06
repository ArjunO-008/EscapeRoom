package player;

import java.awt.Color;

public class Player {
    private int x;
    private int y;
    private int width;
    private int height;
    private Color color;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;

        this.width = 32;
        this.height = 32;
        this.color = Color.CYAN;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Color getColor() {
        return this.color;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

}
