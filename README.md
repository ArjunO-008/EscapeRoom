# Escape Room

A top-down 2D escape room game written in pure Java (Swing / Java2D).
You wake up inside a locked house: explore the rooms, read the notes, find
the keys, crack the codes and get out.

## Download

Grab the latest build from the **[Releases](../../releases/latest)** page.
Java is bundled, there is nothing else to install.

| Platform | File | How to run |
| --- | --- | --- |
| Windows | `EscapeRoom-<version>-windows-setup.exe` | Run the installer, then start **EscapeRoom** from the Start menu |
| Windows (portable) | `EscapeRoom-<version>-windows.zip` | Unzip, run `EscapeRoom\EscapeRoom.exe` |
| Linux (Debian/Ubuntu) | `EscapeRoom-<version>-linux-x64.deb` | `sudo apt install ./EscapeRoom-*.deb`, then launch **EscapeRoom** |
| Linux (any distro) | `EscapeRoom-<version>-linux-x64.tar.gz` | `tar -xzf EscapeRoom-*.tar.gz && ./EscapeRoom/bin/EscapeRoom` |
| Any OS with Java 17+ | `EscapeRoom-<version>.jar` | `java -jar EscapeRoom-<version>.jar` |

## Controls

| Key | Action |
| --- | --- |
| `W` `A` `S` `D` | Move |
| `E` | Interact (doors, notes, keys, safe, symbol lock) |
| `M` | Mute / unmute all sound |
| `Esc` | Close the room selector |

Progress (room, position, items, puzzles) is saved automatically when you
close the game. Choose **Continue** in the main menu to pick up where you left off.

Saves are stored in your user folder, so the game can be installed anywhere:

- Windows: `%APPDATA%\EscapeRoom\save.dat`
- Linux: `~/.local/share/EscapeRoom/save.dat`

## Works offline

The game never touches the network. Sound effects are synthesised in code
when the game starts, and the background music (`src/audio/music/bgm.wav`)
ships inside the game. If no audio device is available the game just runs silently.

## Run from source

Needs JDK 17 or newer.

```bash
# Linux / macOS
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin core.App
```

```powershell
# Windows (PowerShell)
mkdir bin -Force
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp bin core.App
```

When running from source, start from the project root (the folder that
contains `src/`): assets are read from `src/...`.

## Build the packages yourself

Needs a JDK 17+ (with `jpackage`) and `jq`. On Windows, run it from Git Bash;
the `.exe` installer additionally needs [WiX 3](https://wixtoolset.org/).

```bash
./scripts/package.sh      # output lands in dist/
```

Packages are built for the OS you run it on (jpackage cannot cross-compile).

## Releasing

Pushing to `main` builds Windows and Linux packages and publishes them
through GitHub Actions (`.github/workflows/release.yml`). Everything shown on
the release page comes from **`release.json`**:

```json
{
  "version": "1.0.0",
  "title": "Escape Room",
  "description": "One-line summary shown at the top of the release.",
  "notes": ["Change one", "Change two"]
}
```

- Bump `version` -> a new release `v<version>` is created.
- Keep the same `version` -> the existing release is updated with the new build and notes.

`version` must start with numbers like `1.2.3` (an optional suffix such as `-beta` is allowed).

## Project structure

```text
EscapeRoom/
├── .github/workflows/  # CI: build + release
├── packaging/          # App icons
├── scripts/            # package.sh
├── release.json        # Version + release notes
└── src/
    ├── audio/          # Synthesised sound effects, background music
    ├── core/           # Game window, loop, asset loading
    ├── input/          # Keyboard input
    ├── interaction/    # Doors, key, note, safe, symbol lock
    ├── inventory/      # Inventory
    ├── map/            # Tiled (.tmx) maps, tilesets, rendering
    ├── player/         # Player logic and sprites
    ├── puzzle/         # Puzzle systems
    ├── save/           # Save / load
    └── ui/             # Menus, HUD, intro story, win screen
```

Maps are authored in [Tiled](https://www.mapeditor.org/).
