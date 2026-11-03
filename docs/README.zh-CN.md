# Visual Keymap — 社区维护 Fork

[English](../README.md) | 简体中文

> **非官方社区维护 Fork。** Visual Keymap 由 [XeF2](https://github.com/USTC-XeF2) 原创。本仓库重点维护面向 **Minecraft 26.3** 的社区移植版，并非原作者的官方发布。历史版本和原项目资料仍可从[原始仓库](https://github.com/USTC-XeF2/visual-keymap)获取。

## 下载

**当前社区版本：**适用于 Minecraft 26.3 Fabric 的 [`1.4.1-community-beta.1`](https://github.com/panda-piglet/visual-keymap/releases/tag/v1.4.1-26.3-community-beta.1)。

这是 GitHub **Pre-release（预发布版）**，不是 Stable（稳定版）。本版本不宣称同时兼容全部旧版或未来 Minecraft 版本。历史版本请查看[上游仓库及其发布记录](https://github.com/USTC-XeF2/visual-keymap/releases)。

![可视化键位图界面的屏幕截图](screenshot-zh-CN.png)

## Visual Keymap 的功能

Visual Keymap 可以在可视化键盘布局中显示当前绑定的键位，并允许玩家在一个统一配置界面中编辑 Minecraft 和受支持模组的键位。

- 从`选项 → 按键控制 → 可视化键位图`打开。
- 安装 Mod Menu 后，可通过 Visual Keymap 的配置按钮打开。
- 在统一界面中查看、搜索、编辑、清空和重置受支持的键位。
- 集成 Minecraft 原生键位、MaLiLib 热键和 CommandKeys 宏键位。

## 运行要求与安装

必需：

- Minecraft 26.3
- Fabric
- Java 25
- Fabric Loader 0.20.1 或更新的兼容版本
- 适用于 Minecraft 26.3 的 Fabric API（已使用 `0.165.0+26.3` 测试）

安装 Fabric Loader 和 Fabric API，然后将 `visual-keymap-26.3-1.4.1-community-beta.1.jar` 放入客户端的 `mods` 目录。

本次 Community Beta 已验证以下可选集成：

- MaLiLib 0.30.2
- CommandKeys 3.104.1+26.3 **Fabric**
- Mod Menu 21.0.0

这些可选模组不会被打包进 Visual Keymap JAR。Tweakeroo 0.30.1 仅在集成测试中作为真实的 MaLiLib 热键提供者使用，并不是所有玩家都必须安装的依赖。

## 社区版改进

- 将 Visual Keymap 适配到 Minecraft 26.3，同时保留原有用户操作流程和 Provider API。
- 修复导入非法或损坏 JSON 快照时导致客户端崩溃的问题。
- 修复不同模组使用相同 provider 和 binding ID 时产生的快照错误匹配。
- 保持现有 version 1 快照格式兼容。

重要兼容性说明：

- 当不同模组使用相同的 provider 和 binding ID 时，旧版 version 1 快照无法唯一标识目标绑定。导入预览会将这些记录标记为歧义，并可能要求用户手动选择正确目标。
- CommandKeys 宏仍不会进入 Visual Keymap 快照导出，与原版行为一致。
- 部分罕见物理键和额外鼠标按键已经过数据安全验证，但尚未在所有实体设备上测试。

详细信息请查看 [Community Beta 发布说明](https://github.com/panda-piglet/visual-keymap/releases/tag/v1.4.1-26.3-community-beta.1)和[已知问题](release/KNOWN-ISSUES.md)。社区版问题请提交到 [Fork issue tracker](https://github.com/panda-piglet/visual-keymap/issues)。

## 开发者集成

拥有独立键位绑定系统的模组可以通过注册现有的 `visualkeymap` entrypoint 暴露其键位：

```json
"entrypoints": {
  "visualkeymap": [
    "com.example.mymod.MyModVisualKeymapImpl"
  ]
}
```

入口点类需要实现 `dev.xef2.visualkeymap.api.VisualKeymapApi`。内置实现可参考 `dev.xef2.visualkeymap.api.MinecraftImpl`，键位契约可参考 `KeyBinding`。Minecraft 26.3 社区移植版保留了这一 Provider API 及原有 entrypoint 名称。

## 许可证与署名

Visual Keymap 使用 GNU General Public License version 3。原始作者及上游项目署名归 XeF2 和上游贡献者所有。本仓库包含非官方社区维护的 Minecraft 26.3 修改，不代表获得原作者认可或背书。

请参阅 [LICENSE](../LICENSE)、[NOTICE](../NOTICE)及[原始 Visual Keymap 仓库](https://github.com/USTC-XeF2/visual-keymap)。
