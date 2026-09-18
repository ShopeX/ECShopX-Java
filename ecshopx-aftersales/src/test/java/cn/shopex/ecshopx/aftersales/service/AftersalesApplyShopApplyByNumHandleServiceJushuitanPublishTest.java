package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.integration.AftersalesApplyAsyncPortImpl;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrderRefundCompleteJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.common.port.distribution.OfflineAftersalesDistributorHeadReadPort;
import cn.shopex.ecshopx.common.port.order.OrderItemsProfitWritePort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AftersalesApplyShopApplyByNumHandleServiceJushuitanPublishTest {

	/**
	 * Keys locked in {@code entry-01-api-aftersales-apply-plan.md} §5 (Jushuitan trade-aftersales bus
	 * payload).
	 */
	private static final List<String> SECTION5_JUSHUITAN_BUS_PAYLOAD_KEYS =
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
	@Mock private OrderRefundCompleteJobDispatchPublisher orderRefundCompleteJobDispatchPublisher;
	private AftersalesApplyAsyncPort aftersalesApplyAsyncPort;
	@Mock private SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher;
	@Mock private OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort;
	@Mock private OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort;
	@Mock private OfflineAftersalesDistributorHeadReadPort offlineAftersalesDistributorHeadReadPort;
	@Mock private WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	@Mock private TradeRefundDispatchPublisher tradeRefundDispatchPublisher;
	@Mock private TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher;
	@Mock private ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher;

	private final AtomicReference<Aftersales> lastInsertedAftersalesMain = new AtomicReference<>();

	private AftersalesApplyShopApplyByNumHandleService service;

	@BeforeEach
	void setUp() {
		lastInsertedAftersalesMain.set(null);
		aftersalesApplyAsyncPort = new AftersalesApplyAsyncPortImpl(orderRefundCompleteJobDispatchPublisher);
		PlatformTransactionManager ptm = mock(PlatformTransactionManager.class);
		when(ptm.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(companysRedisTemplate.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(anyString(), any(), anyLong())).thenReturn(1L);
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
							p.put("refund_bn", 29001230123456789L);
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
		when(orderNormalOrderItemsReadPort.listItems(anyLong(), anyLong())).thenReturn(List.of(line));

		doAnswer(
						inv -> {
							lastInsertedAftersalesMain.set(inv.getArgument(0));
							return 1;
						})
				.when(aftersalesMapper)
				.insert(any(Aftersales.class));
		when(aftersalesMapper.selectOne(any())).thenAnswer(inv -> lastInsertedAftersalesMain.get());

		service =
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
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);
	}

	@Test
	void publishOnce_forOnlyRefund_afterPostCommitOrdered() {
		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = baseOrderInfo();
		Map<String, Object> trade = baseTrade();
		Map<String, Object> data = baseData("ONLY_REFUND", companyId, orderId);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		ArgumentCaptor<Aftersales> insertedMain = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedMain.capture());
		long aftersalesBn = insertedMain.getValue().getAftersalesBn();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> jstCap = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> invoiceCap = ArgumentCaptor.forClass(Map.class);
		InOrder inOrder =
				inOrder(
						invoiceRedJobDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						wdtErpTradeAfterSaleDispatchPublisher,
						orderRefundCompleteJobDispatchPublisher);
		inOrder.verify(invoiceRedJobDispatchPublisher).publish(invoiceCap.capture());
		inOrder.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher).publish(eq(companyId), eq(aftersalesBn));
		inOrder.verify(jushuitanTradeAftersalesDispatchPublisher).publish(jstCap.capture());
		inOrder.verify(wdtErpTradeAfterSaleDispatchPublisher).publish(any());
		inOrder.verify(orderRefundCompleteJobDispatchPublisher).publish(eq(companyId), eq(orderId));
		Map<String, Object> invoiceSnap = invoiceCap.getValue();
		assertEquals(aftersalesBn, ((Number) invoiceSnap.get("aftersales_bn")).longValue());
		assertEquals(companyId, ((Number) invoiceSnap.get("company_id")).longValue());
		assertEquals(orderId, ((Number) invoiceSnap.get("order_id")).longValue());
		assertEquals("ONLY_REFUND", invoiceSnap.get("aftersales_type"));
		Map<String, Object> payload = jstCap.getValue();
		assertEquals(aftersalesBn, ((Number) payload.get("aftersales_bn")).longValue());
		assertEquals(companyId, ((Number) payload.get("company_id")).longValue());
		assertEquals(orderId, ((Number) payload.get("order_id")).longValue());
		assertEquals(2L, ((Number) payload.get("distributor_id")).longValue());
		assertEquals(1L, ((Number) payload.get("shop_id")).longValue());
		assertEquals("ONLY_REFUND", payload.get("aftersales_type"));
		assertEquals(99L, ((Number) payload.get("salesman_id")).longValue());
	}

	@Test
	void publishOnce_forRefundGoods_afterPostCommitOrdered() {
		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = baseOrderInfo();
		Map<String, Object> trade = baseTrade();
		Map<String, Object> data = baseData("REFUND_GOODS", companyId, orderId);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		ArgumentCaptor<Aftersales> insertedMain = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedMain.capture());
		long aftersalesBn = insertedMain.getValue().getAftersalesBn();

		verify(invoiceRedJobDispatchPublisher, never()).publish(any());
		InOrder refundGoodsNoReturnInOrder =
				inOrder(
						tradeAftersalesDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						wdtErpTradeAfterSaleDispatchPublisher,
						orderRefundCompleteJobDispatchPublisher);
		refundGoodsNoReturnInOrder.verify(tradeAftersalesDispatchPublisher, times(1)).publish(any());
		refundGoodsNoReturnInOrder
				.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1))
				.publish(any());
		refundGoodsNoReturnInOrder
				.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher, times(1))
				.publish(eq(companyId), eq(aftersalesBn));
		refundGoodsNoReturnInOrder.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(any());
		refundGoodsNoReturnInOrder.verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(any());
		refundGoodsNoReturnInOrder
				.verify(orderRefundCompleteJobDispatchPublisher, never())
				.publish(anyLong(), anyLong());
	}

	@Test
	void publishOnce_forRefundGoods_whenGoodsReturned_afterPostCommitOrdered() {
		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = baseOrderInfo();
		Map<String, Object> trade = baseTrade();
		Map<String, Object> data = baseData("REFUND_GOODS", companyId, orderId, true);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		verify(invoiceRedJobDispatchPublisher, never()).publish(any());
		ArgumentCaptor<Aftersales> insertedMainGr = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedMainGr.capture());
		long aftersalesBn = insertedMainGr.getValue().getAftersalesBn();

		InOrder goodsReturnedInOrder =
				inOrder(
						tradeAftersalesDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						wdtErpTradeAfterSaleDispatchPublisher,
						orderRefundCompleteJobDispatchPublisher);
		goodsReturnedInOrder.verify(tradeAftersalesDispatchPublisher, times(1)).publish(any());
		goodsReturnedInOrder
				.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1))
				.publish(any());
		goodsReturnedInOrder
				.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher, times(1))
				.publish(eq(companyId), eq(aftersalesBn));
		goodsReturnedInOrder.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(any());
		goodsReturnedInOrder.verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(any());
		goodsReturnedInOrder.verify(orderRefundCompleteJobDispatchPublisher).publish(eq(companyId), eq(orderId));
	}

	@Test
	void publishPayload_containsAllSection5Keys_forOnlyRefund_afterPostCommitOrdered() {
		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = baseOrderInfo();
		Map<String, Object> trade = baseTrade();
		Map<String, Object> data = baseData("ONLY_REFUND", companyId, orderId);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		ArgumentCaptor<Aftersales> insertedMain = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedMain.capture());
		long aftersalesBn = insertedMain.getValue().getAftersalesBn();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		InOrder onlyRefundInOrder =
				inOrder(
						invoiceRedJobDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						wdtErpTradeAfterSaleDispatchPublisher,
						orderRefundCompleteJobDispatchPublisher);
		onlyRefundInOrder.verify(invoiceRedJobDispatchPublisher).publish(any());
		onlyRefundInOrder.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher).publish(eq(companyId), eq(aftersalesBn));
		onlyRefundInOrder.verify(jushuitanTradeAftersalesDispatchPublisher).publish(cap.capture());
		onlyRefundInOrder.verify(wdtErpTradeAfterSaleDispatchPublisher).publish(any());
		onlyRefundInOrder.verify(orderRefundCompleteJobDispatchPublisher).publish(eq(companyId), eq(orderId));
		Map<String, Object> payload = cap.getValue();
		for (String key : SECTION5_JUSHUITAN_BUS_PAYLOAD_KEYS) {
			assertTrue(payload.containsKey(key), () -> "missing §5 key: " + key);
		}
		assertEquals("ONLY_REFUND", payload.get("aftersales_type"));
	}

	@Test
	void publishPayload_containsAllSection5Keys_forRefundGoods_afterPostCommitOrdered() {
		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = baseOrderInfo();
		Map<String, Object> trade = baseTrade();
		Map<String, Object> data = baseData("REFUND_GOODS", companyId, orderId);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		verify(invoiceRedJobDispatchPublisher, never()).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Aftersales> insertedMainSec5 = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedMainSec5.capture());
		long aftersalesBn = insertedMainSec5.getValue().getAftersalesBn();

		InOrder refundGoodsInOrder =
				inOrder(
						tradeAftersalesDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						wdtErpTradeAfterSaleDispatchPublisher,
						orderRefundCompleteJobDispatchPublisher);
		refundGoodsInOrder.verify(tradeAftersalesDispatchPublisher).publish(any());
		refundGoodsInOrder.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher).publish(any());
		refundGoodsInOrder
				.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher)
				.publish(eq(companyId), eq(aftersalesBn));
		refundGoodsInOrder.verify(jushuitanTradeAftersalesDispatchPublisher).publish(cap.capture());
		refundGoodsInOrder.verify(wdtErpTradeAfterSaleDispatchPublisher).publish(any());
		refundGoodsInOrder.verify(orderRefundCompleteJobDispatchPublisher, never()).publish(anyLong(), anyLong());
		Map<String, Object> payload = cap.getValue();
		for (String key : SECTION5_JUSHUITAN_BUS_PAYLOAD_KEYS) {
			assertTrue(payload.containsKey(key), () -> "missing §5 key: " + key);
		}
		assertEquals("REFUND_GOODS", payload.get("aftersales_type"));
	}

	@Test
	void neverPublish_invoiceRed_forExchangingGoods_afterPostCommitOrdered() {
		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = baseOrderInfo();
		Map<String, Object> trade = baseTrade();
		Map<String, Object> data = baseData("EXCHANGING_GOODS", companyId, orderId);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		verify(invoiceRedJobDispatchPublisher, never()).publish(any());
		ArgumentCaptor<Aftersales> insertedEx = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedEx.capture());
		long aftersalesBn = insertedEx.getValue().getAftersalesBn();

		InOrder exchangingInOrder =
				inOrder(
						tradeAftersalesDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						wdtErpTradeAfterSaleDispatchPublisher,
						orderRefundCompleteJobDispatchPublisher);
		exchangingInOrder.verify(tradeAftersalesDispatchPublisher, times(1)).publish(any());
		exchangingInOrder
				.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1))
				.publish(any());
		exchangingInOrder
				.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher, times(1))
				.publish(eq(companyId), eq(aftersalesBn));
		exchangingInOrder.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(any());
		exchangingInOrder.verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(any());
		exchangingInOrder.verify(orderRefundCompleteJobDispatchPublisher, never()).publish(anyLong(), anyLong());
	}

	private static Map<String, Object> baseOrderInfo() {
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

	private static Map<String, Object> baseData(String aftersalesType, long companyId, long orderId) {
		return baseData(aftersalesType, companyId, orderId, false);
	}

	private static Map<String, Object> baseData(
			String aftersalesType, long companyId, long orderId, boolean goodsReturned) {
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("company_id", companyId);
		d.put("order_id", orderId);
		d.put("user_id", 20L);
		d.put("aftersales_type", aftersalesType);
		d.put("goods_returned", goodsReturned);
		d.put("reason", "want refund");
		d.put("description", "detail text");
		d.put("contact", "Alice");
		d.put("mobile", "13800000000");
		d.put("salesman_id", 99L);
		d.put("operator_type", "shop");
		d.put("operator_id", 1L);
		d.put("return_type", "logistics");
		List<Map<String, Object>> detail = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", 501L);
		row.put("num", 1);
		detail.add(row);
		d.put("detail", detail);
		return d;
	}
}
