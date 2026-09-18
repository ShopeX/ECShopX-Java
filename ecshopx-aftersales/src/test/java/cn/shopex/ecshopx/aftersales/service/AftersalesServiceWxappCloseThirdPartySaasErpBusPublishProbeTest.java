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

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP: wxapp close ThirdParty Bus publish probe")
class AftersalesServiceWxappCloseThirdPartySaasErpBusPublishProbeTest {

	private static final long COMPANY_ID = 9102L;
	private static final long ORDER_ID = 5102L;
	private static final long AFTERSALES_BN = 2026050912222222L;
	private static final long USER_ID = 88001L;

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
	void closeAftersalesForConsumer_afterCommit_publishesThirdPartySaasErpCancelOnce_andDoesNotPublishSaasErpSpringEvent() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
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
		main.setUserId(USER_ID);
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
		updated.setUserId(USER_ID);
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

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", AFTERSALES_BN);
		merged.put("company_id", COMPANY_ID);
		merged.put("user_id", USER_ID);
		merged.put("memo", "消费者主动关闭售后");

		service.closeAftersalesForConsumer(
				COMPANY_ID,
				AFTERSALES_BN,
				USER_ID,
				"user",
				USER_ID,
				merged,
				"售后单号：" + AFTERSALES_BN + "，消费者主动关闭售后",
				this);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartySaasErpCancelPublisher, times(1)).publish(payloadCaptor.capture());
		Map<String, Object> published = payloadCaptor.getValue();
		assertNotNull(published);
		assertEquals(COMPANY_ID, ((Number) published.get("company_id")).longValue());
		assertEquals(AFTERSALES_BN, ((Number) published.get("aftersales_bn")).longValue());
		assertEquals(ORDER_ID, ((Number) published.get("order_id")).longValue());
		assertEquals("ONLY_REFUND", published.get("aftersales_type"));

		verify(applicationEventPublisher, never()).publishEvent(isA(SaasErpAftersalesSpringEvent.class));
	}
}
