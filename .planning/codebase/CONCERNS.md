# 代码库关注事项

**分析日期:** 2026-07-08

## 技术债务

**大量使用 `Map<String, Object>` 替代类型化 DTO：**
- 问题：代码库中有超过 31,600 处 `Map<String, Object>` 的使用。控制器接收和返回无类型的 map，服务层之间传递 map，认证令牌也是 map。这消除了编译时的类型安全性，使重构变得危险，并阻碍了 IDE 导航。
- 文件：`ecshopx-members/src/main/java/cn/shopex/ecshopx/members/api/front/v1/MembersController.java`、`ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/auth/OperatorAuthService.java`，以及几乎所有 service/controller 文件
- 影响：运行时 ClassCastException 风险高，无法对输入进行结构化验证，无法生成 OpenAPI/Swagger 文档，新开发人员必须阅读完整实现才能理解方法契约
- 修复方案：逐步为每个端点引入类型化的请求/响应 DTO。从高流量 API（members、orders、goods）开始。使用 `@FlexibleBody` 配合具体类型替代 `Map<String, Object>`。

**上帝类 — 单个文件超过 1000-3000 行：**
- 问题：53 个 Java 文件超过 30KB。最大的文件（`MembersController.java`）有 3012 行。诸如 `CommonLangModReadService.java`（1878 行）、`CommunityActivityService.java`（2192 行）、`MemberAccountService.java`（2561 行）等服务违反了单一职责原则。
- 文件：
  - `ecshopx-members/src/main/java/cn/shopex/ecshopx/members/api/front/v1/MembersController.java`（3012 行）
  - `ecshopx-members/src/main/java/cn/shopex/ecshopx/members/service/account/MemberAccountService.java`（2561 行）
  - `ecshopx-community/src/main/java/cn/shopex/ecshopx/community/service/CommunityActivityService.java`（2192 行）
  - `ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/service/config/ConfigRequestFieldsApplicationService.java`（1985 行）
  - `ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/CommonLangModReadService.java`（1878 行）
- 影响：难以测试、难以理解、合并冲突频繁、类加载时间长
- 修复方案：按领域概念拆分子服务。例如，将 `MembersController` 拆分为 `MemberProfileController`、`MemberPointController`、`MemberCouponController` 等。

**大范围 `catch (Exception e)` — 1,683 处：**
- 问题：代码库中广泛使用宽泛的异常捕获，吞没了特定异常，使调试变得困难，并可能隐藏 bug。
- 文件：所有模块中普遍存在
- 影响：静默失败、丢失堆栈跟踪、生产环境中难以诊断的问题
- 修复方案：捕获特定异常。使用 `ecshopx-common/src/main/java/cn/shopex/ecshopx/common/exception/GlobalExceptionHandler.java` 中已有的 `GlobalExceptionHandler`，让未预期的异常向上传播。

**333 处分散在代码库中的 `new ObjectMapper()`：**
- 问题：ObjectMapper 创建成本高，应作为共享单例使用。每次调用创建新实例浪费内存和 CPU，且存在序列化配置不一致的风险。
- 文件：分布在所有模块中
- 影响：高负载下性能下降，不同 ObjectMapper 实例之间 JSON 序列化行为不一致
- 修复方案：通过构造函数注入 Spring 自动配置的 `ObjectMapper` Bean。移除所有 `new ObjectMapper()` 调用。

**几乎没有输入验证（12 处 `@Valid`/`@Validated` 对比 1200 处 `@FlexibleBody` 使用）：**
- 问题：自定义的 `@FlexibleBody` 注解（位于 `ecshopx-common/src/main/java/cn/shopex/ecshopx/common/web/FlexibleBody.java`）处理请求解析，但几乎没有端点使用 `@Valid` 或 `@Validated` 进行结构化验证。结合 `Map<String, Object>` 输入，编译时和运行时都没有输入验证。
- 文件：项目中所有 431 个控制器类
- 影响：无效数据到达服务层和数据库。业务规则违反被延迟发现。存在数据损坏的可能性。
- 修复方案：引入类型化 DTO 时，添加 Jakarta Validation 注解（`@NotNull`、`@Size`、`@Pattern` 等），并将 `@FlexibleBody` 与 `@Valid` 配合使用。

**JdbcTemplate 与 MyBatis-Plus 并存（975 处直接使用）：**
- 问题：两种数据访问模式共存但缺乏清晰的分离。`CommonLangModReadService` 使用 `NamedParameterJdbcTemplate` 配合字符串拼接 SQL，而其他代码使用 MyBatis-Plus 的 `LambdaQueryWrapper`。这导致数据层不一致。
- 文件：`ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/CommonLangModReadService.java`、`ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/companycreate/CompanyInitDemoDataService.java`
- 影响：查询模式不一致、难以审计 SQL 注入、无法统一缓存策略
- 修复方案：将 JdbcTemplate 查询迁移到 MyBatis-Plus 或专用的 mapper XML 文件以保持一致性。仅对真正动态的多表操作保留 JdbcTemplate。

## 已知缺陷

**MyBatis SQL 日志在生产配置中被启用：**
- 现象：`mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl` 设置在 `application.properties`（基础配置，不仅仅是本地配置）中。这会在所有环境中将每条 SQL 语句输出到 stdout。
- 文件：`ecshopx-bootstrap/src/main/resources/application.properties:31`
- 触发条件：生产环境中的每一次数据库查询
- 临时方案：应从基础配置中移除；仅在 local/dev profile 中启用。

**业务逻辑中的 TODO 和占位常量：**
- 现象：佣金计划关闭时间计算使用硬编码占位符而非商户特定配置。
- 文件：`ecshopx-popularize/src/main/java/cn/shopex/ecshopx/popularize/service/BrokeragePlanCloseTimeService.java:20`
- 触发条件：任何佣金计划结算
- 临时方案：无文档记录 — 目前使用占位值。

## 安全考量

**源代码中硬编码加密密钥：**
- 风险：一个静态 base64 编码密钥 `"YW53dWx1eWFudGFuZw"`（解码为 `"anwuluyantang"`）直接硬编码在 Java 源码中用于许可证加密/激活。这已提交到 git，任何有仓库访问权限的人都可见。
- 文件：
  - `ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/auth/CompanysActivationCipher.java:80`
  - `ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/activation/CompanysLicensePayloadEncryptor.java:47`
- 当前缓解措施：无
- 建议：迁移到外部化密钥（环境变量或保险库）。轮换密钥。审计使用此密钥加密的数据。

**配置文件中提交了密钥（target/ 和源码）：**
- 风险：`application-local.properties` 在 `.gitignore` 中但仍存在于工作目录（和 `target/` 输出）中。该文件包含明文数据库密码、Redis 密码、OSS 密钥、第三方 API 密钥（聚水潭、Prism、数云）。多个 `application-local copy.properties` 和 `application-local1.properties` 文件未被 gitignore 且出现在未跟踪文件列表中。
- 文件：
  - `ecshopx-bootstrap/src/main/resources/application-local.properties`（已 gitignore 但存在）
  - `ecshopx-bootstrap/src/main/resources/application-local1.properties`（未 gitignore，已跟踪）
  - `ecshopx-bootstrap/target/classes/application-local copy.properties`（在 target/ 中）
- 当前缓解措施：`.gitignore` 仅覆盖 `application-local.properties`
- 建议：将 `application-local1.properties` 和 `*copy*` 模式添加到 `.gitignore`。永远不要提交密钥；使用环境变量或密钥管理器。运行 `git filter-branch` 或 BFG 清除历史记录。

**安全过滤器链使用 `permitAll` 作为兜底规则：**
- 风险：`EspierSecurityConfig.java` 中的默认 `SecurityFilterChain` 对 wxapp 链未匹配的所有 `/api/**` 请求放行。管理端/前端/商户 API 的认证完全依赖自定义拦截器（`@AdminAuth`、`@FrontAuth`）。如果某个端点缺少注解，它将完全无保护。
- 文件：`ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/EspierSecurityConfig.java:39-43`
- 当前缓解措施：自定义注解（`@AdminAuth`、`@FrontAuth` 等）应用于每个控制器方法 — 431 个控制器中有 540 个带注解的方法
- 建议：切换为默认拒绝。显式白名单公开端点。使用 Spring Security 的路径规则作为主要网关，注解作为次要手段。

**MyBatis `${}` 动态列名存在 SQL 注入风险：**
- 风险：`ItemsWxappFilterFacetMapper.xml` 在 WHERE 子句中使用 `${c.column}` 作为动态列名。如果 `c.column` 来源于用户输入且未经严格白名单验证，则存在 SQL 注入风险。
- 文件：`ecshopx-goods/src/main/resources/mapper/ItemsWxappFilterFacetMapper.xml:27-50`
- 当前缓解措施：未知 — 取决于上游是否对 `c.column` 进行了验证
- 建议：在传递给 mapper 之前，对 `c.column` 进行严格的已知列名白名单验证。

**Undertow 允许 URL 中的未转义字符：**
- 风险：`server.undertow.options.server.allow-unescaped-characters-in-url=true` 削弱了 URL 解析安全性。这可能便于路径遍历或基于编码的攻击。
- 文件：`ecshopx-bootstrap/src/main/resources/application.properties:15`
- 当前缓解措施：无
- 建议：审查为何需要此配置。如果是为了查询字符串中的中文字符，应在客户端处理编码或使用正确的 URL 编码。

**全局禁用 CSRF：**
- 风险：两个安全过滤器链都禁用了 CSRF（`csrf.disable()`）。对于一个带有管理面板的多租户 SaaS 平台，这增加了基于会话认证流程的 CSRF 攻击面。
- 文件：`ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/security/EspierSecurityConfig.java:31,41`
- 当前缓解措施：基于 JWT 的认证（无状态）降低了 API 调用的 CSRF 风险，但任何基于 cookie 的会话流程仍然脆弱
- 建议：如果所有认证都是基于令牌的（Bearer header），禁用 CSRF 是可接受的。记录此假设。验证不存在基于 cookie 的认证流程。

## 性能瓶颈

**无连接池调优（使用默认 HikariCP 设置）：**
- 问题：未找到 `spring.datasource.hikari.*` 配置。拥有 50+ 模块、2400+ 端点和大量数据库使用的情况下，默认 Hikari 连接池（10 个连接）在生产负载下可能不足。
- 文件：`ecshopx-bootstrap/src/main/resources/application.properties`（缺少 hikari 配置）
- 原因：依赖 HikariCP 默认值（maximumPoolSize=10）
- 改进方案：根据预期并发量配置 `spring.datasource.hikari.maximum-pool-size`、`minimum-idle`、`connection-timeout` 和 `idle-timeout`。

**无应用级缓存（零 `@Cacheable` 使用）：**
- 问题：尽管已配置 Redis，但没有任何 Spring Cache 注解（`@Cacheable`、`@CacheEvict`）。所有数据访问直接命中数据库。对于一个包含商品目录/定价读取的多租户电商平台，这是一个重大性能缺口。
- 文件：所有模块的所有服务类
- 原因：从未在应用层实现缓存策略
- 改进方案：对频繁读取、很少变更的数据（公司设置、商品目录、促销规则）添加 `@Cacheable`。使用 Redis 作为缓存存储。

**使用 Thread.sleep() 进行限流/背压：**
- 问题：生产代码中使用 `Thread.sleep(50L)` 和 `Thread.sleep(500L)` 来节流批处理操作。这会阻塞线程池中的线程。
- 文件：
  - `ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/service/config/ConfigRequestFieldsApplicationService.java:1172,1281,1348`
  - `ecshopx-kaquan/src/main/java/cn/shopex/ecshopx/kaquan/service/discount/UserDiscountExchangeItemRedisLockService.java:31`
  - `ecshopx-members/src/main/java/cn/shopex/ecshopx/members/dispatch/PushMemberTagRelationJobHandler.java:45`
- 原因：简单粗暴的限流方式
- 改进方案：使用响应式调度、`ScheduledExecutorService` 或限流库（Resilience4j、Guava RateLimiter）。

**14 个配置相似的独立线程池执行器：**
- 问题：`AsyncExecutorConfig.java` 定义了 14 个几乎相同的线程池（core=2, max=4, queue=200-500）。每个池即使空闲也会持有线程。总计：最多 56 个线程保留用于异步任务。
- 文件：`ecshopx-bootstrap/src/main/java/cn/shopex/ecshopx/config/AsyncExecutorConfig.java`
- 原因：每个功能区域一个池，缺乏共享执行器策略
- 改进方案：合并为 2-3 个池（快速/慢速/导出），合理调整大小。或使用共享的 `VirtualThreadPerTaskExecutor`（Java 21+ 迁移路径）。

## 脆弱区域

**通过动态表名实现多语言支持（CommonLangModReadService）：**
- 文件：`ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/CommonLangModReadService.java`（1878 行）
- 为何脆弱：动态构造表名（`OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(lang)`）并使用字符串拼接针对 `NamedParameterJdbcTemplate` 构建原始 SQL。添加新语言需要新的物理表。逻辑在多个方法变体（CN、EN、AR 等）中重复。
- 安全修改：`normalizeLangTableSuffix()` 方法（第 1379 行）使用正则 `[A-Za-z0-9]+` 验证 — 这对注入是安全的，但对演进是脆弱的。
- 测试覆盖：未找到此服务的测试。

**基于拦截器的认证安全模型：**
- 文件：
  - `ecshopx-espier/src/main/java/cn/shopex/ecshopx/espier/web/interceptor/AdminAuthInterceptor.java`
  - `ecshopx-common/src/main/java/cn/shopex/ecshopx/common/annotation/AdminAuth.java`
  - `ecshopx-common/src/main/java/cn/shopex/ecshopx/common/annotation/FrontAuth.java`
- 为何脆弱：安全性依赖于开发人员记得在每个控制器方法上添加正确的注解。缺少注解意味着完全开放访问（由于 `permitAll` 兜底规则）。没有编译时强制机制。
- 安全修改：创建新端点时始终添加认证注解。审计缺失注解的情况。
- 测试覆盖：没有集成测试验证每个端点的认证执行。

**订单模块（857 个 Java 文件）与 12+ 个其他模块耦合：**
- 文件：`ecshopx-orders/src/main/java/` — 导入来源：supplier (99)、members (97)、companys (68)、espier (55)、aftersales (54)、chinaumspay (43)、point (27)、payment (24)、hfpay (20)、thirdparty (9)、deposit (8)、superadmin (7)
- 为何脆弱：12 个依赖模块中任何一个的变更都可能破坏订单模块。订单模块是系统的依赖中心。
- 安全修改：在订单与其依赖之间引入基于接口的边界（端口/适配器模式）。
- 测试覆盖：857 个源文件对应 255 个测试文件（~30% 文件覆盖率，行覆盖率不明）。

## 扩展性限制

**单体部署 — 52 个模块打包为单个 JAR：**
- 当前容量：所有 52 个模块编译为一个 Spring Boot fat JAR，作为单进程部署
- 限制：无法单独扩展各组件（例如，扩展订单处理就必须同时扩展商品目录）。启动时间随模块数量增长。任何模块的 bug 都会导致整个平台宕机。
- 扩展路径：将高流量模块（orders、goods、members）提取为独立服务。使用已有的 `ecshopx-dispatch` 模块作为事件总线边界。

**所有租户和所有模块共用单个 MySQL 数据库：**
- 当前容量：一个数据库（`ecshopx`）承载所有表、所有租户、所有模块
- 限制：数据库成为瓶颈。数据库层面无租户隔离。Schema 迁移同时影响所有租户。
- 扩展路径：为报表（data-cube）引入只读副本。考虑对大租户进行租户级数据库分片。

**无消息队列 — 直接同步耦合：**
- 当前容量：模块间通信为直接方法调用（无 RabbitMQ、Kafka 等）
- 限制：无背压、无重试语义、生产者与消费者之间无解耦。XXL-Job 调度器仅处理定时任务，不处理事件驱动流程。
- 扩展路径：为异步操作（订单事件、库存更新、通知分发）引入消息代理。

## 存在风险的依赖

**wxjava 版本使用带时间戳的 SNAPSHOT（`4.7.6-20250628.205748`）：**
- 风险：这似乎是微信 Java SDK 的本地构建或内部发布的快照版本。无法从 Maven Central 公共仓库复现。如果内部制品仓库丢失，构建将失败。
- 影响：6 个 weixin-java 模块（common、miniapp、open、mp、cp、pay）依赖此版本
- 迁移方案：固定到 Maven Central 的官方发布版本，或记录内部制品仓库及如何重建此版本。

**Spring Boot 补丁版本未锁定：**
- 风险：父 POM 使用 `spring-boot-starter-parent:3.5.12`。`.12` 补丁版本非常新。强制更新场景可能引入回归问题。
- 影响：所有模块继承此版本
- 迁移方案：目前可接受，但需记录升级测试流程并在 CI 中固定版本。

## 缺失的关键功能

**无数据库迁移工具（Flyway/Liquibase）：**
- 问题：未检测到 Flyway 或 Liquibase。Schema 变更通过手动 SQL 脚本管理（`docker/ecshopx.sql` — 17,093 行）。没有版本化、可审计的迁移历史。
- 阻碍：安全的自动化部署、回滚能力、环境间 schema 漂移检测
- 修复方案：引入 Flyway，以当前 schema 作为基线迁移。所有未来变更作为版本化迁移脚本。

**无 API 文档（Swagger/SpringDoc）：**
- 问题：未检测到 springdoc-openapi 或 Swagger 依赖。拥有 2,400+ 端点和 `Map<String, Object>` 输入的情况下，API 在代码层面完全无文档。
- 阻碍：前端团队生产力、第三方集成、API 测试自动化
- 修复方案：添加 `springdoc-openapi-starter-webmvc-ui`。随着 DTO 的引入，逐步添加 `@Operation` 和 `@Schema` 注解。

**无 README 或开发者文档：**
- 问题：根目录仅存在 `readme_cn.md`。无模块级文档、无架构图、无贡献指南、无 API 文档。
- 阻碍：新开发人员入职、理解跨模块职责
- 修复方案：为每个模块创建 README 文件，描述用途、关键类和配置。

## 测试覆盖缺口

**测试覆盖率极低（7,531 个源文件对应 876 个测试文件 — 11.6% 文件比率）：**
- 未测试内容：绝大多数业务逻辑，特别是：
  - `ecshopx-goods` 模块：537 个源文件，仅 14 个测试文件
  - `ecshopx-members` 模块：413 个源文件，仅 29 个测试文件
  - `ecshopx-promotions` 模块：416 个源文件，仅 46 个测试文件
  - `ecshopx-companys` 模块：395 个源文件，仅 10 个测试文件
  - `ecshopx-distribution` 模块：339 个源文件，仅 7 个测试文件
- 文件：大多数测试文件集中在 `ecshopx-bootstrap`（270）和 `ecshopx-orders`（255）。许多模块没有测试。
- 风险：回归问题无法被检测。重构风险大。代码库抗拒变更。
- 优先级：高 — 从支付、认证和订单创建流程开始。

**无集成/端到端测试：**
- 未测试内容：跨模块流程（订单创建 -> 支付 -> 库存扣减 -> 通知）、API 契约测试、多租户隔离
- 文件：所有测试文件似乎都是单元测试
- 风险：模块间交互在生产环境中静默失败
- 优先级：高 — 为关键业务流程（订单生命周期、支付回调、会员注册）引入集成测试。

## 配置管理

**环境配置依赖基于文件的覆盖：**
- `application-local.properties` 模式用于本地开发（已 gitignore）。基础 `application.properties` 包含开发默认值（localhost DB、localhost Redis、明文密码）。生产环境覆盖大概通过环境变量或外部配置注入，但这未被记录。
- docker-compose 使用 `APP_ARGS` 配合 `--spring.*` CLI 覆盖 — 可行但对于大量属性来说较为脆弱。
- 缺失：生产部署所需环境变量的文档。

**基础配置中的开发模式默认值：**
- `ecshopx.is-dev-mode=false` 已设置，但 `mybatis-plus.configuration.log-impl=StdOutImpl`（详细 SQL 日志）被无条件启用。`JWT_SECRET` 在 `application.properties` 中有硬编码默认值。这些应仅存在于特定 profile 的配置中。

---

*关注事项审计：2026-07-08*
