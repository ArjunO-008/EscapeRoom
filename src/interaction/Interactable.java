package interaction;

import java.awt.Graphics2D;
import java.awt.Rectangle;

public interface Interactable {
    Rectangle getInteractionBounds();
    void interact();
    boolean isActive();
    void draw(Graphics2D g);
}
