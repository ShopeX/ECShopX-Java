package cn.shopex.ecshopx.youshu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.cron.wechat.WxappDataCubeVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.wechat.WxappDataCubeVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddOrderSumPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOrderSumRow;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class YoushuTaskServiceTest {

	private static final ObjectMapper OM = new ObjectMapper();

	@Mock
	private YoushuSettingMapper youshuSettingMapper;

	@Mock
	private WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;

	@Mock
	private WxappDataCubeVisitPagePort visitPagePort;

	@Mock
	private WxappDataCubeVisitDistributionPort visitDistributionPort;

	@Mock
	private YoushuDataSourceApiPort youshuDataSourceApiPort;

	@Mock
	private YoushuAnalysisAddWxappVisitPagePort youshuAnalysisAddWxappVisitPagePort;

	@Mock
	private YoushuAnalysisAddWxappVisitDistributionPort youshuAnalysisAddWxappVisitDistributionPort;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private YoushuAnalysisAddOrderSumPort youshuAnalysisAddOrderSumPort;

	private Clock clock;
	private YoushuTaskService service;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-04-20T00:00:00Z"), ZoneOffset.UTC);
		service = new YoushuTaskService(
				youshuSettingMapper,
				weappAuthorizerAppidRepository,
				visitPagePort,
				visitDistributionPort,
				youshuDataSourceApiPort,
				youshuAnalysisAddWxappVisitPagePort,
				youshuAnalysisAddWxappVisitDistributionPort,
				normalOrdersMapper,
				tradeMapper,
				youshuAnalysisAddOrderSumPort,
				clock);
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(YoushuTaskService.class);
		serviceLogger.addAppender(listAppender);
		serviceLogger.setLevel(Level.DEBUG);
	}

	@AfterEach
	void tearDown() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§3 步骤 1 早退：空 list；2.1–2.11/3 不执行（plan §5 早退出）")
	void earlyExit_emptyList_noWeappNoPorts() {
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of());
		service.scheduleAddWxappVisitPage();
		verify(weappAuthorizerAppidRepository, never()).findAuthorizerAppid(anyLong(), any());
		verify(visitPagePort, never()).postVisitPage(any(), any(), any());
		verify(youshuDataSourceApiPort, never()).getOrCreateDataSourceId(any(), anyInt(), any());
		verify(youshuAnalysisAddWxappVisitPagePort, never()).addWxappVisitPage(any(), any(), any());
	}

	@Test
	@DisplayName("§3 步骤 1 早退：null list；2.1–2.11/3 不执行（plan §5 早退出）")
	void earlyExit_nullList_noWeappNoPorts() {
		when(youshuSettingMapper.selectList(any())).thenReturn(null);
		service.scheduleAddWxappVisitPage();
		verify(weappAuthorizerAppidRepository, never()).findAuthorizerAppid(anyLong(), any());
		verify(visitPagePort, never()).postVisitPage(any(), any(), any());
	}

	@Test
	@DisplayName("§3 1+2.1+2.2+2.3 无 appid；2.4–2.10 跳过；2.11 下一行；2.1–2.10 成功+3（plan §5 缺 appid）")
	void missingAppid_noPorts_secondRowStillEvaluated() {
		YoushuSetting a = row(1L, "m1");
		YoushuSetting b = row(2L, "m2");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(a, b));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(1L, "yykweishop")).thenReturn(Optional.empty());
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(2L, "yykweishop")).thenReturn(Optional.of("wxaid"));
		JsonNode visit = sampleVisit();
		when(visitPagePort.postVisitPage(eq("wxaid"), eq("20260419"), eq("20260419"))).thenReturn(visit);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("m2"), eq(0), any(YoushuOpenApiCredentials.class)))
				.thenReturn("ds1");
		service.scheduleAddWxappVisitPage();
		verify(weappAuthorizerAppidRepository, times(1)).findAuthorizerAppid(1L, "yykweishop");
		verify(weappAuthorizerAppidRepository, times(1)).findAuthorizerAppid(2L, "yykweishop");
		verify(visitPagePort, times(1)).postVisitPage(any(), any(), any());
		verify(youshuDataSourceApiPort, times(1)).getOrCreateDataSourceId(any(), anyInt(), any());
		verify(youshuAnalysisAddWxappVisitPagePort, times(1)).addWxappVisitPage(eq("ds1"), eq(visit), any());
	}

	@Test
	@DisplayName("§3 主成功链 2.1–2.10、2.11、3（plan §5 主成功链）")
	void mainSuccess_invokesPortsInOrder() {
		YoushuSetting row = row(10L, "merch");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(10L, "yykweishop"))
				.thenReturn(Optional.of("wxa"));
		JsonNode visit = sampleVisit();
		when(visitPagePort.postVisitPage("wxa", "20260419", "20260419")).thenReturn(visit);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("merch"), eq(0), any(YoushuOpenApiCredentials.class)))
				.thenReturn("ds99");
		service.scheduleAddWxappVisitPage();
		InOrder order = inOrder(visitPagePort, youshuDataSourceApiPort, youshuAnalysisAddWxappVisitPagePort);
		order.verify(visitPagePort).postVisitPage("wxa", "20260419", "20260419");
		order.verify(youshuDataSourceApiPort).getOrCreateDataSourceId(eq("merch"), eq(0), any(YoushuOpenApiCredentials.class));
		order.verify(youshuAnalysisAddWxappVisitPagePort).addWxappVisitPage(eq("ds99"), eq(visit), any());
	}

	@Test
	@DisplayName("§3 2.1 catch：2.5–2.6 postVisitPage 失败；2.7–2.10 未达；2.11/3（plan §5 visit 抛错）")
	void visitPageThrows_swallowed_debugLog_analysisNotCalled() {
		YoushuSetting row = row(3L, "m");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(3L, "yykweishop")).thenReturn(Optional.of("w"));
		when(visitPagePort.postVisitPage(any(), any(), any())).thenThrow(new RuntimeException("visit-boom"));
		service.scheduleAddWxappVisitPage();
		verify(youshuDataSourceApiPort, never()).getOrCreateDataSourceId(any(), anyInt(), any());
		verify(youshuAnalysisAddWxappVisitPagePort, never()).addWxappVisitPage(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("addWxappVisitPage_error"));
	}

	@Test
	@DisplayName("§3 2.1 catch：2.9 失败；2.10 不执行；2.11/3（plan §5 2.9 取数失败）")
	void dataSourceThrows_swallowed_debugLog_analysisNotCalled() {
		YoushuSetting row = row(4L, "m");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(4L, "yykweishop")).thenReturn(Optional.of("w"));
		JsonNode visit = sampleVisit();
		when(visitPagePort.postVisitPage(any(), any(), any())).thenReturn(visit);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(any(), anyInt(), any()))
				.thenThrow(new RuntimeException("ds-boom"));
		service.scheduleAddWxappVisitPage();
		verify(youshuAnalysisAddWxappVisitPagePort, never()).addWxappVisitPage(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage().contains("addWxappVisitPage_error"));
	}

	@Test
	@DisplayName("§3 2.1 catch：2.10 addWxappVisitPage 失败；2.11/3（plan §5 2.10 抛错）")
	void analysisThrows_swallowed_debugLog() {
		YoushuSetting row = row(5L, "m");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(5L, "yykweishop")).thenReturn(Optional.of("w"));
		when(visitPagePort.postVisitPage(any(), any(), any())).thenReturn(sampleVisit());
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(any(), anyInt(), any())).thenReturn("ds");
		doThrow(new RuntimeException("an-boom"))
				.when(youshuAnalysisAddWxappVisitPagePort)
				.addWxappVisitPage(any(), any(), any());
		service.scheduleAddWxappVisitPage();
		verify(youshuAnalysisAddWxappVisitPagePort, times(1)).addWxappVisitPage(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage().contains("addWxappVisitPage_error"));
	}

	private static YoushuSetting row(long companyId, String merchantId) {
		YoushuSetting s = new YoushuSetting();
		s.setCompanyId(companyId);
		s.setMerchantId(merchantId);
		s.setApiUrl("https://api.example.com");
		s.setAppId("app");
		s.setAppSecret("sec");
		return s;
	}

	@Test
	@DisplayName("§3 步骤 1 早退（distribution）：空 list；2.1–2.11/3 不执行")
	void distribution_earlyExit_emptyList_noWeappNoPorts() {
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of());
		service.scheduleAddWxappVisitDistribution();
		verify(weappAuthorizerAppidRepository, never()).findAuthorizerAppid(anyLong(), any());
		verify(visitDistributionPort, never()).postVisitDistribution(any(), any(), any());
		verify(youshuDataSourceApiPort, never()).getOrCreateDataSourceId(any(), anyInt(), any());
		verify(youshuAnalysisAddWxappVisitDistributionPort, never())
				.addWxappVisitDistribution(any(), any(), any());
	}

	@Test
	@DisplayName("§3 步骤 1 早退（distribution）：null list；2.1–2.11/3 不执行")
	void distribution_earlyExit_nullList_noWeappNoPorts() {
		when(youshuSettingMapper.selectList(any())).thenReturn(null);
		service.scheduleAddWxappVisitDistribution();
		verify(weappAuthorizerAppidRepository, never()).findAuthorizerAppid(anyLong(), any());
		verify(visitDistributionPort, never()).postVisitDistribution(any(), any(), any());
	}

	@Test
	@DisplayName("§3 步骤 1+2.1+2.2+2.3 无 appid（distribution）；2.4–2.10 跳过；2.11、3")
	void distribution_missingAppid_noPorts_secondRowStillEvaluated() {
		YoushuSetting a = row(1L, "m1");
		YoushuSetting b = row(2L, "m2");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(a, b));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(1L, "yykweishop")).thenReturn(Optional.empty());
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(2L, "yykweishop")).thenReturn(Optional.of("wxaid"));
		JsonNode visit = sampleVisit();
		when(visitDistributionPort.postVisitDistribution(eq("wxaid"), eq("20260419"), eq("20260419")))
				.thenReturn(visit);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("m2"), eq(8), any(YoushuOpenApiCredentials.class)))
				.thenReturn("ds1");
		service.scheduleAddWxappVisitDistribution();
		verify(weappAuthorizerAppidRepository, times(1)).findAuthorizerAppid(1L, "yykweishop");
		verify(weappAuthorizerAppidRepository, times(1)).findAuthorizerAppid(2L, "yykweishop");
		verify(visitDistributionPort, times(1)).postVisitDistribution(any(), any(), any());
		verify(youshuDataSourceApiPort, times(1)).getOrCreateDataSourceId(any(), anyInt(), any());
		verify(youshuAnalysisAddWxappVisitDistributionPort, times(1))
				.addWxappVisitDistribution(eq("ds1"), eq(visit), any());
	}

	@Test
	@DisplayName("§3 主成功链 2.1–2.10、2.11、3（distribution，data_source_type=8）")
	void distribution_mainSuccess_invokesPortsInOrder() {
		YoushuSetting row = row(10L, "merch");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(10L, "yykweishop"))
				.thenReturn(Optional.of("wxa"));
		JsonNode visit = sampleVisit();
		when(visitDistributionPort.postVisitDistribution("wxa", "20260419", "20260419")).thenReturn(visit);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("merch"), eq(8), any(YoushuOpenApiCredentials.class)))
				.thenReturn("ds99");
		service.scheduleAddWxappVisitDistribution();
		InOrder order = inOrder(visitDistributionPort, youshuDataSourceApiPort, youshuAnalysisAddWxappVisitDistributionPort);
		order.verify(visitDistributionPort).postVisitDistribution("wxa", "20260419", "20260419");
		order.verify(youshuDataSourceApiPort)
				.getOrCreateDataSourceId(eq("merch"), eq(8), any(YoushuOpenApiCredentials.class));
		order.verify(youshuAnalysisAddWxappVisitDistributionPort)
				.addWxappVisitDistribution(eq("ds99"), eq(visit), any());
	}

	@Test
	@DisplayName("§3 2.1 catch：2.5–2.6 postVisitDistribution 失败（distribution）")
	void distribution_visitThrows_swallowed_debugLog_analysisNotCalled() {
		YoushuSetting row = row(3L, "m");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(3L, "yykweishop")).thenReturn(Optional.of("w"));
		when(visitDistributionPort.postVisitDistribution(any(), any(), any()))
				.thenThrow(new RuntimeException("visit-boom"));
		service.scheduleAddWxappVisitDistribution();
		verify(youshuDataSourceApiPort, never()).getOrCreateDataSourceId(any(), anyInt(), any());
		verify(youshuAnalysisAddWxappVisitDistributionPort, never())
				.addWxappVisitDistribution(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("addWxappVisitDistribution_error"));
	}

	@Test
	@DisplayName("§3 2.1 catch：2.9 失败（distribution）；2.10 不执行")
	void distribution_dataSourceThrows_swallowed_debugLog_analysisNotCalled() {
		YoushuSetting row = row(4L, "m");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(4L, "yykweishop")).thenReturn(Optional.of("w"));
		when(visitDistributionPort.postVisitDistribution(any(), any(), any())).thenReturn(sampleVisit());
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(any(), anyInt(), any()))
				.thenThrow(new RuntimeException("ds-boom"));
		service.scheduleAddWxappVisitDistribution();
		verify(youshuAnalysisAddWxappVisitDistributionPort, never())
				.addWxappVisitDistribution(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage().contains("addWxappVisitDistribution_error"));
	}

	@Test
	@DisplayName("§3 2.1 catch：2.10 addWxappVisitDistribution 失败")
	void distribution_analysisThrows_swallowed_debugLog() {
		YoushuSetting row = row(5L, "m");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(5L, "yykweishop")).thenReturn(Optional.of("w"));
		when(visitDistributionPort.postVisitDistribution(any(), any(), any())).thenReturn(sampleVisit());
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(any(), anyInt(), any())).thenReturn("ds");
		doThrow(new RuntimeException("an-boom"))
				.when(youshuAnalysisAddWxappVisitDistributionPort)
				.addWxappVisitDistribution(any(), any(), any());
		service.scheduleAddWxappVisitDistribution();
		verify(youshuAnalysisAddWxappVisitDistributionPort, times(1))
				.addWxappVisitDistribution(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage().contains("addWxappVisitDistribution_error"));
	}

	@Test
	@DisplayName("§3 1 空配置早退；不查 NormalOrders/Trade、不调 data_source / add_order_sum（plan §5）")
	void orderSum_earlyExit_doesNotTouchMappersOrPorts() {
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of());
		service.scheduleAddOrderSum();
		verifyNullOrEmptyNoOrderSideEffects();
	}

	@Test
	@DisplayName("§3 1 null 列表早退；不查聚合、不 HTTP（plan §5）")
	void orderSum_earlyExit_nullList() {
		when(youshuSettingMapper.selectList(any())).thenReturn(null);
		service.scheduleAddOrderSum();
		verifyNullOrEmptyNoOrderSideEffects();
	}

	private void verifyNullOrEmptyNoOrderSideEffects() {
		verify(normalOrdersMapper, never()).sumOrderTotalFeeCentsForYoushu(anyLong(), anyLong(), anyLong());
		verify(tradeMapper, never()).sumTradeTotalFeeCentsForYoushu(anyLong(), anyLong(), anyLong());
		verify(youshuDataSourceApiPort, never()).getOrCreateDataSourceId(any(), anyInt(), any());
		verify(youshuAnalysisAddOrderSumPort, never()).addOrderSum(any(), any(), any());
	}

	@Test
	@DisplayName(
			"§3 2+2.1+2.2+2.3+2.4–2.5+2.6 G1–G3+2.7+2.8+3 主成功链；refDate 与四指标（plan §5）")
	void orderSum_mainSuccess_payloadAndDataSourceType0() {
		YoushuSetting row = row(10L, "merch");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(normalOrdersMapper.sumOrderTotalFeeCentsForYoushu(10L, expectedStartSec(), expectedEndSec()))
				.thenReturn(12_345L);
		when(normalOrdersMapper.countOrdersForYoushu(10L, expectedStartSec(), expectedEndSec())).thenReturn(3L);
		when(tradeMapper.sumTradeTotalFeeCentsForYoushu(10L, expectedStartSec(), expectedEndSec()))
				.thenReturn(9_900L);
		when(tradeMapper.countTradesForYoushu(10L, expectedStartSec(), expectedEndSec())).thenReturn(2L);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("merch"), eq(0), any(YoushuOpenApiCredentials.class)))
				.thenReturn("ds99");
		service.scheduleAddOrderSum();
		ArgumentCaptor<YoushuOrderSumRow> cap = ArgumentCaptor.forClass(YoushuOrderSumRow.class);
		verify(youshuAnalysisAddOrderSumPort, times(1)).addOrderSum(eq("ds99"), cap.capture(), any());
		assertThat(cap.getValue().refDate()).isEqualTo(expectedRefDateString());
		assertThat(cap.getValue().giveOrderAmountSum()).isEqualTo(123.45);
		assertThat(cap.getValue().giveOrderNumSum()).isEqualTo(3L);
		assertThat(cap.getValue().paymentAmountSum()).isEqualTo(99.0);
		assertThat(cap.getValue().payedNumSum()).isEqualTo(2L);
	}

	@Test
	@DisplayName("§3 2.1 catch +2.6 G6：getOrCreate 抛错；2.7 未调；addOrderSum_error")
	void orderSum_dataSourceThrows_addOrderSumNotCalled() {
		YoushuSetting row = row(1L, "m");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(normalOrdersMapper.sumOrderTotalFeeCentsForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(normalOrdersMapper.countOrdersForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(tradeMapper.sumTradeTotalFeeCentsForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(tradeMapper.countTradesForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(any(), anyInt(), any()))
				.thenThrow(new RuntimeException("未查询到腾讯有数对应数据仓库"));
		service.scheduleAddOrderSum();
		verify(youshuAnalysisAddOrderSumPort, never()).addOrderSum(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("addOrderSum_error"));
	}

	@Test
	@DisplayName("§3 2.1 catch：2.6 成功、2.7 抛错；addOrderSum_error")
	void orderSum_addOrderSumThrows_debugOnly() {
		YoushuSetting row = row(2L, "m2");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		stubOrderSumMappers(2L, 0L, 0L, 0L, 0L);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(any(), anyInt(), any())).thenReturn("ds");
		doThrow(new RuntimeException("api-boom")).when(youshuAnalysisAddOrderSumPort).addOrderSum(any(), any(), any());
		service.scheduleAddOrderSum();
		verify(youshuAnalysisAddOrderSumPort, times(1)).addOrderSum(any(), any(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("addOrderSum_error"));
	}

	@Test
	@DisplayName("§3 2.1+2.8+2.2 多公司：首行 2.6 失败、次行成功")
	void orderSum_multiCompany_firstDataSourceFails_secondOk() {
		YoushuSetting a = row(1L, "bad-merch");
		YoushuSetting b = row(2L, "good-merch");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(a, b));
		long ss = expectedStartSec();
		long es = expectedEndSec();
		when(normalOrdersMapper.sumOrderTotalFeeCentsForYoushu(1L, ss, es)).thenReturn(100L);
		when(normalOrdersMapper.countOrdersForYoushu(1L, ss, es)).thenReturn(1L);
		when(tradeMapper.sumTradeTotalFeeCentsForYoushu(1L, ss, es)).thenReturn(0L);
		when(tradeMapper.countTradesForYoushu(1L, ss, es)).thenReturn(0L);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("bad-merch"), eq(0), any()))
				.thenThrow(new RuntimeException("G6"));
		when(normalOrdersMapper.sumOrderTotalFeeCentsForYoushu(2L, ss, es)).thenReturn(200L);
		when(normalOrdersMapper.countOrdersForYoushu(2L, ss, es)).thenReturn(2L);
		when(tradeMapper.sumTradeTotalFeeCentsForYoushu(2L, ss, es)).thenReturn(50L);
		when(tradeMapper.countTradesForYoushu(2L, ss, es)).thenReturn(1L);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("good-merch"), eq(0), any()))
				.thenReturn("ds-ok");
		service.scheduleAddOrderSum();
		verify(youshuAnalysisAddOrderSumPort, times(1)).addOrderSum(eq("ds-ok"), any(), any());
	}

	@Test
	@DisplayName("§3 2.3+§8：聚合 SUM 为 null → 0.0 / 0")
	void orderSum_nullFenCoercesToZero() {
		YoushuSetting row = row(3L, "m3");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		when(normalOrdersMapper.sumOrderTotalFeeCentsForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(null);
		when(normalOrdersMapper.countOrdersForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(tradeMapper.sumTradeTotalFeeCentsForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(null);
		when(tradeMapper.countTradesForYoushu(anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(any(), anyInt(), any())).thenReturn("ds0");
		service.scheduleAddOrderSum();
		ArgumentCaptor<YoushuOrderSumRow> cap = ArgumentCaptor.forClass(YoushuOrderSumRow.class);
		verify(youshuAnalysisAddOrderSumPort).addOrderSum(any(), cap.capture(), any());
		assertThat(cap.getValue().giveOrderAmountSum()).isEqualTo(0.0);
		assertThat(cap.getValue().paymentAmountSum()).isEqualTo(0.0);
		assertThat(cap.getValue().giveOrderNumSum()).isEqualTo(0L);
		assertThat(cap.getValue().payedNumSum()).isEqualTo(0L);
	}

	@Test
	@DisplayName("§3 2.6 子链 G4–G5：getOrCreate 一次、2.7 仍到达（plan §5）")
	void orderSum_getOrCreateCoversG4G5_merged() {
		YoushuSetting row = row(4L, "gm");
		when(youshuSettingMapper.selectList(any())).thenReturn(List.of(row));
		stubOrderSumMappers(4L, 0L, 0L, 0L, 0L);
		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("gm"), eq(0), any())).thenReturn("from-get-or-add");
		service.scheduleAddOrderSum();
		verify(youshuDataSourceApiPort, times(1)).getOrCreateDataSourceId(eq("gm"), eq(0), any());
		verify(youshuAnalysisAddOrderSumPort, times(1))
				.addOrderSum(eq("from-get-or-add"), any(YoushuOrderSumRow.class), any());
	}

	private void stubOrderSumMappers(
			long companyId, Long sumOrderFen, long orderCount, Long payFen, long payCount) {
		long ss = expectedStartSec();
		long es = expectedEndSec();
		when(normalOrdersMapper.sumOrderTotalFeeCentsForYoushu(companyId, ss, es)).thenReturn(sumOrderFen);
		when(normalOrdersMapper.countOrdersForYoushu(companyId, ss, es)).thenReturn(orderCount);
		when(tradeMapper.sumTradeTotalFeeCentsForYoushu(companyId, ss, es)).thenReturn(payFen);
		when(tradeMapper.countTradesForYoushu(companyId, ss, es)).thenReturn(payCount);
	}

	private long expectedStartSec() {
		ZoneId z = clock.getZone();
		LocalDate today = LocalDate.now(clock);
		return today.minusDays(1).atStartOfDay(z).toEpochSecond();
	}

	private long expectedEndSec() {
		ZoneId z = clock.getZone();
		return LocalDate.now(clock).atStartOfDay(z).toEpochSecond();
	}

	private String expectedRefDateString() {
		return expectedStartSec() + "000";
	}

	private static JsonNode sampleVisit() {
		ObjectNode n = OM.createObjectNode();
		n.putArray("list").add(OM.createObjectNode().put("page_path", "/"));
		return n;
	}
}
