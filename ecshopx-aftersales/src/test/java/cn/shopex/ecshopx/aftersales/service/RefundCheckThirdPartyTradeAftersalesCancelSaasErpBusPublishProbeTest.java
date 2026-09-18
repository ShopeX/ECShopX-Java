package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Bus publish boundary for third-party trade-aftersales cancel (reject refund path in
 * {@link AftersalesRefundConfirmService#confirmRefund(Map)}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("refundCheck: third-party trade aftersales cancel Bus publish probe")
class RefundCheckThirdPartyTradeAftersalesCancelSaasErpBusPublishProbeTest {

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
			"Reject refund: confirmRefund invokes third-party cancel publisher once with rejected snapshot payload, before commit")
	void rejectRefund_confirmRefund_invokesThirdPartyCancelPublisherOnce_withRejectedSnapshotPayload() {
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
		param.put("refunds_memo", "拒绝原因说明");
		param.put("refund_fee", 100);
		param.put("refund_point", 0);

		service.confirmRefund(param);

		InOrder ordering = inOrder(txMgr, orderProcessLogPublishPort, thirdPartyTradeAftersalesCancelSaasErpPublisher);
		ordering.verify(txMgr).getTransaction(any());
		ordering.verify(orderProcessLogPublishPort).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> erpCaptor = ArgumentCaptor.forClass(Map.class);
		ordering.verify(thirdPartyTradeAftersalesCancelSaasErpPublisher).publish(erpCaptor.capture());
		ordering.verify(txMgr).commit(any());

		Map<String, Object> published = erpCaptor.getValue();
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(published.get("aftersales_status")).isEqualTo(3);
		assertThat(published.get("progress")).isEqualTo(3);
		assertThat(published.get("aftersales_action")).isEqualTo("cancel");

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}

	@Test
	@DisplayName(
			"Reject refund: confirmRefund with only refund_memo invokes third-party cancel publisher once with rejected snapshot, before commit")
	void rejectRefund_confirmRefund_withRefundMemoOnly_invokesThirdPartyCancelPublisherOnce_withRejectedSnapshotPayload() {
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

		final String adminRefundMemo = "管理员拒绝备注";
		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", COMPANY_ID);
		param.put("aftersales_bn", AFTERSALES_BN);
		param.put("check_refund", false);
		param.put("refund_memo", adminRefundMemo);
		param.put("refund_fee", 100);
		param.put("refund_point", 0);

		service.confirmRefund(param);

		InOrder ordering = inOrder(txMgr, orderProcessLogPublishPort, thirdPartyTradeAftersalesCancelSaasErpPublisher);
		ordering.verify(txMgr).getTransaction(any());
		ordering.verify(orderProcessLogPublishPort).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> erpCaptor = ArgumentCaptor.forClass(Map.class);
		ordering.verify(thirdPartyTradeAftersalesCancelSaasErpPublisher).publish(erpCaptor.capture());
		ordering.verify(txMgr).commit(any());

		Map<String, Object> published = erpCaptor.getValue();
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(published.get("aftersales_status")).isEqualTo(3);
		assertThat(published.get("progress")).isEqualTo(3);
		assertThat(published.get("aftersales_action")).isEqualTo("cancel");
		assertThat(published.get("refuse_reason")).isEqualTo(adminRefundMemo);

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}

	@Test
	@DisplayName("Approve refund: confirmRefund never invokes third-party cancel publisher")
	void approveRefund_confirmRefund_neverInvokesThirdPartyCancelPublisher() {
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

		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, never()).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> updateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(updateCaptor.capture());
		Map<String, Object> updatePayload = updateCaptor.getValue();
		assertThat(updatePayload.get("aftersales_action")).isEqualTo("update");
		assertThat(updatePayload.get("aftersales_status")).isEqualTo(2);
		assertThat(updatePayload.get("progress")).isEqualTo(4);
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
	}
}
