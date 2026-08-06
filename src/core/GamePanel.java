package core;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;

public class GamePanel extends JPanel{

    public GamePanel(){

        setPreferredSize(new Dimension(960,640));
        setBackground(Color.BLACK);

    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
    }
    
}
