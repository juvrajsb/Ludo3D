package ludo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class Lobby {
    private Stage stage;
    private Skin skin;
    private Table mainTable;
    private TextButton startGameButton;
    private List<String> playerList;
    private TextField chatInput;
    private TextArea chatArea;
    private Client client;

    public Lobby(Client client) {
        this.client = client;
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        createUI();
    }

    private void createUI() {
        mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        playerList = new List<>(skin);
        ScrollPane playerScrollPane = new ScrollPane(playerList, skin);

        chatArea = new TextArea("", skin);
        chatArea.setDisabled(true);
        ScrollPane chatScrollPane = new ScrollPane(chatArea, skin);

        chatInput = new TextField("", skin);
        chatInput.setMessageText("Type your message here...");

        startGameButton = new TextButton("Start Game", skin);
        startGameButton.setDisabled(true);

        mainTable.add(new Label("Players:", skin)).colspan(2);
        mainTable.row();
        mainTable.add(playerScrollPane).width(200).height(200).colspan(2);
        mainTable.row();
        mainTable.add(chatScrollPane).width(400).height(200).colspan(2);
        mainTable.row();
        mainTable.add(chatInput).width(300);
        mainTable.add(new TextButton("Send", skin)).width(100);
        mainTable.row();
        mainTable.add(startGameButton).colspan(2);

        // Add listeners for buttons and chat input
        addListeners();
    }

    private void addListeners() {
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                client.sendToServer("START_GAME");
            }
        });

        chatInput.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    sendChatMessage();
                    return true;
                }
                return false;
            }
        });
    }

    private void sendChatMessage() {
        String message = chatInput.getText();
        if (!message.isEmpty()) {
            client.sendToServer("CHAT " + message);
            chatInput.setText("");
        }
    }

    public void updatePlayerList(String[] players) {
        playerList.setItems(players);
        startGameButton.setDisabled(players.length < 2);
    }

    public void addChatMessage(String message) {
        chatArea.appendText(message + "\n");
        chatArea.setCursorPosition(chatArea.getText().length());
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
