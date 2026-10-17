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
        setLocationRelativeTo(null); // centre on screen

        // Remember room / position / inventory when the window is closed.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                panel.saveOnExit();
            }
        });

        setVisible(true);
    }
    
}
