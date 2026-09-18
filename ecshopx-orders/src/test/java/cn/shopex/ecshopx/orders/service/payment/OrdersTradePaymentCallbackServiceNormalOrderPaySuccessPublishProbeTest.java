package cn.shopex.ecshopx.orders.service.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderPaySuccessDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.tradefinish.OrdersTradeFinishNormalOrderPaySuccessApplyService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OrdersTradePaymentCallbackServiceNormalOrderPaySuccessPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Trade.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
		TableInfoHelper.initTableInfo(assistant, cn.shopex.ecshopx.supplier.domain.SupplierOrder.class);
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
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishPublisher;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Test
	void successStatus_afterTradeFinishChain_invokesNormalOrderPaySuccessPublisherOnce() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		when(valueOps.increment(any())).thenReturn(1L);
		when(hashOps.get(any(), any())).thenReturn(null);

		OrdersTradeFinishNormalOrderPaySuccessApplyService applyService =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class),
						orderAssociationsMapper,
						mock(cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper.class),
						mock(cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService.class));
		NormalOrders existing = new NormalOrders();
		existing.setCompanyId(9L);
		existing.setOrderId(501L);
		existing.setOrderStatus("NOTPAY");
		existing.setOrderClass("normal");
		existing.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(any())).thenReturn(existing);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		NormalOrderPaySuccessDispatchPublisher paySuccessPublisher = mock(NormalOrderPaySuccessDispatchPublisher.class);
		OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener bridge =
				new OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener(applyService, paySuccessPublisher);

		doAnswer(
						inv -> {
							bridge.onEvent(inv.getArgument(0));
							return null;
						})
				.when(ordersTradeFinishPublisher)
				.publish(any());

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

		verify(paySuccessPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& Objects.equals(9L, longFrom(m.get("company_id")))
												&& Objects.equals(501L, longFrom(m.get("order_id")))
												&& Objects.equals("alipay", Objects.toString(m.get("pay_type"), null))
												&& Objects.equals("normal", Objects.toString(m.get("trade_source_type"), null))));
	}

	private static Long longFrom(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
