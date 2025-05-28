# Ludo 3D - Project Specification

## Overview
Ludo 3D is a networked multiplayer implementation of the classic Ludo board game, built with LibGDX and featuring 3D graphics. The game supports 2-4 players across a network with a client-server architecture, allowing for both local and online gameplay.

## Technology Stack
- **Core Framework**: LibGDX
- **3D Graphics**: LibGDX's 3D API
- **Network Communication**: Custom TCP/IP socket implementation
- **Persistence**: Custom JSON-based save system
- **Platform**: Desktop (Windows, macOS, Linux)

## Architecture

### Client-Server Model
- **Server**: Standalone Java application that manages game logic, turns, and player connections
- **Client**: LibGDX application that handles rendering, input, and communication with the server

### Core Components

#### Server-Side
1. **Server**: Main entry point for the server application
2. **ServerNetworkHandler**: Manages client connections and message routing
3. **ServerGameStateManager**: Processes game events and maintains game state
4. **GameManager**: Contains core game logic
5. **EventTransmitter**: Sends events to clients
6. **PlayerJoinHandler**: Processes player joining/leaving

#### Client-Side
1. **LudoGame**: Main LibGDX application class
2. **GameStateManager**: Manages client-side game state and server communication
3. **ClientNetworkHandler**: Handles communication with the server
4. **GameScreen**: 3D rendering and user interaction
5. **GameRenderer**: Renders the 3D board, pawns, and animations
6. **GameHUD**: Renders UI elements and player information

### Data Model
1. **Board**: Represents the game board and its spaces
2. **Player**: Represents a player with pawns
3. **Pawn**: Represents a player's game piece
4. **Dice**: Represents the game dice

### Network Protocol
The game uses a custom event-based protocol for communication:

1. **Event**: Base class for all network messages
2. **Client-to-Server Events**: Requests from clients (join, move, dice roll, etc.)
3. **Server-to-Client Events**: Updates from server (game state, turn changes, etc.)

## Game Features

### Core Gameplay
1. **Board Navigation**: 3D camera controls with zoom, rotation, and panning
2. **Dice Rolling**: Visual dice animation with server-verified results
3. **Pawn Movement**: Animated pawn movement with path calculation

## Ludo Game Rules

### Board Layout
1. **Board Structure**: Square board with a cross-shaped path in the center
2. **Player Areas**: Four colored sections (Yellow, Red, Blue, Green) at the corners
3. **Home Base**: Each player has a colored home base with space for four pawns
4. **Main Track**: 52 spaces around the perimeter (13 spaces per side)
5. **Home Column**: Final 6 spaces leading to the finish for each color
6. **Safe Spots**: Specially marked spaces where pawns cannot be captured

### Game Setup
1. **Players**: 2-4 players, each assigned a color (Yellow, Red, Blue, Green)
2. **Pawns**: Each player has 4 pawns of their color
3. **Starting Position**: All pawns begin in their respective home bases
4. **First Player**: Determined by highest dice roll or by server assignment

### Turn Sequence
1. **Rolling**: Player rolls a single six-sided die
2. **Movement Options**:
    - If a 6 is rolled, player may bring a pawn out from home base to the starting position
    - If a pawn is already on the board, player may move it forward by the number of spaces shown on the die
    - If multiple pawns are on the board, player chooses which one to move
    - If no legal move is available, the turn passes to the next player
3. **Extra Turn**: Rolling a 6 gives the player another turn
4. **Three Consecutive Sixes**: If a player rolls three consecutive 6s, the third turn is forfeited

### Movement Rules
1. **Leaving Home**: A pawn can only leave home base if a 6 is rolled
2. **Clockwise Movement**: Pawns move clockwise around the board
3. **Starting Position**: Each color has a designated starting position on the board
4. **Sharing**: Pawns of the same color can occupy a space, pawns of different colors can only share in the safe spots
5. **Overtaking**: Pawns can pass other pawns during movement
6. **Exact Count**: Pawns must enter the home column and reach the finish with an exact roll

### Capture Rules
1. **Capture Mechanism**: Landing on an opponent's pawn sends it back to home base
2. **Safe Spots**: Pawns on safe spots (starred spaces) cannot be captured
3. **Starting Positions**: A pawn on its own color's starting position cannot be captured by another pawn of the same color
4. **Home Column**: Pawns in the home column cannot be captured

### Home Column Rules
1. **Entry Requirement**: Pawns must complete a full circuit before entering the home column
2. **Exact Roll**: A pawn must roll the exact number needed to reach an empty space in the home column
3. **Overshooting**: If a roll would cause a pawn to overshoot the finish, the move is invalid
4. **Final Position**: Pawns that reach the final space in the home column are finished

### Winning Conditions
1. **Game Objective**: Be the first player to move all four pawns to the final position
2. **Ranking**: Players who finish subsequently are ranked second, third, and fourth
3. **Game End**: Game concludes when all players except one have finished, or by mutual agreement

### Multiplayer Features
1. **Game Lobby**: Waiting room for players to join
2. **Player Management**: Username and color selection
3. **Turn System**: Server-managed turn rotation
4. **State Synchronization**: Client-server state reconciliation

### Additional Features
1. **Save/Load**: Game state persistence
2. **Bot Players**: AI players that can fill empty slots
3. **Reconnection**: Players can reconnect after disconnection
4. **Visual Feedback**: Highlighting for valid moves and current player

## User Interface

### Screens
1. **Menu Screen**: Game entry point with play and exit options
2. **Connection Screen**: Server connection setup
3. **Username Screen**: Player name and color selection
4. **Lobby Screen**: Waiting for other players to join
5. **Game Screen**: Main 3D gameplay screen
6. **Save/Load Screen**: Game state management

### In-Game UI
1. **Top Panel**: Current player, dice value, and player info
2. **Bottom Panel**: Game messages and status
3. **HUD Elements**: Network status, turn indicators, etc.
4. **Dialog Windows**: Confirmations and notifications
5. **Dice Display**: Visual representation of dice value

## Graphics and Animation

### 3D Elements
1. **Board**: 3D game board with colored spaces and paths
2. **Pawns**: 3D player pieces with different colors
3. **Dice**: 3D dice with animated rolling

### Animations
1. **Pawn Movement**: Smooth arc movement between spaces
2. **Dice Rolling**: Realistic 3D dice tumbling
3. **Selection/Highlight**: Visual cues for selected pawns
4. **Camera Transitions**: Smooth camera movements

## Implementation Details

### Game Flow
1. **Connection**: Client connects to server
2. **Player Setup**: Choose username and color
3. **Lobby**: Wait for enough players
4. **Game Start**: Server assigns turns and initializes board
5. **Turn Flow**:
    - Current player rolls dice
    - Valid moves are calculated
    - Player selects a pawn to move
    - Move is executed and verified by server
    - If dice shows 6, player gets another turn (up to three consecutive times)
    - Otherwise, turn passes to next player
6. **Game End**: First player to finish all pawns wins

## Game Specifications

### Technical Specifications
1. **Board Dimensions**: 15×15 grid layout
2. **Track Layout**:
    - Main track: 52 spaces (13 per side)
    - Home columns: 6 spaces per color
    - Total spaces: 76 (including home columns)
3. **Coordinate System**:
    - Origin (0,0) at bottom-left corner
    - Grid-based positioning for all game elements
    - 3D world coordinates derived from grid positions

### Performance Requirements
1. **Frame Rate**: Minimum 30 FPS, target 60 FPS
2. **Network Latency**: Maximum 200ms for responsive gameplay
3. **Response Time**: Maximum 100ms for UI interactions

### Rendering Specifications
1. **Camera Properties**:
    - Field of view: 67 degrees
    - Near plane: 1 unit
    - Far plane: 300 units
2. **Lighting Setup**:
    - Ambient light: 40% intensity
    - Main directional light: 80% intensity
    - Fill light: 30% intensity
3. **Model Scales**:
    - Board: 15×15 units
    - Pawns: Scale factor of 7 units
    - Dice: Scale factor of 40 units

### UI Specifications
1. **Screen Resolution**: Minimum support for 800×600
2. **HUD Layouts**:
    - Top panel: 60px height
    - Bottom panel: 50px height
3. **Input Handling**:
    - Mouse for pawn selection and camera rotation
    - Keyboard for camera movement (arrow keys/WASD)
    - Scroll wheel for zoom

### Networking Specifications
1. **Protocol**: TCP/IP for reliable delivery
2. **Port**: Default 12000
3. **Connection Timeout**: 5 seconds
4. **Ping Interval**: 2 seconds
5. **Maximum Reconnection Attempts**: 3

### Save Format Specifications
1. **File Format**: JSON
2. **Save Data Structure**:
    - Save version
    - Player data (name, color, pawn positions)
    - Current player
    - Game state
    - Timestamp
3. **Auto-save Interval**: 60 seconds

### Bot Player Specifications
1. **Difficulty**: Single difficulty level with strategic decision-making
2. **Decision Metrics**:
    - Capture score: 100 points
    - Safety score: 50 points
    - Progress score: 30 points
    - Home stretch score: 80 points
    - Blocking score: 40 points

### Synchronization Mechanism
1. **Server Authority**: Server maintains authoritative game state
2. **Client Prediction**: Client predicts move results for responsiveness
3. **State Reconciliation**: Server corrections are applied to client state
4. **Network Resilience**: Ping/pong heartbeat and reconnection handling

### Save/Load System
1. **Game Persistence**: JSON serialization of game state
2. **Auto-Save**: Periodic state saving during gameplay
3. **Manual Save**: User-triggered state saving
4. **Resuming**: Loading previously saved games

## Project Structure

### Key Packages
1. **ludo.client**: Client-side code
    - **screens**: UI screens
    - **render**: 3D rendering
    - **networking**: Client network handling
    - **ui**: HUD and UI components
    - **state**: Client state management
2. **ludo.core**: Shared code between client and server
    - **entities**: Game objects (Board, Player, Pawn)
    - **events**: Network message definitions
    - **network**: Network communication tools
    - **utils**: Utility classes
    - **game**: Game state and rules
    - **persistence**: Save/load functionality
3. **ludo.server**: Server-side code
    - **networking**: Server network handling
    - **handlers**: Event handlers
    - **state**: Server state management

### Asset Requirements
1. **3D Models**:
    - Board model
    - Pawn models
    - Dice model
2. **Textures**:
    - Board texture
    - Pawn textures
    - UI elements
3. **UI Skin**: Custom UI skin for menus and HUD

## Testing Strategy
1. **Unit Tests**: Test individual components (Board, Player, etc.)
2. **Integration Tests**: Test component interactions
3. **Network Tests**: Test client-server communication
4. **Game Logic Tests**: Test game rules and mechanics

## Future Enhancements
1. **Enhanced Graphics**: Improved textures and lighting
2. **Sound Effects**: Audio feedback for actions
3. **Advanced AI**: More sophisticated bot strategies
4. **Mobile Support**: Touch controls for mobile devices
5. **Cross-Platform Play**: Allow different platforms to play together
6. **Tournament Mode**: Multi-game tournaments with scoring

## Deployment
1. **Desktop Deployment**: JAR file with bundled assets
2. **Server Deployment**: Standalone server application
3. **Distribution**: Downloadable package with client and server

## Development Roadmap
1. **Phase 1**: Core game mechanics and basic networking
2. **Phase 2**: 3D graphics and animations
3. **Phase 3**: Complete UI and game flow
4. **Phase 4**: Save/load system and bot players
5. **Phase 5**: Polish, testing, and optimization
6. **Phase 6**: Deployment and launch

## Resources and Dependencies
1. **LibGDX**: Core framework (https://libgdx.com/)
2. **LWJGL**: Low-level graphics API
3. **Gson**: JSON serialization
4. **JUnit**: Testing framework
