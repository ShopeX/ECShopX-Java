package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesLogiDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
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

/**
 * Admin sendback: after commit, {@link TradeAftersalesLogiDispatchPublisher} runs first; legacy synchronous
 * application events for the same logistics signal are not published from this path.
 */
@ExtendWith(MockitoExtension.class)
class MerchantTradeAftersalesLogiBusPublishProbeTest {

	private static final long COMPANY_ID = 8201L;
	private static final long ORDER_ID = 4201L;
	private static final long AFTERSALES_BN = 202605071200001L;
	private static final long DISTRIBUTOR_ID = 27L;

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
	@SuppressWarnings("unchecked")
	void sendback_afterCommit_publishesTradeAftersalesLogiBusOnceWithoutLegacySaasAftersalesApplicationEvent() {
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
		afterCommitRow.put("company_id", COMPANY_ID);
		afterCommitRow.put("aftersales_bn", AFTERSALES_BN);
		afterCommitRow.put("distributor_id", DISTRIBUTOR_ID);

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

		verify(aftersalesAdminDetailService, times(2))
				.loadFullAftersalesForAdmin(eq(COMPANY_ID), eq(AFTERSALES_BN));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logiPayload = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> jushuitanPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		InOrder inOrder =
				inOrder(
						tradeAftersalesLogiDispatchPublisher,
						jushuitanPublisher,
						applicationEventPublisher,
						wdtDispatchPublisher,
						aftersalesRefundAsyncPort);
		inOrder.verify(tradeAftersalesLogiDispatchPublisher, times(1)).publish(logiPayload.capture());
		inOrder.verify(jushuitanPublisher, times(1)).publish(jushuitanPayloadCaptor.capture());
		inOrder.verify(applicationEventPublisher).publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		inOrder.verify(applicationEventPublisher).publishEvent(any(WdtErpTradeAfterSaleSyncSpringEvent.class));
		inOrder.verify(wdtDispatchPublisher, times(1)).publish(any());
		inOrder.verify(aftersalesRefundAsyncPort, times(1))
				.scheduleSendAfterSaleWaitConfirmNotice(eq(COMPANY_ID), eq(AFTERSALES_BN));

		Map<String, Object> published = logiPayload.getValue();
		assertEquals(COMPANY_ID, ((Number) published.get("company_id")).longValue());
		assertEquals(AFTERSALES_BN, ((Number) published.get("aftersales_bn")).longValue());
		assertEquals(ORDER_ID, ((Number) published.get("order_id")).longValue());
		assertEquals(2, ((Number) published.get("progress")).intValue());
		assertEquals("RETURN_GOODS", String.valueOf(published.get("aftersales_type")));
		assertEquals(DISTRIBUTOR_ID, ((Number) published.get("distributor_id")).longValue());

		Map<String, Object> jushuitanBus = jushuitanPayloadCaptor.getValue();
		assertEquals(2, ((Number) jushuitanBus.get("progress")).intValue());
		assertEquals(COMPANY_ID, ((Number) jushuitanBus.get("company_id")).longValue());

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
	}

}
