# Visual Keymap Phase 3 C3 验收报告

日期：2026-10-31

分支：`port/mc-26.3`

范围：C3-A Mod Menu 21.0.0 集成；C3-B `visualkeymap` entrypoint、`VisualKeymapApi`、`KeyBinding`、公开静态方法、provider 与 version 1 快照契约。未修改生产功能代码、既有公开签名或快照 schema。

## 1. 最终结论

本阶段结论为 **CONDITIONAL GO**，建议进入 Phase 4。

C3 的可自动验证目标均已通过：Mod Menu entrypoint/factory/父屏返回链、Mod Menu 缺席启动、外部包消费方在 26.2 与 26.3 的编译和实际加载、异常 entrypoint 隔离、provider 顺序、编辑/重置/保存回调、搜索/冲突模型、version 1 快照以及既有 public/protected descriptor 对照。

条件是：C3 的真实 GUI 点击仍需人工完成；C1/C2 已保留的 GUI 与实际热键/宏触发项目也不能改写为 PASS。这些项目不要求改变 C3 架构或公开 API，因此不阻碍进入以完整回归为目标的 Phase 4，但在发布前必须收口。

## 2. 迁移前检查

- C3 开始时工作树干净，HEAD 为 C2 文档提交 `b9daaab`。
- 目标依赖为 Minecraft 26.3、Fabric Loader 0.20.1、Fabric API 0.165.0+26.3、Mod Menu 21.0.0、MaLiLib 0.30.2、CommandKeys 3.104.1+26.3 Fabric。
- 所有 C3 运行目录均独立：
  - `runs/mc-26.3-c3a-modmenu/`
  - `runs/mc-26.3-c3a-no-modmenu/`
  - `runs/mc-26.3-c3b-api/`
- 26.2 API 基线使用 detached worktree，源码固定为 `053e736c77b15e0727c93abed7c94c3b0fb64618`，运行目录为 `.tmp/c3-baseline-26.2/runs/mc-26.2-c3b-api/`。
- Windows computer-use 状态仅返回 Codex in-app browser，`apps=[]`，无法枚举原生 Minecraft SDL 窗口。因此没有执行盲坐标点击。

## 3. C3-A：Mod Menu 结果

### 3.1 源码和 API 对照

生产位置：

- `src/main/resources/fabric.mod.json`：`modmenu` entrypoint 为 `dev.xef2.visualkeymap.compact.modmenu.ModMenuImpl`。
- `src/main/java/dev/xef2/visualkeymap/compact/modmenu/ModMenuImpl.java`：`getModConfigScreenFactory()` 返回 `new VisualKeymapScreen(screen, Minecraft.getInstance().options)`。
- 原生控制入口 `ControlsScreenMixin` 也创建相同的 `VisualKeymapScreen`，因此两条入口复用完整主界面，而不是独立简化配置页。

`ModMenuImpl.java` 与 26.2 基线无源码差异；Mod Menu 21.0.0 的既有接口能够直接编译和加载，因此 C3-A 没有生产修复。

### 3.2 动态探针

`c3aTestmod` 在 `CLIENT_STARTED` 后执行以下断言：

1. `FabricLoader.isModLoaded("modmenu")` 为 true，版本为 21.0.0。
2. `getEntrypointContainers("modmenu", ModMenuApi.class)` 中存在 provider `visual-keymap`。
3. entrypoint 实例类为 `ModMenuImpl`。
4. 工厂连续两次都创建 `VisualKeymapScreen`，且不是复用同一实例。
5. 两个屏幕分别安装到客户端后调用 `onClose()`，均回到传入的同一父屏幕。

ignored 证据文件的核心内容：

```text
status=PASS
modmenu.loaded=true
modmenu.version=21.0.0
entrypoint.provider=visual-keymap
entrypoint.class=dev.xef2.visualkeymap.compact.modmenu.ModMenuImpl
screen.class=dev.xef2.visualkeymap.gui.screen.VisualKeymapScreen
screen.instances.distinct=true
parent.return.first=true
parent.return.second=true
```

### 3.3 缺席运行

`runNoModMenuClient -PexcludeModMenuRuntime` 同时保留 MaLiLib/Tweakeroo/CommandKeys，排除 Mod Menu。Loader 到达 `CLIENT_STARTED`，探针记录：

```text
status=PASS
visual-keymap=true
malilib=true
tweakeroo=true
commandkeys=true
modmenu=false
```

没有出现 `com.terraformersmc.modmenu` 缺类、链接或错误 entrypoint 加载。

### 3.4 C3-A 状态

| 项目 | 状态 | 证据边界 |
| --- | --- | --- |
| Mod Menu 识别 Visual Keymap | **PASS（Loader/entrypoint）** | 实际 Mod Menu 列表视觉和配置按钮点击仍 **NOT TESTED**。 |
| 配置工厂打开完整主界面 | **PASS** | 实际对象类型为 `VisualKeymapScreen`；与原生入口同类。 |
| 功能与原生入口相同 | **STATICALLY VERIFIED** | 两条入口创建同一类并使用相同 Options；没有逐个 GUI 操作重跑。 |
| Done/Esc 返回父屏 | **PASS（onClose 路径）/ NOT TESTED（物理输入）** | 运行时两次返回父屏；未实际点击 Done 或按 Esc。 |
| 反复打开关闭 | **PASS（工厂/生命周期）** | 两个独立实例均成功安装和关闭；视觉空白/焦点仍需人工观察。 |
| Mod Menu 缺席启动 | **PASS** | 独立缺席运行和 `isModLoaded=false`。 |

## 4. C3-B：26.2 原版公开 API

### 4.1 README 与消费方

原版 README 提供 `visualkeymap` entrypoint JSON，并要求实现 `dev.xef2.visualkeymap.api.VisualKeymapApi`，但没有给出完整 Java 类。C3 测试模组按该说明声明：

```json
"visualkeymap": [
  "com.example.mymod.ThrowingVisualKeymapImpl",
  "com.example.mymod.MyModVisualKeymapImpl"
]
```

`MyModVisualKeymapImpl` 位于 Visual Keymap 包之外，直接继承 `KeyBinding` 并覆盖核心抽象方法，没有通过同包测试代码绕过访问控制。

### 4.2 可见性疑虑结论

此前文档误读了源码中的 `abstract public` 顺序。Java 允许修饰符采用该顺序；26.2 `javap -public/-protected` 实际结果为：

```text
public abstract String getId();
public abstract List<Integer> getKeyCodes();
public abstract void setBoundKeys(List<InputConstants.Key>);
public abstract boolean isDefault();
public abstract void resetToDefault();
protected List<String> getSearchableStrings();
```

同一消费方在 26.2 成功完成 `compileC3bTestmodJava`，因此外部包可实现性为 **PASS**，不是原版历史缺陷。

### 4.3 26.2 动态结果

26.2 客户端实际加载 Minecraft 26.2、Loader 0.19.3、Fabric API 0.152.2+26.2、Visual Keymap 1.4.0、外部测试模组、MaLiLib 0.29.0 和 Mod Menu 20.0.0-beta.3。探针结果：

```text
status=PASS
minecraft=26.2
visualkeymap.entrypoints=2
throwing.entrypoint.isolated=true
providers=[MinecraftImpl, MyModVisualKeymapImpl, MaLiLibIntegration]
external.provider=c3-external
external.ids=[alpha, conflict, empty, four]
external.modifiers=[341]
external.order=42
external.edit.reset=true
external.save=true
external.snapshot=c3-external|conflict|[68]|[341]
```

为只测试原始源码/API，detached worktree 没有提交修改。由于原 `1.17-SNAPSHOT` 已不能从仓库恢复，测试工作树临时使用现有 Loom 1.19.3；MaLiLib 坐标使用 Phase 2.6 已记录的官方 Modrinth 0.29.0 等价替代。Minecraft、Loader、Fabric API、Visual Keymap 源码和运行时版本仍是 26.2 基线。该调整属于测试构建环境，不改变被测生产源码或 API。

## 5. C3-B：26.3 对外兼容性

同一份 Java 与 metadata 源码在 26.3 编译和运行通过。26.3 provider 列表为：

```text
[MinecraftImpl, MyModVisualKeymapImpl, MaLiLibIntegration, CommandKeysIntegration]
```

这证明既有初始化顺序仍为 Minecraft → 外部 entrypoint → MaLiLib → CommandKeys。失败 entrypoint 未进入列表，有效 provider 继续运行。

### 5.1 Provider 契约结果

| 契约 | 结果 |
| --- | --- |
| provider 名称 | `c3-external`，26.2/26.3 一致。 |
| binding ID/顺序 | `alpha`、`conflict`、`empty`、`four`，保持源列表顺序。 |
| 0–4 键 | 动态覆盖空、单键和四键；`maxBoundKeys=4`。 |
| modifiers | `[341]` 独立于主键，`getFullKeyCodes()` 为 modifier→主键。 |
| comment/display | 组件值动态检查通过。 |
| 搜索 | 基类类别/名称与 provider 自定义 searchable string 均命中。 |
| 冲突 | 同 full key + order 分为一组；修改后解除。 |
| 修改/重置 | `setBoundKeys()`、`resetToDefault()` 回调和计数均通过。 |
| 保存 | `VisualKeymap.saveKeyBindings()` 调用 provider `save()` 一次。 |
| 快照 | version 1、provider、ID、规范整数与 modifiers 保持；精确匹配并应用主键。 |
| modifiers 导入 | 继续保持原版“序列化但不应用”语义，没有擅自修复。 |
| 异常隔离 | 抛异常 entrypoint 被忽略，有效 provider 正常工作。 |

GUI 行、tooltip 样式、点击编辑/重置和可见排序属于 **NOT TESTED**；模型层调用不能替代 GUI 动态证据。

## 6. API 签名对照

| 类型/契约 | 26.2 | 26.3 | 结论 |
| --- | --- | --- | --- |
| `VisualKeymapApi<T extends KeyBinding>` | `getKeyBindings()`、`save()`、default `getProviderName()` | descriptor 相同 | **PASS** |
| `KeyBinding` 构造器 | `(Component, Component, int)` public | 相同 | **PASS** |
| 五个核心抽象方法 | public abstract | 相同 | **PASS** |
| 可选覆盖方法 | `getComment()`、`getModifierKeyCodes()`、`getOrder()` public；`getSearchableStrings()` protected | 相同 | **PASS** |
| final/静态行为 | tooltip、full keys、search；`getConflictBindings()` | descriptor 相同 | **PASS** |
| `VisualKeymap.getKeyBindings()` | `List<? extends KeyBinding>` | 相同 | **PASS** |
| `getApiImplementations()` | `List<VisualKeymapApi<?>>` | 相同 | **PASS** |
| `saveKeyBindings()` | `void` | 相同 | **PASS** |
| `exportKeyBindings(File)` | `void throws IOException` | 相同 | **PASS** |
| `importKeyBindings(File)` | `KeymapSnapshot throws IOException` | 相同 | **PASS** |
| 翻译辅助方法 | 参数、返回类型相同 | 相同 | **PASS** |
| `KeymapSnapshot`/`BindingEntry` | record descriptor 与 accessors | 相同 | **PASS** |
| `ImportMatch` | 构造器、public fields、`getKeysFromEntry()` | 旧成员相同，新增 `canApply()` | **PASS（加法）**；新增方法来自已批准的不可应用安全策略。 |
| entrypoint key | `visualkeymap` | `visualkeymap` | **PASS** |

`CanonicalInputCode`、`MinecraftInputCode` 是 B1/B2 新增的 public 实现类型，位于 `input` 包而非既有 `api` 包。它们没有替换或改变既有 descriptor；C3 未继续扩大该表面。

## 7. 源码修改清单

C3 **没有修改 `src/main`**。新增内容全部是开发期测试设施：

- `build.gradle`
  - `c3aTestmod`、`c3bTestmod` source set；
  - `c3aModMenuClient`、`noModMenuClient`、`c3bApiClient` 隔离运行；
  - `-Pc3Manual=true` 可让 C3-B 客户端在探针完成后保持运行，供人工 GUI 检查。
- `src/c3aTestmod/**`
  - Mod Menu entrypoint、工厂、父屏返回链探针。
- `src/c3bTestmod/**`
  - README 风格外部 provider、抛异常 provider、公开 API 运行探针。
- `OptionalDependencyProbe`
  - 增加 Mod Menu 存在/缺席断言。

发布 JAR `visual-keymap-26.3-1.4.0.jar` 扫描不含 `c1test`、`c2test`、`c3atest`、`c3btest` 或 `com/example/mymod`。

## 8. Git 提交

| 提交 | 范围 |
| --- | --- |
| `9845d7c` `test: verify Mod Menu integration` | C3-A source set、Mod Menu 动态探针、Mod Menu 缺席矩阵。 |
| `61d5d2a` `test: verify public provider API compatibility` | C3-B 外部消费方、异常 entrypoint、26.3 运行探针与人工运行开关。 |
| 本报告提交 | C3 报告、测试状态和先前文档中 API 可见性结论修订。 |

C3-A 与 C3-B 可分别回滚；均不包含生产功能代码。

## 9. 实际命令与结果

所有当前分支命令使用 GraalVM JDK 25.0.4、工作区 Gradle cache 和隔离 run directory。

| 命令 | 结果 |
| --- | --- |
| `gradlew compileC3aTestmodJava runC3aModMenuClient --no-daemon` | **PASS**；Mod Menu 21.0.0 entrypoint/factory/返回链。 |
| `gradlew runNoModMenuClient -PexcludeModMenuRuntime --no-daemon` | **PASS**；Mod Menu 缺席启动。 |
| 26.2 detached worktree `compileC3bTestmodJava` | **PASS**；同一外部包消费方可编译。 |
| 26.2 detached worktree `runC3bApiClient` | **PASS**；entrypoint、异常隔离和 provider 契约。 |
| `gradlew compileC3bTestmodJava --no-daemon` | **PASS**。 |
| `gradlew runC3bApiClient --no-daemon` | **PASS**；26.3 外部 provider 与全部可选模组共存。 |
| `gradlew clean test --no-daemon` | **PASS**；JUnit 7/7，0 failure/error/skipped。 |
| `gradlew clean build --no-daemon` | **PASS**；8 tasks executed，`:test` 实际运行。 |

构建产物：

```text
build/libs/visual-keymap-26.3-1.4.0.jar
SHA-256 A7CC2D7A3708A6B0BEBAADC3D6E25B3049C52C1994EE4624011C68BE6B0EC0AD
```

日志中的 OSHI/WMI 警告、离线 Mojang 服务和 Mod Menu update checker 网络错误没有阻止客户端到达 `CLIENT_STARTED` 或探针完成，不属于 C3 集成失败。

## 10. Architecture Review

结论：**C3 未新增生产架构债务。**

- GUI 和 `KeymapSnapshot` 只依赖 `KeyBinding`/`VisualKeymapApi`，没有外部 provider、Mod Menu、MaLiLib 或 CommandKeys 专属分支。
- Mod Menu 依赖只存在于 `compact/modmenu/ModMenuImpl` 和 metadata entrypoint；主初始化类不引用 Mod Menu API，缺席运行安全。
- MaLiLib 专属转换仍在 `MaLiLibInputCode`，CommandKeys 复用 `MinecraftInputCode`；没有复制公共映射表。
- 第三方 provider 由既有 Loader entrypoint 聚合，无新增注册框架。
- provider 顺序和 `save()` 枚举仍由 `VisualKeymap.apiImpl` 单一路径控制。
- 生产源码扫描未发现 GUI/快照中的第三方模组名称判断。
- C3 测试类通过独立 source set 隔离，不进入发布 JAR。

受控风险：B1/B2 的 `CanonicalInputCode`/`MinecraftInputCode` 为 public 类，但不在原 `api` 包；这是既有迁移实现事实，不是 C3 新增变化。若未来要正式承诺或隐藏它们，需要单独 API 决策，不能在本阶段顺带重构。

## 11. 未完成的人工测试

### 11.1 Mod Menu

在隔离 26.3 客户端运行：

```powershell
.\gradlew.bat runClient --no-daemon
```

1. 主菜单打开 Mods，选择 Visual Keymap，确认存在配置按钮。
2. 点击配置按钮，确认显示完整键盘、绑定列表、搜索、导入、导出、配置和 Done，而不是仅三项设置页。
3. 点击 Done，确认回到同一 Mod Menu 屏幕。
4. 再次打开后按 Esc，确认与 26.2 原版一致地回到父屏。
5. 连续重复三次，观察是否出现空白、错误父屏、焦点丢失或日志异常。

### 11.2 外部 Provider GUI

运行可保持客户端打开的开发配置：

```powershell
.\gradlew.bat runC3bApiClient -Pc3Manual=true --no-daemon
```

该实例只使用 `runs/mc-26.3-c3b-api/`。打开 Visual Keymap 后检查：

1. `External Category - Alpha Action`、comment tooltip 和四个测试绑定可见。
2. 搜索 `external provider needle` 与 `category alpha`。
3. `alpha`/`conflict` 的冲突提示和 order 分组。
4. 编辑、清空、重置按钮及返回保存。
5. 导出文件包含 provider `c3-external`、稳定 ID、规范 keys 和 modifiers；导入预览后确认回调。

### 11.3 延续的 C1/C2 项目

- MaLiLib GUI、多键录入状态机和本地世界实际热键触发。
- CommandKeys GUI/profile 切换、宏编辑与本地世界无害宏触发。
- 多个同时未绑定 CommandKeys 宏的 26.2→26.3 对照。

## 12. Phase 3 完成情况与 Phase 4 建议

Phase 3 的主要开发单元 A、B1、B2/B2.5、C1、C2、C3 均已完成生产适配或兼容性验收：

- 构建、依赖和 JDK 25 可复现；
- 核心规范键码、GUI、SDL 文件对话框和 version 1 快照已迁移；
- Minecraft、MaLiLib、CommandKeys、Mod Menu 和外部 provider 均有动态加载证据；
- clean test/build 通过；
- 没有通过删除集成、空实现或改变 schema 绕过问题。

因此建议 **CONDITIONAL GO 进入 Phase 4**。Phase 4 应以完整人工回归、剩余设备/输入场景、发布工件和版本发布前检查为主，不应在没有新证据时继续改动已通过的生产路径。进入 Phase 4 仍需用户明确授权。
