package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedList;

public class InfoUI {

    private static class Message {
        String text;

        // Set when the message reaches the front of the queue (i.e. when it
        // actually becomes visible), NOT when it was queued.
        long shownAt = -1;

        Message(String text) {
            this.text = text;
        }
    }

    private static final int MAX_QUEUED = 4;

    private final LinkedList<Message> messages = new LinkedList<>();

    // How long a message stays visible.
    private static final long DISPLAY_TIME = 2500;

    // UI dimensions
    private static final int BOX_WIDTH = 420;
    private static final int BOX_HEIGHT = 50;

    public void show(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        // Ignore an exact repeat of the newest message (e.g. mashing E on a
        // locked door) and don't let the queue grow without limit.
        Message last = messages.peekLast();

        if (last != null && last.text.equals(message)) {
            return;
        }

        if (messages.size() >= MAX_QUEUED) {
            return;
        }

        messages.add(new Message(message));
    }

    private long elapsed(Message m) {
        if (m.shownAt < 0) {
            return 0;
        }

        return System.currentTimeMillis() - m.shownAt;
    }

    public void update() {
        Message current = messages.peek();

        if (current == null) {
            return;
        }

        // First frame this message is at the front: start its timer now.
        if (current.shownAt < 0) {
            current.shownAt = System.currentTimeMillis();
            return;
        }

        if (elapsed(current) >= DISPLAY_TIME) {
            messages.poll();
        }
    }

    public void draw(Graphics2D g, int screenWidth, int screenHeight) {

        Message current = messages.peek();

        if (current == null) {
            return;
        }

        long elapsed = elapsed(current);

        // Fade in/out
        float alpha = 1.0f;

        if (elapsed < 300) {
            alpha = elapsed / 300.0f;
        } else if (elapsed > DISPLAY_TIME - 500) {
            alpha =
                    (DISPLAY_TIME - elapsed) / 500.0f;
        }

        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        int x = (screenWidth - BOX_WIDTH) / 2;
        int y = screenHeight - 90;

        // Background
        g2.setComposite(
                java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER,
                        0.88f * alpha
                )
        );

        g2.setColor(new Color(35, 25, 20));
        g2.fillRoundRect(
                x,
                y,
                BOX_WIDTH,
                BOX_HEIGHT,
                14,
                14
        );

        // Border
        g2.setColor(new Color(220, 190, 150));
        g2.drawRoundRect(
                x,
                y,
                BOX_WIDTH,
                BOX_HEIGHT,
                14,
                14
        );

        // Text
        g2.setComposite(
                java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER,
                        alpha
                )
        );

        g2.setColor(Color.WHITE);
        g2.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        15
                )
        );

        // Center text
        int textWidth =
                g2.getFontMetrics().stringWidth(current.text);

        int textX =
                x + (BOX_WIDTH - textWidth) / 2;

        int textY =
                y + 31;

        g2.drawString(
                current.text,
                textX,
                textY
        );

        g2.dispose();
    }
}