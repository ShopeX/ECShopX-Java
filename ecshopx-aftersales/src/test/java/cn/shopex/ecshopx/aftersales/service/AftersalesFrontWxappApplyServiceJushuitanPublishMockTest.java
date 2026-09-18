package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.common.port.distribution.OfflineAftersalesDistributorHeadReadPort;
import cn.shopex.ecshopx.common.port.order.OrderAssociationReadPort;
import cn.shopex.ecshopx.common.port.order.OrderItemsProfitWritePort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import cn.shopex.ecshopx.common.event.SaasErpRefundSpringEvent;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@DisplayName("Wxapp apply via shopApplyByNum; post-commit TradeRefund publish and Jushuitan payloads")
@ExtendWith(MockitoExtension.class)
class AftersalesFrontWxappApplyServiceJushuitanPublishMockTest {

	private static final long MOCK_REFUND_BN = 29001230123456789L;

	private static final Set<String> SECTION5_KEYS =
			Set.of(
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

	@Mock private ShopMenuService shopMenuService;
	@Mock private OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort;
	@Mock private OrderAssociationReadPort orderAssociationReadPort;
	@Mock private OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	@Mock private OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	@Mock private AftersalesApplyCheckApplyService aftersalesApplyCheckApplyService;

	@Mock private AftersalesMapper aftersalesMapper;
	@Mock private AftersalesDetailMapper aftersalesDetailMapper;
	@Mock private AftersalesRefundService aftersalesRefundService;
	@Mock private AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService;
	@Mock private OrderItemsProfitWritePort orderItemsProfitWritePort;
	@Mock private AftersalesBrokeragePort aftersalesBrokeragePort;
	@Mock private JdbcTemplate jdbcTemplate;
	@Mock private OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock private ApplicationEventPublisher applicationEventPublisher;
	@Mock private JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;
	@Mock private InvoiceRedJobDispatchPublisher invoiceRedJobDispatchPublisher;
	@Mock private AftersalesApplyAsyncPort aftersalesApplyAsyncPort;
	@Mock private SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher;
	@Mock private OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort;
	@Mock private OfflineAftersalesDistributorHeadReadPort offlineAftersalesDistributorHeadReadPort;
	@Mock private WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	@Mock private TradeRefundDispatchPublisher tradeRefundDispatchPublisher;
	@Mock private TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher;

	@SuppressWarnings("unchecked")
	private HashOperations<String, Object, Object> redisHashOps;

	private final AtomicReference<Aftersales> lastInsertedAftersalesMain = new AtomicReference<>();

	private AftersalesFrontWxappApplyService frontApplyService;

	@BeforeEach
	void setUp() {
		lastInsertedAftersalesMain.set(null);
		doNothing().when(aftersalesApplyCheckApplyService).checkApply(any());

		PlatformTransactionManager ptm = mock(PlatformTransactionManager.class);
		when(ptm.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
		redisHashOps = mock(HashOperations.class);
		when(companysRedisTemplate.opsForHash()).thenReturn(redisHashOps);
		when(redisHashOps.increment(anyString(), any(), anyLong())).thenReturn(1L);
		when(orderValidityPlatformSettingReadPort.readPlatformSetting(anyLong()))
				.thenReturn(Map.of("is_refund_freight", false));
		when(aftersalesApplyDetailQueryService.sumAppliedNum(anyLong(), anyLong(), anyLong())).thenReturn(0);
		when(aftersalesApplyDetailQueryService.sumAppliedRefundFee(anyLong(), anyLong(), anyLong()))
				.thenReturn(0);
		when(aftersalesApplyDetailQueryService.sumAppliedRefundPoint(anyLong(), anyLong(), anyLong()))
				.thenReturn(0);
		when(aftersalesApplyDetailQueryService.listReturnPointRows(anyLong(), anyLong(), anyLong()))
				.thenReturn(List.of());
		doAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							Map<String, Object> p = inv.getArgument(0);
							p.put("refund_bn", MOCK_REFUND_BN);
							return null;
						})
				.when(aftersalesRefundService)
				.createRefund(any());

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", 501L);
		line.put("num", 1);
		line.put("total_fee", 10_000);
		line.put("point", 0);
		line.put("item_name", "SKU");
		line.put("supplier_id", 3);
		line.put("goods_id", 11L);
		line.put("item_id", 22L);
		line.put("item_bn", "BN1");
		line.put("pic", "");
		line.put("order_item_type", "product");
		line.put("get_points", 0);
		line.put("item_spec_desc", "");
		line.put("delivery_status", "DONE");
		line.put("delivery_item_num", 1);
		line.put("cancel_item_num", 0);
		when(orderNormalOrderItemsReadPort.listItems(anyLong(), anyLong())).thenReturn(List.of(line));

		doAnswer(
						inv -> {
							lastInsertedAftersalesMain.set(inv.getArgument(0));
							return 1;
						})
				.when(aftersalesMapper)
				.insert(any(Aftersales.class));
		when(aftersalesMapper.selectOne(any())).thenAnswer(inv -> lastInsertedAftersalesMain.get());

		AftersalesApplyShopApplyByNumHandleService handleService =
				new AftersalesApplyShopApplyByNumHandleService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesApplyDetailQueryService,
						orderItemsProfitWritePort,
						aftersalesBrokeragePort,
						jdbcTemplate,
						orderProcessLogPublishPort,
						applicationEventPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						jushuitanTradeAftersalesDispatchPublisher,
						invoiceRedJobDispatchPublisher,
						aftersalesApplyAsyncPort,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						orderValidityPlatformSettingReadPort,
						orderNormalOrderItemsReadPort,
						companysRedisTemplate,
						new ObjectMapper(),
						ptm,
						distributorAftersalesAddressDetailReadPort,
						offlineAftersalesDistributorHeadReadPort,
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						tradeRefundDispatchPublisher,
						tradeAftersalesDispatchPublisher,
						mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class));

		AftersalesApplyShopApplyByNumService shopApplyByNumService =
				new AftersalesApplyShopApplyByNumService(
						aftersalesApplyCheckApplyService,
						handleService,
						orderNormalOrderHeaderReadPort,
						orderSuccessTradeReadPort);

		frontApplyService =
				new AftersalesFrontWxappApplyService(
						shopMenuService,
						orderValidityPlatformSettingReadPort,
						orderAssociationReadPort,
						orderNormalOrderHeaderReadPort,
						shopApplyByNumService,
						new ObjectMapper());

		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> header = baseOrderHeader();
		header.put("order_status", "TRADE");
		header.put("receipt_type", "express");
		when(orderAssociationReadPort.getAssociation(companyId, orderId))
				.thenReturn(Optional.of(Map.of("order_type", "normal")));
		when(orderNormalOrderHeaderReadPort.getHeader(companyId, orderId))
				.thenReturn(Optional.of(new LinkedHashMap<>(header)));
		when(orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderId))
				.thenReturn(Optional.of(baseTrade()));
	}

	@Test
	void apply_delegatesThroughAftersalesApplyShopApplyByNumService_andPublishesJushuitanPayloadWithSection5Keys() {
		long companyId = 10L;
		long orderId = 100L;

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", companyId);
		auth.put("user_id", 20L);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", orderId);
		merged.put("user_id", 20L);
		merged.put("aftersales_type", "ONLY_REFUND");
		merged.put("reason", "want refund");
		merged.put("description", "detail text");
		merged.put("contact", "Alice");
		merged.put("mobile", "13800000000");
		merged.put("return_type", "logistics");
		List<Map<String, Object>> detail = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", 501L);
		row.put("num", 1);
		// shopApplyByNum scales per-line total_fee by *100/num before handle; keep aligned with order line 10_000.
		row.put("total_fee", 100);
		detail.add(row);
		merged.put("detail", detail);

		frontApplyService.apply(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> jstCap = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> wdtCap = ArgumentCaptor.forClass(Map.class);
		InOrder jstThenWdt = inOrder(jushuitanTradeAftersalesDispatchPublisher, wdtErpTradeAfterSaleDispatchPublisher);
		jstThenWdt.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(jstCap.capture());
		jstThenWdt.verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(wdtCap.capture());
		Map<String, Object> payload = jstCap.getValue();
		assertTrue(SECTION5_KEYS.stream().allMatch(payload::containsKey));
		Map<String, Object> wdtPayload = wdtCap.getValue();
		assertTrue(wdtPayload.containsKey("company_id"));
		assertTrue(wdtPayload.containsKey("distributor_id"));
		assertTrue(wdtPayload.containsKey("aftersales_bn"));
		assertTrue(wdtPayload.containsKey("order_id"));
	}

	@Test
	void apply_onlyRefund_afterCommit_invokesTradeRefundDispatchPublisher_once_beforeJushuitanWithAftersalesBnAndRefundFee() {
		long companyId = 10L;
		long orderId = 100L;

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", companyId);
		auth.put("user_id", 20L);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", orderId);
		merged.put("user_id", 20L);
		merged.put("aftersales_type", "ONLY_REFUND");
		merged.put("reason", "want refund");
		merged.put("description", "detail text");
		merged.put("contact", "Alice");
		merged.put("mobile", "13800000000");
		merged.put("return_type", "logistics");
		List<Map<String, Object>> detail = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", 501L);
		row.put("num", 1);
		row.put("total_fee", 100);
		detail.add(row);
		merged.put("detail", detail);

		frontApplyService.apply(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> refundCap = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<ApplicationEvent> appEventCap = ArgumentCaptor.forClass(ApplicationEvent.class);
		InOrder ord =
				inOrder(
						redisHashOps,
						tradeRefundDispatchPublisher,
						invoiceRedJobDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						wdtErpTradeAfterSaleDispatchPublisher,
						aftersalesApplyAsyncPort);
		ord.verify(redisHashOps, times(1)).increment(anyString(), eq("orderAftersales"), eq(1L));
		ord.verify(redisHashOps, times(1)).increment(anyString(), eq("2_orderAftersales"), eq(1L));
		ord.verify(tradeRefundDispatchPublisher, times(1)).publish(refundCap.capture());
		ord.verify(invoiceRedJobDispatchPublisher, times(1)).publish(any());
		ord.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher, times(1))
				.publish(anyLong(), anyLong());
		ord.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(any());
		ord.verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(any());
		ord.verify(aftersalesApplyAsyncPort, times(1)).dispatchPostCommitSideEffects(any());

		verify(tradeAftersalesDispatchPublisher, never()).publish(any());

		verify(applicationEventPublisher, times(1)).publishEvent(appEventCap.capture());

		Map<String, Object> refund = refundCap.getValue();
		assertNotNull(refund.get("aftersales_bn"));
		assertTrue(refund.containsKey("order_id"));
		assertTrue(refund.containsKey("company_id"));
		assertEquals(20L, ((Number) refund.get("user_id")).longValue());
		assertEquals(MOCK_REFUND_BN, ((Number) refund.get("refund_bn")).longValue());
		Object refundFee = refund.get("refund_fee");
		assertNotNull(refundFee);
		assertTrue(refundFee instanceof Number);

		ApplicationEvent published = appEventCap.getValue();
		assertTrue(published instanceof SaasErpRefundSpringEvent);
		assertSame(
				refund,
				((SaasErpRefundSpringEvent) published).getPayload(),
				"post-commit only-refund branch uses the same refund map instance for bus fan-out and Saas ERP refund event");
	}

	private static Map<String, Object> baseOrderHeader() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("shop_id", 1L);
		m.put("distributor_id", 2L);
		m.put("merchant_id", 0L);
		m.put("mobile", "13900000000");
		m.put("freight_type", "cash");
		m.put("pay_type", "online");
		return m;
	}

	private static Map<String, Object> baseTrade() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("pay_type", "wxpay");
		m.put("trade_id", "TR1");
		m.put("fee_type", "CNY");
		m.put("cur_fee_type", "CNY");
		m.put("cur_fee_rate", "1");
		m.put("cur_fee_symbol", "¥");
		return m;
	}
}
