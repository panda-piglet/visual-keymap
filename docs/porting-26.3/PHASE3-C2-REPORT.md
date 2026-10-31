# Visual Keymap Phase 3 C2 验收报告

日期：2026-10-31

分支：`port/mc-26.3`

范围：C2-0 CommandKeys Fabric 构件修正、C2-1 CommandKeys adapter 输入编码适配、开发期运行探针和回归验证。未修改 VisualKeymapApi、version 1 schema、Minecraft/MaLiLib adapter、GUI 或 Mod Menu 集成。

## 1. 结论

本阶段结论为 **CONDITIONAL GO**，建议进入 Phase 3 C3。

C2-0 已解除：项目现在固定解析 CurseForge file `9106946` 对应的 CommandKeys `3.104.1+26.3` Fabric JAR。JAR 内 `fabric.mod.json`、manifest、Fabric entrypoint、mod id、Loader loaded-mod 列表和 `FabricLoader.isModLoaded("commandkeys")` 均已获得实际证据，不再使用此前误解析的 NeoForge JAR。

C2-1 的必要生产修改只有一处：`CommandKeysIntegration.CommandKeysKeyBinding#getKeyCodes()` 不再把 Minecraft 26.3 的 SDL 原始整数作为 Visual Keymap 规范整数返回，而是调用既有 `MinecraftInputCode.requireCanonical()`。profile 枚举、名称/说明、搜索、limit key 顺序、写入、保存、重置和空 provider 契约均保持原实现。

两个独立客户端进程已验证 adapter 的单键、limit + 主键、鼠标、未绑定、重置、消息搜索、profile 切换、`Config.save()` 和重启持久化；CommandKeys 独立加载、缺席启动、以及与 MaLiLib/Mod Menu 共存也已通过。仍未完成的是 GUI 可见交互和本地世界内的实际宏触发，因此 K-01～K-04 不能整体标记为人工 PASS。

## 2. Git 提交与修改范围

| 提交 | 范围 | 文件 |
| --- | --- | --- |
| `8ecfc0c` `build: use CommandKeys Fabric artifact` | 用固定 CurseForge file ID 替换会选错 loader 的 Modrinth 版本坐标 | `build.gradle`、`gradle.properties` |
| `e770681` `test: verify CommandKeys Fabric runtime` | 增加不进入发布 JAR 的 C2 source set、隔离 run configuration 和 Loader 探针 | `build.gradle`、`src/c2Testmod/**` |
| `0f5b3bb` `fix: preserve CommandKeys canonical input codes` | adapter 读取端改用集中式规范键码转换 | `CommandKeysIntegration.java` |
| `ebc65a5` `test: cover CommandKeys integration behavior` | 增加 C2 功能/重启探针及 CommandKeys 缺席、独立、共存运行矩阵 | `build.gradle`、`C2IntegrationProbe.java`、`OptionalDependencyProbe.java` |
| 本报告提交 | C2 报告和测试状态 | 本文件、`TEST-PLAN.md` |

生产代码差异仅为 `CommandKeysIntegration.java` 的一个 import 和一个映射调用。测试 source set 未出现在 `build/libs/visual-keymap-26.3-1.4.0.jar` 中；CommandKeys、MaLiLib 和 Mod Menu 也未被打包进发布 JAR。

## 3. C2-0：Fabric 构件核验

### 3.1 错误构件证据

此前坐标：

```text
maven.modrinth:commandkeys:3.104.1+26.3
```

实际缓存 JAR：

```text
SHA-256 8ACD1D79FC8A622FDDE826B63D82436ABEE4B6F20B3B012908D2DB6AB1532B1F
```

内容证据：

- 含 `META-INF/neoforge.mods.toml`；
- 含 `dev/terminalmc/commandkeys/CommandKeysNeoForge.class`；
- 不含 `fabric.mod.json`；
- manifest 为 `Implementation-Title: neoforge`、`Implementation-Version: 3.104.1+26.3`。

因此“类可编译”不能证明它是 Fabric 运行构件，原 C2 阻碍判断成立。

### 3.2 最终采用构件

| 项目 | 值 |
| --- | --- |
| 发布页 | [CommandKeys file 9106946](https://www.curseforge.com/minecraft/mc-mods/commandkeys/files/9106946) |
| CurseForge project ID | `873388` |
| CurseForge file ID | `9106946` |
| Maven 坐标 | `curse.maven:commandkeys-873388:9106946` |
| 版本 | `3.104.1+26.3` |
| JAR SHA-256 | `BA684B93B65C38FF799F9F19CEC2782B6FE6D582AE125E121218AD57C42177B3` |

`build.gradle` 使用 [Curse Maven](https://cursemaven.com/) 的 `exclusiveContent`，只允许 group `curse.maven` 走该仓库；file ID 固定在 `commandkeys_curse_file_id=9106946`。这不依赖个人缓存，也不会按同一版本字符串在不同 loader 构件间猜测。

实际 JAR 内容：

```text
fabric.mod.json:
  id = commandkeys
  version = 3.104.1+26.3
  environment = client
  client entrypoint = dev.terminalmc.commandkeys.CommandKeysFabric

manifest:
  Implementation-Title = fabric
  Implementation-Version = 3.104.1+26.3
  Fabric-Minecraft-Version = 26.3
```

JAR 不含 `neoforge.mods.toml` 或 `CommandKeysNeoForge`。`dependencyInsight` 实际解析结果为：

```text
curse.maven:commandkeys-873388:9106946
\--- runtimeClasspath
BUILD SUCCESSFUL
```

### 3.3 Fabric Loader 运行时证据

`runC2IntegrationClient -Pc2Stage=dependency` 的 Loader 列表明确包含：

```text
commandkeys 3.104.1+26.3
visual-keymap 1.4.0
visual-keymap-c2-test 1.0.0
```

探针在 `CLIENT_STARTED` 后调用 Loader API，写入 ignored 证据文件：

```text
status=PASS
commandkeys.loaded=true
commandkeys.id=commandkeys
commandkeys.version=3.104.1+26.3
commandkeys.name=CommandKeys
```

共存运行还记录 `malilib.loaded=true`、`modmenu.loaded=true`；独立运行记录二者为 false。由此同时满足 loaded-mod 列表和 `isModLoaded` 的动态核验要求。

## 4. CommandKeys 26.2/26.3 API 对应

基线 `gradle.properties@053e736` 使用 CommandKeys `3.103.0+26.2`；原 adapter 已通过以下 API 实现全部功能。对 `3.104.1+26.3` Fabric JAR 的 `javap -public` 检查确认这些签名仍存在：

| 原 adapter 调用 | 26.3 实际签名/字段 | 结论 |
| --- | --- | --- |
| `CommandKeys.profile()` | `public static Profile profile()` | 不变，继续读取当前活动 profile |
| `Profile.macroMap` | `public final transient Multimap<Keybind, Macro> macroMap` | 不变，继续保持既有枚举和顺序来源 |
| `Profile.name` | `public String name` | 不变，provider 内 ID/名称规则不变 |
| `Profile.setKey(...)` | `public void setKey(Macro, Keybind, InputConstants.Key)` | 不变，主键写入仍由 CommandKeys 管理 |
| `Profile.setLimitKey(...)` | `public void setLimitKey(Macro, Keybind, InputConstants.Key)` | 不变，limit key 写入仍由 CommandKeys 管理 |
| `Macro.name` | `public String name` | 不变 |
| `Macro.getMessages()` | `public List<Message> getMessages()` | 不变 |
| `Keybind.getKey/getLimitKey` | 均返回 `InputConstants.Key` | 类型不变，但 26.3 键盘数值语义已变 |
| `Message.string` | `public String string` | 不变 |
| `Config.save()` | `public static void save()` | 不变 |

真正需要适配的差异不在 CommandKeys 方法签名，而在 Minecraft 26.3 `InputConstants.Key#getValue()`：键盘值已是 SDL 后端整数，不能继续作为 Visual Keymap 26.2 规范整数公开。鼠标原生编号也不能与键盘整数直接混用。

## 5. C2-1 生产修改

位置：`src/main/java/dev/xef2/visualkeymap/integration/CommandKeysIntegration.java`，方法 `CommandKeysKeyBinding#getKeyCodes()`。

旧逻辑：

```java
.map(InputConstants.Key::getValue)
```

新逻辑：

```java
.map(MinecraftInputCode::requireCanonical)
```

行为边界：

- `UNKNOWN` 仍先被过滤，未绑定仍返回空 list；
- limit key 仍在主键之前；
- 键盘和鼠标都通过 B1/B2 的 `MinecraftInputCode`；
- 不支持的输入会明确失败，不会静默泄漏或改写 SDL 整数；
- `setBoundKeys()` 仍接收已由通用层解码的 typed `InputConstants.Key`，并调用 CommandKeys 的 `setKey/setLimitKey`；
- 没有新增 CommandKeys 专属整数表。

其他方法保持不变：

- `getKeyBindings()`：当前 `CommandKeys.profile().macroMap`；
- `getId()`：`profile.name + ":" + macro.name`；
- `getName()`：profile 翻译文本 + 可选宏名；
- `getComment()`：首消息/`...`/灰色斜体 `[+N]`；
- `getSearchableStrings()`：追加所有消息；
- `save()`：`Config.save()`；
- `resetToDefault()`：清空主键和 limit key；
- `getProviderName()`：继续使用接口默认空字符串，因此不参与 version 1 快照。

没有修改 public API 或快照 schema。

## 6. 开发期测试基础设施

新增 source set `c2Testmod` 和 run configuration `c2IntegrationClient`：

```text
gameDir = runs/mc-26.3-c2
```

新增缺席运行：

```text
noCommandKeysClient
gameDir = runs/mc-26.3-c2-no-commandkeys
```

探针 source set 不进入发布 JAR。功能探针只创建本地无害宏数据，不连接真实服务器，也不触发聊天消息或命令。

动态探针分为：

- `dependency`：构件、Loader 元数据、共存/独立加载；
- `exercise`：创建两个 profile，检查枚举、顺序、名称/comment、搜索、单键、双键、鼠标、清空/重置、保存和快照排除；
- `verify`：新客户端进程重新加载 `commandkeys.json`，检查持久化和再次枚举。

测试夹具发现两项 CommandKeys 自身规则：

1. 新建 `Macro()` 的消息 list 初始为空，测试必须通过 `addMessage()` 添加首条消息。
2. `Config.save()` 会清理只含空白消息的宏。因此“首消息空白”夹具使用首条空白、第二条非空；保存前验证 `... [+1]`，保存后宏仍可用于持久化验证。

这些只影响测试数据构造，没有修改生产 adapter。

## 7. 实际命令与结果

所有命令使用 GraalVM JDK 25.0.4、工作区 Gradle cache 和普通长路径 `TEMP/TMP`。

| 命令 | 结果 |
| --- | --- |
| `gradlew dependencyInsight --dependency commandkeys --configuration runtimeClasspath --refresh-dependencies --no-daemon` | **PASS**；解析固定 Curse file 9106946。 |
| `gradlew clean compileJava --no-daemon` | **PASS**；C2-0 后 production 编译成功。 |
| `gradlew runC2IntegrationClient -Pc2Stage=dependency --no-daemon` | **PASS**；Loader 与 `isModLoaded` 均确认 CommandKeys 3.104.1。 |
| `gradlew runC2IntegrationClient -Pc2Stage=exercise --no-daemon` | **PASS**；adapter 动态功能验证。 |
| `gradlew runC2IntegrationClient -Pc2Stage=verify --no-daemon` | **PASS**；完全重启后的配置持久化验证。 |
| `gradlew runC2IntegrationClient -Pc2Stage=dependency -PexcludeMaLiLibRuntime -PexcludeModMenuRuntime --no-daemon` | **PASS**；CommandKeys 独立加载。 |
| `gradlew runNoCommandKeysClient -PexcludeCommandKeysRuntime --no-daemon` | **PASS**；CommandKeys 缺席时 Visual Keymap、MaLiLib、Tweakeroo、Mod Menu 正常启动。 |
| `gradlew runNoMaLiLibClient -PexcludeMaLiLibRuntime --no-daemon` | **PASS**；C1 可选依赖缺席矩阵未回归，CommandKeys 同时正常加载。 |
| `gradlew clean test --no-daemon` | **PASS**；JUnit 7 tests，0 failure/error/skipped。 |
| `gradlew clean build --no-daemon` | **PASS**；`:test` 实际执行，8 tasks completed。 |

JUnit 组成：

- `CanonicalInputCodeTest`：1；
- `MaLiLibInputCodeTest`：6。

发布 JAR：

```text
build/libs/visual-keymap-26.3-1.4.0.jar
SHA-256 A7CC2D7A3708A6B0BEBAADC3D6E25B3049C52C1994EE4624011C68BE6B0EC0AD
```

## 8. K-01 至 K-05

| 用例 | 状态 | 实际证据 | 尚未覆盖 |
| --- | --- | --- | --- |
| K-01 活动 profile 枚举 | **PARTIAL** | 动态切换 `C2 Alpha`/`C2 Beta`；adapter 分别只返回 4/1 条当前 profile macroMap 记录，ID 和来源顺序正确。 | GUI 中切 profile 后重新打开界面的可见结果仍 **NOT TESTED**。 |
| K-02 名称与 comment | **PARTIAL** | 非空宏名、空宏名、首消息空白、两条消息均由真实 CommandKeys 对象验证；分别得到可选名称、`... [+1]`、`alpha primary [+1]`。 | GUI 行、tooltip 颜色/样式仍 **NOT TESTED**。 |
| K-03 消息搜索 | **PARTIAL** | `containsSearchText()` 可通过第二条消息及多词命中，且不泄漏另一 profile 的消息；重启后仍命中。 | GUI 文本框输入、高亮和列表排序仍 **NOT TESTED**。 |
| K-04 编辑、保存、重启 | **PARTIAL** | J=`[74]`；Left Ctrl + F1=`[341,290]`，顺序为 limit→main；左键=`[0]`；清空得到 `[]`；重置清除主键/limit；`Config.save()` 后新进程语义保持。 | GUI 按住/释放状态机、实际世界内宏触发仍 **NOT TESTED**。 |
| K-05 不参与快照 | **PASS（自动化）** | provider 仍为空；修改前后及重启后 `KeymapSnapshot.fromBindings(List.of(api)).bindings()` 均为 0。 | 无。 |

额外动态证据：

```text
single.canonical=[74]
single.raw=[key.keyboard.j]
combination.canonical=[341, 290]
combination.raw=[key.keyboard.left.control, key.keyboard.f1]
mouse.canonical=[0]
mouse.raw=[key.mouse.left]
persisted.unbound=[]
snapshotBindings=0
```

## 9. 回归检查

| 范围 | 状态 | 证据 |
| --- | --- | --- |
| B1 规范键码 | **PASS** | CanonicalInputCode JUnit 继续通过；CommandKeys 不再直接返回 `getValue()`。 |
| Minecraft 原生键位 | **STATICALLY VERIFIED / build PASS** | 本阶段未改 Minecraft provider/GUI；clean build 通过。未重复 B2 人工 GUI 全矩阵。 |
| MaLiLib | **PASS（启动/既有 JUnit）** | MaLiLib JUnit 6 项通过；共存启动和无 MaLiLib 启动均通过。未重复 C1 人工热键项目。 |
| SDL 文件对话框 | **NOT RETESTED** | 本阶段无相关改动；沿用 B2.5 证据。 |
| 真实 26.2 Minecraft/MaLiLib 快照 | **NOT RETESTED** | 本阶段未改快照/Minecraft/MaLiLib 路径；沿用 B2.5/C1 证据。 |
| public API/schema | **STATICALLY VERIFIED** | `src/main/.../api`、`KeymapSnapshot` 和 schema 均无 C2 差异。 |

生产源码扫描确认：`CommandKeysIntegration` 已没有 `InputConstants.Key#getValue()`。SDL/规范转换位于 `MinecraftInputCode`；CommandKeys adapter 只负责其 typed key 读写边界。

## 10. Architecture Review

结论：**未新增生产架构债务**。

- 没有在 GUI、快照、排序、冲突检测或通用业务层增加 CommandKeys 条件分支；
- 没有复制 MaLiLib 的转换实现；
- 没有新增未来 adapter 框架；
- CommandKeys 专属 profile/macro/save 逻辑仍全部位于 `CommandKeysIntegration`；
- 规范键码转换复用 B1/B2 `MinecraftInputCode`；
- 依赖存在/缺席只由既有 `FabricLoader.isModLoaded("commandkeys")` 边界控制；
- 新增复杂代码均在开发期 source set，不进入发布 JAR。

测试配置增加了两个通用运行时排除开关 `excludeCommandKeysRuntime` 和 `excludeModMenuRuntime`，仅用于验证可选依赖组合，不改变发布 metadata 或生产初始化。

## 11. 剩余风险与人工测试

1. **GUI 可见行为：NOT TESTED。** 需要人工验证 CommandKeys 行名称、comment tooltip、搜索高亮、profile 切换后重新打开、编辑捕获和重置按钮状态。当前 Windows 控制层此前无法稳定枚举 SDL Minecraft 窗口，不能把 adapter 探针冒充 GUI 证据。
2. **实际宏触发：NOT TESTED。** 需要在本地测试世界用无害文本验证单键及 limit + 主键触发；本阶段探针刻意不发送聊天/命令。
3. **真实 26.2 CommandKeys 配置跨版本：NOT TESTED。** 工作区没有维护者提供的 26.2 CommandKeys profile 原件；当前证据来自 26.3 官方构件创建并重启的真实配置对象。
4. **多个同时未绑定宏：待人工对照。** `Profile.macroMap` 是以 `Keybind.equals()` 为 key 的 `LinkedHashMultimap`；合成构造多个完全相等的未绑定 keybind 时，map entry 可能共享代表 key 对象。现有 26.2 adapter 同样依赖 `macroMap.entries()`，没有 26.2 动态证据证明其用户可见结果。本阶段没有把该现象当作迁移理由去改变枚举契约；最终回归应专门比较 26.2/26.3。
5. OSHI/WMI 警告和离线 Mojang/Mod Menu update-check 报错仍会出现在开发日志，但所有探针均到达 `CLIENT_STARTED` 并正常停止，不是 CommandKeys 适配失败。

## 12. 是否建议进入 C3

建议：**CONDITIONAL GO**。

进入 C3 的条件已满足：正确 Fabric 构件可复现解析、运行时加载、adapter 规范编码、配置持久化、可选依赖缺席和共存启动均通过；没有阻止 Mod Menu 适配的 C2 核心问题。

C3 不应把上节人工缺口改成 PASS。最终完整回归前仍需维护者执行 CommandKeys GUI 和本地世界实际触发测试，并对“多个未绑定宏”做 26.2→26.3 对照。

