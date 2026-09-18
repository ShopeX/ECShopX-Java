package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesLogiDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

@ExtendWith(MockitoExtension.class)
class AftersalesFrontWxappSendbackServiceJushuitanBusPublishProbeTest {

	private static final long COMPANY_ID = 8002L;
	private static final long USER_ID = 20L;
	private static final long ORDER_ID = 5001L;
	private static final long AFTERSALES_BN = 2026050610101010L;
	private static final long DISTRIBUTOR_ID = 7L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	private static PlatformTransactionManager newCommitFiresAfterSynchronizationsTxManager() {
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
		lenient().doNothing().when(txMgr).rollback(any());
		return txMgr;
	}

	@Test
	void sendback_afterCommit_publishesJushuitanBusOnceWithProgress2Payload() {
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformTransactionManager txMgr = newCommitFiresAfterSynchronizationsTxManager();
		JushuitanTradeAftersalesDispatchPublisher dispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		TradeAftersalesLogiDispatchPublisher tradeAftersalesLogiDispatchPublisher =
				mock(TradeAftersalesLogiDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder busPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtBusPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", COMPANY_ID);
		auth.put("user_id", USER_ID);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		Map<String, Object> beforeRow = new LinkedHashMap<>();
		beforeRow.put("aftersales_type", "RETURN_GOODS");
		beforeRow.put("progress", 1);
		beforeRow.put("order_id", ORDER_ID);

		Map<String, Object> afterCommitRow = new LinkedHashMap<>(beforeRow);
		afterCommitRow.put("progress", 2);
		afterCommitRow.put("company_id", COMPANY_ID);
		afterCommitRow.put("user_id", USER_ID);
		afterCommitRow.put("aftersales_bn", AFTERSALES_BN);
		afterCommitRow.put("corp_code", "SF");
		afterCommitRow.put("logi_no", "1234567890");

		when(aftersalesAdminDetailService.loadFullAftersalesForMember(COMPANY_ID, AFTERSALES_BN, USER_ID))
				.thenReturn(beforeRow)
				.thenReturn(afterCommitRow);

		when(aftersalesMapper.update(isNull(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any())).thenReturn(1);

		Aftersales persisted = new Aftersales();
		persisted.setCompanyId(COMPANY_ID);
		persisted.setAftersalesBn(AFTERSALES_BN);
		persisted.setUserId(USER_ID);
		persisted.setOrderId(ORDER_ID);
		persisted.setDistributorId(DISTRIBUTOR_ID);
		persisted.setShopId(1L);
		persisted.setSupplierId(0);
		persisted.setAftersalesType("RETURN_GOODS");
		persisted.setAftersalesStatus(1);
		persisted.setProgress(2);
		persisted.setReason("");
		persisted.setDescription("");
		persisted.setEvidencePic("");
		persisted.setSalesmanId(0L);
		persisted.setContact("");
		persisted.setMobile("");
		persisted.setMerchantId(0L);
		persisted.setSelfDeliveryOperatorId(0L);
		persisted.setIsPartialCancel(false);
		persisted.setReturnType("logistics");
		persisted.setFreight(0);
		persisted.setFreightType("cash");
		persisted.setReturnDistributorId(0L);
		persisted.setAftersalesAddress("");
		when(aftersalesMapper.selectOne(any())).thenReturn(persisted);

		AftersalesFrontWxappSendbackService service =
				new AftersalesFrontWxappSendbackService(
						aftersalesAdminDetailService,
						aftersalesMapper,
						aftersalesDetailMapper,
						orderProcessLogPublishPort,
						applicationEventPublisher,
						aftersalesRefundAsyncPort,
						objectMapper,
						txMgr,
						dispatchPublisher,
						busPayloadBuilder,
						wdtDispatchPublisher,
						wdtBusPayloadBuilder,
						tradeAftersalesLogiDispatchPublisher);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", AFTERSALES_BN);
		merged.put("company_id", COMPANY_ID);
		merged.put("user_id", USER_ID);
		merged.put("corp_code", "SF");
		merged.put("logi_no", "1234567890");

		service.sendback(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> wdtPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logiBusPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		InOrder inOrder =
				inOrder(
						tradeAftersalesLogiDispatchPublisher,
						aftersalesRefundAsyncPort,
						dispatchPublisher,
						applicationEventPublisher,
						wdtDispatchPublisher);
		inOrder.verify(tradeAftersalesLogiDispatchPublisher).publish(logiBusPayloadCaptor.capture());
		Map<String, Object> logiPayload = logiBusPayloadCaptor.getValue();
		assertTrue(logiPayload instanceof LinkedHashMap<?, ?>, "logistics publish should receive a fresh map copy");
		assertEquals(2, ((Number) logiPayload.get("progress")).intValue());
		assertEquals(COMPANY_ID, ((Number) logiPayload.get("company_id")).longValue());
		assertEquals(AFTERSALES_BN, ((Number) logiPayload.get("aftersales_bn")).longValue());
		assertEquals("SF", logiPayload.get("corp_code"));
		assertEquals("1234567890", logiPayload.get("logi_no"));
		inOrder.verify(aftersalesRefundAsyncPort, times(1))
				.scheduleSendAfterSaleWaitConfirmNotice(eq(COMPANY_ID), eq(AFTERSALES_BN));
		inOrder.verify(dispatchPublisher).publish(payloadCaptor.capture());
		inOrder.verify(applicationEventPublisher).publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		inOrder.verify(wdtDispatchPublisher).publish(wdtPayloadCaptor.capture());
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));

		Map<String, Object> busPayload = payloadCaptor.getValue();
		assertEquals(2, ((Number) busPayload.get("progress")).intValue());
		assertEquals(COMPANY_ID, ((Number) busPayload.get("company_id")).longValue());
		assertEquals(ORDER_ID, ((Number) busPayload.get("order_id")).longValue());
		assertEquals(AFTERSALES_BN, ((Number) busPayload.get("aftersales_bn")).longValue());
		assertEquals(DISTRIBUTOR_ID, ((Number) busPayload.get("distributor_id")).longValue());
		Map<String, Object> expectedWdt = wdtBusPayloadBuilder.build(persisted);
		Map<String, Object> wdtPayload = wdtPayloadCaptor.getValue();
		assertEquals(expectedWdt.get("company_id"), wdtPayload.get("company_id"));
		assertEquals(expectedWdt.get("distributor_id"), wdtPayload.get("distributor_id"));
		assertEquals(expectedWdt.get("aftersales_bn"), wdtPayload.get("aftersales_bn"));
		assertEquals(expectedWdt.get("order_id"), wdtPayload.get("order_id"));
		verify(orderProcessLogPublishPort, times(1)).publish(any());
	}

	@Test
	void sendback_invokesOrderProcessLogPublishPortOnce_withSendbackDetailAndParams() {
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformTransactionManager txMgr = newCommitFiresAfterSynchronizationsTxManager();
		JushuitanTradeAftersalesDispatchPublisher dispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		TradeAftersalesLogiDispatchPublisher tradeAftersalesLogiDispatchPublisher =
				mock(TradeAftersalesLogiDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder busPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtBusPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", COMPANY_ID);
		auth.put("user_id", USER_ID);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		Map<String, Object> beforeRow = new LinkedHashMap<>();
		beforeRow.put("aftersales_type", "RETURN_GOODS");
		beforeRow.put("progress", 1);
		beforeRow.put("order_id", ORDER_ID);

		Map<String, Object> afterCommitRow = new LinkedHashMap<>(beforeRow);
		afterCommitRow.put("progress", 2);
		afterCommitRow.put("company_id", COMPANY_ID);
		afterCommitRow.put("user_id", USER_ID);
		afterCommitRow.put("aftersales_bn", AFTERSALES_BN);

		when(aftersalesAdminDetailService.loadFullAftersalesForMember(COMPANY_ID, AFTERSALES_BN, USER_ID))
				.thenReturn(beforeRow)
				.thenReturn(afterCommitRow);

		when(aftersalesMapper.update(isNull(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any())).thenReturn(1);

		Aftersales persisted = new Aftersales();
		persisted.setCompanyId(COMPANY_ID);
		persisted.setAftersalesBn(AFTERSALES_BN);
		persisted.setUserId(USER_ID);
		persisted.setOrderId(ORDER_ID);
		persisted.setDistributorId(DISTRIBUTOR_ID);
		persisted.setShopId(1L);
		persisted.setSupplierId(0);
		persisted.setAftersalesType("RETURN_GOODS");
		persisted.setAftersalesStatus(1);
		persisted.setProgress(2);
		persisted.setReason("");
		persisted.setDescription("");
		persisted.setEvidencePic("");
		persisted.setSalesmanId(0L);
		persisted.setContact("");
		persisted.setMobile("");
		persisted.setMerchantId(0L);
		persisted.setSelfDeliveryOperatorId(0L);
		persisted.setIsPartialCancel(false);
		persisted.setReturnType("logistics");
		persisted.setFreight(0);
		persisted.setFreightType("cash");
		persisted.setReturnDistributorId(0L);
		persisted.setAftersalesAddress("");
		when(aftersalesMapper.selectOne(any())).thenReturn(persisted);

		AftersalesFrontWxappSendbackService service =
				new AftersalesFrontWxappSendbackService(
						aftersalesAdminDetailService,
						aftersalesMapper,
						aftersalesDetailMapper,
						orderProcessLogPublishPort,
						applicationEventPublisher,
						aftersalesRefundAsyncPort,
						objectMapper,
						txMgr,
						dispatchPublisher,
						busPayloadBuilder,
						wdtDispatchPublisher,
						wdtBusPayloadBuilder,
						tradeAftersalesLogiDispatchPublisher);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", AFTERSALES_BN);
		merged.put("company_id", COMPANY_ID);
		merged.put("user_id", USER_ID);
		merged.put("corp_code", "SF");
		merged.put("logi_no", "1234567890");

		service.sendback(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> orderLogCaptor = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(orderLogCaptor.capture());
		Map<String, Object> published = orderLogCaptor.getValue();

		assertEquals(ORDER_ID, toLong(published.get("order_id")));
		assertEquals(COMPANY_ID, toLong(published.get("company_id")));
		assertEquals("user", published.get("operator_type"));
		assertEquals(USER_ID, toLong(published.get("operator_id")));
		assertEquals("订单售后", published.get("remarks"));
		String detailText = String.valueOf(published.get("detail"));
		assertTrue(detailText.contains("寄回"), detailText);
		assertEquals("售后单号：" + AFTERSALES_BN + "，售后单寄回商品", detailText);
		assertFalse(published.containsKey("delivery_remark"));
		assertFalse(published.containsKey("pics"));
		@SuppressWarnings("unchecked")
		Map<String, Object> paramsCaptured = (Map<String, Object>) published.get("params");
		assertEquals(AFTERSALES_BN, toLong(paramsCaptured.get("aftersales_bn")));
		assertEquals(COMPANY_ID, toLong(paramsCaptured.get("company_id")));
		assertEquals(USER_ID, toLong(paramsCaptured.get("user_id")));
		assertEquals("SF", paramsCaptured.get("corp_code"));
		assertEquals("1234567890", paramsCaptured.get("logi_no"));
	}

	@Test
	void sendback_whenPersistedRowMissing_skipsBusPublish() {
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformTransactionManager txMgr = newCommitFiresAfterSynchronizationsTxManager();
		JushuitanTradeAftersalesDispatchPublisher dispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		TradeAftersalesLogiDispatchPublisher tradeAftersalesLogiDispatchPublisher =
				mock(TradeAftersalesLogiDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder busPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);

		Map<String, Object> h5Claims = new LinkedHashMap<>();
		h5Claims.put("company_id", COMPANY_ID);
		h5Claims.put("user_id", USER_ID);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(null);
		when(request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS)).thenReturn(h5Claims);

		Map<String, Object> beforeRow = new LinkedHashMap<>();
		beforeRow.put("aftersales_type", "RETURN_GOODS");
		beforeRow.put("progress", 1);
		beforeRow.put("order_id", ORDER_ID);

		Map<String, Object> afterCommitRow = new LinkedHashMap<>(beforeRow);
		afterCommitRow.put("progress", 2);
		afterCommitRow.put("company_id", COMPANY_ID);
		afterCommitRow.put("user_id", USER_ID);
		afterCommitRow.put("aftersales_bn", AFTERSALES_BN);

		when(aftersalesAdminDetailService.loadFullAftersalesForMember(COMPANY_ID, AFTERSALES_BN, USER_ID))
				.thenReturn(beforeRow)
				.thenReturn(afterCommitRow);

		when(aftersalesMapper.update(isNull(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any())).thenReturn(1);
		when(aftersalesMapper.selectOne(any())).thenReturn(null);

		AftersalesFrontWxappSendbackService service =
				new AftersalesFrontWxappSendbackService(
						aftersalesAdminDetailService,
						aftersalesMapper,
						aftersalesDetailMapper,
						orderProcessLogPublishPort,
						applicationEventPublisher,
						aftersalesRefundAsyncPort,
						objectMapper,
						txMgr,
						dispatchPublisher,
						busPayloadBuilder,
						wdtDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						tradeAftersalesLogiDispatchPublisher);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", AFTERSALES_BN);
		merged.put("company_id", COMPANY_ID);
		merged.put("user_id", USER_ID);
		merged.put("corp_code", "SF");
		merged.put("logi_no", "1234567890");

		service.sendback(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logiWhenMissingCaptor = ArgumentCaptor.forClass(Map.class);
		verify(tradeAftersalesLogiDispatchPublisher, times(1)).publish(logiWhenMissingCaptor.capture());
		Map<String, Object> logiWhenMissing = logiWhenMissingCaptor.getValue();
		assertEquals(2, ((Number) logiWhenMissing.get("progress")).intValue());
		assertEquals(COMPANY_ID, ((Number) logiWhenMissing.get("company_id")).longValue());
		verify(dispatchPublisher, times(0)).publish(any());
		verify(wdtDispatchPublisher, times(0)).publish(any());
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
