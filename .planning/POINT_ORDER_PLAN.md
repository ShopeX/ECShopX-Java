# 积分订单（Point Order）— 实现计划

## Context（背景）

Phase 9 已实现线下扫码核销积分，核销时通过 `PointOfflineConsumeService.consume()` 扣减积分、生成 HX 格式核销单号、写 `point_batch_consume` 记录和 `point_member_log`。

PRD 要求：核销发生时，同时生成一条「积分订单」记录，作为**更高层级的交易聚合视图**，供运营/财务在后台查看、筛选、导出、退款。积分订单与 `point_batch_consume`（批次级消费明细）是 1:N 关系。

---

## 设计决策

### D9：point_order 精简存储 + 关联查询

**决定：** `point_order` 表只存交易核心字段 + `distributor_id`（店铺ID），**不冗余**店铺参数（大区/区域/楼层等）和开票信息（账户名称/银行卡号/开户行）。

**原因：**
- 店铺参数是 EAV 动态字段，数量和名称随公司配置变化，固定列无法覆盖"所有参数都可筛选"的需求
- 开票信息功能尚未开发，数据来源表待建
- 通过 `distributor_id` 关联查询即可获取这些信息，IN 查询走索引性能可接受
- 店铺参数变更后，列表展示/筛选自动适配最新数据，无一致性问题

**查询策略：**
- 列表展示/导出：批量反查 `DistributorAttributesBatchReadPort.getAttributesByIds()` 补充店铺参数
- 按属性筛选：先查属性表拿到匹配的 distributor_id 列表，再 `WHERE distributor_id IN (...)` 筛选
- 开票信息：预留，等功能上线后加反查逻辑

### D10：开票信息（银行信息）— 预留，暂不采集

**决定：** 开票信息（账户名称/银行卡号/开户行）来自店铺关联的开票信息表（功能尚未开发）。当前阶段：
- point_order 表不存这些字段
- 列表/导出时预留位置，等开票功能上线后通过 distributor_id 关联查询补充
- 一个店铺只有一份开票信息，关联查询成本低

### D11：积分订单退款复用 PointBatchRefundService 核心

**决定：** 线下积分订单后台退款只支持**整单全额退款**，不支持部分退款。退款核心仍复用 `PointBatchRefundService` 的原路返还能力，但需要新增线下积分订单专用入口，例如：

```java
pointBatchRefundService.refundOfflinePointOrder(companyId, userId, orderNo)
```

该入口内部按 `order_no` 查询 `point_batch_consume`，对该订单消费的积分做 100% 全额原路返还，并写入新的积分日志类型。

**原因：**
- 退款核心逻辑已在 Phase 4 实现且经过验证
- `point_batch_consume.order_id` 存的是 point_order 的 `order_no`（JF 格式），可以直接传入
- 不能直接调用现有 `refundFullOrder(...)`，因为它当前用于“取消订单返还”，会写 `journal_type=9` 和“取消订单”语义的日志，不适合线下积分订单后台退款审计

**与线上退款的区分：**
- 线上退款入口在 `PointMemberAddPointService`，由订单取消/售后流程触发
- 线下积分订单退款由后台管理员在积分订单列表页手动发起
- 两者最终都走 `PointBatchRefundService` 的原路返还逻辑，但线下入口使用专用 `journal_type`

**新增日志类型：**
- `journal_type=22`：线下积分订单退款
- `point_desc` 示例：`线下积分订单{orderNo}退款返还积分{point}`
- 如果原批次已过期，仍按现有批次退款规则处理：消费记录标记已退款，过期部分不恢复到可用积分，并在描述中体现“过期作废”

### D12：积分汇率存储

**决定：** `point_rate` 存整数，含义是 **N 积分抵扣 1 元**。从积分规则配置中读取，创建订单时写入。

**计算公式：** `trade_amount(分) = trade_point * 100 / point_rate`

**示例：**
| point_rate | 含义 | 750积分 → 金额 |
|---|---|---|
| 1 | 1积分=1元 | 750元 |
| 10 | 10积分=1元 | 75元 |
| 100 | 100积分=1元 | 7.5元 |

**列表显示格式：** `"1:{point_rate}"`（元:积分），如 "1:1"、"1:10"

**来源：** 积分规则中配置的积分抵扣比例。创建订单时读取当前汇率写入，历史订单不受后续汇率变化影响。

**线下核销前置校验：**
- 线下核销创建积分订单需要可计算交易金额，因此必须校验积分抵扣开关
- 从 `PointMemberRuleReadService.getPointRule(companyId)` 读取规则
- `isOpenDeductPoint` 未开启时，接口直接抛错：`积分抵扣未开启，无法线下核销`
- `deduct_point` 为空、非数字或 `<= 0` 时，接口直接抛错：`积分抵扣比例未配置，无法线下核销`
- 本阶段 `point_rate` 保持 `INT`，因此 `deduct_point` 必须是正整数；如果配置为小数，接口直接抛错：`积分抵扣比例必须为正整数`
- `point_rate` 字段保存本次订单使用的 `deduct_point` 快照

### D13：店铺编码

**决定：** 店铺编码字段为 `shop_code`，从 `distribution_distributor.shop_code` 获取。通过 distributor_id 关联查询，不存入 point_order 表（和店铺参数一样走关联查询）。

### D14：筛选项动态展示 — 店铺参数全部可筛选

**决定：** 列表页筛选区域中，店铺参数部分是**动态的**——根据当前公司配置的全部店铺参数动态渲染筛选下拉框。

**实现方式：**
1. 前端调用"获取店铺参数列表" API，拿到参数名 + 每个参数的可选值列表
2. 动态渲染 N 个下拉筛选器（如"大区"下拉、"区域"下拉、"楼层"下拉……）
3. 用户选择某个参数值后，后端查询流程：
   ```
    用户选"大区=华东"
     → 查 distribution_distributor_rel_attributes 拿到 attribute_value_id 对应的 distributor_ids
     → SELECT * FROM point_order WHERE company_id=? AND distributor_id IN (...) AND 其他条件
   ```
4. 多个属性条件同时筛选时，取交集（每个属性条件查出的 distributor_ids 做 AND）

**可选值来源：** `distribution_distributor_attribute_values` 表（每个属性预设的选项值）

### D15：point_order.distributor_id 身份

**决定：** `point_order.distributor_id` 存的是 `distribution_distributor.distributor_id`（店铺表主键），**不是** `Merchant.id`。

**影响：**
- 列表/导出关联 `shop_code`、店铺名称、EAV 属性时，直接用该 ID 查 `distribution_distributor` / `DistributorAttributesBatchReadPort`
- 线下核销入参 `distributor_id` 语义同步统一为店铺主键；店铺存在性校验改为查 `distribution_distributor`（公司匹配 + 有效），不再用 `PointCooperativeDistributorQueryService`（Merchant）冒充
- 活动 `scope_distributor_ids`、批次范围匹配、核销可用积分查询，均使用同一套 `distributor_id`

### D16：线下核销必须指定进行中的活动

**决定：** 线下核销改为「单活动核销」。C 端必须传入 `activity_id`，后端在扣减前完成校验，只从该活动的批次扣积分。

**校验顺序（全部通过才允许扣减）：**
1. `activity_id` 必传且 > 0；活动存在且属于当前 `company_id`
2. 活动状态为进行中：`PointActivityStatus.compute(...) == ACTIVE`
3. 活动适用范围允许线下：`scope_type` 为线下(2) 或全场景(3)
4. 当前店铺适用该活动：`PointBatchScopeMatcher.distributorMatches(activity, distributorId)`
5. 积分抵扣规则可用（见 D12 / D19）：`isOpenDeductPoint` 开启，且 `deduct_point` 为正整数
6. 用户在该活动 + 该店铺下的可用积分（未过期、`remain_point>0`、批次 scope 匹配、`activity_id` 匹配、scope_type∈{2,3}）≥ 本次核销积分数

**扣减约束：** `PointBatchDeductService` 增加按 `activityId` 过滤；线下核销只扣该活动批次。多活动混扣不再发生，创建订单时直接写该 `activity_id`。

**范围说明：** 本计划只定义核销相关接口（`available` / `consume`）必须传 `distributor_id` + `activity_id`。扫码进页、按店铺展示内容等「仅 `distributor_id`」的页面展示接口不在本计划，后续另开计划。

### D17：属性筛选空集返回空分页

**决定：** 动态店铺属性筛选得到的 `matchedDistributorIds` 为空集时，**直接返回空分页**，不得省略 `IN` 条件（否则等于不过滤）。

### D18：退款并发安全

**决定：** `PointOrderRefundService.refund()` 用条件更新保证幂等，禁止纯 select + updateById：

```sql
UPDATE point_order
SET status = 2, is_refunded = 1, refund_time = ?, updated = ?
WHERE id = ? AND company_id = ? AND status = 1 AND is_refunded = 0
```

`affected rows == 0` → 抛「该订单不可退款」。先条件更新成功后再调用 `refundOfflinePointOrder`；若批次退款抛错则整笔事务回滚（订单状态一并回滚）。

### D19：isOpenDeductPoint 未开启则禁止线下核销

**决定：** 与 D12 一致。`isOpenDeductPoint` 未开启时，线下可用积分查询与核销接口均直接拒绝：`积分抵扣未开启，无法线下核销`。

### D21：积分订单编码（point_code）独立生成

**决定：** `point_order.point_code` 是订单级别的独立编码，核销时生成，**不是**从 `PointBatch.point_code` 拼接。一个积分订单对应一个编码。

**生成规则：** `{prefix:5}{yyyyDDD:7}{userId}{Redis seq:06d}`

- `prefix`：5 位字母数字，来自该积分活动的 `point_code_prefix` 配置
- `yyyyDDD`：年 + 年内天数（如 2026198 = 2026年第198天）
- `userId`：消费用户 ID
- `Redis seq`：6 位零填充序号

**Redis key：** `point:order_code:{companyId}:{prefix}:{yyyyDDD}`，TTL 2 天

**示例：** prefix="100SH", 2026年7月17日(第198天), userId=19797030092, seq=1：
→ `100SH202619819797030092000001`

**唯一性保证：** prefix(公司内唯一) + 日期 + userId + Redis 原子自增 + DB `uk_point_code`

### D22：删除 point_batch.point_value 和 point_code 字段

**决定：**
- `point_batch.point_value`（积分对应金额）：永远赋值为 0，无任何逻辑读取
- `point_batch.point_code`（批次级积分编码）：只在发放时写入，从未被读取、查询、展示

两个字段均为 write-only，本次直接删除。

**影响范围：**
- `PointBatch.java` 实体：删除 `pointValue`、`pointCode` 属性及 getter/setter；删除 `uk_point_code` 索引注解
- `PointActivityDistributeService.buildBatchFromActivity()`：删除 `batch.setPointValue(0)` 和 `batch.setPointCode(...)` 行
- `PointCodeGenerator.java`：整个类删除（无其他调用方）
- DDL：`ALTER TABLE point_batch DROP COLUMN point_value, DROP COLUMN point_code, DROP INDEX uk_point_code`

### D23：活动编辑同步 scope 字段到活跃批次

**决定：** 进行中的活动可以修改 scope 相关字段。修改时必须将 `scope_type`、`scope_distributor_type`、`scope_distributor_ids` **三个字段一起**同步到该活动下所有活跃批次。

**当前现状（bug）：** `PointActivityScopeService.appendDistributors()` 只同步了 `scope_distributor_ids`，未同步 `scope_type` 和 `scope_distributor_type`。

**修复方式：** 活动编辑保存时，统一调用：

```sql
UPDATE point_batch
SET scope_type = #{scopeType},
    scope_distributor_type = #{scopeDistributorType},
    scope_distributor_ids = #{scopeDistributorIds},
    updated = #{updated}
WHERE activity_id = #{activityId} AND company_id = #{companyId}
  AND status = 1 AND remain_point > 0
```

**条件说明：** 只同步 `status=1`（有效）且 `remain_point>0`（有余额）的批次。已消费完/过期的批次保持历史快照。

**触发时机：** 活动编辑接口中任何 scope 字段发生变更时调用。

### D24：所有 DB 变更走 Flyway 迁移（不写 docker/*.sql）

**决定：** 本计划涉及的建表 / 改字段 / 删字段一律通过 Flyway 增量迁移落地，遵循 `readme_cn.md` 的「数据库迁移（Flyway）」流程，**不再**新增 `docker/*.sql`（`docker/ecshopx.sql` 仅是整库初始化快照，不作为增量变更载体）。

**流程：**
1. 先写好/改好 MyBatis-Plus domain（`@MpTable`/`@MpField`/`@MpId`），如 `PointOrder.java`、`PointBatch.java`
2. 用 `bin/make-migration <desc>` 生成 `V{yyyyMMddHHmmss}__<desc>.sql`（目录：`ecshopx-bootstrap/src/main/resources/db/migration`）
3. **人工 review** 生成的 SQL：类型、长度、默认值、NULL、注释、列顺序，尤其**二级索引**（make-migration 不会凭空生成普通索引，需手动补 `point_order` 的 `uk_order_no` / `idx_*`）
4. `bin/make-migration migrate` 手动执行；记录入 `flyway_schema_history`
5. 已有库经 `baselineOnMigrate=true` 从版本 0 建基线；`spring.flyway.enabled=false`，不随应用启动自动执行

**本计划涉及的迁移文件（示意命名，最终以生成时间戳为准）：**

| 变更 | 迁移文件（示意） | 说明 |
|------|----------------|------|
| 新建积分订单表 | `V{ts}__create_point_order.sql` | `point_order` 建表 + 索引（对应 9A.1 DDL） |
| 删除 point_batch.point_value | `V{ts}__drop_point_batch_point_value.sql` | 对应 D22 |

**注意：** `DROP COLUMN` 类不可逆变更，make-migration 默认不生成 DROP，需显式 `--allow-drop` 或手写；执行前确认无代码/历史依赖（D22 已确认永远为 0）。

### D20：扣减排序与流水规则（线下/线上共用）

**决定：** 线下核销与线上抵扣共用同一套批次排序与流水粒度；**唯一入口差异是是否传入 `activity_id`**。不生成积分订单的线上场景仍走同一扣减核心，只是调用方不创建 `point_order`。

**批次候选过滤：**
- 有 `activity_id`（线下核销）：只扣该活动下、未过期、有余额、店铺范围匹配、且活动进行中的批次；再叠加调用方允许的 `scope_type`（线下传 `[2,3]`）
- 无 `activity_id`（线上抵扣）：不限定单个活动；只扣**进行中活动**下的批次，并按下方「范围优先级」分层

**范围优先级（仅无 activity_id 时）：**

> ⚠️ 已由 [POINT_ONLINE_DEDUCT_PLAN.md Phase OD-D](./POINT_ONLINE_DEDUCT_PLAN.md) 替代下方旧分层规则。新规则为**统一排序**（不再先分层拼接）：

~~1. 先扣使用范围 = **线上**（`scope_type=1`）的进行中活动批次~~
~~2. 线上扣完仍不足，再扣使用范围 = **全场景/通用**（`scope_type=3`）的进行中活动批次~~
~~3. 不扣仅线下（`scope_type=2`）批次~~

**新规则 — 统一排序（线上/线下共用主排序键）：**

线上（无 `activity_id`）：
1. `expire_time ASC`（0=永不过期排最后）
2. 同 expire_time：scope_type=1+online_scope_type=1（仅前置仓）→ scope_type=1+online_scope_type=0（线上通用）→ scope_type=3（全场景）
3. 范围也相同：`remain_point ASC`
4. `id ASC`
5. 不扣 `scope_type=2`

线下（有 `activity_id`）：
1. `expire_time ASC`（0=永不过期排最后）
2. 同 expire_time：scope_type=2（仅线下）→ scope_type=3（全场景）
3. 范围也相同：`remain_point ASC`
4. `id ASC`
5. 不扣 `scope_type=1`

> 注：线下是单活动维度，D23 同步机制保证同活动下批次 scope_type 一致，范围分层实际不生效。

**~~同层内排序（线下有活动 / 线上每一层都一样）：~~**（已废弃，合并到上方统一排序）
~~1. `expire_time` 升序（先过期先扣；`expire_time=0` 永不过期排最后）~~
~~2. 同过期时间：`remain_point` 升序（剩余少的先扣）~~
~~3. 再同：`id` 升序（稳定次序）~~

> 说明：当前 `PointBatchMapper.selectAvailableForDeduct` 已具备 `expire_time ASC, remain_point ASC, id ASC`；本计划需补齐「进行中活动」过滤、无 activity 时的线上→通用分层，以及有 activity 时的活动限定。

**流水粒度：**
- 每扣减 **一个** `point_batch` → 写 **一条** `point_batch_consume`（已有）
- 同时写 **一条** `point_member_log`（一批次一条流水，禁止再按活动汇总）
- 字段：`batch_id`、`activity_id`、`outcome=本批次扣减积分`、`order_id`（线下核销 log 仍用 HX；consume.order_id 用 JF）
- 一次核销/抵扣跨 N 个批次 → N 条 consume + N 条 log

**公共方法抽取（实现时必须做）：**

在 `PointBatchDeductService`（或同级 Deduct 编排类）抽出统一入口，例如：

```java
/**
 * 扣减结果 DTO
 */
public class DeductResult {
    private int pointDeducted;                    // 实际扣减积分总数
    private List<ConsumeRecord> consumeRecords;   // 每个批次的扣减明细
    
    public static class ConsumeRecord {
        private long batchId;
        private long activityId;
        private int pointConsumed;   // 该批次扣减积分
    }
}

/**
 * @param activityId 可空；非空=只扣该活动（线下）；空=线上分层：线上 → 全场景
 * @param allowedScopeTypes 可空；线下传 [2,3]；线上分层逻辑内部处理 1 再 3
 */
DeductResult deductByRule(
    long companyId, long userId, int pointToDeduct,
    long distributorId, String orderId,
    Long activityId, List<Integer> allowedScopeTypes);
```

- **线下核销：** `deductByRule(..., activityId, [2,3])` → 再创建 `point_order`；log 的 `order_id` 用 writeoffNo
- **线上抵扣：** `deductByRule(..., null, null)` → **不**创建 `point_order`；由订单链路写自己的 orderId
- 排序、分层、一批次一条 consume/log 的细节只维护这一处，避免两套 FIFO

**本计划范围：** D20 的规则与公共方法改造纳入 Phase 9A（核销依赖同一扣减核心）。线上调用方改挂到该公共方法见专项计划：[POINT_ONLINE_DEDUCT_PLAN.md](./POINT_ONLINE_DEDUCT_PLAN.md)；**排序/流水契约以本决策为准，不得再分叉**。

---

## 分阶段规划（3 个子 Phase）

### Phase 9A：数据模型 + 积分订单创建（嵌入核销流程） ✅ 已完成（commit ebd661dc）
### Phase 9B：Admin 后台列表/详情/退款 API + 动态筛选
### Phase 9C：导出功能

---

## Phase 9A 详细设计 — 数据模型 + 创建服务

### 9A.1 数据库表：`point_order`

> **落地方式（D24）：** 下方 DDL 为目标结构参考。实际以 `PointOrder.java` domain + `bin/make-migration` 生成 Flyway 迁移文件落库；生成后人工补齐并核对索引（make-migration 不生成普通二级索引），确保与下方 `uk_*`/`idx_*` 一致。

```sql
CREATE TABLE point_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键自增ID，同时作为交易流水号',
    company_id BIGINT NOT NULL COMMENT '公司ID',
    user_id BIGINT NOT NULL COMMENT '消费用户ID',
    -- 单号
    order_no VARCHAR(64) NOT NULL COMMENT '交易单号（全局唯一，JF格式）',
    writeoff_no VARCHAR(64) DEFAULT NULL COMMENT '核销单号（HX格式）',
    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=已支付, 2=已退款',
    -- 交易信息
    trade_time INT NOT NULL COMMENT '交易时间（Unix时间戳）',
    trade_amount INT NOT NULL DEFAULT 0 COMMENT '交易金额（分）',
    trade_point INT NOT NULL DEFAULT 0 COMMENT '交易积分',
    point_rate INT NOT NULL DEFAULT 1 COMMENT '积分汇率（N积分抵1元）',
    -- 结算/退款
    is_settled TINYINT NOT NULL DEFAULT 0 COMMENT '是否结算：0=否, 1=是（预留）',
    is_refunded TINYINT NOT NULL DEFAULT 0 COMMENT '是否退款：0=否, 1=是',
    refund_time INT DEFAULT NULL COMMENT '退款时间（Unix时间戳）',
    -- 活动信息
    activity_id BIGINT DEFAULT NULL COMMENT '积分活动ID（核销入参，单活动）',
    -- 积分编码
    point_code VARCHAR(64) DEFAULT NULL COMMENT '积分编码（核销时生成，全局唯一）',
    -- 客户信息
    mobile VARCHAR(20) DEFAULT NULL COMMENT '客户手机号',
    -- 店铺信息（仅存 distribution_distributor.distributor_id，其他信息关联查询）
    distributor_id BIGINT DEFAULT NULL COMMENT '店铺ID（distribution_distributor.distributor_id）',
    created INT NOT NULL COMMENT '创建时间',
    updated INT NOT NULL COMMENT '更新时间',
    UNIQUE INDEX uk_order_no (order_no),
    UNIQUE INDEX uk_point_code (point_code),
    INDEX idx_writeoff_no (writeoff_no),
    INDEX idx_company_status_time (company_id, status, trade_time),
    INDEX idx_company_trade_time (company_id, trade_time),
    INDEX idx_mobile (mobile),
    INDEX idx_activity (activity_id),
    INDEX idx_distributor (distributor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分订单表';
```

**与之前方案对比（去掉的列）：**
- ~~distributor_code~~ → 关联 Distributor 查询
- ~~shop_name~~ → 关联 Distributor 查询
- ~~region/area/floor_info~~ → 关联 EAV 属性表查询
- ~~account_name/bank_card_no/bank_name~~ → 关联开票信息表查询（待开发）

### 9A.2 实体 `PointOrder.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/domain/PointOrder.java`

参照 `PointBatchConsume.java` 的注解风格（`@MpTable` + `@MpField` + `@MpId`）。

### 9A.3 Mapper `PointOrderMapper.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/mapper/PointOrderMapper.java`

继承 `BaseMapper<PointOrder>`，新增自定义查询：
- `selectByOrderNo(orderNo, companyId)` — 按订单号查
- `selectByTransactionNo(transactionNo, companyId)` — 按流水号查

### 9A.4 交易单号生成器 `PointOrderNoGenerator.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/service/activity/PointOrderNoGenerator.java`

**交易流水号：** 直接使用 `point_order.id`（数据库自增主键），列表展示时显示此 ID 即可，不需要额外生成。

**交易单号（order_no）：** 需全局唯一，采用项目已有的「epoch-day + 90s slot + Redis HASH increment」模式（对齐 `NormalOrderNumericIdService` / `DepositTradeIdGenerator`）。

```java
@Service
public class PointOrderNoGenerator {
    // 格式：JF{daysSinceEpoch}{minuteSlot:3}{redisSeq:5}{userIdMod:4}
    // epoch 起点：1325347200 (2012-01-01 00:00:00 UTC+8)，与 NormalOrderNumericIdService 一致
    // Redis key: HASH "point_order_no:{yyyyMMdd}" field={minuteSlot}, TTL 1天
    // 每 90s slot 容量 99999，远超业务需求
    
    private static final long EPOCH = 1325347200L;
    private final StringRedisTemplate redisTemplate;
    
    public String nextNo(long userId) {
        long now = System.currentTimeMillis() / 1000;
        long day = (now - EPOCH) / 86400;
        long secondOfDay = (now - EPOCH) % 86400;
        long minuteSlot = secondOfDay / 90;
        
        String dateKey = "point_order_no:" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long seq = redisTemplate.opsForHash().increment(dateKey, String.valueOf(minuteSlot), 1);
        redisTemplate.expire(dateKey, Duration.ofDays(1));
        
        long userMod = userId % 10000;
        String numericPart = String.valueOf(day)
            + String.format("%03d", minuteSlot)
            + String.format("%05d", seq)
            + String.format("%04d", userMod);
        return "JF" + numericPart;
    }
}
```

**唯一性保证：** Redis HASH 原子自增（跨实例安全） + userId mod 散列 + 数据库 `uk_order_no`。每 90s slot 容量 99999 笔，无溢出风险。

### 9A.5 创建服务 `PointOrderCreateService.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/service/activity/PointOrderCreateService.java`

**核心职责：** 在核销完成后，聚合上下文信息创建 `point_order` 记录。

**数据采集来源：**

| 字段 | 来源 |
|------|------|
| id (交易流水号) | 数据库自增主键 |
| order_no (交易单号) | `PointOrderNoGenerator.nextNo(userId)`（Redis HASH，JF格式） |
| trade_point | `deductResult.getPointDeducted()` |
| trade_amount | `trade_point * 100 / point_rate` |
| point_rate | 从积分规则配置 `deduct_point` 读取并校验，作为本次订单快照 |
| activity_id | 核销入参 `activity_id`（已校验进行中）；活动名称/scope_type 通过 activity_id 动态读取 |
| point_code | `PointOrderCodeGenerator.nextCode(companyId, prefix, userId)`（核销时新生成，D21） |
| mobile | `MemberAccountService.getMemberInfo(userId, companyId)` → `mobile` |
| distributor_id | consume 入参的 `distributor_id`（= `distribution_distributor.distributor_id`） |

**伪代码：**
```java
@Service
public class PointOrderCreateService {
    public PointOrder createFromConsume(
            long companyId, long userId, long distributorId, long activityId,
            String writeoffNo, String orderNo, DeductResult deductResult) {
        int now = PointActivityCreateService.nowSec();
        int totalPoint = deductResult.getPointDeducted();
        PointRateSnapshot rate = validateAndReadPointRate(companyId); // 校验抵扣开关 + deduct_point

        PointOrder order = new PointOrder();
        order.setCompanyId(companyId);
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setWriteoffNo(writeoffNo);
        order.setStatus(1);
        order.setTradeTime(now);
        order.setTradePoint(totalPoint);
        order.setTradeAmount(calculateAmountFen(totalPoint, rate)); // N积分抵1元 → 金额（分）
        order.setPointRate(rate.toPersistValue());
        order.setDistributorId(distributorId);

        // 活动ID：直接存，活动名称/scope_type 通过 activity_id 动态读取
        order.setActivityId(activityId);
        // 积分编码：核销时独立生成（D21）
        String prefix = getActivityPointCodePrefix(companyId, activityId);
        order.setPointCode(pointOrderCodeGenerator.nextCode(companyId, prefix, userId));
        // 采集客户手机号
        populateCustomerMobile(order, companyId, userId);

        order.setCreated(now);
        order.setUpdated(now);
        pointOrderMapper.insert(order);
        return order;
    }
}
```

### 9A.6 修改 `PointOfflineConsumeService`（单活动核销）

**接口入参变更：**

| 接口 | 变更 |
|------|------|
| GET `.../offline/available` | 增加必填 `activity_id`；返回该活动在当前店铺下的可用积分 |
| POST `.../offline/consume` | body 增加必填 `activity_id` |

**consume 请求体：**
```json
{
  "distributor_id": 123,
  "activity_id": 456,
  "point_amount": 100
}
```

**调用顺序：**
1. 校验 `point_amount > 0`
2. 校验店铺：`distribution_distributor.distributor_id` 存在且属于当前公司（D15）
3. 校验积分抵扣规则（D12 / D19）
4. 校验活动：存在、进行中、允许线下、店铺适用（D16）
5. 校验该活动+店铺可用积分 ≥ `point_amount`
6. 生成 writeoffNo（HX）+ orderNo（`pointOrderNoGenerator.nextNo(userId)`）
7. 调用公共扣减 `deductByRule(..., orderNo, activityId, [2,3])`（D20：活动内按过期/剩余排序；consume.order_id=JF）
8. **按批次写 log**（D20：每个扣减批次一条 `point_member_log`；`order_id`=writeoffNo；填 `batch_id`/`activity_id`/`outcome`）
9. 创建 `point_order`（含生成 point_code）

**⚠️ point_batch_consume.order_id 语义变更：** 修改后存 JF 格式订单号。退款走 `refundOfflinePointOrder(companyId, userId, orderNo)`，写 `journal_type=22`。

**修改后完整流程：**
```java
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> consume(
        long companyId, long userId, long distributorId, long activityId,
        int pointAmount) {
    if (pointAmount <= 0) throw new BadRequestException("核销积分数必须大于0");
    validateDistributor(companyId, distributorId); // 查 distribution_distributor

    pointOrderCreateService.validatePointDeductRule(companyId); // isOpenDeductPoint + deduct_point
    PointActivity activity = validateActiveOfflineActivity(companyId, activityId, distributorId);

    int available = pointBatchDeductService.getAvailablePoint(
        companyId, userId, distributorId, ALLOWED_SCOPE_TYPES, activityId);
    if (available < pointAmount) {
        throw new BadRequestException("当前活动可用积分不足");
    }

    String writeoffNo = writeoffNoGenerator.nextNo(companyId);
    String orderNo = pointOrderNoGenerator.nextNo(userId);

    DeductResult deductResult = pointBatchDeductService.deductByRule(
        companyId, userId, pointAmount, distributorId, orderNo,
        activityId, ALLOWED_SCOPE_TYPES); // D20 公共扣减

    // D20：一批次一条流水（不再按活动汇总）
    writeLogsByBatch(companyId, userId, distributorId, writeoffNo, deductResult);

    pointOrderCreateService.createFromConsume(
        companyId, userId, distributorId, activityId, writeoffNo, orderNo, deductResult);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("writeoff_no", writeoffNo);
    result.put("order_no", orderNo);
    result.put("point_deducted", deductResult.getPointDeducted());
    result.put("distributor_id", distributorId);
    result.put("activity_id", activityId);
    return result;
}
```

**同步修改 `PointBatchDeductService`（落实 D20）：**
- 抽取公共方法 `deductByRule(...)`：返回 `DeductResult` DTO；有/无 `activityId` 共用排序；无 activity 时线上→全场景分层
- `getAvailablePoint` 与扣减使用同一过滤/排序规则（线下传 activityId + `[2,3]`）
- `DeductResult.consumeRecords` 每条含 `batchId`、`activityId`、`pointConsumed`
- 扣减成功后由调用方（或扣减服务内部约定清晰的一处）按 `consumeRecords` **逐条**写 `point_member_log`（一批次一条）
- 将 `writeLogsByActivity` 改为 `writeLogsByBatch`（或删除汇总逻辑）

**店铺校验改造：** `validateDistributor` 改为查 `distribution_distributor`（`distributor_id` + `company_id` + 有效状态），不再走 Merchant。

**available 同步改造：**
```
GET .../offline/available?distributor_id=X&activity_id=Y
```
进入查询前同样执行 D12/D16/D19 校验（抵扣开关、活动进行中、店铺适用）；通过后返回该活动在当前店铺下的可用积分。

**事务边界（必须保证）：**
- `PointOfflineConsumeService.consume(...)` 使用 `@Transactional(rollbackFor = Exception.class)`，整笔核销在**同一数据库事务**内完成
- 事务内 DB 写入包括：批次 `remain_point`/`status` 更新、`point_batch_consume` 插入、按批次 `point_member_log` 插入、`point_order` 插入
- 任一步失败抛异常 → **上述 DB 变更全部回滚**，不允许出现「已扣积分但无订单/无流水」或部分批次已扣的半成功状态
- `PointBatchDeductService.deductByRule(...)` 加入外层事务（默认 `REQUIRED`），不得另开独立事务把扣减提前提交
- **不在事务内、失败不回滚：** Redis 单号自增；可接受空号，不以 Redis 作为一致性依据

### 9A.7 新增文件清单

| 层 | 文件 | 说明 |
|---|---|---|
| **domain** | `PointOrder.java` | 积分订单实体 |
| **domain** | `DeductResult.java` | 扣减结果 DTO |
| **mapper** | `PointOrderMapper.java` | 订单 Mapper |
| **service/activity** | `PointOrderNoGenerator.java` | JF 格式订单号生成器（epoch-day + Redis HASH） |
| **service/activity** | `PointOrderCodeGenerator.java` | 积分编码生成器（prefix + yyyyDDD + userId + Redis seq） |
| **service/activity** | `PointOrderCreateService.java` | 订单创建（嵌入核销流程） |
| **db/migration** | `V{ts}__create_point_order.sql` | Flyway 建表迁移（D24；对应 9A.1 DDL，手动补索引） |
| **db/migration** | `V{ts}__drop_point_batch_point_value.sql` | Flyway 删列迁移（D22：删 point_value + point_code + uk_point_code 索引） |

**核销单号字段说明：** `point_batch_consume.order_id` 改存 JF 交易单号后，HX 核销号需要落到 `point_order.writeoff_no`。后台订单列表/详情可直接展示和检索核销单号，`point_member_log.order_id` 仍保留 HX 语义。

---

## Phase 9B 详细设计 — Admin 后台 API + 动态筛选

### 9B.1 查询服务 `PointOrderReadService.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/service/activity/PointOrderReadService.java`

**固定筛选条件：**

| 前端筛选项 | 对应 SQL 条件 |
|-----------|-------------|
| 交易流水号 | `id = ?`（自增主键） |
| 交易单号 | `order_no LIKE ?` |
| 核销单号 | `writeoff_no LIKE ?` |
| 积分来源名称 | `activity_id = ?`（活动选择器传 ID） |
| 客户手机号 | `mobile LIKE ?` |
| 状态 | `status = ?`（1=已支付, 2=已退款） |
| 交易时间开始 | `trade_time >= ?` |
| 交易时间结束 | `trade_time <= ?` |

**动态店铺属性筛选（核心差异）：**

前端传参格式：`attributes[{attribute_id}]={attribute_value_id}`（可多个）

后端处理流程：
```java
// 1. 根据 attribute_value_id 查询匹配的 distributor_ids
Set<Long> matchedDistributorIds = null;
for (属性筛选条件 : attributeFilters) {
    Set<Long> ids = queryDistributorIdsByAttributeValue(companyId, attributeId, attributeValueId);
    if (matchedDistributorIds == null) {
        matchedDistributorIds = ids;  // 第一个条件
    } else {
        matchedDistributorIds.retainAll(ids);  // 多条件取交集
    }
}
// 2. 空集直接返回空分页（D17）；非空才加 IN
if (matchedDistributorIds != null && matchedDistributorIds.isEmpty()) {
    return emptyPage(page, pageSize);
}
if (matchedDistributorIds != null) {
    wrapper.in(PointOrder::getDistributorId, matchedDistributorIds);
}
```

### 9B.2 店铺参数筛选选项 API

新增接口：返回当前公司配置的全部店铺参数 + 每个参数的可选值

```java
GET /api/v1/point/order/filter-attributes

响应：
{
  "data": [
    {
      "attribute_id": 1,
      "attribute_name": "大区",
      "values": [
        {"attribute_value_id": 101, "attribute_value": "华东"},
        {"attribute_value_id": 102, "attribute_value": "华南"},
        ...
      ]
    },
    {
      "attribute_id": 2,
      "attribute_name": "区域",
      "values": [...]
    },
    {
      "attribute_id": 3,
      "attribute_name": "楼层",
      "values": [...]
    }
  ]
}
```

数据来源：`distribution_distributor_attributes` + `distribution_distributor_attribute_values` 表

### 9B.3 列表响应 — 补充关联数据

列表查询完 point_order 分页数据后，批量补充关联信息：

```java
// 1. 收集当页所有 distributor_ids 和 activity_ids
Set<Long> distributorIds = pageRecords.stream()
    .map(PointOrder::getDistributorId).filter(Objects::nonNull).collect(toSet());
Set<Long> activityIds = pageRecords.stream()
    .map(PointOrder::getActivityId).filter(Objects::nonNull).collect(toSet());

// 2. 批量查活动信息（名称、scope_type）
Map<Long, PointActivity> activityMap = queryActivitiesByIds(companyId, activityIds);

// 3. 批量查店铺名称 + shop_code
Map<Long, Distributor> distributorMap = queryDistributorsByDistributorIds(companyId, distributorIds);

// 4. 批量查店铺属性（大区/区域/楼层等）
Map<Long, Map<String, String>> attrsMap = distributorAttributesBatchReadPort.getAttributesByIds(companyId, distributorIds);

// 5. 开票信息 — 预留（功能未上线，暂返回空）

// 6. 组装响应
for (PointOrder order : pageRecords) {
    PointActivity activity = activityMap.get(order.getActivityId());
    row.put("activity_name", activity != null ? activity.getActivityName() : null);
    row.put("scope_type", activity != null ? activity.getScopeType() : null);
    row.put("shop_name", distributorMap.get(order.getDistributorId()).getName());
    row.put("shop_code", distributorMap.get(order.getDistributorId()).getShopCode());
    row.put("attributes", attrsMap.get(order.getDistributorId())); // {"大区":"华东","区域":"上海",...}
    row.put("invoice_info", null); // 预留
}
```

### 9B.4 退款服务 `PointOrderRefundService.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/service/activity/PointOrderRefundService.java`

```java
@Service
public class PointOrderRefundService {
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refund(long companyId, long orderId) {
        PointOrder order = pointOrderMapper.selectById(orderId);
        if (order == null || order.getCompanyId() == null || order.getCompanyId() != companyId) {
            throw new BadRequestException("积分订单不存在");
        }
        if (order.getIsSettled() == 1) {
            throw new BadRequestException("已结算的订单不可退款");
        }
        // D18：条件更新抢占，防止并发双退
        int now = PointActivityCreateService.nowSec();
        int affected = pointOrderMapper.markRefundedIfPayable(orderId, companyId, now);
        if (affected == 0) {
            throw new BadRequestException("该订单不可退款");
        }
        // 复用批次退款核心，线下专用 journal_type=22；失败则整事务回滚（含上面的状态更新）
        return pointBatchRefundService.refundOfflinePointOrder(
            companyId, order.getUserId(), order.getOrderNo());
    }
}
```

`PointOrderMapper.markRefundedIfPayable` 对应 SQL：
```sql
UPDATE point_order
SET status = 2, is_refunded = 1, refund_time = #{now}, updated = #{now}
WHERE id = #{id} AND company_id = #{companyId} AND status = 1 AND is_refunded = 0
```

### 9B.5 Admin Controller `PointOrderController.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/api/admin/v1/PointOrderController.java`

参照 `PointMemberController` 模式（`@AdminAuth` + `@DingoResponse` + `@Activated`）：

| 方法 | 路径 | 名称 |
|------|------|------|
| GET | `/api/v1/point/order` | 积分订单列表（分页 + 固定筛选 + 动态属性筛选） |
| GET | `/api/v1/point/order/{id}` | 积分订单详情 |
| POST | `/api/v1/point/order/{id}/refund` | 积分订单退款 |
| GET | `/api/v1/point/order/export` | 积分订单导出 |
| GET | `/api/v1/point/order/filter-attributes` | 店铺参数筛选选项（动态） |

**列表 API 参数：**
```
page, pageSize,
transaction_no, order_no, writeoff_no, activity_id, mobile,
status, date_begin, date_end,
attributes[{attr_id}]={attr_value_id}  (动态属性筛选，可多个)
```

### 9B.6 新增文件清单

| 层 | 文件 | 说明 |
|---|---|---|
| **service/activity** | `PointOrderReadService.java` | 列表/详情/属性筛选 |
| **service/activity** | `PointOrderRefundService.java` | 订单退款（D18 条件更新） |
| **mapper** | `PointOrderMapper.markRefundedIfPayable` | 退款抢占 UPDATE |
| **api/admin/v1** | `PointOrderController.java` | Admin API（静态路径先于 `/{id}`） |

---

## Phase 9C 详细设计 — 导出功能

### 9C.1 导出服务 `PointOrderCsvExportService.java`

路径：`ecshopx-point/src/main/java/cn/shopex/ecshopx/point/service/export/PointOrderCsvExportService.java`

参照 `PointMemberLogCsvExportService` 模式：
- 分页批量查询（batch size 2000）
- 每页批量反查活动信息（名称、scope_type） + 店铺信息 + 属性 + 开票信息（预留）
- 组装 `LinkedHashMap<String, String>` 作为 CSV 列头
- 调用 `ExportCsvFileService.exportCsv()` 上传
- 调用 `ExportLogCreateService.createFinishLog()` 记录导出日志

**CSV 列定义（PRD 要求导出列表全部字段）：**
```java
LinkedHashMap<String, String> titles = new LinkedHashMap<>();
// 固定列
titles.put("id", "交易流水号");
titles.put("order_no", "交易单号");
titles.put("writeoff_no", "核销单号");
titles.put("status_desc", "状态");
titles.put("trade_time_str", "交易时间");
titles.put("trade_amount_yuan", "交易金额(元)");
titles.put("trade_point", "交易积分");
titles.put("point_rate_desc", "积分汇率");
titles.put("is_settled_desc", "是否结算");
titles.put("is_refunded_desc", "是否退款");
titles.put("activity_name", "积分活动");
titles.put("scope_type", "使用范围");
titles.put("point_code", "积分编码");
titles.put("mobile", "客户手机号");
// 关联查询列
titles.put("shop_code", "店铺编码");
titles.put("shop_name", "店铺名称");
// 动态店铺属性列（根据公司配置动态追加）
// 如："大区", "区域", "楼层", ...
// 开票信息列（预留）
// titles.put("account_name", "账户名称");
// titles.put("bank_card_no", "银行卡号");
// titles.put("bank_name", "开户行");
```

**动态列处理：** 导出时先查该公司的全部店铺参数定义，作为额外 CSV 列追加到 titles 末尾。

### 9C.2 导出编排 + 异步 Job

参照 `PointMemberLogExportOrchestratorService` + `MemberPointLogsExportFileJobTypeHandler` 模式：

| 文件 | 说明 |
|------|------|
| `PointOrderExportOrchestratorService.java` | 导出编排（同步/队列） |
| `PointOrderExportFileJobTypes.java` | 常量 `TYPE = "point_order_export"` |
| `PointOrderExportFileJobHandler.java` | 异步 Job handler |
| `PointOrderExportFileJobTypeHandler.java` | 实现 `ExportFileJobTypeHandler` |
| `PointOrderExportFileJobPayloadSupport.java` | payload 解析 |

### 9C.3 新增文件清单

| 层 | 文件 | 说明 |
|---|---|---|
| **service/export** | `PointOrderCsvExportService.java` | CSV 导出 |
| **service/export** | `PointOrderExportOrchestratorService.java` | 导出编排 |
| **dispatch** | `PointOrderExportFileJobTypes.java` | 类型常量 |
| **dispatch** | `PointOrderExportFileJobHandler.java` | Job handler |
| **dispatch** | `PointOrderExportFileJobTypeHandler.java` | 类型注册 |
| **dispatch** | `PointOrderExportFileJobPayloadSupport.java` | payload 支持 |

---

## 依赖关系

```
Phase 9 (已完成) → Phase 9A (数据模型+创建) → Phase 9B (Admin API+动态筛选) → Phase 9C (导出)
```

9A 是 9B 和 9C 的前置。9B 和 9C 可以并行开发。

---

## 注意事项

1. **point_batch_consume.order_id 语义：** 存 JF 格式订单号（非 HX）。HX 落在 `point_order.writeoff_no`；`point_member_log.order_id` 仍用 HX。本功能为新增，不存在历史数据兼容问题。
2. **distributor_id 身份（D15）：** 全链路统一为 `distribution_distributor.distributor_id`。核销校验、scope 匹配、订单落库、EAV/`shop_code` 反查均用同一 ID，禁止再混用 Merchant.id。
3. **店铺属性筛选性能：** 单公司店铺数通常百量级；空集按 D17 直接空分页。
4. **退款幂等（D18）：** `UPDATE ... WHERE status=1 AND is_refunded=0`，`affected==0` 即拒绝。
5. **is_settled 预留：** 字段存在但无操作入口，默认 0，后续功能另行开发。
6. **开票信息预留：** 列表/导出预留列，等开票功能上线后反查，无需改 `point_order` 表。
7. **积分汇率（以 D12 / D19 为准）：** 必须开启 `isOpenDeductPoint`，且 `deduct_point` 为正整数；否则核销/可用积分查询直接抛错。**禁止默认成 1。**
8. **单号生成：** `PointOrderNoGenerator.nextNo(userId)` 使用 epoch-day + Redis HASH 模式，每 90s slot 容量 99999，无溢出风险。
9. **Admin 路由：** `/export`、`/filter-attributes` 等静态路径须注册在 `/{id}` 之前，避免被当成 id。
10. **C 端页面展示不在本计划：** 扫码入参只有 `distributor_id`、按店铺展示数据的接口后续另开计划。本计划核销接口（`available` / `consume`）仍要求同时传 `distributor_id` + `activity_id`。
11. **扣减公共核心（D20）：** 排序与「一批次一条流水」线下/线上必须一致；实现时抽 `deductByRule`，返回 `DeductResult` DTO，禁止两套 FIFO。线上抵扣调用改造可另开跟进计划，但不得改写本决策中的排序/流水契约。
12. **核销事务：** `consume` 整笔 DB 写入同一事务；任一步失败全部回滚。Redis 单号不参与回滚。
13. **积分编码（D21）：** `point_order.point_code` 在核销时独立生成，和 `PointBatch.point_code`（批次级）无关。一个订单一个编码。
14. **删除 point_batch 无用字段（D22）：** `point_value`（从未使用）和 `point_code`（write-only，从未读取）一并删除，同时删除 `PointCodeGenerator.java`。
15. **活动编辑同步 scope（D23）：** 活动 scope 字段变更时，三个字段一起同步到活跃批次。修复当前只同步 `scope_distributor_ids` 的 bug。

---

## 关键参照文件

| 用途 | 文件 |
|------|------|
| 核销流程（主改造点） | `ecshopx-point/.../service/activity/PointOfflineConsumeService.java` |
| 单号生成器模式（epoch-day） | `ecshopx-orders/.../service/excard/NormalOrderNumericIdService.java` |
| 单号生成器模式（带前缀） | `ecshopx-deposit/.../service/DepositTradeIdGenerator.java` |
| 核销单号生成器 | `ecshopx-point/.../service/activity/PointWriteoffNoGenerator.java` |
| 积分编码生成器（参照） | `ecshopx-point/.../service/activity/PointCodeGenerator.java` |
| 退款核心逻辑 | `ecshopx-point/.../service/activity/PointBatchRefundService.java` |
| 实体/Mapper 模式 | `ecshopx-point/.../domain/PointBatchConsume.java` + Mapper |
| Admin Controller 模式 | `ecshopx-point/.../api/admin/v1/PointMemberController.java` |
| CSV 导出模式 | `ecshopx-point/.../service/export/PointMemberLogCsvExportService.java` |
| 异步导出 Job | `ecshopx-point/.../dispatch/MemberPointLogsExportFileJob*.java` |
| EAV 属性批量读取 | `ecshopx-common/.../port/distribution/DistributorAttributesBatchReadPort.java` |
| EAV 属性定义 | `ecshopx-distribution/.../domain/DistributorAttributes.java` |
| EAV 属性值 | `ecshopx-distribution/.../domain/DistributorAttributeValues.java` |
| EAV 属性关联 | `ecshopx-distribution/.../domain/DistributorRelAttributes.java` |
| Distributor 实体（shop_code/name） | `ecshopx-distribution/.../domain/Distributor.java` |
| 活动状态计算 | `ecshopx-point/.../service/activity/PointActivityStatus.java` |
| 店铺范围匹配 | `ecshopx-point/.../service/activity/PointBatchScopeMatcher.java` |
| 活动 scope 同步（当前只同步 ids） | `ecshopx-point/.../service/activity/PointActivityScopeService.java` |
| 用户手机号 | `MemberAccountService.getMemberInfo(userId, companyId)` |
| 积分规则（汇率来源） | `PointMemberRuleReadService.getPointRule(companyId)` |

---

## 验证方式

1. **Phase 9A：**
   - 核销必传 `activity_id`；缺省/非法 → 失败
   - 活动未进行中 / 店铺不适用 / 抵扣未开 / `deduct_point` 非法 → 失败
   - 仅扣该活动批次；同活动内按过期时间→剩余积分升序扣减（D20）
   - 跨 N 个批次核销 → N 条 `point_batch_consume` + N 条 `point_member_log`（一批次一条）
   - `point_order.activity_id` 正确；活动名称/scope_type 通过 activity_id 动态读取
   - `point_order.point_code` 独立生成且全局唯一
   - `point_order.distributor_id` = `distribution_distributor.distributor_id`
   - `point_batch_consume.order_id` 为 JF；`point_order.writeoff_no` 为 HX；log.order_id 为 HX
   - API 返回含 `order_no`、`activity_id`
   - `trade_amount` = `trade_point * 100 / point_rate`
   - 事务：扣减后故意制造写 log/建单失败 → 批次余额与 consume 均回滚，无残留半成功数据
   - `point_batch.point_value` 和 `point_batch.point_code` 字段已删除，`PointCodeGenerator` 已移除，不影响现有功能
   - 活动编辑 scope 后，活跃批次的三个 scope 字段同步更新
2. **Phase 9B：**
   - 固定筛选 + 动态属性筛选正确；属性匹配空集 → 空分页
   - 列表含 shop_name/shop_code/attributes
   - 并发双退：仅一笔成功，另一笔「不可退款」
   - 退款后 status=2、is_refunded=1；`journal_type=22`；批次 remain 恢复
3. **Phase 9C：**
   - CSV 含固定列 + 动态属性列（不限数量） + writeoff_no + point_code
   - 金额分→元、时间格式、汇率 "1:N" 正确
