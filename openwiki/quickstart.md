---
type: quickstart
title: ECShopX-Java Wiki Quickstart
description: Entry point of the ECShopX-Java wiki — a one-paragraph system summary plus a task-to-page routing table that sends coding agents to the right page for module architecture, API endpoints, domain logic, dispatch events/jobs and cron handlers, schema migrations, payment flows, third-party integrations, deployment/configuration, and testing.
tags: [quickstart, routing, navigation, overview, architecture, maven, bootstrap]
sources:
  - id: openwiki-source-ea70eb6c045047448e446296
    resource: repo://.gitignore
  - id: openwiki-source-8037e2358a2c4f9b2c722a11
    resource: repo://AGENTS.md
  - id: openwiki-source-893c0b6ce325c1d94b3d377c
    resource: repo://bin/make-migration
  - id: openwiki-source-0c106052b3286e779bed85e7
    resource: repo://docker-compose.dev.yml
  - id: openwiki-source-e01249088898e3eaeff4752c
    resource: repo://docker-lite/README.md
  - id: openwiki-source-84aeb105875424c9aac7edf8
    resource: repo://ecshopx-bootstrap/pom.xml
  - id: openwiki-source-c83108e0b31507dc4b736d71
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DispatchBusConfig.java
  - id: openwiki-source-225eace12759f1e8a45e5306
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/FinishOrderJobDispatchPublisherImpl.java
  - id: openwiki-source-6154c1d27f3b807ac0dd5b04
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/FinishOrderJobRegistrationConfig.java
  - id: openwiki-source-d796c74a7df3139966d43e8d
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MultiRedisConfig.java
  - id: openwiki-source-7ccfb227c3f9afcfbaaf0bc3
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/XxlJobConfig.java
  - id: openwiki-source-c108088c8dc8a99034aeaa25
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/EcshopxApplication.java
  - id: openwiki-source-89b43138f8e75edb4615495b
    resource: repo://ecshopx-bootstrap/src/main/resources/application.properties
  - id: openwiki-source-ac2fa6c08b8fc916ce8f3abf
    resource: repo://ecshopx-bootstrap/src/main/resources/db/migration/V00000000000005.sql
  - id: openwiki-source-609b6f5d2dab35545395d32b
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/EcshopxApplicationTests.java
  - id: openwiki-source-27bfcace5400d2804562fba2
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersDispatchEventNames.java
  - id: openwiki-source-5ecb22a008c7475c40eec372
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersDispatchJobNames.java
  - id: openwiki-source-68b9a0e3c776f0fe8beabd26
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/payment/PaymentSubjectDistributorIdPort.java
  - id: openwiki-source-2867f272d3b645f1b2e95e5a
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/admin/v1/OperatorStoreMemberController.java
  - id: openwiki-source-09f8ef2d65947e2b57d002cc
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/domain/ProtocolUpdateLog.java
  - id: openwiki-source-f35423ff8c8af35c303879bc
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/protocol/ShopProtocolSetService.java
  - id: openwiki-source-408cf03e9d042472e7119cc7
    resource: repo://ecshopx-digios/src/main/java/cn/shopex/ecshopx/digios/client/DigiOsGatewayClient.java
  - id: openwiki-source-a1462091af0ac6e453fae382
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/DispatchFacade.java
  - id: openwiki-source-3c3a1a52e93c103069ee1377
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/redis/RedisDispatchDriver.java
  - id: openwiki-source-bdf2e5ba61aa04534071bea1
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/redis/RedisDispatchListener.java
  - id: openwiki-source-d1961192a688961d2d90ccf4
    resource: repo://ecshopx-distribution/src/main/java/cn/shopex/ecshopx/distribution/integration/PaymentSubjectDistributorIdPortImpl.java
  - id: openwiki-source-a1f68480771cd92413f00be5
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtAuthenticationFilter.java
  - id: openwiki-source-da7950ab19f4690ae361bd4b
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtIssuer.java
  - id: openwiki-source-3a3f04b8b27821da165d6047
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/items/PlatformItemsDeleteService.java
  - id: openwiki-source-4e2f63b218566d272e8cb370
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/domain/MemberTags.java
  - id: openwiki-source-a204a89b1ce5a22cd0a0fe06
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagWriteGuard.java
  - id: openwiki-source-65806a16fc6ef7f885cfdb29
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/service/systemtag/MemberSystemTagWriteGuardTest.java
  - id: openwiki-source-0a1caaa71538649542913a46
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/admin/v1/NormalOrderController.java
  - id: openwiki-source-f2e1c65d6d39cb3db1a8a7e8
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/WxappPaymentController.java
  - id: openwiki-source-c96e813e9374d09c67f360cd
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/cron/ScheduleConsumptionOrdersHandler.java
  - id: openwiki-source-18a40afbe2f8cf1c732efd6f
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/cron/ScheduleFinishOrdersHandler.java
  - id: openwiki-source-f451eeaaacd8448e25e3c13a
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/brokerage/TradeFinishCountBrokerageBusService.java
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
  - id: openwiki-source-7832b5e57d5d960a02d053c2
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentChannelQueryService.java
  - id: openwiki-source-11504f9b8ac58d0bd088b826
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/payment/OrdersPaymentDoPaymentService.java
  - id: openwiki-source-12342b48156bc08b3014932f
    resource: repo://ecshopx-orders/src/main/resources/mapper/NormalOrdersMapper.xml
  - id: openwiki-source-4c7a3bc700c378ccd65c4327
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/api/open/WxpayNotifyController.java
  - id: openwiki-source-027ba59abcf47758829bb66e
    resource: repo://ecshopx-popularize/src/main/java/cn/shopex/ecshopx/popularize/service/brokerage/NormalOrderFinishBrokerageCoordinator.java
  - id: openwiki-source-6b0c60a3857f097ae7426122
    resource: repo://ecshopx-shuyun/src/main/java/cn/shopex/ecshopx/shuyun/service/openplatform/OperatorStoreMemberReadinessService.java
  - id: openwiki-source-03ffc32a0ca502ab67c54b25
    resource: repo://install.sh
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
  - id: openwiki-source-f1b1aacc63642e5304668821
    resource: repo://readme.md
generated: { by: "openwiki/0.9.0", at: "2026-11-11T17:12:30.774Z" }
verified:
  - by: openwiki/0.9.0
    at: 2026-11-11T17:12:30.774Z
---

# ECShopX-Java Wiki Quickstart

ECShopX-Java is the Java server-side reimplementation of the ECShopX commerce system, keeping business semantics and configuration keys aligned with the legacy PHP version. It is **one multi-module Maven reactor** (parent `cn.shopex:ecshopx`, `${revision}` = `5.5.3`, on `spring-boot-starter-parent:3.5.12` / Java 17) of **53 modules**: four infrastructure modules — `ecshopx-bom`, `ecshopx-common` (shared contracts, no peer dependencies), `ecshopx-dispatch` (the message bus), and `ecshopx-bootstrap` (the only Spring Boot executable, which assembles everything) — plus **49 business Bundles**, one per domain (goods, orders, aftersales, members, marketing, payments, WeChat, third-party integrations, …). The single application (`cn.shopex.ecshopx.EcshopxApplication`) runs on Undertow, port **18080**, and serves the PHP-compatible `/api/`, `/storage/`, and `/wechatAuth/` URL spaces. State lives in **one MySQL database** (`ecshopx`, utf8mb4, `Asia/Shanghai`) reached through **MyBatis-Plus 3.5.16** mappers with pervasive `company_id` tenant scoping, plus **one Redis instance** partitioned into logical databases by hand-built Lettuce factories. All cross-Bundle asynchronous work rides the in-process **dispatch bus** (`DispatchFacade.publishEvent` / `dispatchJob` → Redis lists `dispatch:default` / `dispatch:slow` / `dispatch:sms` plus a delayed ZSET), whose message names preserve the legacy PHP job/event class names; all time-driven work is triggered by **XXL-JOB 2.5.0** (executor appname `ecshopx-executor`, port 19999) against a separately deployed admin console. Cross-Bundle calls never create Maven cycles: the consumer owns a Port or publisher interface (usually in `ecshopx-common`), the provider implements it, and `ecshopx-bootstrap` — which depends on everything — wires the remaining edges through `*DispatchPublisherImpl` and `*RegistrationConfig` classes in its `config` package.

## Route your task

| If your task is… | Read |
|---|---|
| Get the big picture first — reactor layout, runtime topology, ports, the single Spring context, the Bundle catalog | [System Overview and Runtime Topology](architecture/overview.md) |
| Understand the inside of one Bundle and where new code goes (`api/admin/v1` / `api/front/v1` / `api/open`, `service`, `domain`, `mapper`, `integration`, `dispatch`, `cron`, `port`) | [Bundle Anatomy and Layering Conventions](architecture/bundle-layering.md) |
| Add or change an HTTP endpoint — auth annotations (`@AdminAuth` / `@FrontAuth` / `@FrontNoAuth` …), `@FlexibleBody` binding, `ApiResult` / `@DingoResponse` envelopes | [HTTP Request Lifecycle, Auth, and Response Conventions](architecture/request-lifecycle.md), then [Bundle Anatomy](architecture/bundle-layering.md) |
| Work the store-cashier operator surface — pre-checkout member readiness `POST /api/v1/operator/member/ready` (`OperatorStoreMemberController` in ecshopx-companys delegating to Shuyun's `OperatorStoreMemberReadinessService.ensureOfflineMemberAtStore`, which skips when the company's Shuyun open-platform config is disabled/ineligible and otherwise registers the member offline via `member.register`) and the operator cart fast-buy apis | [System Overview and Runtime Topology](architecture/overview.md) (operator api surface), then [Third-Party Integrations](integrations/third-party.md) (Shuyun open platform) |
| Touch the POS auth surface — the POS dual-JWT filter chain (`PosJwtAuthenticationFilter` / `PosJwtIssuer` in ecshopx-espier, `token_kind=pos`, `POS_JWT_ISSUER`=`ecshopx-pos`, blacklist/session-validator Ports) or the `pos_store_account` handling accounts | [HTTP Request Lifecycle, Auth, and Response Conventions](architecture/request-lifecycle.md) (filter chain), then [Stores, Distributors, Merchants, and Salespersons](domains/stores-and-merchants.md) (POS store accounts) |
| Make two Bundles collaborate without a Maven cycle (Ports, publisher interfaces, `ObjectProvider`, bootstrap adapters — e.g. the new `PaymentSubjectDistributorIdPort` in `ecshopx-common`, implemented by `ecshopx-distribution`, consumed by the ecshopx-orders payment services) | [Cross-Bundle Ports and Wiring in Bootstrap](architecture/cross-bundle-communication.md) |
| Add a dispatch event, dispatch job, or the XXL-JOB cron handler that triggers it | [Workflow: Adding a Dispatch Event, Job, or Cron Handler](workflows/add-event-or-job.md) |
| Debug dispatch delivery — queues, fan-out, delays, retries, `failed_jobs` | [Dispatch Bus (ecshopx-dispatch)](concepts/dispatch-bus.md) |
| Understand, add, or re-time scheduled/cron work | [Scheduled Jobs: XXL-JOB Executor and Cron Handlers](concepts/scheduling-xxljob.md) |
| Change the database schema (entity → `bin/make-migration` → Flyway `V*.sql`) | [Workflow: Schema Changes with bin/make-migration and Flyway](workflows/schema-migration.md) |
| Understand persistence, mapper conventions, or the `company_id` tenancy pattern | [Persistence: MySQL, MyBatis-Plus, Mappers, and Tenancy](concepts/persistence-mybatis.md) |
| Debug the money path — cart → order create → payment → async notify → trade-finish fan-out. `OrdersPaymentDoPaymentService.doPayment` splits work into three phases (TX1 ensures the pending trade / completes local point-deposit-pos-zero-fee payments, channel SDK calls run outside any DB transaction, TX2 persists the outcome) and publishes trade-finish only after a successful finalize; it resolves the payment-subject distributor via `PaymentSubjectDistributorIdPort` before validating config or invoking the channel. The order-88 brokerage listener (`TradeFinishCountBrokerageDispatchListener`, ECX-11089) is now a thin gate that delegates to `TradeFinishCountBrokerageBusService.handleTradeFinishRow` — point-pay/blank-id pre-filters, then positive-id re-parses, order-not-found and company-mismatch skips, and a warn-not-throw call into `NormalOrderBrokerageOnFinishService.orderFinishBrokerage` (all logged under `[popularize-trade-finish]`) — which runs the popularize bundle's `NormalOrderFinishBrokerageCoordinator`: one `TransactionTemplate` that resolves the parent promoter through the **buyer's `pid`**, inserts the `first_level` brokerage row keyed on the parent's `user_id` with a non-null `plan_close_time` backfill (from `ecshopx.popularize.brokerage-plan-close.{settle-days,aftersale-days}`, both defaulting to 0), increments the parent's statistics, flips the buyer's `is_buy`, and enqueues the grade-upgrade job. Covers both entries: the wxapp member flow and the admin 代客下单 deferred create-and-pay flow (`POST /api/v1/order/create` in `NormalOrderController` runs `ShopadminNormalOrderCreateOrchestrator`, which enforces `order_type` = `normal_shopadmin`, prices without inserting DB rows, and saves a Redis `WAIT_PAY` snapshot under `shopadmin:deferred:<companyId>:<orderId>`; `POST /api/v1/order/payment` detects the snapshot and `ShopadminDeferredOrderService.payDeferredOrder` charges the channel, materializes the `NormalOrders` row, and refunds on business failure) | [Workflow: Checkout, Payment, Notify, and Trade-Finish Fan-out](workflows/order-payment-flow.md) (payment core and the order-88 brokerage listener), [Orders Domain](domains/orders.md), [Stores, Distributors, Merchants, and Salespersons](domains/stores-and-merchants.md) (pid-based first-level brokerage write) |
| Payment channel surfaces (wechat/alipay/adapay/hfpay/bspay/chinaums notify endpoints, refunds, withdrawals) and the payment-subject contract — which distributor's merchant config a payment charges against (`PaymentSubjectDistributorIdPort` resolves `distributor_id` → 0 = platform config unless the distributor's `payment_subject` flag is 1) | [Payments and Channels](domains/payments.md), [Cross-Bundle Ports and Wiring in Bootstrap](architecture/cross-bundle-communication.md) (port mechanics) |
| Enable, disable, or tune whether a distributor settles with its own payment config — the `payment_subject` flag on the distributor record and `PaymentSubjectDistributorIdPortImpl` in ecshopx-distribution (returns the distributor id only when `payment_subject`=1, else 0 = platform subject) | [Stores, Distributors, Merchants, and Salespersons](domains/stores-and-merchants.md) (distributor model), then [Payments and Channels](domains/payments.md) |
| Enable, disable, or tune a third-party integration (Jushuitan, WdtErp, OME, DM CRM, Prism, Shuyun, Youshu, cross-border customs) | [Third-Party Integrations](integrations/third-party.md) |
| Work on DigiOS retail (`ecshopx-digios`) — gateway client, per-company config, store/account/stock/adjustment sync, `digios_api_log` | [Third-Party Integrations](integrations/third-party.md) (DigiOS section), [Configuration and Secrets Reference](operations/configuration.md) (DigiOS properties) |
| Work on the public Open API surface (`/api/openapi/**`, app_key/sign/timestamp auth with replay guard, handler forward registry, DigiOS stock-push endpoint) | [OpenAPI Gateway (ecshopx-openapi)](integrations/openapi.md) |
| Touch Redis — which logical DB (sessions, carts, captcha, pickup QR, …), which template, settings-store and counter patterns, or the shop-protocol hash that now repopulates from MySQL (`ShopProtocolSetService.get` reads the companys Redis hash first and rebuilds a cold/evicted hash from `companys_protocol_update_log` before answering, ECX-11114 — flushing the companys DB no longer loses published protocols) | [Redis Database Layout](concepts/redis-databases.md), [Stores, Distributors, Merchants, and Salespersons](domains/stores-and-merchants.md) (protocol service contract) |
| Deploy or run the stack (`dev-setup.sh` + `docker-compose.dev.yml` full dev, `docker-lite/pack.sh` + `deploy.sh` offline pack, `install.sh --full/--lite` bootstrap, self-hosted jar) | [Deployment: Compose, Full Dev, and Lite Offline](operations/deployment.md) |
| Find or change a config key, env overlay, or secret — including the new `ecshopx.popularize.brokerage-plan-close.settle-days` / `.aftersale-days` placeholders (both default `0`) feeding the brokerage `plan_close_time` backfill | [Configuration and Secrets Reference](operations/configuration.md) |
| Write, name, or run tests | [Testing Conventions and Verification](testing.md) |

### Domain logic changes

| Domain you are changing | Page |
|---|---|
| Products, SKUs, categories, inventory, checkout-time goods services — including the platform item-delete cascade in `PlatformItemsDeleteService.deleteCore`, which now purges operator carts (`operatorCartCleanupService.deleteByCompanyIdAndItemIds`) **before** removing SKU rows and publishes `itemDeleteEventDispatchPublisher.publishAfterCommit` only after all deletes | [Goods Domain (ecshopx-goods)](domains/goods.md) |
| Orders, cart, logistics, settlement, the `Trade` vs `NormalOrders` model | [Orders Domain (ecshopx-orders)](domains/orders.md) |
| After-sales, refund pipeline and per-channel refund executors | [Aftersales Domain (ecshopx-aftersales)](domains/aftersales.md) |
| Members, login orchestration, member JWT, create/update fan-out — including the wxapp pre-login auto-register path (`createMemberLocalAutoRegister`), whose in-transaction side effects now also 补齐 promoter/distribution state (`AdminMemberCreatePromoterSideEffectPort.createForNewMember`, `AdminMemberCreateDistributorUserSideEffectPort.createData`, `ShopRelMember` insert when `distributor_id` is present, `if_register_promotion` in the create-member-success payload) — member system tags (`tag_kind` / `tag_code`, reconcile services, `MemberSystemTagWriteGuard`) | [Members Domain (ecshopx-members)](domains/members.md) |
| Member-crowd targeting in promotions (`member_tag_ids` / `member_tag_mode` matched by `MemberCrowdMatcher` against member system tags) | [Marketing and Loyalty Domains](domains/marketing.md), backed by [Members Domain](domains/members.md) |
| Promotions, coupons/kaquan, points, points mall, deposit, employee purchase | [Marketing and Loyalty Domains](domains/marketing.md) |
| Companies/shops, distributors-as-stores, merchants, salespersons, suppliers, community group-buy — plus the store-side surfaces that changed recently: shop protocols cached in Redis with a `companys_protocol_update_log` DB repopulation fallback (ECX-11114), non-default-language data-role creates writing empty-string `role_name` placeholders (ECX-11083), the pid-based first-level brokerage write with `plan_close_time` backfill and `[popularize-trade-finish]` diagnostics (ECX-11089), and the `distributor_id=0` default brokerage-list branch (ECX-11088) | [Stores, Distributors, Merchants, and Salespersons](domains/stores-and-merchants.md) |
| WeChat Open Platform, mini-programs, official account, WeCom | [WeChat Ecosystem (ecshopx-wechat, ecshopx-work-wechat)](domains/wechat.md) |
| Espier platform capabilities (storage drivers, uploads, middleware/interceptors) and the other supporting Bundles (super-admin, theme, data-cube, comments, im, form, …) | [Platform Services and Supporting Bundles](domains/platform-services.md) |

## Ground rules for working in this repository

1. **Source code and tests are authoritative.** `AGENTS.md` states this explicitly for this repository: treat source and tests as the ground truth over any brief or README claim, and prefer the narrowest quiet validation that proves the changed behavior. Concretely: when the README disagrees with the code or the compose files, follow the code.
2. **The English readme has a known drift.** Its "Docker Deployment (Recommended)" section describes a root `docker-compose.yml` and a `docker/Dockerfile` that do **not** exist in the working tree — the real stacks are `docker-compose.dev.yml` and `docker-lite/docker-compose.yml` (see the note in [Deployment](operations/deployment.md)). Also, both compose stacks publish the XXL-Job admin on host port **8080**, while `9080` is only the self-hosted default in `application.properties`. In the same "Self-Hosted Deployment" section, the readme claims the repo "already includes" `ecshopx-bootstrap/src/main/resources/application-local.properties` — that file is git-ignored (`.gitignore` line 7) and absent from fresh checkouts; local overrides actually arrive via env vars, Spring Boot CLI args (`APP_ARGS`), or `--spring.config.additional-location=file:./config/` (see [Configuration](operations/configuration.md)). Both compose stacks run the same four services: `mysql:8.0` (seeded from `docker/tables_xxl_job.sql` then `docker/ecshopx.sql`), `redis:7-alpine`, `xuxueli/xxl-job-admin:2.5.0`, and the unified `ecshopx-app` container (Java + OpenResty + Nuxt) built from `docker/Dockerfile.app`, wired with `depends_on: service_healthy` ordering; the three operator paths converge on it — full development via `dev-setup.sh` + `docker-compose.dev.yml`, lite offline via `docker-lite/pack.sh` + `docker-lite/deploy.sh` (prebuilt jar and frontend dists), and `install.sh --full` / `--lite` as the zero-dependency bootstrap that hands off to them.
3. **Javadoc and config comments are largely Chinese.** Write new wiki/code prose in English, but keep config keys, dispatch event/job names (e.g. `event:288:OrdersBundle\Events\NormalOrderPaySuccessEvent`, `job:128:OrdersBundle\Jobs\FinishOrderJob`), handler names (e.g. `finish-orders`), and Chinese error strings quoted verbatim — they are contract identifiers, not prose.
4. **When gathering evidence, skip** `target/`, `.flattened-pom.xml`, docker SQL dumps (`docker/ecshopx.sql`, `docker/tables_xxl_job.sql`), logos/media, and local `logs/`.

## First commands

```bash
# Build one Bundle with its dependencies
./mvnw -pl ecshopx-orders -am clean install -DskipTests

# Run all tests for one Bundle
./mvnw -pl ecshopx-orders test

# Run one test class (the narrowest verification)
./mvnw -pl ecshopx-orders test -Dtest=OrderXxxTest

# Pre-PR gate from the readme's contributing checklist
./mvnw -pl <module> -am verify

# Run the app locally (Maven; profile "local" — note: no application-local.properties is
# shipped: it is git-ignored, so provide overrides via env vars, --key=value program
# arguments, or --spring.config.additional-location=file:./config/)
./mvnw -pl ecshopx-bootstrap -am spring-boot:run -Dspring-boot.run.profiles=local

# Generate / apply a schema migration
bin/make-migration add_order_extra_index   # entity-annotation → SQL diff, review before applying
bin/make-migration migrate                 # Flyway Maven plugin

# Dev compose stack (mysql:8.0 seeded from docker/tables_xxl_job.sql + docker/ecshopx.sql,
# redis:7-alpine, xxl-job-admin:2.5.0, unified ecshopx-app built from docker/Dockerfile.app —
# Java + OpenResty + Nuxt in one container, joined by depends_on service_healthy)
docker compose -f docker-compose.dev.yml up -d
```

Ports to keep free: `18080` (application HTTP), `19999` (XXL-JOB executor), `8080` (XXL-JOB admin, compose) / `9080` (XXL-JOB admin, self-hosted default), `3306` (MySQL), `6379` (Redis). After startup the app exposes `http://localhost:18080/api/`, `http://localhost:18080/storage/`, and `http://localhost:18080/wechatAuth/`.

The one `@SpringBootTest` in the repository (`EcshopxApplicationTests.contextLoads`) needs the configured MySQL/Flyway environment, so it is not part of the everyday loop — see [Testing](testing.md) for the probe/flow test taxonomy and which `mvnw -Dtest=...` command proves which change type.
