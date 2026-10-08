package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import save.SaveManager;

public class WinScreen {

    public void show() {

        System.out.println("You Win");

        // The game has been completed.
        // There should no longer be a continuation save.
        SaveManager.deleteSave();
    }

    public void draw(
            Graphics2D g,
            int width,
            int height
    ) {

        // =========================
        // BACKGROUND
        // =========================

        g.setColor(
                new Color(25, 18, 14)
        );

        g.fillRect(
                0,
                0,
                width,
                height
        );

        // =========================
        // MAIN TITLE
        // =========================

        g.setColor(
                new Color(235, 218, 180)
        );

        g.setFont(
                new Font(
                        "Serif",
                        Font.BOLD,
                        52
                )
        );

        String title = "YOU ESCAPED!";

        int titleWidth =
                g.getFontMetrics()
                        .stringWidth(title);

        g.drawString(
                title,
                (width - titleWidth) / 2,
                230
        );

        // =========================
        // SUBTITLE
        // =========================

        g.setFont(
                new Font(
                        "Serif",
                        Font.PLAIN,
                        22
                )
        );

        String subtitle =
                "Congratulations, you solved the mystery.";

        int subtitleWidth =
                g.getFontMetrics()
                        .stringWidth(subtitle);

        g.drawString(
                subtitle,
                (width - subtitleWidth) / 2,
                275
        );

        // =========================
        // COMPLETION MESSAGE
        // =========================

        g.setFont(
                new Font(
                        "Serif",
                        Font.PLAIN,
                        18
                )
        );

        String message =
                "The door is open. You made it out.";

        int messageWidth =
                g.getFontMetrics()
                        .stringWidth(message);

        g.drawString(
                message,
                (width - messageWidth) / 2,
                315
        );

        // =========================
        // BUTTON
        // =========================

        drawButton(
                g,
                "RETURN TO MENU",
                width / 2,
                410
        );

        // =========================
        // FOOTER
        // =========================

        g.setFont(
                new Font(
                        "Serif",
                        Font.PLAIN,
                        15
                )
        );

        String footer =
                "Press ENTER to return to the main menu";

        int footerWidth =
                g.getFontMetrics()
                        .stringWidth(footer);

        g.drawString(
                footer,
                (width - footerWidth) / 2,
                520
        );
    }

    private void drawButton(
            Graphics2D g,
            String text,
            int centerX,
            int centerY
    ) {

        int buttonWidth = 260;
        int buttonHeight = 55;

        int x =
                centerX - buttonWidth / 2;

        int y =
                centerY - buttonHeight / 2;

        // Button background
        g.setColor(
                new Color(105, 65, 38)
        );

        g.fillRect(
                x,
                y,
                buttonWidth,
                buttonHeight
        );

        // Border
        g.setColor(
                new Color(235, 218, 180)
        );

        g.drawRect(
                x,
                y,
                buttonWidth,
                buttonHeight
        );

        // Text
        g.setFont(
                new Font(
                        "Serif",
                        Font.BOLD,
                        19
                )
        );

        int textWidth =
                g.getFontMetrics()
                        .stringWidth(text);

        int textX =
                centerX - textWidth / 2;

        int textY =
                centerY
                        + g.getFontMetrics()
                                .getAscent() / 2;

        g.drawString(
                text,
                textX,
                textY
        );
    }
}