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
