# Visual Keymap 26.3 功能回归测试计划

## 1. 目标与范围

本计划用于证明 26.3 迁移保持上游 `main@053e736`（Minecraft 26.2）的功能、用户操作方式、数据格式和扩展接口。它不把修复既有问题、功能增强或重构纳入通过条件。

标记：

- **A**：适合自动化（单元、消费方编译、JSON/数据模型测试或启动测试）。
- **M**：需要人工游戏内验证。
- **26.2→26.3**：必须在干净 26.2 基线实例先记录结果，再对 26.3 做相同操作；若现有行为看起来异常，仍按对照结果判定，不擅自修复。

## 2. 现有自动化覆盖

- Phase 3 B2.5 已增加 JUnit 5 测试入口 `CanonicalInputCodeTest`，调用既有 `CanonicalInputCodeSelfTest`；`clean test` 与 `clean build` 会实际执行规范键码表、碰撞和往返检查。
- 独立脚本 `tools/porting-26.3/Test-CanonicalInputCode.ps1` 继续提供不依赖 JUnit 的同范围自检；两条路径当前都报告 121 legacy、127 extension、247 keyboard、8 mouse 通过，扩展表 SHA-256 为 `0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28`。
- CI 仍是 `.github/workflows/release.yml`：tag push 后用 JDK 25 执行 `./gradlew build` 并发布。当前 JUnit 测试已进入该 `build` 生命周期。
- GUI、Mixin 注入、第三方 adapter、配置、SDL 对话框和实际导入导出仍主要依赖人工测试；不得因规范键码单元测试通过而判定这些功能全部通过。

## 3. 测试矩阵与准备

### 3.1 实例组合

| 编号 | 安装组合 | 目的 |
| --- | --- | --- |
| P1 | Minecraft + Fabric Loader/API + Visual Keymap | 验证无可选依赖时启动、原版按键和快捷键。 |
| P2 | P1 + Mod Menu | 验证 Mod Menu entrypoint 和返回链。 |
| P3 | P1 + MaLiLib + 至少一个注册 MaLiLib 热键的模组 | 验证 MaLiLib 读取、写入、保存。 |
| P4 | P1 + CommandKeys，并准备多 profile/宏 | 验证 CommandKeys 当前 profile 集成。 |
| P5 | P1 + MaLiLib + CommandKeys + Mod Menu | 验证 provider 聚合顺序和共同保存。 |
| P6 | P1 + 外部测试模组，声明 `visualkeymap` entrypoint | 验证公开扩展 API、失败隔离和快照 provider。 |

P1、P2、P3、P4、P5 均需建立 26.2 参照与 26.3 目标实例，使用独立 config/game 目录。P6 首先做 26.2 消费方编译；若当前公开签名无法支持 README 示例，应把结果记录为既有基线事实，不在迁移提交中顺带修改。

### 3.2 数据夹具

- 原版：默认绑定、改动绑定、未绑定、两个相同键、debug key + debug modifier。
- MaLiLib：单键、2–4 键组合、鼠标键、带/不带 comment、内部名与显示名不同的热键。
- CommandKeys：多个 profile；空宏名与非空宏名；一条/多条/首条空白消息；无绑定、单键、limit+主键。
- 外部 provider：固定 provider 名和稳定 ID；0–4 键；modifier；自定义 order；会抛异常的第二个 entrypoint。
- 快照：有效版本 1、未知 provider、未知 ID、空 keys、鼠标码、仅 modifiers 不同、未知 version、null 字段、无效 JSON。

### 3.3 环境维度

- 语言：`en_us`、`zh_cn`。
- GUI 缩放：至少小/中/大三档；窗口化最小可用尺寸与全屏。
- 输入：标准键盘、数字小键盘、鼠标左/中/右；有条件时测 M4/M5。
- 文件系统：正常可写；另用明确的只读测试目录验证错误行为，不触碰真实配置。

## 4. 构建、加载与 Mixin

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| B-01 | A | 使用目标 JDK 执行 `./gradlew clean build`。 | 编译、remap、sources jar 和打包成功；无意外缺类。 |
| B-02 | A/M | P1 启动客户端，检查日志。 | 模组被识别为 client-only；三个 required client mixin 均成功应用；无 MaLiLib/CommandKeys/Mod Menu 缺类错误。 |
| B-03 | M | 分别启动 P2–P5。 | 所有组合均进入标题界面，无 entrypoint 或链接错误。 |
| B-04 | A | 检查成品 `fabric.mod.json`。 | 版本属性正确展开；mod id、入口点、Mixin 配置、资源路径保持兼容。 |
| B-05 | A | 对 `ControlsScreenMixin` 做 Mixin 审计/启动注入检查。 | `addOptions` redirect 命中恰好所需调用；原控制选项未丢失或重复。 |

## 5. 入口与导航

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| N-01 | M, 26.2→26.3 | 进入“选项 → 控制”。 | 出现本地化“Visual Keymap...”按钮；原有控制选项仍完整。 |
| N-02 | M | 从控制页打开主界面，再点击完成/按 Esc 返回。 | 父屏幕与 26.2 相同，屏幕栈无循环或空白。 |
| N-03 | M | P2 从 Mod Menu 配置按钮打开并返回。 | 打开的是完整主界面，返回 Mod Menu。 |
| N-04 | M | 在原版控制页找到本模组“打开 Visual Keymap”按键。 | 类别和名称本地化正确，默认未绑定。 |
| N-05 | M | 给该按键绑定键，在游戏内触发；再在已有屏幕上触发。 | 每次消费点击后打开主界面；返回目标与 26.2 一致。 |

## 6. 布局、可视化与本地化

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| V-01 | M | 在两种语言和多种 GUI 缩放打开主界面。 | 标题、搜索、键盘、列表、四个底部按钮无关键遮挡；上下内容比例与 26.2 一致。 |
| V-02 | M | 对照 `KeyLayoutHelper` 检查主键区、功能键、导航键、方向键和鼠标键。 | 键位位置、宽高倍数、标签与点击区域正确。 |
| V-03 | M | 切换“显示数字小键盘”，返回主界面。 | 完整数字小键盘按配置出现/消失，主布局重新缩放。 |
| V-04 | M | 准备未绑定、单绑定、多绑定无冲突、冲突键。 | 键帽依次为深灰、深绿、暗金、暗红；选中白边、搜索命中黄边。 |
| V-05 | M | 悬停含 0、1、5、6+ 绑定的键帽。 | 键名、冲突红字、普通绑定和“更多”数量与 26.2 一致。 |
| V-06 | M | 检查所有专用翻译键与 fallback 键。 | 英文/中文资源生效；没有专用翻译时使用 Minecraft 键名。 |

## 7. 键选择、列表、搜索、排序与冲突

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| L-01 | M | 初次打开主界面。 | 下方列表只显示主键列表为空的绑定。 |
| L-02 | M | 点击一个已绑定键，再次点击同键，再点击另一键。 | 依次显示该键相关绑定、回到未绑定列表、切换到另一键；选中边框同步。 |
| L-03 | M | 检查列表行名称、tooltip、按键文本、重置状态。 | 显示“类别 - 名称”；comment 形式、组合键格式、默认状态与 provider 规则一致。 |
| L-04 | M/A | 输入类别、名称、键名、多个词、大小写混合和多余空格。 | 匹配项置顶且黄色粗体，非匹配项不被过滤；所有词均需命中。相关键帽黄边。 |
| L-05 | M | 搜索 MaLiLib 内部 hotkey 名和 CommandKeys 消息文本。 | 对应绑定命中，即使显示名不含搜索词。 |
| L-06 | M/A | 切换 `DEFAULT`、`BOUND_KEY`、`DISPLAY_NAME`。 | 默认保持 provider 来源顺序；按键按主键后 modifier 的整数列表排序；名称忽略大小写排序。 |
| L-07 | M/A | 组合搜索、冲突置顶开/关和排序模式。 | 优先级严格为搜索 → 可选冲突 → 选定排序模式。 |
| L-08 | A/M | 构造相同 full key + 相同 order、相同 key + 不同 order、键顺序不同、空绑定。 | 只有第一组产生冲突；空绑定行不标冲突。冲突按钮金色且 tooltip 列出其他成员。 |

## 8. 键位修改与保存

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| E-01 | M | 编辑原版绑定并按一个键。 | 最大 1 键，按下即结束；`KeyMapping.resetMapping()` 后实际游戏动作使用新键。 |
| E-02 | M, 26.2→26.3 | 编辑中按 Esc。 | 目标绑定被清空并退出编辑，行为与 26.2 一致。 |
| E-03 | M | 编辑中点击左/中/右/M4/M5。 | 记录对应鼠标键并立即结束，不误触其他界面操作。 |
| E-04 | M | MaLiLib 依次测试 1 键、按住输入 2–4 键、首键提前释放。 | 最多 4 键，顺序和结束时机与 26.2 一致；实际热键触发。 |
| E-05 | M | CommandKeys 输入 1 键及按住输入 2 键。 | 1 键写主 key；2 键写 limit + 主 key；宏触发与 26.2 一致。 |
| E-06 | M | 修改后三类 provider，使用每行重置。 | 原版/MaLiLib 恢复各自默认；CommandKeys 清空；默认时按钮禁用。 |
| E-07 | M, 26.2→26.3 | 分别通过完成、Esc、打开其他屏幕离开主界面，重启。 | 每个 provider 的保存时机和持久化结果与 26.2 对照一致；确认各路径都进入 `VisualKeymapScreen.removed()`，并由父类 `OptionsSubScreen.removed()` 将原版键位写入 options。 |
| E-08 | M | 修改绑定后观察键帽和当前列表。 | 键帽颜色、冲突与行按钮即时刷新，表现与 26.2 一致。 |

## 9. 本模组配置

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| C-01 | M/A | 清空独立测试实例 config 后首次启动。 | 创建 `config/visualkeymap.json`，三项默认值为 `true`、`true`、`default`。 |
| C-02 | M/A | 修改三项设置，离开配置页并重启。 | JSON 写入，重启恢复；界面布局/排序按配置生效。 |
| C-03 | M | 从主界面进入配置再返回。 | 返回重建后的主界面，配置立即反映。 |
| C-04 | A/M, 26.2→26.3 | 使用空文件、`null`、无效 JSON、缺字段、未知字段。 | 逐例记录 26.2 结果；26.3 保持相同，除非迁移所需变更经单独批准。 |
| C-05 | M, 26.2→26.3 | 在明确的只读测试 config 目录运行。 | 日志和用户可见结果与 26.2 一致，不影响真实用户目录。 |

## 10. 导出与导入

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| X-01 | M | 点击导出，检查默认目录、过滤器、取消。 | 默认名 `visualkeymap-export.json`，JSON 过滤器生效；取消不创建文件。 |
| X-02 | A/M | 修改原版、MaLiLib、CommandKeys 和外部 provider 后导出。 | `version = 1`；只含非默认且 provider 非空条目；provider/id/keys/modifiers 准确。CommandKeys 当前不出现。 |
| X-03 | A | 对相同绑定重复导出并解析 JSON。 | 结构可解析，字段名与版本稳定；条目顺序与 provider/绑定枚举顺序一致。 |
| X-04 | M | 导入完全相同的快照。 | 不打开预览，也不修改绑定。 |
| X-05 | M/A | 导入含未知 provider、未知 ID 和部分可匹配条目的快照。 | 只为 provider/id 精确匹配且 `keys` 不同的项目打开预览。 |
| X-06 | M | 在预览中检查标题、名称、旧→新键、默认勾选。 | 数量、文本、颜色、tooltip 与 26.2 一致。 |
| X-07 | M | 分别确认全选、取消部分、取消全部；另测取消按钮和 Esc。 | 只应用确认时勾选项；取消路径不修改；确认后保存、刷新并返回主界面。 |
| X-08 | A/M, 26.2→26.3 | 导入仅 `modifiers` 不同的快照。 | 不因 modifier 差异进入预览，且 modifier 不被应用；保持当前源码行为。 |
| X-09 | A/M | 导入键盘码、0–7 鼠标码、空 keys。 | 转换和清除行为与 26.2 一致；实际触发正确。 |
| X-10 | A/M, 26.2→26.3 | 导入未知 version、null 字段、空 bindings、无效 JSON、不可读文件。 | 记录 26.2 的实际结果，26.3 不得未经批准改变。特别记录是否崩溃、停留原屏或仅写日志。 |
| X-11 | M, 26.2→26.3 | 导出到不可写位置。 | 用户可见反馈与 26.2 一致；不破坏已有文件。 |

## 11. 第三方集成专项

### 11.1 MaLiLib

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| M-01 | M | P3 打开界面，枚举多个 category/hotkey。 | 类别使用 mod name，名称使用 config GUI display name，comment tooltip 正确。 |
| M-02 | M/A | 检查内部键码与 UI 规范键码往返，覆盖 `-99..-92` 鼠标码、普通键盘码以及 MaLiLib 0.30.2 的其他负值（含 gamepad 夹具）。 | 只有 `-99..-92` 按 MaLiLib 鼠标约定与 Visual Keymap 的 `0..7` 往返；其他负值不得被旧版“所有 `< -1` 都加/减 100”的规则误改。未编辑的 gamepad 绑定不得被覆盖；其编辑行为在确认策略前标为待人工验证。 |
| M-03 | M | 修改后离开主屏幕并重启。 | 调用保存后配置落盘，`updateUsedKeys()` 后新热键立即可触发。 |
| M-04 | M | 修改、重置、搜索内部 hotkey 名。 | `isModified`、默认恢复和附加搜索文本行为正确。 |

### 11.2 CommandKeys

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| K-01 | M | P4 在不同活动 profile 下分别重新打开主界面。 | 每次只显示当前 profile 的 macroMap。 |
| K-02 | M | 检查空/非空宏名、首消息空白、多消息。 | 名称与 comment 分别显示 profile、可选宏名、`...`、`[+N]`。 |
| K-03 | M | 用消息文本搜索宏。 | 任意消息文本可命中。 |
| K-04 | M | 修改单键及 limit+主键，保存并重启。 | `setKey/setLimitKey` 语义与 26.2 一致，`Config.save()` 落盘。 |
| K-05 | A/M | 导出包含 CommandKeys 改动的实例。 | 当前快照不含 CommandKeys 条目，因为 provider 为空。 |

### 11.3 Mod Menu

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| MM-01 | M | P2 从 Mod Menu 打开。 | 工厂创建完整 Visual Keymap 主屏幕。 |
| MM-02 | M | 完成/取消返回。 | 回到 Mod Menu 传入的父屏幕。 |
| MM-03 | M | P1 无 Mod Menu 启动。 | 不出现与 `com.terraformersmc.modmenu` 相关的类加载失败。 |

## 12. 对外 API 与消费方兼容

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| API-01 | A, 26.2→26.3 | 在独立测试模组中按 README 声明 `visualkeymap` entrypoint，实现 `VisualKeymapApi` 和自定义 `KeyBinding`。 | 先记录 26.2 是否能够编译；若能，26.3 必须继续编译和加载。若不能，结果作为既有签名事实，不在迁移中修复。 |
| API-02 | M/A | 返回非空 provider、稳定 ID、键、modifier、comment、order。 | GUI 显示、搜索、冲突、修改、重置、保存、导入导出均调用预期方法。 |
| API-03 | A/M | 注册两个实现，其中一个 entrypoint 构造时抛异常。 | 异常实现被忽略，其他 provider 继续工作；行为与 26.2 一致。 |
| API-04 | A | 检查 provider 顺序。 | Minecraft 第一，外部 entrypoints 随后，MaLiLib、CommandKeys最后；默认排序反映该顺序。 |
| API-05 | A | 对公开 `VisualKeymap` 静态方法做二进制/源码签名比较。 | 现有方法名、参数、返回类型未被迁移无故改变。 |

## 13. 当前未覆盖功能与风险

由于现有仓库没有测试，下列全部是当前未自动覆盖面：

- required Mixin 的注入目标与 accessor 字段。
- 26.2 GUI 渲染提取和布局 API 的全部使用。
- 按键捕获状态机，尤其多键必须在释放前完成的交互。
- 原版 debug modifier、`KeyMapping.order` 参与冲突的语义。
- 三个可选模组存在/缺席时的类加载与 API 链接。
- 第三方 provider entrypoint 的真实可实现性。
- 配置读写、错误路径和默认值。
- 快照 schema、provider/ID 稳定性、鼠标码与 modifiers 现状。
- 本地化、GUI 缩放、辅助 narration 与文件对话框的平台差异。

## 14. Minecraft 26.3 SDL 与规范键码专项

本节把 `MIGRATION-PLAN.md` 中的输入兼容要求转成可独立验收的用例。自动化测试框架尚不存在；在引入测试依赖前需把相关构建修改作为独立提交审查。无法自动化的项目必须保留为人工测试，不得用代码阅读代替。

| ID | 类型 | 步骤 | 通过条件 |
| --- | --- | --- | --- |
| S-01 | A | 对 26.2 与 26.3 共同键按稳定 `InputConstants.Key#getName()` 建立双向映射，覆盖 unknown、字母、数字、F1–F12、导航键、modifier 和数字小键盘。 | 26.3 原生 SDL 值能转为 26.2 规范整数并无损转回；unknown 保持既有未绑定语义。 |
| S-02 | A | 枚举键盘 A–E 与 mouse 4–8。 | 每个键盘规范码均与对应数值的鼠标规范码不同；不存在因 SDL `4..8` 引发的类型碰撞。 |
| S-03 | A/M | 覆盖左、中、右、M4–M8 的读、显示、编辑、冲突、筛选、导入、导出与实际触发。 | 每个按钮逐项映射正确；不得使用会交换右键/中键的统一加减偏移。 |
| S-04 | A | 扫描业务源码中 `InputConstants.Key#getValue()`、`Type.KEYBOARD.getOrCreate()` 和鼠标构造调用。 | 原生数值只在集中式兼容层边界出现；GUI、冲突、排序、快照和第三方 adapter 不直接混用 SDL 数字与 26.2 规范整数。 |
| S-05 | A/M, 26.2→26.3 | 使用真实 26.2 version 1 golden file，覆盖键盘、0–7 鼠标、unknown/空绑定和 modifiers。 | 26.3 导入后的绑定与 26.2 一致；再次导出保持 version 1 字段和既有整数语义。 |
| S-06 | A/M | 对 `key.keyboard.f25` 的规范码 `314` 做读取、显示、导入和再次导出；物理触发单独测试。 | 数据不得静默替换或丢失；若 SDL 后端无法产生物理事件，明确记录为上游输入能力限制并等待维护者决策。 |
| S-07 | A/M | 按维护者批准的扩展编码策略测试至少一个 26.3-only 键。 | 不与任何既有键盘/鼠标规范码碰撞；显示、编辑、冲突和 26.3 内往返符合已批准策略。批准前本用例为阻塞门槛，不得用裸 SDL 值临时代替。 |
| S-08 | A/M | 分别通过 Minecraft、MaLiLib、CommandKeys 和外部 provider 读取、修改并保存同类键。 | 所有 adapter 在边界完成转换；provider、ID、顺序、默认值和保存时机保持 26.2 行为。 |

迁移完成的最低通过标准是：B、N、V、L、E、C、X、S 各节全部必需用例通过，P1–P5 均能启动并完成相应专项；API-01 的 26.2 结果被明确记录并在 26.3 保持，不以“顺手修复”替代兼容验证。S-06 的物理触发或 S-07 的新增键策略如受上游能力限制，必须有可复现证据和维护者明确决策，不能以静默丢失通过验收。

## 15. Phase 3 B2.5 真实混合快照执行状态

2026-10-31 收到未修改的 `baseline-private/26.2/version 1.json` 和 `options.txt`。原始 JSON 同时包含 Minecraft 与 MaLiLib provider；原件及个人配置只保存在工作区私有目录，不进入 Git。为隔离未迁移的 C1 adapter，动态测试使用按 provider 过滤出的 Minecraft-only ignored 派生副本。完整哈希和步骤见 `BASELINE-VALIDATION.md` 第 8 节与 `PHASE3-B2.5-REPORT.md` 第 5 节。

状态含义沿用基线报告：`PASS`、`FAIL`、`NOT TESTED`、`BLOCKED`、`STATICALLY VERIFIED`。一个用例只有部分覆盖时，按子项分别列出，不把整行升级为完整 PASS。

| ID/子项 | 状态 | 证据与剩余边界 |
| --- | --- | --- |
| E-01 Minecraft 普通键读写 | **PASS（F8）** | 真实 26.2 记录 `297` 导入后，26.3 原版 Key Binds 立即显示 F8；实际游戏动作触发仍未单独测试。 |
| E-07 保存和重启 | **PASS（Minecraft F8）** | 导入后隔离 `options.txt` 为 `key.keyboard.f8`；完全退出并重启后仍显示 F8。 |
| X-02/X-03 version 1 导出 | **PASS（Minecraft F8）** | 重启后再导出仍为 version 1，provider/ID/297/空 modifiers 与原件语义一致。 |
| X-05 provider/ID 匹配 | **PASS（Minecraft）/ STATICALLY VERIFIED（MaLiLib）** | Minecraft 记录被唯一匹配；MaLiLib 记录从派生文件中排除，等待 C1。 |
| X-06 预览 | **PASS（Minecraft）** | 预览显示 1 个变化和 `Not Bound → F8`，默认勾选。 |
| X-07 确认应用 | **PASS（Minecraft 全选主路径）** | `Import Selected` 后立即应用并持久化；部分选择、全部取消和 Esc 仍未测试。 |
| X-09 键盘码 | **PASS（F8=297）** | 真实跨版本键盘样本通过；该原件没有鼠标 0–7 或空 keys。 |
| S-05 Minecraft F8 | **PASS** | 未修改原件 → 明确标记派生副本 → 26.3 导入/应用/重启/再导出，规范语义完全一致。 |
| S-05 MaLiLib live | **BLOCKED** | 原件含 J=74、F9=298，但 C1 尚未迁移；本阶段没有调用 adapter 写入路径。 |
| S-05 mouse 0–7、unknown/空绑定、非空 modifiers | **NOT TESTED** | 新原件不含这些类别；此前合成/26.3 样本不能冒充真实 26.2 数据。 |

结论：B2.5 中“缺少真实 26.2 Minecraft provider 快照”的单项遗留问题已关闭；S-05 整体仍需上述 MaLiLib 和数据类别子项后才能判为完整 PASS。
