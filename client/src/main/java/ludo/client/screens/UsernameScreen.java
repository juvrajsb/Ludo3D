package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import ludo.client.LudoGame;
import ludo.core.events.serverToClient.Response;

public class UsernameScreen extends BaseScreen {
    private TextField usernameField;
    private Label errorLabel;
    private SelectBox<String> colorSelect;
    private SelectBox<Integer> playerCountSelect;
    private Table playerCountTable;
    private boolean joinInProgress = false;

    public UsernameScreen(final LudoGame game) {
        super(game);

        if (!game.getGameStateManager().isConnected()) {
            game.setScreen(new ConnectionScreen(game));
            return;
        }

        game.getGameStateManager().setCurrentScreen(this);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10).width(200);

        Label titleLabel = new Label("Choose Your Name", skin, "default");
        mainTable.add(titleLabel).colspan(2).pad(50).row();

        mainTable.add(new Label("Username:", skin)).align(Align.right);
        usernameField = new TextField("", skin);
        mainTable.add(usernameField).align(Align.left).row();

        mainTable.add(new Label("Color:", skin)).align(Align.right);
        colorSelect = new SelectBox<>(skin);
        colorSelect.setItems("Red", "Blue", "Green", "Yellow");
        mainTable.add(colorSelect).align(Align.left).row();

        errorLabel = new Label("", skin);
        errorLabel.setColor(1, 0, 0, 1);
        mainTable.add(errorLabel).colspan(2).pad(20).row();

        TextButton joinButton = new TextButton("Join Game", skin);
        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                attemptJoin();
            }
        });
        mainTable.add(joinButton).colspan(2).pad(20);

        stage.addActor(mainTable);

        // Check if first player to show player count selection
        if (game.getGameStateManager().isFirstPlayer()) {
            playerCountTable = new Table();
            playerCountTable.add(new Label("Number of Players:", skin)).align(Align.right);
            playerCountSelect = new SelectBox<>(skin);
            playerCountSelect.setItems(2, 3, 4);
            playerCountTable.add(playerCountSelect).align(Align.left);
            mainTable.add(playerCountTable).colspan(2).row();
        }
    }

    private void attemptJoin() {
        if (joinInProgress) {
            return;
        }

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            errorLabel.setText("Please enter a username");
            return;
        }

        // Clear any previous error
        errorLabel.setText("");
        joinInProgress = true;

        // Attempt to join
        if (!game.getGameStateManager().joinGame(username, colorSelect.getSelected())) {
            joinInProgress = false;
            errorLabel.setText("Failed to send join request");
        }
    }

    public void onJoinResponse(Response response) {
        joinInProgress = false;

        switch (response) {
            case OK:
            case FIRST_PLAYER:
                game.setScreen(new LobbyScreen(game));
                break;
            case COLOR_TAKEN:
                errorLabel.setText("This color is already taken. Please choose another color.");
                break;
            case USERNAME_TAKEN:
                errorLabel.setText("This username is already taken. Please choose another name.");
                break;
            case GAME_FULL:
                errorLabel.setText("The game is full. Please try again later.");
                break;
            default:
                errorLabel.setText("Join failed: " + response);
                break;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render(delta);
    }
}
