# Visual Keymap：Minecraft 26.3 迁移方案

> 调查日期：2026-10-29；就绪性复核：2026-10-30  
> 源码基线：上游 `USTC-XeF2/visual-keymap` 的 `main@053e736`（Minecraft 26.2）  
> 本文状态：迁移分析；尚未执行源码、构建配置或版本号修改

本文在 `BASELINE.md`、`ARCHITECTURE.md`、`COMPATIBILITY.md` 与 `TEST-PLAN.md` 的第一阶段结论上继续调查。除明确标为“待确认”的事项外，结论来自以下一种或多种证据：基线源码、官方 26.2/26.3 客户端 JAR 的类签名/字节码对比、依赖发布 JAR 的类签名对比、官方迁移说明和发布元数据。

## 0. 环境与源码复核（2026-10-29）

本节记录 JDK 25 和 Minecraft 26.3 反编译源码到位后的复核结果。这里的“已解除”只表示原调查阻碍消失，不表示迁移实现或运行回归已经完成。

### 0.1 Java 25

| 检查项 | 实际结果 | 状态 |
| --- | --- | --- |
| 指定 JDK 文件 | `E:/02_Development/05_Environment/graalvm-jdk-25.0.4+7.1/bin/java.exe` 与 `javac.exe` 均存在 | 已解除 |
| 指定 `java -version` | Oracle GraalVM `25.0.4+7-LTS-jvmci-b01`，64-bit Server VM | 已解除 |
| 指定 `javac -version` | `javac 25.0.4` | 已解除 |
| 当前 Codex 进程 `JAVA_HOME` | 2026-10-30 复核为 `E:/02_Development/05_Environment/graalvm-jdk-25.0.4+7.1` | 已解除 |
| 机器级 `JAVA_HOME` | 已正确设置为 GraalVM 25 根目录 | 已配置 |
| 当前终端命令解析 | `Get-Command java` 指向 GraalVM 25；`java -version` 与 `javac -version` 均直接报告 25.0.4 | 已解除 |

结论：**JDK 25 不再缺失，当前终端环境变量刷新问题也已解除**。本调查未修改系统环境变量。当前 Codex Java 进程仍把 `user.home` 报为 `C:/`，导致未指定缓存目录的 wrapper 尝试写入不可写的 `C:/.gradle`；这是当前执行宿主的环境问题，不是 JDK 版本问题。复核命令需显式设置 `GRADLE_USER_HOME`，普通 Windows Terminal 仍应单独验证默认用户目录行为。

### 0.2 Gradle 实际 JVM

复核对象为未修改的上游 `main@053e736` 临时副本，wrapper 仍是项目基线 Gradle 9.5.1。显式注入上述 JDK 25 后：

- 2026-10-30 使用现有临时 Gradle 缓存再次执行 `gradlew --version` 成功，报告 `Gradle 9.5.1`、`Launcher JVM: 25.0.4`，`Daemon JVM` 也指向该 GraalVM 目录；因此“Gradle 未使用 JDK 25”的阻碍已解除。
- 当前 Codex Windows 子进程直接运行 `gradlew help` 仍会在项目配置前失败，堆栈为 `WEPollSelectorImpl -> PipeImpl -> UnixDomainSockets.connect0`，最终错误为 `Unable to establish loopback connection` / `Invalid argument: connect`。沙箱外重试结果相同。
- 仅对单次命令设置 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp` 后，Gradle 能正常建立 single-use daemon，证明该错误不是 Visual Keymap 构建脚本或 JDK 缺失造成。此 workaround 与 [Codex Windows 已知问题 #40902](https://github.com/openai/codex/issues/40902) 中的同堆栈和已验证解法一致。
- 使用 workaround 的离线 `gradlew help` 已进入项目配置，随后明确失败于本地缓存缺少 `net.fabricmc:fabric-loom:1.17.21`。2026-10-30 再次在线探测，60 秒内仍停留在 daemon 启动后且无依赖解析进展，随后被主动终止；此前探测也曾超过 90 秒。因此尚未得到完整项目配置、依赖解析或编译成功证据。

结论：Gradle wrapper 与 Java 25 的组合已验证可启动；先前 loopback 阻碍已有局部、非项目级 workaround。**完整 Gradle 构建仍待在普通 Windows Terminal/PowerShell（推荐）或依赖网络可用的环境执行**。该待确认项不再归因于缺少 JDK。

### 0.3 Minecraft 源码

| 版本 | 当前可用证据 | 状态与用途 |
| --- | --- | --- |
| 26.3 | `E:/02_Development/01_Workspace/04_MinecraftCodeDigging/26.3`；7,303 个 Java 文件、21,633 个资源文件。`README.md` 标明 Minecraft 26.3、Mojang mappings、Vineflower `1.13.0-a712f4c930`；`version.json` 标明稳定版 26.3、world version 5023、Java 25 | 目标端反编译源码阻碍已解除，可做类、字段、方法体和调用点检查 |
| 26.2 | 未在工作区找到完整反编译源码；保留官方客户端 JAR `client-26.2.jar` 及先前 `javap`/字节码检查结果 | 仍无 26.2 行级源码，但不阻碍目标端确认；26.2 旧行为继续以 Visual Keymap 基线源码、官方 JAR 和 26.2 实例回归为证据 |
| Visual Keymap 26.2 | 上游临时 Git 副本仍精确位于 `053e736c77b15e0727c93abed7c94c3b0fb64618`，工作树干净 | 模组源码基线可继续复核 |

`VisualKeyMap26.3` 当前项目根目录仍只包含迁移文档；Minecraft 源码位于相邻工作区。本文引用该源码但未复制、生成或修改它。

### 0.4 原阻碍状态

| 原事项 | 复核结论 |
| --- | --- |
| 缺少 JDK 25 | **已解除**：指定 JDK、当前终端的 `java`/`javac` 和 Gradle JVM 均验证为 25.0.4。 |
| 缺少 Minecraft 26.3 反编译源码 | **已解除**：完整源码已定位并验证版本。 |
| 26.3 Minecraft API 只能做二进制签名检查 | **已解除**：本项目实际依赖的输入、GUI、布局、事件和 Mixin 目标已做源码级复核，见 2.4、B.4、B.5。 |
| 完整 Gradle 配置/编译 | **仍待确认**：loopback 可绕过，但当前 Codex 环境缺少 Loom 缓存且在线解析未完成。 |
| 游戏启动、Mixin 实际应用和用户交互 | **仍待确认**：源码存在不能替代 `runClient` 与人工回归。 |

## 1. 结论摘要

1. Minecraft 26.3 仍使用 Java 25。当前 Fabric 官方 26.3 示例使用 Gradle 9.8.2、Fabric Loader 0.20.1、Loom 1.19-SNAPSHOT 和 Fabric API 0.165.0+26.3；Loom 同一发布线已有稳定版 1.19.3，建议使用稳定版以保证构建可重复。
2. MaLiLib 0.30.2、CommandKeys 3.104.1+26.3、Mod Menu 21.0.0 均有 Minecraft 26.3 Fabric 正式发布物，无需删除任何兼容功能。
3. 主要迁移风险不是 GUI 方法改名，而是 GLFW 到 SDL 的输入模型变化：
   - `InputConstants.Type.KEYSYM` 改为 `InputConstants.Type.KEYBOARD`；
   - 未绑定键值从 `-1` 改为 `0`；
   - 键盘值由 GLFW keycode 改为 SDL scancode，例如 A 从 `65` 改为 `4`、F12 从 `301` 改为 `69`；
   - 鼠标值由 `0..7` 改为 `1..8`，且右键/中键不能用统一偏移量换算。
4. 当前公开 `KeyBinding` 抽象、冲突判断、筛选状态和快照 version 1 都用无类型的 `List<Integer>`。在 26.3 原生值中，键盘 A–E 的 `4..8` 会与鼠标 4–8 的 `4..8` 重叠。只把 `KEYSYM` 替换为 `KEYBOARD` 会产生可编译但错误的显示、搜索、冲突、筛选、导入导出和第三方绑定行为。
5. 建议增加一个集中式输入兼容层：内部仍以 26.2 的稳定键码空间作为 `KeyBinding` 与快照 version 1 的规范编码，通过稳定的 `InputConstants.Key#getName()` 在 26.2 GLFW 值和 26.3 SDL 值之间转换。这样可以保留公开方法签名、现有 0–7 鼠标约定及 version 1 快照格式。
6. 项目实际使用的 GUI 提取接口、按键事件入口、Fabric tick/按键注册 API、三个 Mixin 目标、Loader entrypoint API 和 Mod Menu API 在核对范围内仍存在且签名兼容；这些位置不应为了“顺手现代化”而修改，但必须做运行时回归。
7. GraalVM 25.0.4 已能启动项目 wrapper，Gradle 9.5.1 明确使用 JVM 25。Codex Windows 的 loopback 错误可用单次 `jdk.net.unixdomain.tmpdir=C:\Windows\Temp` workaround 越过；随后离线配置因 Loom 1.17.21 未缓存而失败，在线解析在受限网络下未完成。因此完整依赖解析、编译和客户端启动仍标为**待确认**，但不再是“缺少 JDK”阻碍。

## 2. 证据来源

### 2.1 官方 Minecraft/Fabric 资料

- [Fabric：Minecraft 26.3 迁移说明](https://fabricmc.net/2026/09/15/263.html)：发布时建议 Loom 1.17、Gradle 9.6.0、Loader 0.19.5；明确说明 GLFW 被 SDL 取代、输入常量（包括全部鼠标键）改变，且自定义文本输入控件需要同步 `TextInputManager`。
- [Fabric 官方 26.3 示例 gradle.properties](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/gradle.properties)：当前值为 Minecraft 26.3、Loader 0.20.1、Loom 1.19-SNAPSHOT、Fabric API 0.165.0+26.3。
- [Fabric 官方 26.3 示例 build.gradle](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/build.gradle)：Java release/source/target 均为 25。
- [Fabric 官方 26.3 示例 Gradle Wrapper](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/gradle/wrapper/gradle-wrapper.properties)：Gradle 9.8.2。
- [Fabric 官方 26.3 示例 fabric.mod.json](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/src/main/resources/fabric.mod.json)：`fabricloader >=0.20.1`、`minecraft ~26.3`、`java >=25`。
- [Loom 1.19.3 官方 Maven 发布物](https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.19.3/) 与 [Gradle 插件 marker](https://maven.fabricmc.net/net/fabricmc/fabric-loom/net.fabricmc.fabric-loom.gradle.plugin/1.19.3/)；[Loom 1.19 发布说明](https://github.com/FabricMC/fabric-loom/releases)。
- [Fabric Loader 0.20.1 发布说明](https://github.com/FabricMC/fabric-loader/releases) 与 [官方 Maven 发布物](https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.20.1/)。
- [Fabric API 0.165.0+26.3 发布说明](https://github.com/FabricMC/fabric-api/releases/tag/0.165.0%2B26.3)。
- [Fabric API 26.3 `KeyMappingHelper`](https://github.com/FabricMC/fabric-api/blob/26.3/fabric-key-mapping-api-v1/src/client/java/net/fabricmc/fabric/api/client/keymapping/v1/KeyMappingHelper.java) 与 [26.3 `ClientTickEvents`](https://github.com/FabricMC/fabric-api/blob/26.3/fabric-lifecycle-events-v1/src/client/java/net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents.java)。
- [Fabric Loader 0.20.1 `FabricLoader`](https://github.com/FabricMC/fabric-loader/blob/0.20.1/src/main/java/net/fabricmc/loader/api/FabricLoader.java)：`getEntrypointContainers`、`isModLoaded`、`getConfigDir` 的现有签名。
- [Mojang 官方 26.3 版本元数据](https://piston-meta.mojang.com/v1/packages/5df1795e16eea19d75b923d66ecca9ce183353c5/26.3.json)：`javaVersion.majorVersion = 25`；官方客户端 JAR SHA-1 为 `e877b6a07acd633fb3bb475002175cec036e7b87`。

### 2.2 第三方发布物

- MaLiLib：[CurseForge 26.3 正式文件 0.30.2](https://www.curseforge.com/minecraft/mc-mods/malilib)；[上游 GitHub 26.3 发布线](https://github.com/sakura-ryoko/malilib/releases)。构建坐标 `fi.dy.masa.malilib:malilib-fabric-26.3:0.30.2` 的 POM 地址已只读验证返回 HTTP 200。
- CommandKeys：[GitHub Fabric 26.3 发布 3.104.1](https://github.com/TerminalMC/CommandKeys/releases)；Modrinth 发布物版本为 `3.104.1+26.3`。构建坐标 `maven.modrinth:commandkeys:3.104.1+26.3` 已只读验证。
- Mod Menu：[Modrinth 21.0.0（26.3）](https://modrinth.com/mod/modmenu/version/kyy7dbrZ)；[GitHub v21.0.0](https://github.com/TerraformersMC/ModMenu/releases/tag/v21.0.0)。构建坐标 `com.terraformersmc:modmenu:21.0.0` 已只读验证。

### 2.3 二进制对比范围

对 Mojang 官方 26.2/26.3 客户端 JAR 使用 Java 25 `javap` 比较了项目实际引用的类，包括：

- `InputConstants`、`InputConstants.Key`、`InputConstants.Type`；
- `KeyMapping`、`Options`、`KeyEvent`、`MouseButtonEvent`、`InputWithModifiers`；
- `GuiGraphicsExtractor`、`AbstractWidget`、`AbstractButton`、`ContainerObjectSelectionList.Entry`、`EditBox`、`StringWidget`、`Checkbox`；
- `ControlsScreen`、`OptionsList`、`HeaderAndFooterLayout`、`FrameLayout`、`OptionsSubScreen`；
- `Minecraft`、`Language`。

同时比较了当前项目所用 26.2 依赖与建议 26.3 依赖中 Visual Keymap 实际调用的 MaLiLib、CommandKeys、Mod Menu 类签名。签名检查不能替代完整编译和运行时测试。

### 2.4 本地 26.3 反编译源码复核

目标源码根目录为 `E:/02_Development/01_Workspace/04_MinecraftCodeDigging/26.3`。以下结论已从方法体或字段声明直接确认，不再仅依赖 `javap`：

- `InputConstants.java:198-213,240-266,309-357`：unknown 是 `KEYBOARD/0`，类型为 `KEYBOARD` 与 `MOUSE`，字符串名可经 `getKey(String)` 解析；鼠标 left/middle/right 为 1/2/3，A–E 与 mouse 4–8 确实共享数值 4–8但类型不同。
- `KeyMapping.java:22-31,82-105,138-176`：`key`、`order` 字段仍存在；构造、`setKey()`、`resetMapping()`、默认键和 typed-key 匹配语义与方案中的适配假设一致。
- `Options.java:711-769`：`keyDebugModifier`、`debugKeys` 与 `keyMappings` 仍存在，debug bindings 使用 `KEYBOARD` scan code。
- `OptionsSubScreen.java:67-68` 与 `Options.java:1821-1886`：屏幕移除会调用 `Options.save()`，该方法以 UTF-8 直接重写 options 文件；26.2 官方类字节码确认相同的父类调用。因此原版键位保存调用路径不再是未知项，实际 I/O 结果仍需运行验证。
- `ControlsScreen.java:43-54` 与 `OptionsList.java:38-57`：`addOptions()` 中仍恰有一处 `addSmall(OptionInstance<?>...)` 调用；另一个按钮行调用的是 widget overload，现有 `@Redirect` descriptor 仍能唯一指向目标调用。
- `HeaderAndFooterLayout.java:16-18,102-117` 与 `FrameLayout.java:32-39`：私有 `contentsFrame` 及 `setMinHeight()` 仍存在。
- `GuiGraphicsExtractor.java:181-205,1338-1340`、`AbstractButton.java:35-43`、`AbstractSelectionList.java:363,438-456`、`AbstractWidget.java:59-63`：Visual Keymap 使用的 `fill`、`textRendererForWidget`、`extractContents`、entry `extractContent` 和子控件 `extractRenderState` 均存在。
- `KeyEvent.java:12-20`、`MouseButtonEvent.java:7-18`、`InputWithModifiers.java:12-30`：项目当前调用的 `InputConstants.getKey(KeyEvent)`、`MouseButtonEvent.button()` 与 `isEscape()` 路径仍有效。
- `EditBox.java:367-368,471-476,536-553` 与 `Minecraft.java:2875-2885`：原版 `EditBox` 自身处理 preedit、文本输入区域和焦点通知。因此 Visual Keymap 复用 `EditBox` 时无需增加自定义 `TextInputManager` 调用；IME 的真实平台行为仍必须人工验证。

这些源码证据提高了静态结论置信度，但不能证明 Mixin 在 remap 后成功应用、渲染视觉效果正确或输入硬件行为无回归。

## A. 构建环境和依赖迁移

### A.1 依赖版本对照表

| 项目 | 当前 26.2 | 建议 26.3 | 证据与说明 | 最小修改位置 |
| --- | --- | --- | --- | --- |
| Java | 25 | 25 | Mojang 26.3 元数据和 Fabric 官方示例均为 25 | `build.gradle:55-70` 无需改；开发机/CI 使用 JDK 25 |
| Minecraft | 26.2 | 26.3 | Mojang 正式版本；Fabric 官方 26.3 分支 | `gradle.properties:5`；`fabric.mod.json:30` |
| Gradle | 9.5.1 | 9.8.2 | Fabric 官方当前 26.3 wrapper | `gradle/wrapper/gradle-wrapper.properties:3` |
| Fabric Loom | 1.17-SNAPSHOT | 1.19.3 | 官方当前示例写 `1.19-SNAPSHOT`；1.19.3 是同一发布线的稳定发布物，避免可变快照 | `build.gradle:2` |
| Fabric Loader | 0.19.3 | 0.20.1 | 官方当前示例与 Loader 正式发布 | `gradle.properties:6`；`fabric.mod.json` 由占位符同步 |
| Fabric API | 0.152.2+26.2 | 0.165.0+26.3 | 官方 26.3 最新发布（截至调查日） | `gradle.properties:13` |
| MaLiLib | `26.2:0.29.0` | `26.3:0.30.2` | 26.3 Fabric 正式文件；当前 Maven 坐标形式可解析 | `gradle.properties:14` |
| CommandKeys | `3.103.0+26.2` | `3.104.1+26.3` | GitHub/Modrinth 26.3 Fabric 正式版 | `gradle.properties:15` |
| Mod Menu | `20.0.0-beta.3` | `21.0.0` | 官方 26.3 正式版 | `gradle.properties:16` |

版本结论以 2026-10-29 为准。执行迁移时应先重新检查发布页，若已有更新的 26.3 补丁版，需单独比较变更后再决定是否替换，不能自动追随 `latest`。

### A.2 构建文件的最小修改

1. `build.gradle:2`
   - 旧：`net.fabricmc.fabric-loom` `1.17-SNAPSHOT`。
   - 新：建议固定 `1.19.3`。
   - 原因：官方当前示例已进入 1.19 线；固定正式版避免 SNAPSHOT 随时间变化。
   - 风险：低。项目没有自定义 Loom DSL；仍需运行全部 Gradle 任务确认。

2. `gradle/wrapper/gradle-wrapper.properties:3`
   - 旧：Gradle 9.5.1。
   - 新：Gradle 9.8.2。
   - 原因：与 Fabric 官方当前 26.3 示例一致。
   - 风险：低到中。需检查配置缓存、发布任务和 IDE 导入；项目当前构建脚本较简单。

3. `gradle.properties:5-6,13-16`
   - 只更新上表版本值，不改变依赖 scope、仓库或 artifact 坐标结构。
   - `compileOnly` + `runtimeOnly` 的第三方依赖安排保持不变，避免迁移时改变打包边界。

4. `src/main/resources/fabric.mod.json:30`
   - `minecraft` 从 `~26.2` 改为 `~26.3`。
   - Loader 下限会由 `loader_version` 资源展开自动变成 `>=0.20.1`。
   - 官方示例还声明 `java >=25`，但当前模组元数据未声明 Java。Minecraft 26.3 本身已要求 Java 25，因此本阶段不把新增 Java 依赖声明列为必要迁移项，避免混入元数据增强；如维护者希望补充，应单独审查。

### A.3 构建验证

- `gradlew --version`：确认 JVM 25、Gradle 9.8.2。
- `gradlew dependencies` / `dependencyInsight`：分别确认 Loader、Fabric API、MaLiLib、CommandKeys、Mod Menu 的实际解析版本，且无旧 26.2 artifact 残留。
- `gradlew clean build`：主代码、Mixin AP、资源展开、remap、sources JAR 全部成功。
- `gradlew runClient`：分别运行无可选依赖和全依赖配置。
- 检查产物 `fabric.mod.json`：`minecraft ~26.3`、`fabricloader >=0.20.1`，版本占位符均已展开。

**待确认：**本轮已确认 Gradle 9.5.1 的 Launcher/Daemon JVM 均为 GraalVM 25.0.4，并验证 loopback workaround 可使 Gradle进入项目配置。离线配置因 Loom 1.17.21 未缓存失败，在线解析在当前受限网络环境未完成，故仍未取得成功构建或启动结果。建议在普通 Windows Terminal/PowerShell 中先确认系统 `Path` 指向 JDK 25，再执行项目原始 wrapper；若必须从 Codex Windows 子进程执行，可仅在命令进程设置 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp`。第一次真实 `clean build` 的所有错误仍应作为迁移输入。

## B. Minecraft/Fabric API 适配

### B.1 旧 API 与新 API 对照

| 26.2 | 26.3 | 项目影响 |
| --- | --- | --- |
| GLFW 负责窗口/输入 | SDL 负责窗口/输入 | 不能再导入 `org.lwjgl.glfw.GLFW`；不能假定旧数字仍代表同一键 |
| `InputConstants.Type.KEYSYM` | `InputConstants.Type.KEYBOARD` | `KeyBinding`、`KeymapSnapshot`、`ImportPreviewScreen` 直接编译失败 |
| `InputConstants.UNKNOWN` = keyboard `-1` | `InputConstants.UNKNOWN` = keyboard `0` | `GLFW_KEY_UNKNOWN` 不能机械替成 `-1`；否则创建的不是原版 unknown 对象 |
| A = 65 | A = 4 | 原生整数语义变化 |
| 1 = 49 | 1 = 30 | 原生整数语义变化 |
| F12 = 301 | F12 = 69 | 原生整数语义变化 |
| Left Ctrl = 341 | Left Ctrl = 224 | 原生整数语义变化 |
| 鼠标 left/right/middle = 0/1/2 | 鼠标 left/middle/right = 1/2/3 | 不能用统一 `+1` 或范围判断转换 |
| 鼠标 4..8 = 3..7 | 鼠标 4..8 = 4..8 | 与 SDL 键盘 A–E 的值重叠 |
| `KeyEvent.scancode()` | `KeyEvent.keycode()`，新增 `shortcutKey()` | 本项目不直接调用这些方法；`InputConstants.getKey(KeyEvent)` 与 `isEscape()` 仍可用 |
| `com.mojang.blaze3d.pipeline.RenderPipeline` | `com.mojang.renderpearl.api.pipeline.RenderPipeline` | `GuiGraphicsExtractor` 部分重载的参数包变化；本项目只用无 pipeline 的 `fill` 等方法，无直接 import |

`InputConstants.Key#getType()`、`getValue()`、`getName()`、`getDisplayName()` 均仍存在。对注册键名的二进制检查显示：26.2 的 129 个键名中，除 `key.keyboard.f25` 外均仍在 26.3；26.3 另增加 127 个 SDL 键名。稳定键名可作为兼容映射依据。

### B.2 必须建立的键码兼容层

#### 目标

- 保持 `KeyBinding#getKeyCodes()`、`getModifierKeyCodes()`、`getFullKeyCodes()` 的签名不变。
- 保持 26.2 对外整数语义：鼠标仍为 `0..7`，原有键盘仍为对应 GLFW 值。
- 保持 `KeymapSnapshot.version == 1`、JSON 字段和已有 26.2 快照可导入。
- 在操作 Minecraft 26.3、MaLiLib 0.30.2、CommandKeys 3.104.1 时，边界处转换为实际 SDL `InputConstants.Key`。
- 不改变 modifiers 当前“写出但导入不比较、不应用”的既有行为。

#### 建议的最小实现

增加一个集中式兼容类（建议内部包名如 `dev.xef2.visualkeymap.internal.InputCodeCompat`），提供以下职责；具体公开性需要在执行前审查，避免无意扩大 API：

1. `InputConstants.Key -> 26.2 规范整数`
   - 鼠标按 `key.getName()` 映射到 0–7；不能按 26.3 数值范围判断。
   - 26.2 已有键盘按稳定键名映射回原 GLFW 值。
2. `26.2 规范整数 -> InputConstants.Key`
   - 0–7 按旧规则解释为鼠标，并按名称取得 26.3 `InputConstants.Key`。
   - 其余按旧 GLFW 值查到键名，再用 `InputConstants.getKey(name)` 取得 26.3 键。
3. 显示与筛选一律通过同一转换层，禁止再次散落 `0..7`、`±100` 或 SDL 数值范围判断。
4. 为外部 `VisualKeymapApi` 实现提供可使用的非破坏性转换入口，或至少在 API 文档中明确仍须返回 26.2 规范整数。不得把抽象方法改成新类型，否则会破坏已发布扩展接口。

这是迁移兼容代码，不是快照格式升级。若改成直接导出 SDL 数字，则 version 1 文件无法区分键盘 A（4）和鼠标 4（4），也无法自动判断一个 version 1 文件来自 26.2 还是 26.3，因此不可接受。

**待确认：**

- `key.keyboard.f25` 在 26.3 注册表中不存在。建议至少保留其 version 1 数字 `314` 的读取、显示和再次导出，不得静默改成其他键；实际物理触发是否能在 SDL 后端保持，需要带相应硬件或合成事件人工验证。若无法触发，应记录为上游输入能力限制并单独决策。
- 26.3 新增而 26.2 不存在的 127 个键没有既有规范整数。执行前需决定非冲突的扩展编码或仅在内部使用 typed key；不能回退为裸 SDL 值并与旧编码混用。该决定不得改变已有 26.2 键和快照的语义。

### B.3 需要修改的源码位置

| 位置 | 当前行为 | 建议最小修改 | 原因与潜在影响 | 风险/验证 |
| --- | --- | --- | --- | --- |
| `VisualKeymap.java:17,54-58`，`onInitializeClient()` | 导入 GLFW；用 `GLFW_KEY_UNKNOWN` 创建打开界面键位 | 删除 GLFW import；使用 `InputConstants.UNKNOWN.getValue()`，或显式使用 `Type.KEYBOARD` + unknown 值 | GLFW 已从 26.3 移除；`-1` 已不再是原版 unknown | 中；验证默认未绑定、Controls 中显示“未分配”、绑定后热键打开界面 |
| 新增内部输入兼容类 | 无集中转换 | 建立键名驱动的双向表和鼠标显式映射 | 防止键盘/鼠标数值碰撞，维持公开整数与快照格式 | 高；对全部共同键名做双向单元测试 |
| `api/KeyBinding.java:85-89`，`getDisplayNameFromCode()` | 0–7 判鼠标，其余 `KEYSYM` | 改为通过兼容层从规范整数取得实际 typed key，再显示 | `KEYSYM` 删除；原生 SDL 值范围不能区分类型 | 高；验证显示、搜索、modifier、冲突列表 |
| `api/MinecraftImpl.java:48-58` | 直接返回 `Key#getValue()` | `key` 和 debug modifier 均编码为 26.2 规范整数 | 直接返回 SDL 值会破坏全部整数消费者 | 高；覆盖普通键、A–E、鼠标 4–8、debug modifier |
| `gui/screen/VisualKeymapScreen.java:165-178,246-249` | 用 `key.getValue()` 筛选并用 `Integer selectedKeyCode` 保存选中键 | 筛选和选中状态改用规范整数或 typed key；不得仅保存 SDL 裸值 | A–E 与鼠标 4–8 会互相筛选/同时高亮 | 高；逐一点击重叠值对应的键帽 |
| `gui/widget/KeyboardWidget.java:185-188` | 选中边框比较 `selectedKeyCode == key.getValue()` | 与主屏使用同一规范编码或直接比较 typed key | 防止键盘和鼠标键同时高亮 | 高；与上一项联合验证 |
| `KeymapSnapshot.java:84-88`，`ImportMatch#getKeysFromEntry()` | version 1 的 0–7 作为鼠标，其余 `KEYSYM` | 通过兼容层解码为 26.3 typed key；保持 `version = 1` 和字段不变 | 保持 26.2 快照可导入；`KEYSYM` 已删除 | 高；使用 26.2 真实导出 golden files 验证 |
| `gui/screen/ImportPreviewScreen.java:129-148`，`buildChangeText()` | 所有新键都用 `KEYSYM` 显示 | 通过兼容层显示 version 1 规范值；不得顺带改变 modifiers 或选择逻辑 | 编译适配；预览必须与实际应用键一致 | 中；特别记录 26.2 鼠标预览实际表现，避免顺手修 Bug |
| `integration/CommandKeysIntegration.java:84-89` | `InputConstants.Key#getValue()` 直接进入公开整数列表 | typed key 先转为规范整数；`setBoundKeys()` 保持传 typed key 给 CommandKeys | CommandKeys API 签名没变，但底层值已是 SDL | 高；limit key + 主键、鼠标、保存重启 |
| `integration/MaLiLibIntegration.java:64-84` | 负值统一 `+100`；0–7 统一 `-100` | 先按 MaLiLib 0.30.2 的键类型/明确范围转换为 typed key，再与规范整数互转；写入时用 `Key#getType()`，不能按值范围判断 | 0.30.2 的 `getKeys()` 已是 scan code 语义；鼠标为 `-99..-92`，且新增 gamepad 负值，原 `key < -1` 会误处理 gamepad | 高；键盘、8 个鼠标键、组合键、gamepad 存在时的非破坏行为 |

`KeymapSnapshot.fromBindings()` 本身不需要改 schema；只要所有 provider 的 `getKeyCodes()` 已返回规范整数，导出的 version 1 文件即可保持原语义。

### B.4 已核实无需源码修改的 Minecraft/Fabric 位置

下表结论已由 26.3 反编译源码复核为“当前声明和实现路径兼容”，不是免除运行测试。

| 位置 | 核实结果 | 验证要求 |
| --- | --- | --- |
| `KeyLayoutHelper.KeyLayout#getKey()` | `InputConstants.getKey(String)` 与已有布局所用键名仍存在；只需由新编码层处理其数值 | 检查全部键帽，尤其 Win/GUI、Menu、数字小键盘、鼠标 |
| `VisualKeymapScreen.mouseClicked/keyPressed/keyReleased` | `MouseButtonEvent.button()`、`KeyEvent`、`InputConstants.getKey(KeyEvent)`、`isEscape()` 的调用签名仍可用 | 实测键盘组合、释放结束、Esc、8 个鼠标键 |
| `KeyboardWidget` 渲染 | 项目使用的 `GuiGraphicsExtractor.fill(int...)`、`textRendererForWidget()`、`AbstractButton.extractContents()` 未变 | 多 GUI scale、窗口尺寸、hover/tooltip/narration |
| `KeybindsListWidget`、`ImportPreviewScreen` 条目渲染 | `Entry.extractContent()` 和子控件 `extractRenderState()` 未变 | 滚动、点击、裁剪、checkbox、tooltip |
| `VisualKeymapScreen` 的 `EditBox` | 使用原版 `EditBox`，不是自定义文本输入控件；26.3 源码中 `EditBox.preeditUpdated()`、渲染路径和焦点变更已直接调用原版 `TextInputManager` | 无需手动调用 `TextInputManager`；必须实测中英文输入、IME 预编辑、焦点切换 |
| `MinecraftImpl` 的原版按键访问 | `Options.keyMappings`、`debugKeys`、`keyDebugModifier` 与 `KeyMapping.setKey/resetMapping/isDefault/getDefaultKey` 均仍存在 | 运行时改键、重置、冲突和重启 |
| 原版按键持久化 | `VisualKeymapScreen.removed()` 调用 `super.removed()`；26.3 `OptionsSubScreen.removed()` 调用 `Options.save()`，26.2 字节码同样如此 | 无需给 `MinecraftImpl.save()` 新增重复写盘；验证完成、Esc、屏幕替换与 I/O 错误路径 |
| Fabric API | `KeyMappingHelper.registerKeyMapping()`、`ClientTickEvents.END_CLIENT_TICK` 签名仍存在 | 启动与热键触发 |
| Fabric 26.3 已移除的 API | 移除项为燃料/堆肥、酿造、方块转换等；项目未引用 | 无修改 |

### B.5 Mixin 核查

| Mixin | 26.3 源码/二进制证据 | 迁移结论 | 风险与验证 |
| --- | --- | --- | --- |
| `ControlsScreenMixin` | `ControlsScreen.java:43-54` 显示目标 `addSmall(OptionInstance<?>...)` 调用仍存在且在该方法中唯一；二进制 descriptor 也未变 | 不改注入方法和 `@Redirect` target | 中；Mixin 对 remap、调用次数和顺序敏感，需 `defaultRequire=1` 启动验证按钮与原控件 |
| `HeaderAndFooterLayoutAccessor` | `HeaderAndFooterLayout.java:16-18` 仍声明私有 `FrameLayout contentsFrame` | 不改 accessor | 中；实测布局高度与窗口缩放 |
| `KeyMappingAccessor` | `KeyMapping.java:22-31` 仍有 `protected InputConstants.Key key` 和 `private final int order` | 不改 accessor | 中；实测读取键、排序和 debug modifier |

不应把这些 Mixin 改写成反射、access widener 或其他注入形式；那属于迁移范围外的重构。执行时开启 Mixin 调试日志，确认三项均应用且没有目标计数变化。

### B.6 配置持久化与 entrypoint

- `ModConfig` 使用 `FabricLoader.getConfigDir()`、Gson、`FileReader`/`FileWriter`。Loader 0.20.1 仍提供 `getConfigDir()`；无版本适配代码需求。
- 配置文件名、字段、默认值、平台默认字符集及错误处理保持不变。不得借迁移改 UTF-8、校验或恢复策略。
- Loader 0.20.1 仍提供 `getEntrypointContainers(String, Class)`、`isModLoaded(String)`；`visualkeymap` entrypoint key、`VisualKeymapApi` 方法 descriptor、provider 名和初始化顺序均不改。
- 外部 provider 的整数键码语义必须由兼容层保持为 26.2 规范空间；这是保持扩展接口行为的必要条件。应增加一个最小外部测试模组，不能只验证内置实现。

## C. 第三方模组兼容适配

### C.1 MaLiLib 0.30.2

#### 发布与依赖

- 正式 Fabric 文件：`malilib-fabric-26.3-0.30.2.jar`。
- Gradle 值：`malilib_version=26.3:0.30.2`；现有依赖声明无需改结构。
- 发布 JAR 元数据声明 Minecraft `>=26.3 <26.4`，环境为 client。

#### API 对比

Visual Keymap 使用的以下签名均保留：

- `ConfigManager.getInstance()`、`saveAllConfigs()`；
- `InputEventHandler.getKeybindManager()`；
- `IKeybindManager.getKeybindCategories()`、`updateUsedKeys()`；
- `KeybindCategory.getModName()`、`getHotkeys()`；
- `IHotkey.getKeybind()` 及其配置显示/注释方法；
- `IKeybind.getKeys()`、`clearKeys()`、`addKey()`、`isModified()`、`resetToDefault()`。

因此不建议替换 MaLiLib 集成架构。必要修改集中在 `MaLiLibIntegration.MaLiLibKeyBinding#getKeyCodes()` 与 `setBoundKeys()`。

#### 键码变化

MaLiLib 0.29.0 的 `KeybindMulti` 字段语义是 `keyCodes`；0.30.2 改为 `scanCodes`，与 SDL 迁移一致。0.30.2 仍用 `-99..-92` 表示八个鼠标键，但新增了更低的 gamepad 负数区间。当前 `key < -1 ? key + 100 : key` 会把 gamepad 误当鼠标，必须改为显式鼠标判定。

建议保持原有 keyboard/mouse 功能：

1. 从 MaLiLib 读取时，明确识别 `-99..-92` 为鼠标并转换为 26.3 `Type.MOUSE`；其他键盘 scan code 转为 `Type.KEYBOARD`。
2. 再通过公共兼容层输出 26.2 规范整数。
3. 写入时依据 `InputConstants.Key#getType()` 判断鼠标，鼠标按 MaLiLib 约定减 100；键盘写当前 SDL scan code。
4. 保存路径仍调用 `saveAllConfigs()` 和 `updateUsedKeys()`。

**待确认：**MaLiLib 0.30.2 新增 gamepad 支持。Visual Keymap 26.2 没有 gamepad 功能，迁移不得擅自把它做成新功能，也不得把已有 gamepad 绑定损坏。最低要求是读取时不误转换、不在用户未编辑该绑定时覆盖；编辑含 gamepad 的热键应如何呈现和保留，需要真实实例验证后单独决策。

### C.2 CommandKeys 3.104.1+26.3

#### 发布与依赖

- 正式 Fabric 版本：`3.104.1+26.3`。
- Gradle 值：`commandkeys_version=3.104.1+26.3`。
- 发布 JAR 声明 Java `>=25`、Minecraft `>26.2 <27`、Loader `>=0.18.5`、Fabric API 任意版本。

#### API 对比

与 3.103.0+26.2 对比，Visual Keymap 使用的以下字段/方法 descriptor 未变：

- `CommandKeys.profile()`；
- `Profile.macroMap`、`Profile.name`、`setKey()`、`setLimitKey()`；
- `Macro.name`、`getMessages()`；
- `Keybind.getKey()`、`getLimitKey()`；
- `Message.string`；
- `Config.save()`。

最小修改只是在 `getKeyCodes()` 中把 CommandKeys 返回的 26.3 typed key 转为 26.2 规范整数；写入仍使用 `Profile.setKey/setLimitKey`。活动 profile、limit key 顺序、空 provider 导致不导出等既有行为保持不变。

### C.3 Mod Menu 21.0.0

- 正式版本：21.0.0，明确支持 Minecraft 26.3 Fabric/Quilt client。
- Gradle 值：`mod_menu_version=21.0.0`。
- `ModMenuApi#getModConfigScreenFactory()` 与 `ConfigScreenFactory#create(Screen)` 相比 20.0.0-beta.3 descriptor 完全相同。
- `compact/modmenu/ModMenuImpl.java` 和 `fabric.mod.json` 中 `modmenu` entrypoint 无需源码适配。
- 仍需分别在安装和未安装 Mod Menu 时启动验证；不能因 compileOnly 类存在而跳过“未安装”场景。

### C.4 依赖不可用时的保留方案

当前三项依赖均已有正式 26.3 版本，因此本轮不触发替代实现。若执行时某发布物临时撤回或 Maven 不可用，处理顺序必须是：

1. 使用同一作者的正式发布页/CDN artifact，并保持相同版本与校验值；
2. 将已验证的发布 JAR 放入受控本地 Maven/CI 缓存，仍按 compileOnly/runtimeOnly 使用；
3. 若 API 确实移除，先用隔离 adapter 或针对该版本的兼容实现保持功能；
4. 只有在维护者另行批准后才能改变支持范围。不得通过删除 integration 类、去掉入口或不加载 provider 来“完成”迁移。

## D. 测试及验证

### D.1 建议增加的自动化测试

当前仓库没有 Java 测试源码或游戏测试。迁移至少需要以下回归保护：

1. 输入兼容映射单元测试
   - 对 26.2/26.3 共同键名逐项验证 `26.3 key -> 26.2 code -> 26.3 key`。
   - 明确断言 A–E 与 mouse 4–8 在规范编码中不相等。
   - 精确断言 left/middle/right 和 M4–M8，禁止用统一偏移量。
   - unknown、F1–F12、数字区、导航键、modifier、数字小键盘。
   - `key.keyboard.f25` 与 26.3-only 键走明确的待确认/保留策略，不允许静默碰撞。

2. 快照 golden-file 测试
   - 读取真实 26.2 version 1 文件，匹配并还原键盘、鼠标、空绑定和 modifier 字段。
   - 26.3 导出仍为 version 1，已有键的整数值与 26.2 语义一致。
   - 保持未知 version 当前不校验、modifiers 不应用、CommandKeys provider 为空时不导出的现状。

3. provider adapter 测试
   - Minecraft、MaLiLib、CommandKeys 的读写往返。
   - MaLiLib `-99..-92` 和 gamepad 负值不能混淆。
   - 外部 `visualkeymap` entrypoint 测试模组验证公开接口 descriptor 与规范整数语义。

4. 静态/启动验证
   - Mixin apply 成功且 redirect 命中一次。
   - 可选依赖缺席时不加载其 integration；存在时 API 调用成功。

若增加测试需要改构建脚本，应在执行阶段单独列出并审查；本阶段未做任何修改。

### D.2 运行配置矩阵

| 配置 | 内容 | 主要目的 |
| --- | --- | --- |
| R0 | Visual Keymap + Fabric API | 原版功能、无可选依赖启动 |
| R1 | R0 + Mod Menu | 配置入口与返回路径 |
| R2 | R0 + MaLiLib 0.30.2 + 至少一个实际 MaLiLib 热键模组 | 多键、鼠标、保存、默认恢复 |
| R3 | R0 + CommandKeys 3.104.1+26.3 | profile、limit key、宏消息、保存 |
| R4 | 全部依赖 | provider 顺序、冲突、联合保存、类加载 |
| R5 | R0 + 外部测试模组 | `visualkeymap` entrypoint 与 `VisualKeymapApi` 兼容 |

每个配置至少测试首次启动、重启后持久化、进入世界和仅主菜单环境。

### D.3 功能回归重点

- 控制菜单 Mixin 入口、Mod Menu 入口、未绑定热键和绑定后的热键入口。
- 固定键盘布局、数字小键盘开关、鼠标键帽、搜索、冲突着色、tooltip、列表排序。
- 编辑开始/结束规则：按键按下、释放、Esc、鼠标点击、最大键数。
- A–E 与 mouse 4–8 的筛选、高亮、冲突、显示必须互不串扰。
- 原版 debug modifier 的显示、搜索、冲突和导出保持现状。
- 配置文件创建、读取、写回；无效 JSON、只读目录的行为按 26.2 记录，不顺带修复。
- 26.2 version 1 快照导入 26.3；26.3 导出后再导入；未知 provider/ID/version；null 和无效 JSON。
- 中英文搜索框、中文 IME 预编辑、焦点切换和粘贴。因为使用原版 `EditBox`，预期无需项目代码调用 `TextInputManager`，但该结论必须运行验证。
- 多分辨率、GUI scale、窗口缩放、滚动、checkbox、narration、tooltip。
- 三个 Mixin 在开发环境和打包产物中都应用成功。

完整功能清单仍以 `TEST-PLAN.md` 为准；执行时应在其现有项目上增加 SDL/规范键码专项，而不是替换第一阶段测试计划。

### D.4 风险登记

| 风险 | 等级 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| SDL 键盘/鼠标整数碰撞 | 高 | 错误筛选、冲突、显示、修改、导入导出 | 集中兼容层；禁止裸 `getValue()` 进入公开整数模型 |
| version 1 快照跨版本失真 | 高 | 用户导入后绑定到错误键 | 保留 26.2 规范编码；26.2 golden files |
| MaLiLib scan code/gamepad 语义 | 高 | 热键损坏或误改 | 显式类型/范围；真实 MaLiLib 模组验证；未知类型不覆盖 |
| 外部 `VisualKeymapApi` provider | 高 | 第三方实现行为变化 | 不改 descriptor；保持旧整数语义；外部测试模组 |
| Mixin 私有字段/调用点 | 中 | 启动失败或菜单入口消失 | 字节码已核对；开发/生产启动均验证 Mixin apply |
| GUI/RenderPearl 与 SDL 文本输入 | 中 | 渲染、焦点或 IME 回归 | 项目所用签名不变；多平台人工测试 |
| Loom/Gradle/依赖解析 | 中 | 构建失败 | 固定稳定 Loom 1.19.3；dependencyInsight；CI 冷缓存构建 |
| F25 与 26.3-only 键 | 中 | 极少数键无法准确往返或触发 | 不静默替换；保留数据；实现前确认扩展编码策略 |
| 本次未成功完整构建 | 中 | 尚有未发现的编译/运行问题 | JDK 与 Gradle JVM 已解除；在普通终端或使用已验证的 socket workaround 完成依赖解析与 clean build，并按实际错误更新方案 |

## 3. 建议执行顺序

### 3.1 Phase 3 前置门槛

正式迁移不得直接在当前仅含 `docs/` 的 `VisualKeyMap26.3` 目录中开始。开始实现前必须同时满足：

1. 将上游 Visual Keymap 源码和 `.git` 放入正式工作区，基线必须能解析为 `053e736c77b15e0727c93abed7c94c3b0fb64618`，并确认工作树中除已审查文档外没有来源不明的修改。
2. 在未修改版本和构建配置的基线上，以 JDK 25 完成至少一次 `gradlew help` 和 `gradlew clean build`。若现有 Loom snapshot 已不可解析，先保留完整错误证据；不得把未经验证的构建脚本改动与 26.3 API 适配混在同一提交中。
3. 维护者批准 26.3-only 键的非冲突扩展编码原则。最低约束为：既有 26.2 键盘整数、0–7 鼠标整数、public API 签名和 version 1 既有数据语义不变；不得使用裸 SDL 数字。建议采用带明确命名空间的稳定扩展整数，并由集中式兼容层独占转换。具体数值范围和 version 1 的跨版本行为必须在实现前记录。
4. 建立独立迁移分支和可回滚基线提交。若要为自动化测试增加测试依赖或 source set，该构建变更必须独立提交并单独审查。

### 3.2 可独立实施的工作单元与依赖

| 单元 | 内容 | 依赖 | 单元验收 |
| --- | --- | --- | --- |
| P0 | 正式源码、Git 基线和 26.2 证据归档 | 无 | HEAD 为指定提交或有可审计的文档提交；工作树干净；远端可识别 |
| P1 | JDK 25 与原始 26.2 基线构建冒烟 | P0 | `java`、`javac`、Gradle JVM 均为 25；`help`、`clean build` 成功，或形成可复现且经维护者接受的上游依赖失效记录 |
| A | 只迁移 Gradle/Loom/Loader/Fabric API/Minecraft/第三方版本 | P1 | 目标依赖可解析；`clean build` 到达实际源码编译；产物元数据正确 |
| B1 | 集中式输入兼容层及 S-01–S-07 自动化测试 | A、扩展编码决策 | 共同键、全部鼠标、unknown、F25 和数值碰撞测试通过；没有裸 SDL 值泄漏到业务层 |
| B2 | Minecraft binding、GUI、筛选、冲突、快照调用点适配 | B1 | B/N/V/L/E/C/X/S 中不依赖第三方的用例通过；version 1 golden file 往返通过 |
| C1 | MaLiLib adapter | B1、B2 | M-01–M-04、S-08 对 MaLiLib 的部分通过；其他负值和 gamepad 不被误改 |
| C2 | CommandKeys adapter | B1、B2 | K-01–K-05、S-08 对 CommandKeys 的部分通过 |
| C3 | Mod Menu 与 `visualkeymap` entrypoint/公开 API | A、B2 | MM-01–MM-03、API-01–API-05 通过或精确维持已证实的 26.2 限制 |
| V | 打包、Mixin、无依赖/全依赖启动矩阵与全量回归 | B2、C1–C3 | `clean build`、开发/生产式启动、P1–P6 和 `TEST-PLAN.md` 最低标准通过 |

同一层的 C1、C2、C3 可分别实施和验证；B2 不得绕过 B1 直接在各 GUI 类中散落 SDL 转换。每个单元失败时先停止其依赖单元，保留最小复现，再更新计划；不得用禁用 provider、空实现或删除集成来换取构建通过。

### 3.3 Git 回滚与停止策略

- 从精确基线创建专用迁移分支；不在默认分支直接试改。分支名称由维护者决定，计划不预设名称。
- P0、A、B1、B2、C1、C2、C3、V 分别形成可编译或有明确失败证据的检查点提交。测试基础设施与生产代码分开提交。
- 每次提交前记录 `git status`、执行过的命令和验收结果；生成目录、Gradle 缓存、运行目录和含用户配置的文件不得提交。
- 某单元失败时优先对该检查点使用 `git revert` 或在新分支重做；不得用 `git reset --hard` 清除无法确认归属的修改。
- 触发以下任一条件必须停止并回到计划审查：需要改变 public API/entrypoint/provider ID/snapshot schema；需要删除或禁用第三方兼容；只能通过修复既有 Bug 才能继续；26.2 参照行为与文档冲突；扩展键编码出现碰撞或不能稳定往返。

### 3.4 顺序

1. 先在不改版本的基线副本中用 JDK 25 完成一次 `gradlew help`/`build` 环境冒烟；普通 Windows Terminal 为首选。确认可构建后，才只更新 A 节版本值与 `fabric.mod.json` 的 Minecraft 范围，运行冷缓存依赖解析和 `clean build`，记录实际编译错误。
2. 建立输入兼容层及其单元测试，先覆盖 unknown、共同键名、全部鼠标键和数值碰撞。
3. 修改 `VisualKeymap.java` 的默认热键常量。
4. 修改 `KeyBinding`、`MinecraftImpl`、`VisualKeymapScreen`、`KeyboardWidget`、`KeymapSnapshot`、`ImportPreviewScreen`，使核心 UI 和快照只使用规范整数。
5. 修改 MaLiLib 与 CommandKeys adapter；保持保存、provider、ID、顺序和默认行为不变。
6. 再次执行 `clean build`，检查 Mixin 处理、资源展开和最终 JAR。
7. 按 R0/R1/R5 先验证原版、Mod Menu 和公开 entrypoint，再验证 R2/R3/R4 第三方组合。
8. 执行 `TEST-PLAN.md` 全量回归及 D 节新增 SDL 专项；把所有“待确认”逐项转为运行证据。
9. 若发现 26.2 已有 Bug，只记录并单独提案；不得在本次迁移提交中顺带修复。

## 4. 执行完成判定

只有同时满足以下条件，才能认为 26.3 迁移实现完成：

- 固定版本依赖可从冷缓存解析，Java 25 下 `clean build` 成功；
- 无可选依赖、各单独依赖和全依赖配置均可启动；
- 三个 Mixin 均成功应用且菜单/布局/按键顺序行为不变；
- 键盘、鼠标和第三方按键读写不受 SDL 数值碰撞影响；
- 26.2 version 1 快照可正确导入，26.3 仍保持既有 schema 和整数语义；
- `visualkeymap` entrypoint 和 `VisualKeymapApi` 方法签名、provider 行为保持兼容；
- 第一阶段 `TEST-PLAN.md` 与本文件新增专项均通过，或未通过项已明确标为待确认并经维护者审查。
