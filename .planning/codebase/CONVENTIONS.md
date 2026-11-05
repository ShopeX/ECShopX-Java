# 编码规范

**分析日期:** 2026-07-08

## 命名规范

**包名:**
- 根包: `cn.shopex.ecshopx.{module}`
- 按层级划分子包: `api`, `service`, `domain`, `mapper`, `config`, `cron`, `dispatch`, `integration`, `repository`, `port`
- API 层子包: `api.admin.v1`, `api.front.v1` (admin = 运营端, front = H5/小程序端)
- API 层下的 DTO: `api.admin.v1.dto`
- Service 层下的 DTO: `service.dto`

**类名:**
- 控制器: `{Entity}Controller.java` — 例如 `PagesTemplateController`, `OrderController`, `ItemsController`
- 服务类: 具体类（不使用接口+实现模式）。按具体操作命名: `PagesTemplateListService`, `PagesTemplateAddService`, `AdminOrderDetailService`
- 领域实体: 大驼峰名词 — `PagesTemplate`, `Operators`, `DistributorWechatRel`
- Mapper: `{Entity}Mapper.java` — `PagesTemplateMapper`, `OperatorsMapper`
- DTO/请求对象: `{Entity}{Action}Request.java` — `PagesTemplateAddRequest`, `PagesTemplateCopyRequest`
- 查询对象 (record): `{Entity}{Context}Query.java` — `PagesTemplateListQuery`, `PagesTemplateWidgetItemsQuery`
- 编排器（复杂流程）: `{Entity}{Action}Orchestrator.java` — `ItemsCreateOrchestrator`, `DistributorCreateOrchestrator`
- 定时任务处理器: `Schedule{Action}Handler.java` — `ScheduleEnableTemplateHandler`, `ScheduleRecordStatisticsHandler`
- 分发处理器: `{Action}JobHandler.java` — `PagesTemplateSyncJobHandler`
- 集成适配器: `{Port}Impl.java` 或 `{Name}PortAdapter.java`

**方法名:**
- 小驼峰命名
- Service 方法使用动词: `lists()`, `add()`, `detail()`, `copy()`, `delete()`, `modifyStatus()`
- Controller 方法与端点操作匹配: `getOrderList()`, `getOrderDetail()`, `add()`, `edit()`

**变量名:**
- 局部变量和字段使用小驼峰
- 数据库列名在注解中使用 snake_case: `@TableField("company_id")`
- 常量: UPPER_SNAKE_CASE — `private static final String KEYWORDS_VALIDATION_MSG = "参数必填.";`

**文件:**
- 每个文件一个类
- 文件名与类名完全一致

## 代码风格

**格式化:**
- 未配置自动格式化工具（未检测到 Spotless、Checkstyle 或 google-java-format 插件）
- 缩进: 4 个空格（从源码中观察得出）
- 左大括号在同一行
- 右大括号独占一行
- 方法之间空一行
- 最大行宽: 实际观察约 120 字符

**代码检查:**
- Maven 中未配置代码检查插件
- 依赖 IDE 设置和代码评审

## Import 组织

**顺序（从源文件中观察）:**
1. `cn.shopex.ecshopx.*`（项目内部导入）
2. 第三方库（`com.baomidou.*`, `com.fasterxml.*`, `ch.qos.logback.*`）
3. `jakarta.*` / `java.*`（标准库和 Jakarta EE）
4. `org.springframework.*`（Spring Framework）
5. `org.apache.*`（Apache 库）

**规则:**
- 不使用通配符导入（`*`）— 始终使用显式类导入
- 测试文件中静态导入放在顶部: `import static org.mockito.Mockito.*`
- 不应有未使用的导入（虽然没有工具强制执行）

## 控制器结构

**类级注解（典型的 admin 控制器）:**
```java
@AdminAuth                          // 管理端 JWT 认证
@ShopLog                            // 操作日志记录
@RestController("uniqueBeanName")   // Bean 名称用于跨模块去重
@RequestMapping("/api/v1/...")      // 带版本号的基础路径
public class XxxController { ... }
```

**可选的类级注解:**
```java
@DingoResponse(                     // 错误响应格式控制
    resource = DingoResponse.ResourceStyle.DINGO,
    badRequest = DingoResponse.BadRequestStyle.DINGO_422,
    unauthorized = true,
    notFound = false)
@Activated(routeAlias = "...")      // 权限校验（可用于类或方法级别）
```

**前端（H5/小程序）控制器使用:**
```java
@FrontNoAuth                        // 无需 JWT（公开接口）
@FrontAuth                          // H5 前端 JWT 认证
@FrontMerchantAuth                  // 商户前端认证
```

**方法级模式:**
```java
@Activated(routeAlias = "pagestemplate.lists")
@GetMapping(value = "/lists", name = "模板列表")
public ApiResult<Map<String, Object>> lists(HttpServletRequest request, ...) {
    // 1. 从请求属性中提取 JWT
    Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
    // 2. 验证 JWT Map
    if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
        throw new UnauthorizedException("...");
    }
    // 3. 从 JWT 中提取 companyId
    Map<String, Object> jwt = toStringKeyMap(m);
    long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
    // 4. 委托给 Service 处理
    return ApiResult.ok(someService.doWork(companyId, ...));
}
```

**请求体绑定:**
- 使用 `@FlexibleBody` 注解（自定义）替代 `@RequestBody` — 支持 JSON、multipart 和 form-urlencoded
- 使用 `@RequestParam` 接收查询参数
- 使用 `@PathVariable` 接收路径参数

**响应模式:**
- 包装为 `ApiResult.ok(data)` → `{"code":200,"msg":"success","data":...}`
- 部分接口直接返回 `ResponseEntity<Map<String, Object>>`
- 返回类型通常为 `ApiResult<Map<String, Object>>` 或 `ApiResult<Object>`

## 服务类结构

**模式:** 直接使用 `@Service` 注解的具体类（不使用接口+实现模式）。

```java
@Service
public class PagesTemplateListService {

    private final PagesTemplateMapper pagesTemplateMapper;
    private final SomeOtherService anotherService;

    // 构造器注入（显式构造器或 @RequiredArgsConstructor）
    public PagesTemplateListService(PagesTemplateMapper mapper, ...) {
        this.pagesTemplateMapper = mapper;
        ...
    }

    public Map<String, Object> lists(PagesTemplateListQuery query, String locale) {
        // 使用 MyBatis-Plus LambdaQueryWrapper 编写业务逻辑
        LambdaQueryWrapper<PagesTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PagesTemplate::getCompanyId, query.companyId())
               .isNull(PagesTemplate::getDeletedAt)
               .orderByDesc(PagesTemplate::getCreatedAt);
        // ...
    }
}
```

**服务命名方式:** 每个操作/用例一个服务类（细粒度）:
- `PagesTemplateAddService` — 处理新增
- `PagesTemplateListService` — 处理列表查询
- `PagesTemplateDeleteService` — 处理删除
- `PagesTemplateEditService` — 处理编辑

**依赖注入:**
- 构造器注入（首选模式）
- 部分服务使用 `@RequiredArgsConstructor`（Lombok），其他显式编写构造器
- 使用 `@Qualifier` 消除 Bean 歧义: `@Qualifier("companysRedisTemplate")`

**多租户:** 每个服务方法的第一个参数为 `companyId`（租户隔离）。

## 实体/领域模型规范

**模式:**
```java
@Data                              // Lombok: getters, setters, equals, hashCode, toString
@TableName("pages_template")       // MyBatis-Plus: 映射到数据库表
public class PagesTemplate {

    @TableId(value = "pages_template_id", type = IdType.AUTO)
    private Long pagesTemplateId;

    @TableField("company_id")
    private Long companyId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;  // 软删除模式
}
```

**规范:**
- 所有实体使用 `@Data`（Lombok）
- ID 字段类型为 `Long`，文本为 `String`，标记/时间戳为 `Integer`（部分遗留代码使用 Unix 时间戳）
- 新代码使用 `LocalDateTime` 表示日期；旧代码使用 `Integer`（Unix 秒级时间戳）
- 字段级设置默认值: `private Integer status = 2;`
- 字段使用 Javadoc 注释描述含义: `/** 启用状态 1启用 2未启用，默认 2 */`
- 通过 `deleted_at` 列实现软删除（可为空的 `LocalDateTime`）
- 所有实体包含 `company_id` 用于多租户

## MyBatis Mapper 规范

**简单 Mapper（最常见）:**
```java
@Mapper
public interface PagesTemplateMapper extends BaseMapper<PagesTemplate> {
}
```

**扩展 Mapper（自定义查询）:**
```java
@Mapper
public interface PagesAdPlaceRelMemberTagsMapper extends BaseMapper<PagesAdPlaceRelMemberTags> {
    List<PagesAdPlaceTagJoinRow> selectRelTagsByAdPlaceIds(@Param("ids") List<Long> ids);
}
```

**XML Mapper:** 位于 `src/main/resources/mapper/{MapperName}.xml`。用于复杂的关联查询、动态 SQL 和批量操作。

**服务中的查询构建:** 在 Service 代码中使用 `LambdaQueryWrapper<T>`:
```java
LambdaQueryWrapper<Operators> opQ = new LambdaQueryWrapper<>();
opQ.eq(Operators::getCompanyId, companyId)
   .eq(Operators::getMobile, mobile)
   .eq(Operators::getOperatorType, "staff")
   .last("LIMIT 1");
Operators staff = operatorsMapper.selectOne(opQ);
```

## 异常处理

**自定义异常层次结构:**
- `BadRequestException` — 400，参数校验错误
- `UnauthorizedException` — 401，认证失败
- `ForbiddenException` — 403，授权失败
- `ResourceException` — 422，业务逻辑错误（领域级问题中最常用）

**在 Service 代码中直接抛出:**
```java
if (operator == null || operator.isEmpty()) {
    throw new ResourceException("账号不存在", 403);
}
if (!BCrypt.checkpw(password, hash)) {
    throw new ResourceException("账号密码错误，请重新登录", 403);
}
```

**`GlobalExceptionHandler`**（`ecshopx-common`）统一处理所有异常。控制器上的 `@DingoResponse` 注解配置每个接口的响应格式。

**字段级校验错误:**
```java
throw new BadRequestException("参数校验失败", Map.of("field_name", List.of("validation.required")));
```

## DTO/VO 模式

**API 请求 DTO（Lombok + Jackson）:**
```java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagesTemplateAddRequest {
    @JsonProperty("regionauth_id")
    private Long regionauthId;
    @JsonProperty("template_name")
    private String templateName;
}
```

**Service 层查询 DTO（Java record，不可变）:**
```java
public record PagesTemplateListQuery(
    long companyId,
    int distributorId,
    String weappPages,
    boolean regionauthIdPresent,
    Long regionauthId,
    int page,
    int pageSize) {}
```

**响应:** 通常使用 `Map<String, Object>` — 不使用类型化的 VO 类。Service 返回的 Map 使用 snake_case 键名，以匹配遗留 PHP API 的契约。

## 常用注解

| 注解 | 位置 | 用途 |
|------|------|------|
| `@AdminAuth` | 控制器类/方法 | 管理端 JWT 认证拦截器 |
| `@FrontAuth` | 控制器类/方法 | 前端 JWT 认证 |
| `@FrontNoAuth` | 控制器类/方法 | 公开接口（无需认证） |
| `@Activated(routeAlias="...")` | 控制器方法 | 权限 + 菜单校验 |
| `@ShopLog` | 控制器类 | 操作审计日志 |
| `@DingoResponse(...)` | 控制器类/方法 | 错误响应格式 |
| `@DataPass` | 控制器方法 | 数据权限过滤 |
| `@FlexibleBody` | 方法参数 | 多内容类型请求体绑定 |
| `@Service` | 服务类 | Spring Bean |
| `@Mapper` | Mapper 接口 | MyBatis Mapper |
| `@Data` | 领域类 | Lombok |
| `@TableName` | 领域类 | MyBatis-Plus 表映射 |
| `@TableId` | 领域字段 | 主键 |
| `@TableField` | 领域字段 | 列映射 |
| `@RequiredArgsConstructor` | 服务/控制器类 | Lombok 构造器注入 |
| `@Value("${...}")` | 服务字段 | 配置属性注入 |

## 模块设计

**导出:**
- 各模块通过直接依赖向其他模块暴露服务和领域类
- 无桶文件或显式模块 API — 模块间通过具体类导入耦合

**跨模块调用:**
- 直接注入其他模块的服务: 例如 `MerchantQueryService` 注入到 `ecshopx-companys`
- Port 接口（`cn.shopex.ecshopx.common.port.*`）定义模块间契约；实现位于所属模块中
- 集成适配器（`*PortImpl`, `*PortAdapter`）桥接 Port 接口与具体实现

## 密封类型（现代 Java）

新代码使用 Java 17 密封接口和 record:
```java
private sealed interface WechatBindSnapshot permits WechatBindSnapshot.Bound, WechatBindSnapshot.Unbound {
    record Bound(Map<String, Object> operatorMap) implements WechatBindSnapshot {}
    record Unbound(long companyId, String appId, String openid, String unionid) implements WechatBindSnapshot {}
}
```

使用 `instanceof` 进行模式匹配:
```java
if (snap instanceof WechatBindSnapshot.Bound b) { ... }
if (snap instanceof WechatBindSnapshot.Unbound u) { ... }
```

## 注释

**何时添加注释:**
- 实体字段使用 Javadoc 描述数据库语义
- 使用中文注释解释业务逻辑
- `// 与 XxxController 保持一致` 用于交叉引用共享的工具方法

**不使用 JSDoc/TSDoc** — 这是一个 Java 项目。

---

*规范分析: 2026-07-08*
