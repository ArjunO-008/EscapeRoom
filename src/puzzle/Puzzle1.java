package puzzle;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class Puzzle1 {

    // The code the player must find on the note and enter here.
    private static final String CODE = "4719";

    // --- Theme colors, brown/peach to match the game's look ---
    private static final Color COLOR_BACKGROUND = new Color(222, 184, 135); // peach/burlywood
    private static final Color COLOR_BUTTON = new Color(139, 90, 43);       // warm brown
    private static final Color COLOR_BUTTON_TEXT = Color.WHITE;
    private static final Color COLOR_DISPLAY_BG = new Color(255, 239, 213); // light peach
    private static final Color COLOR_DISPLAY_TEXT = new Color(101, 67, 33); // dark brown

    private PuzzleManager puzzleManager;


    public Puzzle1(PuzzleManager puzzleManager) {

        this.puzzleManager = puzzleManager;
    }


    // Opens the safe's numeric keypad. Called by the safe when interacted with.
    public void show() {

        JDialog dialog = new JDialog((Frame) null, "Safe", true); // true = modal
        dialog.getContentPane().setBackground(COLOR_BACKGROUND);

        // --- Display showing what's been typed so far ---
        StringBuilder entered = new StringBuilder();

        JTextField display = new JTextField();
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        display.setBackground(COLOR_DISPLAY_BG);
        display.setForeground(COLOR_DISPLAY_TEXT);

        // --- Keypad buttons, laid out like a phone: 7 8 9 / 4 5 6 / 1 2 3 / C 0 Enter ---
        JPanel keypad = new JPanel(new GridLayout(4, 3, 5, 5));
        keypad.setBackground(COLOR_BACKGROUND);

        String[] labels = {
                "7", "8", "9",
                "4", "5", "6",
                "1", "2", "3",
                "C", "0", "Enter"
        };

        for (String label : labels) {
            JButton button = new JButton(label);
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            button.setBackground(COLOR_BUTTON);
            button.setForeground(COLOR_BUTTON_TEXT);
            button.setOpaque(true);
            button.setFocusPainted(false); // removes the default blue focus ring

            button.addActionListener(e -> {
                switch (label) {
                    case "C":
                        entered.setLength(0);
                        display.setText("");
                        break;

                    case "Enter":
                        checkCode(dialog, entered, display);
                        break;

                    default:
                        entered.append(label);
                        display.setText(entered.toString());
                        break;
                }
            });

            keypad.add(button);
        }

        dialog.setLayout(new java.awt.BorderLayout(8, 8));
        dialog.add(display, java.awt.BorderLayout.NORTH);
        dialog.add(keypad, java.awt.BorderLayout.CENTER);

        dialog.setSize(260, 280);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true); // blocks here until the player closes the dialog
    }

    // Checks the entered code. Correct -> closes the dialog and solves the puzzle.
    // Wrong -> clears the display so the player can try again without reopening the safe.
    private void checkCode(JDialog dialog, StringBuilder entered, JTextField display) {

        if (entered.toString().equals(CODE)) {
            dialog.dispose();
            solved();
        } else {
            showThemedMessage("Incorrect code.");
            entered.setLength(0);
            display.setText("");
        }
    }

    // A small themed popup matching the keypad's brown/peach look,
    // used instead of the plain default JOptionPane styling.
    private void showThemedMessage(String message) {

        JDialog dialog = new JDialog((Frame) null, "Safe", true); // true = modal
        dialog.getContentPane().setBackground(COLOR_BACKGROUND);

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); // smaller so the longer message fits on one line
        label.setForeground(COLOR_DISPLAY_TEXT);

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        okButton.setBackground(COLOR_BUTTON);
        okButton.setForeground(COLOR_BUTTON_TEXT);
        okButton.setOpaque(true);
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> dialog.dispose());

        dialog.setLayout(new java.awt.BorderLayout(10, 10));
        dialog.add(label, java.awt.BorderLayout.CENTER);
        dialog.add(okButton, java.awt.BorderLayout.SOUTH);

        // A bit of breathing room around the edges.
        ((JPanel) dialog.getContentPane()).setBorder(
                javax.swing.BorderFactory.createEmptyBorder(16, 16, 12, 16));

        dialog.setSize(320, 130); // wide enough for the solved message at this font size
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true); // blocks here until the player clicks OK
    }


    // Call this when Puzzle 1 is solved
    public void solved() {

        puzzleManager.completePuzzle(1);

        showThemedMessage("The safe clicks open. Something glints inside.");
    }

}