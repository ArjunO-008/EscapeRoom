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

    public void moveBy(int dx,int dy, int panelWidth,int panelHeight){
        int newX = this.x + dx;
        int newY = this.y + dy;

        if(newX < 0) newX = 0;
        if(newX > panelWidth - width) newX = panelWidth - width;

        if(newY < 0) newY = 0;
        if(newY > panelHeight - height) newY = panelHeight - height;

        this.x = newX;
        this.y = newY;
        
        

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
