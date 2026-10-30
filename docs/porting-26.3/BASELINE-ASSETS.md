# Visual Keymap 26.2 行为基线材料

本文区分仓库内合成夹具与必须由真实 26.2 客户端产生的证据。合成夹具不能替代真实导出、文件对话框、provider 枚举、保存时机和重启往返。

## 1. 已自动生成的材料

仓库内 [`test-fixtures/porting-26.3/`](../../test-fixtures/porting-26.3/) 已包含：

- version 1 的既有键盘码、鼠标 `0..7`、空绑定、modifier 和 F25 tombstone 输入；
- 已分配扩展区首尾值与一个未分配扩展值；
- Visual Keymap 默认、非默认、`null`、空对象、未知字段和非法 JSON 配置输入。

这些文件使用 `fixture.*` provider/ID，不含真实账号、服务器、聊天或命令数据，也不会被打进模组 JAR。Phase 2 已有的 26.2 runtime 探针还证明单个扩展值 `16777217` 可在原版 `KeymapSnapshot` JSON/对象路径中原值往返。

## 2. 在 26.2 原版导出 version 1 快照

使用已验证可启动的 26.2 开发实例，先退出其他 Minecraft 实例，避免混淆目录。

1. 进入“选项 → 控制”，点击“可视化键位图…”；也可从 Mod Menu 或已绑定的“打开可视化键位图”进入，但本次建议从控制菜单进入，以同时验证 Mixin 入口。
2. 记录并修改下列代表性绑定：
   - 原版普通键：把“跳跃”改为 `J`；
   - 原版清空：选择“丢弃物品”后按 `Esc`，确认显示未绑定；
   - 原版鼠标：保留攻击=左键、使用物品=右键、选取方块=中键；有 M4 硬件时，再把一个低风险绑定临时设为 M4；
   - 原版功能/导航键：将一个低风险绑定临时设为 `F12` 或 `Home`；
   - MaLiLib：一个单键 `K`、一个两键组合 `Left Shift + K`、一个 M4 热键；
   - CommandKeys：一个单键 `F8` 宏、一个 `Left Alt + F9`（limit + 主键）宏，并建立第二个 profile。
3. 返回 Visual Keymap 主界面，点击底部“导出”。源码默认保存位置是当前实例的 `config/visualkeymap-export.json`；在文件对话框中改名为 `visualkeymap-26.2-golden.json` 并保存到单独采集目录。
4. 再点击“导入”，选择刚导出的文件。相同数据不应打开变更预览；随后人为改动一个绑定，再导入同一文件，录下预览与确认/取消行为。
5. 退出界面并彻底关闭客户端，再复制材料。不要在游戏运行时复制正在写入的配置。

导出代码证据为 `VisualKeymapScreen#doExport()` 和 `KeymapSnapshot#fromBindings()/writeToFile()`。文件必须含顶层 `"version": 1`。CommandKeys 当前 provider 名为空，因此不会出现在快照中；这是 26.2 既有行为，不是漏采。

## 3. 配置文件准确位置

对于本仓库 `runClient` 实例，实例根目录是仓库的 `run/`：

| 内容 | 路径 | 说明 |
| --- | --- | --- |
| Minecraft 原生键位 | `run/options.txt` | 含原生 key mapping；可能同时含账号无关但环境相关的其他选项，提交前只复制到私有采集包或做脱敏差异。 |
| Visual Keymap 配置 | `run/config/visualkeymap.json` | `ModConfig` 固定文件名；只含 `showNumpad`、`prioritizeConflictingKeybinds`、`sortMode`。 |
| Visual Keymap 导出默认位置 | `run/config/visualkeymap-export.json` | 文件对话框默认值；建议另存为 `visualkeymap-26.2-golden.json`。 |
| CommandKeys | `run/config/commandkeys.json` | CommandKeys 3.103.0 的 `Config.FILE_NAME` 字节码确认为该文件名，配置根目录来自 Fabric config dir。 |
| MaLiLib 热键 | `run/config/` 下实际拥有该热键的模组配置文件 | MaLiLib `ConfigManager` 保存所有已注册 config handler；热键通常由 Litematica/MiniHUD 等所属模组保存，不应假定所有数据都在一个通用 `malilib.json` 中。 |

若使用外部实例，把 `run/` 替换为该实例目录；其余相对路径不变。

## 4. 获取 MaLiLib 与 CommandKeys 的真实配置数据

### MaLiLib

1. 客户端关闭时，先复制整个 `config/` 目录的文件名、修改时间和 SHA-256 清单。
2. 启动客户端，只修改计划中的三个 MaLiLib 热键并正常退出。
3. 再生成同一清单；只复制新增或哈希变化的所属模组 JSON。
4. 打开文件检查用户名、服务器地址、坐标、路径等；只保留与测试热键有关且可安全分享的文件。若所属模组把大量私有配置写在同一文件，提供脱敏副本和原文件 SHA-256，不要直接提交原文件。

### CommandKeys

1. 在 CommandKeys 配置界面创建两个 profile。
2. 第一个 profile 建立单键 `F8` 宏；第二个建立 `Left Alt + F9` 宏。消息使用无隐私的固定文本，例如本地测试世界中的 `/say vk-baseline`。
3. 正常退出客户端后复制 `run/config/commandkeys.json`。
4. 检查并删除真实服务器链接、私人消息、命令参数、用户名或其他宏；若删除会改变结构，先保留私有原件，再提供结构等价的脱敏副本并记录差异。

## 5. 最值得录屏/人工观察的行为

建议以同一 GUI 缩放比例录一段连续视频，并在关键处停留一秒：

1. 控制菜单、独立快捷键、Mod Menu 三个入口，以及“完成”或返回键回到正确父屏。
2. 点击键帽后的筛选、再次点击取消筛选、未绑定列表、搜索框中英文输入。
3. 选择一行后输入普通键；按 `Esc` 清空；左/右/中鼠标与 M4；MaLiLib 两键组合在按下和释放时的结束时机；CommandKeys limit + 主键顺序。
4. 冲突键帽颜色、冲突 tooltip、冲突项置顶开关、三种排序方式、数字小键盘显示开关。
5. 修改后键帽和行列表即时刷新；逐行重置；通过完成、Esc、打开配置页三种方式离开后重启，确认持久化。
6. 导出对话框默认路径与取消；相同快照导入无预览；有差异时的旧键→新键文本、默认勾选、全选/部分选择/全取消、确认与取消。
7. 至少记录 GUI scale 2 和一个非默认 scale；中文与英文各一次。IME 预编辑若可用，单独录制搜索框输入。

## 6. 交付最小包与隐私检查

请提供一个不放入 Git 的压缩包，至少包含：

- `visualkeymap-26.2-golden.json`；
- `visualkeymap.json`；
- 脱敏后的 `options.txt` 或仅 key mapping 行及原文件 SHA-256；
- 实际变化的 MaLiLib 所属模组配置；
- `commandkeys.json` 的脱敏副本；
- 一段入口/编辑/导入导出/重启往返录屏，以及一份简短操作顺序说明。

提交前搜索并检查：用户名、UUID、access token、服务器地址、世界名、坐标、聊天、命令参数、绝对用户目录。真实材料在收到并验证前仍标记为“待人工验证”。
