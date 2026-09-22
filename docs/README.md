# Documentation & Media Assets

Place your hero gameplay recording (`demo.gif` or screenshots) in this directory.

The root `README.md` references:
```markdown
![Ludo 3D Gameplay Demo](docs/demo.gif)
```

### Tips for Recording Your Gameplay GIF:
1. Start the server in one terminal:
   ```bash
   ./gradlew :server:run
   ```
2. Start two client windows side-by-side:
   ```bash
   ./gradlew :lwjgl3:run
   ```
3. Record a short 15–20 second screen capture of:
   - Connecting to `localhost:12000`
   - Players in the lobby and starting the game
   - Rolling the 3D dice and pawn moving smoothly across the board
   - Real-time synchronization between both windows
4. Export as `demo.gif` and save it directly in this `docs/` folder.
