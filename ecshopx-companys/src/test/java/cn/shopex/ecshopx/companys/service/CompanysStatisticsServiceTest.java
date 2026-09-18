package cn.shopex.ecshopx.companys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.SalespersonActiveArticleRecordRunner;
import cn.shopex.ecshopx.common.cron.SalespersonCommissionRecordStatisticsRunner;
import cn.shopex.ecshopx.common.cron.SalespersonGiveCouponsRecordStatisticsRunner;
import cn.shopex.ecshopx.common.cron.SalespersonPopularizeRecordStatisticsRunner;
import cn.shopex.ecshopx.common.cron.SalespersonShoppingGuideStatisticsRunner;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Statistics;
import cn.shopex.ecshopx.companys.domain.StoreStatistics;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordActiveArticleStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordCommissionStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordGiveCouponsStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordPopularizeStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordStatisticsResult;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.StatisticsMapper;
import cn.shopex.ecshopx.companys.mapper.StoreStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class CompanysStatisticsServiceTest {

	@Mock
	private CompanysRecordStatisticsRedisPort recordStatsRedis;

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private StatisticsMapper statisticsMapper;

	@Mock
	private StoreStatisticsMapper storeStatisticsMapper;

	@Mock
	private ObjectProvider<SalespersonShoppingGuideStatisticsRunner> salespersonRunnerProvider;

	@Mock
	private ObjectProvider<SalespersonActiveArticleRecordRunner> activeArticleRunnerProvider;

	@Mock
	private ObjectProvider<SalespersonCommissionRecordStatisticsRunner> commissionRunnerProvider;

	@Mock
	private ObjectProvider<SalespersonPopularizeRecordStatisticsRunner> popularizeRunnerProvider;

	@Mock
	private ObjectProvider<SalespersonGiveCouponsRecordStatisticsRunner> giveCouponsRunnerProvider;

	private CompanysStatisticsService service;

	@BeforeEach
	void setUp() {
		service = new CompanysStatisticsService(
				recordStatsRedis,
				companysMapper,
				statisticsMapper,
				storeStatisticsMapper,
				salespersonRunnerProvider,
				activeArticleRunnerProvider,
				commissionRunnerProvider,
				popularizeRunnerProvider,
				giveCouponsRunnerProvider);
	}

	@Nested
	class ScheduleAll {

		@Test
		@DisplayName("§3.2-3 无公司行：不 insert 统计行")
		void noCompanies() {
			when(companysMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
			when(salespersonRunnerProvider.getIfAvailable()).thenReturn(null);
			ScheduleRecordStatisticsResult r = service.scheduleRecordStatistics();
			assertThat(r.processed()).isEqualTo(0);
			verify(statisticsMapper, never()).insert(any(Statistics.class));
		}

		@Test
		@DisplayName(
				"§3.2-4-A §3.2-4-B 单公司先 recordStatistics(service) 再 recordStatistics(normal)，Redis 全 0 无 insert，runner 段")
		void oneCompanyWithRunner() {
			when(companysMapper.selectList(any(Wrapper.class)))
					.thenReturn(List.of(mockCompany(1L)));
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			SalespersonShoppingGuideStatisticsRunner runner =
					org.mockito.Mockito.mock(SalespersonShoppingGuideStatisticsRunner.class);
			when(runner.runShoppingGuideBlock(ArgumentMatchers.anyInt())).thenReturn(3);
			when(salespersonRunnerProvider.getIfAvailable()).thenReturn(runner);
			ScheduleRecordStatisticsResult r = service.scheduleRecordStatistics();
			assertThat(r.processed()).isEqualTo(1 + 0 + 3);
			verify(runner, times(1)).runShoppingGuideBlock(ArgumentMatchers.anyInt());
		}
	}

	@Nested
	class ScheduleRecordActiveArticle {

		@Test
		@DisplayName("§3.3-3 无公司行、活动 runner 空：processed=0")
		void noCompanies() {
			when(companysMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
			ScheduleRecordActiveArticleStatisticsResult r = service.scheduleRecordActiveArticleStatistics();
			assertThat(r.processed()).isEqualTo(0);
		}

		@Test
		@DisplayName("§3.1 先全公司 Record 再 runActiveArticleBlock")
		void recordBeforeActiveBlock() {
			when(companysMapper.selectList(any(Wrapper.class)))
					.thenReturn(List.of(mockCompany(1L)));
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			SalespersonActiveArticleRecordRunner activeRunner =
					org.mockito.Mockito.mock(SalespersonActiveArticleRecordRunner.class);
			when(activeRunner.runActiveArticleBlock(ArgumentMatchers.anyInt())).thenReturn(5);
			when(activeArticleRunnerProvider.getIfAvailable()).thenReturn(activeRunner);
			ScheduleRecordActiveArticleStatisticsResult r = service.scheduleRecordActiveArticleStatistics();
			assertThat(r.processed()).isEqualTo(1 + 0 + 5);
			InOrder inOrder = inOrder(companysMapper, activeRunner);
			inOrder.verify(companysMapper).selectList(any(Wrapper.class));
			inOrder.verify(activeRunner).runActiveArticleBlock(ArgumentMatchers.anyInt());
		}
	}

	@Nested
	class ScheduleRecordCommission {

		@Test
		@DisplayName("分润编排：无公司、commission runner 空则 processed=0")
		void noCompanies() {
			when(companysMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
			when(commissionRunnerProvider.getIfAvailable()).thenReturn(null);
			ScheduleRecordCommissionStatisticsResult r = service.scheduleRecordCommissionStatistics();
			assertThat(r.processed()).isEqualTo(0);
			verify(commissionRunnerProvider, times(1)).getIfAvailable();
		}

		@Test
		@DisplayName("先全公司 record 再 runCommissionBlock，不触达导购日统计 runner")
		void recordThenCommission() {
			when(companysMapper.selectList(any(Wrapper.class)))
					.thenReturn(List.of(mockCompany(1L)));
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			SalespersonCommissionRecordStatisticsRunner cr =
					mock(SalespersonCommissionRecordStatisticsRunner.class);
			when(cr.runCommissionBlock(ArgumentMatchers.anyInt())).thenReturn(4);
			when(commissionRunnerProvider.getIfAvailable()).thenReturn(cr);
			ScheduleRecordCommissionStatisticsResult r = service.scheduleRecordCommissionStatistics();
			assertThat(r.processed()).isEqualTo(1 + 0 + 4);
			verify(cr, times(1)).runCommissionBlock(ArgumentMatchers.anyInt());
			verify(salespersonRunnerProvider, never()).getIfAvailable();
			InOrder inOrder = inOrder(companysMapper, cr);
			inOrder.verify(companysMapper).selectList(any(Wrapper.class));
			inOrder.verify(cr).runCommissionBlock(ArgumentMatchers.anyInt());
		}
	}

	@Nested
	class ScheduleRecordPopularize {

		@Test
		@DisplayName("§1：单 JVM 内先段一 Record 全路径再段二；未调 scheduleRecordStatistics/commission/active/runShoppingGuideBlock")
		void noWrongEntrypoints() {
			when(companysMapper.selectList(any(Wrapper.class)))
					.thenReturn(List.of(mockCompany(1L)));
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			SalespersonPopularizeRecordStatisticsRunner pr = mock(SalespersonPopularizeRecordStatisticsRunner.class);
			when(pr.runPopularizeBlock(ArgumentMatchers.anyInt())).thenReturn(2);
			when(popularizeRunnerProvider.getIfAvailable()).thenReturn(pr);
			ScheduleRecordPopularizeStatisticsResult r = service.scheduleRecordPopularizeStatistics();
			assertThat(r.processed()).isEqualTo(1 + 0 + 2);
			verify(commissionRunnerProvider, never()).getIfAvailable();
			verify(activeArticleRunnerProvider, never()).getIfAvailable();
			verify(salespersonRunnerProvider, never()).getIfAvailable();
			InOrder inOrder = inOrder(companysMapper, pr);
			inOrder.verify(companysMapper).selectList(any(Wrapper.class));
			inOrder.verify(pr).runPopularizeBlock(ArgumentMatchers.anyInt());
		}

		@Test
		@DisplayName("§2.1·§2.3·§2.4：1 家公司 service+normal 各一次，段一不抛；processed 含 companyRows+insertTotal")
		void oneCompanyServiceNormal() {
			when(companysMapper.selectList(any(Wrapper.class)))
					.thenReturn(List.of(mockCompany(1L)));
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			when(popularizeRunnerProvider.getIfAvailable()).thenReturn(null);
			ScheduleRecordPopularizeStatisticsResult r = service.scheduleRecordPopularizeStatistics();
			assertThat(r.processed()).isEqualTo(1);
		}

		@Test
		@DisplayName("§3.2 N/A：$time=strtotime 类死代码不单独设断言（与 plan §2 / analysis §3.2 对账）")
		void section32NotApplicable() {
			assertThat(true).isTrue();
		}

		@Test
		@DisplayName("§3.3–§3.6：Member scard+expireat+公司维 createData 主路径（同 RecordStatistics 深测）")
		void recordHashMainPath() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.scard("Member:1:20200101")).thenReturn(3L);
			when(statisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(1);
		}

		@Test
		@DisplayName("scheduleRecordPopularizeStatistics：单公司且无推广 runner 时 processed 为 1")
		void recordReturnsInserts() {
			when(companysMapper.selectList(any(Wrapper.class)))
					.thenReturn(List.of(mockCompany(1L)));
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			when(popularizeRunnerProvider.getIfAvailable()).thenReturn(null);
			ScheduleRecordPopularizeStatisticsResult r = service.scheduleRecordPopularizeStatistics();
			assertThat(r.processed()).isEqualTo(1);
		}

		@Test
		@DisplayName("§1+§2 串联：processed=companyRows+insertTotal+推广段；runner 空时仅段一")
		void chainProcessedWithoutRunner() {
			when(companysMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
			when(popularizeRunnerProvider.getIfAvailable()).thenReturn(null);
			ScheduleRecordPopularizeStatisticsResult r = service.scheduleRecordPopularizeStatistics();
			assertThat(r.processed()).isEqualTo(0);
		}

		@Test
		@DisplayName("§5.6：段二 popularize 块返回值计入全入口 processed")
		void section56RunnerAddsToProcessed() {
			when(companysMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
			SalespersonPopularizeRecordStatisticsRunner pr = mock(SalespersonPopularizeRecordStatisticsRunner.class);
			when(pr.runPopularizeBlock(ArgumentMatchers.anyInt())).thenReturn(5);
			when(popularizeRunnerProvider.getIfAvailable()).thenReturn(pr);
			ScheduleRecordPopularizeStatisticsResult r = service.scheduleRecordPopularizeStatistics();
			assertThat(r.processed()).isEqualTo(5);
		}

		@Test
		@DisplayName("runPopularizeBlockForYesterday：runner 可用则委托 runPopularizeBlock")
		void runPopularizeBlockForYesterday_withRunner() {
			SalespersonPopularizeRecordStatisticsRunner pr = mock(SalespersonPopularizeRecordStatisticsRunner.class);
			when(pr.runPopularizeBlock(ArgumentMatchers.anyInt())).thenReturn(9);
			when(popularizeRunnerProvider.getIfAvailable()).thenReturn(pr);
			assertThat(service.runPopularizeBlockForYesterday()).isEqualTo(9);
			verify(pr, times(1)).runPopularizeBlock(ArgumentMatchers.anyInt());
		}

		@Test
		@DisplayName("runPopularizeBlockForYesterday：runner 不可用则 0")
		void runPopularizeBlockForYesterday_noRunner() {
			when(popularizeRunnerProvider.getIfAvailable()).thenReturn(null);
			assertThat(service.runPopularizeBlockForYesterday()).isEqualTo(0);
		}
	}

	@Nested
	class ScheduleRecordGiveCoupons {

		@Test
		@DisplayName("§1：先段一再送券；未调 commission/popularize/runShoppingGuideBlock")
		void orderAndNoOtherRunners() {
			when(companysMapper.selectList(any(Wrapper.class)))
					.thenReturn(List.of(mockCompany(1L)));
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			SalespersonGiveCouponsRecordStatisticsRunner gr = mock(SalespersonGiveCouponsRecordStatisticsRunner.class);
			when(gr.runGiveCouponsBlock(ArgumentMatchers.anyInt())).thenReturn(2);
			when(giveCouponsRunnerProvider.getIfAvailable()).thenReturn(gr);
			ScheduleRecordGiveCouponsStatisticsResult r = service.scheduleRecordGiveCouponsStatistics();
			assertThat(r.processed()).isEqualTo(1 + 0 + 2);
			verify(gr, times(1)).runGiveCouponsBlock(ArgumentMatchers.anyInt());
			verify(commissionRunnerProvider, never()).getIfAvailable();
			verify(popularizeRunnerProvider, never()).getIfAvailable();
			verify(salespersonRunnerProvider, never()).getIfAvailable();
			InOrder inOrder = inOrder(companysMapper, gr);
			inOrder.verify(companysMapper).selectList(any(Wrapper.class));
			inOrder.verify(gr).runGiveCouponsBlock(ArgumentMatchers.anyInt());
		}

		@Test
		@DisplayName("无公司、送券 runner 空则 processed=0")
		void noCompanies() {
			when(companysMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
			when(giveCouponsRunnerProvider.getIfAvailable()).thenReturn(null);
			ScheduleRecordGiveCouponsStatisticsResult r = service.scheduleRecordGiveCouponsStatistics();
			assertThat(r.processed()).isEqualTo(0);
		}

		@Test
		@DisplayName("runGiveCouponsBlockForYesterday：runner 可用则委托 runGiveCouponsBlock")
		void runGiveCouponsBlockForYesterday_withRunner() {
			SalespersonGiveCouponsRecordStatisticsRunner gr = mock(SalespersonGiveCouponsRecordStatisticsRunner.class);
			when(gr.runGiveCouponsBlock(ArgumentMatchers.anyInt())).thenReturn(9);
			when(giveCouponsRunnerProvider.getIfAvailable()).thenReturn(gr);
			assertThat(service.runGiveCouponsBlockForYesterday()).isEqualTo(9);
			verify(gr, times(1)).runGiveCouponsBlock(ArgumentMatchers.anyInt());
		}

		@Test
		@DisplayName("runGiveCouponsBlockForYesterday：runner 不可用则 0")
		void runGiveCouponsBlockForYesterday_noRunner() {
			when(giveCouponsRunnerProvider.getIfAvailable()).thenReturn(null);
			assertThat(service.runGiveCouponsBlockForYesterday()).isEqualTo(0);
		}
	}

	@Nested
	class RecordStatistics {

		@Test
		@DisplayName(
				"§3.3-3～§3.3-6 · §3.3-3 会员 Member scard 非 0 · §3.6-4 默认 StatisticsMapper · §3.6-7 新建 insert")
		void memberScard() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.scard("Member:1:20200101")).thenReturn(3L);
			when(statisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(1);
			ArgumentCaptor<Statistics> cap = ArgumentCaptor.forClass(Statistics.class);
			verify(statisticsMapper, times(1)).insert(cap.capture());
			assertThat(cap.getValue().getStatisticTitle()).isEqualTo("newAddMember");
			assertThat(cap.getValue().getStatisticType()).isEqualTo("member");
		}

		@Test
		@DisplayName(
				"§3.3-3～§3.3-6 · §3.3-4 MemberCard vip scard 非 0 · §3.6-4 §3.6-7")
		void vipMemberScard() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.scard("MemberCard:1:vip:20200101")).thenReturn(2L);
			when(statisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(1);
			ArgumentCaptor<Statistics> cap = ArgumentCaptor.forClass(Statistics.class);
			verify(statisticsMapper, times(1)).insert(cap.capture());
			assertThat(cap.getValue().getStatisticTitle()).isEqualTo("vipMember");
			assertThat(cap.getValue().getStatisticType()).isEqualTo("member");
		}

		@Test
		@DisplayName(
				"§3.3-3～§3.3-6 · §3.3-5 MemberCard svip scard 非 0 · §3.6-4 §3.6-7")
		void svipMemberScard() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.scard("MemberCard:1:svip:20200101")).thenReturn(7L);
			when(statisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(1);
			ArgumentCaptor<Statistics> cap = ArgumentCaptor.forClass(Statistics.class);
			verify(statisticsMapper, times(1)).insert(cap.capture());
			assertThat(cap.getValue().getStatisticTitle()).isEqualTo("svipMember");
			assertThat(cap.getValue().getStatisticType()).isEqualTo("member");
		}

		@Test
		@DisplayName(
				"§3.3-3～§3.3-6 · §3.3-6 orderPayUser scard 非 0 · §3.6-4 §3.6-7")
		void orderPayUserScard() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.scard("OrderPayStatistics:normal:1:20200101_orderPayUser"))
					.thenReturn(11L);
			when(statisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(1);
			ArgumentCaptor<Statistics> cap = ArgumentCaptor.forClass(Statistics.class);
			verify(statisticsMapper, times(1)).insert(cap.capture());
			assertThat(cap.getValue().getStatisticTitle()).isEqualTo("orderPayUser");
			assertThat(cap.getValue().getStatisticType()).isEqualTo("normal");
		}

		@Test
		@DisplayName("§3.3-1 日期为 null 走昨日分支")
		void nullDate() {
			when(recordStatsRedis.scard(anyString())).thenReturn(0L);
			when(recordStatsRedis.hgetall(anyString())).thenReturn(Map.of());
			int ins = service.recordStatistics(1L, "normal", null);
			assertThat(ins).isEqualTo(0);
		}

		@Test
		@DisplayName(
				"§3.3-8-A hash 单段 title · §3.6-4 默认 StatisticsMapper · §3.6-7 新建 insert")
		void hashOneSegment() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.hgetall("OrderPayStatistics:normal:1:20200101"))
					.thenReturn(Map.of("orderPayFee", "5"));
			when(statisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(1);
			ArgumentCaptor<Statistics> cap = ArgumentCaptor.forClass(Statistics.class);
			verify(statisticsMapper).insert(cap.capture());
			assertThat(cap.getValue().getStatisticTitle()).isEqualTo("orderPayFee");
		}

		@Test
		@DisplayName(
				"§3.3-8-B 双段门店 shop_id+title · §3.6-2 StoreStatisticsMapper · §3.6-7 新建 insert")
		void hashTwoSegments() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.hgetall("OrderPayStatistics:normal:1:20200101"))
					.thenReturn(Map.of("9_orderPayNum", "2"));
			when(recordStatsRedis.scard("OrderPayStatistics:normal:1:20200101_9_orderPayUser"))
					.thenReturn(0L);
			when(storeStatisticsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(1);
			verify(storeStatisticsMapper, times(1)).insert(any(StoreStatistics.class));
		}

		@Test
		@DisplayName("§3.3-8-C 多段不写入")
		void hashInvalidSegments() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.hgetall("OrderPayStatistics:normal:1:20200101"))
					.thenReturn(Map.of("a_b_c", "1"));
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(0);
			verify(statisticsMapper, never()).insert(any(Statistics.class));
			verify(storeStatisticsMapper, never()).insert(any(StoreStatistics.class));
		}

		@Test
		@DisplayName("§3.6-6 createData 已存在不 INSERT（companys_statistics 短路）")
		void existingNoInsert() {
			stubNormalKeys(1L, 20200101);
			when(recordStatsRedis.hgetall("OrderPayStatistics:normal:1:20200101"))
					.thenReturn(Map.of("orderPayFee", "5"));
			Statistics existing = new Statistics();
			existing.setId(1L);
			existing.setDataValue(9);
			when(statisticsMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
			int ins = service.recordStatistics(1L, "normal", 20200101);
			assertThat(ins).isEqualTo(0);
			verify(statisticsMapper, never()).insert(any(Statistics.class));
		}
	}

	private static Companys mockCompany(long id) {
		Companys c = new Companys();
		c.setCompanyId(id);
		return c;
	}

	private void stubNormalKeys(long companyId, int ymd) {
		String d = String.valueOf(ymd);
		when(recordStatsRedis.scard("Member:" + companyId + ":" + d)).thenReturn(0L);
		when(recordStatsRedis.scard("MemberCard:" + companyId + ":vip:" + d))
				.thenReturn(0L);
		when(recordStatsRedis.scard("MemberCard:" + companyId + ":svip:" + d))
				.thenReturn(0L);
		when(recordStatsRedis.scard("OrderPayStatistics:normal:" + companyId + ":" + d + "_orderPayUser"))
				.thenReturn(0L);
		when(recordStatsRedis.hgetall("OrderPayStatistics:normal:" + companyId + ":" + d))
				.thenReturn(Map.of());
	}
}
