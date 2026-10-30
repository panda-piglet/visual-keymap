# Visual Keymap：Minecraft 26.3 迁移就绪性审查

> 历史状态说明：本文主体记录 Phase 2.6 结束时的就绪性结论。维护者随后已批准 Phase 3 A 与最小 B1；实际执行结果见 [`PHASE3-A-B1-REPORT.md`](PHASE3-A-B1-REPORT.md)，26.2 材料步骤见 [`BASELINE-ASSETS.md`](BASELINE-ASSETS.md)。本文原有表格不回写为“当时已经完成”，以保留审计时间线。

> 审查日期：2026-10-30  
> 审查范围：Phase 1、Phase 2 与 Phase 2.6 阻碍解除；未修改功能源码、构建配置或版本号
> 源码基线：`USTC-XeF2/visual-keymap@053e736c77b15e0727c93abed7c94c3b0fb64618`  
> Phase 2.6 复核结论：**CONDITIONAL GO**（仍需维护者批准键码提案并明确批准进入 Phase 3）

## 1. 结论摘要

Phase 2.5 的三个 NO-GO 事项中，P0 正式工作树已经解除，P1 已建立可重复的基线构建/启动反馈回路，B1 已形成具体且经过 26.2 动态探针验证的编码提案。当前状态提升为 **CONDITIONAL GO**：可以在维护者另行明确批准 Phase 3 后从 A（构建环境和依赖）开始，但不得在键码提案获批前实现 B1，也不得在 26.2 对照资产到位前宣告行为兼容完成。

- P0：正式仓库、远端、分支、基线和独立文档提交均已验证，生产文件零差异。
- P1：Loom 1.17.21 可在线恢复；原失败的真实根因是 MaLiLib 0.29.0 的旧 Maven 坐标已返回 404。使用仓库外精确同版本 Modrinth 坐标别名后，`help`、`clean build` 和 `runClient` 启动链均成功。
- B1：提议使用 `0x01000000` 扩展 namespace；首批 127 键为 `0x01000001..0x0100007F`。完整规则见 [`KEYCODE-DESIGN.md`](KEYCODE-DESIGN.md)，仍等待维护者批准。
- 26.2 资产：构建日志、启动日志、默认 options/config 和自动探针已取得；真实交互快照、第三方配置和人工视觉行为仍待准备，但不阻碍 Phase 3 的 A 单元。

## 2. 审查依据与覆盖范围

本次重新阅读了以下全部现有文档：

- [`BASELINE.md`](BASELINE.md)：用户可见功能、入口、关键实现、预期行为和验证方法。
- [`ARCHITECTURE.md`](ARCHITECTURE.md)：初始化、provider、GUI、快照、配置、Mixin 和第三方模块关系。
- [`COMPATIBILITY.md`](COMPATIBILITY.md)：mod/entrypoint/public API/provider/snapshot/Mixin/第三方兼容契约。
- [`TEST-PLAN.md`](TEST-PLAN.md)：26.2→26.3 对照矩阵和功能回归用例。
- [`MIGRATION-PLAN.md`](MIGRATION-PLAN.md)：依赖版本、旧/新 API、修改位置、风险、顺序和验证。
- [`KEYCODE-DESIGN.md`](KEYCODE-DESIGN.md)：Phase 2.6 扩展整数布局、version 1 策略和旧版安全行为。

[`evidence/`](evidence/) 保存 Phase 2.6 的原配置失败日志、环境别名后的成功日志、客户端启动日志、26.2 动态探针输出和冻结映射表。为检查“是否漏查模块”，还重新枚举并核对上游基线：18 个 `src/main/java` 文件全部在上述文档中有对应职责或修改判断；`fabric.mod.json`、`visual-keymap.mixins.json`、两份语言文件、图标、README/README.zh-CN、`build.gradle`、`gradle.properties`、`settings.gradle` 和发布 workflow 均已检查。未发现未调查的生产代码模块。

本次只修订了调查文档：

- 修正 `TEST-PLAN.md` 的 M-02，取消“所有 `< -1` 都按鼠标偏移”的旧假设，限定 MaLiLib 鼠标码为 `-99..-92`，并加入其他负值/gamepad 保留验证。
- 在 `TEST-PLAN.md` 新增 S-01–S-08 SDL/规范键码专项。
- 在 `MIGRATION-PLAN.md` 更新 Java/Gradle 环境事实，并补充 Phase 3 前置门槛、独立实施单元、依赖关系、停止条件和 Git 回滚策略。
- Phase 2.6 新增 `KEYCODE-DESIGN.md` 和构建/探针证据；再次修订 `MIGRATION-PLAN.md` 的 MaLiLib Maven 坐标，并把本报告状态更新为 CONDITIONAL GO。

## 3. 正式审查表

| 审查项目 | 当前状态 | 证据或验证结果 | 剩余风险 | 是否阻碍 Phase 3 |
| --- | --- | --- | --- | --- |
| 用户可见功能清单 | READY | `BASELINE.md` 覆盖三个入口、键盘可视化、筛选/排序/冲突、编辑/重置、配置、导入导出、保存和本地化；关键方法包括 `VisualKeymap.onInitializeClient()`、`ControlsScreenMixin.addKeymapOptionAndRedirect()`、`VisualKeymapScreen`、`KeyboardWidget`、`KeybindsListWidget` | 真实 26.2 交互参照尚未录屏/固化 | 否；作为 Phase 3 首批夹具风险管理 |
| 源码与资源模块覆盖 | READY | 正式仓库含 18/18 Java 文件和 5/5 `src/main/resources` 文件；文档、README、构建文件和 workflow 均已复核 | 无未调查生产模块 | 否 |
| Minecraft 原生按键集成 | PARTIAL | 已定位全部调用点；[`KEYCODE-DESIGN.md`](KEYCODE-DESIGN.md) 定义旧键、鼠标、127 个新增键和 version 1 规则，26.2 探针证明候选扩展值可原值往返 | 提案尚待维护者批准；F25 物理触发和真实 GUI/provider 往返仍待验证 | 只阻碍 B1/B2，不阻碍先执行 A |
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
| 目标依赖版本组合 | PARTIAL | 计划组合为 Java 25、Gradle 9.8.2、Loom 1.19.3、Loader 0.20.1、Fabric API 0.165.0+26.3；MaLiLib 0.30.2 的官方 Modrinth POM 实测可解析 | 尚未在正式项目冷缓存完成 26.3 编译 | 否；这是 Phase 3 A 的首个验收门槛 |
| Minecraft 26.3 源码 | READY | `E:/02_Development/01_Workspace/04_MinecraftCodeDigging/26.3/src/main/resources/version.json` 明确 `id/name=26.3`；7,303 个 Java 文件，目标 API 已源码级复核 | 反编译源码不能代替运行测试 | 否 |
| Minecraft 26.2 证据 | PARTIAL | 上游模组源码、官方 26.2 client JAR 和既有 `javap`/字节码结果可用 | 没有完整 26.2 反编译源码和已运行参照实例 | 否；实际行为由 P1 参照实例补足 |
| 正式 Visual Keymap 工作树 | READY | 正式仓库 HEAD 起点为 `053e736`，分支 `port/mc-26.3`；origin 为 `panda-piglet/visual-keymap`，upstream 为 `USTC-XeF2/visual-keymap`；文档提交 `97cc831` 后工作树干净 | 文档后续更新尚待第二次独立提交 | 否 |
| 基线构建与依赖解析 | PARTIAL | Loom 1.17.21 已在线下载；原坐标直接失败于 MaLiLib 0.29.0 404；仓库外 init script 精确替换为同版本官方 Modrinth artifact 后 `help` 与 `clean build` 成功 | 原构建文件本身仍含失效坐标；目标迁移 A 必须改为官方 Modrinth 坐标 | 否；根因、恢复方法和 Phase 3 最小修改均明确 |
| 编译、启动、测试任务可执行性 | READY | `clean build` 成功，主 JAR/sources JAR 生成；`runClient` 加载 Minecraft 26.2、Fabric、MaLiLib、Mod Menu、Visual Keymap 并完成图形/声音/资源初始化 | 当前 `test` 为 `NO-SOURCE`；只证明任务链和客户端启动，不证明用户交互 | 否 |
| 任务拆分与依赖关系 | READY | `MIGRATION-PLAN.md` 已补 P0/P1/A/B1/B2/C1–C3/V 单元、依赖和单元验收；先构建再 API 的顺序明确（计划层） | 只有在正式 Git 工作树中才能执行 | 否；由工作树 blocker 覆盖 |
| Git 回滚和失败处理 | READY | 专用分支已存在；基线提交与独立文档提交可作为两个检查点，生产文件相对 `053e736` 零差异 | 后续仍需按 A/B1/B2/C/V 分提交 | 否 |

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

Phase 2.6 的首次原配置在线 `help` 已证明 Loom 1.17.21 正常存在并成功下载。项目配置随后失败于：

```text
Could not find fi.dy.masa.malilib:malilib-fabric-26.2:0.29.0
```

原声明仓库中的该 POM/JAR 均返回 404；同一个 MaLiLib 0.29.0 官方发布物仍在 Modrinth，`maven.modrinth:malilib:0.29.0` POM 返回 200。为保持项目文件不变，复核使用仓库外 init script 做精确坐标替换。结果：

- `gradlew help`：成功，36 秒；
- `gradlew clean build`：成功，55 秒；`compileJava`、resources、JAR、sources JAR 和 build 均执行；`test` 为 `NO-SOURCE`；
- `gradlew runClient`：成功进入 26.2 客户端渲染线程，加载 Visual Keymap 1.4.0、MaLiLib 0.29.0、Mod Menu、Fabric API；Mixin subsystem 为 Java 25，OpenGL、OpenAL 和资源图集初始化完成后由审查者主动终止。

日志位于 [`evidence/`](evidence/)。启动中的 Windows performance counter、离线开发用户 401、首次无 MaLiLib 配置文件等错误不导致客户端启动失败，也不是 Visual Keymap 源码问题。

结论：基线构建反馈回路已恢复。原项目构建配置仍不可在无 init script 时直接解析 MaLiLib，这是已定位的发布坐标变化；Phase 3 A 必须把 0.30.2 的两处依赖声明改为官方 Modrinth 坐标，不能继续声称“只改属性值即可”。

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

## 8. Phase 2.6 四项状态

| 项目 | 状态 | 实际证据 | 剩余问题 | 是否仍阻碍迁移 |
| --- | --- | --- | --- | --- |
| P0 正式 Git 工作树 | READY | 正式仓库 `.git` 存在；起点 `053e736c77b15e0727c93abed7c94c3b0fb64618`；`origin=https://github.com/panda-piglet/visual-keymap.git`；`upstream=https://github.com/USTC-XeF2/visual-keymap.git`；分支 `port/mc-26.3`；初始调查文档提交 `97cc831`，Phase 2.6 设计/证据另有独立文档提交；生产文件相对基线无差异 | 无 | 否 |
| P1 原版构建 | PARTIAL | JDK/Gradle 均为 25；Loom 1.17.21 成功解析；环境别名后 `help`、`clean build` 成功且 `runClient` 完成客户端初始化 | 原 MaLiLib 0.29.0 坐标已 404，未经 init script 的原配置仍失败；Phase 3 A 必须改用官方 Modrinth 坐标 | 不阻碍 A；未修坐标前阻碍目标 clean build |
| B1 新增键码设计 | PARTIAL | 121 个旧键、247 个目标键、127 个新增键和 F25 差异已自动枚举；`0x01000001..0x0100007F` 零碰撞；26.2 InputConstants/JSON 探针原值往返成功 | [`KEYCODE-DESIGN.md`](KEYCODE-DESIGN.md) 尚待维护者批准；真实旧版 GUI/provider 往返待测 | **阻碍 B1/B2 实现，不阻碍 A** |
| 26.2 测试资产 | PARTIAL | 已有构建产物、构建/启动日志、默认 `options.txt`、默认 `visualkeymap.json`、扩展码探针和冻结映射表 | 缺真实交互导出的 version 1 文件、第三方配置改动和视觉/交互记录 | 不阻碍 A；阻碍 B2/C/V 的最终兼容验收 |

## 9. 问题分级

### BLOCKER

- **B1 开始前必须批准扩展键码提案。**批准范围是 namespace、冻结 ID、version 1 承载、26.2“保留但不可触发”、未知扩展不覆盖和 F25 tombstone。未经批准不得实现输入兼容层。
- **进入 Phase 3 仍需要维护者明确指令。**本报告的 CONDITIONAL GO 不是自动开工授权。

### RISK

- 原 MaLiLib Maven 坐标已失效；Phase 3 A 必须在独立构建提交中改用 `maven.modrinth:malilib:0.30.2`，保留 `compileOnly`/`runtimeOnly` 语义。
- F25 数据可保留，但 26.3 没有物理键；不得映射到 SDL 314 或近似键。
- MaLiLib gamepad/其他负值必须保持未编辑数据，不得沿用所有 `< -1` 都按鼠标转换的规则。
- required Mixin 目前只有 26.2 启动成功和 26.3 静态目标证据；目标 runtime apply 仍是 V 单元门槛。
- 外部 `KeyBinding` 的 README 可实现性、无可选依赖 classpath、GUI/IME/narration/文件对话框仍需专项验证。
- 真实 26.2 对照资产未齐；在其到位前不能判断用户行为完全等价。

### NON-BLOCKING

- 缺少完整 26.2 反编译源码；官方 JAR、运行探针和正式模组源码已足以支持迁移分析。
- Codex 的 `user.home` 和 Unix-domain socket 异常；明确的 `GRADLE_USER_HOME`/`JAVA_TOOL_OPTIONS` 只影响调查宿主，不要求修改项目。
- `runClient` 中的 Windows performance counter、离线开发用户 401 和首次缺少 MaLiLib 配置文件日志不阻止客户端启动。
- 现有测试任务为 `NO-SOURCE`；可在 Phase 3 以独立测试基础设施提交补充，不与生产适配混合。

## 10. 26.2 最小测试资产清单

以下清单只要求维护者提供必须来自真实 26.2 游戏操作的材料。代码可生成的夹具由迁移执行者负责，不要求维护者手工制作。

### 10.1 自动生成，不需要维护者提供

- 有效/无效 version 1 JSON：未知 provider/ID、空 keys、mouse `0..7`、modifier-only、未知 version、null、缺字段和非法 JSON。
- 121 个旧键、127 个扩展键、F25、unknown、边界值和碰撞测试数据。
- API-01 外部 provider 测试模组源码、异常 entrypoint、稳定 provider/ID/order 夹具。
- 默认 `visualkeymap.json`、期望 schema、public 签名清单、构建产物哈希和自动化断言。
- 只读/不可写目录、重复导出、排序/冲突算法等可由 harness 构造的测试条件。

### 10.2 必须从实际 26.2 游戏操作取得的文件

请使用一个独立的 26.2 实例，安装 Visual Keymap 1.4.0、Fabric API、Mod Menu 20.0.0-beta.3、MaLiLib 0.29.x、至少一个真正注册 MaLiLib 热键的模组、CommandKeys 3.103.0。完成下述最小改动后提供一个压缩包：

1. 一个原版键盘绑定改为非默认键；一个原版绑定改为鼠标；另一个原版绑定清空。
2. 一个 MaLiLib 单键、一个 2 键组合、一个鼠标热键。
3. CommandKeys 准备一个单键宏和一个 limit+主键宏，并至少有两个 profile。
4. 在 Visual Keymap 中切换三项配置后正常离开并重启一次。
5. 通过模组的导出按钮生成真实 `visualkeymap-export.json`。

压缩包只需包含：

- `visualkeymap-export.json`；
- `options.txt`；
- `config/visualkeymap.json`；
- 本次实际发生变化的 MaLiLib 所属模组配置文件；
- CommandKeys 配置文件；
- 对应运行的 `logs/latest.log`；
- 一个简短 `versions.txt`，写明精确 mod 文件名和所用 profile/绑定名称。

不要提供整个 `.minecraft`、账号文件、服务器地址、聊天记录或无关配置。若配置中含隐私内容，应先使用专门测试 profile/宏，避免事后编辑破坏证据。

### 10.3 必须人工观察或录制

最少提供三段短录屏，或一段连续录屏并附时间点：

1. **入口与返回链：**原版“选项→控制”按钮、Visual Keymap 独立快捷键、Mod Menu 入口各打开一次，并显示返回到正确父屏。
2. **输入行为：**原版改键、编辑中 Esc 清空、左/右/中鼠标；MaLiLib 两键组合的按下/释放结束时机；CommandKeys limit+主键。M4/M5 无硬件时只需注明“不可用”，不要求模拟。
3. **GUI 与持久化：**键盘高亮/冲突、搜索和三种排序、导入预览勾选/取消、完成后重启并展示绑定仍存在。另记录通过 Done、Esc、打开其他屏幕离开主界面的保存结果；可在同一录像中分段完成。

语言和缩放最低覆盖 `zh_cn` 与 `en_us` 各一张主界面截图、一个小/中/大 GUI 缩放对照。IME、narration、只读目录和无效 JSON 的异常行为可在后续专项人工回归中记录，不要求放入首批压缩包。

## 11. 最终决策

### 决策：CONDITIONAL GO

P0 已完全解除；P1 已建立可用反馈回路并精确定位旧坐标问题；B1 已从抽象原则收敛为可审查的具体设计。剩余事项不再阻碍从 Phase 3 的 A 单元开始，但必须遵守：

1. 维护者另行明确批准进入 Phase 3；本阶段不自动开始。
2. A 单元先只迁移构建环境和依赖，并修正 MaLiLib 官方 Maven 坐标；验收为目标依赖解析、`clean build` 到达实际编译及产物元数据正确。
3. [`KEYCODE-DESIGN.md`](KEYCODE-DESIGN.md) 获得明确批准前，不得实现 B1/B2。
4. 26.2 实际资产最迟在 B2/C 的行为改动合并前到位；缺失项不能用“源码看起来一致”代替。
5. 自动化测试基础设施、生产适配和第三方 adapter 分开提交，继续使用既定停止/回滚策略。

本审查到此停止。未开始 Phase 3，未修改任何功能源码、构建配置或版本号，未合并主分支，也未发布版本。
