# Phase OD-A 实现计划 — 线上积分抵扣接入公共扣减

## 改造概要

把两条线上扣减路径从 `addPointForManualAdjustment(false)` 切换到 `PointBatchDeductService.deductByRule(...)`，并按批次逐条写 `point_member_log`（journal_type=6）。

---

## Step 1：新建线上抵扣入口方法

**文件：** `ecshopx-point/.../service/activity/PointOnlineOrderDeductService.java`（新建）

**职责：**
1. 校验 `pointToDeduct > 0`、`distributorId > 0`、`orderId` 非空
2. 调用 `pointBatchDeductService.deductByRule(companyId, userId, pointToDeduct, distributorId, orderId, null, null)`
3. 遍历 `deductResult.getConsumeRecords()` 逐条写 `point_member_log`（模式同 `PointOfflineConsumeService.writeLogsByBatch`）
4. 返回扣减结果

**关键字段：**
- `journal_type = 6`（消费购物支出）
- `point_desc = "订单" + orderId + "抵扣积分"`
- `outcome = record.getPointConsumed()`
- `order_id = orderId`（商城订单号）
- `batch_id = record.getBatchId()`
- `activity_id = record.getActivityId()`
- `distributor_id = distributorId`

**事务：** 不加独立 @Transactional，由调用方事务包裹（REQUIRED 传播到 deductByRule 内部的 @Transactional）。

---

## Step 2：改造 OrderCreatePostCommitPortImpl

**文件：** `ecshopx-orders/.../OrderCreatePostCommitPortImpl.java`

**改动：**
1. 构造器注入新增 `PointOnlineOrderDeductService`（替代当前的 `PointMemberAddPointService`）
2. 把积分扣减代码从行77-80移到行82（`res = p.getOrdersInsertResult()`）之后，这样可以拿到 orderId
3. 替换调用：

```java
// Before:
pointMemberAddPointService.addPointForManualAdjustment(
    userId, companyId, pointUse, false, "订单抵扣积分", distributorId);

// After:
String orderIdStr = String.valueOf(res.get("order_id"));
pointOnlineOrderDeductService.deductForOrder(
    companyId, userId, pointUse, distributorId, orderIdStr);
```

**注意：** 移动积分扣减位置不影响语义 — 方法名 `runAfterOrderInsert` 表明订单已插入，`p.getOrdersInsertResult()` 此时已有值。扣减仍在同一事务内，失败仍会回滚整笔订单。

---

## Step 3：改造 OrdersPaymentDoPaymentService

**文件：** `ecshopx-orders/.../OrdersPaymentDoPaymentService.java`

**改动：**
1. 注入 `PointOnlineOrderDeductService`
2. 替换 `pay_type=point` 分支中的扣减调用：

```java
// Before:
pointMemberAddPointService.addPointForManualAdjustment(
    userId, companyId, point, false, "订单" + orderId + "支付扣减积分", distributorId);

// After:
pointOnlineOrderDeductService.deductForOrder(
    companyId, userId, point, distributorId, orderId);
```

**注意：** 此处 `orderId` 已有（从 `data.get("order_id")` 取得），无需额外处理。事务由 `transactionTemplate.execute()` 控制，deductByRule 的 REQUIRED 会加入该事务。

---

## Step 4：移除 PointMemberAddPointService 的订单扣减引用（可选）

如果改造后 `OrderCreatePostCommitPortImpl` 不再需要 `PointMemberAddPointService`，可从构造器移除该依赖。需检查该类是否还有其他用途引用。如果其他方法仍需要，保留注入。

---

## 文件清单

| 操作 | 文件 |
|------|------|
| 新建 | `ecshopx-point/src/main/java/cn/shopex/ecshopx/point/service/activity/PointOnlineOrderDeductService.java` |
| 修改 | `ecshopx-orders/.../OrderCreatePostCommitPortImpl.java` |
| 修改 | `ecshopx-orders/.../OrdersPaymentDoPaymentService.java` |

---

## 不做的事

- 不改 `deductByRule` 本身
- 不改事务边界
- 不创建 `point_order`
- 不动退款/取消（那是 OD-B）
- 不动结算页可用积分（那是 OD-C）
