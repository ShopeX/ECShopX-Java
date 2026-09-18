package cn.shopex.ecshopx.orders.service.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
class OrdersTradePaymentCallbackServiceWdtPublishOrderTest {

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
	void successStatus_publishesJushuitanThenWdtErpTradeFinishInOrder() throws Exception {
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
		notPay.setDistributorId("0");
		notPay.setTradeSourceType("normal");
		notPay.setUserId("3");

		Trade success = new Trade();
		success.setTradeId("T1");
		success.setTradeState("SUCCESS");
		success.setOrderId("501");
		success.setCompanyId("9");
		success.setDistributorId("0");
		success.setTradeSourceType("normal");
		success.setUserId("3");
		success.setTransactionId("tx-1");
		success.setPayFee(100);
		success.setTotalFee(100);

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

		var ord = inOrder(jushuitanPublisher, ordersTradeFinishPublisher, wdtPublisher);
		ord.verify(jushuitanPublisher, times(1)).publish(any());
		ord.verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("order_id")
												&& Objects.equals("501", String.valueOf(m.get("order_id")))
												&& Objects.equals("9", String.valueOf(m.get("company_id")))));
		ord.verify(wdtPublisher, times(1)).publish(any());
	}

	@Test
	void successStatus_publishesJushuitanThenOrdersTradeFinishThenWdtErpInOrder() throws Exception {
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
		notPay.setDistributorId("0");
		notPay.setTradeSourceType("normal");
		notPay.setUserId("3");

		Trade success = new Trade();
		success.setTradeId("T1");
		success.setTradeState("SUCCESS");
		success.setOrderId("501");
		success.setCompanyId("9");
		success.setDistributorId("0");
		success.setTradeSourceType("normal");
		success.setUserId("3");
		success.setTransactionId("tx-1");
		success.setPayFee(100);
		success.setTotalFee(100);
		success.setMobile("13800138000");
		success.setTimeStart("1704067200");

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

		var ord = inOrder(jushuitanPublisher, ordersTradeFinishPublisher, wdtPublisher);
		ord.verify(jushuitanPublisher, times(1)).publish(any());
		ord.verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("mobile")
												&& m.containsKey("pay_type")
												&& m.containsKey("time_start")
												&& m.containsKey("pay_fee")
												&& Objects.equals("501", String.valueOf(m.get("order_id")))
												&& Objects.equals(
														"normal",
														String.valueOf(m.get("trade_source_type")))));
		ord.verify(wdtPublisher, times(1)).publish(any());
	}

	@Test
	void successStatus_normalGroupsTrade_publishesOrdersTradeFinishRowWithGroupFields() throws Exception {
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
		notPay.setDistributorId("0");
		notPay.setTradeSourceType("normal_groups");
		notPay.setUserId("3");

		Trade success = new Trade();
		success.setTradeId("T1");
		success.setTradeState("SUCCESS");
		success.setOrderId("501");
		success.setCompanyId("9");
		success.setDistributorId("0");
		success.setTradeSourceType("normal_groups");
		success.setUserId("3");
		success.setTransactionId("tx-1");
		success.setPayFee(100);
		success.setTotalFee(100);

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

		verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("order_id")
												&& m.containsKey("trade_source_type")
												&& Objects.equals("501", String.valueOf(m.get("order_id")))
												&& Objects.equals("9", String.valueOf(m.get("company_id")))
												&& "normal_groups"
														.equalsIgnoreCase(
																String.valueOf(m.get("trade_source_type"))
																		.trim())));
	}

	@Test
	void successStatus_publishesOrdersTradeFinishRowWithBrokerageDispatchPayloadKeys() throws Exception {
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
		notPay.setDistributorId("0");
		notPay.setTradeSourceType("normal");
		notPay.setUserId("3");

		Trade success = new Trade();
		success.setTradeId("T1");
		success.setTradeState("SUCCESS");
		success.setOrderId("501");
		success.setCompanyId("9");
		success.setDistributorId("0");
		success.setTradeSourceType("normal");
		success.setUserId("3");
		success.setTransactionId("tx-1");
		success.setPayFee(100);
		success.setTotalFee(100);
		success.setPayType("alipay");

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

		verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("order_id")
												&& m.containsKey("pay_type")
												&& m.containsKey("trade_state")
												&& m.containsKey("pay_fee")
												&& Objects.equals("SUCCESS", String.valueOf(m.get("trade_state")))
												&& Objects.equals("501", String.valueOf(m.get("order_id")))
												&& Objects.equals("9", String.valueOf(m.get("company_id")))
												&& "alipay".equalsIgnoreCase(String.valueOf(m.get("pay_type")).trim())));
	}

	@Test
	void successStatus_publishesOrdersTradeFinishRowWithShopAndMemberLinkPayloadKeys() throws Exception {
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
		notPay.setShopId("44");
		notPay.setTradeSourceType("normal");
		notPay.setUserId("3");

		Trade success = new Trade();
		success.setTradeId("T1");
		success.setTradeState("SUCCESS");
		success.setOrderId("501");
		success.setCompanyId("9");
		success.setDistributorId("12");
		success.setShopId("44");
		success.setTradeSourceType("normal");
		success.setUserId("3");
		success.setTransactionId("tx-1");
		success.setPayFee(100);
		success.setTotalFee(100);
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

		verify(ordersTradeFinishPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& m.containsKey("user_id")
												&& m.containsKey("shop_id")
												&& m.containsKey("distributor_id")
												&& Objects.equals("501", String.valueOf(m.get("order_id")))
												&& Objects.equals("9", String.valueOf(m.get("company_id")))
												&& Objects.equals("3", String.valueOf(m.get("user_id")))
												&& Objects.equals("44", String.valueOf(m.get("shop_id")))
												&& Objects.equals("12", String.valueOf(m.get("distributor_id")))));
	}
}
