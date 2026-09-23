package save;

import java.io.*;

public class SaveManager {

    private static final String DATA_FOLDER = "data";
    private static final String SAVE_FILE = DATA_FOLDER + File.separator + "save.dat";

    public static boolean hasSave() {
        File savFile = new File(SAVE_FILE);

        return savFile.exists();
    }

    private static void createDataFolder() {
        File dataFolder = new File(DATA_FOLDER);

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public static void createNewSave() {

        createDataFolder();
        File saveFile = new File(SAVE_FILE);

        try {
            if (saveFile.exists()) {
                saveFile.delete();
            }
            saveFile.createNewFile();
            System.out.println("New Save Created");
        } catch (IOException e) {
            System.out.println("Failed to create save File");
            e.printStackTrace();
        }
    }

    public static void deleteSave() {
        File saveFile = new File(SAVE_FILE);
        if (saveFile.exists()) {
            if (saveFile.delete()) {
                System.out.println("Save deleted.");
            } else {
                System.out.println("Failed to delete save.");
            }
        } else {
            System.out.println("No save exists.");
        }
    }
}
