# SaasErp Matrix 出站补齐 + 触发点核对 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Java 侧 SaasErp/OMS（Matrix `store.trade.*` / `ome.aftersale.status_update`）出站与 PHP 对等：每个触发点有发布、每个 Port 走真实 `SaasErpMatrixOutboundClient`，并有可核验的 `systemlink_oms_queuelog`。

**Architecture:** 共用已落地的 `SaasErpMatrixOutboundClient`（签名、HTTP、OMS 日志、platform 店铺过滤）。各业务 Port 只负责 method 选择与 body；Bus 负责 ERP 绑定门禁与组包。触发点按「PHP event → Java publisher → listener → method」做矩阵核对，缺链路先补发布，再补出站。

**Tech Stack:** Java 17 / Spring Boot、`SaasErpMatrixOutboundClient`、Dispatch Bus（`ThirdPartyDispatchEventNames`）、Redis 证书/`SaasErpBind`、MySQL `systemlink_oms_queuelog`；对照仓库 `ecshopx-api` PHP Listeners。

**Branch:** 延续 `feature/saas-erp-store-trade-add-outbound`（或拆 `feature/saaserp-matrix-outbound-complete`）。

## Global Constraints

- 范围仅 **SaasErp Matrix 出站**；不含 OME OpenAPI、旺店通、聚水屯、发票 InvoicePushOms。
- Matrix method 名必须与 PHP `Request::call` 一致；禁止自造 `store.trade.refund.cancel` / `store.trade.aftersale.cancel` 作为真实 method。
- ERP 绑定统一用 `SaasCertErpBindReadService.isErpBound`（或委托实现的 `SaasErpCompanySaasBindingReadPort`），禁止售后路径继续恒 `true`。
- 出站成功/失败都写 `systemlink_oms_queuelog`（`api_type=request`）。
- 每个任务先红测再实现；不自动 `git commit` / `push`（除非用户明确要求）。
- OpenWiki 生成页不手改；源码改完后再走维护流程刷新。

---

## 文件与职责（实现期）

| 区域 | 文件 | 职责 |
|---|---|---|
| 共用出站 | `…/saaserp/SaasErpMatrixOutboundClient.java` | 已有；各 Port 复用 |
| 绑定 | `DefaultSaasErpTradeAftersaleOutboundSupport` → 改读 `SaasCertErpBindReadService` | 售后/退款门禁与 add 路径一致 |
| Port 真推送 | `DefaultSaasErpStoreTrade*Port` | 调 Matrix client，修正 method |
| 组包 | 新建/充实售后退款 Struct 适配（orders/aftersales） | 对齐 PHP `OrderService` / `OrderAftersalesService` / `OrderRefundService` |
| Update 链路 | 新建 `TradeAftersaleUpdateSendSaasErp*` + publisher + bootstrap 注册 | 补 PHP `TradeAftersalesUpdateEvent` |
| 触发核对 | orders/aftersales 各 publish 点 + probe 测试 | 防止漏发 |

---

## Phase 0 — 触发点总表（核对用，实施前先勾）

对照原则：PHP `event(new …)` 每一处，在 Java 找等价 `*DispatchPublisher.publish`；再确认 listener 已注册且 method 正确。

### A. `store.trade.add` ← `TradeUpdate` / `EVENT_TRADE_UPDATE`

| PHP 触发点 | Java 对应（现状） | 核对 |
|---|---|---|
| `AbstractNormalOrder::orderStatusUpdate`（支付改 PAYED） | `OrdersTradeFinishNormalOrderPaySuccessApplyService.scheduleSaasErpTradeUpdateAfterCommit` | [x] 已对齐 |
| `AbstractNormalOrder` 发货后 `SaasErpUpdateEvent` | `AdminNormalOrderDeliveryCoreService` → `ThirdPartyTradeUpdateDispatchPublisher` | [x] 已对齐 |
| `AbstractNormalOrder` 审核同意取消 → `SaasErpUpdateEvent` | `AdminOrderPassRefundService` → `ThirdPartyTradeUpdateDispatchPublisher` | [x] 已对齐 |
| `RefundByOrderUpdateOrderStatus` 类取消改状态 | `RefundByOrderUpdateOrderStatusJobService` → trade update publish | [x] 已对齐 |
| `OrderService` / 达达闪送等 `SaasErpUpdateEvent` | 自提核销已对齐；达达/闪送/ThirdApi **缺失（延后）** | [x] 已分类 |
| 入站/人工命令 `SendOrderToSaasOmsCommand` | 可选：管理端重推命令（非阻塞） | [x] 明确不做 |

Listener：`TradeUpdateSendSaasErp` → method **`store.trade.add`**（已真推送）。

### B. `store.trade.refund.add` ← 退款申请 / 退款取消 / 售后仅退款分支

| PHP 触发点 | Java 对应（现状） | 核对 |
|---|---|---|
| `AbstractNormalOrder` 取消订单 → `SaasErpRefundEvent` | `AdminNormalOrderFullCancelService` → `TradeRefundDispatchPublisher`（systemlink `EVENT_TRADE_REFUND`，已挂 `TradeRefundSendSaasErp`） | [x] 已对齐 |
| `AftersalesService` 仅退款申请 → `SaasErpRefundEvent` | `AftersalesApplyShopApplyByNumHandleService` → `tradeRefundDispatchPublisher`；部分路径是否漏 SaasErp 需核对 | [x] 已对齐 |
| `AbstractNormalOrder` 拒绝退款取消 → `SaasErpRefundCancelEvent` | `AdminOrderPassRefundService` → `ThirdPartyTradeRefundCancelSaasErpDispatchPublisher` | [x] 已对齐 |
| `TradeAftersaleUpdate` / `TradeAftersaleCancel` 的 `ONLY_REFUND` 分支 | Update 链路缺失；Cancel 走 aftersale cancel Port（method 应对齐 refund.add） | [x] 缺口冻结 |

Listener：`TradeRefundSendSaasErp` / Cancel Bus → 真实 method **`store.trade.refund.add`**（Port 现为空桩）。

### C. `store.trade.aftersale.add` + `ome.aftersale.status_update`

| PHP 触发点 | Java 对应（现状） | 核对 |
|---|---|---|
| `AftersalesService` 退货退款申请 → `SaasErpAftersalesEvent` | `AftersalesApplyShopApplyByNumHandleService` → `ThirdPartyTradeAftersalesSaasErpDispatchPublisher` | [x] 已对齐 |
| Review/Confirm 再推 aftersales | Review/Confirm **通过误用 Add**（应对 Update） | [x] 部分对齐 |
| CLI `SendAftersalesToSaasOmsCommand` | 可选重推 | [x] 明确不做 |

Listener：`TradeAftersalesSendSaasErp` → add + 条件 status_update（Port/组包现为空桩）。

### D. `store.trade.aftersale.status.update`（及 ONLY_REFUND 时的 refund.add）

| PHP 触发点 | Java 对应（现状） | 核对 |
|---|---|---|
| `AftersalesService` 审核/进度更新 → `SaasErpAftersalesUpdateEvent` | **无** `TradeAftersalesUpdate` publisher/listener | [x] **必须补** |
| `TradeAftersaleCancel` 退货退款撤销 → status.update | `TradeAftersaleCancelSendSaasErpBusService`（Port method 名偏差） | [x] 部分对齐 |

### E. `store.trade.aftersale.logistics.update`

| PHP 触发点 | Java 对应（现状） | 核对 |
|---|---|---|
| `AftersalesService` 回寄物流 → `SaasErpAftersalesLogiEvent` | `AftersalesSendbackService` → `TradeAftersalesLogiDispatchPublisher`（已挂 `TradeAfterLogiSendSaasErp`） | [x] 已对齐 |

### F. `store.trade.update`

| PHP 触发点 | Java 对应（现状） | 核对 |
|---|---|---|
| `TradeFinishSendSaasErp`（TradeFinish 上**已注释**） | 无对等 listener；**明确不做** | [x] 废弃 |
| 入站 `UpdateOrderRefund` 回写订单 | `UpdateOrderRefundSaasErpAct` → `SaasErpStoreTradeUpdatePort`（空桩） | [x] 调用已对齐 |

### G. 共用门禁 / 组包

| 项 | 现状 | 核对 |
|---|---|---|
| ERP 绑定 | add 路径已查 Redis；售后 Port 门禁恒 true | [x] 部分对齐 |
| platform + distributor 过滤 | Matrix client 已实现 | [x] 已对齐 |
| 售后/退款 body 组包 | `loadOrderAftersalePayloadOrNull` / ome status payload 恒 null | [x] 缺失 |

---

## Phase 1 — 触发点审计交付物（先做，再改出站）

### Task 1: 产出「触发点差距表」并冻结缺口清单

**Files:** 本 plan 文档勾选；可选附录 `docs/superpowers/plans/2026-11-19-saaserp-trigger-audit.md`

- [x] 用 ripgrep 扫 PHP：`SaasErpUpdateEvent|SaasErpRefundEvent|SaasErpAftersalesEvent|SaasErpAftersalesCancelEvent|SaasErpAftersalesLogiEvent|SaasErpAftersalesUpdateEvent|SaasErpRefundCancelEvent`
- [x] 用 ripgrep 扫 Java publish：`ThirdPartyTradeUpdate|TradeRefundDispatch|ThirdPartyTradeAftersales|TradeAftersalesLogi|ThirdPartyTradeRefundCancel|ThirdPartyTradeAftersalesCancel`
- [x] 对每一行 PHP 触发：标 **已对齐 / 部分对齐 / 缺失 / PHP 已废弃**
- [x] 冻结必须补的缺失项（至少：`TradeAftersalesUpdate` 全链路）
- [x] 对「部分对齐」点列具体类方法名与是否 AFTER_COMMIT

**验收：** 差距表见 `2026-11-19-saaserp-trigger-audit.md`；F1 `TradeFinish→store.trade.update` **明确不做**。

---

## Phase 2 — 共用层修正

### Task 2: 绑定门禁统一

**Files:**
- `ecshopx-third-party/.../DefaultSaasErpTradeAftersaleOutboundSupport.java`
- 相关 Bus 测试

- [x] 写测：`isSaasErpOutboundEnabled` 在未绑定时为 false
- [x] 实现：委托 `SaasCertErpBindReadService.isErpBound`
- [x] `TradeUpdateSendSaasErpBusService` 可继续直调 bind，或改为同一 Port（二选一，避免双逻辑）
- [x] 跑相关 dispatch consumer 测试

### Task 3: Port 签名统一带 `companyId`

**Files:** 各 `SaasErpStoreTrade*Port` / Default 实现 / 调用方 Bus / 入站 Act

- [x] 与已改的 `callStoreTradeAdd(long companyId, Map)` 对齐：refund/aftersale/update/logistics
- [x] 更新 Mockito verify 签名

---

## Phase 3 — 按 method 补真出站（TDD）

每个 method 同一套路：

1. Port 单测：mock `SaasErpMatrixOutboundClient.call(companyId, METHOD, body)`  
2. Default Port 调 client  
3. Bus 测：绑定关闭不调用；绑定开启且有 body 调用  
4. （可选）client 集成测：假 HTTP / WireMock

### Task 4: `store.trade.refund.add`

- [x] `DefaultSaasErpStoreTradeRefundAddPort` 真推送
- [x] `DefaultSaasErpStoreTradeRefundCancelPort` **改为调用 method=`store.trade.refund.add`**（删掉错误的 refund.cancel 日志语义）
- [x] `TradeRefundSendSaasErpBusService` / Cancel Bus：确认 body 来源（PHP `cancelSaasErpRefund` / refund info）；缺组包则加适配器任务
- [x] 触发点抽测：全额取消、通过退款拒绝 → 有 publish probe（已有则补 assert method）

### Task 5: `store.trade.aftersale.add` + `ome.aftersale.status_update`

- [x] AftersaleAdd Port 真推送两个 method
- [ ] 实现 `loadOrderAftersalePayloadOrNull` / `buildAftersaleOmeStatusUpdatePayloadOrNull`（对齐 PHP `OrderAftersalesService`）— **仍 null；Bus 已用 event entities + ome fallback**
- [x] Bus 行为：status 先置 1 推 add；原 status≠1 再推 ome status_update
- [x] 触发点：店铺申请售后、审核相关 publish

### Task 6: `store.trade.aftersale.logistics.update`

- [x] Logistics Port 真推送
- [ ] 确认 `DefaultSaasErpAftersaleLogisticsStructPort` 组包字段与 PHP 一致（不足则补）
- [x] 触发点：`AftersalesSendbackService` publish → listener

### Task 7: `store.trade.aftersale.status.update` + Update 链路

- [x] 新建 `ThirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher` + event name + bootstrap 注册
- [x] 在 Java 售后审核/进度路径（对照 PHP `AftersalesService` 发 Update 的位置）补 `publish`
- [x] Bus：`ONLY_REFUND` → refund.add；否则 → aftersale.status.update
- [x] Cancel Bus：按售后类型选择 **refund.add** 或 **aftersale.status.update**（修正 Port）

### Task 8: `store.trade.update`

- [x] 按 Phase 0 决策：若仅入站需要 → 只改 `DefaultSaasErpStoreTradeUpdatePort`
- [x] 若恢复支付完成 update → 另加 TradeFinish listener（默认不做，除非差距表要求）

---

## Phase 4 — 触发点回归探针

### Task 9: 每个「已对齐」触发点至少一条 publish probe

优先复用现有 `*PublishProbeTest` / `*DispatchConsumerRuntimeTest`：

- [ ] 支付成功 → EVENT_TRADE_UPDATE
- [ ] 发货 → EVENT_TRADE_UPDATE
- [ ] 同意取消/退款拒绝 → update 与/或 refund cancel
- [ ] 仅退款申请 → EVENT_TRADE_REFUND（SaasErp listener）
- [ ] 退货退款申请 → EVENT_TRADE_AFTERSALES_SAAS_ERP
- [ ] 售后取消 → EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP
- [ ] 回寄物流 → EVENT_TRADE_AFTERSALES_LOGI
- [ ] 售后状态更新 → 新 Update event（Task 7 后）

**验收：** probe 断言 publish 次数与 payload 关键字段（`company_id`/`order_id`/`aftersales_bn`/`refund_bn`）。

### Task 10: 本地手工冒烟清单

- [ ] company 绑定 ERP（`SaasErpBind` + prism cert）
- [ ] 支付一单 → queuelog `store.trade.add`
- [ ] 仅退款申请 → `store.trade.refund.add`
- [ ] 退货退款申请 → `store.trade.aftersale.add`（及条件 status_update）
- [ ] 回寄物流 → `store.trade.aftersale.logistics.update`
- [ ] 售后取消/审核更新 → 对应 method
- [ ] 未绑定商户 → 无 request 日志、无 HTTP

---

## Phase 5 — 收尾

### Task 11: 自检与摘要

- [ ] 跑 `ecshopx-third-party` / `ecshopx-aftersales` / `ecshopx-orders` 相关单测子集
- [ ] 差距表全部勾完或标明「明确不做」
- [ ] 输出实现摘要；提示 `$shopex-git-commit`（若有 TBID）

---

## 风险与决策点

1. **`TradeFinishSendSaasErp`（store.trade.update）PHP 已注释** — 默认不恢复，除非业务确认仍要支付后再 update。
2. **Cancel/Update 复用 `store.trade.refund.add`** — 以 PHP body 字段为准，不能只改 method 名。
3. **组包复杂度** — 售后 Struct 可能跨 aftersales/orders；优先在 aftersales 模块实现 Port，third-party 只调接口。
4. **达达/闪送等边缘 TradeUpdate** — Phase 0 扫到再排期，勿阻塞主路径。

---

## 建议执行顺序（压缩）

1. Task 1 触发点差距表（含产品确认）  
2. Task 2–3 共用门禁与签名  
3. Task 4 → 5 → 6 → 7 → 8 出站  
4. Task 9–10 探针 + 冒烟  
5. Task 11 收尾  

---

## 完成定义

- Matrix method 清单 7 项中：`add` 已完成；其余除明确废弃外均真推送。  
- Phase 0 表无未分类触发点；已知缺口（Update 链路）已补或书面放弃。  
- 绑定关闭时所有 SaasErp 出站静默跳过。  
- 相关单测绿；本地至少一条 add + 一条 aftersale/refund 的 queuelog 证据。
