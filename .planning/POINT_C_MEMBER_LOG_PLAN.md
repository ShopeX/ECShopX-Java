# C 端积分流水 / 转赠 / 溯源 — 实现计划

## Context（背景）

C 端会员中心有三块相关页面：

1. **积分流水列表**（含顶部积分总数；总数已有接口，本计划不管）
2. **积分转赠页**（从流水列表点「转赠」进入，展示当前这笔记录并提交转赠）
3. **积分溯源页**（流水若来自他人转赠，可点「溯源」查看转出方信息）

现有接口：

| 接口 | 路径 | 现状 |
|------|------|------|
| 积分总数 | `GET /wxapp/point/member/info` | ✅ 已实现，本计划不改 |
| 积分流水列表 | `GET /wxapp/point/member` | ⚠️ 筛选项部分可用；列表字段远不够撑 UI |
| 转赠 | — | ❌ 未实现（仅活动上有 `transferable` 字段） |
| 溯源 | — | ❌ 未实现 |

> 后管流水字段丰富化见 `POINT_LOG_FIELDS_PLAN.md`（`PointMemberLogEnrichService` 已存在，但 **仅后管列表调用**，C 端未复用）。
> 转赠核心业务规则在 `POINT_ACTIVITY_PLAN.md` Phase 8 有草案，本计划按 **C 端真实 UI** 校正为「指定批次转赠」，并补齐列表展示与溯源。

---

## 与其他计划的关系

| 项 | 归属 |
|----|------|
| 后管流水字段 enrich / 导出 | `POINT_LOG_FIELDS_PLAN`（已落地大部分） |
| 转赠批次扣减 / 继承规则（草案） | `POINT_ACTIVITY_PLAN` Phase 8 |
| 退款过期 batch 仍恢复余额 | `POINT_REFUND_EXPIRED_RESTORE_PLAN`（本计划 can_transfer 不特殊排除退款流水） |
| C 端流水筛选+列表字段、转赠 API、溯源 | **本计划** |

---

## Gap 对照（UI vs 现状）

### 一、流水列表 — 筛选项

| UI | 期望参数 | 现状 | 结论 |
|----|---------|------|------|
| 一级：线上 / 线下 / 通用 | `scope_type=1/2/3` | ✅ 已支持（按 `point_activity.scope_type` 子查询过滤） | 可用 |
| 二级：全部 / 获取 / 支出 | `outin_type` | ⚠️ `outcome`=支出；**空=全部**；`income`=仅收入 | **需补** `all`/`income` 分支（见 D1） |

> 说明：无 `activity_id` 的老流水不会落入任一 `scope_type` Tab（子查询过滤），可接受；实现时在文档注明即可。

### 二、流水列表 — 卡片字段

| UI 展示 | 期望字段 | 现状（C 端返回） | 结论 |
|---------|---------|-----------------|------|
| 活动标题 | `activity_name` | ❌ 无（后管 enrich 有，C 端未调） | 需补 |
| 变动类型副标题 | `journal_type_desc` | ⚠️ 有，但缺 19/20/21 等文案 | 需补映射 |
| +/- 积分 | `income` / `outcome` / `point` + `outin_type` | ✅ 有 | 可用 |
| 使用范围 | `scope_type`（前端映射文案） | ❌ 无 | 需补（见 D6） |
| 范围 | `scope_distributor_type` / `scope_distributor_count` / `scope_desc` | ❌ 无 | 需补（见 D6，不拼 `scope_range_desc`） |
| 创建时间 | `created` | ✅ Unix 秒 | 可用（前端格式化） |
| 订单编号 | `order_id` | ✅ 有（可空） | 可用 |
| 有效时间 | `expire_desc`（活动规则文案） | ❌ 无 | 需补（见 D7） |
| 「转赠」按钮 | `can_transfer` | ❌ 无 | 需补（见 D4，退款流水不特殊排除） |
| 「溯源」按钮 | `has_trace` | ❌ 无 | 需补 |
| 转赠页用的可转余额 | `transferable_point`（≈ batch 剩余） | ❌ 无 | 需补 |
| 转赠/溯源跳转主键 | `id` + `batch_id` | ✅ 有 id/batch_id | 可用 |

**根因：** `PointMemberListService.listsBranchLocalFront` 只 `mapLocalLogRow`，**未调用** `PointMemberLogEnrichService.enrich()`。

### 三、转赠页（依赖列表数据 + 提交接口）

转赠页「当前积分记录」卡片字段均可从列表行带入（或再调列表同源字段），无需单独详情接口。但提交转赠 **尚无 API**。

| 页内字段 | 列表字段来源 |
|---------|-------------|
| 来源类型（如「积分活动导入」） | `journal_type_desc` |
| 活动名称 | `activity_name` |
| 当前可转赠积分 | `transferable_point` |
| 订单编号 | `order_id` |
| 使用范围 | `scope_type`（前端映射） |
| 范围 | `scope_distributor_*` / `scope_desc`（前端拼） |
| 时间 | `created` |
| 有效时间 | `expire_desc` |
| 确认转赠入参 | `batch_id` + 手机号 + 积分数 |

### 四、溯源页

| 页内字段 | 需要 |
|---------|------|
| 积分来源 | 固定/映射：「转赠获取积分」 |
| 活动名称 / 使用范围 / 范围 / 有效时间 | 同列表 enrich |
| 转赠手机号 | **需落库关联转出方**（现状无） |
| 转赠积分 / 转赠时间 | 本条 log 的 `income` / `created` |

列表需 `has_trace=true` 标识；`trace_info` 内嵌见 Phase B+C（**不在 Phase A**）。

---

## 设计决策

### D1：`outin_type` — 保持现状语义

**决定（写死）：保持代码现状，前端无需改动。**

| 传参 | 行为 |
|------|------|
| 空 / 不传 | **全部**（保持现状，兼容旧客户端） |
| `all` | 全部（与空等价，显式语义） |
| `income` | 仅 `income > 0` |
| `outcome` | 仅 `outcome > 0` |

Hope/数云分支：本参数语义 **不动**（D8）。

### D2：C 端列表复用 EnrichService，扩展展示字段

**决定：** `listsBranchLocalFront` 在 `mapLocalLogRow` 后调用 enrich，补 `can_transfer` / `has_trace` / `expire_desc` / 范围原始字段 / `transferable_point` 等。

- 后管已有字段继续兼容；C 端多出的字段不影响后管。
- Enrich 只做组装调度；手机号、商户数等查询委托现有 Service/Port（避免 god-service）。
- **`trace_info` 不进 Phase A**（依赖 B 的 `related_*` 字段），见 D5 / 实施顺序。

### D3：转赠单位 = 指定批次（校正 Phase 8 草案）

**决定：** 与 UI 一致——用户从流水选中一笔「获取」记录，按该记录关联的 `batch_id` 转出，**不是**跨所有可转赠批次 FIFO。

| 规则 | 说明 |
|------|------|
| 入参 | `batch_id`（必填）+ `mobile` + `point_amount`（用户输入，上限为 batch.remain_point）；校验 batch 属于当前登录用户即可 |
| 可转条件 | 见 D4；服务端再验：活动 `transferable=1`、归属、余额、未过期 |
| 扣减 | **仅扣该 batch**；必须用 CAS（见 B.2），禁止裸 `updateById` |
| 转入 | **禁止直接调 `grantBatch`**（见 B.2）：直接 insert 新 batch，**拷贝**源 batch 的 `activity_id` / `expire_time` / scope 三件套；**不要求**活动仍在进行中 |
| 流水 | 转出 `journal_type=19`；转入 `journal_type=20`；双方写 `activity_id`+各自 `batch_id` |
| 关联 | 双方 log 互填 `related_*`（见 D4、B.2 写序） |

> 与 `POINT_ACTIVITY_PLAN` Phase 8「FIFO 多 batch」不一致处：**以本计划为准**（C 端已定稿交互）。
> `grantBatch` 会校验活动进行中，并用「当前时间」重算 `expire_time`（`expire_type=2` 会再延一期），与「继承源批次」冲突，故转入不得复用。

### D4：列表标识 + 可转条件

**落库字段（Phase B）：**

| 字段 | 说明 |
|------|------|
| `related_user_id` | 对方用户 ID（转出记录=接收方；转入记录=转出方） |
| `related_log_id` | 对方流水 ID（成对关联） |

**列表标识：**

```
has_trace = (journal_type == 20)

can_transfer = (income > 0
  && activity_id != null && activity_id > 0
  && batch_id != null && batch_id > 0
  && activity.transferable == 1
  && batch.remain_point > 0
  && batch 未过期)

transferable_point = can_transfer ? batch.remain_point : 0
```

**不按 journal_type 白名单排除。** 退款返还（9/10/22）等也不做特殊处理：它们挂的是**原 batch**，只要该收入流水关联的批次仍有剩余且未过期、活动可转，即可转赠（转的是同一 batch 余额）。无 `activity_id` / `batch_id` 的老流水不可转。

> 过期 batch 即便因退款恢复了 `remain_point`，因「未过期」条件不满足，也不会亮转赠。退款过期恢复规则见 `POINT_REFUND_EXPIRED_RESTORE_PLAN.md`。

**转入后再转赠（写死）：允许。**  
`journal_type=20` 可同时 `has_trace=true` 且 `can_transfer=true`。溯源只展示**直接转出方**（不做多级链）。

### D5：溯源接口形态

**决定：MVP 列表内嵌；详情可选延后。**

1. **列表行内嵌 `trace_info`**（Phase B+C）：转入流水组装对象，前端点「溯源」直接展示。
2. **可选详情** `GET /wxapp/point/member/log/{id}/trace`：Phase 2，不进本计划 MVP。

### D6：「范围」字段返回（`scope_range_desc` → 改为返回原始字段）

**决定：后端不拼文案，返回活动原始字段，前端自行展示。**

返回字段：

| 字段 | 来源 | 说明 |
|------|------|------|
| `scope_type` | activity.scopeType | 1=线上, 2=线下, 3=通用 |
| `scope_distributor_type` | activity.scopeDistributorType | 0=全部, 1=圈选可用, 2=圈选不可用 |
| `scope_distributor_count` | 仅线下(scope_type=2)时计算 | 可用商户数（per-request 按 activity 缓存） |
| `scope_desc` | activity.scopeDesc | 活动上配置的范围描述文案（可空） |

**规则：**
- `scope_type=2`（线下）且 `scope_distributor_type != 0` 时：计算并返回 `scope_distributor_count`
- `scope_type=1`（线上）或 `scope_type=3`（通用）：`scope_distributor_count` 不返回（null），前端根据 `scope_type` 自行展示
- 支出类流水且有 `distributor_id`：额外返回 `distributor_name`（已有 enrich 逻辑）

### D7：有效时间文案（`expire_desc`）

**决定（写死）：列表/溯源展示用活动配置规则文案，不用 batch 绝对日期拼 `expire_desc`。**

| `expire_type` | 规则 | 示例 |
|--------|------|------|
| 0 | 永不过期 | `"永不过期"` |
| 1 | 固定时间段 | `"2026-07-14至2026-08-14"`（活动 start_time ~ end_time） |
| 2 | 发放后相对时间 | `"发放后 30 天到期"`（`expire_days` + `expire_days_unit`） |

**数据来源：** Enrich 已加载的 `activityMap`；无 `activity_id` 时返回空串。建议抽 `PointExpireDescUtil` 与扫码页共用，避免文案漂移。

同时保留 `batch_expire_time` 原始时间戳（前端若要倒计时/精确截止日自行用，**不**作为 `expire_desc` 主文案）。

### D8：Hope/数云对接分支

**决定：** 本计划只管本地 `point_member_log` 分支逻辑。Hope 公司走数云 API 透传，业务层面已控制不进入本地分支，无需在代码中写额外兼容。

---

## Phase A：C 端流水列表对齐 UI（不含溯源内嵌）

### A.1 修正 `outin_type`（按 D1）

**文件：** `PointMemberListService.listsBranchLocalFront`、Controller 注释、`docs/prd/C端积分接口文档.md`

```
空/不传     → 全部（保持现状不动）
all         → 全部（与空等价）
income      → income > 0
outcome     → outcome > 0
```

实现：新增对 `"all"` 和 `"income"` 的判断分支，空值逻辑保持不变。

Hope 分支：不改。

### A.2 C 端调用 enrich + 扩展字段（无 `trace_info`）

| 文件 | 变更 |
|------|------|
| `PointMemberListService` | front 分支调用 `enrich`；按 D1 改筛选 |
| `PointMemberLogEnrichService` | 扩展：`expire_desc`（从 activity 组装）、`scope_distributor_type`/`scope_distributor_count`/`scope_desc`、`can_transfer`、`has_trace`、`transferable_point`；线下商户数 per-request 缓存 |
| `PointMemberJournalTypeDescriptions` | 补 19/20/21/22 文案（即使暂无数据也可先加） |

**本阶段不做：** `trace_info` 组装、`related_*` 字段、转赠 API。

**C 端列表项目标 JSON（Phase A）：**

```json
{
  "id": 1001,
  "journal_type": 18,
  "journal_type_desc": "活动导入积分",
  "activity_id": 101,
  "activity_name": "物业积分福利活动",
  "income": 2000,
  "outcome": 0,
  "outin_type": "in",
  "point": 2000,
  "order_id": "",
  "created": 1750089603,
  "scope_type": 2,
  "scope_distributor_type": 1,
  "scope_distributor_count": 100,
  "scope_desc": "",
  "expire_desc": "发放后 30 天到期",
  "batch_id": 5001,
  "batch_remain_point": 2000,
  "batch_expire_time": 1752681600,
  "can_transfer": true,
  "transferable_point": 2000,
  "has_trace": false
}
```

无 journal_type=20 数据时，`has_trace` 恒为 `false`（可先联调列表 UI）。

### A.3 文档

更新 `docs/prd/C端积分接口文档.md` §2.2：

- **纠正错误表述**：原文「空/其他=仅收入」与代码不符；改为 D1（空/`all`=全部，`income`=收入，`outcome`=支出）
- 完整列表字段表、`can_transfer`/`has_trace`、范围原始字段、`expire_desc` 说明

---

## Phase B：转赠写入与 API

### B.1 模型

```sql
ALTER TABLE point_member_log
  ADD COLUMN related_user_id BIGINT NULL COMMENT '转赠对方用户ID',
  ADD COLUMN related_log_id BIGINT NULL COMMENT '转赠成对流水ID';
```

实体 `PointMemberLog` 同步字段。索引：暂不加。

### B.2 Service（写序 + CAS + 手机号）

**新建** `PointTransferService`：

**手机号 → 用户：** 复用 `MemberAccountService.findMemberByCompanyAndMobile(companyId, mobilePlain)`；未找到 → 业务错误；`userId` 等于当前用户 → 不能转给自己。

**事务内步骤：**

1. 加载并校验源 `batch` + `activity`（batch 归属当前用户；活动存在且 `transferable=1`；batch 未过期；`point_amount > 0` 且 ≤ remain）。**不要求**活动仍在进行中。
2. **CAS 扣减源 batch**（必须；注意 MySQL SET 从左到右，先算 status 再扣减）：
   ```sql
   UPDATE point_batch
   SET status = IF(remain_point <= #{amt}, 2, status),
       remain_point = remain_point - #{amt},
       updated = #{now}
   WHERE id = #{batchId}
     AND user_id = #{userId}
     AND company_id = #{companyId}
     AND remain_point >= #{amt}
     AND (expire_time = 0 OR expire_time > #{now})
   ```
   影响行数 = 0 → 抛「可用积分不足或已过期」（防并发超扣）
3. **为接收方直接 insert 新 batch**（**禁止**调用 `PointActivityDistributeService.grantBatch`）：
   - 拷贝源 batch：`activity_id`、`expire_time`、`scope_type`、`scope_distributor_type`、`scope_distributor_ids`
   - `user_id`=接收方；`total_point`=`remain_point`=`point_amount`；`status=1`
4. **成对写 log（互指）：**
   1. insert 转出 log（19）：`related_user_id=接收方`，`related_log_id` 暂空
   2. insert 转入 log（20）：`related_user_id=转出方`，`related_log_id=转出log.id`
   3. update 转出 log：`related_log_id=转入log.id`
5. 返回摘要

**不写** `point_batch_consume`（转赠非消费核销）。

**防重复：** MVP 不做 Redis 幂等；依赖 CAS + 前端防抖。若联调出现双提交问题，再加短 TTL key（可选增强，非本阶段必做）。

### B.3 API

路径：`/api/v1/h5app` + `@FrontAuth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/wxapp/point/transfer` | 确认转赠 |

**Request：**

```json
{
  "batch_id": 5001,
  "mobile": "13800138000",
  "point_amount": 500
}
```

**Response：**

```json
{
  "data": {
    "from_log_id": 2001,
    "to_log_id": 2002,
    "point_amount": 500,
    "to_user_id": 4002
  }
}
```

**错误：** 活动不可转赠 / 余额不足或已过期 / 手机号无效 / 不能转给自己 / 积分数非法 等。

### B.4 journal_type 文案

| 编码 | 描述（与 UI 对齐） |
|------|------|
| 19 | 转赠支出 |
| 20 | 转赠获取积分 |

写入 `PointMemberJournalTypeDescriptions`（若 A 已预加则核对文案一致）。

### B.5 文档同步

- `docs/prd/C端积分接口文档.md`：新增转赠接口节
- `docs/prd/积分数据流说明.md`：§七改为「已实现」摘要；journal 对照表补 19/20

---

## Phase C：溯源内嵌（与 B 同批交付）

> **必须与 Phase B 同 PR / 同发布**，避免列表有 `has_trace` 却无 `trace_info`。

### C.1 列表内嵌 `trace_info`

在 Enrich（或 front 专用后置步骤）中：当 `journal_type==20` 且存在 `related_user_id` 时组装：

```json
{
  "journal_type": 20,
  "journal_type_desc": "转赠获取积分",
  "can_transfer": true,
  "has_trace": true,
  "transferable_point": 800,
  "trace_info": {
    "source_desc": "转赠获取积分",
    "activity_name": "物业积分福利活动",
    "scope_type": 2,
    "scope_distributor_type": 1,
    "scope_distributor_count": 100,
    "scope_desc": "",
    "from_mobile": "167****7654",
    "transfer_point": 1000,
    "transfer_time": 1750089603,
    "expire_desc": "2026-07-14至2026-08-14"
  }
}
```

- `from_mobile`：`MemberAccountService.resolvePlainMobileForMember` + 脱敏（中间四位 `*`）
- 手机号查询按 `related_user_id` **批量去重**，禁止 N+1
- Enrich 委托 Member Port/Service，自身只组装

### C.2 详情接口

**本计划不做**（见 D5 Phase 2）。

---

## 实施顺序

```
Phase A（列表筛选+展示字段，无 trace_info）
    ↓ 可独立联调列表 UI
Phase B + C（同批：模型 + 转赠 API + 溯源内嵌 + 数据流文档）
```

- A 完成后：`can_transfer`/`has_trace` 在无 19/20 数据时多为 false，但不阻塞列表字段联调
- B+C 同批：保证转入流水立刻可溯源

---

## 改造文件清单（预估）

| 阶段 | 模块 | 文件 | 变更 |
|------|------|------|------|
| A | point/service | `PointMemberListService.java` | D1 outin_type；front enrich |
| A | point/service | `PointMemberLogEnrichService.java` | 展示字段 + can_transfer/has_trace（无 trace_info）；expire_desc 从 activity 组装；线下返回 scope_distributor_count |
| A | point/service | `PointMemberJournalTypeDescriptions.java` | 19/20/21/22 |
| A | point/service/activity | `PointExpireDescUtil.java` | **新建**（可选，与扫码页共用活动文案） |
| A | docs | `C端积分接口文档.md` | §2.2（含纠正 outin_type 错误说明） |
| B | point/domain | `PointMemberLog.java` | related_user_id / related_log_id |
| B | migration | `V...__add_point_member_log_transfer_related.sql` | 新字段 |
| B | point/mapper | `PointBatchMapper.java` | CAS 扣减 SQL |
| B | point/service/activity | `PointTransferService.java` | **新建**（转入拷贝源 batch，不调 grantBatch） |
| B | point/api/front/v1 | `PointTransferController.java` | **新建** |
| B | docs | `C端积分接口文档.md`、`积分数据流说明.md` | 转赠 + journal |
| C | point/service | `PointMemberLogEnrichService.java` | `trace_info` + 批量手机号 |
| A/B/C | test | 对应单测 | 筛选、can_transfer、CAS、成对 log、溯源 |

---

## 验证方式

1. **筛选：** `scope_type` 三档；`outin_type` 空=全部、`all`=全部、`income`=仅收入、`outcome`=仅支出
2. **列表字段：** 导入收入流水核对活动名、范围字段、`expire_desc`（活动规则文案，如「发放后 30 天到期」）、订单号
3. **转赠按钮：** 活动 transferable=1 + 收入流水 + 关联 batch 有余额且未过期 → `can_transfer=true`；退款返还流水挂同一未过期有余额 batch 时也可 true；过期 batch → false
4. **转入继承：** 接收方新 batch 的 `expire_time`/scope 与源 batch 一致；活动已结束仍可转（不调 grantBatch）
5. **CAS：** 并发两笔转同一 batch，一笔成功一笔失败，余额不出现负数
6. **成对 log：** 19/20 的 `related_log_id` / `related_user_id` 互指正确
7. **溯源：** 转入流水 `has_trace=true`，`trace_info.from_mobile` 为转出方脱敏手机号
8. **编译：** `mvn test -pl ecshopx-point -am -Dtest=...`

---

## 明确不做（本计划外）

- 积分总数字段/接口改造（已有）
- 后管流水页再改一轮（已在 `POINT_LOG_FIELDS_PLAN`）
- Hope/数云模式下的转赠与溯源
- 转赠撤销 / 多级溯源链路
- 全局 FIFO 跨批次转赠
- 转赠详情 `GET .../trace`（Phase 2）
- 转赠 Redis 幂等（可选增强，非 MVP）
- 退款过期是否恢复余额（见 `POINT_REFUND_EXPIRED_RESTORE_PLAN.md`）

---

## Review 修订记录（2026-07-18）

| 项 | 处理 |
|----|------|
| A/B/C 边界：`trace_info` 误放进 A | ✅ 改为仅 B+C；A 无内嵌 |
| `related_log_id` 成对写入 | ✅ B.2 明确三步写序 |
| 转赠并发超扣 | ✅ 强制 CAS UPDATE（先 status 后 remain） |
| D1 / 接口文档错误 | ✅ 空=全部（与代码一致）；A.3 纠正文档「空=仅收入」 |
| `can_transfer` / 退款返还 | ✅ 不按 journal 特殊排除；看收入 + 活动可转 + batch 有余额未过期 |
| 转入后再转 | ✅ 允许；可与 `has_trace` 同真 |
| `expire_desc` 口径 | ✅ **活动规则文案**（非 batch 绝对日期）；`batch_expire_time` 另返 |
| 禁止 `grantBatch` 转入 | ✅ 直接 insert 拷贝源 batch；不校验活动进行中 |
| 商户数 N 性能 | ✅ per-request 按 activity 缓存 |
| 手机号查找入口 | ✅ `MemberAccountService.findMemberByCompanyAndMobile` / `resolvePlainMobileForMember` |
| 数据流文档 | ✅ B.5 同步 §七与 journal 表 |
| Enrich god-service | ✅ 只组装，查询委托 Port/Service |
