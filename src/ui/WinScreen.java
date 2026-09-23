package ui;

import save.SaveManager;

public class WinScreen {

    public void show() {

        System.out.println("You Win");

        // The game has been completed.
        // There should no longer be a continuation save.
        SaveManager.deleteSave();
    }
}