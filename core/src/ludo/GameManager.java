package src.ludo;

import src.ludo.server.LudoGame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameManager {
    private Client client;
    private int currentPlayerId;
    private boolean isCurrentPlayerTurn;
    private String playerColor;
    private boolean gameStarted = false;

    public GameManager(Client client) {
        this.client = client;
        this.currentPlayerId = -1;
        this.isCurrentPlayerTurn = false;
    }

    public void update() {
        // Handle game state updates
    }

    public boolean isCurrentPlayerTurn() {
        return isCurrentPlayerTurn;
    }

    public int getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void rollDice() {
        if (isCurrentPlayerTurn) {
            client.sendToServer("ROLL_DICE");
        } else {
            System.out.println("It's not your turn to roll the dice");
        }
    }

    public void movePawn(int pawnIndex, int steps) {
        if (isCurrentPlayerTurn) {
            if (pawnIndex < 0 || pawnIndex >= 4) {
                System.out.println("Invalid pawn index");
                return;
            }
            if (steps < 1 || steps > 12) {
                System.out.println("Invalid number of steps");
                return;
            }
            client.sendToServer("MOVE_PAWN " + pawnIndex + " " + steps);
        } else {
            System.out.println("It's not your turn to move a pawn");
        }
    }

    public void setPlayerColor(String color) {
        this.playerColor = color;
    }

    public void setCurrentPlayerTurn(String playerColor) {
        isCurrentPlayerTurn = this.playerColor.equals(playerColor);
        // Notify the client to update UI
        client.updateTurnIndicator(isCurrentPlayerTurn);
    }

    public void handleServerMessage(String message) {
        try {
            String[] parts = message.split(" ", 2);
            switch (parts[0]) {
                case "ERROR":
                    client.handleErrorMessage(parts[1]);
                    break;
                case "TURN_TIMEOUT":
                    client.handleTurnTimeout(parts[1]);
                    break;
                case "PLAYER_REPLACED":
                    handlePlayerReplaced(parts[1]);
                    break;
                case "TURN":
                    setCurrentPlayerTurn(parts[1]);
                    break;
                case "DICE_RESULT":
                    int result = Integer.parseInt(parts[1]);
                    client.setDiceRollResult(result);
                    break;
                case "MOVE_RESULT":
                    boolean success = Boolean.parseBoolean(parts[1]);
                    if (!success) {
                        client.showInvalidMoveMessage();
                    }
                    break;
                case "GAME_STATE":
                    client.updateGameState(parts[1]);
                    break;
                case "GAME_OVER":
                    client.handleGameOver(parts[1]);
                    break;
                case "LOBBY_UPDATE":
                    client.lobby.updatePlayerList(parts[1].split(","));
                    break;
                case "CHAT":
                    client.lobby.addChatMessage(parts[1]);
                    break;
                case "GAME_STARTED":
                    setGameStarted(true);
                    client.updateGameStatus("Game started!");
                    break;
                default:
                    System.out.println("Unknown message type: " + parts[0]);
            }
        } catch (Exception e) {
            System.err.println("Error handling server message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerReplaced(String message) {
        String[] parts = message.split(" ");
        String replacedColor = parts[0];
        String replacementType = parts[1];
        System.out.println("Player " + replacedColor + " was replaced by a " + replacementType);
        // Update the game state or UI to reflect the player replacement
    }

    public void dispose() {
        // Clean up resources if needed
    }

    public void setGameStarted(boolean started) {
        this.gameStarted = started;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }
}