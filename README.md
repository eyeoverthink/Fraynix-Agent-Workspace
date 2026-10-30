# Fraynix Agent Workspace

**A local visual environment for agents to browse, create runnable artifacts, test behavior and reuse verified skills.**

> **Latest system edition: [README 005](README-005.md)** · [Interactive explorer](docs/readmes/005/index.html) · [Design and scope](docs/AGENT_WORKSPACE_DESIGN.md) · [Edition history](docs/readmes/INDEX.md)

Independent successor to [Fraynix Studio](https://github.com/eyeoverthink/Fraynix-Studio), starting from verified commit `6341404`. The original application and personal project state remain separate. Full game sources, assets, licenses and earlier editions are preserved here.

## Start

Run **Launch-Agent-Workspace.bat** with JDK 21 and Node.js 22.12 or newer installed. First setup downloads pinned Electron/Chromium dependencies. Use an installed local Ollama model for agent tasks. Docker and WSL are not required.

The app includes a real browser, live creation canvas, editable source, versioned artifact library, local-model agent, execution timeline and tested recipe library. **Run guided proof** demonstrates the complete local workflow. **Open Game Lab** launches the preserved Java editor in its own successor project directory.

Local `gpt-oss:20b` is the verified default for the first agent fixture. The custom FRAYNIX 3B trial returned an Ollama native-format error; advertised tool capability alone does not establish task reliability. You can select other eligible installed models explicitly.

![The real browser and runnable canvas in the successor workspace](docs/readmes/005/images/workspace.png)

Application frame export composed from the actual shell and child Chromium renderers. See [verification](docs/readmes/005/verification.json) for integration checks, the live-model fixture and independent acceptance assertions.

## Verify

```powershell
.\gradlew.bat check installDist :workspace-engine:installDist
cd workspace-desktop
npm.cmd ci --no-audit --no-fund
npm.cmd test
npm.cmd run test:integration
# Optional actual local-model fixture:
$env:FRAYNIX_LIVE_MODEL='gpt-oss:20b'
npm.cmd run test:live
```

Project files, test receipts, learned recipes, browser cookies and event history live under `%USERPROFILE%/.fraynix-agent-workspace/projects/default`, outside Git. `FRAYNIX_WORKSPACE_PROJECT` selects another independent project.

The current skill type is a tested web-artifact recipe. General code workers, broader computer control, visual-model perception and evaluated neural/system improvement are subsequent milestones in the [design plan](docs/AGENT_WORKSPACE_DESIGN.md).
