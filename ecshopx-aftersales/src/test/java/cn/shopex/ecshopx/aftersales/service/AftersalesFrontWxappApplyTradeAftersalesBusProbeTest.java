package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
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
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

/**
 * Proves {@link AftersalesFrontWxappApplyService} → {@link AftersalesApplyShopApplyByNumService} →
 * {@link AftersalesApplyShopApplyByNumHandleService} on return-goods / exchange paths publishes
 * {@link TradeAftersalesDispatchPublisher} (system link trade-aftersales fan-out) and
 * {@link ThirdPartyTradeAftersalesSaasErpDispatchPublisher} (third-party Saas ERP trade-aftersales
 * event) after commit, in that order before wait-deal and Jushuitan publishers—shared handle with the
 * admin shop-by-num entry.
 *
 * <p>Each {@link AftersalesApplyShopApplyByNumService} iteration over detail rows (or quantity splits)
 * invokes the handle once; {@link #apply_refundGoodsThroughFrontWxapp_twoDetailRows_publishesSaasErpPerIteration()}
 * asserts one Saas ERP publish per iteration when two single-quantity lines are applied.
 *
 * <p>The same handle builds the order-process log payload consumed via {@link OrderProcessLogPublishPort}
 * ({@code EVENT_ORDER_PROCESS_LOG} on the unified dispatch bus); see {@link
 * #apply_refundGoodsThroughFrontWxapp_invokesOrderProcessLogPublishPortOnce()} and {@link
 * #apply_exchangingGoodsThroughFrontWxapp_invokesOrderProcessLogPublishPortOnce()}.
 */
@ExtendWith(MockitoExtension.class)
class AftersalesFrontWxappApplyTradeAftersalesBusProbeTest {

	private static final long MOCK_REFUND_BN = 29001230123456789L;
	private static final long COMPANY_ID = 10L;
	private static final long ORDER_ID = 100L;
	private static final int LINE_SUPPLIER_ID = 3;

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
	@Mock
	private SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher;

	@Mock private OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort;
	@Mock private OfflineAftersalesDistributorHeadReadPort offlineAftersalesDistributorHeadReadPort;
	@Mock private WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	@Mock private TradeRefundDispatchPublisher tradeRefundDispatchPublisher;
	@Mock private TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher;
	@Mock private ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher;

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
		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);
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
		line.put("supplier_id", LINE_SUPPLIER_ID);
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
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

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

		Map<String, Object> header = baseOrderHeader();
		header.put("order_status", "TRADE");
		header.put("receipt_type", "express");
		when(orderAssociationReadPort.getAssociation(COMPANY_ID, ORDER_ID))
				.thenReturn(Optional.of(Map.of("order_type", "normal")));
		when(orderNormalOrderHeaderReadPort.getHeader(COMPANY_ID, ORDER_ID))
				.thenReturn(Optional.of(new LinkedHashMap<>(header)));
		when(orderSuccessTradeReadPort.primarySuccessTrade(COMPANY_ID, ORDER_ID))
				.thenReturn(Optional.of(baseTrade()));
	}

	@Test
	void apply_refundGoodsThroughFrontWxapp_publishesTradeAftersalesBusOnce() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", COMPANY_ID);
		auth.put("user_id", 20L);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		LinkedHashMap<String, Object> merged = frontApplyMerged("REFUND_GOODS");

		frontApplyService.apply(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> busCap = ArgumentCaptor.forClass(Map.class);
		verify(tradeAftersalesDispatchPublisher, times(1)).publish(busCap.capture());
		Map<String, Object> busPayload = busCap.getValue();
		assertThat(busPayload.get("company_id")).isNotNull();
		assertThat(busPayload.get("order_id")).isNotNull();
		assertThat(busPayload.get("aftersales_bn")).isNotNull();
		assertThat(busPayload.get("aftersales_type").toString()).isEqualTo("REFUND_GOODS");

		ArgumentCaptor<Long> bnCaptor = ArgumentCaptor.forClass(Long.class);
		verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher, times(1))
				.publish(eq(COMPANY_ID), bnCaptor.capture());
		assertThat(bnCaptor.getValue()).isPositive();

		InOrder ord =
				inOrder(
						redisHashOps,
						tradeAftersalesDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher);
		ord.verify(redisHashOps, times(1)).increment(anyString(), eq("orderAftersales"), eq(1L));
		ord.verify(redisHashOps, times(1)).increment(anyString(), eq("2_orderAftersales"), eq(1L));
		ord.verify(tradeAftersalesDispatchPublisher).publish(any());
		ord.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(any());
		ord.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher).publish(eq(COMPANY_ID), anyLong());
		ord.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(any());
	}

	@Test
	void apply_exchangingGoodsThroughFrontWxapp_publishesTradeAftersalesBusOnce() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", COMPANY_ID);
		auth.put("user_id", 20L);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		LinkedHashMap<String, Object> merged = frontApplyMerged("EXCHANGING_GOODS");

		frontApplyService.apply(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> busCap = ArgumentCaptor.forClass(Map.class);
		verify(tradeAftersalesDispatchPublisher, times(1)).publish(busCap.capture());
		Map<String, Object> busPayload = busCap.getValue();
		assertThat(busPayload.get("company_id")).isNotNull();
		assertThat(busPayload.get("order_id")).isNotNull();
		assertThat(busPayload.get("aftersales_bn")).isNotNull();
		assertThat(busPayload.get("aftersales_type").toString()).isEqualTo("EXCHANGING_GOODS");

		ArgumentCaptor<Long> bnCaptorEx = ArgumentCaptor.forClass(Long.class);
		verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher, times(1))
				.publish(eq(COMPANY_ID), bnCaptorEx.capture());
		assertThat(bnCaptorEx.getValue()).isPositive();

		InOrder ordEx =
				inOrder(
						redisHashOps,
						tradeAftersalesDispatchPublisher,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						sendAfterSaleWaitDealNoticeJobDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher);
		ordEx.verify(redisHashOps, times(1)).increment(anyString(), eq("orderAftersales"), eq(1L));
		ordEx.verify(redisHashOps, times(1)).increment(anyString(), eq("2_orderAftersales"), eq(1L));
		ordEx.verify(tradeAftersalesDispatchPublisher).publish(any());
		ordEx.verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(1)).publish(any());
		ordEx.verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher).publish(eq(COMPANY_ID), anyLong());
		ordEx.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(any());
	}

	/**
	 * Two single-quantity detail lines produce two handle iterations; each publishes Saas ERP
	 * trade-aftersales once (same count as system-link trade-aftersales). A single row with
	 * {@code num} &gt; 1 is not used here because the shop split fee adjustment re-applies scaling each
	 * inner iteration against the original line count, which would inflate {@code total_fee} on
	 * subsequent passes for this fixture shape.
	 */
	@Test
	void apply_refundGoodsThroughFrontWxapp_twoDetailRows_publishesSaasErpPerIteration() {
		int iterations = 2;
		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", COMPANY_ID);
		auth.put("user_id", 20L);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		LinkedHashMap<String, Object> merged = frontApplyMergedMultiRow("REFUND_GOODS", iterations);

		frontApplyService.apply(request, merged);

		verify(tradeAftersalesDispatchPublisher, times(iterations)).publish(any());
		verify(thirdPartyTradeAftersalesSaasErpDispatchPublisher, times(iterations)).publish(any());
		verify(sendAfterSaleWaitDealNoticeJobDispatchPublisher, times(iterations))
				.publish(eq(COMPANY_ID), anyLong());
	}

	@Test
	void apply_refundGoodsThroughFrontWxapp_invokesOrderProcessLogPublishPortOnce() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", COMPANY_ID);
		auth.put("user_id", 20L);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		LinkedHashMap<String, Object> merged = frontApplyMerged("REFUND_GOODS");

		frontApplyService.apply(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		Map<String, Object> log = logCaptor.getValue();
		assertThat(log.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(log.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(log.get("supplier_id")).isEqualTo((long) LINE_SUPPLIER_ID);
		assertThat(log.get("remarks")).isEqualTo("订单售后");
		assertThat(log.get("detail").toString())
				.contains("后台申请售后")
				.contains("申请原因")
				.contains("probe reason");
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) log.get("params");
		assertThat(params).isNotNull();
		assertThat(params.get("order_id")).isNotNull();
		assertThat(params.get("company_id")).isNotNull();
		assertThat(params.get("reason")).isEqualTo("probe reason");
	}

	@Test
	void apply_exchangingGoodsThroughFrontWxapp_invokesOrderProcessLogPublishPortOnce() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", COMPANY_ID);
		auth.put("user_id", 20L);
		when(request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR)).thenReturn(auth);

		LinkedHashMap<String, Object> merged = frontApplyMerged("EXCHANGING_GOODS");

		frontApplyService.apply(request, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCaptor.capture());
		Map<String, Object> log = logCaptor.getValue();
		assertThat(log.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(log.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(log.get("supplier_id")).isEqualTo((long) LINE_SUPPLIER_ID);
		assertThat(log.get("remarks")).isEqualTo("订单售后");
		assertThat(log.get("detail").toString())
				.contains("后台申请售后")
				.contains("申请原因")
				.contains("probe reason");
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) log.get("params");
		assertThat(params).isNotNull();
		assertThat(params.get("order_id")).isNotNull();
		assertThat(params.get("company_id")).isNotNull();
		assertThat(params.get("reason")).isEqualTo("probe reason");
	}

	private LinkedHashMap<String, Object> frontApplyMerged(String aftersalesType) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", ORDER_ID);
		merged.put("user_id", 20L);
		merged.put("aftersales_type", aftersalesType);
		merged.put("reason", "probe reason");
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
		return merged;
	}

	private static LinkedHashMap<String, Object> frontApplyMergedMultiRow(String aftersalesType, int rowCount) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", ORDER_ID);
		merged.put("user_id", 20L);
		merged.put("aftersales_type", aftersalesType);
		merged.put("reason", "probe reason");
		merged.put("description", "detail text");
		merged.put("contact", "Alice");
		merged.put("mobile", "13800000000");
		merged.put("return_type", "logistics");
		List<Map<String, Object>> detail = new ArrayList<>();
		for (int i = 0; i < rowCount; i++) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", 501L);
			row.put("num", 1);
			row.put("total_fee", 100);
			detail.add(row);
		}
		merged.put("detail", detail);
		return merged;
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
