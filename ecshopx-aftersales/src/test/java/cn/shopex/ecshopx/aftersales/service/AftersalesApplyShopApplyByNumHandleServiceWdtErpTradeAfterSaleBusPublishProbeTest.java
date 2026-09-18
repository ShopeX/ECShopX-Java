package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.integration.AftersalesApplyAsyncPortImpl;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrderRefundCompleteJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
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
import org.junit.jupiter.api.DisplayName;
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
 * Post-commit Bus publish order on the FrontApi wxapp apply-by-num chain
 * ({@code AftersalesApplyShopApplyByNumHandleService#shopApplyByNumHandle}): Jushuitan trade-aftersales
 * then WDT ERP trade-aftersale, for the {@code createByNum} / {@code __createAftersalesByNum} entry.
 * Does not cover the admin {@code create} / {@code __createAftersales} apply path.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName(
		"FrontApi wxapp 按件申请售后（createByNum / __createAftersalesByNum）— 事务提交后聚水潭再旺店通 Bus 探针；非管理端 create 路径")
class AftersalesApplyShopApplyByNumHandleServiceWdtErpTradeAfterSaleBusPublishProbeTest {

	@Mock private AftersalesMapper aftersalesMapper;
	@Mock private AftersalesDetailMapper aftersalesDetailMapper;
	@Mock private AftersalesRefundService aftersalesRefundService;
	@Mock private AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService;
	@Mock private OrderItemsProfitWritePort orderItemsProfitWritePort;
	@Mock private AftersalesBrokeragePort aftersalesBrokeragePort;
	@Mock private JdbcTemplate jdbcTemplate;
	@Mock private OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock private ApplicationEventPublisher applicationEventPublisher;
	@Mock private JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;
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

	private final AtomicReference<Aftersales> lastInsertedAftersalesMain = new AtomicReference<>();

	private AftersalesApplyShopApplyByNumHandleService service;

	@BeforeEach
	void setUp() {
		lastInsertedAftersalesMain.set(null);
		reset(jushuitanTradeAftersalesBusPayloadBuilder);
		lenient()
				.when(jushuitanTradeAftersalesBusPayloadBuilder.build(any(Aftersales.class), any()))
				.thenAnswer(
						inv -> {
							Aftersales main = inv.getArgument(0);
							@SuppressWarnings("unchecked")
							Map<String, Object> ctx = inv.getArgument(1);
							return new JushuitanTradeAftersalesBusPayloadBuilder().build(main, ctx);
						});
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
						jushuitanTradeAftersalesBusPayloadBuilder,
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
	}

	@Test
	@DisplayName("事务提交后：先聚水潭 trade-aftersales 再旺店通 ERP 售后 Bus 投递")
	void performPostCommitOrdered_invokesWdtPublisherAfterJushuitanPublisher_whenJstPayloadPresent() {
		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("shop_id", 1L);
		orderInfo.put("distributor_id", 2L);
		orderInfo.put("merchant_id", 0L);
		orderInfo.put("mobile", "13900000000");
		orderInfo.put("freight_type", "cash");
		orderInfo.put("pay_type", "online");
		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("pay_type", "wxpay");
		trade.put("trade_id", "TR1");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", "1");
		trade.put("cur_fee_symbol", "¥");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("order_id", orderId);
		data.put("user_id", 20L);
		data.put("aftersales_type", "ONLY_REFUND");
		data.put("goods_returned", false);
		data.put("reason", "want refund");
		data.put("description", "detail text");
		data.put("contact", "Alice");
		data.put("mobile", "13800000000");
		data.put("salesman_id", 99L);
		data.put("operator_type", "shop");
		data.put("operator_id", 1L);
		data.put("return_type", "logistics");
		List<Map<String, Object>> detail = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", 501L);
		row.put("num", 1);
		detail.add(row);
		data.put("detail", detail);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		ArgumentCaptor<Aftersales> insertedMain = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedMain.capture());
		Aftersales persisted = insertedMain.getValue();
		long aftersalesBn = persisted.getAftersalesBn();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> wdtCap = ArgumentCaptor.forClass(Map.class);
		InOrder jstThenWdt = inOrder(jushuitanTradeAftersalesDispatchPublisher, wdtErpTradeAfterSaleDispatchPublisher);
		jstThenWdt.verify(jushuitanTradeAftersalesDispatchPublisher).publish(any());
		jstThenWdt.verify(wdtErpTradeAfterSaleDispatchPublisher).publish(wdtCap.capture());

		Map<String, Object> wdtPayload = wdtCap.getValue();
		assertEquals(companyId, ((Number) wdtPayload.get("company_id")).longValue());
		assertEquals(2L, ((Number) wdtPayload.get("distributor_id")).longValue());
		assertEquals(aftersalesBn, ((Number) wdtPayload.get("aftersales_bn")).longValue());
		assertEquals(orderId, ((Number) wdtPayload.get("order_id")).longValue());
		assertEquals(companyId, persisted.getCompanyId());
		assertEquals(orderId, persisted.getOrderId());
		assertEquals(2L, persisted.getDistributorId());
		assertEquals(aftersalesBn, persisted.getAftersalesBn());
	}

	@Test
	@DisplayName("事务提交后：聚水潭载荷为空时仍投递旺店通；四键 payload 一致")
	void performPostCommitOrdered_invokesWdtPublisher_whenJstPayloadNull() {
		when(jushuitanTradeAftersalesBusPayloadBuilder.build(any(Aftersales.class), any())).thenReturn(null);

		long companyId = 10L;
		long orderId = 100L;
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("shop_id", 1L);
		orderInfo.put("distributor_id", 2L);
		orderInfo.put("merchant_id", 0L);
		orderInfo.put("mobile", "13900000000");
		orderInfo.put("freight_type", "cash");
		orderInfo.put("pay_type", "online");
		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("pay_type", "wxpay");
		trade.put("trade_id", "TR1");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", "1");
		trade.put("cur_fee_symbol", "¥");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("order_id", orderId);
		data.put("user_id", 20L);
		data.put("aftersales_type", "ONLY_REFUND");
		data.put("goods_returned", false);
		data.put("reason", "want refund");
		data.put("description", "detail text");
		data.put("contact", "Alice");
		data.put("mobile", "13800000000");
		data.put("salesman_id", 99L);
		data.put("operator_type", "shop");
		data.put("operator_id", 1L);
		data.put("return_type", "logistics");
		List<Map<String, Object>> detail = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", 501L);
		row.put("num", 1);
		detail.add(row);
		data.put("detail", detail);

		service.shopApplyByNumHandle(orderInfo, trade, data);

		ArgumentCaptor<Aftersales> insertedMain = ArgumentCaptor.forClass(Aftersales.class);
		verify(aftersalesMapper).insert(insertedMain.capture());
		Aftersales persisted = insertedMain.getValue();
		long aftersalesBn = persisted.getAftersalesBn();

		verify(jushuitanTradeAftersalesDispatchPublisher, never()).publish(any());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> wdtCap = ArgumentCaptor.forClass(Map.class);
		verify(wdtErpTradeAfterSaleDispatchPublisher).publish(wdtCap.capture());
		Map<String, Object> wdtPayload = wdtCap.getValue();
		assertEquals(4, wdtPayload.size());
		assertEquals(companyId, ((Number) wdtPayload.get("company_id")).longValue());
		assertEquals(2L, ((Number) wdtPayload.get("distributor_id")).longValue());
		assertEquals(aftersalesBn, ((Number) wdtPayload.get("aftersales_bn")).longValue());
		assertEquals(orderId, ((Number) wdtPayload.get("order_id")).longValue());
	}
}
