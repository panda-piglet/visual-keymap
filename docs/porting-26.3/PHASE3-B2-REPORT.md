# Visual Keymap Phase 3 B2 执行报告

日期：2026-10-30

分支：`port/mc-26.3`

范围：B2 核心输入、GUI、文件对话框与快照核心路径；不包含 C1/C2/C3 第三方 adapter 完整迁移。

## 1. 阶段结论

B2 的 8 个已知编译错误已经全部消除，生产源码可用 JDK 25、Minecraft 26.3、Loom 1.19.3 完成 `clean assemble`。Minecraft 26.3 客户端实际启动成功，原生控制菜单入口和 Visual Keymap 主界面可打开；普通键、右键、未绑定/重置、搜索文本输入、SDL 保存对话框与取消返回完成了实际 GUI 冒烟验证。

本报告不把 B2 判定为“完整功能回归通过”。真实 26.2 version 1 golden file 尚未在安全隔离的 26.3 gameDir 中完成导入、确认和再次导出；完整鼠标范围、扩展键、F25、错误路径、重启持久化和多语言/多缩放也未完成动态验证。

当前**暂不建议直接进入 C1/C2/C3**。建议先完成以下两个 B2 验证收口条件：

1. 为 Loom `client` run configuration 设置真正独立的 `runDirectory`，不得只修改 `runClient` Java 进程的 `workingDir`。
2. 在该隔离实例中完成真实 26.2 golden file 的导入预览、应用、保存和 version 1 再导出往返。

这不是建议删除、禁用或空实现任何第三方功能；C1/C2 仍需继续保留并迁移 MaLiLib 与 CommandKeys 行为。

## 2. 迁移前状态与基线

- B2 开始时分支为 `port/mc-26.3`，工作树干净。
- Phase 3 A/B1 提交存在：`d62692f`、`b88ce4b`、`0f811dd`、`0f40f55`。
- 真实 26.2 快照已保存为 `test-fixtures/porting-26.3/snapshots/real-26.2-malilib-v1.json`，原始文件证据见 `BASELINE-VALIDATION.md`。
- 该 golden file 的静态复核结果为 `version=1`、2 个 binding、规范键码 `74`（J）和 `298`（F9）。
- 真实 26.2 `visualkeymap.json`、MaLiLib/Tweakeroo 配置、CommandKeys 配置、真实鼠标快照及重启持久化证据仍未提供；见 `BASELINE-VALIDATION.md` 的证据边界。

## 3. Git 提交与变更范围

| B2 单元 | 提交 | 主要文件 | 内容 |
| --- | --- | --- | --- |
| B2-1 | `da17421` | `input/MinecraftInputCode.java`、`VisualKeymap.java`、`api/KeyBinding.java`、`api/MinecraftImpl.java`、`KeymapSnapshot.java` | 建立 Minecraft typed key 与 B1 规范整数的唯一核心边界；原版键位读写、显示和快照应用改用规范码；未知扩展值和 F25 tombstone 不应用到 live binding。 |
| B2-2 | `f919e6b` | `VisualKeymapScreen.java`、`KeyboardWidget.java`、`ImportPreviewScreen.java`、中英文语言文件 | GUI 选择、筛选、键帽状态和编辑接入规范码；导入预览显示不可用键并禁用对应勾选；增加不可用/不支持键码文本。 |
| B2-3 | `480f5f4` | `gui/NativeFileDialog.java`、`VisualKeymap.java`、`VisualKeymapScreen.java` | 用 Minecraft 26.3 已附带的 LWJGL SDL 3 文件对话框替换 tinyfd；保留标题、JSON 过滤、默认导出文件名、取消语义和导入预览流程。 |
| B2-4 | 本报告提交（哈希见最终交付） | `docs/porting-26.3/PHASE3-B2-REPORT.md` | 编译、启动、GUI 冒烟、测试状态、剩余风险和后续门槛。 |

没有修改：

- `integration/MaLiLibIntegration.java`
- `integration/CommandKeysIntegration.java`
- Mod Menu entrypoint 实现
- `api/VisualKeymapApi.java` 及既有 public `List<Integer>` 签名
- 快照 `version = 1` schema
- Mixin 配置和注入点
- Gradle 构建配置、依赖版本或发布配置

## 4. 原有 8 个编译错误

基线命令：

```powershell
$env:GRADLE_USER_HOME='E:\02_Development\01_Workspace\VisualKeyMap26.3\.gradle-user-home'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp'
.\gradlew.bat clean compileJava --no-daemon --console=plain
```

基线结果：8 个错误。处理结果如下。

| # | 原位置 | 原功能 | 26.3 变化 | 最小处理 | 行为风险与验证 |
| --- | --- | --- | --- | --- | --- |
| 1 | `VisualKeymap.java` GLFW import | 声明默认未绑定快捷键 | Minecraft 26.3 输入后端改为 SDL，不再提供原 GLFW 编译入口 | 删除 GLFW import | `compileJava` PASS；客户端原控制页显示该快捷键默认 `Not Bound`。 |
| 2 | `VisualKeymap.java` `GLFW_KEY_UNKNOWN` | 注册默认未绑定 KeyMapping | 同上；`InputConstants.UNKNOWN` 仍是目标源码中的未绑定 typed key | 用 `InputConstants.Type.KEYBOARD` 与 `InputConstants.UNKNOWN.getValue()` 构造 | 实际 GUI 显示默认未绑定；未验证重启和游戏内快捷键触发。 |
| 3 | `VisualKeymapScreen.java` tinyfd import | 原生打开/保存对话框 | 26.3 运行类路径没有原 tinyfd API；已有 `org.lwjgl:lwjgl-sdl:3.4.3` | 删除 tinyfd、MemoryStack 和 PointerBuffer import | `compileJava` PASS；SDL 保存对话框实际打开。 |
| 4 | `VisualKeymapScreen#doExport` tinyfd save call | 导出快照 | SDL 文件对话框为异步 callback，不可作同步字符串返回值替换 | `NativeFileDialog.showSave`，callback 结果在客户端 tick 消费 | 默认名、过滤器、取消和焦点返回实测 PASS；实际写入及失败路径未测。 |
| 5 | `VisualKeymapScreen#doImport` tinyfd open call | 选择快照并进入预览 | 同上 | `NativeFileDialog.showOpen`，保留原匹配和预览逻辑 | 编译/静态流程 PASS；真实 golden 动态导入 BLOCKED。 |
| 6 | `api/KeyBinding.java` `Type.KEYSYM` | 按规范整数取得显示名 | 26.3 类型为 `KEYBOARD`，且 keyboard native value 已改成 SDL；直接改枚举名会破坏旧整数 | 统一调用 `MinecraftInputCode.getDisplayName` | B1 自检 PASS；普通键和右键显示实测 PASS。 |
| 7 | `ImportPreviewScreen.java` `Type.KEYSYM` | 将快照整数转换为 live key | 同上 | 通过 `MinecraftInputCode.decode`；不可应用项默认不选且 checkbox 禁用 | 静态验证 PASS；未知扩展/F25 预览尚未动态验证。 |
| 8 | `KeymapSnapshot.ImportMatch` `Type.KEYSYM` | 确认导入时生成 typed key | 同上 | `canApply()` 后集中解码；不支持值拒绝应用 | B1 自检覆盖 F25、未知扩展和边界；真实快照动态往返 BLOCKED。 |

错误收敛记录：

- B2-1 后：8 → 4（剩余 ImportPreview 1 + tinyfd 3）。
- B2-2 后：4 → 3（只剩 tinyfd 3）。
- B2-3 后：3 → 0，`compileJava` 成功。

## 5. 核心输入和 GUI 实现

### 5.1 Minecraft 输入边界

`dev.xef2.visualkeymap.input.MinecraftInputCode` 是 B2 核心路径唯一的 Minecraft typed-key 边界：

- `encode(InputConstants.Key)`：keyboard 按稳定 key name 进入 B1；mouse 按 B1 显式原生按钮映射进入 `0..7`。
- `decode(int)`：旧规范 keyboard、mouse 和已分配扩展值转回 26.3 typed key。
- `canApply(int)`：F25 tombstone、未分配扩展值和未知旧值返回 false。
- `getDisplayName(int)`：支持值使用 Minecraft 名称，不支持值显示明确文本。

核心 GUI、原版 provider 和快照模块未直接把 SDL keyboard integer 当成公开规范码。源码扫描只发现两个与 `InputConstants.Key#getValue()` 相关的 B2 核心位置：

1. `VisualKeymap.java` 为注册默认未绑定快捷键读取 `InputConstants.UNKNOWN` 的 native sentinel；它不是公开键码或快照数据。
2. `MinecraftInputCode.java` 在集中边界读取原生 mouse button。

### 5.2 选择、筛选和编辑

- `VisualKeymapScreen#getBindingsForKey`、`setSelectedKey` 使用规范整数，避免 SDL keyboard `4..8` 与 public mouse `4..7` 碰撞。
- `KeyboardWidget.KeyWidget` 缓存规范码用于选择边框比较。
- 编辑时无法编码的 26.3 native key 被消费但不写入 binding；不会泄漏裸 SDL 值。
- 原版 provider 的 `getKeyCodes()` 使用集中编码，`setBoundKeys()` 仍通过 typed key 调用 `KeyMapping#setKey` 并执行 `KeyMapping.resetMapping()`。
- `ImportPreviewScreen` 对不支持值显示明确文本、取消默认勾选并禁用 checkbox；确认路径再次检查 `canApply()`。

### 5.3 文本输入和渲染

目标 26.3 `EditBox` 源码仍自行处理 preedit 更新和 `TextInputManager.setTextInputArea`。Visual Keymap 没有添加重复的 TextInputManager 调用。实际运行中搜索框可获得焦点、输入 `Visual Keymap`，列表即时排序并出现黄色匹配文本，清空后恢复。

本阶段没有进行 UI 改版；布局类、按钮顺序、屏幕返回关系和配置页面代码均保持原结构。

## 6. SDL 文件对话框

### 6.1 技术依据

目标运行类路径已包含 LWJGL SDL 3.4.3，不新增第三方库。实现使用：

- `SDL_ShowFileDialogWithProperties`
- `SDL_DialogFileCallback`
- `SDL_DialogFileFilter`
- `SDL_CreateProperties` / `SDL_DestroyProperties`

SDL 官方说明确认：文件对话框为异步操作；callback 可能在其他线程调用；`filelist` 只在 callback 返回前有效；filter memory 必须至少保持到 callback 被调用；filter pattern 使用扩展名（本实现为 `json`），不是 tinyfd 的 `*.json`。

实现因此：

- 在 callback 中立即复制首个 UTF-8 path，不保存 `filelist` 指针。
- 用 active request set 保持 callback、filter struct 和 UTF-8 buffer 的强引用。
- 在客户端 tick 执行原文件操作；再延后一 tick 释放 callback 和 native memory，避免 SDL 尚在从 callback 返回时释放 trampoline。
- 把 Minecraft SDL window handle 作为父窗口，保持 modal 和焦点返回。
- cancel 或 SDL error 均不调用消费方，维持原来的无操作/无用户提示语义。

参考：[SDL dialog header/API](https://github.com/libsdl-org/SDL/blob/main/include/SDL3/SDL_dialog.h)、[SDL_ShowFileDialogWithProperties](https://wiki.libsdl.org/SDL3/SDL_ShowFileDialogWithProperties)、[SDL_DialogFileCallback](https://wiki.libsdl.org/SDL3/SDL_DialogFileCallback)。

### 6.2 实际 GUI 结果

- 保存窗口标题：`Export Keybindings`。
- 默认文件名：`visualkeymap-export.json`。
- 文件类型：`JSON (*.json)`。
- 对话框为 Minecraft owned modal；取消后返回 Visual Keymap，输入焦点恢复。
- 取消后检查 `run/config/visualkeymap-export.json`：不存在。
- 实际保存、覆盖已有文件、打开对话框、无效 JSON、不可读/不可写路径均未动态测试。

## 7. Gradle 命令与结果

所有 Gradle 命令均使用：

```powershell
$env:GRADLE_USER_HOME='E:\02_Development\01_Workspace\VisualKeyMap26.3\.gradle-user-home'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp'
```

| 命令 | 状态 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat clean compileJava --no-daemon --console=plain`（B2 前） | FAIL | 精确重现 8 个已知 API 错误。 |
| `.\gradlew.bat compileJava --no-daemon --console=plain`（B2-3 后） | PASS | `BUILD SUCCESSFUL in 14s`，已知错误为 0。 |
| `.\tools\porting-26.3\Test-CanonicalInputCode.ps1` | PASS | `legacy=121 extension=127 assignedKeyboard=247 mouse=8`；扩展表 SHA-256 为 `0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28`。 |
| `.\gradlew.bat clean build --no-daemon --console=plain` | FAIL | 编译、资源、jar、sourcesJar、testClasses 均完成；`:test` 因有 test source 但未发现 JUnit 测试而失败。现有 `CanonicalInputCodeSelfTest` 是独立 main 自检。不是 B2 生产编译错误，但会阻止 release 风格 `build`。 |
| `.\gradlew.bat clean assemble --no-daemon --console=plain` | PASS | `compileJava`、资源、jar、sourcesJar、assemble 全部成功，`BUILD SUCCESSFUL in 33s`。 |
| 临时 init script + `.\gradlew.bat runClient ...` | PASS（启动）/ BLOCKED（隔离） | Minecraft 26.3、Loader 0.20.1、Fabric API 0.165.0+26.3、Java 25 启动；创建 SDL Windows 窗口并进入主菜单。Loom gameDir 仍为项目 `run/`，见第 9 节。 |

## 8. 功能验证状态

状态只使用 `PASS`、`FAIL`、`NOT TESTED`、`BLOCKED`、`STATICALLY VERIFIED`。一个测试只完成部分步骤时，不把整个 TEST-PLAN 用例标为 PASS。

| TEST-PLAN 项 | 状态 | 实际证据或边界 |
| --- | --- | --- |
| B-01 `clean build` | FAIL | `:test` 无已发现测试；生产 `clean assemble` 另为 PASS。 |
| B-02 P1 客户端启动 | NOT TESTED | 本次不是无可选依赖的 P1。混合实例进入主菜单；Visual Keymap、MaLiLib 0.30.2、Mod Menu 21.0.0 初始化，无 B2 链接崩溃；该会话未加载 CommandKeys。 |
| B-03 P2–P5 组合 | NOT TESTED | 只观察到 MaLiLib + Mod Menu 组合启动，未逐一建立 P2–P5，不能把该观察扩展为全部组合 PASS。 |
| B-04 成品资源 | PASS | 实际打开 `build/libs/visual-keymap-26.3-1.4.0.jar`：`fabric.mod.json` 展开为 Minecraft `~26.3`、Loader `>=0.20.1`，client/modmenu entrypoint、资源和 `visual-keymap.mixins.json` 均存在。 |
| B-05 Controls Mixin | PASS | 目标客户端“Options → Controls”实际出现 `Visual Keymap...`，原控制按钮仍显示。 |
| N-01 控制菜单入口 | PASS | 实际点击入口并打开完整主界面。 |
| N-02 返回链 | NOT TESTED | 本次未完整执行 Done 与 Esc 两条路径。 |
| N-04 打开键默认状态 | PASS | 主界面列表显示 `Visual Keymap - Open Visual Keym...` 为 `Not Bound`。 |
| N-05 游戏内快捷键触发 | NOT TESTED | 仅编辑过显示值，未退出屏幕触发。 |
| V-01 多语言/多缩放布局 | NOT TESTED | 仅英文、当前窗口和当前缩放观察到主界面无关键遮挡，不足以覆盖完整用例。 |
| V-02 键盘布局 | NOT TESTED | 主键区、功能键、导航、方向和数字小键盘可见；未逐键核对点击区和倍数。 |
| V-03 数字小键盘配置 | NOT TESTED | 当前默认显示数字小键盘，未切换配置。 |
| V-04/V-05 冲突颜色和 tooltip | NOT TESTED | 未构造完整 0/1/多绑定和 5/6+ 数据集。 |
| L-01 未绑定列表 | PASS | 初次主界面下方只显示 `Not Bound` 项。 |
| L-04 搜索 | NOT TESTED | 单个 `Visual Keymap` 查询实际 PASS；类别、键名、多词、大小写和空格矩阵未完成。 |
| E-01 普通键编辑 | NOT TESTED | 将本模组打开键从未绑定改为 F8 并即时显示成功；未验证游戏内实际动作。 |
| E-02 Esc 清空 | NOT TESTED | 未执行。 |
| E-03 鼠标编辑 | NOT TESTED | Right Button 实际写入和显示 PASS；左/中/M4/M5 未执行，不能判整个用例 PASS。 |
| E-06 重置 | NOT TESTED | Minecraft 打开键由 Right Button 重置为 `Not Bound` PASS；MaLiLib/CommandKeys 未测。 |
| E-07 保存和重启 | BLOCKED | gameDir 未正确隔离；停止继续验证。 |
| E-08 即时刷新 | PASS | F8、Right Button、Not Bound 在编辑/重置后立即刷新。 |
| C-01..C-05 配置 | BLOCKED | 本次错误使用项目 `run/`，不再把配置写入视为合格隔离证据。 |
| X-01 导出对话框和取消 | PASS | 标题、默认名、JSON 过滤、取消和无导出文件均有实际证据。默认目录来自错误的 `run/config`，隔离问题另列。 |
| X-02/X-03 实际导出 | BLOCKED | 为避免继续写旧 `run/`，没有保存文件。schema 静态保持 version 1。 |
| X-04..X-11 导入/预览/错误路径 | BLOCKED | 需要正确隔离的 26.3 gameDir；未把静态代码阅读当作动态 PASS。 |
| S-01/S-02 共同键与碰撞 | PASS | B1 自检覆盖 121 legacy、127 extension、247 keyboard、8 mouse、A–E 与 mouse 4–8 碰撞边界和逐项往返。 |
| S-03 全鼠标路径 | NOT TESTED | 自动映射覆盖 8 个 mouse；动态只验证 Right Button 编辑/显示。 |
| S-04 集中边界扫描 | STATICALLY VERIFIED | B2 核心 GUI、冲突、快照无裸 SDL keyboard integer；默认 unknown 注册 sentinel 是唯一核心例外，不进入公开数据。第三方 adapter 明确留到 C。 |
| S-05 真实 26.2 golden 往返 | BLOCKED | golden 文件静态解析 PASS（version 1、J=74、F9=298）；26.3 导入/再导出未做。 |
| S-06 F25 tombstone | STATICALLY VERIFIED | B1 decode 为不可应用；预览禁用路径已接入。动态显示/导出未测。 |
| S-07 26.3-only 扩展键 | PASS（自动）/ NOT TESTED（GUI） | 固定表、自检、碰撞和 encode/decode 往返 PASS；GUI 编辑、快照往返未动态测试。 |

## 9. 运行目录隔离事故与证据

### 9.1 发生情况

为避免复用现有 `run/`，第一次启动使用位于忽略目录 `build/` 的临时 Gradle init script 修改 `runClient.workingDir`。启动日志仍出现：

```text
...\VisualKeyMap26.3-repo\run\config\malilib.json
```

第二次增加 task graph 时的强制 workingDir 校验，日志确认 JavaExec workingDir 为：

```text
...\runs\mc-26.3-b2-isolated
```

但 SDL 保存对话框和 MaLiLib 日志仍显示 Fabric gameDir 为项目 `run/config`。这证明仅改 JavaExec workingDir 不能覆盖 Loom run configuration 的 `runDir`。

Fabric Loom 官方文档也说明默认所有 run configuration 使用 `${projectDir}/run`；Loom 1.17+ 迁移表把旧 `runDir` 对应到新 `runDirectory` 属性：[Loom 文档](https://docs.fabricmc.net/develop/loom/)、[Loom options](https://docs.fabricmc.net/develop/loom/options)。后续应在临时测试配置或经批准的构建配置中设置 `loom.runs.client.runDirectory`，然后在启动前检查 Fabric 实际 `getGameDir()`，不能只检查进程工作目录。

### 9.2 被更新的忽略文件

20:10 之后确认被此次启动创建或更新时间的项目 `run/` 内容：

- `.fabric/processedMods/placeholder-api-3.2.0+26.3-*.jar`
- `config/visualkeymap.json`
- `data/fabric_default_resource_packs.json`
- `logs/latest.log`、`logs/debug.log` 及轮换 `.gz`
- `options.txt`

`config/visualkeymap.json` 当前内容仍是三项默认值：`showNumpad=true`、`prioritizeConflictingKeybinds=true`、`sortMode=DEFAULT`。取消保存后不存在 `run/config/visualkeymap-export.json`。

这些路径均被 Git 忽略，没有进入提交。由于启动前没有为每个 ignored file 建立 hash/备份，无法安全证明或恢复其精确旧内容；本阶段没有删除、覆盖回滚或清理这些文件。维护者应把该 `run/` 视为已被 26.3 启动触碰，不再作为纯 26.2 行为证据。

## 10. 快照和数据兼容结论

### 已证实

- `KeymapSnapshot.fromBindings` 仍写 `version = 1`，字段仍为 `provider/id/keys/modifiers`。
- public `List<Integer>` 接口没有修改。
- B1 固定编码表和碰撞检查继续 PASS。
- 原版 Minecraft provider 的 public keys 通过 key name/mouse map 转为规范整数，不写裸 SDL keyboard value。
- 未知扩展值和 F25 不会被 `ImportPreviewScreen` 确认路径写入 live binding。
- 真实 26.2 golden file 未被修改，静态内容仍为 J=74、F9=298。

### 尚未证实

- 真实 golden 在 26.3 中的导入预览文本、确认应用、保存和再次导出。
- 包含 mouse `0..7`、空 keys、modifiers、F25 或扩展值的真实 26.2 用户文件往返。
- 未知扩展值在完整“导入 → 编辑其他项 → 保存/导出”流程中是否始终保留；当前代码保证“不应用到 live binding”，但动态数据保留流程未完成。
- 不可读、无效 JSON、未知 version、null 字段和不可写输出的 26.2→26.3 行为对照。

## 11. 对 C1/C2/C3 的影响

### MaLiLib（C1）

`MaLiLibIntegration#getKeyCodes` 仍按 26.2 规则对所有 `< -1` 值加 100；`setBoundKeys` 仍读取 `InputConstants.Key#getValue()`。在 SDL 后端，这不能视为兼容：

- keyboard native SDL value 会泄漏到 MaLiLib，而不是规范整数。
- `-99..-92` mouse 和 MaLiLib 0.30.2 其他负值需要明确区分。
- gamepad/新增负值必须避免旧“所有负值统一偏移”误改。

本阶段未改动这些代码；C1 必须把 adapter 边界接入集中兼容层，且不得删除 MaLiLib 功能。

### CommandKeys（C2）

`CommandKeysIntegration#getKeyCodes` 仍直接返回 `InputConstants.Key#getValue()`；在 26.3 会把 SDL keyboard value 暴露给 GUI、冲突、排序和快照逻辑。`setBoundKeys` 虽接收 typed key，但其读回语义仍需 C2 对称适配。本次实际客户端 mod list 没有 CommandKeys，因此没有动态链接或行为证据。

本阶段未改动这些代码；C2 必须保留 profile、limit key、主 key、搜索文本和保存语义。

### Mod Menu（C3）

Mod Menu 21.0.0 在本次客户端初始化，无启动错误；但没有从 Mod Menu 配置按钮打开并返回，因此 MM-01/MM-02 仍是 NOT TESTED。entrypoint 代码未修改。

### 对外 entrypoint/API

`VisualKeymapApi`、`KeyBinding` 既有抽象方法签名、entrypoint key 和 provider 顺序未修改。新增 `MinecraftInputCode`/`NativeFileDialog` 不位于公开 `api` 包，不能替代 API-01..API-05 的消费方测试。C 阶段仍需独立测试模组验证。

## 12. 剩余问题与建议

### 必须先收口

1. **Loom gameDir 隔离**：设置 run configuration 的 `runDirectory` 并在游戏日志/对话框中确认实际 config 路径，不再依赖 JavaExec workingDir。
2. **真实 v1 golden 动态往返**：完成导入预览、应用、重启、再次导出，比较 schema 和规范整数。
3. **Gradle `build` 测试任务**：决定把 `CanonicalInputCodeSelfTest` 接入 Gradle 可发现测试，或把它移出 `src/test` 并由独立验证任务执行。不得以 `failOnNoDiscoveredTests=false` 静默掩盖未运行测试。

### 可在后续控制的风险

- SDL dialog 在 Windows 已冒烟；Linux/macOS 平台属性支持和焦点行为未验证。
- 文件 IO 失败仍与原版一样静默；本阶段未改变用户可见行为，也未把历史行为改良纳入迁移。
- F25 和未知扩展预览只做静态验证。
- 完整 mouse、GUI 缩放、多语言、冲突 tooltip 和配置错误矩阵未执行。
- C1/C2 adapter 仍会使用 SDL raw value，必须在其授权阶段修复，不能据当前核心 GUI 通过宣称第三方兼容。

## 13. 停止点

B2 生产实现、独立提交、编译和有限运行验证已完成。本阶段在此停止：

- 未进入 C1/C2/C3。
- 未修改第三方 adapter。
- 未修改 public API 或快照 schema。
- 未清理或回滚不明 ignored runtime 文件。
- 未发布、合并主分支或推送远程。
