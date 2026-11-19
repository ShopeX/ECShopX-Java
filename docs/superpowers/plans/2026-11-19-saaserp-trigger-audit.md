# SaasErp 触发点差距表（Task 1）

扫描时间：2026-11-19。对照 `ecshopx-api` PHP `event(new SaasErp*)` 与 Java `*DispatchPublisher.publish`。

状态定义：

- **已对齐**：有等价 publish，且目标 listener/method 语义正确（出站 Port 可仍为空桩）
- **部分对齐**：有 publish，但事件类型/method/时机与 PHP 不一致
- **缺失**：Java 无等价 publish 或无 Update 链路
- **PHP 已废弃 / 明确不做**：本轮不实现

---

## A. `store.trade.add` ← TradeUpdate / `EVENT_TRADE_UPDATE`

| # | PHP | Java | 状态 | 备注 |
|---|---|---|---|---|
| A1 | `AbstractNormalOrder::orderStatusUpdate` L733（支付改状态，非 PART_PAYMENT） | `OrdersTradeFinishNormalOrderPaySuccessApplyService.scheduleSaasErpTradeUpdateAfterCommit` | 已对齐 | AFTER_COMMIT |
| A2 | `AbstractNormalOrder` 发货 L1874 `SaasErpUpdateEvent` | `AdminNormalOrderDeliveryCoreService` → `ThirdPartyTradeUpdateDispatchPublisher` | 已对齐 | 有 probe |
| A3 | `AbstractNormalOrder` 审核同意取消 L3408 | `AdminOrderPassRefundService` → trade update | 已对齐 | 有 probe |
| A4 | 退款改订单状态（RefundByOrder 类） | `RefundByOrderUpdateOrderStatusJobService` | 已对齐 | 有 probe |
| A5 | `OrderService` 自提核销 L2197 | `NormalOrderZitiWriteoffService` → trade update | 已对齐 | 有 probe |
| A6 | 拼团相关 `UpdateGroupsActivityOrder` L133 | `GroupPromotionOrderPayedService` / `NormalGroupsTradeFinishRefundOrchestrator` | 已对齐 | 有 probe |
| A7 | 达达 `DadaCenter/OrderService` L428 | 无 `LocalDelivery*` TradeUpdate publish | **缺失** | 边缘；本轮记入冻结缺口，不阻塞主路径 |
| A8 | 闪送 `ShansongCenter/OrderService` L421 | 同上 | **缺失** | 同 A7 |
| A9 | `ThirdApi/V1/Action/Order` L56 | 未见等价 OpenAPI 主动推 OMS | **缺失** | 边缘；可选 |
| A10 | CLI `SendOrderToSaasOmsCommand` | 无 | **明确不做** | 人工重推，非阻塞 |

Listener：`TradeUpdateSendSaasErp` → `store.trade.add`（**已真推送**）。

---

## B. `store.trade.refund.add`

| # | PHP | Java | 状态 | 备注 |
|---|---|---|---|---|
| B1 | `AbstractNormalOrder` 取消订单 L3027 `SaasErpRefundEvent` | `AdminNormalOrderFullCancelService` → `TradeRefundDispatchPublisher` | 已对齐 | systemlink `EVENT_TRADE_REFUND` 已挂 SaasErp listener；Port 空桩 |
| B2 | `AftersalesService` 仅退款申请（L440/803/3509/3804） | `AftersalesApplyShopApplyByNumHandleService` → `tradeRefundDispatchPublisher`；`AftersalesShopPartialCancelCreateService` | 已对齐 | Port 空桩 |
| B3 | `AbstractNormalOrder` 拒绝退款取消 L3499 `SaasErpRefundCancelEvent` | `AdminOrderPassRefundService` → `ThirdPartyTradeRefundCancelSaasErpDispatchPublisher` | 已对齐 | Cancel Port 假 method；须改为真实 `store.trade.refund.add` |
| B4 | Update/Cancel 的 `ONLY_REFUND` 分支 | Cancel Bus 存在；Update 链路缺失 | **部分对齐 / 缺失** | 见 D |

---

## C. `store.trade.aftersale.add` + `ome.aftersale.status_update`

| # | PHP | Java | 状态 | 备注 |
|---|---|---|---|---|
| C1 | `AftersalesService` 退货退款申请 L437/800/3507/3801 | `AftersalesApplyShopApplyByNumHandleService` → `ThirdPartyTradeAftersalesSaasErpDispatchPublisher` | 已对齐 | Port/组包空桩 |
| C2 | Review/Confirm **通过** 时 PHP 发 **Update**，非 Add | `AftersalesReviewService` / `AftersalesRefundConfirmService` 通过时发 **AftersalesSaasErp（Add）** | **部分对齐** | 事件类型错误；应改 Update（Task 7） |
| C3 | CLI `SendAftersalesToSaasOmsCommand` | 无 | **明确不做** | 人工重推 |

---

## D. `store.trade.aftersale.status.update`

| # | PHP | Java | 状态 | 备注 |
|---|---|---|---|---|
| D1 | `AftersalesService` 审核通过 L1092 `SaasErpAftersalesUpdateEvent` | **无** Update publisher/listener | **缺失** | **必须补** |
| D2 | `AftersalesService` 退款确认通过 L1373 Update | 同上；Java Confirm 误用 Add | **缺失 / 部分对齐** | 同 D1 + C2 |
| D3 | 审核拒绝 → Cancel L1090/1371 | `AftersalesReviewService` / `AftersalesRefundConfirmService` / `AftersalesService` → Cancel publisher | 已对齐（发布） | Port method 名偏差，须对齐 status.update 或 refund.add |

---

## E. `store.trade.aftersale.logistics.update`

| # | PHP | Java | 状态 | 备注 |
|---|---|---|---|---|
| E1 | `AftersalesService` 回寄 L1471/1553 | `AftersalesSendbackService` / `AftersalesFrontWxappSendbackService` → `TradeAftersalesLogiDispatchPublisher` | 已对齐 | Port 空桩 |

---

## F. `store.trade.update`

| # | PHP | Java | 状态 | 备注 |
|---|---|---|---|---|
| F1 | `TradeFinishSendSaasErp`（注释掉） | 无 | **明确不做** | 按计划风险 #1 冻结 |
| F2 | 入站 `UpdateOrderRefund` 回写 | `UpdateOrderRefundSaasErpAct` → `SaasErpStoreTradeUpdatePort` | 已对齐（调用） | Port 空桩，Task 8 真推送 |

---

## G. 共用门禁 / 组包

| # | 项 | 状态 | 备注 |
|---|---|---|---|
| G1 | ERP 绑定 | **部分对齐** | add 已查 Redis；`DefaultSaasErpTradeAftersaleOutboundSupport.isSaasErpOutboundEnabled` **恒 true** |
| G2 | platform + distributor 过滤 | 已对齐 | Matrix client |
| G3 | 售后/退款 body 组包 | **缺失** | `loadOrderAftersalePayloadOrNull` / ome status 恒 null |

---

## 冻结缺口清单（本轮必须做）

1. **G1** 绑定门禁统一（Task 2）
2. **Port companyId 签名**（Task 3）
3. **B 真出站** `store.trade.refund.add`（含 Cancel 改正确 method）（Task 4）
4. **C 真出站** aftersale.add + ome status_update + 组包（Task 5）
5. **E 真出站** logistics.update（Task 6）
6. **D1/D2/C2** 新建 `TradeAftersalesUpdate` 全链路；Review/Confirm **通过** 改发 Update（Task 7）
7. **F2** `store.trade.update` 真出站（Task 8）
8. Publish probe 覆盖已对齐点 + 新 Update（Task 9）

## 本轮明确不做 / 延后

- F1 TradeFinish → update
- A10 / C3 CLI 重推
- A7/A8 达达/闪送回调 TradeUpdate（延后边缘）
- A9 ThirdApi V1 Order 主动推 OMS（延后）

## 未分类行

无。
