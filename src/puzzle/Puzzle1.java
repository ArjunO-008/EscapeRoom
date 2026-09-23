package puzzle;

public class Puzzle1 {

    private PuzzleManager puzzleManager;


    public Puzzle1(PuzzleManager puzzleManager) {

        this.puzzleManager = puzzleManager;
    }


    public void show() {

        System.out.println("Puzzle 1");

    }


    // Call this when Puzzle 1 is solved
    public void solved() {

        puzzleManager.completePuzzle(1);

    }

}
