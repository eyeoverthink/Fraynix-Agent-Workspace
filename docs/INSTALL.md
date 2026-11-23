
# Download and run Fraynix Agent Workspace

The current launch path builds the app locally with Java 21 and the locked desktop dependencies. The first preparation needs internet access. After that, the workspace can run offline with its local project data and cached dependencies.

## macOS

Use macOS 13 Ventura or newer, with Apple silicon or an Intel Mac. Install JDK 21 and Node.js 24 LTS for your Mac's architecture. In **About This Mac**, an Apple M-series chip uses ARM64 and an Intel processor uses x64. Electron's macOS minimum applies to the desktop app. [Electron platform requirements](https://www.electronjs.org/blog/electron-44-0#removed-macos-12-support)

If you use [Homebrew](https://brew.sh), open Terminal and run the following commands.

```bash
brew install --cask temurin@21
brew install node@24
export PATH="$(brew --prefix node@24)/bin:$PATH"
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
java -version
node -v
git --version
```

You can also use the [Temurin 21 installer](https://adoptium.net/temurin/releases/?version=21) and the [Node.js installer](https://nodejs.org/en/download). If a new Mac prompts you to install Apple's command-line tools when you run Git, let that installation finish first.

Download the workspace directly from GitHub with a sparse clone. This fetches the current code, runtime assets and the bundled motion dataset while leaving the full historical evidence archive on GitHub.

```bash
git clone --depth 1 --single-branch --branch main --filter=blob:none --sparse https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git
cd Fraynix-Agent-Workspace
git sparse-checkout set assets fraynix-modules gradle levels libs licenses models scripts src workspace-desktop workspace-engine docs/datasets/delfys75-013
./Launch-Agent-Workspace.sh --prepare
./Launch-Agent-Workspace.sh
```

In the app, click **Run guided proof** to check that the Java engine, browser and creation canvas work together. Open **Game Lab** and choose **Relay Garden** to check the embedded renderer. These checks run without a model. For model chat, install a local provider such as [Ollama](https://ollama.com/download/mac) and select a model in the workspace.

For later launches, open Terminal in the checkout and run `./Launch-Agent-Workspace.sh`. If you installed Node through Homebrew's versioned formula, keep its `bin` directory on your PATH in that Terminal.

## Linux and Windows

- **Linux x64.** Use JDK 21 and Node.js 24 LTS with a graphical desktop. On Ubuntu 24.04, install any missing runtime libraries with `sudo apt install libgtk-3-0t64 libnss3 libgbm1 libasound2t64 libgl1 libgl1-mesa-dri`. Use the same sparse download and Bash launcher shown above. Run it from your graphical session.
- **Windows x64.** Use JDK 21 and Node.js 24 LTS. Git for Windows can use the same sparse clone from Git Bash, or the commands up to launch can be pasted into PowerShell. Then run `.\Launch-Agent-Workspace.bat -PrepareOnly` followed by `.\Launch-Agent-Workspace.bat` in PowerShell.

The portability workflow covers Windows x64, Linux x64, Intel macOS and Apple silicon macOS. See the edition 205 results for the desktop, Bullet physics and Game Lab checks. The embedded Game Lab needs a local OpenGL 4.1 display on macOS. If the app reports that the display is unavailable, try from the Mac's local graphical session and check the referenced game log. The rest of the workspace can still run. On macOS, choose the CPU backend for the optional neural compute experiment. [Edition 205 results](../README-205.md). [Portable workspace runs](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/workflows/portable-workspace.yml) · [Apple graphics support](https://developer.apple.com/videos/play/wwdc2020/10631/)

## Updates and offline use

Use **Stop Java & close app** before updating a checkout that owns a running Java process. Then run `git pull --ff-only` and the preparation command again.

On a prepared Mac or Linux checkout, `./Launch-Agent-Workspace.sh --offline` opens the app using the installed dependencies. On Windows use `.\Launch-Agent-Workspace.bat -Offline` after preparation. Project data defaults to `~/.fraynix-agent-workspace/projects/default`. Setting `FRAYNIX_WORKSPACE_PROJECT` before launch selects a different project directory.

If the desktop cannot complete startup, check `desktop-startup.log` in the project directory. If Java cannot start, check `java -version` and `node -v`, then read `engine.log` in the project directory. A partial download or an update to dependencies can be repaired by running preparation again with internet access.
