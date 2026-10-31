# Visual Keymap 26.3 人工回归指南

日期：2026-10-31

## 1. 使用原则

本指南只列自动化或动态探针尚不能充分证明的真实 GUI、物理输入和文件系统行为。已经由 7 个 JUnit、真实 adapter 探针和 P1–P6 成品 JAR启动矩阵充分覆盖的纯模型项目不要求重复手测。

证据状态必须使用：`PASS`、`FAIL`、`NOT TESTED`、`BLOCKED`、`STATICALLY VERIFIED`。若只完成一个子项，不能把整组标为 PASS。

测试原始文件不得直接修改：

- `baseline-private/26.2/version 1.json`；
- `baseline-private/26.2/options.txt`；
- 任何真实 MaLiLib/CommandKeys 用户配置。

先复制到对应测试 gameDir，再导入副本。证据中不要提交用户名、绝对个人路径、服务器地址、聊天内容或其他个人配置。

## 2. 实例与最少重启方案

优先只使用三个 26.3 会话：

| 会话 | 组合 | 主要测试 |
| --- | --- | --- |
| A | P1：Visual Keymap + Loader/API | 原生入口、核心 GUI、键鼠编辑、配置、SDL 对话框、Minecraft 快照、语言/缩放。 |
| B | P5：P1 + MaLiLib/Tweakeroo + CommandKeys + Mod Menu | 三项可选集成、provider 共存、真实热键/宏触发。 |
| C | P6：P1 + 外部 Provider/失败 Provider | 公开 API 的真实 GUI、tooltip、编辑、重置、保存和快照。 |

错误文件、只读目录和“首次配置”使用会话 D（P1 的额外独立副本），避免污染会话 A。F25/扩展键按设备条件使用会话 A。

成品 JAR 夹具命令示例：

```powershell
.\gradlew.bat clean test build --no-daemon
.\gradlew.bat -p test-fixtures/porting-26.3/production-smoke runClient -Pphase4Profile=P1 -Pphase4Manual=true --no-daemon
```

把 `P1` 改为 `P5` 或 `P6` 即可启动其他会话。夹具使用 `production-smoke/runs/pN` 独立目录，实际加载 `build/libs/visual-keymap-26.3-1.4.0.jar`。若要验证真正的首次启动，请新建独立实例或复制工作区；不要删除、覆盖现有证据目录，也不要复用旧 `run/`。

每个会话至少保存：

- `logs/latest.log`；
- `phase4-production-smoke.txt`；
- 操作前后 `options.txt`、相关 config 和导出 JSON 的副本/哈希；
- 关键页面截图；
- 涉及按住/释放、返回链或实际触发时的短录屏。

## 3. 会话 A：P1 核心功能

### MR-01 原生入口与返回链

- 对应：N-01、N-02、N-04、N-05、B-05。
- 前置：P1；默认打开键未绑定。
- 操作：
  1. 标题界面进入 Options → Controls。
  2. 确认原控制项完整，打开 `Visual Keymap...`。
  3. 点击 Done 返回；再次打开后按 Esc 返回；连续重复三次。
  4. 在原版 Key Binds 中给“打开 Visual Keymap”绑定一个无冲突键。
  5. 进入本地单人世界触发；在已有屏幕打开时再触发一次。
- 26.2 已确认预期：源码与基线调查确认 Controls 页插入本地化按钮，主屏使用传入父屏；快捷键默认未绑定并在 tick 消费点击。Done 主路径在 26.3 已动态通过；26.2 实际 Esc/已有屏幕触发证据不足，需同操作对照。
- 26.3 应达到：按钮唯一、原控制项不丢失；Done/Esc 回到同一父屏；无空白/循环；快捷键每次消费后打开完整主屏。
- 证据：Controls 与主屏截图、连续打开录屏、日志、绑定前后 `options.txt`。
- PASS：所有子项符合 26.2；否则按具体子项 FAIL，不得只凭按钮存在判整组通过。

### MR-02 布局、本地化与 GUI Scale

- 对应：V-01～V-06、V-03、屏幕缩放。
- 前置：P1；准备 `en_us` 和 `zh_cn`。
- 操作：在两种语言下分别测试 GUI Scale 小/中/大、窗口最小可用尺寸和全屏；检查主键区、F 键、导航、方向、鼠标、数字小键盘；切换 Show Numpad 并返回。
- 26.2 已确认预期：固定 `KeyLayoutHelper` 布局、数字小键盘配置和两种语言资源；B2.5 已验证 showNumpad 即时/重启持久化。像素级多缩放 26.2 证据不足。
- 26.3 应达到：标题、搜索、键盘、列表和四个底部按钮无关键遮挡；键帽标签/点击区正确；数字区出现/消失并重排；没有原始翻译 key。
- 证据：每种语言至少三张代表截图，最小窗口和全屏截图，配置 JSON。
- PASS：所有组合可用且与 26.2 对照无迁移新增遮挡；设备无法全屏时标 BLOCKED。

### MR-03 列表、搜索、排序、冲突与 tooltip

- 对应：L-01～L-08、V-04、V-05。
- 前置：准备未绑定、单绑定、相同键同 order、相同键不同 order、组合顺序不同的绑定。
- 操作：
  1. 初次打开检查只列未绑定项。
  2. 点击已绑定键、再次点击、切换另一键。
  3. 搜索类别、名称、键名、多词、大小写混合、多余空格。
  4. 切换 DEFAULT、BOUND_KEY、DISPLAY_NAME，并开关冲突置顶。
  5. 悬停 0、1、5、6+ 条绑定的键帽和冲突按钮。
- 26.2 已确认预期：源码确定匹配项置顶而非过滤，优先级为搜索→冲突→排序；full key + order 决定冲突；tooltip 最多显示 5 条后显示“更多”。完整 GUI 动态基线不足。
- 26.3 应达到：选择/取消、高亮、颜色、排序和 tooltip 与上述规则一致；键盘 A–E 不得与 mouse 4–8 串扰。
- 证据：搜索前后、三种排序、冲突 tooltip 和 A–E/侧键对比截图。
- PASS：模型与可见结果一致；单一搜索样本不能替代完整矩阵。

### MR-04 原生键盘编辑、清空、重置与持久化

- 对应：E-01、E-02、E-06～E-08、N-05、S-08 Minecraft。
- 前置：选择一个可在本地世界观察的原版动作和 Visual Keymap 打开键。
- 操作：分别绑定普通字母、F 键、modifier；编辑中按 Esc；执行 Reset；通过 Done、Esc、切换其他屏幕三条离开路径保存；每次完全退出并重启；在本地世界验证动作触发。
- 26.2 已确认预期：原版 binding 最大 1 键，按下即完成；编辑中 Esc 清空；`OptionsSubScreen.removed()` 保存 options。真实 F8 26.2→26.3 导入/重启已通过。
- 26.3 应达到：显示、实际动作、即时冲突、Reset、`key.keyboard.unknown` 和重启结果一致；三条离开路径均保存。
- 证据：录屏、重启前后 `options.txt`、按键页面截图、日志。
- PASS：不仅 UI 文本正确，实际游戏动作也由新键触发。

### MR-05 鼠标左/中/右与侧键

- 对应：E-03、S-03、X-09 鼠标。
- 前置：物理鼠标；有 M4/M5 时启用，有更多侧键时记录设备能力。
- 操作：逐个编辑绑定为左、中、右、M4、M5（有条件扩展到 M8）；检查显示、冲突、筛选、导出整数、重启和实际触发。
- 26.2 已确认预期：公共/快照整数 left=0、right=1、middle=2、M4..M8=3..7；B2.5 已动态验证左/右/中和中键导出 2，侧键仅自动映射通过。
- 26.3 应达到：右/中不能互换；A–E 不与 M4–M8 互相高亮/冲突；导出保持 0–7。
- 证据：每个物理键的短录屏、导出 JSON、搜索/冲突截图。
- PASS：逐键语义正确；没有相应硬件的 M6–M8 标 BLOCKED，不可用合成表冒充物理 PASS。

### MR-06 SDL 文件对话框和正常快照流程

- 对应：X-01～X-09（正常路径）、S-05 Minecraft。
- 前置：使用真实 26.2 混合快照的副本；另准备相同快照、未知 provider/ID、空 keys、mouse 0–7 和仅 modifiers 不同夹具。
- 操作：
  1. 导出并检查标题、默认名、默认目录、JSON filter；取消一次。
  2. 打开导入对话框并取消一次，确认焦点返回。
  3. 导入完全相同快照，确认不打开预览。
  4. 导入部分匹配快照，检查预览、全选、取消部分、取消全部、Cancel、Esc。
  5. 确认后完全退出/重启，再导出并做 provider/ID/规范 keys/modifiers 语义比较。
- 26.2 已确认预期：version 1，只导出非默认且 provider 非空条目；精确 provider/ID 匹配；modifiers 差异不触发预览且不应用。真实 F8 路径已动态通过；其他错误/取消子路径未完整动态对照。
- 26.3 应达到：用户操作方式、预览默认选择和保存时机保持；取消不创建/修改文件；原始 golden 哈希不变。
- 证据：文件对话框和预览截图、导入副本与再导出文件、SHA-256、重启日志。
- PASS：按语义字段比较，不以 JSON 字节哈希不同直接判 FAIL。

### MR-07 配置读写与异常夹具

- 对应：C-01～C-05、X-10、X-11。
- 前置：会话 D，确认是全新测试目录；准备 `test-fixtures/porting-26.3/config/` 和快照异常夹具。只读目录必须是专用测试目录。
- 操作：验证首次默认配置；修改三项并重启；依次测试空文件、`null`、无效 JSON、缺字段、未知字段；导入未知 version/null/无效 JSON/不可读文件；导出到不可写位置和已存在文件。
- 26.2 已确认预期：源码显示配置直接 Gson 读取、异常时回默认并打印；快照导入只捕获 IOException，`JsonSyntaxException` 可能逃逸。26.3 无效 JSON 已实际崩溃；26.2 尚无动态对照。
- 26.3 应达到：与同一 26.2 操作一致，即使原行为不理想也不得自行“改善”。
- 证据：26.2/26.3 成对录屏、日志/崩溃报告、操作前后文件和哈希。
- PASS：只有取得 26.2 对照后才能判；此前标 `NOT TESTED` 或 `STATICALLY VERIFIED`，不能把“崩溃”直接判迁移 FAIL。

## 4. 会话 B：P5 全依赖

### MR-10 Mod Menu 实际入口

- 对应：N-03、MM-01、MM-02。
- 前置：P5，Mod Menu 21.0.0。
- 操作：Mods → Visual Keymap → 配置按钮；检查完整主界面；Done 返回；再次打开按 Esc；连续三次。
- 26.2 已确认预期：Mod Menu factory 创建与 Controls 入口相同的 `VisualKeymapScreen`。26.3 工厂/onClose 已程序化通过，真实列表点击未测。
- 26.3 应达到：配置按钮可见，打开完整界面，父屏正确，无空白/错误引用。
- 证据：Mod 列表、主屏、返回后的截图和录屏，日志。
- PASS：真实点击与物理 Esc/Done 都通过；仅工厂探针不够。

### MR-11 MaLiLib GUI、搜索、排序与冲突

- 对应：M-01、M-04、L-05、S-08 MaLiLib。
- 前置：P5，Tweakeroo 注册热键；至少准备单键、组合、鼠标、有/无 comment 的条目。
- 操作：检查 category/mod name、display name、comment tooltip；搜索内部 hotkey 名；切换排序/冲突置顶；与 Minecraft/CommandKeys 相同键制造冲突。
- 26.2 已确认预期：provider=`malilib`，ID=`IHotkey.getName()`，显示名/comment 和内部名搜索规则来自原 adapter。真实 GUI 动态证据不足。
- 26.3 应达到：217+ Tweakeroo 条目可枚举，名称、tooltip、搜索、顺序和跨 provider 冲突正确。
- 证据：代表条目和 tooltip、搜索、冲突/排序截图。
- PASS：可见 GUI 与原规则一致，不能用 217 条模型枚举替代。

### MR-12 MaLiLib 多键编辑、鼠标、重置与实际触发

- 对应：E-04、E-06、M-02、M-03。
- 前置：本地测试世界；选择无副作用的 Tweakeroo 热键。
- 操作：分别录入 1 键、按住 2–4 键、首键提前释放、键鼠组合；清空、Reset；离开并重启；实际触发 Free Camera/Hold Attack 或等价无害功能。
- 26.2 已确认预期：最大 4 键，按键顺序保留；saveAllConfigs/updateUsedKeys 后立即生效。26.3 adapter 已验证 `[J,F9,Mouse Left,Mouse Right]` 保存重启，但 GUI 状态机/触发未测。
- 26.3 应达到：录入结束时机、顺序、按钮状态、实际触发和重启保持一致；其他负值/gamepad 未编辑部分不被覆盖。
- 证据：按住/释放录屏、`tweakeroo.json` 前后副本、重启后触发录屏。
- PASS：配置落盘和实际触发都通过。

### MR-13 CommandKeys profile、名称/comment 与搜索

- 对应：K-01～K-03。
- 前置：至少两个 profile；空/非空宏名；首条空白+第二条非空；多消息；单键/双键/未绑定宏。
- 操作：每次切换活动 profile 后重新打开 Visual Keymap；检查只显示当前 profile；检查 `...`、`[+N]`、宏名；用任意消息文本搜索。
- 26.2 已确认预期：原 adapter 读取当前 profile 的 `macroMap`，名称和 comment 规则不变；26.3 模型探针通过，GUI 未测。
- 26.3 应达到：profile 隔离、名称、tooltip、消息搜索高亮和顺序正确。
- 证据：两个 profile 的截图、tooltip、搜索前后截图、配置副本。
- PASS：真实 GUI 结果与规则一致。

### MR-14 CommandKeys 编辑、保存、重启与实际宏触发

- 对应：E-05、E-06、K-04、K-05。
- 前置：本地测试世界；宏只发送无害本地测试文本，不连接真实服务器，不执行有副作用命令。
- 操作：录入单键和 limit+主键；检查 limit 在主键前；清空/重置；保存并完全重启；实际触发；导出快照。
- 26.2 已确认预期：1 键写主 key，2 键写 limit+主；Reset 清空；`Config.save()`；provider 为空所以不导出。26.3 配置模型和重启探针通过，GUI/触发未测。
- 26.3 应达到：GUI 捕获顺序、实际宏触发和重启保持；version 1 不含 CommandKeys。
- 证据：编辑/触发录屏、`commandkeys.json`、导出 JSON、日志。
- PASS：实际宏触发和配置持久化均正确；仅模型 `[341,290]` 不够。

### MR-15 多个未绑定 CommandKeys 宏对照

- 对应：C2 遗留风险。
- 前置：26.2 与 26.3 各建立多个完全未绑定宏。
- 操作：分别打开 Visual Keymap，记录枚举数量、名称、编辑其中一个后其他条目是否变化。
- 26.2 已确认预期：尚无动态证据；底层 `macroMap` 以 `Keybind.equals()` 为 key，可能共享代表 key 对象。
- 26.3 应达到：与 26.2 完全一致，不把潜在历史行为在迁移中修复。
- 证据：两版成对录屏、profile 配置、条目数量记录。
- PASS：两版一致；无 26.2 证据时标 BLOCKED/NOT TESTED。

### MR-16 全 provider 共存与联合保存

- 对应：P5、API-04、S-08。
- 前置：Minecraft、MaLiLib、CommandKeys 各有代表绑定。
- 操作：检查默认顺序 Minecraft→MaLiLib→CommandKeys；制造跨 provider 冲突；分别编辑并离开；完全重启；导出。
- 26.2 已确认预期：条件加载顺序和逐 provider save 枚举由 `VisualKeymap` 决定；CommandKeys 不导出。
- 26.3 应达到：顺序、冲突、即时刷新、各配置持久化和快照 provider 保持。
- 证据：排序/冲突截图、三类配置、导出 JSON、重启录屏。
- PASS：三类 provider 都保存且没有互相覆盖。

## 5. 会话 C：P6 外部 Provider

### MR-20 外部 Provider 完整 GUI 契约

- 对应：API-02、API-03、P6。
- 前置：P6；确认 `phase4-production-smoke.txt` 为 PASS。
- 操作：检查 `Phase 4 - External Binding`、modifier、tooltip/order；搜索；制造/解除冲突；编辑为 A/B；Reset；离开触发 save；导出、导入预览、确认并重启。
- 26.2 已确认预期：同类外部消费方在原始 26.2 可编译/加载；失败 entrypoint 被隔离；provider 位于 Minecraft 后。C3 模型层已对 26.2/26.3 通过，GUI 未测。
- 26.3 应达到：有效 Provider 继续工作，失败 Provider 不出现在列表；GUI 和 version 1 使用 provider `phase4-external`、稳定 ID、规范 keys 和 modifiers。
- 证据：Provider 行/tooltip/搜索/冲突截图，导出 JSON，保存回调对应日志/证据文件，重启录屏。
- PASS：GUI、回调和快照三条路径均通过；只看到 provider 类名不够。

## 6. 设备条件专项

### MR-17 F25 tombstone 与 26.3-only 键

- 对应：S-06、S-07。
- 前置：有 F25 或可产生对应 SDL 事件的硬件；至少一个 26.3-only 键。无设备时不要使用裸 SDL 整数伪造公共数据。
- 操作：导入 F25=314 快照，检查不可应用提示；尝试物理 F25；编辑/导出一个 26.3-only 键并重启再导出。
- 26.2 已确认预期：314 是旧 F25 数据；26.2 无法识别新增扩展值但应保留数据。26.3 合成预览已验证 F25/未知 extension 不应用、不覆盖。
- 26.3 应达到：F25 数据不映射其他键；扩展键使用 `0x01000000..0x0100FFFF` 固定 ID，26.3 内完整往返；原始 SDL 数字不出现在 JSON。
- 证据：硬件型号、按键录屏、导入/导出 JSON、哈希、日志。
- PASS：物理和数据语义都正确；缺硬件时物理部分标 BLOCKED，但不可应用/数据保护可单独 PASS。

## 7. 最终记录模板

每个编号按以下格式记录：

```text
测试编号：MR-xx
实例/P 组合：
Minecraft / Loader / Visual Keymap / 可选模组版本：
执行时间：
状态：PASS / FAIL / NOT TESTED / BLOCKED / STATICALLY VERIFIED
实际步骤：
实际结果：
26.2 对照证据：
26.3 证据路径：
日志关键行：
若 FAIL，最小复现：
```

最低发布前人工门槛：MR-01、MR-02、MR-03、MR-04、MR-05（设备可用键）、MR-06、MR-10～MR-16、MR-20 全部 PASS；MR-07 必须完成 26.2→26.3 对照；MR-17 可因硬件 BLOCKED，但必须保留已通过的数据安全策略。任何生产逻辑修复都需先单独报告和批准。
