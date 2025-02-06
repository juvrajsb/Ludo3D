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
    private static final String SAVE_FILE = "ludo_save.json";
    private static final String AUTO_SAVE_FILE = "ludo_autosave.json";
    private static final int CURRENT_SAVE_VERSION = 1;
    private static final Gson gson = new GsonBuilder()
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
        public Board.GridPosition gridPosition;

        public boolean isValid() {
            return position >= 0 && position < Constants.BOARD_SIZE;
        }
    }

    public static void saveGame(List<Player> players, String currentPlayerColor, GameState gameState) {
        saveGame(players, currentPlayerColor, gameState, SAVE_FILE);
    }

    public static void autoSave(List<Player> players, String currentPlayerColor, GameState gameState) {
        saveGame(players, currentPlayerColor, gameState, AUTO_SAVE_FILE);
    }

    private static void saveGame(List<Player> players, String currentPlayerColor, GameState gameState, String fileName) {
        GameSaveData saveData = new GameSaveData();
        saveData.currentPlayerColor = currentPlayerColor;
        saveData.gameState = gameState;

        for (Player player : players) {
            PlayerSaveData playerData = new PlayerSaveData();
            playerData.name = player.getName();
            playerData.color = player.getColor();

            for (Pawn pawn : player.getPawns()) {
                PawnSaveData pawnData = new PawnSaveData();
                pawnData.position = pawn.getPosition();
                pawnData.isHome = pawn.isHome();
                pawnData.isFinished = pawn.isFinished();

                // Save grid position if pawn is on board
                if (!pawn.isHome()) {
                    Board board = new Board(); // Create temporary board for position conversion
                    pawnData.gridPosition = board.getGridPosition(pawn.getPosition());
                }

                playerData.pawns.add(pawnData);
            }

            saveData.players.add(playerData);
        }

        if (!saveData.isValid()) {
            LOGGER.severe("Attempted to save invalid game state");
            throw new IllegalStateException("Invalid game state");
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(saveData, writer);
            LOGGER.info("Game saved successfully to " + fileName);
        } catch (IOException e) {
            LOGGER.severe("Failed to save game: " + e.getMessage());
            throw new RuntimeException("Failed to save game", e);
        }
    }

    public static GameSaveData loadGame() {
        return loadGame(SAVE_FILE);
    }

    public static GameSaveData loadAutoSave() {
        return loadGame(AUTO_SAVE_FILE);
    }

    private static GameSaveData loadGame(String fileName) {
        File saveFile = new File(fileName);
        if (!saveFile.exists()) {
            LOGGER.info("No save file found at " + fileName);
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
        File file = new File(fileName);
        if (file.exists()) {
            if (file.delete()) {
                LOGGER.info("Successfully deleted " + fileName);
            } else {
                LOGGER.warning("Failed to delete " + fileName);
            }
        }
    }
}
