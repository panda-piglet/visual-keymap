# Visual Keymap 26.2 架构调查

## 1. 基线

本文针对上游 `main@053e736`（Minecraft 26.2）。调查时本地工作区为空，所列源码路径均为该提交中的仓库相对路径。以下结论除明确标注“待人工验证”外，均由源码直接确认。

## 2. 模块关系

```text
Fabric Loader client entrypoint
  └─ VisualKeymap
      ├─ 发现 visualkeymap entrypoint → VisualKeymapApi 实现
      ├─ 条件注册 MaLiLibIntegration / CommandKeysIntegration
      ├─ 注册原版 KeyMapping + END_CLIENT_TICK
      ├─ 加载 ModConfig
      └─ 为 GUI 汇总 KeyBinding

入口层
  ├─ ControlsScreenMixin ─┐
  ├─ ModMenuImpl ─────────┼─→ VisualKeymapScreen
  └─ 打开界面快捷键 ──────┘

VisualKeymapScreen
  ├─ KeyboardWidget ← KeyLayoutHelper
  ├─ KeybindsListWidget
  ├─ ConfigScreen ←→ ModConfig
  └─ ImportPreviewScreen ←→ KeymapSnapshot

统一绑定模型
  └─ KeyBinding
      ├─ MinecraftImpl.MinecraftKeyBinding
      ├─ MaLiLibIntegration.MaLiLibKeyBinding
      └─ CommandKeysIntegration.CommandKeysKeyBinding
```

## 3. 初始化与生命周期

`src/main/resources/fabric.mod.json` 把 `dev.xef2.visualkeymap.VisualKeymap` 注册为 `client` entrypoint。`VisualKeymap.onInitializeClient()` 的顺序是：

1. `apiImpl` 在类加载时已包含一个 `MinecraftImpl`。
2. 通过 `FabricLoader.getEntrypointContainers("visualkeymap", VisualKeymapApi.class)` 获取外部实现，按 Loader 返回顺序追加；单个 entrypoint 实例化抛出的任何 `Throwable` 都被忽略。
3. 若 mod id `malilib` 已加载，追加 `MaLiLibIntegration`。
4. 若 mod id `commandkeys` 已加载，追加 `CommandKeysIntegration`。
5. 以 `Identifier.fromNamespaceAndPath("visualkeymap", "visualkeymap")` 注册 `KeyMapping.Category`，再通过 Fabric API 注册默认未绑定的“打开 Visual Keymap”按键。
6. 在 `ClientTickEvents.END_CLIENT_TICK` 中消费该按键并创建主屏幕。
7. 最后调用 `ModConfig.load()`。

由此得到的 provider 顺序是：Minecraft → 外部 `visualkeymap` entrypoints → MaLiLib（若存在）→ CommandKeys（若存在）。`VisualKeymap.getKeyBindings()` 每次调用时都会重新向所有 provider 取列表并扁平化，不维护长期绑定对象缓存；GUI 自身在打开和导入刷新时保存一次聚合后的列表。

## 4. 核心抽象

### 4.1 `VisualKeymapApi<T extends KeyBinding>`

位置：`src/main/java/dev/xef2/visualkeymap/api/VisualKeymapApi.java`。

- `List<T> getKeyBindings()`：返回 provider 当前可见绑定。
- `void save()`：由主界面移除或导入确认时调用。
- `default String getProviderName()`：默认空字符串；用于快照导入/导出身份。
- 接口标注 `@Environment(EnvType.CLIENT)`。

### 4.2 `KeyBinding`

位置：`src/main/java/dev/xef2/visualkeymap/api/KeyBinding.java`。

构造数据为 `category`、`name`、`maxBoundKeys`。它负责统一：

- 显示名：`category + " - " + name`。
- 可选注释和 tooltip。
- 主键、modifier 与完整键码序列。
- 搜索字符串及多词 AND 匹配。
- 绑定文本本地化。
- 默认状态、重置和写入的抽象协议。
- 冲突分组：`ConflictKey(fullKeyCodes, order)`。

迁移敏感点：`getId()`、`getKeyCodes()`、`setBoundKeys()`、`isDefault()`、`resetToDefault()` 在当前源码中没有显式访问修饰符。README 宣称外部模组可实现 API，但外部包能否实际提供可实例化的 `KeyBinding` 子类需通过独立消费方编译测试确认；这是兼容性验证项，不在本阶段修改。

### 4.3 `KeymapSnapshot`

位置：`src/main/java/dev/xef2/visualkeymap/KeymapSnapshot.java`。

- 不可变 record：`version` 与 `bindings`。
- `BindingEntry`：`provider`、`id`、`keys`、`modifiers`；紧凑构造器只对各字段的 null 做空值替代。
- `fromBindings()` 生成版本 1，只收集 provider 非空且非默认的绑定。
- `matchBindings()` 用 provider 和 ID 精确匹配。
- `ImportMatch` 保存目标绑定、快照条目和默认 `selected = true`；`getKeysFromEntry()` 只转换 `entry.keys()`，以 0–7 判定鼠标键，其余判为 keysym。

## 5. GUI 数据流

### 5.1 主屏幕

`VisualKeymapScreen` 继承 Minecraft `OptionsSubScreen`，使用其 `HeaderAndFooterLayout`：

- `addTitle()` 创建标题和搜索框。搜索 responder 更新共享字符串并让列表重建。
- `addContents()` 重新聚合 provider 绑定，创建 `KeyboardWidget` 与 `KeybindsListWidget`。
- `SharedData` 在两个 widget 间共享搜索文本、所选键码与待编辑绑定。
- `KeyboardWidget` 通过回调向主屏幕查询某键关联的绑定，并通过回调更改所选键。
- `KeybindsListWidget` 不读取全局 provider；它只持有主屏幕传入的当前子集副本。
- `repositionElements()` 使用 `HeaderAndFooterLayoutAccessor` 设置内容 frame 最小高度，并把内容区近似平分给两个 widget。
- `removed()` 是统一保存边界。

### 5.2 录入状态机

待编辑绑定保存在 `SharedData.selectedKeyBinding`，本轮已按下键保存在主屏幕的 `pressedKeys`：

1. 点击某行的按键按钮设置目标绑定。
2. `keyPressed()` 加入非重复且非 Esc 的键并立即写入。
3. 达到 `maxBoundKeys`、按 Esc、任意 `keyReleased()` 或任意鼠标点击时结束。
4. 结束时清空目标和 `pressedKeys`；每次写入后刷新键盘与列表行视觉状态。

这套状态机定义了现有用户操作方式，迁移不可随意改成另一种组合键录入语义。

### 5.3 列表与冲突

`KeybindsListWidget.createEntries()` 先计算当前列表子集内的冲突，再依次应用搜索命中、可选冲突置顶、可选按键/名称排序。搜索只影响顺序和视觉，不进行过滤。`updateAllEntries()` 重新计算冲突并更新行按钮、提示、重置可用状态。

`KeyboardWidget.KeyWidget.updateBindings()` 则针对该键关联的完整子集独立计算冲突，并构造 tooltip。因此键帽颜色/提示与当前下方列表选择来自同一查询，但各自计算展示。

## 6. Provider 适配层

### 6.1 Minecraft 原生按键系统

`MinecraftImpl.getKeyBindings()` 直接遍历 `Minecraft.getInstance().options.keyMappings`，每次包装为新的 `MinecraftKeyBinding`。

读取路径：

- 公开 API：`KeyMapping.getCategory()`、`getName()`、`isDefault()`、`getDefaultKey()`。
- Mixin accessor：私有字段 `KeyMapping.key` 与 `KeyMapping.order`。
- debug 特例：`Options.debugKeys`、`Options.keyDebugModifier`。

写入路径：

- `KeyMapping.setKey(first-or-UNKNOWN)`。
- `KeyMapping.resetMapping()` 重建静态映射。
- `MinecraftImpl.save()` 不做操作，但 `VisualKeymapScreen.removed()` 在 `VisualKeymap.saveKeyBindings()` 后调用 `super.removed()`；26.2 官方 `OptionsSubScreen` 字节码确认该父方法调用 `Minecraft.options.save()`。因此保存调用时机已确认是主屏幕移除；真实文件 I/O 成功和错误路径仍需人工验证。

### 6.2 MaLiLib

`MaLiLibIntegration` 依赖 `ConfigManager`、`InputEventHandler`、`IHotkey`、`IKeybind`、`KeybindCategory`。它从全局 keybind manager 获取 category/hotkey，并直接操作 MaLiLib 的 keybind 集合。鼠标键码在 MaLiLib 负数编码与本模组 0–7 编码间做 `±100` 转换。保存阶段强制保存所有 MaLiLib 配置并更新已使用键集合。

### 6.3 CommandKeys

`CommandKeysIntegration` 依赖 `CommandKeys.profile()` 以及 `Profile`、`Macro`、`Keybind`、`Message`、`Config`。它只包装活动 profile 的 `macroMap.entries()`；通过 `Profile.setKey()`、`setLimitKey()` 修改，通过 `Config.save()` 保存。该类未覆盖 `getProviderName()`，因此继承空字符串。

### 6.4 Mod Menu

`ModMenuImpl` 仅实现 `ModMenuApi.getModConfigScreenFactory()`，没有独立配置页，而是复用主 `VisualKeymapScreen`。类由 `fabric.mod.json` 的 `modmenu` entrypoint 暴露。

## 7. 持久化边界

| 数据 | 文件/所有者 | 读取 | 写入 |
| --- | --- | --- | --- |
| 本模组 UI 配置 | Fabric config 目录的 `visualkeymap.json` | `ModConfig.load()`，客户端初始化末尾 | 加载后立即写回；`ConfigScreen.removed()` |
| Minecraft 按键 | Minecraft `Options` / `options.txt` | `MinecraftImpl` 每次枚举 | `KeyMapping.setKey()`；主屏幕移除时由 `OptionsSubScreen.removed()` 调用 `Options.save()` |
| MaLiLib 热键 | MaLiLib 配置系统 | `InputEventHandler` / `IHotkey` | `ConfigManager.saveAllConfigs()` + `updateUsedKeys()` |
| CommandKeys 宏键 | CommandKeys 配置系统 | 当前 `Profile.macroMap` | `Config.save()` |
| 可移植快照 | 用户选择的 JSON 文件 | `KeymapSnapshot.readFromFile()` | `KeymapSnapshot.writeToFile()` |

## 8. 26.3 迁移敏感依赖面

### Minecraft 内部/客户端 API

- `OptionsSubScreen` 的 `layout`、`list`、`lastScreen`、`options` 生命周期与 `addTitle/addContents/addOptions/addFooter/repositionElements/removed` 钩子。
- `HeaderAndFooterLayout.contentsFrame` 私有字段名和 `FrameLayout.setMinHeight()`。
- `ControlsScreen.addOptions()` 内部对 `OptionsList.addSmall(OptionInstance<?>[])` 的具体调用，Mixin 使用精确 descriptor 重定向。
- `KeyMapping.key`、`KeyMapping.order` 私有字段，以及静态 `resetMapping()`。
- `Options.keyMappings`、`debugKeys`、`keyDebugModifier`。
- 26.2 输入事件类 `KeyEvent`、`MouseButtonEvent`、`InputWithModifiers`。
- 26.2 渲染提取 API：`GuiGraphicsExtractor`、`extractContent/extractContents/extractRenderState()`、`textRendererForWidget()`。
- `ContainerObjectSelectionList` 的布局、子项和滚动 API。
- `OptionInstance.Enum` 与 `StringRepresentable`。
- `InputConstants` 键解析、类型、显示名和值域语义。
- `Screen` / `Minecraft.setScreenAndShow()` / `client.gui.screen()`。

### Fabric API / Loader

- Loader：`ClientModInitializer`、`FabricLoader`、typed entrypoint containers、config 目录、mod presence。
- Fabric API：`KeyMappingHelper.registerKeyMapping()`、`ClientTickEvents.END_CLIENT_TICK`。
- Mixin 配置：客户端三个 mixin，Java 25，`defaultRequire: 1`，因此关键注入失败应视为启动/应用失败而非可忽略退化。

### 第三方 API

- MaLiLib 与 CommandKeys API 均在 Java 类型层直接引用；必须用对应 26.3 版本重新编译验证。
- Mod Menu 接口在独立 entrypoint 类中直接引用；需要验证其 26.3 API 与 Loader 在 Mod Menu 缺席时的类加载行为。
- TinyFileDialogs 通过 Minecraft/LWJGL 运行时提供，文件对话框与平台行为需运行验证。

## 9. 自动化现状

- 仓库没有 `src/test`、测试依赖、JUnit、GameTest 或测试任务自定义代码。
- `.github/workflows/release.yml` 只在 tag push 时使用 JDK 25 执行 `./gradlew build`，随后发布；它是构建自动化，不是功能测试。
- 因此 UI、Mixin 注入、按键修改、第三方集成、配置与快照格式目前均没有自动化回归保护。完整回归矩阵见 `TEST-PLAN.md`。
