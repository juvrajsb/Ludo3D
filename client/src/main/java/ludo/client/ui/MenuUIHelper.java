package ludo.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;

/**
 * Helper class providing unified styling, colors, and UI card components
 * for the landing and menu screens.
 */
public class MenuUIHelper {
    // Brand 4-color palette
    public static final Color LUDO_YELLOW = new Color(0.96f, 0.77f, 0.19f, 1f);
    public static final Color LUDO_BLUE = new Color(0.18f, 0.58f, 0.92f, 1f);
    public static final Color LUDO_RED = new Color(0.92f, 0.26f, 0.21f, 1f);
    public static final Color LUDO_GREEN = new Color(0.24f, 0.70f, 0.29f, 1f);

    // Theme neutral colors
    public static final Color CARD_BG = new Color(0.10f, 0.12f, 0.17f, 0.90f);
    public static final Color CARD_HEADER_BG = new Color(0.15f, 0.18f, 0.24f, 0.95f);
    public static final Color CARD_BORDER = new Color(0.22f, 0.27f, 0.36f, 0.85f);
    public static final Color TEXT_PRIMARY = Color.WHITE;
    public static final Color TEXT_MUTED = new Color(0.70f, 0.75f, 0.82f, 1f);
    public static final Color ACCENT_GOLD = new Color(1.0f, 0.84f, 0.30f, 1f);
    public static final Color SUCCESS_GREEN = new Color(0.28f, 0.82f, 0.40f, 1f);
    public static final Color ERROR_RED = new Color(0.94f, 0.30f, 0.28f, 1f);
    public static final Color INPUT_BG = new Color(0.06f, 0.08f, 0.11f, 0.9f);

    /**
     * Creates a styled modern card container with a dark translucent background.
     */
    public static Table createCard(Skin skin, float pad) {
        Table card = new Table();
        card.setBackground(skin.newDrawable("white", CARD_BG));
        card.pad(pad);
        return card;
    }

    /**
     * Creates a decorative 4-color horizontal accent stripe (Yellow, Blue, Red, Green).
     */
    public static Table createColorStripe(Skin skin, float height) {
        Table stripe = new Table();
        Image y = new Image(skin.newDrawable("white", LUDO_YELLOW));
        Image b = new Image(skin.newDrawable("white", LUDO_BLUE));
        Image r = new Image(skin.newDrawable("white", LUDO_RED));
        Image g = new Image(skin.newDrawable("white", LUDO_GREEN));

        stripe.add(y).expandX().fillX().height(height);
        stripe.add(b).expandX().fillX().height(height);
        stripe.add(r).expandX().fillX().height(height);
        stripe.add(g).expandX().fillX().height(height);
        return stripe;
    }

    /**
     * Creates a card header with title and optional subtitle.
     */
    public static Table createHeader(String titleText, String subtitleText, Skin skin) {
        Table header = new Table();
        header.defaults().align(Align.center);

        Label titleLabel = new Label(titleText, skin, "window");
        titleLabel.setColor(TEXT_PRIMARY);
        titleLabel.setAlignment(Align.center);
        header.add(titleLabel).padBottom(4).row();

        if (subtitleText != null && !subtitleText.isEmpty()) {
            Label subtitleLabel = new Label(subtitleText, skin);
            subtitleLabel.setColor(TEXT_MUTED);
            subtitleLabel.setAlignment(Align.center);
            header.add(subtitleLabel).padBottom(15).row();
        }

        return header;
    }

    /**
     * Creates a clean container for an input field with its label.
     */
    public static Table createInputField(String labelText, TextField textField, Skin skin, float fieldWidth) {
        Table row = new Table();
        Label label = new Label(labelText, skin);
        label.setColor(TEXT_MUTED);

        row.add(label).left().padRight(15).width(120);
        row.add(textField).width(fieldWidth).height(38).left();
        return row;
    }

    /**
     * Returns color corresponding to player color name.
     */
    public static Color getColorForName(String colorName) {
        if (colorName == null) return TEXT_PRIMARY;
        switch (colorName.toUpperCase()) {
            case "YELLOW": return LUDO_YELLOW;
            case "BLUE": return LUDO_BLUE;
            case "RED": return LUDO_RED;
            case "GREEN": return LUDO_GREEN;
            default: return TEXT_PRIMARY;
        }
    }
}
