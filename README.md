# Ludo 3D

## Overview
Ludo 3D is a networked multiplayer implementation of the classic Ludo board game, built with LibGDX and featuring 3D graphics. The game supports 2-4 players across a network with a client-server architecture, allowing for both local and online gameplay.

## Technology Stack
- **Core Framework**: LibGDX
- **Language**: Java
- **3D Graphics**: LibGDX's 3D API
- **Network Communication**: Custom TCP/IP socket implementation
- **Persistence**: JSON-based save/load system
- **Build Tool**: Gradle

## Project Structure
The project is divided into several modules:
- **/core**: Contains the shared code for game logic, entities, and network events, used by both the client and server.
- **/client**: Implements the client-side application, including screen management, rendering, and user interface.
- **/server**: Contains the standalone server application that manages game state, player connections, and game logic.
- **/lwjgl3**: The desktop launcher for the client application (Windows, macOS, Linux).

## How to Build
This project uses Gradle to manage dependencies and builds. To build the necessary JAR files, run the following command from the root directory of the project:

```bash
./gradlew build
```
This command will compile the code for all modules and create executable JAR files in the `build/libs` directory of the `server` and `lwjgl3` modules.

## How to Run

### 1. Start the Server
First, you need to run the server application. It will listen for client connections and manage the game. The server uses port `12000` by default.

```bash
java -jar release/ludo-server-1.0.jar
```

### 2. Run the Client
Once the server is running, you can launch one or more client applications. Each client will connect to the server to join the game.

```bash
java -jar release/ludo-1.0.jar
```

for macOS
```bash
java -XstartOnFirstThread -jar release/ludo-1.0.jar
```

After launching, the client will present a menu where you can connect to the server by providing its IP address (use `localhost` if running on the same machine) and port (`12000` by default).

## Game Features
- **Networked Multiplayer**: Play with 2-4 players over a network.
- **3D Graphics**: A fully 3D rendered board, pawns, and dice.
- **Save/Load Game**: The game state can be saved and loaded, allowing games to be resumed later. This functionality is handled through JSON files.
- **Bot Players**: The game supports AI-controlled bot players to fill empty slots.
- **Client-Server Architecture**: A robust server manages the authoritative game state, while clients handle rendering and user input.
- **Event-Driven Networking**: Communication between the client and server is handled through a system of events for actions like joining a game, rolling the dice, and moving pawns.
