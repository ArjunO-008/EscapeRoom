package save;

import java.io.Serializable;

public class SaveData implements Serializable {

    // Used to identify the save format version.
    // Useful if we change the save structure later.
    private static final long serialVersionUID = 1L;


    // --------------------------------------------------
    // Save data
    // --------------------------------------------------

    // Is there a game that can be continued?
    private boolean continuity;

    // Player position
    private float playerX;
    private float playerY;

    // Current room
    private int currentRoomId;

    // IDs of collected items
    private int[] inventory;

    // Puzzle completion status
    // Index 0 = Puzzle 1
    // Index 1 = Puzzle 2
    // etc.
    private boolean[] puzzlesCompleted;


    // --------------------------------------------------
    // Constructor
    // --------------------------------------------------

    public SaveData(
            boolean continuity,
            float playerX,
            float playerY,
            int currentRoomId,
            int[] inventory,
            boolean[] puzzlesCompleted) {

        this.continuity = continuity;
        this.playerX = playerX;
        this.playerY = playerY;
        this.currentRoomId = currentRoomId;
        this.inventory = inventory;
        this.puzzlesCompleted = puzzlesCompleted;
    }


    // --------------------------------------------------
    // Getters
    // --------------------------------------------------

    public boolean isContinuity() {
        return continuity;
    }

    public float getPlayerX() {
        return playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    public int getCurrentRoomId() {
        return currentRoomId;
    }

    public int[] getInventory() {
        return inventory;
    }

    public boolean[] getPuzzlesCompleted() {
        return puzzlesCompleted;
    }


    // --------------------------------------------------
    // Setters
    // --------------------------------------------------

    public void setContinuity(boolean continuity) {
        this.continuity = continuity;
    }

    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }

    public void setPlayerY(float playerY) {
        this.playerY = playerY;
    }

    public void setCurrentRoomId(int currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public void setInventory(int[] inventory) {
        this.inventory = inventory;
    }

    public void setPuzzlesCompleted(boolean[] puzzlesCompleted) {
        this.puzzlesCompleted = puzzlesCompleted;
    }
}

