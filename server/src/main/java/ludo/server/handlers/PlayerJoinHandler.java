package ludo.server.handlers;

import ludo.core.game.GameManager;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.core.entities.Player;
import ludo.core.events.serverToClient.WaitingRoomUpdateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ludo.core.utils.Constants.*;

public class PlayerJoinHandler {
    private final GameManager gameManager;
    private final EventTransmitter eventTransmitter;
    private final List<String> availableColors;

    public PlayerJoinHandler(Server server) {
        this.gameManager = GameManager.getInstance();
        this.eventTransmitter = new EventTransmitter(server.getAllConnections());
        this.availableColors = new ArrayList<>();
        initializeAvailableColors();
    }

    private void initializeAvailableColors() {
        availableColors.add(RED);
        availableColors.add(BLUE);
        availableColors.add(GREEN);
        availableColors.add(YELLOW);
    }

    public boolean canPlayerJoin(String playerName) {
        // Check if game is full
        if (gameManager.getPlayers().size() >= MAX_PLAYERS) {
            return false;
        }

        // Check if game already started
        if (gameManager.isGameStarted()) {
            return false;
        }

        // Check if name is already taken
        return gameManager.getPlayers().stream()
            .noneMatch(p -> p.getName().equals(playerName));
    }

    public String getAvailableColor() {
        List<String> usedColors = gameManager.getPlayers().stream()
            .map(Player::getColor)
            .collect(Collectors.toList());

        return availableColors.stream()
            .filter(color -> !usedColors.contains(color))
            .findFirst()
            .orElse(null);
    }

    public void handlePlayerJoin(String playerName, String desiredColor) {
        if (!canPlayerJoin(playerName)) {
            Server.LOGGER.warning("Player cannot join: " + playerName);
            return;
        }

        // Validate and possibly adjust color choice
        String assignedColor = validateColor(desiredColor);
        if (assignedColor == null) {
            Server.LOGGER.warning("No colors available for player: " + playerName);
            return;
        }

        // Create and add new player
        Player newPlayer = new Player(playerName, assignedColor);
        gameManager.addPlayer(newPlayer);

        // Update waiting room for all players
        updateWaitingRoom();

        // Check if game can start
        checkGameStart();
    }

    private String validateColor(String desiredColor) {
        if (desiredColor != null && isColorAvailable(desiredColor)) {
            return desiredColor;
        }
        return getAvailableColor();
    }

    private boolean isColorAvailable(String color) {
        return gameManager.getPlayers().stream()
            .noneMatch(p -> p.getColor().equals(color));
    }

    private void updateWaitingRoom() {
        List<String> playerNames = gameManager.getPlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());

        WaitingRoomUpdateEvent updateEvent = new WaitingRoomUpdateEvent(
            playerNames,
            gameManager.getPlayers().stream()
                .collect(Collectors.toMap(Player::getName, Player::getColor)),
            MAX_PLAYERS
            );

        try {
            eventTransmitter.broadcast(updateEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to broadcast waiting room update: " + e.getMessage());
        }
    }

    private void checkGameStart() {
        if (gameManager.getPlayers().size() >= 2 && !gameManager.isGameStarted()) {
            // Start game automatically when minimum players reached
            gameManager.startGame();
        }
    }

    public void handlePlayerDisconnect(String playerName) {
        gameManager.getPlayers().stream()
            .filter(p -> p.getName().equals(playerName))
            .findFirst()
            .ifPresent(player -> {
                // Return color to available pool
                if (!gameManager.isGameStarted()) {
                    availableColors.add(player.getColor());
                }

                // Remove player
                gameManager.getPlayers().remove(player);

                // Update waiting room
                updateWaitingRoom();
            });
    }
}
