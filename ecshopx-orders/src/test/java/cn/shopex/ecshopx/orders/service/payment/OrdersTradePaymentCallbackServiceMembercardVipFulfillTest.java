package cn.shopex.ecshopx.orders.service.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.order.port.VipGradeMembercardTradePaidPort;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OrdersTradePaymentCallbackServiceMembercardVipFulfillTest {

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
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishPublisher;

	@Mock
	private WdtErpTradeFinishDispatchPublisher wdtPublisher;

	@Mock
	private VipGradeMembercardTradePaidPort vipGradeMembercardTradePaidPort;

	@Test
	void successNotify_membercard_invokesVipGradeMembercardTradePaidPortOnce() {
		stubRedis();
		Trade notPay = baseTrade("NOTPAY", "membercard");
		Trade success = baseTrade("SUCCESS", "membercard");
		stubSelectThenSuccess(notPay, success);

		OrdersTradePaymentCallbackService svc = newService(vipGradeMembercardTradePaidPort);
		svc.applyTradePaymentNotify(
				"5353724000040002", "SUCCESS", Map.of("pay_type", "wxpay", "transaction_id", "tx-vip"));

		verify(vipGradeMembercardTradePaidPort, times(1))
				.onTradeSuccess(1L, "5353724000010002", 2L);
		verify(ordersTradeFinishPublisher, times(1)).publish(any());
	}

	@Test
	void successNotify_normal_doesNotInvokeVipGradeMembercardTradePaidPort() {
		stubRedis();
		Trade notPay = baseTrade("NOTPAY", "normal");
		Trade success = baseTrade("SUCCESS", "normal");
		stubSelectThenSuccess(notPay, success);

		OrdersTradePaymentCallbackService svc = newService(vipGradeMembercardTradePaidPort);
		svc.applyTradePaymentNotify("T1", "SUCCESS", Map.of("pay_type", "wxpay", "transaction_id", "tx-1"));

		verify(vipGradeMembercardTradePaidPort, never()).onTradeSuccess(any(Long.class), anyString(), any(Long.class));
	}

	private OrdersTradePaymentCallbackService newService(VipGradeMembercardTradePaidPort port) {
		@SuppressWarnings("unchecked")
		ObjectProvider<VipGradeMembercardTradePaidPort> provider = mock(ObjectProvider.class);
		org.mockito.Mockito.lenient().when(provider.getIfAvailable()).thenReturn(port);
		return new OrdersTradePaymentCallbackService(
				tradeMapper,
				new ObjectMapper(),
				redisTemplate,
				jushuitanPublisher,
				ordersTradeFinishPublisher,
				wdtPublisher,
				provider);
	}

	private void stubRedis() {
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		when(valueOps.increment(anyString())).thenReturn(1L);
		when(hashOps.get(anyString(), any())).thenReturn(null);
	}

	private void stubSelectThenSuccess(Trade notPay, Trade success) {
		AtomicInteger selects = new AtomicInteger();
		when(tradeMapper.selectById(anyString()))
				.thenAnswer(inv -> selects.incrementAndGet() <= 2 ? notPay : success);
		when(tradeMapper.updateById(any(Trade.class))).thenReturn(1);
		when(tradeMapper.update(any(), any())).thenReturn(1);
	}

	private static Trade baseTrade(String state, String sourceType) {
		Trade t = new Trade();
		t.setTradeId("5353724000040002");
		t.setTradeState(state);
		t.setOrderId("5353724000010002");
		t.setCompanyId("1");
		t.setDistributorId("0");
		t.setTradeSourceType(sourceType);
		t.setUserId("2");
		t.setPayFee(100);
		t.setTotalFee(100);
		return t;
	}
}
