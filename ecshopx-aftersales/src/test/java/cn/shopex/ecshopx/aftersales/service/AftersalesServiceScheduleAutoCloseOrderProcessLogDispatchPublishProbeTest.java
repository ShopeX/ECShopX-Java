package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAutoRefuseWxaTemplatePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCancelNoticeJobPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderAutoCloseAftersalesCronPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.payment.AdapayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.common.port.payment.BspayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Scheduled auto-close aftersales: asserts {@link OrdersDispatchEventNames#EVENT_ORDER_PROCESS_LOG} is published
 * through {@link OrderProcessLogPublishPortImpl} after transaction commit, with a mocked {@link DispatchFacade}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: schedule auto-close aftersales publishEvent probe")
class AftersalesServiceScheduleAutoCloseOrderProcessLogDispatchPublishProbeTest {

	private static final long COMPANY_ID = 9203L;
	private static final long ORDER_ID = 5203L;
	private static final long AFTERSALES_BN = 2026050913333333L;

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	private static TransactionOperations newCommitFiresAfterSynchronizationsTxOps() {
		return new TransactionOperations() {
			@Override
			public <T> T execute(TransactionCallback<T> callback) {
				boolean started = !TransactionSynchronizationManager.isSynchronizationActive();
				if (started) {
					TransactionSynchronizationManager.initSynchronization();
				}
				try {
					T result = callback.doInTransaction(new SimpleTransactionStatus(true));
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization s :
								TransactionSynchronizationManager.getSynchronizations()) {
							s.afterCommit();
						}
					}
					return result;
				} finally {
					if (started) {
						TransactionSynchronizationManager.clear();
					}
				}
			}
		};
	}

	@Test
	void closeAftersalesForSchedule_withOrderProcessLogPublishPortImpl_afterCommit_invokesDispatchFacadePublishEventOnce_withSchedulePayload() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		AftersalesAutoRefuseWxaTemplatePort aftersalesAutoRefuseWxaTemplatePort =
				mock(AftersalesAutoRefuseWxaTemplatePort.class);
		NormalOrderAutoCloseAftersalesCronPort normalOrderAutoCloseAftersalesCronPort =
				mock(NormalOrderAutoCloseAftersalesCronPort.class);
		AdapayScheduleAutoPaymentConfirmationPort adapayScheduleAutoPaymentConfirmationPort =
				mock(AdapayScheduleAutoPaymentConfirmationPort.class);
		BspayScheduleAutoPaymentConfirmationPort bspayScheduleAutoPaymentConfirmationPort =
				mock(BspayScheduleAutoPaymentConfirmationPort.class);
		CompanysMapper companysMapper = mock(CompanysMapper.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesCancelNoticeJobPort aftersalesCancelNoticeJobPort = mock(AftersalesCancelNoticeJobPort.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder busPayloadBuilder = new JushuitanTradeAftersalesBusPayloadBuilder();
		TradeAftersalesCancelDispatchPublisher tradeAftersalesCancelDispatchPublisher =
				mock(TradeAftersalesCancelDispatchPublisher.class);
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartySaasErpCancelPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);

		TransactionOperations tx = newCommitFiresAfterSynchronizationsTxOps();

		AftersalesService service =
				new AftersalesService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesAdminDetailService,
						aftersalesRefundService,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						aftersalesAutoRefuseWxaTemplatePort,
						normalOrderAutoCloseAftersalesCronPort,
						adapayScheduleAutoPaymentConfirmationPort,
						bspayScheduleAutoPaymentConfirmationPort,
						tx,
						companysMapper,
						applicationEventPublisher,
						aftersalesCancelNoticeJobPort,
						jushuitanPublisher,
						busPayloadBuilder,
						tradeAftersalesCancelDispatchPublisher,
						thirdPartySaasErpCancelPublisher);

		Aftersales main = new Aftersales();
		main.setCompanyId(COMPANY_ID);
		main.setAftersalesBn(AFTERSALES_BN);
		main.setAftersalesStatus(0);
		main.setOrderId(ORDER_ID);
		main.setUserId(1L);
		main.setDistributorId(5L);

		AftersalesRefund refund = new AftersalesRefund();
		when(aftersalesRefundService.findRefundByAftersalesBn(COMPANY_ID, AFTERSALES_BN)).thenReturn(refund);
		when(aftersalesMapper.update(any(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(any(), any())).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(COMPANY_ID), eq(AFTERSALES_BN), any()))
				.thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

		Aftersales updated = new Aftersales();
		updated.setCompanyId(COMPANY_ID);
		updated.setAftersalesBn(AFTERSALES_BN);
		updated.setAftersalesStatus(4);
		updated.setProgress(7);
		updated.setOrderId(ORDER_ID);
		updated.setDistributorId(5L);
		updated.setShopId(1L);
		updated.setSupplierId(0);
		updated.setUserId(1L);
		updated.setAftersalesType("ONLY_REFUND");
		updated.setReason("");
		updated.setDescription("");
		updated.setEvidencePic("");
		updated.setSalesmanId(0L);
		updated.setContact("");
		updated.setMobile("");
		updated.setMerchantId(0L);
		updated.setSelfDeliveryOperatorId(0L);
		updated.setIsPartialCancel(false);
		updated.setReturnType("logistics");
		updated.setFreight(0);
		updated.setFreightType("cash");
		updated.setReturnDistributorId(0L);
		updated.setAftersalesAddress("");
		when(aftersalesMapper.selectOne(any())).thenReturn(main, updated);

		service.closeAftersalesForSchedule(COMPANY_ID, AFTERSALES_BN, this);

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
		assertEquals("system", published.get("operator_type"));
		assertEquals(0L, ((Number) published.get("operator_id")).longValue());
		assertEquals("订单售后", published.get("remarks"));
		assertEquals("售后单号：" + AFTERSALES_BN + "，到期自动关闭售后", published.get("detail"));

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) published.get("params");
		assertNotNull(params);
		assertEquals(2, params.size());
		assertEquals(COMPANY_ID, ((Number) params.get("company_id")).longValue());
		assertEquals(AFTERSALES_BN, ((Number) params.get("aftersales_bn")).longValue());

		verify(thirdPartySaasErpCancelPublisher, times(1)).publish(any());
		verify(applicationEventPublisher, never()).publishEvent(isA(SaasErpAftersalesSpringEvent.class));
	}
}
