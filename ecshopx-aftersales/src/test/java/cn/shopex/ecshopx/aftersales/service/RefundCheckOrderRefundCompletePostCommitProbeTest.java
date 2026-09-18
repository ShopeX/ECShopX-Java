package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import cn.shopex.ecshopx.orders.event.WdtErpTradeAfterSaleSyncSpringEvent;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 聚焦「售后退款审核」路径：同意路径在事务提交后调度 OrderRefundComplete、旺店通统一 Bus；拒绝路径校验不调度该 Job，
 * 并对 {@link OrderProcessLogPublishPort#publish(Map)} 的拒绝退款 {@code detail}（与退款审核接口对齐）做载荷断言；
 * 商家端与运营后台共用同一套退款审核确认服务，本夹具以 Mockito 与手写事务管理器模拟提交边界。
 */
@DisplayName("售后退款审核：事务提交后的旺店通统一 Bus 投递（同意 / 拒绝边界）")
@ExtendWith(MockitoExtension.class)
class RefundCheckOrderRefundCompletePostCommitProbeTest {

	private static final long COMPANY_ID = 8001L;
	private static final long ORDER_ID = 4001L;
	private static final long AFTERSALES_BN = 202602061111222L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	@DisplayName(
			"Shop/buyer refund-check success path matches Admin: schedules orderRefundComplete once after transaction commit")
	void approveRefund_afterTransactionCommit_schedulesOrderRefundCompleteOnce() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(3L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);
		aftersales.setUserId(0L);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", true);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);
		param.put("operator_id", 99L);

		service.confirmRefund(param);

		Map<String, Object> expectedBus =
				payloadBuilder.build(aftersales, new LinkedHashMap<>());
		Map<String, Object> expectedWdtBus = wdtPayloadBuilder.build(aftersales);

		InOrder crossOrder =
				inOrder(
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						aftersalesRefundAsyncPort,
						jushuitanPublisher,
						applicationEventPublisher,
						wdtPublisher,
						aftersalesRefundAsyncPort);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> saasErpCaptor = ArgumentCaptor.forClass(Map.class);
		crossOrder
				.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1))
				.publish(saasErpCaptor.capture());
		assertThat(saasErpCaptor.getValue().get("aftersales_action")).isEqualTo("update");
		assertThat(saasErpCaptor.getValue().get("aftersales_status")).isEqualTo(2);
		assertThat(saasErpCaptor.getValue().get("progress")).isEqualTo(4);
		crossOrder
				.verify(aftersalesRefundAsyncPort, times(1))
				.scheduleOrderRefundComplete(eq(COMPANY_ID), eq(ORDER_ID));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> busCaptorApprove = ArgumentCaptor.forClass(Map.class);
		crossOrder.verify(jushuitanPublisher, times(1)).publish(busCaptorApprove.capture());
		assertThat(busCaptorApprove.getValue().keySet()).isEqualTo(expectedBus.keySet());
		assertThat(busCaptorApprove.getValue().get("aftersales_status")).isEqualTo(2);
		assertThat(busCaptorApprove.getValue().get("progress")).isEqualTo(4);
		crossOrder
				.verify(applicationEventPublisher, times(1))
				.publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> wdtBusCaptor = ArgumentCaptor.forClass(Map.class);
		crossOrder.verify(wdtPublisher, times(1)).publish(wdtBusCaptor.capture());
		assertThat(wdtBusCaptor.getValue()).isEqualTo(expectedWdtBus);
		ArgumentCaptor<Map<String, Object>> invoiceRedCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		crossOrder.verify(aftersalesRefundAsyncPort, times(1)).scheduleInvoiceRed(invoiceRedCaptor.capture());

		Map<String, Object> invoiceRedPayload = invoiceRedCaptor.getValue();
		assertThat(invoiceRedPayload.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(invoiceRedPayload.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(invoiceRedPayload.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(invoiceRedPayload.get("aftersales_status")).isEqualTo(2);
		assertThat(invoiceRedPayload.get("progress")).isEqualTo(4);
		verify(applicationEventPublisher, never()).publishEvent(any(WdtErpTradeAfterSaleSyncSpringEvent.class));
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, never()).publish(any());
	}

	@Test
	void rejectRefund_neverSchedulesOrderRefundComplete() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(7L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", false);
		param.put("refunds_memo", "拒绝原因");
		param.put("refund_fee", 100);
		param.put("refund_point", 0);

		service.confirmRefund(param);

		verify(aftersalesRefundAsyncPort, never()).scheduleOrderRefundComplete(anyLong(), anyLong());
		verify(aftersalesRefundAsyncPort, never()).scheduleInvoiceRed(any());
		verify(jushuitanPublisher, times(1)).publish(any());
		verify(wdtPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, times(1)).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}

	@Test
	@DisplayName(
			"Shop refund check reject after commit: single Jushuitan Bus publish and Jushuitan sync Spring event (Admin refund-check parity)")
	void rejectRefund_afterTransactionCommit_publishesJushuitanBusOnce_andStillPublishesJushuitanSyncSpringEvent() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(5L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		String refuseMemo = "拒绝原因";
		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", false);
		param.put("refunds_memo", refuseMemo);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);

		service.confirmRefund(param);

		Map<String, Object> expectedBus = payloadBuilder.build(aftersales, new LinkedHashMap<>());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> busCaptor = ArgumentCaptor.forClass(Map.class);
		verify(jushuitanPublisher, times(1)).publish(busCaptor.capture());
		assertThat(busCaptor.getValue().keySet()).isEqualTo(expectedBus.keySet());
		assertThat(busCaptor.getValue().get("aftersales_status")).isEqualTo(3);
		assertThat(busCaptor.getValue().get("progress")).isEqualTo(3);

		@SuppressWarnings({"unchecked", "rawtypes"})
		ArgumentCaptor<ApplicationEvent> evtCaptor = ArgumentCaptor.forClass((Class) ApplicationEvent.class);
		verify(applicationEventPublisher, atLeastOnce()).publishEvent(evtCaptor.capture());
		List<JushuitanTradeAftersalesSyncSpringEvent> jwtEvents =
				evtCaptor.getAllValues().stream()
						.filter(JushuitanTradeAftersalesSyncSpringEvent.class::isInstance)
						.map(JushuitanTradeAftersalesSyncSpringEvent.class::cast)
						.toList();
		assertThat(jwtEvents).hasSize(1);
		assertThat(jwtEvents.get(0).getPayload().get("aftersales_status")).isEqualTo(3);
		assertThat(jwtEvents.get(0).getPayload().get("progress")).isEqualTo(3);
		assertThat(jwtEvents.get(0).getPayload().get("refuse_reason")).isEqualTo(refuseMemo);
		verify(wdtPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, times(1)).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}

	@Test
	@DisplayName(
			"Reject refund (refundCheck): OrderProcessLogPublishPort receives one publish with 拒绝退款 detail and user operator")
	void rejectRefund_invokesOrderProcessLogPublishPortOnce_withRejectDetail() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(7L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		String refundsMemo = "质检不合格";
		String bnLabel = String.valueOf(AFTERSALES_BN);
		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", false);
		param.put("refunds_memo", refundsMemo);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);
		param.put("user_id", 42L);

		service.confirmRefund(param);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		Map<String, Object> published = logCaptor.getValue();
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("operator_type")).isEqualTo("user");
		assertThat(published.get("operator_id")).isEqualTo(42L);
		assertThat(published.get("remarks")).isEqualTo("订单售后");
		assertThat(published.get("detail"))
				.isEqualTo("售后单号：" + bnLabel + " 拒绝退款，拒绝退款原因：" + refundsMemo);
		@SuppressWarnings("unchecked")
		Map<String, Object> paramEcho = (Map<String, Object>) published.get("params");
		assertThat(paramEcho).isNotNull();
		assertThat(paramEcho.get("check_refund")).isEqualTo(false);
		assertThat(paramEcho.get("refunds_memo")).isEqualTo(refundsMemo);

		verify(aftersalesRefundAsyncPort, never()).scheduleOrderRefundComplete(anyLong(), anyLong());
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, times(1)).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}

	@Test
	@DisplayName(
			"Agree refund (refundCheck): OrderProcessLogPublishPort receives one publish with 同意退款 detail and admin operator")
	void agreeRefund_invokesOrderProcessLogPublishPortOnce_withAgreeDetail() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(7L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);
		aftersales.setUserId(0L);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		String bnLabel = String.valueOf(AFTERSALES_BN);
		long operatorId = 99L;
		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", true);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);
		param.put("operator_id", operatorId);

		service.confirmRefund(param);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		Map<String, Object> published = logCaptor.getValue();
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("operator_type")).isEqualTo("admin");
		assertThat(published.get("operator_id")).isEqualTo(operatorId);
		assertThat(published.get("remarks")).isEqualTo("订单售后");
		assertThat(published.get("detail")).isEqualTo("售后单号：" + bnLabel + "，售后单同意退款");
		@SuppressWarnings("unchecked")
		Map<String, Object> paramEcho = (Map<String, Object>) published.get("params");
		assertThat(paramEcho).isNotNull();
		assertThat(paramEcho.get("check_refund")).isEqualTo(true);
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(any());
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
	}

	@Test
	@DisplayName(
			"Agree refund: order process log uses aftersales user id when set, overriding request operator_id")
	void agreeRefund_orderProcessLogPublish_operatorIdPrefersAftersalesUserIdOverParam() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		long buyerUserId = 501L;
		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(7L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);
		aftersales.setUserId(buyerUserId);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		String bnLabel = String.valueOf(AFTERSALES_BN);
		long paramOperatorId = 99L;
		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", true);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);
		param.put("operator_id", paramOperatorId);

		service.confirmRefund(param);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		Map<String, Object> published = logCaptor.getValue();
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("operator_type")).isEqualTo("admin");
		assertThat(published.get("operator_id")).isEqualTo(buyerUserId);
		assertThat(published.get("operator_id")).isNotEqualTo(paramOperatorId);
		assertThat(published.get("remarks")).isEqualTo("订单售后");
		assertThat(published.get("detail")).isEqualTo("售后单号：" + bnLabel + "，售后单同意退款");
		@SuppressWarnings("unchecked")
		Map<String, Object> paramEcho = (Map<String, Object>) published.get("params");
		assertThat(paramEcho).isNotNull();
		assertThat(paramEcho.get("check_refund")).isEqualTo(true);
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(any());
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
	}

	@Test
	@DisplayName(
			"Agree refund (Admin refundCheck): merged param map as AftersalesController#refundCheck, check_refund=true; agree detail, params echo, operator_id from aftersales user when request lacks operator_id")
	void agreeRefund_refundCheck_adminMergedParams_checkRefundTrue_documentsAgreeDetailAndParamsEcho() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		long buyerUserId = 602L;
		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(7L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);
		aftersales.setUserId(buyerUserId);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		String bnLabel = String.valueOf(AFTERSALES_BN);
		String refundMemo = "退款审核备注";
		LinkedHashMap<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", true);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);
		param.put("refund_memo", refundMemo);
		assertThat(param.containsKey("operator_id")).isFalse();

		service.confirmRefund(param);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		Map<String, Object> published = logCaptor.getValue();
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("operator_type")).isEqualTo("admin");
		assertThat(published.get("operator_id")).isEqualTo(buyerUserId);
		assertThat(published.get("remarks")).isEqualTo("订单售后");
		assertThat(published.get("detail")).isEqualTo("售后单号：" + bnLabel + "，售后单同意退款");
		@SuppressWarnings("unchecked")
		Map<String, Object> paramEcho = (Map<String, Object>) published.get("params");
		assertThat(paramEcho).isEqualTo(param);
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(any());
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
	}

	@Test
	@DisplayName(
			"Reject refund (Admin key probe): only refund_memo in param, no refunds_memo; documents detail suffix vs str(param.get(\"refunds_memo\")) and params echo")
	void rejectRefund_onlyRefundMemo_withoutRefundsMemo_documentsDetailAndParamsEcho() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(7L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		String refundMemoOnly = "后台单键拒绝原因";
		String bnLabel = String.valueOf(AFTERSALES_BN);
		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", false);
		param.put("refund_memo", refundMemoOnly);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);
		param.put("user_id", 55L);

		service.confirmRefund(param);

		assertThat(param.containsKey("refunds_memo")).isFalse();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		Map<String, Object> published = logCaptor.getValue();
		assertThat(published.get("detail"))
				.isEqualTo("售后单号：" + bnLabel + " 拒绝退款，拒绝退款原因：" + refundMemoOnly);
		@SuppressWarnings("unchecked")
		Map<String, Object> paramEcho = (Map<String, Object>) published.get("params");
		assertThat(paramEcho).isNotNull();
		assertThat(paramEcho.get("refund_memo")).isEqualTo(refundMemoOnly);
		assertThat(paramEcho.containsKey("refunds_memo")).isFalse();
		assertThat(paramEcho.get("check_refund")).isEqualTo(false);

		verify(aftersalesRefundAsyncPort, never()).scheduleOrderRefundComplete(anyLong(), anyLong());
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, times(1)).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}

	@Test
	@DisplayName("Reject refund order process log: operator_id defaults to 0 when user_id absent")
	void rejectRefund_orderProcessLogPublish_operatorIdZeroWithoutUserId() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder payloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtPublisher = mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(COMPANY_ID);
		aftersales.setAftersalesBn(AFTERSALES_BN);
		aftersales.setOrderId(ORDER_ID);
		aftersales.setDistributorId(7L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						jushuitanPublisher,
						payloadBuilder,
						wdtPublisher,
						wdtPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

		String refundsMemo = "缺货";
		String bnLabel = String.valueOf(AFTERSALES_BN);
		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", false);
		param.put("refunds_memo", refundsMemo);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);

		service.confirmRefund(param);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		assertThat(logCaptor.getValue().get("operator_id")).isEqualTo(0L);
		assertThat(logCaptor.getValue().get("detail"))
				.isEqualTo("售后单号：" + bnLabel + " 拒绝退款，拒绝退款原因：" + refundsMemo);
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, times(1)).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}
}
