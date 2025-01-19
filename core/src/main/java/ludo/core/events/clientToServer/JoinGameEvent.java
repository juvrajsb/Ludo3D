//package ludo.core.events.clientToServer;
//
//import ludo.core.events.Event;
//
///**
// * Event that represent a Client joining a game.
// * It contains the username that the Client wants
// * to use in the game.
// */
//public class JoinGameEvent extends Event {
//    private final String playerName;
//    private final String desiredColor;
//
//    public JoinGameEvent(String playerName, String desiredColor) {
//        this.ID = "JOIN_GAME_EVENT";
//        this.playerName = playerName;
//        this.desiredColor = desiredColor;
//    }
//
//    public String getPlayerName() {
//        return playerName;
//    }
//
//    public String getDesiredColor() {
//        return desiredColor;
//    }
//
//    @Override
//    public void callHandler() {
//        ConnectionsController.getInstance().handle(this);
//    }
//}
