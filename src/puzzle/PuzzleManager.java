package puzzle;

import save.SaveData;
import save.SaveManager;

public class PuzzleManager {

    private Puzzle1 puzzle1;

    // Stores whether each puzzle has been completed.
    // Index 0 = Puzzle 1
    // Index 1 = Puzzle 2
    // etc.
    private boolean[] solvedPuzzles;


    public PuzzleManager() {

        puzzle1 = new Puzzle1(this);

        // Currently we have only 1 puzzle.
        solvedPuzzles = new boolean[1];

        // Try to load previous puzzle state
        loadPuzzleState();
    }


    // --------------------------------------------------
    // Open a puzzle
    // --------------------------------------------------

    public void openPuzzle(int puzzleId) {

        // First check whether the puzzle is already solved
        if (isPuzzleSolved(puzzleId)) {

            System.out.println(
                "Puzzle " + puzzleId + " is already solved."
            );

            return;
        }


        switch (puzzleId) {

            case 1:
                puzzle1.show();
                break;

            default:
                System.out.println("Unknown Puzzle");
                break;
        }
    }


    // --------------------------------------------------
    // Check whether a puzzle is solved
    // --------------------------------------------------

    public boolean isPuzzleSolved(int puzzleId) {

        // Convert puzzle ID to array index
        int index = puzzleId - 1;

        // Invalid puzzle ID
        if (index < 0 || index >= solvedPuzzles.length) {
            return false;
        }

        return solvedPuzzles[index];
    }

    // --------------------------------------------------
    // Mark puzzle as completed
    // --------------------------------------------------

    public void completePuzzle(int puzzleId) {

        int index = puzzleId - 1;

        // Invalid puzzle ID
        if (index < 0 || index >= solvedPuzzles.length) {
            System.out.println(
                "Cannot complete unknown puzzle: " + puzzleId
            );

            return;
        }


        // Mark puzzle as solved
        solvedPuzzles[index] = true;

        System.out.println(
            "Puzzle " + puzzleId + " completed."
        );


        // Save the updated puzzle state
        savePuzzleState();
    }


    // --------------------------------------------------
    // Save puzzle state
    // --------------------------------------------------

    private void savePuzzleState() {

        // Load the current save
        SaveData saveData = SaveManager.load();

        // No save exists yet
        if (saveData == null) {

            System.out.println(
                "Cannot save puzzle state: no save exists."
            );

            return;
        }


        // Update puzzle state
        saveData.setPuzzlesCompleted(solvedPuzzles);

        // Save everything again
        SaveManager.save(saveData);
    }


    // --------------------------------------------------
    // Load puzzle state
    // --------------------------------------------------

    private void loadPuzzleState() {

        SaveData saveData = SaveManager.load();

        // No existing save
        if (saveData == null) {
            return;
        }


        boolean[] savedPuzzles =
                saveData.getPuzzlesCompleted();


        // Make sure the saved data fits
        // the number of puzzles currently in the game.
        if (savedPuzzles == null) {
            return;
        }


        // Copy the saved puzzle states
        int count = Math.min(
            solvedPuzzles.length,
            savedPuzzles.length
        );


        for (int i = 0; i < count; i++) {

            solvedPuzzles[i] = savedPuzzles[i];
        }
    }
}