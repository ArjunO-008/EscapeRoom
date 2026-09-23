package ui;

import java.awt.*;

public class RoomSelector {

    private boolean visible = false;
    private int currentRoomId;

    private int hoveredRoomId = -1;

    public void show(int currentRoomId) {
        this.currentRoomId = currentRoomId;
        this.visible = true;
        this.hoveredRoomId = -1;
    }

    public void hide() {
        visible = false;
        hoveredRoomId = -1;
    }

    public boolean isVisible() {
        return visible;
    }

    public void draw(Graphics2D g, int width, int height) {

        if (!visible)
            return;

        // =========================
        // PANEL
        // =========================

        int panelHeight = (int) (height * 0.18);
        int panelY = height - panelHeight;

        // Wooden border
        g.setColor(new Color(70, 42, 25));
        g.fillRect(0, panelY, width, panelHeight);

        // Beige interior
        g.setColor(new Color(235, 218, 180));
        g.fillRect(
                6,
                panelY + 6,
                width - 12,
                panelHeight - 12
        );

        // =========================
        // CONTENT
        // =========================

        int x = 25;
        int y = panelY + 25;

        // Title
        g.setColor(new Color(75, 45, 27));

        g.setFont(
                new Font(
                        "Serif",
                        Font.BOLD,
                        20
                )
        );

        g.drawString(
                "Where To Go?",
                x,
                y
        );

        y += 25;

        // =========================
        // ROOMS
        // =========================

        String[] rooms = {
                "Bedroom",
                "Kitchen",
                "Living Room"
        };

        int[] roomIds = {
                1,
                2,
                3
        };

        for (int i = 0; i < rooms.length; i++) {

            if (roomIds[i] == currentRoomId)
                continue;

            drawOption(
                    g,
                    rooms[i],
                    roomIds[i],
                    x,
                    y
            );

            y += 24;
        }

        // Cancel
        drawOption(
                g,
                "Cancel",
                0,
                x,
                y
        );
    }

    private void drawOption(
            Graphics2D g,
            String text,
            int id,
            int x,
            int y
    ) {

        boolean hovered = hoveredRoomId == id;

        g.setFont(
                new Font(
                        "Serif",
                        hovered ? Font.BOLD : Font.PLAIN,
                        17
                )
        );

        if (hovered) {

            // Small horizontal movement
            x += 8;

            g.setColor(
                    new Color(95, 55, 30)
            );

            // Pointer-style marker
            g.drawString(
                    ">",
                    x - 14,
                    y
            );

        } else {

            g.setColor(
                    new Color(100, 60, 35)
            );
        }

        g.drawString(
                text,
                x,
                y
        );
    }

    // =========================
    // CLICK
    // =========================

    public void handleClick(
            int mouseX,
            int mouseY,
            int width,
            int height
    ) {

        if (!visible)
            return;

        int panelHeight = (int) (height * 0.15);
        int panelY = height - panelHeight;

        int x = 25;
        int y = panelY + 25;

        // Skip title
        y += 25;

        String[] rooms = {
                "Bedroom",
                "Kitchen",
                "Living Room"
        };

        int[] roomIds = {
                1,
                2,
                3
        };

        for (int i = 0; i < rooms.length; i++) {

            if (roomIds[i] == currentRoomId)
                continue;

            if (isInsideOption(
                    mouseX,
                    mouseY,
                    x,
                    y
            )) {

                System.out.println(
                        "Selected: " + rooms[i]
                );

                hide();
                return;
            }

            y += 24;
        }

        // Cancel
        if (isInsideOption(
                mouseX,
                mouseY,
                x,
                y
        )) {

            hide();
        }
    }

    // =========================
    // HOVER
    // =========================

    public void handleMouseMove(
            int mouseX,
            int mouseY,
            int width,
            int height
    ) {

        if (!visible)
            return;

        hoveredRoomId = -1;

        int panelHeight = (int) (height * 0.15);
        int panelY = height - panelHeight;

        int x = 25;
        int y = panelY + 25;

        y += 25;

        String[] rooms = {
                "Bedroom",
                "Kitchen",
                "Living Room"
        };

        int[] roomIds = {
                1,
                2,
                3
        };

        for (int i = 0; i < rooms.length; i++) {

            if (roomIds[i] == currentRoomId)
                continue;

            if (isInsideOption(
                    mouseX,
                    mouseY,
                    x,
                    y
            )) {

                hoveredRoomId = roomIds[i];

                setHandCursor(true);

                return;
            }

            y += 24;
        }

        // Cancel
        if (isInsideOption(
                mouseX,
                mouseY,
                x,
                y
        )) {

            hoveredRoomId = 0;

            setHandCursor(true);

            return;
        }

        setHandCursor(false);
    }

    private boolean isInsideOption(
            int mouseX,
            int mouseY,
            int x,
            int y
    ) {

        /*
         * Large invisible hit area.
         *
         * The text itself doesn't need
         * to have a background.
         */

        return mouseX >= x &&
               mouseX <= x + 220 &&
               mouseY >= y - 18 &&
               mouseY <= y + 5;
    }

    private void setHandCursor(boolean hand) {

        // Cursor handling is done by GamePanel.
    }
}