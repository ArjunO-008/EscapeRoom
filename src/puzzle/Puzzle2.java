package puzzle;

import audio.SoundManager;
import audio.SoundManager.Sfx;
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
    private Timer flashTimer;

    // How long a symbol stays lit, and the dark gap between two symbols.
    // The gap is what makes two identical symbols in a row look like two flashes.
    private static final int FLASH_MS = 450;
    private static final int GAP_MS = 200;


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
    // Each symbol has an ON phase (border shown) and an OFF phase (border
    // cleared, short dark gap) so repeated symbols are clearly two flashes.
    private void playSequence() {
        if (flashTimer != null) {
            flashTimer.stop();
        }

        for (JButton b : buttons) {
            b.setBorder(null);
        }

        int[] step = {0};          // how many symbols have been lit so far
        boolean[] lit = {false};   // true while a symbol is currently lit

        flashTimer = new Timer(GAP_MS, null);

        flashTimer.addActionListener(e -> {

            // Dialog was closed mid-demo: stop quietly.
            if (dialog == null || !dialog.isDisplayable()) {
                flashTimer.stop();
                return;
            }

            if (lit[0]) {
                // OFF phase: clear the border that was lit.
                buttons[sequence.get(step[0] - 1)].setBorder(null);
                lit[0] = false;
                flashTimer.setDelay(GAP_MS);

                if (step[0] >= sequence.size()) {
                    flashTimer.stop();
                    acceptingInput = true;
                }
                return;
            }

            // ON phase: light the next symbol (and play its tone).
            SoundManager.playSymbol(sequence.get(step[0]));
            buttons[sequence.get(step[0])].setBorder(
                    BorderFactory.createLineBorder(Color.WHITE, 4));
            step[0]++;
            lit[0] = true;
            flashTimer.setDelay(FLASH_MS);
        });

        flashTimer.setInitialDelay(300);
        flashTimer.start();
    }

    private void onButtonClicked(int index) {
        if (!acceptingInput) {
            return; // ignore clicks while the demo sequence is still playing
        }

        playerInput.add(index);

        int pos = playerInput.size() - 1;

        if (!playerInput.get(pos).equals(sequence.get(pos))) {
            SoundManager.play(Sfx.ERROR);
            showThemedMessage("Wrong sequence! Watch again.");
            startNewRound(); // reshuffle - fresh random sequence each failed attempt
            return;
        }

        // Correct press: play that button's tone.
        SoundManager.playSymbol(index);

        if (playerInput.size() == sequence.size()) {
            dialog.dispose();
            solved();
        }
    }

    private void solved() {
        SoundManager.play(Sfx.LOCK);
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
        okButton.addActionListener(e -> {
            SoundManager.play(Sfx.CLICK);
            popup.dispose();
        });

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