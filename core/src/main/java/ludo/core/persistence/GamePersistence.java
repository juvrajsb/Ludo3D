package ludo.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ludo.core.game.GameState;
import ludo.core.utils.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class GamePersistence {
    private static final Logger LOGGER = Logger.getLogger(GamePersistence.class.getName());
    private static final String SAVE_DIR = "../assets/LudoSaves";
    public static final String SAVE_FILE = "ludo_save.json";
    public static final String AUTO_SAVE_FILE = "ludo_autosave.json";

    private static final int CURRENT_SAVE_VERSION = 1;
    public static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();

    public static class GameSaveData {
        public int saveVersion;
        public List<PlayerSaveData> players;
        public String currentPlayerColor;
        public GameState gameState;
        public long timestamp;

        public GameSaveData() {
            this.saveVersion = CURRENT_SAVE_VERSION;
            this.players = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            if (players == null || currentPlayerColor == null || gameState == null) {
                return false;
            }

            // Check if all players have valid data
            for (PlayerSaveData player : players) {
                if (!player.isValid()) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class PlayerSaveData {
        public String name;
        public String color;
        public List<PawnSaveData> pawns;

        public PlayerSaveData() {
            this.pawns = new ArrayList<>();
        }

        public boolean isValid() {
            return name != null && !name.isEmpty() &&
                   color != null && !color.isEmpty() &&
                   pawns != null && pawns.size() == 4 &&
                   pawns.stream().allMatch(PawnSaveData::isValid);
        }
    }

    public static class PawnSaveData {
        public int position;
        public boolean isHome;
        public boolean isFinished;

        public boolean isValid() {
            // Allow -1 for home position, otherwise must be within board bounds or in home column
            if (isHome) {
                return position == -1;
            }
            if (isFinished) {
                return position >= Constants.BOARD_SIZE;
            }
            return position >= 0 && position < Constants.BOARD_SIZE + Constants.HOME_COLUMN_SIZE;
        }
    }

    public static List<String> listSaveFiles() {
        File saveDir = new File(SAVE_DIR);
        LOGGER.info("Listing save files from directory: " + saveDir.getAbsolutePath());
        if (!saveDir.exists()) {
            LOGGER.warning("Save directory does not exist at the checked path.");
        }
        File[] saveFiles = saveDir.listFiles((dir, name) -> name.startsWith("ludo_") && name.endsWith(".json"));

        List<String> saveFileNames = new ArrayList<>();
        if (saveFiles != null) {
            // Sort by last modified time, most recent first
            Arrays.sort(saveFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File file : saveFiles) {
                saveFileNames.add(file.getName());
            }
        }

        return saveFileNames;
    }

    public static GameSaveData loadGame(String fileName) {
        // If the fileName already contains the full path, use it directly
        File saveFile = new File(SAVE_DIR, fileName);
        if (!saveFile.exists()) {
            LOGGER.info("No save file found at " + saveFile.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(saveFile)) {
            GameSaveData saveData = gson.fromJson(reader, GameSaveData.class);

            if (saveData == null) {
                LOGGER.severe("Failed to parse save file: null data");
                return null;
            }

            if (saveData.saveVersion > CURRENT_SAVE_VERSION) {
                LOGGER.severe("Save file version " + saveData.saveVersion +
                    " is newer than current version " + CURRENT_SAVE_VERSION);
                return null;
            }

            if (!saveData.isValid()) {
                LOGGER.severe("Invalid save data detected");
                return null;
            }

            LOGGER.info("Game loaded successfully from " + fileName);
            return saveData;
        } catch (IOException e) {
            LOGGER.severe("Failed to load game: " + e.getMessage());
            return null;
        }
    }

    public static void saveGame(GameSaveData saveData, String fileName) throws IOException {
        // Ensure the single, correct save directory exists.
        File saveDir = new File(SAVE_DIR);
        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new IOException("Failed to create save directory: " + saveDir.getAbsolutePath());
            }
        }

        // Create the final File object from the correct directory and filename.
        File saveFile = new File(saveDir, fileName);
        String json = gson.toJson(saveData);
        try (FileWriter writer = new FileWriter(saveFile)) {
            writer.write(json);
        }
    }
}
