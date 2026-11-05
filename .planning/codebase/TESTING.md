# 测试模式

**分析日期:** 2026-07-08

## 测试框架

**运行器:**
- JUnit 5 (Jupiter) — 通过 `spring-boot-starter-test` 提供
- 配置: 无独立测试配置文件；测试仅使用注解

**断言库:**
- AssertJ (`org.assertj.core.api.Assertions.assertThat`) — 首选的流式断言库
- JUnit 5 `Assertions` (`org.junit.jupiter.api.Assertions.assertThrows`) — 用于异常测试

**Mock 框架:**
- Mockito 5.x (通过 spring-boot-starter-test 提供)
- Mockito JUnit 5 扩展: `@ExtendWith(MockitoExtension.class)`

**运行命令:**
```bash
./mvnw test                    # 运行所有测试
./mvnw test -pl ecshopx-theme # 运行单个模块的测试
./mvnw verify                  # 运行测试 + 集成检查
```

## 测试文件组织

**位置:**
- 遵循标准 Maven 布局: `{module}/src/test/java/cn/shopex/ecshopx/{module}/...`
- 镜像主源代码包结构

**命名:**
- `{ClassUnderTest}Test.java` — 例如 `PagesTemplateServicesTest.java`
- 描述性探针测试: `{ClassUnderTest}{BehaviorDescription}Test.java` — 例如 `DistributorCreateOrchestratorDistributionAddDispatchPublishProbeTest.java`

**结构:**
```
ecshopx-{module}/
├── src/main/java/cn/shopex/ecshopx/{module}/
│   ├── service/
│   ├── cron/
│   └── integration/
└── src/test/java/cn/shopex/ecshopx/{module}/
    ├── service/          # 服务类的测试
    ├── cron/             # 定时任务处理器的测试
    └── integration/      # 端口适配器的测试
```

## 测试结构

**套件组织:**
```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // 当存根可能未被全部使用时使用
class PagesTemplateServicesTest {

    @BeforeAll
    static void initMybatisPlusTableMetadata() {
        // 必需: 注册实体元数据以便 MyBatis-Plus LambdaQueryWrapper 在测试中正常工作
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            PagesTemplate.class);
    }

    @Mock
    private PagesTemplateMapper pagesTemplateMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    private PagesTemplateServices service;  // 被测类

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(new SimpleTransactionStatus());
        service = new PagesTemplateServices(pagesTemplateMapper, transactionManager);
    }

    @Test
    @DisplayName("分析 §3 步骤1-2：构造 count 条件后 count<1 早退，不查页不写库")
    void scheduleEnableTemplate_countZero_skipsPageAndUpdates() {
        when(pagesTemplateMapper.selectCount(any())).thenReturn(0L);
        service.scheduleEnableTemplate();
        verify(pagesTemplateMapper).selectCount(any());
        verify(pagesTemplateMapper, never()).selectPage(any(), any());
    }
}
```

**观察到的模式:**
- `@DisplayName` 使用中文描述解释正在测试的业务逻辑分支
- 方法名使用 `methodUnderTest_condition_expectedBehavior` 模式
- 每个测试验证实现中的特定分支/条件
- 测试侧重于验证交互（mock verify 调用）而非状态

## Mock

**框架:** Mockito (通过 `@ExtendWith(MockitoExtension.class)`)

**模式:**
```java
// 字段级别的 mock
@Mock
private SomeMapper someMapper;

@Mock
private SomeService someService;

// 构造函数注入（在 setUp 中手动注入）
@InjectMocks
private ClassUnderTest classUnderTest;

// 或手动构造:
@BeforeEach
void setUp() {
    classUnderTest = new ClassUnderTest(someMapper, someService);
}
```

**Mock 验证模式:**
```java
// 验证被调用
verify(pagesTemplateMapper).selectCount(any());

// 验证从未被调用
verify(pagesTemplateMapper, never()).selectPage(any(), any());

// 验证调用次数
verify(pagesTemplateMapper, times(2)).update(isNull(), any());

// 验证调用顺序
InOrder inOrder = inOrder(dispatchFacade);
inOrder.verify(dispatchFacade).dispatchJob(eq("job1"), any());
inOrder.verify(dispatchFacade).dispatchJob(eq("job2"), any());

// 参数匹配器
verify(dispatchFacade).dispatchJob(anyString(), argThat(opts ->
    opts.mode() == DispatchMode.ASYNC && "slow".equals(opts.queue())));
```

**需要 Mock 的对象:**
- MyBatis-Plus mapper（所有数据库访问）
- `PlatformTransactionManager`（事务控制）
- 外部服务门面和客户端
- `DispatchFacade`（异步任务调度）
- `ApplicationEventPublisher`（Spring 事件）
- Redis 模板（`StringRedisTemplate`）

**不需要 Mock 的对象:**
- 被测类本身
- 简单的 POJO 和领域对象（构造真实实例）
- 静态工具方法

## 测试中的 MyBatis-Plus 表元数据

**关键模式:** 当测试使用 `LambdaQueryWrapper` 的服务时，必须在 `@BeforeAll` 块中初始化 MyBatis-Plus 表元数据:

```java
@BeforeAll
static void initMybatisPlusTableMetadata() {
    MybatisConfiguration cfg = new MybatisConfiguration();
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), EntityClass.class);
    // 根据需要注册其他实体
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AnotherEntity.class);
}
```

如果没有这个初始化，`LambdaQueryWrapper` 的方法引用将在测试时抛出异常，因为在完整 Spring 上下文之外实体元数据不可用。

## 测试数据工厂

**测试数据:**
```java
private static PagesTemplate templateRow() {
    PagesTemplate row = new PagesTemplate();
    row.setPagesTemplateId(100L);
    row.setCompanyId(1L);
    row.setRegionauthId(0L);
    row.setDistributorId(0);
    row.setWeappPages("index");
    row.setTimerStatus(1);
    return row;
}
```

**方法:**
- 在测试类中使用私有静态辅助方法
- 逐字段直接构造（不使用 builder 模式或测试工厂库）
- 使用 Lombok `@Data` 的 setter 方法设置领域对象属性

**位置:**
- 内联在每个测试类中（无共享 fixture 目录）

## Logback 日志断言

**测试日志输出的模式:**
```java
private ListAppender<ILoggingEvent> listAppender;
private Logger handlerLogger;

@BeforeEach
void attachLogAppender() {
    listAppender = new ListAppender<>();
    listAppender.start();
    listAppender.list.clear();
    handlerLogger = (Logger) LoggerFactory.getLogger(ScheduleRecordStatisticsHandler.class);
    handlerLogger.addAppender(listAppender);
}

@AfterEach
void detachLogAppender() {
    handlerLogger.detachAppender(listAppender);
    listAppender.stop();
}

// 在测试中:
assertThat(listAppender.list)
    .extracting(ILoggingEvent::getLevel)
    .contains(Level.INFO);
```

## 覆盖率

**要求:** 无强制覆盖率阈值（Maven 中未配置 JaCoCo 或覆盖率插件）

**估计覆盖率:**
- 876 个测试文件覆盖约 7531 个源文件 ≈ 约 12% 的文件覆盖率
- 覆盖率集中在: `ecshopx-companys`、`ecshopx-distribution`、`ecshopx-theme`、`ecshopx-bspay`、`ecshopx-data-cube`、`ecshopx-orders`、`ecshopx-selfservice`
- 许多模块有 `src/test` 目录但可能没有或只有少量测试文件

## 测试类型

**单元测试（主要）:**
- 范围: 单个服务类方法
- 模式: Mock 所有依赖，验证行为
- 框架: JUnit 5 + Mockito
- 不加载 Spring 上下文（仅使用 `@ExtendWith(MockitoExtension.class)`）

**集成测试:**
- 未检测到。未发现 `@SpringBootTest` 或 `@DataJpaTest` 的使用
- 未检测到 Testcontainers 或嵌入式数据库配置

**端到端测试:**
- 不存在

## 定时任务测试

**模式:** 定时任务处理器有专门的测试类验证调度行为:
```java
@ExtendWith(MockitoExtension.class)
class ScheduleRecordStatisticsHandlerTest {

    @Mock
    private DispatchFacade dispatchFacade;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ScheduleRecordStatisticsHandler handler;

    @Test
    @DisplayName("success: two dispatchJob calls in order")
    void executeOk() {
        // 验证调度调用按正确顺序发生
        // 验证失败时调用事件发布器
    }
}
```

**定时任务测试 Mock 配置:** 每个包含定时任务测试的模块在 `src/main/java` 中有一个 `cron/testcfg/CronTestMockConfig.java` — 用于测试 Mock 支持的配置。

## 常用模式

**异常测试:**
```java
@Test
void scheduleEnableTemplate_secondUpdateFails_triggersRollback() {
    when(pagesTemplateMapper.update(isNull(), any()))
        .thenReturn(1)
        .thenThrow(new RuntimeException("db"));
    assertThrows(RuntimeException.class, () -> service.scheduleEnableTemplate());
    verify(transactionManager).rollback(any());
}
```

**事务测试:**
```java
// Mock 事务管理器
when(transactionManager.getTransaction(any(TransactionDefinition.class)))
    .thenReturn(new SimpleTransactionStatus());

// 测试后验证提交或回滚
verify(transactionManager).commit(any());
verify(transactionManager, never()).rollback(any());
```

**测试中的 TransactionSynchronizationManager:**
```java
// 某些测试手动管理 Spring 的 TransactionSynchronizationManager
TransactionSynchronizationManager.initSynchronization();
// ... 测试代码 ...
TransactionSynchronizationManager.clearSynchronization();
```

## 测试命名规范

测试遵循描述性命名模式。观察到两种风格:

1. **行为聚焦:** `methodName_condition_expectedOutcome`
   - `scheduleEnableTemplate_countZero_skipsPageAndUpdates`
   - `delegatesToConfirmationService`

2. **探针式（长描述性类名）:**
   - `DistributorCreateOrchestratorDistributionAddDispatchPublishProbeTest`
   - `DistributorH5GetDistributorListServiceLatLngEnqueueTest`

`@DisplayName` 注解提供中英文混合的可读描述，说明每个测试验证的内容。

## 添加新测试

**添加位置:**
- `{module}/src/test/java/cn/shopex/ecshopx/{module}/{layer}/{ClassUnderTest}Test.java`

**新单元测试模板:**
```java
package cn.shopex.ecshopx.{module}.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewServiceTest {

    @BeforeAll
    static void initMybatisPlusTableMetadata() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            YourEntity.class);
    }

    @Mock
    private YourEntityMapper yourEntityMapper;

    private NewService service;

    @BeforeEach
    void setUp() {
        service = new NewService(yourEntityMapper);
    }

    @Test
    @DisplayName("描述测试场景")
    void methodName_scenario_expectedResult() {
        // given
        when(yourEntityMapper.selectOne(any())).thenReturn(null);

        // when / then
        assertThat(service.doSomething(1L)).isNull();
        verify(yourEntityMapper).selectOne(any());
    }
}
```

---

*测试分析: 2026-07-08*
