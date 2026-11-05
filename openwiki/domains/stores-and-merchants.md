---
type: domain-reference
title: Stores, Distributors, Merchants, and Salespersons
description: The store-side and B2B2C Bundles — companies/shops/operators and product models (ecshopx-companys), the POS store account subsystem with import/export and a DigiOS create hook, distributors as physical stores with create/update after-commit fan-out and DigiOS sync listeners (ecshopx-distribution), promoter QR codes and first-level brokerage (ecshopx-popularize), merchant onboarding with FrontMerchantAuth JWT (ecshopx-merchant), WeCom salesperson tasks and commissions (ecshopx-salesperson), plus ecshopx-selfservice, ecshopx-reservation, ecshopx-community group-buy chief write-off, and the supplier_order split out of normal orders (ecshopx-supplier).
tags: [distributors, stores, companies, merchants, salesperson, popularize, community, supplier, dispatch-bus, payment-subject, distributor-items, onboarding, settlement, pos-store-account, digios]
sources:
  - id: openwiki-source-4460b2ae6ffdfa7f6511ddc1
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/CreateDistributorJobDispatchPublisherImpl.java
  - id: openwiki-source-1adeed0e9d51f13c65d98eb6
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsDistributorCreateDispatchListenerRegistrationConfig.java
  - id: openwiki-source-977cd9522d33d425335c4bc9
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsDistributorUpdateDispatchListenerRegistrationConfig.java
  - id: openwiki-source-5d5b8a9a439f1fe1b537f7b7
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DistributionAddEventDispatchPublisherImpl.java
  - id: openwiki-source-7ae0d3bc6703e766bf0c96e4
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DistributionEditEventDispatchPublisherImpl.java
  - id: openwiki-source-8f75a7cae7a69b0a9cd102a3
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DistributorCreateEventDispatchListenerRegistrationConfig.java
  - id: openwiki-source-0cd6e12e061b481f3af8b1a9
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DistributorCreateEventDispatchPublisherImpl.java
  - id: openwiki-source-480ef2b81b6b4c1d52c9cf13
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/EspierExportFileJobDispatchHandler.java
  - id: openwiki-source-d48a5c9efd004f70cd62e80b
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/HfpayDistributionAddEnterapplyDispatchListenerRegistrationConfig.java
  - id: openwiki-source-f3b71eb9d6873d40cbc6bb31
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/PosStoreAccountCreateEventDispatchPublisherImpl.java
  - id: openwiki-source-6d77f0b8aa3c2cb5cd92e0cf
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/PosStoreAccountExportDispatchPublisherImpl.java
  - id: openwiki-source-26ae01f007910ee5bdaa08d6
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformDistributorCreateDispatchListenerRegistrationConfig.java
  - id: openwiki-source-6ef3227b25a21e819e4fc01a
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ThirdPartyDistributionAddPushMarketingCenterDispatchListenerRegistrationConfig.java
  - id: openwiki-source-201046f4a1e3bd734f14eede
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/config/DistributionAddEventDispatchPublisherImplDualPublishFacadeProbeTest.java
  - id: openwiki-source-85785a30ce788dca4d2c8534
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/dispatch/CreateDistributorJobDispatchFlowTest.java
  - id: openwiki-source-3156261d8db1b6c2cce6096c
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/dispatch/DistributorCreateEventSyncBusDispatchFlowTest.java
  - id: openwiki-source-8c9ccbc017d82a99f7343031
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/dispatch/DistributorUpdateEventSyncBusDispatchFlowTest.java
  - id: openwiki-source-c0c2361e788fa42a7b16fbd2
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/CompanysDispatchEventNames.java
  - id: openwiki-source-ff3f271173ecc685e342fc54
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/DistributionDispatchEventNames.java
  - id: openwiki-source-05ab276ed18a53768f89eadd
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/DistributionDispatchJobNames.java
  - id: openwiki-source-4cece1d0dc21f12377ecd9c8
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/EspierDispatchJobNames.java
  - id: openwiki-source-f7687100d882f581d013877d
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/MerchantDispatchJobNames.java
  - id: openwiki-source-8042ebe9fde9d02083a2cbd9
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/SalespersonDispatchJobNames.java
  - id: openwiki-source-e4c74586aedc9141bacf93b9
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/port/digios/PosStoreAccountDigiOsCreatePort.java
  - id: openwiki-source-9bceb1316bcda54f295cedd8
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/port/digios/PosStoreAccountDigiOsCreatePortNoOp.java
  - id: openwiki-source-2393966349c071caeee8dce3
    resource: repo://ecshopx-community/src/main/java/cn/shopex/ecshopx/community/package-info.java
  - id: openwiki-source-be10fe503b04d52d483b5158
    resource: repo://ecshopx-community/src/main/java/cn/shopex/ecshopx/community/service/chief/CommunityChiefQrWriteoffService.java
  - id: openwiki-source-9d44d60aa3db9b37155e8c0a
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/admin/v1/PosStoreAccountController.java
  - id: openwiki-source-82792828c2a574492be2e644
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/domain/PosStoreAccount.java
  - id: openwiki-source-86bf8cc5e41d85dd55ca5179
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/package-info.java
  - id: openwiki-source-0ced4eb2aefb8260715e3891
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/espier/PosStoreAccountEspierImportRowSink.java
  - id: openwiki-source-c6d26c34e8d6b81c0e515e09
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/merchant/CompanysMerchantMainOperatorProvisioner.java
  - id: openwiki-source-4fbd0011a9965297c75e16bc
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountExportService.java
  - id: openwiki-source-ebc91c9c020dd1a99e042eaf
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountImportService.java
  - id: openwiki-source-c93a16c79c2c26a1e8ad9294
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountPasswordGenerator.java
  - id: openwiki-source-e5b262761a01b14dbdecee27
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountPasswordService.java
  - id: openwiki-source-c648c2a32ec3b8c2d6266da6
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountQueryService.java
  - id: openwiki-source-4d2e3df995091c6cac425262
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountScope.java
  - id: openwiki-source-7c170152328dcd04be3f765d
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountStatusService.java
  - id: openwiki-source-26499232cc5422538ecc24fc
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posaccount/PosStoreAccountWriteService.java
  - id: openwiki-source-5f03676cc4237886d836c758
    resource: repo://ecshopx-digios/src/main/java/cn/shopex/ecshopx/digios/dispatch/DistributorCreateDigiOsSyncDispatchListener.java
  - id: openwiki-source-5db0356764f92b4d36d49de8
    resource: repo://ecshopx-digios/src/main/java/cn/shopex/ecshopx/digios/port/PosStoreAccountDigiOsCreatePortImpl.java
  - id: openwiki-source-667d6bbe173e5c7c06ad3e26
    resource: repo://ecshopx-digios/src/main/java/cn/shopex/ecshopx/digios/service/DigiOsDistributorSyncDispatchService.java
  - id: openwiki-source-70ea0fc2572e5eb9bc37ed15
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/DispatchOptions.java
  - id: openwiki-source-260f163fd9cabc7c65a6abc5
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/api/admin/v1/DistributorController.java
  - id: openwiki-source-813e655515e392fffe017eba
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/openapi/thirdapi/v2/OpenapiThirdApiV2DistributorCreateService.java
  - id: openwiki-source-185d0c6ba3c09b54d8f8dd26
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/package-info.java
  - id: openwiki-source-172b2a52ae139f0209f376ad
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorCreateOrchestrator.java
  - id: openwiki-source-dba0633f76a3c4a3a2b6eb5f
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorCreateParamNormalizer.java
  - id: openwiki-source-d8034efe092c20e18d05b0ff
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorCreateService.java
  - id: openwiki-source-bfa064df2a75a39b4d51d402
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorPaymentSubjectService.java
  - id: openwiki-source-7cee526b2344c23fd2c07034
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorUpdateOrchestrator.java
  - id: openwiki-source-005cd2478c5bdca6ba8ecc85
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorUpdateParamNormalizer.java
  - id: openwiki-source-ef78e2d739ca7f975c8382ae
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/DistributorUpdateService.java
  - id: openwiki-source-3972cfab29a2ffbd956e5d12
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/service/PagesTemplateNewDistributorFacade.java
  - id: openwiki-source-702bcaecd507ee9ebfb6e9cf
    resource: repo://ecshopx-distribution/src/test/java/cn/shopex/ecshopx/distribution/service/DistributorCreateOrchestratorDistributionAddDispatchPublishProbeTest.java
  - id: openwiki-source-4a7d8173b45e0b7bd344640b
    resource: repo://ecshopx-distribution/src/test/java/cn/shopex/ecshopx/distribution/service/DistributorUpdateOrchestratorDistributionEditDispatchPublishProbeTest.java
  - id: openwiki-source-20bea0951bab22debabf05ca
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/dispatch/PosStoreAccountExportFileJobTypeHandler.java
  - id: openwiki-source-dec800e8f40f4157f34897b2
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/service/upload/EspierUploadHandlerBeans.java
  - id: openwiki-source-2ebfe908466d4b6d1e59c80c
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/service/upload/EspierUploadHeaderCatalog.java
  - id: openwiki-source-c47b0102b62dec8fc0d98f93
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/DistributorGoodsSyncService.java
  - id: openwiki-source-7e78af38c8a63b7842f49ac8
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/config/security/FrontMerchantAuthInterceptor.java
  - id: openwiki-source-5535fc73ece87960d8767e8e
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/config/security/MerchantWxappJwtAuthenticationFilter.java
  - id: openwiki-source-78fa646850248542c2669960
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/package-info.java
  - id: openwiki-source-46632ae4971ac6450ff04117
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/service/MerchantSettlementApplyAuditService.java
  - id: openwiki-source-465f76e0ed8d9fa273c2687e
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/service/wxapp/MerchantSettlementApplyProgressService.java
  - id: openwiki-source-169b78b34e418b695e3a3eb2
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/service/wxapp/MerchantWxappLoginService.java
  - id: openwiki-source-6ffb4137d9207197034001d7
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/service/wxapp/MerchantWxappSettlementApplySaveService.java
  - id: openwiki-source-11c92ab35f4ce1f9788dbd95
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/WxappNormalOrderCreateOrchestrator.java
  - id: openwiki-source-ab420cef928add1ab3268149
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/supplier/service/SupplierOrderSplitOnNormalOrderAddService.java
  - id: openwiki-source-eac2d38af5c5bab525237449
    resource: repo://ecshopx-popularize/src/main/java/cn/shopex/ecshopx/popularize/package-info.java
  - id: openwiki-source-027ba59abcf47758829bb66e
    resource: repo://ecshopx-popularize/src/main/java/cn/shopex/ecshopx/popularize/service/brokerage/NormalOrderFinishBrokerageCoordinator.java
  - id: openwiki-source-da77f35495f81b4cc4a1331b
    resource: repo://ecshopx-popularize/src/main/java/cn/shopex/ecshopx/popularize/service/PromoterFrontGetPromoterQrcodeService.java
  - id: openwiki-source-288454d0e854236a24e79d5c
    resource: repo://ecshopx-popularize/src/main/java/cn/shopex/ecshopx/popularize/service/PromoterFrontNewQrcodePngService.java
  - id: openwiki-source-f31cc454394a8b4c10d49a3a
    resource: repo://ecshopx-reservation/src/main/java/cn/shopex/ecshopx/reservation/package-info.java
  - id: openwiki-source-7544a54376d5013f054faa68
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/api/admin/v1/SalespersonTaskController.java
  - id: openwiki-source-d34334b99e89fdc8ac8c3874
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/dispatch/SalespersonTaskJobHandler.java
  - id: openwiki-source-aa05dafee07f9040cb3735ac
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/package-info.java
  - id: openwiki-source-43e1427600d6092d7275135b
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/service/SalespersonTaskCreateService.java
  - id: openwiki-source-e85f4b516d751504331c6cfb
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/service/WxappSalespersonSessionAuthService.java
  - id: openwiki-source-10687baf99ef7873b3e5d2ff
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/web/QywxSalespersonAuthInterceptor.java
  - id: openwiki-source-a34636f6657202e2d8a079f3
    resource: repo://ecshopx-selfservice/src/main/java/cn/shopex/ecshopx/selfservice/package-info.java
  - id: openwiki-source-5895edaca96277d53a43148a
    resource: repo://ecshopx-supplier/src/main/java/cn/shopex/ecshopx/supplier/package-info.java
  - id: openwiki-source-3619796531667b6781e35a09
    resource: repo://ecshopx-theme/src/main/java/cn/shopex/ecshopx/theme/dispatch/CreateDistributorJobHandler.java
generated: { by: "openwiki/0.7.3", at: "2026-11-04T17:09:55.577Z" }
verified:
  - by: openwiki/0.7.3
    at: 2026-11-04T17:09:55.577Z
---

# Stores, Distributors, Merchants, and Salespersons

This page groups the store-side and B2B2C Bundles of the ecshopx reactor. Each Bundle's `package-info.java` states its domain in Chinese, and those statements are the authoritative scope definitions: `ecshopx-companys` is `企业与多门店域` (companies and multi-store), `ecshopx-distribution` is `分销与推广域` (distribution and promotion), `ecshopx-popularize` is `分销推广域模块` (distribution-promotion module), `ecshopx-merchant` is `商户入驻与设置域` (merchant onboarding and settings), `ecshopx-salesperson` is `业务员/导购域模块` (salesperson / shopping-guide), `ecshopx-supplier` is `供应商域` (suppliers), `ecshopx-community` is `社区与互动域` (community and interaction), `ecshopx-reservation` is `预约域模块` (reservations), and `ecshopx-selfservice` is `自助服务域模块` (self-service).

**Terminology first, because it is the most common source of confusion.** A **distributor** (`分销商` in the code, but in practice 门店, a *physical store*) is one row in `distribution_distributor` — the pickup point / delivery-capable shop that a member selects at checkout. It is *not* a reseller in the western sense. Two Bundles split the "distribution" namespace: `ecshopx-distribution` owns the **store** (门店) concepts — distributor CRUD, geofences, whitelists, delivery capability, payment subject, aftersales addresses — while `ecshopx-popularize` owns the **promoter** (推广员/分销员) concepts — the member-turned-referrer with QR codes and brokerage rows. Product-model scoping (which shop types exist for a company) comes from `companys.menu_type` in `ecshopx-companys`.

The store-side half of the goods contract also lives here conceptually: `distribution_distributor_items` is the per-shop copy of the catalog, but **the sync that populates it is executed by the goods bundle** — `DistributorGoodsSyncService.syncGoods` and `job:62 AddDistributorItems` — see [Goods Domain](goods.md) for that flow.

## The Distributor state model

`Distributor` (`distribution_distributor`, ~60 columns) is the center of gravity. The load-bearing columns:

| column | meaning |
| --- | --- |
| `distributor_self` | `1` marks the HQ self-pickup store (总部自提); **singleton per company** — creating a second one throws `总部自提店铺已存在` |
| `is_distributor` | main-shop flag, defaults true; distinguishes the "main store" from auxiliaries |
| `is_valid` | string soft-delete axis: `"true"` live, `"delete"` soft-deleted (never a physical delete) |
| `is_default` | the company's default store for member-less contexts; first non-wxapp-sourced store becomes default automatically |
| `auto_sync_goods` | goods fan-out flag: shops with `true` receive every new/edited SKU copy via `syncGoods` |
| `source_from` | `1` admin, `2` wxapp self-registered, `3` openapi — source-2 stores never become `is_default` |
| `merchant_id` | owning merchant (商户) for multi-merchant malls |
| `distribution_type` | `0` 自营 (self-operated), `1` 加盟 (franchise) |
| `payment_subject` | `0` platform collects, `1` shop collects (HfPay ledger split) — see below |
| `is_open` + `rate` | HfPay split-payment switch and platform rate (basis points) |
| `wdt_shop_no` / `wdt_shop_id` | WDT ERP shop binding (uniqueness-enforced per company) |
| `jst_shop_id` | Jushuitan shop binding (uniqueness-enforced per company) |
| `is_dada` / `dada_shop_create` / `shansong_shop_create` / `shansong_store_id` | local-delivery couriers bound to the store |
| `offline_aftersales*` | offline aftersales policy: self-handled, other-shops (same merchant only), or other |
| `is_audit_goods` | when flipped `true` → `false`, every shop-scoped item is auto-approved (see update flow) |

Uniqueness guards per company (all application-level, checked in `DistributorCreateService.performInsertAndEvents`): `name`, `shop_code`, `mobile`, `wdt_shop_no`, `jst_shop_id`, and the `distributor_self` singleton.

## Entrypoints

### Admin REST (`/api/v1`, `@AdminAuth`-style operator JWT)

`DistributorController` (~1,600 lines) is the hub; controller order is consistently gate → normalizer → validator → orchestrator, plus an operator-log write:

| route | purpose |
| --- | --- |
| `POST /distributor` | create store (route alias `distributor.create`) |
| `PUT /distributor/{id}` | update store (`distributor.update`) |
| `PUT /distributor/{id}/payment-subject` | set payment subject (`distributor.payment.subject.set`) — sealed `PaymentSubjectParse` reproduces legacy error semantics (below) |
| `GET /distributors`, `/distributors/info`, `/distributor/easylist`, `/distributor/count/{id}` | lists, detail, simplified list, per-store statistics |
| `GET /distributor/wxacode`, `GET /distributor/getShop` | shop wxapp QR code, valid-store list |
| `GET/POST/DELETE /distributor/items`, `GET /distributor/items/export`, `PUT /distributors/item` | the shop↔SKU binding surface (`distribution_distributor_items`): list, add, delete (cascades `goods_can_sale`), export, per-shop price/store override |
| `POST /distributor/default` | re-point the default store |
| `GET /distributors/aftersales` | stores selectable as aftersales targets (same-merchant constraint) |
| `GET/POST /distribution/getdistance` / `setdistance`, `/distributor/list/background` | distance config, store-list background images |

Sibling admin controllers: `DistributorAftersalesAddressController`, `DistributorCategoryController`, `DistributorGeofenceController` (Amap polygon, ≥3 vertices), `DistributorTagsController`, `DistributorWhiteListController`, `DistributorSalespersonRoleController`, `PickupLocationController`, `CashWithdrawalController` (withdrawal 1–800元 limits), `SelfDeliveryController`, `BasicConfigController`, `DistributionConfigController`.

The **legacy PHP route `POST …/v1/shops`** (referenced by the orchestrator Javadoc as `DistributorShopController#createShops`, no longer a Java controller of its own) is converged onto the same `DistributorCreateOrchestrator` — the `entry03_shopsCreateRoute_*` test in `DistributorCreateOrchestratorDistributionAddDispatchPublishProbeTest` asserts that the legacy route name shares this orchestrator's `afterCommit` publish path with `POST /api/v1/distributor`.

A **third create entrypoint** bypasses the orchestrator entirely: `OpenapiThirdApiV2DistributorCreateService.executeOpenapiCreate` (openapi thirdapi v2) inserts the row directly in a `TransactionTemplate` with `source_from=3`, `is_valid="false"`, `review_status=true`, and encrypted contact/mobile — then its `afterCommit` enqueues `job:63`, the dual `DistributionAdd` publish, *and* `DistributorCreateEvent` (event:263). So all three entrypoints end with the same fan-out shape even though only two share the orchestrator.

### Front wxapp (`/api/v1/h5app/wxapp/distributor/*`)

A read-mostly surface for the member-facing store picker: `list`, `alllist`, `get_all_distributor`, `self` (HQ self-pickup store), `default`, `count`, `is_valid`, `getDistributorInfo`, `aftersaleaddress`, `aftersaleslocation` (nearby return points), `whitelistByMember`, `checkUserInWhite`, `deliverytype`, `pickuplocation`, `salesperson/qrcode` (the store guide's QR code), `category/list`, `getAreaByAddress`/`getAreaByJwd` (Amap reverse geocoding), `config/inRule` (+ `POST config/inRule/check`, the `DistributionStoreEntryRuleRedisService` gate), and the shop-screen `advertisements`/`slider`; plus `CashWithdrawalController` for shop-side withdrawal.

### Merchant wxapp (`/api/v1/h5app/wxapp/merchant`)

`MerchantController` (front): `POST /login`, `GET /basesetting` (`@FrontNoAuth`), `GET /settlementapply/step`, `POST /settlementapply/{step}` (progress resolver: none → 1, `settled_type` → 2, `merchant_name` → 3, `license_url` → 4/awaiting audit; only steps 1–3 persist — the save switch rejects anything else with `入驻信息填写步骤错误`), `GET /settlementapply/detail`, `GET /settlementapply/auditstatus`, `GET /type/list`, `POST /password/reset` — all under `@DingoResponse`, with `@FrontMerchantAuth` guarding everything except `login`/`basesetting`.

### Salesperson WeCom wxapp

`QywxSalespersonAuthInterceptor` (a `HandlerInterceptor` honoring the `@QywxSalespersonAuth` annotation) guards the guide-facing endpoints; auth is header-session based, not JWT (below).

### Dispatch jobs

| job name | handler | purpose |
| --- | --- | --- |
| `job:63:ThemeBundle\Jobs\CreateDistributorJob` | `CreateDistributorJobHandler` (ecshopx-theme) → `PagesTemplateNewDistributorFacade` | bind default storefront page templates for the new store (slow queue) |
| `job:1..4:MerchantBundle\Jobs\*Notice` | merchant notice handlers | audit success/fail, enter success, password reset |
| `job:45:SalespersonBundle\Jobs\SalespersonTask` | `SalespersonTaskJobHandler` | task complete → marketing-center push (slow queue) |
| `job:46:SalespersonBundle\Jobs\SalespersonRelationshipContinuity` | continuity handler | member↔guide relationship bookkeeping |
| `job:138:SalespersonBundle\Jobs\SalespersonItemsShelvesJob` | shelves handler | guide-visible shelf recomputation; `job:139`–`149` are aliases of the same bus message name |
| `job:62:DistributionBundle\Jobs\AddDistributorItems` | goods bundle | the store-side catalog fan-out (see [Goods Domain](goods.md)) |

## The create flow and its after-commit fan-out

```mermaid
sequenceDiagram
    participant C as DistributorController or legacy ShopsController
    participant ORCH as DistributorCreateOrchestrator
    participant SVC as DistributorCreateService
    participant BUS as Dispatch bus
    participant JOB as CreateDistributorJob job:63
    participant OME as SystemLink ShopCreateSendOme
    participant SY as Shuyun shop sync
    participant DGY as DigiOS store sync
    participant MC as marketing center
    participant YS as youshu
    participant HF as HfPay enterapply

    C->>ORCH: create(merged, user, lang) in a transaction
    ORCH->>ORCH: duplicate name and shop_code, HfPay ledger assert, Dada create, WDT and JST checks
    ORCH->>SVC: performInsertAndEvents
    SVC->>SVC: uniqueness guards, distributor_self singleton, first default store
    SVC->>BUS: DistributorCreateEvent event:263 SYNC in transaction
    BUS->>OME: ShopCreateSendOme runs inline before commit
    BUS->>SY: Shuyun shop sync enqueued
    BUS->>DGY: DigiOS profile plus warehouse sync enqueued
    SVC-->>ORCH: inserted API row snapshot
    ORCH->>ORCH: pickup location rels and aftersales address
    Note over ORCH: afterCommit
    ORCH->>JOB: enqueue slow-queue job:63 with row snapshot
    ORCH->>BUS: DistributionAddEvent event:281 ASYNC REDIS
    ORCH->>BUS: DistributionAddEvent event:289 ASYNC REDIS
    BUS->>MC: push shop (async default queue)
    BUS->>YS: push shop (async default queue)
    BUS->>HF: enterapply init (async default queue)
```

Caption: the store create pipeline — one SYNC in-transaction event for OME/Shuyun, then after commit the theme template job runs first and the dual CSV DistributionAdd publishes fan out to marketing center, youshu, and HfPay.

1. **Pre-persistence validations** (`DistributorCreateOrchestrator.create`, `@Transactional`): duplicate name (`店铺名称已存在`) and `shop_code`; `HfpayLedgerConfigReadService.assertOpenAndRateAllowedIfNeeded` when split payment is requested; `LocalDeliveryShopCreateClient.applyCreateShopIfDada` registers the store with Dada; WDT (`assertShopExistsIfWdtNo`) and Jushuitan (`assertEnabledIfJstIdPositive`) bindings are checked against the ERP.
2. **Insert + in-tx event** (`DistributorCreateService.performInsertAndEvents`): mobile format (or the WeCom synthetic `d-d-d` mobile), the six uniqueness guards, `distributor_self` singleton, and `needDefault = sourceFrom != 2 && !existsDefaultDistributor(companyId)` — the first non-wxapp store becomes the default. The row is inserted, multi-language shards are written, and `DistributorCreateEvent` (`event:263:DistributionBundle\Events\DistributorCreateEvent`) is published **inside the transaction in SYNC mode** (`DispatchOptions.eventDefaults()` is `DispatchMode.SYNC`) so the OME listener runs inline before commit. Its registered listeners: `listener:SystemLinkBundle\Listeners\ShopCreateSendOme` (SYNC), `listener:shuyun.open_platform.shop_sync_on_distributor_create` (async default queue), and `listener:digios.distributor_sync_on_distributor_create` (async default queue, store-profile PUT + warehouse creation — see the DigiOS section after the update flow).
3. **Side tables.** Pickup-location relations from `pickup_location`, and the offline aftersales address — whose product-model slug comes from `companys.menu_type` via `CompanyMenuTypeProductModel.toSlug` (null-safe default `platform`). (The merged params reach this point only after the create-plane normalizer described below has run.)
4. **afterCommit ordering.** `TransactionSynchronizationManager.registerSynchronization` enqueues `job:63` (theme templates, **slow queue**, ASYNC REDIS) *before* calling `distributionAddEventDispatchPublisher.publish(rowSnapshot)`. The publisher (`DistributionAddEventDispatchPublisherImpl` in bootstrap) publishes the **same payload twice** — `event:281` and `event:289` — both ASYNC REDIS. This is the deliberate CSV-plane split: `event:281` carries the third-party marketing-center push + youshu listeners, `event:289` carries the HfPay `HfEnterapplyInit` listener. The ordering (job before events) and the dual publish are pinned by `DistributorCreateOrchestratorDistributionAddDispatchPublishProbeTest` and `DistributionAddEventDispatchPublisherImplDualPublishFacadeProbeTest`.

## The update flow

`DistributorUpdateOrchestrator.update` mirrors the create shape: optional HfPay ledger assert when `is_open` goes true, datapass-blocked column refill (`mobile`/`contact` re-read from the DB row), name/shop_code duplicate check, Dada update, WDT/JST checks, and the same-merchant constraint on `offline_aftersales_distributor_id` (`售后门店只能选择同一商户下的其他门店`).

`DistributorUpdateService.performUpdateAndEvents` then: re-defaults (`is_default=1` forced when no default exists), validates mobile, enforces the update-side WDT/Jushuitan uniqueness guards (`旺店通门店已绑定其他店铺` / `聚水潭店铺已绑定其他店铺`, excluding the store itself), and — notably — when `is_audit_goods` flips from `true` to a falsy value it bulk-approves every shop-scoped item via `ItemsAuditByDistributorJdbcRepository.updateAuditStatusApprovedByDistributorId` (disabling item audit must not leave previously-submitted items stuck). Only whitelisted `COLUMN_KEYS` (61 column names) are applied via per-column setters (typed coercion, plus `updated` and recomputed `first_letter` on name changes); everything else in the merged params is ignored.

### The param normalizers ahead of create/update

Between the request gate and the validator sits a normalizer on each plane — `DistributorCreateParamNormalizer` and `DistributorUpdateParamNormalizer` — mutating the merged map before persistence:

* **Operator-scoping**: `merchant` operators are pinned to their own `merchant_id` with `distribution_type="1"` (franchise); `distributor` operators have `is_audit_goods` and `auto_sync_goods` stripped so a store admin cannot change its own audit/sync policy. On create, a merchant operator without a distribution type is reset to `merchant_id=0`.
* **Ledger rate scaling**: when `is_open` is truthy, `rate` is converted from a percent to basis points (×100, HALF_UP) and must land in 0–3000 (`平台服务费率超出允许范围`) — the stored `rate` column is therefore basis points.
* **Offline aftersales**: `offline_aftersales` cascades to `offline_aftersales_self`; the aftersales store id list is joined to a CSV string. On **create** every id is DB-validated to belong to the same merchant (`售后店铺须与当前商户一致`); on **update** the presence of the path store in the list derives `offline_aftersales_self`.
* **Misc**: 2-element `hour` lists become the `"a - b"` display string; `regions` lists back-fill `province`/`city`/`area` (a 2-element list clears `area`); `"false"` `is_dada` becomes the empty string (legacy tri-state); tri-bool string flags (`is_ziti`, `is_delivery`, …) coerce to booleans; on update `distributor_self=1` forces `is_valid="true"`, `review_result` folds into `is_valid` + `review_status`, and `open_divided` normalizes to 0/1.

afterCommit publishes **DistributionEdit first, DistributorUpdate second**:

* `event:282` + `event:290` (`DistributionEditEvent`, dual CSV publish, ASYNC REDIS) → listeners: third-party marketing-center push + youshu (282), HfPay `HfEnterapplyInit@edit` (290).
* `event:264` (`DistributorUpdateEvent`) → published **SYNC** (`DispatchOptions.eventDefaults()`) → listeners: `listener:SystemLinkBundle\Listeners\ShopUpdateSendOme` (SYNC), `listener:shuyun.open_platform.shop_sync_on_distributor_update` (async), and `listener:digios.distributor_sync_on_distributor_update` (async default queue) — see DigiOS below.

### DigiOS store-profile sync on distributor create/update

Two bootstrap registration configs attach DigiOS listeners to the same store-lifecycle events: `DigiOsDistributorCreateDispatchListenerRegistrationConfig` (event:263 → `listener:digios.distributor_sync_on_distributor_create`) and `DigiOsDistributorUpdateDispatchListenerRegistrationConfig` (event:264 → `listener:digios.distributor_sync_on_distributor_update`), both ASYNC on the default queue. The listeners (`DistributorCreateDigiOsSyncDispatchListener` / `DistributorUpdateDigiOsSyncDispatchListener` in `ecshopx-digios`) extract `company_id` + `distributor_id` from the payload `entities` and delegate to `DigiOsDistributorSyncDispatchService`, which **skips silently unless the company's DigiOS config is enabled**, reloads the persisted distributor row, and calls `DigiOsDistributorSyncService.syncOnCreate` (store-profile PUT + warehouse creation) / `syncOnUpdate` (store-profile POST). The gateway, token, and payload mapping live in [Third-Party Integrations](../integrations/third-party.md); the key contract here is that the distribution bundle needed no change — DigiOS is purely a registered listener.

Pinned by `DistributorUpdateOrchestratorDistributionEditDispatchPublishProbeTest`, `DistributionEditEventDispatchPublisherImplDualPublishFacadeProbeTest`, and `DistributorUpdateEventSyncBusDispatchFlowTest`.

### Payment subject: legacy error semantics preserved

`PUT /distributor/{id}/payment-subject` parses `payment_subject` through a **sealed interface** `PaymentSubjectParse` with three outcomes: `Valid(0|1)`, `Invalid` (returns Dingo-style `payment_subject参数错误，只能为0（平台）或1（店铺）` with status 422), and `SqlBug` — the case where a legacy PHP/MySQL write would have failed with `SQLSTATE[HY000]: General error: 1366 Incorrect integer value: '' for column 'payment_subject'`, which the Java port deliberately reproduces (HTTP 200 with Dingo `message`/`status_code` 500 in the body, matching the old client contract).

The service behind it, `DistributorPaymentSubjectService.setPaymentSubject`, refuses to switch while the store has unfinished refunds (`有未完成的退款单，不允许切换收款主体`), and switching **to** shop-subject (`1`) requires at least one open payment channel (WeChat Pay or Alipay) *and* complete per-shop channel configuration for each open channel; switching to platform (`0`) instead validates the platform (distributor 0) channel config. The `PaymentSubjectDistributorIdPortImpl` in this bundle exposes the current subject to the payment and orders bundles through a common port.

## POS store accounts (ecshopx-companys)

`pos_store_account` rows are the per-store **POS terminal accounts** — the login identities of the physical shop's staff (店长 store_manager / 主管 supervisor / 店员 clerk), distinct from both the back-office `operators` and the WeCom `shop_salesperson`. The table carries a **globally unique** `employee_no` (`uk_employee_no`, case-insensitive under `utf8mb4_unicode_ci`), a reversible `password_cipher`, and `status` in `enabled` / `disabled` / `resigned`. Indexes are `(company_id, distributor_id, status)` and `(company_id, role)`.

### Admin surface and the operator-shape scope

`PosStoreAccountController` (`/api/v1/store/pos-accounts`, `@AdminAuth` operator JWT, route alias `store.pos_account` for both `@DataPass` and `@Activated`) exposes list/detail, create, update, reset-password, single + batch status change, role change, shop transfer (`/{id}/shop`), and export. Reads reuse the standard activation middleware: the operator's JWT `distributor_ids` drive `PosStoreAccountScope` — non-`admin` operators see and may touch only accounts of their own stores (`无访问权限`), and list responses include per-status counts.

Field rules live in `PosStoreAccountRules`: `employee_no` is required, ≤30 chars, alphanumerics only (it doubles as the DigiOS `acc`); `name` is Chinese/letters/digits ≤30; email and mobile optional but format-checked; role must be one of the three slugs. The write service additionally supports **optimistic concurrency** — a stale `updated` in the update body is rejected with `数据已变更，请刷新后重试` — and the store must exist within the company (`请选择调入门店`).

### Status lifecycle: resign is one-way

`PosStoreAccountStatusService.change` / `batch` map actions `enable` / `disable` / `resign` onto statuses; the batch variant returns `{success, skipped, failed}` (no-op targets are skipped, per-id failures collected). The one hard invariant: **`resigned` is terminal** — a resigned account can never transition back (`离职账号不可恢复`), and role change, shop transfer, and password reset all refuse resigned accounts.

### Passwords: generated, encrypted, exportable

There is no operator-chosen password. `PosStoreAccountPasswordGenerator` builds an 8-char password guaranteed to contain ≥1 letter and ≥1 digit (`SecureRandom` + Fisher–Yates shuffle); `PosStoreAccountPasswordService` encrypts it with the common `SensitiveFieldEncryptor` and stores only the cipher. The plaintext surfaces exactly twice: in the create response and in the reset-password response (`POST /{id}/reset-password`). The CSV export **decrypts** passwords into the file — which is why export is gated to super-admins only (`仅超级管理员可导出账号`).

### Create: DigiOS pre-sync, insert, after-commit event

<!-- openwiki: mermaid parse failed and this diagram was converted to a text fence so it does not break rendering. Fix the diagram source and restore the mermaid fence. Parser error: Parse error on line 13: ...adRequest on failure W->>DB: insert Expecting '()', 'SOLID_OPEN_ARROW', 'DOTTED_OPEN_ARROW', 'SOLID_ARROW', 'SOLID_ARROW_TOP', 'SOLID_ARROW_BOTTOM', 'STICK_ARROW_TOP', 'STICK_ARROW_BOTTOM', 'SOLID_ARROW_TOP_DOTTED', 'SOLID_ARROW_BOTTOM_DOTTED', 'STICK_ARROW_TOP_DOTTED', 'STICK_ARROW_BOTTOM_DOTTED', 'SOLID_ARROW_TOP_REVERSE', 'SOLID_ARROW_BOTTOM_REVERSE', 'STICK_ARROW_TOP -->
```text
sequenceDiagram
    participant OP as operator
    participant C as PosStoreAccountController
    participant W as PosStoreAccountWriteService
    participant DG as PosStoreAccountDigiOsCreatePort
    participant DB as pos_store_account
    participant BUS as Dispatch bus

    OP->>C: POST /api/v1/store/pos-accounts
    C->>W: create(jwt, body)
    W->>W: scope + rules + employee_no uniqueness
    W->>DG: ensureSyncedOnCreate(companyId, employeeNo, ...)
    DG-->>W: no-op when DigiOS disabled; BadRequest on failure
    W->>DB: insert row with encrypted generated password
    Note over W,BUS: afterCommit
    W->>BUS: event:companys:pos_store_account_create (SYNC)
    C-->>OP: public row + plaintext password
```

`PosStoreAccountWriteService.create` runs the common-port hook **`PosStoreAccountDigiOsCreatePort.ensureSyncedOnCreate` before the local insert**: when the company's DigiOS integration is enabled, the `@Primary` implementation in `ecshopx-digios` upserts the account over the gateway (`account.upsert`, mobile mandatory) and any failure aborts the local create with `DigiOS同步失败：…`; when disabled (or running without the digios bundle) the `PosStoreAccountDigiOsCreatePortNoOp` default makes it a pass-through. The import path applies the same gate to both inserts and updates. After a successful insert, `event:companys:pos_store_account_create` (`{id, company_id, employee_no}`) is published **after commit** — immediately when no transaction synchronization is active — through `PosStoreAccountCreateEventDispatchPublisherImpl` in bootstrap. (`CompanysDispatchEventNames` reserves `listener:digios.account_sync_on_pos_store_account_create` for this event.)

### Import via espier, export via dispatch job

* **Import.** The espier upload plane registers file type `pos_store_account` (`EspierUploadHandlerBeans`, Chinese headers 姓名/员工号/邮箱/手机号/角色/门店/状态 mapped by `EspierUploadHeaderCatalog`) and routes each row to `PosStoreAccountEspierImportRowSink` → `PosStoreAccountImportService.acceptRow`. Import rows are stricter than the form: name/email/mobile required, Chinese role/status labels mapped to slugs, the shop resolved by `shop_code`. An account with an existing `employee_no` is **updated** (name/email/mobile/role/shop/status), otherwise inserted with a generated password; an in-memory `acceptedKeys` set rejects duplicate employee numbers within one file, and rows removed on failure so a retry can re-attempt them. Scope checks are rebuilt from the importing operator's `operator_type` + `distributor_ids`.
* **Export.** `POST /export` enqueues **`job:10 EspierBundle\Jobs\ExportFileJob`** (ASYNC REDIS, **slow queue**) with `type=pos_store_account` via `PosStoreAccountExportDispatchPublisherImpl`. The generic `EspierExportFileJobDispatchHandler` in bootstrap routes by `type` to `PosStoreAccountExportFileJobTypeHandler` in `ecshopx-espier`, which rebuilds the scoped query, decrypts passwords, writes the CSV (门店编码/门店 joined per row), and records the export-log entry the admin downloads from.

`PosStoreAccountQueryService.mapOrderInfoByIds` is the cross-bundle read: order list/detail code batches in POS handler info (`id`, `employee_no`, `name`, `mobile`, `role`, `distributor_id`, `status` — never the password) as the order-side `pos_store_account_info` object.

## Merchants: apply → audit → account

```mermaid
sequenceDiagram
    participant M as merchant wxapp user
    participant MC as MerchantController front
    participant L as MerchantWxappLoginService
    participant F as Jwt filter plus interceptor
    participant A as admin audit endpoint
    participant AS as MerchantSettlementApplyAuditService
    participant MO as Merchant row plus main operator
    participant J as notice jobs

    M->>MC: POST merchant login with mobile and vcode and company_id
    MC->>L: find or create settlement apply
    L-->>M: HS256 JWT sub merchantaccount_id
    M->>MC: POST settlementapply steps 1 to 3
    F->>F: verify signature issuer exp nbf, reload apply row
    A->>AS: POST merchant settlement apply audit
    AS->>AS: only audit_status 1 rows
    alt audit passes
        AS->>MO: insert merchant and multi-lang and main operator
        AS->>J: job:1 AuditSuccessNotice
    else audit fails
        AS->>J: job:2 AuditFailNotice
    end
```

Caption: the merchant settlement lifecycle — wxapp self-service apply under a dedicated JWT plane, then a single admin audit that materializes the merchant and its main operator.

* **Apply.** `merchant_settlement_apply` rows carry `audit_status` `1` (pending) / `2` (passed) / `3` (refused) and `source` admin/h5. Login with mobile + SMS vcode + `company_id` finds or creates the apply row and issues an HS256 JWT (`MerchantWxappJwtIssuer`, subject `merchantaccount_{id}`, claims `account_id`/`company_id`/`mobile`, `operator_type=user`, iss/nbf/exp from `MerchantWxappJwtProperties`).
* **The JWT plane.** Two parallel guards authenticate `@FrontMerchantAuth` endpoints: the servlet-level `MerchantWxappJwtAuthenticationFilter` (covers `POST settlementapply/**`, `POST password/reset`, and four specific GETs: step/detail/auditstatus/type-list) and the MVC `FrontMerchantAuthInterceptor` (annotation-driven, same verification logic). Both verify the HS256 signature, issuer, and exp/nbf claims (with `leewaySeconds`) and then **reload the `merchant_settlement_apply` row** so a deleted apply invalidates an unexpired token; a *refused* apply stays authenticated on purpose — step 3 re-submission resets `audit_status` from refused back to pending. This is a *separate* auth plane from both the operator JWT (admin) and the member session (wxapp), which is why merchant onboarding can happen before any operator exists.
* **Audit.** Only `audit_status=1` rows are auditable. Success materializes the `merchant` row (mirroring apply fields + `settlement_apply_id`), its multi-language shards, and the **main operator** — provisioned through `CompanysMerchantMainOperatorProvisioner` in `ecshopx-companys` (operator_type `merchant`, `is_merchant_main`, encrypted mobile, bcrypt 6-digit password or none) — then enqueues `job:1 AuditSuccessNotice`. Failure enqueues `job:2`. Jobs 3/4 cover enter-success and password-reset notices.

## Salespersons: WeCom session auth and task lifecycle

`shop_salesperson` rows (types `admin`, `verification_clerk` 核销员, `shopping_guide` 导购) bind a WeCom `work_userid` to stores through `shops_rel_salesperson` (`store_type=distributor`); `DistributorSalesmanRole` assigns per-store roles.

Auth flow (`QywxSalespersonAuthInterceptor` → `WxappSalespersonSessionAuthService.authenticate`): the `x-wxapp-session` header is looked up in the shared wechat Redis as `frontSession3rd:{token}` (written by `H5WxappWorkWechatLoginPortImpl`); the JSON session is cross-checked against the DB (`company_id` + `work_userid` + allowed `salesperson_type`, `is_valid=true`) — mismatched type throws `会话与库不一致` — and the DB's `salesperson_id` **overrides** the session's, with the first `store_type=distributor` shop (lowest `shop_id`) injected as `distributor_id` (0 when unbound). A `salesperson-type` header narrows the allowed types.

Admin `/api/v1/salesperson/task` CRUD defines tasks of types 1–4 scoped to all or selected distributors, with overlap rejection. Front completions (`subtask`/`post`) enqueue **`job:45`** (slow queue) whose handler pushes completion to the marketing center through `MarketingCenterTasksCompletePort`; the member↔guide relationship continuity job is **`job:46`**; and shelf recomputation (triggered by package-promotion, marketing-activity, and seckill create/update/end events) is **`job:138`**, with `job:139`–`149` being *aliases of the same bus message name* — one handler registration, many legacy PHP trigger rows (`SalespersonDispatchJobNames`).

## Popularize: promoters and first-level brokerage

`popularize_promoter` (pid hierarchy, `shop_status` 0–4, `is_promoter` flag, Alipay payout binding) and `popularize_brokerage` (integer **fen** amounts) live in `ecshopx-popularize`. On trade finish, the `TradeFinishCountBrokerage` listener invokes `NormalOrderFinishBrokerageCoordinator.onNormalOrderFinishBrokerage`, which: skips unless the company's `open_popularize` setting is literal-true; is **idempotent per order** (existing brokerage rows short-circuit); requires the buyer to be a promoter with a `pid` parent who is an active promoter; computes the first-level rebate as `order_money` (or profit, per config) × first-level ratio percent, rounded to fen, capped at `Integer.MAX_VALUE`; and enqueues the promoter grade-upgrade job. Store QR codes are unlimited-type WeChat codes returned base64. HfPay withdrawal events (268/270/296) drive promoter payout.

## Community group-buy: the chief write-off

`community_chief` / `community_activity` / `community_order_rel_activity` model the group; apply flow is `0` pending → `1` creates the chief / `2` refused with reason. `CommunityChiefQrWriteoffService.executeQrWriteoff` is the only write-off path: it resolves the ziti QR code (Redis) to an order, verifies the `community_order_rel_activity` row belongs to **this chief** (`只能核销自己开团的订单`), rejects already-DONE orders (`该订单已完成自提，请重新确认`) and orders with pending cancel requests (`订单有未处理的取消申请，不能核销`), then delegates the actual state transition to `NormalOrderZitiWriteoffService.orderZitiWriteoffForChief` in the orders bundle — community never duplicates order status logic.

## Supplier orders: the split from normal orders

`ecshopx-supplier`'s `supplier_order` rows are **per-supplier projections of a normal order**. The split runs in `SupplierOrderSplitOnNormalOrderAddService.split` (physically in `ecshopx-orders`, package `cn.shopex.ecshopx.supplier.service`, so it can read `normal_orders*` tables directly): it collects distinct `supplier_id`s from order items and `normal_orders_rel_supplier` rows, and for each inserts one `supplier_order` with aggregated item/total/market/cost/commission fen sums plus the supplier's freight, starting at `order_status=NOTPAY`/`pay_status=NOTPAY`, **guarded by an existence check** (companyId+orderId+userId+supplierId) so re-running is a no-op. The production trigger is a **direct synchronous call** in both normal-order-create orchestrators — `WxappNormalOrderCreateOrchestrator` and `ShopadminNormalOrderCreateOrchestrator` invoke `splitSupplierOrders` right after the transactional runner and before publishing `event:orders:normal_order_add`. `OrdersSupplierOrderSplitOnNormalOrderAddDispatchListener` wraps the same service on that bus plane as `listener:orders.supplier_order_split_on_normal_order_add`; the dispatch-flow tests register it as the first listener (before youshu and the marketing-center push) to pin the fan-out shape. Admin surface: `/api/v1/supplier/get_order_list` and `POST /order_paid_confirm`.

## Reservation and selfservice (briefly)

`ecshopx-reservation` models shifts, resource levels, and reservation records; its operational heartbeats are the expire-cancel job and reservation-finish WXA template listeners. `ecshopx-selfservice` handles registration activities and form submissions. Both follow the standard layering and are covered by the common request lifecycle ([Request Lifecycle](../architecture/request-lifecycle.md)).

## Data model

```mermaid
erDiagram
    companys ||--o{ distribution_distributor : "menu_type scopes the product model"
    distribution_distributor ||--o{ distribution_distributor_items : "per-shop SKU copies"
    distribution_distributor ||--o{ shops_rel_salesperson : "store_type distributor"
    distribution_distributor ||--o{ pos_store_account : "POS staff accounts per store"
    shop_salesperson ||--o{ shops_rel_salesperson : "guide to store binding"
    distribution_distributor ||--o{ pickup_location_rel_distributor : "pickup relations"
    merchant ||--o{ distribution_distributor : "merchant_id"
    merchant_settlement_apply ||--|{ merchant : "audit materializes merchant"
    normal_orders ||--o{ supplier_order : "per-supplier split rows"
    popularize_promoter ||--o{ popularize_brokerage : "first-level rebates in fen"
    community_chief ||--o{ community_order_rel_activity : "chief owns the rel rows"
    community_order_rel_activity }o--|| normal_orders : "order of the group buy"
    distribution_distributor {
        bigint distributor_id PK
        bigint company_id
        int distributor_self
        int is_distributor
        string is_valid
        int is_default
        int auto_sync_goods
        int source_from
        bigint merchant_id
        int distribution_type
        int payment_subject
        string is_open
        int rate
        string wdt_shop_no
        bigint jst_shop_id
        boolean is_audit_goods
    }
    merchant_settlement_apply {
        bigint id PK
        bigint company_id
        int audit_status
        string source
        string settled_type
        string merchant_name
    }
    merchant {
        bigint merchant_id PK
        bigint company_id
        bigint settlement_apply_id
    }
    distribution_distributor_items {
        bigint id PK
        bigint company_id
        bigint distributor_id
        bigint item_id
        int goods_can_sale
    }
    shop_salesperson {
        bigint salesperson_id PK
        bigint company_id
        string work_userid
        string salesperson_type
        string is_valid
    }
    pos_store_account {
        bigint id PK
        bigint company_id
        string employee_no UK
        string role
        bigint distributor_id
        string status
        string password_cipher
    }
    shops_rel_salesperson {
        bigint id PK
        bigint company_id
        bigint salesperson_id
        string store_type
        bigint shop_id
    }
    popularize_promoter {
        bigint id PK
        bigint company_id
        bigint user_id
        bigint pid
        int is_promoter
        int shop_status
    }
    popularize_brokerage {
        bigint id PK
        bigint company_id
        string order_id
        int brokerage_fee
    }
    community_chief {
        bigint chief_id PK
        bigint company_id
        bigint user_id
        int status
    }
    community_order_rel_activity {
        bigint id PK
        bigint order_id
        bigint chief_id
        bigint company_id
    }
    supplier_order {
        bigint id PK
        bigint company_id
        bigint order_id
        int supplier_id
        string order_status
        string pay_status
    }
```

Caption: the store-side tables — the distributor as the join point between companies, merchants, guides, promoters, chiefs, and per-supplier order projections.

## Invariants and failure semantics

* **afterCommit ordering is contractual.** On create: `job:63` enqueue *then* the dual DistributionAdd publishes; on update: DistributionEdit dual publish *then* DistributorUpdate. Publishing `DistributorCreateEvent`/`DistributorUpdateEvent` SYNC *inside or right after* the transaction keeps OME shop sync aligned with the DB write, while the fan-out events only fire after a real commit so listeners always reload persisted state.
* **Dual CSV publishes are deliberate.** `event:281`/`289` (and `282`/`290`) carry the same payload on two bus planes so legacy PHP listener names keep working; do not "deduplicate" them.
* **Company scoping everywhere.** Every distributor/merchant/promoter/salesperson query is `company_id`-bounded; duplicate-name checks and ERP bindings are per company.
* **Soft delete only.** `is_valid` flips to `"delete"`; update wrappers always add `.ne(is_valid, "delete")`.
* **Money is fen** (`brokerage_fee`, supplier order sums, `rate` in basis points); the payment-subject legacy 1366 error must be preserved byte-for-byte.
* **Singletons and defaults.** One `distributor_self` per company; at most the first non-wxapp store is auto-default; update re-defaults when none exists.
* **Audit-disable cascade.** `is_audit_goods` true→false must auto-approve pending shop items, or they become invisible to buyers.
* **Resign is one-way.** A `resigned` POS store account can never return to enabled/disabled, and resigned accounts reject role change, shop transfer, and password reset; `employee_no` is globally unique (case-insensitive), so reusing a resigned employee number means importing onto the same row.
* **DigiOS gates creation, never silently diverges.** When a company's DigiOS config is enabled, POS account create/import fails closed on `account.upsert` errors (the local row is not written); when disabled the port is a no-op. Distributor profile sync is instead listener-driven off event:263/264 and skips silently when disabled.
* **POS plaintext passwords are transient.** They exist only in the create/reset responses and the super-admin-only CSV export; at rest they are `SensitiveFieldEncryptor` ciphertext.
* **Idempotent brokerage and supplier split.** Both are guarded by existence checks on (company, order[, supplier]) so listener retries cannot double-count.
* **Salesperson identity comes from the DB, not the session.** Redis supplies the session, the DB row is authoritative for `salesperson_id` and distributor binding; a type mismatch between the two is a hard reject.
* **Chief write-off ownership.** Only the chief who opened the group can write off, and only before completion/cancel-pending states — the delegation to `orderZitiWriteoffForChief` keeps order-state transitions in one place.

## Extension points

* **React to store lifecycle** — register a listener in a bootstrap `*RegistrationConfig` against `event:263`/`264`/`281`/`289`/`282`/`290`; distribution services need no change ([Cross-Bundle Communication](../architecture/cross-bundle-communication.md)).
* **New store-side setting** — extend the `Distributor` entity, add the column to `DistributorUpdateService.COLUMN_KEYS` with its setter case, and mirror any coercion in the two param normalizers; gate/validator layers stay untouched.
* **New per-shop goods behavior** — extend `distributor_items` consumers; the sync itself stays in the goods bundle (`DistributorGoodsSyncService`, `job:62`).
* **New merchant onboarding step** — add to `MerchantWxappSettlementApplySaveService` and the `MerchantSettlementApplyProgressService` resolver; the JWT plane (issuer + filter + interceptor) needs no change.
* **New guide task type** — task types are data-driven in the task tables; job:45/46/138 planes are unchanged.
* **New local-delivery courier** — the Dada/Shansong clients behind `LocalDeliveryShopCreateClient` are the pattern.
* **React to POS account creation** — register a listener against `event:companys:pos_store_account_create`; to block creation on an external precondition instead, provide another `PosStoreAccountDigiOsCreatePort` (the digios `@Primary` bean is the template).
* **New POS account field** — extend `pos_store_account`, `PosStoreAccountRules`, and `toPublicRow`/`toOrderInfoRow`; the scope and status machinery needs no change.
* **New bulk-import type** — implement `EspierImportRowSink` and register the header catalog entry, the pattern used by `pos_store_account`; exports plug into `job:10` as a new `ExportFileJobTypeHandler` keyed by `type`.

## Focused tests

The dispatch-flow tests are the executable spec for the ordering claims above:

* `DistributorCreateOrchestratorDistributionAddDispatchPublishProbeTest` (incl. the `entry03_shopsCreateRoute_*` legacy-route case) and `DistributorUpdateOrchestratorDistributionEditDispatchPublishProbeTest` — afterCommit job-before-events (create) and edit-before-update (update) ordering, with row snapshots.
* `DistributionAddEventMarketingCenterAndYoushuFanOutDispatchFlowTest`, `DistributionAddHfpayEnterapplyEvent289AsyncBusDispatchFlowTest`, `DistributionAddEventDispatchPublisherImplDualPublishFacadeProbeTest` — the dual 281/289 publish and its fan-out.
* `DistributorUpdateEventSyncBusDispatchFlowTest`, `DistributorCreateEventSyncBusDispatchFlowTest`, `DistributionEditEventMarketingCenterAndYoushuFanOutDispatchFlowTest`, `DistributionEditHfpayEnterapplyEvent290AsyncBusDispatchFlowTest` — update-plane ordering and event:263/264 SYNC semantics.
* `CreateDistributorJobDispatchFlowTest` — job:63 slow-queue envelope and `PagesTemplateNewDistributorFacade` delegation.
* `MerchantAuditSuccessNoticeJobDispatchFlowTest`, `MerchantAuditFailNoticeJobDispatchFlowTest`, `MerchantEnterSuccessNoticeJobDispatchFlowTest`, `MerchantResetPasswordNoticeJobDispatchFlowTest` — the four merchant notice jobs (jobs 1–4) and their publishers.
* `SalespersonTaskJobDispatchFlowTest`, `SalespersonRelationshipContinuityJobDispatchFlowTest`, `SalespersonItemsShelvesJobDispatchFlowTest` — jobs 45/46/138 wiring. (The `WxappSalespersonSessionAuthService` auth cross-check itself has no dedicated test; its contract is pinned by the interceptor's usage.)
* `NormalOrderAddEventAsyncFanOutSupplierAndYoushuDispatchFlowTest`, `NormalOrderAddOrderAddPushMarketingCenterDispatchFlowTest`, `OrdersSupplierOrderSplitOnNormalOrderAddDispatchListenerDispatchConsumerRuntimeTest` — supplier split listener plane and fan-out order.
* `TradeFinishCountBrokerage*DispatchFlowTests`, `TradeFinishCountBrokerageUpgradePromoterGradeEnqueueTest` — idempotent first-level brokerage and grade-upgrade enqueue.
* `DistributorGoodsSyncServiceAddDistributorItemsDispatchTest`, `ItemsUpdateOrchestratorDistributorSyncTest` — the goods→store catalog contract (in [Goods Domain](goods.md)).
* `PosStoreAccountWriteServiceTest`, `PosStoreAccountImportServiceTest`, `PosStoreAccountStatusServiceTest`, `PosStoreAccountPasswordServiceTest`, `PosStoreAccountPasswordGeneratorTest`, `PosStoreAccountRulesTest`, `PosStoreAccountQueryServiceTest`, `PosStoreAccountExportServiceTest` (ecshopx-companys) — the POS account scope, rules, resign-one-way lifecycle, password generation/encryption, DigiOS fail-closed gate, and CSV row shape; `PosStoreAccountDigiOsCreatePortImplTest` (ecshopx-digios) pins the gateway upsert/error mapping.

## Related pages

* [Goods Domain](goods.md) — the other half of `distributor_items`: auto-sync fan-out, `job:62`, price/store overrides.
* [Orders Domain](orders.md) — normal order create (which calls the supplier split), ziti write-off, cancel/restore.
* [Marketing Domain](marketing.md) — distributor scoping of coupon issue and activity goods; salesperson shelves jobs 138–149.
* [Dispatch Bus](../concepts/dispatch-bus.md) — the SYNC/ASYNC, queue, and fan-out semantics under every event and job here.
* [Cross-Bundle Communication](../architecture/cross-bundle-communication.md) — the publisher-in-common / listener-in-bootstrap split all these flows follow.
* [Request Lifecycle](../architecture/request-lifecycle.md) — the operator-JWT vs wxapp-session vs FrontMerchantAuth planes.
* [Redis Databases](../concepts/redis-databases.md) — the wechat/shared Redis split behind `frontSession3rd:` sessions and ziti QR codes.
* [Third-Party Integrations](../integrations/third-party.md) — the receiving side of the DigiOS flows referenced here (distributor profile/warehouse sync on event:263/264, `account.upsert` for POS accounts and staff operators), alongside OME, Shuyun, WDT, and Jushuitan.
