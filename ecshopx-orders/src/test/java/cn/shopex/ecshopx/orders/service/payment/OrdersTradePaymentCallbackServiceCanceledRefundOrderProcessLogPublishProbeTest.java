package cn.shopex.ecshopx.orders.service.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishAfterCommitDispatchPublisher;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.refund.OfflinePayTradeFinishCanceledNormalOrderRefundService;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("Alipay trade notify: canceled normal order refund order-process-log publish probe")
class OrdersTradePaymentCallbackServiceCanceledRefundOrderProcessLogPublishProbeTest {

	private static final String LISTENER_NAME = "listener:orders.trade_finish_offline_pay_canceled_normal_refund";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Trade.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
	}

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOps;

	@Mock
	private HashOperations<String, Object, Object> hashOps;

	@Mock
	private JushuitanTradeFinishDispatchPublisher jushuitanPublisher;

	@Mock
	private WdtErpTradeFinishDispatchPublisher wdtPublisher;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private SupplierOrderMapper supplierOrderMapper;

	@Mock
	private OrderSuccessTradeReadPort orderSuccessTradeReadPort;

	@Mock
	private AftersalesRefundService aftersalesRefundService;

	@Test
	void successStatus_applyTradePaymentNotify_whenCanceledNormalOrder_invokesRefundOrderProcessLogPublishOnce() {
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		OfflinePayTradeFinishCanceledNormalOrderRefundService refundService =
				new OfflinePayTradeFinishCanceledNormalOrderRefundService(
						normalOrdersMapper,
						supplierOrderMapper,
						orderSuccessTradeReadPort,
						aftersalesRefundService,
						orderProcessLogPublishPort);
		OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener tradeFinishListener =
				new OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener(refundService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				tradeFinishListener);

		DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of());
		DispatchFacade dispatchFacade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		OrdersTradeFinishDispatchPublisher ordersTradeFinishPublisher =
				new OrdersTradeFinishAfterCommitDispatchPublisher(
						dispatchFacade, OrdersDispatchEventNames.EVENT_TRADE_FINISH);

		ObjectMapper objectMapper = new ObjectMapper();
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		when(valueOps.increment(anyString())).thenReturn(1L);
		when(hashOps.get(anyString(), any())).thenReturn(null);

		NormalOrders canceled = new NormalOrders();
		canceled.setOrderId(501L);
		canceled.setCompanyId(9L);
		canceled.setOrderStatus("CANCEL");
		canceled.setOrderClass("normal");
		canceled.setUserId(3L);
		canceled.setPayType("alipay");
		canceled.setTotalFee("100");
		canceled.setFreightFee(0);
		canceled.setFreightType("cash");
		canceled.setShopId(88L);
		canceled.setDistributorId(12L);
		canceled.setPoint(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(canceled);
		when(supplierOrderMapper.selectList(any())).thenReturn(List.of());

		Map<String, Object> tradeMap = new LinkedHashMap<>();
		tradeMap.put("trade_id", "T1");
		tradeMap.put("pay_type", "alipay");
		tradeMap.put("fee_type", "CNY");
		tradeMap.put("cur_fee_type", "CNY");
		tradeMap.put("cur_fee_rate", 1.0);
		tradeMap.put("cur_fee_symbol", "￥");
		tradeMap.put("merchant_id", 88L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(9L, 501L)).thenReturn(Optional.of(tradeMap));

		doNothing().when(aftersalesRefundService).createRefund(any());

		Trade notPay = new Trade();
		notPay.setTradeId("T1");
		notPay.setTradeState("NOTPAY");
		notPay.setOrderId("501");
		notPay.setCompanyId("9");
		notPay.setDistributorId("12");
		notPay.setTradeSourceType("normal");
		notPay.setUserId("3");
		notPay.setMerchantId(88L);
		notPay.setDiscountFee(5);
		notPay.setPayFee(100);

		Trade success = new Trade();
		success.setTradeId("T1");
		success.setTradeState("SUCCESS");
		success.setOrderId("501");
		success.setCompanyId("9");
		success.setDistributorId("12");
		success.setTradeSourceType("normal");
		success.setUserId("3");
		success.setMerchantId(88L);
		success.setTransactionId("tx-1");
		success.setPayFee(100);
		success.setTotalFee(100);
		success.setDiscountFee(5);
		success.setPayType("alipay");

		AtomicInteger selects = new AtomicInteger();
		when(tradeMapper.selectById(eq("T1")))
				.thenAnswer(inv -> selects.incrementAndGet() <= 2 ? notPay : success);
		when(tradeMapper.updateById(any(Trade.class))).thenReturn(1);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		OrdersTradePaymentCallbackService svc =
				new OrdersTradePaymentCallbackService(
						tradeMapper,
						objectMapper,
						redisTemplate,
						jushuitanPublisher,
						ordersTradeFinishPublisher,
						wdtPublisher,
						org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class));

		svc.applyTradePaymentNotify("T1", "SUCCESS", Map.of("pay_type", "alipay", "transaction_id", "tx-1"));

		verify(orderProcessLogPublishPort, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& Objects.equals(501L, toLong(m.get("order_id")))
												&& Objects.equals(9L, toLong(m.get("company_id")))
												&& Objects.equals("system", String.valueOf(m.get("operator_type")))
												&& Objects.equals(0L, toLong(m.get("operator_id")))
												&& Objects.equals("订单退款", m.get("remarks"))
												&& Objects.equals("订单号：501，系统自动同意退款", m.get("detail"))
												&& Boolean.FALSE.equals(m.get("is_show"))
												&& m.get("params") instanceof Map<?, ?> params
												&& params.size() == 3
												&& Objects.equals(9L, toLong(params.get("company_id")))
												&& Objects.equals(501L, toLong(params.get("order_id")))
												&& Objects.equals(3L, toLong(params.get("user_id")))));
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
