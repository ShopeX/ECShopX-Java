package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.NormalOrderAutoCloseAftersalesCronPort;
import cn.shopex.ecshopx.common.port.order.PendingAutoCloseOrderItemRow;
import cn.shopex.ecshopx.common.port.payment.AdapayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.common.port.payment.BspayScheduleAutoPaymentConfirmationPort;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AftersalesServiceScheduleCloseOrderItemTest {

	@Mock
	private NormalOrderAutoCloseAftersalesCronPort cronPort;

	@Mock
	private AdapayScheduleAutoPaymentConfirmationPort adapayPort;

	@Mock
	private BspayScheduleAutoPaymentConfirmationPort bspayPort;

	private AftersalesService service;

	private ListAppender<ILoggingEvent> serviceLogAppender;
	private Logger serviceLogger;

	@BeforeEach
	void init() {
		service =
				new AftersalesService(
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						cronPort,
						adapayPort,
						bspayPort,
						TransactionOperations.withoutTransaction(),
						null,
						null,
						null,
						Mockito.mock(JushuitanTradeAftersalesDispatchPublisher.class),
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						Mockito.mock(TradeAftersalesCancelDispatchPublisher.class),
						Mockito.mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class));
		serviceLogAppender = new ListAppender<>();
		serviceLogAppender.start();
		serviceLogAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(AftersalesService.class);
		serviceLogger.addAppender(serviceLogAppender);
	}

	@AfterEach
	void detachServiceLog() {
		serviceLogger.detachAppender(serviceLogAppender);
		serviceLogAppender.stop();
	}

	@Test
	@DisplayName(
			"§3 步骤 1：countPendingItems(now) 时间基准；筛选条件由 Port 实现（order_status=DONE、order_class=normal、子单 auto_close_aftersales_time<=now 且 IS NOT NULL、子单 aftersales_status IS NULL）")
	void step1_countPendingItemsReceivesEpochSecNow() {
		ArgumentCaptor<Integer> nowCap = ArgumentCaptor.forClass(Integer.class);
		when(cronPort.countPendingItems(nowCap.capture())).thenReturn(0L);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isZero();
		int captured = nowCap.getValue();
		int expected = (int) (System.currentTimeMillis() / 1000L);
		assertThat(Math.abs(captured - expected)).isLessThanOrEqualTo(2);
	}

	@Test
	@DisplayName("§3 步骤 2：分页 listPendingItems 的 pageSize 恒为 20")
	void step2_pageSizeIs20() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(1L);
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of());
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isZero();
		verify(cronPort).listPendingItems(anyInt(), eq(1), eq(20));
	}

	@Test
	@DisplayName("§3 步骤 3：totalCount 驱动分页，ceil(N/20) 次 listPendingItems")
	void step3_totalCountDrivesPageLoop() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(45L);
		when(cronPort.listPendingItems(anyInt(), anyInt(), eq(20))).thenReturn(List.of());
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isZero();
		verify(cronPort, times(3)).listPendingItems(anyInt(), anyInt(), eq(20));
		verify(cronPort).listPendingItems(anyInt(), eq(1), eq(20));
		verify(cronPort).listPendingItems(anyInt(), eq(2), eq(20));
		verify(cronPort).listPendingItems(anyInt(), eq(3), eq(20));
	}

	@Test
	@DisplayName("§3 步骤 4：日志出现 totalCount 与现网 key 一致")
	void step4_logsTotalCount() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(7L);
		when(cronPort.listPendingItems(anyInt(), anyInt(), eq(20))).thenReturn(List.of());
		service.scheduleAutoCloseOrderItemAftersales();
		assertThat(serviceLogAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("totalCount")
								&& e.getFormattedMessage().contains("7"));
	}

	@Test
	@DisplayName("§3 步骤 5：totalPage — N=21 为 2 页；N=20 为 1 页")
	void step5_totalPageFromTotalCount() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(21L);
		when(cronPort.listPendingItems(anyInt(), anyInt(), eq(20))).thenReturn(List.of());
		service.scheduleAutoCloseOrderItemAftersales();
		verify(cronPort, times(2)).listPendingItems(anyInt(), anyInt(), eq(20));

		reset(cronPort);
		when(cronPort.countPendingItems(anyInt())).thenReturn(20L);
		when(cronPort.listPendingItems(anyInt(), anyInt(), eq(20))).thenReturn(List.of());
		service.scheduleAutoCloseOrderItemAftersales();
		verify(cronPort, times(1)).listPendingItems(anyInt(), anyInt(), eq(20));
	}

	@Test
	@DisplayName("§3 步骤 6：早退出无循环 — count=0 不调用 listPendingItems，返回 0，无 7-B 内逻辑")
	void step6_whenNoPending_returns0WithoutList() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(0L);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isZero();
		verify(cronPort, never()).listPendingItems(anyInt(), anyInt(), anyInt());
		verify(adapayPort, never()).scheduleAutoPaymentConfirmation(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
		verify(bspayPort, never()).scheduleAutoPaymentConfirmation(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	@DisplayName("§3 步骤 7：非零进主流程 — count>0 时进入 7-A～7-B-4")
	void step7_whenPositive_entersMainFlow() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(1L);
		var row = new PendingAutoCloseOrderItemRow(99L, 1L, 2L, "wxpay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(row));
		when(cronPort.updateItemAftersalesClosed(99L)).thenReturn(1);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isEqualTo(1);
		verify(cronPort).listPendingItems(anyInt(), eq(1), eq(20));
	}

	@Test
	@DisplayName("§3 步骤 7-A：bspay Map 跨页累积 — 页末批调含前一页已收集的主单")
	void step7A_bspayMapAccumulatesAcrossPages() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(40L);
		var p1 = new PendingAutoCloseOrderItemRow(1L, 10L, 100L, "bspay");
		var p2 = new PendingAutoCloseOrderItemRow(2L, 10L, 200L, "bspay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(p1));
		when(cronPort.listPendingItems(anyInt(), eq(2), eq(20))).thenReturn(List.of(p2));
		when(cronPort.updateItemAftersalesClosed(org.mockito.ArgumentMatchers.anyLong())).thenReturn(1);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isEqualTo(2);
		verify(bspayPort, times(2)).scheduleAutoPaymentConfirmation(10L, 100L);
		verify(bspayPort, times(1)).scheduleAutoPaymentConfirmation(10L, 200L);
	}

	@Test
	@DisplayName("§3 步骤 7-B：totalPage=2 时 listPendingItems 依次 page=1、page=2")
	void step7B_paginationTwoPages() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(25L);
		when(cronPort.listPendingItems(anyInt(), anyInt(), eq(20))).thenReturn(List.of());
		service.scheduleAutoCloseOrderItemAftersales();
		verify(cronPort).listPendingItems(anyInt(), eq(1), eq(20));
		verify(cronPort).listPendingItems(anyInt(), eq(2), eq(20));
	}

	@Test
	@DisplayName(
			"§3 步骤 7-B-1：第二页 listPendingItems(page=2,pageSize=20)；取列排序 ORDER BY auto_close_aftersales_time ASC（ecshopx-orders NormalOrderAutoCloseAftersalesMapper.xml）")
	void step7B1_secondPageListed() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(21L);
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of());
		when(cronPort.listPendingItems(anyInt(), eq(2), eq(20))).thenReturn(List.of());
		service.scheduleAutoCloseOrderItemAftersales();
		verify(cronPort).listPendingItems(anyInt(), eq(2), eq(20));
	}

	@Test
	@DisplayName("§3 步骤 7-B-2（无支付确认分支）：pay_type=wxpay 仅 updateItemAftersalesClosed，不调两支付 Port")
	void step7B2_wxpay_closesItemOnly_noPaymentPorts() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(1L);
		var row = new PendingAutoCloseOrderItemRow(50L, 8L, 900L, "wxpay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(row));
		when(cronPort.updateItemAftersalesClosed(50L)).thenReturn(1);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isEqualTo(1);
		verify(adapayPort, never()).scheduleAutoPaymentConfirmation(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
		verify(bspayPort, never()).scheduleAutoPaymentConfirmation(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	@DisplayName(
			"§3 步骤 7-B-2（pay_type==adapay）·§3 scheduleAutoPaymentConfirmation 步骤 B·§3 scheduleAutoPaymentConfirmation 步骤 C：关单后调 Adapay Port；B/C 子单 CLOSED/INSERT 语义见 ecshopx-adapay 单测")
	void step7B2_adapay_invokesAdapayPort() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(1L);
		var row = new PendingAutoCloseOrderItemRow(10L, 1L, 100L, "adapay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(row));
		when(cronPort.updateItemAftersalesClosed(10L)).thenReturn(1);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isEqualTo(1);
		verify(adapayPort, times(1)).scheduleAutoPaymentConfirmation(1L, 100L);
		verify(bspayPort, never()).scheduleAutoPaymentConfirmation(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	@DisplayName(
			"§3 步骤 7-B-2（pay_type==bspay）·§3 scheduleAutoPaymentConfirmation 步骤 B·§3 scheduleAutoPaymentConfirmation 步骤 C：循环内入 Map、页末批调 Bspay Port；B/C 语义见 ecshopx-bspay 单测")
	void step7B2_bspay_defersToPageEnd() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(1L);
		var row = new PendingAutoCloseOrderItemRow(2L, 3L, 9L, "bspay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(row));
		when(cronPort.updateItemAftersalesClosed(2L)).thenReturn(1);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isEqualTo(1);
		verify(adapayPort, never()).scheduleAutoPaymentConfirmation(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
		verify(bspayPort, times(1)).scheduleAutoPaymentConfirmation(3L, 9L);
	}

	@Test
	@DisplayName("§3 步骤 7-B-3：Map 非空时日志含 bspayOrderIds")
	void step7B3_logsBspayOrderIdsWhenMapNonEmpty() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(1L);
		var row = new PendingAutoCloseOrderItemRow(2L, 3L, 9L, "bspay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(row));
		when(cronPort.updateItemAftersalesClosed(2L)).thenReturn(1);
		service.scheduleAutoCloseOrderItemAftersales();
		assertThat(serviceLogAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("bspayOrderIds"));
	}

	@Test
	@DisplayName("§3 步骤 7-B-4：同一主单 bspay 跨两页 — 页末对同一 order_id 再次调 BspayScheduleAutoPaymentConfirmationPort")
	void step7B4_sameBspayOrderAcrossPages_invokesPortTwiceForSameKey() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(40L);
		var page1 = new PendingAutoCloseOrderItemRow(1L, 5L, 77L, "bspay");
		var page2 = new PendingAutoCloseOrderItemRow(2L, 5L, 77L, "bspay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(page1));
		when(cronPort.listPendingItems(anyInt(), eq(2), eq(20))).thenReturn(List.of(page2));
		when(cronPort.updateItemAftersalesClosed(org.mockito.ArgumentMatchers.anyLong())).thenReturn(1);
		assertThat(service.scheduleAutoCloseOrderItemAftersales()).isEqualTo(2);
		verify(bspayPort, times(2)).scheduleAutoPaymentConfirmation(5L, 77L);
	}

	@Test
	@DisplayName("§3 scheduleAutoPaymentConfirmation 步骤 A：编排层向 Port 传入 company_id 与 order_id")
	void scheduleAutoPaymentConfirmation_stepA_companyIdAndOrderIdPassed() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(1L);
		var row = new PendingAutoCloseOrderItemRow(10L, 1L, 100L, "adapay");
		when(cronPort.listPendingItems(anyInt(), eq(1), eq(20))).thenReturn(List.of(row));
		when(cronPort.updateItemAftersalesClosed(10L)).thenReturn(1);
		service.scheduleAutoCloseOrderItemAftersales();
		var companyCap = ArgumentCaptor.forClass(Long.class);
		var orderCap = ArgumentCaptor.forClass(Long.class);
		verify(adapayPort).scheduleAutoPaymentConfirmation(companyCap.capture(), orderCap.capture());
		assertThat(companyCap.getValue()).isEqualTo(1L);
		assertThat(orderCap.getValue()).isEqualTo(100L);
	}

	@Test
	@DisplayName("§3 步骤 3+4+5：总数、日志 totalCount、totalPage 可在一个 count=21 流程中同时观测")
	void step3_4_5_combined_observableInOneRun() {
		when(cronPort.countPendingItems(anyInt())).thenReturn(21L);
		when(cronPort.listPendingItems(anyInt(), anyInt(), eq(20))).thenReturn(List.of());
		service.scheduleAutoCloseOrderItemAftersales();
		verify(cronPort, times(2)).listPendingItems(anyInt(), anyInt(), eq(20));
		assertThat(serviceLogAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("totalCount")
								&& e.getFormattedMessage().contains("21"));
	}
}
