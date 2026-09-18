package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.TradeAftersalesCancelSpringEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AftersalesServiceCloseAftersalesJushuitanBusPublishProbeTest {

	private static final long COMPANY_ID = 1L;
	private static final long AFTERSALES_BN = 10L;
	private static final long ORDER_ID = 100L;
	private static final long DISTRIBUTOR_ID = 5L;

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
	@DisplayName(
			"closeAftersalesForSchedule (cron/XXL batch): after-commit Jushuitan bus publish order matches wxapp close path")
	void closeAftersales_afterCommit_publishesBusPayloadBeforeSyncSpringEventAndCancelNoticeJob() {
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
		main.setDistributorId(DISTRIBUTOR_ID);
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
		updated.setDistributorId(DISTRIBUTOR_ID);
		updated.setShopId(1L);
		updated.setSupplierId(0);
		updated.setUserId(20L);
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

		InOrder inOrder =
				inOrder(
						applicationEventPublisher,
						tradeAftersalesCancelDispatchPublisher,
						thirdPartySaasErpCancelPublisher,
						jushuitanPublisher,
						aftersalesCancelNoticeJobPort);
		inOrder.verify(applicationEventPublisher).publishEvent(any(TradeAftersalesCancelSpringEvent.class));
		inOrder.verify(tradeAftersalesCancelDispatchPublisher).publish(any());
		inOrder.verify(thirdPartySaasErpCancelPublisher).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> busPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		inOrder.verify(jushuitanPublisher, times(1)).publish(busPayloadCaptor.capture());
		inOrder.verify(applicationEventPublisher).publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		inOrder.verify(aftersalesCancelNoticeJobPort).scheduleSendAftersaleCancelNotice(COMPANY_ID, AFTERSALES_BN);

		Map<String, Object> busPayload = busPayloadCaptor.getValue();
		assertEquals(7, ((Number) busPayload.get("progress")).intValue());
		assertEquals(4, ((Number) busPayload.get("aftersales_status")).intValue());
		assertEquals(AFTERSALES_BN, ((Number) busPayload.get("aftersales_bn")).longValue());
		assertEquals(COMPANY_ID, ((Number) busPayload.get("company_id")).longValue());
		assertEquals(ORDER_ID, ((Number) busPayload.get("order_id")).longValue());
		assertEquals(DISTRIBUTOR_ID, ((Number) busPayload.get("distributor_id")).longValue());
		assertEquals(new LinkedHashMap<>(busPayloadBuilder.build(updated, new LinkedHashMap<>())), busPayload);
	}

}
