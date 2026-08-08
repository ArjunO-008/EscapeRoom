package core;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Timer;

// import java.awt.event.ActionListener;

import player.Player;
import input.InputSystem;

public class GamePanel extends JPanel{

    private Timer gameTimer;
    private Player player;
    private InputSystem inputSystem;

    
    public GamePanel(){
        setPreferredSize(new Dimension(960,640));
        setBackground(Color.BLACK);

        this.player = new Player(100, 100); // Player created on coord 100,100

        inputSystem = new InputSystem(this); // Accessing Keybord Deteciton on Key Pressed and releases
        setFocusable(true);
        requestFocusInWindow();

        //GameLoop Timer
        this.gameTimer = new Timer(16, e -> {
            // 16 means 1sec / 60FPS = 16ms, time ticks.
            update();
            repaint();
        });
        gameTimer.start();

        
    }

    //Updating Frames
    private void update(){
        final int speed = 4;
        int dx = 0, dy = 0;

        if(this.inputSystem.isUp()) dy -= speed;
        if(this.inputSystem.isDown()) dy += speed;
        if(this.inputSystem.isLeft()) dx -= speed;
        if(this.inputSystem.isRight()) dx += speed;

        player.moveBy(dx, dy, getWidth(), getHeight());       

    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        g.setColor(player.getColor());
        g.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());
       
    }
    
}
