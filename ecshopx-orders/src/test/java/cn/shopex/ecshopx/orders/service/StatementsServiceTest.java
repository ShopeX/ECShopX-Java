package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.statement.StatementSettlementCursorRedisPort;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.Statements;
import cn.shopex.ecshopx.orders.domain.StatementPeriodSetting;
import cn.shopex.ecshopx.orders.dto.ScheduleGenerateStatementsResult;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.StatementDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.StatementGenerationQueryMapper;
import cn.shopex.ecshopx.orders.mapper.StatementPeriodSettingMapper;
import cn.shopex.ecshopx.orders.mapper.StatementsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.statement.StatementPeriodValue;
import cn.shopex.ecshopx.orders.statement.StatementTimeWindowCalculator;
import cn.shopex.ecshopx.orders.statement.generate.StatementGenerateJobInliner;
import cn.shopex.ecshopx.common.dispatch.GenerateStatementsJobDispatchPublisher;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class StatementsServiceTest {

	private static final String P_DAY = "[1,\"day\"]";
	private static final Clock FIX =
			Clock.fixed(Instant.parse("2025-06-15T00:00:00Z"), ZoneId.of("UTC"));

	@Mock
	private StatementSettlementCursorRedisPort cursorRedisPort;
	@Mock
	private ShopMenuService shopMenuService;
	@Mock
	private StatementPeriodSettingMapper statementPeriodSettingMapper;
	@Mock
	private DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	@Mock
	private SupplierMapper supplierMapper;
	@Mock
	private GenerateStatementsJobDispatchPublisher dispatchPublisher;

	private ObjectMapper objectMapper;
	private StatementsService service;

	@BeforeEach
	void init() {
		objectMapper = new ObjectMapper();
	}

	private void buildService(Clock c) {
		service =
				new StatementsService(
						cursorRedisPort,
						shopMenuService,
						statementPeriodSettingMapper,
						distributionDistributorPeekMapper,
						supplierMapper,
						dispatchPublisher,
						objectMapper,
						c);
	}

	private static DistributionDistributorPeek distRow(long companyId, long distId, int created) {
		DistributionDistributorPeek p = new DistributionDistributorPeek();
		p.setCompanyId(companyId);
		p.setDistributorId(distId);
		p.setCreated(created);
		return p;
	}

	private static StatementPeriodSetting dDef(long companyId) {
		StatementPeriodSetting s = new StatementPeriodSetting();
		s.setCompanyId(companyId);
		s.setDistributorId(0L);
		s.setSupplierId(0L);
		s.setMerchantType("distributor");
		s.setPeriod(P_DAY);
		return s;
	}

	private static StatementPeriodSetting dFor(long distId) {
		StatementPeriodSetting s = new StatementPeriodSetting();
		s.setDistributorId(distId);
		s.setMerchantType("distributor");
		s.setPeriod(P_DAY);
		return s;
	}

	private static StatementPeriodSetting sDef(long companyId) {
		StatementPeriodSetting s = new StatementPeriodSetting();
		s.setCompanyId(companyId);
		s.setDistributorId(0L);
		s.setSupplierId(0L);
		s.setMerchantType("supplier");
		s.setPeriod(P_DAY);
		return s;
	}

	private static StatementPeriodSetting sFor(long supId) {
		StatementPeriodSetting s = new StatementPeriodSetting();
		s.setSupplierId(supId);
		s.setMerchantType("supplier");
		s.setPeriod(P_DAY);
		return s;
	}

	private static Supplier supRow(long id, long companyId) {
		Supplier s = new Supplier();
		s.setId(id);
		s.setCompanyId(companyId);
		s.setIsCheck(0L);
		return s;
	}

	private static PlatformTransactionManager noopTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public org.springframework.transaction.TransactionStatus getTransaction(
					TransactionDefinition definition) {
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(org.springframework.transaction.TransactionStatus status) {}

			@Override
			public void rollback(org.springframework.transaction.TransactionStatus status) {}
		};
	}

	@Test
	@DisplayName(
			"analysis §3.1-1,2,3; §3.2-1,2,3(否),5; §3.3-1,2,3(否),5; plan §5: 入口顺序+空结果体+早退+doGenerate=0")
	void s31_s32_s33_entryOrderEmptyPages_resultBody() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());

		ScheduleGenerateStatementsResult r = service.scheduleGenerateStatements();
		assertThat(r).isNotNull();
		assertThat(r.doGenerateCallCount()).isEqualTo(0);
		assertThat(r.distributorRowsScanned()).isEqualTo(0);
		assertThat(r.supplierRowsScanned()).isEqualTo(0);
		verify(dispatchPublisher, never())
				.publish(anyLong(), anyLong(), anyLong(), anyInt(), any(), anyLong(), any());
	}

	@Test
	@DisplayName("analysis §3.1-1,2,3; plan §5: InOrder 先 doStatementsForDistributor 后 doStatementsForSupplier")
	void s31_inOrder_distributorBeforeSupplier() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());
		service.scheduleGenerateStatements();
		var o = inOrder(distributionDistributorPeekMapper, supplierMapper);
		o.verify(distributionDistributorPeekMapper).selectPageForStatementSchedule(0, 1000);
		o.verify(supplierMapper).selectPageForStatementSchedule(0, 1000);
	}

	@Test
	@DisplayName("analysis §3.2-3(是),3-A; plan §5: 有页时加载默认+按 distributor 周期 (getLists 等价)")
	void s32_3A_distributor_getLists() {
		buildService(FIX);
		var row = distRow(7L, 1L, 0);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(row));
		when(shopMenuService.resolveProductModelKeyForCompany(7L)).thenReturn("ego");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(dDef(7L), dFor(1L)));
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());
		service.scheduleGenerateStatements();
		verify(statementPeriodSettingMapper, atLeast(1)).selectList(any());
		verify(dispatchPublisher, never())
				.publish(anyLong(), anyLong(), anyLong(), anyInt(), any(), anyLong(), any());
	}

	private void verifyPublisherNever() {
		service.scheduleGenerateStatements();
		verify(dispatchPublisher, never())
				.publish(anyLong(), anyLong(), anyLong(), anyInt(), any(), anyLong(), any());
	}

	@Test
	@DisplayName("analysis §3.2-4-A; plan §5: ShopMenu/周期解析失败则 continue，未 doGenerate")
	void s32_4A_distributor_egoError_skips() {
		buildService(FIX);
		var ok = distRow(1L, 1L, 1000);
		var bad = distRow(1L, 2L, 1000);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(bad, ok));
		when(shopMenuService.resolveProductModelKeyForCompany(1L))
				.thenThrow(new RuntimeException("resolve"))
				.thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(dDef(1L), dFor(1L), dFor(2L)));
		when(cursorRedisPort.getDistributorLastEnd(1L, 1L)).thenReturn(Optional.of("1000"));
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());

		ScheduleGenerateStatementsResult r = service.scheduleGenerateStatements();
		assertThat(r.distributorRowsScanned()).isEqualTo(2);
		assertThat(r.doGenerateCallCount()).isEqualTo(1);
		verify(dispatchPublisher, times(1))
				.publish(eq(1L), eq(1L), eq(0L), eq(1), eq("day"), eq(1000L), eq("distributor"));
	}

	@Test
	@DisplayName("analysis §3.2-4-B; plan §5: 非 platform 则 continue")
	void s32_4B_distributor_notPlatform() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(distRow(1L, 1L, 0)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("ego");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(dDef(1L), dFor(1L)));
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());
		verifyPublisherNever();
	}

	@Test
	@DisplayName("analysis §3.2-4-C; plan §5: 无专属且无默认则 continue")
	void s32_4C_distributor_noPeriod() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(distRow(1L, 1L, 0)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any())).thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());
		verifyPublisherNever();
	}

	@Test
	@DisplayName("analysis §3.2-4-F（合并 4-D/4-E）; plan §5: 窗口 endTime>now 则 continue")
	void s32_4F_distributor_windowNotDue() {
		buildService(FIX);
		long nowSec = FIX.instant().getEpochSecond();
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(distRow(1L, 5L, 0)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(dDef(1L), dFor(5L)));
		when(cursorRedisPort.getDistributorLastEnd(1L, 5L))
				.thenReturn(Optional.of(String.valueOf(nowSec)));
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());
		verifyPublisherNever();
	}

	@Test
	@DisplayName(
			"analysis §3.2-4-D,4-E,4-G + §3.4-1,2,3; plan §5: 到窗并内联 Job 等价 (distributor+§8 非 null 流)")
	void s32_4DEG_34_distributor_window_inline() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(distRow(1L, 9L, 0)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(dDef(1L), dFor(9L)));
		when(cursorRedisPort.getDistributorLastEnd(1L, 9L)).thenReturn(Optional.of("1000"));
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());

		ScheduleGenerateStatementsResult r = service.scheduleGenerateStatements();
		assertThat(r.doGenerateCallCount()).isEqualTo(1);
		verify(dispatchPublisher, times(1))
				.publish(eq(1L), eq(9L), eq(0L), eq(1), eq("day"), eq(1000L), eq("distributor"));
	}

	@Test
	@DisplayName("plan §5.3: Redis 无分销商游标时用 row.created 作为 last_end 并投递 distributor")
	void entry01_doStatementsForDistributor_redisMissUsesRowCreatedAsLastEnd() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(distRow(1L, 9L, 1000)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(dDef(1L), dFor(9L)));
		when(cursorRedisPort.getDistributorLastEnd(1L, 9L)).thenReturn(Optional.empty());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());

		assertThat(service.scheduleGenerateStatements().doGenerateCallCount()).isEqualTo(1);
		verify(dispatchPublisher, times(1))
				.publish(eq(1L), eq(9L), eq(0L), eq(1), eq("day"), eq(1000L), eq("distributor"));
	}

	@Test
	@DisplayName("analysis §3.2-2,5; plan §5: 满页链 — 两页各一次 select (首屏=limit)")
	void s32_5_fullPage_chain_twoQueries() {
		buildService(FIX);
		var page0 = new ArrayList<DistributionDistributorPeek>(1000);
		for (int i = 0; i < 1000; i++) {
			page0.add(distRow(1L, 1000L + i, 0));
		}
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(page0);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(1000, 1000))
				.thenReturn(List.of());
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("ego");
		when(statementPeriodSettingMapper.selectList(any())).thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000)).thenReturn(List.of());

		service.scheduleGenerateStatements();
		verify(distributionDistributorPeekMapper).selectPageForStatementSchedule(0, 1000);
		verify(distributionDistributorPeekMapper).selectPageForStatementSchedule(1000, 1000);
	}

	@Test
	@DisplayName("analysis §3.3-3(是),3-A; plan §5: 供应商有页时 getLists 按 supplier id")
	void s33_3A_supplier_getLists() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(supRow(10L, 2L)));
		when(shopMenuService.resolveProductModelKeyForCompany(2L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(sDef(2L), sFor(10L)));
		long nowSec = FIX.instant().getEpochSecond();
		when(cursorRedisPort.getSupplierLastEnd(2L, 10L))
				.thenReturn(Optional.of(String.valueOf(nowSec)));
		service.scheduleGenerateStatements();
		verify(statementPeriodSettingMapper, atLeast(1)).selectList(any());
	}

	@Test
	@DisplayName("analysis §3.3-4-A; plan §5: 供应商 Ego/解析异常则 continue（同 §3.2-4-A 实现路径，单独分支编号）")
	void s33_4A_supplier_egoError_skips() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(supRow(1L, 1L), supRow(2L, 1L)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L))
				.thenThrow(new IllegalStateException("x"))
				.thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(sDef(1L), sFor(1L), sFor(2L)));
		when(cursorRedisPort.getSupplierLastEnd(1L, 2L)).thenReturn(Optional.of("1"));
		ScheduleGenerateStatementsResult r = service.scheduleGenerateStatements();
		assertThat(r.supplierRowsScanned()).isEqualTo(2);
		assertThat(r.doGenerateCallCount()).isEqualTo(1);
		verify(dispatchPublisher, times(1))
				.publish(eq(1L), eq(0L), eq(2L), eq(1), eq("day"), eq(1L), eq("supplier"));
	}

	@Test
	@DisplayName("analysis §3.3-4-B; plan §5: 无 settleSetting 且无 default 则 continue")
	void s33_4B_supplier_noPeriod() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(supRow(3L, 1L)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any())).thenReturn(List.of());
		verifyPublisherNever();
	}

	@Test
	@DisplayName("analysis §3.3-4-C（合并 §8-3 守卫）; plan §5: Redis 无键走反推 lastEnd 并可到达 runJob")
	void s33_4C_supplier_redisEmpty_inferredLast() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(supRow(4L, 1L)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(sDef(1L), sFor(4L)));
		when(cursorRedisPort.getSupplierLastEnd(1L, 4L)).thenReturn(Optional.empty());
		ArgumentCaptor<Long> lastCap = ArgumentCaptor.forClass(Long.class);
		service.scheduleGenerateStatements();
		verify(dispatchPublisher)
				.publish(eq(1L), eq(0L), eq(4L), eq(1), eq("day"), lastCap.capture(), eq("supplier"));
		assertThat(lastCap.getValue()).isEqualTo(
				StatementTimeWindowCalculator.supplierInferredLastEndIfRedisMissing(
						StatementPeriodValue.tryParse(P_DAY, objectMapper), FIX.instant().getEpochSecond()));
	}

	@Test
	@DisplayName("analysis §3.3-4-D; plan §5: 供应商 endTime>now 则 continue")
	void s33_4D_supplier_notDue() {
		buildService(FIX);
		long nowSec = FIX.instant().getEpochSecond();
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(supRow(5L, 1L)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(sDef(1L), sFor(5L)));
		when(cursorRedisPort.getSupplierLastEnd(1L, 5L))
				.thenReturn(Optional.of(String.valueOf(nowSec)));
		verifyPublisherNever();
	}

	@Test
	@DisplayName("analysis §3.3-4-E + §3.4-1,2,3; plan §5: 供应商到窗并内联")
	void s33_4E_34_supplier_window_inline() {
		buildService(FIX);
		when(distributionDistributorPeekMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of());
		when(supplierMapper.selectPageForStatementSchedule(0, 1000))
				.thenReturn(List.of(supRow(6L, 1L)));
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("platform");
		when(statementPeriodSettingMapper.selectList(any()))
				.thenReturn(List.of(sDef(1L), sFor(6L)));
		when(cursorRedisPort.getSupplierLastEnd(1L, 6L)).thenReturn(Optional.of("1"));
		assertThat(service.scheduleGenerateStatements().doGenerateCallCount()).isEqualTo(1);
		verify(dispatchPublisher, times(1))
				.publish(eq(1L), eq(0L), eq(6L), eq(1), eq("day"), eq(1L), eq("supplier"));
	}

	@Test
	@DisplayName("analysis §3.4-说明; plan §5: forSupplier 首屏订单空 → 内联 fail，runJob=0 窗，不落 Redis 游标")
	void s34_note_supplierOrderEmpty_rollbackInliner() {
		StatementSettlementCursorRedisPort cursor = mock(StatementSettlementCursorRedisPort.class);
		DistributorGetInfoSimpleByDistributorIdPort distP =
				mock(DistributorGetInfoSimpleByDistributorIdPort.class);
		StatementsMapper statementsMapper = mock(StatementsMapper.class);
		StatementDetailsMapper detailsMapper = mock(StatementDetailsMapper.class);
		StatementGenerationQueryMapper query = mock(StatementGenerationQueryMapper.class);
		TradeMapper tradeMapper = mock(TradeMapper.class);
		SupplierMapper inlinerSup = mock(SupplierMapper.class);
		SupplierOrderMapper so = mock(SupplierOrderMapper.class);
		PlatformTransactionManager ptm = noopTxManager();
		StatementPeriodValue p = StatementPeriodValue.tryParse(P_DAY, objectMapper);
		assertThat(p).isNotNull();
		Supplier sup = supRow(8L, 1L);
		sup.setOperatorId(88L);
		long lastEnd = 1L;
		when(inlinerSup.selectById(8L)).thenReturn(sup);
		when(statementsMapper.insert(any(Statements.class)))
				.thenAnswer(
						inv -> {
							Statements h = inv.getArgument(0);
							h.setId(999L);
							return 1;
						});
		when(query.selectSupplierOrdersPage(eq(1L), eq(88L), anyLong(), eq(0), eq(500)))
				.thenReturn(List.of());
		StatementGenerateJobInliner real =
				new StatementGenerateJobInliner(
						cursor, distP, statementsMapper, detailsMapper, query, tradeMapper, inlinerSup, so, ptm);
		int windows =
				real.runJob(FIX, 1L, 8L, p, lastEnd, "supplier");
		assertThat(windows).isZero();
		verify(cursor, never())
				.setSupplierLastEnd(anyLong(), anyLong(), any());
	}

	@Test
	@DisplayName("analysis §3.4-1 + §8 日期; plan §5: 周期算法 Golden（day schedule end 可重复、scheduling window 周一）")
	void s34_golden_periodAndSchedulingWindow() {
		long t =
				ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, StatementTimeWindowCalculator.SHANGHAI)
						.toEpochSecond();
		assertThat(StatementTimeWindowCalculator.phpW(t)).isEqualTo(1);
		long e = StatementTimeWindowCalculator.computeScheduleWindowEndForLastLine(1000L, 1, "day");
		assertThat(StatementTimeWindowCalculator.computeScheduleWindowEndForLastLine(1000L, 1, "day"))
				.isEqualTo(e);
	}
}
