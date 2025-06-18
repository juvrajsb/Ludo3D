package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
//import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import ludo.client.LudoGame;

public class MenuScreen extends BaseScreen {
//    private Texture logoTexture;
//    private Image logoImage;

    public MenuScreen(final LudoGame game) {
        super(game);

//        logoTexture = new Texture(Gdx.files.internal("images/iconw.png"));
//        logoImage = new Image(logoTexture);

        createUI();
    }

    private void createUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10).width(200);

        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        titleStyle.font = skin.getFont("default");
        titleStyle.fontColor = Color.WHITE;

        Label titleLabel = new Label("LUDO 3D", titleStyle);
        titleLabel.setFontScale(3.0f);

        mainTable.add(titleLabel).pad(50).row();

        // Add logo at the top
//        mainTable.add(logoImage).width(400).height(200).padBottom(50).row();

        TextButton playButton = new TextButton("Play", skin);
        TextButton rulesButton = new TextButton("Rules", skin);
        TextButton exitButton = new TextButton("Exit", skin);
        disconnectButton.remove();

        mainTable.add(playButton).width(200).height(30).row();
        mainTable.add(rulesButton).width(200).height(30).row();
        mainTable.add(exitButton).width(200).height(30).row();

        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ConnectionScreen(game));
            }
        });

        rulesButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showRulesDialog();
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        stage.addActor(mainTable);
    }

    private void showRulesDialog() {
        Dialog dialog = new Dialog("Game Rules", skin, "dialog");
        dialog.setModal(true);
        dialog.setMovable(true);

        String rulesText = "Board Layout:\n" +
            "  - Board Structure: Square board with a cross-shaped path.\n" +
            "  - Player Areas: Four colored sections (Y, R, B, G).\n" +
            "  - Home Base: Each player has a base for four pawns.\n" +
            "  - Main Track: 52 spaces around the perimeter.\n" +
            "  - Home Column: Final 6 spaces leading to the finish.\n" +
            "  - Safe Spots: Marked spaces where pawns cannot be captured.\n\n" +

            "Game Setup:\n" +
            "  - Players: 2-4 players, each assigned a color.\n" +
            "  - Pawns: Each player has 4 pawns.\n" +
            "  - Starting: All pawns begin in their home bases.\n\n" +

            "Turn Sequence:\n" +
            "  - Rolling: Player rolls a single six-sided die.\n" +
            "  - Movement Options:\n" +
            "      - Roll a 6 to bring a pawn from home to the start.\n" +
            "      - Move an active pawn forward by the die value.\n" +
            "      - If no move is possible, the turn passes.\n" +
            "  - Extra Turn: Rolling a 6 gives an another turn.\n" +

            "Movement Rules:\n" +
            "  - Leaving Home: A 6 must be rolled.\n" +
            "  - Movement: Pawns move clockwise.\n" +
            "  - Exact Count: Pawns need an exact roll to enter the Target Base.\n\n" +

            "Capture Rules:\n" +
            "  - Capture: Landing on an opponent's pawn sends it home.\n" +
            "  - Safe Spots: Pawns on safe spots cannot be captured.\n" +
            "  - Home Column: Pawns in the home column cannot be captured.\n\n" +

            "Winning Conditions:\n" +
            "  - Objective: Be the first to move all four pawns to the finish.\n" +
            "  - Game End: The game ends when a Player finishes.";

        Label rulesLabel = new Label(rulesText, skin);
        rulesLabel.setWrap(true);

        ScrollPane scrollPane = new ScrollPane(rulesLabel, skin);
        scrollPane.setFadeScrollBars(false);

        dialog.getContentTable().add(scrollPane).width(Gdx.graphics.getWidth() * 0.8f).height(Gdx.graphics.getHeight() * 0.7f);

        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(closeButton).pad(10);
        dialog.show(stage);
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        super.dispose();
//        if (logoTexture != null) {
//            logoTexture.dispose();
//        }
    }
}
