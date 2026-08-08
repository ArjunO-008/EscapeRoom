package core;

import javax.swing.JFrame;

public class GameWindow extends JFrame{

    //Creation of Game Windows with name "Escape Room"
    public GameWindow(){
        setTitle("Escape Room");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        //GamePanel Deals with The rendering on Game
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setVisible(true);
    }
    
}
