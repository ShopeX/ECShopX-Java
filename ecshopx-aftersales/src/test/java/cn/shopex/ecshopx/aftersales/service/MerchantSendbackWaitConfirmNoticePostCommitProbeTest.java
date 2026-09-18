package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesLogiDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import cn.shopex.ecshopx.orders.event.WdtErpTradeAfterSaleSyncSpringEvent;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Merchant admin sendback post-commit probe only; member wxapp sendback is documented on {@link AftersalesFrontWxappSendbackService} and covered by bootstrap {@code SendAfterSaleWaitConfirmNoticeJobDispatchFlowTest}. */
@ExtendWith(MockitoExtension.class)
class MerchantSendbackWaitConfirmNoticePostCommitProbeTest {

	private static final long COMPANY_ID = 8001L;
	private static final long ORDER_ID = 4001L;
	private static final long AFTERSALES_BN = 202602061111223L;
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

	/** Callback failure rolls back; {@link PlatformTransactionManager#commit} is never invoked. */
	private static PlatformTransactionManager newRollbackOnlyTxManager() {
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
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.rollback(any());
		return txMgr;
	}

	@Test
	void sendback_afterTransactionCommit_schedulesWaitConfirmNoticeOnce() {
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformTransactionManager txMgr = newCommitFiresAfterSynchronizationsTxManager();
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
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
		jwt.put("operator_id", 99L);
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
		persisted.setDistributorId(DISTRIBUTOR_ID);
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
						jushuitanPublisher,
						busPayloadBuilder,
						wdtDispatchPublisher,
						wdtBusPayloadBuilder,
						tradeAftersalesLogiDispatchPublisher);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", AFTERSALES_BN);
		merged.put("corp_code", "SF");
		merged.put("logi_no", "1234567890");

		service.sendback(merged, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> busPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		InOrder inOrder =
				inOrder(
						tradeAftersalesLogiDispatchPublisher,
						jushuitanPublisher,
						applicationEventPublisher,
						wdtDispatchPublisher,
						aftersalesRefundAsyncPort);
		inOrder.verify(tradeAftersalesLogiDispatchPublisher, times(1)).publish(any());
		inOrder.verify(jushuitanPublisher, times(1)).publish(busPayloadCaptor.capture());
		inOrder.verify(applicationEventPublisher).publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		inOrder.verify(applicationEventPublisher).publishEvent(any(WdtErpTradeAfterSaleSyncSpringEvent.class));
		inOrder.verify(wdtDispatchPublisher, times(1)).publish(any());
		inOrder.verify(aftersalesRefundAsyncPort, times(1))
				.scheduleSendAfterSaleWaitConfirmNotice(eq(COMPANY_ID), eq(AFTERSALES_BN));
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));

		Map<String, Object> busPayload = busPayloadCaptor.getValue();
		assertEquals(2, ((Number) busPayload.get("progress")).intValue());
		assertEquals(COMPANY_ID, ((Number) busPayload.get("company_id")).longValue());
		assertEquals(ORDER_ID, ((Number) busPayload.get("order_id")).longValue());
		assertEquals(AFTERSALES_BN, ((Number) busPayload.get("aftersales_bn")).longValue());

		verify(orderProcessLogPublishPort, times(1)).publish(any());
	}

	@Test
	void sendback_whenMainRowMissing_neverSchedulesWaitConfirmNotice() {
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformTransactionManager txMgr = newRollbackOnlyTxManager();
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
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
		jwt.put("operator_id", 99L);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);

		Map<String, Object> beforeCommitRow = new LinkedHashMap<>();
		beforeCommitRow.put("aftersales_type", "RETURN_GOODS");
		beforeCommitRow.put("progress", 1);
		beforeCommitRow.put("order_id", ORDER_ID);

		when(aftersalesAdminDetailService.loadFullAftersalesForAdmin(COMPANY_ID, AFTERSALES_BN))
				.thenReturn(beforeCommitRow);

		when(aftersalesMapper.update(isNull(), any())).thenReturn(0);

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
						jushuitanPublisher,
						busPayloadBuilder,
						wdtDispatchPublisher,
						wdtBusPayloadBuilder,
						tradeAftersalesLogiDispatchPublisher);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", AFTERSALES_BN);
		merged.put("corp_code", "SF");
		merged.put("logi_no", "1234567890");

		assertThrows(ResourceException.class, () -> service.sendback(merged, request));

		verify(aftersalesRefundAsyncPort, never()).scheduleSendAfterSaleWaitConfirmNotice(anyLong(), anyLong());
		verify(applicationEventPublisher, never()).publishEvent(any());
		verify(jushuitanPublisher, never()).publish(any());
		verify(tradeAftersalesLogiDispatchPublisher, never()).publish(any());
	}
}
