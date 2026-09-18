package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
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
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import cn.shopex.ecshopx.orders.event.WdtErpTradeAfterSaleSyncSpringEvent;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 运营台与商户侧售后审核均委托 {@link AftersalesReviewService#aftersalesReview}；本类校验事务提交后聚水潭 /
 * 旺店通 Dispatch Bus 与相关 Spring 域事件的发布组合（拒绝路径与两类入口共用）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AftersalesReviewService：运营台与商户侧共用审核与 Bus 发布")
class AftersalesReviewServiceJushuitanBusPublishTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	private static final List<String> BUS_PAYLOAD_KEYS =
			List.of(
					"aftersales_bn",
					"company_id",
					"order_id",
					"distributor_id",
					"shop_id",
					"supplier_id",
					"user_id",
					"aftersales_type",
					"aftersales_status",
					"progress",
					"reason",
					"description",
					"evidence_pic",
					"salesman_id",
					"contact",
					"mobile",
					"merchant_id",
					"self_delivery_operator_id",
					"is_partial_cancel",
					"return_type",
					"freight",
					"freight_type",
					"return_distributor_id",
					"aftersales_address");

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	@DisplayName(
			"仅退款且同意（部分取消自动审核与运营侧共用）：事务提交后聚水潭与旺店通 Dispatch 各一次；不发布旺店通同步 Spring 事件")
	@SuppressWarnings("unchecked")
	void onlyRefundApprove_afterCommit_publishesJushuitanBusOnce_andNeverPublishesJushuitanSyncSpringEvent() {
		long companyId = 10L;
		long aftersalesBn = 202601011234567L;
		long orderId = 100L;

		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = basePendingRow(companyId, aftersalesBn, orderId);
		Aftersales reloaded = baseCommittedRow(companyId, aftersalesBn, orderId);
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
		AftersalesRefund refund = new AftersalesRefund();
		refund.setPayType("wxpay");
		when(aftersalesRefundService.findRefundByAftersalesBn(companyId, aftersalesBn)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any()))
				.thenReturn(1);

		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
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
		JushuitanTradeAftersalesDispatchPublisher publisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);

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
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						publisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", companyId);
		param.put("aftersales_bn", aftersalesBn);
		param.put("is_approved", true);
		param.put("refund_fee", 50);
		param.put("refund_point", 0);
		param.put("freight", 0);
		param.put("operator_type", "admin");
		param.put("operator_id", 1L);

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", companyId);
		jwt.put("operator_type", "admin");
		jwt.put("operator_id", 1L);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);

		service.aftersalesReview(new LinkedHashMap<>(param), request);

		verify(tradeAftersalesDispatchPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher, never()).publish(any());

		ArgumentCaptor<Map<String, Object>> onlyRefundOrderLogCap = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(onlyRefundOrderLogCap.capture());
		Map<String, Object> onlyRefundOrderLog = onlyRefundOrderLogCap.getValue();
		assertEquals("订单售后", onlyRefundOrderLog.get("remarks"));
		String onlyRefundDetail = (String) onlyRefundOrderLog.get("detail");
		assertEquals(
				"售后单号：" + aftersalesBn + "，同意退款",
				onlyRefundDetail,
				"detail aligns with ONLY_REFUND approve branch (full-width comma before 同意退款)");
		assertEquals(orderId, ((Number) onlyRefundOrderLog.get("order_id")).longValue());
		assertEquals(companyId, ((Number) onlyRefundOrderLog.get("company_id")).longValue());
		assertEquals("admin", onlyRefundOrderLog.get("operator_type"));
		assertEquals(1L, ((Number) onlyRefundOrderLog.get("operator_id")).longValue());
		Map<String, Object> onlyRefundOrderLogParams = (Map<String, Object>) onlyRefundOrderLog.get("params");
		for (Map.Entry<String, Object> e : param.entrySet()) {
			assertEquals(e.getValue(), onlyRefundOrderLogParams.get(e.getKey()), () -> "params key: " + e.getKey());
		}

		verify(publisher, times(1)).publish(any());
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(publisher).publish(cap.capture());
		Map<String, Object> payload = cap.getValue();
		for (String key : BUS_PAYLOAD_KEYS) {
			assertTrue(payload.containsKey(key), () -> "missing bus key: " + key);
		}

		verify(applicationEventPublisher, never())
				.publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		verify(applicationEventPublisher, never())
				.publishEvent(any(WdtErpTradeAfterSaleSyncSpringEvent.class));
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));

		ArgumentCaptor<Map<String, Object>> saasErpCap = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(saasErpCap.capture());
		Map<String, Object> saasErpPayload = saasErpCap.getValue();
		assertEquals("update", saasErpPayload.get("aftersales_action"));
		assertEquals(companyId, ((Number) saasErpPayload.get("company_id")).longValue());
		assertEquals(orderId, ((Number) saasErpPayload.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) saasErpPayload.get("aftersales_bn")).longValue());
		assertEquals("ONLY_REFUND", saasErpPayload.get("aftersales_type"));

		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		Map<String, Object> expectedWdtPayload = wdtPayloadBuilder.build(reloaded);
		ArgumentCaptor<Map<String, Object>> wdtCap = ArgumentCaptor.forClass(Map.class);
		verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(wdtCap.capture());
		assertEquals(expectedWdtPayload, wdtCap.getValue());

		verify(aftersalesRefundAsyncPort, times(1)).scheduleOrderRefundComplete(companyId, orderId);
		verify(aftersalesRefundAsyncPort, times(1)).scheduleInvoiceRed(any());
	}

	@Test
	@DisplayName("仅退款且同意（部分取消自动审核与运营侧共用）：旺店通 Dispatch 负载与提交后重载售后单一致")
	@SuppressWarnings("unchecked")
	void onlyRefundApprove_publishPayload_usesReloadedAftersalesRow() {
		long companyId = 10L;
		long aftersalesBn = 202601011234568L;
		long orderId = 100L;

		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = basePendingRow(companyId, aftersalesBn, orderId);
		Aftersales reloaded = baseCommittedRow(companyId, aftersalesBn, orderId);
		reloaded.setProgress(99);
		reloaded.setAftersalesStatus(1);
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
		AftersalesRefund refund = new AftersalesRefund();
		refund.setPayType("wxpay");
		when(aftersalesRefundService.findRefundByAftersalesBn(companyId, aftersalesBn)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any()))
				.thenReturn(1);

		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
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
		JushuitanTradeAftersalesDispatchPublisher publisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);

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
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						publisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", companyId);
		param.put("aftersales_bn", aftersalesBn);
		param.put("is_approved", true);
		param.put("refund_fee", 50);
		param.put("refund_point", 0);
		param.put("freight", 0);
		param.put("operator_type", "admin");
		param.put("operator_id", 1L);

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", companyId);
		jwt.put("operator_type", "admin");
		jwt.put("operator_id", 1L);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);

		service.aftersalesReview(new LinkedHashMap<>(param), request);

		verify(tradeAftersalesDispatchPublisher, never()).publish(any());
		verify(thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher, never()).publish(any());

		ArgumentCaptor<Map<String, Object>> onlyRefundOrderLogCap = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(onlyRefundOrderLogCap.capture());
		Map<String, Object> onlyRefundOrderLog = onlyRefundOrderLogCap.getValue();
		assertEquals("订单售后", onlyRefundOrderLog.get("remarks"));
		String onlyRefundDetail = (String) onlyRefundOrderLog.get("detail");
		assertEquals(
				"售后单号：" + aftersalesBn + "，同意退款",
				onlyRefundDetail,
				"detail aligns with ONLY_REFUND approve branch (full-width comma before 同意退款)");
		assertEquals(orderId, ((Number) onlyRefundOrderLog.get("order_id")).longValue());
		assertEquals(companyId, ((Number) onlyRefundOrderLog.get("company_id")).longValue());
		assertEquals("admin", onlyRefundOrderLog.get("operator_type"));
		assertEquals(1L, ((Number) onlyRefundOrderLog.get("operator_id")).longValue());
		Map<String, Object> onlyRefundOrderLogParams = (Map<String, Object>) onlyRefundOrderLog.get("params");
		for (Map.Entry<String, Object> e : param.entrySet()) {
			assertEquals(e.getValue(), onlyRefundOrderLogParams.get(e.getKey()), () -> "params key: " + e.getKey());
		}

		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));

		ArgumentCaptor<Map<String, Object>> saasErpCap = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(saasErpCap.capture());
		Map<String, Object> saasErpPayload = saasErpCap.getValue();
		assertEquals("update", saasErpPayload.get("aftersales_action"));
		assertEquals(companyId, ((Number) saasErpPayload.get("company_id")).longValue());
		assertEquals(orderId, ((Number) saasErpPayload.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) saasErpPayload.get("aftersales_bn")).longValue());

		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(publisher, times(1)).publish(cap.capture());
		assertEquals(99, ((Number) cap.getValue().get("progress")).intValue());
		assertEquals(1, ((Number) cap.getValue().get("aftersales_status")).intValue());

		WdtErpTradeAfterSaleBusPayloadBuilder wdtPayloadBuilder = new WdtErpTradeAfterSaleBusPayloadBuilder();
		Map<String, Object> expectedWdtPayload = wdtPayloadBuilder.build(reloaded);
		ArgumentCaptor<Map<String, Object>> wdtCap = ArgumentCaptor.forClass(Map.class);
		verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(wdtCap.capture());
		assertEquals(expectedWdtPayload, wdtCap.getValue());
	}

	private static Aftersales basePendingRow(long companyId, long aftersalesBn, long orderId) {
		Aftersales a = new Aftersales();
		a.setCompanyId(companyId);
		a.setAftersalesBn(aftersalesBn);
		a.setOrderId(orderId);
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

	private static Aftersales baseCommittedRow(long companyId, long aftersalesBn, long orderId) {
		Aftersales a = basePendingRow(companyId, aftersalesBn, orderId);
		a.setProgress(9);
		a.setAftersalesStatus(1);
		return a;
	}

	@Test
	@DisplayName(
			"拒绝审核：事务提交后聚水潭 Bus 发布一次，并仍发布聚水潭同步 Spring 事件（运营台与商户侧共用路径）")
	@SuppressWarnings("unchecked")
	void rejectReview_afterCommit_publishesJushuitanBusOnce_andStillPublishesJushuitanSyncSpringEvent() {
		long companyId = 10L;
		long aftersalesBn = 202601011234569L;
		long orderId = 100L;

		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = basePendingRow(companyId, aftersalesBn, orderId);
		Aftersales reloadedRejected = baseRejectedReloadedRow(companyId, aftersalesBn, orderId);
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = selectPass.getAndIncrement();
							return n == 0 ? loaded : reloadedRejected;
						});
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of());
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(0L);

		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any()))
				.thenReturn(1);

		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
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
		JushuitanTradeAftersalesDispatchPublisher publisher = mock(JushuitanTradeAftersalesDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder =
				new WdtErpTradeAfterSaleBusPayloadBuilder();
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);

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
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						publisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						wdtErpTradeAfterSaleBusPayloadBuilder,
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", companyId);
		param.put("aftersales_bn", aftersalesBn);
		param.put("is_approved", false);
		param.put("refuse_reason", "not eligible");
		param.put("operator_type", "admin");
		param.put("operator_id", 1L);

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", companyId);
		jwt.put("operator_type", "admin");
		jwt.put("operator_id", 1L);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);

		service.aftersalesReview(new LinkedHashMap<>(param), request);

		ArgumentCaptor<Map<String, Object>> orderProcessLogCap = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(orderProcessLogCap.capture());
		Map<String, Object> orderLog = orderProcessLogCap.getValue();
		assertEquals("订单售后", orderLog.get("remarks"));
		String orderLogDetail = (String) orderLog.get("detail");
		assertTrue(orderLogDetail.contains("售后单驳回"));
		assertTrue(orderLogDetail.contains("驳回原因"));
		assertTrue(orderLogDetail.contains(String.valueOf(aftersalesBn)));
		assertTrue(orderLogDetail.contains("not eligible"));
		assertEquals(orderId, ((Number) orderLog.get("order_id")).longValue());
		assertEquals(companyId, ((Number) orderLog.get("company_id")).longValue());
		assertEquals("admin", orderLog.get("operator_type"));
		assertEquals(1L, ((Number) orderLog.get("operator_id")).longValue());
		Map<String, Object> orderLogParams = (Map<String, Object>) orderLog.get("params");
		for (Map.Entry<String, Object> e : param.entrySet()) {
			assertEquals(e.getValue(), orderLogParams.get(e.getKey()), () -> "params key: " + e.getKey());
		}

		verify(publisher, times(1)).publish(any());
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(publisher).publish(cap.capture());
		Map<String, Object> payload = cap.getValue();
		for (String key : BUS_PAYLOAD_KEYS) {
			assertTrue(payload.containsKey(key), () -> "missing bus key: " + key);
		}
		assertEquals(3, ((Number) payload.get("aftersales_status")).intValue());
		assertEquals(3, ((Number) payload.get("progress")).intValue());

		Aftersales rejectMemory = basePendingRow(companyId, aftersalesBn, orderId);
		rejectMemory.setProgress(3);
		rejectMemory.setAftersalesStatus(3);
		rejectMemory.setRefuseReason("not eligible");
		Map<String, Object> expectedWdtPayload = wdtErpTradeAfterSaleBusPayloadBuilder.build(rejectMemory);
		ArgumentCaptor<Map<String, Object>> wdtCap = ArgumentCaptor.forClass(Map.class);
		verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(wdtCap.capture());
		assertEquals(expectedWdtPayload, wdtCap.getValue());

		verify(applicationEventPublisher, never()).publishEvent(any(WdtErpTradeAfterSaleSyncSpringEvent.class));
		verify(applicationEventPublisher, times(1)).publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		verify(applicationEventPublisher, never()).publishEvent(any(SaasErpAftersalesSpringEvent.class));
		verify(aftersalesRefundAsyncPort, never()).scheduleOrderRefundComplete(anyLong(), anyLong());

		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, never()).publish(any());

		ArgumentCaptor<Map<String, Object>> cancelCap = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher, times(1)).publish(cancelCap.capture());
		Map<String, Object> cancelPayload = cancelCap.getValue();
		assertEquals("cancel", cancelPayload.get("aftersales_action"));
		assertEquals(companyId, ((Number) cancelPayload.get("company_id")).longValue());
		assertEquals(orderId, ((Number) cancelPayload.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) cancelPayload.get("aftersales_bn")).longValue());

		ArgumentCaptor<Map<String, Object>> tradeCap = ArgumentCaptor.forClass(Map.class);
		verify(tradeAftersalesDispatchPublisher, times(1)).publish(tradeCap.capture());
		Map<String, Object> tradePayload = tradeCap.getValue();
		assertEquals(companyId, ((Number) tradePayload.get("company_id")).longValue());
		assertEquals(orderId, ((Number) tradePayload.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) tradePayload.get("aftersales_bn")).longValue());
	}

	private static Aftersales baseRejectedReloadedRow(long companyId, long aftersalesBn, long orderId) {
		Aftersales a = basePendingRow(companyId, aftersalesBn, orderId);
		a.setProgress(3);
		a.setAftersalesStatus(3);
		a.setRefuseReason("not eligible");
		return a;
	}
}
