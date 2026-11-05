# 线上积分抵扣 — 实现计划

## Context（背景）

线下核销与积分订单计划（`POINT_ORDER_PLAN.md`）已在 **D20** 约定：线下/线上共用同一套批次排序与「一批次一条流水」规则，并通过 `PointBatchDeductService.deductByRule(...)` 抽取公共扣减入口。

本计划只覆盖**线上订单积分抵扣**调用方改造：

- **不传** `activity_id`
- **不生成** `point_order`
- 扣减排序 / 流水粒度以 D20 为准，本文件不另起分叉规则

覆盖两条现网扣减路径：

1. **下单抵扣**：`OrderCreatePostCommitPortImpl`（`point_use > 0` 且非 `pay_type=point`）
2. **纯积分支付**：`OrdersPaymentDoPaymentService`（`pay_type=point`，支付时扣 `point_amount`）

---

## 与 POINT_ORDER_PLAN 的关系

| 项 | 归属 |
|----|------|
| `deductByRule` 实现、排序、分层、一批次一条 consume/log | `POINT_ORDER_PLAN` Phase 9A（D20） |
| 线下核销 + `point_order` | `POINT_ORDER_PLAN` |
| 线上订单抵扣 / 纯积分支付改挂 `deductByRule`、联调、验证 | **本计划** |

**前置依赖（硬条件）：** Phase 9A 中 `deductByRule` 已落地，且满足：

- 无 `activity_id` 时按「线上 → 全场景」分层
- **每扣减一个批次 → 写一条 `point_batch_consume` + 一条 `point_member_log`**（一批次一条，禁止按活动汇总）
- `deductByRule` 事务传播为 `REQUIRED`（可并入调用方事务），不得 `REQUIRES_NEW` 提前提交

可先实现公共方法，再并行本计划。

---

## 设计决策

### OD1：线上抵扣不带活动、不建积分订单

**决定：** 线上抵扣（含纯积分支付）调用：

```java
pointBatchDeductService.deductByRule(
    companyId, userId, pointToDeduct,
    distributorId, orderIdStr,
    null,   // activityId：线上不传
    null);  // allowedScopeTypes：由 D20 内部做「线上 → 全场景」分层
```

- 不创建 `point_order`
- `point_batch_consume.order_id` = 商城订单号（字符串）
- `point_member_log.order_id` = 同一商城订单号
- 跨 N 个批次 → N 条 consume + N 条 log（一批次一条）

### OD2：替换现状「手工调整式扣减」；覆盖两条入口

**现状问题：**

1. 下单后抵扣：`OrderCreatePostCommitPortImpl` → `addPointForManualAdjustment(..., plus=false, "订单抵扣积分", distributorId)`
2. 纯积分支付：`OrdersPaymentDoPaymentService` → `addPointForManualAdjustment(..., plus=false, "订单{orderId}支付扣减积分", distributorId)`

共同问题：

- 未走 `PointBatchDeductService`，与批次余额模型不一致
- 下单抵扣未传入订单号，无法与 `point_batch_consume` / 退款原路返还对齐
- 流水不是按批次拆分；且走手工调整路径时 `journal_type=12`（商家手动修改），语义不符

**决定：**

- 上述两条路径均改为显式调用批次扣减公共方法（或经 point 模块薄封装）
- 不再用 `addPointForManualAdjustment(false)` 承担「订单抵扣 / 积分支付扣减」职责
- `addPointForManualAdjustment` 仍保留给真正的后台手工加减等场景

### OD3：范围与排序（复述 D20，线上专用视角）

> ⚠️ 下方第 2-4 条已被 **Phase OD-D** 替代。新规则为统一排序：expire_time → 范围窄优先 → remain_point → id。

无 `activity_id` 时：

1. 只扣**进行中活动**下的未过期、有余额、店铺范围匹配的批次  
2. ~~先 `scope_type=1`（线上）；该层匹配不到或积分不足时，再扣 `scope_type=3`（全场景/通用）~~  
3. **不扣** `scope_type=2`（仅线下）  
4. ~~同层内：`expire_time ASC`（0 排最后）→ `remain_point ASC` → `id ASC`~~  
5. 合计仍不足 → 抛错，由调用方事务回滚（见 OD4）

### OD4：事务 — 保持现有流程，不做额外改造

**现网事实（已核实）：**

- `WxappNormalOrderCreateTransactionalRunner` / `ShopadminNormalOrderCreateTransactionalRunner` 均为 `@Transactional`，`runAfterOrderInsert`（含积分扣减）在**同一事务内**调用，并非真正的「事务已提交后」。扣减失败会连带订单创建回滚 → **表现为未生成订单**。
- 纯积分支付在订单已创建之后的支付阶段扣减；扣减失败则支付失败，订单保持未支付。此为既有语义，本计划不改。

**决定：**

- 不新造事务方案；只把扣减实现从手工调整换成 `deductByRule`
- 批次扣减 + consume + log 三者在 `deductByRule` 内同事务，并随调用方事务一并提交/回滚
- 下单抵扣路径：积分不足 / 无匹配批次 → 抛错 → 整笔下单回滚（未生成订单）
- 纯积分支付路径：保持现有「支付失败、订单已存在」表现

### OD5：退款/取消 — 只走批次原路返还，删除余额直加兼容

已有 `PointBatchRefundService.refundFullOrder` / `refundByRatio`，按 `order_id` 查 `point_batch_consume`。

**决定：**
- 线上抵扣改挂批次后，取消/售后退积分**只走**批次退款（按 order_id 查 consume 记录原路返还）
- **删除**「无 consume 记录时走 point_member 余额直加」的遗留兼容分支
- 无 consume 记录 = 该订单未扣过批次积分，无积分可退，不做任何返还
- 涉及文件：`PointMemberAddPointService`（取消订单返还）、`PointMemberCancelOrderReturnPointsService`（取消返还）中的「无 consume 走余额」分支需删除

### OD6：journal_type 沿用既有「消费购物（支出）」

**现状核对：**

| 来源 | journal_type | 说明 |
|------|--------------|------|
| `PointMemberJournalTypeDescriptions` | **6** =「消费购物（支出）」 | 字典已有，语义即订单消费扣积分 |
| 现网 `addPointForManualAdjustment` | **12** =「商家手动修改」 | 因误走手工调整路径产生，非业务本意 |
| 同字典 5 | 「积分换购」 | 换购场景，非普通订单抵扣 |

**决定：** 不新增 journal_type。线上抵扣 / 纯积分支付写 `point_member_log` 时使用 **`journal_type=6`**（消费购物支出）。描述沿用/贴近现网文案（如「订单抵扣积分」「订单{orderId}支付扣减积分」），并带上订单号；一批次一条。

### OD7：`distributor_id` 身份

**决定：** 下单 / 支付参数中的 `distributor_id` **就是** `distribution_distributor.distributor_id`，与活动 `scope_distributor_ids`、批次店铺匹配同一 ID 空间（与 D15 一致）。本计划不做 Merchant.id 映射。

店铺范围匹配：先按该 ID 匹配 `scope_type=1` 批次；匹配落空或不足再走 `scope_type=3`；仍无可扣批次则抛错并按 OD4 回滚。

### OD8：明确不做

- **达摩 CRM 积分打通**：本计划不考虑、不支持；接入 CRM 的公司不在本改造范围内（不再经 `addPointForManualAdjustment` 的达摩分支）
- **升值积分**（`uppoint_use` / `point_up_use` / 升值抵扣服务）：不考虑；批次扣减只针对真实会员积分 `point_use` / `point_amount`

---

## 分阶段规划

### Phase OD-A：接入公共扣减（主）

1. 确认/等待 `deductByRule`（D20）可用，且已按批次写 `point_member_log`  
2. 新增或调整 point 侧线上抵扣入口（建议）：

```java
// 示例：PointMemberOrderDeductService / 或在现有 Service 中增加方法
public Map<String, Object> deductForOrder(
    long companyId, long userId, int pointUse,
    long distributorId, String orderId) {
  // 校验 pointUse > 0、distributorId、orderId
  // 可选：校验 isOpenDeductPoint（与下单前校验对齐）
  return pointBatchDeductService.deductByRule(
      companyId, userId, pointUse, distributorId, orderId, null, null);
}
```

3. 改造 **两条** 调用方（保持既有触发条件，只换实现）：
   - `OrderCreatePostCommitPortImpl`：`point_use > 0` 且 `!"point".equals(payType)` → 调用上述入口，传入**真实订单号**（从 `ordersInsertResult.order_id` 读取，须在扣减前取得）与 `distributor_id`
   - `OrdersPaymentDoPaymentService`：`pay_type=point` → 同样改挂，传入已有 `orderId` 与 `distributor_id`
4. journal_type：**6**（消费购物支出）；一批次一条，描述含订单号  
5. 单测：无活动分层、排序、多批次多流水、不足抛错、两入口均覆盖

### Phase OD-B：取消/售后回归

1. 下单抵扣后取消订单 → `refundFullOrder` 按订单号恢复批次  
2. 纯积分支付订单取消 / 退款同样按 `order_id` 命中 consume  
3. 售后退款比例返还仍可用  
4. **删除**「无 consume 时旧余额直加」分支（OD5）：无 consume 记录则不退积分，不做遗留兼容
5. **改造 `PointBatchRefundService.doRefund` 退款排序与流水粒度：**
   - **排序：** 退款时按关联批次的 `expire_time` **降序**处理（先返还到期时间晚的批次），确保短期内要过期的批次最后才被恢复（优先把积分还到存活时间更长的批次）
   - **实现：** `selectRefundableByOrderId` 改为 JOIN `point_batch` 取 `expire_time`，按 `expire_time DESC`（0=永不过期排最前）排序
   - **流水粒度：** 每个批次恢复积分时写**一条** `point_member_log`（一批次一条），不再写单条汇总 log。字段含 `batch_id`、`activity_id`、`income=该批次恢复积分`
   - **过期批次：** 仍不恢复可用积分，标记已退款，在 log 描述中体现"过期作废"；过期批次也单独写一条 log（income=0，描述含过期作废信息）

### Phase OD-D：扣减排序改造 — 有效期优先，同效期按范围窄优先 ✅ Done

**背景：** 当前实现是「先分层（线上/通用两层拼接），层内按 expire_time 排序」。需求调整为**有效期排序在最外层**，仅当有效期相同时，再按使用范围从窄到宽排序。线下同理。

**新排序规则（替代 OD3 第 2-4 条与 D20 范围优先级）：**

**线上抵扣（`activityId == null`）：**

统一排序（不再拆两层拼接）：
1. `expire_time ASC`（0=永不过期排最后）
2. 同 expire_time 时，按范围从窄到宽：
   - 优先级 1：`scope_type=1` 且 `activity.online_scope_type=1`（线上-仅前置仓）
   - 优先级 2：`scope_type=1` 且 `activity.online_scope_type=0`（线上-通用）
   - 优先级 3：`scope_type=3`（全场景/通用）
3. 范围也相同时：`remain_point ASC`（剩余少的先扣，尽快耗完小批次）
4. 再同：`id ASC`（稳定次序）
5. 仍**不扣** `scope_type=2`（仅线下）

**线下抵扣（`activityId != null`，`distributorId > 0`）：**

统一排序：
1. `expire_time ASC`（0=永不过期排最后）
2. 同 expire_time 时，按范围从窄到宽：
   - 优先级 1：`scope_type=2`（仅线下）
   - 优先级 2：`scope_type=3`（全场景/通用）
3. 范围也相同时：`remain_point ASC`（剩余少的先扣）
4. 再同：`id ASC`（稳定次序）
5. 仍**不扣** `scope_type=1`（仅线上）

> **注意：** 线下核销是单活动维度，D23 编辑同步机制保证同活动下所有活跃批次 scope_type 一致，因此线下的范围分层实际不会产生效果。代码保留此排序作为防御性兜底（应对同步窗口期或历史数据），但正常流程下等价于 `expire_time → remain_point → id`。

**实现要点：**

1. **删除旧 `deduct()` 方法：** `PointBatchDeductService` 中的 `deduct(companyId, userId, pointToDeduct, distributorId, orderId, allowedScopeTypes)` 及其无参重载在生产代码中已无调用方（仅测试引用），Phase OD-D 实施时一并删除，相关测试迁移到 `deductByRule` 上。

2. **批次关联活动的 `online_scope_type`：** `PointBatch` 无此字段，需通过 `activityId` 关联查询 `PointActivity.onlineScopeType`。在 `deductByRule` 中批量 preload 涉及的活动 ID → `onlineScopeType` 映射。

2. **改造 `deductByRule()` 线上分支：**
   - 废弃 `onlineFirst` / `universal` 两个 list 的拼接方式
   - 过滤后的 matched 列表使用 `Comparator` 统一排序：
     ```java
     matched.sort(Comparator
         .comparingInt((PointBatch b) -> b.getExpireTime() == null || b.getExpireTime() == 0
             ? Integer.MAX_VALUE : b.getExpireTime())
         .thenComparingInt(b -> scopePriority(b, activityOnlineScopeMap))
         .thenComparingInt(b -> b.getRemainPoint() == null ? 0 : b.getRemainPoint())
         .thenComparingLong(PointBatch::getId));
     ```
   - `scopePriority` 线上实现：scope_type=1 + online_scope_type=1 → 0；scope_type=1 + online_scope_type=0 → 1；scope_type=3 → 2

3. **改造 `deductByRule()` 线下分支：**
   - 同样改为 Comparator 统一排序（不再依赖纯 SQL 排序 + allowedScopeTypes 过滤后不分层）：
     ```java
     matched.sort(Comparator
         .comparingInt((PointBatch b) -> b.getExpireTime() == null || b.getExpireTime() == 0
             ? Integer.MAX_VALUE : b.getExpireTime())
         .thenComparingInt(b -> b.getScopeType() != null && b.getScopeType() == 2 ? 0 : 1)
         .thenComparingInt(b -> b.getRemainPoint() == null ? 0 : b.getRemainPoint())
         .thenComparingLong(PointBatch::getId));
     ```

4. **SQL 排序简化：** `selectAvailableForDeduct` / `selectAvailableForDeductByActivity` 的 ORDER BY 可保留（作为初步排序），Java 侧 Comparator 是最终定序。或者移除 SQL 排序改为纯 Java 排（数据量不大，单用户维度）。

5. **`getAvailablePoint` 对齐：** 查询可用积分的方法不受排序影响（只求总和），无需改动。

**涉及文件：**
- `PointBatchDeductService.java` — 主改造
- `PointActivityMapper.java` 或 `PointActivityCreateService.java` — 可能需要批量按 ID 查活动的 online_scope_type
- `PointBatchMapper.java` — SQL ORDER BY 可选调整

---

### Phase OD-C（必须）：下单前可用积分查询对齐

**调查结论（2026-07-18 确认）：偏差已存在且 OD-A 上线后会累积恶化。**

**现状：**
- 结算页调用 `PointMemberBalanceReadService.getPointBalance(companyId, userId)` → 直接读 `point_member.point` 字段
- 该字段是旧体系余额：批次系统（grant/deduct/refund）均不操作此字段（设计如此，注释明确写了"不再同步 point_member.point，余额改为实时聚合查询"）
- 下单前校验：`NormalOrderPointDeductionApplyService.apply()` 也用 `point_member.point` 判断"积分不足"

**偏差来源：**

| 偏差因素 | 影响 |
|----------|------|
| `point_member.point` 与批次系统完全脱钩 | 批次发放不加、扣减不减、退款不恢复 |
| 仅线下批次（scope_type=2）不可用于线上 | 展示比实际可用多 |
| 过期批次不可扣 | 展示比实际可用多 |
| 店铺范围不匹配的批次不可扣 | 展示比实际可用多 |

**失败场景：** 结算页展示 `point_member.point=1000`，但线上合格批次只有 500 → 用户填 600 → 下单时 `deductByRule` 积分不足 → 抛错回滚。

**改造方案：结算页"可用积分"改为实时聚合批次余额查询**

~~OD-C.1 方案（同步 point_member.point）已否决~~：批次系统发放时不加此字段，若扣减时减会导致不对称（字段变负 / 永久失真）。正确做法是读时聚合。

**实现：修改 `PointMemberBalanceReadService.getPointBalance`**

改为按规则聚合 `point_batch.remain_point`（不再读 `point_member.point`）：

```sql
SELECT COALESCE(SUM(b.remain_point), 0)
FROM point_batch b
INNER JOIN point_activity a ON b.activity_id = a.id
WHERE b.user_id = #{userId}
  AND b.company_id = #{companyId}
  AND b.status = 1
  AND (b.expire_time = 0 OR b.expire_time > #{now})
  AND b.remain_point > 0
  AND a.status = 1
  AND b.scope_type IN (1, 3)
```

**说明：**
- 只计 scope_type=1（线上）和 3（全场景）的进行中活动未过期批次
- 不加 `distributorId` 店铺匹配过滤（结算页还未选定店铺时无法过滤；即使选定了，scope_type=3 是通用范围无需匹配）
- 如果需要精确到店铺级别，可在已知 `distributorId` 时额外传入做 `PointBatchScopeMatcher` 过滤，但作为 MVP 先按上述 SQL 即可
- `NormalOrderPointDeductionApplyService.apply()` 中的校验也需同步改为调用此方法

**涉及文件：**
- `PointMemberBalanceReadService.java`（改查询逻辑）
- `PointBatchMapper.java`（新增聚合 SQL）
- `NormalOrderPointDeductionApplyService.java`（如果校验逻辑独立读余额的话，改为调同一方法）

**优先级：必须随 OD-A 一起上线。**

---

## 关键改造点（代码索引）

| 用途 | 文件 |
|------|------|
| 下单后扣积分入口 | `ecshopx-orders/.../OrderCreatePostCommitPortImpl.java` |
| 纯积分支付扣积分入口 | `ecshopx-orders/.../OrdersPaymentDoPaymentService.java` |
| 旧手工扣减（需摘掉订单抵扣 / 积分支付用法） | `ecshopx-point/.../PointMemberAddPointService.java` |
| 公共扣减（D20） | `ecshopx-point/.../PointBatchDeductService.java` |
| journal_type 字典 | `ecshopx-point/.../PointMemberJournalTypeDescriptions.java`（确认 6） |
| 取消返还 | `ecshopx-point/.../PointMemberCancelOrderReturnPointsService.java` |
| 批次退款 | `ecshopx-point/.../PointBatchRefundService.java` |
| 结算抵扣计算（金额↔积分，非批次 FIFO） | `NormalOrderCheckoutPointDeductService` / `NormalOrderPointDeductionApplyService` |

---

## 明确不在本计划

- 线下核销、`point_order`、HX/CO 双单号（见 `POINT_ORDER_PLAN`）
- C 端扫码页/店铺展示接口
- 修改 D20 排序契约
- 开票/结算标记业务
- 达摩 CRM 积分打通
- 升值积分（uppoint）逻辑改造
- 改变现有下单 / 支付事务边界（保持原流程）

---

## 验证方式

1. 用户仅有「线上」批次积分下单抵扣 → 只扣线上批次；流水条数 = 扣减批次数；`journal_type=6`  
2. 线上不足（或店铺范围匹配落空）再扣「全场景」批次；不扣「仅线下」批次  
3. 同过期时间优先扣 `remain_point` 更小的批次  
4. `point_batch_consume.order_id` / `point_member_log.order_id` = 商城订单号  
5. **无** `point_order` 记录  
6. 取消订单后批次 `remain_point` 按规则恢复；过期批次不复活（既有退款规则）  
7. 下单抵扣积分不足 / 无匹配批次 → 抛错且**未生成订单**（事务回滚）  
8. 纯积分支付路径同样走 `deductByRule`；扣减失败时支付失败（订单已存在、保持未支付——既有表现）  

---

## 注意事项

1. 规则真源在 `POINT_ORDER_PLAN` D20；本计划只描述线上调用方，避免两处复制后漂移。  
2. **（已确认）** `deductByRule` 不写 `point_member_log`，只负责扣批次余额 + 写 `point_batch_consume`。调用方需遍历 `DeductResult.getConsumeRecords()`（含 batchId、activityId、pointConsumed）逐条写 log。线下核销同此模式。线上入口写 log 时用 `journal_type=6`、描述含商城订单号。  
3. `OrderCreatePostCommitPortImpl` 命名含 PostCommit，但实际仍在订单创建事务内；改造时勿误迁到事务外。
