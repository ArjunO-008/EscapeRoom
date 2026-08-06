package core;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Timer;

// import java.awt.event.ActionListener;

import player.Player;

public class GamePanel extends JPanel{

    private Timer gameTimer;
    private Player player;

    public GamePanel(){
        setPreferredSize(new Dimension(960,640));
        setBackground(Color.BLACK);

        this.player = new Player(100, 100);

        this.gameTimer = new Timer(16, e -> {
            update();
            repaint();
        });
        gameTimer.start();

        
    }

    private void update(){

    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        g.setColor(player.getColor());
        g.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());
       
    }
    
}
