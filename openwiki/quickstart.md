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
  - id: openwiki-source-609b6f5d2dab35545395d32b
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/EcshopxApplicationTests.java
  - id: openwiki-source-27bfcace5400d2804562fba2
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersDispatchEventNames.java
  - id: openwiki-source-5ecb22a008c7475c40eec372
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersDispatchJobNames.java
  - id: openwiki-source-a1462091af0ac6e453fae382
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/DispatchFacade.java
  - id: openwiki-source-3c3a1a52e93c103069ee1377
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/redis/RedisDispatchDriver.java
  - id: openwiki-source-bdf2e5ba61aa04534071bea1
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/redis/RedisDispatchListener.java
  - id: openwiki-source-0a1caaa71538649542913a46
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/admin/v1/NormalOrderController.java
  - id: openwiki-source-f2e1c65d6d39cb3db1a8a7e8
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/WxappPaymentController.java
  - id: openwiki-source-c96e813e9374d09c67f360cd
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/cron/ScheduleConsumptionOrdersHandler.java
  - id: openwiki-source-18a40afbe2f8cf1c732efd6f
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/cron/ScheduleFinishOrdersHandler.java
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
  - id: openwiki-source-12342b48156bc08b3014932f
    resource: repo://ecshopx-orders/src/main/resources/mapper/NormalOrdersMapper.xml
  - id: openwiki-source-4c7a3bc700c378ccd65c4327
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/api/open/WxpayNotifyController.java
  - id: openwiki-source-03ffc32a0ca502ab67c54b25
    resource: repo://install.sh
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
  - id: openwiki-source-f1b1aacc63642e5304668821
    resource: repo://readme.md
generated: { by: "openwiki/0.7.2", at: "2026-10-23T03:04:20.471Z" }
---

# ECShopX-Java Wiki Quickstart

ECShopX-Java is the Java server-side reimplementation of the ECShopX commerce system, keeping business semantics and configuration keys aligned with the legacy PHP version. It is **one multi-module Maven reactor** (parent `cn.shopex:ecshopx`, `${revision}` = `5.2.0`, on `spring-boot-starter-parent:3.5.12` / Java 17) of **52 modules**: four infrastructure modules — `ecshopx-bom`, `ecshopx-common` (shared contracts, no peer dependencies), `ecshopx-dispatch` (the message bus), and `ecshopx-bootstrap` (the only Spring Boot executable, which assembles everything) — plus **48 business Bundles**, one per domain (goods, orders, aftersales, members, marketing, payments, WeChat, third-party integrations, …). The single application (`cn.shopex.ecshopx.EcshopxApplication`) runs on Undertow, port **18080**, and serves the PHP-compatible `/api/`, `/storage/`, and `/wechatAuth/` URL spaces. State lives in **one MySQL database** (`ecshopx`, utf8mb4, `Asia/Shanghai`) reached through **MyBatis-Plus 3.5** mappers with pervasive `company_id` tenant scoping, plus **one Redis instance** partitioned into logical databases by hand-built Lettuce factories. All cross-Bundle asynchronous work rides the in-process **dispatch bus** (`DispatchFacade.publishEvent` / `dispatchJob` → Redis lists `dispatch:default` / `dispatch:slow` / `dispatch:sms` plus a delayed ZSET), whose message names preserve the legacy PHP job/event class names; all time-driven work is triggered by **XXL-JOB 2.5.0** (executor appname `ecshopx-executor`, port 19999) against a separately deployed admin console. Cross-Bundle calls never create Maven cycles: the consumer owns a Port or publisher interface (usually in `ecshopx-common`), the provider implements it, and `ecshopx-bootstrap` — which depends on everything — wires the remaining edges through `*DispatchPublisherImpl` and `*RegistrationConfig` classes in its `config` package.

## Route your task

| If your task is… | Read |
|---|---|
| Get the big picture first — reactor layout, runtime topology, ports, the single Spring context, the Bundle catalog | [System Overview and Runtime Topology](architecture/overview.md) |
| Understand the inside of one Bundle and where new code goes (`api/admin/v1` / `api/front/v1` / `api/open`, `service`, `domain`, `mapper`, `integration`, `dispatch`, `cron`, `port`) | [Bundle Anatomy and Layering Conventions](architecture/bundle-layering.md) |
| Add or change an HTTP endpoint — auth annotations (`@AdminAuth` / `@FrontAuth` / `@FrontNoAuth` …), `@FlexibleBody` binding, `ApiResult` / `@DingoResponse` envelopes | [HTTP Request Lifecycle, Auth, and Response Conventions](architecture/request-lifecycle.md), then [Bundle Anatomy](architecture/bundle-layering.md) |
| Make two Bundles collaborate without a Maven cycle (Ports, publisher interfaces, `ObjectProvider`, bootstrap adapters) | [Cross-Bundle Ports and Wiring in Bootstrap](architecture/cross-bundle-communication.md) |
| Add a dispatch event, dispatch job, or the XXL-JOB cron handler that triggers it | [Workflow: Adding a Dispatch Event, Job, or Cron Handler](workflows/add-event-or-job.md) |
| Debug dispatch delivery — queues, fan-out, delays, retries, `failed_jobs` | [Dispatch Bus (ecshopx-dispatch)](concepts/dispatch-bus.md) |
| Understand, add, or re-time scheduled/cron work | [Scheduled Jobs: XXL-JOB Executor and Cron Handlers](concepts/scheduling-xxljob.md) |
| Change the database schema (entity → `bin/make-migration` → Flyway `V*.sql`) | [Workflow: Schema Changes with bin/make-migration and Flyway](workflows/schema-migration.md) |
| Understand persistence, mapper conventions, or the `company_id` tenancy pattern | [Persistence: MySQL, MyBatis-Plus, Mappers, and Tenancy](concepts/persistence-mybatis.md) |
| Debug the money path — cart → order create → payment → async notify → trade-finish fan-out; covers both entries: the wxapp member flow and the admin 代客下单 deferred create-and-pay flow (`POST /api/v1/order/create` → Redis `WAIT_PAY` snapshot → `POST /api/v1/order/payment` materialize-and-charge with refund-on-failure) | [Workflow: Checkout, Payment, Notify, and Trade-Finish Fan-out](workflows/order-payment-flow.md), [Orders Domain](domains/orders.md) |
| Payment channel surfaces (wechat/alipay/adapay/hfpay/bspay/chinaums notify endpoints, refunds, withdrawals) | [Payments and Channels](domains/payments.md) |
| Enable, disable, or tune a third-party integration (Jushuitan, WdtErp, OME, DM CRM, Prism, Shuyun, Youshu, cross-border customs) | [Third-Party Integrations](integrations/third-party.md) |
| Work on the public Open API surface (`/api/openapi/**`, sign verification, forward registry) | [OpenAPI Gateway](integrations/openapi.md) |
| Touch Redis — which logical DB, which template, settings-store and counter patterns | [Redis: Logical Database Isolation and Templates](concepts/redis-databases.md) |
| Deploy or run the stack (dev compose, lite offline pack, self-hosted jar) | [Deployment: Compose, Full Dev, and Lite Offline](operations/deployment.md) |
| Find or change a config key, env overlay, or secret | [Configuration and Secrets Reference](operations/configuration.md) |
| Write, name, or run tests | [Testing Conventions and Verification](testing.md) |

### Domain logic changes

| Domain you are changing | Page |
|---|---|
| Products, SKUs, categories, inventory, checkout-time goods services | [Goods Domain](domains/goods.md) |
| Orders, cart, logistics, settlement, the `Trade` vs `NormalOrders` model | [Orders Domain](domains/orders.md) |
| After-sales, refund pipeline and per-channel refund executors | [Aftersales Domain](domains/aftersales.md) |
| Members, login orchestration, member JWT, create/update fan-out | [Members Domain](domains/members.md) |
| Promotions, coupons/kaquan, points, points mall, deposit, employee purchase | [Marketing and Loyalty Domains](domains/marketing.md) |
| Companies/shops, distributors-as-stores, merchants, salespersons, suppliers, community group-buy | [Stores, Distributors, Merchants, and Salespersons](domains/stores-and-merchants.md) |
| WeChat Open Platform, mini-programs, official account, WeCom | [WeChat Ecosystem](domains/wechat.md) |
| Espier platform capabilities (storage drivers, uploads, middleware/interceptors) and the other supporting Bundles (super-admin, theme, data-cube, comments, im, form, …) | [Platform Services and Supporting Bundles](domains/platform-services.md) |

## Ground rules for working in this repository

1. **Source code and tests are authoritative.** `AGENTS.md` states this explicitly for this repository: treat source and tests as the ground truth over any brief or README claim, and prefer the narrowest quiet validation that proves the changed behavior. Concretely: when the README disagrees with the code or the compose files, follow the code.
2. **The English readme has a known drift.** Its "Docker Deployment (Recommended)" section describes a root `docker-compose.yml` and a `docker/Dockerfile` that do **not** exist in the working tree — the real stacks are `docker-compose.dev.yml` and `docker-lite/docker-compose.yml` (see the note in [Deployment](operations/deployment.md)). Also, both compose stacks publish the XXL-Job admin on host port **8080**, while `9080` is only the self-hosted default in `application.properties`. In the same "Self-Hosted Deployment" section, the readme claims the repo "already includes" `ecshopx-bootstrap/src/main/resources/application-local.properties` — that file is git-ignored (`.gitignore` line 7) and absent from fresh checkouts; local overrides actually arrive via env vars, Spring Boot CLI args (`APP_ARGS`), or `--spring.config.additional-location=file:./config/` (see [Configuration](operations/configuration.md)).
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

# Dev compose stack (mysql + redis + xxl-job-admin + ecshopx-app)
docker compose -f docker-compose.dev.yml up -d
```

Ports to keep free: `18080` (application HTTP), `19999` (XXL-JOB executor), `8080` (XXL-JOB admin, compose) / `9080` (XXL-JOB admin, self-hosted default), `3306` (MySQL), `6379` (Redis). After startup the app exposes `http://localhost:18080/api/`, `http://localhost:18080/storage/`, and `http://localhost:18080/wechatAuth/`.

The one `@SpringBootTest` in the repository (`EcshopxApplicationTests.contextLoads`) needs the configured MySQL/Flyway environment, so it is not part of the everyday loop — see [Testing](testing.md) for the probe/flow test taxonomy and which `mvnw -Dtest=...` command proves which change type.
