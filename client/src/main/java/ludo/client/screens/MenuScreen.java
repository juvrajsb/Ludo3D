package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import ludo.client.LudoGame;
import ludo.client.ui.MenuUIHelper;

public class MenuScreen extends BaseScreen {
    private Texture logoTexture;

    public MenuScreen(final LudoGame game) {
        super(game);
        disconnectButton.remove();

        try {
            logoTexture = new Texture(Gdx.files.internal("images/iconw.png"));
            logoTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error("MenuScreen", "Could not load logo: " + e.getMessage());
            logoTexture = null;
        }

        createUI();
    }

    private void createUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();

        // Centered glassmorphic card
        Table card = MenuUIHelper.createCard(skin, 24);
        card.defaults().align(Align.center).padBottom(10);

        // 1. Signature 4-Color Accent Stripe
        card.add(MenuUIHelper.createColorStripe(skin, 4)).fillX().expandX().padBottom(18).row();

        // 2. Logo Badge
        if (logoTexture != null) {
            Image logoImage = new Image(logoTexture);
            logoImage.setScaling(Scaling.fit);
            card.add(logoImage).size(96, 96).padBottom(12).row();
        }

        // 3. Hero Title
        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        titleStyle.font = skin.getFont("window");
        titleStyle.fontColor = MenuUIHelper.TEXT_PRIMARY;

        Label titleLabel = new Label("LUDO 3D", titleStyle);
        titleLabel.setFontScale(1.6f);
        titleLabel.setAlignment(Align.center);
        card.add(titleLabel).padBottom(4).row();

        // Subtitle
        Label subtitleLabel = new Label("Classic Strategy - Real-Time 3D Multiplayer", skin);
        subtitleLabel.setColor(MenuUIHelper.TEXT_MUTED);
        subtitleLabel.setAlignment(Align.center);
        card.add(subtitleLabel).padBottom(24).row();

        // 4. Action Buttons
        TextButton playButton = new TextButton("Play Multiplayer", skin);
        TextButton rulesButton = new TextButton("Game Rules", skin);
        TextButton exitButton = new TextButton("Exit Game", skin);

        card.add(playButton).width(260).height(46).padBottom(12).row();
        card.add(rulesButton).width(260).height(40).padBottom(12).row();
        card.add(exitButton).width(260).height(36).padBottom(16).row();

        // 5. Footer info
        Label footerLabel = new Label("2-4 Players - Online Multiplayer & Bot Support", skin);
        footerLabel.setColor(new Color(0.5f, 0.55f, 0.65f, 0.9f));
        footerLabel.setFontScale(0.85f);
        footerLabel.setAlignment(Align.center);
        card.add(footerLabel).padTop(4).row();

        // Button listeners
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

        rootTable.add(card).width(420);
        stage.addActor(rootTable);
    }

    private void showRulesDialog() {
        Dialog dialog = new Dialog("Game Rules & Guide", skin);
        dialog.setModal(true);
        dialog.setMovable(true);

        Table content = new Table();
        content.defaults().left().padBottom(6);

        addRuleSection(content, "1. OBJECTIVE",
            "Be the first player to guide all 4 pawns from your Home Base around the perimeter track and into your center Target Finish.");

        addRuleSection(content, "2. STARTING & ROLLING",
            "- Each player rolls a single 6-sided die on their turn.\n" +
            "- You must roll a 6 to move a pawn out of Home Base onto the track.\n" +
            "- Rolling a 6 grants you an EXTRA TURN!");

        addRuleSection(content, "3. MOVEMENT & TRACK",
            "- Pawns move clockwise around the 52 perimeter track spaces by the exact die value.\n" +
            "- After completing a lap, enter your colored Home Column leading to the center target.\n" +
            "- An exact die roll is required to enter the Target Finish.");

        addRuleSection(content, "4. CAPTURING & SAFE SPOTS",
            "- Landing on an opponent's pawn captures it and sends it back to their Home Base!\n" +
            "- Star tiles (marked with an arrow or star) are Safe Spots where pawns cannot be captured.\n" +
            "- Pawns inside your colored Home Column are completely safe from capture.");

        addRuleSection(content, "5. WINNING THE GAME",
            "- The game ends when a player successfully moves all 4 pawns into their center home triangle!");

        ScrollPane scrollPane = new ScrollPane(content, skin);
        scrollPane.setFadeScrollBars(false);

        float dialogWidth = Math.min(Gdx.graphics.getWidth() * 0.85f, 580);
        float dialogHeight = Math.min(Gdx.graphics.getHeight() * 0.75f, 440);

        dialog.getContentTable().pad(15);
        dialog.getContentTable().add(scrollPane).width(dialogWidth).height(dialogHeight);

        TextButton closeButton = new TextButton("Got It!", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(closeButton).width(140).height(38).pad(10);
        dialog.show(stage);
    }

    private void addRuleSection(Table table, String headerText, String bodyText) {
        Label headerLabel = new Label(headerText, skin);
        headerLabel.setColor(MenuUIHelper.ACCENT_GOLD);
        table.add(headerLabel).padTop(10).padBottom(4).left().row();

        Label bodyLabel = new Label(bodyText, skin);
        bodyLabel.setWrap(true);
        bodyLabel.setColor(MenuUIHelper.TEXT_PRIMARY);
        table.add(bodyLabel).width(500).padBottom(8).left().row();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (logoTexture != null) {
            logoTexture.dispose();
        }
    }
}
