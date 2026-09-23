package save;

import java.io.*;

public class SaveManager {

    // --------------------------------------------------
    // Save file location
    // --------------------------------------------------

    private static final String DATA_FOLDER = "data";

    private static final String SAVE_FILE =
            DATA_FOLDER + File.separator + "save.dat";


    // --------------------------------------------------
    // Initialize save system
    // --------------------------------------------------

    public static void initializeDataFolder() {

        createDataFolder();
    }


    // --------------------------------------------------
    // Check whether a save exists
    // --------------------------------------------------

    public static boolean hasSave() {

        File saveFile = new File(SAVE_FILE);

        return saveFile.exists();
    }


    // --------------------------------------------------
    // Create data folder
    // --------------------------------------------------

    private static void createDataFolder() {

        File dataFolder = new File(DATA_FOLDER);

        if (!dataFolder.exists()) {

            dataFolder.mkdirs();
        }
    }


    // --------------------------------------------------
    // Create a new game
    // --------------------------------------------------

    public static void createNewSave() {

        // Make sure data/ exists
        createDataFolder();

        File saveFile = new File(SAVE_FILE);


        try {

            // ------------------------------------------
            // Delete previous save
            // ------------------------------------------

            if (saveFile.exists()) {

                saveFile.delete();
            }


            // ------------------------------------------
            // Create initial game state
            // ------------------------------------------

            int[] inventory = new int[0];

            boolean[] puzzlesCompleted =
                    new boolean[1];


            SaveData newGame = new SaveData(

                    true,       // Continuity

                    100.0f,     // Player X
                    100.0f,     // Player Y

                    1,          // Starting Room ID

                    inventory,

                    puzzlesCompleted
            );


            // ------------------------------------------
            // Save initial game state
            // ------------------------------------------

            save(newGame);

        }

        catch (Exception e) {

            System.out.println(
                    "Failed to create new game."
            );

            e.printStackTrace();
        }
    }


    // --------------------------------------------------
    // Save game data
    // --------------------------------------------------

    public static void save(SaveData saveData) {

        // Make sure data/ exists
        createDataFolder();


        try (
                FileOutputStream fileOutputStream =
                        new FileOutputStream(SAVE_FILE);

                ObjectOutputStream objectOutputStream =
                        new ObjectOutputStream(fileOutputStream)
        ) {

            objectOutputStream.writeObject(saveData);

            System.out.println("Game Saved.");

        }

        catch (IOException e) {

            System.out.println(
                    "Failed to save game."
            );

            e.printStackTrace();
        }
    }


    // --------------------------------------------------
    // Load game data
    // --------------------------------------------------

    public static SaveData load() {

        File saveFile = new File(SAVE_FILE);


        // No save exists
        if (!saveFile.exists()) {

            System.out.println(
                    "No save found."
            );

            return null;
        }


        try (
                FileInputStream fileInputStream =
                        new FileInputStream(SAVE_FILE);

                ObjectInputStream objectInputStream =
                        new ObjectInputStream(fileInputStream)
        ) {

            SaveData saveData =
                    (SaveData) objectInputStream.readObject();


            System.out.println("Game Loaded.");

            return saveData;

        }

        catch (IOException | ClassNotFoundException e) {

            System.out.println(
                    "Failed to load save."
            );

            e.printStackTrace();

            return null;
        }
    }


    // --------------------------------------------------
    // Delete save
    // --------------------------------------------------

    public static void deleteSave() {

        File saveFile = new File(SAVE_FILE);


        if (!saveFile.exists()) {

            System.out.println(
                    "No save exists."
            );

            return;
        }


        if (saveFile.delete()) {

            System.out.println(
                    "Save Deleted."
            );

        }

        else {

            System.out.println(
                    "Failed to delete save."
            );
        }
    }
}