package ludo.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ludo.core.entities.Board;
import ludo.core.entities.Player;
import ludo.core.entities.Pawn;
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
    private static final String SAVE_DIR = "LudoSaves";
    public static final String SAVE_FILE = SAVE_DIR + "/ludo_save.json";
    public static final String AUTO_SAVE_FILE = SAVE_DIR + "/ludo_autosave.json";
    private static final int CURRENT_SAVE_VERSION = 1;
    public static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();

    // Data class to represent saveable game state
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

    // Data class to represent saveable player state
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

    // Data class to represent saveable pawn state
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

    public static void saveGame(List<Player> players, String currentPlayerColor, GameState gameState) {
        // Generate timestamped filename for manual saves
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = SAVE_DIR + "/ludo_save_" + timestamp + ".json";
        saveGame(players, currentPlayerColor, gameState, fileName);
    }

    public static void autoSave(List<Player> players, String currentPlayerColor, GameState gameState) {
        saveGame(players, currentPlayerColor, gameState, AUTO_SAVE_FILE);
    }

    private static void saveGame(List<Player> players, String currentPlayerColor, GameState gameState, String fileName) {
        // Ensure save directory exists
        File saveDir = new File(SAVE_DIR);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        LOGGER.info("Starting save process to file: " + fileName);
        LOGGER.info("Input parameters - Players count: " + (players != null ? players.size() : "null") +
            ", Current color: " + currentPlayerColor +
            ", Game state: " + gameState);

        if (players == null || players.isEmpty()) {
            LOGGER.severe("Cannot save game: players list is null or empty");
            throw new IllegalStateException("Invalid game state: no players");
        }

        if (currentPlayerColor == null || currentPlayerColor.isEmpty()) {
            LOGGER.severe("Cannot save game: current player color is null or empty");
            throw new IllegalStateException("Invalid game state: no current player");
        }

        if (gameState == null) {
            LOGGER.severe("Cannot save game: game state is null");
            throw new IllegalStateException("Invalid game state: null game state");
        }

        GameSaveData saveData = new GameSaveData();
        saveData.currentPlayerColor = currentPlayerColor;
        saveData.gameState = gameState;

        LOGGER.info("Processing " + players.size() + " players");
        for (Player player : players) {
            if (player != null) {
                LOGGER.info("Processing player: " + player.getName() + " (Color: " + player.getColor() + ")");
                PlayerSaveData playerData = new PlayerSaveData();
                playerData.name = player.getName();
                playerData.color = player.getColor();

                List<Pawn> playerPawns = player.getPawns();
                if (playerPawns != null) {
                    LOGGER.info("Player has " + playerPawns.size() + " pawns");
                    for (Pawn pawn : playerPawns) {
                        if (pawn != null) {
                            PawnSaveData pawnData = new PawnSaveData();
                            pawnData.position = pawn.getPosition();
                            pawnData.isHome = pawn.isHome();
                            pawnData.isFinished = pawn.isFinished();
                            LOGGER.info("Saving pawn - Position: " + pawnData.position +
                                ", isHome: " + pawn.isHome() +
                                ", isFinished: " + pawn.isFinished());
                            playerData.pawns.add(pawnData);
                        } else {
                            LOGGER.warning("Null pawn found for player: " + player.getName());
                        }
                    }
                } else {
                    LOGGER.warning("Null pawns list for player: " + player.getName());
                }

                saveData.players.add(playerData);
                LOGGER.info("Added player data to save - Name: " + playerData.name +
                    ", Color: " + playerData.color +
                    ", Pawns count: " + playerData.pawns.size());
            } else {
                LOGGER.warning("Null player found in players list");
            }
        }

        LOGGER.info("Final save data - Players count: " + saveData.players.size() +
            ", Current color: " + saveData.currentPlayerColor +
            ", Game state: " + saveData.gameState);

        if (!saveData.isValid()) {
            LOGGER.severe("Save data validation failed");
            throw new IllegalStateException("Invalid game state");
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(saveData, writer); // todo check autosave is being saved in the server module
            LOGGER.info("Game saved successfully to " + fileName);
        } catch (IOException e) {
            LOGGER.severe("Failed to save game: " + e.getMessage());
            throw new RuntimeException("Failed to save game", e);
        }
    }

    public static GameSaveData loadGame() {
        // Find the most recent save file
        File saveDir = new File(SAVE_DIR);
        File[] saveFiles = saveDir.listFiles((dir, name) -> name.startsWith("ludo_save_") && name.endsWith(".json"));

        if (saveFiles != null && saveFiles.length > 0) {
            // Sort by last modified time, most recent first
            Arrays.sort(saveFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            return loadGame(saveFiles[0].getName());
        }

        return null;
    }

    public static List<String> listSaveFiles() {
        File saveDir = new File(SAVE_DIR);
        File[] saveFiles = saveDir.listFiles((dir, name) -> name.startsWith("ludo_save_") && name.endsWith(".json"));

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
        File saveFile = fileName.startsWith(SAVE_DIR) ? new File(fileName) : new File(SAVE_DIR, fileName);
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

    public static boolean hasSaveGame() {
        return new File(SAVE_FILE).exists();
    }

    public static boolean hasAutoSave() {
        return new File(AUTO_SAVE_FILE).exists();
    }

    public static void deleteSaveGame() {
        deleteFile(SAVE_FILE);
    }

    public static void deleteAutoSave() {
        deleteFile(AUTO_SAVE_FILE);
    }

    private static void deleteFile(String fileName) {
        File file = fileName.startsWith(SAVE_DIR + "/") ? new File(fileName) : new File(SAVE_DIR, fileName);
        if (file.exists()) {
            if (file.delete()) {
                LOGGER.info("Successfully deleted " + fileName);
            } else {
                LOGGER.warning("Failed to delete " + fileName);
            }
        }
    }

    // Save a winning scenario
    public static void saveWinningScenario(List<Player> players, String currentPlayerColor, GameState gameState) {
        saveGame(players, currentPlayerColor, gameState, SAVE_DIR + "/demo_winning.json");
    }

    // Save a mid-game scenario
    public static void saveMidGameScenario(List<Player> players, String currentPlayerColor, GameState gameState) {
        saveGame(players, currentPlayerColor, gameState, SAVE_DIR + "/demo_midgame.json");
    }

    // Save a start game scenario
    public static void saveStartGameScenario(List<Player> players, String currentPlayerColor, GameState gameState) {
        saveGame(players, currentPlayerColor, gameState, SAVE_DIR + "/demo_start.json");
    }

    public static GameSaveData loadAutoSave() {
        return loadGame(AUTO_SAVE_FILE);
    }

    public static void saveGame(GameSaveData saveData, String fileName) throws IOException {
        String json = gson.toJson(saveData);
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(json);
        }
    }
}
