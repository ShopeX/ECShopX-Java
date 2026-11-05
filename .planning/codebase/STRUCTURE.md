# 代码库结构

**分析日期：** 2026-07-08

## 目录布局

```
ecshopx-java/
├── ecshopx-bom/                    # 物料清单（依赖版本管理）
├── ecshopx-bootstrap/              # 应用入口及组装模块
├── ecshopx-common/                 # 共享抽象、注解、端口接口
├── ecshopx-dispatch/               # 事件/任务分发基础设施（Redis/RabbitMQ）
├── ecshopx-espier/                 # Web 基础设施（拦截器、JWT、存储）
│
├── ecshopx-companys/               # 公司/租户管理、操作员认证、设置
├── ecshopx-orders/                 # 订单生命周期、支付、开票、物流
├── ecshopx-goods/                  # 商品目录、分类、属性
├── ecshopx-members/                # 会员/客户管理
├── ecshopx-aftersales/             # 售后/退款处理
├── ecshopx-promotions/             # 促销、优惠券、折扣
├── ecshopx-distribution/           # 分销/经销商网络
├── ecshopx-merchant/               # 商户入驻与管理
├── ecshopx-supplier/               # 供应商管理
├── ecshopx-community/              # 社区团购
├── ecshopx-salesperson/            # 导购/销售员管理
├── ecshopx-reservation/            # 预约/预定
├── ecshopx-selfservice/            # 自助服务终端
├── ecshopx-deposit/                # 储值/预存款账户
├── ecshopx-point/                  # 积分系统
├── ecshopx-pointsmall/             # 积分商城（积分兑换商品）
├── ecshopx-popularize/             # 推广/联盟营销
├── ecshopx-comments/               # 商品评论/评价
├── ecshopx-data-cube/              # 数据分析/报表
├── ecshopx-form/                   # 自定义表单
├── ecshopx-im/                     # 即时通讯
├── ecshopx-kaquan/                 # 卡券系统
├── ecshopx-employee-purchase/      # 员工内购
├── ecshopx-one-code/               # 二维码/一码多用功能
│
├── ecshopx-payment/                # 支付抽象层
├── ecshopx-adapay/                 # AdaPay 支付集成
├── ecshopx-hfpay/                  # 汇付支付集成
├── ecshopx-bspay/                  # BS Pay 集成
├── ecshopx-chinaums-pay/           # 银联商务
│
├── ecshopx-wechat/                 # 微信小程序及公众号
├── ecshopx-work-wechat/            # 企业微信（WeCom）
├── ecshopx-ali/                    # 支付宝集成
├── ecshopx-aliyunsms/              # 阿里云短信服务
├── ecshopx-shuyun/                 # 数云 CRM 集成
├── ecshopx-youshu/                 # 有数营销集成
├── ecshopx-kujiale/                # 酷家乐家居设计集成
├── ecshopx-wsugc/                  # WSUGC 内容集成
│
├── ecshopx-openapi/                # 第三方 OpenAPI 网关
├── ecshopx-third-party/            # 外部 API 客户端（Prism、Shopex）
├── ecshopx-system-link/            # ERP 系统对接（聚水潭、旺店通）
├── ecshopx-cross-border/           # 跨境电商（海关对接）
├── ecshopx-shopmenu-border/        # 店铺菜单边界配置
├── ecshopx-tb-items/               # 淘宝商品同步
├── ecshopx-tdkset/                 # TDK（标题/描述/关键词）SEO
├── ecshopx-theme/                  # 店铺主题/页面管理
├── ecshopx-super-admin/            # 平台超级管理员（SaaS 管理）
├── ecshopx-shopex-ai/              # Shopex AI 功能
│
├── docker/                         # Docker 部署配置
├── scripts/                        # 工具脚本
├── logs/                           # 运行时日志（已 gitignore）
├── pom.xml                         # 根 Maven POM（父 POM）
└── .planning/                      # 规划文档（本目录）
```

## 模块分类

**基础设施（所有模块依赖）：**
| 模块 | 用途 |
|--------|---------|
| `ecshopx-bom` | 依赖版本管理 |
| `ecshopx-bootstrap` | Spring Boot 应用组装、端口适配器装配、分发配置 |
| `ecshopx-common` | 共享注解、异常、基础实体、端口接口、分发发布者契约 |
| `ecshopx-dispatch` | 事件总线基础设施：DispatchFacade、Redis/RabbitMQ 驱动、重试、扇出 |
| `ecshopx-espier` | Web 层基础设施：认证拦截器、JWT 安全、文件存储、上传处理 |

**核心业务：**
| 模块 | 用途 |
|--------|---------|
| `ecshopx-companys` | 租户（公司）管理、操作员认证、角色、设置、激活 |
| `ecshopx-orders` | 订单 CRUD、支付流程、物流、开票、对账、财务 |
| `ecshopx-goods` | 商品目录、商品、分类、SKU、属性、推荐 |
| `ecshopx-members` | 客户/会员档案、VIP 等级、标签 |
| `ecshopx-aftersales` | 退货/退款处理 |
| `ecshopx-promotions` | 优惠券、折扣、限时抢购、砍价 |
| `ecshopx-merchant` | 商户（多商家）管理 |

**扩展业务：**
| 模块 | 用途 |
|--------|---------|
| `ecshopx-distribution` | 多级分销/经销商网络 |
| `ecshopx-community` | 社区团购 |
| `ecshopx-salesperson` | 导购、客户关系管理 |
| `ecshopx-reservation` | 服务预约/预定 |
| `ecshopx-deposit` | 储值/预付费账户 |
| `ecshopx-point` | 积分获取/消耗逻辑 |
| `ecshopx-pointsmall` | 积分兑换商城 |
| `ecshopx-popularize` | 推广/分销员营销 |
| `ecshopx-supplier` | 供应商管理 |
| `ecshopx-employee-purchase` | 企业员工内购 |

**支付集成：**
| 模块 | 用途 |
|--------|---------|
| `ecshopx-payment` | 支付抽象层 |
| `ecshopx-adapay` | AdaPay（汇付天下 AdaPay） |
| `ecshopx-hfpay` | 汇付支付 |
| `ecshopx-bspay` | BS Pay |
| `ecshopx-chinaums-pay` | 银联商务 |

**外部集成：**
| 模块 | 用途 |
|--------|---------|
| `ecshopx-wechat` | 微信开放平台、小程序、公众号 |
| `ecshopx-work-wechat` | 企业微信（WeCom） |
| `ecshopx-ali` | 支付宝小程序 |
| `ecshopx-aliyunsms` | 阿里云短信 |
| `ecshopx-shuyun` | 数云 CRM |
| `ecshopx-youshu` | 有数营销 |
| `ecshopx-third-party` | Prism/Shopex 网关客户端 |
| `ecshopx-system-link` | 聚水潭/旺店通 ERP 集成 |
| `ecshopx-openapi` | 面向外部开发者的 OpenAPI 网关 |

**平台/管理：**
| 模块 | 用途 |
|--------|---------|
| `ecshopx-super-admin` | SaaS 平台级管理（管理公司/模板） |
| `ecshopx-theme` | 店铺页面搭建、广告位管理 |
| `ecshopx-data-cube` | 业务分析、统计报表 |

## 模块内部包结构

每个业务模块遵循一致的内部包布局：

```
ecshopx-{module}/
└── src/main/java/cn/shopex/ecshopx/{module}/
    ├── api/                    # REST 控制器
    │   ├── admin/v1/           #   后台管理端点（@AdminAuth）
    │   │   └── dto/            #   请求/响应 DTO（较新模式）
    │   └── front/v1/           #   前台客户端点（@FrontAuth）
    ├── config/                 # Spring @Configuration 配置类
    ├── cron/                   # XXL-JOB 定时任务处理器
    │   └── testcfg/            # 测试专用 cron 配置
    ├── dispatch/               # 分发事件处理器/监听器
    ├── domain/                 # 实体类（@TableName 注解）
    ├── dto/                    # 共享 DTO（跨层使用）
    ├── event/                  # Spring ApplicationEvent 类
    ├── integration/            # 跨模块集成实现
    ├── listener/               # Spring 事件监听器
    ├── mapper/                 # MyBatis-Plus Mapper 接口
    │   └── dto/                # Mapper 专用查询结果 DTO
    ├── openapi/                # OpenAPI/第三方控制器
    │   └── thirdapi/v1/        # 第三方 API 版本化端点
    ├── port/                   # 端口接口实现
    ├── repository/             # 复杂查询类（超出简单 mapper 范围）
    ├── service/                # 业务逻辑服务
    │   ├── admin/              # 管理端服务
    │   ├── front/              # 前台/客户端服务
    │   └── {subdomain}/        # 子领域分组
    └── web/                    # Web 特定工具（模块专属）
```

## 关键文件位置

**入口点：**
- `ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/EcshopxApplication.java`：主启动类
- `ecshopx-bootstrap/src/main/resources/application.properties`：主配置文件

**配置文件：**
- `ecshopx-bootstrap/src/main/resources/application.properties`：所有应用配置
- `ecshopx-bootstrap/src/main/resources/application-local.properties`：本地开发覆盖配置
- `ecshopx-bootstrap/src/main/resources/application-request-field.yml`：请求字段配置
- `ecshopx-bootstrap/src/main/resources/logback-spring.xml`：日志配置
- `pom.xml`：根 Maven POM（依赖版本、模块列表）
- `ecshopx-bootstrap/pom.xml`：Bootstrap POM（所有模块依赖）

**核心抽象：**
- `ecshopx-common/…/common/core/domain/ApiResult.java`：统一 API 响应包装类
- `ecshopx-common/…/common/core/domain/BaseEntity.java`：实体基类（审计字段）
- `ecshopx-common/…/common/core/page/PageQuery.java`：分页支持
- `ecshopx-common/…/common/exception/GlobalExceptionHandler.java`：全局异常处理
- `ecshopx-common/…/common/annotation/`：所有自定义注解（AdminAuth、FrontAuth 等）
- `ecshopx-common/…/common/dispatch/DispatchHandler.java`：异步处理器接口
- `ecshopx-common/…/common/dispatch/DispatchListener.java`：事件监听器接口

**安全/认证：**
- `ecshopx-espier/…/espier/security/EspierSecurityConfig.java`：安全配置
- `ecshopx-espier/…/espier/security/OperatorJwtAuthenticationFilter.java`：管理员 JWT 过滤器
- `ecshopx-espier/…/espier/security/WxappMemberSessionAuthenticationFilter.java`：小程序认证
- `ecshopx-espier/…/espier/web/interceptor/AdminAuthInterceptor.java`：管理员认证拦截器
- `ecshopx-espier/…/espier/web/interceptor/FrontAuthInterceptor.java`：前台认证拦截器
- `ecshopx-espier/…/espier/web/interceptor/ActivatedInterceptor.java`：权限校验
- `ecshopx-companys/…/companys/service/auth/OperatorAuthService.java`：登录/认证逻辑

**分发基础设施：**
- `ecshopx-dispatch/…/dispatch/DispatchFacade.java`：分发主 API
- `ecshopx-dispatch/…/dispatch/DispatchCore.java`：核心路由逻辑
- `ecshopx-dispatch/…/dispatch/redis/RedisDispatchDriver.java`：Redis Stream 驱动
- `ecshopx-dispatch/…/dispatch/rabbit/RabbitDispatchDriver.java`：RabbitMQ 驱动
- `ecshopx-dispatch/…/dispatch/DispatchFanOutPlanner.java`：事件扇出到多个监听器

**MyBatis 配置：**
- `ecshopx-common/…/common/config/MybatisPlusCommonConfig.java`：分页插件
- `ecshopx-common/…/common/config/MybatisPlusMetaHandler.java`：自动填充审计字段
- `ecshopx-common/…/common/mybatis/FqcnMapperBeanNameGenerator.java`：FQCN Bean 命名（解决 Mapper 冲突）

**测试：**
- `ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/config/`：集成测试分发配置
- `ecshopx-bootstrap/src/test/java/cn/shopex/ecshopx/dispatch/`：分发流程测试

## 命名约定

**模块：**
- 模式：`ecshopx-{domain-name}`（小写，连字符分隔）
- 示例：`ecshopx-orders`、`ecshopx-companys`、`ecshopx-cross-border`

**包：**
- 根包：`cn.shopex.ecshopx.{module}`
- 示例：`cn.shopex.ecshopx.orders`、`cn.shopex.ecshopx.companys`、`cn.shopex.ecshopx.goods`

**控制器：**
- 模式：`{Entity}Controller.java`，位于 `api/admin/v1/` 或 `api/front/v1/`
- 管理端：`@RestController("{module}AdminV1{Entity}")`，使用显式 Bean 名称避免冲突
- 前台端：`@RestController` 配合 `@FrontAuth` 注解
- 路由映射：`@RequestMapping("/api/v1")`（管理端和前台共享 `/api/v1` 前缀）

**服务：**
- 模式：`{Action}{Entity}Service.java` — 每个用例一个服务
- 示例：`AdminOrderListService`、`WxappOrderCreateService`、`AdminOrderCancelService`

**Mapper：**
- 模式：`{Entity}Mapper.java`，继承 `BaseMapper<{Entity}>`
- 示例：`OrdersMapper`、`ItemsCategoryProfitMapper`

**领域实体：**
- 模式：`{TableNameInCamelCase}.java`，使用 `@TableName("{snake_case_table}")` 注解
- 示例：`ItemsCategory`、`Companys`、`NormalOrdersRelZiti`

**DTO：**
- 模式：`{Purpose}Request.java` / `{Purpose}Data.java`，位于 `api/.../dto/`
- 示例：`WxLiteLoginRequest`、`UpdateCompanyInfoData`

**分发发布者：**
- 模式：`{EventName}DispatchPublisher.java`（common 中的接口）
- 模式：`{EventName}DispatchPublisherImpl.java`（bootstrap/config 中的实现）

## 新代码添加指南

**在现有模块中添加新业务功能：**
1. 领域实体：`ecshopx-{module}/…/{module}/domain/{Entity}.java`
2. Mapper：`ecshopx-{module}/…/{module}/mapper/{Entity}Mapper.java`
3. 服务：`ecshopx-{module}/…/{module}/service/{subdomain}/{ActionNameService}.java`
4. 控制器（管理端）：`ecshopx-{module}/…/{module}/api/admin/v1/{Entity}Controller.java`
5. 控制器（前台）：`ecshopx-{module}/…/{module}/api/front/v1/{Entity}Controller.java`
6. XML Mapper（复杂 SQL 时）：`ecshopx-{module}/src/main/resources/mapper/{Entity}Mapper.xml`

**新增模块：**
1. 创建 `ecshopx-{new-module}/` 目录，使用标准 Maven 布局
2. 在根 `pom.xml` 中添加 `<module>ecshopx-{new-module}</module>`
3. 在 `ecshopx-bootstrap/pom.xml` 中添加 `<dependency>`
4. 在根 `<dependencyManagement>` 中添加版本管理
5. 包名：`cn.shopex.ecshopx.{newmodule}`
6. 遵循内部包结构：`api/`、`domain/`、`mapper/`、`service/` 等

**新增跨模块端口接口：**
1. 接口：`ecshopx-common/…/common/port/{domain}/{InterfaceName}Port.java`
2. 实现：`ecshopx-bootstrap/…/bootstrap/{domain}/{InterfaceName}PortAdapter.java`

**新增分发事件/任务：**
1. 发布者接口：`ecshopx-common/…/common/dispatch/{EventName}DispatchPublisher.java`
2. 发布者实现：`ecshopx-bootstrap/…/config/{EventName}DispatchPublisherImpl.java`
3. 处理器：`ecshopx-{module}/…/{module}/dispatch/{EventName}Handler.java`
4. 注册配置：`ecshopx-bootstrap/…/config/{EventName}RegistrationConfig.java`

**新增定时任务：**
1. 处理器：`ecshopx-{module}/…/{module}/cron/{JobName}Handler.java`
2. 在 XXL-JOB 管理后台注册（外部操作）

**新增请求 DTO（新端点推荐方式）：**
1. DTO 类：`ecshopx-{module}/…/{module}/api/admin/v1/dto/{ActionRequest}.java`
2. 使用 Jakarta Bean Validation 注解（`@NotNull`、`@Size` 等）

## 特殊目录

**`ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/`：**
- 用途：分发发布者实现和监听器注册配置
- 是否自动生成：否
- 是否提交：是
- 说明：所有跨模块装配在此进行（端口适配器、事件注册）

**`ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/bootstrap/`：**
- 用途：桥接模块边界的端口适配器实现
- 是否自动生成：否
- 是否提交：是

**`ecshopx-common/…/common/port/`：**
- 用途：跨模块契约接口（依赖反转）
- 是否自动生成：否
- 是否提交：是
- 说明：仅存放接口；实现位于消费模块或 bootstrap 中

**`ecshopx-common/…/common/dispatch/`：**
- 用途：分发发布者接口和事件/任务名称常量
- 是否自动生成：否
- 是否提交：是

**`logs/`：**
- 用途：运行时应用日志
- 是否自动生成：是
- 是否提交：否（已 gitignore）

**`docker/`：**
- 用途：Docker/部署配置
- 是否自动生成：否
- 是否提交：是

**`scripts/`：**
- 用途：工具/运维脚本
- 是否自动生成：否
- 是否提交：部分（当前为未跟踪状态）

**`src/main/resources/mapper/`：**
- 用途：MyBatis XML Mapper 文件（复杂 SQL 查询）
- 是否自动生成：否
- 是否提交：是
- 说明：仅部分模块使用 XML Mapper；大多数使用注解方式或 BaseMapper 方法

---
*结构分析：2026-07-08*
