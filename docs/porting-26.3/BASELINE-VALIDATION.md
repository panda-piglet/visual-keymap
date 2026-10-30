# Minecraft 26.2 真实行为基线验收

> 2026-10-31 补充：第 1–7 节记录首批 `baseline-26.2/` 材料；第 8 节记录后来提供的私有混合 provider 原件，并取代前文“没有真实 Minecraft provider 快照”的相关结论。私有原件没有复制到仓库。

## 1. 结论与证据边界

本次验收检查了工作区 `baseline-26.2/` 中的全部材料，并逐帧抽查两段录屏。材料直接证明了 Visual Keymap 1.4.0 在 Minecraft 26.2 中的主界面、控制页入口、MaLiLib 单键编辑、导出、导入预览和确认应用主路径。没有发现与 Phase 1 功能清单冲突的实际行为，也没有发现已完成 B1 代码与真实 J/F9 编码不一致。

本次不能证明“26.2 全功能通过”。`TEST-RESULTS.md` 是 0 字节空文件；Visual Keymap、MaLiLib 和 CommandKeys 的实际配置文件未提供；录屏没有覆盖重启持久化、鼠标编辑、多键组合、取消/部分导入、Mod Menu 入口、外部 entrypoint 和错误路径。下文对这些项目保持 `NOT TESTED`，不以文件或截图存在代替执行结果。

状态含义：

- **PASS**：完整步骤已执行，并有录屏、真实文件或既有运行日志直接支持。
- **FAIL**：已执行且实际结果不符合预期。
- **NOT TESTED**：未执行完整步骤；即使存在部分画面也不升级为 PASS。
- **BLOCKED**：因环境或依赖无法执行。
- **STATICALLY VERIFIED**：仅由源码、构建元数据或文件结构确认，未获得对应真实运行行为。

本次矩阵中没有 `FAIL` 或 `BLOCKED`；这表示尚未观察到失败或环境阻碍，不表示未测项目通过。

## 2. 版本与材料清单

### 2.1 版本

| 项目 | 结论 | 证据与限制 |
| --- | --- | --- |
| Minecraft | **26.2，PASS** | 两段录屏的窗口标题显示 `Minecraft* 26.2`。 |
| Visual Keymap | **1.4.0，PASS** | `screenshots/00_ALLmods.png` 的 Mod Menu 详情显示 Visual Keymap 1.4.0。 |
| Fabric Loader/API | **0.19.3 / 0.152.2+26.2，STATICALLY VERIFIED** | 精确值来自基线提交 `053e736` 的 `gradle.properties`；截图没有显示运行时精确版本。 |
| MaLiLib | **0.29.0，STATICALLY VERIFIED** | 基线依赖与 Phase 2.6 启动日志使用 0.29.0；截图只直接证明已安装且其绑定被枚举。 |
| CommandKeys | **3.103.0+26.2，STATICALLY VERIFIED** | 基线依赖给出精确版本；截图/tooltip 直接证明模组存在，但未显示精确版本。 |
| Mod Menu | **20.0.0-beta.3，STATICALLY VERIFIED** | 基线依赖给出精确版本；截图直接证明 Mod Menu 存在。 |
| Tweakeroo | **已安装，精确版本待确认** | 截图、录屏和真实快照直接证明其 MaLiLib hotkey 被 Visual Keymap 枚举；材料未显示版本号。 |
| 语言与 GUI 缩放 | **zh_cn / 4，文件证据** | `configs/options.txt` 含 `lang:zh_cn`、`guiScale:4`、`version:4903`。 |

### 2.2 文件清单、哈希与处理决定

以下路径均相对于未纳入 Git 的本地证据根 `baseline-26.2/`。

| 文件 | 字节 | SHA-256 | 可直接证明的内容 | 处理决定 |
| --- | ---: | --- | --- | --- |
| `TEST-RESULTS.md` | 0 | `E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855` | 无；文件为空 | 不作为证据、不提交 |
| `configs/options.txt` | 5,365 | `EAC94636408CEEA88149851183ED457F34597027F38780E721CD87ED6C7C885B` | 语言、缩放、原版及模组 KeyMapping 的持久化文本 | 属于完整个人实例配置；仅本地引用 |
| `configs/test_config.json` | 301 | `C883934384A7676649C4222D34C4381472DF5D89F5E34D8455A0ED14D116500F` | 原版导出的 version 1 快照 | 原件保持不变；无个人字段，可建立语义测试副本 |
| `screenshots/00_ALLmods.png` | 2,402,335 | `B514F4F3E44244F380D9AACF1136DEC9B60F073E4CF6278938576FF4EDD5CE08` | 模组组合及 Visual Keymap 版本 | 未见账号/服务器/聊天，但未经额外发布批准仅本地引用 |
| `screenshots/01_main.png` | 1,761,784 | `604A79034B459BAEC87CF6DE6335F3EE876017D4C44D9264DF81C67EB89E021A` | 中文主界面、键盘/鼠标布局、四个底部按钮 | 仅本地引用 |
| `screenshots/02_search.png` | 1,751,673 | `ADAB0522623724B741BB72319B11DB96BE15CAEE45217CDA5D5A4406F41669B3` | 搜索“地形雾”命中 Tweakeroo 项 | 仅本地引用 |
| `screenshots/03-tooltip.png.png` | 1,715,202 | `88972D971C618EA8559874DF94CCB4ECB8623FF56F171EE32BBB1CC0D9B81441` | C 键跨 Minecraft/MaLiLib/Tweakeroo 的 tooltip 与冲突可视化 | 仅本地引用 |
| `screenshots/04-edit.png.png` | 1,880,957 | `843CFB386CCCD4817DAC8F88E8B4905E0338B1A9EE3989C5CE27AC0C2ED45D29` | 左 Alt 选中/编辑状态与列表 | 仅本地引用 |
| `videos/change_key.mp4` | 90,538,042 | `2671EC901F80169ED26C59E42A830CC9EF9149F36EC89D72871659B69EDA3DDF` | 编辑 J/F9、即时刷新、导出和返回控制页 | 文件对话框暴露本地实例路径；不得提交 |
| `videos/import_config.mp4` | 40,520,331 | `985A587AC805A88B9A9C3AA5F9BDEB745B3DC7C64AE5832CEB1DAC537E2A8B69` | 清空两项、导入、预览和确认恢复 | 文件对话框暴露本地路径；不得提交 |

未提供：`config/visualkeymap.json`、MaLiLib/Tweakeroo 的实际配置、CommandKeys 配置、客户端日志，以及非空人工测试记录。因此不能从本批材料验证三类配置的落盘内容或重启恢复。

## 3. 真实 version 1 快照验证

### 3.1 原件与来源链

权威原件是本地 `baseline-26.2/configs/test_config.json`，SHA-256 为 `C883934384A7676649C4222D34C4381472DF5D89F5E34D8455A0ED14D116500F`。`change_key.mp4` 记录了在原版客户端把默认名 `visualkeymap-export.json` 改为 `test_config.json` 并保存；`import_config.mp4` 随后选择该文件并显示两项预览。因此“由 26.2 客户端导出且可被同一客户端读取”有运行证据，不是只根据文件名推断。

原件未被修改。仓库中的 [`real-26.2-malilib-v1.json`](../../test-fixtures/porting-26.3/snapshots/real-26.2-malilib-v1.json) 是供解析测试使用的语义副本；补有一个末尾 LF，SHA-256 为 `ADB9ED428C08840E22522CE63F951F10BCEC33294BB9793A085A74E2E0E86314`，不得冒充原始字节流。需要字节级溯源时必须使用上述原件哈希。

### 3.2 格式与编码

快照解析结果：

| 字段 | 实际值 | 结论 |
| --- | --- | --- |
| `version` | `1` | 符合 `KeymapSnapshot.fromBindings()` 固定写入的 version 1。 |
| `bindings` | 2 项 | 顶层结构与 `KeymapSnapshot` record 一致。 |
| 第 1 项 | provider `malilib`；ID `tweakFreeCamera`；keys `[74]`；modifiers `[]` | `74` 是 26.2 规范键码 J。 |
| 第 2 项 | provider `malilib`；ID `tweakHoldAttack`；keys `[298]`；modifiers `[]` | `298` 是 26.2 规范键码 F9。 |

`CanonicalInputCode` 的冻结表分别在 `key.keyboard.j → 74` 和 `key.keyboard.f9 → 298` 定义相同值；本次复跑 `tools/porting-26.3/Test-CanonicalInputCode.ps1` 得到：

```text
PASS CanonicalInputCodeSelfTest
legacy=121 extension=127 assignedKeyboard=247 mouse=8
extensionTableSha256=0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28
```

由此可以确认本快照的两个键盘整数与 B1 集中兼容层一致。快照不含鼠标整数，不能据此验证 0–7；不含 extension、F25、unknown/空 keys 或 modifiers，不能替代这些类别的合成夹具或真实行为测试。

### 3.3 容易被迁移破坏的数据与 golden file 资格

- `provider = malilib` 和两个内部 ID 是匹配键；不得改成显示名或本地化字符串。
- 条目顺序是 `tweakFreeCamera` 后 `tweakHoldAttack`，可用于监测 provider/绑定枚举顺序变化。
- keys 是公共规范整数，不是 26.3 SDL 原始整数；B2 导入、GUI、保存和再次导出必须始终保持 74/298。
- `modifiers` 字段虽然为空，仍实际存在；schema 或序列化器不得删除/改名。
- 文件只含两个非默认 MaLiLib 项，符合 `KeymapSnapshot.fromBindings()` 跳过默认绑定和空 provider 的源码规则；不能仅凭此文件断言 Minecraft、CommandKeys 或外部 provider 的完整导出行为。

结论：该文件可以作为 26.3 的真实 golden file，适用范围是“version 1 解析、MaLiLib provider/ID 匹配、J/F9 预览与应用、语义往返”。验收应比较解析后的字段和值，并另行保留原件哈希；它不能承担 `TEST-PLAN` S-05 所要求的鼠标、空绑定和 modifier 全覆盖。

## 4. 已执行场景：步骤、预期与实际

| 场景 | 执行步骤 | 预期 | 实际证据 | 状态 |
| --- | --- | --- | --- | --- |
| RV-01 入口与主界面 | 选项 → 控制 → Visual Keymap | 打开完整中文主界面，能返回控制页 | 两段录屏均经过控制页进入；`01_main.png` 显示完整键盘、鼠标区、搜索、列表以及导出/导入/配置/完成按钮；`change_key.mp4` 末尾返回控制页 | **PASS** |
| RV-02 搜索/tooltip/编辑画面 | 搜索“地形雾”；悬停 C；选择左 Alt 行 | 搜索命中、tooltip 聚合绑定、编辑选择态可见 | `02_search.png`、`03-tooltip.png.png`、`04-edit.png.png` 分别记录上述画面 | **PASS（仅所示样本）** |
| RV-03 MaLiLib 单键编辑与导出 | 搜索 freecamera，设 J；搜索 atta，给持续攻击设 F9；导出为 `test_config.json` | 列表与键帽即时更新；导出 version 1 文件 | `change_key.mp4` 约 39.7 秒显示 J，约 59.0 秒显示 F9，随后文件对话框显示默认名/JSON 过滤器并保存；原件内容与之相符 | **PASS** |
| RV-04 导入主路径 | 在同一客户端清空 J/F9；导入原件；确认 | 预览两项且默认勾选；确认后恢复 J/F9并刷新界面 | `import_config.mp4` 约 13.4–20.2 秒清空，约 26.9–31.5 秒显示“导入预览 - 2 个键位变更”及“未指定 → J/F9”，约 33.6 秒后主界面恢复 | **PASS** |
| RV-05 原版 options 文件 | 保存实例 options 并检查文本 | 文件可解析并含原版/模组 KeyMapping | 文件含标准移动/操作键、debug modifier、CommandKeys 编辑键 K、Mod Menu unknown；未记录 Visual Keymap J/F9，因为它们属于 MaLiLib 配置 | **PASS（文件内容）** |

上述录屏在同一次客户端运行中完成。没有“退出游戏 → 重启 → 再检查/触发”的画面，因此不能把 RV-03/RV-04扩展为持久化或实际热键触发 PASS。

## 5. 与 TEST-PLAN.md 的逐项对应

部分证据在“证据/缺口”列明确列出；只要完整通过条件没有覆盖，状态仍为 `NOT TESTED`。

### 5.1 构建、入口、布局、列表与编辑

| ID | 状态 | 证据或缺口 |
| --- | --- | --- |
| B-01 | **PASS** | Phase 2.6 已保存 26.2 `clean build` 成功日志；本批材料没有新增构建结论。 |
| B-02 | **NOT TESTED** | P5 类组合能进入 GUI，但未提供本次日志，不能审计三个 required mixin 或 P1 无可选依赖组合。 |
| B-03 | **NOT TESTED** | 只观察到全模组组合，未分别启动 P2–P5。 |
| B-04 | **STATICALLY VERIFIED** | 基线产物元数据和 `fabric.mod.json` 已在 Phase 2 审计；不属于本批交互证据。 |
| B-05 | **STATICALLY VERIFIED** | 入口在 26.2 运行中存在，但 redirect 精确命中次数只有源码/既有启动证据。 |
| N-01 | **PASS** | 两段录屏直接显示“选项 → 控制 → Visual Keymap”入口与原控制页。 |
| N-02 | **NOT TESTED** | 观察到完成后返回控制页；未单独验证 Esc 返回分支和所有父屏路径。 |
| N-03 | **NOT TESTED** | 未从 Mod Menu 配置按钮进入。 |
| N-04 | **PASS** | 控制页画面加 `options.txt` 的未绑定状态支持本地化类别/名称及默认 unknown。 |
| N-05 | **NOT TESTED** | 未绑定并触发本模组打开快捷键。 |
| V-01 | **NOT TESTED** | 只有 zh_cn、GUI scale 4、一个窗口尺寸。 |
| V-02 | **NOT TESTED** | `01_main.png` 只证明一个布局样本，未逐键检查标签和点击区域。 |
| V-03 | **NOT TESTED** | 未切换数字小键盘配置。 |
| V-04 | **NOT TESTED** | 截图含多种颜色/边框，但没有按测试数据逐一建立状态与颜色对应。 |
| V-05 | **NOT TESTED** | 有一个冲突 tooltip 样本，未覆盖 0/1/5/6+ 数量。 |
| V-06 | **NOT TESTED** | 只有中文样本，未覆盖全部专用翻译与 fallback。 |
| L-01 | **NOT TESTED** | 初始列表画面存在，但未证明“只含主键为空”的完整集合。 |
| L-02 | **NOT TESTED** | 未执行同键二次点击和另一键切换的完整序列。 |
| L-03 | **NOT TESTED** | 行名称与 tooltip 有样本；组合键、reset 状态和各 provider 规则未覆盖。 |
| L-04 | **NOT TESTED** | 搜索“地形雾”有证据；类别/键名、多词、大小写、空格和非匹配项排序未覆盖。 |
| L-05 | **NOT TESTED** | 未搜索 MaLiLib 内部名或 CommandKeys 消息文本。 |
| L-06 | **NOT TESTED** | 未切换三种排序。 |
| L-07 | **NOT TESTED** | 未组合搜索、冲突置顶与排序。 |
| L-08 | **NOT TESTED** | tooltip 有冲突样本；相同键/order、不同 order、顺序和空绑定矩阵未构造。 |
| E-01 | **NOT TESTED** | 录屏修改的是 MaLiLib，不是 Minecraft 原版绑定，也未实际触发游戏动作。 |
| E-02 | **NOT TESTED** | 未在编辑中按 Esc。 |
| E-03 | **NOT TESTED** | 未编辑任何鼠标按钮。 |
| E-04 | **NOT TESTED** | 只覆盖 MaLiLib 单键 J/F9；2–4 键、释放时机和实际触发未测。 |
| E-05 | **NOT TESTED** | 未编辑 CommandKeys 宏。 |
| E-06 | **NOT TESTED** | 清空 J/F9 不等于逐 provider reset；默认按钮状态未测。 |
| E-07 | **NOT TESTED** | 没有完成/Esc/其他屏幕三路径后重启的对照。 |
| E-08 | **PASS** | `change_key.mp4` 直接显示编辑后行文本和对应 J/F9 键帽立即刷新。 |

### 5.2 配置、导入导出与第三方集成

| ID | 状态 | 证据或缺口 |
| --- | --- | --- |
| C-01 | **STATICALLY VERIFIED** | 默认值和文件路径可由源码确认；本批未提供首次创建出的 Visual Keymap 配置。 |
| C-02 | **NOT TESTED** | 无配置改动、落盘和重启证据。 |
| C-03 | **NOT TESTED** | 主界面有配置按钮，但未进入/返回。 |
| C-04 | **NOT TESTED** | 合成输入存在，不等于已在 26.2 客户端执行。 |
| C-05 | **NOT TESTED** | 未运行只读 config 目录。 |
| X-01 | **NOT TESTED** | 录屏证明默认名、JSON 过滤器和保存；未执行取消及检查“不创建文件”。 |
| X-02 | **NOT TESTED** | 真实导出证明 version 1 和两个非默认 MaLiLib 项；未覆盖 Minecraft、CommandKeys、外部 provider。 |
| X-03 | **STATICALLY VERIFIED** | 原件可解析且字段正确；没有对相同状态重复导出，不能验证稳定顺序/字节。 |
| X-04 | **NOT TESTED** | 导入前先清空了两项，不是导入完全相同快照。 |
| X-05 | **STATICALLY VERIFIED** | `matchBindings()` 和 keys 比较源码已确认；未知 provider/ID 文件未在真实客户端执行。 |
| X-06 | **PASS** | 录屏直接显示标题数量 2、两个本地化名称、`未指定 → J/F9` 和默认勾选。 |
| X-07 | **NOT TESTED** | 只覆盖“全选后确认”；取消部分/全部、取消按钮和 Esc 未测。 |
| X-08 | **STATICALLY VERIFIED** | 源码只以 `keys` 决定预览并只应用 keys；真实 modifier-only 文件未运行。 |
| X-09 | **NOT TESTED** | 只覆盖两个键盘码；0–7 鼠标、空 keys 和实际触发未测。 |
| X-10 | **NOT TESTED** | 未执行任何错误输入。 |
| X-11 | **NOT TESTED** | 未导出到不可写位置。 |
| M-01 | **NOT TESTED** | GUI 显示多个 MaLiLib/Tweakeroo 名称，但未系统检查 category/display name/comment。 |
| M-02 | **NOT TESTED** | 真实文件只覆盖普通键盘码；MaLiLib 鼠标和其他负值/gamepad 未测。 |
| M-03 | **NOT TESTED** | 无 MaLiLib 配置文件、重启或实际触发证据。 |
| M-04 | **NOT TESTED** | 单键修改有证据；reset、`isModified` 和内部 hotkey 名搜索未测。 |
| K-01 | **NOT TESTED** | 未切换 CommandKeys profile。 |
| K-02 | **NOT TESTED** | tooltip 只显示 CommandKeys 的“Edit Active Profile”键，不是宏枚举样本。 |
| K-03 | **NOT TESTED** | 未搜索宏消息。 |
| K-04 | **NOT TESTED** | 未修改或触发宏，无配置文件。 |
| K-05 | **STATICALLY VERIFIED** | provider 为空导致快照跳过的源码规则已确认；未实际修改宏后导出。 |
| MM-01 | **NOT TESTED** | Mod Menu 已安装，但未从其配置按钮打开 Visual Keymap。 |
| MM-02 | **NOT TESTED** | 未验证返回 Mod Menu 父屏。 |
| MM-03 | **NOT TESTED** | 未运行不含 Mod Menu 的 P1。 |

### 5.3 对外 API 与 SDL/规范键码专项

| ID | 状态 | 证据或缺口 |
| --- | --- | --- |
| API-01 | **NOT TESTED** | 未提供独立消费方测试模组。 |
| API-02 | **NOT TESTED** | 未运行外部 provider。 |
| API-03 | **NOT TESTED** | 未运行抛异常 entrypoint。 |
| API-04 | **STATICALLY VERIFIED** | provider 顺序已由 `VisualKeymap.onInitializeClient()` 静态确认。 |
| API-05 | **STATICALLY VERIFIED** | B1 未改公开签名；尚无 26.3 消费方编译结果。 |
| S-01 | **PASS** | 本次复跑 B1 自检通过 121 个 legacy 与 127 个 extension 的 name-based 往返；真实文件另确认 J/F9。 |
| S-02 | **PASS** | B1 自检覆盖 A–E 与 mouse 4–8 的 namespace/type 碰撞。 |
| S-03 | **NOT TESTED** | 没有真实鼠标读、显示、编辑、快照和触发材料。 |
| S-04 | **NOT TESTED** | B2 尚未实施，业务调用点尚未接入集中层，不能验收。 |
| S-05 | **PASS（Minecraft F8 子项）/ NOT TESTED（其余）** | 第 8 节新增真实混合 provider 原件；Minecraft F8 已在 26.3 完成导入、应用、重启和再导出。0–7 鼠标、空 keys/unknown、非空 modifiers 以及 MaLiLib live 往返仍未覆盖。 |
| S-06 | **STATICALLY VERIFIED** | B1 保留 F25 `314` tombstone；无 26.2/26.3 GUI、导入导出或物理事件证据。 |
| S-07 | **NOT TESTED** | 冻结表碰撞/往返已自动通过；显示、编辑、冲突和 26.3 应用链尚未实施。 |
| S-08 | **NOT TESTED** | 四类 provider 的同类键读改存未执行。 |

## 6. 对 B1、B2 的影响

### 6.1 B1

真实快照中的 `74` 和 `298` 与 `CanonicalInputCode` 的 J/F9 映射完全一致；B1 自检再次通过。因此没有发现需要修改 B1 冻结表或编码规则的证据。鼠标、新增键、F25 与 26.2 旧客户端“保留但不可触发”策略仍未获得真实客户端材料，本次不能把这些条件升级为端到端通过。

### 6.2 B2

B2 尚未完成，当前不存在可宣称“已经回归”的 B2 行为；也未发现应在本任务中擅自修复的已完成迁移回归。真实材料把后续最小验收约束具体化为：

1. `KeymapSnapshot.fromBindings()` 必须继续导出 `version = 1`、provider/ID、74/298 和显式空 `modifiers`，不能泄漏 SDL 值。
2. `KeymapSnapshot.ImportMatch#getKeysFromEntry()` 当前仍使用 26.2 的 `Type.KEYSYM.getOrCreate(code)`；B2 必须改为通过集中兼容层解析已支持值，并按已批准规则拒绝把未知 extension/F25 直接应用到 live binding。
3. `VisualKeymapScreen.doImport()` 必须继续按 provider + ID 匹配、只以 keys 差异进入预览；真实基线要求清空后的两项显示为“未指定 → J/F9”。
4. `ImportPreviewScreen` 必须保持默认勾选、确认后保存/刷新/返回主界面的行为；未知值应显示不受支持且不应用，不能影响已知 J/F9 项。
5. 再导出时应保持两个条目的规范整数和 provider 枚举顺序。是否要求字节完全一致应与语义字段测试分开，因为原件无尾换行，而测试副本有尾换行。

建议在 B2 获批后先用真实副本建立“解析 → 匹配 → 预览模型 → 应用 → 再导出 → 语义比较”自动测试，再做 26.3 客户端人工复现。上述建议不改变 schema、API 或既有功能，也不授权在本阶段修改生产代码。

## 7. 剩余人工验证清单

优先补齐以下 26.2 基线，否则对应 26.3 回归只能按源码推断：

1. 退出并重启后验证 J/F9 持久化和实际热键触发，同时提供对应 Tweakeroo/MaLiLib 配置。
2. 原版 Minecraft 绑定、MaLiLib 2–4 键组合、CommandKeys 单键/limit+主键的编辑、重置、保存与触发。
3. 左/中/右/M4–M8 的编辑、显示、导出、清空、导入和实际触发；另导出包含这些值的真实 version 1 快照。
4. 导入预览的取消部分、取消全部、取消按钮、Esc、完全相同快照、未知 provider/ID、modifier-only 和错误 JSON。
5. Mod Menu 配置入口及返回链；本模组打开快捷键在游戏内和已有屏幕上的行为。
6. Visual Keymap 三项配置的默认创建、修改、返回即时生效和重启恢复；保留脱敏后的真实 `visualkeymap.json`。
7. 英文、三种 GUI 缩放、最小窗口/全屏、数字小键盘开关和 tooltip 数量边界。
8. 外部 `visualkeymap` entrypoint 消费方、异常隔离、provider 顺序及公开 API 编译。

## 8. 2026-10-31 补充：混合 provider 原件与 B2 跨版本验证

### 8.1 私有原件与完整性

维护者在工作区私有目录补充了两个未修改文件。它们不在 Git 仓库内，本次没有复制、暂存或提交：

| 本地路径 | 字节 | SHA-256 | 处理决定 |
| --- | ---: | --- | --- |
| `baseline-private/26.2/version 1.json` | 448 | `B3244C2560E54D84D459277AD9C12BCCB19F6AB9D5DF11D2226052FBFD320970` | 原始 26.2 导出；私有保留 |
| `baseline-private/26.2/options.txt` | 5,360 | `8CB130FEF450BD991D55A9C89D48E3891FAF4BA72C1389F85C720D45EC9FBB28` | 完整个人实例配置；私有保留 |

读取和哈希命令：

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath 'baseline-private/26.2/version 1.json'
Get-FileHash -Algorithm SHA256 -LiteralPath 'baseline-private/26.2/options.txt'
Get-Content -Raw -Encoding UTF8 -LiteralPath 'baseline-private/26.2/version 1.json' | ConvertFrom-Json
Select-String -LiteralPath 'baseline-private/26.2/options.txt' -Pattern 'visualkeymap'
```

测试结束后再次计算 JSON 原件哈希，仍为 `B3244C...20970`；原件未被修改。

### 8.2 version 1 结构与 provider

快照可正常解析，顶层 `version=1`，共有 3 条记录，provider 集合为 `minecraft, malilib`。该文件是混合 provider 快照，不得标记为纯 Minecraft 快照：

| provider | ID | keys | modifiers | 结论 |
| --- | --- | --- | --- | --- |
| `minecraft` | `visualkeymap.key.open_keymap` | `[297]` | `[]` | F8 的 26.2 规范整数 |
| `malilib` | `tweakFreeCamera` | `[74]` | `[]` | J，与既有真实 MaLiLib 快照一致 |
| `malilib` | `tweakHoldAttack` | `[298]` | `[]` | F9，与既有真实 MaLiLib 快照一致 |

`options.txt` 含：

```text
key_visualkeymap.key.open_keymap:key.keyboard.f8
```

这与快照的 Minecraft provider、稳定 ID 和规范码 `297` 交叉一致。MaLiLib 热键不会写入 Minecraft `options.txt`，因此不能用该文件交叉验证 J/F9 的第三方配置落盘；其 provider/ID/键码只在本阶段做静态确认，live 行为等待 C1。

### 8.3 安全派生与测试边界

为避免尚未迁移的 `MaLiLibIntegration` 影响第三方真实配置，测试只从原件过滤 `provider == "minecraft"`，生成 ignored 文件：

```text
runs/mc-26.3-b2.5/config/b25-derived-from-real-26.2-minecraft-only.json
SHA-256 D76F01E5EAC78436CED81C5ECE53FF68001413CE1650822F12F215FDB2A58127
```

派生文件保留 `version=1` 以及原始 Minecraft 记录的 provider、ID、keys 和 modifiers。它明确是由真实原件生成的测试副本，不是原版直接导出结果。原始混合快照和私人 `options.txt` 均未放入隔离 gameDir。

### 8.4 Minecraft provider 动态往返

执行命令：

```powershell
$env:JAVA_HOME='E:\02_Development\05_Environment\graalvm-jdk-25.0.4+7.1'
$env:GRADLE_USER_HOME='E:\02_Development\01_Workspace\VisualKeyMap26.3\.gradle-user-home'
$env:TEMP='E:\02_Development\01_Workspace\VisualKeyMap26.3\.tmp'
$env:TMP=$env:TEMP
.\gradlew.bat runClient --no-daemon
```

| 步骤 | 预期 | 实际 | 状态 |
| --- | --- | --- | --- |
| 启动隔离 26.3 实例 | 当前打开键为未绑定 | Visual Keymap 显示 `Not Bound`，隔离 `options.txt` 为 `key.keyboard.unknown` | **PASS** |
| 导入派生副本 | 只匹配 Minecraft 记录 | 预览标题为 `1 keybindings changes`，显示 `Not Bound → F8`，默认勾选 | **PASS** |
| 确认导入 | live binding 变为 F8 | 原版 Key Binds 立即显示 F8；隔离 `options.txt` 写 `key.keyboard.f8` | **PASS** |
| 完全退出并重启 | F8 被持久化 | 第二次启动后原版 Key Binds 仍显示 F8 | **PASS** |
| 再导出 version 1 | Minecraft 规范语义不变 | 生成 `b25-real-26.2-minecraft-roundtrip.json`，SHA-256 `15E948A1D8BCDB0A7DCA913F91DA7D32D01AE0211CFEBB73B2B75D3596E3D662` | **PASS** |
| 语义比较 | version/provider/ID/keys/modifiers 相等 | 原件与再导出均为 `1` 和 `minecraft|visualkeymap.key.open_keymap|297|`，`SemanticEqual=True` | **PASS** |

两次 `runClient` 均通过游戏内 `Quit Game` 正常退出，Gradle 分别返回 `BUILD SUCCESSFUL`。隔离实例导入后的 `options.txt` SHA-256 为 `30EC0316B5916DEE0935D8C190D3A7FF020F2BA7BA9540BAA14624F7BB276155`；该值只用于本地测试证据，不代表 26.2 私有原件。

### 8.5 结论与剩余边界

- “缺少真实 26.2 Minecraft provider 快照”遗留问题已关闭。
- Minecraft provider 的 F8 读取、预览、应用、保存、重启恢复和 version 1 语义往返均为 **PASS**。
- 同一原件中的 MaLiLib 两条记录仅为 **STATICALLY VERIFIED**；其 26.3 live 往返仍 **BLOCKED** 于 C1，不能因 Minecraft 子集通过而升级状态。
- 原件不含鼠标 0–7、unknown/空绑定或非空 modifiers；TEST-PLAN S-05 的这些子项仍为 **NOT TESTED**。

本次工作到基线验收和文档/夹具整理为止；未修改功能源码、构建配置或版本号，未进入 B2 或后续迁移阶段。
