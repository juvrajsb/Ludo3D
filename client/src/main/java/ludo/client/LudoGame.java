package ludo.client;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ludo.client.networking.Client;
import ludo.client.render.DiceRenderer;
import ludo.client.render.GameCamera;
import ludo.client.render.PawnRenderer;
import ludo.client.screens.MenuScreen;
import ludo.core.entities.Player;


public class LudoGame extends Game {
    private SpriteBatch batch;
    private GameCamera camera;
    private PawnRenderer pawnRenderer;
    private DiceRenderer diceRenderer;
    private Player[] players;
    private Client client;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new GameCamera();
        pawnRenderer = new PawnRenderer();
        diceRenderer = new DiceRenderer();
        client = new Client();

        setScreen(new MenuScreen(this));

        // Initialize players
        players = new Player[4];
        players[0] = new Player("Player 1", "red");
        players[1] = new Player("Player 2", "blue");
        players[2] = new Player("Player 3", "green");
        players[3] = new Player("Player 4", "yellow");
    }

    public Client getClient() {
        return client;
    }

    @Override
    public void render() {
        super.render();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Render pawns for each player
        for (Player player : players) {
            pawnRenderer.render(player);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        pawnRenderer.dispose();
        diceRenderer.dispose();
    }
}

