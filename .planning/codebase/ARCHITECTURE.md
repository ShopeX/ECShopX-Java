# 架构

<!-- refreshed: 2026-07-08 -->
**分析日期:** 2026-07-08

## 系统概览

```text
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        API 层 (REST Controllers)                                 │
│   admin/v1 (@AdminAuth)  │  front/v1 (@FrontAuth)  │  thirdapi/v1-v2 (OpenAPI) │
│   `/api/v1/*`            │  `/api/v1/*`             │  `/api/thirdapi/v1/*`     │
└────────┬─────────────────┴────────┬─────────────────┴────────┬──────────────────┘
         │                          │                           │
         ▼                          ▼                           ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     拦截器 / 安全层                                               │
│  `ecshopx-espier/…/web/interceptor/AdminAuthInterceptor.java`                   │
│  `ecshopx-espier/…/web/interceptor/FrontAuthInterceptor.java`                   │
│  `ecshopx-espier/…/web/interceptor/ActivatedInterceptor.java`                   │
│  `ecshopx-espier/…/security/OperatorJwtAuthenticationFilter.java`               │
└────────┬────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         服务层 (业务逻辑)                                         │
│  细粒度: 每个用例对应一个 @Service (例如 AdminOrderListService)                    │
│  `ecshopx-{module}/…/service/**/*.java`                                         │
└────────┬───────────────────────────┬────────────────────────────────────────────┘
         │                           │
         ▼                           ▼
┌──────────────────────────┐  ┌───────────────────────────────────────────────────┐
│  端口 / 集成层            │  │              数据访问层                             │
│  common 中的接口          │  │  Mapper (MyBatis-Plus BaseMapper<T>)               │
│  `common/port/**`        │  │  Repository (复杂查询)                              │
│  impl 在 bootstrap/      │  │  `ecshopx-{module}/…/mapper/*.java`                │
│  dispatch 模块中          │  │  `ecshopx-{module}/…/repository/*.java`            │
└──────────┬───────────────┘  └───────────────────────┬───────────────────────────┘
           │                                          │
           ▼                                          ▼
┌──────────────────────────┐  ┌───────────────────────────────────────────────────┐
│  调度层 (事件总线)        │  │       MySQL (单数据库, 通过 company_id 列实现        │
│  Redis / RabbitMQ        │  │       多租户)                                       │
│  `ecshopx-dispatch/`     │  │                                                   │
└──────────────────────────┘  └───────────────────────────────────────────────────┘
```

## 组件职责

| 组件 | 职责 | 文件 |
|------|------|------|
| EcshopxApplication | 启动入口；组件扫描与 Mapper 扫描 | `ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/EcshopxApplication.java` |
| ecshopx-common | 共享注解、异常、基础实体、端口接口、调度发布接口 | `ecshopx-common/src/main/java/cn/shopex/ecshopx/common/` |
| ecshopx-dispatch | 事件总线基础设施 (Redis streams + RabbitMQ 驱动, 扇出, 重试) | `ecshopx-dispatch/src/main/java/cn/shopex/ecshopx/dispatch/` |
| ecshopx-espier | Web 基础设施: 拦截器、JWT 安全、存储、文件上传 | `ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/` |
| ecshopx-bootstrap | 组装模块: 端口适配器实现、调度配置装配、application.properties | `ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/` |
| 业务模块 | 按限界上下文划分的领域逻辑 (orders, goods, companys, members 等) | `ecshopx-{module}/src/main/java/cn/shopex/ecshopx/{module}/` |

## 模式概览

**整体架构:** 模块化单体 — 50+ 个 Maven 模块部署为单个 Spring Boot fat-JAR。

**关键特征:**
- 单一部署物 (`ecshopx-bootstrap` 通过 Maven 依赖聚合所有模块)
- 通过 `scanBasePackages = "cn.shopex.ecshopx"` 扫描所有模块的组件
- 通过 `@MapperScan(basePackages = "cn.shopex.ecshopx.**.mapper")` 扫描 Mapper
- 模块间通过 Port 接口通信 (在 `ecshopx-common` 中定义, 在消费模块或 bootstrap 中实现)
- 通过自定义 `DispatchFacade` 进行异步事件调度 (Redis streams 或 RabbitMQ)
- 通过所有租户范围表上的 `company_id` 列实现多租户

## 分层

**API 层 (Controllers):**
- 职责: HTTP 请求路由、输入提取、响应格式化
- 位置: `ecshopx-{module}/…/api/admin/v1/` 和 `ecshopx-{module}/…/api/front/v1/`
- 包含: 带有 `@AdminAuth`、`@FrontAuth` 或 `@FrontNoAuth` 注解的 `@RestController` 类
- 依赖: 服务层, `FlexibleBody`/`FlexibleHttpServletParameterMap` 用于灵活的参数解析
- 使用方: 外部 HTTP 客户端 (管理后台前端、小程序、H5)

**拦截器 / 安全层:**
- 职责: JWT 认证、授权 (菜单权限)、激活检查、日志记录
- 位置: `ecshopx-espier/…/web/interceptor/` 和 `ecshopx-espier/…/security/`
- 包含: `HandlerInterceptor` 实现、JWT 过滤器
- 依赖: 注解标记 (`@AdminAuth`, `@FrontAuth`, `@Activated`)
- 使用方: Spring MVC 调度链

**服务层:**
- 职责: 业务逻辑；每个用例对应一个服务类 (细粒度)
- 位置: `ecshopx-{module}/…/service/`
- 包含: `@Service` Bean, 通常使用构造器注入
- 依赖: Mapper、Repository、端口接口、其他服务
- 使用方: Controller、调度处理器、定时任务

**数据访问层:**
- 职责: 数据库交互
- 位置: `ecshopx-{module}/…/mapper/` (MyBatis-Plus 接口) 和 `ecshopx-{module}/…/repository/` (复杂查询类)
- 包含: 继承 `BaseMapper<T>` 的 `@Mapper` 接口, 以及可选的 XML Mapper 在 `src/main/resources/mapper/` 中
- 依赖: 领域实体, MyBatis-Plus 框架
- 使用方: 服务层

**领域层:**
- 职责: 实体/表映射 (贫血模型)
- 位置: `ecshopx-{module}/…/domain/`
- 包含: 带有 `@TableId`、`@TableField` 注解的 `@TableName` POJO
- 依赖: MyBatis-Plus 注解, Lombok
- 使用方: Mapper、服务层

**端口 / 集成层:**
- 职责: 跨模块契约 (接口在 common 中, 实现在消费模块或 bootstrap 中)
- 位置: `ecshopx-common/…/port/` (接口), `ecshopx-bootstrap/…/bootstrap/` (适配器)
- 包含: Port 接口 (如 `OrderProcessLogPublishPort`), 适配器实现
- 依赖: 无 (接口无依赖)
- 使用方: 任何模块的服务层

**调度层 (事件总线):**
- 职责: 支持重试和扇出的异步作业/事件处理
- 位置: `ecshopx-dispatch/` (基础设施), `ecshopx-common/…/dispatch/` (发布接口)
- 包含: `DispatchFacade`, Redis/Rabbit 驱动, `DispatchHandler`/`DispatchListener` 消费者
- 依赖: Redis, RabbitMQ
- 使用方: 服务发布事件; 处理器/监听器消费事件

## 数据流

### 主要管理后台 API 请求路径
1. HTTP → Undertow → Spring MVC DispatcherServlet
2. `AdminAuthInterceptor.preHandle()` 验证 JWT Bearer token (`ecshopx-espier/…/web/interceptor/AdminAuthInterceptor.java`)
3. `ActivatedInterceptor.preHandle()` 检查菜单权限 + 激活状态 (`ecshopx-espier/…/web/interceptor/ActivatedInterceptor.java`)
4. Controller 方法被调用, 从 JWT claims 和请求参数中提取 `companyId` (`ecshopx-{module}/…/api/admin/v1/*Controller.java`)
5. 服务执行业务逻辑, 所有数据库查询通过 `company_id` 限定范围 (`ecshopx-{module}/…/service/**`)
6. Mapper/Repository 执行 MyBatis-Plus 查询 (`ecshopx-{module}/…/mapper/*.java`)
7. 响应包装为 `ApiResult<T>` 或 Dingo 风格的信封 (注解驱动)

### 前端 (H5/小程序) 请求路径
1. HTTP → `FrontAuthInterceptor` 验证 H5 JWT 或小程序 session token
2. 带有 `@FrontAuth` 或 `@FrontNoAuth` 注解的 Controller 处理请求
3. 服务逻辑通过认证属性中的 `company_id` + `user_id` 限定范围
4. 使用相同的 Mapper/Repository 层进行数据库访问

### 异步事件调度路径
1. 服务调用 `DispatchPublisher.publish(payload)` (`ecshopx-common/…/dispatch/` 中的接口)
2. 实现 (在 `ecshopx-bootstrap/…/config/` 中) 调用 `DispatchFacade.publishEvent()` 或 `dispatchJob()`
3. `DispatchCore` 路由到驱动 (Redis stream 或 RabbitMQ queue)
4. `RedisDispatchConsumer` / `RabbitDispatchConsumer` 轮询并调用已注册的 `DispatchHandler` 或 `DispatchListener`
5. 处理器执行异步业务逻辑 (通知、同步、导出)

**状态管理:**
- 无状态 HTTP; 所有状态存储在 MySQL + Redis 中
- JWT claims 携带操作员身份信息 (companyId, operatorId, shopIds)
- Redis 用于 session token、缓存、限流、调度队列

## 关键抽象

**Port 接口:**
- 职责: 解耦模块; 一个模块可以调用另一个模块的逻辑而无需直接依赖
- 示例: `ecshopx-common/…/port/order/OrderProcessLogPublishPort.java`, `ecshopx-common/…/port/members/`
- 模式: 接口在 `ecshopx-common` 中, 实现在消费模块或 `ecshopx-bootstrap` 中

**调度发布器:**
- 职责: 命名的事件/作业发布契约 (每种事件类型一个接口)
- 示例: `ecshopx-common/…/dispatch/NormalOrderPaySuccessDispatchPublisher.java`
- 模式: 接口在 `ecshopx-common` 中, 实现在 `ecshopx-bootstrap/…/config/` 中装配

**FlexibleBody / FlexibleHttpServletParameterMap:**
- 职责: 兼容遗留系统的灵活请求参数解析 (PHP API 兼容性)
- 示例: `ecshopx-common/…/web/FlexibleBody.java`, `ecshopx-common/…/web/FlexibleHttpServletParameterMap.java`
- 模式: 自定义 `HandlerMethodArgumentResolver`, 将查询参数 + JSON body 合并为 `Map<String, Object>`

**BaseEntity:**
- 职责: 审计字段自动填充 (create_time, update_time 为 Unix 时间戳)
- 示例: `ecshopx-common/…/core/domain/BaseEntity.java`
- 模式: MyBatis-Plus `MetaObjectHandler` 在插入/更新时填充字段

## 入口点

**应用启动:**
- 位置: `ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/EcshopxApplication.java`
- 触发方式: `java -jar` 或 IDE 运行
- 职责: Spring 上下文初始化、组件扫描、Mapper 扫描

**管理后台 API:**
- 位置: `ecshopx-{module}/…/api/admin/v1/*Controller.java`
- 触发方式: 来自管理后台前端 (SPA) 的 HTTP 请求
- 职责: 后台管理的 CRUD 操作

**前端 API:**
- 位置: `ecshopx-{module}/…/api/front/v1/*Controller.java`
- 触发方式: 来自 H5 页面、微信小程序的 HTTP 请求
- 职责: 面向消费者的操作 (下单、商品浏览)

**OpenAPI (第三方):**
- 位置: `ecshopx-{module}/…/openapi/thirdapi/v1/` 和 `v2/`
- 触发方式: 外部系统集成 (ERP、电商平台)
- 职责: 面向第三方开发者的签名 API 调用

**定时任务:**
- 位置: `ecshopx-{module}/…/cron/`
- 触发方式: XXL-JOB 调度器 (外部管理端口 9080)
- 职责: 周期性任务 (订单过期、统计、同步)

**调度消费者:**
- 位置: `ecshopx-{module}/…/dispatch/` (处理器注册)
- 触发方式: Redis stream / RabbitMQ 消息到达
- 职责: 异步处理 (通知、导出、同步)

## 架构约束

- **线程模型:** 每个 HTTP 请求单线程 (Undertow I/O 线程 → 工作线程); 异步工作卸载到调度系统 (Redis/Rabbit)
- **全局状态:** `DispatchFacade` 和 `DispatchRegistry` 是应用级单例; Redis 连接跨租户共享
- **多租户:** 通过 `company_id` 列实现逻辑租户隔离; 不为每个租户使用独立数据库。所有查询必须包含 `company_id` 过滤条件。
- **模块耦合:** 模块通过端口接口或调度事件通信; 在同一扫描范围内的服务之间存在直接 `@Autowired` 跨模块注入 (由于是单一部署物, 这是可接受的)
- **循环引用:** 通过 `ecshopx-common` 中的端口接口缓解; 例如, 订单模块依赖会员端口接口, 而非直接依赖会员模块
- **数据库:** 所有模块/租户共享单个 MySQL 数据库实例
- **嵌入式服务器:** Undertow (非 Tomcat); 在 `application.properties` 中配置

## 多租户

**模型:** 共享数据库, 通过 `company_id` 列区分。

**租户识别:**
- 管理后台 API: 从 JWT claims 中提取 `company_id` (操作员隶属于某个公司)
- 前端 API: 从小程序/H5 认证上下文中解析 `company_id`
- OpenAPI: `company_id` 作为请求参数传递, 对 API 凭证进行验证

**关键配置:**
- `common.system-is-saas=true/false` — 控制 SaaS 模式还是单租户模式
- `common.system-companys-id=1` — 单租户模式下的默认公司 ID
- `ecshopx.common.demo-company-ids=` — 功能受限的演示租户 ID

**领域实体模式:**
```java
@TableField("company_id")
private Long companyId;
```
每个租户范围的实体都有一个 `company_id` 字段。

## 反模式

### 过度细粒度的服务
**现象:** 每个用例都有自己的 `@Service` 类 (例如 `AdminOrderCancelService`, `AdminOrderConfirmCancelService`, `AdminOrderDetailService`)。Controller 注入 20+ 个服务。
**问题所在:** 构造器参数列表变得难以管理; 相关逻辑分散在多个小文件中。
**建议做法:** 将共享大量状态/逻辑的相关用例归入单个门面服务。对于真正独立的操作, 接受当前模式。

### FlexibleBody Map 式参数传递
**现象:** Controller 接收 `FlexibleHttpServletParameterMap` (本质上是 `Map<String, Object>`) 而非类型化的 DTO。
**问题所在:** 没有编译时验证、没有自动文档生成、容易遗漏必填字段。
**建议做法:** 对于新端点, 使用带有 Jakarta Bean Validation 的类型化请求 DTO (参见 `ecshopx-companys/…/api/admin/v1/dto/` 中的新兴模式)。

## 错误处理

**策略:** 通过 `GlobalExceptionHandler` 中的 `@RestControllerAdvice` 集中处理。

**模式:**
- `ResourceException` → 422 (业务规则违反)
- `BadRequestException` → 400 (无效输入)
- `UnauthorizedException` → 401 (认证失败)
- `ForbiddenException` → 403 (权限不足)
- Controller/方法上的 `@DingoResponse` 注解控制响应信封样式 (遗留 Dingo 风格 vs 标准 ApiResult)
- 验证错误 (`MethodArgumentNotValidException`) → 422 并附带字段级错误信息

**响应信封 (标准):**
```json
{"code": 200, "msg": "success", "data": {...}}
```

**响应信封 (Dingo 遗留风格):**
```json
{"data": {"message": "...", "status_code": 422}}
```

## 横切关注点

**认证:**
- 管理后台: JWT Bearer token, 由 `AdminAuthInterceptor` 验证 (`ecshopx-espier/…/web/interceptor/AdminAuthInterceptor.java`)
- 前端/H5: JWT 由 `FrontAuthInterceptor` 验证 (`ecshopx-espier/…/web/interceptor/FrontAuthInterceptor.java`)
- 小程序: Session token 由 `WxappMemberSessionAuthenticationFilter` 验证
- OpenAPI: 基于签名的认证 (app_key + sign)
- JWT 签名: 使用 `JWT_SECRET` 属性中 base64 编码的 32 字节密钥进行 HMAC-SHA 签名

**授权:**
- 菜单级: `@Activated(routeAlias = "...")` 由 `ActivatedInterceptor` 检查
- 门店级: `@DataPass` 注解触发 `DataPassInterceptor` 进行门店范围的数据过滤
- 基于角色: 操作员 → 角色 → 权限, 在拦截器层级检查

**日志:**
- `@ShopLog` 注解触发 `ShopLogInterceptor` 进行门店操作审计日志
- `@AdminLog` 触发 `AdminLogAspect` 进行管理后台操作记录
- `@OperationLog` 触发 `OperationLogAspect` 进行业务操作审计
- SLF4J 通过 Logback (`logback-spring.xml`)

**验证:**
- 较新 DTO 上使用 Jakarta Bean Validation
- 服务中手动验证 (遗留模式, 大部分现有代码)

**国际化:**
- 通过 `CountryCodeLocaleFilter` 和 `RequestLangTag` 支持多语言
- 已配置语言列表: zh-CN, en-CN, ar-SA (默认: zh-CN)

---
*架构分析: 2026-07-08*
