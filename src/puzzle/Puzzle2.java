package puzzle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class Puzzle2 {

    private static final int SEQUENCE_LENGTH = 4;
    private static final int NUM_BUTTONS = 4;
    private static final int STEP_DELAY_MS = 600;

    // The four symbol colors - peach, brown, light green, dark green.
    private static final Color[] COLORS = {
        new Color(255, 218, 185), // peach
        new Color(139, 90, 43),   // brown
        new Color(144, 238, 144), // light green
        new Color(0, 100, 0)      // dark green
    };

    // --- Theme colors for the dialog/popups, matching the safe's keypad ---
    private static final Color COLOR_BACKGROUND = new Color(222, 184, 135); // peach/burlywood
    private static final Color COLOR_BUTTON = new Color(139, 90, 43);       // warm brown
    private static final Color COLOR_BUTTON_TEXT = Color.WHITE;
    private static final Color COLOR_TEXT = new Color(101, 67, 33);         // dark brown

    private PuzzleManager puzzleManager;

    private List<Integer> sequence = new ArrayList<>();
    private List<Integer> playerInput = new ArrayList<>();

    private JDialog dialog;
    private JButton[] buttons;
    private boolean acceptingInput = false;


    public Puzzle2(PuzzleManager puzzleManager) {
        this.puzzleManager = puzzleManager;
    }


    // Opens the symbol-matching dialog. Called by the symbol lock when interacted with.
    public void show() {
        buildDialog();
        startNewRound();
        dialog.setVisible(true); // modal - blocks here until the dialog is closed
    }

    private void buildDialog() {
        dialog = new JDialog((Frame) null, "Symbol Lock", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(COLOR_BACKGROUND);

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setBackground(COLOR_BACKGROUND);
        grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buttons = new JButton[NUM_BUTTONS];

        for (int i = 0; i < NUM_BUTTONS; i++) {
            int index = i; // needs to be effectively final to use inside the lambda below

            JButton button = new JButton();
            button.setBackground(COLORS[i]);
            button.setOpaque(true);
            button.addActionListener(e -> onButtonClicked(index));

            buttons[i] = button;
            grid.add(button);
        }

        dialog.add(grid, BorderLayout.CENTER);
        dialog.setSize(260, 260);
        dialog.setLocationRelativeTo(null);
    }

    // Picks a brand new random sequence and plays it back to the player.
    private void startNewRound() {
        sequence.clear();
        playerInput.clear();
        acceptingInput = false;

        Random random = new Random();

        for (int i = 0; i < SEQUENCE_LENGTH; i++) {
            // nextInt(NUM_BUTTONS) returns a random number from
            // 0 (inclusive) to NUM_BUTTONS (exclusive) - i.e. 0-3.
            sequence.add(random.nextInt(NUM_BUTTONS));
        }

        playSequence();
    }

    // Flashes each button in the sequence, one at a time, using a Timer.
    private void playSequence() {
        Timer timer = new Timer(STEP_DELAY_MS, null);
        int[] step = {0}; // array trick so the lambda below can modify it

        timer.addActionListener(e -> {
            if (step[0] > 0) {
                buttons[sequence.get(step[0] - 1)].setBorder(null);
            }

            if (step[0] >= sequence.size()) {
                timer.stop();
                acceptingInput = true;
                return;
            }

            buttons[sequence.get(step[0])].setBorder(
                    BorderFactory.createLineBorder(Color.WHITE, 4));

            step[0]++;
        });

        timer.setInitialDelay(300);
        timer.start();
    }

    private void onButtonClicked(int index) {
        if (!acceptingInput) {
            return; // ignore clicks while the demo sequence is still playing
        }

        playerInput.add(index);

        int pos = playerInput.size() - 1;

        if (!playerInput.get(pos).equals(sequence.get(pos))) {
            showThemedMessage("Wrong sequence! Watch again.");
            startNewRound(); // reshuffle - fresh random sequence each failed attempt
            return;
        }

        if (playerInput.size() == sequence.size()) {
            dialog.dispose();
            solved();
        }
    }

    private void solved() {
        puzzleManager.completePuzzle(2);
        showThemedMessage("The lock clicks into place!");
    }

    // A small themed popup matching the keypad's brown/peach look.
    private void showThemedMessage(String message) {

        JDialog popup = new JDialog((Frame) null, "Symbol Lock", true); // true = modal
        popup.getContentPane().setBackground(COLOR_BACKGROUND);

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        label.setForeground(COLOR_TEXT);

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        okButton.setBackground(COLOR_BUTTON);
        okButton.setForeground(COLOR_BUTTON_TEXT);
        okButton.setOpaque(true);
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> popup.dispose());

        popup.setLayout(new BorderLayout(10, 10));
        popup.add(label, BorderLayout.CENTER);
        popup.add(okButton, BorderLayout.SOUTH);

        ((JPanel) popup.getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(16, 16, 12, 16));

        popup.setSize(320, 130);
        popup.setResizable(false);
        popup.setLocationRelativeTo(null);

        popup.setVisible(true); // blocks here until the player clicks OK
    }
}