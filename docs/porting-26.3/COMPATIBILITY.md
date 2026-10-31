# Visual Keymap 26.3 兼容性基线

## 1. 原则

本文件记录从 Minecraft 26.2 迁移到 26.3 时必须维持的外部接口、第三方集成和可观察行为。基线为上游 `main@053e736`。这里只定义兼容目标，不包含 Bug 修复、功能增强或重构建议。

标记：

- **已证实**：当前源码直接确定。
- **待验证**：源码不足以证明真实运行效果，必须在 26.2 建立参照并在 26.3 对照。

## 2. 构建与运行依赖基线

来源：`build.gradle`、`gradle.properties`、`fabric.mod.json`、`visual-keymap.mixins.json`。

| 项目 | 26.2 基线 | 兼容要求 |
| --- | --- | --- |
| Minecraft | `26.2`；metadata 为 `~26.2` | 迁移时只做 26.3 所需适配，不改变功能契约。 |
| Java | release/toolchain 25；Mixin `JAVA_25` | 已由 26.3 `version.json`、Fabric 官方示例和本地 GraalVM 25.0.4 验证：目标版本仍为 Java 25；无需因 Minecraft 版本迁移改变源码语言级别或 Mixin compatibility level。 |
| Fabric Loader | `0.19.3`，metadata 下限同属性展开 | 核实 26.3 推荐/最低版本及 typed entrypoint 行为。 |
| Fabric API | `0.152.2+26.2` | 核实 key mapping helper 与 end-client-tick API。 |
| MaLiLib | `26.2:0.29.0`，`compileOnly` + `runtimeOnly` | 寻找匹配 26.3 的 API/构件并逐签名验证。生产 metadata 不把它声明为硬依赖，运行逻辑按 mod id 条件启用。 |
| CommandKeys | `3.103.0+26.2`，`compileOnly` + `runtimeOnly` | 寻找 26.3 对应版本并验证当前 profile/macro API。生产 metadata 不声明硬依赖。 |
| Mod Menu | `20.0.0-beta.3`，`compileOnly` + `runtimeOnly` | 验证 `ModMenuApi` 与 `ConfigScreenFactory`；生产 metadata 不声明硬依赖。 |
| Loom | `1.17-SNAPSHOT` | 属于构建适配面，不是用户功能接口。 |
| Gradle Wrapper | `9.5.1` | 当前 release CI 使用 wrapper 构建。 |

`runtimeOnly` 使三项可选集成出现在开发运行配置中，但 `fabric.mod.json` 的 `depends` 只包含 Fabric Loader、Fabric API 和 Minecraft。发布后的真实“可选依赖缺席”行为必须单独测试。

## 3. 模组标识、入口点与资源契约

### 3.1 标识必须区分

- Fabric mod id：`visual-keymap`（带连字符），见 `fabric.mod.json`。
- Java 常量/翻译 namespace/自定义 entrypoint key：`visualkeymap`（不带连字符），见 `VisualKeymap.MOD_ID` 及 `getTranslationKey()`。
- 外部 provider 的 Fabric entrypoint key 必须继续是 `visualkeymap`：

```json
"entrypoints": {
  "visualkeymap": [
    "com.example.mymod.MyModVisualKeymapImpl"
  ]
}
```

把两种 ID 合并或改名会破坏 metadata、翻译键、按键类别或外部集成，不能作为迁移清理。

### 3.2 Loader 行为

`VisualKeymap.onInitializeClient()` 使用 `getEntrypointContainers("visualkeymap", VisualKeymapApi.class)`。外部实现实例化抛出的 `Throwable` 当前被静默忽略；实现成功则追加到 provider 列表。该失败隔离行为是当前兼容基线。

### 3.3 语言与资源

- 翻译键前缀继续为 `visualkeymap.`；按键类别另使用原版约定 `key.category.visualkeymap.visualkeymap`。
- 至少维持 `en_us`、`zh_cn` 当前所有键。
- 键盘专用键名缺失时必须继续回退到 Minecraft `InputConstants.Key` 显示名。
- 模组图标路径继续由 metadata 指向 `assets/visualkeymap/icon.png`。

## 4. 对外 Java API

### 4.1 `VisualKeymapApi`

公开全名：`dev.xef2.visualkeymap.api.VisualKeymapApi<T extends KeyBinding>`。

必须维持的当前签名/语义：

- `List<T> getKeyBindings()`：每次 GUI 聚合时可被调用。
- `void save()`：主界面移除与导入确认时被调用。
- `default String getProviderName()`：默认 `""`；非空名称参与快照导入/导出。
- 客户端环境限定。

### 4.2 `KeyBinding`

公开全名：`dev.xef2.visualkeymap.api.KeyBinding`。

构造器契约：`KeyBinding(Component category, Component name, int maxBoundKeys)`。当前可覆盖/调用点包括：

- 标识、主键列表、写入、默认判断、恢复默认。
- 可选 `getComment()`、`getModifierKeyCodes()`、`getOrder()`、`getSearchableStrings()`。
- final 的 tooltip、完整键码、搜索匹配和本地化按键文本逻辑。
- 静态 `getConflictBindings()`。

**C3 验证结论：**五个核心抽象方法在源码中采用合法但不常见的 `abstract public` 顺序；`javap -public/-protected` 证实它们均为 `public abstract`。同一外部包消费方已在 `main@053e736`（Minecraft 26.2）和当前 26.3 编译并动态加载通过，因此此前的可见性疑虑已解除。C3 没有修改这些修饰符或其他既有 API descriptor。

### 4.3 `VisualKeymap` 静态入口

当前公开方法也构成潜在外部调用面：

- `getKeyBindings()`
- `getApiImplementations()`（返回内部可变 list 的直接引用）
- `saveKeyBindings()`
- `exportKeyBindings(File)`
- `importKeyBindings(File)`
- `getTranslationKey(String)`
- `getTranslatedComponent(String, Object...)`

26.3 迁移不应无故删除、改名、改变返回类型或改变 provider 顺序。

## 5. Provider 身份与绑定 ID

快照兼容依赖 `(provider, id)` 精确相等。以下字符串必须视为持久化格式的一部分：

| Provider 实现 | provider | binding id | 备注 |
| --- | --- | --- | --- |
| `MinecraftImpl` | `minecraft` | `KeyMapping.getName()` | 通常为翻译键；不能改成显示文本。 |
| `MaLiLibIntegration` | `malilib` | `IHotkey.getName()` | 不能改成 GUI 显示名。 |
| 外部实现 | `getProviderName()` 返回值 | 其 `KeyBinding.getId()` | 由集成方负责稳定。 |
| `CommandKeysIntegration` | 空字符串（继承默认） | `profile.name + ":" + macro.name` | 因 provider 为空，当前不会导出；这是已证实的现状。 |

Provider 发现顺序为 Minecraft → 外部 entrypoints → MaLiLib → CommandKeys。默认排序模式依赖来源列表的稳定顺序，因此该顺序同样影响用户可见结果。

## 6. 快照文件兼容

当前 JSON 逻辑模型：

```json
{
  "version": 1,
  "bindings": [
    {
      "provider": "minecraft",
      "id": "key.example",
      "keys": [65],
      "modifiers": []
    }
  ]
}
```

必须保持的当前行为：

- 导出只包含非默认绑定和非空 provider。
- `version` 写为 1；读取端当前不检查版本。
- `provider` 与 `id` 精确匹配，不做别名或模糊匹配。
- null 的 entry 字段分别回落到空字符串/空列表。
- 键码 0–7 在 `ImportMatch.getKeysFromEntry()` 中当作鼠标键，其余当作 keysym。
- 预览与导入只比较/使用 `keys`；`modifiers` 虽被序列化但不应用。迁移不能隐式改变此格式行为。
- 文件读写使用平台默认字符集的 `FileReader` / `FileWriter`；是否在所有目标平台产生一致编码需**待人工验证**。

## 7. Minecraft 原生兼容面

### 7.1 按键枚举与修改

必须继续覆盖 `Minecraft.getInstance().options.keyMappings` 中全部按键，包括原版、Fabric 注册键和 Visual Keymap 自身按键。修改时只取传入列表第一项，空列表写 `InputConstants.UNKNOWN`，随后调用 `KeyMapping.resetMapping()`。

### 7.2 Mixin/内部字段

| 依赖点 | 当前用途 | 26.3 必验项 |
| --- | --- | --- |
| `KeyMapping.key` accessor | 读取实际绑定 | 字段名、类型、映射与 accessor 是否仍可应用。 |
| `KeyMapping.order` accessor | 冲突 key 的第二维 | 字段是否存在、语义是否变化；用相同按键的原版 debug/普通绑定做行为对照。 |
| `HeaderAndFooterLayout.contentsFrame` accessor | 主屏幕内容高度 | 字段名/类型与布局效果。 |
| `ControlsScreen.addOptions()` redirect | 插入菜单按钮 | 方法签名、目标 `OptionsList.addSmall` descriptor、调用次数和原控件保留。 |

`visual-keymap.mixins.json` 设为 required 且 `defaultRequire = 1`。不能通过让注入静默失效来“完成”迁移。

### 7.3 Debug modifier

`MinecraftImpl.MinecraftKeyBinding.getModifierKeyCodes()` 以对象身份判断绑定是否在 `Options.debugKeys`，若是则读取 `options.keyDebugModifier` 的私有 `key`。该显示、搜索、冲突和导出行为需在 26.3 保留；其落盘与导入仍按当前基线处理。

## 8. MaLiLib 兼容契约

来源：`integration/MaLiLibIntegration.java`。

必须验证并保持：

- mod id 检测值 `malilib`。
- `InputEventHandler.getKeybindManager().getKeybindCategories()` → category hotkeys 的发现路径。
- 显示类别 `KeybindCategory.getModName()`、名称 `IHotkey.getConfigGuiDisplayName()`、ID `IHotkey.getName()`、注释 `IHotkey.getComment()`。
- 最多 4 键及键顺序。
- MaLiLib 鼠标负数键码与 InputConstants 0–7 之间的 `±100` 变换。
- `IKeybind.clearKeys()` 后逐项 `addKey()` 的写入方式。
- 默认判断 `!isModified()` 与 `resetToDefault()`。
- 保存 `ConfigManager.saveAllConfigs()`，随后 `updateUsedKeys()`；当前有 `UnstableApiUsage` 抑制。
- MaLiLib 缺席时不加载/不调用集成类。此点需真实无 MaLiLib 实例**待人工验证**。

## 9. CommandKeys 兼容契约

来源：`integration/CommandKeysIntegration.java`。

必须验证并保持：

- mod id 检测值 `commandkeys`。
- 只读取 `CommandKeys.profile()` 当前 profile，而非全部 profile。
- `macroMap.entries()` 中 entry value 为 `Macro`、entry key 为 `Keybind` 的当前假设。
- 名称、ID、首条消息摘要与 `[+N]` 展示。
- 搜索覆盖全部宏消息。
- 键序为 limit key 再主 key；最大 2 键。
- 一键写成主 key，两键写成 limit + 主 key；写入调用 `Profile.setKey()` 与 `setLimitKey()`。
- 空绑定视为默认，重置清空；保存调用 `Config.save()`。
- 当前 provider 名为空导致快照跳过。不得在纯版本迁移中擅自增加 provider 名。
- CommandKeys 缺席时不加载/不调用集成类。此点需真实无 CommandKeys 实例**待人工验证**。

## 10. Mod Menu 兼容契约

- `fabric.mod.json` 的 `modmenu` entrypoint 保持为 `dev.xef2.visualkeymap.compact.modmenu.ModMenuImpl`。
- 配置工厂返回主 `VisualKeymapScreen`，不是仅含三项设置的 `ConfigScreen`。
- 传入屏幕必须作为返回父屏幕。
- Mod Menu 安装/缺席两种启动方式都需验证。

## 11. 待人工确认清单

以下不能仅凭本仓库源码确认：

1. 原版保存调用路径已确认：`VisualKeymapScreen.removed()` 调用 `super.removed()`，26.2/26.3 的 `OptionsSubScreen.removed()` 均调用 `Options.save()`。仍需人工验证完成、Esc、屏幕替换等实际路径是否都触发移除，以及文件系统错误时的真实结果。
2. 三个 required Mixin 在 26.2 参考实例和 26.3 目标实例的实际应用结果。
3. README 所述第三方 API 能否由仓库外 Java 包成功实现、Loader 是否能发现并调用。
4. MaLiLib、CommandKeys 与 Mod Menu 缺席时 JVM 类加载是否完全安全。
5. MaLiLib 26.3 键码编码，特别是鼠标键的负值约定。
6. CommandKeys 26.3 profile/macro 数据结构、limit key 语义与保存时机。
7. TinyFileDialogs 在目标操作系统上的取消、扩展名过滤、覆盖和错误行为。
8. 无效/不完整配置 JSON、快照 JSON、不可读写文件的真实用户体验；源码只对 `IOException` 做有限处理。

这些验证结果若暴露旧版本已有缺陷，应单独提议和审查，不能自动扩大 26.3 迁移范围。
