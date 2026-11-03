# BUG-01A：非法 JSON 导入崩溃的跨版本根因调查

## 1. 调查结论

BUG-01 的直接根因已经确认：`KeymapSnapshot.readFromFile()` 使用 Gson 反序列化用户选择的文件，Gson 对非法 JSON 或根节点类型错误抛出未检查的 `JsonParseException`（本次实际为 `JsonSyntaxException`）；`VisualKeymapScreen.doImport()` 只捕获 `IOException`，因此该异常沿客户端线程调用栈逃逸。Minecraft 26.3 将其视为主循环中的未报告异常，生成 `Unexpected error` 崩溃报告并终止客户端。

Minecraft 26.3 的 SDL 异步文件对话框不是 JSON 解析错误的来源，但它改变了异常传播路径：文件路径由 SDL 回调放入队列，随后 `NativeFileDialog.poll()` 在 `END_CLIENT_TICK` 上调用导入 consumer。未捕获的解析异常因此直接进入 Minecraft 客户端主循环的通用 `Throwable` 处理分支。

Minecraft 26.2 原版具有同一个未处理异常缺陷：相同的 `readFromFile()` 实现、相同的 Gson 2.14.0，并且同步 tinyfd 导入路径同样只捕获 `IOException`。使用两版实际编译类执行的解析探针，对本次 `invalid.json` 均得到相同的 `JsonSyntaxException`。但是，现有材料没有 26.2 的日志或崩溃报告，当前自动化环境也无法操作原生 Minecraft 窗口，所以用户观察到的“界面没有明显反应、未观察到崩溃”尚不能被解释为 26.2 内部吞掉了异常。

推荐采用两层最小修复：

1. 在 `KeymapSnapshot.readFromFile()` 完成最小结构校验，拒绝 Gson 返回的 `null` 以及缺少 `bindings` 的对象，并以精确的 `JsonParseException` 子类报告数据格式错误；不要吞异常或返回伪造的空快照。
2. 在 `VisualKeymapScreen.doImport()` 的用户导入边界分别捕获 `IOException` 和 `JsonParseException`，记录可诊断日志并安全返回；不要在 `NativeFileDialog` 中捕获所有 consumer 异常。

这不会改变 version 1 schema、公开方法签名、BUG-02 匹配算法、modifiers 语义或键码保护。

## 2. 调查范围与 Git 状态

| 项目 | 结果 |
| --- | --- |
| 26.3 基线 | `port/mc-26.3`，`7d41278` |
| 调查分支 | `fix/bug01-invalid-json-26.3`，从 `7d41278` 创建 |
| 26.2 源码基线 | `053e736c77b15e0727c93abed7c94c3b0fb64618` |
| 26.2 调查工作树 | 独立 detached worktree `.tmp/bug01-26.2-baseline` |
| 生产源码修改 | 无 |
| BUG-02 修改 | 未修改、未回滚 |
| 私人证据 | 只读分析，未加入 Git |

主工作树原有未跟踪文件 `docs/bugfixes/EVIDENCE-INVENTORY.md` 未被修改、删除或加入提交。

## 3. 原始证据与完整性

私人材料位于工作区的 `baseline-private/bug-investigation/bug-01/`。下表只记录相对位置和哈希，不提交原文件。

| 文件 | 大小 | SHA-256 | 用途 |
| --- | ---: | --- | --- |
| `invalid.json` | 92 B | `824737B0E86E4428F8E8D98BCAD368BF86B77E13EC22F0FE58E1D89A6320E69B` | 用户实际选择的非法输入 |
| `RawOutput.log` | 43,008 B | `8E1425B6FE941DAE77329CAACA5F6F71F90CBC3F09468E9ECA244669F2987AED` | 26.3 运行输出 |
| `crash report.zip` | 53,693 B | `2918864866F3F67D2908C7C03CAC1D85272E35DF6FE95EAF393FD7A31FE7F31D` | 崩溃报告、`latest.log` 与启动器诊断材料 |

ZIP 中关键文件：

| ZIP 条目 | SHA-256 |
| --- | --- |
| `crash-2026-11-02_13.58.26-client.txt` | `C51932F354D02E2C699A06072A009F4BD168F2984E2E20B88FC4114F85F154E9` |
| `latest.log` | `32457672D81866AB493F0831B208F09E564318CF044BDD8EB6C5416CBDD25DDC` |
| `26.3-Fabric 0.20.1.json` | `8123BCB067D5442453164723E14B25CCEA70FCB50104FB582CE35084B00C178B` |

`invalid.json` 是 92 个 ASCII 字节，首字符为 `e`，不是合法 JSON。原文件未被修改。日志没有记录被选择文件的完整路径，因此“该哈希文件就是该次崩溃所选文件”由用户材料目录和异常内容共同支持，但不是由日志路径直接证明；这一证据链标记为 **PARTIAL**。

## 4. Minecraft 26.3 实际崩溃链

### 4.1 动态证据

崩溃报告明确记录：

- Minecraft 26.3
- Fabric Loader 0.20.1
- Visual Keymap 1.4.0
- 线程：Render thread
- 异常：`com.google.gson.JsonSyntaxException`
- 根因：`Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $`

调用链为：

```text
Gson.fromJson(...)
KeymapSnapshot.readFromFile(KeymapSnapshot.java:47)
VisualKeymap.importKeyBindings(VisualKeymap.java:89)
VisualKeymapScreen.lambda$doImport$0(VisualKeymapScreen.java:121)
NativeFileDialog.poll(NativeFileDialog.java:62)
VisualKeymap.lambda$onInitializeClient$1(VisualKeymap.java:63)
ClientTickEvents.END_CLIENT_TICK
Minecraft.tick(...)
Minecraft.run(...)
```

`latest.log` 在 13:58:25 记录 `Unreported exception thrown!`，13:58:26 开始停止并保存集成服务器，随后生成客户端崩溃报告。该结果为 **动态确认**。

### 4.2 源码机制

- `src/main/java/dev/xef2/visualkeymap/KeymapSnapshot.java`，`readFromFile()`（48–51 行）：直接返回 `GSON.fromJson(...)`，只声明 `IOException`，不验证返回值和结构。
- `src/main/java/dev/xef2/visualkeymap/VisualKeymap.java`，`importKeyBindings()`（88–90 行）：原样委托给 `readFromFile()`，没有异常处理。
- `src/main/java/dev/xef2/visualkeymap/gui/screen/VisualKeymapScreen.java`，`doImport()`（116–134 行）：consumer 内只捕获 `IOException`；`JsonSyntaxException` 是运行时异常，不在该 catch 范围内。
- `src/main/java/dev/xef2/visualkeymap/gui/NativeFileDialog.java`，`poll()`（51–68 行）：在客户端线程调用 `selectedPathConsumer.accept(path)`；`finally` 只安排原生资源清理，不捕获 consumer 异常。
- `src/main/java/dev/xef2/visualkeymap/VisualKeymap.java`，`onInitializeClient()`（62–67 行）：在 `END_CLIENT_TICK` 直接调用 `NativeFileDialog.poll()`。

解析失败发生在构造 `KeymapSnapshot` 期间，早于 `matchBindings()`、导入预览、`setBoundKeys()` 和导入确认。因此本次异常路径本身不会应用快照键位，也不会从导入逻辑调用 provider `save()`。完整崩溃退出是否触发其他 Minecraft 生命周期行为不应与导入成功混为一谈，回归测试仍需用 spy 明确确认零写入、零保存。

## 5. Minecraft 26.2 与 26.3 对比

### 5.1 Visual Keymap 源码差异

| 环节 | 26.2 原版 | 26.3 迁移版 | 结论 |
| --- | --- | --- | --- |
| 文件对话框 | `TinyFileDialogs.tinyfd_openFileDialog()` 同步返回路径 | SDL `NativeFileDialog.showOpen()` 异步返回，结果由 `poll()` 分发 | 调度方式变化 |
| 取消 | tinyfd 返回 `null`，不进入导入 | SDL 结果路径为 `null`，`poll()` 不调用 consumer | 两版都与解析失败区分 |
| 解析线程 | 按钮回调所在的 render/client 线程 | `END_CLIENT_TICK` 的 render/client 线程 | 两版均在客户端主线程解析 |
| 解析实现 | `GSON.fromJson(reader, KeymapSnapshot.class)` | 相同 | 没有迁移差异 |
| UI 捕获 | 只捕获 `IOException` | 只捕获 `IOException` | 同一缺陷 |
| 传播入口 | 按钮回调调用栈 | `NativeFileDialog.poll()` → tick | 26.3 崩溃报告的直接调用链变化 |

26.2 位置：

- `KeymapSnapshot.readFromFile()`：44–47 行。
- `VisualKeymapScreen.doImport()`：124–151 行。
- `tinyfd_openFileDialog()`：130–136 行。
- 只捕获 `IOException`：148–149 行。

### 5.2 Gson 与 Minecraft 顶层处理

Loom 缓存中的 Mojang 版本清单显示：

| Minecraft | Java major | Gson |
| --- | ---: | --- |
| 26.2 | 25 | `com.google.code.gson:gson:2.14.0` |
| 26.3 | 25 | `com.google.code.gson:gson:2.14.0` |

对两版 mapped Minecraft JAR 的 `net.minecraft.client.Minecraft.run()` 进行 `javap -c -p` 对比：

- 两版主循环都捕获 `ReportedException` 并调用 `emergencySaveAndCrash()`。
- 两版都另行捕获通用 `Throwable`，记录 `Unreported exception thrown!`，构造标题为 `Unexpected error` 的 `CrashReport`，然后调用 `emergencySaveAndCrash()`。
- 相邻的事件轮询差异是 26.2 调用无参 `RenderSystem.pollEvents()`，26.3 调用带 `SDLEventHandler` 的重载；这不改变从 tick 逃逸异常的顶层致命处理。

所以没有源码或字节码证据支持“26.2 会由 Minecraft 自动吞掉同一异常”。若同一文件在 26.2 到达 Gson 且异常逃逸，顶层策略同样会将其作为致命异常处理。

### 5.3 26.2 动态验证状态

本次创建了干净的 26.2 基线 worktree，并用 JDK 25、Gradle 9.8.2、Loom 1.19.3 的临时 init script 成功启动隔离客户端：日志确认 Minecraft 26.2、Fabric Loader 0.19.3、Visual Keymap 1.4.0、MaLiLib 0.29.0 和 Mod Menu 20.0.0-beta.3 已加载，独立 gameDir 为 `.tmp/bug01-26.2-game`。

当前桌面自动化接口不能枚举或控制原生 Minecraft 窗口，因此未能可靠执行 tinyfd 文件选择。客户端在没有执行导入测试的情况下被主动终止。该次启动只证明测试环境可用，不证明 26.2 GUI 的导入结果。

当前证据状态：

| 事项 | 状态 | 说明 |
| --- | --- | --- |
| 用户在 26.2 看到“无明显反应” | USER-REPORTED | 没有对应日志、崩溃报告或录像可供交叉验证 |
| 26.2 源码存在未捕获路径 | STATICALLY VERIFIED | 与 26.3 同样只捕获 `IOException` |
| 26.2 对原始 `invalid.json` 的底层解析 | PROGRAMMATICALLY VERIFIED | 实际 26.2 编译类抛出与 26.3 相同的 `JsonSyntaxException` |
| 26.2 原生 GUI 端到端 | NOT TESTED | 当前自动化不能操作原生窗口 |

因此，BUG-01 可归类为 26.2 源码中已存在的潜在上游缺陷，并在 26.3 有完整动态崩溃证据；但“为什么用户那次 26.2 没有看到崩溃”仍是待确认事项，不能归因于 tinyfd、Gson 或 Minecraft 的异常策略。

## 6. 输入边界探针

调查使用两版实际 `KeymapSnapshot` 编译类和同一 Gson 2.14.0 运行只读探针。该探针不是 GUI 端到端测试，只用于确认解析和 `matchBindings()` 的边界行为。

| 输入 | 26.2 | 26.3 | 当前风险分类 |
| --- | --- | --- | --- |
| 原始 `invalid.json` | `JsonSyntaxException` | 相同 | 应安全拒绝 |
| 空文件 | 返回 `null` | 相同 | 随后在 UI 路径 NPE，应安全拒绝 |
| 不完整对象 `{"version":1,` | `JsonSyntaxException`（EOF） | 相同 | 应安全拒绝 |
| JSON 字符串标量 | `JsonSyntaxException` | 相同 | 应安全拒绝 |
| JSON 数字标量 | `JsonSyntaxException` | 相同 | 应安全拒绝 |
| JSON `null` | 返回 `null` | 相同 | 随后 NPE，应安全拒绝 |
| JSON 数组 | `JsonSyntaxException` | 相同 | 应安全拒绝 |
| `{}` | `version=0, bindings=null`；匹配时 NPE | 相同 | 应安全拒绝 |
| `{"version":1}` | `bindings=null`；匹配时 NPE | 相同 | 应安全拒绝 |
| version 类型错误 | `JsonSyntaxException` | 相同 | 应安全拒绝 |
| bindings 类型错误 | `JsonSyntaxException` | 相同 | 应安全拒绝 |
| `{"version":1,"bindings":[]}` | 正常，0 matches | 相同 | 有效 version 1 |
| `{"version":2,"bindings":[]}` | 当前接受，0 matches | 相同 | 现有兼容边界，见 Decision Required |
| 文件不存在 | `FileNotFoundException` | 相同 | I/O 错误，应与 JSON 错误区分 |

这表明只补抓 `JsonSyntaxException` 不足以覆盖空文件、JSON `null` 和缺失 `bindings` 的对象；这些输入会在解析之后以 `NullPointerException` 逃逸。最小结构校验是必要的，但不应使用 `catch (Exception)` 掩盖后续 provider 或匹配代码中的真实编程错误。

## 7. 异常处理边界评估

### A. `KeymapSnapshot.readFromFile()`

责任：文件内容到快照数据模型的解析与最小结构合法性。

建议：

- 保留 `IOException` 表示文件打开/读取失败。
- 让 Gson 的 `JsonParseException` 表示语法、根类型和字段类型错误。
- Gson 返回 `null` 或 `bindings == null` 时，主动抛出 `JsonSyntaxException`（或内部等价的 `JsonParseException` 子类），避免下游 NPE。
- 不在这里记录 GUI 提示、不返回空快照、不吞异常。

不建议在这一层把所有错误转成 `IOException`：格式错误不是 I/O 故障，而且会丢失诊断类别。

### B. `VisualKeymap.importKeyBindings()`

这是现有公开静态入口。建议继续委托 `readFromFile()`，不增加 checked exception，不改变参数、返回类型或签名。运行时 `JsonParseException` 可以原样传播给调用方；GUI 调用方负责处理用户输入失败。

在该层吞异常或返回 `null` 会使外部调用方无法区分成功与失败，也会制造新的 NPE，因此不推荐。

### C. `VisualKeymapScreen.doImport()`

这是推荐的用户错误处理边界。它知道操作来自用户选择文件，也负责是否进入预览。

建议分别处理：

- `IOException`：文件不存在、不可读或读取失败。
- `JsonParseException`：JSON 语法、类型或最小结构错误。

处理后应：

- 不调用 `matchBindings()`、不打开预览、不应用绑定、不保存 provider。
- 记录包含异常类型和脱敏文件名的 WARN/ERROR；不要记录快照内容。
- 保持当前 `VisualKeymapScreen` 可继续使用，允许下一次导入。
- 不捕获任意 `RuntimeException`、provider 异常或 `Error`，这些仍应暴露为真实缺陷。

### D. `NativeFileDialog.poll()`

责任是 SDL 原生资源生命周期、取消语义和把已选路径分发回客户端线程，不是理解 Visual Keymap JSON。

不建议让该层捕获并吞掉所有 consumer 异常：它同时服务导入与导出，广泛捕获会隐藏业务代码缺陷。现有 `finally` 已确保即使 consumer 失败也会安排原生资源清理；预期的导入错误应在 C 层被精确处理。

## 8. 推荐最小修复方案

### 8.1 生产代码范围

建议只修改：

1. `src/main/java/dev/xef2/visualkeymap/KeymapSnapshot.java`
   - 在 `GSON.fromJson()` 后验证结果不为 `null` 且 `bindings` 不为 `null`。
   - 对结构缺失抛出精确的 `JsonSyntaxException`。
   - 不修改 version 1 字段、`BindingEntry`、`matchBindings()` 或 `ImportMatch`。
2. `src/main/java/dev/xef2/visualkeymap/gui/screen/VisualKeymapScreen.java`
   - 在 `doImport()` 的 consumer 内分别捕获 `IOException` 与 `JsonParseException`。
   - 记录错误，安全返回并保持当前 screen。
   - 不调用保存。
3. 如维护者批准可见反馈，再最小增加英文/中文本地化键；不改正常导入交互。

不需要修改：

- `NativeFileDialog`
- `VisualKeymap.importKeyBindings()` 的公开签名
- `ImportPreviewScreen` / `ImportSelection`
- BUG-02 的分组匹配和一对一选择
- `MinecraftInputCode`、F25 tombstone 或未知扩展键保护
- MaLiLib / CommandKeys adapter

### 8.2 用户反馈选择

最低限度且最接近 26.2 用户观察的行为是：记录日志、保持当前界面、不显示预览。更易理解的方案是增加一个本地化错误提示，并在确认后返回同一个 `VisualKeymapScreen`。

若使用提示，建议由 `VisualKeymapScreen` 显示，而不是由解析器或文件对话框显示。`AlertScreen` 在 26.3 有稳定构造器，可明确返回父屏幕；非模态 toast 也可行，但需要选择合适的 toast token。由于这会改变非法输入时的用户可见行为，列为维护者决策，不作为防崩溃的必要条件。

### 8.3 失败后的状态保证

实现应先完成完整解析和最小结构验证，再调用 `matchBindings()`。任何已知输入错误发生时：

- live binding 写入次数 = 0
- provider `save()` 调用次数 = 0
- 导入预览创建次数 = 0
- 当前 screen 仍可操作
- 下一次有效导入可正常进入 BUG-02 安全预览流程

## 9. 对正常导入与 BUG-02 的影响

- 有效 version 1 快照继续走原有 `matchBindings()`，行为不变。
- BUG-02 的重复 `(provider, id)` 分组、一对一选择和歧义默认不应用均不需要修改。
- modifiers 仍只序列化，不因本修复改变导入差异判断或应用语义。
- F25 tombstone 与未知 extension 的 `ImportMatch.canApply()` 仍在解析成功后的预览/应用阶段生效；非法 JSON 不会到达该阶段。
- 取消对话框仍由 `path == null` 表示，consumer 不被调用，因此不会被记录为解析失败。
- CommandKeys 仍不参与 version 1 快照；第三方 provider 逻辑不受影响。

## 10. 自动化回归测试设计

### 10.1 解析与结构测试

新增 JUnit 5 测试，使用临时文件，覆盖：

1. 原始 `invalid.json` 的未修改副本：抛出 `JsonParseException`。
2. 空文件：安全拒绝，不返回 `null`。
3. 非法 JSON 语法：安全拒绝。
4. 合法字符串、数字、数组及 JSON `null`：安全拒绝。
5. `{}`、仅有 `version`、bindings 类型错误、version 类型错误：安全拒绝。
6. 有效 version 1 空快照与代表性真实快照：正常读取。
7. 不存在/不可读文件：保持 `IOException` 类别，不被误报为 JSON 错误。

### 10.2 导入控制流测试

将“读取并决定是否进入预览”的内部流程保持 package-private/private 可测试，或通过最小 spy fixture 验证：

1. 每一种无效输入均为零 `matchBindings`、零 `setBoundKeys`、零 `save`。
2. 无效输入之后立刻导入有效 version 1，正常产生预览。
3. 文件选择取消不调用解析器、不记录解析失败。
4. I/O 错误与 JSON 错误进入不同结果/日志分支。
5. 不捕获由 provider/matching 人为抛出的非解析 `RuntimeException`，避免掩盖代码缺陷。

### 10.3 既有回归

继续运行现有全部测试，特别是：

- `KeymapSnapshotMatchingTest`：BUG-02 重复身份、顺序变化、重复 keys、modifiers、鼠标和扩展键。
- `ImportSelectionTest`：一对一选择、取消、F25 tombstone 和未知 extension 不可应用。
- `CanonicalInputCodeTest` 与 `MaLiLibInputCodeTest`。

修复阶段至少执行：

```text
.\gradlew.bat clean test build --no-daemon
```

### 10.4 真实客户端测试

在独立 26.3 gameDir 中人工执行：

1. 选择原始 `invalid.json` 的测试副本，确认不崩溃且没有绑定变化。
2. 依次测试空文件、非法语法、标量、字段缺失、字段类型错误。
3. 测试取消，确认没有错误提示或日志误报。
4. 失败后不关闭 Visual Keymap，立即导入有效 version 1 快照，确认预览、确认与保存仍正常。
5. 验证 BUG-02 重复 ID 快照仍显示安全歧义逻辑。
6. 验证 F25/未知 extension 仍不可应用。
7. 完全退出并重启，确认失败输入未造成任何持久化变化。

26.2 如要向上游提交独立修复，应在 tinyfd 同步路径执行同一组测试；当前仍缺少该版本的 GUI 端到端证据。

## 11. Decision Required

以下两项会改变当前边界行为，建议在 BUG-01B 实施前由维护者确认：

1. **非法输入的用户可见反馈**
   - 方案 A（最小兼容）：记录日志，保持界面，无弹窗。
   - 方案 B（推荐可用性）：显示本地化 `AlertScreen`，确认后返回 Visual Keymap；同时记录日志。
   - 两者都能满足防崩溃和零写入要求；差异只在非法输入时的用户体验。
2. **未知快照 version**
   - 当前 26.2/26.3 都接受 `{"version":2,"bindings":[]}`，且对带 bindings 的未知版本会按现有字段继续匹配。
   - 从格式安全角度应拒绝非 version 1，但这属于既有行为变化，不应在 BUG-01 中静默加入。
   - 建议 BUG-01B 先为未知 version 建立锁定现状的测试，除非维护者另行批准“明确拒绝未知 version”。

文件为空、根节点为标量/null、缺少 bindings 或字段类型错误不具备可用的 version 1 语义，建议直接纳入 BUG-01 的安全拒绝范围，不需要改变 schema。

## 12. 最终判断

根因调查已经足以进入 BUG-01B：26.3 的致命传播路径、两版源码差异、Gson 行为、客户端顶层异常策略以及修复边界均有实际证据。

仍未完成的是 26.2 原生 GUI 的端到端复现，以及非法输入时采用“仅日志”还是“可见提示”、未知 version 是否拒绝的维护者决策。前者不阻碍 26.3 修复；后两项应在实施前明确，以避免把历史行为变化混入最小补丁。
