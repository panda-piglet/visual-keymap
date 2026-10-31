# Visual Keymap Phase 4 完整集成验收报告

日期：2026-10-31  
分支：`port/mc-26.3`  
迁移基线：`053e736c77b15e0727c93abed7c94c3b0fb64618`  
Phase 4 测试设施 HEAD：`29ea281`（报告提交另计）

## 1. 结论

本阶段结论为 **CONDITIONAL GO**，可以进入 Phase 5，但 Phase 5 必须先执行 `MANUAL-REGRESSION-GUIDE.md` 中的高优先级人工回归；在这些项目通过前不得创建 tag 或发布 Release。

已取得的硬证据：

- GraalVM JDK 25.0.4 下执行 `clean test build` 成功，JUnit 7/7 实际执行并通过。
- Loom 1.19.3 生成的发布 JAR 元数据正确，测试探针、测试 Provider、MaLiLib、CommandKeys 和 Mod Menu 均未被打包。
- 独立的 Fabric 客户端夹具没有引用主项目 `sourceSets.main` 输出，Fabric Loader 的实际 mod origin 直接指向发布 JAR；P1–P6 六种组合全部到达 `CLIENT_STARTED` 并通过断言。
- 三个 required Mixin 目标在成品 JAR 客户端中被强制加载或实际使用，没有注入、缺类、链接错误。
- provider 顺序分别保持 Minecraft → 外部 entrypoint → MaLiLib → CommandKeys 的既有条件顺序。
- 相对 26.2 基线的生产差异均可归入必要 API、规范键码、第三方 adapter 或运行兼容性适配；没有发现迁移无关的生产功能改动。

尚未收口的是实际 GUI 点击、物理输入、游戏内热键/宏触发、语言与 GUI Scale、文件系统异常路径等人工项目。这些缺口不要求继续改生产架构，因此不阻碍进入“人工回归与发布准备”性质的 Phase 5；但它们阻止最终发布判定。

## 2. Git 与可追溯性

Phase 4 开始时：

- 分支为 `port/mc-26.3`；
- HEAD 为 `7a7e97e`（Phase 3 C3 报告）；
- 工作树干净；
- Phase 3 A/B1/B2/B2.5/C1/C2/C3 的生产、测试和文档提交均在当前分支历史中；
- `ARCHITECTURE-GUARDRAILS.md` 不存在，架构约束以 `ARCHITECTURE.md`、`COMPATIBILITY.md`、各阶段报告和用户授权为准。

Phase 4 新增提交：

| 提交 | 范围 |
| --- | --- |
| `bfbc14f` `test: validate production jar integration matrix` | 独立 Loom/Fabric 测试夹具，以成品 JAR 为外部 mod 执行 P1–P6。 |
| `29ea281` `test: support manual production matrix runs` | 增加 `-Pphase4Manual=true`，供人工 GUI/物理输入回归时保持客户端运行。 |
| 本报告提交 | 三份 Phase 4 文档；不修改生产源码。 |

两个测试提交仅位于 `test-fixtures/porting-26.3/production-smoke/`，没有修改 `src/main`、根构建的发布配置或版本号。

## 3. 干净构建与自动化测试

### 3.1 环境

| 项目 | 实际值 |
| --- | --- |
| Java | Oracle GraalVM `25.0.4+7-LTS-jvmci-b01` |
| Gradle Wrapper | 9.8.2 |
| Fabric Loom | 1.19.3 |
| Minecraft | 26.3 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.165.0+26.3 |

当前 Codex Windows 终端继续显式使用工作区长路径 `TEMP/TMP` 和 `GRADLE_USER_HOME`，以避开已记录的 JDK 25 loopback/8.3 临时路径问题。该设置不写入项目构建配置。

### 3.2 指定命令

```powershell
.\gradlew.bat clean test build --no-daemon
```

结果：**PASS**，`BUILD SUCCESSFUL in 36s`，8 个任务全部执行；`:test` 不是 `UP-TO-DATE` 或跳过状态。

JUnit XML：

| Suite | tests | failures | errors | skipped |
| --- | ---: | ---: | ---: | ---: |
| `CanonicalInputCodeTest` | 1 | 0 | 0 | 0 |
| `MaLiLibInputCodeTest` | 6 | 0 | 0 | 0 |
| 合计 | 7 | 0 | 0 | 0 |

覆盖包括 121 个旧规范键、127 个扩展键、247 个可用键盘编码、8 个鼠标编码、碰撞和双向往返，以及 MaLiLib `-99..-92`、其他负值保留和编辑替换。

Gradle 仍报告“deprecated features will be incompatible with Gradle 10”。当前 wrapper 是 9.8.2，构建成功；该警告是后续构建维护风险，不是 26.3 发布阻碍。本阶段未为消除警告做无关构建重构。

## 4. 发布 JAR 审计

### 4.1 产物

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| `build/libs/visual-keymap-26.3-1.4.0.jar` | 98,973 | `A7CC2D7A3708A6B0BEBAADC3D6E25B3049C52C1994EE4624011C68BE6B0EC0AD` |
| `build/libs/visual-keymap-26.3-1.4.0-sources.jar` | 38,220 | `F14C5C00DDB24FEC3C0D65435260B8FA5348BFBE2A361204A43D0DB68F980C52` |

发布 JAR manifest 记录 `Fabric-Mapping-Namespace: official`、Minecraft 26.3、Loader 0.20.1、Loom 1.19.3 和 Gradle 9.8.2。

### 4.2 `fabric.mod.json`

成品中的展开值：

- id：`visual-keymap`；
- version：`1.4.0`；
- environment：`client`；
- client entrypoint：`dev.xef2.visualkeymap.VisualKeymap`；
- Mod Menu entrypoint：`dev.xef2.visualkeymap.compact.modmenu.ModMenuImpl`；
- Mixin：`visual-keymap.mixins.json`；
- Loader：`>=0.20.1`；
- Fabric API：`*`；
- Minecraft：`~26.3`。

Mixin 配置保持 `required: true`、`JAVA_25`、`defaultRequire: 1`，client 列表仍为 `ControlsScreenMixin`、`HeaderAndFooterLayoutAccessor`、`KeyMappingAccessor`。

### 4.3 内容边界

成品 JAR 共 65 个 entry。扫描结果：

- C1/C2/C3/optional/Phase 4 测试类：0；
- `com/example/mymod` 外部测试 Provider：0；
- `fi/dy/masa`、`dev/terminalmc/commandkeys`、`com/terraformersmc/modmenu` 第三方类：0；
- nested JAR：0；
- sources JAR 同样不含测试 source set 或第三方源码。

因此 compileOnly/runtimeOnly 和测试 source set 没有污染发布物。Fabric API、MaLiLib、CommandKeys、Mod Menu 仍由客户端独立提供。

## 5. 成品 JAR 独立加载

测试夹具：`test-fixtures/porting-26.3/production-smoke/`。

该夹具有独立 `settings.gradle`、Loom 工程和 gameDir；它只把 `build/libs/visual-keymap-26.3-1.4.0.jar` 放入运行时，不引用根项目 `sourceSets.main`。每次运行均断言：

1. `visual-keymap` 实际 mod origin 是普通 JAR 文件；
2. Java classpath 不含根项目 `build/classes/java/main`；
3. 成品 JAR SHA-256 与上表一致；
4. 指定可选依赖存在，其余可选依赖确实缺席；
5. provider 列表和顺序正确；
6. `ControlsScreen` 目标类可转换、`KeyMappingAccessor` 实际被 provider 使用、`HeaderAndFooterLayoutAccessor` 在主屏初始化中被使用；
7. `VisualKeymapScreen` 可安装并返回父屏。

Fabric Loader 记录的 origin 在六组中均为：

```text
...\build\libs\visual-keymap-26.3-1.4.0.jar
```

这比根项目 `runClient` 更接近普通安装：被测 Visual Keymap 是发布 JAR，而不是开发 class directory。启动仍由独立 Loom 生成 Fabric 客户端命令，并非 Mojang Launcher UI；因此账号登录、启动器 profile 与下载器本身不在本报告证明范围内。

## 6. P1–P6 结果

| 组合 | 实际加载 | provider 顺序 | 结果 |
| --- | --- | --- | --- |
| P1 | Visual Keymap + Fabric API | `MinecraftImpl` | **PASS**；63 bindings；无可选依赖。 |
| P2 | P1 + Mod Menu 21.0.0 | `MinecraftImpl` | **PASS**；Loader 找到 `visual-keymap` 的 `modmenu` entrypoint，公开工厂创建 `VisualKeymapScreen`。 |
| P3 | P1 + MaLiLib 0.30.2 + Tweakeroo 0.30.1 | `MinecraftImpl, MaLiLibIntegration` | **PASS**；280 bindings；实际有 MaLiLib 绑定贡献。 |
| P4 | P1 + CommandKeys 3.104.1 Fabric | `MinecraftImpl, CommandKeysIntegration` | **PASS**；Loader 实际加载 `commandkeys`。 |
| P5 | P1 + 三项可选集成 | `MinecraftImpl, MaLiLibIntegration, CommandKeysIntegration` | **PASS**；282 bindings；Mod Menu entrypoint 同时验证。 |
| P6 | P1 + 外部 Provider/失败 Provider | `MinecraftImpl, Phase4Provider` | **PASS**；失败 entrypoint 被隔离；外部编辑、modifier、save 与 version 1 快照模型通过。 |

每组使用独立目录 `production-smoke/runs/p1` 至 `p6`。证据文件为各目录的 `phase4-production-smoke.txt`，运行日志为 `logs/latest.log`；这些是 ignored 本地运行证据，不提交用户配置。

日志扫描在 P1–P6 均未发现 `InjectionError`、`InvalidMixin`、`NoClassDefFoundError`、`ClassNotFoundException` 或 `LinkageError`。

P3/P5 首次空目录启动时 MaLiLib/Tweakeroo 以 ERROR 级别记录配置文件尚不存在，随后生成配置。P5 第二次完整启动不再出现这两项。持续存在的 ERROR/WARN 是宿主 WMI/perflib、离线 Mojang 服务和 Mod Menu 更新检查，均未阻止 `CLIENT_STARTED` 或探针 PASS，不是 Visual Keymap 集成故障。

## 7. 依赖解析

`runtimeClasspath` 实际包含：

- `net.fabricmc:fabric-loader:0.20.1`；
- `net.fabricmc.fabric-api:fabric-api:0.165.0+26.3`；
- `maven.modrinth:malilib:0.30.2`；
- `maven.modrinth:tweakeroo:fWxKATn6`（仅开发期真实 MaLiLib provider）；
- `curse.maven:commandkeys-873388:9106946`；
- `com.terraformersmc:modmenu:21.0.0`。

P4/P5 的 Loader 列表确认 Curse file 9106946 是 `commandkeys 3.104.1+26.3` Fabric，而不是此前误取的 NeoForge JAR。

## 8. 架构与回归审查摘要

- GUI、排序、冲突和快照仍只依赖 `KeyBinding`/`VisualKeymapApi`；没有 MaLiLib/CommandKeys 专属判断。
- MaLiLib 特殊负数保留在 `MaLiLibInputCode`；CommandKeys 复用 `MinecraftInputCode`。
- SDL/规范整数转换集中在 `CanonicalInputCode` 和 `MinecraftInputCode`。业务层没有直接把 SDL `Key#getValue()` 当作公共整数。
- Mod Menu 类引用仍隔离在 metadata entrypoint 与 `compact/modmenu/ModMenuImpl`；P1 缺席启动成功。
- 既有 public/protected descriptor、`visualkeymap` entrypoint、provider ID/顺序和 version 1 字段未改变。
- Mixin 源码和配置相对 26.2 没有差异；目标端类加载和访问器使用通过。
- 根 `build.gradle` 的五个测试 source set、运行时排除开关和 Tweakeroo 测试依赖不进入发布 JAR，也不修改发布 metadata。

完整分类见 `MIGRATION-DIFF-REVIEW.md`。

## 9. 未完成测试与发布阻碍

仍需人工执行：

- MaLiLib GUI 显示、内部名搜索、冲突/排序、多键编辑状态机和世界内实际触发；
- CommandKeys profile 切换、名称/comment、消息搜索、编辑/清空/重置、重启和本地世界实际宏触发；
- Mod Menu 列表中的真实配置按钮、Done/Esc 和反复打开关闭；
- 外部 Provider 的 GUI、tooltip、编辑、重置和文件对话框快照流程；
- `en_us`/`zh_cn`、三种 GUI Scale、窗口最小尺寸/全屏；
- SDL 文件对话框的部分选择、Esc、不可读/不可写/覆盖等异常路径；
- M4–M8、F25 物理事件和至少一个 26.3-only 物理键；
- 真实 26.2 mouse 0–7、空 keys、非空 modifiers golden 数据；
- 配置无效 JSON、只读目录等 26.2→26.3 精确对照。

已知的无效快照 JSON 在 26.3 会因 `JsonSyntaxException` 崩溃；26.2 源码具有相同未捕获路径，但没有 26.2 动态证据。它目前作为可能的历史行为记录，不在迁移中修复。精确判断必须按人工指南补做 26.2 对照。

## 10. Phase 5 门槛

建议 **CONDITIONAL GO**，条件如下：

1. Phase 5 首先执行 `MANUAL-REGRESSION-GUIDE.md` 的 MR-01～MR-07、MR-10～MR-16 与 MR-20；设备受限的 MR-17 可明确记录为 BLOCKED，但数据不可丢失策略必须保持 PASS。
2. 所有新发现的差异先判定是否为 26.2 既有行为；需要修改生产逻辑时必须单独批准。
3. 至少完成 P1、P5、P6 三个成品 JAR 人工会话，保存截图、日志、导入/导出文件和重启结果。
4. 在上述人工项目通过前，不得 tag、发布或合并主分支。

本阶段没有发现必须立即修改生产源码的 BLOCKER，也没有进入 Phase 5、创建 tag、推送或发布。
