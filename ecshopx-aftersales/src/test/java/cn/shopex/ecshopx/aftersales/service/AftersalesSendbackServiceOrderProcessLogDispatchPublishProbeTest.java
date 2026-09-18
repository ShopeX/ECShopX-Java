package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesLogiDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Admin POST {@code /aftersales/sendback}: asserts {@link OrdersDispatchEventNames#EVENT_ORDER_PROCESS_LOG} is
 * published through {@link OrderProcessLogPublishPortImpl} after transaction commit, with a real port wiring and a
 * mocked {@link DispatchFacade} so {@link DispatchFacade#publishEvent} invocation is the Bus-level proof.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: aftersales sendback publishEvent probe")
class AftersalesSendbackServiceOrderProcessLogDispatchPublishProbeTest {

	private static final long COMPANY_ID = 9101L;
	private static final long ORDER_ID = 5101L;
	private static final long AFTERSALES_BN = 2026050911111111L;

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
	void sendback_withOrderProcessLogPublishPortImpl_afterCommit_invokesDispatchFacadePublishEventOnce_withSendbackPayloadAndParams() {
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformTransactionManager txMgr = newCommitFiresAfterSynchronizationsTxManager();
		JushuitanTradeAftersalesDispatchPublisher dispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder busPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleDispatchPublisher wdtDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtBusPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		TradeAftersalesLogiDispatchPublisher tradeAftersalesLogiDispatchPublisher =
				mock(TradeAftersalesLogiDispatchPublisher.class);

		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", COMPANY_ID);
		jwt.put("operator_type", "merchant");
		jwt.put("operator_id", 77L);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);

		Map<String, Object> beforeCommitRow = new LinkedHashMap<>();
		beforeCommitRow.put("aftersales_type", "RETURN_GOODS");
		beforeCommitRow.put("progress", 1);
		beforeCommitRow.put("order_id", ORDER_ID);

		Map<String, Object> afterCommitRow = new LinkedHashMap<>(beforeCommitRow);
		afterCommitRow.put("progress", 2);

		when(aftersalesAdminDetailService.loadFullAftersalesForAdmin(COMPANY_ID, AFTERSALES_BN))
				.thenReturn(beforeCommitRow)
				.thenReturn(afterCommitRow);

		when(aftersalesMapper.update(isNull(), any())).thenReturn(1);
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.update(isNull(), any())).thenReturn(1);

		Aftersales persisted = new Aftersales();
		persisted.setCompanyId(COMPANY_ID);
		persisted.setAftersalesBn(AFTERSALES_BN);
		persisted.setOrderId(ORDER_ID);
		persisted.setDistributorId(1L);
		persisted.setShopId(1L);
		persisted.setSupplierId(0);
		persisted.setUserId(0L);
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

		AftersalesSendbackService service =
				new AftersalesSendbackService(
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
		merged.put("corp_code", "YTO");
		merged.put("logi_no", "1234567890");

		service.sendback(merged, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> published = payloadCaptor.getValue();
		assertNotNull(published);
		assertEquals(ORDER_ID, ((Number) published.get("order_id")).longValue());
		assertEquals(COMPANY_ID, ((Number) published.get("company_id")).longValue());
		assertEquals("merchant", published.get("operator_type"));
		assertEquals(77L, ((Number) published.get("operator_id")).longValue());
		assertEquals("订单售后", published.get("remarks"));
		assertEquals("售后单号：" + AFTERSALES_BN + "，售后单寄回商品", published.get("detail"));

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) published.get("params");
		assertNotNull(params);
		assertEquals(AFTERSALES_BN, ((Number) params.get("aftersales_bn")).longValue());
		assertEquals("YTO", params.get("corp_code"));
		assertEquals("1234567890", params.get("logi_no"));
		assertEquals(COMPANY_ID, ((Number) params.get("company_id")).longValue());
		assertEquals("merchant", params.get("operator_type"));
		assertEquals(77L, ((Number) params.get("operator_id")).longValue());
	}
}
