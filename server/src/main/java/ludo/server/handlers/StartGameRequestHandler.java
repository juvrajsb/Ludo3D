package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.core.events.clientToServer.StartGameRequestEvent;
import ludo.core.events.serverToClient.GameStartedEvent;
import ludo.core.events.serverToClient.StartGameResponseEvent;
import ludo.core.game.GameManager;
import ludo.core.persistence.GamePersistence;
import ludo.server.state.ServerGameStateManager;

import static ludo.server.Server.LOGGER;

public class StartGameRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        StartGameRequestEvent req = (StartGameRequestEvent) event;
        String connectionId = req.getConnection().getConnectionID();
        GameManager gameManager = gsm.getGameManager();

        if (!gsm.isFirstPlayer(connectionId)) {
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.NOT_ADMIN)
            );
            return;
        }

        boolean enableBots = req.isBotsEnabled();
        boolean loadSavedGame = req.isLoadSavedGame();
        LOGGER.info("Game start request details - Real players: " + gameManager.getPlayers().size() +
            ", Enable bots: " + enableBots +
            ", Load saved game: " + loadSavedGame);

        if (gameManager.isGameStarted()) {
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.GAME_ALREADY_STARTED)
            );
            return;
        }

        if (loadSavedGame) {
            LOGGER.info("Attempting to load saved game");
            String saveFileName = req.getSaveFileName();
            if (saveFileName != null) {
                GamePersistence.GameSaveData saveData = GamePersistence.loadGame(saveFileName);
                if (saveData != null) {
                    LOGGER.info("Successfully loaded save data - Players: " + saveData.players.size() +
                        ", Current color: " + saveData.currentPlayerColor +
                        ", Game state: " + saveData.gameState);

                    gsm.loadSavedGameState(saveData, connectionId);

                    GameStartedEvent gameStartedEvent = new GameStartedEvent(
                        gameManager.getPlayers(),
                        gameManager.getCurrentPlayer().getName(),
                        gameManager.getPlayers().size()
                    );
                    gsm.getNetworkHandler().broadcast(gameStartedEvent);

                    gsm.broadcastGameState();
                    gsm.checkAndPlayBotTurn();
                    return;
                }
            }
            LOGGER.warning("Failed to load saved game data, falling back to new game");
        }

        int realPlayerCount = gameManager.getPlayers().size();
        if (realPlayerCount < 2 && !enableBots) {
            LOGGER.info("Not enough players to start a new game");
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.NOT_ENOUGH_PLAYERS)
            );
            return;
        }

        if (enableBots) {
            gsm.addBotPlayers();
        }

        gsm.initializePlayersWithColors(gameManager.getPlayers(), enableBots);
        gameManager.startGame();

        GameStartedEvent gameStartedEvent = new GameStartedEvent(
            gameManager.getPlayers(),
            gameManager.getCurrentPlayer().getName(),
            gameManager.getPlayers().size()
        );
        gsm.getNetworkHandler().broadcast(gameStartedEvent);

        gsm.getNetworkHandler().sendToClient(
            connectionId,
            new StartGameResponseEvent(StartGameResponseEvent.Response.OK)
        );

        gsm.broadcastGameState();
        gsm.checkAndPlayBotTurn();
        gsm.logGameState();
    }
}
