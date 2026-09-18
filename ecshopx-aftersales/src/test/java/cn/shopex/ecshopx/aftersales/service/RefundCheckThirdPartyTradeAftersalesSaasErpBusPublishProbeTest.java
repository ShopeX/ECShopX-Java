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
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import jakarta.servlet.http.HttpServletRequest;
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
 * Bus publish for third-party trade-aftersales SaaS ERP update (approve refund path in
 * {@link AftersalesRefundConfirmService#confirmRefund(Map)}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("refundCheck: third-party trade aftersales SaaS ERP update Bus publish probe")
class RefundCheckThirdPartyTradeAftersalesSaasErpBusPublishProbeTest {

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
			"Agree refund: confirmRefund invokes third-party SaaS ERP publisher once with agreed snapshot payload, before commit")
	void agreeRefund_confirmRefund_invokesThirdPartySaasErpPublisherOnce_withAgreedSnapshotPayload() {
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

		InOrder ordering = inOrder(txMgr, orderProcessLogPublishPort, thirdPartyTradeAftersalesSaasErpDispatchPublisher);
		ordering.verify(txMgr).getTransaction(any());
		ordering.verify(orderProcessLogPublishPort).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> erpCaptor = ArgumentCaptor.forClass(Map.class);
		ordering.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher).publish(erpCaptor.capture());
		ordering.verify(txMgr).commit(any());

		Map<String, Object> published = erpCaptor.getValue();
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(published.get("aftersales_status")).isEqualTo(2);
		assertThat(published.get("progress")).isEqualTo(4);
		assertThat(published.get("aftersales_action")).isEqualTo("update");

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, never()).publish(any());
	}

	@Test
	@DisplayName(
			"Agree refund (Admin refundCheck path): refundCheck merges JWT, sets operator_type admin, publishes SaaS ERP update before commit")
	void agreeRefund_refundCheck_serviceEntry_invokesThirdPartySaasErpPublisherOnce_withAdminWxappShapedMergedParams() {
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

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(Map.of("company_id", COMPANY_ID, "operator_id", 77L, "operator_type", "admin"));

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

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", AFTERSALES_BN);
		merged.put("check_refund", true);
		merged.put("refund_fee", 100);
		merged.put("refund_point", 0);
		merged.put("refund_memo", "wxapp 审核备注");

		service.refundCheck(merged, request);

		InOrder ordering = inOrder(txMgr, orderProcessLogPublishPort, thirdPartyTradeAftersalesSaasErpDispatchPublisher);
		ordering.verify(txMgr).getTransaction(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
		ordering.verify(orderProcessLogPublishPort).publish(logCaptor.capture());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> erpCaptor = ArgumentCaptor.forClass(Map.class);
		ordering.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher).publish(erpCaptor.capture());
		ordering.verify(txMgr).commit(any());

		Map<String, Object> logPublished = logCaptor.getValue();
		assertThat(logPublished.get("operator_type")).isEqualTo("admin");
		@SuppressWarnings("unchecked")
		Map<String, Object> paramsEcho = (Map<String, Object>) logPublished.get("params");
		assertThat(paramsEcho).isNotNull();
		assertThat(paramsEcho.get("operator_type")).isEqualTo("admin");

		Map<String, Object> published = erpCaptor.getValue();
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(published.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertThat(published.get("aftersales_status")).isEqualTo(2);
		assertThat(published.get("progress")).isEqualTo(4);
		assertThat(published.get("aftersales_action")).isEqualTo("update");

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, never()).publish(any());
	}

	@Test
	@DisplayName("Reject refund: confirmRefund never invokes third-party SaaS ERP update publisher")
	void rejectRefund_confirmRefund_neverInvokesThirdPartySaasErpPublisher() {
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
		param.put("refunds_memo", "拒绝原因");
		param.put("refund_fee", 100);
		param.put("refund_point", 0);

		service.confirmRefund(param);

		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesCancelSaasErpPublisher, times(1)).publish(any());
	}
}
