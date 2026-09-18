package cn.shopex.ecshopx.orders.service.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
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
class OrdersTradePaymentCallbackServiceTradePayFinishStatisticsPublishTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
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

	@Test
	void successStatus_publishesOrdersTradeFinishRowWithTradePayFinishStatisticsKeys() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		when(valueOps.increment(anyString())).thenReturn(1L);
		when(hashOps.get(anyString(), any())).thenReturn(null);

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

		AtomicInteger selects = new AtomicInteger();
		when(tradeMapper.selectById(eq("T1")))
				.thenAnswer(
						inv -> selects.incrementAndGet() <= 2 ? notPay : success);
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

		// EVENT_TRADE_FINISH row must surface order_id, company_id, user_id, discount fee, pay fee, distributor
		// (sync trade-finish listeners).
		verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("order_id")
												&& m.containsKey("trade_source_type")
												&& m.containsKey("trade_state")
												&& (m.containsKey("total_fee") || m.containsKey("pay_fee"))
												&& m.containsKey("user_id")
												&& m.containsKey("distributor_id")
												&& m.containsKey("pay_fee")
												&& m.containsKey("discount_fee")
												&& m.containsKey("merchant_id")
												&& Objects.equals("501", String.valueOf(m.get("order_id")))
												&& Objects.equals("9", String.valueOf(m.get("company_id")))
												&& Objects.equals(
														"SUCCESS", String.valueOf(m.get("trade_state")))
												&& Objects.equals("3", String.valueOf(m.get("user_id")))
												&& Objects.equals(100, intValueOfMapNumber(m.get("pay_fee")))
												&& Objects.equals(5, intValueOfMapNumber(m.get("discount_fee")))
												&& Objects.equals("12", String.valueOf(m.get("distributor_id")))));
	}

	@Test
	void successStatus_ordersTradeFinishPublish_includesTradeFinishFapiaoGateKeys() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		when(valueOps.increment(anyString())).thenReturn(1L);
		when(hashOps.get(anyString(), any())).thenReturn(null);

		Trade notPay = new Trade();
		notPay.setTradeId("T2");
		notPay.setTradeState("NOTPAY");
		notPay.setOrderId("502");
		notPay.setCompanyId("9");
		notPay.setDistributorId("12");
		notPay.setTradeSourceType("normal");
		notPay.setUserId("3");
		notPay.setMerchantId(88L);
		notPay.setDiscountFee(5);
		notPay.setPayFee(100);

		Trade success = new Trade();
		success.setTradeId("T2");
		success.setTradeState("SUCCESS");
		success.setOrderId("502");
		success.setCompanyId("9");
		success.setDistributorId("12");
		success.setTradeSourceType("normal");
		success.setUserId("3");
		success.setMerchantId(88L);
		success.setTransactionId("tx-2");
		success.setPayFee(100);
		success.setTotalFee(100);
		success.setDiscountFee(5);
		success.setPayType("alipay");

		AtomicInteger selects = new AtomicInteger();
		when(tradeMapper.selectById(eq("T2")))
				.thenAnswer(
						inv -> selects.incrementAndGet() <= 2 ? notPay : success);
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

		svc.applyTradePaymentNotify("T2", "SUCCESS", Map.of("pay_type", "alipay", "transaction_id", "tx-2"));

		verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("order_id")
												&& m.containsKey("user_id")
												&& m.containsKey("trade_source_type")
												&& Objects.equals(
														"SUCCESS", String.valueOf(m.get("trade_state")))
												&& Objects.equals("502", String.valueOf(m.get("order_id")))
												&& Objects.equals(
														"alipay",
														String.valueOf(m.get("pay_type")))));
	}

	private static int intValueOfMapNumber(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}
}
