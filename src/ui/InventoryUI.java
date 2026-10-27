package ui;

import inventory.InventoryManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class InventoryUI {

    private static final int SLOT_COUNT = 3;
    private static final int SLOT_SIZE = 56;

    private static final int MARGIN = 0;
    private static final int PADDING = 7;

    private static final int HEADER_FONT_SIZE = 19;
    private static final int HEADER_RADIUS = 14;

    private static final int BORDER_THICKNESS = 3;

    private static final Color WOOD = new Color(70, 42, 25);
    private static final Color BEIGE = new Color(235, 218, 180);
    private static final Color DARKER_BEIGE = new Color(205, 185, 145);

    private final InventoryManager inventoryManager;

    private final Map<Integer, BufferedImage> itemImages = new HashMap<>();

    private final Font headerFont =
            new Font(Font.SERIF, Font.BOLD, HEADER_FONT_SIZE);

    public InventoryUI(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
        loadItemImages();
    }

    private void loadItemImages() {
        try {
            BufferedImage keyImage = core.Assets.image("src/map/resources/Key.png");

            itemImages.put(1, keyImage);

        } catch (Exception e) {
            System.err.println(
                    "Failed to load inventory item images.");
        }
    }

    public void draw(Graphics2D g, int screenHeight) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setStroke(new BasicStroke(
                    BORDER_THICKNESS,
                    BasicStroke.CAP_SQUARE,
                    BasicStroke.JOIN_ROUND));

            int bottomX = MARGIN;
            int bottomY = MARGIN + getHeaderHeight() - 2;

            int bottomWidth = SLOT_COUNT * SLOT_SIZE;
            int bottomHeight = SLOT_SIZE;

            drawBottomSection(
                    g2,
                    bottomX,
                    bottomY,
                    bottomWidth,
                    bottomHeight);

            drawHeader(
                    g2,
                    bottomX,
                    bottomY,
                    bottomWidth);

            drawItems(g2, bottomX, bottomY);

        } finally {
            g2.dispose();
        }
    }

    private int getHeaderHeight() {
        java.awt.FontMetrics metrics =
                java.awt.Toolkit.getDefaultToolkit()
                        .getFontMetrics(headerFont);

        return metrics.getHeight() + PADDING * 2;
    }

    private void drawHeader(
            Graphics2D g,
            int bottomX,
            int bottomY,
            int bottomWidth) {

        java.awt.FontMetrics metrics = g.getFontMetrics(headerFont);

        String title = "Inventory";

        int textWidth = metrics.stringWidth(title);
        int headerWidth = textWidth + PADDING * 2;
        int headerHeight = metrics.getHeight() + PADDING * 2;

        headerWidth = Math.min(
                headerWidth,
                (int) (bottomWidth * 0.75));

        int x = bottomX;
        int y = bottomY - headerHeight + 2;

        int right = x + headerWidth;
        int radius = HEADER_RADIUS;

        // Square top-left, rounded top-right,
        // and square bottom corners.
        Path2D.Double header = new Path2D.Double();

        header.moveTo(x, y);
        header.lineTo(right - radius, y);

        header.quadTo(
                right, y,
                right, y + radius);

        header.lineTo(right, bottomY);
        header.lineTo(x, bottomY);
        header.closePath();

        // Beige header background.
        g.setColor(WOOD);
        g.fill(header);

        // Brown header border.
        g.setColor(WOOD);
        g.draw(header);

        // Brown header text.
        g.setFont(headerFont);
        g.setColor(BEIGE);

        int textX = x + (headerWidth - textWidth) / 2;
        int textY = y + PADDING + metrics.getAscent();

        g.drawString(title, textX, textY);
    }

    private void drawBottomSection(
            Graphics2D g,
            int x,
            int y,
            int width,
            int height) {

        // Brown outer rectangle.
        g.setColor(WOOD);
        g.fillRect(x, y, width, height);

        // Three slot backgrounds:
        // warm beige, darker beige, warm beige.
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = x + i * SLOT_SIZE;

            g.setColor(
                    i == 1 ? DARKER_BEIGE : BEIGE);

            g.fillRect(
                    slotX + 2,
                    y + 2,
                    SLOT_SIZE - 4,
                    height - 4);
        }

        // Brown outer border.
        g.setColor(WOOD);
        g.drawRect(x, y, width, height);

        // Brown dividers between slots.
        for (int i = 1; i < SLOT_COUNT; i++) {
            int dividerX = x + i * SLOT_SIZE;

            g.drawLine(
                    dividerX,
                    y,
                    dividerX,
                    y + height);
        }
    }

    private void drawItems(
            Graphics2D g,
            int slotsX,
            int slotsY) {

        if (!inventoryManager.hasItem(1)) {
            return;
        }

        BufferedImage keyImage = itemImages.get(1);

        if (keyImage == null) {
            return;
        }

        int itemSize = 34;

        int itemX = slotsX + (SLOT_SIZE - itemSize) / 2;
        int itemY = slotsY + (SLOT_SIZE - itemSize) / 2;

        g.drawImage(
                keyImage,
                itemX,
                itemY,
                itemSize,
                itemSize,
                null);
    }
}