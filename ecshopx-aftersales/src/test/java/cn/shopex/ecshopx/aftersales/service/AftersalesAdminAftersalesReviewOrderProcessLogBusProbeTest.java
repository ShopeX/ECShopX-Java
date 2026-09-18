package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.NormalOrderPartialCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Probes {@link AftersalesReviewService#aftersalesReview} on the admin-console review path (reject, ONLY_REFUND
 * approve, and non–ONLY_REFUND approve / wait–return-goods flows) → {@link OrderProcessLogPublishPort#publish}, without
 * HTTP ingress.
 *
 * <p>End-to-end dispatch for {@code EVENT_ORDER_PROCESS_LOG} (slow queue) is documented in orders integration and in
 * bootstrap tests such as {@code SupplierOrderPaidConfirmOrderProcessLogPublishTest}; this class only asserts the
 * port call and payload shape after transaction commit, parallel in structure to {@link
 * AftersalesAdminApplyOrderProcessLogBusProbeTest}.
 *
 * <p>Covers operator-console review paths that share {@link AftersalesReviewService}: reject must not touch the SaaS
 * ERP update publisher; approve paths must publish exactly one update payload with {@code aftersales_action}
 * {@code update} and no legacy {@link cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent} publish.
 */
@ExtendWith(MockitoExtension.class)
class AftersalesAdminAftersalesReviewOrderProcessLogBusProbeTest {

	@Mock
	private ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;

	@Mock
	private ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher;

	private static final long COMPANY_ID = 9001L;
	private static final long ORDER_ID = 5001L;
	private static final long AFTERSALES_BN = 202601091200001L;
	private static final long ADMIN_OPERATOR_ID = 700L;
	private static final String REFUSE_REASON = "probe reject reason";

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

	/** Captured from the latest reject / ONLY_REFUND approve probe invocation. */
	private OrderProcessLogPublishPort orderProcessLogPortProbe;

	private void runAdminRejectReviewProbe() {
		OrderProcessLogPublishPort logPort = mock(OrderProcessLogPublishPort.class);
		this.orderProcessLogPortProbe = logPort;

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = pendingRow();
		Aftersales reloaded = rejectedReloadedRow();
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = selectPass.getAndIncrement();
							return n == 0 ? loaded : reloaded;
						});
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of());
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(0L);

		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);

		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		NormalOrderPartialCancelRestorePort normalOrderPartialCancelRestorePort =
				mock(NormalOrderPartialCancelRestorePort.class);
		OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort =
				mock(OrderValidityPlatformSettingReadPort.class);
		DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort =
				mock(DistributorAftersalesAddressDetailReadPort.class);
		LangueProperties langueProperties = mock(LangueProperties.class);

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any(TransactionDefinition.class)))
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
								for (TransactionSynchronization sync :
										TransactionSynchronizationManager.getSynchronizations()) {
									sync.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesReviewService service =
				new AftersalesReviewService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						aftersalesBrokeragePort,
						logPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						jushuitanPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("aftersales_bn", AFTERSALES_BN);
		body.put("is_approved", false);
		body.put("refuse_reason", REFUSE_REASON);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(
						Map.of(
								"company_id", COMPANY_ID,
								"operator_type", "admin",
								"operator_id", ADMIN_OPERATOR_ID));

		service.aftersalesReview(new LinkedHashMap<>(body), request);

		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
		verify(tradeAftersalesDispatchPublisher, times(1)).publish(any());
	}

	private void runAdminOnlyRefundApproveProbe() {
		OrderProcessLogPublishPort logPort = mock(OrderProcessLogPublishPort.class);
		this.orderProcessLogPortProbe = logPort;

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = pendingRow();
		Aftersales persistedApprove = onlyRefundApprovedPersistedRow();
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = selectPass.getAndIncrement();
							return n == 0 ? loaded : persistedApprove;
						});
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of());
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(0L);

		AftersalesRefund refundRow = new AftersalesRefund();
		refundRow.setPayType("wxpay");

		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		when(aftersalesRefundService.findRefundByAftersalesBn(eq(COMPANY_ID), eq(AFTERSALES_BN)))
				.thenReturn(refundRow);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);

		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		NormalOrderPartialCancelRestorePort normalOrderPartialCancelRestorePort =
				mock(NormalOrderPartialCancelRestorePort.class);
		OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort =
				mock(OrderValidityPlatformSettingReadPort.class);
		DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort =
				mock(DistributorAftersalesAddressDetailReadPort.class);
		LangueProperties langueProperties = mock(LangueProperties.class);

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any(TransactionDefinition.class)))
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
								for (TransactionSynchronization sync :
										TransactionSynchronizationManager.getSynchronizations()) {
									sync.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesReviewService service =
				new AftersalesReviewService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						aftersalesBrokeragePort,
						logPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						jushuitanPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("aftersales_bn", AFTERSALES_BN);
		body.put("is_approved", true);
		body.put("refund_fee", 50);
		body.put("refund_point", 0);
		body.put("freight", 5);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(
						Map.of(
								"company_id", COMPANY_ID,
								"operator_type", "admin",
								"operator_id", ADMIN_OPERATOR_ID));

		service.aftersalesReview(new LinkedHashMap<>(body), request);

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> saasCap = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(saasCap.capture());
		assertThat(saasCap.getValue().get("aftersales_action")).isEqualTo("update");
		assertThat(saasCap.getValue().get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(saasCap.getValue().get("order_id")).isEqualTo(ORDER_ID);
		assertThat(saasCap.getValue().get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(saasCap.getValue().get("aftersales_type")).isEqualTo("ONLY_REFUND");
		verify(tradeAftersalesDispatchPublisher, never()).publish(any());
	}

	private void runAdminRefundGoodsApproveProbe() {
		OrderProcessLogPublishPort logPort = mock(OrderProcessLogPublishPort.class);
		this.orderProcessLogPortProbe = logPort;

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		Aftersales loaded = refundGoodsPendingRow();
		when(aftersalesMapper.selectOne(any())).thenReturn(loaded);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(0L);

		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		NormalOrderPartialCancelRestorePort normalOrderPartialCancelRestorePort =
				mock(NormalOrderPartialCancelRestorePort.class);
		OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort =
				mock(OrderValidityPlatformSettingReadPort.class);
		when(orderValidityPlatformSettingReadPort.readPlatformSetting(anyLong()))
				.thenReturn(Map.of("auto_refuse_time", 0));
		DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort =
				mock(DistributorAftersalesAddressDetailReadPort.class);
		LangueProperties langueProperties = mock(LangueProperties.class);

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any(TransactionDefinition.class)))
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
								for (TransactionSynchronization sync :
										TransactionSynchronizationManager.getSynchronizations()) {
									sync.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesReviewService service =
				new AftersalesReviewService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						aftersalesBrokeragePort,
						logPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						jushuitanPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("aftersales_bn", AFTERSALES_BN);
		body.put("is_approved", true);
		body.put("refund_fee", 50);
		body.put("refund_point", 0);
		body.put("freight", 5);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(
						Map.of(
								"company_id", COMPANY_ID,
								"operator_type", "admin",
								"operator_id", ADMIN_OPERATOR_ID));

		service.aftersalesReview(new LinkedHashMap<>(body), request);

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> saasCap = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(saasCap.capture());
		assertThat(saasCap.getValue().get("aftersales_action")).isEqualTo("update");
		assertThat(saasCap.getValue().get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(saasCap.getValue().get("aftersales_type")).isEqualTo("REFUND_GOODS");
		verify(tradeAftersalesDispatchPublisher, never()).publish(any());
	}

	@Test
	void adminAftersalesReview_reject_invokesOrderProcessLogPublishPortOnceWithExpectedPayload() {
		runAdminRejectReviewProbe();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPortProbe, times(1)).publish(captor.capture());
		Map<String, Object> log = captor.getValue();

		assertThat(log.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(log.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(log.get("operator_type")).isEqualTo("admin");
		assertThat(log.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
		assertThat(log.get("remarks")).isEqualTo("订单售后");
		assertThat(log.get("detail").toString())
				.isEqualTo("售后单号：" + AFTERSALES_BN + " 售后单驳回，驳回原因：" + REFUSE_REASON);

		@SuppressWarnings("unchecked")
		Map<String, Object> paramsMap = (Map<String, Object>) log.get("params");
		assertThat(paramsMap).isNotNull();
		assertThat(paramsMap.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(paramsMap.get("operator_type")).isEqualTo("admin");
		assertThat(paramsMap.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
		assertThat(paramsMap.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(paramsMap.get("is_approved")).isEqualTo(false);
		assertThat(paramsMap.get("refuse_reason")).isEqualTo(REFUSE_REASON);
		assertThat(paramsMap.get("refund_fee")).isEqualTo(0);
		assertThat(paramsMap.get("refund_point")).isEqualTo(0);
		assertThat(paramsMap.get("freight")).isEqualTo(0);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cancelCap = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher, times(1)).publish(cancelCap.capture());
		assertThat(cancelCap.getValue().get("aftersales_action")).isEqualTo("cancel");
		assertThat(cancelCap.getValue().get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(cancelCap.getValue().get("order_id")).isEqualTo(ORDER_ID);
		assertThat(cancelCap.getValue().get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(cancelCap.getValue().get("aftersales_type")).isEqualTo("ONLY_REFUND");

		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());
	}

	@Test
	void onlyRefundApprove_adminAftersalesReview_invokesPublishOnce() {
		runAdminOnlyRefundApproveProbe();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPortProbe, times(1)).publish(captor.capture());
		Map<String, Object> log = captor.getValue();

		String fullDetail = "售后单号：" + AFTERSALES_BN + "，同意退款";
		assertThat(log.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(log.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(log.get("operator_type")).isEqualTo("admin");
		assertThat(log.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
		assertThat(log.get("remarks")).isEqualTo("订单售后");
		assertThat(log.get("detail").toString()).isEqualTo(fullDetail);
		assertThat(log.get("detail").toString())
				.contains("同意退款")
				.doesNotContain("驳回")
				.doesNotContain("驳回原因");

		@SuppressWarnings("unchecked")
		Map<String, Object> paramsMap = (Map<String, Object>) log.get("params");
		assertThat(paramsMap).isNotNull();
		assertThat(paramsMap.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(paramsMap.get("operator_type")).isEqualTo("admin");
		assertThat(paramsMap.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
		assertThat(paramsMap.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(paramsMap.get("is_approved")).isEqualTo(true);
		assertThat(paramsMap.get("refund_fee")).isEqualTo(50);
		assertThat(paramsMap.get("refund_point")).isEqualTo(0);
		assertThat(paramsMap.get("freight")).isEqualTo(5);

		verify(thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(any());
	}

	@Test
	void refundGoodsApprove_adminAftersalesReview_invokesPublishOnce_withWaitReturnGoodsDetail() {
		runAdminRefundGoodsApproveProbe();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPortProbe, times(1)).publish(captor.capture());
		Map<String, Object> log = captor.getValue();

		String expectedDetail = "售后单号：" + AFTERSALES_BN + "，售后单审核通过，等待商品回寄";
		assertThat(log.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(log.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(log.get("operator_type")).isEqualTo("admin");
		assertThat(log.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
		assertThat(log.get("remarks")).isEqualTo("订单售后");
		assertThat(log.get("detail").toString()).isEqualTo(expectedDetail);
		assertThat(log.get("detail").toString()).contains("售后单审核通过，等待商品回寄");
		assertThat(log.get("detail").toString()).doesNotContain("同意退款");
		assertThat(log.get("detail").toString()).doesNotContain("驳回");

		@SuppressWarnings("unchecked")
		Map<String, Object> paramsMap = (Map<String, Object>) log.get("params");
		assertThat(paramsMap).isNotNull();
		assertThat(paramsMap.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(paramsMap.get("operator_type")).isEqualTo("admin");
		assertThat(paramsMap.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
		assertThat(paramsMap.get("is_approved")).isEqualTo(true);
		assertThat(paramsMap.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(paramsMap.get("refund_fee")).isEqualTo(50);
		assertThat(paramsMap.get("refund_point")).isEqualTo(0);
		assertThat(paramsMap.get("freight")).isEqualTo(5);

		verify(thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher, never()).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> saasAgain = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(saasAgain.capture());
		assertThat(saasAgain.getValue().get("aftersales_action")).isEqualTo("update");
		assertThat(saasAgain.getValue().get("order_id")).isEqualTo(ORDER_ID);
		assertThat(saasAgain.getValue().get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
	}

	private static Aftersales pendingRow() {
		Aftersales a = new Aftersales();
		a.setCompanyId(COMPANY_ID);
		a.setAftersalesBn(AFTERSALES_BN);
		a.setOrderId(ORDER_ID);
		a.setUserId(20L);
		a.setAftersalesType("ONLY_REFUND");
		a.setAftersalesStatus(0);
		a.setProgress(0);
		a.setRefundFee(100);
		a.setRefundPoint(0);
		a.setFreight(10);
		a.setDistributorId(2L);
		a.setShopId(1L);
		a.setSalesmanId(88L);
		return a;
	}

	private static Aftersales rejectedReloadedRow() {
		Aftersales a = pendingRow();
		a.setProgress(3);
		a.setAftersalesStatus(3);
		a.setRefuseReason(REFUSE_REASON);
		return a;
	}

	private static Aftersales onlyRefundApprovedPersistedRow() {
		Aftersales a = pendingRow();
		a.setProgress(9);
		a.setAftersalesStatus(1);
		return a;
	}

	private static Aftersales refundGoodsPendingRow() {
		Aftersales a = pendingRow();
		a.setAftersalesType("REFUND_GOODS");
		return a;
	}
}
