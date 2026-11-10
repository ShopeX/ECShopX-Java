---
type: domain-reference
title: Members Domain (ecshopx-members)
description: The member/account bundle — H5 and wxapp login orchestration behind espier's LoginController, HS256 JWT issuing/verification and blacklist-backed token refresh, member account creation with in-transaction promoter/distribution side-effects and the post-commit CreateMemberSuccess dispatch fan-out to CRM/Shuyun/Youshu/promotions/points/datacube, member update and mobile-change flows, wechat fans/tag sync, the goods-arrival subscription surface (缺货订阅) whose wxapp subscribe-message notices fire from the goods bundle's event:258 item-store update, overseas trust login (Apple/Google/Facebook/Line), member-side SMS verification codes, the hardened address buy_user_id scoping on the front MembersController, and the member system-tag subsystem (tag_kind/tag_code schema, new_customer/birthday_this_month/sleeping_user reconcile services with cron and dispatch listeners, write guard, audit table).
tags: [members, h5, login, jwt, auth, wxapp, trust-login, wechat-fans, dispatch-bus, create-member-success, sms, shuyun, youshu, dm-crm, datacube, promotions, redis, goods-arrival, subscribe-notice, wxa-notify, item-store-update, system-tags, member-tags, reconcile, address-scope, xxl-job, audit]
sources:
  - id: openwiki-source-aefde5c5815dc42e82aa67f0
    resource: repo://ecshopx-ali/src/main/java/cn/shopex/ecshopx/ali/service/alitemplate/ItemStoreGoodsArrivalAliTemplateOrchestrator.java
  - id: openwiki-source-068fed664bb9743658663609
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DatacubeRegisterNumStatsCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-49000d10a7386485eccf605d
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemStoreUpdatedEventBusPublisherImpl.java
  - id: openwiki-source-c41d01f5b782c3b4360a1869
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ItemStoreUpdatedEventDispatchListenerRegistrationConfig.java
  - id: openwiki-source-1b5abe9a617b898225d1c6dd
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MembersCreateMemberSuccessDispatchPublisherImpl.java
  - id: openwiki-source-4079f3bdff589d9fa360eb2e
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MembersCreateMemberSuccessNewCustomerDispatchListenerRegistrationConfig.java
  - id: openwiki-source-f5d41797c5b93672758b511c
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MembersCreateMemberSuccessNoticeDispatchListenerRegistrationConfig.java
  - id: openwiki-source-9ce82340fe6f5be0bfcfe0a7
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MembersNormalOrderCancelNewCustomerDispatchListenerRegistrationConfig.java
  - id: openwiki-source-0315455e54e2688cacf7a480
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MembersNormalOrderPaySuccessNewCustomerDispatchListenerRegistrationConfig.java
  - id: openwiki-source-1934321a6d758092fa3b0e5a
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/PromotionsCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-1a74edff54f16eb2ef107c71
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/PromotionsSendMembercardCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-c50797cf3bd5f6a4c988d6a9
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/RegisterPointCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-0987b0da75587e95d8e17649
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShopexCrmCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-ebbcd0aeec7940f4f0465f31
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-ae5ddc2afd4f9ce163c8bc34
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformItemStoreProductSyncDispatchListenerRegistrationConfig.java
  - id: openwiki-source-2843c6d975e62568ed349924
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/YoushuCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-89b43138f8e75edb4615495b
    resource: repo://ecshopx-bootstrap/src/main/resources/application.properties
  - id: openwiki-source-306979beecba6167a958ddc4
    resource: repo://ecshopx-bootstrap/src/main/resources/db/migration/V00000000000006.sql
  - id: openwiki-source-b0eb945d98bdcd196458e4d1
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/dispatch/ItemStoreUpdatedAliTemplateBusDispatchFlowTest.java
  - id: openwiki-source-18166d6db75da9333bacd41b
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/auth/H5JwtBlacklistPort.java
  - id: openwiki-source-7265523c083a528e5f5d8893
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/GoodsDispatchEventNames.java
  - id: openwiki-source-03eb47a2257dd8b22aaaa750
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/MembersCreateMemberSuccessDispatchPublisher.java
  - id: openwiki-source-e88aaedda534a005ae27fbdf
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/MembersUpdateMemberSuccessDispatchPublisher.java
  - id: openwiki-source-a3330859dff3ac5b6939c02d
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/port/goods/DistributorItemTotalStorePort.java
  - id: openwiki-source-d1d0e9b6a5833759aebc5ea7
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/port/members/GoodsArrivalWxaSubscribeSendPort.java
  - id: openwiki-source-d96e319986cc81e25dd912b7
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/api/front/v1/LoginController.java
  - id: openwiki-source-fde2d214766049b8bb873842
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/H5JwtBlacklistService.java
  - id: openwiki-source-af38db4fe5df8b1855c6eed9
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/service/front/FrontWxappNewLoginService.java
  - id: openwiki-source-9fd6b26e404a7a61b8dbad21
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/FrontAuthInterceptor.java
  - id: openwiki-source-443dfcf543bf42ef5d465397
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/FrontNoAuthInterceptor.java
  - id: openwiki-source-8d11fe28b36d2eaf4389512a
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/integration/members/DistributorItemTotalStorePortImpl.java
  - id: openwiki-source-367cd46d4f84bd018f370f54
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/api/admin/v1/MembersSubscribeController.java
  - id: openwiki-source-80077d2f69a839e9693e1a07
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/api/front/v1/MembersController.java
  - id: openwiki-source-b6b6207b4d34f97dd04aed61
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/api/front/v1/TrustLoginAppleCallbackController.java
  - id: openwiki-source-d047c7468d273d91dfd23e49
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/config/H5JwtProperties.java
  - id: openwiki-source-0dc8108aad3a021385563889
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/cron/ScheduleBirthdayThisMonthTagHandler.java
  - id: openwiki-source-d1ae183c81bdc1fdae2f0ac4
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/dispatch/CreateMemberSuccessNewCustomerDispatchListener.java
  - id: openwiki-source-d332b3afaea16086bf6d2dd6
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/dispatch/NormalOrderCancelNewCustomerDispatchListener.java
  - id: openwiki-source-07dc9354b98cdbc7dac4f2d1
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/dispatch/NormalOrderPaySuccessNewCustomerDispatchListener.java
  - id: openwiki-source-f3a1f1ca529c7133c628618f
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/domain/SubscribeNotice.java
  - id: openwiki-source-ad7bfcc65994f6bf9f3fb14e
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/listener/CompanyActivatedSystemTagListener.java
  - id: openwiki-source-452f7de77c6a6d3a53a5bf10
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/listener/ItemStoreUpdatedGoodsArrivalDispatchListener.java
  - id: openwiki-source-329e433271ed5b9c82fd2046
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/listener/MemberSystemTagHistoricalSeedRunner.java
  - id: openwiki-source-37f8235b20fdd10827bbdef2
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/listener/TradeRefundNewCustomerListener.java
  - id: openwiki-source-88e01cb173099cd318238ee4
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/security/LegacyFixedMobileEncrypt.java
  - id: openwiki-source-d72b1aefaba3b3b4fbda25c4
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/account/MemberAccountService.java
  - id: openwiki-source-ac7a26dc1054fecc29313023
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/admin/MembersSubscribeNoticeListService.java
  - id: openwiki-source-6958e663841b98e13623fc53
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/email/MemberSyntheticMobileService.java
  - id: openwiki-source-03a7f358ce9fb661e2ca984b
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/GoodsArrivalSubscribeMatchService.java
  - id: openwiki-source-8cd9b4bb39e3bdd7c0366be9
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/auth/H5AuthStrategy.java
  - id: openwiki-source-9897b468017456cf381a4f01
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/auth/H5AuthType.java
  - id: openwiki-source-88a0c499a7f6cc6063a4684a
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/auth/WxAppH5AuthStrategy.java
  - id: openwiki-source-bcddf0e5f5b144bdff1039b9
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/EspierLocalUserResolver.java
  - id: openwiki-source-cc41dee55bf05e6db3079efb
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/H5BearerJwtClaimsService.java
  - id: openwiki-source-7adc9c2422df14699df5f15a
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/H5JwtIssuer.java
  - id: openwiki-source-d6db6599c5f76b9420c3224a
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/H5JwtUserLoginInfoEnrichService.java
  - id: openwiki-source-ac00df043e31c2f779007414
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/H5LoginOrchestrator.java
  - id: openwiki-source-f8895f53dee34ac1909e2253
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/H5SocialOAuthLoginService.java
  - id: openwiki-source-97b316b1fbeafbc81adc8312
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/prelogin/H5WxappLoginRegisterFacade.java
  - id: openwiki-source-66d49b363643c4169f112cb5
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/prelogin/H5WxappPreLoginService.java
  - id: openwiki-source-e2488bae085338c037053721
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/WxappMemberMobileChangeService.java
  - id: openwiki-source-5d5765d32775f5ad02f58757
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/WxappMemberUpdateMemberService.java
  - id: openwiki-source-da52a7049271f0ed0625b0f2
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/ItemStoreGoodsArrivalWxaTemplateOrchestrator.java
  - id: openwiki-source-1712ab006fdbf434bafc3169
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/MemberTagsCreateService.java
  - id: openwiki-source-6ced3d2bfe3c421ea3098928
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/sms/MemberSmsVerificationCodeDeliveryService.java
  - id: openwiki-source-ddc4441d84575dd506f9b822
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/BirthdayThisMonthReconcileService.java
  - id: openwiki-source-71f800dcbe73d59f7d04af1d
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/MemberAudienceTagValidator.java
  - id: openwiki-source-e2cfac4ae8d1079dfdd55c73
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagCodes.java
  - id: openwiki-source-ec1010c0d063326efaf9de3f
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagQueryScope.java
  - id: openwiki-source-7b93b434cb08968740f6b9e2
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagSeedService.java
  - id: openwiki-source-a204a89b1ce5a22cd0a0fe06
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagWriteGuard.java
  - id: openwiki-source-1d035a8dc7df21b6b6e6293f
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/NewCustomerEffectivePaidOrderQuery.java
  - id: openwiki-source-7681fec725fb0dff41f89dce
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/NewCustomerTagReconcileService.java
  - id: openwiki-source-cb52f315fb94b2b597d772ca
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/SleepingUserLastCompletedOrderQuery.java
  - id: openwiki-source-2fa3fab0fcfbd1087f3968d6
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/SleepingUserTagReconcileService.java
  - id: openwiki-source-9e5ba1b03959db9c3ca0ecc3
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/trustlogin/SocialTrustLoginService.java
  - id: openwiki-source-57a176485cdb255bfe68fd95
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/trustlogin/SocialTrustLoginTypes.java
  - id: openwiki-source-97f325e6c13a487b3e7ef6f4
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/WechatFansSyncService.java
  - id: openwiki-source-3bb58f0ac86d69010cf539ec
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/wxapp/GoodsArrivalNoticeSubscribeRedisKeys.java
  - id: openwiki-source-2da9676c04bb79e8e655eb5e
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/wxapp/WxappGoodsArrivalNoticeSubscribeService.java
  - id: openwiki-source-2425b7a1af8631937f2e830a
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/api/front/v1/MembersControllerAddressBuyUserScopeTest.java
  - id: openwiki-source-4836dae026b0cbd94cd9416b
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/account/MemberAccountServiceCreateMemberForWxappBindNoBindSalespersonDispatchTest.java
  - id: openwiki-source-6abe756b6b1ec9215d78d6c9
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/account/MemberAccountServiceCreateMemberLocalAutoRegisterDispatchTest.java
  - id: openwiki-source-87fb25a888939ab7f8b6ec03
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/GoodsArrivalSubscribeMatchServiceTest.java
  - id: openwiki-source-ebeaa8eab25d1fdbf30994ef
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/h5/prelogin/H5NewLoginPathsCreateMemberSuccessDispatchProbeTest.java
  - id: openwiki-source-5328b6eb55d9372bad32201a
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/ItemStoreGoodsArrivalWxaTemplateOrchestratorTest.java
  - id: openwiki-source-c590b40d0618e09eba6414ee
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagSeedServiceTest.java
  - id: openwiki-source-65806a16fc6ef7f885cfdb29
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagWriteGuardTest.java
  - id: openwiki-source-8b507d26166423a2c08f6bce
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/systemtag/NewCustomerTagReconcileServiceTest.java
  - id: openwiki-source-b8aa34ba6ad29cda61e4b104
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/systemtag/SleepingUserTagReconcileServiceTest.java
  - id: openwiki-source-8978c6a395936578f3e3b971
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/finish/FinishOrderJobService.java
  - id: openwiki-source-043e01b99cf95e87d89828b8
    resource: repo://ecshopx-promotions/src/main/java/cn/shopex/ecshopx/promotions/integration/members/GoodsArrivalWxaSubscribeSendAdapter.java
  - id: openwiki-source-df26736cc03664d777529023
    resource: repo://ecshopx-promotions/src/main/java/cn/shopex/ecshopx/promotions/service/MarketingActivityCreatePersistenceService.java
  - id: openwiki-source-9c61992f85949d88e97006d2
    resource: repo://ecshopx-promotions/src/main/java/cn/shopex/ecshopx/promotions/service/wxatemplate/WxaTemplateSceneDefinitions.java
  - id: openwiki-source-eab9c10485100198740b4183
    resource: repo://ecshopx-wechat/src/main/java/cn/shopex/ecshopx/wechat/repository/WeappAuthorizerAppidRepository.java
generated: { by: "openwiki/0.9.0", at: "2026-11-09T17:09:44.795Z" }
verified:
  - by: openwiki/0.9.0
    at: 2026-11-09T17:09:44.795Z
---

# Members Domain (ecshopx-members)

**`ecshopx-members`** (会员与账户域, see `ecshopx-members/src/main/java/cn/shopex/ecshopx/members/package-info.java`) is the member/account Bundle: member records and their wechat/alipay/social associations, the H5 (wxapp/H5/PC) login orchestration that mints member JWTs, the JWT verification that every member-facing endpoint relies on, the member-create / member-update fan-out that drives CRM sync, register promotions, register points, datacube counters and membercard gifts, and the goods-arrival subscription surface (缺货订阅) whose wechat notices fire from the goods bundle's item-store event.

Module layout follows the standard split ([Bundle Layering](../architecture/bundle-layering.md)): `api/front/v1` + `api/admin/v1` controllers, `service/{account,h5,front,admin,reg,sms,trustlogin,wxapp,...}` orchestration, `dispatch/` job publishers/handlers/listeners, `listener/` cross-bundle event listeners, `integration/` port impls for other bundles, `domain/` + `mapper/` persistence. `ecshopx-members` declares `ecshopx-common`, `ecshopx-dispatch`, `ecshopx-third-party`, `ecshopx-companys`, `ecshopx-work-wechat` and `ecshopx-wechat` (`ecshopx-members/pom.xml`) plus `nimbus-jose-jwt` for JWT.

## The espier/members split for login endpoints

One architectural fact that surprises everyone first: **the H5 login *routes* live in `ecshopx-espier`, the login *services* live in `ecshopx-members`**. `ecshopx-espier` depends on `ecshopx-members` (not the other way), so espier's `LoginController` (`ecshopx-espier/.../api/front/v1/LoginController.java`) is a thin `@FrontNoAuth` `@RestController("/api/v1/h5app")` whose single endpoint `POST /api/v1/h5app/wxapp/new_login` (name `C端新登录`) merges query/body params via `H5LoginRequestAssembler` and hands off to `FrontWxappNewLoginService`. The controller deliberately publishes no dispatch messages itself: whenever the path creates a new member record, follow-on work is coordinated from the member account layer after the surrounding transaction commits (the controller's own Javadoc says exactly this).

The mirror-image route `POST /api/v1/h5app/wxapp/login` (`C端登录`) plus `GET /api/v1/h5app/wxapp/token/refresh` (`C端刷新token`) live in members' own `MembersController` (`ecshopx-members/.../api/front/v1/MembersController.java`), also `@FrontNoAuth`, calling the same `H5LoginOrchestrator` / `H5MemberTokenRefreshService`. Both controllers are `@DingoResponse(resource = DINGO, badRequest = DINGO_400, unauthorized = true)` because the legacy H5 frontend parses `data.message` / `data.status_code` ([Request Lifecycle](../architecture/request-lifecycle.md)).

`H5LoginRequestAssembler` is the PHP-parity merge step: query parameters first, JSON body over them, plus the `Origin` header as `origin`.

## Login orchestration: FrontWxappNewLoginService → H5LoginOrchestrator → H5AuthStrategy

`FrontWxappNewLoginService.login` normalizes the request per `auth_type`, runs the *pre-login* (which can create the member), then delegates to the members-side orchestrator:

* reads `company_id` from the `H5_COMPANY_ID` request attribute (set by `FrontNoAuthInterceptor`, below) and throws `UnauthorizedException` when it is not positive;
* `auth_type=local` → a fresh credential map (`username`, `vcode`, `check_type` defaulting to `password`, `password`, `auto_register`, `silent`, `company_id`); `pre_login_data` echoes `{mobile: username}`;
* `auth_type=wxapp` → copyWhitelisted params (`appid`, `code`, `iv`, `encryptedData`, `inviter_id`, `salesperson_id`, `work_userid`, `channel`, `shuyunappid`, ...), inject `wxa_appid` / `authorizer_appid` / `api_from` from existing JWT claims when present, then `H5WxappPreLoginService.wxappPreLogin(params)`;
* `auth_type=aliapp` → the analogous `H5AliappPreLoginService.aliappPreLogin`;
* `auth_type=wx_offiaccount` / `social_oauth` → pass-through credentials with `company_id` + `origin` forced;
* anything else → `ResourceException("缺少参数，登录失败！")`.

Then `H5LoginOrchestrator.attempt(credentials)` runs:

1. `EspierLocalUserResolver.retrieve(credentials)` — resolves `company_id` from the Origin domain when missing, picks a strategy from `auth_type`, and on success appends accepted protocol logs (`MembersProtocolLogOnLoginService`) and runs `CompanysUserAuthChecker.checkUserAuth` (tenant gate).
2. If a user resolved, `H5JwtIssuer.issue(user)` mints the JWT; a `JOSEException` is wrapped as `IllegalStateException`.
3. `H5LoginAttemptResult(success, jwtToken, credentialsRejected)` is returned; the caller distinguishes a hard failure from a soft ("please register") rejection.

**The strategy registry is the extension point.** `H5AuthStrategy` is a two-method interface (`type()` / `resolve(credentials)`), and `EspierLocalUserResolver` builds an `EnumMap<H5AuthType, H5AuthStrategy>` from all Spring beans. Adding a login channel = new enum constant + new `@Component` strategy; no resolver edit. Current strategies in `service/h5/auth/`:

| `H5AuthType` | Strategy | What it does |
|---|---|---|
| `LOCAL` (default when `auth_type` blank) | `LocalH5AuthStrategy` | mobile-or-email + password / SMS vcode / email OTP, optional `auto_register` → `createMemberLocalAutoRegister`; email login requires `email_verified_at` |
| `WXAPP` | `WxAppH5AuthStrategy` | mini-program `code2session` (or pre-supplied `openid`), `getWechatSimpleUser`/`getWechatUserInfo`, OEM-Shuyun silent-register bridge, `need_transfer` guard in transfer mode |
| `OAUTH` | `OauthH5AuthStrategy` | PC web OAuth: `member:oauth:login:<token>` Redis payload must match the wechat user's `unionid` |
| `WX_OFFIACCOUNT` | `WxOffiaccountH5AuthStrategy` | official-account `oauth2SnsAccessToken` + `snsUserinfo`, creates the fan row via `createOffiaccountFans` |
| `PC_WXQRCODE` | `PcWxQrcodeH5AuthStrategy` | PC QR-scan: same official-account calls, but with caller-supplied `appid`/`secret` |
| `ALIAPP` | `AliAppH5AuthStrategy` | Alipay mini-program: `alipay_user_id` (given or exchanged from `code`) → `members_associations` row with `user_type=ali` |
| `SOCIAL_OAUTH` | `SocialOAuthH5AuthStrategy` → `H5SocialOAuthLoginService` | overseas trust login, see below |
| `UNKNOWN` | `DefaultH5AuthStrategy` | always returns `Optional.empty()` (soft failure) |

Each strategy returns `H5GenericUser`, a thin map wrapper whose `getJwtSubject()` is the `id` attribute and whose `getJwtCustomClaims()` is the whole attribute map. Login paths therefore build the **`id` claim as an `_espier_`-joined composite**: `"<userId>_espier_<open_id>_espier_<unionid>"` for wechat (`WxAppH5AuthStrategy.buildUserAttrs`, `OauthH5AuthStrategy`, `WxOffiaccountH5AuthStrategy`) or `"<userId>_espier_alipay_espier_<alipayUserId>"` for Alipay. `MemberAccountService.getTokenData` instead builds `"<user_id>_espier_companyid_espier_<company_id>"` for the local/bind paths — the same shape `H5SocialOAuthLoginService` builds for trust-login members. The `_espier_` shape is not decoration — `H5JwtUserLoginInfoEnrichService` parses it back on every authenticated request (below).

### wxapp pre-login: where the member is actually created

`H5WxappPreLoginService.wxappPreLogin` is the new-login workhorse. After validating `appid`/`code`/`iv`/`encryptedData` and asserting the appid is recognized, it: `miniProgramCode2Session` → decrypt the phone number (`purePhoneNumber`) → in transfer mode repair a stale unionid → resolve inviter params from `uid`/`puid`/existing wechat row → then the branching that decides whether a member is created:

* no member by mobile → `H5WxappLoginRegisterFacade.registerMemberForWxapp`;
* member exists but no `wechat` association with a matching unionid → also `registerMemberForWxapp` (which will take the "existing member, bind only" branch);
* OEM-Shuyun + member with a *different* unionid → `ResourceException("该手机号已注册为会员，请更换手机号！")`.

Afterwards it binds a salesperson if `salesperson_id` was supplied, records the wxapp fan row, and re-syncs the Shuyun open platform (failures do not block login, matching PHP `maybeSync…AfterPreLogin`).

`H5WxappLoginRegisterFacade.registerMemberForWxapp` is `@Transactional` and holds the registration policy: whitelist check (`assertWhitelist` against `members_whitelist` with the fixed-encrypted mobile), employee-auth (`employee_auth` payload → optional `H5WxappRegisterEmployeeAuthenticator` bean, `enterprise_id` required) **or** invite code (`invite_code` → optional `H5WxappRegisterInviteBinder` bean, Redis lock acquired first and quietly unlocked on failure), the two OEM-Shuyun short-circuits, then `memberAccountService.createMemberLocalAutoRegister(companyId, mobile, randomPassword, registrationExtras)` plus `recordWxappFansAfterMember` (writes `wechat_users`, and `members_associations` only when `memberUserId > 0`) and `ShuyunOpenPlatformWxappMemberSyncPort.syncWxappOnlineIfEnabled`. `registerMemberForAliapp` mirrors this for Alipay, routing to `registerShuyunMember` under OEM-Shuyun and otherwise `createMemberLocalAutoRegister` + `ensureAliMemberAssociation`.

Both invite-binder and employee-authenticator are `ObjectProvider`-injected optional extensions — the facade does not fail startup when the implementing bundles are absent, but it does throw `ResourceException` if a request asks for a feature whose bean is missing.

```mermaid
sequenceDiagram
    participant H5 as H5 or wxapp client
    participant LC as LoginController espier
    participant FW as FrontWxappNewLoginService
    participant PL as H5WxappPreLoginService
    participant RF as H5WxappLoginRegisterFacade
    participant MAS as MemberAccountService
    participant ORC as H5LoginOrchestrator
    participant JI as H5JwtIssuer

    H5->>LC: POST /api/v1/h5app/wxapp/new_login auth_type=wxapp
    LC->>FW: login(request, merged params)
    FW->>PL: wxappPreLogin(whitelisted params)
    PL->>PL: code2session, decrypt phone, resolve inviter
    PL->>RF: registerMemberForWxapp(params, wxSession)
    RF->>MAS: createMemberLocalAutoRegister(companyId, mobile, pwd, extras)
    MAS->>MAS: insert members + members_info, register afterCommit
    RF->>RF: recordWxappFansAfterMember, employee-auth, invite bind
    PL->>FW: user_id, open_id, unionid
    FW->>ORC: attempt(credentials with openid and unionid)
    ORC->>ORC: EspierLocalUserResolver WxAppH5AuthStrategy
    ORC->>JI: issue(H5GenericUser)
    JI-->>ORC: HS256 JWT
    ORC-->>FW: H5LoginAttemptResult success
    FW-->>H5: data.token
```

Caption: the new-login path — pre-login creates the member inside a transaction, then the orchestrator re-resolves the user by openid/unionid and issues the JWT.

## JWT: issuing, verification, refresh and blacklist

All H5 member tokens are **HS256 (Nimbus) JWTs**, configured by `ecshopx.h5.jwt.*` (`H5JwtProperties`, bound by `MembersH5Configuration`):

| Property | Default (in `ecshopx-bootstrap/src/main/resources/application.properties`) | Meaning |
|---|---|---|
| `ecshopx.h5.jwt.secret-base64` | `${JWT_SECRET}` (must be Base64 of a 32-byte key) | shared signing key |
| `ecshopx.h5.jwt.ttl-minutes` | `${JWT_TTL:120}` | access-token lifetime |
| `ecshopx.h5.jwt.refresh-ttl-minutes` | `${JWT_REFRESH_TTL:20160}` (14 days) | window from `iat` in which refresh is accepted |
| `ecshopx.h5.jwt.issuer` | `ecshopx` | required `iss` claim value |
| `ecshopx.h5.jwt.leeway-seconds` | 0 | clock skew allowance for `exp`/`nbf`/`iat` |
| `ecshopx.h5.jwt.blacklist-grace-period-seconds` | `${JWT_BLACKLIST_GRACE_PERIOD:0}` | seconds the blacklisted token still works after invalidation |
| `ecshopx.h5.jwt.required-claims` | `iss,iat,exp,nbf,sub,jti` | startup-validated: must contain all six |

`H5JwtProperties.resolveSecretBytes` does a strict Base64 decode and zero-pads keys shorter than 32 bytes so Nimbus `MACSigner` accepts them. `@PostConstruct` validation refuses a `required-claims` list missing any of `iss,iat,exp,nbf,sub,jti` and a negative grace period, so the app fails fast on bad configuration. `JWT_SECRET` is injected by the docker compose files as both an env var and an `--JWT_SECRET=` app arg; it is *not* stored in the repository.

**Issuing** (`H5JwtIssuer.issue`): sets `iss`, `sub` (the `_espier_` composite `id`), a random UUID `jti`, `iat`/`nbf` = now, `exp` = now + ttl, then copies every non-null attribute as a custom claim **except the reserved names** `iss,sub,aud,exp,nbf,iat,jti`. An unconfigured secret throws `IllegalStateException("ecshopx.h5.jwt.secret-base64 must be configured")`.

**Verification** (`H5BearerJwtClaimsService.verifyAndExtractClaims`) is the single gate for `@FrontAuth` endpoints. It rejects, in order: a missing/non-`Bearer` `Authorization` header, a blacklisted compact token, an empty secret, a non-HS256 alg, a bad `MACVerifier` signature, a wrong `iss`, a time-claim failure (with leeway), a missing required claim, and a non-positive/non-integer `company_id` claim. On success it copies the claim map and hands it to `H5JwtUserLoginInfoEnrichService.enrichClaims`, then returns it. All parse/verify failures are `Optional.empty()` — never an exception — so the interceptor decides the HTTP shape.

**`FrontAuthInterceptor`** (espier, `ecshopx-espier/.../web/interceptor/FrontAuthInterceptor.java`) applies to handlers annotated `@FrontAuth` (method or bean) that are not `@FrontNoAuth`. On an empty claims result it writes a Dingo 401 body (`{"data":{"message":"Unable to authenticate user.","code":401001,...}}`); if the enriched claims carry `disabled` (truthy `true`/`"1"`/`"true"`) it writes a Dingo 403 with code `403001` (`该账号已被禁用.`). Otherwise it sets two request attributes consumed everywhere downstream: `H5FrontAuthAttributes.H5_AUTH_CLAIMS` (the whole claim map) and `H5_COMPANY_ID` (the positive-int `company_id`).

**`FrontNoAuthInterceptor`** (espier) is the partner for `@FrontNoAuth` handlers. It first tries the same Bearer-JWT path (and on success enriches claims the same way, plus back-fills a missing `mobile` from the member row via `memberAccountService.resolveMemberMobileForH5Context`); when there is no usable token it builds **anonymous** claims with `H5JwtUserLoginInfoEnrichService.buildAnonymousAuthClaims(companyId, appid, openId, unionid)` — `user_id=0`, `api_from=h5app`, `wxapp_appid` resolved from a `wx…` appid (and `woa_appid` from the tenant's service account), or `alipay_appid` for anything else. The anonymous `company_id` is resolved in PHP `FrontNoAuthMiddleWare` priority: appid lookup (wechat or Alipay port) → fixed non-SaaS / main-tenant id (`common.system-companys-id` / `common.system-main-companys-id`) → `company_id` query → Origin-domain lookup → `UnauthorizedException`. The single carve-out is `MembersController.getNoAuthDecryptPhoneNumber`, which proceeds with `H5_COMPANY_ID=0`.

**Claim rehydration** is what makes the stateless JWT usable: `H5JwtUserLoginInfoEnrichService.enrichClaims` re-reads member and wechat state on every request, mirroring PHP `EspierLocalUserProvider::getUserLoginInfo`. Given the `sub` (or `id`) `_espier_` composite it splits `<userId>`, `<openIdPart>`, `<unionidPart>`; `openIdPart=alipay` resolves the `ali` association, anything else resolves `getWechatUserInfo(unionid, open_id)` and from the wxa appid the company and the woa appid; `openIdPart=companyid` means the second segment *is* the company id. When a member row is found it overwrites/fills `user_id`, `company_id`, `disabled`, `open_id`, `unionid`, `nickname`, `headimgurl`, `grade_id`, `mobile`, `username`, `user_card_code`, `offline_card_code`, `operator_type=user`, `inviter_id`, `source_id`, `monitor_id`, `latest_source_id`, `latest_monitor_id`, `alipay_appid` and `alipay_user_id`. The point is freshness: disable a member and the very next request is rejected, even though the JWT itself is still inside its TTL.

**Refresh** (`GET /api/v1/h5app/wxapp/token/refresh` → `H5MemberTokenRefreshService.tokenRefresh` → `H5JwtIssuer.refreshAccessToken`): the compact token is parsed and verified (same alg/iss/required-registered-claims checks; only registered claims are checked here because a refresh must tolerate a stale custom-claim set), `iat` must be within `refresh-ttl-minutes`, and then the old token is **blacklisted** with `H5JwtBlacklistPort.addToBlacklist(compactJwt, exp)` before a new token is minted from the surviving custom claims (`id` back-filled from `sub` when missing). Blacklisted refresh returns `UnauthorizedException(H5JwtBlacklistPort.TOKEN_BLACKLISTED_MESSAGE)`, which `GlobalExceptionHandler` maps to a bare 401 without a business code.

**The blacklist lives in Redis DB 2 (prism)**: `H5JwtBlacklistService` (espier, implementing the `ecshopx-common` `H5JwtBlacklistPort` used by both the members issuer and verifier) stores `h5_jwt_invalidate:<sha256(compactJwt)>` → `validUntilSec = now + blacklistGracePeriodSeconds`, TTL'd to the token's `exp` (minimum 1s, and at least long enough to cover the grace period). `isBlacklisted` returns true once `now >= validUntilSec` — i.e. the grace period is the window in which a just-invalidated token is still deliberately accepted (PHP `JWT_BLACKLIST_GRACE_PERIOD` parity). See [Redis Databases](../concepts/redis-databases.md) for the DB layout.

## The CreateMemberSuccess after-commit fan-out (canonical example)

`event:members:create_member_success` is the reference AFTER_COMMIT dispatch in this codebase ([Dispatch Bus](../concepts/dispatch-bus.md), [Cross-Bundle Communication](../architecture/cross-bundle-communication.md)). The publisher is the one-method interface `MembersCreateMemberSuccessDispatchPublisher` in `ecshopx-common`, implemented in bootstrap by `MembersCreateMemberSuccessDispatchPublisherImpl` as `DispatchMode.ASYNC`, `DispatchDriverType.REDIS`, default queue, `RetryPolicy.platformDefault()` — so members never depends on the bus directly.

`MemberAccountService` publishes it from a `TransactionSynchronization.afterCommit` in every registration path, and **each path throws `BadRequestException` up front when `TransactionSynchronizationManager.isSynchronizationActive()` is false** — an explicit invariant that the event can never be published for rows that rolled back:

| Registration path | Also in the same `afterCommit` |
|---|---|
| `createMemberForWxappBind` (wxapp bind) | `sharedStringRedisTemplate` daily active set `Member:<companyId>:<yyyyMMdd>` |
| `createMemberLocalAutoRegister` (pre-login, local auto-register) | daily active set; and when `work_userid` + `channel=1`: `BindSalsepersonJobDispatchPublisher.enqueueBindSalseperson` — or `enqueueBindSalsepersonAfterDmMemberCreate` when `DmCrmSettingReadPort.isPointIntegrationOpen(companyId)`. (The promoter/distributor ports + `ShopRelMember` run **in-transaction**, before the synchronization is registered — see below.) |
| `registerShuyunMember` (OEM-Shuyun / Alipay) | daily active set |
| `creatMemberForH5Post` (front registration, social OAuth) via `registerAfterCommitH5CreateMemberSideEffects` | optional daily active set (skipped for re-activated upload members); `work_userid` + `channel=1` bind-salesperson |
| `AdminMemberCreateMemberService.createMember` / `createMemberWithEmail` (admin console) | nothing (the port-driven side effects run in-transaction instead) |

The payload keys are stable across all paths — `company_id`, `user_id`, `mobile`, `openid`, `wxa_appid`, `inviter_id`, `distributor_id`, `source_id`, `monitor_id`, `salesperson_id`, `if_register_promotion` — because downstream listeners read exactly these. `if_register_promotion` is false when the tenant's `member_logout_config.new_rights` is off *and* a `members_delete_record` row exists for that mobile (i.e. the member had deregistered, so they do not get new-member rights again).

### In-transaction side effects of `createMemberLocalAutoRegister`

The wxapp pre-login auto-register path (`createMemberLocalAutoRegister`, called from `H5WxappLoginRegisterFacade.registerMemberForWxapp`, `registerMemberForAliapp` and `LocalH5AuthStrategy` with `auto_register`) does more than insert the `members` / `members_info` rows. For a **new** member it resolves the tracking ids out of the request extras (`inviter_id`, `source_id`, `monitor_id`, `latest_source_id`, `latest_monitor_id` — each persisted on the `members` row only when positive) and then runs the same promoter/distribution side-effect pipeline as the admin/H5-post paths, **inside the registration transaction**: `AdminMemberCreatePromoterSideEffectPort.createForNewMember(companyId, userId, mobilePlain, gradeId)` (popularize promoter), `AdminMemberCreateDistributorUserSideEffectPort.createData(companyId, userId, distributorId, salespersonId, inviterId)` (distribution user), and — when the extras carry a positive `distributor_id` — `insertShopRelMemberIfAbsent` inserts the `ShopRelMember` row. The `create_member_success` eventData is then built with the same `distributor_id` / `salesperson_id` / `inviter_id` values and the `if_register_promotion` flag (resolved via `resolveIfRegisterPromotionForNewMember`), and only then is the after-commit synchronization registered — with the usual `BadRequestException("createMemberLocalAutoRegister requires active transaction synchronization")` guard when no synchronization is active. Note the existing-member branch (a mobile that already has a `members` row) skips this whole pipeline: it updates the row and returns `is_new=0` (or `1` for an upload member) with no promoter/distributor side effects and no publish.

Bootstrap registers **nine listeners** on the event (each in its own `*DispatchListenerRegistrationConfig`, wired in `@PostConstruct` with `DispatchRegistry.registerEventListener`):

| Listener name | Bundle / listener | Queue | Gate |
|---|---|---|---|
| `listener:youshu.member_create_success` | youshu `CreateMemberSuccessYoushuDispatchListener` → `YoushuMemberCreateSuccessSyncService.syncMemberAfterCreate` | default | needs `company_id` + `user_id` |
| `listener:shuyun.open_platform.member_register_on_create_member_success` | shuyun `CreateMemberSuccessShuyunMemberRegisterDispatchListener` → `MemberRegisterDispatchService.dispatchCreateMemberSuccess` (falls back to the `members` row for distributor/mobile) | default | — |
| `listener:thirdparty.shopex_crm.sync_add_member` | third-party `CreateMemberSuccessShopexCrmSyncDispatchListener` → `CreateMemberSuccessShopexCrmSyncExecutionService` | default | no-op unless `crm.crm-sync` is configured |
| `listener:promotions.create_member_success` | promotions `CreateMemberSuccessPromotionsDispatchListener` → `executionMarketingAfterMemberCreate` | default | `if_register_promotion` must be `true`; needs `company_id`, `distributor_id`, `user_id`, `mobile` |
| `listener:members.register_point` | promotions `CreateMemberSuccessRegisterPointDispatchListener` → register-gift points via `PointMemberAddPointService` | default | Redis register-point config `is_open` and positive `point` |
| `listener:datacube.register_num_stats` | datacube `CreateMemberSuccessRegisterNumStatsDispatchListener` → `datecube_tracklog` Redis hashes + source tags | slow | failures swallowed at debug |
| `listener:members.create_member_success_notice` | members' own `CreateMemberSuccessNoticeDispatchListener` → `CreateMemberSuccessNoticeExecutionService` (wxapp template message "welcome / 邀请人 …") | slow | requires `openid` and `wxa_appid` |
| `listener:promotions.create_member_success_send_membercard` | promotions `CreateMemberSuccessSendMembercardDispatchListener` → `RegisterPromotionsMembercardPromotionService` (membercard register promotion, coupons via kaquan's `VipGradeOrderReceiveMembercardPromotionService`) | default | `if_register_promotion` must be `true` |
| `listener:members.create_member_success_new_customer` | members' own `CreateMemberSuccessNewCustomerDispatchListener` → `NewCustomerTagReconcileService.reconcile(companyId, userId, register)` | default | needs positive `company_id` + `user_id`; see the [member system-tag subsystem](#member-system-tags-tag_kindtag_code--three-reconcile-loops) |

The marketing-side view of the same fan-out is in [Marketing Domain](marketing.md).

### In-transaction side effects of the H5 post path

`creatMemberForH5Post` (front `POST /wxapp/member` registration / `WxappMemberCreatMemberFacade`) differs from the auto-register paths: for a **new** member it also calls `AdminMemberCreatePromoterSideEffectPort.createForNewMember` (popularize promoter), `AdminMemberCreateDistributorUserSideEffectPort.createData` (distribution user), inserts `members_rel_shop` for a positive `distributor_id`, and inserts a `work_wechat_rel` bind for a positive `salesperson_id`. It then creates the platform association according to `api_from`/`auth_type` (`wechat` → `createMemberWechatAssociation`, `ali`/`aliapp` → `ensureAliMemberAssociation`, `social_oauth` → `createMemberPlatformAssociation` with the payload's `user_type`). An "upload member" (a row created by admin import, flagged `is_upload_member` in `members_info.other_params`) is treated as new and its flag is cleaned up afterwards, so the imported member gets the full new-member pipeline on first self-registration.

## Member update, mobile change, and the update event

`PUT /api/v1/h5app/wxapp/member` → `WxappMemberUpdateMemberService.updateMember` (`@FrontAuth`): strips masked placeholder fields (`****-**-*` birthdays, `******` addresses), validates custom fields through `FrontMemberInfoUpdateValidationPort`, optionally calls the Shuyun open platform `member.modify` **before** the local write (A-MOD-01), then `MemberAccountService.memberInfoUpdate`, then registers an after-commit `MembersUpdateMemberSuccessDispatchPublisher.publish` with a fresh `members_info` snapshot. `event:members:update_member_success` currently has one listener in bootstrap (`listener:thirdparty.shopex_crm.sync_update_member`, also gated on `crm.crm-sync`). After the local write it patches the wechat profile (`nickname`/`headimgurl`) when the claims carry `unionid` + `open_id`, runs the OEM-Shuyun profile modify when the open platform is *not* enabled, and calls DM CRM `updateMemberInfoByMobile` when DM point integration is open.

`PUT /api/v1/h5app/wxapp/member/mobile` → `WxappMemberMobileChangeService.updateMemberMobile`: enforces a **30-day cooldown per member** via the Redis key `updateMemberMobileCD:<userId>:<sha1(companyId)>` (`members.update-mobile.ttl-seconds`, default 2592000) on the members Redis template, validates the CN mobile pattern, verifies the SMS code against `member-update:company<companyId>:<newMobile>`, refuses a duplicate mobile within the tenant, updates `members.mobile` (fixed-encrypted), `region_mobile` and `mobile_country_code`, writes a `member_operate_log` row (`operate_type=mobile`, operater `用户在云店自行修改`) with the old/new JSON, then sets the cooldown key.

### Mobile storage transform

`members.mobile` is written through `LegacyFixedMobileEncrypt.fixedEncryptMobile` — a deterministic AES (default `AES-128-CBC`, fixed IV) transform matching the legacy PHP app-key payload layout, so mobile lookups can be equality queries. It is configured by `LegacyFixedMobileEncryptConfigurer` from `ecshopx.compat-crypto.app-key` + `ecshopx.compat-crypto.cipher` + `common.encrypt-sensitive-data`; when `common.encrypt-sensitive-data=false` the transform is the identity (plaintext `members.mobile`), which is the repository default. The plaintext is kept alongside in `members.region_mobile`, which is why change/registration paths write both.

## Member-side SMS codes

`GET /api/v1/h5app/wxapp/member/sms/code` (`@FrontNoAuth`) → `WxappMemberSmsCodeSendService.getSmsCode`. Accepted `type` values are `sign`, `forgot_password`, `login`, `update`, `merchant_login` (default `sign`). The type changes the semantics: `sign` refuses an already-registered mobile (unless it is an upload member), `forgot_password` refuses an *unregistered* mobile (and, when authenticated, sends to the account's stored mobile), `update` skips the image captcha and requires the caller's own mobile shape, and `sign` also refuses when the caller's wechat association is already bound to a phone. Every non-`update` type must first pass the image captcha (`verifyAndConsumeMemberImageVcode`, from `GET /wxapp/member/image/code`).

`MemberSmsVerificationCodeDeliveryService.deliver` (members Redis template) enforces `common.sms-send-limit` (default 5) sends per `(company, day, type, mobile)` via the `yzmsend:<companyId>:<yyyyMMdd>:<type>:<mobile>` counter, generates a 6-digit code, stores it for 300 seconds under `member-<type>:company<companyId>:<mobile>`, logs the code (legacy parity), and sends through `CompanySmsSendPort.sendVerificationCodeForScene(..., "verification_code")` — a failure raises `ResourceException("短信发送失败")`. Login-time verification is `MemberRegSettingService.checkSmsVcode`, used by `LocalH5AuthStrategy` for `check_type=mobile`.

## Wechat fans and tag sync

Members owns the `wechat_users` / `wechat_fans` / `wechat_tags` / `wechat_fans_bind_wechat_tag` tables and three dispatch listeners of its own (all registered in bootstrap):

* `event:members:sync_wechat_fans` (slow queue) → `SyncWechatFansDispatchListener` → `SyncWechatFansPageNotificationProcessor.handle`: for each `open_ids` page of ≤100, batch-fetch user info and persist via `WechatFansSyncPersistService.saveUser`. The producer is `WechatFansSyncService.syncWechatFans`, which throttles to **3 syncs/day and one per 10 minutes** per company via the `syncUsersCount<yyyyMMdd>:<sha1(companyId)>` Redis list (members Redis DB, `EXPIREAT` end of day) and pages the official-account `user/get` API 10,000 openids at a time, publishing one event per page.
* `event:members:sync_wechat_tags` (default queue) → `SyncWechatTagsDispatchListener` → `WechatFansTagsSyncService.syncWechatTags`: diff the remote official-account tag list against local `wechat_tags`, upsert the remote set and delete locals that no longer exist, inside a `TransactionTemplate`.
* `event:wechat:subscribe` (default queue, event name from `WechatDispatchEventNames`) → `WechatSubscribeDispatchListener`: `subscribe` batch-fetches the one user and persists the fan (only when WeChat returns a `unionid`); `unsubscribe` clears the fan's bound tags.

`MemberAccountService.createWxappFans` is the shared upsert behind login-time fan recording: insert-or-update the `wechat_users` row keyed by `(company_id, authorizer_appid, open_id, unionid)`, then — when a `bind_member_user_id` was supplied and no `wechat` association exists yet — insert the `members_associations` row, and return `{wechatuser, memberInfo, is_new}` (is_new = "no association", which is what the official-account login strategy uses to decide whether the fan still needs to register). `updateWechatUnionId` migrates a stale unionid across both `wechat_users` and `members_associations` and clears `need_transfer` (the transfer-mode repair path).

## Goods-arrival subscription (缺货订阅, subscribe notices)

Members owns the member side of "notify me when this is back in stock": the `members_subscribe_notice` table (`SubscribeNotice` domain, `SubscribeNoticeMapper`), the wxapp subscribe/unsubscribe endpoints over it, and the wechat leg of the goods-arrival notify fan-out that the goods bundle's item-store event triggers. The Alipay leg (`source=alipay`) is owned by ecshopx-ali; see [Third-Party Integrations](../integrations/third-party.md).

### The `subscribe_notice` row and its lifecycle

`SubscribeNotice` (订阅通知) is one row per `(company_id, user_id, rel_id=item_id, source, distributor_id, sub_type='goods', sub_status='NO')` — `sub_id` auto-increment PK, `open_id` (the wechat openid or alipay `alipay_user_id` used as the notify recipient), `source` (`wechat` default / `alipay`), `distributor_id` (店铺id, `0` = headquarters/platform scope), `remarks` (item name snapshot at subscribe time), `created`/`updated` epoch seconds. `sub_status` is the lifecycle column: `NO` (未通知, not notified) → `SUCCESS` (已通知, notified); `ERROR` (通知失败, notify failed) exists in the schema comment but **no Java write path sets it** — `err_reason` is likewise never written. Rows are deleted, not archived, when a member unsubscribes.

Two endpoints drive it from `MembersController` (`@FrontAuth` is the class default; the status check overrides it with `@FrontNoAuth` because it must answer for anonymous visitors):

* `POST /api/v1/h5app/wxapp/member/subscribe/item/{item_id}` (缺货订阅) → `WxappGoodsArrivalNoticeSubscribeService.itemsSubscribe`. It is a **toggle**: a matching row (same user/company/item/source/distributor, `sub_type=goods`, `sub_status=NO`) that already exists is *deleted*; otherwise the item must exist (`MembersSubscribeNoticeItemNamePort.itemExists`, else `ResourceException("商品不存在")`) and a new row is inserted with `remarks` = the resolved multi-language item name. The response echoes the resulting state — the row's `{user_id, item_id, item_name}` after a subscribe, an empty list after an unsubscribe. The same toggle is applied to the Redis bookkeeping hash: the `arrivalNoticeSub` key exists with the user as field → delete the field; absent → write JSON `{"open_id": …, "wxa_appid": …}` (the appid recorded at subscribe time is what the notify leg reads back later). The two toggles are evaluated independently, so state loss on one side desynchronizes the pair (after a Redis flush the row is kept while the hash field is gone); the notify-time `markSuccess` now re-synchronizes them by clearing the hash field for every matched row (see below). The open id and `source` come from the caller's enriched claims — `open_id`/`wxapp_appid` for wechat, `alipay_user_id` for alipay.
* `GET /api/v1/h5app/wxapp/member/item/is_subscribe/{item_id}` (是否订阅商品) → `isSubscribe`: reads the *Redis* key (not the table) and returns `{user_id, item_id, item_name}` (empty list when the field is absent or the caller is anonymous), re-resolving `item_name` from `MembersSubscribeNoticeItemNamePort` for a positive numeric item id. Only the Redis field reflects "currently subscribed" — the table row survives a Redis flush as history until the next toggle deletes it, which is what causes the desync described above.

The admin console reads the same table read-only: `GET /api/v1/members/subscribe/list` (订阅列表, `MembersSubscribeController`, `@AdminAuth` + `@Activated(routeAlias = "member.subscribe.list")`) → `MembersSubscribeNoticeListService`, which filters by `sub_type` (default `goods`), `sub_status`, `rel_id` and created-time range, joins the member username per row (falling back to 匿名 for rows without a member) and re-resolves the item name for `goods` rows — the page at which operators see the `NO`/`SUCCESS` split and the never-written `err_reason` column.

The Redis key shape is the shared contract between subscribe and notify — `WxappGoodsArrivalNoticeSubscribeService` writes it on the **shared** template (`sharedStringRedisTemplate`, Redis DB 0, [Redis Databases](../concepts/redis-databases.md)):

* wechat source: `arrivalNoticeSub:<companyId>:<itemId>` or `arrivalNoticeSub:<companyId>:<itemId>:<distributorId>` when the shop scope is positive;
* alipay source: `arrivalNoticeSub:<source>:<companyId>:<itemId>[:<distributorId>]` (the source segment is prepended, keeping alipay rows out of the wechat key namespace).

The Redis hash is written by the subscribe toggle, read back by `is_subscribe` and the wechat notify leg, and cleared per matched row by `markSuccess` after a restock consume — the alipay leg never reads it (it sends straight to the ali template job with `to_user_id` = the alipay user id) — and no expiry is set: a subscribe stays "armed" until it is unsubscribed or a restock event consumes and clears it.

### The notify leg: `event:258` → wechat `goodsArrivalNotice`

```mermaid
sequenceDiagram
    participant GS as goods ItemStoreService
    participant BUS as DispatchFacade publishEvent
    participant LIS as ItemStoreUpdatedGoodsArrivalDispatchListener
    participant ORC as ItemStoreGoodsArrivalWxaTemplateOrchestrator
    participant MCH as GoodsArrivalSubscribeMatchService
    participant RES as ItemCompanyIdResolver
    participant ADP as GoodsArrivalWxaSubscribeSendPort
    participant SND as promotions WxaTemplateMsgActivityRemindSendService

    GS->>BUS: publish(item_id, store, distributor_id) SYNC event 258
    BUS->>LIS: onEvent(payload)
    alt store is zero or negative
        LIS->>LIS: return, nothing happens
    else store is positive
        LIS->>ORC: dispatchIfApplicable(itemId, store, distributorId)
        ORC->>RES: findCompanyIdByItemId(itemId)
        RES-->>ORC: companyId
        ORC->>MCH: listPending(companyId, itemId, dist, wechat)
        MCH-->>ORC: pending subscribe_notice rows
        loop each row that has an open_id
            ORC->>ORC: resolve appid from arrivalNoticeSub hash, yykweishop fallback
            ORC->>ADP: send(payload, forceFire is true)
            ADP->>SND: WXA subscribe send, scene goodsArrivalNotice
        end
        LIS->>MCH: markSuccess(companyId, itemId, dist)
        MCH->>MCH: matched rows NO to SUCCESS, stamp updated
        MCH->>MCH: clear arrivalNoticeSub hash field per matched row
    end
```

Caption: the goods-arrival wechat notify — goods publishes `event:258` in SYNC mode, the members listener runs inline, sends one subscribe message per pending wechat subscriber, marks the matched rows notified and clears their `arrivalNoticeSub` hash fields.

The chain, end to end:

1. **Publisher (goods).** `ItemStoreService.saveItemStore(itemId, store, distributorId)` and `saveItemStoreForWdtSync` (which publishes *before* the Redis write to preserve WDT ordering) call the common `ItemStoreUpdatedEventBusPublisher`, implemented in bootstrap by `ItemStoreUpdatedEventBusPublisherImpl` as `DispatchFacade.publishEvent` of `GoodsDispatchEventNames.EVENT_ITEM_STORE_UPDATE` = `event:258:GoodsBundle\Events\ItemStoreUpdateEvent` with payload `{item_id, store, distributor_id}` and `DispatchOptions.eventDefaults()` — i.e. **SYNC**, so every listener runs inline in the publisher's thread and transaction ([Dispatch Bus](../concepts/dispatch-bus.md)). Producers include item create/edit (`PlatformItemsAddService`, `SupplierItemsAddService`), the admin batch store edit, the distributor-items shop store edit (`DistributorItemsUpdateOrchestratorImpl` — the source of shop-scoped events with `distributor_id>0`), openapi item-store sync, and the Jushuitan/WdtErp inventory pulls; see the goods page's inventory section ("Inventory: three pools, one resolver") and its [events table](goods.md#events-published-and-who-listens) for the full publisher list.
2. **Listener registration.** Bootstrap's `ItemStoreUpdatedEventDispatchListenerRegistrationConfig` (`@Order(75)`) registers `listener:members.listeners.ItemStoreUpdatedGoodsArrival` → `ItemStoreUpdatedGoodsArrivalDispatchListener` with `ListenerDispatchOptions.syncDefaults()`, alongside the sibling `listener:goods.listeners.SendTemplateMsgListener` (ali leg, registered first so it runs first) in the same `@PostConstruct`; a third `event:258` listener, `listener:shuyun_open_platform.product_sync_on_item_store_update`, is registered separately in `ShuyunOpenPlatformItemStoreProductSyncDispatchListenerRegistrationConfig`. Because the publish is SYNC with multiple listeners, they all run inline inside the publish call — the members listener after the ali leg (pinned by `ItemStoreUpdatedAliTemplateBusDispatchFlowTest`'s `InOrder` assertion).
3. **The gate.** `ItemStoreUpdatedGoodsArrivalDispatchListener.onEvent` parses `item_id`/`store`/`distributor_id` (Number-or-String lenient parsing) and returns immediately when `store <= 0` — out-of-stock transitions never notify. Otherwise it runs the two legs in order: the wechat send (`ItemStoreGoodsArrivalWxaTemplateOrchestrator.dispatchIfApplicable`), then its *own* company resolution via `ItemCompanyIdResolver` (goods' impl reads the `items` row; empty → skip marking) and `GoodsArrivalSubscribeMatchService.markSuccess(companyId, itemId, dist)` with `dist = normalizeDistributorId(distributorId)` (clamped to `[0, Integer.MAX_VALUE]`, so a negative or oversized long becomes a valid int). The resolver is queried twice per event (once inside the orchestrator, once here) — deliberate redundancy that decouples the two legs: the send leg can resolve the company and fail quietly while the marking leg still resolves it independently. The listener itself catches nothing — it is the orchestrator's own try/catch that makes the send leg failure-tolerant (below); a throwing `markSuccess` would still fail the publisher's transaction, matching the SYNC-event contract.
4. **Send (wechat leg).** `ItemStoreGoodsArrivalWxaTemplateOrchestrator.dispatchIfApplicable` (PHP `SendTemplateMsgListener` wechat-branch parity for `goodsArrivalNotice`) wraps everything in a `try/catch(RuntimeException)` that logs at debug and returns — a notify problem must never break the stock write. It re-checks `store > 0` and `itemId > 0`, resolves the company, matches pending rows with `listPending(companyId, itemId, dist, "wechat")`, and returns silently when there are none. Otherwise it builds the shared data fields — `item_name` = the multi-language item name truncated to 20 code points (`MembersSubscribeNoticeItemNamePort.resolveItemName(companyId, itemId, "zh-CN")`), `notice` = the fixed text **亲！您想要的商品已经到货，可以购买啦！** — and loops the pending rows, skipping any without an `open_id`. Per row:
   * the appid is resolved by *the row's own* shop scope, `arrivalNoticeSub:<companyId>:<itemId>[:<rowDistributorId>]` — hash field `userId`, value parsed as JSON with a `wxa_appid` key (i.e. the appid the member was on when subscribing); when the hash misses or the user id is null, the fallback is the wechat `wechat_weapp` authorizer appid for template name `yykweishop` (`WeappAuthorizerAppidRepository.findAuthorizerAppid(companyId, "yykweishop")`); an empty appid skips the row;
   * the send payload is `{scenes_name: "goodsArrivalNotice", company_id, appid, openid: row.open_id.trim(), data: {item_name, notice}, page_query_str: "id=" + itemId}`, passed through the common `GoodsArrivalWxaSubscribeSendPort` with **`forceFire=true`** (arrival notices are never delayed by the template's `send_time_desc` config);
   * the port is implemented in promotions by `GoodsArrivalWxaSubscribeSendAdapter` → `WxaTemplateMsgActivityRemindSendService.send` — the central WXA subscribe sender shared by every member-notify producer, which looks up the `promotions_notice_template` row by `(company_id, scenes_name, template_name)` with the template name from the `wechat_weapp` row, gates on `is_open` + `template_id`, and posts the subscribe message through `WxJavaMaRuntime` (goodsArrivalNotice resolves the default page `pages/index` + the query string). The full send pipeline (validation, template gating, the delayed `job:11` path, and the sibling orders/aftersales/wechat producers) is on [Marketing Domain — WXA subscribe messaging](marketing.md#wxa-subscribe-messaging--the-central-sender-owned-by-promotions); the scene definition is `WxaTemplateSceneDefinitions.GOODS_ARRIVAL_NOTICE` (template id `5019`, 商品到货通知, keywords `item_name`/`notice`).
5. **Marking.** `GoodsArrivalSubscribeMatchService.markSuccess` re-runs the same matching (**any source** — it is the only writer of `SUCCESS` on this path; the ali leg sends but never marks) and updates the matched `sub_id`s with `sub_status: NO → SUCCESS` plus `updated = now`. Note the two-phase shape: the wechat **send** is driven by `listPending(..., "wechat")` while the **marking** is source-agnostic and happens even when the wechat send skipped (no pending wechat rows, no appid, or the orchestrator caught a runtime error) — so an alipay subscriber's row is marked by this listener after the ali leg's own send, and a wechat row can be marked even when its individual send was skipped (missing appid, missing open_id). The deliberate consequence is that both legs must share the matching semantics, or a row can be marked without a send.
6. **Redis cleanup.** Immediately after a non-empty marking, `markSuccess` also clears the subscribe-state hash: for every matched row `clearArrivalNoticeSubscribeRedis` deletes hash field `userId` from the row's own key (`GoodsArrivalNoticeSubscribeRedisKeys.hashKey(row.source, companyId, row.relId, row.distributorId)`, defaulting a blank source to `wechat`) on the shared template. This is what makes `GET .../item/is_subscribe/{item_id}` stop answering 「已订阅到货通知」 once the notice has been sent, and it re-synchronizes the table with the Redis half of the toggle.

### Matching semantics: shop stock vs headquarters stock

`GoodsArrivalSubscribeMatchService` is the single matcher both legs share (`listPending` for send, `markSuccess` for marking), and its shop-scope rule is the interesting part:

* **Shop stock event (`eventDistributorId > 0`)** — only rows with exactly that `distributor_id` match (plus the always-applied base predicate `company_id`, `rel_id=itemId`, `sub_type='goods'`, `sub_status='NO'`; `listPending` additionally filters `source`). The total-store port is not consulted.
* **Headquarters stock event (`eventDistributorId == 0`)** — a plain `distributor_id=0` row matches trivially, and a *shop-scoped* row (`distributor_id>0`) matches **only when that shop currently draws the SKU from headquarters stock**: `DistributorItemTotalStorePort.filterDistributorIdsUsingTotalStore(companyId, itemId, candidateShopIds)` (goods' `DistributorItemTotalStorePortImpl` over `distributor_items.is_total_store`) decides which shop rows ride along. A shop that keeps local stock is not notified by a platform restock — its own shop-scoped event will come when its `distributor_items.store` is written.

`normalizeDistributorId` and `hasOpenId` are the two static helpers the other classes borrow; `hasOpenId` guards the per-row send (a row without a usable `open_id` is skipped but still marked).

### Failure, retry and idempotency notes

* The wechat send leg is **best-effort by design**: `dispatchIfApplicable` swallows any `RuntimeException` at debug level, and the promotions sender itself is tolerant (missing `Weapp` row, closed template, or a downstream WeChat error all return without throwing). There is no retry and no `ERROR`/`err_reason` write — a failed send still marks the row `SUCCESS` (and clears the Redis hash field), so the subscriber does not get a second chance until they re-subscribe. This mirrors the legacy PHP notifier's behavior.
* There is no dedupe on the notify path beyond the `NO → SUCCESS` transition itself: the second `event:258` for the same item finds no `NO` rows and does nothing. The Redis `arrivalNoticeSub` hash is therefore subscribe-state bookkeeping (which appid, which user), not a send-dedupe lock: the send leg only *reads* it, while writes belong to the subscribe toggle, the unsubscribe path, and — since the Redis-clearing change — `markSuccess` itself.
* The marking update is guarded `sub_status = 'NO'` inside the `UPDATE` itself (`eq(SubscribeNotice::getSubStatus, "NO")`), so concurrent events for the same item cannot double-mark.

Pinned by `GoodsArrivalSubscribeMatchServiceTest` (shop event = exact `distributor_id` match with the total-store port never consulted; headquarters event = `distributor_id=0` row plus only the `is_total_store=true` shop rows; `markSuccess` deletes the row's `arrivalNoticeSub` hash field, e.g. `arrivalNoticeSub:1:10:88` for a shop row and `arrivalNoticeSub:1:10` for a headquarters row) and `ItemStoreGoodsArrivalWxaTemplateOrchestratorTest` (`store<=0` skips everything; the send payload carries `scenes_name=goodsArrivalNotice`, the resolved `appid`, `openid` and `page_query_str id=<itemId>` with `forceFire=true`; a headquarters event reads the appid from the **row's** `arrivalNoticeSub:<companyId>:<itemId>:<rowDistributorId>` key). The end-to-end bus behavior — publish `event:258` runs both listeners inline, then the ali leg enqueues its delayed-able template job — is pinned by bootstrap's `ItemStoreUpdatedAliTemplateBusDispatchFlowTest`.


## Trust login (Apple / Google / Facebook / Line)

Overseas social login is its own small sub-domain in `service/trustlogin/`:

* `SocialTrustLoginTypes.SOCIAL_TYPES = [apple, google, facebook, line]` (PHP `SocialTrustLoginService::SOCIAL_TYPES` parity); `isSocialProvider` does a case-insensitive membership test.
* `GET /api/v1/h5app/wxapp/trustlogin/params` (`TrustLoginController`, `@FrontNoAuth`) → `TrustLoginParamsService.getTrustLoginParams(companyId, trustlogin_tag, version_tag, h5Host, redirectUrl)`, where the controller derives `h5Host` from the `Origin` header. `company_id` is resolved from the request attribute → claims → query, else `UnauthorizedException`.
* `GET /api/v1/h5app/wxapp/trustlogin/list` → `AdminTrustLoginListService.getTrustLoginListForFront(companyId, versionTag)` — the enabled-channel list the H5 page renders. The default template (weixin for `standard`; weixin + apple/google/facebook/line for `touch`) is `TrustLoginDefaultProperties` / the importable `ecshopx-members/src/main/resources/application-members.yml` (`spring.config.import=optional:classpath:application-members.yml` is already wired in bootstrap's `application.properties`), every row `status: "false"` until configured.
* `H5SocialOAuthLoginService.loginOrRegister` (`auth_type=social_oauth`): validates the tag is social and the channel row is enabled, resolves the h5 host (else `缺少 H5 域名配置`), exchanges `code` for a `SocialOAuthUser` through `SocialTrustLoginService.resolveUserFromCode`, then either maps an existing `members_associations` row (`user_type` from the provider, `unionid` = the provider user id) to the member, or **allocates a synthetic mobile** (`MemberSyntheticMobileService.allocateUnique`: `10` + 9 random digits, unique per company, ≤80 attempts) and creates the member through `creatMemberForH5Post` with `auth_type=social_oauth` — which is why the social path joins the CreateMemberSuccess fan-out too.
* **Apple needs a server-side callback** because Apple posts the authorization code as a form: `TrustLoginAppleCallbackController` (`/api/v1/h5app/wxapp/trustlogin/apple/callback`, GET *and* POST, deliberately **without** `@FrontNoAuth`, matching PHP `routes/frontapi/member.php`) resolves the h5 host **only from the one-time remembered `state`** and then 302-redirects to the H5 landing URL with the normalized `code` (or the `error`/`error_description` pair). The authorize step (`getAuthorizeUrl`) asks Apple for `response_mode=form_post`, generates an opaque `state` (`encodeAppleOAuthState` — 32 random bytes, base64url) and stores the `state → h5Host` binding in the shared Redis template under `ecx:apple_oauth:h5:<md5(state)>` for 600 s (`rememberAppleOAuthH5Host`). `resolveAppleCallbackH5Host` is hardened: the query `h5_host` and unsigned state payloads are ignored, the Redis binding is `GETDEL`-consumed (`recallAppleOAuthH5Host`) and must equal `common.h5-base-url` — otherwise the callback answers 500 rather than redirecting to an attacker-chosen host. `SocialTrustLoginService` also carries the redirect/callback URI builders (against `common.api-base-url` / `APP_URL`), `normalizeApplePrivateKey`, the ES256 client-secret signing (`generateAppleClientSecret`, 180-day `exp`) and the Apple `id_token` verification against `https://appleid.apple.com/auth/keys` (RS256, issuer and audience checked).

## Member system tags (tag_kind/tag_code + three reconcile loops)

New this window: members grew a **system-tag subsystem** — tenant-level, rule-driven member tags that no operator may edit, reconciled automatically from order/birthday state. The schema half is Flyway `V00000000000006` (see [Schema Migration](../workflows/schema-migration.md)):

* `members_tags` gains `tag_code` (varchar 64, nullable; required for system tags), `tag_kind` (`manual` | `system`, default `manual`) and `rule_version` (default 1), plus the unique key **`uk_company_tag_code (company_id, tag_code)`** — one tag of a given code per tenant (manual rows with a NULL code are unaffected).
* `members_rel_tags` gains `source` (default `manual`), `rule_version`, `effective_at` and `updated_at`.
* New table **`members_rel_tags_audit`**: one row per attach/detach with `action` (`attach`/`detach`), `source`, `rule_version`, `reason`, `event_id` and `created`, keyed `idx_company_user_tag (company_id, user_id, tag_id)`.
* The same migration also adds `member_tag_ids` (JSON) + `member_tag_mode` (`include`/`exclude`) to `promotions_marketing_activity` and `promotions_seckill_activity` — the audience columns whose tag ids are validated by `MemberAudienceTagValidator` (below; see [Marketing Domain](marketing.md)).

`MemberSystemTagCodes` is the single vocabulary: three reserved codes — **`new_customer`** (新客, "当前没有有效已付订单的会员"), **`birthday_this_month`** (当月生日, "生日月份等于当前自然月的会员") and **`sleeping_user`** (沉睡用户, "距离上次完成订单超过 180 天的会员", threshold constant `SLEEPING_THRESHOLD_SECONDS = 180 * 86400`, strictly greater-than: exactly 180 days does not match) — plus `RULE_VERSION = 1`, the `SOURCE_*` provenance strings (`register`, `pay`, `refund`, `cancel`, `bind`, `disable`, `enable`, `compensate`, `daily`) and the audit `ACTION_ATTACH`/`ACTION_DETACH`.

**Seeding.** `MemberSystemTagSeedService.seedCompany(companyId)` idempotently inserts the three tags (`tag_kind=system`, `distributor_id=0`, color `#ff1939`, status `online`, source `self`) keyed by `(company_id, tag_code)`, so the unique index is the guard against duplicates. Three triggers call it: `MemberSystemTagHistoricalSeedRunner` (`ApplicationRunner`, `@Order(200)`) sweeps every tenant at boot; `CompanyActivatedSystemTagListener` seeds the new tenant `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` on `CompanyActivatedEvent` in a `REQUIRES_NEW` transaction (the sibling of the staff-tag listener); and every reconcile service lazily re-seeds its own tag when the lookup misses, so a tenant that lost a row self-heals on the next event.

### The reconcile pattern

All three services follow the same shape: compute the desired membership from a rule query, diff against the existing `members_rel_tags` row, and write **one audit row per actual change** (attaches catch `DuplicateKeyException` for idempotency; detaches delete the rel row by id; nothing is written when the desired state already holds). Every audit insert stamps the `SOURCE_*` of the triggering event and the current `rule_version`.

* **`new_customer`** — `NewCustomerTagReconcileService.reconcile(companyId, userId, source)` (`@Transactional`): a blank source falls back to `compensate`; `disable`, a missing member, or `disabled=true` **detaches every system tag** of that member (not just new_customer). Otherwise `NewCustomerEffectivePaidOrderQuery.countEffectivePaid` (raw `JdbcTemplate` over `orders_normal_orders`): an *effective paid* order is `pay_status='PAYED'` **and** `cancel_status != 'SUCCESS'` **and** (`total_fee = 0` or `total_fee` greater than the sum of its `SUCCESS` refunds in `aftersales_refund`); `paid == 0` ⇒ attach, `paid > 0` ⇒ detach the existing rel.
* **`birthday_this_month`** — `BirthdayThisMonthReconcileService`, driven by the XXL-Job cron handler **`member-system-tag-birthday-this-month`** (`ScheduleBirthdayThisMonthTagHandler`, which publishes a `CronAlertEvent` and rethrows on failure): `executeMonthly` runs **only when today is the 1st of the month** (otherwise it logs a skip), loops all tenants, and a tenant failure is logged per-company then surfaced as one `IllegalStateException` at the end. Per company (`reconcileCompany`, `@Transactional`) the desired set is non-disabled members whose birth month — `members_info.month` when valid (1–12), else parsed from the `birthday` string (`ISO_LOCAL_DATE`, `yyyy-MM-dd HH:mm:ss`, or the first 10 chars) — equals the current month; attach/detach diffs against existing rels, with audit rows sourced `compensate` and `reason="birthday_this_month"`.
* **`sleeping_user`** — `SleepingUserTagReconcileService`, driven from the orders bundle's `FinishOrderJobService` (the daily finish-orders job `FinishOrderJobHandler` runs it after the batch, failure logged, never failing the job): per company it pages `SleepingUserLastCompletedOrderQuery.listUserIdsLastCompletedBefore` (users whose `MAX(end_time)` over `order_status='DONE'` orders is older than `now - 180d`, page size 200) and additionally re-reconciles every member already carrying the tag, so both directions settle. `reconcile` per member: missing/disabled ⇒ detach; else desired = last completed `end_time` more than 180 days ago (a member with **no** completed order is never sleeping); audit rows sourced `daily` from the sweep.

### The listeners that feed it

| Trigger | Kind / registration | Listener → reconcile source |
|---|---|---|
| `event:members:create_member_success` | dispatch, bootstrap `MembersCreateMemberSuccessNewCustomerDispatchListenerRegistrationConfig` (default queue, async) | `CreateMemberSuccessNewCustomerDispatchListener` → `SOURCE_REGISTER` |
| `event:288:OrdersBundle\Events\NormalOrderPaySuccessEvent` | dispatch, bootstrap `MembersNormalOrderPaySuccessNewCustomerDispatchListenerRegistrationConfig` (default queue, async, after the wsugc listener) | `NormalOrderPaySuccessNewCustomerDispatchListener` → `SOURCE_PAY` |
| `event:orders:normal_order_cancel` | dispatch, bootstrap `MembersNormalOrderCancelNewCustomerDispatchListenerRegistrationConfig` (default queue, async, after the youshu listener) | `NormalOrderCancelNewCustomerDispatchListener` → `SOURCE_CANCEL` |
| `TradeRefundSpringEvent` | Spring `@EventListener` in `REQUIRES_NEW`, gated on payload `refund_status == SUCCESS` | `TradeRefundNewCustomerListener` → `SOURCE_REFUND` |

The two orders listeners resolve `user_id` leniently: when the payload's `user_id` is not positive they look it up from `order_id` via `NewCustomerEffectivePaidOrderQuery.findUserIdByOrder`, and return silently when neither is positive — the same `MemberSystemTagPayloadIds` Number-or-String parsing the goods-arrival listener uses. Pay-success and cancel/re-refund both route through the same idempotent reconcile, so a refund of the member's only paid order re-attaches the 新客 tag.

### Read-side and the write guard

System tags appear beside manual tags in the admin tag list/detail: `MemberSystemTagQueryScope.applyListScope` / `applyDetailScope` scope `MemberTagsListService` queries so that `distributor_id`-scoped manual tags and tenant-wide system tags are both visible (with an optional `tag_kind` filter), `MemberTagGroupWithSystemHelper` adds the system group to tag groupings, and `MemberDetailTagFieldsAssembler` splits a member's tags into manual vs system fields for the member detail page. On the write side **`MemberSystemTagWriteGuard`** throws `ResourceException("系统标签不可编辑")` — from `rejectWrite` (any update of a `tag_kind=system` row), `rejectCreatePayload` (creating a manual tag with `tag_kind=system`/`type=system` or one of the reserved codes) and `rejectTagId(s)` (attaching/detaching a system tag to a member by id). Its callers are the admin tag services (`MemberTagsCreateService`, `MemberTagsUpdateService`), the rel-tag writer `MemberRelTagsBatchCreateService` (used by the datacube source-tag listener), and the openapi tag batch/cover/update/delete services — so the reconcile services are the **only** writers of system-tag rels on any surface. The matching read-side gate for the new promotion audience columns is `MemberAudienceTagValidator.assertUsable` (used by promotions' `MarketingActivityCreatePersistenceService` and `SeckillActivityCreateService`): every referenced tag id must exist in the tenant, and a *manual* tag must belong to the activity's `distributor_id`, while system tags are usable from any distributor.

## Address buy_user_id scoping (ECX-10913 / ECX-10923)

The front `MembersController` hardened the address surface against cross-member access (PHP `MembersOwnerScopeGuard::assertActingUserBoundToAuth` parity — Java has no shopping-guide acting-on-behalf-of flow, so any foreign target is rejected):

* `POST /api/v1/h5app/wxapp/member/address` (添加地址, `@FrontAuth`) resolves the acting user through `resolveEffectiveUserIdForCreate(body, authUserId)`: only when the body carries a truthy `promoter_user_id` loosely equal to the authenticated user **and** a truthy `buy_user_id` is the latter consulted — a positive `buy_user_id` different from the authenticated user throws `ResourceException("无权访问该数据")`; every other combination (no `buy_user_id`, self `buy_user_id`, a `promoter_user_id` that is not the caller) falls through to `authUserId`. `MembersControllerAddressBuyUserScopeTest` pins all four branches.
* `GET /api/v1/h5app/wxapp/member/addresslist` (地址列表) applies the same rule to the filter user via `resolveFilterUserIdForAddressList(request, promoter_user_id, buy_user_id, authUserId)`, and additionally **always returns `authUserId` instead of the old null fallback** — the orphan-address bypass (a `filterUserId=null` list query) no longer exists.

## Other members-owned surfaces (brief)

* `service/email/` — member email registration + verification (`MemberEmailVerificationService` powers `check_type=email_otp` local login, requiring `email_verified_at`).
* `service/reg/` — registration settings (`MemberRegSettingService` for SMS vcode check, `AdminMemberRegisterSettingService` for image vcode).
* `service/account/`, `service/admin/`, `service/export/`, `service/importrow/` — admin console member CRUD, batch operations, export jobs (`AdminMemberExportFileJob*`), import.
* Tag services (`MemberTags*`, `TagsCategory*`, `MemberTagLibrary*`, `segment/`) — member tags/groups/segments, including `MemberRelTagsBatchCreateService` used by the datacube source-tag listener; the rule-driven system tags (`tag_kind=system`) on top of them are documented in [Member system tags](#member-system-tags-tag_kindtag_code--three-reconcile-loops) above.
* `MedicationPersonnel*` services + controller — pharmacy-industry medication personnel records.
* `WxappMemberDeleteMemberService` (`DELETE /wxapp/member`, 会员注销) — writes `members_delete_record`, which is what makes the *next* registration of that mobile lose `if_register_promotion`.
* `CompanyActivatedStaffTagListener` — a Spring `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` on `CompanyActivatedEvent` that seeds the default `员工` staff tag per tenant; one of the few members listeners that is a Spring event rather than a dispatch listener.
* `ItemStoreUpdatedGoodsArrivalDispatchListener` — the other entry in `listener/` besides the staff-tag listener: a dispatch listener (registered in bootstrap on `event:258`) rather than a Spring event listener; see the [Goods-arrival subscription](#goods-arrival-subscription-缺货订阅-subscribe-notices) section above for the full flow.
* OpenAPI member creation runs as a dispatch job: `OpenapiCreateMemberJobHandler` → `OpenapiThirdApiV2MemberCreateService`, `MemberRegisterJobHandler` (OEM-Shuyun silent registration), `BatchActionMembersJobHandler`, `PushMemberTagRelationJobHandler`, `UpdateAddressLatAndLngJobHandler`, `SendMemberEmailActivationJobHandler`.

## Configuration reference

| Property | Default | Used by |
|---|---|---|
| `ecshopx.h5.jwt.secret-base64` | `${JWT_SECRET}` (required) | `H5JwtIssuer`, `H5BearerJwtClaimsService`, `FrontAuthInterceptor`, `FrontNoAuthInterceptor` |
| `ecshopx.h5.jwt.ttl-minutes` / `refresh-ttl-minutes` | `${JWT_TTL:120}` / `${JWT_REFRESH_TTL:20160}` | token lifetime / refresh window |
| `ecshopx.h5.jwt.blacklist-grace-period-seconds` | `${JWT_BLACKLIST_GRACE_PERIOD:0}` | `H5JwtBlacklistService` (Redis DB 2) |
| `ecshopx.h5.jwt.issuer` / `leeway-seconds` / `required-claims` | `ecshopx` / `0` / six registered claims | issuer + time-skew + required-claim policy (startup-validated) |
| `ecshopx.h5.local.system-is-saas` / `system-companys-id` / `system-main-companys-id` | `${common.system-is-saas:false}` / `${common.system-companys-id:1}` / empty | `LocalH5AuthStrategy` company resolution (non-SaaS/main-tenant pinning) |
| `ecshopx.h5.local.oem-shuyun` / `transfer-mode` | `false` / `false` | OEM-Shuyun silent register & unionid transfer paths |
| `ecshopx.h5.wx.mini-program-secret-by-appid.<appid>` | — | `WxOpenPlatformClient` direct mini-program credentials |
| `common.encrypt-sensitive-data` + `ecshopx.compat-crypto.app-key` / `cipher` | `false` / empty / `AES-128-CBC` | `LegacyFixedMobileEncrypt` mobile storage transform |
| `common.sms-send-limit` | 5 | SMS vcode daily cap |
| `members.update-mobile.ttl-seconds` | 2592000 (30 days) | mobile-change cooldown |
| `crm.crm-sync` | empty | Shopex CRM create/update-member listeners |
| `ecshopx.trustlogin.default.standard|touch` | builtin template (see `application-members.yml`) | trust-login channel list |

## Focused tests that pin this domain

* **`MemberAccountService*DispatchTest` family** (`ecshopx-members/src/test/.../service/account/`) pins exactly which listeners fire per registration path, using `MemberAccountServiceDispatchIntegrationTestConfiguration` (mocked mappers + mocked publishers + a real `PlatformTransactionManager`): `MemberAccountServiceCreateMemberForWxappBindNoBindSalespersonDispatchTest` (no `BindSalseperson` without salesperson identifiers; payload keys/values of the wxapp-bind path), `MemberAccountServiceCreateMemberLocalAutoRegisterDispatchTest` (promoter/distributor side-effect ports invoked in-transaction with the resolved `inviter_id`/`distributor_id`/`salesperson_id` and `ShopRelMember` inserted when `distributor_id>0`; the published payload carries the extras' `openid`/`wxa_appid`/`source_id`/`monitor_id`/`inviter_id`; `work_userid`+`channel=1` → `enqueueBindSalseperson`, DM-open → `enqueueBindSalsepersonAfterDmMemberCreate`, other channels/blank work_userid → neither; rollback → nothing published), `MemberAccountServiceRegisterShuyunMemberDispatchTest`, `MemberAccountServiceCreatMemberForH5PostDispatchTest`, and the `*ShopexCrmDispatchFlowTest` siblings that run the real bootstrap publisher impl against a captured dispatch driver.
* `AdminMemberCreateMemberServiceCreateMemberDispatchTest` / `AdminMemberCreateMemberShopexCrmDispatchFlowTest` — the admin path's after-commit publication.
* `H5NewLoginPathsCreateMemberSuccessDispatchProbeTest` (in `service/h5/prelogin`) — end-to-end from the three real login entries (`H5WxappLoginRegisterFacade.registerMemberForWxapp`, `registerMemberForAliapp`, `LocalH5AuthStrategy.resolve` with `auto_register`) to the published payload, plus the rollback case (`status.setRollbackOnly()` → **no** publish), which is the invariant the `afterCommit` registration enforces.
* `CreateMemberSuccessEventDispatchFlowTest` / `CreateMemberSuccessShopexCrmSyncDispatchFlowTest` / `SyncWechatFansEventDispatchFlowTest` (bootstrap) — the registry/fan-out/consume side: N registered listeners ⇒ N enqueued `DispatchMessage`s in registration order, each consumed by `DispatchConsumerRuntime`.
* `LoginControllerWxappNewLoginWebTest` (espier) — the controller slice: merged params are handed to `FrontWxappNewLoginService` unchanged.
* `WxappMemberUpdateMemberShopexCrmDispatchFlowTest`, `WxappMemberBindTxServiceNewMemberBranchTest`, `AdminMemberMembersInfoUpdateShopexCrmDispatchFlowTest`, `WechatSubscribeDispatchListenerUnsubscribeBranchTest` — the update event, the bind transaction's new-member branch, and the wechat unsubscribe branch.
* **Member system-tag family** (`ecshopx-members/src/test/.../service/systemtag/`) — `NewCustomerTagReconcileServiceTest` (zero effective paid ⇒ single attach + audit with the caller's `SOURCE`; a paid order detaches the existing rel; `disable`/missing/disabled member detaches **all** system tags; `DuplicateKeyException` on attach is swallowed without an audit row), `BirthdayThisMonthReconcileServiceTest`, `SleepingUserTagReconcileServiceTest` (the 180-day strict threshold and the daily sweep's attach/detach diffs), `MemberSystemTagWriteGuardTest` (system tags rejected for edit/attach, reserved codes rejected for create), `MemberSystemTagSeedServiceTest` (idempotent per-tenant seed), plus `MemberSystemTagQueryScopeTest` / `MemberTagGroupWithSystemHelperTest` / `MemberDetailTagFieldsAssemblerTest` / `MemberTagsSnakeFormatterSystemTagTest` for the read-side; `TradeRefundNewCustomerListenerTest` pins the refund listener's `refund_status=SUCCESS` gate. `MembersControllerAddressBuyUserScopeTest` pins the address `buy_user_id` self-scope (foreign id rejected with 无权访问该数据, foreign `promoter_user_id` ignored).
* **Goods-arrival subscribe family** (`ecshopx-members/src/test/.../service/`) — `GoodsArrivalSubscribeMatchServiceTest` pins the shop-vs-headquarters matching rule (shop event = exact `distributor_id`, headquarters event = `distributor_id=0` rows plus only `is_total_store=true` shops) plus the marking leg's Redis cleanup (`markSuccess` clears the matched rows' `arrivalNoticeSub` hash fields), and `ItemStoreGoodsArrivalWxaTemplateOrchestratorTest` pins the send leg (skip on `store<=0`, payload `{scenes_name: goodsArrivalNotice, appid, openid, data, page_query_str id=<itemId>}` with `forceFire=true`, per-row `arrivalNoticeSub:<companyId>:<itemId>:<rowDistributorId>` appid lookup). The bus-level flow — `event:258` running both listeners inline then the ali template job — is pinned by bootstrap's `ItemStoreUpdatedAliTemplateBusDispatchFlowTest`.

## Change guidance

* **New login channel:** add an `H5AuthType` constant + an `H5AuthStrategy` `@Component`; register it in the `FrontWxappNewLoginService` switch only if it needs a pre-login step. Do not touch `H5LoginOrchestrator` or the JWT layer.
* **New claim:** put it in the strategy's `H5GenericUser` attributes (anything but the reserved JWT names), and expect it to be *overwritten* on subsequent requests by `H5JwtUserLoginInfoEnrichService` — that service, not the token, is the source of truth for member state. If a claim must survive refresh, it already does (refresh copies non-registered claims).
* **New post-registration side effect:** add a bootstrap `@Configuration` that registers a `DispatchListener` on `EVENT_CREATE_MEMBER_SUCCESS`; never publish inside the controller. Keep the payload contract (`company_id`, `user_id`, `mobile`, `openid`, `wxa_appid`, `inviter_id`, `distributor_id`, `source_id`, `monitor_id`, `salesperson_id`, `if_register_promotion`) additive. If the side effect must run *inside* the registration transaction (like the promoter/distributor ports), follow the `createMemberLocalAutoRegister` pattern: run it before registering the after-commit synchronization, and keep the `isSynchronizationActive()` guard so a non-transactional caller fails fast instead of silently dropping the publish.
* **New member-facing endpoint:** annotate `@FrontAuth` (claims injected via `H5_AUTH_CLAIMS` / `H5_COMPANY_ID`) or `@FrontNoAuth` (anonymous claims with `user_id=0`). For a `@FrontNoAuth` handler that must work without any resolvable company, follow the explicit `getNoAuthDecryptPhoneNumber` carve-out in `FrontNoAuthInterceptor` rather than loosening the interceptor.
* **Mobile columns:** always write through `LegacyFixedMobileEncrypt.fixedEncryptMobile` and read through `findMemberByCompanyAndMobile`/`resolvePlainMobileForMember`; a raw `members.mobile` equality query breaks the moment `common.encrypt-sensitive-data` is switched on.
* **Goods-arrival notify changes:** keep `listPending` and `markSuccess` in sync — they must apply the same shop/headquarters matching, or rows get marked without a send. A new `sub_type` (a new kind of subscribe notice) needs a new matching predicate *and* a new orchestrator/scene; the current code only ever queries `sub_type='goods'`. Do not move the send out of the `try/catch` in `ItemStoreGoodsArrivalWxaTemplateOrchestrator.dispatchIfApplicable` — a notify failure must not roll back the caller's stock write, because the publisher is a SYNC dispatch event running inside the goods transaction.
* **New system tag:** add the reserved code + name/description to `MemberSystemTagCodes` (which extends `isReservedCode` and the write guard automatically), seed it in `MemberSystemTagSeedService.seedCompany`, and write a reconcile service following the compute-desired → diff → audit pattern. Membership must only ever change through that reconcile — never through `MemberTagsCreateService`/`MemberRelTagsBatchCreateService`, which the write guard rejects for system rows. A new trigger is a dispatch listener registered in its own bootstrap config (or a Spring `@EventListener` like `TradeRefundNewCustomerListener`) that calls `reconcile` with a new or existing `SOURCE_*`.
