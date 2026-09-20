package puzzle;

public class PuzzleManager {

    private Puzzle1 puzzle1;

    public PuzzleManager() {
        puzzle1 = new Puzzle1();
    }

    public void openPuzzle(int puzzleId) {

        switch (puzzleId) {
            case 1:
                puzzle1.show();
                break;

            default:
                System.out.println("Unknown Puzzle");
                break;
        }
    }

}
