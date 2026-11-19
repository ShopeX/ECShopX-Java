---
type: domain-reference
title: Goods Domain (ecshopx-goods)
description: The product Bundle — item/SKU/category/tag/barcode management, platform-vs-supplier goods flows, three-pool inventory orchestration (ItemInventoryOrchestratorService, InventoryDeductTargetResolver, Redis item_store keys), checkout-time goods services for order create, the operator-cart facade family including the shopadmin fast-buy cloud bucket and platform-store resolution (OnsalePlatformStoreSupport), the wxapp list orchestrator, the Elasticsearch goods-search subsystem (goods_store_item index, blue-green rebuild, dirty-event incremental sync, synonyms, query facade), and the OME / Jushuitan / WdtErp item and inventory sync surfaces.
tags: [goods, items, sku, inventory, categories, barcodes, dispatch-bus, ome, jushuitan, wdterp, checkout, orders, multi-language, recommendations, operator-cart, fast-buy, wxapp-list, elasticsearch, goods-search, synonyms, search-rebuild]
sources:
  - id: openwiki-source-91823d7dc6daf2fa438acd3f
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemAddEventDispatchPublisherImpl.java
  - id: openwiki-source-c11ee8f4aaa9cbf8871c0372
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemBatchEditStatusEventDispatchListenerRegistrationConfig.java
  - id: openwiki-source-3dd3fc812ce333e1934aef32
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemCreateEventDispatchListenerRegistrationConfig.java
  - id: openwiki-source-e236c52c4d8b6f65f943affe
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemCreateEventDispatchPublisherImpl.java
  - id: openwiki-source-9aab24e3a6d9050f203f7aca
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemDeleteEventDispatchListenerRegistrationConfig.java
  - id: openwiki-source-c41d01f5b782c3b4360a1869
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemStoreUpdatedEventDispatchListenerRegistrationConfig.java
  - id: openwiki-source-86880bd9da1913b90b40eef3
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemTagEditEventDispatchListenerRegistrationConfig.java
  - id: openwiki-source-0efb63292c0898d92c370a01
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/SearchRebuildJobDispatchPublisherImpl.java
  - id: openwiki-source-ae5ddc2afd4f9ce163c8bc34
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformItemStoreProductSyncDispatchListenerRegistrationConfig.java
  - id: openwiki-source-c245b30d4b56e7f701f3d388
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ThirdPartyItemAddPushMarketingCenterDispatchListenerRegistrationConfig.java
  - id: openwiki-source-f3449b902cbcc3b4ac95efbc
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/YoushuItemAddDispatchListenerRegistrationConfig.java
  - id: openwiki-source-270d61e0fbdd1c36c95bb04e
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/YoushuItemCategoryDispatchListenerRegistrationConfig.java
  - id: openwiki-source-05ab276ed18a53768f89eadd
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/DistributionDispatchJobNames.java
  - id: openwiki-source-8037f32d986efd35fe393fa0
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/GoodsBundleDispatchJobNames.java
  - id: openwiki-source-7265523c083a528e5f5d8893
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/GoodsDispatchEventNames.java
  - id: openwiki-source-f6d1625023ee3c6ecf8a1b60
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/operatorcart/OperatorCartFastBuyFacade.java
  - id: openwiki-source-23521877b190e50eccec4c0a
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/admin/v1/OperatorCartController.java
  - id: openwiki-source-d615d35129728ef237866431
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/operatorcart/OperatorCartCleanupService.java
  - id: openwiki-source-d3fc99a2ed9386c1382db86d
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/operatorcart/OperatorFastBuyStockValidator.java
  - id: openwiki-source-fb11e56e003a1604d7d9ab7a
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/operatorcart/OperatorShopFastBuyRedisService.java
  - id: openwiki-source-418a764976cc07a563f09f07
    resource: repo://ecshopx-goods/pom.xml
  - id: openwiki-source-d9ade624e0049278a0727013
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/api/admin/v1/ItemsController.java
  - id: openwiki-source-97fedf7e99d48cc8679f5ba0
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/api/admin/v1/ItemsGroupController.java
  - id: openwiki-source-c56638a6f4ae4f5088a0b14b
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/api/admin/v1/ItemsTagsController.java
  - id: openwiki-source-243422ced17769fe49ddc64b
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/api/front/v1/ItemsController.java
  - id: openwiki-source-0bb845d502f3fb8cbdc5c113
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/cron/ScheduleGoodsSearchRebuildHandler.java
  - id: openwiki-source-ad916be12b605c2e4737848c
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/cron/testcfg/CronTestMockConfig.java
  - id: openwiki-source-349f8e5d173616a903dd1042
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/dispatch/ItemsApproveStatusSyncDispatchListener.java
  - id: openwiki-source-4e7beef49f70c145391abf3b
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/dispatch/MedicineItemsSubmitAuditJobHandler.java
  - id: openwiki-source-b746a088d04a04fa4cb3acb7
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/domain/GoodsSearchSynonym.java
  - id: openwiki-source-53076ea432a0f248e7894603
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/domain/Items.java
  - id: openwiki-source-8d11fe28b36d2eaf4389512a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/integration/members/DistributorItemTotalStorePortImpl.java
  - id: openwiki-source-6c6e8bff86285fa211f3f279
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/integration/order/OrderCancelItemStoreRestorePortImpl.java
  - id: openwiki-source-c41f670e867e274d5f3bb473
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/integration/TradeFinishItemSalesIncrementPortImpl.java
  - id: openwiki-source-d63653b2939086ba85d16f19
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/openapi/OpenapiItemStoreSyncPortImpl.java
  - id: openwiki-source-275dbf5dc73225d0a1310eac
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/openapi/OpenapiNormalItemsEntityListPortImpl.java
  - id: openwiki-source-9cf33b4716411b029c0c6147
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/package-info.java
  - id: openwiki-source-999488e07803c8696764656d
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/repository/ItemsBarcodeRepository.java
  - id: openwiki-source-f869b1971f5f9d4b86d57e1c
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/GoodsSearchFacade.java
  - id: openwiki-source-2163fb1f195904f48f634ded
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/index/GoodsSearchDocument.java
  - id: openwiki-source-bae927064575e192939caf08
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/index/GoodsSearchIndexNames.java
  - id: openwiki-source-83649260ef3eb1bd1346ab45
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/index/GoodsSearchMappingService.java
  - id: openwiki-source-26e9034c84f38e52b64bc159
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/index/MybatisSearchIndexSourceLoader.java
  - id: openwiki-source-410557eb602b78f9e9af98d1
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/price/SearchEffectivePriceCalculator.java
  - id: openwiki-source-b80c81bfc2aa0622fe22f9c3
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/query/GoodsSearchEsQueryBuilder.java
  - id: openwiki-source-53fd56bb55ad53eac1c5c098
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/query/GoodsSearchQueryService.java
  - id: openwiki-source-d06abc53183de96931fcb0be
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/rebuild/SearchRebuildJobHandler.java
  - id: openwiki-source-5104912c2e0dd2286860ad8b
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/rebuild/SearchRebuildService.java
  - id: openwiki-source-554819b0ae924b822bd10c56
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/SearchEsClientConfig.java
  - id: openwiki-source-81cee125171492ab5fdeb912
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/SearchEsProperties.java
  - id: openwiki-source-5c8392644c10d3730f3e231c
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/sync/GoodsSearchIndexDirtyFanout.java
  - id: openwiki-source-180ad5fe96bda3a39aaa25d5
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/sync/GoodsSearchIndexPromotionSyncListener.java
  - id: openwiki-source-e574685cf9ffc7b239038722
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/sync/ItemStoreUpdatedSearchIndexDispatchListener.java
  - id: openwiki-source-e18412041f7508495d35a065
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/sync/SearchIndexSyncListeners.java
  - id: openwiki-source-cf7076b808179a7f10172d21
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/sync/SearchIndexSyncScheduler.java
  - id: openwiki-source-b7ec4571fbe5a976a79fa0c5
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/sync/SearchIndexUpdater.java
  - id: openwiki-source-b60c8f430b7650770647d36e
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/synonym/SearchSynonymEsReloader.java
  - id: openwiki-source-bd3665777605d5af32590a39
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/synonym/SearchSynonymImportFinishPortImpl.java
  - id: openwiki-source-284ed48be91d61fac09ff44b
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/synonym/SearchSynonymKeywordExpander.java
  - id: openwiki-source-368e6fd13e6164ccdf948b1c
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/search/synonym/SearchSynonymService.java
  - id: openwiki-source-44d1fb47aa15c7379fef56f1
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/cart/wxapp/CheckoutCartLogisticsSupplierStoreClampService.java
  - id: openwiki-source-964c7fc37a2b8c220b7cf92a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/distributor/DistributorItemsPagedAddRunner.java
  - id: openwiki-source-719565090cf1a121f8d59ecf
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/distributor/DistributorItemsRelListCoreService.java
  - id: openwiki-source-b82b5293cc853319938ea8e0
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/GoodsSearchRebuildCronService.java
  - id: openwiki-source-c47b0102b62dec8fc0d98f93
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/DistributorGoodsSyncService.java
  - id: openwiki-source-7f5c18a9cc00d9897d2ffa5f
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/GoodsItemsBatchApproveStatusService.java
  - id: openwiki-source-237817c18b88db6295d51020
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/GoodsItemsCreateRequestGate.java
  - id: openwiki-source-23c3985b4aa613638a332a5a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/GoodsItemsListFacadeService.java
  - id: openwiki-source-049119100d6e1371c0249622
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/InventoryDeductTargetResolver.java
  - id: openwiki-source-6543108b1d9a5702cf274a44
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemAvailableStoreResolver.java
  - id: openwiki-source-5b98c98dc9a93c24f781fe31
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemInventoryOrchestratorService.java
  - id: openwiki-source-6ed56c6f6706b798432313cd
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemsAuditUpdateService.java
  - id: openwiki-source-0fb528aa924a7af9fa4d40b5
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemsBatchUpdateStoreOrchestrator.java
  - id: openwiki-source-638a47423376dbfac322e5d8
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemsCreateOrchestrator.java
  - id: openwiki-source-57dfabd6152641e6b71cab02
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemsMultiLangWriteService.java
  - id: openwiki-source-313e4ed6edab304d73bedf10
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemsPriceStoreStatusWriteService.java
  - id: openwiki-source-559a49241f70e701d3c70642
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemStoreService.java
  - id: openwiki-source-b8851fd7367f8b95aab5f74c
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemsUpdateOrchestrator.java
  - id: openwiki-source-64e50d6005b4f9569beb5cc1
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/ItemTypeHandler.java
  - id: openwiki-source-d8b2b03bc61aa13476633554
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/NormalItemTypeHandler.java
  - id: openwiki-source-7e55cc8ff3679d4054a5c6f3
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/OnsalePlatformStoreSupport.java
  - id: openwiki-source-b7c91e3bc9e30683048b3b4a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/PlatformItemsAddService.java
  - id: openwiki-source-3a3f04b8b27821da165d6047
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/PlatformItemsDeleteService.java
  - id: openwiki-source-90dd535b173250881e8f323f
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/SupplierItemsAddService.java
  - id: openwiki-source-8d7161eb7467f6465a798c47
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/SupplierItemsReviewGoodsService.java
  - id: openwiki-source-8d12d4d83b1a23e2bf49c70c
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/SupplierItemStoreService.java
  - id: openwiki-source-bf7b277f5766cd9273d993ad
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/ItemsCategoryDistributorIdResolver.java
  - id: openwiki-source-5f98b13fc0c46bccafec9eea
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/ItemsCategoryFrontDisplayResolver.java
  - id: openwiki-source-b71185b19624d50635cb481a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/ItemsCategorySaveService.java
  - id: openwiki-source-b9201082cbd1f6125524734a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/ItemsGroupGetGroupItemsService.java
  - id: openwiki-source-776a970930510c2275a6ec4d
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/ItemsTagsQueryService.java
  - id: openwiki-source-0cc8325ac8d9b948a0f4af61
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/jushuitan/JushuitanInventoryPersistService.java
  - id: openwiki-source-5dba78181b63a301c92640e4
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/jushuitan/JushuitanQueryInventoryOrchestratorService.java
  - id: openwiki-source-e4f469cdebd293c5f098c798
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/jushuitan/JushuitanUploadItemsOrchestratorService.java
  - id: openwiki-source-da65b6a7f6df1c5c256663e5
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/keywords/GoodsKeywordsQueryService.java
  - id: openwiki-source-cd9cb6b079ca1b66f254e1eb
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/keywords/GoodsKeywordsWriteService.java
  - id: openwiki-source-f0296d5d66100085b2e7dcf4
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/memberprice/MemberPriceSaveService.java
  - id: openwiki-source-78f76a52c291312bf7fb5c68
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/ome/OmeItemsFromOmePagedSyncRunner.java
  - id: openwiki-source-08ff5f6cd6e732ced25d5918
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/operatorcart/OperatorCartDataListFacadeImpl.java
  - id: openwiki-source-6d42ce597e8782d354259630
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/operatorcart/OperatorCartFastBuyFacadeImpl.java
  - id: openwiki-source-e9dc6f7af94fb10be75c468e
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/operatorcart/OperatorCartSkuLoadFacadeImpl.java
  - id: openwiki-source-42f1758a2412c6fd4cd28ec2
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/operatorcart/OrderCheckoutCartPortImpl.java
  - id: openwiki-source-2aacf5e6353209ac14416d45
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/GiftActivityStoreAdjustService.java
  - id: openwiki-source-8bdf88efd44481d5750cec20
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCheckoutFullGiftService.java
  - id: openwiki-source-938af9294e98b8cfeebec1de
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateFormatDataPortImpl.java
  - id: openwiki-source-f56f19c3e20e208ef2258517
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateInventoryPortImpl.java
  - id: openwiki-source-205eb0e137e885252a2341ff
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateItemCheckPortImpl.java
  - id: openwiki-source-2cff9b037a9bec1cf44ec08b
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateNeedParamsPortImpl.java
  - id: openwiki-source-33c13f7b0df2deb42e58e484
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/recommend/GoodsRecommendMatchService.java
  - id: openwiki-source-94da57754485343bd7cb7683
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/wdterp/WdtErpUploadItemsOrchestratorService.java
  - id: openwiki-source-f676f676ccc235acd7c4b14a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/wxapp/WxappGoodsItemsListQueryOrchestrator.java
  - id: openwiki-source-b8bfd2250069805b8af88bc1
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/wxapp/WxappGoodsItemsListServiceImpl.java
  - id: openwiki-source-1d581316f942699f1aafc301
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/support/ItemLangShardJdbc.java
  - id: openwiki-source-d5db0205a21e6b21df413b16
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/repository/ItemsBarcodeRepositoryShopScopedTest.java
  - id: openwiki-source-4d672d78212fce96c487df2a
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/search/rebuild/SearchRebuildServiceTest.java
  - id: openwiki-source-014d47f4cf11f2ca9adf5c7a
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/InventoryDeductTargetResolverTest.java
  - id: openwiki-source-2bf1ba61082f6ce058dff2b1
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/ItemsBatchUpdateStoreOrchestratorScopeTest.java
  - id: openwiki-source-f8c4ff033990df6b01dcbe82
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/ItemsBatchUpdateStoreOrchestratorSupplierAuditTest.java
  - id: openwiki-source-11063aa1b259c6be4917558c
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/ItemsUpdateOrchestratorDistributorSyncTest.java
  - id: openwiki-source-3a9dcea26ced98e99c49cd90
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/PlatformItemsDeleteServiceItemDeleteEventPublishProbeTest.java
  - id: openwiki-source-e01da6476a3e75c45d854ba1
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/SupplierItemsAddServiceSyncPlatformIfNeededTest.java
  - id: openwiki-source-84ae714fd4b33a8f08f702aa
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/SupplierItemsReviewGoodsServiceAuditSyncSplitTest.java
  - id: openwiki-source-74a7efd119ec7c5080b6a290
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/items/SupplierItemsReviewGoodsServiceShopBarcodeDualWriteTest.java
  - id: openwiki-source-25ad5756a543273d2d6431c2
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/ItemsCategoryFrontDisplayResolverTest.java
  - id: openwiki-source-79efd8ae53e68c86bc5176b2
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/ItemsGroupGetGroupItemsServiceCompanyScopeTest.java
  - id: openwiki-source-affd691d2fa8ad5b9788f690
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/ItemsTagsQueryServiceCompanyScopeTest.java
  - id: openwiki-source-757aaf6b8b030ef7934937ba
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/keywords/GoodsKeywordsQueryServiceCompanyScopeTest.java
  - id: openwiki-source-465d4f371609d2b1579b034e
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/keywords/GoodsKeywordsWriteServiceCompanyScopeTest.java
  - id: openwiki-source-d88124982cafc993afc38e41
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCheckoutFullGiftServiceItemOrderTest.java
  - id: openwiki-source-441578cb812cdd5a2db8995d
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateFormatDataPortImplDepositBalanceTest.java
  - id: openwiki-source-1e676c82ee2adf8dbb7e0bcc
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/order/normal/OrderCreateInventoryPortImplLimitedBuyTest.java
  - id: openwiki-source-95a5ecc88bde8a2cc8bde414
    resource: repo://ecshopx-goods/src/test/java/cn/shopex/ecshopx/goods/service/wxapp/WxappGoodsItemsListQueryOrchestratorCategoryTest.java
  - id: openwiki-source-03a7f358ce9fb661e2ca984b
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/GoodsArrivalSubscribeMatchService.java
  - id: openwiki-source-2e8626d63f714e784ae5de1d
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/create/OrderCreatePostCommitPortImpl.java
  - id: openwiki-source-c9da549abb50240724e0a2e2
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateOrchestrator.java
  - id: openwiki-source-6bee7c44879ca7bd01cdede7
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/normal/shopadmin/ShopadminNormalOrderCreateTransactionalRunner.java
  - id: openwiki-source-16e29be3e3318eda98c7aa9f
    resource: repo://ecshopx-supplier/src/main/java/cn/shopex/ecshopx/supplier/repository/SupplierItemsRepository.java
  - id: openwiki-source-8d15062ef9d8156f9a51625c
    resource: repo://ecshopx-supplier/src/main/java/cn/shopex/ecshopx/supplier/service/SupplierItemBatchSyncToPoolService.java
  - id: openwiki-source-04b4e069925a03b744d9bb2f
    resource: repo://ecshopx-theme/src/main/java/cn/shopex/ecshopx/theme/service/PagesTemplateWidgetItemsService.java
generated: { by: "openwiki/0.9.0", at: "2026-11-16T17:12:44.218Z" }
verified:
  - by: openwiki/0.9.0
    at: 2026-11-16T17:12:44.218Z
---

# Goods Domain (ecshopx-goods)

`ecshopx-goods` is the product Bundle (`商品域模块`, see `package-info.java`): everything that turns a merchant's catalog into sellable SKUs and everything order creation needs from that catalog. It owns the `items` table (SKU grain) plus its satellites — `items_category`, `items_rel_cats`, `item_rel_attributes`, `items_barcode`, `items_medicine`, multi-language shard tables — and the shop-scoped `distributor_items` copies maintained together with the distribution bundle. On top of the data it provides: admin/front item CRUD, platform-vs-supplier goods flows, batch status/store edits, barcodes, keywords, tags, recommendations, the inventory pools, the goods-side half of order create (`OrderCreateFormatDataPortImpl`, `OrderCreateInventoryPortImpl`, checkout cart ports, full-gift), the wxapp item-list orchestration chain (`WxappGoodsItemsListQueryOrchestrator`), the Elasticsearch goods-search subsystem under `search/` (the `goods_store_item` index, blue-green rebuild, dirty-event incremental sync, synonyms, and the `GoodsSearchFacade` query path the front list uses when `ecshopx.search.es.enabled`), and the operator-cart facade family the **companys** bundle drives over HTTP (`OperatorCartFastBuyFacade`, `OperatorCartSkuLoadFacade`, `OperatorCartDataListFacade`, `OperatorCartScanBarcodeFacade` — all implemented here in `service/operatorcart/`).

The Bundle follows the standard layering ([Bundle Layering](../architecture/bundle-layering.md)): controllers in `api/admin/v1` and `api/front/v1`, orchestration in `service/`, dispatch job handlers in `dispatch/`, openapi port impls in `openapi/`, order/promotion-facing ports in `integration/`, and entities in `domain/`. Its POM depends on 20 other bundles — notably `ecshopx-orders` (cart tables), `ecshopx-promotions`, `ecshopx-distribution`, `ecshopx-supplier`, `ecshopx-deposit`, `ecshopx-pointsmall` — which is why order-create logic can live here while staying decoupled through common ports ([Cross-Bundle Communication](../architecture/cross-bundle-communication.md)).

## State model: one SKU row, two status axes, three ownership scopes

`Items` (`domain/Items.java`) is one row **per SKU**:

| column | meaning |
| --- | --- |
| `item_id` | auto-increment PK |
| `nospec` | `"true"` single-spec (one SKU per goods), `"false"` multi-spec (SKU matrix) |
| `goods_id` / `default_item_id` / `is_default` | group sibling SKUs of one goods; `default_item_id` points at the representative SKU (lowest price by default), `is_default` marks it |
| `distributor_id` | `0` = mall/platform-owned, `>0` = shop-owned goods |
| `supplier_id` / `supplier_item_id` / `is_market` | link to supplier master data (`supplier_items`) and its sellable flag |
| `approve_status` | `onsale` (front sellable), `offline_sale` (hidden), `instock` (not sellable), `only_show` (display only) |
| `audit_status` | `submitting` / `approved` / `processing` / `rejected`, with `audit_reason` / `audit_date` |
| `store` / `price` / `cost_price` | store is a plain int; **prices are integer fen** |
| `is_medicine` / `is_prescription` | pharma gating flags (medicine master data lives in `items_medicine`) |

A unique index enforces `(company_id, distributor_id, item_bn)`; a composite index `(company_id, default_item_id, approve_status, item_type, item_category)` backs the default-SKU list queries. Every table access is company-scoped — the multi-tenant rule across the whole reactor.

## Entrypoints

### Admin REST (`/api/v1/goods`, `@AdminAuth @ShopLog`)

18 controllers in `api/admin/v1`; the central one is `ItemsController` (~1,600 lines):

| route | service | purpose |
| --- | --- | --- |
| `POST /items` | `ItemsCreateOrchestrator` | add goods (route alias `goods.items.create`) |
| `PUT /items/{item_id}` | `ItemsUpdateOrchestrator` | update goods (`goods.items.update`) |
| `DELETE /items/{item_id}` | `ItemsDeleteOrchestrator` | delete goods (`goods.items.delete`), `distributor_id` query param selects the shop scope |
| `PUT /audit/items` | `ItemsAuditUpdateService` | write `audit_status`/`audit_reason` by `goods_id` clause (`goods.items.audit`) |
| `PUT /itemstatusupdate` | `GoodsItemsBatchApproveStatusService` | batch onsale/instock (`goods.status.upate`) |
| `PUT /itemstoreupdate` | `ItemsBatchUpdateStoreOrchestrator` | batch store write; the controller forces `operator_type=supplier` when the request carries `operate_source=supplier`, so the orchestrator routes on the effective operator type |
| `PUT /itemsupdate` | `ItemsPriceStoreStatusWriteService` | price/store/shelf combined edit |
| `GET /items`, `GET /items/onsale`, `GET /sku` | `GoodsItemsListFacadeService` | admin/SKU lists |
| `GET /items/{item_id}` | `GoodsItemsDetailFacadeService` | admin detail |
| `POST /setItemsCategory` | `ItemsRelCatsWriteService` | replace item↔category rows |
| `POST /setItemsTemplate`, `POST /setItemsSort`, `POST /rebateconf`, `POST /warning_store`, `POST /set_commission_ratio`, `PUT /itemsisgiftupdate`, `POST /items/params` | per-field write services | freight template, sort, rebate config, warning store, commission, gift flag, params |
| `POST/GET/DELETE /keywords…` | `GoodsKeywords*Service` | shop hot keywords |
| `GET/PUT /search/synonyms`, `DELETE /search/synonyms/{id}` | `SearchSynonymController` → `SearchSynonymService` | goods-search synonym groups (see [Goods search](#goods-search-elasticsearch)) |
| `POST /medicineItems/sync` | `ItemsMedicineService` | medicine data sync |
| `GET /goodsbycoupon/{coupon_id}` | `GoodsItemsByCouponService` | items usable by a coupon |

Sibling controllers: `ItemsCategoryController` (`/category` CRUD + flat `/createcategory` + saleable-filter switch), `ItemsTagsController`, `ItemsAttributesController`, `ItemsGroupController`, `ItemsCommissionController`, `ItemsProfitController` / `ItemsCategoryProfitController`, `MemberPriceController`, `ServiceLabelsController`, `EpidemicItemsController`, `ExportItemsController` (async export files), `GoodsRecommendRuleAdminController` + `GoodsRecommendDisplaySettingController`, and the three ERP surfaces `JushuitanController` (`POST /upload/items`, `POST /query/inventory`), `WdtErpController` (`POST /upload/wdterp/items`) and `SyncFromOmeController` (`POST /sync/items|itemCategory|itemSpec|brand`).

### Front wxapp (`/api/v1/h5app/wxapp/goods`, `@FrontNoAuth` + Dingo response)

`ItemsController` (front) serves `GET /items`, `/items/filter`, `/shopitems` (small-shop list), `/itemintro/{item_id}` (HTML description, charset-gated), `/memberprice/{item_id}`, `/keywords`, `/items/{item_id}/fav`, `/checkshare/items`, `/share/items/{item_id}`, `/items/batch`, `/items/{item_id}` (detail), `/newitems`, `/items_price_store/{item_id}`, and `POST /scancodeAddcart` (barcode → cart). `CategoryController` serves `/category*` reads, `ItemsGroupController` `GET /get_group_items`, `GoodsRecommendFrontController` `POST /recommendations/match`, and `SalespersonItemsController` the salesperson item list/detail under `/api/v1/h5app/wxapp/goods/salesperson/*`.

### OpenAPI

`openapi/` holds ~30 port impls behind `ecshopx-openapi`'s `/api/openapi/internal/v2` surface (`ecx.items.entity.*`, item price/store sync, brand/category CRUD, product goods/SKU lists v1+v2, distributor item download/update). They delegate to `openapi/thirdapi/v2` services inside this bundle, so external integrations reuse the same mappers.

### Dispatch jobs

All job handlers live in `dispatch/`; registration happens in bootstrap configs (see [Dispatch Bus](../concepts/dispatch-bus.md)):

| job name | handler | purpose |
| --- | --- | --- |
| `job:62:DistributionBundle\Jobs\AddDistributorItems` | `AddDistributorItemsJobHandler` | fan a goods' SKUs into `distributor_items` (self-chaining pages of 100) |
| `job:72:GoodsBundle\Jobs\MedicineItemsSubmitAudit` | `MedicineItemsSubmitAuditJobHandler` | push medicine items to kuaizhen580, write audit result back |
| `job:74:GoodsBundle\Jobs\ItemBatchEditStatusEventJob` | `ItemBatchEditStatusEventJobHandler` (bootstrap) | decoupled republish of the batch-status event |
| `job:75-78:SystemLinkBundle\Jobs\Get*FromOme` | `GetItemsFromOmeJobHandler` etc. | OME paged inbound sync (items / category / spec / brand) |
| `job:79:…UploadItemsToWdtErpJob` | `UploadItemsToWdtErpJobHandler` | WDT ERP item upload pages |
| `job:82:…UploadItemsToJushuitanJob` | `UploadItemsToJushuitanJobHandler` | Jushuitan item upload pages |
| `job:84:…InventoryQueryFromJushuitanJob` | `InventoryQueryFromJushuitanJobHandler` | Jushuitan inventory query batches |
| `job:198:GoodsBundle\Jobs\SearchRebuildJob` | `SearchRebuildJobHandler` | goods search index full rebuild / per-company upsert (see [Goods search](#goods-search-elasticsearch)) |
| export file jobs | `*ExportFileJobTypeHandler` | async CSV/XLSX exports (items, codes, tags, distributor items, epidemic registers, point-mall items) |

The bundle's only production cron code is the goods-search rebuild handler (below); everything else under `cron/` is `cron/testcfg/CronTestMockConfig` for the `test-cron` profile — scheduling is otherwise owned by system-link/bootstrap.

## The create pipeline

```mermaid
sequenceDiagram
    participant C as ItemsController
    participant GATE as GoodsItemsCreateRequestGate
    participant ORCH as ItemsCreateOrchestrator
    participant PLAT as PlatformItemsAddService
    participant SUP as SupplierItemsAddService
    participant DGS as DistributorGoodsSyncService
    participant BUS as Dispatch bus

    C->>GATE: validateBeforeCreate (operator, activation, distributor scope)
    GATE-->>C: merged params with distributor_id injected
    C->>ORCH: createItems(merged)
    alt supplier operator (params supplier_id above 0)
        ORCH->>SUP: addItemsTransactional
        SUP->>SUP: persist supplier_items SKUs, sync linked platform rows
    else platform or shop operator
        ORCH->>PLAT: addItemsTransactional returns defaultItemId
        loop one row per spec_items SKU, or the single SKU
            PLAT->>PLAT: insert or update items, multi-lang upsert, saveBarcode, saveItemStore
        end
        PLAT->>PLAT: unify goods_id and default_item_id, promotion guards
        PLAT->>BUS: ItemCreateEvent published SYNC inside the transaction
        PLAT->>BUS: ItemAddEvent scheduled after commit per SKU
        ORCH->>DGS: syncGoods(companyId, defaultItemId)
        DGS->>DGS: auto-sync distributors, async pages when more than 3
    end
```

Caption: the item create flow — permission gating, supplier-vs-platform routing, per-SKU persistence with side effects, and the two item lifecycle events.

1. **Gate.** `POST /api/v1/goods/items` merges query/form/JSON params, copies `operator_type` from the operator JWT, forces `supplier_id=0` — unless the operator is a supplier, in which case `supplier_id=operator_id` and `audit_status` defaults to `submitting` (other operators get `approved`). `GoodsItemsCreateRequestGate.validateBeforeCreate` then enforces: logged-in operator with non-empty `operator_type`; super-admin route permission per route alias (skipped for `salesperson_workwechat` source); company activation (`ForbiddenException 未激活` when missing/zero, skipped for salesperson source); and distributor operators must carry distributor bindings and may only manage a distributor they own (else `您没有权限管理此店铺`), with their selected `distributor_id` injected into the merged params.
2. **Routing.** `ItemsCreateOrchestrator.createItems` sends `supplier_id>0` to `SupplierItemsAddService` and everything else to `PlatformItemsAddService`, then (platform path only) calls `DistributorGoodsSyncService.syncGoods` and marks the goods search-index dirty via `GoodsSearchIndexDirtyFanout.scheduleUpsertForDefaultItem` (see [Goods search](#goods-search-elasticsearch)).
3. **Platform persistence.** `PlatformItemsAddService.addItemsTransactional` (`@Transactional`) picks an `ItemTypeHandler` (`normal` vs default `services`), enriches medicine params, and — for multi-spec (`nospec=false`) — loops `spec_items` persisting one SKU per row through `persistOneSku`; single-spec persists one. `default_item_id` falls out as the lowest-price SKU (first on-sale SKU, else the first), and `goods_id` defaults to `default_item_id`. After the loop: gift items run `ItemCreatePromotionGuardService.checkNotFinishedActivityValid`, prices run `checkItemPrice`, then `updateByItemIds` + `clearDefaultFlagExcept` + `setDefaultItem` stamp the whole sibling group, and `applyRelData` writes category rows (`ItemsRelCatsWriteService`), brand and params on the default SKU.
4. **Per-SKU side effects.** `persistOneSku` inserts/updates the `items` row, writes multi-language rows, saves the shop-scoped barcode, seeds the Redis store (`itemStoreService.saveItemStore(itemId, store, 0)`), writes `item_spec` attribute rows with spec images, and honors `point_access=items` point-mall upserts. `NormalItemTypeHandler.preRelItemParams` validates store (`0-999999999`), converts `cost_price`/`rebate` money to fen, and generates `item_bn` when missing.
5. **Events.** `ItemCreateEvent` is published **inside the transaction in SYNC mode** (the promotions listener runs inline before commit); `ItemAddEvent` is scheduled `afterCommit` per SKU (or immediately when no transaction synchronization is active) and published ASYNC on the Redis bus. The transaction also schedules a search-index upsert for the resolved default SKU (`GoodsSearchIndexDirtyFanout`, fired after commit), and `ItemsCreateOrchestrator` schedules a second upsert after `syncGoods` returns — both converge on the same upsert.
6. **Distributor fan-out.** `syncGoods` selects distributors with `auto_sync_goods=true` (excluding `is_valid=delete`) and either runs all pages synchronously (≤3 distributors) or enqueues `job:62 AddDistributorItems` — `DistributorItemsPagedAddRunner.consumeOneQueuedPage` inserts/updates `distributor_items` rows per 100-SKU page and re-enqueues the next page until done, also firing Shuyun product sync per touched goods.

## Update, audit, delete, batch status

* **Update** — `PUT /items/{item_id}` reuses the gate (`validateBeforeUpdate`) and `GoodsItemsCreateParamValidator.validateForUpdate` (name/pics/sort required; `normal` items require freight template + brand; supplier goods require `audit_status`), then `ItemsUpdateOrchestrator.updateItems` branches: supplier operator → `SupplierItemsAddService` with audit reset to `submitting` (unless already `processing`); platform operator editing a supplier good (`supplier_id>0` in body) → supplier add when `audit_status=approved`, then `SupplierItemsReviewGoodsService.reviewGoods`; plain platform good → platform add + `syncGoods` + search-index dirty upsert. The branch exclusivity is pinned by `ItemsUpdateOrchestratorDistributorSyncTest` (supplier paths never call `syncGoods`).
* **Audit** — `PUT /audit/items` → `ItemsAuditUpdateService.auditItems` writes `audit_status`/`audit_reason` by a `goods_id` clause that accepts null/single/list semantics.
* **Delete** — `ItemsDeleteOrchestrator.delete` routes by `operator_type`. `PlatformItemsDeleteService.deleteCore` loads the row company-scoped (`删除商品信息有误` when missing) and refuses a mismatched `distributor_id` query (`店铺商品信息有误，不可删除`), expands multi-spec to all sibling SKUs (by `default_item_id`, falling back to the row itself when the sibling query comes back empty), and assembles the `ItemDeleteEvent` payload (`item_id`, `company_id`, `del_ids`, full `item_info` snapshot) **before any delete runs**. The deletes then execute in a fixed order inside the one transaction: first `OperatorCartCleanupService.deleteByCompanyIdAndItemIds` purges the affected `operator_cart` rows (the companys bundle's shopadmin cart table) so a deleted SKU can never linger in an operator cart; then per SKU the `items` row + Redis `item_store` key + attribute rows; then rel-cats (default SKU), services rel-type, the search-index delete fan-out (`GoodsSearchIndexDirtyFanout.scheduleDeleteForDefaultItem`, scheduled before the `distributor_items` rows disappear so the shop list is still resolvable), `distributor_items`, member prices and barcodes; and only at the end does `itemDeleteEventDispatchPublisher.publishAfterCommit` schedule the event **after commit**.
* **Supplier-linked sync.** When a supplier edits its master data, `SupplierItemsAddService.syncPlatformIfNeeded` propagates a PHP-whitelist field patch — `store`, `pics`, `other_product_materials`, `approve_status`, `item_name`, optional `cost_price` (yuan→fen, non-negative only)/`start_num`, and an audit reset (`audit_reason=""`, `audit_date=NULL`) when the edit carries `submiting`/`submitting`/`processing` — to every `items` row linked by `supplier_item_id` and refreshes each linked Redis store. It is skipped unless the goods' `supplier_items.is_sync_platform` flag is on (`isSyncPlatformEnabledForGoods`), and the surrounding `addItemsTransactional` resets that flag to `0` afterwards, so the platform pool owns the rows until the next explicit `syncToPool`. Pinned by `SupplierItemsAddServiceSyncPlatformIfNeededTest`.
* **Supplier review and sync are split.** `SupplierItemsReviewGoodsService` implements three common interfaces the **supplier bundle** drives: `SupplierItemsReviewGoodsExecutor.reviewGoods` (called from `SupplierItemBatchReviewService`, and by `ItemsUpdateOrchestrator` when a platform operator audits a supplier good) now only writes `audit_status`/`audit_reason`/`audit_date` across the supplier `goods_id` — even an `approved` verdict no longer syncs rows into the platform pool. All three entrypoints first verify the supplier row belongs to the caller's company and reject foreign rows with `商品不存在或无权`. Pool copies happen exclusively through the explicit `SupplierItemsSyncToPoolExecutor.syncToPool` (driven by the supplier bundle's `SupplierItemBatchSyncToPoolService`), which requires the row to already be `approved` (`仅审核通过的商品可同步到商品池`), inserts/updates one pool `items` row per supplier SKU (with item-store seeding and pool-scope barcodes), deletes vanished SKUs, stamps `is_sync_platform=1`, and — under the `platform` product model — re-syncs every shop that previously copied the goods. `SupplierItemsSyncToShopExecutor.syncToShop` is likewise restricted to `platform` product-model companies, requires `approved`, and writes shop-scoped `items` rows plus dual-written shop barcode rows (`SupplierItemsReviewGoodsServiceAuditSyncSplitTest`, `SupplierItemsReviewGoodsServiceShopBarcodeDualWriteTest`).
* **Batch status** — `PUT /itemstatusupdate` expands a truthy `query` filter into distinct `goods_id`s, then `GoodsItemsBatchApproveStatusService.updateItemsStatus`: `onsale` rejects supplier-controlled goods (`供应商商品不可售`) and prescription drugs unless the pharma-industry setting is on and their audit is not pending (`instock` skips those checks — deliberate); each goods gets `updateApproveStatusByCompanyAndGoodsId` plus an enqueue of the batch-status event through `job:74` (slow queue). The `job:74` handler republishes `EVENT_ITEM_BATCH_EDIT_STATUS`, whose listeners are the third-party marketing-center push **and** the goods-bundle's own `ItemsApproveStatusSyncDispatchListener`, which cascades the status to `distributor_items`.
* **Batch store** — `PUT /itemstoreupdate` → `ItemsBatchUpdateStoreOrchestrator.batchUpdate`. The controller resolves `operator_type` from the JWT but overrides it to `supplier` when the merged request carries `operate_source=supplier`, so the orchestrator only branches on the effective operator type: supplier writes go to `supplier_items` rows (`updateStoreByCompanyAndProductKey`, keyed by item id or goods-group id via `treatItemIdAsDefaultItemId`) **and simultaneously reset their `audit_status` to `processing`** (`updateAuditStatusByCompanyAndProductKey` — a store touch re-queues the supplier goods for review, per the repository Javadoc 批量改库存后自动进审; pinned by `ItemsBatchUpdateStoreOrchestratorSupplierAuditTest`), plus each row's `supplier_item_store` Redis key; platform writes go to `items` — all sibling SKUs by `default_item_id` with every sibling's Redis `item_store` refreshed when the row id is treated as the default, otherwise a company-scoped single-row update whose Redis key is only seeded when the update actually hit (`ItemsBatchUpdateStoreOrchestratorScopeTest`).

## Inventory: three pools, one resolver

```mermaid
flowchart TD
    CTX["ItemInventoryLineContext built by order create or cancel restore"] --> ORCH["ItemInventoryOrchestratorService"]
    ORCH --> LOAD["load items row, fill supplier_item_id for logistics supplier lines"]
    LOAD --> RES["InventoryDeductTargetResolver"]
    RES -->|"receipt_type logistics and supplier_id set"| SUP["SUPPLIER_ITEMS"]
    RES -->|"is_total_store false and distributor_id set"| SHOP["SHOP_DISTRIBUTOR_ITEMS"]
    RES -->|otherwise| PLAT["PLATFORM_ITEMS"]
    SUP --> SS["SupplierItemStoreService: Redis supplier_item_store key, supplier_items write-back"]
    SHOP --> DS["ItemStoreService: shop-prefixed Redis key, distributor_items write-back"]
    PLAT --> PS["ItemStoreService: plain Redis key, items write-back"]
```

Caption: deduct-target resolution — the PHP `ItemStoreService::minusItemStore` pool rules, kept bit-for-bit in Java.

1. **Resolver.** `InventoryDeductTargetResolver.resolve` maps the line context to a pool: logistics receipt with `supplier_id>0` → `SUPPLIER_ITEMS`; `is_total_store=false` with `distributor_id>0` → `SHOP_DISTRIBUTOR_ITEMS`; otherwise `PLATFORM_ITEMS`. The Javadoc explicitly pins alignment with the PHP implementation.
2. **Orchestrator.** `ItemInventoryOrchestratorService.minusItemStore` loads the `items` row to fill a missing `supplier_item_id` (only for logistics supplier lines), routes to `SupplierItemStoreService` or `ItemStoreService`, and treats a negative `num` as a restore (`restoreItemStore`). Every pool returns `false` when stock is insufficient.
3. **Redis mechanics.** `ItemStoreService.minusItemStore` decrements `item_store:{distributorId}_{itemId}` (shop pool, writes back `distributor_items`) or `item_store:{itemId}` (platform pool, writes back `items`) with `INCRBY -num`; if the result goes negative it re-increments to roll back and returns `false` — the oversell guard. `saveItemStore` seeds the key and publishes `EVENT_ITEM_STORE_UPDATE` synchronously; `saveItemStoreForWdtErp` publishes the bus event **before** writing Redis to preserve WDT ordering. `SupplierItemStoreService` mirrors this on `supplier_item_store:{supplierItemId}` with `supplier_items` write-back, and `resolveSupplierItemStore` returns `0` for rows that are not marketable (`is_market != 1`). The pool routing is pinned by `ItemInventoryOrchestratorServiceTest` (total-store → plain key/`items`; shop pool → prefixed key/`distributor_items`; platform → plain key).
4. **Order create.** `OrderCreateInventoryPortImpl.minusStores` skips `normal_employee_purchase` orders entirely, computes each line's platform/total flag as `uses_platform_item_stock || is_total_store` (the order-level `uses_platform_item_stock` comes from format; a line only counts as shop stock when `is_total_store` is exactly `false`), routes point-mall orders to `PointsmallPartialCancelItemStorePort` instead of the orchestrator, throws `商品库存不足` when a deduct fails, and afterwards records limited-buy purchases (`LimitPersonBuyService.createLimitPerson`, mirroring PHP `AbstractNormalOrder`) and limited-time-sale/seckill purchases (`SeckillUserBuysStoreService`) — pinned by `OrderCreateInventoryPortImplLimitedBuyTest`. Order cancels restore through `OrderCancelItemStoreRestorePortImpl`, and trade finish bumps `items.sales` per line via `TradeFinishItemSalesIncrementPortImpl`.

### Available-store clamping

`ItemAvailableStoreResolver.resolveAvailable` computes what a buyer may actually purchase per delivery mode: logistics + supplier → supplier store (falling back to `logistics_store` without a `supplier_item_id`); non-logistics → local store (`store` when `is_total_store=false`, otherwise `store - logistics_store`, floored at 0); cart display without a chosen receipt type takes the max of both paths. `CheckoutCartLogisticsSupplierStoreClampService` applies these rules **in memory at checkout only** (quantities are clamped for pricing, never written back to the cart).

### Platform-store resolution (cloud-warehouse buys)

`OnsalePlatformStoreSupport` (the PHP `ItemsService::appendPlatformStoreForOnsaleSkuList` parity, kept as a static helper) answers a different question: *how much of this SKU lives in the platform/cloud bucket a shop sells on behalf of headquarters?* `resolvePlatformStoreForSkuRow` follows a strict precedence:

1. `logistics_store` present → that value (supplier-shared/logistics-warehouse stock),
2. else headquarters shipment (`is_total_store` truthy, per `DistributorPlatformStoreBuySupport.isSkuHeadquartersShipment`) → `store`,
3. else a `distributor_store` key present → `0` (independent shop stock, nothing platform-borne),
4. else `store` (platform rows without distributor semantics).

`appendPlatformStoreForOnsaleSkuList` writes this as `platform_store` on each onsale SKU row — `GoodsItemsListFacadeService.listOnsaleItems` exposes it only when the request resolves a single shop (`onsaleDistributorId > 0`) **and** that shop's `is_platform_store_buy` switch is on (`DistributorPlatformStoreBuyGateService.isEnabled`), and echoes `is_platform_store_buy` in the response so the cashier client can show the fast-buy entry. The operator-cart fast-buy path (below) reuses the same row-level resolution to gate the cloud bucket.

The same `is_total_store` semantics cross the bundle boundary in the other direction through `DistributorItemTotalStorePortImpl` (goods' impl of common `DistributorItemTotalStorePort`): it answers which of a set of shops currently draw a given SKU from headquarters stock, which the **members** bundle's goods-arrival subscribe matching (`GoodsArrivalSubscribeMatchService`) uses to decide whether a headquarters `EVENT_ITEM_STORE_UPDATE` should also notify per-shop subscribers.

## Goods-side services of order create

The bundle implements the common order-create ports the orders bundle calls. For shopadmin (代客下单) creates, the caller is the orders bundle's `ShopadminNormalOrderCreateOrchestrator.create`, which runs the staged goods ports synchronously in a fixed order — `OrderCreateNeedParamsPort.validate` → `OrderCheckoutCartPort.fillItemsFromOperatorCart` (the fast-buy bucket detour below) → `OrderCreateItemCheckPort.check` → `OrderCreateFormatDataPort.format` → `OrderCreateFormatDataPort.assertDepositBalance` — then caches a deferred Redis snapshot instead of persisting. `OrderCreateInventoryPort.minusStores` is only invoked later, inside the payment-materialization transaction (`ShopadminNormalOrderCreateTransactionalRunner.persistPreparedSnapshot`), so an abandoned shopadmin checkout never deducts stock (the deferred pipeline is documented on the [Orders Domain](orders.md) page).

```mermaid
sequenceDiagram
    participant ORD as Orders bundle NormalOrderCreate
    participant NPC as OrderCreateNeedParamsPortImpl
    participant OCP as OrderCheckoutCartPortImpl
    participant FBF as OperatorCartFastBuyFacadeImpl
    participant OIC as OrderCreateItemCheckPortImpl
    participant FMT as OrderCreateFormatDataPortImpl
    participant INV as OrderCreateInventoryPortImpl

    ORD->>NPC: validate (company, user, receipt_type whitelist)
    ORD->>OCP: fill items from operator, member or fast-buy cart
    alt cart_type fastbuy
        OCP->>FBF: revalidateFastBuyPlatformStock then getFastBuyCartDataList
    else operator or member/distributor cart
        OCP->>OCP: checked rows from cart services
    end
    ORD->>OIC: check items (max 100 kinds, supplier count, shop scope)
    ORD->>FMT: format order data and apply promotions
    FMT->>FMT: full-gift and plus-buy from checkout meta, coupons, deposit
    ORD->>INV: minusStores, then limited-buy and seckill records
    INV->>INV: ItemInventoryOrchestratorService per line
```

Caption: the checkout-time port chain — goods owns five of the normal-order-create steps, including the shopadmin fast-buy revalidation detour.

* **`OrderCreateNeedParamsPortImpl`** — requires `company_id`/`user_id`, and `receipt_type` must be empty or one of `logistics`/`ziti`/`dada`/`merchant`; `normal_groups` orders additionally require `bargain_id`.
* **`OrderCheckoutCartPortImpl`** — `fillItemsFromOperatorCart` branches on `cart_type`: a `fastbuy` request first passes `DistributorPlatformStoreBuyGateService.assertShopadminPlatformStoreBuyAllowed` (the shop must exist with the `is_platform_store_buy` cloud switch on, else `该店铺未开启云仓可购买`) and `OperatorCartFastBuyFacade.revalidateFastBuyPlatformStock`, then reads the fast-buy bucket; any other cart type goes through `OperatorCartDataListFacade.getCartDataList` with `isSubmit=true`. Both paths take checked rows from the first `valid_cart` block (`购物车为空` when none) and attach `_checkout_cart_meta`; `fillItemsFromMemberDistributorCart` does the same for member/distributor carts (test: `OrderCheckoutCartPortImplTest` pins that checkout-recommend lines merge at the bottom and stay separate from the same item's main line).
* **`OrderCreateItemCheckPortImpl`** — cart-line orders: max 100 distinct SKUs (`单次购买商品种类不能超过100种`), all requested items must resolve in the SKU query (`购买商品无效` / `部分商品无效`), at most 10 distinct suppliers (`单次订单供应商数量不能超过10个`), cart line nums must match the SKU list, and `promotion=normal` orders on a shop may only contain shop-scoped SKUs (via `distributor_items` existence or the SKU's own `distributor_id`); point-mall orders validate through `PointsmallCartSkuLoadService`; single-`item_id` orders check sellability (`approve_status` must be on-sale-like, else `商品{0}已下架`).
* **`OrderCreateFormatDataPortImpl`** — rebuilds `orderData` from scratch (`shop_id=0` matching PHP `OrderService::_formatOrderData`), resolves the order slug through `OrderTypeSlugMapper`, and stamps `uses_platform_item_stock=1` **only** for shopadmin fastbuy orders (order_class `shopadmin` + `cart_type=fastbuy`, PHP `_formatOrderData` parity — every other order gets `0`, which is what makes `OrderCreateInventoryPortImpl` deduct such lines from the platform/total bucket); shopadmin orders also passthrough the seven `receiver_*` address fields and fastbuy forces `is_logistics=true`. Online wxapp normal/groups/employee-purchase orders merge live SKU data (`WxappGoodsItemsListQueryOrchestrator.querySkuItemsList`) — the cart-driven wxapp-online branch additionally skips `promotion`/`operator_id` overrides and the logistics `receipt_type` echo — amounts are prorated in fen with `RoundingMode.DOWN`, and then the layers run: checkout cart promotion merge → **full-gift** → plus-buy (skipped for `fastbuy`) → optimal coupon (for non-online-wxapp-normal inside `format`; for online orders after freight via `applyCheckoutCouponAfterFreight`) → directed crowd discount → gift fee allocation. `assertDepositBalance` rejects `pay_type=deposit` orders when `UserDepositBalanceReadService.getUserDepositTotal` is below `total_fee` (`OrderCreateFormatDataPortImplDepositBalanceTest` pins that `format` itself never throws for balance).

### Full-gift at checkout

`OrderCheckoutFullGiftService.applyGiftActivitiesFromCheckoutMeta` merges gift lines **immediately after their linked main item** (same ordering as the cart), honors per-activity join limits by reading the Redis hash `MarketingUserJoinNum:{companyId}:{marketingId}`, and accumulates per-SKU reservations (main items + already-granted gifts) across activities so the same SKU cannot be oversold by two overlapping activities. `GiftActivityStoreAdjustService` clamps `gift_num` only when the platform `check_gift_store` setting is on, using the same `ItemAvailableStoreResolver` rules as normal items, and surfaces `gift_unavailable_tip` (`赠品暂时无货`) for dropped gifts. Pinned by `OrderCheckoutFullGiftServiceItemOrderTest` and `OrderCheckoutFullGiftServiceGiftStoreTipTest`.

## Operator cart: facades the companys bundle calls

The **companys** bundle owns the operator (shopadmin) cart surface — `POST /api/v1/operator/cartfastbuyadd`, `/cartdataadd`, `/cartdataupdate`, `GET /cartdatalist`, `DELETE /cartdatadel`, `POST /scancodeAddcart` (`OperatorCartController`, `@AdminAuth`) — but every item-facing decision is delegated through facade interfaces declared in `ecshopx-common/operatorcart` and implemented here, so companys never touches `items` tables directly:

| facade (common interface) | goods implementation | purpose |
| --- | --- | --- |
| `OperatorCartSkuLoadFacade` | `OperatorCartSkuLoadFacadeImpl` | load SKU snapshots (`OperatorCartSkuRowDto`), shop-SKU replace, member prices, tag ids, coupon scopes, single-SKU add resolution |
| `OperatorCartDataListFacade` | `OperatorCartDataListFacadeImpl` | operator cart list/checkout data, with lazy cleanup — cart rows whose SKUs vanished are deleted from `operator_cart` via `OperatorCartCleanupService` |
| `OperatorCartFastBuyFacade` | `OperatorCartFastBuyFacadeImpl` | shopadmin cloud fast-buy bucket: add / list / revalidate / clear |
| `OperatorCartScanBarcodeFacade` | `OperatorCartScanBarcodeFacadeImpl` | shop-scoped barcode → item_id for scan-add |

`OperatorCartSkuRowDto` is the shared snapshot currency: item ids, fen prices, `store`, the delivery-dimension flags `isTotalStore`/`logisticsStore`/`distributorStore` (each nullable with documented "unknown" semantics), `specialType` (mirrors `items.special_type`, defaults `normal`), and the marketing fields (tags, brand, medicine flags, crossborder tax) the coupon/promotion layers need.

`OperatorCartDataListFacadeImpl.getCartDataList` is also the **lazy cleanup** counterpart of the delete-time purge: it loads the operator's rows for the `(company_id, operator_id, distributor_id)` scope (checked-only when `isSubmit`), resolves them through `loadSkus`, and when no SKU survives it throws `商品已失效` on submit or deletes all loaded cart rows and returns the empty body; likewise, after the valid-cart handling + format/total chain, rows that landed in `invalid_cart` are physically removed by `OperatorCartCleanupService.deleteByCartIds`. The eager delete-time purge and this lazy list-time sweep share one companys-bundle service, so `operator_cart` never surfaces a SKU the goods bundle has already deleted.

`OperatorCartSkuLoadFacadeImpl.resolveSkuForOperatorCartAdd` is the single add-time resolver shared by the fast-buy bucket and the regular operator-cart add (`OperatorCartAddDataService` in companys): platform branch (`distributor_id=0` or `platform` product model) forces `is_total_store=true`; a shop branch reads `distributor_items` — `is_total_store=false` swaps in the shop's store/price and records `distributor_store`, `is_can_sale=false` zeroes the store; rows the shop neither owns (`items.distributor_id` equals the shop) nor has a relation for are refused with `无效商品`; supplier goods always get their `logistics_store` and a combined effective store via `ItemLogisticsStoreEnricher`.

### Fast-buy facade (店务立即购买，云仓分桶)

```mermaid
sequenceDiagram
    participant CTRL as OperatorCartController
    participant FBF as OperatorCartFastBuyFacadeImpl
    participant LOAD as OperatorCartSkuLoadFacadeImpl
    participant GATE as DistributorPlatformStoreBuyGateService
    participant PS as OnsalePlatformStoreSupport
    participant VAL as OperatorFastBuyStockValidator
    participant REDIS as OperatorShopFastBuyRedisService

    CTRL->>FBF: addFastBuyCartData(company, operator, shop, item, num, accumulate)
    FBF->>LOAD: resolveSkuForOperatorCartAdd
    FBF->>GATE: assertShopadminFastBuyAllowed(itemInfo)
    GATE->>GATE: headquarters shipment skips the cloud switch
    FBF->>PS: resolvePlatformStoreForSkuRow
    FBF->>FBF: resolveShopStockForFastBuy(shop model, shipment)
    FBF->>VAL: validate(shopStock, platformStock, num)
    VAL-->>CTRL: 库存不足 or 当前商品门店有库存，请从收银加购
    FBF->>REDIS: set bucket shop_fastbuy:sha1(company:operator:shop), TTL 600s
```

Caption: the shopadmin fast-buy add flow — gate, bucket split, and the two stock gates before the Redis write.

`OperatorCartFastBuyFacadeImpl` implements the four operations of the facade over the per-(company, operator, distributor) Redis bucket:

* **add** (`addFastBuyCartData`) validates params (`管理员信息有误`/`企业信息有误`/`店铺信息有误`/`购物车商品有误`/`购买数量有误`), resolves the SKU, runs `assertShopadminFastBuyAllowed` (headquarters-shipment SKUs pass unconditionally; store-shipment SKUs need the shop's `is_platform_store_buy` switch on, else `该店铺未开启云仓可购买`), then splits the two buckets: `platformStock` from `OnsalePlatformStoreSupport.resolvePlatformStoreForSkuRow` and `shopStock` from `resolveShopStockForFastBuy` — `0` when `distributor_id<=0`, the company runs the `platform` product model, or the SKU is headquarters-shipped; otherwise the row store. `OperatorFastBuyStockValidator.validate` then refuses shop-stock purchases (`当前商品门店有库存，请从收银加购` — the cashier must sell local stock from the cashier flow) and insufficient cloud stock (`库存不足`). The bucket holds a single `{company_id, operator_id, distributor_id, item_id, num, is_checked: true, special_type, cart_id: 0}` row; `accumulate=true` adds to the existing num when the item matches, otherwise it replaces.
* **list** (`getFastBuyCartDataList`) reads the bucket (empty → `购物车为空` when `isSubmit`, otherwise `{invalid_cart:[], valid_cart:[]}`), re-resolves the SKU, on submit re-runs the stock validation, lifts the display store to `max(store, platformStock)`, then routes the synthetic cart row (with `shop_type=shop_offline`, `shop_id=distributorId`, `activity_type=normal`) through the same `OperatorCartHandleValidCartService` + `SalespersonDistributorCartFormatAndTotalService` chain as the regular operator cart so pricing/promotion output is identical.
* **revalidate** (`revalidateFastBuyPlatformStock`) re-runs gate + both stock gates against the persisted bucket — `OrderCheckoutCartPortImpl.fillItemsFromOperatorCart` calls it immediately before submit so a stale bucket (expired or unknown item) fails fast with `立即购买已失效，请重新加购` rather than mid-checkout.
* **clear** (`clearFastBuyCart`) overwrites the bucket with an empty map — the orders bundle's `OrderCreatePostCommitPortImpl` calls it after a successful `normal_shopadmin` fastbuy order (regular operator carts are emptied item-by-item instead).

The bucket itself is `OperatorShopFastBuyRedisService` (companys bundle): key `shop_fastbuy:{sha1(companyId:operatorId:distributorId)}` on the `companysRedisTemplate`, JSON row, 600-second TTL, `clear` writes an empty map. Note the member-side (non-operator) fastbuy bucket is a different, older key — `fastbuy:{sha1(companyId+userId)}` on the shared template, read directly by `OrderCheckoutCartPortImpl.fillItemsFromGroupsFastBuyCart`.

### The cloud-buy purchase record

<!-- openwiki: broken internal link [../workflows/operator-fast-buy-flow.md] file "../workflows/operator-fast-buy-flow.md" does not exist. Fix the href or restore the target, then delete this comment. -->
The end-to-end shopadmin fast-buy flow (add → checkout → order create → bucket clear, including where each gate fires) is traced in [Operator Fast-Buy Flow](../workflows/operator-fast-buy-flow.md); this page owns the goods-side pieces above plus the `uses_platform_item_stock` flag that carries the platform-bucket deduction decision through `OrderCreateFormatDataPortImpl` into `OrderCreateInventoryPortImpl`.

## Goods search (Elasticsearch)

The `search/` package is the optional ES-backed recall path for the front goods list plus the index-maintenance machinery behind it. Everything is gated on `ecshopx.search.es.*` (`SearchEsProperties`): `enabled` (default `false`) turns the subsystem on, `likeFallback` decides whether an ES failure falls back to the DB list or returns an empty body, `hosts`/`alias` (default alias `goods_store_item`)/timeouts configure the `ElasticsearchClient` built in `SearchEsClientConfig`. The legacy `rank.*` weights are retained only for config compatibility — ranking is the hierarchical sort below.

### Index shape and document materialization

One document per **store-item** — the `(company_id, distributor_id, default_item_id)` triple (`GoodsSearchDocumentId`), so a goods exists once for the mall (`distributor_id=0`) and once per shop that sells it. `GoodsSearchMappingService` creates physical indices named `goods_store_item_yyyyMMddHHmmss` with: IK text (`ik_max_word` / `ik_smart`) on `item_name`/`brief`/`tags` plus a 1–2-gram CJK `.ngram` subfield, pinyin keyword+ngram (2–20) fields for full-pinyin and first-letter infix matching, keyword `item_bn`/`barcode`, long price/sort/sales/listed_at, `flattened` `member_prices.grade`/`vipGrade` maps, and **nested** `skus` (`sku_id`, `store`, `approve_status`) and `activities` (`type`, `activity_id`, `activity_price`, `start_at`, `end_at`, `priority` — only seckill / limited-time-sale / groups price-changing activities). `GoodsSearchDocument.hasStock()` is true when any SKU has `store > 0` and a sellable status.

`MybatisSearchIndexSourceLoader` materializes one source per store-item key (paged by `(default_item_id, distributor_id)`): the default SKU anchors name/brief/prices, the sibling SKU rows become the nested SKU list (shop rows take their store/price from `distributor_items`), shop documents are materialized only when the goods is on that shop's shelf (`goods_can_sale`) or shop-owned — mirroring the DB list scope — and `SearchIndexMemberPriceMaterializer` / `SearchIndexMemberActivityRepository` add the member-price buckets and live activity rows. An empty materialization page does not end the scan (orphan keys are skipped), so rebuilds cannot stall on a deleted `items` row.

### Query path: ES recall + DB hydrate

```mermaid
sequenceDiagram
    participant SVC as WxappGoodsItemsListServiceImpl
    participant GATE as Orchestrator salesman gate
    participant FAC as GoodsSearchFacade
    participant QRY as GoodsSearchQueryService
    participant ES as Elasticsearch (goods_store_item alias)
    participant ORCH as WxappGoodsItemsListQueryOrchestrator

    SVC->>GATE: resolveSalesmanGateForWxappItemList (short-circuit nosalestore)
    SVC->>FAC: queryListWithHydration(effective params, GoodsSearchQuery)
    FAC->>QRY: search(query)
    QRY->>QRY: expand keywords via SearchSynonymKeywordExpander
    QRY->>ES: bool query (hard filters + keyword shoulds), hierarchical sort
    ES-->>QRY: hits (x5 over-fetch when price filter/sort)
    QRY->>QRY: SearchEffectivePriceCalculator per hit, Java price filter/sort, page slice
    QRY-->>FAC: GoodsSearchPageResult (total, hits, approxTotal)
    FAC->>ORCH: queryItemListData(item_id = ES-ordered ids, page 1, size min(50, hits))
    ORCH-->>FAC: hydrated DB rows
    FAC->>FAC: reorder to ES order, write sale_price into price + __list_sale_price
    FAC-->>SVC: total_count (adjusted for dropped hits) + list
```

Caption: the ES recall path — synonym-expanded keyword search, Java-side effective-price post-processing, then DB hydration in ES order.

* **Entry.** `WxappGoodsItemsListServiceImpl.execute` uses ES only for the default-goods list (`is_default != false`) when `searchEsProperties.isEnabled()`. With ES on, the param builder is told to skip the DB keyword LIKE (`skipKeywordDbLike`); the salesman gate is pre-resolved so a gated request short-circuits before touching ES; the tag intersection is applied in Java (`applyTagItemIdConstraint`); and `GoodsSearchQuery.fromWxappParams` maps the orchestration params (company, primary `distributor_id`, keywords, the `item_id ⊕ category_resolved_item_ids` allow list, category keys, approve/audit status, fen price bounds, `goodsSort`, page/pageSize, member tier). A `GoodsSearchEsException` falls back to the DB orchestrator when `likeFallback` is on, otherwise the response is an empty list body.
* **Recall.** `GoodsSearchQueryService` short-circuits the `[-1]` no-match allow list, expands keywords through the synonym groups, then builds the ES bool query (`GoodsSearchEsQueryBuilder`): hard filters on company/distributor/approve/audit/allow-list/category, and — when keywords exist — a should-set of IK `multi_match` (`item_name^3`, `brief^2`, `tags^1`), a single-codepoint-token `.ngram` `multi_match`, `item_bn`/`barcode` prefixes, and pinyin `multi_match`; without keywords it is a constant-score match-all. Sorting is hierarchical: `has_stock` desc → `_score` → back-office `sort` desc → `sales` desc → `listed_at` desc; `goodsSort` sales/new map to a single-field ES sort.
* **Effective price in Java.** Each hit's effective price is computed by `SearchEffectivePriceCalculator` with the exclusive-branch precedence **activity price > custom member price > tier-discount price > sale price**. Price filters and price sorts are therefore applied in Java: the service over-fetches `page * pageSize * 5` (capped at `MAX_WINDOW` 5000), filters/sorts, slices the requested page, and marks the total approximate; non-price queries paginate natively in ES (window beyond 5000 → `GoodsSearchEsException`).
* **Hydration.** `GoodsSearchFacade` re-queries the DB through `WxappGoodsItemsListQueryOrchestrator.queryItemListData` with `item_id` set to the ES-ordered ids (page 1, size `min(50, max(ids,1))`), reorders rows back into ES order, writes the ES document's `sale_price` into the row's `price` (plus the internal `__list_sale_price` marker; enrichment may still add `activity_price`, and `restoreSalePriceWhenNoActivityPriceTag` cleans up rows with no price-changing activity), and adjusts `total_count` down by the number of hits that failed hydration (e.g. a shop's `goods_can_sale` flipped between index and query). Member-price display fields (`member_price`/`vip_price`) are applied later by `WxappGoodsItemsListMemberPriceApplyService` and never overwrite `price`.

### Incremental sync: dirty events after commit

```mermaid
flowchart LR
    subgraph sources["Dirty sources"]
        A1["create / update / audit / price-store-status<br/>member price save<br/>(GoodsSearchIndexDirtyFanout.scheduleUpsert)"]
        A2["delete<br/>(scheduleDelete, before distributor_items purge)"]
        A3["EVENT_ITEM_STORE_UPDATE<br/>(ItemStoreUpdatedSearchIndexDispatchListener)"]
        A4["seckill / groups commit + seckill end<br/>(GoodsSearchIndexPromotionSyncListener<br/>→ GoodsSearchIndexDirtyByItemIdsPort)"]
    end
    A1 & A2 & A3 & A4 --> SCH["SearchIndexSyncScheduler<br/>(GoodsSearchDirtyEvent afterCommit,<br/>immediate without tx)"]
    SCH --> LIS["SearchIndexSyncListeners<br/>@TransactionalEventListener AFTER_COMMIT"]
    LIS --> UPD["SearchIndexUpdater<br/>upsert = fetchOne → map → bulkIndex<br/>(missing source → delete); delete; 3 retries, warn-only"]
    UPD --> ES[("goods_store_item alias")]
```

Caption: incremental search-index sync — every goods-mutating path fans out a dirty event per `(company, distributor, default_item_id)`; writes happen only after the DB transaction commits.

`GoodsSearchIndexDirtyFanout` expands a `default_item_id` to the mall (`distributor_id=0`) plus every shop holding the goods in `distributor_items`, so one goods edit dirties one document per selling store. The write paths that call it: platform create/update (`PlatformItemsAddService` in-transaction, plus `ItemsCreateOrchestrator`/`ItemsUpdateOrchestrator` after `syncGoods` on the platform path only), `ItemsAuditUpdateService`, `ItemsPriceStoreStatusWriteService`, `MemberPriceSaveService`, and delete (`scheduleDeleteForDefaultItem`, deliberately scheduled **before** the `distributor_items` rows are purged so the fan-out can still resolve the shop list). `ItemStoreUpdatedSearchIndexDispatchListener` additionally listens to `event:258 ItemStoreUpdateEvent` so every stock change refreshes `has_stock`/`skus.store`, and `GoodsSearchIndexPromotionSyncListener` listens to seckill commit (`@EventListener`), seckill end and groups commit (`@TransactionalEventListener AFTER_COMMIT`, item ids re-read from the rel tables) and fans out through the common `GoodsSearchIndexDirtyByItemIdsPort` (goods' impl maps sku ids → default ids).

`SearchIndexSyncScheduler` defers each dirty event to `afterCommit` (publishing immediately when no transaction synchronization is active, e.g. tests), and `SearchIndexSyncListeners` consumes the Spring event after commit and calls `SearchIndexUpdater`: an upsert re-materializes the source and bulk-indexes it — a source that no longer materializes (deleted goods, goods dropped from the shelf) becomes a document **delete** — and every op retries 3 times and only logs on final failure, so a search-index outage never breaks the goods write path. All of it is a no-op while `ecshopx.search.es.enabled=false`.

### Full rebuild and scheduled calibration

`job:198 GoodsBundle\Jobs\SearchRebuildJob` (`SearchRebuildJobHandler`, registered in bootstrap's `SearchRebuildJobRegistrationConfig`, enqueued ASYNC on the Redis slow queue) supports three payload shapes: `all_companies=true` → `rebuildCompanies` (blue-green rebuild of every active, unexpired company into one new physical index, then alias switch); `company_id` + optional `distributor_id` → `reindexCompanyIntoAlias` (upsert the company's pages into the current alias **without** switching); and `company_id` + `blue_green=true` → single-company blue-green (`rebuild`) — which replaces the site-wide alias and is explicitly marked dangerous for multi-tenant use. `SearchRebuildService` pages the source loader in batches of 500 keyed by `(default_item_id, distributor_id)`; a bulk failure aborts before the alias switch, so a failed rebuild never swaps in a partial index (`SearchRebuildServiceTest`).

The XXL-Job handler `goods-search-rebuild` (`ScheduleGoodsSearchRebuildHandler`) periodically runs `GoodsSearchRebuildCronService.enqueueRebuildForActiveCompanies`, enqueuing one per-company upsert job per active company — the recurring drift-calibration loop that heals missed incremental events (and returns 0 without enqueuing when ES is disabled).

### Synonyms

Synonyms are per-company groups stored in `goods_search_synonyms` (unique `(company_id, group_key)`; `SearchSynonymService`, with an in-memory per-company list cache) and administered from `SearchSynonymController` (`GET/PUT /api/v1/goods/search/synonyms`, `DELETE /search/synonyms/{id}`). They take effect in **two** places:

1. **Query-time expansion (the live path).** `GoodsSearchQueryService.withSynonymExpandedKeywords` asks `SearchSynonymKeywordExpander` to grow the keyword string: when the trimmed query equals or token-matches any word of a group, all other words of that group are appended (space-joined) before the `multi_match` runs — no index-side synonym filter needed (`SearchSynonymKeywordExpanderTest`).
2. **ES 8 synonyms set (hot reload).** `SearchSynonymEsReloader` pushes the company's groups as a synonyms set (`goods_synonyms_{companyId}` via `SearchSynonymSetNames`) through the ES Synonyms API and calls `reloadSearchAnalyzers` on every physical index bound to the alias; `GoodsSearchMappingService.buildUpdatableSynonymFilter` declares the matching `updateable` synonym filter (per-company analyzer selection on the shared index is not yet wired into the query path). `replaceGroup`/`deleteGroup` invalidate the cache and reload ES; a reload `IOException` surfaces as `SearchSynonymEsReloadException` (同义词 ES 热更新失败). Bulk imports write groups without ES reload (`upsertGroupWithoutEsReload` via `SearchSynonymImportRowService`), and the espier import-finish port (`SearchSynonymImportFinishPortImpl`) triggers one reload at the end, degrading to a warning hint (数据已保存，搜索同义词热更新失败…) instead of failing the import.

## Categories

`ItemsCategorySaveService.saveItemsCategory` is the tree persistence entry (`package-info.java` points here): it validates the tree (name required, ≤ 50 UTF-8 bytes), applies `customize_page_id` defaults, saves levels 1–3 recursively with comma-joined `path`, and resolves each node's `distributor_id` via `ItemsCategoryDistributorIdResolver` — main categories always `0`; under the `platform` product model the JWT distributor id wins; the product model comes from `companys.menu_type` (1..5 → `all`/`b2c`/`platform`/`standard`/`in_purchase`) with `${common.product-model:platform}` as default. An `afterCommit` synchronization publishes `EVENT_ITEM_CATEGORY_ADD` and dispatches Shuyun open-platform category sync for level-2/3 categories (`ItemsCategorySaveServiceApiConvergenceDispatchTest`).

Front display decisions live in `ItemsCategoryFrontDisplayResolver` (ECX-9537): platform mall pages (`distributor_id=0`) always read management categories and skip the sale→management fallback; `standard`-model shops share the platform sale categories (distributor `0`); customize pages bind level-1 management categories only under `platform`.

## Multi-language writes

`ItemsMultiLangWriteService` mirrors PHP `MultiLangService` for module `items`: `item_name`/`brief`/`intro` are upserted into per-language shard tables `item_multi_lang_mod_lang_*` (via `ItemLangShardJdbc`). Create writes all three fields (empty string when absent); update writes only fields present in params. The language tag resolves from merged request params, then query/Accept-Language through `RequestCountryCode`, defaulting to `LangueProperties.defaultLang`. Front list/detail services apply the shard rows back onto response rows.

## Tags, keywords, goods groups

Three small catalog surfaces were hardened to never leak or hand over another tenant's rows — foreign-company (and company-less) rows behave exactly like missing ones:

* **Tags** (ECX-10948) — `ItemsTagsQueryService.getTagInfoByTagId` (admin `GET /goods/tag/{tag_id}`) returns an empty list when the loaded row is missing, belongs to another company, or has a null `company_id`, instead of echoing the foreign tag (`ItemsTagsQueryServiceCompanyScopeTest`).
* **Keywords** (ECX-10947) — `GoodsKeywordsQueryService.getInfoById` (admin `GET /goods/keywordsDetail`) returns `null` under the same conditions, and the controller turns that into an empty-list response. The write side was hardened earlier (ECX-10889): `GoodsKeywordsWriteService.addKeywords` refuses to update an id owned by another company with `记录不存在`, so a foreign keyword row can never be overwritten or re-taken (`GoodsKeywordsWriteServiceCompanyScopeTest`).
* **Goods groups** (ECX-10946) — `ItemsGroupGetGroupItemsService.listGoodsIdsByGroupId` (admin `GET /goods/get_group_items`, route `goods.items.get_group_items`) always filters the `items_group_rel_item` page by `company_id` (plus `group_id`), so foreign or company-less relations page back empty (`ItemsGroupGetGroupItemsServiceCompanyScopeTest`). The theme bundle's page-widget `items_group` data source uses a separate cross-tenant `listAllGoodsIdsByGroupId` whose own widget query stays company-scoped downstream in `WxappGoodsItemsListQueryOrchestrator`.

## Wxapp item list orchestration

`GET /api/v1/h5app/wxapp/goods/items` (`WxappGoodsItemsListServiceImpl`) and `/shopitems` share one orchestration layer, `WxappGoodsItemsListQueryOrchestrator`, with four entrypoints — `querySkuItemsList` (full SKU pull, no paging), `querySalespersonSkuItemsList` (same plus `goodsSort` ordering), `queryItemListData` (default-goods pages, page size clamped to 50) and `queryShopItemListData` (no clamp). Every entrypoint follows the same pipeline:

1. **Param build** (`WxappGoodsItemsListParamBuilder`) whitelists request params and pre-resolves the two "expanded" filters: a `keywords` search merges name/brief, item_bn/barcode and tag matches into `__keyword_default_item_ids` (writing an **empty** list on no hit — otherwise the orchestrator would fall back to the full catalog), and `category_id`/`category` expand the category tree into `category_resolved_item_ids` (empty tree → `[-1]`, forcing no match).
2. **Merchant/salesperson gates** — distributors linked to disabled merchants are stripped from the `distributor_id` filter; the salesperson gate may patch params or short-circuit to an empty response (`nosalestore`).
3. **Repo param assembly** (`buildRepositoryParams`) — `audit_status` defaults `approved`, `approve_status` passes through, price bounds convert yuan→cents, and the item-id family composes `item_id` ⊕ `category_resolved_item_ids` ⊕ `__keyword_default_item_ids` ⊕ `tag_id` into one `KEY_ITEM_ID_OR_DEFAULT_IDS` set, intersecting when several are present and collapsing to `[-1]` whenever an intersection empties (a category or keyword with no hits must yield an empty list, not the whole catalog — pinned by `WxappGoodsItemsListQueryOrchestratorCategoryTest` and `…KeywordTest`).
4. **Distributor scoping** — `standard` model with `is_can_sale` and a single shop joins through the store shelf (`dist_rel_virt_` virtual join on `distributor_items`/`goods_can_sale`, regardless of whether the SKU itself has independent stock); any other combination falls back to a direct `items.distributor_id` filter.

`effectiveBusinessParamsForWxappItemList` replays steps 2 (minus the paging internals) so the response's `newFilter` echo reflects exactly the conditions the query used. When `ecshopx.search.es.enabled` is on and the request is the default-goods list (`is_default != false`), `WxappGoodsItemsListServiceImpl` routes the recall through Elasticsearch instead — `GoodsSearchQuery.fromWxappParams` maps the same effective params into an ES query and `GoodsSearchFacade` hydrates the DB rows in ES order (full flow in [Goods search](#goods-search-elasticsearch)); the orchestrator's `[-1]` no-match convention is honored on the ES side too. After the orchestrator/facade returns, the service layer applies member prices, promotion enrichment, promoter pricing, distributor sku-replace (`SalespersonCartDistributorSkuReplaceService`, same field rules as the rel-list core) and the wxapp display-store composition.

`OrderCreateFormatDataPortImpl` reuses `querySkuItemsList` (with just `item_id` ids) to merge live SKU data onto wxapp-online order lines at checkout — the same enrichment a buyer saw in the cart.

## Shop-scoped rel listing

`DistributorItemsRelListCoreService` is the shared pagination core behind the shopadmin goods list (`DistributorItemsListOrchestratorImpl`, driven from distribution's admin controller), the Jushuitan/WdtErp upload page queries, the distributor CSV export, and the openapi SPU page (`queryOpenapiSpuPage`, minus filter echo and medicine enrichment). `applyDistributorItemsScopeFilter` is its shop scoping: `standard` model + `distributor_id>0` lists only goods related to that shop through the `dist_rel_virt_` virtual join (with the request's `is_can_sale` mapped to `d_items.goods_can_sale`, PHP `getDistributorRelItemList` parity); any other product model filters by `items.distributor_id` directly. Row post-processing is product-model dependent — `standard` runs `applyDistributorSkuReplace` (shop store/price replace, `distributor_store`, `logistics_store` preservation of headquarters stock for onsale rows, `is_total_store`/`is_can_sale` flags) and sums SPU stock across SKUs unless `is_sku=true`; non-standard runs the platform pipeline (admin-list type labels, medicine data) without the shop overlay. `toFilterSnapshot` translates internal repo keys back to the `__getItemFilter` API shape for the response echo.

## ERP integrations

* **OME (inbound).** `SyncFromOmeController` triggers paged inbound sync (`OmeItemsFromOmePagedSyncRunner` for goods, plus category/spec/brand runners). Each page calls `ShopexErpOpenApiClient` (`goods.getList`, fixed page size 10 per the OME contract) gated on the company's `is_openapi_open` Redis setting, persists the page, then either re-enqueues itself with `page+1` through `GetItemsFromOmeJobDispatchPublisher` (`job:75`) or — on the last page of the window — advances the Redis last-modify cursor in `OmeLastTimeRedisAccessor`. The self-chaining shape is shared by all four runners (jobs 75–78).
* **Jushuitan.** `POST /goods/upload/items` → `JushuitanUploadItemsOrchestratorService`: requires the shop's `jst_shop_id` binding when a `distributor_id` is given, filters `normal` + `audit_status=approved` + default SKUs, either a single `item_id` or everything with `updated >=` the last-upload Redis cursor, and enqueues pages (`job:82`) handled by `JushuitanUploadItemsBatchHandler`. `POST /goods/query/inventory` → `JushuitanQueryInventoryOrchestratorService` enqueues batches of 100 item ids (`job:84`); `JushuitanInventoryPersistService` maps `sku_id` back to `item_bn`, computes `store = qty + virtual_qty + purchase_qty + return_qty + in_qty − order_lock − local_frozen` (frozen = unpaid orders past their cancel deadline), writes `items.store` and re-seeds Redis through `saveItemStore` — which is why a JST inventory push fans out `EVENT_ITEM_STORE_UPDATE` (`JushuitanInventoryPersistItemStoreDispatchPublishProbeTest`).
* **WdtErp (outbound).** `POST /goods/upload/wdterp/items` mirrors the Jushuitan shape (`wdt_shop_no` binding check, `job:79`, `WdtErpUploadItemsBatchHandler`), and the WDT-specific store path publishes the bus event before the Redis write (see `saveItemStoreForWdtSync` above).

## Events published and who listens

Publisher interfaces live in `ecshopx-goods/dispatch` (some in `ecshopx-common`), implementations and listener registrations in `ecshopx-bootstrap/config` — the standard split ([Cross-Bundle Communication](../architecture/cross-bundle-communication.md)). Names come from `GoodsDispatchEventNames`:

| event | name | published from | listeners |
| --- | --- | --- | --- |
| `ItemCreateEvent` | `event:257:GoodsBundle\Events\ItemCreateEvent` | `PlatformItemsAddService` **in-tx, SYNC** | `promotions.CreateItemSuccessPromotions` (sync, inline) |
| `ItemAddEvent` | `event:271:…\ItemAddEvent` | per-SKU `afterCommit`, ASYNC Redis | third-party marketing-center push, youshu item add |
| `ItemDeleteEvent` | `event:247:…\ItemDeleteEvent` | delete `afterCommit`, ASYNC Redis | marketing-center push (sync listener), youshu `items_item_delete` (async) |
| `ItemStoreUpdateEvent` | `event:258:…\ItemStoreUpdateEvent` | `ItemStoreService.saveItemStore` / `saveItemStoreForWdtErp` (SYNC) | ali template message (`goods.listeners.SendTemplateMsgListener`), members goods-arrival notices, Shuyun open-platform product sync, `ItemStoreUpdatedSearchIndexDispatchListener` (search-index upsert) |
| item category add | `event:goods:item_category_add` | category save `afterCommit`, ASYNC Redis | youshu `item_category` |
| `ItemTagEditEvent` | `event:259:…\ItemTagEditEvent` | `ItemsTagsRelService` (SYNC) | `promotions.ItemTagEditSuccessPromotions` |
| `ItemBatchEditStatusEvent` | `event:248:…\ItemBatchEditStatusEvent` | `job:74` handler republish (ASYNC Redis) | marketing-center push, goods `items_approve_status_sync` → `distributor_items` |

Note the deliberate asymmetry: `ItemCreateEvent` must run inside the transaction (promotions index the new goods before anything else sees it), while `ItemAddEvent`/`ItemDeleteEvent` only fire after a real commit so listeners always reload persisted state.

## Data model

```mermaid
erDiagram
    items ||--o{ item_rel_attributes : "spec value and brand rows per SKU"
    items ||--o{ items_rel_cats : "categories linked on the default SKU"
    items ||--o{ items_barcode : "shop-scoped barcode rows"
    items ||--o| items_medicine : "medicine master data"
    items ||--o{ distributor_items : "per-shop SKU copies"
    supplier_items ||--o{ items : "supplier_item_id links pool SKUs"
    items_category ||--o{ items_rel_cats : "category side of the link"
    items {
        bigint item_id PK
        bigint company_id
        int distributor_id
        string item_type
        string nospec
        bigint goods_id
        bigint default_item_id
        boolean is_default
        string approve_status
        string audit_status
        int store
        int price
        int supplier_id
        int supplier_item_id
        smallint is_medicine
        string item_bn
    }
    items_barcode {
        bigint company_id PK
        bigint distributor_id PK
        string barcode PK
        bigint item_id
        bigint default_item_id
    }
    items_category {
        bigint category_id PK
        bigint company_id
        bigint parent_id
        int category_level
        boolean is_main_category
        bigint distributor_id
        string path
    }
```

Caption: the goods tables — SKU grain in `items`, goods grain via `goods_id`/`default_item_id`, shop scoping on `distributor_id`, barcodes keyed `(company_id, distributor_id, barcode)`.

Barcodes deserve their own note: `ItemsBarcodeRepository.saveBarcode` deletes the item's rows, normalizes comma (incl. full-width `，`) separated codes, and inserts one row per code — rejecting any code already used by **another SKU in the same shop** (`条形码已经存在`) while different shops may share a code. `WxappScanCodeAddCartService` resolves scanned barcodes through this shop scope before adding to the cart (`WxappScanCodeAddCartServiceDistributorBarcodeTest`).

## Recommendations

`POST /api/v1/h5app/wxapp/goods/recommendations/match` → `GoodsRecommendMatchService`: scene-gated rule matching keyed by `goods_id`, salability filtering (default-SKU store, max SKU store per SPU), member-price application and i18n. Per ECX-10357, already-carted / current-page main items are **no longer excluded** from results — only unsellable items drop out. At checkout, `GoodsRecommendCheckoutMergeService` appends recommend lines after the main items and marks them `item_is_recommend` so `OrderCreateFormatDataPortImpl` prices them from their own spec instead of the cart row.

## Invariants and failure semantics

* **Oversell guard is Redis-first.** Every deduct is an `INCRBY -num` with a compensating rollback when the result would go negative; the DB write-back of the new store happens only on success. Order create turns a `false` into `商品库存不足`. Operator fast-buy adds an earlier, UI-level guard on top: shop stock present → `当前商品门店有库存，请从收银加购`; cloud stock short → `库存不足`.
* **Company + shop scoping everywhere.** Items, barcodes, categories and cart/promotion scope queries are all `(company_id, distributor_id…)`-bounded; the create gate refuses operators touching a distributor they do not own; the operator-cart SKU resolver refuses cross-shop items with `无效商品`. Tag/keyword detail lookups and group-item relation pages treat foreign-company or company-less rows as missing (ECX-10946/47/48), and keyword writes refuse to take over another tenant's row (`记录不存在`).
* **The fast-buy bucket is a single line per (company, operator, distributor).** A new item replaces the previous one unless `accumulate=true` and the item matches; the bucket TTL is 600 seconds, and checkout revalidates gate + stock before reading it.
* **Money is fen.** All write paths convert yuan input to integer fen (`moneyToFen` in `NormalItemTypeHandler`, `moneyFen` in supplier add) before persistence.
* **Post-commit only after real commit.** `ItemAddEvent`, `ItemDeleteEvent` and the category-add event are registered as `TransactionSynchronization.afterCommit` (or run immediately when no synchronization is active, e.g. in tests). `ItemCreateEvent` is the intentional in-transaction exception.
* **SKU-group integrity.** Create/update unify `goods_id` + `default_item_id` + `is_default` across siblings; delete cascades across all sibling SKUs and their satellites in one transaction; the unique `(company_id, distributor_id, item_bn)` index is the backstop.
* **A deleted SKU never lingers in an operator cart.** Delete purges `operator_cart` rows eagerly (`OperatorCartCleanupService.deleteByCompanyIdAndItemIds`, first step of `deleteCore`, inside the same transaction), and the operator-cart list path lazily removes rows whose SKUs no longer resolve (`商品已失效` on submit).
* **Barcode uniqueness is per shop**, enforced by application-level check (delete-then-insert), pinned by `ItemsBarcodeRepositoryShopScopedTest`.
* **Pharma gating.** Prescription drugs cannot go on sale without the pharma-industry setting and a passed medicine audit; medicine submits go through `job:72` to kuaizhen580 with audit results written back to `items_medicine`.
* **ERP failures never break the write path** — JST/WDT/OME jobs are async bus messages; the JST inventory persist is idempotent per item (store recompute from a full snapshot).
* **Search-index failures never break the write path** — every incremental dirty event is deferred to `afterCommit`, retried 3 times and only logged on final failure; the front list degrades to the DB path (`likeFallback=true`) or an empty body. An upsert whose source no longer materializes becomes a document delete, and the per-company cron rebuild converges any residue a listener missed.

## Extension points

* **New item type behavior** — implement `ItemTypeHandler` (`preRelItemParams`/`createRelItem`) and both `PlatformItemsAddService` and `SupplierItemsAddService` pick it up by `item_type`.
* **New integration reacting to item lifecycle** — register a listener in a bootstrap `*RegistrationConfig` against one of the `GoodsDispatchEventNames` events; goods services need no change.
* **New inventory pool** — extend `InventoryDeductTargetResolver` (and the `InventoryDeductTarget` enum in common); the orchestrator and order-create port adapt automatically.
* **New order-create concern on the goods side** — the common ports (`OrderCreateFormatDataPort`, `OrderCreateInventoryPort`, `OrderCreateItemCheckPort`, `OrderCheckoutCartPort`, `OrderCreateNeedParamsPort`) are single-implementation interfaces; compose inside the existing impls.
* **New operator-cart concern** — the `ecshopx-common` operatorcart facades (`OperatorCartSkuLoadFacade`, `OperatorCartDataListFacade`, `OperatorCartFastBuyFacade`, `OperatorCartScanBarcodeFacade`) are goods-implemented single-implementation interfaces that companys/kaquan depend on; adding item-facing behavior to the shopadmin cart goes into these impls (or a new common facade), never into companys.
* **New OpenAPI item surface** — add a port impl in `openapi/` and expose it from `ecshopx-openapi`; mappers are reused.
* **New search-index freshness source** — any write path that changes list-visible goods data calls `GoodsSearchIndexDirtyFanout.scheduleUpsertForDefaultItem` (or listens to a promotion/stock event and goes through `GoodsSearchIndexDirtyByItemIdsPort`); the scheduler/updater chain needs no change.

## Focused tests

`src/test` pins the behaviors this page describes (use them as invariants when refactoring):

* `ItemsBarcodeRepositoryShopScopedTest` — same-shop duplicate barcode throws, different shop inserts, platform pool uses distributor `0`.
* `ItemInventoryOrchestratorServiceTest` / `InventoryDeductTargetResolverTest` — deduct pool routing (total-store → `items`, shop pool → `distributor_items`, platform → plain key; supplier logistics → supplier pool).
* `OrderCreateInventoryPortImplLimitedBuyTest` — limited-buy and seckill purchase recording after `minusStores`.
* `OrderCreateFormatDataPortImplDepositBalanceTest` / `…ShopIdAlignPhpTest` — deposit gating, `shop_id=0` PHP parity.
* `OperatorCartFastBuyShopStockTest` — fast-buy shop-stock split (platform model → 0, headquarters shipment → 0, store shipment → item store).
* `OperatorCartSkuLoadFacadeImplCouponScopeTest` — coupon scope tag merge (SPU tags onto SKU).
* `OperatorCartScanBarcodeFacadeImplTest` — shop-scoped barcode resolution for scan-add.
* `WxappGoodsItemsListQueryOrchestratorCategoryTest` / `…KeywordTest` — empty category/keyword hits force `[-1]` no-match; standard `is_can_sale` uses the store-shelf virtual join, non-standard keeps the direct distributor filter.
* `WxappGoodsItemsListParamBuilderCategoryTest` — empty category tree resolves to `[-1]` before the orchestrator sees it.
* `OrderCheckoutFullGiftServiceItemOrderTest` / `OrderCheckoutFullGiftServiceGiftStoreTipTest` / `GiftActivityStoreAdjustServiceTest` — gift placement, join limits, store clamping and tips.
* `CheckoutCartLogisticsSupplierStoreClampServiceTest` / `ItemAvailableStoreResolverTest` — checkout clamp rules.
* `WxappH5DistributorCartPromotionServiceScopeBoundTest` — distributor cart promotion scope (tag-scope vs specified-SKU fallback).
* `WxappScanCodeAddCartServiceDistributorBarcodeTest` — shop-scoped barcode resolution.
* `PlatformItemsAddServiceItemCreatePublishUpdateParamsWireTest` — `ItemCreateEvent` published exactly once, `ItemAddEvent` scheduled per SKU.
* `PlatformItemsDeleteServiceItemDeleteEventPublishProbeTest` — the delete flow: operator-cart purge invoked with the company + SKU ids, `ItemDeleteEvent` published exactly once via `publishAfterCommit` with the `item_id`/`company_id`/`del_ids`/`item_info` payload shape, for both default and shop-scoped (`distributor_id` query) deletes. Plus `ItemsUpdateOrchestratorDistributorSyncTest`, `SupplierItemsAddServiceSyncPlatformIfNeededTest`, `SupplierItemsReviewGoodsService*Test` — update/supplier flows and event wiring.
* `ItemsBatchUpdateStoreOrchestratorSupplierAuditTest` / `…ScopeTest` — supplier batch store also stamps `audit_status=processing`; the single-item platform path only seeds Redis when the company-scoped update hits.
* `ItemsTagsQueryServiceCompanyScopeTest`, `GoodsKeywordsQueryServiceCompanyScopeTest`, `GoodsKeywordsWriteServiceCompanyScopeTest`, `ItemsGroupGetGroupItemsServiceCompanyScopeTest` — the ECX-10946/47/48 (and ECX-10889) tenant-isolation pins for tag detail, keyword detail/write and group-item pages.
* `DistributorGoodsSyncServiceAddDistributorItemsDispatchTest` — auto-sync fan-out and job enqueue.
* `ItemsCategorySaveServiceApiConvergenceDispatchTest`, `ItemsCategoryFrontDisplayResolverTest` — category save dispatch + front display rules.
* `ItemsMultiLangWriteServiceTest`, `ItemsApproveStatusSyncServiceTest`, `ItemsMedicineServiceUpdateItemMedicineDataDispatchTest` — multi-lang, status cascade, medicine dispatch.
* `JushuitanInventoryPersistItemStoreDispatchPublishProbeTest`, `Ome*InitialDispatchTest` — ERP sync wiring.
* `WxappGoodsItemsListServiceImplEsSwitchTest`, `WxappGoodsItemsListRestoreSalePriceTest` — the ES/DB list switch and the sale-price restore rule.
* `GoodsSearchQueryServiceTest` / `…RankingTest` — wxapp-param mapping, synonym-expanded keywords feeding the query builder, hard-filter composition, effective-price filter consistency and the hierarchical default sort.
* `SearchRebuildServiceTest`, `GoodsSearchIndexRepositoryTest`, `GoodsSearchMappingServiceTest`, `GoodsSearchDocumentMaterializationTest`, `SearchIndexMapperTest`, `SearchIndexStoreItemKeyOrderTest`, `MybatisSearchIndexSourceLoaderFetchPageTest` / `…StoreOwnedTest` — blue-green create/bulk/switch ordering, no-switch-on-bulk-failure, mapping payload, document materialization and the store-visibility rules of the source loader.
* `SearchIndexUpdaterTest` / `…MemberActivityTest`, `GoodsSearchIndexDirtyFanoutTest`, `GoodsSearchIndexPromotionSyncListenerTest` — incremental upsert/delete (retry, upsert-becomes-delete), the distributor `0` + shop fan-out set, and the seckill/groups committed-event triggers.
* `SearchSynonymServiceTest`, `SearchSynonymKeywordExpanderTest`, `SearchSynonymEsReloaderTest`, `SearchSynonymImportRowServiceTest`, `SearchSynonymImportFinishPortImplTest` — synonym group CRUD/cache invalidation, query-time expansion, ES synonyms-set reload and import-row persistence.
* `GoodsRecommend*Test` (match, scene, salability, checkout merge) and `OrderCheckoutCartPortImplTest` — recommendation and checkout-merge behavior.

## Related pages

* [Persistence (MyBatis)](../concepts/persistence-mybatis.md) — the `@MpField`/`@MpTable` metadata behind the `items` schema.
* [Dispatch Bus](../concepts/dispatch-bus.md) — the transport under every event and job here.
* [Redis Databases](../concepts/redis-databases.md) — the `companysRedisTemplate` vs `sharedStringRedisTemplate` split used by `item_store` keys and cart.
* [Orders Domain](orders.md) — the other half of order create: the port callers, order status, cancel/restore flows.
* [Stores and Merchants](stores-and-merchants.md) — distributors, product models and the `distributor_items` contract.
* [Marketing and Loyalty Domains](marketing.md) — the seckill/groups/full-gift activities materialized into the search index and applied at checkout.
<!-- openwiki: broken internal link [../workflows/operator-fast-buy-flow.md] file "../workflows/operator-fast-buy-flow.md" does not exist. Fix the href or restore the target, then delete this comment. -->
* [Operator Fast-Buy Flow](../workflows/operator-fast-buy-flow.md) — the end-to-end shopadmin fast-buy trace across the facades documented here.
* [Third-Party Integrations](../integrations/third-party.md) — OME, Jushuitan, WdtErp, youshu, marketing-center and Shuyun consumer surfaces.
