package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import ludo.client.LudoGame;

public class UsernameScreen extends BaseScreen {
    private TextField usernameField;
    private Label errorLabel;
    private final SelectBox<String> colorSelect;

    public UsernameScreen(final LudoGame game) {
        super(game);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10).width(200);

        // Title
        Label titleLabel = new Label("Choose Your Name", skin, "default");
        mainTable.add(titleLabel).colspan(2).pad(50);
        mainTable.row();

        // Username field
        mainTable.add(new Label("Username:", skin)).align(Align.right);
        usernameField = new TextField("", skin);
        mainTable.add(usernameField).align(Align.left);
        mainTable.row();

        // Color selection
        mainTable.add(new Label("Color:", skin)).align(Align.right);
        colorSelect = new SelectBox<>(skin);
        colorSelect.setItems("Red", "Blue", "Green", "Yellow");
        mainTable.add(colorSelect).align(Align.left);
        mainTable.row();

        // Error label
        errorLabel = new Label("", skin);
        errorLabel.setColor(1, 0, 0, 1);
        mainTable.add(errorLabel).colspan(2).pad(20);
        mainTable.row();

        // Join button
        TextButton joinButton = new TextButton("Join Game", skin);
        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                attemptJoin();
            }
        });
        mainTable.add(joinButton).colspan(2).pad(20);

        stage.addActor(mainTable);
    }

    private void attemptJoin() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            errorLabel.setText("Please enter a username");
            return;
        }

        // Attempt to join with selected username and color
        if (game.getGameStateManager().joinGame(username, colorSelect.getSelected())) {
            // Move to lobby screen
            game.setScreen(new LobbyScreen(game));
        } else {
            errorLabel.setText("Username or color already taken");
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render(delta);
    }
}
