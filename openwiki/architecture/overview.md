---
type: architecture-overview
title: System Overview and Runtime Topology
description: The big-picture map of ECShopX-Java — the Spring Boot 3.5 / Java 17 multi-module Maven reactor under cn.shopex:ecshopx, the single ecshopx-bootstrap Spring Boot entry that assembles 49 business Bundles, and the runtime topology (Undertow on 18080, MySQL via MyBatis-Plus, multi-DB Redis, XXL-JOB admin + executor, Redis-backed dispatch queues, and the /api, /storage, /wechatAuth URL spaces).
tags: [architecture, overview, maven, bootstrap, undertow, redis, xxl-job, dispatch, deployment, topology]
sources:
  - id: openwiki-source-ea70eb6c045047448e446296
    resource: repo://.gitignore
  - id: openwiki-source-0c106052b3286e779bed85e7
    resource: repo://docker-compose.dev.yml
  - id: openwiki-source-dd5481b4e6b92198f192e715
    resource: repo://docker/app-nginx.conf
  - id: openwiki-source-c68757c87447d8ed7a07d480
    resource: repo://ecshopx-bom/pom.xml
  - id: openwiki-source-84aeb105875424c9aac7edf8
    resource: repo://ecshopx-bootstrap/pom.xml
  - id: openwiki-source-e7cc7316dd2ab59d048ac799
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/AsyncExecutorConfig.java
  - id: openwiki-source-63a9376edbf2e27b60bd70b7
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsAftersalesReturnAdjustmentDispatchListenerRegistrationConfig.java
  - id: openwiki-source-b9efedc29ed060e05f8a7583
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsAftersalesReturnAdjustmentDispatchPublisherImpl.java
  - id: openwiki-source-1adeed0e9d51f13c65d98eb6
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsDistributorCreateDispatchListenerRegistrationConfig.java
  - id: openwiki-source-214f4a43a8dbc13e5179d2dc
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsNormalOrderDeliveryAdjustmentDispatchListenerRegistrationConfig.java
  - id: openwiki-source-893123407cdb2a42fed65796
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DigiOsOperatorCreateDispatchListenerRegistrationConfig.java
  - id: openwiki-source-c83108e0b31507dc4b736d71
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/DispatchBusConfig.java
  - id: openwiki-source-225eace12759f1e8a45e5306
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/FinishOrderJobDispatchPublisherImpl.java
  - id: openwiki-source-6154c1d27f3b807ac0dd5b04
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/FinishOrderJobRegistrationConfig.java
  - id: openwiki-source-0904c0e3cebac22f3a004917
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MigrationSeedRunner.java
  - id: openwiki-source-d796c74a7df3139966d43e8d
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/MultiRedisConfig.java
  - id: openwiki-source-f3b71eb9d6873d40cbc6bb31
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/PosStoreAccountCreateEventDispatchPublisherImpl.java
  - id: openwiki-source-ebbcd0aeec7940f4f0465f31
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformCreateMemberSuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-6794b7e88ab1775f38c31571
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/ShuyunOpenPlatformNormalOrderPaySuccessDispatchListenerRegistrationConfig.java
  - id: openwiki-source-919f62d871898539e11d1107
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/StrictHttpFirewallConfig.java
  - id: openwiki-source-ee9e1dcff5d604ff92225a67
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/UndertowMultipartMaxEntityCustomizer.java
  - id: openwiki-source-7ccfb227c3f9afcfbaaf0bc3
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/XxlJobConfig.java
  - id: openwiki-source-c108088c8dc8a99034aeaa25
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/EcshopxApplication.java
  - id: openwiki-source-89b43138f8e75edb4615495b
    resource: repo://ecshopx-bootstrap/src/main/resources/application.properties
  - id: openwiki-source-b5bab8b393f9be290ee40867
    resource: repo://ecshopx-bootstrap/src/main/resources/db/migration/V00000000000001.sql
  - id: openwiki-source-306979beecba6167a958ddc4
    resource: repo://ecshopx-bootstrap/src/main/resources/db/migration/V00000000000006.sql
  - id: openwiki-source-ed1e2ce5fcfefbaf52c68d75
    resource: repo://ecshopx-bootstrap/src/main/resources/db/migration/V00000000000007.sql
  - id: openwiki-source-27abacbd20fe67d63b27d863
    resource: repo://ecshopx-bootstrap/src/main/resources/seed/menu/platform_menu.json
  - id: openwiki-source-edac2d50792508faa99e2b5d
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/auth/PosJwtIssuerPort.java
  - id: openwiki-source-c0c2361e788fa42a7b16fbd2
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/CompanysDispatchEventNames.java
  - id: openwiki-source-809e54f4e38e5137797c41dd
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/FinishOrderJobDispatchPublisher.java
  - id: openwiki-source-5ecb22a008c7475c40eec372
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/dispatch/OrdersDispatchJobNames.java
  - id: openwiki-source-2c4a54ebc839a0b89bdbcdf0
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/mybatis/FqcnMapperBeanNameGenerator.java
  - id: openwiki-source-23521877b190e50eccec4c0a
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/admin/v1/OperatorCartController.java
  - id: openwiki-source-c4239fd10cbbee9db016ba54
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/admin/v1/OperatorsController.java
  - id: openwiki-source-2867f272d3b645f1b2e95e5a
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/admin/v1/OperatorStoreMemberController.java
  - id: openwiki-source-e8ae20ad9a0b9aab932192bb
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/pos/v1/PosAuthController.java
  - id: openwiki-source-80ae9d88e25e6269f15157ab
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posauth/PosAuthService.java
  - id: openwiki-source-48f750a8142d8a7487ac4de7
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posauth/PosJwtSessionValidator.java
  - id: openwiki-source-27d0d0b9eb5f333a0fcbcf84
    resource: repo://ecshopx-digios/pom.xml
  - id: openwiki-source-bdf2e5ba61aa04534071bea1
    resource: repo://ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/redis/RedisDispatchListener.java
  - id: openwiki-source-c3ce781e36cc4f90ca50966a
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/package-info.java
  - id: openwiki-source-a1f68480771cd92413f00be5
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtAuthenticationFilter.java
  - id: openwiki-source-043701b554e6049237c22c70
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtBlacklistService.java
  - id: openwiki-source-da7950ab19f4690ae361bd4b
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtIssuer.java
  - id: openwiki-source-a7b2be30f60d866cf32e516a
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtPathWhitelist.java
  - id: openwiki-source-b56e4ca177932e823056dbc7
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/storage/StorageWebMvcConfigurer.java
  - id: openwiki-source-9cf33b4716411b029c0c6147
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/package-info.java
  - id: openwiki-source-6d42ce597e8782d354259630
    resource: repo://ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/service/operatorcart/OperatorCartFastBuyFacadeImpl.java
  - id: openwiki-source-2a6ed641b7be57a3f7fcd6a5
    resource: repo://ecshopx-openapi/src/main/java/cn/shopex/ecshopx/openapi/web/OpenapiAuthInterceptor.java
  - id: openwiki-source-90f352ef36e29440d99e03dd
    resource: repo://ecshopx-openapi/src/main/java/cn/shopex/ecshopx/openapi/web/OpenapiPathPatterns.java
  - id: openwiki-source-4889f150849f66bdb3e774ab
    resource: repo://ecshopx-openapi/src/main/java/cn/shopex/ecshopx/openapi/web/OpenapiSignedRequestReplayGuard.java
  - id: openwiki-source-c3326a09b0b53b767ed6480a
    resource: repo://ecshopx-openapi/src/main/java/cn/shopex/ecshopx/openapi/web/OpenapiWebMvcConfigurer.java
  - id: openwiki-source-86ec1788b9f975eaacc0039f
    resource: repo://ecshopx-openapi/src/test/java/cn/shopex/ecshopx/openapi/web/OpenapiAuthInterceptorReplayTest.java
  - id: openwiki-source-05f1689fa8b246986a24bcd3
    resource: repo://ecshopx-openapi/src/test/java/cn/shopex/ecshopx/openapi/web/OpenapiSignedRequestReplayGuardTest.java
  - id: openwiki-source-18a40afbe2f8cf1c732efd6f
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/cron/ScheduleFinishOrdersHandler.java
  - id: openwiki-source-a911baf6f19f0480e6a5726f
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/package-info.java
  - id: openwiki-source-4c7a3bc700c378ccd65c4327
    resource: repo://ecshopx-payment/src/main/java/cn/shopex/ecshopx/payment/api/open/WxpayNotifyController.java
  - id: openwiki-source-6af025b4a30ccc1921cecad2
    resource: repo://ecshopx-shopmenu-border/src/main/java/cn/shopex/ecshopx/shopmenuborder/service/ShopMenuUploadService.java
  - id: openwiki-source-360776dc08765b4b83e96cfb
    resource: repo://ecshopx-shuyun/src/main/java/cn/shopex/ecshopx/shuyun/package-info.java
  - id: openwiki-source-6b0c60a3857f097ae7426122
    resource: repo://ecshopx-shuyun/src/main/java/cn/shopex/ecshopx/shuyun/service/openplatform/OperatorStoreMemberReadinessService.java
  - id: openwiki-source-fa3bf25fcd64c709a6ccaa9b
    resource: repo://ecshopx-tb-items/src/main/java/cn/shopex/ecshopx/tbitems/package-info.java
  - id: openwiki-source-0faf211cd4710387baf9c288
    resource: repo://ecshopx-wechat/src/main/java/cn/shopex/ecshopx/wechat/api/open/WechatOpenPlatformMessageCallbackController.java
  - id: openwiki-source-4a45b8522d5a5e9782931f61
    resource: repo://ecshopx-wechat/src/main/java/cn/shopex/ecshopx/wechat/api/open/WxappLegacyQrcodeController.java
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
  - id: openwiki-source-f1b1aacc63642e5304668821
    resource: repo://readme.md
generated: { by: "openwiki/0.9.0", at: "2026-11-10T17:10:58.497Z" }
verified:
  - by: openwiki/0.9.0
    at: 2026-11-10T17:10:58.497Z
---

# System Overview and Runtime Topology

ECShopX-Java is the Java server-side implementation of the ECShopX commerce system, rewritten from the legacy PHP codebase with aligned business semantics and one-to-one mapped configuration keys. The whole repository is a single **multi-module Maven reactor**: the root `pom.xml` declares the parent project `cn.shopex:ecshopx` (version `${revision}` = `5.5.2`, CI-friendly, Java 17, MyBatis-Plus 3.5.16, jjwt 0.12.6) under `spring-boot-starter-parent:3.5.12`, and lists every module as a `<module>`. The reactor has 53 modules: exactly four infrastructure modules plus 49 business "Bundles", and every Bundle is also pinned in the parent `dependencyManagement` so Bundle POMs never declare versions:

* **`ecshopx-bom`** — a `pom`-packaging bill-of-materials module that imports `spring-boot-dependencies:3.5.12` and pins MyBatis-Plus / Lombok versions.
* **`ecshopx-common`** — shared cross-cutting code with **no** Shopex peer dependencies (only Spring Boot starters, MyBatis-Plus, Lombok): unified response body, exception hierarchy, pagination, audit base classes, annotations, the `common/auth` JWT ports (`OperatorJwtIssuerPort`, `PosJwtIssuerPort`, `PosJwtSessionValidatorPort`, blacklist ports), and the ~150 `common/dispatch` interfaces (dispatch job/event names + publisher ports) that keep cross-Bundle wiring acyclic.
* **`ecshopx-dispatch`** — the in-process dispatch-bus core (`DispatchFacade`, drivers, registry, retry/failed-job recording), depending on common + Spring Data Redis + Spring AMQP.
* **`ecshopx-bootstrap`** — the only Spring Boot executable. Its POM declares all 49 business Bundles plus the runtime starters (Undertow, Data Redis, AMQP, Flyway core+mysql, MySQL connector, `xxl-job-core:2.5.0`), and the `spring-boot-maven-plugin` is re-enabled here (`skip=false`, `mainClass=cn.shopex.ecshopx.EcshopxApplication`) while the parent POM disables it everywhere else.

One business domain = one Bundle = one Maven module. Bundles are plain libraries that expose capabilities; `ecshopx-bootstrap` wires everything together and runs as one Spring application. For how a single Bundle is structured internally, see [Bundle Layering](bundle-layering.md); for how Bundles communicate without Maven cycles, see [Cross-Bundle Communication](cross-bundle-communication.md).

## The single Spring Boot entry and the @MapperScan decision

```java
@SpringBootApplication(scanBasePackages = "cn.shopex.ecshopx")
@MapperScan(
        basePackages = "cn.shopex.ecshopx.**.mapper",
        nameGenerator = FqcnMapperBeanNameGenerator.class,
        sqlSessionFactoryRef = "sqlSessionFactory",
        lazyInitialization = "true")
public class EcshopxApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcshopxApplication.class, args);
    }
}
```

The main class `cn.shopex.ecshopx.EcshopxApplication` (in `ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/EcshopxApplication.java`) is deliberately the **only** place `@MapperScan` appears, and the Javadoc on the class records why:

1. **Bean-name collisions.** With 49 Bundles each carrying `mapper/` packages, same-name `*Mapper` interfaces are inevitable. MyBatis' default bean name is the short interface name (e.g. `statisticsMapper`), so two `StatisticsMapper` interfaces in different Bundles would collide. `FqcnMapperBeanNameGenerator` (in `ecshopx-common`, `common/mybatis/FqcnMapperBeanNameGenerator.java`) replaces every `.` in the fully-qualified class name with `_` for any class whose name ends in `Mapper`, and falls back to Spring's default generator otherwise.
2. **Registration order vs. auto-configuration.** Placing `@MapperScan` on the main class makes `MapperScannerRegistrar` register the `MapperFactoryBean` definitions while the *primary configuration class* is being processed. That satisfies MyBatis-Plus's `@ConditionalOnMissingBean(MapperFactoryBean)` condition, so MyBatis-Plus's own auto mapper scanning backs off — the standard Spring Boot + MyBatis pattern, with no placeholder `MapperScannerConfigurer` needed.
3. **`lazyInitialization = "true"`** defers mapper bean instantiation until first use, which keeps the (very large) single application context's startup cost bounded.

The scan root is `cn.shopex.ecshopx` for components and `cn.shopex.ecshopx.**.mapper` for mappers, so any new Bundle rooted at its own `cn.shopex.ecshopx.<domain>` package is picked up automatically; new mappers should simply live under a `mapper` package. Mapper XML files are located through `mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml`, which is why every Bundle's `resources/mapper/` XML files are found without per-Bundle configuration.

## Runtime topology

```mermaid
flowchart TD
    subgraph app["ecshopx-app container (single Spring context)"]
        APP["EcshopxApplication on Undertow :18080"]
        BUNDLES["49 business Bundles"]
        CORE["ecshopx-common + ecshopx-dispatch"]
        EXE["XxlJobSpringExecutor :19999"]
        DRIVERS["DispatchCore with Sync and Redis drivers"]
        REG["InMemoryDispatchRegistry"]
    end

    subgraph redis["Redis (multi logical DB)"]
        DB0["db0 default + companys + datacube + wechat + espier + members + dispatch queues"]
        DB1["db1 deposit day recharge totals"]
        DB2["db2 prism"]
    end

    subgraph mysql["MySQL 8 (utf8mb4, Asia/Shanghai)"]
        BIZDB[("ecshopx business schema")]
        XXLDB[("xxl_job scheduler schema")]
        FLY[("flyway_schema_history")]
    end

    subgraph xxl["XXL-JOB admin (separate deployment)"]
        ADMIN["xxl-job-admin admin console"]
    end

    CLIENT["Browsers, mini-programs, H5, OpenAPI consumers"] -- "/api, /storage, /wechatAuth" --> APP
    ADMIN -- "trigger + auto-register" --> EXE
    EXE -- "callbacks" --> ADMIN
    APP -- "MyBatis-Plus mappers" --> BIZDB
    ADMIN -- "scheduler state" --> XXLDB
    APP -- "Flyway migrate at startup" --> FLY
    APP -- "Lettuce factories" --> DB0
    APP -- "Lettuce factories" --> DB1
    APP -- "Lettuce factories" --> DB2
    APP -- "enqueue dispatch:default, dispatch:slow, dispatch:sms, dispatch:delayed zset" --> DB0
    CORE --> DRIVERS
    DRIVERS --> REG
    BUNDLES --> CORE
    APP --> BUNDLES

    PAYGW["Payment and WeChat gateways"] -- "async notify callbacks" --> APP
```

Runtime topology of ECShopX-Java: one Spring application inside the `ecshopx-app` container, backed by MySQL (business + `xxl_job` schemas, Flyway history), a single Redis instance partitioned into logical databases, and a separately deployed XXL-JOB admin console; external payment and WeChat gateways push asynchronous notifications into the `/wechatAuth` URL space.

### The HTTP surface: Undertow on 18080 and URL-space routing

```mermaid
flowchart LR
    subgraph edge["Container edge"]
        NGX["OpenResty/nginx :80 (Docker)"]
    end

    subgraph app["Undertow :18080 (single Spring context)"]
        direction TB
        subgraph apiSpace["/api/**"]
            ADMIN["/api/v1/… operator/admin controllers (JWT)"]
            H5["/api/v1/h5app/… buyer H5/mini-program controllers (JWT)"]
            POSA["/api/v1/pos/auth/** POS login/logout/me (POS JWT)"]
            OAPI["/api/openapi/** third-party gateway"]
        end
        STORE["/storage/** local static files"]
        WXA["/wechatAuth/** unauthenticated gateway callbacks"]
        FIREWALL["StorageAwareHttpFirewall (strict, lenient under /storage)"]
    end

    EXT["Browsers, mini-programs, H5"] --> NGX --> FIREWALL
    GW["Payment / WeChat gateways"] --> NGX
    FIREWALL --> ADMIN
    FIREWALL --> H5
    FIREWALL --> POSA
    FIREWALL --> OAPI
    FIREWALL --> STORE
    FIREWALL --> WXA
```

The concrete mappings:

`application.properties` pins `server.port=18080` and selects **Undertow** as the embedded server (the bootstrap POM excludes `spring-boot-starter-tomcat` and adds `spring-boot-starter-undertow`; `server.tomcat.*` is therefore ignored). Three Undertow behaviors exist specifically for PHP-era URL and upload compatibility:

* `server.undertow.options.server.allow-unescaped-characters-in-url=true` — allows raw UTF-8 and other unescaped bytes in the query string, matching `io.undertow.UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL` so legacy PHP URLs keep working.
* `server.undertow.max-http-post-size=64MB` — a total POST body cap kept above `spring.servlet.multipart.*` (50 MB file / 55 MB request) so large multipart bodies reach Spring validation.
* `UndertowMultipartMaxEntityCustomizer` — Undertow 2.3+ enforces its own multipart entity cap near 2 MB; this customizer raises `MULTIPART_MAX_ENTITY_SIZE` to 64 MB and also enables `ALLOW_ENCODED_SLASH` so encoded `%2F` reaches the servlet mapping layer.

Two related web-infrastructure decisions live in bootstrap config and `ecshopx-common`:

* `StrictHttpFirewallConfig` installs a `StorageAwareHttpFirewall` — Spring Security's `StrictHttpFirewall` (with encoded-slash, double-slash, and semicolon relaxations) for every path, except `/storage/**`, which is served by the lenient `DefaultHttpFirewall` so local-disk image URLs containing Chinese or mojibake filenames are not rejected by the printable-ASCII URI check (`StorageUnicodeHttpFirewallTest` proves both sides: `/storage/…微信图片….jpg` passes, `/api/v1/espier/微信` is still rejected).
* `FlexibleBodyWebMvcConfigurer` (in `ecshopx-common`) registers the `FlexibleBodyMethodArgumentResolver` that merges query-string parameters and a flexible JSON body into one `Map<String, Object>` for controllers, and keeps semicolon matrix content in the path (`UrlPathHelper.setRemoveSemicolonContent(false)`). `spring.mvc.servlet.pathmatch.use-trailing-slash-match=true` additionally matches collection URLs with or without a trailing slash — both PHP-compatibility measures.

The three externally meaningful URL spaces are:

| URL space | Served by | Purpose |
|---|---|---|
| `/api/…` | Business-Bundle controllers (`api/admin/v1`, `api/front/v1`), `/api/openapi/…` gateway from `ecshopx-openapi` | All business HTTP. Admin routes live under `/api/v1/…`, buyer (H5 / Mini Program) routes under `/api/v1/h5app/…`. |
| `/storage/**` | `StorageWebMvcConfigurer` in `ecshopx-espier` (active only when `ecshopx.storage.driver=local`) | Maps the local storage root (`ecshopx.storage.local.root`, default `storage/app/public`) to a static resource handler so uploaded files are directly downloadable. With `driver=oss/qiniu/aws/cosv5` files are served by the cloud bucket instead. |
| `/wechatAuth/…` | Unauthenticated callback controllers in `api/open` packages (`payment/api/open/WxpayNotifyController` at `/wechatAuth/wxpay/notify`, `wechat/api/open/WechatOpenPlatformMessageCallbackController` at `/wechatAuth/callback/{authorizerAppId}`, `wechat/api/open/WxappLegacyQrcodeController` at `/wechatAuth/wxapp/qrcode.png`) | WeChat Pay async notify, WeChat Open Platform message callbacks, and legacy PHP URL-compat routes. These deliberately bypass the admin/front auth interceptors. |

In Docker deployments an OpenResty/nginx front (port 80) routes `/api/`, `/storage/`, and `/wechatAuth/` to the Java process on `127.0.0.1:18080` inside the same container, keeping the URL spaces identical to the PHP deployment.

The `/api/openapi/**` gateway adds a security layer distinct from both the operator/H5 JWT families and the `/wechatAuth` callbacks. `OpenapiWebMvcConfigurer` (in `ecshopx-openapi`) registers `OpenapiAuthInterceptor` on `/api/openapi/**` and `/api/openapi/internal/**`; the interceptor skips the internal prefix outright, and for public paths it requires `version`, `timestamp`, and `app_key`, enforces a ±600 s timestamp tolerance, looks up developer credentials through the `OpenapiDeveloperLookupPort` port in `ecshopx-common`, and verifies the legacy sign via `OpenapiSignSupport`. Since ECX-10869 it then calls `OpenapiSignedRequestReplayGuard.assertOnce(appKey, sign, timestamp, ttl)`, which `SETNX`s `openapi:auth:replay:{sha256(app_key|sign|timestamp)}` on the default `StringRedisTemplate` with a TTL equal to the 600 s tolerance — the first sighting of a signed request passes, a replayed `app_key + sign + timestamp` within the window is rejected as `请求已失效或重复`. Setting `openapi.debug=1` bypasses signature and replay checks and authenticates as the system company — a development switch only.

### POS terminal authentication: a fourth token family

Alongside the admin/operator and H5 JWT families, POS cash-register terminals get their own auth surface, added for the ECX-10532 POS login flow. The HTTP entry is `PosAuthController` (`ecshopx-companys`, `api/pos/v1`, base path `/api/v1/pos/auth`) with three endpoints: `POST /login` (employee number + password → POS JWT), `POST /logout` (blacklists the presented token), and `GET /me` (role/permission snapshot from the request attribute the filter installed).

* **Issuing.** `PosAuthService` (companys `service/posauth`) looks up the `pos_store_account` row by employee number, decrypts the stored password cipher through `SensitiveFieldEncryptor`, and rejects disabled/resigned accounts and accounts whose shop no longer allows login; on success it builds claims and delegates to the **`PosJwtIssuerPort`** interface in `ecshopx-common/auth`. The implementation, `PosJwtIssuer` in `ecshopx-espier/security`, signs an HS512 compact JWT keyed from `JWT_SECRET` with `token_kind=pos`, issuer `POS_JWT_ISSUER` (default `ecshopx-pos`), a random `jti`, and TTL from `JWT_TTL` minutes; `invalidate()` (logout) stores the token hash in Redis until its `exp` via `PosJwtBlacklistService` (keys `pos_jwt_invalidate:<sha256>` on the **prism** Redis template, DB 2 by default).
* **Enforcement.** `PosJwtAuthenticationFilter` (espier `security`, `@Order(HIGHEST_PRECEDENCE + 40)`) is a `OncePerRequestFilter` that *only* engages when the `Authorization: Bearer` token looks like a POS token (`token_kind=pos` or the POS issuer) — every other request, including admin and H5 JWTs, passes through untouched, and `POST /api/v1/pos/auth/login` is always skipped. For POS tokens it enforces `PosJwtPathWhitelist` (auth endpoints, `/api/v1/order*`, `/api/v1/aftersales`, `/api/v1/checkout`, operator cart paths, goods read, members/tag read, logistics read, delivery/write-off POSTs, and an explicit deny on `/api/v1/store/pos-accounts`), rejects blacklisted or expired tokens, then calls **`PosJwtSessionValidatorPort`**. The implementation, `PosJwtSessionValidator` in companys `service/posauth`, re-reads the `pos_store_account` row and invalidates the token immediately when the account was disabled, resigned, reassigned to another store, or re-roled (spec rule G-L9). On success the claims map is attached to the request as `POS_JWT_USER_DATA`, which the controller's `/me` and `/logout` handlers read.
* **Account provisioning → DigiOS.** When a POS store account is created, companys publishes through `PosStoreAccountCreateEventDispatchPublisher` (another `ecshopx-common` port); bootstrap's `PosStoreAccountCreateEventDispatchPublisherImpl` emits `event:companys:pos_store_account_create` (`CompanysDispatchEventNames.EVENT_POS_STORE_ACCOUNT_CREATE`) so downstream listeners can sync the account to DigiOS POS. This follows the same pattern as the existing `DigiOs*DispatchListenerRegistrationConfig` classes in bootstrap config, which register the `ecshopx-digios` sync listeners (operator create, distributor create/update, and the delivery/aftersales store-adjustment listeners) against their respective `DistributionDispatchEventNames`/`CompanysDispatchEventNames` events on the `default` queue.

### MySQL persistence

A single MySQL database `ecshopx` (JDBC URL pinned to `utf8mb4` and `serverTimezone=Asia/Shanghai`) is accessed through **MyBatis-Plus 3.5.16** with `map-underscore-to-camel-case` enabled. All persistence is mapper-based: `@Mapper` interfaces extending `BaseMapper<T>` per Bundle, with XML in each Bundle's `resources/mapper/`, aggregated by `mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml`.

Schema evolution is **Flyway**, enabled at startup (`spring.flyway.enabled=true`, `locations=classpath:db/migration`, `baseline-on-migrate=true`, `baseline-version=0`). The baseline behavior matters operationally: existing databases imported from `docker/ecshopx.sql` (the legacy business schema + demo data) are baselined at version 0 rather than replaying history. Migration files live only in `ecshopx-bootstrap/src/main/resources/db/migration` (currently `V00000000000001.sql` through `V00000000000007.sql`); `bin/make-migration` generates new files in `VyyyyMMddHHmmss__description.sql` format by diffing the live schema against MyBatis-Plus `@TableName`/`@TableId`/`@TableField` annotations (see the repository readme for the caveats — generated SQL must be reviewed for types, defaults, NULLability, and indexes; DROPs are only emitted with `--allow-drop`).

The current schema head is **`V00000000000007.sql`** (release 5.5.0): it adds merchant-settlement onboarding fields — `contact_name`, `main_category_selection` (JSON), `license_registered_address`, and `license_legal_representative` on `merchant`, plus those plus `user_id` on `merchant_settlement_apply`, which also gains a `uk_company_user (company_id, user_id)` unique key so each member can file at most one settlement application per company. The previous head, `V00000000000006.sql` (release 5.4.7, member-tag governance plus member-tag promotion targeting), created `members_rel_tags_audit` (an attach/detach audit trail with `source`, `rule_version`, `reason`, `event_id`, indexed on `company_id,user_id,tag_id`), added `source` / `rule_version` / `effective_at` / `updated_at` to `members_rel_tags`, added `tag_code` / `tag_kind` (`manual`|`system`) / `rule_version` plus a `uk_company_tag_code` unique key to `members_tags`, and added `member_tag_ids` (JSON) / `member_tag_mode` (`include`|`exclude`) to both `promotions_marketing_activity` and `promotions_seckill_activity`.

After Flyway completes, `MigrationSeedRunner` (an `ApplicationRunner` in bootstrap) aligns seed data with the PHP `EspierBundle\Listeners\UpdateMenuListener`: it imports logistics companies only when the `logistics` table is empty (failure logs but does not block startup), and — when `common.use-system-menu=true` (the default in `application.properties`) — atomically deletes all `company_id=0` system menus (and their menu-type relations), then re-imports six role-based JSON seed files under `classpath:seed/menu/` — `platform_menu.json`, `it_menu.json`, `shop_menu.json`, `dealer_menu.json`, `merchant_menu.json`, and `supplier_menu.json` — in a fixed order, inside a programmatic `TransactionTemplate` transaction (a proxied `@Transactional` would be bypassed by the self-call, and half-filled menus must roll back together).

Each seed file is a JSON array of menu rows whose `version` groups a menu generation, `menu_type` names the menu types the row belongs to, and `name_lang` carries per-locale names (currently `zh-CN`, `en-CN`, `ar-SA`, `zh-TW`, `ja-JP`, `ru-RU`). `ShopMenuUploadService.uploadMenus` (in `ecshopx-shopmenu-border`, `@Transactional(rollbackFor = Exception.class)` per file) first deletes existing `company_id=0` rows matching the incoming versions — including their `shop_menu_rel_type` rows and `name_lang` translations written through companys' `CommonLangModWriteService` — then re-inserts rows with fresh sequential `shopmenu_id`s, remaps each file-local `pid` to the new parent id, rebuilds the menu-type relations, and re-writes the translations; rows missing `alias_name` are rejected.

The current `platform_menu.json` head row (`shopmenu_id` 423, `概况`/`index`, `menu_type: ["all"]`, version 1) binds the operator-facing store-cashier APIs into the platform overview menu: its `apis` list ends with `companys.operator.member.ready` and `companys.operator.cartfastbuy.add`, and `companys.operator.member.ready` additionally appears on the 会员列表 row (`shopmenu_id` 453). The same seed file now also ends with a new operation-log row: `shopmenu_id` 681 (`操作日志`, alias `operator-logs`, version 1, level 3 under 系统信息 `shopmenu_id` 680 ← 设置, `menu_type: ["all"]`, url `/setting/systeminfo/operatorlogs`) whose `apis` is exactly `company.get.operatorlogs`, granting the platform operation-log listing on the system-information menu. That alias is served by `OperatorsController.getCompanysLogs` in `ecshopx-companys` (`@Activated(routeAlias = "company.get.operatorlogs")`, `GET /api/v1/company/operatorlogs`), which pages through `operatorLogsQueryService.getCompanysLogs(companyId, jwtMerchantId, operatorType, page, pageSize, listFilterHasNonemptyMerchantId)` — merchant operators are automatically scoped to their own `merchant_id`, and pagination defaults to page 1 / 20 rows. The two store-cashier endpoints behind the head-row aliases:

* **`POST /api/v1/operator/member/ready`** — `OperatorStoreMemberController.postReady` in `ecshopx-companys` (`@Activated(routeAlias = "companys.operator.member.ready")`, 门店会员就绪（开单前）): before the store cashier pulls a cart, it delegates to Shuyun's `OperatorStoreMemberReadinessService.ensureOfflineMemberAtStore(companyId, distributorId, userId)`, which skips cleanly when the company's Shuyun open-platform config is disabled or ineligible, otherwise requires the member to exist under the same company with a mobile number and registers them offline with the open platform (`member.register`) when not already synced — failures surface as `ResourceException` so the cashier flow stops before cart pull.
* **`POST /api/v1/operator/cartfastbuyadd`** — `OperatorCartController.cartFastBuyAdd` (店务立即购买（云仓分桶）): the companion fast-buy endpoint. Note the drift in the seed: the Java controller is still annotated `@Activated(routeAlias = "companys.operator.cartdata.fastbuyadd")`, while the menu seed row was updated to the newer alias `companys.operator.cartfastbuy.add` — a mismatch to be aware of when auditing `@Activated` route-alias enforcement against seeded menu permissions. The handler goes through the `OperatorCartFastBuyFacade` port in `ecshopx-common`, implemented by `OperatorCartFastBuyFacadeImpl` in `ecshopx-goods`, which resolves the SKU, gates the purchase through distribution's platform-store buy rules, validates shop/platform stock via `OperatorFastBuyStockValidator`, and stores the fast-buy row in Redis through companys' `OperatorShopFastBuyRedisService`.

### Redis: one instance, many logical databases

The application deliberately bypasses Spring Boot's Redis auto-configuration and builds its own Lettuce `RedisConnectionFactory` beans in `MultiRedisConfig`. Each factory gets TCP keepalive, auto-reconnect, configurable command/connect timeouts (`ecshopx.redis.lettuce.*`), and `validateConnection` — the Javadoc notes this exists because Docker/idle `Connection reset` must be handled here rather than by Boot's auto-config. The named factories and their logical DB selectors:

| Bean | DB selector | Default | Used for |
|---|---|---|---|
| `springDataRedisConnectionFactory` / `sharedStringRedisTemplate` / `dispatchBusStringRedisTemplate` | `spring.data.redis.database` | 0 | Platform-default Redis access and the dispatch bus queues |
| `companysRedisConnectionFactory` / `companysRedisTemplate` (`@Primary` StringRedisTemplate) | `ecshopx.redis.companys.database` | 0 | Company/shop settings |
| `prismRedisConnectionFactory` / `prismRedisTemplate` | `ecshopx.redis.prism.database` | 2 | Prism gateway integration state |
| `datacubeRedisConnectionFactory` / `datacubeStringRedisTemplate` | `ecshopx.redis.datacube.database` | 0 | Datacube statistics |
| `depositRedisConnectionFactory` / `depositStringRedisTemplate` | `ecshopx.redis.deposit.database` | 1 | Deposit day recharge totals (e.g. `dayRechargeTotal{yyyy-MM-dd}`) |
| `membersRedisConnectionFactory` / `membersStringRedisTemplate` | `ecshopx.redis.members.database` | falls back to `spring.data.redis.database` | Member-scoped keys (sync throttling) |
| `espierRedisConnectionFactory` / `espierStringRedisTemplate` | `ecshopx.redis.espier.database` | falls back to `spring.data.redis.database` | Espier platform settings |
| `wechatRedisConnectionFactory` / `wechatStringRedisTemplate` | `ecshopx.redis.wechat.database` | falls back to `spring.data.redis.database` | WeChat access tokens / settings |

The deposit/members/espier/wechat factories additionally accept dedicated `ecshopx.redis.<name>.host|port|password` overrides, so a single logical Redis can be split per business area without running multiple servers. `MultiRedisConfigLettuceFactoryTest` pins the keepalive/validate/autoReconnect behavior.

### XXL-JOB: admin + executor

All scheduled work goes through **XXL-JOB 2.5.0**. The admin console (`xuxueli/xxl-job-admin:2.5.0`, its `xxl_job` schema initialized from `docker/tables_xxl_job.sql`) is deployed as a separate container/service and is *not* part of the Spring application. The application side is one bean: `XxlJobConfig` in bootstrap creates an `XxlJobSpringExecutor` configured from `xxl.job.*` properties — `admin.addresses` (repo default `http://127.0.0.1:9080/xxl-job-admin`; both Docker Compose stacks override it to `http://xxl-job-admin:8080/xxl-job-admin` on the container network, with host port 8080 published), `accessToken`, `executor.appname=ecshopx-executor`, `executor.port=19999`, log path `logs/xxl-job`, and 7-day log retention. The executor auto-registers with the admin and receives trigger callbacks on port 19999.

Business handlers are `@XxlJob("<short-name>")` methods in each Bundle's `cron/` package (e.g. `orders/cron/ScheduleFinishOrdersHandler` registered under handler name `finish-orders`). The uniform handler shape is: invoke the cron service, log cost, and on exception publish a `CronAlertEvent` (a Spring `ApplicationEvent` from `common/cron/event`) for alerting, then rethrow so XXL-JOB records the failure. There is no Quartz, no Spring `@Scheduled` cron for business jobs — only the small set of `@EnableScheduling` configs (orders, payment, selfservice, shuyun) that drive infrastructure polls such as the dispatch-queue pollers.

### The dispatch bus: Redis-backed queues

Cross-Bundle asynchronous work (the PHP system's Laravel-style jobs and events) is carried by the dispatch bus in `ecshopx-dispatch`, wired in bootstrap's `DispatchBusConfig`:

* **Publishing.** `DispatchFacade.dispatchJob(...)` / `publishEvent(...)` build a `DispatchMessage` (messageType JOB or EVENT, mode SYNC/ASYNC, driverType, messageName, payload, queue, delay, retryPolicy, occurredAt, traceId, listenerName) and hand it to `DispatchCore`. SYNC messages execute immediately through `SyncDispatchDriver` (in-process registry lookup); ASYNC messages are routed to the driver registered for their type. Message names intentionally preserve the legacy PHP class names — e.g. `OrdersDispatchJobNames.FINISH_ORDER_JOB = "job:128:OrdersBundle\\Jobs\\FinishOrderJob"` — so migration parity is auditable.
* **Registration.** `InMemoryDispatchRegistry` maps message name → job handler, and message name → ordered list of `(listenerName, options, DispatchListener)`. Duplicate job handlers or duplicate listener names throw at registration time. Bootstrap contains ~200 `*RegistrationConfig` classes whose `@PostConstruct` hooks register each Bundle's handlers/listeners — this is the explicit wiring that lets Bundles stay Maven-decoupled.
* **Drivers.** `DispatchDriverType` has three values — `SYNC`, `REDIS`, `RABBITMQ`. `DispatchCore.asyncReady` is built in `DispatchBusConfig` with only the Redis driver mapped; the RabbitMQ driver and listener classes exist in `ecshopx-dispatch` (with a `RabbitDispatchTopologyProvisioner` that declares a direct exchange, durable queues, and bindings per routing key, and `@RabbitListener(queues = "${dispatch.rabbit.queue:dispatch}")`) but are **not registered** in the default configuration, so Redis is the only active async transport.
* **Redis transport.** `RedisDispatchDriver` (bound to the `dispatchBusStringRedisTemplate`, i.e. the default Redis DB) encodes each message as JSON via `RedisDispatchMessageCodec` and `rightPush`es it onto `dispatch:<queue>`; blank queue names become `default`; a dead-letter list `dispatch:dead:<queue>` receives terminal failures. Messages with a delay land in the `dispatch:delayed` ZSET scored by ready-time, and `RedisDispatchDelayedPollScheduler` (`@Scheduled` every 1 s, disable via `dispatch.redis.delayed-mover.enabled=false`) moves due entries back onto their ready queue through `RedisDelayedDispatchMover`.
* **Consumption.** `RedisDispatchListener` polls (default every 1 s, `dispatch.redis.poll-delay-ms`) and pops one message from `dispatch:default`, then up to `dispatch.redis.slow.batch-size` (default 20) each from `dispatch:slow` and `dispatch:sms`, feeding each to `DispatchConsumerRuntime`. The **queue names are load-class signals**: export/statistics/finish-order jobs go to `slow` (e.g. `FinishOrderJobDispatchPublisherImpl` publishes `FINISH_ORDER_JOB` with `DispatchMode.ASYNC`, `DispatchDriverType.REDIS`, queue `slow`), SMS/notification work to `sms`, everything else to `default`.
* **Failure semantics.** `DispatchConsumerRuntime.consume` resolves the handler (jobs) or the matching listener by `listenerName` (events); on success it records an ack. On a `RuntimeException`, if `currentAttempt < retryPolicy.maxAttempts()` it records a retry and rethrows (the driver re-enqueues); otherwise it logs a structured line, records the failure through `FailedJobRecorder` into the `failed_jobs` table (via `FailedJobMapper`'s XML insert), and records the fail. `RetryPolicy.platformDefault()` is 3 attempts / 3 s next delay. Note `FailedJobMapper` has no MyBatis-Plus `BaseMapper` backing and the `failed_jobs` table comes from the legacy SQL dump, so bootstrap currently installs a stub `FailedJobMapper` bean returning 1 — persistence of failed jobs is a wiring point to be completed rather than production-grade today.

See [Dispatch Bus](../concepts/dispatch-bus.md) for the message-flow details.

### Multi-Redis and dispatch interplay

The dispatch bus uses the *shared default* Redis logical DB (`dispatchBusStringRedisTemplate` ← `springDataRedisConnectionFactory`), not a dedicated one — dispatch queue keys (`dispatch:*`) therefore share DB 0 with platform-default keys by design, while deposit (DB 1) and prism (DB 2) remain isolated. This matters when planning key expiry or FLUSHDB operations per logical database.

## The Bundle catalog

Every business Bundle declares its domain in a Chinese `package-info.java` at its package root. The complete catalog (grouped by domain family; Chinese domain text from each `package-info.java`):

**Core commerce**
| Bundle | Domain (from package-info.java) |
|---|---|
| `ecshopx-goods` | 商品域模块 |
| `ecshopx-orders` | 订单、购物车、物流与结算域 |
| `ecshopx-aftersales` | 售后域模块 |
| `ecshopx-members` | 会员与账户域 |
| `ecshopx-comments` | 评论域模块 |
| `ecshopx-deposit` | 储值域模块 |
| `ecshopx-supplier` | 供应商域 |
| `ecshopx-cross-border` | 跨境电商域模块 |

**Marketing and engagement**
| Bundle | Domain |
|---|---|
| `ecshopx-promotions` | 营销与促销活动域 |
| `ecshopx-kaquan` | 卡券域模块 |
| `ecshopx-point` | 积分域模块 |
| `ecshopx-pointsmall` | 积分商城域模块 |
| `ecshopx-distribution` | 分销与推广域 |
| `ecshopx-popularize` | 分销推广域模块 |
| `ecshopx-employee-purchase` | 员工内购域模块 |
| `ecshopx-community` | 社区与互动域 |
| `ecshopx-wsugc` | 用户生成内容域 |
| `ecshopx-form` | 表单与成绩单域模块 |
| `ecshopx-reservation` | 预约域模块 |
| `ecshopx-one-code` | 一物一码域模块 |
| `ecshopx-shopex-ai` | Shopex AI 域模块 |

**Stores, merchants and operations**
| Bundle | Domain |
|---|---|
| `ecshopx-merchant` | 商户入驻与设置域 |
| `ecshopx-companys` | 企业与多门店域 |
| `ecshopx-salesperson` | 业务员/导购域模块 |
| `ecshopx-selfservice` | 自助服务域模块 |
| `ecshopx-super-admin` | 平台级公告等能力域 |
| `ecshopx-shopmenu-border` | 店铺菜单域模块 |
| `ecshopx-im` | 即时通讯域模块 |

**Payments**
| Bundle | Domain |
|---|---|
| `ecshopx-payment` | 支付配置与通道域 |
| `ecshopx-adapay` | AdaPay 域模块 |
| `ecshopx-hfpay` | 汇付支付域模块 |
| `ecshopx-bspay` | 汇付斗拱支付域模块 |
| `ecshopx-chinaums-pay` | 银联商务支付域模块 |

**WeChat ecosystem**
| Bundle | Domain |
|---|---|
| `ecshopx-wechat` | 微信与小程序对接域 |
| `ecshopx-work-wechat` | 企业微信对接域 |

**Integrations and platform services**
| Bundle | Domain |
|---|---|
| `ecshopx-ali` | 阿里域模块 |
| `ecshopx-aliyunsms` | 阿里云短信域模块 |
| `ecshopx-shuyun` | 数云对接域（遗留 UAPI + 开放平台 open-api） |
| `ecshopx-system-link` | 系统联通域模块 |
| `ecshopx-youshu` | 有数分析配置域 |
| `ecshopx-kujiale` | 酷家乐域模块 |
| `ecshopx-tb-items` | 外部商品同步等业务域 |
| `ecshopx-third-party` | 第三方对接域模块 |
| `ecshopx-digios` | DigiOS POS direct integration (declared in its POM description) |
| `ecshopx-openapi` | OpenAPI 设置域模块 |
| `ecshopx-espier` | 平台通用能力（上传、地址、升级等）域 |
| `ecshopx-theme` | 装修、模板与页面配置域 |
| `ecshopx-tdkset` | TDK 设置域模块 |
| `ecshopx-data-cube` | 数据魔方与统计分析域 |

Three of these Bundles carry the newest cross-system surface and are worth locating on the map:

* **`ecshopx-digios`** — DigiOS POS direct integration (店铺/账号/库存同步): it consumes companys/distribution/orders events through the bootstrap `DigiOs*DispatchListenerRegistrationConfig` wiring, and it *publishes* store adjustments of its own — bootstrap's `DigiOsAftersalesReturnAdjustmentDispatchPublisherImpl` plus the `DigiOsAftersalesReturnAdjustmentDispatchListenerRegistrationConfig` / `DigiOsNormalOrderDeliveryAdjustmentDispatchListenerRegistrationConfig` registrations let DigiOS originate aftersales-return and order-delivery stock adjustments onto the bus.
* **`ecshopx-openapi`** — the `/api/openapi/**` public gateway for third-party developers, including the Redis replay guard described above.
* **`ecshopx-shuyun`** — 数云 (Shuyun) CRM integration in two flavors: the legacy UAPI and the open-platform `open-api`; bootstrap subscribes ~12 `ShuyunOpenPlatform*DispatchListenerRegistrationConfig` listeners (member create, distributor create/update, order pay/delivery/confirm/cancel, refund finish, offline benefit consume, item-store product sync) to their bus events.

Adding a Bundle means: create the module rooted at `cn.shopex.ecshopx.<domain>` with a `package-info.java` stating the domain (the newest Bundle, `ecshopx-digios`, is the current exception — it declares its domain only in its POM description), add it to the parent `<modules>` and `dependencyManagement`, add the dependency to `ecshopx-bootstrap`, and (for async work) a bootstrap `*RegistrationConfig` plus the `*DispatchPublisher` interface in `ecshopx-common`.

## Where bootstrap breaks the rules — and why

`ecshopx-bootstrap` is where the cross-Bundle edges are wired precisely because it depends on everything. Its `config/` package contains, alongside `DispatchBusConfig`/`MultiRedisConfig`/`XxlJobConfig`:

* ~160 `*DispatchPublisherImpl` components that implement `ecshopx-common` publisher interfaces by calling `DispatchFacade` with the correct mode/driver/queue/delay/retry tuple (e.g. `FinishOrderJobDispatchPublisherImpl` → ASYNC/REDIS/`slow`; `AliTemplateMsgSendDispatchPublisherImpl` resolves a per-company send delay from Redis before publishing to `default`; `PosStoreAccountCreateEventDispatchPublisherImpl` emits `event:companys:pos_store_account_create` when a POS store account is created).
* ~200 `*RegistrationConfig` classes that call `dispatchRegistry.registerJob(...)` / `registerEventListener(...)` in `@PostConstruct`, wiring producers to consumers without either side naming the other. This includes the `DigiOs*DispatchListenerRegistrationConfig` classes that subscribe the `ecshopx-digios` sync listeners (operator create, distributor create/update, and the delivery/aftersales store-adjustment listeners) to their distribution/companys events on the `default` queue, and the ~12 `ShuyunOpenPlatform*DispatchListenerRegistrationConfig` classes doing the same for the Shuyun open-platform sync listeners.
* A handful of `bootstrap/<domain>/` adapters (`WxappCommonSettingFacadeImpl`, `HfpayPopularizeCashWithdrawalMerchantPaymentService`, `OmeDistributorShopStructPortImpl`, …) that implement one Bundle's Port by composing several other Bundles' beans — the last resort when even Port inversion cannot break a cycle.
* `AsyncExecutorConfig` (`@EnableAsync`) with ~15 named `ThreadPoolTaskExecutor` beans for per-concern `@Async` work (OME sync, jushuitan upload, exports, `dispatchBusInternalExecutor`).

## Deployment shapes

Three supported paths share the same runtime topology (see [Deployment](../operations/deployment.md) and [Platform Services](../domains/platform-services.md) for details):

* **Full development** — `./dev-setup.sh` / `bash install.sh --full` → `docker-compose.dev.yml`: `mysql` (8.0, imports `docker/tables_xxl_job.sql` then `docker/ecshopx.sql`), `redis` (7-alpine, password `redispassword`, AOF), `xxl-job-admin` (2.5.0, `xxl_job` DB in the same MySQL), and `ecshopx-app` (built from `docker/Dockerfile.app`, a Maven builder stage plus a Temurin-17/Node/OpenResty runtime base). The app container exposes 80 (nginx, host-routed admin/H5/PC), 18080 (Java direct), and 19999 (executor); business parameters are injected via `APP_ARGS` in Spring Boot CLI style, preserving case-sensitive properties like `xxl.job.accessToken`. `dev-setup.sh` additionally clones and builds the three sibling frontends (`ECShopX-Java_Admin`, `ECShopX-Java_Mobile`, `ECShopX-Java_Web`).
* **Lite offline** — `docker-lite/deploy.sh` / `bash install.sh --lite` → `docker-lite/docker-compose.yml`: same four services but the `ecshopx-app` image is prebuilt and the release archive bind-mounts `docker-lite/app/ecshopx-bootstrap.jar` as `app.jar`.
* **Self-hosted jar** — build and run the executable jar (`./mvnw -pl ecshopx-bootstrap -am package`, then `java -jar … --spring.profiles.active=local`), overriding datasource/Redis/XXL properties; Flyway migrates on startup against the existing schema. Local overrides come from environment variables, CLI-style arguments (`--spring.datasource.url=…`, `--spring.config.additional-location=file:./config/`, as the readme's Method B shows), or an external config location — **not** from a shipped sample: `ecshopx-bootstrap/src/main/resources/application-local.properties` is git-ignored (`.gitignore` line 7) and absent from fresh checkouts, so the readme's claim that the project "already includes" it is drift.

Ports to keep free: `18080` (app HTTP), `19999` (XXL executor), `9080` or `8080` (XXL admin), `3306` (MySQL), `6379` (Redis).

## Focused tests that pin this topology

* `EcshopxApplicationTests` (`@SpringBootTest` contextLoads) — the smoke test that the assembled context starts.
* `DispatchBusConfigTest` — boots a stripped `AnnotationConfigApplicationContext` with `DispatchBusConfig` plus a mocked `dispatchBusStringRedisTemplate` and asserts the core beans (`DispatchRegistry`, `SyncDispatchDriver`, `DispatchCore`, `DispatchFacade`, `DispatchStructuredLogger`, `DispatchConsumerRuntime`, `FailedJobMapper`) exist.
* `DispatchBusIntegrationTest` — end-to-end dispatch semantics: a sync job executes its handler; `RedisDispatchListener.poll()` decodes a `dispatch:default` payload and feeds it to the runtime; `RabbitDispatchListener.onMessage` does the same for the AMQP path.
* `RedisDispatchListenerTest` / `RedisDispatchDriverTest` / `RedisDelayedDispatchMoverTest` / `DispatchConsumerRuntimeTest` — queue polling, JSON round-trip through `RedisDispatchMessageCodec`, delayed-message relocation, and the retry-vs-failed-jobs decision (`DispatchConsumerRuntimeTest.failedListenerPastMaxAttemptsWritesFailedJobs` proves a listener past `maxAttempts` triggers the structured `failed` log and `FailedJobRecorder.recordFailure`).
* `MultiRedisConfigLettuceFactoryTest` — asserts each Lettuce factory has `validateConnection`, TCP keepalive, and autoReconnect.
* `StorageUnicodeHttpFirewallTest` — `/storage/**` with Chinese/mojibake filenames passes the firewall while non-storage Chinese paths are rejected.
* `MigrationSeedRunnerTest` — seed/menu re-import behavior after startup.
* `OpenapiSignedRequestReplayGuardTest` / `OpenapiAuthInterceptorReplayTest` — the OpenAPI replay guard: first sighting of an `app_key + sign + timestamp` passes, the identical replay within the TTL window is rejected.
* The ~200 `*DispatchFlowTest` classes under `ecshopx-bootstrap/src/test` — per-message integration probes that publish through the real `DispatchFacade` and consume through `DispatchConsumerRuntime`, one per job/event (e.g. `FinishOrderJobDispatchFlowTest` verifies `FINISH_ORDER_JOB` lands on the `slow` queue and the consumer invokes `FinishOrderJobService`).
