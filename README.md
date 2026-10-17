## Project Structure

```text
ESCAPEROOM/
├── bin/                # Compiled Java class files (ignored by Git)
├── data/               # Save file (data/save.dat, ignored by Git)
├── src/
│   ├── audio/          # Synthesised sound effects (incl. intro story sounds)
│   ├── core/           # Core game systems
│   ├── input/          # Keyboard input handling
│   ├── interaction/    # Player interactions (doors, key, note, safe, lock)
│   ├── inventory/      # Inventory
│   ├── map/            # Maps (Tiled .tmx), tilesets, rendering
│   ├── player/         # Player logic and sprites
│   ├── puzzle/         # Puzzle systems
│   ├── save/           # Save / load
│   └── ui/             # Menus, HUD, animated intro story (IntroUI + IntroScenes), win screen
├── .gitignore
└── README.md
```

## How to run

The game loads its images and maps by relative path (`src/...`), so always
run these commands from the project root (the folder that contains `src/`).

Linux / macOS:

```
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin core.App
```

Windows (PowerShell):

```
mkdir bin -Force
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp bin core.App
```

Controls: W A S D to move, E to interact, M to mute, ESC closes the room
selector. Progress (room, position, items, puzzles) is saved automatically
and restored with **Continue**.

## Java installation verification
```
java -version
javac -version
```