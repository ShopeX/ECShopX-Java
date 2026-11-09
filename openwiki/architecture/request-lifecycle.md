---
type: architecture-convention
title: HTTP Request Lifecycle, Auth, and Response Conventions
description: What happens to an HTTP request in the single ecshopx-bootstrap Spring application — Undertow's PHP-compatibility settings and the StorageAwareHttpFirewall, the annotation-driven auth/middleware interceptor chain (FrontAuth/FrontNoAuth/AdminAuth/FrontMerchantAuth/QywxSalespersonAuth/Activated/DataPass/ShopLog), the POS dual-JWT chain (PosJwtAuthenticationFilter, PosJwtIssuer, PosJwtBlacklist, PosJwtPathWhitelist, DualJwtSupport), the merchant-wxapp JWT servlet filter, the openapi signed-request auth interceptor with Redis replay guard, FlexibleBody binding of JSON/form/multipart plus the merged-params controller idiom, locale resolution, and the ApiResult / @DingoResponse error-envelope conventions picked by GlobalExceptionHandler.
tags: [architecture, request-lifecycle, undertow, http-firewall, interceptors, auth, jwt, pos-auth, dual-jwt, merchant-wxapp-jwt, openapi-sign, replay-guard, flexiblebody, locale, apiresult, dingo-response, error-handling, cross-tenant-scope]
sources:
  - id: openwiki-source-84aeb105875424c9aac7edf8
    resource: repo://ecshopx-bootstrap/pom.xml
  - id: openwiki-source-7647c49446ef66219a2d0a86
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/StorageAwareHttpFirewall.java
  - id: openwiki-source-919f62d871898539e11d1107
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/StrictHttpFirewallConfig.java
  - id: openwiki-source-ee9e1dcff5d604ff92225a67
    resource: repo://ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/UndertowMultipartMaxEntityCustomizer.java
  - id: openwiki-source-89b43138f8e75edb4615495b
    resource: repo://ecshopx-bootstrap/src/main/resources/application.properties
  - id: openwiki-source-c31a57d8341de369d93baf28
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/aftersales/api/AftersalesReviewEndpointMappingTest.java
  - id: openwiki-source-987e8166f1c97b184480ef6c
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/companys/api/admin/v1/ShopexBindControllerWebSliceTest.java
  - id: openwiki-source-d270efd3600a297b7f0598cc
    resource: repo://ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/config/StorageUnicodeHttpFirewallTest.java
  - id: openwiki-source-d0d4216e2c7ccb8fb5412394
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/annotation/DingoResponse.java
  - id: openwiki-source-254d4979da642f362a9ae19c
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/annotation/OperationLog.java
  - id: openwiki-source-7166d5fa4542e949048c5056
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/aspectj/OperationLogAspect.java
  - id: openwiki-source-064af8f6d82116460315d01c
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/auth/DualJwtSupport.java
  - id: openwiki-source-18166d6db75da9333bacd41b
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/auth/H5JwtBlacklistPort.java
  - id: openwiki-source-5a81a9170be98d0125671cc1
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/auth/OperatorJwtRequestAttributes.java
  - id: openwiki-source-ed7b288eb2787a6530e341f5
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/auth/PosJwtRequestAttributes.java
  - id: openwiki-source-2305102efbd26dc34144f946
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/config/CommonWebLocaleAutoConfiguration.java
  - id: openwiki-source-eb0be55db75eca832d3c9492
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/config/FlexibleBodyWebMvcConfigurer.java
  - id: openwiki-source-030e0e53126d27f04037ffc3
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/config/LangueProperties.java
  - id: openwiki-source-4b5263faca517f81ac3be446
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/core/domain/ApiResult.java
  - id: openwiki-source-e5faeba7f67d50580a0bc502
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/exception/BadRequestException.java
  - id: openwiki-source-5ca885ed4fd631f49af8a1bb
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/exception/GlobalExceptionHandler.java
  - id: openwiki-source-3f17bf146b5e4b644b6a94e5
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/exception/ResourceException.java
  - id: openwiki-source-5b44911ef0644f226b41475d
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/openapi/OpenapiAuthAttributes.java
  - id: openwiki-source-a680619c2778d63c421a49fa
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/openapi/OpenapiSignSupport.java
  - id: openwiki-source-2247669a54256479e83875d3
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/FlexibleBody.java
  - id: openwiki-source-e7124ff1339f931e45499060
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/FlexibleBodyMethodArgumentResolver.java
  - id: openwiki-source-f5b72d94539a045244d65773
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/FlexibleHttpServletParameterMap.java
  - id: openwiki-source-bf5f5574673d482e1b9cbc2a
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/H5FrontAuthAttributes.java
  - id: openwiki-source-dac0deed98c6ab281df71156
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/locale/CountryCodeLocaleFilter.java
  - id: openwiki-source-aee67eb3ab23f76c9c8af9b4
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/locale/RequestMessageLocale.java
  - id: openwiki-source-81b81ba0fd38ecc3f4a2b036
    resource: repo://ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/WxappMemberAuthAttributes.java
  - id: openwiki-source-15019a68c41f802e71925535
    resource: repo://ecshopx-common/src/test/java/cn/shopex/ecshopx/common/auth/DualJwtSupportTest.java
  - id: openwiki-source-e8ae20ad9a0b9aab932192bb
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/api/pos/v1/PosAuthController.java
  - id: openwiki-source-407e259d6448c8084d7844f0
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/activation/ActivatedMiddlewareService.java
  - id: openwiki-source-3fe260ad41b93a3fc9fce4f7
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/activation/CompanysActivationService.java
  - id: openwiki-source-71fa09bfe6f954033da11ac4
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/datapass/ShopOperatorDatapassApplyService.java
  - id: openwiki-source-80ae9d88e25e6269f15157ab
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posauth/PosAuthService.java
  - id: openwiki-source-48f750a8142d8a7487ac4de7
    resource: repo://ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/posauth/PosJwtSessionValidator.java
  - id: openwiki-source-3b61905c32147bcd7759c9bf
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/EspierSecurityConfig.java
  - id: openwiki-source-fde2d214766049b8bb873842
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/H5JwtBlacklistService.java
  - id: openwiki-source-ecc705da626fec47a869310b
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/OperatorJwtAuthenticationFilter.java
  - id: openwiki-source-02e56d57e0b01f3c04ff1cb3
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/OperatorJwtBlacklistService.java
  - id: openwiki-source-d229dab5a02385013d416e5e
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/OperatorJwtCompactCodec.java
  - id: openwiki-source-a1f68480771cd92413f00be5
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtAuthenticationFilter.java
  - id: openwiki-source-043701b554e6049237c22c70
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtBlacklistService.java
  - id: openwiki-source-da7950ab19f4690ae361bd4b
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtIssuer.java
  - id: openwiki-source-a7b2be30f60d866cf32e516a
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/PosJwtPathWhitelist.java
  - id: openwiki-source-a8e8115b1b9151476c94a813
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/WxappMemberSessionAuthenticationFilter.java
  - id: openwiki-source-b56e4ca177932e823056dbc7
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/storage/StorageWebMvcConfigurer.java
  - id: openwiki-source-94af9a02b28a5d428a590c4e
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/ActivatedInterceptor.java
  - id: openwiki-source-b5679dcbfa12176984c7af97
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/AdminAuthInterceptor.java
  - id: openwiki-source-94cafc2ba615d2469b6e32e0
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/AdminLogAspect.java
  - id: openwiki-source-352261220335d160bfb65b9e
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/DataPassInterceptor.java
  - id: openwiki-source-6f3861e0f61d9552ada32544
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/EspierAgreementRateLimitInterceptor.java
  - id: openwiki-source-9fd6b26e404a7a61b8dbad21
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/FrontAuthInterceptor.java
  - id: openwiki-source-443dfcf543bf42ef5d465397
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/FrontNoAuthInterceptor.java
  - id: openwiki-source-b29e8d4b63cc7c5ad37a7e48
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/MiddlewareWebMvcConfigurer.java
  - id: openwiki-source-cb7b1b089af43117147fa9fd
    resource: repo://ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/ShopLogInterceptor.java
  - id: openwiki-source-80077d2f69a839e9693e1a07
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/api/front/v1/MembersController.java
  - id: openwiki-source-cc41dee55bf05e6db3079efb
    resource: repo://ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/h5/H5BearerJwtClaimsService.java
  - id: openwiki-source-2425b7a1af8631937f2e830a
    resource: repo://ecshopx-members/src/test/java/cn/shopex/ecshopx/members/api/front/v1/MembersControllerAddressBuyUserScopeTest.java
  - id: openwiki-source-4aa174a9de75c564b5ea5bdf
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/config/MerchantWxappJwtProperties.java
  - id: openwiki-source-7e78af38c8a63b7842f49ac8
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/config/security/FrontMerchantAuthInterceptor.java
  - id: openwiki-source-3faa728c05e4c966142451ad
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/config/security/MerchantInterceptorWebMvcConfigurer.java
  - id: openwiki-source-5535fc73ece87960d8767e8e
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/config/security/MerchantWxappJwtAuthenticationFilter.java
  - id: openwiki-source-77e486142ae903ca0a7f6211
    resource: repo://ecshopx-merchant/src/main/java/cn/shopex/ecshopx/merchant/service/wxapp/MerchantWxappAuthAttributes.java
  - id: openwiki-source-167baa4ee8f825cf1730b5a8
    resource: repo://ecshopx-openapi/src/main/java/cn/shopex/ecshopx/openapi/thirdapi/OpenapiRequestParamCollector.java
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
  - id: openwiki-source-bfe6d6f18351fc248cd7b204
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/RightsController.java
  - id: openwiki-source-07fb8468822e1637135de269
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/WxappOrderController.java
  - id: openwiki-source-f2e1c65d6d39cb3db1a8a7e8
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/api/front/v1/WxappPaymentController.java
  - id: openwiki-source-847db9a10639078a376af1af
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/config/OrdersFrontV1BadRequestFieldErrorsAdvice.java
  - id: openwiki-source-da30d999830db13c520004dd
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/front/wxapp/RightsFrontWxappGetRightsCodeService.java
  - id: openwiki-source-f08b4f8c06e4e935888e75cd
    resource: repo://ecshopx-orders/src/main/java/cn/shopex/ecshopx/orders/service/pos/PosOrderDocAccessGuard.java
  - id: openwiki-source-0e983b4fde485bf2f006c24e
    resource: repo://ecshopx-orders/src/test/java/cn/shopex/ecshopx/orders/service/front/wxapp/RightsFrontWxappGetRightsCodeOwnershipTest.java
  - id: openwiki-source-0ec099f633b299c7da3a55f9
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/config/SalespersonWebMvcConfigurer.java
  - id: openwiki-source-e85f4b516d751504331c6cfb
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/service/WxappSalespersonSessionAuthService.java
  - id: openwiki-source-10687baf99ef7873b3e5d2ff
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/web/QywxSalespersonAuthInterceptor.java
  - id: openwiki-source-37308fb144b4036f8384abe8
    resource: repo://ecshopx-salesperson/src/main/java/cn/shopex/ecshopx/salesperson/web/ShopSalespersonFrontRequestMerge.java
generated: { by: "openwiki/0.9.0", at: "2026-11-08T17:14:16.576Z" }
verified:
  - by: openwiki/0.9.0
    at: 2026-11-08T17:14:16.576Z
---

# HTTP Request Lifecycle, Auth, and Response Conventions

Every HTTP request enters one Spring application (`ecshopx-bootstrap` on Undertow, port 18080) and passes through a deliberately layered pipeline: servlet-container settings tuned for legacy PHP URL compatibility, a Spring Security firewall that carves out an exception for `/storage/**`, two thin security filter chains, a locale filter, the POS- and operator-JWT servlet filters plus the merchant-wxapp JWT filter, and only then the DispatcherServlet and the annotation-driven interceptor chain registered by `MiddlewareWebMvcConfigurer` (espier) plus its merchant/salesperson/openapi siblings. After the handler is chosen, `@FlexibleBody` binds JSON / form / multipart bodies, controllers merge query parameters with the body map, and both success (`ApiResult`) and failure (`@DingoResponse` legacy envelopes via `GlobalExceptionHandler`) responses follow conventions inherited from the PHP system.

This page documents each stage, the annotation-to-interceptor matrix, and the response/error conventions a **new endpoint must choose between**. For where controllers live per Bundle, see [Bundle Anatomy and Layering Conventions](bundle-layering.md); for the runtime topology, see [System Overview and Runtime Topology](overview.md).

## The pipeline at a glance

```mermaid
sequenceDiagram
    autonumber
    participant Client as H5 or Mini Program client
    participant FW as StorageAwareHttpFirewall
    participant SEC as Security chains
    participant LOC as CountryCodeLocaleFilter
    participant POSF as PosJwtAuthenticationFilter
    participant OPF as OperatorJwtAuthenticationFilter
    participant NOAUTH as FrontNoAuthInterceptor
    participant AUTH as FrontAuthInterceptor
    participant BIND as FlexibleBody resolver
    participant CTRL as Controller method
    participant GEX as GlobalExceptionHandler

    Client->>FW: POST /api/v1/h5app/wxapp/payment with optional Bearer JWT
    FW->>FW: strict rules for every non-storage path
    FW->>SEC: firewalled request reaches chain matching
    SEC->>LOC: permitAll chain lets the request through
    LOC->>LOC: country_code or Accept-Language sets the thread locale
    LOC->>POSF: request continues down the servlet filter chain
    POSF->>POSF: engages only when the Bearer token is a POS token
    POSF->>OPF: operator or front token passes through untouched
    OPF->>OPF: front h5app path is skipped by the operator JWT filter
    OPF->>NOAUTH: DispatcherServlet maps the handler then interceptors run
    NOAUTH->>NOAUTH: optional JWT check then anonymous company_id resolution
    alt no company_id can be resolved
        NOAUTH-->>Client: 401 with dingo or ApiResult envelope
    end
    NOAUTH->>AUTH: next interceptor in registration order
    opt handler method carries FrontAuth
        AUTH->>AUTH: strict H5 JWT verify with blacklist check
        alt JWT missing or invalid
            AUTH-->>Client: 401 with code 401001
        else account disabled claim
            AUTH-->>Client: 403 with code 403001
        end
    end
    AUTH->>BIND: h5AuthClaims and h5CompanyId attributes available
    BIND->>CTRL: binds JSON or form body to the method parameter
    CTRL->>CTRL: merges parameter map then body wins then calls service
    alt service throws ResourceException or BadRequestException
        CTRL->>GEX: exception propagates out of the handler
        GEX-->>Client: envelope chosen by DingoResponse or plain ApiResult
    else success
        CTRL-->>Client: 200 with ApiResult.ok data
    end
```

The request path for a front-end (H5 / Mini Program) endpoint: firewall → security chains → locale filter → POS-JWT filter (skips non-POS tokens) → operator-JWT filter (skipped for front paths and POS sessions) → interceptor chain → `FlexibleBody` binding → controller → `GlobalExceptionHandler` on error.

## Stage 1 — Undertow and the servlet-container edge

`ecshopx-bootstrap` runs on **Undertow** (the POM excludes `spring-boot-starter-tomcat` and adds `spring-boot-starter-undertow`, so `server.tomcat.*` is ignored). Five settings exist specifically so URLs and uploads that the legacy PHP system accepted keep working:

| Setting | Value | Why |
|---|---|---|
| `server.undertow.options.server.allow-unescaped-characters-in-url` | `true` | Allows raw UTF-8 and other unescaped bytes in the query string (maps to `io.undertow.UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL`); legacy PHP clients send unencoded Chinese query values. |
| `server.undertow.max-http-post-size` | `64MB` | Total POST body cap, deliberately above `spring.servlet.multipart.*` so large multipart bodies reach Spring and business validation instead of dying at the connector. |
| `spring.mvc.servlet.pathmatch.use-trailing-slash-match` | `true` | Collection URLs match with or without a trailing slash (`/api/v1/members` and `/api/v1/members/` hit the same handler), mirroring PHP route behavior. |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | `50MB` / `55MB` | Spring-side multipart limits (batch imports like restricted-goods xlsx; Boot defaults of 1MB/10MB were too small). |
| `UndertowMultipartMaxEntityCustomizer` | `MULTIPART_MAX_ENTITY_SIZE = 64MB`, `ALLOW_ENCODED_SLASH = true` | Undertow 2.3+ enforces its *own* multipart entity cap near 2MB, independent of Spring's settings; the customizer raises it so Spring's limits govern. `ALLOW_ENCODED_SLASH` lets encoded `%2F` reach the servlet mapping layer. |

A sixth PHP-compatibility measure lives in `FlexibleBodyWebMvcConfigurer` (`ecshopx-common`): a `UrlPathHelper` with `setRemoveSemicolonContent(false)`, so semicolon content in the path survives handler mapping.

### The firewall: `StorageAwareHttpFirewall`

`StrictHttpFirewallConfig` (bootstrap) exposes the firewall as a bean and installs it on Spring Security's `WebSecurity` through a `WebSecurityCustomizer`. The implementation is a path-aware composite:

* Every path **except** `/storage/**` (and the exact `/storage`) goes through `StrictHttpFirewall`, relaxed to `setAllowUrlEncodedSlash(true)`, `setAllowUrlEncodedDoubleSlash(true)`, and `setAllowSemicolon(true)` — still rejecting non-printable-ASCII request URIs.
* `/storage/**` goes through the lenient `DefaultHttpFirewall` (with encoded slash allowed), because local-disk image URLs routinely contain Chinese or mojibake filenames that `StrictHttpFirewall`'s printable-ASCII URI check would reject. `/storage/**` is served as static resources by `StorageWebMvcConfigurer` in `ecshopx-espier` when `ecshopx.storage.driver=local` (maps `ecshopx.storage.local.root`, default `storage/app/public`, onto the `/storage/**` resource handler).

The firewall runs inside the Spring Security `FilterChainProxy` before any chain matching, so it applies to *all* requests — including `/storage/**` and `/wechatAuth/**`, which match no security chain at all. `StorageUnicodeHttpFirewallTest` pins both sides: `/storage/…微信图片….jpg` passes, `/api/v1/espier/微信` is still rejected.

### Security chains and the operator-JWT servlet filter

`EspierSecurityConfig` (`ecshopx-espier`) deliberately keeps Spring Security thin — its Javadoc states that authentication has moved to annotations + interceptors. Only two chains remain:

1. **`wxappMemberSessionSecurityFilterChain`** (`@Order(-2)`, matcher `/api/v1/wxapp/**`): CSRF disabled, `anyRequest().authenticated()`, with `WxappMemberSessionAuthenticationFilter` inserted before `UsernamePasswordAuthenticationFilter`. That filter reads the `x-wxapp-session` header, resolves the session through `WxappMemberAuthService`, stores the auth map under the `WxappMemberAuthAttributes.REQUEST_ATTR` request attribute, and sets a `UsernamePasswordAuthenticationToken` in the `SecurityContext`. Failures are written as a **Dingo-style envelope with HTTP 200** (`{"data":{"message":…,"status_code":400|401}}`).
2. **`defaultPermitAllSecurityFilterChain`** (`@Order(100)`, matcher `/api/**`): CSRF disabled, everything permitted. All real authorization happens later, in interceptors.

Because handler-level auth is annotation-driven, `OperatorJwtAuthenticationFilter` (`ecshopx-espier`, a plain `@Component OncePerRequestFilter` auto-registered as a servlet filter for all URLs) provides a **safety net for admin API paths that forgot an annotation**: it requires a valid operator Bearer JWT for every request that maps to a controller method and is not on its allowlist. Its `shouldNotFilter` skips:

* Paths with no handler mapping (unmapped paths fall through to the DispatcherServlet's normal 404/405 instead of a spurious 401 — the filter consults `RequestMappingHandlerMapping` directly).
* **POS territory**: every `/api/v1/pos/**` path, plus any request that already carries `PosJwtRequestAttributes.POS_JWT_USER_DATA` (set by the POS filter below) — operator JWT never authenticates a POS session.
* The anonymous admin endpoints (login, oauth/shuyun login, getLevel, token refresh, resetpassword, image/SMS codes, basic, credential, authorizeurl, logout, wechat/workwechat operator binding) — the code comments note this list must stay in sync with `MiddlewareWebMvcConfigurer`'s `AdminAuthInterceptor` excludes.
* All callback/notify paths (`/api/v1/alipay/notify`, chinaums, doumen-intl, adapay, bspay, `/wechatAuth/**`, workwechat notify paths, saascert matrix callbacks), the install-agreement endpoint, the meiqia / datacube public reads.
* `/api/openapi/**` (sign-based auth) and the front surfaces `/api/v1/h5app/**` and `/api/v1/wxapp/**` (independent auth).

On success it mirrors `AdminAuthInterceptor`'s work — verify the compact token with `OperatorJwtCompactCodec`, check `exp`, consult the operator JWT blacklist (Redis prism DB, key `operator_jwt_invalidate:<sha256(jwt)>`), enrich shop-app member tokens, run `CompanysActivationService.checkUserAuth`, attach operator id and selected distributor — and stores the claims map under the `OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA` request attribute (`cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA`), the same key controllers and downstream interceptors read. Errors are Dingo-style `{"data":{"message":…,"status_code":401}}` (401) or `ApiResult.fail(403, …)` (403).

### The POS auth chain: a second JWT lifecycle beside the operator JWT

POS (收银台) terminals authenticate against the **same admin controllers** but with their own token type, issued and validated independently of the operator JWT:

* **Issuance** — `PosAuthController` (`ecshopx-companys`, `/api/v1/pos/auth`, `@DingoResponse(unauthorized = true, unauthorizedPassMessage = true)`) exposes `login` / `logout` / `me`. `PosAuthService.login` verifies the employee number and password against `PosStoreAccount` (password decrypted with `SensitiveFieldEncryptor`), rejects `disabled` / `resigned` accounts and closed/deleted shops, then `PosJwtIssuer` (espier, behind the `PosJwtIssuerPort` interface in `ecshopx-common`) mints a compact HS512 token with `token_kind=pos`, `iss=$POS_JWT_ISSUER` (default `ecshopx-pos`), `sub=id`, a random `jti`, and TTL from `JWT_TTL` (default 120 minutes). Logout calls `PosJwtIssuerPort.invalidate`, which blacklists the token until its `exp`.
* **Blacklist** — `PosJwtBlacklistService` implements `PosJwtBlacklistPort` with Redis keys `pos_jwt_invalidate:<sha256(jwt)>` in the prism DB — a separate key prefix from the operator blacklist so the two lifecycles never collide.
* **Authentication** — `PosJwtAuthenticationFilter` (espier, `@Component @Order(HIGHEST_PRECEDENCE + 40) OncePerRequestFilter`, i.e. it runs **before** the operator filter) engages only when the Bearer token *looks like* a POS token (`token_kind=pos` or the POS issuer); operator or front tokens pass through untouched. For POS tokens it enforces, in order: the `PosJwtPathWhitelist` (a deny-by-default table of POS-reachable method+path pairs — checkout, carts, order/aftersales reads, goods/member reads, write-off — with `/api/v1/store/pos-accounts/**` explicitly denied as admin-only), the POS blacklist, signature + `exp`, the POS kind/issuer check again, and `PosJwtSessionValidatorPort.assertSessionStillValid` (`PosJwtSessionValidator` in companys: the token dies immediately when the `PosStoreAccount` row's status, `distributor_id`, or `role` no longer matches the token snapshot). Success stores the claims under `PosJwtRequestAttributes.POS_JWT_USER_DATA`; every failure is a Dingo-style 401 (`{"data":{"message":…,"status_code":401}}`). `POST /api/v1/pos/auth/login` itself is anonymous.
* **Downstream dual-JWT handling** — the operator filter and `AdminAuthInterceptor` both skip any request that already carries `POS_JWT_USER_DATA`, so a POS token satisfies `@AdminAuth` without the operator session-token checks. Shared code that needs the actor reads claims through `DualJwtSupport` (`ecshopx-common`): POS claims win when both attributes are present, `requireCompanyId` reads `company_id`, and `requireActingOperatorId` returns `pos_store_account_id` (fallback `id`) for POS sessions vs `operator_id` for operator sessions — throwing `UnauthorizedException("未登录")` when neither is usable.

### The merchant-wxapp JWT servlet filter

Beside the annotation-driven `@FrontMerchantAuth` interceptor, the merchant Bundle also runs a **servlet filter** — `MerchantWxappJwtAuthenticationFilter` (`ecshopx-merchant`, a `@Component OncePerRequestFilter`) — that guards the *one* merchant endpoint that is authenticated but must run before any `@FrontMerchantAuth` handler annotation applies: `GET /api/v1/h5app/wxapp/merchant/type/list` (ECX-10617). Its `shouldNotFilter` returns `true` for every path **except** that single GET, and explicitly skips the settlement-apply and password-reset surfaces (`/api/v1/h5app/wxapp/merchant/settlementapply/**`, `…/auditstatus`, `…/detail`, `…/step`, `…/password/reset`), which deliberately authenticate as a *mall member* through `@FrontAuth` instead.

When it engages, it performs the same Nimbus HS256 verification as `FrontMerchantAuthInterceptor` (secret from `MerchantWxappJwtProperties.resolveSecretBytes`, issuer check, `exp`/`nbf` with leeway, positive `company_id`/`account_id`, and a `MerchantSettlementApply` row whose `company_id` matches), then stores a `MerchantWxappAuthAttributes(companyId, accountId)` under the `MerchantWxappAuthAttributes.REQUEST_ATTR` request attribute **and** installs a `UsernamePasswordAuthenticationToken` in the `SecurityContext`. Every failure writes the Dingo `401001` envelope (`{"data":{"message":"Unable to authenticate user.","code":401001,"status_code":401}}`) at HTTP 401. The older `MerchantWxappPasswordResetSecurityConfig` (a dedicated `SecurityFilterChain` for this surface) is now a deprecated empty shell — merchant H5 auth moved to `@FrontMerchantAuth` plus this filter.

### Locale resolution

`CommonWebLocaleAutoConfiguration` (`ecshopx-common`, servlet web apps only) registers `CountryCodeLocaleFilter` on `/*` at order `Ordered.LOWEST_PRECEDENCE - 50` — deliberately late so Undertow has already parsed the query string (the filter also touches `getParameterMap()` first and falls back to parsing `country_code` out of the raw query string). The filter:

1. Takes `country_code` (query parameter) if present, else the first tag of `Accept-Language`.
2. Normalizes it through `LangueProperties.resolveToSupportedTag` — the supported list comes from `langue.list` (`application.properties` pins six tags: `zh-CN, en-CN, ar-SA, zh-TW, ja-JP, ru-RU`, which is also the code default) with `langue.default-lang=zh-CN`; unknown/blank tags fall back to the default. Startup fails if `langue.default-lang` is blank or not in `langue.list`.
3. Converts the tag to a `MessageSource` locale via `RequestMessageLocale.toMessageLocale` (note the deliberate `en-CN → en-US` mapping, because the English message bundle is `messages_en_US`), sets `LocaleContextHolder` for the request thread, and resets it after the chain.

Downstream code reads the tag back with `RequestLangTag.current(langueProperties)` or resolves per-company effective languages via `LanguageTagSupport` / `CompanyLanguageResolver` (company-enabled language clamping). `CountryCodeLocaleFilterTest` pins the param-wins, invalid-falls-back, and Accept-Language-fallback behaviors.

## Stage 2 — The interceptor chain

### Registration and order

The espier/merchant/salesperson `WebMvcConfigurer`s register the main MVC interceptor chain; interceptors execute in registration order (explicit `.order(...)` values). The openapi module registers its auth interceptor separately (see [Stage 2b](#stage-2b--the-openapi-signed-request-interceptor)).

| Order | Interceptor | Registered by | Path pattern |
|---|---|---|---|
| 5 | `EspierAgreementRateLimitInterceptor` | `MiddlewareWebMvcConfigurer` (espier) | `/api/v1/espier/system/agreement` |
| 10 | `AdminAuthInterceptor` | `MiddlewareWebMvcConfigurer` | `/api/**` minus the admin anonymous excludes |
| 15 | `FrontNoAuthInterceptor` | `MiddlewareWebMvcConfigurer` | `/api/**` |
| 20 | `FrontAuthInterceptor` | `MiddlewareWebMvcConfigurer` | `/api/**` |
| 21 | `QywxSalespersonAuthInterceptor` | `SalespersonWebMvcConfigurer` (salesperson) | `/api/**` |
| 25 | `FrontMerchantAuthInterceptor` | `MerchantInterceptorWebMvcConfigurer` (merchant) | `/api/**` |
| 100 | `ActivatedInterceptor` | `MiddlewareWebMvcConfigurer` | `/api/**` |
| 200 | `DataPassInterceptor` | `MiddlewareWebMvcConfigurer` | `/api/**` |
| 300 | `ShopLogInterceptor` | `MiddlewareWebMvcConfigurer` | `/api/**` |

Every interceptor starts its `preHandle` with the same two guards: skip unless the handler is a `HandlerMethod`, and skip unless *its* annotation is present (method level first, then class level). An interceptor without its annotation is a no-op — the chain is effectively annotation-routed. `EspierAgreementRateLimitInterceptor` is the exception: it throttles by client IP (Redis `espier:agreement:throttle:<ip>`, 30 requests/minute) on the install-agreement endpoint and writes `{"message":"Too Many Attempts.","status_code":429}` with HTTP 429.

### The annotation matrix

| Annotation | Consumed by | Effect on the request | Attribute(s) it leaves behind |
|---|---|---|---|
| `@FrontAuth` | `FrontAuthInterceptor` (order 20) | **Strict** H5 member JWT required; verifies HS256 signature, issuer, time claims (with leeway), required claims (`iss, iat, exp, nbf, sub, jti`), a positive `company_id` claim, and the H5 JWT blacklist (Redis prism DB, `h5_jwt_invalidate:<sha256>`, grace period configurable). Rejects with HTTP 401 `{"data":{"message":"Unable to authenticate user.","code":401001,"status_code":401}}`; a truthy `disabled` claim rejects with HTTP 403 and code `403001`. Enriches claims via `H5JwtUserLoginInfoEnrichService` (rehydrates WeChat/member fields like PHP `EspierLocalUserProvider::getUserLoginInfo`). | `h5AuthClaims` (`H5FrontAuthAttributes.H5_AUTH_CLAIMS`), `h5CompanyId` |
| `@FrontNoAuth` | `FrontNoAuthInterceptor` (order 15) | **Optional** JWT / anonymous tenant resolution (see below). Also *suppresses* `@FrontAuth` when both would apply — a method-level `@FrontNoAuth` makes `FrontAuthInterceptor` skip the method even if the class carries `@FrontAuth`. | `h5CompanyId`, `h5AuthClaims` (JWT claims or anonymous claims) |
| `@AdminAuth` | `AdminAuthInterceptor` (order 10) | Operator (admin console) JWT required — **skips outright when the request already carries `POS_JWT_USER_DATA`** (POS sessions were fully vetted by `PosJwtAuthenticationFilter`). Otherwise: Bearer token, `OperatorJwtCompactCodec` compact verify (own HMAC check with a jjwt fallback for legacy tokens), `exp` check, shop-app member token enrichment, `CompanysActivationService.checkUserAuth` (validates the encrypted session token inside the `id` claim unless `operator_type` is `shopadmin`/`user` or `source` is `user`). Rejects 401 Dingo-style (`data.message` + `data.status_code`, no `code` key); a `ResourceException` from `checkUserAuth` rejects 403 as `ApiResult.fail(403, msg)`. Attaches the selected distributor for `operator_type=distributor`. | `OPERATOR_JWT_USER_DATA` claims map |
| `@FrontMerchantAuth` | `FrontMerchantAuthInterceptor` (order 25, merchant Bundle) | Merchant-settlement (商户入驻) Mini Program JWT under **separate** properties `ecshopx.merchant.wxapp.jwt.*` (issuer `ecshopx`, secret from `JWT_SECRET`, 60-minute TTL). Verifies HS256, issuer, time claims with leeway, positive `company_id`/`account_id` claims, and that a `MerchantSettlementApply` row exists for `account_id` in the same company. Rejects with the same `401001` Dingo envelope as `@FrontAuth`. | `MerchantWxappAuthAttributes` record (`companyId`, `accountId`) |
| `@QywxSalespersonAuth` | `QywxSalespersonAuthInterceptor` (order 21, salesperson Bundle) | Work-WeChat salesperson Mini Program session auth: reads the `x-wxapp-session` header, loads `frontSession3rd:<session>` from the wechat Redis template via `WxappSalespersonSessionAuthService`, and re-validates the salesperson/shop/role rows. Rejects with Dingo-style envelopes carrying business codes `400001` (bad request), `401001` (unauthorized), `403001` (forbidden) at the matching HTTP status. | `QywxSalespersonAuthAttributes.REQUEST_ATTR` auth map |
| `@Activated(routeAlias = "…")` | `ActivatedInterceptor` (order 100) → `ActivatedMiddlewareService` | Menu permission + company activation + distributor scope. Checks the route alias against the operator's role menus (`ShopMenuService.collectApisFromMenus`, menu version by operator type: distributor 3, dealer 5, merchant 6, supplier 7, others 1); `admin` operators skip unless `common.check-superadmin-permission=true`; a fixed whitelist and delivery-staff paths bypass. Verifies the company exists and is not disabled, and computes the distributor scope. Failures throw `ResourceException` (→ 422 or Dingo envelope downstream). | `cn.shopex.ecshopx.activated.distributor_id` and `…distributorIds` (see `ActivatedRequestAttributes`) |
| `@DataPass(pathAlias = "…")` | `DataPassInterceptor` (order 200) → `ShopOperatorDatapassApplyService` | Sensitive-data access audit. `admin` operators, merchant-main accounts, and self-delivery staff are exempt; otherwise, if the operator lacks datapass authorization the interceptor sets the request attribute **`x-datapass-block` = 1** (controllers read it and mask data), and when authorized it inserts an `OperatorDataPassLog` row. Method-level only. Never blocks the request. | `x-datapass-block` (only when blocked) |
| `@ShopLog` | `ShopLogInterceptor` (order 300, `afterCompletion`) | Writes a merchant operation log row (`log_type=operator`, operator/company ids from the JWT claims, URI, first `X-Forwarded-For` IP or remote address) for **non-GET** requests after the handler completes. Failures are swallowed (debug log). | none |
| `@OperationLog(title, businessType, …)` | `OperationLogAspect` (AOP, `ecshopx-common`) | Timed operation logging to the SLF4J logger named `operation-log` (`[title] METHOD uri | method | Nms | OK/ERROR`). Not an interceptor, and currently unused by controllers in the tree — it is the reserved hook for future DB-backed operation logs. | none |
| `@AdminLog` | `AdminLogAspect` (AOP, espier) | After a successful 2xx `ResponseEntity` whose `ApiResult.data` contains a `token`, records a login audit log; also records the failed-login audit for `POST /api/v1/operator/getLevel`. | none |
| `@DingoResponse` | `GlobalExceptionHandler` + module advices | Not auth: selects the legacy PHP error envelope per endpoint (see [Response and error conventions](#response-and-error-conventions)). | none |
| `@OpenapiResponse` | `OpenapiResponseBodyAdvice` (openapi Bundle) | Wraps returns in the PHP OpenAPI envelope — see [OpenAPI Integration](../integrations/openapi.md). | none |

The typical admin controller stacks them: `@DingoResponse(...) @AdminAuth @ShopLog` on the class, `@Activated(routeAlias = "trade.setting.set")` on individual methods (see `orders/api/admin/v1/TradeSettingController`). Front controllers use `@FrontNoAuth` on the class and `@FrontAuth` on the subset of methods that require login (`WxappPaymentController` does exactly this).

### `FrontNoAuthInterceptor`: anonymous tenant resolution

`FrontNoAuthInterceptor` mirrors PHP `FrontNoAuthMiddleWare`: it must always produce a `company_id`, with or without a login. Its resolution order:

1. **Bearer JWT** — if an `Authorization: Bearer` token verifies (same HS256/issuer/time/required-claim/positive-`company_id` rules as `@FrontAuth`), the claims are enriched (`H5JwtUserLoginInfoEnrichService` plus a member-mobile backfill from the DB when the claim is missing) and stored as `h5AuthClaims`.
2. **Anonymous resolution**, otherwise. In order: the `appid` query parameter or `authorizer-appid` header (wx-prefixed appids look up `WechatAuthorizerLookupMapper`, others go through `AliMiniAppCompanyIdLookupPort`); then the fixed system company (`common.system-companys-id` when not SaaS, `common.system-main-companys-id` when SaaS); then the `company_id` query parameter; then the `Origin` header domain via `CompanyDomainLookupService`. If nothing resolves, it throws `UnauthorizedException("无权访问该API,非法访问！")`, and the response envelope depends on the handler's `@DingoResponse.unauthorized()` flag (Dingo nested-`data` 401 with code `401001`/`401002` vs. plain `ApiResult.fail(401, …)`). One carve-out: `MembersController.getNoAuthDecryptPhoneNumber` is allowed through with `company_id = 0`.
3. Anonymous requests get a synthetic anonymous claims map (`buildAnonymousAuthClaims`) stored in `h5AuthClaims`, so controllers can treat both paths uniformly.

## Stage 2b — the openapi signed-request interceptor

The public `/api/openapi/**` gateway authenticates with **signed requests**, not JWTs. `OpenapiWebMvcConfigurer` (ecshopx-openapi) registers `OpenapiAuthInterceptor` on `/api/openapi/**` and `/api/openapi/internal/**` (it runs alongside, and independent of, the espier chain — the operator JWT filter already skips `/api/openapi`, and `/api/openapi/**` matches no `@AdminAuth`/`@FrontAuth` surface). `OpenapiContentCachingFilter` (`@Order(HIGHEST_PRECEDENCE + 20)`, i.e. *before* the POS filter at `+40`) wraps public openapi requests in a `ContentCachingRequestWrapper` so the JSON body can be re-read for the signature merge.

`OpenapiAuthInterceptor.preHandle`:

1. Skips the internal prefix (`/api/openapi/internal/**`) outright, and skips any path not under the public `/api/openapi` prefix.
2. Collects the merged parameter map via `OpenapiRequestParamCollector.collect` (query/form via `FlexibleHttpServletParameterMap.toObjectMap` **plus** the cached JSON body, with `method` defaulted from the URL tail).
3. Requires `version`, `timestamp`, and `app_key` (missing → `VALIDATION_MISSING_PARAMS` / `E4001`).
4. **Debug bypass** — when `openapi.debug=1`, it authenticates as the system company (`openapi.system-company-id`, defaulting to `common.system-companys-id` = `1`) and skips signature/replay checks entirely. Development switch only.
5. Enforces a ±600 s timestamp tolerance (`VALIDATION_TIMESTAMP_ERROR` / `E4002` on drift).
6. Requires `sign`, looks up developer credentials by `app_key` through the `OpenapiDeveloperLookupPort` (`OpenapiDeveloperLookupPortImpl` → `OpenapiDeveloperMapper`; unknown key → `VALIDATION_APPKEY_ERROR` / `E4003`), and verifies the legacy MD5 sign via `OpenapiSignSupport.signMatches` (`token + assemble(params) + token`, upper-cased hex; mismatch → `SIGN_ERROR` / `E4004`).
7. **Replay guard (ECX-10869)** — `OpenapiSignedRequestReplayGuard.assertOnce(appKey, sign, timestamp, 600)` does a Redis `SETNX` on `openapi:auth:replay:{sha256(app_key|sign|timestamp)}` with a TTL equal to the timestamp tolerance, on the default `StringRedisTemplate`. The first sighting of a signed request passes; a replayed `app_key + sign + timestamp` within the window is rejected as `请求已失效或重复` (`VALIDATION_TIMESTAMP_ERROR` / `E4002`). A Redis failure fails *closed* (treated as a replay).
8. On success, stores the developer's `company_id` via `OpenapiAuthAttributes.setAuth(request, companyId)` — downstream openapi controllers read it with `OpenapiAuthAttributes.requireCompanyId`, which throws `UnauthorizedException("未授权")` when absent.

Every auth failure is written directly as the PHP OpenAPI envelope (`OpenapiEnvelope.fail(code, message, params)`, HTTP 200 with `{status:"fail", code, message, data}`), never reaching `GlobalExceptionHandler`. See [OpenAPI Integration](../integrations/openapi.md).

## Stage 3 — `FlexibleBody` binding and the merged-params idiom

### `@FlexibleBody` and its resolver

`@FlexibleBody` is `ecshopx-common`'s replacement for `@RequestBody` that accepts **`application/json`, `multipart/form-data`, and `application/x-www-form-urlencoded`** on the same parameter — because the legacy PHP frontends post the same endpoint as JSON *or* as form data. `FlexibleBodyWebMvcConfigurer` (servlet web apps) prepends `FlexibleBodyMethodArgumentResolver` to the custom argument-resolver list. The resolver dispatches on the declared parameter type:

* **`JsonNode`** — JSON bodies are read from the input stream and parsed; form bodies are converted via `FlexibleHttpServletParameterMap.toObjectMap(request)` then `valueToTree`.
* **`Map`** — JSON bodies are deserialized to the declared generic type; form/multipart bodies become the `toObjectMap` map directly.
* **POJO** — JSON bodies are deserialized; form/multipart bodies are converted with `objectMapper.convertValue`, falling back to `ServletRequestDataBinder` binding if conversion fails.

`required` (default `true`) controls emptiness semantics: with `required = false`, an empty body or empty JSON object resolves to `null` (or an empty instance for POJOs). With `required = true` an empty body throws `BadRequestException("请求体不能为空")`. Validation is opt-in per parameter with **`@Valid`** (not `@Validated`): the resolver runs the jakarta `Validator` itself and converts violations into a `MethodArgumentNotValidException`, which `GlobalExceptionHandler` renders as 422 with `field: message` pairs.

`FlexibleHttpServletParameterMap.toObjectMap(request)` is the PHP-style parameter expander that both the resolver and controllers rely on. It converts the servlet parameter map (query string + form fields + multipart text parts) into a `Map<String, Object>`:

* `name[]=a&name[]=b` and repeated `name` keys become a `List` under `name`;
* indexed keys `name[0]=a&name[1]=b` fold into an ordered `List` under `name`;
* nested keys `rates[0][item_id]=…` become nested maps/lists under `rates`, and all-numeric-key maps are converted to index-ordered lists at any depth;
* scalar parameters stay `String`.

It then **merges the `Activated` middleware attributes**: `distributor_id` and `distributorIds` from `ActivatedRequestAttributes` are injected when the parameters do not already carry them — which is how admin list endpoints transparently become distributor-scoped.

### The controller idiom: merge parameters, then body wins

The dominant front-controller pattern (and the one new endpoints should copy) is `WxappPaymentController.payment`:

```java
@PostMapping(
    value = "/payment", name = "发起支付",
    consumes = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE,
        MediaType.APPLICATION_FORM_URLENCODED_VALUE
    },
    produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<ApiResult<Map<String, Object>>> payment(
        HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
    Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
    if (body != null) {
        merged.putAll(body);
    }
    LinkedHashMap<String, Object> auth = resolveH5Auth(request, merged);
    Map<String, Object> payResult = wxappPaymentService.payment(request, merged, auth);
    ...
    return ResponseEntity.ok(ApiResult.ok(payResult));
}
```

The idiom reproduces PHP/Laravel's `$request->merge()` semantics: **query/form parameters form the base map, and the parsed body overrides on key conflicts** (`putAll`). Because `toObjectMap` reads the servlet parameter map (query + form) while `@FlexibleBody` supplies the JSON body, a JSON POST still sees its query string; a form POST sees its fields once. GET handlers use the same base map without a body (`doPayment` above). The salesperson Bundle has a small local variant (`ShopSalespersonFrontRequestMerge`) with the same "parameters first, body wins" rule.

`resolveH5Auth` in the same controller shows the second half of the idiom: identity comes from request attributes, not parameters — it prefers the wxapp session attribute, then `h5AuthClaims`, backfills `company_id` from `h5CompanyId`, then from the merged params, and only throws `UnauthorizedException` when no source produced a positive company id.

### Identity-scoping: user-controlled ids must bind to the authenticated identity

Because front endpoints authenticate via `h5AuthClaims` but accept arbitrary ids in the merged parameter map, the recent hardening rule is: **any user-controlled id that scopes a query or mutation must be proven to belong to the authenticated `user_id`/`company_id` before the service runs** — mirroring the PHP owner-scope guards (`MembersOwnerScopeGuard`, `GuideOrderTenantScopeGuard`). The two canonical examples, pinned by focused unit tests, are:

* **Members address create/list** (`MembersController`, `@FrontAuth` on the class, ecshopx-members; ECX-10913 / ECX-10923). `createAddress` resolves the acting user through `resolveEffectiveUserIdForCreate(body, authUserId)`: the acting user is always the authenticated `user_id` from `h5AuthClaims` (via `resolveUserId(claims)`); a `buy_user_id` that points at another user is only meaningful when `promoter_user_id` loosely equals the auth user — and even then a foreign `buy_user_id` is rejected with `ResourceException("无权访问该数据")`. The address list applies the same rule through `resolveFilterUserIdForAddressList`, which always returns the auth user id (the former fall-through to `null` — an orphan-address bypass — was removed) and rejects a foreign `buy_user_id` the same way. `MembersControllerAddressBuyUserScopeTest` pins all four branches (no `buy_user_id` → self; self `buy_user_id` → self; foreign `buy_user_id` → reject; foreign promoter → treated as self).
* **Rights write-off code minting** (`RightsController.getRightsCode` → `RightsFrontWxappGetRightsCodeService`, ecshopx-orders; ECX-10924). Before any barcode is generated, the service loads the `Rights` row by id and requires `row.company_id == auth.company_id` **and** `row.user_id == auth.user_id` — a missing row, another user's rights, or another tenant's rights all throw `ResourceException("无权访问该权益")`, with **no side effects** (no Redis write, no barcode generation). Malformed or non-positive `rights_id` values fail earlier with `BadRequestException("获取核销码出错", 422)`. `RightsFrontWxappGetRightsCodeOwnershipTest` pins each rejection path and the side-effect-free guarantee.

The same rule is enforced **at the service layer** for cross-tenant scoping, so a leaked or forged id cannot traverse the `company_id` / `store` / `distributor` boundary even when the controller forgot to check: the `Activated` middleware already injects `distributor_id`/`distributorIds` into the merged params (see [`toObjectMap`](#flexiblebody-and-its-resolver)), and POS order writes are guarded by `PosOrderDocAccessGuard` (orders, implementing `common/auth/PosOrderDocAccessPort`), which requires same-store and handler-clerk ownership before a POS session may mutate an order. Front controller endpoints additionally guard ownership directly — e.g. `WxappOrderController` throws `UnauthorizedException("无权访问该API,非法访问！")` when no positive `company_id` can be resolved from the authenticated claims/attributes rather than trusting a request-supplied id.

When adding a front endpoint that takes an id (address, order, rights, coupon, …) from the request, follow this pattern: derive identity from `h5AuthClaims`/`h5CompanyId` (never from the merged params), and reject mismatches with `ResourceException`/`BadRequestException` so the envelope conventions below apply. See [Members Domain](../domains/members.md) and [Orders Domain](../domains/orders.md).

## Response and error conventions

### `ApiResult` — the standard envelope

`ApiResult<T>` (`ecshopx-common`) is the success envelope everywhere: `ApiResult.ok(data)` → `{"code":200,"msg":"success","data":…}` and `ApiResult.fail(422,"xxx")` → `{"code":422,"msg":"xxx"}` (`data` is omitted via `@JsonInclude(NON_NULL)`). Controllers return `ResponseEntity<ApiResult<…>>` (or `ApiResult<…>` directly).

### `@DingoResponse` — the legacy PHP error envelope

The PHP system (Dingo API + Laravel HttpException) returned errors as a **nested `data` object** rather than a flat body. `@DingoResponse`, placed on a controller class or method, declares which endpoints still speak that dialect so `GlobalExceptionHandler` can match it — new endpoints do **not** need to edit any routing table in the handler; they just add the annotation with the right attributes:

```java
@DingoResponse(
    resource = DingoResponse.ResourceStyle.DINGO,   // ResourceException style
    badRequest = DingoResponse.BadRequestStyle.DINGO_400,  // BadRequestException style
    unauthorized = true,                            // UnauthorizedException → legacy 401 shapes
    unauthorizedHttpOk = false,                     // true → HTTP 200, error in data.status_code
    unauthorizedPassMessage = false,                // true → 401 envelope passes the exception message through (still code 401001)
    notFound = false)                               // NoResourceFoundException → legacy 404
```

Three envelope families are produced:

* **`dingoStyleError(message, statusCode[, businessCode])`** — **HTTP 200** with body `{"data":{"message":…,"code":<businessCode>,"status_code":…}}` (the `code` key appears only when a business code is present). Matches PHP Dingo's default of reporting errors in the body with an OK status.
* **`dingoStyleErrorHttp(message, statusCode)`** — HTTP status *equals* `status_code` (PHP `HttpException` behavior). Used for `ConflictException` (409), `TooManyRequestsException` (429), and `ForbiddenException` (403) when the annotation is present.
* **Fixed 401/404 shapes** — `UnauthorizedException` under `unauthorized = true` maps to (checked in this order): `unauthorizedHttpOk = true` → HTTP 200 `dingoStyleError(msg, 403)` (the operator login endpoint sets this); message `"该账号已被禁用."` → `code 401002`; the H5 blacklist message `"The token has been blacklisted"` (`H5JwtBlacklistPort.TOKEN_BLACKLISTED_MESSAGE`) → 401 without a business code; `unauthorizedPassMessage = true` → 401 passing the exception message through with `code 401001` (used by the POS auth endpoints so i18n login errors survive); otherwise the fixed `"Unable to authenticate user."` with `code 401001`. `notFound = true` maps `NoResourceFoundException` to `dingoStyleError("404 Not Found", 404)`.

### How `GlobalExceptionHandler` picks an envelope

`GlobalExceptionHandler` is a `@RestControllerAdvice` at `Ordered.LOWEST_PRECEDENCE`. For each exception it first resolves the handler's `@DingoResponse` from `HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE` (method annotation first, then class annotation) and only then decides:

| Exception | Without `@DingoResponse` (or attribute off) | With `@DingoResponse` |
|---|---|---|
| `ResourceException` | 422 `ApiResult.fail(422, msg)` | `resource()` style: `DINGO` → `dingoStyleError(msg, embeddedStatusCode ?? 422, embeddedBusinessCode)`; `DINGO_FIXED` → `dingoStyleError(msg, 422)`; `PLAIN` → plain 422 ApiResult |
| `BadRequestException` | 400 `ApiResult.fail(400, msg)` | `badRequest()` style: `DINGO_400` → `dingoStyleError(msg, embedded ?? 400)`; `DINGO_422` → `embedded ?? 422`; `DINGO_FIXED` → fixed 422; `NONE` → plain 400 ApiResult |
| `UnauthorizedException` | 401 `ApiResult.fail(401, msg)` | if `unauthorized()`: the fixed 401 shapes above (honoring `unauthorizedHttpOk`) |
| `ForbiddenException` | 403 `ApiResult.fail(403, msg)` | `dingoStyleError(msg, 403)` (HTTP 200) |
| `ConflictException` | 409 ApiResult | `dingoStyleErrorHttp(msg, 409)` (HTTP 409) |
| `TooManyRequestsException` | 429 ApiResult | `dingoStyleErrorHttp(msg, 429)` (HTTP 429) |
| `NoResourceFoundException` | 404 `ApiResult.fail(404, "资源不存在")` | if `notFound()`: `dingoStyleError("404 Not Found", 404)` |
| `MethodArgumentNotValidException` | 422 ApiResult with `field: message; …` joined (covers `@FlexibleBody @Valid` failures) | never Dingo |
| `MissingServletRequestParameterException` / `MethodArgumentTypeMismatchException` / `HttpMessageNotReadableException` | 400 ApiResult with a Chinese message (`缺少参数: …`, `参数类型错误: …`, `请求体格式错误`) | never Dingo |
| `HttpRequestMethodNotSupportedException` | 405 ApiResult | never Dingo |
| any other `Exception` | 500 `ApiResult.fail(500, "服务器内部错误")`, logged at error | never Dingo |

Business and validation metadata travels **inside the exceptions**, not the annotation: `ResourceException` and `BadRequestException` can carry `embeddedStatusCode`, `embeddedBusinessCode`, and `fieldErrors` (a `Map<String, List<String>>` of field → error-code list, e.g. `validation.required`). `GlobalExceptionHandler` does not expand `fieldErrors`; that is done by module-level advices.

### Module advices in front of `GlobalExceptionHandler`

Several Bundles register `@RestControllerAdvice(basePackages = "…")` beans at `HIGHEST_PRECEDENCE` that run *before* `GlobalExceptionHandler` and return `null` to defer: `OrdersFrontV1BadRequestFieldErrorsAdvice` and `SalespersonFrontV1BadRequestFieldErrorsAdvice` add the `data.errors` map (field-error codes) to `BadRequestException` responses under the Dingo envelope; `pointsmall`'s front `ItemsController` declares its own `@ExceptionHandler`s for the same purpose. When adding an endpoint that needs the `errors` validation shape, follow that pattern rather than changing `GlobalExceptionHandler`.

### Envelopes written before the handler runs

Auth failures never reach `GlobalExceptionHandler` — the interceptors/filters write JSON directly (see the matrix above for the exact shapes): `FrontAuthInterceptor` (401 `401001` / 403 `403001` Dingo), `FrontNoAuthInterceptor` (Dingo when `unauthorized = true`, else `ApiResult.fail(401, …)`), `AdminAuthInterceptor`, `OperatorJwtAuthenticationFilter`, and `PosJwtAuthenticationFilter` (Dingo 401 without a `code` key; the operator-side two also use `ApiResult.fail(403, …)`), `FrontMerchantAuthInterceptor` and its servlet-filter twin `MerchantWxappJwtAuthenticationFilter` (Dingo `401001`), `QywxSalespersonAuthInterceptor` (Dingo `400001`/`401001`/`403001`), `WxappMemberSessionAuthenticationFilter` (Dingo shape but **HTTP 200**), `EspierAgreementRateLimitInterceptor` (HTTP 429), and `OpenapiAuthInterceptor` (the PHP OpenAPI envelope, HTTP 200).

### Choosing a style for a new endpoint

* **New internal/admin endpoints with no PHP twin:** plain `ApiResult` — omit `@DingoResponse` entirely.
* **Migrated endpoints whose PHP frontend parses `data.message` / `data.status_code`:** `@DingoResponse(resource = DINGO, badRequest = DINGO_400 or DINGO_422, unauthorized = true)`, matching what the legacy route returned. Look at the sibling controllers in the same Bundle (e.g. `TradeSettingController` for admin, `WxappPaymentController` for front) and copy their attribute values.
* **OpenAPI `/api/openapi/**` endpoints:** `@OpenapiResponse`, never `@DingoResponse` — see [OpenAPI Integration](../integrations/openapi.md).

## Configuration reference

| Property | Default | Used by |
|---|---|---|
| `server.undertow.options.server.allow-unescaped-characters-in-url` | `true` | Undertow URL parsing |
| `server.undertow.max-http-post-size` | `64MB` | Undertow POST cap |
| `spring.mvc.servlet.pathmatch.use-trailing-slash-match` | `true` | MVC path matching |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | `50MB` / `55MB` | Spring multipart |
| `JWT_SECRET` (→ `ecshopx.h5.jwt.secret-base64`, `ecshopx.merchant.wxapp.jwt.secret-base64`, operator/POS JWT key) | must be set (Base64 of a 32-byte key) | All four JWT verifiers/issuers; startup fails if blank |
| `JWT_TTL` / `JWT_REFRESH_TTL` | 120 / 20160 minutes | H5 JWT TTL / refresh window; `JWT_TTL` is also the POS JWT TTL |
| `POS_JWT_ISSUER` | `ecshopx-pos` | `PosJwtIssuer` `iss` claim and `PosJwtAuthenticationFilter` token-kind detection |
| `ecshopx.h5.jwt.blacklist-grace-period-seconds` | 0 | H5 blacklist grace |
| `ecshopx.merchant.wxapp.jwt.ttl-minutes` | 60 | Merchant JWT TTL |
| `openapi.debug` | `0` | `OpenapiAuthInterceptor` signature/replay bypass (dev only) |
| `openapi.system-company-id` | `common.system-companys-id` (`1`) | Company used when `openapi.debug=1` |
| `common.system-is-saas` / `common.system-companys-id` / `common.system-main-companys-id` | `false` / `1` / empty | `FrontNoAuthInterceptor` fixed-company resolution |
| `common.check-superadmin-permission` | `false` | `ActivatedMiddlewareService` permission check for admins |
| `langue.list` / `langue.default-lang` | six tags / `zh-CN` | Locale resolution |
| `ecshopx.storage.driver` / `ecshopx.storage.local.root` | `local` / `storage/app/public` | `/storage/**` static handler and the firewall carve-out |

See [Operations — Configuration](../operations/configuration.md) for the operations-level view and [Members Domain](../domains/members.md) for the H5 login/JWT issuance side.

## Focused tests that pin this pipeline

* `StorageUnicodeHttpFirewallTest` (bootstrap) — `/storage/**` with Chinese/mojibake filenames passes the firewall; non-storage Chinese paths are rejected by `StrictHttpFirewall`.
* `CountryCodeLocaleFilterTest` (common) — `country_code` wins, invalid tags fall back to `langue.default-lang`, `Accept-Language` is the fallback; `LocaleContextHolder` is reset after the chain.
* `LanguePropertiesTest` (common) — supported-tag resolution and the startup validation of `default-lang` against `list`.
* Web-slice tests under `ecshopx-bootstrap/src/test` and Bundle test trees build standalone `MockMvc` with `setCustomArgumentResolvers(new FlexibleBodyMethodArgumentResolver(objectMapper, null))` and inject the operator JWT map via `requestAttr(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA, …)` — the pattern for testing a controller without booting the interceptor chain: `ShopexBindControllerWebSliceTest`, `ShopsSetWxShopsSettingWebSliceTest`, `OperatorsGetCredentialsWebSliceTest`, `AftersalesReviewEndpointMappingTest`, and `LoginControllerWxappNewLoginWebTest` (espier, asserting the merged-params hand-off to `FrontWxappNewLoginService`).
* `ReservationFrontV1ReservationControllerCreateReservationFinishPublishWebMvcTest` (reservation) — the `/api/v1/wxapp/**` session-secured surface.
* POS chain: `PosAuthServiceTest` (companys) — employee-number login, account/shop status rejection, claim shape; `PosJwtSessionValidatorTest` (companys) — a changed `status` / `distributor_id` / `role` invalidates an otherwise-valid POS token; `PosJwtIssuerTest` and `PosJwtPathWhitelistTest` (espier) — `token_kind=pos` issuance/invalidate round-trip and the deny-by-default path table; `DualJwtSupportTest` (common) — POS claims win over operator claims and the acting-id mapping (`pos_store_account_id` vs `operator_id`).
* OpenAPI signed requests: `OpenapiSignedRequestReplayGuardTest` and `OpenapiAuthInterceptorReplayTest` (openapi) — the first sighting of an `app_key+sign+timestamp` passes, a replay within the tolerance window is rejected, and `openapi.debug=1` bypasses both signature and replay checks.
* Identity-scoping hardening: `MembersControllerAddressBuyUserScopeTest` (members) — address create restricts `buy_user_id` to the authenticated user; `RightsFrontWxappGetRightsCodeOwnershipTest` (orders) — rights write-off code minting requires the rights row to belong to the auth user and tenant, with no side effects on rejection.
