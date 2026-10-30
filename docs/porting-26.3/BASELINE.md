# Visual Keymap 26.2 功能基线

## 1. 调查范围与证据约定

- 调查源码：上游仓库 `https://github.com/USTC-XeF2/visual-keymap` 的 `main` 分支，提交 `053e736`（Minecraft `26.2`）。
- 正式标签 `v1.4.0-26.2` 位于前一提交 `ef4bf5d`；当前 `main` 在该标签之上增加了排序模式配置并把 CommandKeys 依赖更新到 `3.103.0+26.2`。为覆盖当前已有功能，本基线采用 `main@053e736`。
- 调查时工作区中没有源码或 Git 元数据，因此源码路径均指上游该提交中的仓库相对路径；本次只在工作区新增本目录下的调查文档。
- **源码已证实**：可直接由 Java、资源、Mixin 或构建文件确定。
- **待人工验证**：需要启动游戏、真实第三方模组、文件选择器或外部集成测试模组才能确认。文档不对这些行为作推测。
- 本文只描述当前行为。反常行为同样属于迁移回归基线，不代表建议修复。

## 2. 用户入口

| 功能 | 入口 | 关键实现 | 源码已证实的预期行为 | 验证方法 |
| --- | --- | --- | --- | --- |
| 原版控制菜单入口 | `选项 → 控制` | `mixin/ControlsScreenMixin.java`：`addKeymapOptionAndRedirect()` | Mixin 重定向 `ControlsScreen.addOptions()` 对 `OptionsList.addSmall(...)` 的调用，在原有选项控件之前加入“Visual Keymap...”按钮；点击后打开 `VisualKeymapScreen`，父屏幕为当前 `ControlsScreen`。 | 启动游戏进入控制菜单，确认按钮存在、原有控件仍存在，点击与返回路径正确。**待人工验证 Mixin 注入点在 26.2/26.3 的实际匹配。** |
| Mod Menu 入口 | Mod Menu 中本模组的配置按钮 | `compact/modmenu/ModMenuImpl.java`：`getModConfigScreenFactory()`；`fabric.mod.json` 的 `modmenu` entrypoint | 工厂直接创建 `VisualKeymapScreen`，父屏幕由 Mod Menu 传入。 | 分别在安装与未安装 Mod Menu 时启动；安装时打开配置页并验证返回。未安装时验证不会因 `ModMenuImpl` 缺失依赖而启动失败。后者为**待人工验证**。 |
| 独立快捷键入口 | 原版按键设置中的“Visual Keymap / Open Visual Keymap” | `VisualKeymap.onInitializeClient()` | 注册一个默认未绑定（`GLFW_KEY_UNKNOWN`）的客户端按键；每个客户端 tick 末尾循环消费点击并打开 `VisualKeymapScreen`，父屏幕为当时的 `client.gui.screen()`。 | 给快捷键绑定按键，在游戏内及其他屏幕场景触发，确认打开与返回行为；默认应显示为未绑定。 |

## 3. 主界面与键盘可视化

| 功能 | 入口 | 关键实现 | 源码已证实的预期行为 | 验证方法 |
| --- | --- | --- | --- | --- |
| 主界面布局 | 任一入口打开 Visual Keymap | `gui/screen/VisualKeymapScreen.java`：`addTitle()`、`addContents()`、`addFooter()`、`repositionElements()`；`mixin/HeaderAndFooterLayoutAccessor.java` | 顶部为标题和搜索框；内容区上半部为键盘、下半部为绑定列表；底部依次为导出、导入、配置、完成按钮。内容高度通过 `HeaderAndFooterLayout.contentsFrame` accessor 调整。 | 在多种窗口尺寸、GUI 缩放下检查遮挡、溢出、滚动与按钮可用性。 |
| 键盘布局 | 主界面上半部 | `gui/widget/KeyLayoutHelper.java`：`getLayout()`；`KeyboardWidget` 构造器 | 固定展示主键区、F1–F12、导航区、方向键，以及鼠标左/中/右/M4/M5；配置开启时还展示数字小键盘。键位由 `InputConstants.getKey("key.keyboard.*"/"key.mouse.*")` 解析。 | 对照实体布局逐键检查标签和点击目标；测试英文、简体中文和缺少专用翻译的键。 |
| 键位占用颜色 | 每个键帽 | `KeyboardWidget.KeyWidget.extractContents()` | 未绑定为深灰；恰好一个绑定为深绿；至少两个但无冲突为暗金色；存在冲突为暗红色。选中键白色边框；搜索命中的相关键黄色边框；否则黑色边框。 | 构造未绑定、单绑定、多绑定无冲突、冲突四种数据，检查颜色与边框。颜色常量分别为 `DARK_GRAY`、`0xFF006400`、`0xFFB8860B`、`0xFF8B0000`。 |
| 键帽提示 | 鼠标悬停键帽 | `KeyboardWidget.KeyWidget.updateBindings()` | 首行是金色粗体键名；冲突绑定以红色列出；其余绑定按顺序列出。非冲突显示受 `MAX_DISPLAYED_BINDINGS = 5` 限制，超出后显示“and %s more.../还有 %s 个...”。 | 给同一键构造 0、1、5、6 个以上绑定并悬停；同时检查冲突项与普通项组合。 |
| 键位选择与列表切换 | 点击键帽 | `VisualKeymapScreen.setSelectedKey()`、`getBindingsForKey()`、`getUnboundBindings()` | 初始列表只显示 `getKeyCodes().isEmpty()` 的绑定。点击某键后列表显示 `getFullKeyCodes()` 包含该键码的绑定；再次点击同一键取消选择并回到未绑定列表。切换键时取消当前待编辑绑定。 | 打开界面、选择不同键、重复点击同一键；确认列表和白色边框同步变化。 |
| 绑定列表行 | 主界面下半部 | `gui/widget/KeybindsListWidget.java`：`Entry` | 每行显示“类别 - 名称”、当前按键按钮和重置按钮；名称悬停提示由 `KeyBinding.getTooltip()` 产生；列表固定行高 20、行宽 340，并可滚动。 | 检查原版、MaLiLib、CommandKeys 行的名称、提示、按键文本、重置状态和滚动。 |
| 搜索 | 顶部搜索框 | `VisualKeymapScreen.addTitle()`；`KeyBinding.containsSearchText()`；`KeybindsListWidget.createEntries()` | 搜索**不删除/过滤**不匹配项，而是把匹配项稳定地排到前面，并将匹配行文字改为黄色粗体；相关键帽显示黄色边框。搜索词转小写后按空格拆分，所有非空词都必须出现在合并后的可搜索文本中。可搜索文本默认含类别、名称、完整键码显示名；集成可扩展。空搜索不匹配任何项。 | 输入名称、类别、按键名、多个词、大小写和空格；确认匹配项置顶但其他项仍在。MaLiLib 另测内部 hotkey 名；CommandKeys 另测宏消息。 |
| 排序 | 配置页选择排序模式后返回主界面 | `ModConfig.SortMode`；`ConfigScreen.getOptions()`；`KeybindsListWidget.getComparator()` | 第一优先级始终是搜索命中；若启用冲突置顶，第二优先级为冲突；之后才应用排序模式。`DEFAULT` 保留来源列表的稳定顺序，`BOUND_KEY` 按主键码列表再按 modifier 键码列表逐项升序，`DISPLAY_NAME` 按显示名忽略大小写排序。 | 为三种模式准备顺序可区分的数据，同时组合搜索与冲突置顶开关，确认优先级。 |
| 冲突判定与显示 | 键帽颜色、绑定按钮文字和提示 | `KeyBinding.getConflictBindings()`；`KeybindsListWidget.Entry.update()` | 只有两个以上绑定拥有完全相同的 `getFullKeyCodes()` 列表且 `getOrder()` 相同时才归为一组冲突。空主键绑定不在列表行上显示冲突。冲突绑定按钮文字为金色，提示列出同组的其他绑定。 | 构造同键同 order、同键不同 order、同组合键但顺序不同、未绑定等组合。`order` 的原版含义与 26.3 映射需另行确认。 |

## 4. 键位读取、修改与保存

| 功能 | 入口 | 关键实现 | 源码已证实的预期行为 | 验证方法 |
| --- | --- | --- | --- | --- |
| 开始录入 | 点击绑定列表中的当前按键按钮 | `KeybindsListWidget.Entry` 按钮回调；`VisualKeymapScreen.SharedData.selectedKeyBinding` | 目标按钮显示黄色的 `> 当前键 <` 状态。 | 对每种 provider 点击编辑按钮并观察状态。 |
| 键盘录入 | 编辑状态下按键/松键 | `VisualKeymapScreen.keyPressed()`、`keyReleased()`、`setKeyBinding()` | 不重复记录同一个 `InputConstants.Key`。非 Esc 按下时加入列表并立即调用 `setBoundKeys()`；按 Esc 不加入键并结束，因列表为空会清除绑定。达到 provider 的 `maxBoundKeys` 时结束；否则任意键释放会再次应用并结束。由此，多键绑定按按下顺序记录，必须在首键释放前按下后续键。 | 原版（最大 1）、CommandKeys（最大 2）、MaLiLib（最大 4）分别测试单键、同时按住的组合键、重复按键、提前释放、Esc 清除。 |
| 鼠标录入 | 编辑状态下点击鼠标键 | `VisualKeymapScreen.mouseClicked()` | 把鼠标按钮转换为 `InputConstants.Type.MOUSE` 键，立即应用并结束编辑。 | 测试左/中/右/M4/M5；确认界面点击本身不会同时触发其他控件。 |
| 重置 | 每行“Reset/重置” | `KeybindsListWidget.Entry`；各 provider 的 `resetToDefault()` | 仅 `isDefault()` 为 false 时可点击。重置后刷新键盘颜色和当前行状态。原版恢复原始默认键；MaLiLib 调用自身默认恢复；CommandKeys 清空绑定。 | 修改后三类绑定，逐一重置并重启验证。 |
| 退出时保存 | 主界面被移除 | `VisualKeymapScreen.removed()`；`VisualKeymap.saveKeyBindings()`；原版 `OptionsSubScreen.removed()` | 先对所有注册的 `VisualKeymapApi` 实现调用 `save()`，随后调用 `super.removed()`；26.2 官方类字节码确认后者调用 `Minecraft.options.save()`。因此原版键位与其他 options 的保存触发点也是主界面移除，不限于点击“完成”。 | 分别用完成、Esc、切换到其他屏幕离开并重启验证各 provider。保存调用路径已证实；真实文件 I/O 成功、错误日志和平台行为仍需人工验证。 |
| 显示文本 | 列表按键按钮 | `KeyBinding.getBoundKeysLocalizedText()` | 未绑定使用原版 `key.keyboard.unknown`；modifier 以斜体 `[ modifier + ] ` 前缀显示；多个主键以 ` + ` 连接。 | 覆盖普通键、鼠标键、组合键、原版 debug modifier。 |

## 5. 配置

| 配置项 | 默认值 | 关键实现 | 源码已证实的行为 | 验证方法 |
| --- | --- | --- | --- | --- |
| 显示数字小键盘 `showNumpad` | `true` | `ModConfig`；`ConfigScreen.getOptions()`；`VisualKeymapScreen.addContents()` | 决定创建 `KeyboardWidget` 时是否请求完整布局。 | 开/关后返回重建的主界面，确认小键盘出现/消失。 |
| 置顶冲突 `prioritizeConflictingKeybinds` | `true` | `ModConfig`；`KeybindsListWidget.getComparator()` | 启用时，搜索优先级之后把冲突项排在非冲突项前。 | 组合开关、搜索和三种排序模式验证。 |
| 排序模式 `sortMode` | `DEFAULT` | `ModConfig.SortMode`；`ConfigScreen`；语言资源 | 值为 `default`、`bound_key`、`display_name`，界面提供对应本地化文本。 | 切换、保存、重启后确认值和排序效果。 |
| 配置文件 | 不适用 | `ModConfig.load()`、`save()` | 文件固定为 Fabric config 目录下 `visualkeymap.json`。客户端初始化末尾加载；不存在或反序列化结果为 null 时使用默认值；随后无条件写回。配置页被移除时写回。I/O 错误只记日志。 | 首次启动检查自动创建；修改后检查 JSON 与重启恢复；只读目录、无效 JSON 的实际结果列为**待人工验证**。 |

配置页由主界面底部“Config Options/配置选项”打开。其父屏幕是一个重新创建的 `VisualKeymapScreen`，因此返回时会按当前配置重建键盘和列表，而不是复用原屏幕实例（`VisualKeymapScreen.addFooter()`）。

## 6. 导出与导入

| 功能 | 入口 | 关键实现 | 源码已证实的预期行为 | 验证方法 |
| --- | --- | --- | --- | --- |
| 导出文件选择 | 底部“Export/导出” | `VisualKeymapScreen.doExport()` | 使用 LWJGL TinyFileDialogs 保存对话框，只接受 `*.json`；默认路径为 Fabric config 目录下 `visualkeymap-export.json`。取消不执行操作；`IOException` 被静默忽略。 | 在 Windows/Linux（若支持）测试默认路径、改名、取消、不可写目标；错误是否无 UI 反馈。 |
| 快照内容 | 确认导出路径 | `KeymapSnapshot.fromBindings()`、`writeToFile()` | JSON 顶层含 `version: 1` 与 `bindings`。只导出 provider 名非空且 `isDefault() == false` 的绑定。每项含 `provider`、`id`、`keys`、`modifiers`。 | 修改原版与各集成绑定后导出，逐项核对。当前 CommandKeys provider 名为空，因此源码确定其条目被跳过。 |
| 导入匹配 | 底部“Import/导入” | `VisualKeymapScreen.doImport()`；`KeymapSnapshot.readFromFile()`、`matchBindings()` | 使用 TinyFileDialogs 单选 JSON。按 provider 字符串精确匹配 API，再按绑定 `id` 精确匹配。仅当当前 `getKeyCodes()` 与条目 `keys` 不相等时进入预览；快照 `version` 未在源码中校验。取消或无差异时保持当前屏幕。 | 导入同一文件、不同 provider/id、未知条目、不同版本号、无匹配条目；确认是否打开预览。无差异时没有消息提示。 |
| 导入预览 | 有匹配且主键不同 | `gui/screen/ImportPreviewScreen.java` | 标题显示变更数；每项默认勾选，显示绑定名、旧键和 `→` 绿色新键；可逐项取消。确认只应用仍勾选项，调用所有 provider 的 `save()`，刷新父 `VisualKeymapScreen` 并返回；取消不修改。 | 测试全选、部分取消、全部取消、取消按钮和 Esc；重启验证保存。 |
| modifiers 现状 | 导出/导入组合键 | `KeymapSnapshot.BindingEntry`、`ImportMatch.getKeysFromEntry()`、`ImportPreviewScreen` | 导出会写 `modifiers`，但差异判断只比较 `keys`，导入应用也只把 `entry.keys` 转成按键传给 `setBoundKeys()`；`entry.modifiers` 未在导入路径使用。这是当前源码行为，迁移阶段不得未经授权改变。 | 用原版 debug modifier 或自定义 provider 构造仅 modifier 不同的快照，确认不会进入预览/不会应用 modifier。 |

## 7. Provider 可见行为

| Provider | 关键实现 | 显示、读取与修改行为 | 验证方法 |
| --- | --- | --- | --- |
| Minecraft | `api/MinecraftImpl.java`、`mixin/KeyMappingAccessor.java`、`VisualKeymapScreen.removed()` | 枚举 `Minecraft.getInstance().options.keyMappings`。类别为 `KeyMapping.Category.label()`，名称为 `keyMapping.getName()` 的翻译，ID 为该名称。最大 1 键；通过 accessor 读取私有 `key`、`order`；写入使用 `KeyMapping.setKey()` 后 `KeyMapping.resetMapping()`。默认与重置委托 `KeyMapping.isDefault()` / `getDefaultKey()`。若绑定位于 `options.debugKeys`，把 `options.keyDebugModifier` 作为 modifier。provider 的 `save()` 虽为空，但主屏幕随后经 `OptionsSubScreen.removed()` 调用原版 `Options.save()`。 | 覆盖普通原版键、其他 Fabric 注册键、本模组打开键、未绑定键、debug 组合键；修改后测试实际触发与重启持久化。 |
| MaLiLib | `integration/MaLiLibIntegration.java` | 仅安装 `malilib` 时加入。枚举 keybind categories 及 hotkeys；类别显示 `category.getModName()`，名称显示 `getConfigGuiDisplayName()`，ID 为 `hotkey.getName()`，注释为 `hotkey.getComment()`，最大 4 键。搜索额外包含内部 hotkey 名。MaLiLib 小于 `-1` 的键码读取时加 100；写入时鼠标码 0–7 减 100。保存调用 `ConfigManager.saveAllConfigs()` 并刷新 `updateUsedKeys()`。 | 安装真实 MaLiLib 及至少一个使用其热键的模组；覆盖键盘、鼠标、组合键、注释、默认恢复、实际触发和重启。 |
| CommandKeys | `integration/CommandKeysIntegration.java` | 仅安装 `commandkeys` 时加入。只读取当前 `CommandKeys.profile()` 的 `macroMap`。类别为 `key.category.commandkeys.main`；名称含 profile，宏名非空时追加宏名；ID 为 `profile.name + ":" + macro.name`；最大 2 键。显示第一条消息（空白时 `...`），多余消息显示 `[+N]`；搜索包含所有消息。读取顺序为 limit key、主 key；写入 1 键时作为主 key，2 键时第一键为 limit、第二键为主 key。未绑定即默认，重置为未绑定；保存调用 `Config.save()`。 | 用多个 profile、重名/空名宏、多消息、单键、limit+主键、鼠标键测试。当前只显示活动 profile；切换 profile 后重新打开界面验证刷新。 |

## 8. 本地化与资源

- `assets/visualkeymap/lang/en_us.json` 与 `zh_cn.json` 覆盖界面标题、按钮、配置、快捷键类别、特殊键名和导入导出文本。
- 普通键名优先使用 `visualkeymap.key.<layout-key>`；不存在时回退到 `InputConstants.Key.getDisplayName()`（`KeyboardWidget` 构造器）。
- `assets/visualkeymap/icon.png` 是模组图标，`fabric.mod.json` 直接引用。
- `docs/screenshot.png` 与 `docs/screenshot-zh-CN.png` 展示不含数字小键盘的主界面、搜索置顶/高亮、键帽颜色和绑定列表。截图只能作为视觉参考，不替代运行时验证。

## 9. 明确不纳入本次迁移目标的事项

- 不改变上述现有交互、文件格式、provider ID、排序/冲突规则或入口路径。
- 不把静默错误处理、导入 modifiers 使用情况、CommandKeys 导出情况、API 可扩展性等现状顺带改造成新行为。
- 不新增功能、不重新设计 UI、不优化架构；若 26.3 迫使签名适配，应以保持本基线的可观察行为为准。
