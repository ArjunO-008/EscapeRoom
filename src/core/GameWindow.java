package core;

import javax.swing.JFrame;

public class GameWindow extends JFrame{

    public GameWindow(){
        setTitle("Escape Room");
        setSize(960,640);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
    }
    
}
