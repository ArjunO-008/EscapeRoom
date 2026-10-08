package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import save.SaveManager;

public class MainMenu {

    private boolean hasSave;

    public void show() {

        hasSave = SaveManager.hasSave();

        System.out.println("Main menu");

        if (hasSave) {
            System.out.println("Continue Game available.");
        } else {
            System.out.println("No previous game.");
            System.out.println("Start New Game available.");
        }
    }

    public void startNewGame() {

        System.out.println("Starting new game...");

        SaveManager.createNewSave();
    }

    public void continueGame() {

        if (!SaveManager.hasSave()) {

            System.out.println(
                    "No game available to continue."
            );

            return;
        }

        System.out.println("Continuing game...");

        SaveManager.load();
    }

    /**
     * Draws the main menu.
     */
    public void draw(
            Graphics2D g,
            int width,
            int height
    ) {

        /*
         * Background.
         */
        g.setColor(
                new Color(25, 18, 14)
        );

        g.fillRect(
                0,
                0,
                width,
                height
        );

        /*
         * Title.
         */
        g.setColor(
                new Color(235, 218, 180)
        );

        g.setFont(
                new Font(
                        "Serif",
                        Font.BOLD,
                        56
                )
        );

        String title = "ESCAPE ROOM";

        int titleWidth =
                g.getFontMetrics().stringWidth(title);

        g.drawString(
                title,
                (width - titleWidth) / 2,
                180
        );

        /*
         * Subtitle.
         */
        g.setFont(
                new Font(
                        "Serif",
                        Font.PLAIN,
                        20
                )
        );

        String subtitle =
                "A Mystery Awaits...";

        int subtitleWidth =
                g.getFontMetrics().stringWidth(subtitle);

        g.drawString(
                subtitle,
                (width - subtitleWidth) / 2,
                220
        );

        /*
         * Start New Game.
         */
        drawButton(
                g,
                "START NEW GAME",
                width / 2,
                330
        );

        /*
         * Continue Game.
         */
        if (hasSave) {

            drawButton(
                    g,
                    "CONTINUE",
                    width / 2,
                    400
            );
        }

        /*
         * Instruction.
         */
        g.setFont(
                new Font(
                        "Serif",
                        Font.PLAIN,
                        16
                )
        );

        String instruction =
                "Press ENTER to start a new game";

        int instructionWidth =
                g.getFontMetrics().stringWidth(
                        instruction
                );

        g.drawString(
                instruction,
                (width - instructionWidth) / 2,
                540
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

        /*
         * Button background.
         */
        g.setColor(
                new Color(105, 65, 38)
        );

        g.fillRect(
                x,
                y,
                buttonWidth,
                buttonHeight
        );

        /*
         * Border.
         */
        g.setColor(
                new Color(235, 218, 180)
        );

        g.drawRect(
                x,
                y,
                buttonWidth,
                buttonHeight
        );

        /*
         * Text.
         */
        g.setFont(
                new Font(
                        "Serif",
                        Font.BOLD,
                        20
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