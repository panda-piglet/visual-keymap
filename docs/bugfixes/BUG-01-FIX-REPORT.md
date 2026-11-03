# BUG-01B：非法 JSON 导入崩溃最小修复报告

## 1. 结论

BUG-01B 已在独立分支 `fix/bug01-invalid-json-26.3` 完成实现、自动化测试和发布 JAR 冒烟验证。修复精确区分 JSON/结构错误与 I/O 错误，显示英中本地化 `AlertScreen`，并确保失败发生在匹配、写入和保存之前。

自动化结果为 **39/39 JUnit PASS**；生产 JAR 在 P1、P3、P5、P6 四种组合下均通过实际客户端启动探针。维护者随后在 Minecraft 26.3 实际客户端中完成 `BUG-01-MANUAL-TEST.md` 的 M-01 至 M-10，报告 **10/10 PASS**。人工结果由维护者提供，不是 Agent 自行执行；未提供新的截图、录像、日志或哈希。

## 2. Git 与范围

| 项目 | 结果 |
| --- | --- |
| 迁移基线 | `port/mc-26.3`，`7d41278` |
| 修复分支 | `fix/bug01-invalid-json-26.3` |
| 调查报告 | `51de118` — `docs: document BUG-01 invalid JSON root cause` |
| 先行失败测试 | `e9d9919` — `test: reproduce invalid snapshot import failures` |
| 生产修复 | `8368fe6` — `fix: reject invalid keymap snapshots safely` |
| 副作用与生产冒烟测试 | `b6f66f8` — `test: verify invalid import remains side-effect free` |
| 本报告与人工清单 | 本文档所在提交 |

未执行 merge、push、tag 或 Release。`port/mc-26.3` 未被修改。私人 `baseline-private/` 证据没有加入 Git。

## 3. 生产源码修改

### 3.1 `KeymapSnapshot.readFromFile()`

文件：`src/main/java/dev/xef2/visualkeymap/KeymapSnapshot.java`

- 保留 `FileReader` 的 `IOException` 语义。
- 保留 Gson 对语法、根节点和字段类型错误抛出的 `JsonParseException`。
- 对 Gson 返回 `null`、缺少 `bindings`、`bindings` 中的 null 记录，以及 keys/modifiers 中的 null 整数执行最小结构校验并抛出 `JsonSyntaxException`。
- 不返回伪造空快照，不捕获通用 `Exception`。
- 保留 `{}` binding 的历史归一化：空 provider、空 id、空 keys 和 modifiers。
- 未知 version 与缺失 version 的既有行为不变：前者继续保留其数值，后者仍为历史默认值 0。

### 3.2 `VisualKeymapScreen.doImport()`

文件：`src/main/java/dev/xef2/visualkeymap/gui/screen/VisualKeymapScreen.java`

- 分别捕获 `IOException` 与 `JsonParseException`，记录包含完整异常的开发者日志。
- 对两类失败显示不同的本地化正文，均使用 Minecraft 26.3 的 `AlertScreen`。
- 提示按钮和 Esc 均返回同一个 `VisualKeymapScreen`。
- 打开提示会使 Minecraft 调用父屏幕 `removed()`；新增一次性内部标志，只抑制这一次由错误提示切屏引发的 provider 保存。返回后正常关闭 Visual Keymap 仍按原逻辑保存。
- 提取 package-private `prepareImport(Path, List<VisualKeymapApi<?>>)` 供测试验证“解析成功后才枚举 provider”。没有新增或修改 public/protected API。

### 3.3 本地化资源

修改 `en_us.json` 与 `zh_cn.json`，各增加三项：导入失败标题、无效快照正文、I/O 失败正文。提示不包含快照内容、完整用户路径或 Java 堆栈。

未修改 `NativeFileDialog.poll()`、`VisualKeymap.importKeyBindings()` 公共签名、BUG-02 匹配/选择代码、键码兼容层、MaLiLib/CommandKeys adapter 或 Mod Menu 集成。

## 4. 测试优先证据

生产修复前运行新解析测试：10 个测试中 4 个失败，分别证明本地化键缺失，以及空/null/缺结构、null binding/null 键码未被安全拒绝。原始非法文本、非法语法和标量根节点当时已由 Gson 直接拒绝；这不削弱崩溃复现，因为 UI 边界仍未捕获该异常。

生产修复后，同一组测试全部通过。随后加入控制流和客户端冒烟测试并执行完整构建。

### 4.1 输入矩阵

| 输入 | 自动化结果 | 语义 |
| --- | --- | --- |
| 用户 `invalid.json` 的测试副本 | PASS | `JsonParseException` |
| 空文件、截断 JSON、JSON `null` | PASS | 安全拒绝 |
| 字符串、数字、数组根节点 | PASS | 安全拒绝 |
| 缺少 `bindings` | PASS | 安全拒绝 |
| `bindings`/`version` 类型错误 | PASS | 安全拒绝 |
| null binding、null keys/modifiers 元素 | PASS | 匹配前安全拒绝 |
| 有效 version 1 空/非空快照 | PASS | 正常读取 |
| 未知 version | PASS | 保留历史接受行为 |
| 缺失 version | PASS | 保留历史 version 0 行为 |
| 文件不存在 | PASS | 保持 `IOException` |

测试资源是从私人输入内容复制的独立测试夹具，SHA-256 为 `63BA68ADB542172BD08E750C8844ADB0A8D8B7750E8F13049846B8BF58D4A338`；换行形式使其哈希不同于 92 B 私人原件。私人原件哈希仍为 `824737B0E86E4428F8E8D98BCAD368BF86B77E13EC22F0FE58E1D89A6320E69B`，原件未修改且未提交。

### 4.2 无副作用与后续恢复

`ImportFailureFlowTest` 使用 spy provider/binding 证明：

- 无效输入时 provider 枚举 0 次、`setBoundKeys()` 0 次、`save()` 0 次，live keys 不变。
- 同一次测试中紧接着读取有效 version 1 快照会正常生成一个预览匹配。
- 仅准备预览不会提前写入或保存。

这些是程序化控制流证据，不代替真实客户端完全退出后的配置文件比较。

## 5. 完整构建

执行：

```text
.\gradlew.bat clean test build --no-daemon
```

结果：**BUILD SUCCESSFUL**。

- JUnit suites：7
- tests：39
- failures：0
- errors：0
- skipped：0
- 既有 BUG-02、规范键码、MaLiLib 转换与导入选择测试与新增测试共同通过。

## 6. 发布 JAR

| 项目 | 值 |
| --- | --- |
| 路径 | `build/libs/visual-keymap-26.3-1.4.0.jar` |
| 大小 | 107,838 B |
| SHA-256 | `87DCC986995EC9F9CFFC2623403200C7B4CDE515BEE07F1597848B2DD616703B` |
| Minecraft | 26.3 |
| Java | GraalVM JDK 25.0.4 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.165.0+26.3 |
| Loom | 1.19.3 |

JAR 共 68 个条目；扫描结果为：测试类/探针 0、嵌套 JAR 0、MaLiLib/CommandKeys/Mod Menu 类 0。第三方模组未被错误打包。

## 7. 生产 JAR 客户端组合验证

Phase 4 production-smoke 以发布 JAR 作为外部 Fabric mod，使用彼此隔离的 gameDir。更新后的探针会：用公共 `VisualKeymap.importKeyBindings()` 拒绝 `null` 快照；比较所有 live binding；动态打开并关闭错误 `AlertScreen`；在 P6 额外确认外部 provider 没有因提示切屏而保存。

| 组合 | 加载结果 | Provider / binding | BUG-01 探针 |
| --- | --- | --- | --- |
| P1 原生 | PASS | `MinecraftImpl` / 63 | rejected、bindings unchanged、alert lifecycle 全部 PASS |
| P3 MaLiLib/Tweakeroo | PASS | `MinecraftImpl, MaLiLibIntegration` / 280 | 同上 |
| P5 全部可选依赖 | PASS | `MinecraftImpl, MaLiLibIntegration, CommandKeysIntegration` / 282 | 同上 |
| P6 外部 Provider | PASS | `MinecraftImpl, Phase4Provider` / 64 | 同上，且外部 provider save 次数不变 |

P5 实际加载 CommandKeys 3.104.1+26.3、MaLiLib 0.30.2、Tweakeroo 0.30.1、Mod Menu 21.0.0。运行中出现的 Windows WMI/性能计数器警告及离线服务检查失败与本修复无关，未使探针失败。

该探针是实际 Minecraft 客户端中的程序化验证，但没有点击 SDL 文件对话框，也没有人工观察渲染文本，因此不能标记人工 GUI 项为 PASS。

## 8. 正常导入、BUG-02 与兼容性

- `KeymapSnapshot.matchBindings()`、`ImportSelection` 和 `ImportPreviewScreen` 的 BUG-02 算法未修改。
- 重复 ID 的分组匹配、歧义默认不应用和一对一约束继续由既有 JUnit 覆盖。
- version 1 schema、provider/id/keys/modifiers 字段未改变；modifiers 的历史导入语义未改变。
- F25 tombstone 与未知扩展键 `canApply()` 保护未修改，既有测试继续通过。
- 文件对话框取消仍由 `path == null` 在 `NativeFileDialog` 层处理，不进入 consumer；真实取消操作留待人工验收。
- 公开 `VisualKeymapApi` 和其他 public/protected 签名未改变。

## 9. 人工验收

维护者报告已在 Minecraft 26.3 实际客户端中完成 `docs/bugfixes/BUG-01-MANUAL-TEST.md` 的全部项目：

- M-01 至 M-10：**10/10 PASS**。
- 覆盖真实 SDL 文件选择、非法输入连续导入、取消、失败后有效导入、BUG-02 重复 ID 回归、完全退出/重启、英文/简体中文提示，以及 P1/P5 日志和 Provider 共存。
- 人工测试未发现问题。

证据边界：以上是维护者提供的真实客户端人工验收结论，不是 Agent 自动化结果；本次反馈未附带新的截图、录像、日志或哈希，本文不补造相关证据。可选的稳定“文件不可读”竞态不是 M-01 至 M-10 的强制项目，仍未单独取得证据，不影响已确认的非法 JSON 修复范围。

## 10. 剩余设计事项

未知 version 的既有接受行为按维护者决策保留，并已有回归测试锁定。未来是否明确拒绝未知 version 应作为独立格式设计议题处理，不属于 BUG-01，也未实现 version 2。

## 11. 建议

自动化、生产 JAR 启动矩阵和维护者报告的 10/10 人工验收均已通过。建议在完成合并前差异复核后，将 BUG-01 以非 fast-forward 方式合入 `port/mc-26.3`。
