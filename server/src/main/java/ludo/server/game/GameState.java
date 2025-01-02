package ludo.server.game;

public enum GameState {
    WAITING_FOR_PLAYERS("Waiting for players to join..."),
    IN_PROGRESS("Game in progress"),
    DICE_ROLLED("Dice has been rolled"),
    WAITING_FOR_MOVE("Waiting for player move"),
    PLAYER_MOVED("Player has moved"),
    GAME_OVER("Game is over");

    private final String description;

    GameState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
