package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedList;
import java.util.Queue;

public class InfoUI {

    private static class Message {
        String text;
        long createdAt;

        Message(String text) {
            this.text = text;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private final Queue<Message> messages = new LinkedList<>();

    // How long a message stays visible.
    private static final long DISPLAY_TIME = 2500;

    // UI dimensions
    private static final int BOX_WIDTH = 420;
    private static final int BOX_HEIGHT = 50;

    public void show(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        messages.add(new Message(message));
    }

    public void update() {
        Message current = messages.peek();

        if (current == null) {
            return;
        }

        long elapsed =
                System.currentTimeMillis() - current.createdAt;

        if (elapsed >= DISPLAY_TIME) {
            messages.poll();
        }
    }

    public void draw(Graphics2D g, int screenWidth, int screenHeight) {

        Message current = messages.peek();

        if (current == null) {
            return;
        }

        long elapsed =
                System.currentTimeMillis() - current.createdAt;

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