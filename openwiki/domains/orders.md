---
type: domain-reference
title: Orders Domain (ecshopx-orders)
description: The order/cart/logistics/settlement Bundle — the two-level Trade vs NormalOrders/NormalOrdersItems model with order_class variants, the wxapp create-and-pay pipeline, the admin 代客下单 deferred create-and-pay pipeline (Redis snapshot, materialize-after-pay, refund-on-failure, cloud-warehouse fast-buy branch), the trade-finish → order pay-success bridge with the shopadmin/shopscreen ziti auto-complete, auto-cancel/finish cron families, statements settlement, invoicing, the cross-bundle cart ports (goods and promotions), rights redemption ownership guards, and the POS order-document access guard.
tags: [orders, cart, trade, settlement, invoice, logistics, dispatch-bus, xxl-job, wxapp, payment, supplier-split, shopadmin, deferred-create, fast-buy, shopscreen, notify, pos, rights]
sources:
  - id: openwiki-source-83df98cc525ce829d359eba9
    resource: repo://ecshopx-aftersales/src/main/java/cn/shopex/ecshopx/aftersales/service/AftersalesApplyService.java
  - id: openwiki-source-214f4a43a8dbc13e5179d2dc
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsNormalOrderDeliveryAdjustmentDispatchListenerRegistrationConfig.java
  - id: openwiki-source-9ce82340fe6f5be0bfcfe0a7
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MembersNormalOrderCancelNewCustomerDispatchListenerRegistrationConfig.java
  - id: openwiki-source-0315455e54e2688cacf7a480
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MembersNormalOrderPaySuccessNewCustomerDispatchListenerRegistrationConfig.java
  - id: openwiki-source-54c2942be6423a972467854a
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListenerRegistrationConfig.java
  - id: openwiki-source-614eaf4ca124989edf62b782
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformNormalOrderCancelDispatchListenerRegistrationConfig.java
  - id: openwiki-source-b6335fa9c0ece965e6db4d50
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformNormalOrderConfirmReceiptDispatchListenerRegistrationConfig.java
  - id: openwiki-source-f324880274cc5f35bf9d36f4
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformNormalOrderDeliveryDispatchListenerRegistrationConfig.java
  - id: openwiki-source-6794b7e88ab1775f38c31571
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformNormalOrderPaySuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-cc86c87bac74caebe654b604
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformOfflineBenefitConsumeCancelDispatchListenerRegistrationConfig.java
  - id: openwiki-source-5f8f8dc234430bba81295584
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformOfflineBenefitConsumePaySuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-905c2d15218c5790932d35d3
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/TradeFinishCountBrokerageDispatchListenerRegistrationConfig.java
  - id: openwiki-source-547e956e62dedaff163cd4e6
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/dispatch/TradeFinishCountBrokerageEventSyncDispatchFlowTest.java
  - id: openwiki-source-c006b43585c4309283fc9235
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/auth/PosOrderDocAccessPort.java
  - id: openwiki-source-27bfcace5400d2804562fba2
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersDispatchEventNames.java
  - id: openwiki-source-5ecb22a008c7475c40eec372
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersDispatchJobNames.java
  - id: openwiki-source-e3f608a3254de204f775497c
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersTradeFinishDispatchPublisher.java
  - id: openwiki-source-4863090110282765e669140c
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/order/OrderTypeSlugMapper.java
  - id: openwiki-source-68b9a0e3c776f0fe8beabd26
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/payment/PaymentSubjectDistributorIdPort.java
  - id: openwiki-source-d1961192a688961d2d90ccf4
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/integration/PaymentSubjectDistributorIdPortImpl.java
  - id: openwiki-source-77ed351f057e456baa8b5b11
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorPlatformStoreBuyGateService.java
  - id: openwiki-source-6b736dfa4c0754defee19eb0
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/order/normal/OrderCreateDistributorCheckPortImpl.java
  - id: openwiki-source-a1f68480771cd92413f00be5
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtAuthenticationFilter.java
  - id: openwiki-source-a7b2be30f60d866cf32e516a
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtPathWhitelist.java
  - id: openwiki-source-cccea56ead2aaf5463693ac8
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/cart/wxapp/WxappCartAddPortImpl.java
  - id: openwiki-source-6d42ce597e8782d354259630
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/operatorcart/OperatorCartFastBuyFacadeImpl.java
  - id: openwiki-source-42f1758a2412c6fd4cd28ec2
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/operatorcart/OrderCheckoutCartPortImpl.java
  - id: openwiki-source-938af9294e98b8cfeebec1de
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateFormatDataPortImpl.java
  - id: openwiki-source-f56f19c3e20e208ef2258517
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateInventoryPortImpl.java
  - id: openwiki-source-0a1caaa71538649542913a46
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/admin/v1/NormalOrderController.java
  - id: openwiki-source-22d01d814b19408aafd92925
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/CartController.java
  - id: openwiki-source-bfe6d6f18351fc248cd7b204
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/RightsController.java
  - id: openwiki-source-07fb8468822e1637135de269
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/WxappOrderController.java
  - id: openwiki-source-2f2f509ddef48c4bd871109f
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/cron/ScheduleCancelNormalOrdersHandler.java
  - id: openwiki-source-863ce4338f295783b77f3eeb
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/dispatch/TradeFinishCountBrokerageDispatchListener.java
  - id: openwiki-source-ed65984a7a7370ed7a4a3265
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/domain/NormalOrders.java
  - id: openwiki-source-4f7f81d28106c51d16420d2c
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/domain/NormalOrdersItems.java
  - id: openwiki-source-727f1b9d4ec6792888af22bd
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/domain/OrderAssociations.java
  - id: openwiki-source-27731f2b06169bae431088f8
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/domain/Trade.java
  - id: openwiki-source-027a9fe2ae3d1715d031f295
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/integration/notify/OrderDeliverySuccessNotifyNoOp.java
  - id: openwiki-source-189b7866f8db73ca77451821
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/integration/notify/OrderDeliverySuccessNotifyWechatService.java
  - id: openwiki-source-46a47b89a6fc2819309e8cb8
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/listener/OrderProcessLogSpringEventListener.java
  - id: openwiki-source-ed0b6806b9f7db96c86e6e50
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/admin/AdminNormalOrderDeliveryCoreService.java
  - id: openwiki-source-33cab7854832bc5d0f92ec3d
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/admin/AdminNormalOrderFullCancelService.java
  - id: openwiki-source-98844588f9706a41812b3335
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/admin/AdminOrderCancelService.java
  - id: openwiki-source-f451eeaaacd8448e25e3c13a
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/brokerage/TradeFinishCountBrokerageBusService.java
  - id: openwiki-source-ac4f22cc6b429722429f379f
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/excard/NormalOrderNumericIdService.java
  - id: openwiki-source-b30ccaf3a71ef98378fd3e75
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/finish/FinishOrderBatchService.java
  - id: openwiki-source-da30d999830db13c520004dd
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/RightsFrontWxappGetRightsCodeService.java
  - id: openwiki-source-d43f41d57c721be74e7da527
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/RightsFrontWxappGetRightsDetailService.java
  - id: openwiki-source-a4cf0a0e6cd3b01a1aa649b3
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappCartParamMergeSupport.java
  - id: openwiki-source-d6fb3304229c6ae20e8ccc1d
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappNormalOrderConfirmReceiptService.java
  - id: openwiki-source-11c92ab35f4ce1f9788dbd95
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappNormalOrderCreateOrchestrator.java
  - id: openwiki-source-5fd8daefc46d870a83fb5aa8
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappOfflinePaymentUpdateVoucherService.java
  - id: openwiki-source-bd603f0f3a098aa945fef8a0
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappOrderCreateService.java
  - id: openwiki-source-e1364acc237f099b221cab9b
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappOrderTypeRegistry.java
  - id: openwiki-source-48c58afd50c9934ddd5eb49b
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappPaymentService.java
  - id: openwiki-source-18cd744f01124cb4427d573a
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappUpdateCartItemPromotionService.java
  - id: openwiki-source-02717f5a86862fdbd267bdd9
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/localdelivery/LocalDeliveryBusinessReceiptService.java
  - id: openwiki-source-3175f232841b782e3f190944
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/create/NormalOrderCreatePrePersistEnrichmentService.java
  - id: openwiki-source-1e59ed1d09620110f69b2b0a
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/create/OrderCreatePersistencePortImpl.java
  - id: openwiki-source-2e8626d63f714e784ae5de1d
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/create/OrderCreatePostCommitPortImpl.java
  - id: openwiki-source-c2e0d21be538ee1e2cb4e432
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/NormalOrderBrokerageOnFinishService.java
  - id: openwiki-source-6bc2855e5564660b1ef558f5
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminCheckoutService.java
  - id: openwiki-source-d378afc8d5bf5b75d478055a
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminCreateUserOrderParamBuilder.java
  - id: openwiki-source-5ad0ccbf8a294aa72e1cc7b3
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminCreateUserOrderService.java
  - id: openwiki-source-a252c563ca705b5943957b38
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminPaymentQueryService.java
  - id: openwiki-source-5a2ec1c085dae82424db4467
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminPaymentService.java
  - id: openwiki-source-7243b2e27f3844d47ea37bac
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminDeferredOrderCache.java
  - id: openwiki-source-d18042d85f451076cf2b7d72
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminDeferredOrderService.java
  - id: openwiki-source-b8188fdc89f9b38a85e3bc61
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminDeferredOrderSnapshot.java
  - id: openwiki-source-6aff19aa34047820a63c0f48
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminDeferredOrderStatus.java
  - id: openwiki-source-c9da549abb50240724e0a2e2
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateOrchestrator.java
  - id: openwiki-source-6bee7c44879ca7bd01cdede7
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateTransactionalRunner.java
  - id: openwiki-source-8959b81dab6f8cb461e0aaff
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderMarkdownValidator.java
  - id: openwiki-source-b9920cf297410e01675f7de9
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderTempInfoEnrichmentService.java
  - id: openwiki-source-797ddb93053b8733950dadcf
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/NormalOrderCronService.java
  - id: openwiki-source-27cb7ccb2a77ddb62a32ee13
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/OrderInvoiceService.java
  - id: openwiki-source-9e8f48b139cc5f29ab56dcdf
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/orderlog/OrderProcessLogBusService.java
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
  - id: openwiki-source-f08b4f8c06e4e935888e75cd
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/pos/PosOrderDocAccessGuard.java
  - id: openwiki-source-70666a8a14ab1658f8f9bc57
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/setting/OrderValiditySettingRedisReadService.java
  - id: openwiki-source-7ac91aedda87207ccbed77d5
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/StatementsService.java
  - id: openwiki-source-393459e0e5faa669429f6481
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/tradefinish/OrdersTradeFinishNormalOrderPaySuccessApplyService.java
  - id: openwiki-source-5fa0c2d7cf8af41cb2355785
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/statement/generate/StatementGenerateJobInliner.java
  - id: openwiki-source-ab420cef928add1ab3268149
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/supplier/service/SupplierOrderSplitOnNormalOrderAddService.java
  - id: openwiki-source-0e983b4fde485bf2f006c24e
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/front/wxapp/RightsFrontWxappGetRightsCodeOwnershipTest.java
  - id: openwiki-source-d6719c5406b11d3876199f13
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappOfflinePaymentUpdateVoucherOrderOwnershipTest.java
  - id: openwiki-source-bfa6962bc8fd8f6d10b3b674
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/normal/create/OrderCreatePersistencePortImplShopIdAlignPhpTest.java
  - id: openwiki-source-4ee1ed8a9b50ea016a31ed25
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/normal/create/OrderCreatePostCommitPortImplNormalOrderCreateUserOrderOrderProcessLogDispatchPublishProbeTest.java
  - id: openwiki-source-7aab81459a9c49fc95cc70fd
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/NormalOrderAdminCreateUserOrderServiceNormalOrderAddDispatchPublishProbeTest.java
  - id: openwiki-source-42b59755b5fcb0f2909b92e8
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest.java
  - id: openwiki-source-1dadfec169f4df087b2904a0
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentDoPaymentServicePaymentSubjectTest.java
  - id: openwiki-source-1e0309e2b800473aab8b0e9e
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest.java
  - id: openwiki-source-e1a3872ac3208de5388c4a15
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/pos/PosOrderDocAccessGuardTest.java
  - id: openwiki-source-791cd7b9c29e1613315c4aab
    resource: repo://ecshopx-promotions/src/main/java/cn/shopex/ecshopx/promotions/integration/orders/WxappCartGoodsPromotionApplyRulesPortImpl.java
  - id: openwiki-source-3188f5eb948fe45eeed8fdf9
    resource: repo://ecshopx-promotions/src/main/java/cn/shopex/ecshopx/promotions/integration/orders/WxappCheckPlusItemPortImpl.java
  - id: openwiki-source-d88d74fead180ae405fa9428
    resource: repo://ecshopx-promotions/src/main/java/cn/shopex/ecshopx/promotions/service/systemtag/MemberCrowdMatcher.java
  - id: openwiki-source-4cc10a5188beb43abe30da3a
    resource: repo://ecshopx-promotions/src/main/java/cn/shopex/ecshopx/promotions/service/wxapp/WxappSeckillItemTicketService.java
  - id: openwiki-source-8533324b29578695d2393415
    resource: repo://ecshopx-promotions/src/test/java/cn/shopex/ecshopx/promotions/service/systemtag/MemberCrowdMatcherTest.java
generated: { by: "openwiki/0.9.0", at: "2026-11-11T17:12:30.774Z" }
verified:
  - by: openwiki/0.9.0
    at: 2026-11-11T17:12:30.774Z
---

# Orders Domain (ecshopx-orders)

`ecshopx-orders` is the largest Bundle: the order, cart, logistics, and settlement domain (its `package-info.java` reads 订单、购物车、物流与结算域). It owns the physical-order lifecycle end to end — creation (17 wxapp order-type strategies plus the admin 代客下单 deferred pipeline), payment-facing trade handling, pay-success fan-out, delivery / self-pickup / local-delivery, auto-cancel and auto-finish, supplier order splitting, distributor/supplier settlement statements, invoicing (fapiao), order process logging, and the `orders_cart` table. Every business rule mirrors the legacy PHP ECShopX semantics, and all money is fen-denominated integers.

The Bundle follows the standard layering ([Bundle Layering](../architecture/bundle-layering.md) — which uses this Bundle as its worked example): controllers in `api/admin/v1` and `api/front/v1`, orchestration in `service/` (subpackages `front/wxapp`, `normal/shopadmin`, `normal/create`, `payment`, `tradefinish`, `finish`, `consumption`, `statement`, `invoice`, `localdelivery`, `dada`, `kdniao`, `fapiao/hangxin`, …), dispatch handlers/listeners in `dispatch/`, XXL-Job entries in `cron/`, owned port interfaces in `port/`, other bundles' ports implemented in `integration/` and `openapi/`. Its Maven dependencies draw the boundary: common, dispatch, work-wechat, third-party, supplier, reservation, espier, members, companys, point, aftersales, payment, chinaums-pay, deposit, hfpay, popularize, super-admin (`ecshopx-orders/pom.xml`) — notably **not** goods, which is why both cart add/list logic and large parts of the create pipeline live behind ports (below).

## The two-level payment model: Trade vs NormalOrders

Payments are two separate state machines joined by `trade.order_id`:

* **`Trade`** (`trade` table, string PK `trade_id`, `domain/Trade.java`) is the payment ticket. `trade_state` vocabulary: `NOTPAY` (created, awaiting channel), `SUCCESS` (paid), `REFUND`, `CLOSED`, `REVOKED`, `PAYERROR`. Alongside it live `pay_type` (the chosen channel: `wxpay*`, `alipay*`, `hfpay`, `bspay`, `adapay`, `offline_pay`, `paypal`, `doumen_intl`, `point`, `prepaid_point`, `deposit`, `pos`, `localPay` for zero-fee), `pay_channel` (adapay sub-channel), `trade_source_type` (membercard / normal / servers / normal_community / deposit / order_pay …), `trade_no` (a `MMdd-seq` daily sequence assigned only on SUCCESS), `transaction_id`, and the `cur_*` currency columns (`cur_fee_rate`/`cur_fee_type`/`cur_fee_symbol`/`cur_pay_fee`) storing the exchange-rate conversion of a multi-currency order.
* **`NormalOrders`** (`orders_normal_orders`, bigint `order_id` PK) is the business order. `order_status`: `NOTPAY` → (`PART_PAYMENT`) → `PAYED` / `WAIT_GROUPS_SUCCESS` (groups) → `WAIT_BUYER_CONFIRM` → `DONE`, or `CANCEL`. Independent columns track each aspect: `pay_status` (NOTPAY/PAYED/ADVANCE_PAY/TAIL_PAY), `delivery_status` (PENDING/PARTAIL/DONE), `ziti_status` (self-pickup), `self_delivery_status` (merchant self-delivery), `cancel_status` (NO_APPLY_CANCEL/WAIT_PROCESS/REFUND_PROCESS/SUCCESS/FAILS), `receipt_type` (logistics/ziti/dada/merchant), `invoice_status` (PENDING/PARTAIL/DONE). `order_class` records the business variant (`normal`, `groups`, `seckill`, `bargain`, `community`, `shopguide`, `pointsmall`, `excard`, `employee_purchase`, `shopadmin`) while `order_type` distinguishes physical (`normal`) from service orders.
* **`OrderAssociations`** (`orders_associations`) mirrors the order header across all order types — `order_id`, `order_type`, `order_class`, `order_status`, receiver/discount fields — so cross-type listing and the payment guard can work without knowing which detail table owns the row. Most state changes update `orders_normal_orders` **and** `orders_associations` together. **The admin 代客下单 pipeline deliberately delays this row**: at create time neither `orders_normal_orders` nor `orders_associations` exists, and the "does this order exist" test is exactly what later routes the payment into the deferred path (below).

The mapping between the API `order_type` slug and the two DB columns is the contract `OrderTypeSlugMapper` (`ecshopx-common/order/OrderTypeSlugMapper.java`): the slug (`normal_seckill`, `normal_shopadmin`, …) routes to a strategy, while `orders.order_type` only takes the coarse class (`normal` / `service` / `bargain`) and `orders.order_class` the fine variant (`seckill`, `shopadmin`, …). Seckill auto-cancel relies on `order_type='normal' AND order_class='seckill'` — never write the slug into `order_type`.

The invariant that ties the two together: **only `NOTPAY` trades may transition**. `OrdersTradePaymentCallbackService.applyTradePaymentNotify` (the notify entry called by the Alipay/Chinaums/DoumenIntl port impls in this bundle — see [Payments Domain](payments.md) for the notify surfaces) persists the raw callback into `trade.inital_response`, then re-reads the trade and throws "更新已处理，不需要更新" unless the fresh row is still `trade_state = NOTPAY` — the guard that makes repeated channel notifies idempotent. On `SUCCESS` it stamps `time_expire`, assigns `trade_no` from a Redis daily sequence (`h_trade_no_{company}_{distributor}_{MMdd}` hash keyed by order id, `c_trade_no_*` counter, 86400s TTL), fulfils the membercard VIP port for `trade_source_type=membercard`, and publishes the Jushuitan + trade-finish + WdtErp fan-out. The sibling internal path `OrdersPaymentDoPaymentService.finalizeTradeSuccess` (point/prepaid_point/deposit/pos/localPay) does the same NOTPAY guard as a genuine CAS — the UPDATE carries `WHERE trade_state = 'NOTPAY'` — and an already-`SUCCESS` trade re-publishes the trade-finish event idempotently instead of failing.

`NormalOrdersItems` (`orders_normal_orders_items`) carries per-line state that aftersales depends on: `aftersales_status` (WAIT_SELLER_AGREE/WAIT_BUYER_RETURN_GOODS/WAIT_SELLER_CONFIRM_GOODS/SELLER_REFUSE_BUYER/SELLER_SEND_GOODS/REFUND_SUCCESS/REFUND_CLOSED/CLOSED), `delivery_status`, `delivery_item_num`, `auto_close_aftersales_time`, and `left_aftersales_num` on the header is the aftersales quota apply decrements (see [Aftersales Domain](aftersales.md)).

```mermaid
erDiagram
    trade }o--o| orders_normal_orders : "order_id when order-backed"
    orders_normal_orders ||--|{ orders_normal_orders_items : "order_id"
    orders_normal_orders ||--|| orders_associations : "order_id mirrored header"
    orders_normal_orders ||--o{ supplier_order : "supplier split"
    orders_normal_orders ||--o{ orders_delivery : "shipments"
    orders_normal_orders ||--o{ orders_cancel_orders : "cancel applications"
    orders_normal_orders ||--o{ orders_invoice : "fapiao apply"
    orders_cart ||--o{ orders_normal_orders : "checkout source"
    trade {
        string trade_id PK
        string order_id
        string trade_state
        string pay_type
        string pay_channel
        string trade_source_type
        string trade_no
        string transaction_id
        int pay_fee
        float cur_fee_rate
    }
    orders_normal_orders {
        bigint order_id PK
        bigint company_id
        bigint user_id
        string order_status
        string pay_status
        string delivery_status
        string ziti_status
        string cancel_status
        string receipt_type
        string order_class
        string order_type
        smallint uses_platform_item_stock
        boolean is_shopscreen
        boolean is_logistics
        int left_aftersales_num
        int prescription_status
        bigint original_order_id
    }
    orders_normal_orders_items {
        bigint id PK
        bigint order_id
        string aftersales_status
        string delivery_status
        int delivery_item_num
    }
    orders_associations {
        bigint order_id PK
        string order_type
        string order_class
        string order_status
        bigint user_id
        bigint promoter_user_id
    }
```

Caption: the two-level payment model — `trade` (payment ticket, many per order but at most one SUCCESS) joined to the order tables via `order_id`, with `orders_associations` as the cross-type header mirror and `supplier_order` rows created by the post-create split.

## Entrypoints

### Admin REST (`/api/v1`, `@AdminAuth`)

`OrderController` is the operator workbench: `GET /orders` (list), `GET /order/{order_id}` (detail), `GET /order/process/{orderId}` (OPL history), `POST /order/{order_id}/cancel`, `POST /delivery`, `PUT` update-delivery (merchant and courier variants), `POST /confirmReceipt`, `POST /writeoff/{order_id}` + `/qr_writeoff` (self-pickup verification), `POST /order/markdown` + `/order/markdown/confirm` (price change), `POST /businessreceipt/{orderId}` and `/confirm/goods/{orderId}` (dada local delivery), invoice endpoints, order-validity settings, drug-order processing. Around it: `NormalOrderController` (the shopadmin 代客下单 checkout / create / payment / payment-query surface — below), `StatementsController` (summarized/detail/export/confirm), `InvoiceController`/`FapiaoController`/`InvoiceSellerController`, `OfflinePaymentController` (offline voucher audit), `RightsController`/`RightsLogsController`, `TradeController`/`TradeRateController`, `ShippingTemplateController`, `CompanyRel{Logistics,Dada,Shansong,Delivery}Controller` (logistics credentials), `StatementPeriodSettingController`, `DeliveryController`, `KuaidiController`, `RefundErrorLogsController` (`/trade/refunderrorlogs/*`).

### Front wxapp (`/api/v1/h5app/wxapp`)

`WxappOrderController`: `POST /order` (create-and-pay), `POST /order_new` (create, H5-auth flavour), `GET /orders`, `GET /order/{order_id}` and `/order_new/{order_id}`, `POST /order/cancel`, `POST /order/confirmReceipt`, `POST /order/delivery` (merchant self-delivery), `POST /order/updateDelivery/{delivery_id}`, `POST /order/cancel/deliverystaff`, `POST /order/deliverypackag/confirm`, `POST /order/bind/{order_id}`, `GET /ziticode`, `GET /pickupcode/{order_id}`, `GET /trackerpull` (logistics tracking), offline voucher upload/update, `POST /getFreightFee`, `GET /groupOrders`. `WxappPaymentController` (in the same package): `POST /payment` (initiate payment for an existing order), `GET /payment/config`. `CartController` is the member cart surface — see [The cart is owned here but implemented in goods and promotions](#the-cart-is-owned-here-but-implemented-in-goods-and-promotions).

### OpenAPI

`openapi/` implements the `Openapi*Port` interfaces the `ecshopx-openapi` gateway exposes at `/api/openapi/internal/v2`: order list (v1/v2)/incr/sold-list/detail, order cancel / confirm-cancel / deliver / writeoff, trade list, shipping templates, member order v2, member point order v2. Read ports reuse the same mappers as the admin surface.

### XXL-Job (`cron/`, all publish `CronAlertEvent` on failure)

| handler | service | batch shape |
| --- | --- | --- |
| `cancel-orders` | `NormalOrderCronService.scheduleCancelOrders` | direct worker, 20/page |
| `cancel-offlinepay-orders` | `NormalOrderCronService.scheduleOfflinePayCancelOrders` | direct worker (offline_pay rows) |
| `cancel-group-orders` | `GroupsOrderService.scheduleCancelOrders` | direct worker (order_class=groups) |
| `cancel-service-group-orders` | `GroupsServiceOrderService.scheduleCancelOrders` | service groups |
| `cancel-seckill-orders` | `SeckillNormalOrderService.scheduleCancelOrders` | direct worker (seckill) |
| `cancel-pointsmall-orders` | `PointsmallNormalOrderService.scheduleCancelOrders` | direct worker (pointsmall) |
| `finish-orders` | `NormalOrderCronService.scheduleFinishOrders` | publishes slow-queue job |
| `consumption-orders` | `NormalOrderCronService.scheduleConsumptionOrders` | publishes slow-queue job (skipped under OEM-shuyun flag) |
| `generate-statements` | `StatementsService.scheduleGenerateStatements` | pages merchants, publishes job per window |
| `create-invoice`, `query-invoice`, `query-invoice-red` | `OrderInvoiceService` | enqueue per pending invoice |
| `settle-order-profit`, `share-order-profit`, `update-rights-status`, `chinaums-division-{transfer,resubmit,download}-sftp` | profit/rights/division services | |

See [Scheduling (XXL-Job)](../concepts/scheduling-xxljob.md) for the executor wiring and seeded crons.

## Order creation: registry → orchestrator → transactional runner → split → fan-out

```mermaid
sequenceDiagram
    participant C as WxappOrderController POST /order
    participant S as WxappOrderCreateService
    participant R as WxappOrderTypeRegistry
    participant ST as WxappOrderTypeCreateStrategy bean
    participant O as WxappNormalOrderCreateOrchestrator
    participant TX as WxappNormalOrderCreateTransactionalRunner
    participant PC as OrderCreatePostCommitPortImpl
    participant SP as SupplierOrderSplitOnNormalOrderAddService
    participant BUS as NormalOrderAddDispatchPublisher

    C->>S: createOrder with merged params and auth
    S->>S: resolve source_from, client_ip, salesman_id, epidemic register payload
    S->>R: WxappOrderCreateContext
    R->>R: order_type slug to bean name, else throws 无此类型订单
    R->>ST: create ctx
    ST->>O: physical normal order create (NormalOrderCreateState)
    O->>O: validate, checkout-cart fill, item check, distributor check, format data, coupon after freight
    O->>TX: runInTransaction
    TX->>TX: cross-border identity, point deduct format, markdown, pre-persist enrich
    TX->>TX: persistOrder (NormalOrders, items, OrderAssociations, OrderPromotions, rel tables)
    TX->>TX: groups and employee-purchase extends, inventory minus
    TX->>PC: runAfterOrderInsert (same transaction)
    PC->>PC: ziti rel, coupon consume, point deduction, cart empty, OPL publish, pay-remind job
    O->>SP: splitSupplierOrders in its own transaction
    O->>BUS: publish normal-order-add payload
    S->>S: OPL 订单创建, pay-remind, then doPayment for auto-pay flavours
```

Caption: the wxapp create pipeline — the type registry picks a strategy bean, the physical-order strategy runs the shared orchestrator, one transaction persists everything including post-commit side effects, then supplier split and dispatch fan-out run after.

1. **`WxappOrderCreateService.createOrder`** normalizes params (`WxappOrderParamMergeSupport`), resolves `source_from`/`client_ip`/`salesman_id`, validates the epidemic-register payload, then delegates to `WxappOrderTypeRegistry.create`. `POST /order_new` (`createNewOrder`) is the H5 variant that additionally checks the distributor whitelist and supports promoter-as-salesman rewrites.
2. **`WxappOrderTypeRegistry`** maps the `order_type` slug to one of 17 strategy beans (`WxappOrderTypeCreateStrategy`): `normal`, `normal_groups`, `normal_seckill`, `bargain`, `normal_bargain`, `normal_pointsmall`, `normal_excard`, `normal_community`, `normal_shopguide`, `normal_shopadmin`, `normal_employee_purchase`, `normal_drug`, `service`, `service_groups`, `service_seckill`, `membercard`, `supplier_order`. Unknown slug → "无此类型订单！". All physical classes funnel into `WxappPhysicalNormalOrderCreateSupport` → the shared `WxappNormalOrderCreateOrchestrator`, with receiver validation (zip/name/mobile regexes) before the cart for normal/pointsmall and service-order required params for `service`.
3. **`WxappNormalOrderCreateOrchestrator.create`** runs the staged pipeline: `OrderCreateNeedParamsPort.validate` → recommend-merge → checkout-cart fill (per type: employee-purchase cart, groups fast-buy, member/distributor cart, pointsmall cart, operator cart) → store-adjust block check → item check → distributor check → `OrderCreateFormatDataPort.format` (+ deposit balance assert, coupon-after-freight, currency default, employee-purchase prepaid quota) → groups post-format hook → `WxappNormalOrderCreateTransactionalRunner.runInTransaction`. `getOrderTempInfo` runs the same stages without persisting (order confirmation preview).
4. **The transactional runner** is `@Transactional`: cross-border identity (CN ID card regex), point-deduct formatting, markdown (改价), pre-persist enrichment, `OrderCreatePersistencePortImpl.persistOrder`, groups/employee-purchase extend hooks, inventory minus, then **`OrderCreatePostCommitPortImpl.runAfterOrderInsert` inside the same transaction** — it saves the ziti relation (skipped for shopadmin and community classes), consumes the coupon, persists directed-crowd discount totals, deducts points (pointsmall orders deduct `order.point` at create; normal orders deduct `point_use` when pay_type ≠ point — the two branches are mutually exclusive to avoid double-deduction), empties the cart per order type, publishes the "订单创建" OPL and the pay-remind job. The cart-empty branch (`emptyCartIfSupported`) keys off `params.order_type` + `params.cart_type`: `normal_employee_purchase` → the employee-purchase empty-cart port; `normal_shopadmin` + `cart_type=fastbuy` → `OperatorCartFastBuyFacade.clearFastBuyCart` (the operator cloud fast-buy bucket); `normal_shopadmin` otherwise → per-item `OperatorCartDeleteDataService.delCartData` over `operator_cart`; community/excard orders skip; a member `cart_type=fastbuy` clears the Redis `fastbuy:{sha1(companyId+userId)}` key (writing `[]` with a 600 s TTL); and the default path deletes `orders_cart` rows matching company+user+shop(`distributor_id`) for the ordered item ids.
5. **`persistOrder`** reuses a preassigned `order_id` from `orderData` when one is present and only falls back to `NormalOrderNumericIdService.generate(userId)` otherwise — the shopadmin orchestrator pre-assigns the id before the snapshot is cached (below), so the create-and-materialize paths write the same key. `generate(userId)` is a Redis hash increment on the `yyyyMMdd` date key (companys Redis template, 1-day TTL) where the id is days-since-`2012-01-01` + (minute-of-day ÷ 90, 3 digits, capped 999) + the 5-digit zero-padded hash value (incremented by a random 1–9 per call) + `userId mod 10000` (4 digits). When `order_type` or `order_class` arrives blank (callers that bypass `OrderCreateFormatDataPort`), both are derived via `OrderTypeSlugMapper.resolve` on `order_type_slug`, with the slug defaulting to `normal_shopadmin`. `shop_id` is taken verbatim from `orderData` (the PHP `_formatOrderData` parity — 0 when `isCheckShopValid=false`), never rewritten to `distributor_id`; currency fields (`fee_type`/`fee_rate`/`fee_symbol`) come from `CompanyDefaultCurrencyService` when the order data lacks them and are stamped onto the order, every item line, and the association row; `NormalOrdersRelSupplier` rows are inserted from the `supplier_freight_fee` map. The shopadmin `receiver_*` address keys the format port passed through are copied onto the order row (`applyReceiverFieldsFromOrderData`, `receiver_name` truncated to 50 code points), and `uses_platform_item_stock` is normalized to the persisted `0`/`1` flag. The new row starts `order_status=NOTPAY`, `pay_status=NOTPAY`, `delivery_status=PENDING`, `cancel_status=NO_APPLY_CANCEL`, `ziti_status=NOTZITI`, `self_delivery_status=NOTMERCHANT`.
6. **Supplier split** (`SupplierOrderSplitOnNormalOrderAddService.split`, its own transaction) collects supplier ids from items and `NormalOrdersRelSupplier`, then inserts one `supplier_order` row per supplier sharing the parent `order_id` — payment, cancel, and settlement all consult these children (adapay/bspay payment loads them as `sub_orders` for split payment).
7. **`NormalOrderAddDispatchPublisher`** fans the new order out (`event:orders:normal_order_add`) to youshu and marketing-center listeners (registered in bootstrap configs); the wxapp service then continues to `doPayment` for the auto-pay flavours (`point` when the client explicitly chose it and total is 0; `offline_pay` salesman orders).

## Admin 代客下单: the deferred create-and-pay pipeline

The shop-admin POS flow (`NormalOrderController`, `@AdminAuth`, route aliases `order.checkout` / `order.create` / `order.payment` / `order.payment.query`) is a **create-and-pay** surface for staff creating an order on behalf of a walk-in customer (代客下单). Its defining property: **creating the order does not persist it**. The order exists only as a Redis snapshot until the money actually lands, so an abandoned checkout leaves no `orders_normal_orders` row, no inventory deduction, and no `normal_order_add` fan-out to clean up.

* `POST /checkout` (购物车结算) → `NormalOrderAdminCheckoutService.checkout`: the same staged pipeline as create (`NormalOrderAdminCreateUserOrderParamBuilder.build` → `assertKnownOrderType` (only `normal_shopadmin`) → need-params → operator-cart fill → item check → distributor check → format → cross-border identity → point-deduct format → markdown), ending in `ShopadminNormalOrderTempInfoEnrichmentService.apply` — the order-confirmation preview (it pre-assigns an `order_id` and `auto_cancel_time` too, but nothing is persisted).
* `POST /order/create` (代客下单) → `NormalOrderAdminCreateUserOrderService.createUserOrder` → `ShopadminNormalOrderCreateOrchestrator.create`: the same staged pipeline plus `assertDepositBalance`, but instead of the transactional runner it calls only `ShopadminNormalOrderCreateTransactionalRunner.preparePricingSnapshot` — the **non-transactional** pricing half (cross-border identity, point-deduct formatting, markdown, pre-persist enrichment). It then pre-assigns the `order_id` (`NormalOrderNumericIdService.generate(userId)`, defaulting `pay_type` to `pos` when neither orderData nor params carry one) and writes a `ShopadminDeferredOrderSnapshot` (status `WAIT_PAY`, `expireAt = now + ttl`, TTL = `auto_cancel_time − now` delta, falling back to 15 × 60 s) into Redis under `shopadmin:deferred:{companyId}:{orderId}` (shared Redis, JSON-serialized `params` + `orderData`). **No DB persist and no `normal_order_add` publish** — pinned by `ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest` and `NormalOrderAdminCreateUserOrderServiceNormalOrderAddDispatchPublishProbeTest`, which verify only `preparePricingSnapshot` + `saveWaitPay` are invoked and the response carries the preassigned `order_id` with `order_status=NOTPAY`.
* `POST /order/payment` (下单支付) → `NormalOrderAdminPaymentService.payment`: requires `pay_type` or `auth_code` ("支付方式必填"); an `auth_code` is classified by regex — `^1[0-5][0-9]{16}$` → `wxpaypos` (WeChat micropay), `^(25|26|27|28|29|30)[0-9]{14,22}$` → `alipaypos` (Alipay face-to-face barcode) — otherwise `pay_type` defaults to `wxpayh5`. It looks the order up in `orders_associations`; **when that row is missing but the deferred cache has the snapshot, it delegates to `ShopadminDeferredOrderService.payDeferredOrder`** — the fallback that makes the two-phase create transparent to the caller. With an association row present it runs the ordinary admin pay path (supplier_order rejection, NOTPAY/PART_PAYMENT guard, effective-type check, order bundle build, point-forcing for zero-cash, pay-type persist, prescription/deposit/alipaymini guards) and calls `doPayment`.
* `GET /order/payment/query` (支付结果查询) → `NormalOrderAdminPaymentQueryService.queryPayment` (trade_id required, "支付单号不存在" when blank/0): resolves the trade state through `OrdersPaymentTradeQueryService.resolve` (a SUCCESS trade answers directly; otherwise the channel is queried — wxpay `pay/orderquery`, Alipay openapi, doumen — and a confirmed SUCCESS is finalized through `finalizeTradeSuccessAfterChannelQuery`), then, for a paid trade, calls `ShopadminDeferredOrderService.supplementAfterPaymentQuery` — the recovery leg (below).

```mermaid
sequenceDiagram
    participant A as Admin operator
    participant C as NormalOrderController
    participant O as ShopadminNormalOrderCreateOrchestrator
    participant CA as ShopadminDeferredOrderCache Redis
    participant P as NormalOrderAdminPaymentService
    participant S as ShopadminDeferredOrderService
    participant D as OrdersPaymentDoPaymentService
    participant TX as persistPreparedSnapshot transaction
    participant B as PaySuccess bridge and split and add publish

    A->>C: POST /order/create 代客下单
    C->>O: createUserOrder state
    O->>O: need-params, operator-cart fill, item check, distributor check, format, deposit assert
    O->>O: preparePricingSnapshot without a transaction
    O->>CA: saveWaitPay WAIT_PAY snapshot with TTL
    O-->>A: order_id and order_status NOTPAY, nothing persisted
    A->>C: POST /order/payment with auth_code or pay_type
    C->>P: payment
    P->>CA: association row missing, snapshot exists
    P->>S: payDeferredOrder
    S->>CA: withLock shopadmin:deferred:lock:orderId
    S->>D: doPayment pos or wxpaypos or alipaypos
    alt pay_status true
        S->>TX: persistPreparedSnapshot persistOrder, minusStores, runAfterOrderInsert
        TX->>B: markPayedIfNeeded then split then normal_order_add
        S->>CA: delete snapshot ORDERED
    else ResourceException business failure
        S->>S: channel refund then trade SUCCESS to REFUND then REFUNDED
    else other RuntimeException
        S->>CA: snapshot PAID_WAIT_ORDER with 24h compensation TTL
        S-->>A: 订单创建失败，请稍后查询
    end
    A->>C: GET /order/payment/query
    C->>S: supplementAfterPaymentQuery for a paid trade
    S->>TX: materialize from snapshot or refund or retry refund under the lock
```

Caption: the admin deferred create-and-pay pipeline — create only caches a WAIT_PAY snapshot, payment materializes the order, marks it paid, splits suppliers and publishes normal_order_add, and every failure path either refunds the customer or parks the snapshot for query-time recovery.

### Snapshot lifecycle

```mermaid
stateDiagram-v2
    [*] --> WAIT_PAY : create writes snapshot with auto_cancel TTL
    WAIT_PAY --> ORDERED : doPayment success then persistPreparedSnapshot
    WAIT_PAY --> PAID_WAIT_ORDER : materialization raised a RuntimeException
    PAID_WAIT_ORDER --> ORDERED : payment query materializes from snapshot
    PAID_WAIT_ORDER --> REFUNDED : refund succeeds on retry
    WAIT_PAY --> REFUNDED : ResourceException then channel refund ok
    REFUNDED --> [*] : 订单已退款
    ORDERED --> [*] : cache delete after split and publish
    WAIT_PAY --> REFUND_FAILED : channel refund failed
    PAID_WAIT_ORDER --> REFUND_FAILED : refund retry failed
    REFUND_FAILED --> REFUNDED : query retries refund under the lock
    REFUND_FAILED --> ORDERED : query finds the order already materialized
    WAIT_PAY --> [*] : TTL expiry
```

Caption: `ShopadminDeferredOrderSnapshot` statuses with the 24h `COMPENSATION_TTL_SECONDS` window for every non-ORDERED terminal or recovery state.

`ShopadminDeferredOrderService.payDeferredOrder` resolves the pay type first: `auth_code` regex classification (same patterns), else the request `pay_type`, else the snapshot's — and only `DEFERRED_PAY_TYPES` (`pos`, `wxpaypos`, `alipaypos`) are accepted ("暂不支持该支付方式"). A `REFUNDED` snapshot answers "订单已退款". Execution runs under the `shopadmin:deferred:lock:{orderId}` Redis lock (`setIfAbsent` with a UUID token, 120 s TTL, token-checked unlock; contention → "订单处理中，请稍后查询", Redis outage → "订单缓存不可用"/"订单处理繁忙，请稍后查询"). Inside the lock, in order:

1. **Already materialized** (`orders_normal_orders` has the id) → `recoverExistingOrder`: if the order is still `NOTPAY` apply `markPayedIfNeeded`, then re-run the supplier split; return a paid response.
2. **Already-paid trade exists** (`trade_state` SUCCESS/REFUND for the order) → REFUND answers "订单已退款", SUCCESS materializes from the snapshot (`materializeFromSnapshotAfterPaid`) and returns the paid result.
3. **Otherwise `doPayment`** with the payment data rebuilt from the snapshot (`trade_source_type=normal`, `pay_fee = snapshot.totalFee`, `auth_code` forwarded). `pos` finalizes synchronously; `wxpaypos`/`alipaypos` invoke the WeChat micropay / Alipay barcode channel in `OrdersExternalPayParamBuildService` and return `pay_status` when the channel confirms (Alipay `10003` USERPAYING stays pending). A non-true `pay_status` returns the channel result unchanged — the snapshot stays `WAIT_PAY`.
4. **On `pay_status=true`** → `persistAndFinishAfterPay` under the lock: `ShopadminNormalOrderCreateTransactionalRunner.persistPreparedSnapshot` (one `@Transactional`: `persistOrder` → `minusStores` → `runAfterOrderInsert`), then `OrdersTradeFinishNormalOrderPaySuccessApplyService.markPayedIfNeeded` (snake_case trade row — the same NOTPAY→PAYED/DONE/WAIT_GROUPS_SUCCESS CAS the bridge listener uses; a counter ziti order jumps straight to `DONE` through the ziti auto-complete, while a `uses_platform_item_stock` fast-buy order stays `PAYED` awaiting delivery), then `splitAndPublishOnce` (supplier split + the **single** `normal_order_add` publish for shopadmin orders), then the cache entry is deleted (`ORDERED` is therefore observable only as absence). Split and publish are exactly-once because they run only on this path and under the lock.
5. **Failure semantics after a successful charge.** `ResourceException` (business rejection — invalid snapshot data, shop/distributor state) → `refundAfterBusinessFailure`: channel refund via `WxpaySecapiAftersalesRefundRunner` (wxpay*, requires merchant API cert) or `AlipayTradeAftersalesRefundRunner` (alipay*, SUCCESS or PROCESSING counts as ok), `pos` is treated as refunded (cash, nothing to reverse); on success the trade is flipped `SUCCESS→REFUND` by CAS (`WHERE trade_state='SUCCESS'`) and the snapshot becomes `REFUNDED`; on failure it becomes `REFUND_FAILED` (both with the 24 h `COMPENSATION_TTL_SECONDS` so operators can still see and retry them). Any other `RuntimeException` → `markPaidWaitOrder` (`PAID_WAIT_ORDER`, 24 h TTL) and the caller sees "订单创建失败，请稍后查询" — the money is held, not refunded.
6. **Recovery** — `GET /order/payment/query` on a `SUCCESS` trade calls `supplementAfterPaymentQuery`: if the order exists, `recoverExistingOrder` (markPayedIfNeeded when still NOTPAY, then re-split — the idempotent landing for a payment that succeeded after a crash); if the snapshot is gone entirely, `refundWithoutSnapshot` refunds the orphaned charge and CAS-flips the trade; a `REFUND_FAILED` snapshot retries `retryRefundLocked` under the lock (or recovers an already-materialized order first); any other snapshot materializes from the cached state.

The param builder fixes the shopadmin semantics: `promotion=normal`, `order_source=shop_offline`, `is_online_order=false`, `receipt_type=ziti` — **unless `cart_type=fastbuy`, which flips it to `logistics`** (the cloud-warehouse 立即购买 variant ships from the platform bucket rather than being picked up at the counter) — `order_type=normal_shopadmin`, `source_from=dianwu`, `pay_type` defaulting to `pos`, markdown pre-validated by `ShopadminNormalOrderMarkdownValidator` ("改价参数格式错误", "确认整单还是按件改价", "整单改价金额必填", "按件改价商品ID必填", "按件改价商品总价和折扣必须设置一个"), and a `user_id` resolved to a member row via `MemberLookupByUserIdOnlyService.requireMemberRowForFirstHop` (mobile/appids copied into params); the seven `receiver_*` address keys present in the request are copied into params for the fastbuy shipping variant. The operator cart itself lives in **companys** (`OperatorCartController` — `POST /cartfastbuyadd` 店务立即购买（云仓分桶）, `/cartdataadd`, `/cartdataupdate`, `/scancodeAddcart`, `GET /cartdatalist`, delete — over `operator_cart` plus the `OperatorShopFastBuyRedisService` bucket), read through the goods-implemented `OperatorCartDataListFacade` / `OperatorCartFastBuyFacade` and filled into the create state by the goods-implemented `OrderCheckoutCartPort.fillItemsFromOperatorCart` — which branches on `cart_type=fastbuy` **before** the ordinary operator-cart read: the platform-store gate `DistributorPlatformStoreBuyGateService.assertShopadminPlatformStoreBuyAllowed` (立即购买需指定有效店铺 / 无效店铺 / 该店铺未开启云仓可购买), then `revalidateFastBuyPlatformStock` (立即购买已失效，请重新加购 for a stale bucket), then `getFastBuyCartDataList`; every other cart type goes through `OperatorCartDataListFacade.getCartDataList`. Both paths take checked rows from the first `valid_cart` block ("购物车为空" when none) and attach `_checkout_cart_meta`; at materialization `runAfterOrderInsert` empties them as described above.

## Payment: `WxappPaymentService` → `OrdersPaymentDoPaymentService`

`POST /payment` (`WxappPaymentService.payment`) is the pay-an-existing-order surface. Guards: order must exist in `orders_associations`; `supplier_order` type is rejected ("暂不支持该订单类型的支付"); `order_status` must be `NOTPAY` or `PART_PAYMENT` ("当前订单不需要支付"); the effective order type must be payment-bundle supported; an order whose cash total is 0 with points > 0 is forced to `pay_type=point`; prescription orders must have an approved prescription (`OrderPrescriptionPaymentGuardService`); deposit requires a logged-in member card, alipaymini requires the alipay user id. `GET /payment/config` (`doPayment`, wxpay/hfpay/deposit only) is the QR-scan surface: with an `order_id` it pays an existing NOTPAY/PART_PAYMENT order, without one it builds a store-consumption trade (`trade_source_type=order_pay`) from distributor/shop + amount.

`OrdersPaymentDoPaymentService.doPayment` is the shared core both call (and the one the deferred path charges through). It is deliberately split into **three phases** so no external channel SDK or HTTP call ever runs while a database transaction is open: TX1 (`preparePaymentInTx`) finds-or-inserts the NOTPAY trade and completes the purely local pay types in the same short transaction; the channel invocation then happens outside any transaction; and TX2 (`persistExternalPaymentInTx`) persists the channel outcome and finalizes synchronously-paid trades. `OrdersPaymentDoPaymentServicePaymentSubjectTest` pins this split — it counts `transactionTemplate.execute` invocations (1 for a local `point` payment, 2 for a `wxpayh5` payment) and asserts the mocked channel builder ran at transaction depth 0.

* **Input normalization** — `pay_type` is lowercased (`Locale.ROOT`) and written back into `data` before anything reads it. The payment-subject distributor id is then resolved once: `PaymentSubjectDistributorIdPort.resolveActualDistributorId(companyId, distributorId)` (interface in `ecshopx-common/payment`, implemented in **ecshopx-distribution** `integration/PaymentSubjectDistributorIdPortImpl` — see [Stores & Merchants / distribution](stores-and-merchants.md)) keeps the distributor id only when that distributor row carries `payment_subject=1` (it collects payment itself) and returns 0 otherwise — platform-held payment subject — and also 0 when the distributor id is 0 or the row is missing. The resolved `distributorIdForSetting` is used **only** for payment-config validation and channel config lookup; the trade row still records the original `distributor_id`.
* **Config gate** — per pay_type against `distributorIdForSetting`: wxpay/alipay validation services (`assertConfigComplete`), the hfpay port, the bspay Redis setting (`bspaySetting:{sha1(companyId)}` in companys Redis), the adapay Redis setting (`adaPaySetting:{sha1(companyId)}` in shared Redis); `point`/`prepaid_point`/`deposit`/`pos`/`offline_pay`/`paypal`/`doumen_intl` skip validation entirely, and an unknown pay type fails here with "无此类型支付".
* **Cache reuse** — for adapay/bspay only, supplier `sub_orders` are loaded into `data` first, then if a NOTPAY trade for the same company+order+pay_type+pay_fee+(pay_channel) already carries `payment_params`, the stored client payload is decoded and returned without touching the channel again.
* **TX1 — `preparePaymentInTx`** — `findOpenTradeWithoutParams` reuses a matching open NOTPAY trade, else `insertPendingTrade` inserts one (encrypted mobile, dealer/merchant ids resolved from the distributor peek, shop-code-prefixed `trade_id`, and currency conversion for wxpay/alipay/hfpay: when a fee rate is supplied or the company's default currency rate applies, `pay_fee`/`total_fee` are stored converted and the original amounts go to `cur_pay_fee` etc.). Before inserting, a zero-cash order with `point_amount > 0` whose pay type is not `point`/`deposit` is rewritten to `pay_type=point` with `pay_fee=point_amount` (source comment: 现金应付 0 且存在积分抵扣：按纯积分支付建单，避免落 localPay). The local pay types then complete entirely inside TX1 and return `pay_status=true` with `trade_info`: zero-fee non-prescription trades finalize as `localPay`; `point` deducts member points *unless* `trade_source_type=normal_pointsmall` (积分商城已在创建时扣 order.point；此处仅完结交易，避免纯积分自动支付双扣); `prepaid_point` (employee-purchase prepaid quota, deducted at create — 禁止微信通道) and `pos` finalize directly; `deposit` runs `DepositTradeConsumeService.consume` then finalizes. Only the external pay types (`bspay`, `adapay`, `wxpay*`, `alipay*`, `hfpay`, `offline_pay`, `paypal`, `doumen_intl`) fall through as `pendingExternal`; anything else throws "无此类型支付".
* **Channel invocation (no DB transaction)** — `bspay`/`adapay` call the membercard SDK `createPayment` services with the Redis merchant config; the `wxpay*`/`alipay*`/`hfpay`/`offline_pay`/`paypal`/`doumen_intl` family delegates to `OrdersExternalPayParamBuildService.buildPayParamsOrInvoke(payType, companyId, distributorIdForSetting, …)`. The synchronous barcode channels live there: `wxpaypos` sends a WeChat V2 micropay (requires `auth_code`, 缺少付款码 otherwise) and returns `pay_status=true` with `transaction_id` when the channel confirms; `alipaypos` calls `tradePayBarcodeFaceToFace` and maps code `10000` to `pay_status=true`/`status=SUCCESS`, `10003` to `USERPAYING` (pending), invalid-barcode sub-codes to `BadRequest` and anything else to `ResourceException`.
* **TX2 — `persistExternalPaymentInTx`** — persists the bspay (`bspay_req_date`/`inital_request`/`transaction_id`) and adapay (`inital_request`/`transaction_id`) outcomes, calls `finalizeTradeSuccess` only when the client result carries `pay_status=true` (which is why micropay/barcode confirmations settle in-process while async channels stay NOTPAY until the notify or the query path), and always `savePaymentParams` — the JSON client payload that the adapay/bspay replay cache above reads back.
* **`finalizeTradeSuccess`** — re-reads the trade: an already-`SUCCESS` row re-publishes the trade-finish event idempotently and returns; a non-`NOTPAY` row returns silently. Otherwise it assigns the `MMdd-seq` `trade_no` from Redis — the shared Redis template hash `h_trade_no_{companyId}_{distributorId}_{MMdd}` maps order id → sequence (a re-read wins over a new increment, so the same order keeps one sequence) fed by the counter `c_trade_no_*`, both with 86400 s expiry — then CAS-updates `WHERE trade_id=? AND trade_state='NOTPAY'` to `SUCCESS` with `time_expire` (and `pay_type=localPay` on the zero-fee override), throws 交易单状态更新失败 on a lost race, re-reads, fulfils `maybeFulfillVipGradeMembercardSale` for `trade_source_type=membercard`, and publishes the **snake_case trade row** through `OrdersTradeFinishDispatchPublisher` (the `OrdersTradeFinishAfterCommitDispatchPublisher` `@Primary` bean in bootstrap for `event:291:OrdersBundle\Events\TradeFinishEvent` — after-commit when a transaction is active). `finalizeTradeSuccessAfterChannelQuery` is the same flow for the channel-order-query path, additionally stamping `transaction_id`. The publish row carries every gate key the downstream listeners filter on — `company_id`/`order_id`/`pay_type` for brokerage, item-sales and work-wechat notify, `user_id`/`shop_id`/`distributor_id` for member-link, `pay_fee`/`discount_fee` for the printer, `trade_source_type` (e.g. `normal_groups`) for groups and fapiao — pinned by `OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest` across the zero-fee wxpay, point, groups and sync-success wxpay paths.

```mermaid
sequenceDiagram
    participant C as WxappPaymentController
    participant W as WxappPaymentService
    participant D as OrdersPaymentDoPaymentService
    participant T as trade table
    participant CH as Channel SDK or OrdersExternalPayParamBuildService
    participant P as TradeFinish publishers

    C->>W: POST /payment with order_id and pay_type
    W->>W: guards, order bundle, point-only forcing, prescription guard
    W->>D: doPayment auth data
    D->>D: lowercase pay_type, resolve payment-subject distributor id
    D->>T: reuse cached NOTPAY trade with payment_params for adapay or bspay
    rect rgb(240, 248, 255)
        note over D,T: TX1 preparePaymentInTx
        D->>T: find-or-insert NOTPAY trade with currency conversion
        alt localPay, point, prepaid_point, deposit, pos
            D->>T: finalizeTradeSuccess CAS NOTPAY to SUCCESS
        end
    end
    alt bspay or adapay
        D->>CH: membercard SDK createPayment outside any transaction
    else wxpay, alipay, hfpay, offline_pay, paypal, doumen_intl
        D->>CH: buildPayParamsOrInvoke outside any transaction
    end
    rect rgb(240, 255, 240)
        note over D,T: TX2 persistExternalPaymentInTx
        D->>T: persist channel outcome, finalize when pay_status true, save payment_params
    end
    D->>P: publish event 291 trade finish with snake_case trade row
    P-->>W: client pay params with trade_info
```

Caption: the payment core — order/payment guards and the payment-subject resolution up front, TX1 find-or-insert with in-transaction completion for local pay types, channel invocation outside any DB transaction, TX2 outcome persistence, and the trade-finish publish that unlocks the pay-success bridge.

## Trade-finish fan-out and the pay-success bridge

Every trade that reaches SUCCESS publishes the trade-finish event (`event:291:OrdersBundle\Events\TradeFinishEvent`, plus the `event:292` twin for admin offline-bank approval). One of its listeners — registered in bootstrap (`OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListenerRegistrationConfig`, `@Order(77)`, sync) — is the orders-side bridge that converts a paid trade into a paid order:

`OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener.onEvent` → `OrdersTradeFinishNormalOrderPaySuccessApplyService.markPayedIfNeeded` (one transaction):

* Only for `trade_state=SUCCESS` payloads whose `trade_source_type` is in `NORMAL_TRADE_SOURCE_TYPES` (`normal`, `normal_normal`, `normal_groups`, `normal_seckill`, `normal_community`, `bargain`, `normal_shopguide`, `normal_pointsmall`, `normal_employee_purchase`).
* Target status: `WAIT_GROUPS_SUCCESS` for `normal_groups`; `PAYED` otherwise — **except shopadmin-class orders, which jump to `DONE` only when the order is NOT `uses_platform_item_stock`** (the PHP parity: 店务收银自提支付后 DONE；云仓立即购买（uses_platform_item_stock）保持 PAYED 待发货 — a cloud-warehouse fast-buy order has goods to ship, so it lands in `PAYED` awaiting delivery, and only the counter self-pickup variant completes immediately). The update is guarded by `order_status = 'NOTPAY'` on the main row — zero affected rows means someone else already paid/cancelled, and the method returns empty so no pay-success event fires.
* Mirrors the same status onto `orders_associations` and (for `PAYED`/`DONE` targets) promotes `supplier_order` children from `NOTPAY`/`PART_PAYMENT`, setting their `pay_status=PAYED`.
* **Ziti auto-complete for shopadmin and shopscreen orders** (`maybeAutoCompleteShopadminOrShopscreenZiti`, comment 店务代客 / 大屏自提单支付成功后自动完成自提与收货): fires when the order class is `shopadmin` **or** `is_shopscreen` is true, `pay_status=PAYED` and `receipt_type=ziti`. It computes `order_auto_close_aftersales_time = now + latest_aftersale_time days` from `OrderValiditySettingRedisReadService.readPlatformSetting(companyId)` (`service/setting/`, companys Redis hash with defaults), then `markNonLogisticsItemsDelivered` sets every non-logistics line (`is_logistics` false or null) to `delivery_status=DONE`, `delivery_item_num=num`, `auto_close_aftersales_time`. The header update then stamps `ziti_status=DONE`; when the whole order is not `is_logistics`, the order jumps straight to `DONE` with `delivery_status=DONE`, `delivery_time`, `end_time`, `order_auto_close_aftersales_time` and `left_aftersales_num` summed from the item nums; when it is a logistics order and any non-logistics line was delivered, the header only flips `delivery_status=PARTAIL` (the mixed case — platform-shipped lines still await courier delivery).
* On success it builds the pay-success payload (order, user, pay_type, trade ids, fees) and the bridge publishes `NormalOrderPaySuccessDispatchPublisher` → `event:288:OrdersBundle\Events\NormalOrderPaySuccessEvent` (ASYNC/REDIS), which youshu, wsugc, members new-customer, Shuyun open-platform (trade.sync + offline-benefit consume) and employee-purchase listeners consume.

The same `markPayedIfNeeded` is invoked **directly** (not via the bus) by the deferred pipeline after `persistPreparedSnapshot`, which is why a shopadmin order can reach `DONE` without a dispatch round-trip — the trade-finish event the charge published found no order row yet, so the bridge returned empty at that moment.

The rest of the trade-finish fan-out lives in `dispatch/TradeFinish*DispatchListener` beans in this bundle: `OrdersTradeFinishPaymentSuccessOrderProcessLogDispatchListener` (writes the "订单支付" OPL for SUCCESS trades), work-wechat and wxa-template notifications, `TradeFinishCountBrokerageDispatchListener`, `TradeFinishProfitDispatchListener`, `TradeFinishCustomDeclareOrderDispatchListener` (customs), `TradeFinishFapiaoDispatchListener`, `TradeFinishLinkMemberDispatchListener`, notify-push and SMS listeners, `UpdateItemSalesDispatchListener` (item sales counts). Jushuitan and WdtErp trade-finish upload/ack assembly live in `integration/` (`JushuitanTradeFinishOrderUploadAssemblePortImpl`, `WdtErpTradeFinishPushSelfAssemblePortImpl`).

### Trade-finish brokerage listener (ECX-11089 delegating shape)

`TradeFinishCountBrokerageDispatchListener` — registered in bootstrap as `listener:orders.listeners.TradeFinishCountBrokerage`, async on the `default` queue, against **both** `EVENT_TRADE_FINISH` (`event:291`) and its `EVENT_TRADE_FINISH_CSV292` twin — runs the finish-time brokerage hooks for a paid trade so the commission path does not wait for confirm-receipt. In the ECX-11089 shape the listener itself is a thin gate that delegates to `TradeFinishCountBrokerageBusService.handleTradeFinishRow` (`service/brokerage/`):

* **Listener-level early exits** (all logged at info under the `[popularize-trade-finish]` tag): a null/empty payload returns silently; an `order_id` that is blank, non-numeric, or non-positive (Number or string form) is skipped as `dispatch_skip invalid_order_id`; `pay_type=point` (case-insensitive) is skipped as `dispatch_skip point pay_type` — pure-point trades carry no cash commission. Anything else is logged `dispatch_onEvent` and handed to the bus service with the raw payload map.
* **Bus-service guard chain** (`handleTradeFinishRow`, same `[popularize-trade-finish]` tag): the empty-payload skip is repeated (`listener_skip empty_payload`), then `company_id` **and** `order_id` must both parse to positive longs (Number or numeric string — `listener_skip invalid_ids` otherwise); `pay_type=point` is re-checked (`listener_skip pay_type_point`). Only then is the order loaded via `NormalOrdersMapper.selectById(orderId)`: a missing row is skipped (`listener_skip order_not_found`), and a row whose `company_id` disagrees with the payload's is skipped as a cross-tenant guard (`listener_skip company_mismatch`).
* **Delegation and failure isolation** — the surviving row goes to `NormalOrderBrokerageOnFinishService.orderFinishBrokerage(companyId, orderId, order)`, the same finish-time brokerage entry used by confirm-receipt, the finish batch, and the ziti write-off flows (it publishes the `event:orders:normal_order_confirm_receipt` parent event, runs the popularize `NormalOrderFinishBrokerageCoordinator`, and updates the brokerage plan close time). Any `Throwable` out of that call is caught and logged at **warn** (`listener_orderFinishBrokerage_failed`) — the listener never throws back into the dispatch bus, so a brokerage failure can neither break nor retry the trade-finish fan-out for the sibling listeners.

The dispatch envelope is pinned in bootstrap by `TradeFinishCountBrokerageEventSyncDispatchFlowTest` (blank `order_id` and `pay_type=point` never reach `handleTradeFinishRow`; alipay and positive-fee `offline_pay` payloads invoke it exactly once) and `TradeFinishCountBrokerageWxappPaymentSyncDispatchFlowTest`; the producer-side gate keys (`company_id`/`order_id`/`pay_type`) the listener filters on are pinned by `OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest` in this bundle.

## Order Process Log (OPL)

The order process log (`orders_process_log`) is written exclusively through `OrderProcessLogPublishPort` (interface in ecshopx-common, implemented in **ecshopx-dispatch**): `publish` registers an after-commit synchronization that emits `event:orders:order_process_log` via `DispatchOptions.oplQueuedAfterCommit()` (publishes inline when no transaction is active). The `OrderProcessLogDispatchListener` in this bundle consumes it and `OrderProcessLogBusService.persistEntitiesMap` inserts the row (operator_type/operator_id, remarks/detail, params JSON, is_show). A secondary Spring-event path exists for service/excard orders: `OrderProcessLogSpringEvent` + `OrderProcessLogSpringEventListener` (`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`) calling the same bus service. Roughly 60 `*OrderProcessLogEventAsyncConsumeTest` / `*OrderProcessLogDispatchPublishProbeTest` classes under `src/test/.../orders/integration` pin the publish and async-consume envelope for every state change — the `WxappOrderController` Javadoc documents the re-verification commands (e.g. `mvn -pl ecshopx-orders -Dtest=AdminOrderDeliveryOrderProcessLogDispatchPublishProbeTest,AdminApiV1OrderDeliveryOrderProcessLogEventAsyncConsumeTest test`). See [Dispatch Bus](../concepts/dispatch-bus.md) for the transport.

## Cancellation

Buyer-side `POST /order/cancel` (`WxappOrderCancelService`) validates the association row: the caller must be the buyer or the community group chief (matched via `CommunityOrderRelActivity.chief_id`); only `order_type=normal`; groups orders in `WAIT_GROUPS_SUCCESS` cannot be cancelled. It then routes by delivery state: `PENDING` → `AdminNormalOrderFullCancelService.execute` (whole-order cancel), anything else → `AdminNormalOrderPartialCancelService` (per-line aftersales creation, see [Aftersales Domain](aftersales.md)).

`AdminNormalOrderFullCancelService` (`@Transactional`) is the shared whole-order cancel for admin, buyer, and chief:

* Guards: `cancel_status` must be `NO_APPLY_CANCEL` or `FAILS`; for non-dada receipts the order must be `NOTPAY`/`REVIEW_PASS`/`PAYED` and `delivery_status=PENDING` (with explicit platform-self-sub-cancel and shop-remaining-full-cancel escape hatches for partially shipped orders); approved drug orders cannot cancel; dada orders require a `NormalOrdersRelDada` row in a cancellable status.
* `NOTPAY` branch: insert an `orders_cancel_orders` row, close the order, restore points and upoints, schedule the third-party trade-update dispatch.
* `PAYED` branch: insert/update the cancel row, create refunds (aftersales-refund rows through `AftersalesRefundService`), apply `cancel_status=WAIT_PROCESS`, call dada formal cancel when applicable.
* For paid cancels it publishes the Jushuitan and WdtErp trade-cancel dispatch events plus the `WdtErpTradeCancelSpringEvent` inside the transaction, cancels the invoice, writes the OPL; then registers after-commit `TradeRefund` (`@Qualifier("tradeRefundAsyncFanOut")`) **before** `NormalOrderCancel` (`event:orders:normal_order_cancel`) so the refund dispatch lands first — the source comment pins this ordering.

The auto-cancel cron family (`NormalOrderCronService.scheduleCancelOrders` and siblings) pages 20-at-a-time over `NOTPAY` orders whose string `auto_cancel_time` is due (CAST-computed against `now+60s`), scoped by `order_class` and excluding `offline_pay`/`point` pay types (offline_pay has its own handler that also flips `offline_payment.check_status=9` per order, wrapped in try/catch so one failure skips only that order). Beyond flipping `order_status`/associations to `CANCEL`, each order runs the restore fan-out: invoice cancellation, supplier-order cancellation, marketing join-count decrement (Redis), point/upoint return, the "订单取消" OPL, the marketing normal-order-cancel dispatch (after-commit when a transaction is active), employee-purchase restore, per-item store and purchase-limit restores, limited-time-sale quota restores, and coupon/directed-promotion restore. The seckill variant restores the seckill ticket counts per item; the groups variant restores group activity stock; the pointsmall variant restores points.

Because the deferred snapshot is keyed by the preassigned `order_id` and nothing is persisted, the WAIT_PAY window self-cleans: the cache TTL is exactly the auto-cancel window, so an unpaid 代客下单 order expires out of Redis rather than needing the cancel cron at all.

## Confirm receipt and auto-finish

`WxappNormalOrderConfirmReceiptService.confirmReceipt` (wxapp `POST /order/confirmReceipt`; the admin surface uses `AdminNormalOrderConfirmReceiptService` with the same two services): only `order_type=normal`; the caller must own the order; `order_status` must be `WAIT_BUYER_CONFIRM`; delivery must be `DONE`/`PARTAIL`; `cancel_status` must be `FAILS`/`NO_APPLY_CANCEL`; and no item line may sit in `aftersales_status` `WAIT_SELLER_AGREE`/`WAIT_BUYER_RETURN_GOODS`/`WAIT_SELLER_CONFIRM_GOODS` ("售后中的订单不能确认收货!"). Then `NormalOrderConfirmReceiptTransactionService.applyConfirmReceiptInTransaction` flips the order, associations and supplier orders to `DONE` (guarded by `order_status='WAIT_BUYER_CONFIRM'`), stamps `order_auto_close_aftersales_time = now + latest_aftersale_time days` on the header and every line, and `NormalOrderConfirmReceiptAfterCommitService.run` performs the post-transaction side effects: supplier receipt sync, the "订单完成" OPL, chinaums division relation write, bonus-point grant, brokerage settlement, and profit-plan close time.

The `finish-orders` cron does not process inline — `scheduleFinishOrders` publishes the `FinishOrderJob` (slow queue) each tick and the `FinishOrderJobHandler` → `FinishOrderJobService` → `FinishOrderBatchService.runEquivalentToFinishOrderJobHandle` executes the equivalent batch: pages `WAIT_BUYER_CONFIRM` orders past `auto_finish_time`, applies the same per-order semantics (transaction service + after-commit effects), skipping lines in the aftersales-blocking statuses. `consumption-orders` similarly publishes `ConsumptionOrderJob` (member consumption aggregation for grade upgrades; skipped entirely when `ecshopx.request-field.oem-shuyun=true`).

## Settlement statements

`generate-statements` → `StatementsService.scheduleGenerateStatements` pages distributors (1000/page, only companies whose product model is `platform`) and suppliers, resolves each merchant's settlement period (per-distributor/per-supplier `statement_period_setting` with company defaults), computes the next window end from the Redis cursor (`StatementSettlementCursorRedisPort.getDistributorLastEnd`/`getSupplierLastEnd`, falling back to the row's `created` or an inferred last-end), and when the window is fully in the past dispatches one `GenerateStatementsJob` per merchant with `{company_id, distributor_id|supplier_id, period, last_end_time, merchant_type}`.

`GenerateStatementsJobHandler` → `StatementGenerateJobInliner.runJob`: one transaction per window, looping windows forward while `endTime < now`. Inside a window it inserts the `statements` header (`merchant_type` distributor/supplier, `statement_no` generated from the merchant id + random), pages settled-eligible orders 500 at a time into `statement_details` (distributor fee = total − refund − intra-city freight − cost, with dada receipts counted as intra-city freight and others as main freight), marks the consumed trades `is_settled=true`, accumulates the header totals, and advances the Redis cursor to the window end. Any failure aborts only that merchant's window (logged at debug) and returns false so the loop stops — the cursor stays put and the next cron tick retries. `statement_status` runs `ready` → `confirmed` → `done` through the admin `StatementsController` confirm/settle surface. Alongside: `settle-order-profit` / `share-order-profit` crons feed the profit-sharing tables (`OrderProfit`, `OrderProfitSharing`, `OrderItemsProfit`).

## Invoicing (fapiao)

`OrderInvoiceService` schedules three crons over `orders_invoice`: `create-invoice` selects `invoice_status=pending AND invoice_method=online` rows and dispatches per-company with the apply-node setting deciding the time constraint (order end time vs aftersales close time); `query-invoice` re-queries `inProgress` rows stale by 180 seconds; `query-invoice-red` re-queries `waste`-status red-flush invoices missing `invoice_file_url_red`. The job handlers call `HangxinFapiaoService` (the Hangxin e-invoice gateway, DES-key transport via `HangxinRequestClient`/`HangxinTransportCrypto`) to create/query/red-flush. Red-flushing is also triggered by aftersales refunds through `OrderInvoiceRedFromAftersalesPortImpl` (the port aftersales calls), and order cancellation cancels pending invoices via `OrderInvoiceCancelOnOrderCancelService`.

## Logistics and local delivery

* **Courier delivery** — admin `POST /delivery` and `PUT /order/updateDelivery` funnel into `AdminNormalOrderDeliveryCoreService` (the shared core for every delivery-writing surface): it inserts `orders_delivery` (+items), stamps `delivery_corp`/`delivery_code`, flips `delivery_status` (PENDING→PARTAIL→DONE → `order_status=WAIT_BUYER_CONFIRM` on both header and association), publishes the "订单发货" OPL, the `event:orders:wx_order_shipping` and `event:orders:normal_order_delivery` fan-outs (youshu, marketing-center, DM-CRM, Shuyun trade.sync, DigiOS store-adjustment — the dispatch publishes registered after-commit, the third-party trade update too), and then fires the **delivery-success notify hook**: `sendDeliveryNotices` calls `OrderDeliverySuccessNotifyPort.sendDeliverySuccessNotice` once per shipment, or once per item with `item_name` for a `sep` (按包裹分开发货) delivery. The port has two impls selected by `ecshopx.orders.delivery-success-notify.enabled`: `orderDeliverySuccessNotifyWechat` (default on, `matchIfMissing=true`) builds the `orderDeliverySucc` subscribe-message payload — recipient (wxa_appid + openId) from `OrderTradeMiniProgramRecipientLookupPort`, `item_name` falling back to the order title, the courier display name — and sends it through `OrderDeliverySuccessWxaSubscribeSendPort` (implemented in promotions, `OrderDeliverySuccessWxaSubscribeSendAdapter` → the template-message sender), swallowing any failure at debug; `orderDeliverySuccessNotifyNoOp` (`havingValue="false"`) sends nothing. This mirrors the PHP `AbstractNormalOrder::sendDeliverySucc` contract and the payload envelope is pinned by `OrderDeliverySuccessNotifyWechatServiceTest`. `GET /trackerpull` pulls tracking from the kdniao (快递鸟) integration (`service/kdniao`); `CompanyRelLogisticsController` manages courier credentials and mappings.
* **Self-pickup (ziti)** — `ziti_code`/`pickupcode` endpoints and the `NormalOrderZitiWriteoffService` write-off flow (admin `/writeoff/{order_id}`, `/qr_writeoff`, openapi writeoff port) verify and complete the pickup; `NormalOrdersRelZiti` stores pickup location/date; the ziti QR mapping lives in Redis. The shopadmin 代客下单 flow is `receipt_type=ziti` end to end and completes itself on payment (no writeoff step).
* **Local delivery (same-city)** — `receipt_type=dada` orders get a `NormalOrdersRelDada` row; `LocalDeliveryBusinessReceiptService.businessReceipt` (admin `/businessreceipt/{orderId}`, 达达商家接单) branches on the `common.local-delivery-dirver` property (`dada` default, `shansong` the alternative — only these two are supported), calls the respective port (`DadaLocalDeliveryReceiptPort` / `ShansongLocalDeliveryReceiptPort`), updates the rel row's `dada_status=1`, and publishes the "商家接单" OPL shared by both drivers. Full cancel refuses when the rider has picked up (non-cancellable `dada_status`).
* **Merchant self-delivery** — `self_delivery_status` (CONFIRMING→RECEIVEORDER→PACKAGED→DELIVERING→DONE) advanced by the wxapp `/order/delivery` and `/order/updateDelivery` endpoints reusing the admin services; OPL is emitted from the service branches, never the controller (the controller Javadoc pins this).

## The cart is owned here but implemented in goods and promotions

The `orders_cart` table (`Cart` entity, `CartMapper`) and `CartController` (`@FrontAuth`, `/api/v1/h5app/wxapp`) live in this bundle, but the read-heavy endpoints delegate through ports declared in `orders/port/` and implemented in other bundles — the documented pattern that keeps the Maven graph free of an orders→goods/promotions edge:

| endpoint | delegate | implemented in |
| --- | --- | --- |
| `POST /cart` 加购 | `WxappCartAddPort.addCart` (params merged with the wxapp/H5 auth claim map by `WxappCartParamMergeSupport.applyAuthAndCartDefaults`, then the add + a list refresh) | **ecshopx-goods** `WxappCartAddPortImpl` |
| `GET /cart` 购物车列表 | `WxappCartListPort.getCartList(companyId, userId, shopId, shopType)` | **ecshopx-goods** `WxappCartListPortImpl` |
| `GET /cart/list` 分销商购物车 | `WxappCartListPort.getDistributorCartList` | **ecshopx-goods** |
| `GET /cartcount` 购物车数量 | `WxappCartListPort.getCartItemCount(companyId, authUserId, shopId, cartType, shopType, iscrossborder, isShopScreen, promoterUserId, buyUserId)` | **ecshopx-goods** |
| `POST /cart/check/plusitem` 加价购 | `WxappCheckPlusItemPort.checkPlusItem` (records the chosen plus-buy SKU in the `PlusBuyCartRedisKeys` bucket, zeroed when the gift-item lookup misses) | **ecshopx-promotions** `WxappCheckPlusItemPortImpl` |

Local cart mutations stay in this bundle over `orders_cart`: `DELETE /cartdel` + `DELETE /cartdelbat` (`WxappDeleteCartDataService`), `PUT /cartupdate/checkstatus` 选中状态 (`WxappUpdateCartCheckStatusService` — cart ids accept single, array and comma-string forms, else `购物车参数错误`; `is_checked` is parsed loosely), `PUT /cartupdate/num` 商品数量 (`WxappUpdateCartNumService`), `PUT /cartupdate/batchnum` 批量数量 (`WxappBatchUpdateCartNumService`) and `PUT /cartupdate/promotion` 促销 (`WxappUpdateCartItemPromotionService`, which itself crosses to promotions for `WxappCartGoodsPromotionApplyRulesPort.assertUserCanApplyGoodsPromotion` and stamps `activity_type=goods_promotion`).

The promotions-side rules check is the full gate chain (all in `WxappCartGoodsPromotionApplyRulesPortImpl.assertUserCanApplyGoodsPromotion`, i18n `ResourceException` on failure): the `MarketingActivity` row must exist and be `check_status=agree`, not yet released (`release_time ≤ now`), inside `[start_time, end_time)`; when `use_bound > 0` the SKU must be scoped into the activity via `MarketingActivityCatalogAccess.listMarketingIdsHitBySkuItem` (商品不适用该营销活动); when `use_shop > 0` the cart row's shop must be in the parsed `shop_ids` list unless it contains `all` (当前店铺不适用该营销活动); under the `platform` product model the cart shop must equal the activity's `source_id` (平台活动与店铺不匹配); a non-empty `valid_grade` JSON list requires the member's `grade_id` (resolved through `MemberAccountService.getMemberInfo`) to be listed (您的会员等级不能参加该活动); then the **member-crowd tag gate** runs through `MemberCrowdMatcher.matchesFullCrowdActivity` — see below; and finally a `join_limit > 0` is enforced against the `MarketingUserJoinNum:{companyId}:{marketingId}` Redis hash counter `user_{userId}` (已超过活动参与次数).

Controller-level guards worth knowing: every authed endpoint resolves the claim map from the wxapp request attribute, falling back to the H5 claims, else throws `UnauthorizedException("未登录")`; `GET /cart/list` is the one `@FrontNoAuth` surface (company id from claims, then the H5 company attribute, else `UnauthorizedException("未授权")`, and a falsy `user_id` short-circuits to an empty list — except `cart_type=offline`, which lists for walk-in buyers); `POST /cart` strips a blank `shop_type` so the goods add port is the one that rejects it, and `WxappCartParamMergeSupport` applies the buy-on-behalf rule there (`promoter_user_id == session user_id` and non-zero → `user_id` becomes `buy_user_id`); `GET /cartcount` throws `BadRequestException("参数非法")` when `shop_type` is blank and defaults `cart_type=cart`; `PUT /cartupdate/batchnum` throws `BadRequestException("参数非法")` when `cart` is missing, not a list, or an entry is not a map (an empty list returns `[]` without calling the service); `/cartupdate/checkstatus` applies the simpler buy-on-behalf rule (`buy_user_id` non-zero replaces the auth user, unconditionally); `/cartupdate/promotion` validates `cart_id`/`activity_id` strictly (`购物车参数错误` / `商品促销id错误`), the service accepts only `shop_type=distributor` (default when blank, else `ResourceException("无此购车类型")`) and re-checks via the promotions port before stamping `activity_type=goods_promotion`; the shop filter accepts `shop_id` or `distributor_id` and treats `undefined`/blank/`0` as unset. See [Goods Domain](goods.md).

The same pattern carries the whole create pipeline: `OrderCreateNeedParamsPort`, `OrderCheckoutCartPort` (operator/member/distributor/pointsmall/groups carts — the operator branch with its fastbuy gate is described above), `OrderCreateItemCheckPort`, `OrderCreateFormatDataPort` (stamps `uses_platform_item_stock=1` only for shopadmin fastbuy and passes through the shopadmin `receiver_*` keys; `assertDepositBalance`, `applyCheckoutCouponAfterFreight`), `OrderCreateInventoryPort` (store minus — a `uses_platform_item_stock` order forces `is_total_store` on every line, which is what makes the platform bucket the deduction target) are declared in `ecshopx-common/order/normal` and implemented in **ecshopx-goods** (`goods/service/order/normal/*`, `goods/service/operatorcart/OrderCheckoutCartPortImpl`); `OrderCreateDistributorCheckPort` is implemented in **ecshopx-distribution** (店铺不存在或已失效 / 该门店不支持自提). Both the wxapp orchestrator and the shopadmin orchestrator inject exactly the same port beans, which is what makes the 代客下单 pipeline a true reuse of the wxapp semantics; `OrderTypeSlugMapper` in ecshopx-common is the shared slug → order_type/order_class contract between both create chains, cron scoping and list filters.

### Member-crowd matching (`MemberCrowdMatcher`)

The crowd scoping behind the cart-promotion gate lives in ecshopx-promotions `service/systemtag/MemberCrowdMatcher`: an activity carries a JSON list `member_tag_ids` and a `member_tag_mode` of `include` (default) or `exclude`. When the id list parses empty, every member matches; with ids configured the matcher loads the member's tag ids from `members_rel_tags` (`MemberRelTagsMapper`, scoped by company + user) and tests intersection — `include` requires a hit, `exclude` requires no hit; an anonymous user (`user_id ≤ 0`) with a non-empty list always fails. Two type guards decide whether the tag check applies at all: `matchesFullCrowdActivity` only gates `MarketingActivity` rows whose `marketing_type` is `full_minus` / `full_discount` / `full_gift`, and `matchesLimitedTimeSale` only gates `SeckillActivity` rows whose `seckill_type` is `limited_time_sale`; every other activity type passes through unconditionally. Beyond `PUT /cartupdate/promotion` the same matcher is consulted by the goods-detail/list promotion enrichers (`GoodsItemsListPromotionEnrichmentService`, `SkuValidMarketingActivityService`, `WxappGoodsDetailValidMarketingActivityService`, `MarketingActivityItemListService`) and the wxapp seckill surfaces (`WxappSeckillActivity{Info,List}Service`, `WxappSeckillItemTicketService` — a `limited_time_sale` outside the member's crowd fails the ticket fetch with 您不在该活动适用人群内), all pinned by `MemberCrowdMatcherTest`. See [Marketing Domain](marketing.md).

## Rights ownership guards and offline-voucher hardening

Two member-facing surfaces on `RightsController` were hardened (ECX-10924 / ECX-10881, aligning the PHP `GuideOrderTenantScopeGuard::assertRightsBelongsToAuthUser`) so a rights record can never be read or redeemed across members or tenants:

* `GET /rights/{rights_id}` → `RightsFrontWxappGetRightsDetailService.getRightsDetail` validates the id (blank/non-numeric/`<1` → `BadRequestException("获取权益详情出错", 422)`), loads the `Rights` row, and rejects with `获取权益信息有误，请确认权益 ID` unless the row's `company_id` equals the auth company **and** its `user_id` equals the auth user — checked **before** `RightsTimesCardRowFactory` assembles (and decrypts) the detail row.
* `GET /rightscode/{rights_id}` → `RightsFrontWxappGetRightsCodeService.getRightsCode` applies the same id validation (`获取核销码出错`, 422) and the same company+user ownership check (`无权访问该权益`), and only then mints a 16-digit numeric redemption code, stores it in the **wechat Redis** under `timescardcode:{code}` → `rights_id` with a 60 s TTL, and returns barcode/QR data-URLs via `MemberBarcodeGenerateService`. A failed check touches neither Redis nor the barcode generator — pinned by `RightsFrontWxappGetRightsCodeOwnershipTest` (other-user, foreign-company, missing and malformed ids all rejected without side effects) and the sibling `RightsFrontWxappGetRightsDetailOwnershipTest`.

The offline bank-transfer voucher flow got the matching guard (ECX-10905, aligning `assertOfflineVoucherBelongsToOrder`): `POST /order/offline/update/voucher` → `WxappOfflinePaymentUpdateVoucherService.updateOfflineVoucher` rejects with `凭证不存在` when the `OfflinePayment` row is missing **or** its `company_id`/`order_id` do not match the authenticated company and the supplied `order_id` — the voucher must belong to that exact order in that tenant. The order itself must exist, still be `order_status=NOTPAY` (订单已支付，请勿重复操作), have `pay_type=offline_pay`, and belong to the caller: with a truthy `promoter_user_id` the order's `salesman_id` must equal it, otherwise the order's `user_id` must equal the auth user (`订单用户错误`). Only a voucher in `check_status=2` (refused) can be re-submitted (`不能修改凭证`); the update resets it to `check_status=0`, re-snapshots the target `OfflineBankAccount`, syncs `orders_normal_orders.offline_payment_status`, and publishes the 线下转账提交 OPL. The ownership/tenant rejection shape is pinned by `WxappOfflinePaymentUpdateVoucherOrderOwnershipTest` (cross-order and cross-company vouchers rejected without any UPDATE).

## POS order-document access guard

`PosOrderDocAccessGuard` (`orders/service/pos/`) implements `PosOrderDocAccessPort` from ecshopx-common and is the service-layer enforcement behind the POS JWT surface: `PosJwtAuthenticationFilter` (ecshopx-espier) authenticates POS tokens, rejects paths outside `PosJwtPathWhitelist` (a deliberately narrow set — `/api/v1/order*`, `/api/v1/aftersales*`, `/api/v1/checkout`, `/api/v1/operator/cart*`, `POST /api/v1/delivery`, `POST /api/v1/qr_writeoff`, `/api/v1/writeoff/*`, plus a handful of exact GET reads — with `/api/v1/store/pos-accounts` explicitly denied even though it would match a prefix), and the whitelist is what *lets* a POS session reach the order write endpoints at all; the guard then scopes **what the POS session may touch** on those endpoints. The port is invoked by the order write paths in this bundle (`AdminOrderCancelService.cancelOrder`, `AdminOrderDeliveryService` deliver, `AdminOrderMarkDownService` markdown) and cross-bundle by ecshopx-aftersales (`AftersalesApplyService`, `AftersalesRefundConfirmService`) — which is why the port is declared in ecshopx-common with the implementation here, the same dependency inversion used for the cart ports.

Rules (`G-P2/P3/B4`, POS 写单据 — 本店隔离；店员仅经手): a non-POS session returns immediately (operator JWTs are unaffected); a POS session loads the order by company + id (`订单不存在` when missing) and asserts the JWT `distributor_id` equals the order's `distributor_id` (`无权操作其他门店单据`). `role=store_manager` / `supervisor` may then write any of the store's orders; any other role (clerk) may only write an order whose `pos_store_account_id` equals the caller's `pos_store_account_id` claim (falling back to the `id` claim), and an order with no handler (`pos_store_account_id` 0/null) is unwritable by clerks — `无权操作该单据` (`ForbiddenException`). The pure-rule half (`assertStoreAndClerkWrite`) is pinned by `PosOrderDocAccessGuardTest` (manager any-in-store, cross-store rejected, clerk own/zero-id/others' orders).

## Events published and who listens

Event and listener name constants live in `ecshopx-common/dispatch/OrdersDispatchEventNames` (see [Dispatch Bus](../concepts/dispatch-bus.md) for the naming grammar); listeners are registered in bootstrap `*RegistrationConfig` classes.

| event | name | publishers | listeners |
| --- | --- | --- | --- |
| `EVENT_TRADE_FINISH` | `event:291:OrdersBundle\Events\TradeFinishEvent` (+`event:292` twin for offline bank approval) | `OrdersTradeFinishAfterCommitDispatchPublisher` (`@Primary` bean in bootstrap), notify path | pay-success bridge (sync, `@Order(77)`), OPL 订单支付, work-wechat, wxa template, **brokerage** (`listener:orders.listeners.TradeFinishCountBrokerage`, async default queue, registered on both 291 and 292 via `@Order(88)` config — delegates to `TradeFinishCountBrokerageBusService`), profit, custom declare, fapiao, link member, notify push, SMS, item sales, Jushuitan trade-finish upload, WdtErp push, hfpay listener |
| `EVENT_NORMAL_ORDER_PAY_SUCCESS` | `event:288:OrdersBundle\Events\NormalOrderPaySuccessEvent` | bridge listener (only when the order actually flipped) | youshu, wsugc, **members new-customer** (`listener:members.normal_order_pay_success_new_customer`, registered `@DependsOn` the wsugc registration), **Shuyun open-platform trade.sync**, **Shuyun offline-benefit consume (result.push.v2 USED)**, employee-purchase behavior log |
| `EVENT_NORMAL_ORDER_ADD` | `event:orders:normal_order_add` | wxapp orchestrator after supplier split; shopadmin deferred pipeline **at materialization** (payment), not at create | youshu (`listener:youshu.orders_normal_order_add`), thirdparty marketing-center push (`listener:thirdparty.order_add_push_marketing_center_on_normal_order_add`) |
| `EVENT_NORMAL_ORDER_CANCEL` | `event:orders:normal_order_cancel` | full cancel + every auto-cancel batch (after commit) | youshu, **members new-customer** (`listener:members.normal_order_cancel_new_customer`), **Shuyun open-platform trade.sync** (`listener:shuyun.open_platform.trade_sync_on_normal_order_cancel`), **Shuyun offline-benefit consume (result.push.v2 NOT_USED)** (`listener:shuyun.open_platform.offline_benefit_consume_on_normal_order_cancel`), thirdparty schedule-cancel marketing-center push, orders auto-pass refund listener |
| `EVENT_NORMAL_ORDER_DELIVERY` / `EVENT_WX_ORDER_SHIPPING` | `event:orders:normal_order_delivery` / `event:orders:wx_order_shipping` | delivery services | youshu, thirdparty marketing-center push, DM-CRM push, **Shuyun open-platform trade.sync**, **DigiOS store-adjustment outbound** (`listener:digios.store_adjustment_on_normal_order_delivery`, 订单发货 → 门店库存调整单出站, registered `@Order(80)`) |
| `EVENT_NORMAL_ORDER_CONFIRM_RECEIPT` | `event:orders:normal_order_confirm_receipt` | confirm-receipt services | youshu, thirdparty marketing-center push, Shopex CRM sync (`sync_confirm_receipt_order`), **Shuyun open-platform trade.sync**, orders invoice finish listener (`listener:orders.listeners.OrderFinishInvoice`, sync, `@Order(96)`) |
| `EVENT_ORDER_PROCESS_LOG` | `event:orders:order_process_log` | every OPL publish port call | `OrderProcessLogDispatchListener` → `orders_process_log` insert |

## Jobs this bundle owns

| job name | queue | purpose |
| --- | --- | --- |
| `job:128:OrdersBundle\Jobs\FinishOrderJob` | slow | auto-confirm-receipt batch |
| `job:129:OrdersBundle\Jobs\ConsumptionOrderJob` | slow | member consumption aggregation |
| `job:118:OrdersBundle\Jobs\GenerateStatementsJob` | slow | one settlement window per merchant |
| `job:124:OrdersBundle\Jobs\InvoiceCreateJob` / `job:126:...InvoiceQueryJob` / `job:125:...InvoiceRedQueryJob` | invoice queue | fapiao lifecycle |
| `job:115:OrdersBundle\Jobs\SendPayOrdersRemindJob` | slow | unpaid-order reminder (scheduled at create) |
| `job:20:OrdersBundle\Jobs\InvoiceRedJob` | invoice | red-flush on refund |
| `job:66:OrdersBundle\Jobs\TradeRefundStatistics` | slow | refund statistics (published by `doRefund`) |

Handlers live in `dispatch/`; registration in bootstrap `*RegistrationConfig` classes.

## Invariants and failure semantics

* **Money is fen (int).** All fee fields are integer fen; currency-converted trades keep the original amount in `cur_pay_fee` and the converted one in `pay_fee`.
* **Idempotency via NOTPAY guards.** The notify path (`applyTradePaymentNotify`) re-reads and throws on any trade that is not `NOTPAY`; the internal finalize path is CAS-guarded (`WHERE trade_id=? AND trade_state='NOTPAY'`, 交易单状态更新失败 on a lost race) and re-publishes idempotently on an already-SUCCESS trade — repeated callbacks either see "更新已处理" or affect zero rows. The finalize-time `trade_no` is stable per order because the `h_trade_no_*` hash value for the order id wins over a fresh counter increment.
* **No external call inside a DB transaction.** `doPayment` keeps channel SDK/HTTP invocations between TX1 (`preparePaymentInTx`) and TX2 (`persistExternalPaymentInTx`), so a slow or failing channel never holds row locks on `trade`; local pay types never leave TX1 at all. The pay-success bridge's `order_status='NOTPAY'` guard likewise prevents double pay-success events; auto-cancel and confirm-receipt batches guard their status transitions the same way; the deferred refund path CAS-guards `SUCCESS→REFUND`.
* **Trade reuse.** `doPayment` never stacks pending trades for the same payment intent: an open NOTPAY trade (channel-keyed for adapay/bspay/offline_pay/paypal) is reused, and a cached `payment_params` payload short-circuits channel re-invocation.
* **Post-commit dispatch.** Trade-finish, normal-order-cancel, and OPL publishes are registered as `afterCommit` synchronizations and run inline when no transaction is active — listeners always observe committed state.
* **Trade-finish brokerage never throws back into the bus.** The ECX-11089 brokerage listener gate-skips empty payloads, invalid/non-positive `company_id`/`order_id`, `pay_type=point`, unknown orders, and company-mismatch rows before delegating to `NormalOrderBrokerageOnFinishService.orderFinishBrokerage`, and any `Throwable` from that call is logged at warn under `[popularize-trade-finish]` — a brokerage failure is invisible to the dispatch retry machinery.
* **Point deduction happens exactly once.** Pointsmall orders deduct at create; `normal_pointsmall` trades do not deduct again at payment; zero-cash mixed orders are forced to `pay_type=point` rather than `localPay`.
* **No charge without an order, no order without a charge.** The deferred pipeline only persists after `doPayment` reports `pay_status=true`; when persistence fails the charge is either refunded (business failure) or parked in `PAID_WAIT_ORDER`/`REFUND_FAILED` with a 24 h compensation window for query-driven recovery; supplier split and `normal_order_add` publish run exactly once, under the per-order Redis lock, only on the materialization path.
* **Bounded batches.** Every cron batch pages (20/page for cancels and finish, 500 for statement details, 1000 for merchant scans) and isolates per-order/per-merchant failures so one bad row never aborts a run.
* **ERP failures never break the transaction** — Jushuitan/WdtErp publishes are fire-and-forget bus messages with sync Spring-event mirrors.

## Extension points

* **New wxapp order type** — add a `WxappOrderTypeCreateStrategy` bean and a slug→bean-name case in `WxappOrderTypeRegistry.resolveBeanName`, plus a slug→(order_type, order_class) case in `OrderTypeSlugMapper`; reuse `WxappPhysicalNormalOrderCreateSupport` for anything that persists a `NormalOrders` row.
* **New payment channel** — add a branch in `OrdersPaymentDoPaymentService` (or extend `OrdersExternalPayParamBuildService`); channel refund executors register separately as `AftersalesRefundPayChannelExecutor` beans (see [Payments Domain](payments.md)). The deferred pipeline only accepts pay types added to `DEFERRED_PAY_TYPES` and extended in `executeChannelRefund`.
* **New trade-finish / pay-success listener** — implement a `DispatchListener` bean and register it in a bootstrap `*RegistrationConfig` against `EVENT_TRADE_FINISH` or `EVENT_NORMAL_ORDER_PAY_SUCCESS`; no orders change needed.
* **New outbound integration on order state change** — publisher interface in `ecshopx-common/dispatch`, impl in `ecshopx-bootstrap`, called from the service's post-commit block; mirror `JushuitanTradeFinishOrderUploadAssemblePortImpl` for payload assembly that stays in this bundle.
* **New local-delivery driver** — currently only `dada`/`shansong` are accepted by `LocalDeliveryBusinessReceiptService` (config `common.local-delivery-dirver`); adding one means a new receipt port plus widening that check.
* **New create-pipeline stage** — implement the port in the owning bundle (goods/distribution) and add the call in both `WxappNormalOrderCreateOrchestrator` and `ShopadminNormalOrderCreateOrchestrator`; the two pipelines are meant to stay stage-for-stage identical except for the persist step.

## Focused tests

* `ShopadminNormalOrderCreateOrchestratorNormalOrderAddDispatchPublishProbeTest` / `NormalOrderAdminCreateUserOrderServiceNormalOrderAddDispatchPublishProbeTest` — pin the deferred create: only `preparePricingSnapshot` + `saveWaitPay`, no persistence and no `normal_order_add` on create.
* `OrderCreatePersistencePortImplShopIdAlignPhpTest` — PHP parity of `persistOrder`: preassigned `order_id` reuse without calling `generate`, `shop_id` from orderData (never `distributor_id`) on order/item/association rows, freight point fields, company-default currency fallback, item spec desc.
* `OrderCreatePostCommitPortImplNormalOrderCreateUserOrderOrderProcessLogDispatchPublishProbeTest` + `OrderCreatePostCommitPortImplSendPayRemindTest` — the 订单创建 OPL envelope (operator_id from orderData user_id, full params map, excard remarks variant) and the pay-remind payload for the shopadmin create path.
* `OrdersPaymentDoPaymentServicePaymentSubjectTest` — payment-subject resolution feeds `distributorIdForSetting=0` into pay-config validation and the channel builder while the trade keeps the original distributor id; the TX1/TX2 transaction split (one `transactionTemplate.execute` for local `point`, two for `wxpayh5`) with the channel invocation asserted at transaction depth 0.
* `OrdersPaymentDoPaymentServiceWxappSyncTradeFinishPublishTest` — the publish-on-success contract: exactly one snake_case trade-row publish on the zero-fee wxpay (`pay_type` stamped `localPay`), point, `normal_groups` and sync-success wxpay paths, carrying the brokerage/item-sales, member-link, printer, fapiao, customs and work-wechat gate keys, plus the zero-cash-with-points rewrite to `pay_type=point`.
* `TradeFinishCountBrokerageEventSyncDispatchFlowTest` / `TradeFinishCountBrokerageWxappPaymentSyncDispatchFlowTest` (ecshopx-bootstrap) — the ECX-11089 brokerage listener dispatch envelope: async `default`-queue registration on `EVENT_TRADE_FINISH`, blank-`order_id` and `pay_type=point` payloads skipped before `TradeFinishCountBrokerageBusService.handleTradeFinishRow`, alipay/offline_pay payloads delegated exactly once.
* `OrdersTradeFinishNormalOrderPaySuccessApplyServiceTest` — NOTPAY→PAYED transition, supplier sync, empty-Optional on non-normal source types.
* `OrderDeliverySuccessNotifyWechatServiceTest` — the `orderDeliverySucc` payload envelope: recipient lookup short-circuit, order-title fallback for `item_name`, per-item `sep` payload preferring `item_name`/`logi_name`.
* `WxappUpdateCartCheckStatusServiceTest` — the local check-status mutation: one UPDATE with `cart_id IN`, the single-id overload delegating to the batch, empty list skipping the write.
* `NormalOrderCronServiceTest` + `NormalOrderCronServiceScheduleCancel*OrderProcessLogDispatchPublishProbeTest` — cancel-query shape, restore fan-out, OPL publish envelope for both cancel families.
* `GroupsOrderService*`, `SeckillNormalOrderService*`, `PointsmallNormalOrderService*` (each with a `ScheduleCancelOplDispatchPublishProbeTest`) — per-class cancel semantics.
* `NormalOrderCheckoutPointDeduct*`, `NormalOrderPointDeductFormatterPayTypePointTest`, `NormalOrderCreatePrePersistEnrichmentServiceAutoCancelTimeTest` — pricing stages shared by both create pipelines.
* `RightsFrontWxappGetRightsCodeOwnershipTest` / `RightsFrontWxappGetRightsDetailOwnershipTest` — the rights ownership guards: cross-member, cross-company, missing and malformed rights ids rejected before any Redis/barcode or decrypt side effect.
* `WxappOfflinePaymentUpdateVoucherOrderOwnershipTest` — voucher-to-order/tenant binding for the offline voucher update (cross-order and cross-company rejected with no UPDATE).
* `PosOrderDocAccessGuardTest` — POS write scope: manager any-in-store, cross-store forbidden, clerk own-handler only, unhandled (0) orders unwritable by clerks.
* `MemberCrowdMatcherTest` (ecshopx-promotions) — the include/exclude member-tag matching that the cart promotion gate and the seckill/full-crowd surfaces share.
* `StatementsServiceTest`, statement tests under `statement/` — window computation, cursor handling, platform-product-model filter.
* `OrderInvoiceServiceTest` — apply-node branching, stale inProgress threshold, red-flush query.
* The ~60 `integration/*OrderProcessLogEventAsyncConsumeTest` classes — async consume envelope for every OPL-emitting flow (create, cancel, delivery, writeoff, refund, offline payment, notify).
* Cron handler tests under `cron/testcfg` use the `test-cron` profile with `CronTestMockConfig` (no-op publishers/ports) so XXL-Job entrypoints stay testable without Redis.

## Related pages

* [Bundle Layering](../architecture/bundle-layering.md) — uses this Bundle as the worked example for the api/service/dispatch/cron/port/integration split.
* [Cross-Bundle Communication](../architecture/cross-bundle-communication.md) — why cart add/list/check-plus-item and the create-pipeline stages are ports implemented by goods/promotions/distribution.
* [Dispatch Bus](../concepts/dispatch-bus.md) — the transport under every publisher/listener above, including the OPL queue.
* [Scheduling (XXL-Job)](../concepts/scheduling-xxljob.md) — the executor wiring and seeded cron entries for all handlers listed here.
* [Persistence (MyBatis)](../concepts/persistence-mybatis.md) — the `@MpTable` metadata style of `Trade` and friends, and the string-typed tenant ids in `trade`.
* [Redis Databases](../concepts/redis-databases.md) — which logical database (shared vs companys) backs the deferred snapshot cache, the order-id generator, the fast-buy buckets, and the `shopadmin:deferred:` keys.
* [Aftersales Domain](aftersales.md) — the refund execution half (`doRefund`, `NormalOrderRefundCompleteJobBusinessService`, `left_aftersales_num`).
* [Payments Domain](payments.md) — payment-channel notify entry points and refund channel executors.
* [Goods Domain](goods.md) — implements the wxapp cart add/list ports and the order-create pipeline ports this bundle owns.
* [Marketing Domain](marketing.md) — the promotions bundle hosting `MemberCrowdMatcher` and the cart-promotion rules port described above.
* [Order Payment Flow](../workflows/order-payment-flow.md) — the member-side pay-an-existing-order path over the same `doPayment` core.
