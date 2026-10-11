
package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class IntroUI {

    private static final Color WOOD = new Color(70, 42, 25);
    private static final Color BEIGE = new Color(235, 218, 180);

    private static final int PANEL_WIDTH = 820;
    private static final int PANEL_HEIGHT = 520;

    private int buttonX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;

    public void draw(Graphics2D original, int screenWidth, int screenHeight) {
        Graphics2D g = (Graphics2D) original.create();

        try {
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            // Dim background.
            g.setColor(new Color(0, 0, 0, 190));
            g.fillRect(0, 0, screenWidth, screenHeight);

            // Fit panel to screen.
            int panelWidth = Math.min(PANEL_WIDTH, screenWidth - 32);
            int panelHeight = Math.min(PANEL_HEIGHT, screenHeight - 32);

            int panelX = (screenWidth - panelWidth) / 2;
            int panelY = (screenHeight - panelHeight) / 2;

            // Panel.
            g.setColor(BEIGE);
            g.fillRoundRect(
                    panelX, panelY, panelWidth, panelHeight, 24, 24
            );

            g.setColor(WOOD);
            g.setStroke(new BasicStroke(4));
            g.drawRoundRect(
                    panelX, panelY, panelWidth, panelHeight, 24, 24
            );

            // Title.
            g.setColor(WOOD);
            g.setFont(new Font("Serif", Font.BOLD, 34));

            drawCentered(
                    g,
                    "A Little Trouble at Home",
                    screenWidth / 2,
                    panelY + 60
            );

            // Story.
            g.setFont(new Font("Serif", Font.PLAIN, 19));

            int textX = panelX + 42;
            int textY = panelY + 112;
            int lineHeight = 23;

            String[] story = {
                "The family has recently bought a new locker",
                "to keep their belongings safe.",
                "",
                "The children put the door key inside and",
                "locked it using a four-digit password.",
                "",
                "Unfortunately, the password was written on",
                "paper that the children tore into pieces",
                "and scattered around the room.",
                "",
                "Now the key is locked away, and the door",
                "cannot be opened."
            };

            for (String line : story) {
                if (!line.isEmpty()) {
                    g.drawString(line, textX, textY);
                }

                textY += lineHeight;
            }

            // Position mission using the actual end of the story.
            int missionY = textY + 8;

            g.setFont(new Font("Serif", Font.BOLD, 18));

            g.drawString(
                    "Your mission: Find the password, retrieve the key,",
                    textX,
                    missionY
            );

            g.drawString(
                    "unlock the locker, and escape the room!",
                    textX,
                    missionY + 24
            );

            // Button inside the panel.
            buttonWidth = 280;
            buttonHeight = 46;

            buttonX = (screenWidth - buttonWidth) / 2;
            buttonY = panelY + panelHeight - buttonHeight - 12;

            g.setColor(WOOD);
            g.fillRoundRect(
                    buttonX, buttonY,
                    buttonWidth, buttonHeight,
                    14, 14
            );

            g.setColor(BEIGE);
            g.setFont(new Font("Serif", Font.BOLD, 19));

            FontMetrics metrics = g.getFontMetrics();
            int buttonTextY = buttonY
                    + (buttonHeight - metrics.getHeight()) / 2
                    + metrics.getAscent();

            drawCentered(
                    g,
                    "BEGIN EXPLORATION",
                    screenWidth / 2,
                    buttonTextY
            );

        } finally {
            g.dispose();
        }
    }

    private void drawCentered(
            Graphics2D g,
            String text,
            int centerX,
            int y
    ) {
        FontMetrics metrics = g.getFontMetrics();

        int x = centerX - metrics.stringWidth(text) / 2;
        g.drawString(text, x, y);
    }

    public boolean isBeginButtonClicked(int mouseX, int mouseY) {
        return mouseX >= buttonX
                && mouseX <= buttonX + buttonWidth
                && mouseY >= buttonY
                && mouseY <= buttonY + buttonHeight;
    }
}
