package ludo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends ApplicationAdapter {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    private Model boardModel;
    private Model pawnModel;
    private Model diceModel;
    private List<ModelInstance> modelInstances;
    private ModelInstance[] diceInstances;

    private boolean isRollingDice;
    private float diceRollTime;
    private static final float DICE_ROLL_DURATION = 2f;

    private GameManager gameManager;

    private int selectedPawnIndex = -1;
    private boolean waitingForMove = false;
    private int lastDiceRoll = 0;

    Lobby lobby;
    private Label turnIndicator;
    private Label gameStatusLabel;

    private List<Pawn> pawns;

    @Override
    public void create() {
        setupGraphics();
        loadAssets();
        gameManager = new GameManager(this);
        lobby = new Lobby(this);
        createGameUI();

        pawns = new ArrayList<>();
        // Create 4 pawns for each color
        for (String color : new String[]{"RED", "BLUE", "GREEN", "YELLOW"}) {
            for (int i = 0; i < 4; i++) {
                pawns.add(new Pawn(color));
            }
        }
    }


    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a new thread to handle server messages
            new Thread(this::handleServerMessages).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupGraphics() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 10f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        modelInstances = new ArrayList<>();
    }

    private void loadAssets() {
        // Load board texture
        Texture boardTexture = new Texture(Gdx.files.internal("images/board.png"));
        Material boardMaterial = new Material(TextureAttribute.createDiffuse(boardTexture));

        // Create board model
        ModelBuilder modelBuilder = new ModelBuilder();
        boardModel = modelBuilder.createBox(10f, 0.1f, 10f, boardMaterial,
                                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        ModelInstance boardInstance = new ModelInstance(boardModel);
        modelInstances.add(boardInstance);

        // Load pawn model
        pawnModel = new G3dModelLoader(new UBJsonReader()).loadModel(Gdx.files.internal("models/pawn.g3db"));
        // Add pawn instances (adjust positions as needed)
        for (int i = 0; i < 16; i++) {
            ModelInstance pawnInstance = new ModelInstance(pawnModel);
            pawnInstance.transform.setToTranslation(new Vector3((i % 4) - 1.5f, 0.5f, (i / 4) - 1.5f));
            modelInstances.add(pawnInstance);
        }

        // Load dice model
        diceModel = new G3dModelLoader(new UBJsonReader()).loadModel(Gdx.files.internal("models/dice.g3db"));
        diceInstances = new ModelInstance[2];
        diceInstances[0] = new ModelInstance(diceModel);
        diceInstances[1] = new ModelInstance(diceModel);
        diceInstances[0].transform.setToTranslation(-0.5f, 1f, 0f);
        diceInstances[1].transform.setToTranslation(0.5f, 1f, 0f);
        modelInstances.add(diceInstances[0]);
        modelInstances.add(diceInstances[1]);
    }

    private void handleServerMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Lost connection to server: " + e.getMessage());
            handleDisconnection();
        }
    }

    private void handleDisconnection() {
        // Implement logic to handle disconnection (e.g., show a message, try to reconnect)
        System.out.println("Disconnected from server. Attempting to reconnect...");
        connectToServer();
    }

    private void processServerMessage(String message) {
        try {
            String[] parts = message.split(" ", 2);
            switch (parts[0]) {
                case "ERROR":
                    handleErrorMessage(parts[1]);
                    break;
                case "TURN_TIMEOUT":
                    handleTurnTimeout(parts[1]);
                    break;
                case "GAME_STARTED":
                    gameManager.setGameStarted(true);
                    updateGameStatus("Game started!");
                    break;
                case "TURN":
                    handleTurnChange(parts[1]);
                    break;
                case "ROLL_RESULT":
                    int[] results = new int[2];
                    results[0] = Integer.parseInt(parts[1]);
                    results[1] = Integer.parseInt(parts[2]);
                    // Update dice model to show the correct faces
                    updateDiceDisplay(results);
                    break;
                case "GAME_STATE":
                    updateGameState(parts[1]);
                    break;
                case "MOVE":
                    // Handle move updates
                    handleMoveUpdate(parts[1]);
                    break;
                case "GAME_OVER":
                    handleGameOver(parts[1]);
                    break;
                case "LOBBY_UPDATE":
                    lobby.updatePlayerList(parts[1].split(","));
                    break;
                case "CHAT":
                    lobby.addChatMessage(parts[1]);
                    break;
                default:
                    System.out.println("Unknown message type: " + parts[0]);
            }
        } catch (Exception e) {
            System.err.println("Error processing server message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateDiceDisplay(int[] results) {

    }

    void handleErrorMessage(String errorMessage) {
        // Display error message to the user
        System.out.println("Error: " + errorMessage);
        // You might want to update the UI to show this error message
    }

    void handleTurnTimeout(String playerColor) {
        // Handle turn timeout
        System.out.println("Player " + playerColor + " turn timed out");
    }

    private void handleTurnChange(String playerColor) {
        gameManager.setCurrentPlayerTurn(playerColor);
        updateTurnIndicator(gameManager.isCurrentPlayerTurn());
    }

    void handleGameOver(String winnerColor) {
        // Display game over message and winner
        // Disable game controls
    }

    void updateGameState(String stateString) {
        String[] playerStates = stateString.split(";");
        for (String playerState : playerStates) {
            String[] playerData = playerState.split(":");
            String playerColor = playerData[0];
            String[] pawnPositions = playerData[1].split(",");
            for (int i = 0; i < pawnPositions.length; i++) {
                int position = Integer.parseInt(pawnPositions[i]);
                updatePawnPosition(playerColor, i, position);
            }
        }
    }

    private void handleMoveUpdate(String moveData) {
        String[] parts = moveData.split(" ");
        String playerColor = parts[0];
        int pawnIndex = Integer.parseInt(parts[1]);
        int newPosition = Integer.parseInt(parts[2]);
        updatePawnPosition(playerColor, pawnIndex, newPosition);
    }

    private void updatePawnPosition(String playerColor, int pawnIndex, int position) {
        int playerIndex = getPlayerIndexFromColor(playerColor);
        int pawnInstanceIndex = playerIndex * 4 + pawnIndex + 1; // +1 because the first instance is the board
        ModelInstance pawnInstance = modelInstances.get(pawnInstanceIndex);
        Vector3 newPosition = calculateBoardPosition(position);
        pawnInstance.transform.setTranslation(newPosition);
    }

    private int getPlayerIndexFromColor(String color) {
        // Implement this method to return the player index based on color
        // For example: RED = 0, BLUE = 1, GREEN = 2, YELLOW = 3
        return 0;
    }

    private Vector3 calculateBoardPosition(int position) {
        // Implement the logic to convert a board position to a 3D coordinate
        // For now, we'll use a simple circular layout
        float angle = (float) (position * (2 * Math.PI / 52));
        float x = 4f * (float) Math.cos(angle);
        float z = 4f * (float) Math.sin(angle);
        return new Vector3(x, 0.5f, z);
    }

    public void rollDice() {
        if (!isRollingDice && gameManager.isCurrentPlayerTurn()) {
            isRollingDice = true;
            diceRollTime = 0f;
            gameManager.rollDice();
        }
    }

    public void setDiceRollResult(int result) {
        lastDiceRoll = result;
        waitingForMove = true;
    }

    private void updateDiceRoll(float delta) {
        if (isRollingDice) {
            diceRollTime += delta;
            if (diceRollTime < DICE_ROLL_DURATION) {
                // Animate dice rolling
//                for (ModelInstance diceInstance : diceInstances) {
//                    Quaternion rotation = new Quaternion(Vector3.Y, 360f * (diceRollTime / DICE_ROLL_DURATION));
//                    rotation.mul(new Quaternion(Vector3.X, 360f * (diceRollTime / DICE_ROLL_DURATION)));
//                    diceInstance.transform.setToRotation(rotation);
//                    diceInstance.transform.translate(0f, 1f + 0.5f * (float)Math.sin(diceRollTime * Math.PI * 2), 0f);
//                }
            } else {
                isRollingDice = false;
                // Set final dice positions based on the rolled values
                // This should be set when receiving the roll result from the server
            }
        }
    }

    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        updateDiceRoll(Gdx.graphics.getDeltaTime());

        camera.update();
        modelBatch.begin(camera);
        for (ModelInstance instance : modelInstances) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();

        gameManager.update();

        // Handle input
        if (Gdx.input.justTouched()) {
            handleUserInput(Gdx.input.getX(), Gdx.input.getY());
        }

        if (gameManager.isGameStarted()) {
            // Render 3D game
            // ... existing 3D rendering code ...

            // Render UI
//            uiStage.act(Gdx.graphics.getDeltaTime());
//            uiStage.draw();
        } else {
            // Render lobby
            lobby.render();
        }
    }

    private void handleUserInput(int screenX, int screenY) {
        if (isRollingDice || !gameManager.isCurrentPlayerTurn()) {
            return;
        }

        if (!waitingForMove) {
            rollDice();
        } else {
            selectOrMovePawn(screenX, screenY);
        }
    }

    private void selectOrMovePawn(int screenX, int screenY) {
        Ray ray = camera.getPickRay(screenX, screenY);
        int closestPawnIndex = -1;
        float closestDistance = Float.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            int pawnInstanceIndex = gameManager.getCurrentPlayerId() * 4 + i + 1;
            ModelInstance pawnInstance = modelInstances.get(pawnInstanceIndex);
            Vector3 position = new Vector3();
            pawnInstance.transform.getTranslation(position);

            float distance = ray.origin.dst2(position);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPawnIndex = i;
            }
        }

        if (closestPawnIndex != -1) {
            if (selectedPawnIndex == -1) {
                selectedPawnIndex = closestPawnIndex;
                highlightSelectedPawn(selectedPawnIndex);
            } else if (selectedPawnIndex == closestPawnIndex) {
                // Pawn is already selected, try to move it
                gameManager.movePawn(selectedPawnIndex, lastDiceRoll);
                selectedPawnIndex = -1;
                waitingForMove = false;
            } else {
                // Deselect previous pawn and select new one
                unhighlightSelectedPawn(selectedPawnIndex);
                selectedPawnIndex = closestPawnIndex;
                highlightSelectedPawn(selectedPawnIndex);
            }
        }
    }

    private void highlightSelectedPawn(int pawnIndex) {
        int pawnInstanceIndex = gameManager.getCurrentPlayerId() * 4 + pawnIndex + 1;
        ModelInstance pawnInstance = modelInstances.get(pawnInstanceIndex);
        // Add highlighting effect (e.g., change color or scale)
        pawnInstance.transform.scale(1.2f, 1.2f, 1.2f);
    }

    private void unhighlightSelectedPawn(int pawnIndex) {
        int pawnInstanceIndex = gameManager.getCurrentPlayerId() * 4 + pawnIndex + 1;
        ModelInstance pawnInstance = modelInstances.get(pawnInstanceIndex);
        // Remove highlighting effect
        pawnInstance.transform.scale(1f, 1f, 1f);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        boardModel.dispose();
        pawnModel.dispose();
        diceModel.dispose();
        gameManager.dispose();
        lobby.dispose();
    }

    private void createGameUI() {
        Stage uiStage = new Stage(new ScreenViewport());
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table uiTable = new Table();
        uiTable.setFillParent(true);
        uiStage.addActor(uiTable);

        turnIndicator = new Label("Waiting for game to start...", skin);
        gameStatusLabel = new Label("", skin);

        uiTable.add(turnIndicator).pad(10);
        uiTable.row();
        uiTable.add(gameStatusLabel).pad(10);
    }

    public void updateTurnIndicator(boolean isCurrentPlayerTurn) {
        if (isCurrentPlayerTurn) {
            turnIndicator.setText("It's your turn!");
        } else {
            turnIndicator.setText("Waiting for other player's turn...");
        }
    }

    public void updateGameStatus(String status) {
        gameStatusLabel.setText(status);
    }

    public void sendToServer(String message) {
        out.println(message);
    }

    public void showInvalidMoveMessage() {

    }

    private Color getColorFromString(String colorString) {
        switch (colorString) {
            case "RED": return Color.RED;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "YELLOW": return Color.YELLOW;
            default: throw new IllegalArgumentException("Invalid color: " + colorString);
        }
    }
}
