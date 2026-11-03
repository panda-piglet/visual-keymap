# 可视化键位图

[English](../README.md) | 简体中文

> **Minecraft 26.3 社区维护预发布版：**此分支是对 [XeF2 原版 Visual Keymap](https://github.com/USTC-XeF2/visual-keymap) 的社区迁移，当前尚不是稳定版。

![可视化键位图界面的屏幕截图](screenshot-zh-CN.png)

本 Mod 允许你在游戏中以键盘布局可视化查看当前绑定的键位，并且在统一的配置页面中更改来自各个 Mod 的键位绑定。

可以通过 `按键控制 - 可视化键位图` 菜单打开键位图界面，或是在 ModMenu 中进入本 Mod 配置屏幕。本 Mod 目前提供了 MaLiLib 与 CommandKeys 的集成。

## 支持平台

- Minecraft 26.3
- Fabric Loader 0.20.1 或更高版本
- 适用于 Minecraft 26.3 的 Fabric API（已使用 0.165.0+26.3 测试）
- Java 25

## 安装

1. 安装 Minecraft 26.3、Fabric Loader 和 Fabric API。
2. 将 Visual Keymap JAR 放入客户端的 `mods` 目录。
3. 从`选项 - 按键控制 - 可视化键位图`打开。

本次迁移已测试以下可选集成：

- Mod Menu 21.0.0：可通过模组配置按钮打开 Visual Keymap。
- MaLiLib 0.30.2：显示兼容 MaLiLib 模组注册的热键；集成测试使用了 Tweakeroo 0.30.1。
- CommandKeys 3.104.1+26.3 **Fabric**：显示当前 CommandKeys profile 中的绑定。

这些可选模组不会被打包进 Visual Keymap JAR。

## 快照兼容性

- 继续支持原有 version 1 快照格式。
- 当不同模组使用相同的 provider 和 binding ID 时，旧版 version 1 快照无法唯一标识绑定。Visual Keymap 会显示歧义提示并要求用户选择正确目标；歧义项默认不会应用。
- CommandKeys 宏仍不会进入 Visual Keymap 快照导出，与原版行为一致。
- 非法或损坏的 JSON 快照会在游戏内显示错误提示，不再导致客户端崩溃。

这是预发布版本。罕见硬件按键和部分文件系统失败路径尚未完整测试。测试前请阅读[已知问题](release/KNOWN-ISSUES.md)，社区迁移版问题请提交到 [Fork issue tracker](https://github.com/panda-piglet/visual-keymap/issues)。

## 集成

所有拥有独立键位绑定系统的 Mod 都可以通过设置入口点使用此功能。

```json
"entrypoints": {
  "visualkeymap": [
    "com.example.mymod.MyModVisualKeymapImpl"
  ]
}
```

入口点类需要实现 `dev.xef2.visualkeymap.api.VisualKeymapApi` 接口，具体实现方式可参考 `dev.xef2.visualkeymap.api.MinecraftImpl` 类。

## 许可证与署名

Visual Keymap 使用 GNU General Public License version 3。原项目及作者署名继续归于 XeF2；本仓库包含社区维护的 Minecraft 26.3 修改。详见 [NOTICE](../NOTICE) 与 [LICENSE](../LICENSE)。
