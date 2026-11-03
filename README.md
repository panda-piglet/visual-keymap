# Visual Keymap — Community-maintained Fork

English | [简体中文](docs/README.zh-CN.md)

> **Unofficial community fork.** Visual Keymap was created by [XeF2](https://github.com/USTC-XeF2). This repository maintains a community port focused on **Minecraft 26.3** and is not an official release by the original author. Historical versions and upstream project information remain available from the [original repository](https://github.com/USTC-XeF2/visual-keymap).

## Download

**Current community release:** [`1.4.1-community-beta.1`](https://github.com/panda-piglet/visual-keymap/releases/tag/v1.4.1-26.3-community-beta.1) for Minecraft 26.3 Fabric.

This build is a **GitHub Pre-release**, not a Stable release. It does not claim compatibility with every older or future Minecraft version. For historical releases, use the [upstream repository and its release pages](https://github.com/USTC-XeF2/visual-keymap/releases).

![Screenshot of Visual Keymap Screen](docs/screenshot.png)

## What Visual Keymap does

Visual Keymap displays currently bound keys on a visual keyboard layout and lets players edit bindings from Minecraft and supported mods in one configuration screen.

- Open it from `Options → Controls → Visual Keymap`.
- With Mod Menu installed, open it from Visual Keymap's configuration button.
- View, search, edit, clear and reset supported bindings in a unified interface.
- Integrate Minecraft bindings, MaLiLib hotkeys and CommandKeys macro bindings.

## Requirements and installation

Required:

- Minecraft 26.3
- Fabric
- Java 25
- Fabric Loader 0.20.1 or a newer compatible release
- Fabric API for Minecraft 26.3 (tested with `0.165.0+26.3`)

Install Fabric Loader and Fabric API, then place `visual-keymap-26.3-1.4.1-community-beta.1.jar` in the client's `mods` directory.

Optional integrations tested with this Community Beta:

- MaLiLib 0.30.2
- CommandKeys 3.104.1+26.3 **Fabric**
- Mod Menu 21.0.0

Optional mods are not bundled in the Visual Keymap JAR. Tweakeroo 0.30.1 was used only as a real MaLiLib hotkey provider during integration testing; it is not required for all players.

## Community edition changes

- Ported Visual Keymap to Minecraft 26.3 while retaining the original user workflow and Provider API.
- Fixed a client crash when importing invalid or damaged JSON snapshots.
- Fixed incorrect snapshot matching when different mods use the same provider and binding ID.
- Kept the existing version 1 snapshot schema compatible.

Important compatibility notes:

- Old version 1 snapshots cannot uniquely identify bindings from different mods when they share the same provider and binding ID. The import preview marks these entries as ambiguous and may require the user to select the intended target.
- CommandKeys macros are intentionally excluded from Visual Keymap snapshot exports, matching the original behavior.
- Some rare physical keys and extra mouse buttons have data-safety coverage but have not been tested on every physical device.

See the [Community Beta release notes](https://github.com/panda-piglet/visual-keymap/releases/tag/v1.4.1-26.3-community-beta.1) and [Known Issues](docs/release/KNOWN-ISSUES.md) for details. Report community-edition problems on the [fork issue tracker](https://github.com/panda-piglet/visual-keymap/issues).

## Developer integration

Mods with independent key binding systems can expose their bindings by registering the existing `visualkeymap` entrypoint:

```json
"entrypoints": {
  "visualkeymap": [
    "com.example.mymod.MyModVisualKeymapImpl"
  ]
}
```

The entrypoint class implements `dev.xef2.visualkeymap.api.VisualKeymapApi`. See `dev.xef2.visualkeymap.api.MinecraftImpl` for the built-in implementation and `KeyBinding` for the binding contract. The Minecraft 26.3 community port preserves this Provider API and its existing entrypoint name.

## License and credits

Visual Keymap is licensed under the GNU General Public License version 3. Original authorship and upstream project credit belong to XeF2 and the upstream contributors. This repository contains unofficial community-maintained Minecraft 26.3 changes and does not imply endorsement by the original author.

See [LICENSE](LICENSE), [NOTICE](NOTICE), and the [original Visual Keymap repository](https://github.com/USTC-XeF2/visual-keymap).
