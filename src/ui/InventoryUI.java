package ui;

import inventory.InventoryManager;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class InventoryUI {

    private static final int PANEL_WIDTH = 80;
    private static final int SLOT_SIZE = 60;
    private static final int SLOT_GAP = 10;

    private final InventoryManager inventoryManager;

    private final Map<Integer, BufferedImage> itemImages =
            new HashMap<>();

    public InventoryUI(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;

        loadItemImages();
    }

    private void loadItemImages() {

        try {
            BufferedImage keyImage =
                    ImageIO.read(
                            new File("src/map/resources/Key.png")
                    );

            itemImages.put(1, keyImage);

        } catch (Exception e) {
            System.out.println(
                    "Failed to load inventory item images."
            );
        }
    }

    public void draw(Graphics2D g, int screenHeight) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        /*
         * Inventory position
         *
         * Left side of the screen.
         */
        int panelX = 10;
        int panelY = 20;

        /*
         * Background
         */
        g2.setComposite(
                AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        0.88f
                )
        );

        g2.setColor(new Color(30, 24, 20));

        int panelHeight =
                55 + SLOT_SIZE + SLOT_GAP + 10;

        g2.fillRoundRect(
                panelX,
                panelY,
                PANEL_WIDTH,
                panelHeight,
                12,
                12
        );

        /*
         * Border
         */
        g2.setColor(
                new Color(220, 190, 150)
        );

        g2.drawRoundRect(
                panelX,
                panelY,
                PANEL_WIDTH,
                panelHeight,
                12,
                12
        );

        /*
         * Inventory title
         */
        g2.setColor(Color.WHITE);

        g2.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        10
                )
        );

        String title = "ITEMS";

        int titleWidth =
                g2.getFontMetrics()
                        .stringWidth(title);

        g2.drawString(
                title,
                panelX +
                        (PANEL_WIDTH - titleWidth) / 2,
                panelY + 20
        );

        /*
         * Item slot
         */
        int slotX =
                panelX +
                (PANEL_WIDTH - SLOT_SIZE) / 2;

        int slotY =
                panelY + 28;

        g2.setColor(
                new Color(15, 15, 15)
        );

        g2.fillRoundRect(
                slotX,
                slotY,
                SLOT_SIZE,
                SLOT_SIZE,
                8,
                8
        );

        g2.setColor(
                new Color(150, 130, 110)
        );

        g2.drawRoundRect(
                slotX,
                slotY,
                SLOT_SIZE,
                SLOT_SIZE,
                8,
                8
        );

        /*
         * Draw Key
         */
        if (inventoryManager.hasItem(1)) {

            BufferedImage keyImage =
                    itemImages.get(1);

            if (keyImage != null) {

                int padding = 8;

                g2.drawImage(
                        keyImage,
                        slotX + padding,
                        slotY + padding,
                        SLOT_SIZE - padding * 2,
                        SLOT_SIZE - padding * 2,
                        null
                );
            }
        }

        g2.dispose();
    }
}