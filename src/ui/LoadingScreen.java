package ui;

import save.SaveManager;

public class LoadingScreen {

    public void show() {

        System.out.println("Loading Screen...");

        // Make sure the data folder exists
        SaveManager.initializeDataFolder();
    }
}