# Visual Keymap：Minecraft 26.3 迁移就绪性审查

> 审查日期：2026-10-30  
> 审查范围：Phase 1、Phase 2 调查成果，未修改功能源码、构建配置或版本号  
> 源码基线：`USTC-XeF2/visual-keymap@053e736c77b15e0727c93abed7c94c3b0fb64618`  
> 最终结论：**NO-GO**

## 1. 结论摘要

调查内容本身已经达到较高完整度：18 个 Java 源文件、5 个资源/元数据文件、README、构建文件和发布工作流均已纳入 Phase 1/2；Minecraft 26.3 源码、主要 API 变化和全部第三方 26.3 发布物也已有证据。JDK 25 阻碍已经解除，当前终端和 Gradle wrapper 均已实际使用 GraalVM 25.0.4。

当前仍**不适合开始 Phase 3**，原因不是缺少迁移方向，而是缺少可执行、可回滚的正式起点：

1. 正式工作区 `VisualKeyMap26.3` 目前只有 `docs/`，没有 Visual Keymap 源码、Gradle wrapper 或 `.git`。现有源码只存在于临时调查副本，不能作为正式迁移工作树。
2. 原始 26.2 基线尚未完成一次成功的 Gradle 项目初始化和构建。离线 `help` 精确失败于未缓存的 Loom 1.17.21；在线解析连续无进展后被主动终止。因此目前不能证明该环境能执行编译、`runClient` 或测试任务。
3. 26.3-only 键没有 26.2 规范整数。若不先批准稳定的扩展编码及 version 1 行为，核心输入兼容层会在 public `List<Integer>`、鼠标 `0..7` 和快照语义之间产生不可逆的设计分歧。

以上三项均有最小解除路径；第三方功能无需删除、禁用或用空实现替代。解除后应重新做一次短就绪性复核，再决定进入 Phase 3。

## 2. 审查依据与覆盖范围

本次重新阅读了以下全部现有文档：

- [`BASELINE.md`](BASELINE.md)：用户可见功能、入口、关键实现、预期行为和验证方法。
- [`ARCHITECTURE.md`](ARCHITECTURE.md)：初始化、provider、GUI、快照、配置、Mixin 和第三方模块关系。
- [`COMPATIBILITY.md`](COMPATIBILITY.md)：mod/entrypoint/public API/provider/snapshot/Mixin/第三方兼容契约。
- [`TEST-PLAN.md`](TEST-PLAN.md)：26.2→26.3 对照矩阵和功能回归用例。
- [`MIGRATION-PLAN.md`](MIGRATION-PLAN.md)：依赖版本、旧/新 API、修改位置、风险、顺序和验证。

目录内没有其他调查报告。为检查“是否漏查模块”，还重新枚举并核对上游基线：18 个 `src/main/java` 文件全部在上述文档中有对应职责或修改判断；`fabric.mod.json`、`visual-keymap.mixins.json`、两份语言文件、图标、README/README.zh-CN、`build.gradle`、`gradle.properties`、`settings.gradle` 和发布 workflow 均已检查。未发现未调查的生产代码模块。

本次只修订了调查文档：

- 修正 `TEST-PLAN.md` 的 M-02，取消“所有 `< -1` 都按鼠标偏移”的旧假设，限定 MaLiLib 鼠标码为 `-99..-92`，并加入其他负值/gamepad 保留验证。
- 在 `TEST-PLAN.md` 新增 S-01–S-08 SDL/规范键码专项。
- 在 `MIGRATION-PLAN.md` 更新 Java/Gradle 环境事实，并补充 Phase 3 前置门槛、独立实施单元、依赖关系、停止条件和 Git 回滚策略。

## 3. 正式审查表

| 审查项目 | 当前状态 | 证据或验证结果 | 剩余风险 | 是否阻碍 Phase 3 |
| --- | --- | --- | --- | --- |
| 用户可见功能清单 | READY | `BASELINE.md` 覆盖三个入口、键盘可视化、筛选/排序/冲突、编辑/重置、配置、导入导出、保存和本地化；关键方法包括 `VisualKeymap.onInitializeClient()`、`ControlsScreenMixin.addKeymapOptionAndRedirect()`、`VisualKeymapScreen`、`KeyboardWidget`、`KeybindsListWidget` | 真实 26.2 交互参照尚未录屏/固化 | 否；作为 Phase 3 首批夹具风险管理 |
| 源码与资源模块覆盖 | READY | 18/18 Java 文件和 5/5 `src/main/resources` 文件已纳入文档；README、构建文件和 workflow 也已复核 | 临时源码副本不是正式工作树 | 正式工作树问题另列为 blocker |
| Minecraft 原生按键集成 | PARTIAL | 已定位 `MinecraftImpl.getKeyCodes()/setBoundKeys()`、`KeyMappingAccessor`、`KeyMapping.resetMapping()`、`Options.save()`；26.3 `InputConstants`/`KeyMapping` 源码已核对 | GLFW→SDL 数值、unknown、鼠标顺序、F25 和新增键仍需实现/运行验证 | 新增键编码决策阻碍；其余可在迁移中控制 |
| GUI、渲染和文本输入 API | PARTIAL | 26.3 源码确认 `OptionsSubScreen`、layout、`GuiGraphicsExtractor`、`InputWithModifiers`、`TextInputManager` 等所用接口仍存在；计划未要求无关重构 | 渲染缩放、焦点、IME、narration、文件对话框只能运行时验证 | 否 |
| Fabric API/Loader 集成 | READY | `ClientModInitializer`、`KeyMappingHelper`、`ClientTickEvents.END_CLIENT_TICK`、Loader entrypoint 机制均有目标版本和签名证据（静态） | 尚未在目标运行时加载 | 否；运行矩阵是 Phase 3 验收门槛 |
| Mixin 目标 | PARTIAL | `ControlsScreenMixin`、`HeaderAndFooterLayoutAccessor`、`KeyMappingAccessor` 的 26.3 类/字段/调用点已做源码/字节码核对 | redirect 命中次数、accessor 映射和生产环境 apply 仍未运行 | 否；必须在 V 单元失败即停 |
| 第三方 26.3 发布物 | READY | MaLiLib 0.30.2、CommandKeys 3.104.1+26.3、Mod Menu 21.0.0 均有 26.3 Fabric 正式发布物和 JAR API 核对（可获得性） | 尚未通过 Gradle 在本机完整解析 | 否；本机构建问题另列为 blocker |
| 第三方行为迁移方法 | PARTIAL | adapter 修改位置、保存调用、provider/ID/顺序和最小转换方式已明确 | MaLiLib gamepad/其他负值只能真实实例验证；不能误写 | 否；已有保留方案和停止条件 |
| `visualkeymap` entrypoint/public API | PARTIAL | `VisualKeymapApi`、`KeyBinding`、`VisualKeymap.getKeyBindings()/saveKeyBindings()/exportKeyBindings()`、entrypoint 枚举顺序已记录 | README 对外实现示例与 `KeyBinding` 部分 package-private 抽象方法可能不一致；尚无 26.2 消费方编译证据 | 否；API-01 先记录既有事实，禁止顺带修复 |
| 快照与配置兼容 | PARTIAL | version 1 schema、provider/ID、0–7 鼠标码、`ModConfig.load()/save()`、`VisualKeymapScreen.removed()` 保存链已明确 | 缺真实 26.2 golden file；26.3-only 键导出规则未批准 | 新增键规则阻碍 B1；golden file 可在 P1 后生成 |
| 测试覆盖设计 | READY | B/N/V/L/E/C/X/M/K/MM/API 和新增 S-01–S-08 覆盖用户可见行为、依赖缺席、外部 provider、Mixin 与 SDL 碰撞（计划层） | 仓库无测试框架、测试代码或已运行结果；P6 消费方夹具未建立 | 否；测试基础设施需独立提交审查 |
| JDK 25 | READY | 当前 `java -version`、`javac -version` 均为 25.0.4；process/machine `JAVA_HOME` 均指向 GraalVM 25.0.4 | Codex Java 的 `user.home=C:/` 是单独的宿主环境异常 | 否 |
| Gradle 实际 JVM | READY | 原项目 wrapper 报告 Gradle 9.5.1；Launcher JVM 25.0.4，Daemon JVM 指向 GraalVM 25.0.4 | 未指定 `GRADLE_USER_HOME` 时尝试写 `C:/.gradle`；Codex daemon 需 Unix-domain temp workaround | 否；有明确命令级规避，普通终端需复核 |
| 目标依赖版本组合 | PARTIAL | 计划组合为 Java 25、Gradle 9.8.2、Loom 1.19.3、Loader 0.20.1、Fabric API 0.165.0+26.3；发布元数据/官方示例支持该组合 | 尚未在正式项目冷缓存解析与编译 | 是，与基线构建/正式源码共同构成执行阻碍 |
| Minecraft 26.3 源码 | READY | `E:/02_Development/01_Workspace/04_MinecraftCodeDigging/26.3/src/main/resources/version.json` 明确 `id/name=26.3`；7,303 个 Java 文件，目标 API 已源码级复核 | 反编译源码不能代替运行测试 | 否 |
| Minecraft 26.2 证据 | PARTIAL | 上游模组源码、官方 26.2 client JAR 和既有 `javap`/字节码结果可用 | 没有完整 26.2 反编译源码和已运行参照实例 | 否；实际行为由 P1 参照实例补足 |
| 正式 Visual Keymap 工作树 | BLOCKED | 当前根目录仅有 `docs/`，`Test-Path .git=False`；临时副本 HEAD 为目标提交且干净，但不在正式工作区 | 无法实施、提交、比较或安全回滚 | **是** |
| 基线构建与依赖解析 | BLOCKED | 离线 `gradlew help` 失败于未缓存 `net.fabricmc:fabric-loom:1.17.21`；在线探测在 daemon 启动后 60 秒无进展并终止；没有成功 `build` | 不能区分后续错误是环境、旧 snapshot 失效还是迁移引入 | **是** |
| 编译、启动、测试任务可执行性 | BLOCKED | `--version` 可执行，但尚无 `help`、`clean build`、`runClient` 或测试成功记录 | Phase 3 无可用反馈回路 | **是** |
| 任务拆分与依赖关系 | READY | `MIGRATION-PLAN.md` 已补 P0/P1/A/B1/B2/C1–C3/V 单元、依赖和单元验收；先构建再 API 的顺序明确（计划层） | 只有在正式 Git 工作树中才能执行 | 否；由工作树 blocker 覆盖 |
| Git 回滚和失败处理 | PARTIAL | 已规定专用分支、分单元检查点、测试/生产分离提交、`git revert`、禁止清除不明修改和停止门槛 | 当前没有 `.git`，策略无法落地 | **是**，直到 P0 完成 |

## 4. 完整性、一致性与假设审查

### 4.1 已证实完整的部分

- 三个用户入口均已识别：原版控制菜单 Mixin、独立快捷键和 Mod Menu factory。
- GUI 行为链已从入口追到 `VisualKeymapScreen.addContents()/addOptions()/addFooter()`、`KeyboardWidget.KeyWidget.onPress()`、`KeybindsListWidget.createEntries()/updateAllEntries()` 和 `VisualKeymapScreen.setKeyBinding()`。
- 原版保存链已确认：`VisualKeymapScreen.removed()` 调用 provider 保存并进入 `super.removed()`；26.3 `OptionsSubScreen.removed()` 最终调用 `Options.save()`。这已解除 Phase 2 的静态待确认项，但仍需 E-07 运行验证。
- 快照创建、过滤、写入、读取、匹配和预览均已覆盖，核心位置为 `KeymapSnapshot`、`VisualKeymapScreen.doExport()/doImport()` 与 `ImportPreviewScreen`。
- 所有需要适配的输入数值消费点已在 `MIGRATION-PLAN.md` B.3 列出；明确禁止只做 `KEYSYM→KEYBOARD` 的表面替换。

### 4.2 已修正的矛盾

Phase 2 已证明 MaLiLib 0.30.2 的负值不再都代表鼠标，只有 `-99..-92` 是当前 adapter 需要转换的鼠标范围；原 `TEST-PLAN.md` M-02 却仍要求“所有 `< -1` 经 `+100/-100` 往返”。该用例会把旧缺陷错误地固化为验收标准，现已修正，并要求其他负值和 gamepad 夹具不被误改。

### 4.3 仍未验证但没有被冒充为事实的项目

- F25 在 26.3 SDL 注册表中缺失后的物理触发能力。
- 26.3-only 键的最终规范整数和 version 1 跨版本行为。
- Mixin 在开发/生产映射环境中的实际 apply。
- README 所示外部 `KeyBinding` 实现是否能在 26.2 独立包中编译。
- MaLiLib gamepad 绑定的编辑表现；最低保证仅是未编辑时不覆盖、不误转换。
- 文件对话框取消/错误路径、IME、缩放、narration 和各保存离开路径的真实行为。

以上均已在测试计划或停止门槛中显式标为待确认，没有把推测写成验证结论。

## 5. 开发环境就绪性

### 5.1 Java 与 Gradle

2026-10-30 实测：

- `java -version`：Oracle GraalVM `25.0.4+7-LTS-jvmci-b01`。
- `javac -version`：`javac 25.0.4`。
- process 和 machine `JAVA_HOME`：`E:/02_Development/05_Environment/graalvm-jdk-25.0.4+7.1`。
- `Get-Command java`：上述 JDK 的 `bin/java.exe`。
- 原项目 `gradlew --version`：Gradle 9.5.1，Launcher JVM 25.0.4，Daemon JVM 为上述 JDK。

因此 JDK 25 与 Gradle JVM 是 **READY**。当前 Codex 子进程还存在两个宿主级限制：Java `user.home` 被解析为 `C:/`，需要显式 `GRADLE_USER_HOME`；daemon socket 需要单次 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp`。两者均有命令级规避，不要求修改项目配置。

### 5.2 Minecraft 源码

26.3 源码树可访问，嵌入的 `version.json` 确认目标版本为正式 26.3。26.2 没有完整反编译树，但官方 client JAR、字节码检查和模组基线源码足以继续静态比较；缺少 26.2 行级源码不是 Phase 3 blocker。真正需要补的是可运行的 26.2 参照实例和 golden file。

### 5.3 构建反馈回路

当前只能运行 wrapper 版本检查，不能完成项目配置。离线错误为：

```text
Could not resolve net.fabricmc:fabric-loom:1.17.21
No cached version ... available for offline mode
```

在线探测没有在合理时间内产生进展。未取得以下任一成功证据：`gradlew help`、`gradlew clean build`、`gradlew runClient`、测试任务。因此“迁移后可编译、可启动、可测试”目前是 **BLOCKED**，不能仅凭版本元数据判为就绪。

## 6. 第三方依赖与功能保留审查

| 依赖/接口 | 26.2 原版功能 | 26.3 兼容实现与迁移方法 | 主要风险 | 结论 |
| --- | --- | --- | --- | --- |
| Minecraft 原生按键 | `MinecraftImpl` 包装 `Options.keyMappings`，读写主键、debug modifier，调用 `KeyMapping.resetMapping()`；父屏保存 options | 目标类和保存 API 存在；所有 SDL/native key 只在集中式兼容层转换为 26.2 规范整数 | 键盘/鼠标数值碰撞、unknown 变化、右/中键顺序、F25、新增键 | 方法明确；新增键编码是 blocker |
| MaLiLib | 枚举 config manager/hotkey，显示名称/comment，最多四键，保存全部配置并 `updateUsedKeys()` | 使用 MaLiLib 0.30.2；只把 `-99..-92` 当鼠标，键盘通过 26.3 `InputConstants.Key` 边界转换；保留 save/update 调用 | 0.30.2 新增 gamepad/其他负值，旧 `< -1` 规则会破坏数据 | 可迁移；禁止删除集成，gamepad 为受控风险 |
| CommandKeys | 读取当前 profile 的 `macroMap`，映射主键/limit key，保存 `Config.save()`；provider 为空所以不进入快照 | 使用 3.104.1+26.3；JAR API 签名与现有调用兼容，adapter 统一走键码兼容层 | 当前 profile、两键顺序和 provider 为空的既有行为易被“整理”改变 | READY（静态），需 K/S 运行验证 |
| Mod Menu | `ModMenuImpl.getModConfigScreenFactory()` 返回带原父屏的 `VisualKeymapScreen` | 使用 Mod Menu 21.0.0；接口签名兼容，无需业务改写 | 可选依赖缺席时类加载、返回链 | READY（静态），需 P1/P2 和 MM 用例 |
| `visualkeymap` entrypoint / `VisualKeymapApi` | Loader 枚举外部 provider；异常实现被隔离；provider 顺序参与默认排序/保存/快照 | 保持 entrypoint key、public 签名、provider/ID/顺序；建立独立消费方测试模组 | README 示例与实际抽象方法可见性可能是 26.2 既有问题 | 可保留；先测 26.2，不得借迁移修 API |

若某第三方 artifact 在执行时临时不可解析，保留功能的顺序应为：固定已确认正式版本并检查仓库/网络；本地验证发布 JAR；必要时将 adapter 隔离为独立 source boundary；最后才考虑与上游协调。删除 integration、伪造空 provider 或吞掉保存均不是可接受方案。

## 7. 迁移方案可执行性

`MIGRATION-PLAN.md` 已被修订为以下依赖链：

`P0 正式源码/Git → P1 26.2 基线构建 → A 依赖迁移 → B1 输入兼容层 → B2 核心/GUI/快照 → C1/C2/C3 第三方与 API → V 打包/启动/全量回归`

该拆分满足“先构建环境、后具体 API”，并允许 MaLiLib、CommandKeys、Mod Menu/public API 分别实施和验收。B2 必须依赖 B1，防止 SDL 转换散落到 GUI 和 adapter。每个单元都有停止条件和验收标准。

行为比较依赖两类基线资产：

1. 26.2 参照实例：按 `TEST-PLAN.md` 的 P1–P6 记录交互、错误路径和保存行为。
2. 数据/二进制夹具：真实 version 1 快照、配置文件、公开 API 消费方编译结果和 public 签名清单。

Git 策略现已明确：从精确基线创建专用分支，按 P0/A/B1/B2/C1/C2/C3/V 设置检查点，测试基础设施独立提交；失败时 revert 单一检查点或在新分支重做，不对不明修改执行硬重置。涉及 API/schema/provider ID、删除第三方功能、修既有 Bug 或键码碰撞时必须停止并重新审查。

## 8. 问题分级

### BLOCKER：Phase 3 前必须解决

#### B-1 正式源码和 Git 基线缺失

当前工作区只有文档且无 `.git`。必须把 Visual Keymap 正式源码放入本工作区，验证 HEAD/基线为 `053e736c77b15e0727c93abed7c94c3b0fb64618`，确认远端和工作树状态，并建立专用迁移分支。临时调查目录不能替代此步骤。

#### B-2 没有成功的基线构建反馈回路

必须在不修改原版本/构建配置的情况下完成 `gradlew help` 和 `gradlew clean build`，或证明旧 Loom snapshot 已从上游不可恢复并由维护者批准把“仅构建环境恢复”作为独立、首个可回滚任务。还需至少确认 `runClient` 能进入启动流程。否则后续无法可靠归因迁移错误。

#### B-3 26.3-only 键的扩展编码与快照规则未决

建议批准以下原则：既有 26.2 规范整数和鼠标 `0..7` 不变；为新增键使用带命名空间、无碰撞、可稳定往返的扩展整数；裸 SDL 值只存在于兼容层边界；version 1 对扩展值的写入/读回行为必须明确并有测试。若选择 typed key，也必须证明不会改变 public `List<Integer>` 和现有 snapshot schema。该决策必须在 B1 实现前完成。

### RISK：可在迁移过程中控制

- F25 数据可保留，但物理触发可能受 26.3 SDL 后端限制；S-06 要求可复现证据，不能静默换键。
- MaLiLib gamepad/其他负值超出原 26.2 功能范围；未编辑时必须无损，编辑行为等待真实实例验证。
- required Mixin 只有静态证据；任何 apply 失败都应停止 V 单元。
- 外部 `KeyBinding` 的 README 可实现性可能存在 26.2 既有缺陷；API-01 先建立旧版本事实。
- 真实 26.2 golden snapshot、P1–P6 运行目录和测试消费方尚未创建。
- GUI 缩放、IME、narration、文件对话框与错误路径依赖人工回归。
- 默认构建同时声明多个可选 runtime 依赖；“无可选依赖”实例的 classpath/run 方案需在测试基础设施中明确，不能只删除 JAR 后假定有效。
- 目标依赖组合已有发布证据但未做本机冷缓存解析；执行时仍可能出现仓库或 transitive dependency 问题。

### NON-BLOCKING：不妨碍迁移目标

- 没有完整 26.2 反编译源码；官方 JAR、基线源码和运行参照可以补足。
- 当前 Codex 的 `user.home`/Unix-domain socket 异常；已有单次环境变量 workaround，且不要求修改项目。
- 现有仓库没有自动化测试；测试计划已完整，测试基础设施可作为 Phase 3 独立提交建立。
- 26.2 已有 Bug、README/实现不一致或异常路径问题；只记录并保持，不纳入迁移修复。
- 三个第三方模组已有正式 26.3 发布物，不需要功能降级方案。

## 9. 开始迁移前需要维护者作出的技术决策

1. **必须决定：26.3-only 键编码。**建议采用稳定的 tagged integer namespace，保持所有既有整数与 version 1 字段不变，并在 S-07 中验证。还需明确旧 26.2 客户端读到扩展值时的预期是“保留但不可触发”还是拒绝导入；不得默认为裸 SDL 数字。
2. **建议确认：允许添加最小测试基础设施。**若批准，应把 test source set/依赖修改作为独立提交；若不批准，则必须接受 SDL 映射、golden file 和 API 消费方只能用外部 harness/人工方式执行，回归置信度会降低。
3. **执行时决定：若旧 Loom 1.17.21 已无法解析，是否接受独立恢复提交。**该提交只恢复 26.2 基线构建，不得同时改 Minecraft 版本或功能代码，并应在迁移分支上保留完整失败证据。

## 10. 最终决策与最小解除清单

### 决策：NO-GO

JDK、目标源码、API 调查和第三方版本已经足够，但正式源码/Git 工作树、成功构建反馈回路和新增键编码契约尚未到位。此时进入 Phase 3 会让实现无法安全提交/回滚，也无法判断编译失败来自基线环境还是迁移，因此不能给出 GO 或 CONDITIONAL GO。

解除 NO-GO 所需的最小工作：

1. 在正式工作区放置 Visual Keymap Git 仓库，验证精确基线、远端、工作树和专用迁移分支。
2. 使用当前已验证的 JDK 25，在未改版本的基线完成 `gradlew help`、`gradlew clean build`，并确认 `runClient` 至少进入启动流程；保存命令和日志。若旧 Loom 不可恢复，先提交证据并取得“独立恢复构建环境”的批准。
3. 维护者批准 26.3-only 键的扩展编码与 version 1 行为原则。
4. 固化最小 26.2 比较资产：至少一份真实 version 1 快照、一份原版/第三方配置集和 API-01 的 26.2 编译结果。
5. 按上述结果更新本报告中的 BLOCKED 行；全部解除后再给出 GO/CONDITIONAL GO，并从 `MIGRATION-PLAN.md` 的 P0/P1/A 开始。

本审查到此停止。未开始 Phase 3，未修改任何功能源码、构建配置或版本号。
