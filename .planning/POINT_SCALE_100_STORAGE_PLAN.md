# 积分支持两位小数（×100 存储）— 实现计划

## 1. 背景与结论

`point_batch_consume.point_consumed` 等积分数字段当前为整型「1 展示积分 = 1 库存」。业务需要最多 **2 位小数**。

对比方案：

| 方案 | 结论 |
|------|------|
| `FLOAT` / `DOUBLE` | 否决：加减/退款比较易误差 |
| `DECIMAL` + `BigDecimal` | 正确但改动面最大（全链路重写运算） |
| **整型 ×100 存储** | **选用**：与 `trade_amount`（分）一致，扣减/退款继续用 `int` |

**结论：发放与扣减统一按「展示积分 × 100」入库；页面 / API 读出再 ÷100 显示。**

### 1.0 本次范围（仅批次体系）— 已定

**只改「已接入 `point_batch` 账本」的功能**；仍走旧 `point_member.point`、或不写 batch 的能力，**不在本次功能改造范围内**。

#### 范围内（写/读 batch 或 batch 关联流水）

| 能力 | 关键服务 |
|------|----------|
| 活动发放 / 通用 Grant | `PointActivityDistributeService`、`PointBatchGrantService` |
| 下单送积分（有活动） | `addPointForNormalOrderBonus` → Grant；定时 `ScheduleSendMemberPoint` / `PointMemberService` |
| 人工**加分**（有活动） | `PointMemberManualAdjustmentService` plus → Grant |
| 注册送积分 | `CreateMemberSuccessRegisterPointExecutionService` → Grant |
| Espier 批次导入 | `PointBatchEspierImportRowSink` → Grant |
| CRM 抵现卡发放 / 券转积分 | `CrmCashCardImportService`、`CrmVoucherToPointImportService` |
| CRM 抵现卡消费 | `CrmCashCardConsumeImportService` |
| 线下核销 / 线上抵扣 | `PointOfflineConsumeService`、`PointOnlineOrderDeductService`、`PointBatchDeductService` |
| 转赠 | `PointTransferService` |
| 取消订单积分返还（批次） | `PointMemberCancelOrderReturnPointsService` → `refundFullOrder` |
| 售后退积分（批次） | `addPointForAftersalesRefund` → `refundByAmount` |
| 线下积分订单退款 | `PointOrderRefundService` → `refundOfflinePointOrder` |
| 核销建单 / 读单 | `PointOrderCreateService`、`PointOrderReadService` |
| 余额查询 | `PointMemberBalanceReadService`（聚合 `remain_point`） |
| 批次相关列表/导出/扫码页 | 流水 enrich（`batch_id`）、核销订单、活动 `total_issued`、线下扫码可用积分等 |

#### 范围外（本次不改功能逻辑）

| 能力 | 说明 |
|------|------|
| UGC 笔记积分 | 只写 member+log，无 batch |
| 储值兑积分 | 只写 member+log |
| 大转盘消耗/中奖、签到送分 | 旧 AddPoint，无 batch |
| 分销结算、自助报名送分 | 旧 AddPoint |
| OpenAPI / 会员导入改分 | 旧 AddPoint |
| 人工**减分**、升值积分扣回 | 旧 member 减分 |
| `addPointForOrderCancelReturn`（如积分商城取消） | 旧返分，**不是** `refundFullOrder` |
| 达摩外呼分支 | 不写本地 batch |
| `point_member.point` | 已废弃，不迁 ×100 |

#### 共享表约束：`point_member_log`

流水表被批次路径与旧路径共用。全表 `income`/`outcome` 做 `*100` 后：

- **范围内**写路径必须 `toStore`，读出 ÷100，保证不报错、账正确；
- **范围外**写 log：**本次不做兼容**；若新流水单位错乱或连带报错，**先不管**（见已确认项）。

`gain_limit` 月度汇总（`journal_type=7` 下单送）属于批次发放，**纳入**本次。

### 1.1 展示层约定（已定 / 待定）

| 项 | 状态 | 说明 |
|----|------|------|
| 接口返回上述积分字段必须 ÷100 | **已定** | API / 列表 / 详情 / 导出读路径统一 `toDisplay` 后再给前端；**前端不再 ÷100** |
| 前端是否还要处理缩放 | **已定：不需要** | 前后端约定：出参已是展示积分；前端只负责展示（及后续待定的取整/格式化） |
| 展示是否取整 | **待定** | 后续再定：原样两位小数 / 去掉尾零 / 四舍五入取整 / 向下取整等；**不影响库内 ×100 存储与账本运算** |
| 取整落点 | 待定后补充 | 优先后端出参格式化或前端展示层；避免在扣减/退款写路径取整丢精度 |

原则：

- **库内 ×100；接口出参 ÷100（展示分）**。
- 前端按展示分直接渲染，**不必再除 100**。
- 取整/去尾零可后期改，不回改库内单位。

---

## 2. 单位约定

| 概念 | 定义 | 示例 |
|------|------|------|
| 展示积分 | API/前端/文案使用的值（÷100 之后） | `1.50`（若后续定取整，页面可能显示 `2` 或 `1`） |
| 库存积分（库内值） | DB / 内部 Service 使用的整型 | `150` |
| 换算（账本） | `store = round(display * 100)`；读出基础值 `display = store / 100` | |
| 换算（页面呈现） | 在 ÷100 之后，按「待定」的取整/格式化策略渲染 | 与账本解耦 |

### 2.1 架构原则

```
前端页面（直接展示 API 返回值；不再 ÷100；是否取整后续定）
        ▲
API 出参（读服务 toDisplay → 业务分）
        │
跨模块 / Controller（传业务分；禁止自行 *100）
        │
门面 Service（入参业务分 → 内部 toStore）
  · PointBatchGrantService / addPointForNormalOrderBonus / AftersalesRefund
  · PointOnlineOrderDeductService / PointOfflineConsumeService
  · PointTransferService
        │
底层账本（入参/运算均为库内分）
  · PointBatchDeductService / PointBatchRefundService / grantBatch
        │
DB（库内分）
```

- 门面以上（含门面入参）：**业务分**；底层账本：**库内分**。  
- 账本加减 **禁止 float**。  
- 比例配置（`point_rate` / `gain_point` / `deduct_point`）仍按业务分语义，不 ×100。  
- 金额分（`trade_amount`）不变。  
- 读出固定 ÷100；取整/去尾零仅作用于展示，不得写回库内。

### 2.2 工具类（建议）

新建 `PointScale`（建议放 `ecshopx-point` 或 `ecshopx-common`）：

```java
/** 业务分（展示积分）↔ 库内积分（×100） */
public final class PointScale {
    public static final int FACTOR = 100;

    /** 业务分 → 库内；最多 2 位小数，超出拒绝或 HALF_UP */
    public static int toStore(BigDecimal display);

    /**
     * 库内 → 业务分（÷100）。
     * 默认 scale=2；是否再取整/去尾零由展示策略决定（待定），可后续加 formatForUi。
     */
    public static BigDecimal toDisplay(int store);

    /** 兼容旧整型入参：整数业务分 → 库内 */
    public static int toStoreFromIntDisplay(int displayPoints);

    // 待定：formatForUi(store) — 取整 / stripTrailingZeros / 固定两位等
}
```

### 2.3 换算边界（防双重 ×100 / ÷100）— 已定

**只允许在门面（或直写库的导入服务）内 `toStore` 一次，禁止调用方再乘。**

| 层级 | 单位 | 职责 |
|------|------|------|
| Controller / CRM·Espier 导入解析 | **业务分** | 出参 `toDisplay`；入参业务分交给门面 |
| 跨模块（orders 支付/收货/售后/取消批次返还） | **业务分** | 直接传业务分；**禁止**调用前 `toStore` |
| 批次发放门面：`PointBatchGrantService`、`addPointForNormalOrderBonus`、`addPointForAftersalesRefund` | **入参 = 业务分** | 内 toStore；售后转 `refundByAmount(库内分)` |
| 核销 / 转赠门面 | **入参 = 业务分** | Online/Offline Deduct、Transfer |
| `PointBatchDeductService` / `PointBatchRefundService` | **库内分** | 仅门面调用 |
| CRM / Espier 直写 batch | 业务分 → 服务内 `toStore` | 只 toStore 一次 |

**已定（批次门面）**：入参业务分；调用方不得再乘。  
**范围外**的 `addPointFor*`（转盘/OpenAPI 等）本次不改；log 单位错乱先不管。

**退款调用约束（已定）：**  
`addPointForAftersalesRefund`（售后门面）收**业务分** → 门面内 `toStore` → 再调 `refundByAmount(…, 库内分)`。  
orders/售后调用方禁止再乘；`refundByAmount` 保持底层库内分，不改为收业务分。

---

## 3. 必须统一 ×100 的字段

| 表 | 字段 | 字段说明（改造后 comment） |
|----|------|---------------------------|
| `point_batch` | `total_point` | 批次总积分（存储值为展示积分×100） |
| `point_batch` | `remain_point` | 剩余可用积分（存储值为展示积分×100） |
| `point_batch_consume` | `point_consumed` | 本批次扣减积分数（存储值为展示积分×100） |
| `point_batch_consume` | `refunded_point` | 已退还积分数（支持部分退款；存储值为展示积分×100） |
| `point_member_log` | `income` | 入账积分（存储值为展示积分×100） |
| `point_member_log` | `outcome` | 出账积分（存储值为展示积分×100） |
| `point_order` | `trade_point` | 交易积分（存储值为展示积分×100） |
| `point_activity` | `total_issued` | 已发放总积分（存储值为展示积分×100） |

实现时同步改 Domain `@MpField(comment=...)` 与 Flyway 列注释。

**共 5 张表、8 个字段**（不含已废弃的 `point_member.point`）。

### 3.0 已废弃：`point_member.point`（不纳入必改 ×100）

| 项 | 说明 |
|----|------|
| 状态 | **已废弃**；可用余额以 `point_batch.remain_point` 实时聚合为准（OD-C / `PointMemberBalanceReadService`） |
| 迁移 | **不必**对历史 `point_member.point` 做 `*100`（非真相源） |
| 读 | 批次相关读路径用 `getPointBalance`；大转盘等残留读点**范围外，先不管** |
| 写 | 旧路径仍可能写该列，**不依赖、不迁移**；停写不在本次 |

### 3.1 不改动的字段

| 字段 | 原因 |
|------|------|
| `point_member.point` | **已废弃**，见 §3.0 |
| `point_order.trade_amount` | 金额（分），已是 ×100 |
| `point_order.point_rate` | 「N **展示**积分抵 1 元」，比例不是余额 |
| 会员规则 `gain_point` / `deduct_point` / `gain_amount` | 配置比例 / 门槛 |
| 各类 `created` / `updated` / `expire_time` | 时间戳 |
| ID、人数 `total_users` 等 | 非积分数 |

### 3.2 订单侧快照（建议同步）

若线上抵扣也要支持小数积分，订单快照可选：

- `normal_orders.point` / `real_use_point`（及行项目相关 point）
- 售后 / 取消路径中的 `refund_point`
- `bonus_points` / `extra_points`

**推荐策略（与 §2.3 一致）**：订单表与订单计算继续用**业务分**；调用加分/核销/退款**门面**时传业务分，由门面 `toStore`。  
**不要**在 orders 调用前自行 `toStore`。若未来订单表改为库内分，须单独立项并改齐读写。

---

## 4. 金额 ↔ 积分换算（必改公式）

### 4.1 线下核销金额

现状（`PointOrderCreateService`）：

```text
amountFen = tradePoint * 100 / pointRate   // tradePoint = 展示积分
```

缩放后 `trade_point` 为库内分：

```text
amountFen = tradePointStore / pointRate
// 等价 (tradePointStore/100)*100/pointRate
```

`point_rate` **不要** ×100，继续表示「N 展示积分抵 1 元」。

### 4.2 订单抵扣金额

`NormalOrderPointDeductionApplyService.phpPointToMoneyFen`：`deduct_point` 仍按业务分比例理解。

- **抵扣金额计算全程用业务分**（与现网一致，公式可少改）。  
- 真正扣批次时走 `PointOnlineOrderDeductService`（业务分入参），由门面 `toStore`，orders **不再**自行 ×100。

### 4.3 CRM 导入

`CrmCashCardImportOptions.toPoints(incomeFen)` 等：得到业务分后，**仅在导入服务写库时** `toStore` 一次；若改为调用 `addPointFor*`，则传业务分、去掉导入侧 `toStore`。

---

## 4.4 限额 / 汇总类（批次相关）

| 流程 | 是否本次 | 注意 |
|------|----------|------|
| 下单送 `gain_limit` 月度 SUM（journal_type=7） | **是** | `PointMemberService`：SUM 后 ÷100 再与规则（业务分）比 |
| 退款结果 `point_refunded` | **是** | 对外/日志需 toDisplay |
| 商户 can_use/total/used（全量 log SUM） | **可读路径 ÷100** | 混入范围外未缩放新流水时统计可能脏，接受 |
| UGC 日限额 | **否** | 范围外；报错先不管 |

---

## 5. 改动范围（按模块）

### 5.1 DB 迁移（Flyway）

1. 字段注释改为「存储值为展示积分×100」。
2. 历史数据一次性放大：

```sql
UPDATE point_batch SET total_point = total_point * 100, remain_point = remain_point * 100;
UPDATE point_batch_consume SET point_consumed = point_consumed * 100, refunded_point = refunded_point * 100;
UPDATE point_member_log SET income = income * 100, outcome = outcome * 100;
UPDATE point_order SET trade_point = trade_point * 100;
UPDATE point_activity SET total_issued = total_issued * 100;
-- 不迁移 point_member.point（已废弃，见 §3.0）
-- 若订单快照也迁：normal_orders.point 等同步 * 100
```

3. 评估 `INT` 溢出：单笔/汇总偏大则改为 `BIGINT`（优先评估 `remain_point`、`total_issued`、log 汇总）。
4. **迁移与发版同窗口**，禁止新旧代码混跑未缩放数据。

### 5.2 Domain / DTO

| 文件 | 改动 |
|------|------|
| `PointBatch.java` | 注释标明 ×100 |
| `PointBatchConsume.java` | 同上 |
| `PointMemberLog.java` | 同上 |
| `PointMember.java` | `point` 注释标明**已废弃**（非 ×100 必改） |
| `PointOrder.java` | `trade_point` 注释 |
| `PointActivity.java` | `total_issued` 注释 |
| `DeductResult.java` | `pointConsumed` 语义为库内分 |

字段类型可保持 `Integer`/`Long`，不引入 `Double`。

### 5.3 发放（增加）— 仅批次路径

| 服务 / 模块 | 说明 |
|-------------|------|
| **`PointBatchGrantService`** | 门面：业务分 → `toStore` → `grantBatch` + log（`batch_id`） |
| `PointActivityDistributeService.grantBatch` | 底层建批次；入参库内分 |
| `addPointForNormalOrderBonus` | → Grant（无活动则跳过，不写旧账） |
| `ScheduleSendMemberPoint` / `PointMemberService` | 算奖（业务分）→ `addPointForNormalOrderBonus`；含 `gain_limit` |
| `ExtraPointActivityOrderBonusService` | 额外积分算奖 → 排程/发放（业务分） |
| `PointMemberManualAdjustmentService`（**仅 plus**） | → Grant |
| `CreateMemberSuccessRegisterPointExecutionService` | → Grant |
| `PointBatchEspierImportRowSink` | → Grant |
| `CrmCashCardImportService` / `CrmVoucherToPointImportService` | 直写 batch+log；`addIssued` delta 用库内分；兼写 member 可忽略 |
| `PointTransferService`（转入） | 新 batch + income |

**本次不改**：`addPointForTurntable*` / Sign / Popularize / Selfservice / OpenAPI / MemberImport / Manual **reduce** / Deposit / UGC / `addPointForMemberRegisterGift`。

### 5.4 扣减 / 退款 — 仅批次路径

| 服务 | 说明 |
|------|------|
| `PointBatchDeductService` | 底层库内分 |
| `PointOfflineConsumeService` / `PointOnlineOrderDeductService` | 门面业务分 → Deduct + log（+ 线下 point_order） |
| `CrmCashCardConsumeImportService` | 扣 batch + consume + order + log |
| `PointBatchRefundService` | `refundFullOrder` / `refundByAmount` / `refundOfflinePointOrder` |
| `PointMemberCancelOrderReturnPointsService` | → `refundFullOrder` |
| `PointOrderRefundService` | → `refundOfflinePointOrder` |
| `addPointForAftersalesRefund` | → `refundByAmount`（门面内 toStore） |
| `PointOrderCreateService` | `trade_point` 库内分 + 金额公式 |
| `PointTransferService`（转出） | `casDeductForTransfer` |
| 支付/下单 | → `PointOnlineOrderDeductService`（业务分） |

**本次不改**：大转盘消耗、`addPointForOrderCancelReturn`、升值积分扣回、旧 member 减分。

Mapper（批次相关）：`incrementRemainPoint` / `subtractRemainIfEnough` / `casDeductForTransfer` / consume 比较；`addIssued`（CRM）。

### 5.5 读出 / 展示 — 批次相关

| 位置 | 处理 |
|------|------|
| `PointMemberBalanceReadService` | 聚合 remain → **÷100** 业务分 |
| 流水列表 / Enrich / CSV（含 `batch_id` 字段） | income/outcome/remain 等 ÷100 |
| `PointOrderReadService` / 核销导出 | `trade_point` ÷100 |
| `PointActivityCreateService` | `total_issued` ÷100 |
| `PointOfflineScanPageService` / 核销查询 | `available_point` 等 ÷100 |
| 转赠 / 核销 / 退款 API 出参 | `point_amount` / `point_deducted` / `point_refunded` ÷100 |
| CRM 导入结果 `issued_points` / `consumed_points` | ÷100 |

**本次不改**：大转盘读 member、UGC、OpenAPI 旧改分、储值兑等；其报错/错账先不管。

文案「当前剩余积分」在**批次写 log** 路径用 toDisplay。

### 5.6 API 契约

| 接口 | 建议 |
|------|------|
| `PointTransferController` | 入参业务分（可小数）→ `PointTransferService` |
| `PointOfflineConsumeController` | 入参业务分 → 核销门面 |
| 会员积分 / 流水 / 批次 / 核销订单查询 | **出参已 ÷100**，前端直接展示 |
| 订单 `real_use_point`（走 OnlineDeduct 时） | 业务分传入门面 |
| OpenAPI 改积分 / 非批次流水协议 | **范围外**（余额查询若走 getPointBalance 则出参 ÷100） |

**默认策略：**

| 方向 | 单位 | 谁处理 |
|------|------|--------|
| 写库 | 库内分（×100） | 后端入参收到展示分后 `toStore` |
| 接口返回 / 导出 | 展示分（÷100） | 后端读出时 `toDisplay` |
| 前端页面 | 展示分 | **直接显示，不再 ÷100** |

**DB 列对应出参（÷100）**：`total_point`、`remain_point`、`point_consumed`、`refunded_point`、`income`、`outcome`、`trade_point`、`total_issued`；余额类出参来自 `getPointBalance` 等聚合（**不要**用废弃的 `point_member.point`）。

**派生 / 聚合出参（同样 ÷100，易漏）**：

| 出参 key（示例） | 来源 |
|------------------|------|
| `available_point` / `total_available_point` | 可用余额聚合 |
| `can_use` / 余额 `point` | `getPointBalance` / log 汇总 |
| `point_deducted` / `point_amount` | 核销、转赠结果 |
| `point_refunded` / `refund_point` | 退款结果 |
| `batch_remain_point` / `expire_point` / `transferable_point` | 流水 enrichment |
| `issued_points` / `consumed_points` | CRM 导入结果汇总 |
| `transfer_point` | 转赠追溯 |

出参 JSON 类型建议：用 `number`（可小数）或字符串；**不要**在已 ÷100 后再用整型截断，否则两位小数丢失。是否固定两位 / 取整见待确认项。

### 5.7 跨模块边界（批次相关）

| 调用方 | 被调 | 注意 |
|--------|------|------|
| 支付 / 下单 PostCommit | `PointOnlineOrderDeductService` | 传业务分 |
| 确认收货 / Finish / 定时送分 | `addPointForNormalOrderBonus` | 传业务分 |
| 取消（走批次返还） | `PointMemberCancelOrderReturnPointsService` | `refundFullOrder` |
| 售后 | `addPointForAftersalesRefund` | 门面内 toStore → `refundByAmount` |
| 订单 bonus/extra 快照 | → 下单送 Grant | 业务分 |

**不在边界内**：`addPointForOrderCancelReturn`、OpenAPI 改分、分销/UGC/转盘等。

### 5.8 测试（批次）

- Deduct / Refund / Transfer / Offline·Online / Grant·Distribute / CRM 导入
- `PointMemberServiceTest`（gain_limit + 下单送）
- orders：OnlineDeduct、售后 `refundByAmount`、CancelReturn（批次）

用例：发 1.50 → 扣 0.50 → 退 0.30 → 余 1.20（库内 150/50/30/120）。

---

## 6. 实施分批（均限定 §1.0 范围内）

> 批次写路径与 Flyway `*100` **同窗口**；范围外功能不改造。

### 第一批 — 批次账本闭环

1. `PointScale` + §2.3（Grant/核销/转赠门面 = 业务分；Deduct/Refund 底层 = 库内分）  
2. Flyway：§3 字段历史 `*100`（**不含** `point_member.point`）  
3. 写：Grant/Distribute、下单送、注册送、人工加、Espier、CRM 发/消、Deduct 门面、Transfer、三类 Refund  
4. `gain_limit` SUM ÷100  
5. 读：`getPointBalance`、核销/转赠/活动/流水（批次相关）÷100；核销金额公式  
6. 批次相关单测（**不做**范围外 log 兼容）  

### 第二批（仍属批次）— 体验

1. 批次相关 API 入参小数  
2. 导出/列表格式  
3. 展示取整（若已定）  

### 不在本次里程碑

UGC / 储值兑 / 转盘签到 / 分销自助 / OpenAPI 改分 / 旧取消返分 / 达摩 / 停写废弃 `point_member.point` 等。

---

## 7. 验收用例

| # | 场景 | 期望 |
|---|------|------|
| 1 | 发放 1.50 | batch.total/remain=150；log.income=150；查询显示 1.50 |
| 2 | 扣减 0.50（单批次） | remain=100；consume.point_consumed=50；outcome=50 |
| 3 | 再退 0.30 | refunded_point=30；remain=130；未满退 is_refunded=0 |
| 4 | 再退 0.20 | refunded_point=50；is_refunded=1 |
| 5 | 跨批次 FIFO | 按 expire/scope/remain 排序，库内分连续扣 |
| 6 | 转赠 0.01 | 转出 outcome=1；转入 income=1、新 batch=1 |
| 7 | 线下核销金额 | 展示 10 分、rate=1 → 仍为 1000 分金额（公式用库内分校正后） |
| 8 | 历史数据 | 迁移前 10 分 → 库内 1000；展示仍为 10.00 |
| 9 | 超过 2 位小数入参 | 拒绝或按约定 HALF_UP，行为文档化 |

---

## 8. 风险与规避

| 风险 | 规避 |
|------|------|
| 漏乘 / 漏除导致账不平 | 写路径统一经 `PointScale`；Code Review 清单对表 |
| **双重 ×100**（调用方 + Service 各乘一次） | 严格执行 §2.3；单测断言入库值 |
| `point_rate` / `deduct_point` 误乘 | 比例字段明确「展示积分语义」；单测锁公式 |
| `point_desc` 拼库内分 | 新写入描述统一 `toDisplay`；历史文案中的数字不回溯改 |
| 迁移与代码不同步 | 同窗口发布；必要时维护窗口禁写 |
| **迁移脚本执行两次** | Flyway 版本号只跑一次；或加哨兵（如 `remain_point` 分位数/配置标记）防重复 `*100` |
| INT 溢出 | 迁移前 `SELECT MAX(field)`，确认 `MAX*100 <= INT_MAX`；必要字段改 BIGINT；`toStore` 溢出要报错 |
| 范围外写 log 单位错 | **接受**（已确认先不管） |
| 流水列表混入范围外未缩放新行 | 展示可能异常，接受；批次路径自身正确即可 |
| 出参整型截断丢小数 | 批次相关出参保留小数能力 |
| 派生字段漏 ÷100 | 按 §5.6 批次相关清单回归 |

---

## 9. 改动文件清单（初稿）

| 区域 | 文件 / 位置 | 操作 |
|------|-------------|------|
| 公共 | `PointScale.java`（新建） | 换算工具 |
| 迁移 | `ecshopx-bootstrap/.../db/migration/V*__point_scale_x100.sql` | 历史 ×100 + 注释 |
| Domain | `PointBatch` / `PointBatchConsume` / `PointMemberLog` / `PointOrder` / `PointActivity` / `DeductResult` | 注释标明 ×100；`PointMember.point` 标注废弃即可 |
| 发放 | Grant / Distribute / NormalOrderBonus / Manual plus / 注册送 / CRM&Espier / Transfer | 门面/导入内 toStore |
| 扣减退款 | Offline/Online Deduct + BatchDeduct/Refund + OrderCreate + AftersalesRefund + CancelReturn | 门面业务分；底层库内分 |
| 读取导出 | BalanceRead / 批次相关 List·Enrich·CSV / OrderRead / OfflineScan | toDisplay |
| API | Transfer / OfflineConsume / 批次相关会员流水与核销 | 出入参业务分 |
| 订单边界 | Payment / PostCommit / Finish / 批次 Cancel·售后 | 传业务分 |
| 测试 | 批次相关 + gain_limit + orders OnlineDeduct/售后/CancelReturn | 防双重缩放 |

---

## 10. 待确认项

1. API 入参：`number`（1.5）还是字符串（`"1.50"`）？超过 2 位是拒绝还是四舍五入？  
2. API 出参类型：JSON number 还是字符串？是否固定两位小数？  
3. 订单快照 `point` / `bonus_points`：保持业务分即可（推荐，本次不改表）  
4. **页面 / 导出显示形态**：是否取整、去尾零、固定两位**后续再定**。  

### 已确认（无需再议）

- **本次只改已实现的批次功能**（§1.0）；旧 member 路径功能不改造。  
- 库内 ×100；批次相关接口出参 ÷100；前端不再除。  
- Grant/核销/转赠门面 = 业务分；Deduct/Refund 底层 = 库内分。  
- **售后退积分**：`addPointForAftersalesRefund` 收业务分 → 门面 `toStore` → `refundByAmount(库内分)`。  
- `point_member.point` 废弃，不迁 ×100。  
- 取整显示后续再定。  
- **验收标准**：§1.0 **范围内**功能正确、不报错即可；范围外写 `point_member_log` **不做 toStore 兼容**，新流水错乱/报错先不管。

---

## 11. 计划审查结论

### 11.1 首轮（2026-08-02）

| 级别 | 结论 |
|------|------|
| 总体 | 方向正确：×100 整型存储 + 出参 ÷100 + 前端不再除 |
| 已吸收 | 防双重缩放；CRM/Espier 并入第一批；派生出参；迁移幂等；AddPoint = 业务分 |

### 11.2 二轮（2026-08-02）

| 级别 | 发现 | 处理 |
|------|------|------|
| 高 | §3.2 / §5.6 / §9 仍写「orders 调用前 toStore」，与 §2.3 冲突 | **已改**：orders 只传业务分 |
| 高 | §2.1 架构图把所有 Service 画成库内分入参，与门面业务分矛盾 | **已改**：区分门面 vs 底层 |
| 中 | `refundByAmount` 若被 orders 直调，单位易错 | **已写入 §2.3 退款约束**，实施时选门面或改公开方法收业务分 |
| 中 | 验收有 `1.50`，但第二批才「API 入参小数」 | **已说明**：第一批 `int` 业务分即可；小数签名第二批；单测可先覆盖 `toStore(1.5)` |
| 中 | `getPointBalance` 被 API 与 orders 共用 | **已写**：统一返回业务分 |
| 低 | §2.2 / §2.3 章节顺序颠倒 | **已理顺** |
| 低 | CRM 直写与走 AddPoint 可能双重 toStore | **已写** §4.3 / §2.3 |

| 项 | 二轮结论 |
|----|----------|
| 总体 | 与 §2.3 对齐后可执行；无新的方向性否决 |
| 待定（不挡第一批） | §10：出参 JSON 类型、取整、外部系统单位、订单表是否改库内分 |
| 开工前建议 | 定退款公开 API 单位；扫 `PointBatchRefundService` / `PointBatchGrantService` 直接调用方 |

### 11.6 范围收窄为「仅批次」（2026-08-02）

对照代码后重新划界（详见 §1.0）：

| 纳入本次 | 移出本次 |
|----------|----------|
| Grant/Distribute、下单送、注册送、人工加、Espier、CRM 发/消 | UGC、储值兑、转盘/签到 |
| Offline/Online Deduct、Transfer | 分销结算、自助报名、OpenAPI/会员导入改分 |
| refundFullOrder / refundByAmount / 线下积分单退 | `addPointForOrderCancelReturn`、人工减分、升值扣回 |
| getPointBalance、核销/转赠/活动/批次流水读 | 大转盘读 member、达摩外呼 |
| `gain_limit`（journal_type=7） | UGC 日限额 |

**共享 log**：全表 `*100` 后范围外新流水可能单位错/报错 — **已确认先不管**；只保证范围内。

### 11.3～11.5 历史扫描（已过时处以下文为准）

早期把 UGC/转盘/储值等也写进「必改」。**以 §1.0 / §11.6 为准**：那些为范围外，不做 toStore、不改读路径。

批次写入口核对（仍有效）：

- Grant 调用方：NormalOrderBonus、Manual plus、Espier、注册送 ✅  
- Refund：Aftersales→`refundByAmount`、CancelReturn→`refundFullOrder`、OrderRefund→offline ✅  
- 无其它生产退款调用方 ✅  
- `casDeductForTransfer` / `addTradeAmountAndPoint` / `addIssued` 均在批次/CRM 内 ✅  

### 11.7 本轮审查（范围收窄后，2026-08-02）

| 级别 | 发现 | 处理 |
|------|------|------|
| 高 | §11.4 仍写 UGC「须 toStore」，与「范围外先不管」冲突 | **已废止该要求**；以 §1.0 为准 |
| 高 | §11.5 要求第一批改大转盘读 member | **已废止**；大转盘范围外 |
| 中 | §2.1 仍画笼统 `PointMemberAddPointService` | **已改**为 Grant/Bonus/Aftersales 等批次门面 |
| 中 | §8/§9/§10 仍掺 OpenAPI、达摩、第三批订单迁表 | **已收束**到批次相关 |
| 中 | 流水列表对全表 ÷100：范围外新流水会显示异常 | **接受**（验收不管） |
| 低 | 达摩开启时 Bonus 不写本地 batch | 现网行为，本次不改达摩 |
| 低 | 验收用例含 1.50，第一批 API 可仍为 int | 单测可覆盖 `toStore(1.5)`；API 小数第二批 |

| 项 | 结论 |
|----|------|
| 总体 | 范围清晰后**可执行**；方向无否决 |
| 验收 | 只卡 §1.0 范围内正确不报错 |
| 开工前 | 售后门面形态**已定**；迁移前做 `MAX*100` 溢出检查即可 |

---

## 12. 参考

- 金额分模式：`point_order.trade_amount`（交易金额分）  
- 相关服务：`PointBatchDeductService`、`PointBatchRefundService`、`PointMemberAddPointService`  
- 既有计划风格：`.planning/POINT_BALANCE_SHOP_SCOPE_PLAN.md` 等
