package core;

import javax.swing.JFrame;

public class GameWindow extends JFrame{

    public GameWindow(){
        setTitle("Escape Room");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setVisible(true);
    }
    
}
