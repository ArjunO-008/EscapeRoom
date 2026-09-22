package ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RoomSelector {

    public static void showRoomSelector(String currentRoom) {

        List<String> rooms = new ArrayList<>();

        rooms.add("Bedroom");
        rooms.add("Kitchen");
        rooms.add("Living Room");

        // Remove the room the player is currently in
        rooms.removeIf(room ->
                room.equalsIgnoreCase(currentRoom)
        );

        JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));

        JLabel title = new JLabel("Where to Go?", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));

        panel.add(title);

        for (String room : rooms) {
            JButton button = new JButton(room);

            button.addActionListener(e -> {
                System.out.println("Going to: " + room);

                // Room transition will go here later
            });

            panel.add(button);
        }

        JButton cancelButton = new JButton("Cancel");

        cancelButton.addActionListener(e -> {
            // Close UI
        });

        panel.add(cancelButton);

        JOptionPane.showOptionDialog(
                null,
                panel,
                "Room Selection",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new Object[]{},
                null
        );
    }
}