package ui;

import save.SaveManager;

public class MainMenu {

    public void show() {

        System.out.println("Main menu");


        // Check whether a previous game exists
        if (SaveManager.hasSave()) {

            System.out.println("Continue Game available.");

        } else {

            System.out.println("No previous game.");
            System.out.println("Start New Game available.");
        }
    }


    // --------------------------------------------------
    // Start New Game
    // --------------------------------------------------

    public void startNewGame() {

        System.out.println("Starting new game...");

        SaveManager.createNewSave();
    }


    // --------------------------------------------------
    // Continue Game
    // --------------------------------------------------

    public void continueGame() {

        if (!SaveManager.hasSave()) {

            System.out.println(
                    "No game available to continue."
            );

            return;
        }


        System.out.println("Continuing game...");

        SaveManager.load();
    }
}