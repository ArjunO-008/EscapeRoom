package ui;

import java.awt.*;


public class RoomSelector {

    private boolean visible = false;
    private int currentRoomId;

    public void show(int currentRoomId) {
        this.currentRoomId = currentRoomId;
        this.visible = true;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void handleClick(int mouseX, int mouseY, int width, int height) {

        if (!visible)
            return;

        int panelHeight = 150;
        int panelY = height - panelHeight;

        // Cancel
        if (mouseX >= width / 2 - 60 &&
                mouseX <= width / 2 + 60 &&
                mouseY >= panelY + 105 &&
                mouseY <= panelY + 135) {

            hide();
            return;
        }

        int buttonWidth = 150;
        int buttonHeight = 40;
        int gap = 20;

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

        int availableRooms = 0;

        for (int roomId : roomIds) {
            if (roomId != currentRoomId)
                availableRooms++;
        }

        int totalWidth = availableRooms * buttonWidth +
                (availableRooms - 1) * gap;

        int x = (width - totalWidth) / 2;

        for (int i = 0; i < rooms.length; i++) {

            if (roomIds[i] == currentRoomId)
                continue;

            if (mouseX >= x &&
                    mouseX <= x + buttonWidth &&
                    mouseY >= panelY + 55 &&
                    mouseY <= panelY + 55 + buttonHeight) {

                System.out.println("Selected: " + rooms[i]);

                hide();
                return;
            }

            x += buttonWidth + gap;
        }
    }

    public void draw(Graphics2D g, int width, int height) {

        if (!visible)
            return;

        int panelHeight = 150;
        int panelY = height - panelHeight;

        // Background
        g.setColor(new Color(20, 20, 20, 230));
        g.fillRect(0, panelY, width, panelHeight);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        String title = "Where to Go?";
        int titleWidth = g.getFontMetrics().stringWidth(title);

        g.drawString(
                title,
                (width - titleWidth) / 2,
                panelY + 30);

        // Buttons
        int buttonWidth = 150;
        int buttonHeight = 40;
        int gap = 20;

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

        int availableRooms = 0;

        for (int roomId : roomIds) {
            if (roomId != currentRoomId)
                availableRooms++;
        }

        int totalWidth = availableRooms * buttonWidth +
                (availableRooms - 1) * gap;

        int x = (width - totalWidth) / 2;

        for (int i = 0; i < rooms.length; i++) {

            if (roomIds[i] == currentRoomId)
                continue;

            g.setColor(Color.DARK_GRAY);
            g.fillRect(
                    x,
                    panelY + 55,
                    buttonWidth,
                    buttonHeight);

            g.setColor(Color.WHITE);
            g.drawRect(
                    x,
                    panelY + 55,
                    buttonWidth,
                    buttonHeight);

            String text = rooms[i];

            int textWidth = g.getFontMetrics().stringWidth(text);

            g.drawString(
                    text,
                    x + (buttonWidth - textWidth) / 2,
                    panelY + 80);

            x += buttonWidth + gap;
        }

        // Cancel
        g.setColor(Color.GRAY);
        g.fillRect(
                width / 2 - 60,
                panelY + 105,
                120,
                30);

        g.setColor(Color.WHITE);
        g.drawString(
                "Cancel",
                width / 2 - 25,
                panelY + 126);
    }
}