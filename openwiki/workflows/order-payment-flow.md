---
type: workflow
title: "Workflow: Checkout, Payment, Notify, and Trade-Finish Fan-out"
description: End-to-end trace of the money path — cart → order creation → payment initiation (wxapp member flow and admin 代客下单 deferred flow) → channel async notify (WeChat and Alipay facades) → trade state CAS → trade-finish event fan-out (pay-success bridge, notifications, statistics, ERP pushes, profit sharing) — plus the deposit recharge variant.
tags: [payment, trade, notify, wxpay, alipay, dispatch-bus, trade-finish, fan-out, orders, deposit, sms, statistics, jushuitan, wdterp, profit-sharing, shopadmin, deferred-order, pos, micropay, refund]
sources:
  - id: openwiki-source-ea70eb6c045047448e446296
    resource: repo://.gitignore
  - id: openwiki-source-7050bfe0cde098993d6b2bce
    resource: repo://ecshopx-adapay/src/main/java/cn/shopex/ecshopx/adapay/service/callback/handler/AdapayCallbackPaymentHandler.java
  - id: openwiki-source-fd94d375300df90e9a5bdcb1
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/OrdersTradeFinishDispatchPublisherBeansConfig.java
  - id: openwiki-source-cc0af63e7b93bd7afc7164f3
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/TradeFinishSmsNotifyDispatchListenerRegistrationConfig.java
  - id: openwiki-source-89b43138f8e75edb4615495b
    resource: repo://ecshopx-bootstrap/src/main/resources/application.properties
  - id: openwiki-source-423c7bde5a386038c2869dcc
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/deposit/service/AdapayDepositTradeRechargeCallbackServiceIdempotencyTest.java
  - id: openwiki-source-48403aa24e1185d0a351e2e3
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/dispatch/WxappPaymentTradeFinishSmsNotifySyncDispatchFlowTest.java
  - id: openwiki-source-e3f608a3254de204f775497c
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersTradeFinishDispatchPublisher.java
  - id: openwiki-source-68b9a0e3c776f0fe8beabd26
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/payment/PaymentSubjectDistributorIdPort.java
  - id: openwiki-source-c9890b49121b99230c30246e
    resource: repo://ecshopx-deposit/src/main/java/cn/shopex/ecshopx/deposit/integration/alipay/AlipayNotifyDepositSidePortImpl.java
  - id: openwiki-source-f70b6d04e8550513ce6b3aa6
    resource: repo://ecshopx-deposit/src/main/java/cn/shopex/ecshopx/deposit/service/AdapayDepositTradeRechargeCallbackService.java
  - id: openwiki-source-fb3cf06a693439057652555e
    resource: repo://ecshopx-deposit/src/main/java/cn/shopex/ecshopx/deposit/service/DepositTradeWxappPaymentService.java
  - id: openwiki-source-dc6e550003128c844e8ece4d
    resource: repo://ecshopx-deposit/src/main/java/cn/shopex/ecshopx/deposit/service/DepositWxpayRechargeUnifyService.java
  - id: openwiki-source-d1961192a688961d2d90ccf4
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/integration/PaymentSubjectDistributorIdPortImpl.java
  - id: openwiki-source-0a1caaa71538649542913a46
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/admin/v1/NormalOrderController.java
  - id: openwiki-source-f2e1c65d6d39cb3db1a8a7e8
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/WxappPaymentController.java
  - id: openwiki-source-48ad2faabde51a9f74198a8b
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/dispatch/OrdersTradeFinishAfterCommitDispatchPublisher.java
  - id: openwiki-source-2578fa97e7cd4e074c6dff42
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/dispatch/OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener.java
  - id: openwiki-source-0f82b454b43d90e72507a58c
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/alipay/AlipayNotifyOrderSidePortImpl.java
  - id: openwiki-source-48c58afd50c9934ddd5eb49b
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappPaymentService.java
  - id: openwiki-source-1e59ed1d09620110f69b2b0a
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/create/OrderCreatePersistencePortImpl.java
  - id: openwiki-source-a252c563ca705b5943957b38
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminPaymentQueryService.java
  - id: openwiki-source-5a2ec1c085dae82424db4467
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminPaymentService.java
  - id: openwiki-source-7243b2e27f3844d47ea37bac
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminDeferredOrderCache.java
  - id: openwiki-source-d18042d85f451076cf2b7d72
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminDeferredOrderService.java
  - id: openwiki-source-6aff19aa34047820a63c0f48
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminDeferredOrderStatus.java
  - id: openwiki-source-c9da549abb50240724e0a2e2
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateOrchestrator.java
  - id: openwiki-source-6bee7c44879ca7bd01cdede7
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateTransactionalRunner.java
  - id: openwiki-source-541592d1df9edc4c8c641347
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/notify/TradeFinishSmsNotifyBusService.java
  - id: openwiki-source-623ecfb6b4a37473cdb9935b
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/payment/OrdersExternalPayParamBuildService.java
  - id: openwiki-source-7832b5e57d5d960a02d053c2
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentChannelQueryService.java
  - id: openwiki-source-11504f9b8ac58d0bd088b826
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentDoPaymentService.java
  - id: openwiki-source-2a5af3193c4467af90799bc6
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentTradeQueryService.java
  - id: openwiki-source-ff50bf9d82f8a9d1db0a8c21
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/payment/OrdersTradePaymentCallbackService.java
  - id: openwiki-source-26dda26957015219704de19d
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/sales/UpdateItemSalesBusService.java
  - id: openwiki-source-393459e0e5faa669429f6481
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/tradefinish/OrdersTradeFinishNormalOrderPaySuccessApplyService.java
  - id: openwiki-source-ab420cef928add1ab3268149
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/supplier/service/SupplierOrderSplitOnNormalOrderAddService.java
  - id: openwiki-source-42b59755b5fcb0f2909b92e8
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest.java
  - id: openwiki-source-1dadfec169f4df087b2904a0
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentDoPaymentServicePaymentSubjectTest.java
  - id: openwiki-source-1e0309e2b800473aab8b0e9e
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest.java
  - id: openwiki-source-8c69e7dfe69ee3ac9984cd76
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/api/admin/v1/PaymentNotifyController.java
  - id: openwiki-source-1ccbca38ff13b6c8b4a6239c
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/api/front/v1/PaymentController.java
  - id: openwiki-source-4c7a3bc700c378ccd65c4327
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/api/open/WxpayNotifyController.java
  - id: openwiki-source-64fa255fa9b2b100903e0ba4
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/AlipayAsyncNotifyVerificationService.java
  - id: openwiki-source-aaad8326cee5c8f000796994
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/AlipayH5SyncReturnService.java
  - id: openwiki-source-fc41d8fb1ed3fb46beacafec
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/AlipayNotifyFacade.java
  - id: openwiki-source-44c42192a41742b2edb67e6b
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/AlipayNotifyPaymentContextLoader.java
  - id: openwiki-source-aa7828341fef670e794e9d62
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/AlipayPaymentConfigValidationService.java
  - id: openwiki-source-4ac568ccd29169fa2fd1ca52
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/front/FrontPaymentSettingReadService.java
  - id: openwiki-source-a03bf98f742e0148fbe2281e
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/orderrefund/AlipayTradeAftersalesRefundRunner.java
  - id: openwiki-source-9e4ae9aa6e977090eae5591e
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/orderrefund/WxpaySecapiAftersalesRefundRunner.java
  - id: openwiki-source-b6f21c41b3fac2e74d0a8460
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/settings/AlipayPaymentSettingWriter.java
  - id: openwiki-source-f1d910dfde8ea50422ca49b0
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/WxpayNotifyFacade.java
  - id: openwiki-source-84f8cd25ebcbd75c25e4b431
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/service/WxpayPaymentConfigValidationService.java
  - id: openwiki-source-c95fb5887e03d45919d723b1
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/support/PaymentSettingRedisKeys.java
  - id: openwiki-source-04d8f988a1b772b617717322
    resource: repo://ecshopx-payment/src/test/java/cn/shopex/ecshopx/payment/service/AlipayNotifyFacadeDepositSmsDispatchTest.java
generated: { by: "openwiki/0.9.0", at: "2026-11-09T03:34:48.355Z" }
verified:
  - by: openwiki/0.9.0
    at: 2026-11-09T03:34:48.355Z
---

# Workflow: Checkout, Payment, Notify, and Trade-Finish Fan-out

This page traces one payment from end to end: the customer's cart is emptied
into a new order, the order is paid through `POST /api/v1/h5app/wxapp/payment`,
the channel calls back asynchronously onto `POST /wechatAuth/wxpay/notify`
(WeChat) or `POST /api/v1/alipay/notify` (Alipay), the trade row is flipped
from `NOTPAY` to `SUCCESS`, and the resulting trade-finish event fans out to a
dozen listeners (order pay-success, supplier promotion, notifications,
statistics, ERP pushes, profit sharing). The deposit-recharge variant routes
through a different port and ends in the deposit balance instead.

There is a second entry to the same money path: the **admin 代客下单 deferred
flow** (staff creating an order for a walk-in customer), where `POST /api/v1/order/create`
writes only a Redis `WAIT_PAY` snapshot and the order is materialized in the
same call as the charge (`POST /api/v1/order/payment`). It is documented in its
own section below — the trade CAS, the notify surface and the trade-finish
fan-out it lands on are the ones already described here.

The bundles involved are **ecshopx-orders** (order + trade state), **ecshopx-payment** (channel config + notify facades), **ecshopx-deposit** (recharge), **ecshopx-dispatch** (the bus), and **ecshopx-bootstrap** (all the wiring: publisher beans and listener registrations). See [Orders Domain](../domains/orders.md) for the state model, [Payments Domain](../domains/payments.md) for the channel surfaces, and [Dispatch Bus](../concepts/dispatch-bus.md) for the transport.

```mermaid
sequenceDiagram
    participant CL as Client
    participant CT as Controller
    participant PS as PaymentService
    participant CH as Channel
    participant NF as NotifyFacade
    participant CB as CallbackService
    participant DB as DispatchBus
    participant LN as Listeners
    participant AO as Admin operator
    participant DF as ShopadminDeferredOrderService
    participant RC as Redis snapshot
    participant DP as OrdersPaymentDoPaymentService

    rect rgb(235, 245, 255)
    note over CL,LN: wxapp immediate-pay path — order row exists before payment
    CL->>CT: POST /api/v1/h5app/wxapp/payment
    CT->>PS: payment with order_id and pay_type
    PS->>PS: order guards, order bundle, pay_type forcing
    PS->>DP: doPayment
    DP->>DP: TX1 ensure NOTPAY trade, local types finalize inside
    DP->>CH: channel SDK call outside any DB transaction
    CH-->>DP: prepay_id or code_url or mweb_url
    DP->>DP: TX2 persist payment_params, finalize when pay_status
    DP-->>CL: client pay params plus trade_info
    CL->>CH: user confirms payment
    CH->>NF: POST /wechatAuth/wxpay/notify XML
    NF->>NF: parse XML, resolve company and apiKey
    NF->>NF: MD5 verify sign
    alt attach contains depositRecharge
        NF->>CB: AlipayNotifyDepositSidePort rechargeCallback
    else order trade
        NF->>CB: AlipayNotifyOrderSidePort applyTradePaymentAfterAlipay
    end
    NF-->>CH: XML SUCCESS even on already-handled business errors
    CB->>CB: store inital_response, NOTPAY guard, CAS to status
    CB->>DB: publish trade row on three planes
    DB->>LN: Jushuitan plane, trade finish plane, WdtErp plane
    LN->>LN: pay-success bridge, SMS, WXA template, statistics, profit
    end

    rect rgb(255, 245, 235)
    note over AO,DP: admin shopadmin deferred path — order materialized after the charge
    AO->>CT: POST /api/v1/order/create
    CT->>RC: saveWaitPay WAIT_PAY snapshot with auto-cancel TTL
    CT-->>AO: preassigned order_id, order_status NOTPAY, no DB rows
    AO->>CT: POST /api/v1/order/payment with auth_code or pay_type
    CT->>DF: payDeferredOrder, no orders_associations row but cache hit
    DF->>RC: withLock shopadmin deferred lock order_id
    DF->>DF: resolve pay type, only pos or wxpaypos or alipaypos
    DF->>DP: doPayment
    DP->>DP: TX1 insertPendingTrade NOTPAY, pos finalizes locally
    DP->>CH: micropay or alipay.trade.pay outside the transaction
    CH-->>DP: synchronous confirmation, pay_status true
    DP->>DP: TX2 finalizeTradeSuccess, publish registration
    DP->>DB: trade-finish publish on TX2 commit, order row not yet written
    DB->>LN: pay-success bridge finds no order row and returns empty
    DF->>DF: persistPreparedSnapshot then direct markPayedIfNeeded to DONE
    DF->>DB: supplier split then single normal_order_add publish
    DF->>RC: delete snapshot
    DF-->>AO: pay_status true with order_id
    end
```

*Two entries into the same money path: the wxapp immediate-pay flow (top — the order row exists before payment, so the async notify drives everything) and the admin 代客下单 deferred flow (bottom — create only caches a Redis snapshot for pos/wxpaypos/alipaypos, and `ShopadminDeferredOrderService` charges and materializes the order in one locked call, so the trade-finish publish fires before any order row exists). Every step is grounded in the classes named in the sections below.*

## Step 1 — cart → order creation

Payment presupposes an order. `POST /order` (`WxappOrderController`) runs the
wxapp create pipeline (type registry → orchestrator → transactional runner),
and inside the same transaction `OrderCreatePostCommitPortImpl.runAfterOrderInsert`
empties the cart after persisting the order: `orders_cart` rows are deleted by
`(company_id, user_id, shop_id, item_id)` for the purchased items,
`fastbuy` carts are cleared through a short-lived Redis key, employee-purchase
and shopadmin use their own cart ports, and community/excard orders skip
cart clearing entirely. The new `NormalOrders` row starts
`order_status=NOTPAY`, and a `supplier_order` child is split per supplier in
`SupplierOrderSplitOnNormalOrderAddService.split` right after the transaction.
The full pipeline is documented on [Orders Domain](../domains/orders.md); the
cart-emptying behaviour is what makes this page's entrypoint reachable.

## Step 2 — `POST /api/v1/h5app/wxapp/payment`

`WxappPaymentController` (in `ecshopx-orders/api/front/v1`, mapped to
`/api/v1/h5app/wxapp`, `@FrontNoAuth` at the class level — only
`/payment/config` and `/payment_deposit` carry a per-method `@FrontAuth`)
exposes three surfaces:

| endpoint | method | purpose |
| --- | --- | --- |
| `/payment` | POST | pay an existing order (the main money path) |
| `/payment/config` | GET | QR-scan surface; with `order_id` pays an order, without one builds a store-consumption trade (`trade_source_type=order_pay`) |
| `/payment/query` | POST | poll the trade; falls back to an active channel query |
| `/payment_deposit` | POST | legacy stub returning an empty 200 |

The controller merges query-string params with the (optional) flexible body,
resolves the wxapp/H5 auth attribute map from the request, and logs the merged
params and the pay result as JSON. `company_id` is mandatory — it is taken from
the auth claims, the H5 company id attribute, or the merged params, in that
order, and `UnauthorizedException` ("无权访问该API,非法访问！") is thrown when
none is present. A falsy `trade_id` (null/0/blank/`"0"`/empty collection) on
`/payment/query` is rejected as "支付单号不存在" before the service is called.
`splitLogisticsItems` post-processes the pay result: items with
`is_logistics = true/1` are moved out of `order_info.items` into
`order_info.logistics_items`.

`WxappPaymentService.payment` is the guard layer:

* `order_id` is mandatory ("订单号不存在"); an `order_id` whose first two
  code points are `CZ` (case-insensitive) is a **deposit recharge**, and the
  whole call is handed to `DepositTradeWxappPaymentService.depositPayment`
  instead — see the deposit variant below.
* `pay_type` is mandatory unless an `auth_code` (scan-to-pay barcode) is
  present; `auth_code` prefixes decide the derived pay type (`1x…17 digits` →
  `wxpaypos`, `25…30` prefix → `alipaypos`), otherwise it defaults to
  `wxpayh5`. `adapay` additionally requires `pay_channel`.
* The order must exist in `orders_associations` for the company
  ("当前订单不需要支付"), must not be `order_type=supplier_order`
  ("暂不支持该订单类型的支付"), and its `order_status` must be `NOTPAY` or
  `PART_PAYMENT`.
* The effective order type (from `OrderAssociationEffectiveTypeService`) must
  be payment-bundle supported: `normal`, `normal_shopadmin`, `service`,
  `service_*`, `bargain`, `normal_bargain` or any `normal_*` except
  `membercard` and `supplier_order`.
* `updatePayTypeIfApplicable` writes the chosen `pay_type` (and non-empty
  `pay_channel`) onto `orders_normal_orders` before the trade is created.
* A zero cash total with `point > 0` forces `pay_type=point`; prescription
  orders must pass `OrderPrescriptionPaymentGuardService`; `deposit` pay
  requires a logged-in member card, `alipaymini` requires the alipay user id.

With the guards passed, `buildPaymentData` assembles the trade payload
(`company_id`, `order_id`, `pay_type`, `trade_source_type` derived as
`order_type + '_' + order_class` for PHP parity, user/shop/distributor ids,
`pay_fee` = `cny_fee` falling back to `total_fee` or the point amount for
pure-point payments, currency metadata, `auto_cancel_time`, `client_ip`), and
`OrdersPaymentDoPaymentService.doPayment` is invoked.

## Step 3 — trade creation and channel unify

`OrdersPaymentDoPaymentService.doPayment` is the shared core. Before anything
runs it resolves the **payment subject**: `PaymentSubjectDistributorIdPort.resolveActualDistributorId(companyId, distributorId)`
(implemented by `PaymentSubjectDistributorIdPortImpl` in the distribution
bundle, declared in `ecshopx-common` so orders and payment can both depend on
it) returns the order's distributor id **only when that distributor's
`payment_subject = 1`** — any other value, a missing distributor row, or
`distributor_id = 0` all resolve to `0` (the platform merchant). The resolved
id decides *whose merchant config is used*: it is passed to
`assertPayConfigReady` (which validates the `(d?)wxPaymentSetting:` /
`(d?)alipayPaymentSetting:` Redis config for that distributor) and onward to
`OrdersExternalPayParamBuildService.buildPayParamsOrInvoke` and
`OrdersPaymentChannelQueryService`, so a distributor running its own WeChat/
Alipay merchant account charges and queries against its own credentials while
everyone else falls back to the platform config. The trade row itself still
carries the order's real `distributor_id` (see
[Stores and Merchants](../domains/stores-and-merchants.md) for the
`payment_subject` flag). The front-end payment-type list
(`FrontPaymentSettingReadService`) resolves the same port, so the cashier only
offers channels the effective subject actually has configured.

After subject resolution, `doPayment` runs as a **three-phase transaction
split** — no channel SDK is ever called with a database transaction open:

1. **Pre-channel (no transaction)** — `assertPayConfigReady` validates the
   channel config for the resolved subject before anything is persisted
   (wxpay/alipay validation services, hfpay port, bspay
   `bspaySetting:{sha1(companyId)}`, adapay `adaPaySetting:`); an
   unconfigured wxpay/alipay channel fails with a subject-specific message —
   "店铺微信支付配置不完整，请先完成配置后再切换收款主体" /
   "平台微信支付配置不完整，请先完成配置后再切换收款主体" (and the alipay
   twins) depending on whether the resolved subject is a distributor or the
   platform — while an unavailable hfpay/adapay channel still fails with
   "不支持支付服务，请联系商家", in every case before an orphan trade can be
   created. For `bspay`/`adapay` the supplier sub-orders are
   attached to the payload and `findExistingWithPaymentParams` returns a
   previously stored `payment_params` JSON instead of re-unifying.
2. **TX1 — `preparePaymentInTx`** (one `transactionTemplate` execution) —
   `findOpenTradeWithoutParams` reuses an open `NOTPAY` trade matching
   `(company, order, pay_type, pay_fee, trade_source_type[, pay_channel])`;
   otherwise `insertPendingTrade` inserts one with `trade_state=NOTPAY`, an
   encrypted mobile (`SensitiveFieldEncryptor`), dealer/merchant ids resolved
   from the distributor, `trade_id` built from
   `NormalOrderNumericIdService.generate` prefixed by the distributor shop
   code, and currency conversion for wxpay/alipay/hfpay (converted
   `pay_fee`/`total_fee`, originals preserved in the `cur_*` columns). The
   **local-payment branches finalize inside this same transaction**: zero-fee
   non-prescription orders finalize as `localPay`; `point` deducts member
   points *unless* `trade_source_type=normal_pointsmall` (pointsmall already
   deducted at create, pinned against double-deduction); `prepaid_point`,
   `pos` and `deposit` (through `DepositTradeConsumeService.consume`) all
   finalize here too. Each ends in `finalizeTradeSuccess` — the internal twin
   of the notify path below — and returns `pay_status=true` without ever
   touching the channel (one transaction total). The external types
   (`bspay`, `adapay`, `wxpay*`, `alipay*`, `hfpay`, `offline_pay`,
   `paypal`, `doumen_intl`) instead return the pending trade, and any other
   pay type throws "无此类型支付".
3. **Outside any transaction — `invokeExternalChannel`** — the channel SDK
   calls run with no DB transaction open, so a slow gateway cannot hold row
   locks or exhaust the connection pool: `bspay`/`adapay` call the membercard
   SDK services; `wxpay*`/`alipay*`/`hfpay`/`offline_pay`/`paypal`/
   `doumen_intl` delegate to
   `OrdersExternalPayParamBuildService.buildPayParamsOrInvoke(payType, companyId,
   distributorIdForSetting, authInfo, data, trade)`.
4. **TX2 — `persistExternalPaymentInTx`** (a second `transactionTemplate`
   execution) — persists the channel outcome: for bspay the
   `bspay_req_date`/`inital_request`/`transaction_id` triple, for adapay
   `inital_request`/`transaction_id`, and for every channel the full client
   param map as the `payment_params` JSON (which is what the adapay/bspay
   reuse in step 1 reads back). When the client map carries
   `pay_status=true` — the synchronous micropay/barcode confirmations —
   `finalizeTradeSuccess` runs here, so the trade-finish publish is
   registered on TX2's synchronization and fires when TX2 commits.

```mermaid
sequenceDiagram
    participant SVC as OrdersPaymentDoPaymentService
    participant PORT as PaymentSubjectDistributorIdPort
    participant TX1 as TX1 preparePaymentInTx
    participant CH as Channel SDKs
    participant TX2 as TX2 persistExternalPaymentInTx
    participant BUS as OrdersTradeFinishDispatchPublisher

    SVC->>PORT: resolveActualDistributorId companyId and distributorId
    PORT-->>SVC: distributor id when payment_subject 1 else 0 platform
    SVC->>SVC: assertPayConfigReady with resolved subject
    SVC->>TX1: transactionTemplate execute
    TX1->>TX1: reuse or insert NOTPAY trade
    alt local payment point or deposit or pos or zero fee
        TX1->>TX1: finalizeTradeSuccess inside TX1
        TX1->>BUS: publish registered on TX1 commit
        TX1-->>SVC: pay_status true
    else external channel types
        TX1-->>SVC: pending trade
        SVC->>CH: invokeExternalChannel no DB transaction open
        CH-->>SVC: client params or synchronous confirmation
        SVC->>TX2: transactionTemplate execute
        TX2->>TX2: persist bspay or adapay outcome and payment_params JSON
        opt channel already confirmed pay_status true
            TX2->>TX2: finalizeTradeSuccess inside TX2
            TX2->>BUS: publish registered on TX2 commit
        end
        TX2-->>SVC: client pay params plus trade_info
    end
```

*The three-phase split inside `doPayment`: payment-subject resolution and the
config gate run first, TX1 ensures the `NOTPAY` trade and completes all local
payment types in one transaction, the channel SDK is invoked with no
transaction open, and TX2 persists the channel outcome (`inital_request`,
`transaction_id`, `payment_params`) and finalizes the trade when the channel
confirmed synchronously.*

For the wxpay family, `wxPayClientParams` loads the per-distributor config
from Redis, builds the outer `attach`
`company_id=<enc>&pay_type=<enc>` (URL-encoded), maps the pay type to a
`trade_type` (`wxpayh5`→MWEB, `wxpayapp`→APP, `wxpaypc`→NATIVE, default JSAPI),
and calls the WeChat unified-order endpoint (servicer mode uses
`servicer_app_id`/`sub_mch_id`, plain mode `appid`/`mch_id`/`openid`). The
response is shaped per trade type:
`mweb_url`, `code_url`, APP sign bundle, or the JSAPI
`appId/timeStamp/nonceStr/package/signType/paySign` set. `wxpaypos`
(micropay) is synchronous and returns `pay_status=true` plus
`transaction_id`, which makes the caller finalize the trade immediately.

The `notify_url` is the deployment property
`ecshopx.payment.wechat.notify-url`, whose **shipped default is the local
placeholder `http://127.0.0.1/wechatAuth/wxpay/notify`** in
`application.properties` (the comment above it in that file reads
微信支付 v2 统一下单异步通知完整 URL（需公网可达；默认仅本地占位）— the WeChat v2
unified-order async-notify URL must be publicly reachable; the default is a
local placeholder only). A blank value fails the unify call outright with
"不支持支付服务，请联系商家" (the guard sits before the request is built), so a
real deployment always overrides it — per environment, via env vars or
`--key=value` program arguments, because the only in-repo value is the
placeholder. `application-local.properties` is **git-ignored** (`.gitignore`
line 7) and not shipped, so it is not a configuration source the repo provides
(see [Configuration](../operations/configuration.md) for the override
mechanics; the alipay channel reads
`ecshopx.payment.alipay.notify-url`, default
`http://localhost:8080/api/v1/alipay/notify`, the same way). The gateway will
POST back to whatever base URL the deployment declares.

The alipay family is the mirror image at the charge step. `alipayh5` builds an
`alipay.trade.wap.pay` form (`product_code=QUICK_WAP_WAY`) carrying
`passback_params = urlenc("company_id=<id>&pay_type=alipayh5")` plus the
configured `alipayNotifyUrl` and the caller's `return_url`; `alipaymini` and
`alipayapp` go through `alipay.trade.app.pay` (`sdkAppPayOrderString`, an
`orderString` returned to the client); and `alipaypos` is the synchronous
`alipay.trade.pay` barcode call (`FACE_TO_FACE_PAYMENT` by auth code), whose
synchronous confirmation finalizes the trade without any notify. The
asynchronous alipay surfaces that complete the asynchronous branches are step
4a below.

## Step 4 — the WeChat async notify

`POST /wechatAuth/wxpay/notify` (`WxpayNotifyController`, `ecshopx-payment`,
`@DingoResponse(PLAIN)`, consumes XML/text/all, produces XML) is exempt from
operator JWT auth by the `/wechatAuth/` prefix exemption, so every request
must prove itself cryptographically. `WxpayNotifyFacade.handle`:

1. **Load and resolve** — `WxpayNotifyPaymentContextLoader.load` reads the
   raw body, parses it as XML with `WxpayNotifyXmlSupport.tryParseNotifyXml`
   (secure processing, DOCTYPE disallowed; an unparseable body yields
   `Optional.empty()` → immediate `FAIL`/`invalid notify`), then resolves
   `companyId`: first `WxpayNotifyCompanyIdPort.resolveCompanyIdByAuthorizerAppId`
   on the notify's `appid`, falling back to the Redis appId→companyId maps
   (`wechatAppPayment:companyId:{appId}` for `trade_type=APP`, then
   `wechatPayment:`, then `wechatServicerPayment:`). Zero company →
   "微信支付信息未配置，请联系商家".
2. **Distributor for settings** — `AlipayNotifyOrderSidePort.resolveDistributorIdForAlipayRedis`
   (orders' `AlipayNotifyOrderSidePortImpl`) loads the trade, and returns its
   distributor id only when the distributor's `payment_subject = 1` — the
   notify-side twin of the `PaymentSubjectDistributorIdPort` resolution the
   charge path runs in step 3 (same flag, same fall-back to `0`/platform; this
   one re-derives the subject from the trade row because the notify carries no
   distributor context). The notify then reads
   `(distributorId?)wxPaymentSetting:sha1(companyId)` from the companys Redis
   template and takes the `key` as the API key. Despite the "Alipay" name this
   is the shared entry both facades reuse — the port inversion keeps
   `ecshopx-payment` free of an orders dependency.
3. **Verify** — `WxpayAsyncNotifyVerificationService.verifySignedNotify`
   recomputes MD5 over the sorted non-empty params (skipping `sign`)
   concatenated with `&key=<apiKey>` and compares case-insensitively.
   A missing sign, missing key, or mismatch throws
   `BadRequestException` ("无效的微信支付通知" / "微信支付验签失败").
4. **Classify** — `result_code=SUCCESS` maps to status `SUCCESS`; otherwise
   the notify's `trade_state` is used (defaulting to `PAYERROR`). The
   URL-decoded `attach` (via `WxpayNotifyAttachParser.parseAttach`) is the
   router: a `depositRecharge` value sends the call to the deposit side,
   everything else to the order side. The `pay_type` is re-read from the
   attach, not the notify body, because the trade was created with the
   request-time pay type (e.g. `wxpayh5`) while the gateway only knows its own
   trade type.
5. **Dispatch** — the order side is
   `AlipayNotifyOrderSidePort.applyTradePaymentAfterAlipay(outTradeNo, status, options)`
   (a one-line delegate to
   `OrdersTradePaymentCallbackService.applyTradePaymentNotify`); the deposit
   side is `AlipayNotifyDepositSidePort.rechargeCallback`.

### Failure handling: swallow-and-ack

The facade wraps the port call in `catch (BadRequestException |
ResourceException)`, logs at debug ("wxpay notify business already handled
out_trade_no=…"), and **still returns XML `SUCCESS`**. The already-processed
guard ("更新已处理，不需要更新"), the unknown-trade error ("未查询到更新数据")
and every other business exception therefore ack the gateway so it stops
retrying — a replayed notify is harmless. Only an *unexpected* exception
(signature failure, unreadable body, missing config, serialization failure)
falls into the outer `catch (Exception e)`, which logs at error and returns
`<xml><return_code>FAIL</return_code><return_msg><![CDATA[…]]></return_msg></xml>`
(HTTP 200 with a FAIL body — the code never returns a non-200 status, the
return code is the signal). `cdataSafe` neutralizes `]]>` inside the message.

## Step 4a — the Alipay async notify and H5 sync return

`POST /api/v1/alipay/notify` (`PaymentNotifyController`, `ecshopx-payment`'s
`api/admin/v1`, `name = 支付宝异步通知`) is the Alipay twin of step 4, handled by
`AlipayNotifyFacade.handle`. The shape is the same as wxpay — load, prove
authenticity, classify, dispatch to the same two ports — but the mechanism
differs at every layer:

1. **Load and resolve** — `AlipayNotifyPaymentContextLoader.load` flattens the
   form parameters, **requires `passback_params`** (its absence yields
   `Optional.empty()` and the facade answers `{"data":[]}`), transcodes values
   from the declared `charset` (default GB2312) to UTF-8, and URL-decodes the
   passback query string. The passback carries `company_id` and `pay_type`
   (written at charge time by `OrdersExternalPayParamBuildService`); a missing
   or malformed `company_id` throws "缺少企业编号" / "企业编号格式错误".
2. **Ownership cross-check, before any verification** — the facade resolves
   the *owning* company from the database row named by `out_trade_no`:
   `AlipayNotifyOrderSidePort.requireCompanyIdByTradeId` for order trades,
   `AlipayNotifyDepositSidePort.requireCompanyIdByDepositTradeId` when the
   decoded passback has `attach=depositRecharge`. A mismatch with the
   passback's `company_id` throws "订单企业与回调企业不一致" /
   "储值单企业与回调企业不一致", so a replayed or forged passback cannot
   redirect the callback onto another tenant's trade (pinned by
   `AlipayNotifyFacadeDepositSmsDispatchTest`'s
   `depositRecharge_passbackCompanyMismatch_rejectsWithoutRechargeOrVerify`,
   which also verifies the signature check never runs).
3. **Config gate and signing material** —
   `AlipayPaymentConfigValidationService.assertAsyncNotifyConfigComplete`
   requires `app_id` + `ali_public_key` + `private_key` in
   `(distributorId?)alipayPaymentSetting:{sha1(companyId)}` (companys Redis),
   throwing "支付宝信息未配置，请联系商家" otherwise; `loadSigningMaterial`
   then re-reads the same key and refuses a blank `app_id`. The distributor
   override is resolved through the same
   `resolveDistributorIdForAlipayRedis` port (orders' impl) as the wxpay
   facade uses.
4. **Verify** — `AlipayAsyncNotifyVerificationService.verifySignedNotify`
   runs the Alipay SDK `AlipaySignature.rsaCheckV1` over the flattened params
   with the configured `ali_public_key`, honouring the notify's `sign_type`
   (default RSA2); empty params, a missing `sign`, a missing public key, or a
   mismatch each throw `BadRequestException` ("支付宝通知参数无效" /
   "支付宝通知缺少签名字段" / "支付宝公钥未配置" / "支付宝异步通知验签失败").
5. **Classify and dispatch** — `trade_status` `TRADE_SUCCESS` or
   `TRADE_FINISHED` normalizes to `SUCCESS` (anything else passes through
   verbatim); `options` carries `pay_type` re-read from the passback and
   `transaction_id` from the notify's `trade_no`. The deposit branch calls
   `AlipayNotifyDepositSidePort.rechargeCallback`, the order branch
   `applyTradePaymentAfterAlipay` — the exact same ports, and therefore the
   same `OrdersTradePaymentCallbackService` CAS and trade-finish fan-out, as
   the wxpay facade.

The ack contract is the inverse of the wxpay facade's: a successful dispatch
answers the plain-text body `success` (HTTP 200); **any** exception — a
forged notify, a mismatched company, the deposit already-handled
`ResourceException`, an unknown trade — is caught by the outer
`catch (Exception)`, logged at error with the `out_trade_no`, and answered
with a 200 response whose **body is empty**, which Alipay treats as a failure
and retries. There is no swallow-and-ack for business exceptions here: a
replayed deposit recharge acks empty and Alipay keeps retrying until its own
retry budget expires. Unparseable requests (`Optional.empty()`) answer
`{"data":[]}`.

The **H5 sync return** `GET /payment/alipay/result` (`PaymentController`'s
`alipayResult`, `name = 支付宝结果页`) is the polling-flavoured twin, handled by
`AlipayH5SyncReturnService`: it requires the front H5 session company id
(`UnauthorizedException` otherwise), verifies the return URL's signature the
same way, then **queries the channel**
(`AlipayOpenapiTradeQueryService.queryTrade` → `alipay.trade.query`) and
trusts the channel's answer, not the browser's params. It re-parses the
`passback_params` returned by the query, enforces the same ownership check
(session company ↔ trade/deposit owner, plus passback company ↔ owner for
deposits), and on `SUCCESS` dispatches to the same two ports — so a lost or
delayed alipay async notify is repaired by the customer's own browser
landing on the result page. This is the alipay analogue of `/payment/query`'s
`finalizeTradeSuccessAfterChannelQuery`.

## Step 5 — `OrdersTradePaymentCallbackService.applyTradePaymentNotify`

This is the single mutation point for a channel-confirmed trade:

1. A blank `out_trade_no` throws `BadRequestException` ("回调数据无效").
2. The trade is loaded by `trade_id`; missing → `ResourceException`
   ("未查询到更新数据").
3. The **entire notify map is persisted** into `trade.inital_response` as
   JSON (a serialization failure throws "回调数据无法序列化") — this happens
   *before* the state guard, so even a replayed notify refreshes the audit
   trail.
4. The trade is re-read and the **NOTPAY idempotency guard** applies: any
   `trade_state` other than `NOTPAY` throws
   `ResourceException`("更新已处理，不需要更新"). This is what the facade
   swallows on a replay.
5. The guarded UPDATE sets `trade_state = status`, `pay_type`,
   `pay_channel`, `bank_type`, `coupon_fee`, `coupon_info`, and
   `transaction_id` when present. On `SUCCESS` it also stamps `time_expire`
   (unix seconds) and assigns `trade_no` from the Redis daily sequence
   (`h_trade_no_{company}_{distributor}_{MMdd}` hash keyed by order id,
   `c_trade_no_*` counter, 86400s TTL — `MMdd-seq`, e.g. `0507-12`).
   Zero affected rows throws "交易单状态更新失败".
6. Only `SUCCESS` continues to `publishSystemLinkTradeFinishAfterSuccess`:
   `maybeFulfillVipGradeMembercardSale` (the membercard VIP port, PHP-parity
   with `UpdateOrderStatusListener`), then the trade row is converted to a
   snake_case map and published on **three planes**: `jushuitanTradeFinishDispatchPublisher`,
   `ordersTradeFinishDispatchPublisher`, `wdtErpTradeFinishDispatchPublisher`.

Note that non-`SUCCESS` statuses (`REFUND`, `CLOSED`, `REVOKED`, `PAYERROR`)
*are written* to `trade_state` but publish nothing — the fan-out is a
pay-success-only affair on this path.

The sibling internal path `OrdersPaymentDoPaymentService.finalizeTradeSuccess`
(point/prepaid_point/deposit/pos/localPay inside TX1, synchronous channel
confirmations inside TX2, and `/payment/query`'s
`finalizeTradeSuccessAfterChannelQuery`) performs the same CAS
(`WHERE trade_state='NOTPAY'`), the same `trade_no` assignment, the same
membercard fulfilment, and the same trade-finish publish; when the trade is
already `SUCCESS` it *re-publishes* idempotently rather than failing. This is
how a zero-fee or point-only order reaches the same listener fan-out without
ever touching the gateway.

The publish contract on the internal path: every `finalizeTradeSuccess`
converts the full `Trade` row to a snake_case map
(`SNAKE_ROW` Jackson mapper) and hands it to
`OrdersTradeFinishDispatchPublisher.publish` — the `ecshopx-common` interface
whose orders implementation, `OrdersTradeFinishAfterCommitDispatchPublisher`,
registers a `TransactionSynchronization` and publishes **after commit** when a
transaction is active (immediately otherwise). On the `doPayment` path that
means the event fires when the enclosing TX1 (local payments) or TX2
(synchronous channel confirmations) commits.
`finalizeTradeSuccessAfterChannelQuery(trade, transactionId, channelResult)`
is the channel-query-confirmed twin used by `/payment/query` and the admin
deferred query leg: same NOTPAY→SUCCESS CAS, optionally persisting the
channel-reported `transaction_id`, same publish — the one difference is that
a trade already in a non-NOTPAY, non-SUCCESS state throws
`BadRequestException`("更新已处理，不需要更新") instead of silently returning.

## Step 6 — trade-finish publication and the three planes

`OrdersTradeFinishDispatchPublisherBeansConfig` (bootstrap) exposes two beans
of the same `OrdersTradeFinishDispatchPublisher` interface, both implemented
by `OrdersTradeFinishAfterCommitDispatchPublisher`, which registers a
`TransactionSynchronization` and publishes **after commit** when a
transaction is active (immediately otherwise):

* `@Primary ordersTradeFinishDispatchPublisher` → `EVENT_TRADE_FINISH`
  (`event:291:OrdersBundle\Events\TradeFinishEvent`) — the gateway-notify
  plane.
* `ordersTradeFinishDispatchPublisherOfflineCsv292` (qualified) →
  `EVENT_TRADE_FINISH_CSV292` (`event:292:…`) — the admin offline
  bank-transfer approval plane (`OfflinePaymentDoCheckService`).

Both planes use `DispatchOptions.eventDefaults()` = **SYNC**, so on the
notify path every registered listener runs in-process, in the
`@Order(n)` sequence of its registration config, once the notify transaction
commits. The other two planes are ASYNC+REDIS:

* `JushuitanTradeFinishDispatchPublisherImpl` →
  `event:systemlink:jushuitan_trade_finish` →
  `listener:systemlink.trade_finish_send_jushuitan` (asyncDefaults).
* `WdtErpTradeFinishDispatchPublisherImpl` →
  `event:systemlink:wdt_erp_trade_finish` →
  `listener:systemlink.trade_finish_send_wdt_erp` (asyncDefaults).

Both use `RetryPolicy.platformDefault()` (3 attempts / 3s) so the payment
callback never blocks on a slow ERP. The dedicated per-ERP events exist so
each ERP can evolve its payload and retry policy independently; OME, the
marketing centre and DmCrm ride the shared `event:291` plane instead.
Saas-ERP is *not* directly fed from this notify path — its
`event:thirdparty:trade_update` event is published by the delivery, cancel,
write-off and groups-team-success services (see the groups listener in step
7), not by `OrdersTradePaymentCallbackService`.

## Step 7 — the listener fan-out on `event:291`/`event:292`

Every registration config in `ecshopx-bootstrap/config` registers its
listener on **both** planes with the same name and options. In `@Order`
sequence:

| @Order | listener name | mode | what it does |
| --- | --- | --- | --- |
| 76 | `listener:HfPayBundle\Listeners\HfpayTradeRecordListener@paySuccess` | sync | hfpay ledger `paySuccess` |
| 77 | `listener:orders.trade_finish_normal_order_pay_success_bridge` | sync | **the pay-success bridge** (below) |
| 78 | `listener:orders.listeners.TradeFinishSmsNotify` | async `sms` | pay-success SMS |
| 79 | `listener:orders.listeners.TradeFinishNotifyPush` | async | payment websocket push |
| 81 | `listener:orders.listeners.TradeFinishWxaTemplateMsg` | async `default` | WXA subscribe template message |
| 82 | `listener:thirdparty.dm_crm.trade_finish` | async | DmCrm point change + order sync |
| 83 | `listener:thirdparty.marketing_center.trade_finish_basics_order_pay` | async | marketing centre `basics.order.pay` |
| 84 | `listener:orders.trade_finish_help_pay_bargain_plus_one` | async | bargain help-pay plus one |
| 85 | `listener:kaquan.trade_finish_consume_card_pay_bill` | async | coupon card consumption |
| 86 | `listener:orders.listeners.UpdateItemSalesListener` | async `default` | item sales count increment |
| 87 | `listener:orders.listeners.UpdateGroupsActivityOrder` | sync | groups activity order update (`normal_groups` only) |
| 88 | `listener:orders.listeners.TradeFinishCountBrokerage` | async `default` | brokerage counting (skips `point`) |
| 89 | `listener:orders.listeners.TradeFinishLinkMember` | async `default` | member link |
| 90 | `listener:orders.listeners.TradePayFinishStatistics` | async `slow` | pay-finish statistics |
| 91 | `listener:orders.listeners.TradeFinishProfit` | sync | profit sharing |
| 92 | `listener:systemlink.trade_finish_send_ome` | async `default` | OME `ome.order.add` push |
| 93 | `listener:orders.listeners.PrinterOrder` | sync | printer order |
| 94 | `listener:orders.listeners.TradeFinishFapiao` | async `slow` | fapiao create |
| 95 | `listener:orders.listeners.TradeFinishCustomDeclareOrder` | sync | WeChat customs declaration |
| 96 | `listener:orders.listeners.TradeFinishWorkWechatNotify` | sync | work-wechat notify |
| 97 | `listener:orders.listeners.TradeFinishPaymentSuccessOrderProcessLog` | sync | the "订单支付" OPL |
| 98 | `listener:orders.trade_finish_offline_pay_canceled_normal_refund` | sync | offline-pay cancel refund |

Although the parent publish is SYNC, listeners registered with
`ListenerDispatchOptions.async(...)` still run inline on this plane — the
options only matter on ASYNC-published planes (where the fan-out planner
expands one event into one queued message per listener); the queue names
(`default`, `slow`, `sms`) in the table document the queue each listener
*would* use on an async plane.

### The pay-success bridge (order 77)

`OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener.onEvent` →
`OrdersTradeFinishNormalOrderPaySuccessApplyService.markPayedIfNeeded` (one
transaction, `@Transactional(rollbackFor = Exception.class)`):

* Only `trade_state=SUCCESS` payloads with a positive `company_id`/`order_id`
  whose `trade_source_type` is in `NORMAL_TRADE_SOURCE_TYPES` (`normal`,
  `normal_normal`, `normal_groups`, `normal_seckill`, `normal_community`,
  `bargain`, `normal_shopguide`, `normal_pointsmall`,
  `normal_employee_purchase`) are considered.
* Target status: `WAIT_GROUPS_SUCCESS` for `normal_groups`; `DONE` for
  `shopadmin`-class orders **unless** the order uses platform item stock
  (`uses_platform_item_stock != 0`, the 云仓 immediate-purchase case, which
  stays `PAYED` awaiting shipment — PHP parity, per the code comment);
  otherwise `PAYED`. The header update is guarded by
  `order_status = 'NOTPAY'` and sets `pay_status = PAYED` — zero affected rows
  means someone else already paid or cancelled, and the method returns
  `Optional.empty()` so **no pay-success event is published** (the bridge is
  itself idempotent).
* The same status is mirrored onto `orders_associations`, and for
  `PAYED`/`DONE` the `supplier_order` children are promoted from
  `NOTPAY`/`PART_PAYMENT` with `pay_status=PAYED` — this is the
  **supplier split promotion** that makes the split created at order time
  payable downstream.
* Shopadmin/shopscreen self-pickup (`ziti`) orders auto-complete: non-logistics
  item lines are marked delivered with an auto-close-aftersales time derived
  from the platform `latest_aftersale_time` setting, `ziti_status=DONE`, and
  pure non-logistics orders jump straight to `DONE` with
  `delivery_status=DONE`.
* On success the bridge publishes
  `NormalOrderPaySuccessDispatchPublisher` →
  `event:288:OrdersBundle\Events\NormalOrderPaySuccessEvent`
  (ASYNC+REDIS, `RetryPolicy.platformDefault()`), whose own listeners are
  youshu (`listener:youshu.orders_normal_order_pay_success`),
  wsugc (`listener:WsugcBundle\Listeners\Order`),
  shuyun open-platform trade-sync and offline-benefit-consume, and
  employee-purchase behaviour-log — all async `default`.

### The notification listeners and their gates

* **SMS (order 78)** — `TradeFinishSmsNotifyBusService.handleTradeFinishRow`
  skips `point`/`deposit` pay types, requires a resolvable company, a
  non-blank passport uid (`CompanyPassportUidByCompanyIdPort`) and a
  non-blank mobile, formats `pay_money` as yuan (`pay_fee/100`) and
  `pay_time` from `time_start`, and calls
  `TradePaySuccessTemplatedSmsPort.sendTradePaySuccess`. Send failures are
  caught and logged at debug. Operational caveat: the mobile arrives
  straight off the trade row, which `insertPendingTrade` stores through
  `SensitiveFieldEncryptor.encrypt` (AES-CBC when
  `common.encrypt-sensitive-data=true`, otherwise plaintext), and neither
  the bus service nor the bootstrap adapter (`TradePaySuccessTemplatedSmsPortAdapter`
  → `SmsSendTestService.sendTemplatedNoticeSms`) decrypts it — with
  encryption enabled the SMS is therefore sent to the ciphertext, not a
  phone number.
* **WebSocket push (order 79)** —
  `TradeFinishNotifyPushBusService.handlePaymentNotifyTradeFinishRow` skips
  `point`/`deposit` and any `pay_fee <= 0`, then sends a camelCase envelope
  (`payFee` as a BigDecimal in yuan, `payType`, `shopId`, `payDate`) through
  `PaymentMsgNotifyWebSocketSendPort.sendPaymentNotify`; port failures are
  caught and logged.
* **WXA template (order 81)** —
  `TradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable`
  skips `membercard` trades, only proceeds for `wxpay`/`deposit` pay types
  (returning early before the subscribe message for `deposit`), and for a
  found normal order publishes the waiting-delivery notice job (non-`ziti`
  receipt with a distributor) plus the WXA subscribe template through
  `TradeFinishPaymentSuccWxaSubscribeSendPort` with shop name resolution from
  `TradeFinishPaymentSuccShopNamePort`.
* **Statistics (order 90)** — `TradePayFinishStatisticsDispatchListener`
  requires `trade_state=SUCCESS` and an eligible `trade_source_type` before
  calling `TradePayFinishStatisticsBusService.recordPayFinishStatistics`
  (queue `slow`).
* **Brokerage (order 88)** — `TradeFinishCountBrokerageDispatchListener`
  skips blank/non-positive `order_id` and `point` pay type.
* **Item sales (order 86)** — `UpdateItemSalesBusService` keys only on
  `company_id`/`order_id`; it does **not** skip `point` — the contrast with
  the SMS/brokerage listeners is deliberate and pinned by the
  `WxappPayment*` tests.
* **Groups (order 87)** —
  `UpdateGroupsActivityOrderBusService.handleTradeFinishRow` runs only for
  `trade_source_type=normal_groups` with `trade_state=SUCCESS`. It first
  tries `NormalGroupsTradeFinishRefundOrchestrator` (refund a disabled
  member in an already-formed team and publish the trade-update event); only
  when that branch returns neither `REFUND_PUBLISHED` nor `REFUND_SKIPPED`
  does it call `NormalGroupsTradeFinishOnPayPort.onTradePaySuccess`, whose
  promotions implementation enables the team member, increments the join
  count, and — when the team reaches success — marks every member's
  `normal_groups` order `PAYED` and publishes the Saas-ERP trade-update and
  OME trade-update events per order
  (`GroupPromotionOrderPayedService.markNormalGroupOrderPayedAndPublishErpSync`).
  This is the path that actually feeds Saas-ERP from a payment.
* **OPL (order 97)** —
  `OrdersTradeFinishPaymentSuccessOrderProcessLogDispatchListener` writes
  the "订单支付" order process log (operator_type=system, is_show=true,
  detail `订单号：<order_id>，订单支付成功`) only for `trade_state=SUCCESS`.
* **Profit sharing (order 91)** — `TradeFinishProfitDispatchListener`
  requires positive `order_id`, `company_id` and `user_id` before delegating
  to `TradeFinishProfitBusService.handleTradeFinishRow`.

## The deposit recharge variant

When the wxapp `order_id` starts with `CZ`,
`WxappPaymentService.payment` short-circuits to
`DepositTradeWxappPaymentService.depositPayment`, which validates the
`deposit_trade` row (company match, `trade_status=NOTPAY`, positive money)
and — in the current code, **for wxpay channels only** (`wxpay*` prefixes;
anything else is rejected with "无此类型支付！") — calls
`DepositWxpayRechargeUnifyService.unifyAndBuildClientPayParams`, its own
unified-order client with a **different attach**:
`company_id=<enc>&pay_type=<enc>&attach=depositRecharge`. The alipay
`depositRecharge` route on the notify side is therefore reachable only for
deposit trades created outside the wxapp front surface (e.g. by an adapay or
admin recharge flow) — the facade, the H5 sync return and the adapay callback
handler all still branch on it.

On the notify side the same `WxpayNotifyFacade` runs, but the decoded
`attach` contains `depositRecharge`, so the call is routed to
`AlipayNotifyDepositSidePort.rechargeCallback` → deposit's
`AlipayNotifyDepositSidePortImpl` → `AdapayDepositTradeRechargeCallbackService.rechargeCallback`
(the alipay facade and the H5 sync return reach the same port method through
their own `attach=depositRecharge` classification, after their ownership
cross-checks):

* A blank or unknown `deposit_trade_id` throws
  `ResourceException`("储值单不存在或主键无效，无法完成回调").
* Only `status=SUCCESS` proceeds (any other status returns without touching
  the row — there is no failure state for a deposit trade).
* An already-`SUCCESS` deposit trade throws
  `ResourceException`("更新已处理，不需要更新") — the same already-handled
  exception the wxpay facade swallows, so a replayed recharge notify also acks.
* The status flip is a **guarded conditional update** (ECX-10915 — atomic
  takeover, aligned with PHP's `FOR UPDATE` + lock-then-check, preventing
  concurrent double crediting): the UPDATE carries `WHERE trade_status !=
  'SUCCESS'` and sets `trade_status`, `bank_type`, `transaction_id`,
  `pay_type` and `time_expire`; **zero affected rows throws the same
  already-handled "更新已处理，不需要更新"**, so a concurrent or replayed
  notify that loses the atomic takeover produces no side effects — the
  balance credits and the SMS job only run after the guarded update
  succeeds.
* The balance is added **synchronously in Redis** (not via a trade-finish
  listener): `shopDepositTotal` company hash, the user's deposit total via
  `UserDepositBalanceMutationService.addUserDepositTotal`, and the daily
  `dayRechargeTotal{yyyy-MM-dd}` hash.
* When the deposit trade has a mobile, a
  `RechargeSendSmsNoticeJobDispatchPublisher.publishRechargeSendSmsNotice`
  is scheduled **after commit** (the same `TransactionSynchronization`
  pattern as the orders plane).

So the deposit variant reuses the notify endpoint, the signature
verification and the company resolution, but replaces the trade CAS + fan-out
with a deposit balance mutation and an SMS job — there is no `event:291`
publication for a recharge.

## The admin 代客下单 deferred variant

The shop-admin POS surface (`NormalOrderController`, `@AdminAuth`,
`/api/v1/*` with route aliases `order.checkout` / `order.create` /
`order.payment` / `order.payment.query`) is the **create-and-pay** flow for
staff ordering on behalf of a walk-in customer. Its defining property:
**creating the order does not persist it**. The order exists only as a Redis
snapshot until the money lands, so an abandoned checkout leaves no
`orders_normal_orders` row, no `orders_associations` row, no charge, no
inventory deduction and no `normal_order_add` fan-out to clean up. This is the
mirror image of the wxapp flow above, where the order exists (and is
cancellable) before payment.

```mermaid
sequenceDiagram
    participant A as Admin operator
    participant C as NormalOrderController
    participant O as ShopadminNormalOrderCreateOrchestrator
    participant CA as ShopadminDeferredOrderCache
    participant P as NormalOrderAdminPaymentService
    participant S as ShopadminDeferredOrderService
    participant D as OrdersPaymentDoPaymentService
    participant T as trade table
    participant TX as persistPreparedSnapshot transaction

    A->>C: POST /order/create
    C->>O: createUserOrder
    O->>O: guards, operator-cart fill, format, preparePricingSnapshot
    O->>CA: saveWaitPay WAIT_PAY snapshot with auto-cancel TTL
    O-->>A: order_id with order_status NOTPAY, nothing persisted
    A->>C: POST /order/payment with auth_code or pay_type
    C->>P: payment
    P->>P: orders_associations row missing
    P->>CA: deferred cache hit for order_id
    P->>S: payDeferredOrder
    S->>CA: withLock shopadmin deferred lock order_id
    S->>S: pay type from auth_code regex or pay_type or snapshot
    S->>D: doPayment pos or wxpaypos or alipaypos
    D->>T: insertPendingTrade NOTPAY then charge
    alt channel confirms pay_status true
        S->>TX: persistPreparedSnapshot persistOrder minusStores runAfterOrderInsert
        S->>S: markPayedIfNeeded then supplier split then normal_order_add
        S->>CA: delete snapshot
        S-->>A: pay_status true with order_id
    else ResourceException business failure
        S->>S: channel refund then trade REFUND then snapshot REFUNDED
    else other RuntimeException
        S->>CA: snapshot PAID_WAIT_ORDER with 24h TTL
        S-->>A: 订单创建失败，请稍后查询
    end
    A->>C: GET /order/payment/query
    C->>S: supplementAfterPaymentQuery for a SUCCESS trade
    S->>TX: materialize or refund or retry refund under the lock
```

*The admin deferred create-and-pay pipeline: create only caches a `WAIT_PAY` snapshot, payment charges and materializes the order in the same locked call, and every failure path either refunds the customer or parks the snapshot for query-time recovery.*

### Create — only the Redis snapshot

`POST /order/create` → `NormalOrderAdminCreateUserOrderService.createUserOrder`
→ `ShopadminNormalOrderCreateOrchestrator.create`. The orchestrator runs the
same staged pipeline as the wxapp one (only `order_type=normal_shopadmin`
passes `assertKnownOrderType` — otherwise "无此类型订单！" — then need-params,
operator-cart fill via the goods-implemented `OrderCheckoutCartPort`, item
check, distributor check, format, `assertDepositBalance`) but stops after the
**non-transactional pricing half**
`ShopadminNormalOrderCreateTransactionalRunner.preparePricingSnapshot`
(cross-border identity, point-deduct formatting, markdown, pre-persist
enrichment). It then pre-assigns the `order_id`
(`NormalOrderNumericIdService.generate(userId)`, `pay_type` defaulting to
`pos` when neither orderData nor params carry one) and writes a
`ShopadminDeferredOrderSnapshot` through `ShopadminDeferredOrderCache.saveWaitPay`
into **shared Redis** under `shopadmin:deferred:{companyId}:{orderId}`: status
`WAIT_PAY`, `expireAt = now + ttl`, TTL = the `auto_cancel_time − now` delta
falling back to 15 × 60 s, carrying the JSON-serialized `params` and
`orderData` maps (so the whole create state round-trips), the pay type and the
`total_fee`. The create response returns the preassigned `order_id` with
`order_status=NOTPAY`. **No DB rows and no `normal_order_add` publish** —
pinned by `ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest`
and `NormalOrderAdminCreateUserOrderServiceNormalOrderAddDispatchPublishProbeTest`,
which verify only `preparePricingSnapshot` + `saveWaitPay` are invoked.
Because the TTL is exactly the auto-cancel window, an unpaid snapshot expires
out of Redis instead of needing the cancel cron.

### Pay — the deferred route and its guards

`POST /order/payment` → `NormalOrderAdminPaymentService.payment`. It reads the
operator JWT (`company_id` and auth map come from
`OPERATOR_JWT_USER_DATA`, "未登录" otherwise), requires `order_id` and — unlike
the wxapp surface — requires `pay_type` **or** `auth_code` ("支付方式必填");
`pay_type` defaults to `wxpayh5` only when an `auth_code` is present, and an
`auth_code` is classified by the same regexes as the wxapp flow
(`^1[0-5][0-9]{16}$` → `wxpaypos`, `^(25|26|27|28|29|30)[0-9]{14,22}$` →
`alipaypos`).

The routing test is the heart of the variant: it looks the order up in
`orders_associations` and, **when that row is missing but the deferred cache
has the snapshot, delegates to
`ShopadminDeferredOrderService.payDeferredOrder`** — the missing-association
test is exactly what distinguishes a deferred order from a persisted one.
With an association row present the ordinary admin path runs instead
(supplier_order rejection, NOTPAY/PART_PAYMENT guard, effective-type check,
order bundle, point-forcing, `updatePayTypeIfApplicable`, prescription /
deposit / alipaymini guards) and calls `doPayment` directly.

`payDeferredOrder` re-derives the pay type — `auth_code` regex first, else the
request `pay_type`, else the snapshot's — and only
`DEFERRED_PAY_TYPES` (`pos`, `wxpaypos`, `alipaypos`) are accepted
("暂不支持该支付方式"); a `REFUNDED` snapshot answers "订单已退款". Execution
runs under the `shopadmin:deferred:lock:{orderId}` Redis lock
(`setIfAbsent` with a UUID token, 120 s TTL, token-checked unlock; contention
→ "订单处理中，请稍后查询", Redis failure → "订单缓存不可用" /
"订单处理繁忙，请稍后查询"). Inside the lock, in order:

1. **Already materialized** (`orders_normal_orders` has the id) →
   `recoverExistingOrder`: if the order is still `NOTPAY` apply
   `markPayedIfNeeded`, then re-run the supplier split; return a paid
   response.
2. **Already-paid trade exists** (`trade_state` in SUCCESS/REFUND for the
   order, newest first) → REFUND answers "订单已退款", SUCCESS materializes
   from the snapshot and returns the paid result.
3. **Otherwise `OrdersPaymentDoPaymentService.doPayment`** with the payment
   data rebuilt from the snapshot (`trade_source_type=normal`,
   `pay_fee = snapshot.totalFee`, `auth_code` forwarded, user/shop/
   distributor/mobile/title from the cached `orderData`,
   `authorizer_appid` from `woa_appid` and `wxa_appid` from `wxapp_appid` in
   the operator auth map). This is the same
   `doPayment` as the wxapp flow — same payment-subject resolution, same
   config gate, same TX1 `insertPendingTrade` NOTPAY insert, same
   out-of-transaction channel branches — reached from the admin surface.
   `pos` finalizes synchronously inside TX1 (cash — the `pos` branch simply
   calls `finalizeTradeSuccess`); `wxpaypos` invokes the WeChat
   micropay `WxPayMicropayRequest` and `alipaypos` the Alipay
   `alipay.trade.pay` barcode call inside
   `OrdersExternalPayParamBuildService` (outside any transaction), both
   returning `pay_status=true` plus `transaction_id` when the channel
   confirms synchronously — which TX2 turns into the CAS flip and publish
   (Alipay `10003` USERPAYING returns `pay_status=false` and the snapshot
   stays `WAIT_PAY`). A non-true `pay_status` returns the channel result
   unchanged.
4. **On `pay_status=true`** → `persistAndFinishAfterPay`: the trade is
   re-read by `trade_info.trade_id` (falling back to the newest paid trade),
   and in one sequence `ShopadminNormalOrderCreateTransactionalRunner.persistPreparedSnapshot`
   runs one `@Transactional` (`OrderCreatePersistencePortImpl.persistOrder` →
   `OrderCreateInventoryPort.minusStores` →
   `OrderCreatePostCommitPortImpl.runAfterOrderInsert` — the same
   cart-emptying, points-deduction and OPL writes as the wxapp create, with
   the preassigned `order_id` reused), then
   `OrdersTradeFinishNormalOrderPaySuccessApplyService.markPayedIfNeeded` is
   invoked **directly** with the snake_case trade row (the same method the
   pay-success bridge listener calls — shopadmin-class orders jump straight
   to `DONE` unless they use platform item stock, then the ziti auto-complete
   marks non-logistics items delivered
   and closes the order; the conversion is defensive — an
   `IllegalArgumentException` becomes "支付状态更新失败"), then
   `splitAndPublishOnce` runs the supplier split
   (`SupplierOrderSplitOnNormalOrderAddService.split`, which itself skips
   already-split suppliers — the select-count guard is what makes the
   recovery leg's re-split idempotent) and publishes the **single**
   `normal_order_add` event (`NormalOrderAddDispatchPublisher` →
   `event:orders:normal_order_add`, ASYNC+REDIS, payload
   `{company_id, order_id, pay_type}` — the same event and payload shape the
   wxapp orchestrator publishes after its create). The cache entry is then
   deleted, so `ORDERED` is observable only as the key's absence. Split and
   publish are exactly-once because they run only on this path and under the
   lock.

Note the ordering subtlety: the charge itself (`doPayment` →
`finalizeTradeSuccess`, registered on the TX1 or TX2 synchronization)
publishes the trade-finish event on `event:291` when
its own `transactionTemplate` transaction commits — i.e. **before**
`persistPreparedSnapshot` has created any order row. The pay-success bridge
(order 77) therefore finds no `NormalOrders` row and returns empty, so **no
`event:288` pay-success event is published for a deferred order on this
path**; the **direct** `markPayedIfNeeded` call after materialization is what
actually flips the shopadmin order to `DONE` (its return payload is ignored
by the deferred service — it does not forward it to the pay-success
publisher). The same applies to the order-row-dependent listeners on the
trade-finish plane: item sales and profit sharing read
`orders_normal_orders_items`/`order_profit`, both written inside
`persistOrder`, so on the happy path they find nothing and no-op; the
trade-field-only listeners (statistics, the pay-success SMS, websocket push,
the "订单支付" OPL) are unaffected by the missing order row and run on their
own gates. The recovery leg does not re-publish either —
`supplementAfterPaymentQuery` re-applies `markPayedIfNeeded` (idempotently,
for an order that materialized but stayed `NOTPAY`) and re-runs the supplier
split, but the only trade-finish publish it can trigger is
`finalizeTradeSuccessAfterChannelQuery` inside the query resolve, which again
fires before materialization. Consumers that must react to a shopadmin order
becoming paid should therefore key off the `normal_order_add` plane (or the
order row itself), not `event:288`.

### Failure semantics after a successful charge

* **`ResourceException`** (business rejection — invalid snapshot data,
  shop/distributor state) → `refundAfterBusinessFailure`: a channel refund
  through the payment bundle's runners — `wxpay*` through
  `WxpaySecapiAftersalesRefundRunner` (the `secapi/pay/refund` call,
  merchant API cert, `out_refund_no = orderId`), `alipay*` through
  `AlipayTradeAftersalesRefundRunner` (`alipay.trade.refund`, SUCCESS or
  PROCESSING both count as ok), and `pos` is treated as refunded
  (cash, nothing to reverse). On success the trade is CAS-flipped
  `SUCCESS → REFUND` (`WHERE trade_state='SUCCESS'`) and the snapshot becomes
  `REFUNDED`; on channel failure it becomes `REFUND_FAILED`. Both use the
  24 h `COMPENSATION_TTL_SECONDS` window so operators can still see and
  retry them. The original `ResourceException` is then re-thrown to the
  caller.
* **Any other `RuntimeException`** → `markPaidWaitOrder`: the snapshot
  becomes `PAID_WAIT_ORDER` (with the trade id) on the 24 h compensation TTL
  and the caller sees "订单创建失败，请稍后查询" — **the money is held, not
  refunded**, waiting for the query leg.

### `GET /order/payment/query` — the compensation driver

`NormalOrderAdminPaymentQueryService.queryPayment` first resolves the trade
through the same `OrdersPaymentTradeQueryService.resolve` as the wxapp poll
(already-`SUCCESS` answers directly; otherwise the active channel is queried
and a confirmed `SUCCESS` is finalized through
`finalizeTradeSuccessAfterChannelQuery`, which itself re-publishes the
trade-finish event). Then, for a paid trade with a positive `company_id`, it
calls `ShopadminDeferredOrderService.supplementAfterPaymentQuery` — the
recovery leg:

* **Order already materialized** → `recoverExistingOrder` (markPayedIfNeeded
  when still `NOTPAY`, then re-split) — the idempotent landing for a payment
  that succeeded after a crash between charge and materialize.
* **Snapshot gone entirely** → `refundWithoutSnapshot`: refund the orphaned
  charge through the channel runners and CAS-flip the trade to `REFUND`.
* **Snapshot `REFUND_FAILED`** → retry the refund under the lock
  (`retryRefundLocked`, which first recovers an already-materialized order);
  on success the trade flips and the snapshot becomes `REFUNDED`.
* **Snapshot `REFUNDED`** → nothing to do.
* **Any other snapshot (incl. `WAIT_PAY`/`PAID_WAIT_ORDER`)** → materialize
  the order from the snapshot under the lock, with the same failure semantics
  as the payment call.

So the admin deferred variant shares `doPayment`, the trade CAS, the notify
surface and the trade-finish fan-out with the wxapp flow, and adds: a Redis
snapshot instead of an order row, a per-order Redis lock around
charge+materialize, a direct (non-bus) `markPayedIfNeeded`, exactly-once
supplier split + `normal_order_add`, and a query-driven refund/retry
compensation that the wxapp flow does not need (its order row already exists
when the charge happens). The state machine of the snapshot itself
(`WAIT_PAY`/`PAID_WAIT_ORDER`/`ORDERED`/`REFUNDED`/`REFUND_FAILED` and the
TTL windows) is documented in the [Orders Domain deferred
section](../domains/orders.md#admin-代客下单-the-deferred-create-and-pay-pipeline).

## `/payment/query` — the polling fallback

`WxappPaymentService.query(tradeId)` delegates to
`OrdersPaymentTradeQueryService.resolve`: an already-`SUCCESS` trade returns
`{status: SUCCESS, msg: 支付成功}` immediately; otherwise the channel is
queried through `OrdersPaymentChannelQueryService`, and when the channel
reports `SUCCESS`, `finalizeTradeSuccessAfterChannelQuery` performs the CAS
flip (persisting `transaction_id`) and publishes the trade-finish event —
the same fan-out as the notify, reached from the client's poll instead. This
is the recovery path when a notify is lost or delayed.

## Invariants and failure semantics

* **Only `NOTPAY` trades transition.** Both the notify path (explicit
  re-read + throw) and the internal paths (CAS `WHERE trade_state='NOTPAY'`)
  enforce it. The notify path throws and the facade swallows; the internal
  path re-publishes idempotently for an already-`SUCCESS` trade. The deposit
  recharge callback applies the twin guard on its own table (ECX-10915): a
  conditional `WHERE trade_status != 'SUCCESS'` update atomically takes over
  the row and zero rows throws the same already-handled
  `ResourceException` — a concurrent double callback cannot credit the
  balance twice.
* **The ack is decoupled from the outcome.** Business exceptions (already
  handled, unknown trade) still produce XML `SUCCESS`; only unexpected
  exceptions produce `FAIL`. This is what makes replayed notifies harmless —
  but it also means an operator cannot distinguish "processed" from
  "ignored" by reading the gateway's retry log.
* **Signature verification is a precondition.** No state is written before
  the MD5 (wxpay) or RSA2 (alipay) check passes; a failed check is an
  unexpected exception → `FAIL` (wxpay) or an empty-body 200 (alipay). On the
  alipay facade the **tenant-ownership cross-check runs even earlier**: the
  trade/deposit row's owning company must equal the passback `company_id`
  before the signature is verified at all, so a forged passback cannot steer
  the callback across tenants.
* **The fan-out is transactionally ordered.** The orders plane publishes
  after commit, so listeners never see a trade that is not actually
  `SUCCESS`. The pay-success bridge additionally guards on the order row's
  `order_status='NOTPAY'`, so a trade can succeed without the order flipping
  (e.g. a second concurrent trade) and then no `event:288` is published.
* **Channel SDKs never run inside a database transaction.** The
  `doPayment` split (TX1 → channel call → TX2) keeps gateway latency off the
  connection pool; local payment types collapse to TX1 only. The mirrored
  pin lives in `OrdersPaymentDoPaymentServicePaymentSubjectTest`, which
  asserts the channel answer arrives at tx-depth 0. The trade-off: a crash
  between the channel charge and TX2 leaves a `NOTPAY` trade whose money may
  already have moved — that is exactly what `/payment/query`'s
  `finalizeTradeSuccessAfterChannelQuery` repairs.
* **`pay_type` semantics differ by listener.** `point`/`deposit` skip SMS,
  websocket push, brokerage and coupon consumption but **not** item sales,
  statistics or profit sharing; `membercard` trades skip the WXA template and
  instead fulfil the VIP port inside the callback service. Anyone adding a
  listener must decide explicitly which pay types it serves.
* **`trade_no` is assigned exactly once**, on the first SUCCESS, from the
  Redis daily sequence; the hash is keyed by order id so a repeated trade for
  the same order reuses its sequence number.
* **A deferred order exists only after the money lands.** The 代客下单 create
  writes no DB row; the charge, the `persistPreparedSnapshot` transaction and
  the direct `markPayedIfNeeded` all run under the same per-order Redis lock,
  so a concurrent retry of `POST /order/payment` for the same order either
  finds the materialized order (step 1) or contends on the lock. Business
  failure after a charge refunds through the channel; a non-business crash
  parks `PAID_WAIT_ORDER` with the money held, and only
  `GET /order/payment/query` resolves either state — the operator-side
  contract is "keep querying until it answers".

## Configuration and operations

| setting | where | effect |
| --- | --- | --- |
| `ecshopx.payment.wechat.notify-url` | `application.properties` (default `http://127.0.0.1/wechatAuth/wxpay/notify`, a local placeholder) | the absolute URL handed to WeChat as `notify_url`; must be publicly reachable — override per deployment via env/args, and blank fails unify with "不支持支付服务，请联系商家" |
| `ecshopx.payment.alipay.notify-url` | `application.properties` (default `http://localhost:8080/api/v1/alipay/notify`) | same for the alipay channel |
| `(d?)alipayPaymentSetting:{sha1(companyId)}` | companys Redis | the alipay channel config — `app_id`/`private_key` gate the charge, `ali_public_key` verifies the notify and the H5 sync return; the `d` prefix selects a per-distributor override |
| `(d?)wxPaymentSetting:{sha1(companyId)}` | companys Redis | the wxpay channel config incl. the `key` used for both request signing and notify verification; the `d` prefix selects a per-distributor override |
| `distribution_distributor.payment_subject` | DB (distributor row) | which merchant subject a distributor's charges use: `1` = the distributor's own `(d?)wxPaymentSetting:`/`(d?)alipayPaymentSetting:` config (resolved by `PaymentSubjectDistributorIdPort` at charge time and by `resolveDistributorIdForAlipayRedis` at notify time), anything else falls back to distributor `0` (the platform merchant config) |
| `wechatPayment:companyId:{appId}` (+`wechatAppPayment:`/`wechatServicerPayment:`) | companys Redis | appId→companyId fallback resolution when the authorizer-appid port cannot resolve |
| `bspaySetting:{sha1(companyId)}`, `adaPaySetting:{sha1(companyId)}` | companys/shared Redis | the bspay/adapay config gates used by `assertPayConfigReady` |
| `h_trade_no_{company}_{distributor}_{MMdd}` / `c_trade_no_*` | shared Redis, 86400s TTL | the daily `trade_no` sequence |
| `shopadmin:deferred:{companyId}:{orderId}` | shared Redis, TTL = auto-cancel window (default 15 × 60 s), compensation states 24 h | the 代客下单 deferred snapshot |
| `shopadmin:deferred:lock:{orderId}` | shared Redis, 120 s TTL | the per-order lock serializing charge + materialize |

Operations notes: because the wxpay facade resolves the company from the
notify's `appid` (with Redis fallbacks), a merchant console whose appid
mapping is missing fails with "微信支付信息未配置，请联系商家" and a `FAIL` —
the fix is the Redis mapping, not the code. The alipay facade instead trusts
the passback's `company_id` but cross-checks it against the trade/deposit
row before verifying, so a missing or stale `(d?)alipayPaymentSetting:` credential
set surfaces as "支付宝信息未配置，请联系商家" and an empty-body 200 that
Alipay retries. The `inital_response` column is
the audit trail for every raw notify; replayed notifies overwrite it with the
latest body.

## Extension points

* **A new trade-finish listener** — implement `DispatchListener` in your
  bundle's `dispatch/` package, add a `*RegistrationConfig` in bootstrap
  registering on **both** `EVENT_TRADE_FINISH` and `EVENT_TRADE_FINISH_CSV292`
  (the twin planes share the listener set by design), and pick an `@Order`
  outside the occupied band. Follow [Add Event or Job](add-event-or-job.md).
* **A new channel** — add the pending-external arm in
  `preparePaymentInTx` and the invocation arm in `invokeExternalChannel`
  (usually delegating to
  `OrdersExternalPayParamBuildService.buildPayParamsOrInvoke`), the config
  gate in `assertPayConfigReady`, and a notify controller + facade that
  verifies before writing and reaches the trade through
  `AlipayNotifyOrderSidePort` (or your own port, as bspay/adapay do).
  Remember the SDK call runs outside any transaction by design — keep it
  that way, and put every write the call produces into TX2
  (`persistExternalPaymentInTx`). If the channel validates against
  per-distributor settings, the resolved `distributorIdForSetting` is what
  both the gate and the SDK call receive.
* **A new pay-success consumer** — listen on
  `event:288:OrdersBundle\Events\NormalOrderPaySuccessEvent` rather than the
  trade-finish plane; the bridge only publishes it when the order actually
  flipped, which is the stronger guarantee most consumers want. Caveat for the
  deferred flow: a 代客下单 order never produces `event:288` (see the ordering
  note above), so a consumer that must cover shopadmin orders needs the
  `event:orders:normal_order_add` plane or the order row instead.
* **A new deferred pay type** — extend `DEFERRED_PAY_TYPES` in
  `ShopadminDeferredOrderService` (and the corresponding branch in
  `OrdersPaymentDoPaymentService`/`OrdersExternalPayParamBuildService`) plus a
  refund route in `executeChannelRefund`; remember `pos` needs no refund
  runner, and every new type must also decide what
  `supplementAfterPaymentQuery` does with an orphaned charge of that type.

## Focused tests

The bootstrap `WxappPaymentTradeFinish*SyncDispatchFlowTest` family is the
 behavioural pin for this flow. Each test builds an
`InMemoryDispatchRegistry` + `DispatchCore.asyncReady` + `SyncDispatchDriver`
facade with the production listener and listener name, publishes an
`EVENT_TRADE_FINISH` payload shaped like a real trade row (`pay_type`,
`pay_fee`, `company_id`, `order_id`, `time_start`), and verifies the
downstream port:

* `WxappPaymentTradeFinishSmsNotifySyncDispatchFlowTest` — wxpay zero-fee
  with an eligible mobile sends the templated SMS with `pay_money=0.00` and
  the formatted `pay_time`; `point` pay sends nothing at all
  (`verifyNoInteractions`).
* `WxappPaymentTradeFinishNotifyPushSyncDispatchFlowTest` — `point` and
  `deposit` pay types and zero/absent `pay_fee` skip the websocket port; a
  wxpay payload with `pay_fee=150` sends exactly one camelCase envelope with
  `payFee=1.50`.
* `WxappPaymentTradeFinishWxaTemplateMsgSyncDispatchFlowTest` — both wxpay
  zero-fee and `point` payloads reach
  `TradeFinishWxaTemplateWaitingDeliveryDispatchService.dispatchIfApplicable`
  (the listener delegates; the pay-type gate lives in the service, which is
  why the test still verifies the delegation).
* `UpdateItemSalesWxappPaymentSyncDispatchFlowTest` and
  `TradeFinishCountBrokerageWxappPaymentSyncDispatchFlowTest` — the
  deliberate contrast: item sales increments for **both** wxpay and `point`
  (keys only on company/order), brokerage runs for wxpay but skips `point`.
* `TradePayFinishStatisticsEventSyncDispatchFlowTest` — `trade_state=NOTPAY`
  and unknown `trade_source_type` skip statistics; a normal SUCCESS payload
  records once.
* `TradeFinishProfitEventSyncDispatchFlowTest` — missing
  `order_id`/`company_id` skip the profit bus; present keys invoke it once.
* `JushuitanTradeFinishEventDispatchFlowTest` /
  `WdtErpTradeFinishEventDispatchFlowTest` — the ASYNC Redis planes: the
  fanned-out message carries the listener name and `default` queue, and
  `DispatchConsumerRuntime.consume` runs the listener.
* `TradeFinishWxaTemplateMsgEventSyncDispatchFlowTest` and
  `TradeFinishWorkWechatNotifyEventSyncDispatchFlowTest` cover the same
  listeners from non-wxapp entry shapes.

The deposit recharge idempotency guard is pinned in
`AdapayDepositTradeRechargeCallbackServiceIdempotencyTest` (bootstrap,
ECX-10915): the update wrapper's SQL segment carries the `trade_status`
guard; a takeover loss (guarded update returns 0) throws
"更新已处理，不需要更新" with **no** Redis balance mutation and no SMS publish;
a successful first callback credits the user's deposit total exactly once.

The alipay facade is pinned by `AlipayNotifyFacadeDepositSmsDispatchTest`
(`ecshopx-payment`): a `depositRecharge` passback with `TRADE_SUCCESS`
reaches the deposit port with the passback's `pay_type` and the notify's
`trade_no` as `transaction_id`; a passback `company_id` that disagrees with
the trade/deposit row's owning company rejects **before** signature
verification and without touching either port; and a non-deposit payload
never touches the deposit port. `AlipayNotifyOrderSidePortImplTest`
(`ecshopx-orders`) pins the port's own guards (blank/unknown trade id,
invalid company).

The deferred variant is pinned in `ecshopx-orders`:

* `ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest`
  ("POST /api/v1/order/create shopadmin — deferred cache only, no
  NORMAL_ORDER_ADD on create") — a `normal_shopadmin` state with
  `pay_type=wxpaypos` invokes exactly `preparePricingSnapshot` +
  `saveWaitPay(state, orderId, companyId, wxpaypos, 100, ttl)` and returns
  the preassigned `order_id` with `order_status=NOTPAY`; no persistence port,
  no publisher.
* `NormalOrderAdminCreateUserOrderServiceNormalOrderAddDispatchPublishProbeTest`
  ("admin POST /api/v1/order/create — createUserOrder writes deferred cache,
  no dispatch on create") — the same pin one level up, through the param
  builder into the orchestrator.

The `doPayment` core itself is pinned by two sibling test classes in
`ecshopx-orders`:

* `OrdersPaymentDoPaymentServicePaymentSubjectTest` — a distributor with the
  platform payment subject (`resolveActualDistributorId(141, 285) → 0`)
  validates and charges against distributor **0** config
  (`assertConfigComplete(141, 0)` and `buildPayParamsOrInvoke(..., 0, ...)`),
  the wxpay unify runs with **no transaction open** (tx-depth probe inside the
  stubbed channel answer), exactly two `transactionTemplate` executions wrap
  the external path (TX1 + TX2), and the inserted trade still records the
  order's real `distributor_id` (`"285"`). The point-pay case pins the local
  branch: a single transaction, no external channel interaction at all
  (`verifyNoInteractions` on the external build service and both membercard
  SDK services).
* `OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest` — the
  synchronous-SUCCESS publish contract: exactly one
  `OrdersTradeFinishDispatchPublisher.publish` per finalize, and the captured
  snake_case row carries the gate keys each downstream listener reads —
  `company_id`/`order_id`/`pay_type` (brokerage), `user_id`/`shop_id`/
  `distributor_id` (link-member), `pay_fee`/`discount_fee` (printer),
  `trade_source_type`/`trade_state` (fapiao, statistics, groups), and
  `trade_id` for the wxpay customs-declare listener. It also pins that a
  zero-cash order with `point_amount > 0` is built as `pay_type=point`
  (points deducted once) rather than `localPay`, and that positive-fee wxpay
  synchronous confirmations run the channel call outside the transaction
  with two template executions.

## Related pages

- [Dispatch Bus](../concepts/dispatch-bus.md) — the SYNC/ASYNC routing, fan-out planner and Redis driver this flow rides on.
- [Orders Domain](../domains/orders.md) — the `Trade`/`NormalOrders` state model, the create pipeline that precedes payment, the trade-finish bridge, and the [deferred-snapshot state machine](../domains/orders.md#admin-代客下单-the-deferred-create-and-pay-pipeline).
- [Payments Domain](../domains/payments.md) — all channel notify surfaces, the port inversion, and the channel settings layout.
- [Aftersales Domain](../domains/aftersales.md) — what happens to a paid order when it is refunded.
- [Configuration](../operations/configuration.md) — how per-deployment overrides (incl. `ecshopx.payment.wechat.notify-url`) reach the runtime.
- [Add Event or Job](add-event-or-job.md) — how to add a listener to these planes.
