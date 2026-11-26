/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.orders.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.order.port.VipGradeMembercardTradePaidPort;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OrdersTradePaymentCallbackServiceIdempotencyTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock private TradeMapper tradeMapper;
	@Mock private StringRedisTemplate redisTemplate;
	@Mock private ValueOperations<String, String> valueOps;
	@Mock private HashOperations<String, Object, Object> hashOps;
	@Mock private JushuitanTradeFinishDispatchPublisher jushuitanPublisher;
	@Mock private OrdersTradeFinishDispatchPublisher ordersTradeFinishPublisher;
	@Mock private WdtErpTradeFinishDispatchPublisher wdtPublisher;
	@Mock private ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort;

	private OrdersTradePaymentCallbackService service;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
		lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
		lenient().when(valueOps.increment(anyString())).thenReturn(1L);
		lenient().when(hashOps.get(anyString(), any())).thenReturn(null);
		service =
				new OrdersTradePaymentCallbackService(
						tradeMapper,
						new ObjectMapper(),
						redisTemplate,
						jushuitanPublisher,
						ordersTradeFinishPublisher,
						wdtPublisher,
						vipGradeMembercardTradePaidPort);
	}

	@Test
	@DisplayName("SUCCESS 更新 WHERE 必须带 trade_state=NOTPAY")
	void success_claimUpdateRequiresNotPay() {
		stubNotPayTrade();
		when(tradeMapper.updateById(any(Trade.class))).thenReturn(1);
		when(tradeMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		service.applyTradePaymentNotify(
				"T1", "SUCCESS", Map.of("pay_type", "wxpay", "transaction_id", "tx-1"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<Trade>> captor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(tradeMapper).update(isNull(), captor.capture());
		LambdaUpdateWrapper<Trade> uw = captor.getValue();
		assertThat(uw.getSqlSegment()).containsIgnoringCase("trade_state");
		assertThat(uw.getSqlSet()).containsIgnoringCase("trade_state");
		assertThat(uw.getParamNameValuePairs().values()).contains("NOTPAY");
		assertThat(uw.getExpression().getNormal().size()).isGreaterThanOrEqualTo(2);
	}

	@Test
	@DisplayName("抢占影响行数为 0 时不发布 TradeFinish")
	void success_whenClaimAffectedZero_skipsFinishPublish() {
		stubNotPayTrade();
		when(tradeMapper.updateById(any(Trade.class))).thenReturn(1);
		when(tradeMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		service.applyTradePaymentNotify(
				"T1", "SUCCESS", Map.of("pay_type", "wxpay", "transaction_id", "tx-1"));

		verify(jushuitanPublisher, never()).publish(any());
		verify(ordersTradeFinishPublisher, never()).publish(any());
		verify(wdtPublisher, never()).publish(any());
	}

	private void stubNotPayTrade() {
		Trade notPay = new Trade();
		notPay.setTradeId("T1");
		notPay.setTradeState("NOTPAY");
		notPay.setOrderId("501");
		notPay.setCompanyId("9");
		notPay.setDistributorId("0");
		when(tradeMapper.selectById(eq("T1"))).thenReturn(notPay);
	}
}
