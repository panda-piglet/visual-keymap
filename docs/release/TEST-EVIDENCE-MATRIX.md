# Visual Keymap 26.3 发布测试证据矩阵

## 1. 证据口径

生产代码基线：`port/mc-26.3@5e69d2383e91b6f2a4a08fe1d6cd065bda1fbad9`。Phase 5B 审计基于 Phase 5A 文档提交 `2b0e7ccbc5982744d1817f69f77915ceaaaede1a`，并纳入维护者本轮提供的七项真实客户端反馈。反馈没有附带截图、日志或配置副本，因此只按明确描述的范围计入人工证据。

状态定义：

- **PASS**：该编号定义的关键行为已有与其类型相符的证据。
- **PARTIAL**：算法/模型或代表性路径已通过，但仍缺真实 GUI、物理触发、完整输入矩阵或特定退出路径。
- **NOT TESTED**：没有足以证明该行为的运行证据。
- **BLOCKED**：受硬件或外部条件限制；不等于数据安全测试失败。
- **FAIL**：实际结果不符合预期。当前没有未解决的 FAIL。

证据类型严格区分：JUnit/探针是自动化证据；`runClient` 或 production-smoke 是真实客户端中的程序化证据；只有维护者明确反馈的实际操作才记作人工证据。相同证据只在最相关行计数，交叉引用不重复计算。

## 2. 构建、加载与入口

| 原始编号 | 功能 | 自动化/探针 | 真实客户端人工 | 状态 | 证据来源 | 尚缺验证 | 阻碍预发布 / 稳定版 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| B-01 | 干净构建 | `clean test build`，39/39 JUnit | 不适用 | PASS | BUG-01 final；Phase 4 | 无 | 否 / 否 |
| B-02 | P1、required Mixin、可选依赖缺席 | production JAR 探针 PASS | 原生入口曾实际打开 | PASS | Phase 4；B2.5 | 无 | 否 / 否 |
| B-03；P1–P6 | 可选依赖组合启动 | 六组合均到达 `CLIENT_STARTED`；BUG 后 P1/P3/P5/P6 复测 | P1/P5 用于 BUG 人工验收 | PASS | Phase 4；BUG-01/02 final | 无需重复启动穷举 | 否 / 否 |
| B-04 | JAR metadata | 展开值和内容审计 PASS | 不适用 | PASS | Phase 4；本次审计 | 无 | 否 / 否 |
| B-05 | Controls Mixin | 目标类转换与 accessor 使用通过 | N-01 实际入口通过 | PASS | Phase 4；B2.5 | 无 | 否 / 否 |
| N-01 | 原生控制菜单入口 | Mixin 探针 | 实际点击并打开主界面 | PASS | B2.5 | 无 | 否 / 否 |
| N-02 | 主屏 Done/Esc 返回 | `onClose()` 生命周期通过 | Done 已验证；主屏 Esc 未单独记录 | PARTIAL | B2.5 | 主屏物理 Esc 返回 | 否 / 是 |
| N-03；MM-01 | Mod Menu 配置按钮 | entrypoint/factory PASS | BUG-02 M-08 从 Mod Menu 打开并完成共存回归 | PASS | C3；BUG-02 manual | 无需重复“能打开” | 否 / 否 |
| MM-02 | Mod Menu Done/Esc 返回 | `onClose()` 返回父屏通过 | 未明确记录 Mod Menu 父屏上的物理 Done/Esc 两条路径 | PARTIAL | C3 | 一次 Done、一次 Esc、一次重复打开 | 否 / 是 |
| MM-03 | Mod Menu 缺席 | 缺席启动 PASS | 不需要人工 | PASS | C3；Phase 4 P1 | 无 | 否 / 否 |
| N-04 | 模组打开键默认未绑定 | 模型可见 | 实际显示 `Not Bound` | PASS | B2.5 | 无 | 否 / 否 |
| N-05 | 打开键在游戏/已有屏幕触发 | 无端到端物理触发 | 无 | NOT TESTED | TEST-PLAN | 两种上下文各触发一次 | 是 / 是 |

## 3. Minecraft 原生 GUI、配置与输入

| 原始编号 | 功能 | 自动化/探针 | 真实客户端人工 | 状态 | 证据来源 | 尚缺验证 | 阻碍预发布 / 稳定版 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| V-01；V-06 | 双语、GUI Scale、整体布局/翻译 | 语言资源可解析；BUG-01 两种提示存在 | BUG-01 英中提示通过；完整主屏多 Scale 未测 | PARTIAL | BUG-01 final；B2.5 | 英中主屏各一次、代表性 Scale | 否 / 是 |
| V-02 | 键盘/鼠标布局和点击区 | 固定布局源码与客户端初始化 | 代表键、数字区和三鼠标键可操作 | PARTIAL | BASELINE；B2.5 | 快速目视功能/导航/方向区；不逐键穷举 | 否 / 是 |
| V-03 | 数字小键盘开关 | 配置读写模型 | 实际开关、重启通过 | PASS | B2.5 | 无 | 否 / 否 |
| V-04 | 四种键帽状态与边框 | 冲突模型和搜索模型 | 代表性冲突/搜索已观察；Phase 5B 故意制造热键冲突未见异常，但未明确覆盖四态 | PARTIAL | B2.5；BUG-02；Phase 5B 反馈 2 | 一个屏幕构造四态 | 否 / 是 |
| V-05 | 键帽 tooltip 0/1/5/6+ | 无真实 GUI 证据 | 无 | NOT TESTED | TEST-PLAN | 只需验证 1 与 6+ 两个边界 | 否 / 否（可披露） |
| L-01；L-02 | 初始未绑定列表、键帽选择切换 | 业务逻辑静态明确 | 无独立记录 | NOT TESTED | BASELINE | 初始、同键取消、换键三步 | 否 / 是 |
| L-03 | 行名称、tooltip、按键文本、Reset 状态 | provider 模型通过 | BUG-02 歧义页显示类别/名称/键；普通列表完整矩阵未测 | PARTIAL | C1/C2/C3；BUG-02 | 三 Provider 代表行 | 否 / 是 |
| L-04 | 通用搜索 | 搜索算法覆盖 | `Visual Keymap` 代表查询通过 | PARTIAL | B2.5 | 多词、大小写、空格各一例 | 否 / 是 |
| L-05 | MaLiLib 内部名、CommandKeys 消息搜索 | 对象层通过 | GUI 输入/高亮未测 | PARTIAL | C1/C2 | P5 各一例 | 是 / 是 |
| L-06；L-07 | 三种排序、搜索/冲突优先级 | 比较器静态/模型证据 | 无完整 GUI 证据 | PARTIAL | BASELINE；Phase 4 | 三模式一次，冲突置顶开/关 | 否 / 是 |
| L-08 | 冲突算法与提示 | JUnit/外部 Provider 模型通过 | Phase 5B 已故意制造热键冲突且未见异常；反馈未说明是否为跨 Provider | PARTIAL | C3；BUG-02；Phase 5B 反馈 2 | 明确验证一次跨 Provider 冲突 | 否 / 是 |
| E-01 | 原生键编辑及实际动作 | 写入/保存模型和 F8 往返 | 编辑、保存通过；实际游戏动作未测 | PARTIAL | B2.5 | 本地世界触发一个无害动作 | 是 / 是 |
| E-02 | 编辑中 Esc 清空 | 源码明确 | 无真实操作记录 | NOT TESTED | BASELINE | 一个测试绑定操作一次 | 否 / 是 |
| E-03；S-03 | 鼠标键 | 0–7 编码全覆盖 | 左/中/右通过；M4–M8 无硬件证据 | PARTIAL / BLOCKED | B1/B2.5 | 有侧键则抽测一个；无设备记录 BLOCKED | 否 / 否 |
| E-06 | 清空与 Reset | 原生、MaLiLib、CommandKeys adapter 通过 | 原生 Reset 通过；第三方 GUI Reset 未明确记录 | PARTIAL | B2.5；C1/C2 | P5 各点一次 Reset | 否 / 是 |
| E-07 | 保存与重启 | 各 provider 探针跨进程保持 | F8、BUG-02 M-08 以及 Phase 5B 复杂键位配置多次完整重启均保持；不同离开路径未全测 | PASS（代表性持久化） | B2.5；BUG-02；Phase 5B 反馈 3 | Done/Esc 返回链归 R-01/R-14，不再质疑数据持久化主路径 | 否 / 否 |
| E-08 | 编辑后即时刷新 | 代表键、鼠标、未绑定通过 | 已实际观察 | PASS | B2.5 | 无 | 否 / 否 |
| C-01 | 首次配置创建与默认值 | 隔离 gameDir 文件证据 | 客户端实际生成 | PASS | B2.5 | 无 | 否 / 否 |
| C-02 | 三项配置保存/重启 | 配置模型 | `showNumpad` 通过；排序/冲突置顶未测 | PARTIAL | B2.5 | 与 L-06/L-07 合并测试 | 否 / 是 |
| C-03 | 配置页返回重建 | `showNumpad` 返回即时生效 | 已实际观察 | PASS | B2.5 | 无 | 否 / 否 |
| C-04；C-05 | 无效配置/只读 config | 源码只说明回默认和日志 | 无跨版本真实证据 | NOT TESTED | TEST-PLAN | 可作为已知历史限制；若测，必须用副本 | 否 / 否（披露） |

## 4. 快照与文件对话框

| 原始编号 | 功能 | 自动化/探针 | 真实客户端人工 | 状态 | 证据来源 | 尚缺验证 | 阻碍预发布 / 稳定版 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| X-01 | 打开/保存对话框、默认名、过滤、取消 | SDL 路径启动 | 默认目录/名/过滤/取消通过；BUG-01 再验证取消 | PASS | B2.5；BUG-01 M-04 | 无 | 否 / 否 |
| X-02；X-03 | version 1 导出内容与稳定结构 | JUnit/探针 | Minecraft、MaLiLib 样本和 BUG-02 往返通过 | PASS | B2.5；C1；BUG-02 | 无 | 否 / 否 |
| X-04 | 原样导入不变 | BUG-02 JUnit | BUG-02 M-01 PASS | PASS | BUG-02 final | 无 | 否 / 否 |
| X-05 | provider/ID 精确匹配 | BUG-02/JUnit、真实快照探针 | 唯一 ID 和歧义场景实际通过 | PASS | B2.5；BUG-02 | 无 | 否 / 否 |
| X-06；X-07 | 预览、选择、确认、取消、Esc | JUnit 一对一约束 | BUG-02 M-03～M-06 全部通过 | PASS | BUG-02 final | 无 | 否 / 否 |
| X-08 | modifiers 历史语义 | JUnit 锁定“不比较/不应用” | 无需重复人工 | PASS | BUG-02 tests | 无；作为已知限制披露 | 否 / 否 |
| X-09 | 键盘、鼠标、空绑定 | 编码与导入测试覆盖 | F8、中键、空绑定实际通过 | PASS（语义） | B2.5；BUG-02 | 侧键物理触发按硬件 BLOCKED | 否 / 否 |
| X-10 | 损坏结构、无效 JSON、未知 version、读取失败 | BUG-01 输入矩阵；未知 version 锁定 | BUG-01 M-01～M-05/M-10 PASS；稳定不可读竞态未单列 | PASS（BUG-01 范围）/ PARTIAL（不可读竞态） | BUG-01 final | 不可读文件可选补测 | 否 / 否 |
| X-11 | 不可写/覆盖导出 | 无动态证据 | Phase 5B 已用新导出覆盖原有无效文件，新文件可正常读取；不可写目标未测 | PARTIAL | Phase 4；Phase 5B 反馈 7 | 仅剩不可写目标失败路径 | 是 / 是 |
| BUG01-M-01～10 | 非法导入修复 | 39/39 JUnit 与 P1/P3/P5/P6 探针 | 维护者 10/10 PASS | PASS | BUG-01 final | 不重测 | 否 / 否 |
| BUG02-M-01～08 | 重复 ID 安全匹配 | JUnit 与真实 `export1.json` 探针 | 维护者 8/8 PASS | PASS | BUG-02 final | 不重测 | 否 / 否 |

## 5. MaLiLib、CommandKeys、Mod Menu 与外部 API

| 原始编号 | 功能 | 自动化/探针 | 真实客户端人工 | 状态 | 证据来源 | 尚缺验证 | 阻碍预发布 / 稳定版 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| M-01 | MaLiLib 分类/名称/comment | 217 条真实绑定枚举 | BUG-02 歧义页证明代表类别/名称可读；普通行 tooltip 未完整验证 | PARTIAL | C1；BUG-02 | P5 抽查 2 条与 tooltip | 是 / 是 |
| M-02 | MaLiLib 键码边界 | JUnit + 真实对象，`-99..-92`、其他负值通过 | 不要求 217 条逐项操作 | PASS（转换） | C1 | GUI 状态机归 E-04 | 否 / 否 |
| M-03 | MaLiLib 保存、`updateUsedKeys()`、触发 | 保存/重启探针通过 | BUG-02 M-08 联合保存通过；Phase 5B 四键组合可正常编辑和使用，复杂配置多次重启保持 | PASS（代表性四键链路） | C1；BUG-02；Phase 5B 反馈 1、3 | 不再要求重复四键触发；GUI 元数据/搜索/Reset 归 M-01/M-04 | 否 / 否 |
| M-04 | MaLiLib Reset/内部名搜索 | adapter reset 通过 | GUI Reset、内部名搜索未测 | PARTIAL | C1 | 与 M-01 一次完成 | 是 / 是 |
| E-04 | MaLiLib 1～4 键编辑状态机 | 四键数据保存/重启通过 | Phase 5B 四键组合编辑和实际使用通过；未明确覆盖 1～3 键及首键提前释放 | PARTIAL | C1；Phase 5B 反馈 1 | 只补代表性单键/双键和首键提前释放；不重测四键 | 否 / 是 |
| K-01 | CommandKeys 活动 profile | 真实对象切换只枚举当前 profile | GUI 重新打开未测 | PARTIAL | C2 | 两 profile 各打开一次 | 是 / 是 |
| K-02 | 宏名称/comment | 空名、非空名、首消息空和多消息模型通过 | GUI 行/tooltip 未测 | PARTIAL | C2 | 三个代表宏目视 | 是 / 是 |
| K-03 | 消息搜索 | `containsSearchText()` 动态通过 | GUI 输入/高亮未测 | PARTIAL | C2 | 第二条消息搜索一次 | 是 / 是 |
| K-04；E-05 | 单键、limit+主键、清空/保存/触发 | 真实对象写入、重启通过 | BUG-02 M-08 联合保存通过；GUI 状态机和宏触发未测 | PARTIAL | C2；BUG-02 | 单键、组合、清空、触发 | 是 / 是 |
| K-05 | CommandKeys 不参与快照 | 自动化多阶段为 0 条 | BUG-02 M-08 及 Phase 5B 再次确认 PASS | PASS | C2；BUG-02；Phase 5B 反馈 4 | 无 | 否 / 否 |
| C2 遗留：多个未绑定宏 | `Keybind` map 等价性风险仅静态识别 | Phase 5B 多个未绑定宏未发生丢失；未说明“只编辑一个”的隔离结果，也无 26.2 对照 | PARTIAL | C2 report；Phase 5B 反馈 5 | 单个宏编辑隔离；26.2 对照或维护者明确接受该兼容性缺口 | 是 / 是 |
| API-01 | 外部消费方编译/加载 | 同一消费方 26.2/26.3 PASS | 不需要人工 | PASS | C3 | 无 | 否 / 否 |
| API-02 | Provider 完整 GUI 契约 | 模型、回调、快照通过 | Phase 5B 确认外部 Provider 能检测并加载；GUI/tooltip/编辑/Reset/save/快照回调仍未人工验证 | PARTIAL | C3；P6；Phase 5B 反馈 6 | 一次完整 GUI 编辑/Reset/save/快照 | 否 / 是 |
| API-03 | 失败 entrypoint 隔离 | 26.2/26.3 动态 PASS | 不需要人工 | PASS | C3；P6 | 无 | 否 / 否 |
| API-04；API-05 | 顺序与签名 | 动态顺序、descriptor 比较 PASS | P5/P6 可见 provider 共存 | PASS | C3；Phase 4 | 无 | 否 / 否 |
| S-01；S-02；S-04 | 规范码、碰撞、集中边界 | 全量自动化/源码扫描 PASS | 不需要穷举 | PASS | B1/B2；39 JUnit | 无 | 否 / 否 |
| S-05 | 真实 26.2 快照 | Minecraft F8、MaLiLib J/F9 动态往返 | BUG-02/01 实际快照流程通过 | PASS（代表数据） | B2.5；C1；BUG reports | 旧 golden 未覆盖的侧键不要求阻断 | 否 / 否 |
| S-06 | F25 tombstone | 不可应用测试 | BUG-02 M-07 PASS；物理 F25 未测 | PASS（数据）/ BLOCKED（硬件） | BUG-02 final | 无设备则记录限制 | 否 / 否 |
| S-07 | 26.3-only 扩展键 | 127 表、碰撞、往返自动化 | 物理键未测 | PASS（编码）/ BLOCKED（硬件） | B1；BUG-02 | 有设备可抽测，否则限制 | 否 / 否 |
| S-08 | 多 adapter 同类键 | adapter 转换、P5 加载通过 | 基础共存/联合保存、多次重启和代表性冲突均无异常；未确认冲突是否跨 Provider，也未完成联合编辑矩阵 | PARTIAL | C1/C2；BUG-02 M-08；Phase 5B 反馈 2、3 | 明确的跨 Provider 冲突与三 Provider 联合编辑 | 是 / 是 |

## 6. 证据缺口结论

当前没有未解决 FAIL。自动化已经充分覆盖纯算法、编码、快照安全和 API descriptor，不应要求人工逐键或逐条绑定重测。剩余高价值缺口集中在：

1. 原生真实触发、搜索/排序和编辑边界。
2. MaLiLib 的 GUI 元数据、内部名搜索、Reset、单/双键及提前释放；四键编辑、使用和代表性重启已通过。
3. CommandKeys profile、显示、搜索、组合编辑及实际触发；多个未绑定宏“不丢失”已通过，但单对象编辑隔离和 26.2 对照仍缺。
4. 外部 Provider 的 GUI/callback/快照闭环；检测和加载已通过。
5. 明确的 P5 跨 Provider 冲突/排序与三 Provider 联合编辑。
6. 导出不可写目标的数据安全路径；覆盖无效旧文件后可读已通过。

BUG-01、BUG-02 已关闭，不列入剩余测试。

Phase 5C 维护者已接受上述未覆盖范围作为第一个社区 Pre-release 的披露风险。表内 PASS / PARTIAL / NOT TESTED / BLOCKED 状态保持不变；该风险接受只解除预发布准备流程门槛，不适用于 Stable 判定。
