package interaction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import player.Player;

public class InteractionSystem {

    private List<Interactable> interactables = new ArrayList<>();
    private Interactable nearby;

    public void setInteractables(List<Interactable> list) {
        interactables = (list != null) ? list : new ArrayList<>();
        nearby = null;
    }

    public void update(Player player) {
        Rectangle playerBounds = new Rectangle(
                player.getX(),
                player.getY(),
                player.getWidth(),
                player.getHeight());

        nearby = null;

        for (Interactable obj : interactables) {
            if (!obj.isActive()) {
                continue;
            }

            if (playerBounds.intersects(obj.getInteractionBounds())) {
                nearby = obj;
                break;
            }
        }
    }

    public void tryInteract() {
        if (nearby != null) {
            nearby.interact();
        }
    }

    public void drawObjects(Graphics2D g) {
        for (Interactable obj : interactables) {
            if (obj.isActive()) {
                obj.draw(g);
            }
        }
    }

    public void drawPrompt(Graphics2D g, int screenWidth, int screenHeight) {
        if (nearby == null) {
            return;
        }
        g.setColor(Color.WHITE);
        g.drawString("Press E to interact", 20, screenHeight - 20);
    }
}