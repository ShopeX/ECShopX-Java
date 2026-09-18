package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
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
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
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
 * Scheduled auto-refuse: asserts {@link OrdersDispatchEventNames#EVENT_ORDER_PROCESS_LOG} is published through
 * {@link OrderProcessLogPublishPortImpl} after transaction commit, with a mocked {@link DispatchFacade}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: schedule auto-refuse aftersales publishEvent probe")
class AftersalesServiceScheduleAutoRefuseOrderProcessLogDispatchPublishProbeTest {

	private static final long COMPANY_ID = 9301L;
	private static final long ORDER_ID = 5301L;
	private static final long AFTERSALES_BN = 2026050914990001L;
	private static final long DETAIL_ID = 149901L;

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
	void scheduleAutoRefuse_withOrderProcessLogPublishPortImpl_afterCommit_invokesDispatchFacadePublishEventOnce_withAutoRefusePayload() {
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

		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.selectPage(any(), any()))
				.thenAnswer(
						invocation -> {
							Page<?> p = invocation.getArgument(0);
							AftersalesDetail d = new AftersalesDetail();
							d.setDetailId(DETAIL_ID);
							d.setCompanyId(COMPANY_ID);
							d.setAftersalesBn(AFTERSALES_BN);
							IPage<AftersalesDetail> page = new Page<>(p.getCurrent(), p.getSize());
							page.setRecords(List.of(d));
							return page;
						});

		Aftersales main = new Aftersales();
		main.setAftersalesBn(AFTERSALES_BN);
		main.setCompanyId(COMPANY_ID);
		main.setUserId(1L);
		main.setOrderId(ORDER_ID);
		main.setAftersalesType("REFUND_GOODS");
		main.setRefuseReason("原因");
		main.setRefundFee(1);
		when(aftersalesMapper.selectOne(any())).thenReturn(main);
		when(aftersalesMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(COMPANY_ID), eq(AFTERSALES_BN), any()))
				.thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

		assertThat(service.scheduleAutoRefuse()).isTrue();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> published = payloadCaptor.getValue();
		assertThat(published).isNotNull();
		assertThat(published).doesNotContainKeys("operator_id", "params");
		assertThat(((Number) published.get("order_id")).longValue()).isEqualTo(ORDER_ID);
		assertThat(((Number) published.get("company_id")).longValue()).isEqualTo(COMPANY_ID);
		assertThat(published.get("operator_type")).isEqualTo("system");
		assertThat(published.get("remarks")).isEqualTo("订单售后");
		assertThat(published.get("detail"))
				.isEqualTo(
						"售后单号："
								+ AFTERSALES_BN
								+ " 自动驳回，驳回原因："
								+ "未收到商品自动驳回");
	}
}
