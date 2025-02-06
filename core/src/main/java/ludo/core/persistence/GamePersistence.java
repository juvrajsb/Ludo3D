package ludo.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ludo.core.entities.Board;
import ludo.core.entities.Player;
import ludo.core.entities.Pawn;
import ludo.core.game.GameState;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GamePersistence {
    private static final String SAVE_FILE = "ludo_save.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Data class to represent saveable game state
    public static class GameSaveData {
        public List<PlayerSaveData> players;
        public String currentPlayerColor;
        public GameState gameState;
        public long timestamp;

        public GameSaveData() {
            this.players = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
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
    }

    // Data class to represent saveable pawn state
    public static class PawnSaveData {
        public int position;
        public boolean isHome;
        public boolean isFinished;
        public Board.GridPosition gridPosition;
    }

    public static void saveGame(List<Player> players, String currentPlayerColor, GameState gameState) {
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

        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(saveData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameSaveData loadGame() {
        File saveFile = new File(SAVE_FILE);
        if (!saveFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(saveFile)) {
            return gson.fromJson(reader, GameSaveData.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean hasSaveGame() {
        File saveFile = new File(SAVE_FILE);
        return saveFile.exists();
    }

    public static void deleteSaveGame() {
        File saveFile = new File(SAVE_FILE);
        if (saveFile.exists()) {
            saveFile.delete();
        }
    }
}
