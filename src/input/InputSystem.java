package input;

import javax.swing.JComponent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class InputSystem {
    private boolean up, down, left, right;

    public InputSystem(JComponent component) {

        // UP KEY-Press:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "up-pressed");
        component.getActionMap().put("up-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                up = true;
            }
        });
        // DOWN KEY-Press:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "down-pressed");
        component.getActionMap().put("down-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                down = true;
            }
        });
        // LEFT KEY-Press:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "left-pressed");
        component.getActionMap().put("left-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                left = true;
            }
        });
        // RIGHT KEY-Press:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "right-pressed");
        component.getActionMap().put("right-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                right = true;
            }
        });

        //UP KEY-RELEASE:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W,0,true),"up-release");
        component.getActionMap().put("up-release", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e){
                up = false;
            }
        });
         //DOWN KEY-RELEASE:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S,0,true),"down-release");
        component.getActionMap().put("down-release", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e){
                down = false;
            }
        });
         //LEFT KEY-RELEASE:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A,0,true),"left-release");
        component.getActionMap().put("left-release", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e){
                left = false;
            }
        });
         //RIGHT KEY-RELEASE:
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_D,0,true),"right-release");
        component.getActionMap().put("right-release", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e){
                right = false;
            }
        });

    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

}
