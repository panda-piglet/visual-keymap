# Visual Keymap 26.2 → 26.3 迁移差异审查

日期：2026-10-31  
比较基线：`053e736c77b15e0727c93abed7c94c3b0fb64618`  
审查目标：`port/mc-26.3`，Phase 4 测试设施 HEAD `29ea281`

## 1. 审查结论

**未发现迁移无关的生产功能改动、公共 API 破坏或 version 1 schema 变化。**

19 个构建/生产文件的变化均可追溯到：目标依赖、GLFW/tinyfd 到 SDL、规范键码兼容、MaLiLib/CommandKeys adapter 或不支持输入的数据保护。Mixin 源码和配置没有改变。测试 source set 和 P1–P6 夹具未进入发布 JAR。

没有基于风格偏好提出重构。本报告只记录有 diff、源码扫描、构建产物或运行证据支持的结论。

## 2. 分类总览

| 分类 | 主要文件 | 目的 | 审查结论 |
| --- | --- | --- | --- |
| A. Minecraft/Fabric API 必需适配 | `VisualKeymap.java`、`NativeFileDialog.java`、`VisualKeymapScreen.java`、版本/metadata | 移除 GLFW/tinyfd 调用，使用 26.3 keyboard 类型和 SDL 异步文件对话框 | 必需；主路径动态通过。 |
| B. 键码数据语义兼容 | `CanonicalInputCode`、`MinecraftInputCode`、Minecraft provider、GUI、快照、导入预览 | 保持 26.2 规范整数、鼠标 0–7、version 1 与扩展键/tombstone | 必需；自动化和真实 F8/MaLiLib 往返通过。 |
| C. 第三方 Adapter 适配 | `MaLiLibInputCode`、`MaLiLibIntegration`、`CommandKeysIntegration` | 隔离 MaLiLib scan code/负值和 CommandKeys SDL typed key | 必需；动态 adapter/启动/持久化通过。 |
| D. 运行兼容性与错误处理 | `ImportMatch.canApply()`、导入 checkbox、SDL request 生命周期 | 未知 extension/F25 不写 live binding；异步回调在 client tick 执行 | 由批准的安全策略和 SDL API 强制；没有扩大为通用错误处理重写。 |
| E. 其他修改 | 两条本地化字符串、测试设施、文档 | 显示“不支持/不可用”；验证而非生产功能 | 与迁移直接相关；未发现无关生产变化。 |

## 3. 构建与元数据差异

| 位置 | 差异 | 分类 | 发布影响 |
| --- | --- | --- | --- |
| `gradle/wrapper/gradle-wrapper.properties` | 9.5.1 → 9.8.2 | A | 与 26.3 工具链组合一致；build 通过。 |
| `build.gradle` Loom | 1.17-SNAPSHOT → 1.19.3 | A | 固定正式版；避免可变 snapshot。 |
| `gradle.properties` | MC 26.3、Loader 0.20.1、Fabric API 0.165.0、MaLiLib 0.30.2、CommandKeys 3.104.1 Fabric、Mod Menu 21.0.0 | A/C | P1–P6 Loader 列表验证实际版本。 |
| MaLiLib 坐标 | 旧 Masa 坐标 → Modrinth Maven | C | 官方 26.3 构件；不打包。 |
| CommandKeys 坐标 | 模糊 Modrinth 版本 → Curse file 9106946 | C | 明确 Fabric JAR；NeoForge 误取已消除。 |
| `fabric.mod.json` | Minecraft `~26.2` → `~26.3` | A | 成品展开正确；入口点和 mixin 不变。 |
| JUnit/test source sets | 新增 | E（测试） | `build` 实际执行 7 tests；发布/源码 JAR 扫描均不含测试类。 |
| Loom run configs/排除开关 | 新增独立 gameDir 与可选依赖矩阵 | E（测试） | 只影响开发任务；不进入 metadata/JAR。 |
| Tweakeroo runtimeOnly | 开发期真实 MaLiLib provider | E（测试） | 只在开发 runtimeClasspath；发布 JAR 不含该模组。 |

`commandkeys_version` 属性仍作为可读版本记录，实际构件由固定 file ID 选择；这是冗余属性而非生产行为问题。本阶段不为清理属性做无关改动。

## 4. A 类：Minecraft/Fabric API 必需适配

### 4.1 默认打开按键

`VisualKeymap.onInitializeClient()` 删除 GLFW import，将旧 `GLFW_KEY_UNKNOWN` 构造改为 `InputConstants.Type.KEYBOARD` + `InputConstants.UNKNOWN.getValue()`。26.3 不再提供 GLFW 键常量，unknown typed key 的值也从旧 `-1` 变化为 0。

预期行为仍是默认未绑定。B2.5 已验证控制列表显示 Not Bound、绑定/重置和重启；P1 成品 JAR启动通过。

### 4.2 文件对话框

旧 `TinyFileDialogs.tinyfd_*` 在 26.3 不可用。`NativeFileDialog` 使用 Minecraft 随 SDL 暴露的异步文件对话框：

- JSON filter；
- 保存默认路径 `config/visualkeymap-export.json`；
- Minecraft SDL window owner；
- 单文件选择；
- 回调结果在 client tick 处理；
- callback/filter/property native 资源延迟到下一 tick 释放。

`VisualKeymapScreen` 的导入/导出业务、IOException 忽略行为和导入预览流程保持原结构。异步时序是底层 API 强制差异，不是 UI 改版。B2.5 已动态验证打开、保存、过滤、取消、实际导入/导出和焦点返回；异常文件系统矩阵仍需人工补齐。

## 5. B 类：键码语义兼容

### 5.1 集中层

新增：

- `CanonicalInputCode`：26.2 规范整数、8 个显式鼠标映射、F25 tombstone、127 个固定扩展 ID；
- `MinecraftInputCode`：26.3 `InputConstants.Key` 与规范整数的唯一 Minecraft 边界。

扩展命名空间为 `0x01000000..0x0100FFFF`。测试固定表哈希：

`0E45BE43A316246A555578E89F6BFF2016BB5CCD3FFDA713571DDAF30044AC28`

### 5.2 消费位置

| 位置 | 旧行为 | 新行为 | 必要性 |
| --- | --- | --- | --- |
| `MinecraftImpl` | 直接返回 `Key#getValue()` | `MinecraftInputCode.encode()` | 防止 SDL A–E 与鼠标 4–8 碰撞。 |
| `KeyBinding` 显示 | 0–7/KEYSYM 分支 | 集中显示解码 | KEYSYM 已移除；保持旧整数。 |
| `VisualKeymapScreen` | 选择/筛选保存 raw value | 规范整数 | 防止高亮、筛选和冲突串键。 |
| `KeyboardWidget` | 与 raw value 比较 | 缓存规范整数 | 与主屏同一语义。 |
| `KeymapSnapshot.ImportMatch` | 0–7/KEYSYM 构造 | 集中解码 + `canApply()` | 旧 version 1、extension、F25 安全。 |
| `ImportPreviewScreen` | KEYSYM 显示，所有项可勾选 | 集中显示，不可应用项禁用 | 防止未知值静默覆盖 live binding。 |

version 1 record 字段、Gson 读写和默认过滤没有改变。`canApply()` 是加法方法；既有成员未删除或改 descriptor。

### 5.3 原始值扫描

生产源码中的 `Key#getValue()`/`Type.*.getOrCreate()` 只出现在：

- 默认 unknown typed-key 构造；
- `MinecraftInputCode`；
- `MaLiLibInputCode` adapter 边界；
- `VisualKeymapScreen.mouseClicked()` 把 SDL mouse event 先构造成 typed key，随后立即进入集中编码。

GUI、快照、冲突、排序和 CommandKeys adapter 没有把裸 SDL 数字作为公共规范值。

## 6. C 类：第三方 Adapter

### 6.1 MaLiLib

旧逻辑把所有 `< -1` 值加 100，26.3 会误处理 gamepad/其他负值。新 `MaLiLibInputCode`：

- 只把 `-99..-92` 识别为 8 个鼠标键；
- 正 keyboard scan code 通过 `MinecraftInputCode`；
- 无法表示的负值不进入公共整数列表；
- 编辑支持的键时保留原有不支持负值，不静默覆盖。

特殊逻辑只存在于 integration 包。JUnit 和真实 Tweakeroo 对象已验证键盘、8 鼠标、组合、其他负值、保存/重启和真实 26.2 MaLiLib 快照语义。

### 6.2 CommandKeys

唯一生产差异是 `getKeyCodes()` 从 `InputConstants.Key#getValue()` 改为 `MinecraftInputCode.requireCanonical()`。profile、宏名/comment、消息搜索、limit→main 顺序、写入、`Config.save()`、清空/重置和空 provider 均未改。

P4/P5 成品 JAR 启动、C2 动态配置保存和重启通过。特殊逻辑仍局限在 `CommandKeysIntegration`。

### 6.3 Mod Menu

`ModMenuImpl.java` 没有生产 diff。P2/P5 的成品 JAR 运行通过 metadata entrypoint 获取工厂并创建完整 `VisualKeymapScreen`；P1 缺席运行没有类加载错误。

## 7. D 类：运行兼容性与错误处理

只有两类迁移所需行为：

1. SDL 异步 dialog request 的生命周期管理；
2. F25 tombstone/未知 extension 在预览中显示但不可选择、不可写入 live binding。

没有借机统一异常反馈、修复无效 JSON 崩溃、改变 IOException 忽略策略或调整配置容错。无效 JSON 的历史路径继续保留，等待 26.2 动态对照。

## 8. 公共接口与 schema

相对基线：

- `VisualKeymapApi` descriptor 不变；
- `KeyBinding` public constructor、五个 public abstract 方法和 protected 搜索扩展点不变；
- `VisualKeymap.getKeyBindings()`、`getApiImplementations()`、`saveKeyBindings()`、导入/导出方法不变；
- entrypoint key 仍为 `visualkeymap`；
- provider ID、binding ID、order 和 modifiers 语义不变；
- `KeymapSnapshot(version, bindings)` 与 `BindingEntry(provider,id,keys,modifiers)` 不变；
- CommandKeys provider 仍为空，不参与 version 1 导出；
- modifiers 仍写出但不因差异进入预览，也不在导入时应用。

加法表面：`ImportMatch.canApply()`、`CanonicalInputCode`、`MinecraftInputCode`、`NativeFileDialog`。后三者不在既有 `api` 包，但 Java 类型是 public。它们没有替换旧 API；这是一项非阻塞的表面扩展风险。若未来要承诺或收窄其 API，需要单独兼容性决策，不能在本次迁移中顺带修改。

## 9. GUI 与用户语义

没有布局结构、按钮顺序、搜索模型、排序模式、冲突算法、编辑最大键数或保存入口的无关变化。生产 GUI diff 仅包括：

- 规范键码选择/显示；
- SDL 文件对话框调用；
- 不支持键的 checkbox 禁用和两条本地化说明。

这些变化是保持旧操作语义或执行已批准的数据保护策略所必需。像素级布局、真实 Done/Esc、IME、物理多键和不同 GUI Scale 仍需人工证据，不能由源码 diff 直接判 PASS。

## 10. Mixin 审查

以下路径相对基线无 diff：

- `ControlsScreenMixin`；
- `HeaderAndFooterLayoutAccessor`；
- `KeyMappingAccessor`；
- `visual-keymap.mixins.json`。

成品 JAR P1–P6 强制加载 `ControlsScreen`，通过 Minecraft provider 实际使用 `KeyMappingAccessor`，并初始化 `VisualKeymapScreen` 使用 layout accessor。六组日志均没有 Mixin 注入错误。

## 11. 测试/发布边界

根构建新增的 C1/C2/C3 testmod source set、JUnit、可选依赖排除开关和独立 runDir 只用于开发验证。Phase 4 另有独立 production-smoke 工程。发布 JAR/源码 JAR 扫描结果均为：测试 entry 0、第三方 class 0、nested JAR 0。

因此测试设施没有改变普通发布内容。默认根 `runClient` 会加载开发期 runtimeOnly 可选依赖和 Tweakeroo，这只影响开发运行组合；P1–P6 已用独立夹具明确覆盖发布 JAR 的缺席/存在矩阵。

## 12. 发现与风险

| 项目 | 状态 | 影响 |
| --- | --- | --- |
| 迁移无关生产变化 | 未发现 | 无。 |
| 公共 API 破坏 | 未发现 | C3 同源消费方在 26.2/26.3 编译运行通过。 |
| version 1 schema 变化 | 未发现 | 真实 Minecraft/MaLiLib 规范整数往返通过。 |
| 分散第三方判断 | 未发现 | 名称判断只在既有初始化边界和 adapter。 |
| 重复 SDL 映射 | 未发现 | Minecraft 与 MaLiLib 各自有单一边界；CommandKeys 复用 Minecraft 层。 |
| 新 public 实现类型 | 非阻塞风险 | 加法表面，不影响旧二进制；未来 API 策略需单独决策。 |
| SDL dialog 异常路径 | 人工待测 | 主路径通过；不可读/不可写/覆盖需回归。 |
| Gradle 10 deprecation | 非阻塞 | 当前 9.8.2 构建成功；不属本次功能迁移。 |
| GUI/物理输入 | 发布前风险 | 详见人工指南；没有自动证据时不得判 PASS。 |

最终审查结论：生产差异范围受控，架构边界保持；没有需要 Phase 4 擅自修改生产代码的问题。
