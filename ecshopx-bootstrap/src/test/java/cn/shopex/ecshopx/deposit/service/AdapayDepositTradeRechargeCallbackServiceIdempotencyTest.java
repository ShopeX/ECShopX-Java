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

package cn.shopex.ecshopx.deposit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.RechargeSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/** ECX-10915：储值充值回调必须原子抢占，防止并发双次入账。 */
class AdapayDepositTradeRechargeCallbackServiceIdempotencyTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), DepositTrade.class);
	}

	private static DepositTrade pendingTrade() {
		DepositTrade pending = new DepositTrade();
		pending.setDepositTradeId("d1");
		pending.setCompanyId("10");
		pending.setUserId("20");
		pending.setMobile("13900001001");
		pending.setTradeStatus("NOTPAY");
		pending.setCurPayFee("5000");
		pending.setMoney("8000");
		return pending;
	}

	private static Map<String, Object> successOptions() {
		Map<String, Object> options = new HashMap<>();
		options.put("bank_type", "ALIPAY");
		options.put("transaction_id", "tx1");
		options.put("pay_type", "alipay");
		return options;
	}

	@Test
	void updateWrapper_carriesTradeStatusGuard() {
		DepositTradeMapper depositTradeMapper = mock(DepositTradeMapper.class);
		UserDepositBalanceMutationService mutation = mock(UserDepositBalanceMutationService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(any(), any(), any(Long.class))).thenReturn(1L);
		RechargeSendSmsNoticeJobDispatchPublisher publisher =
				mock(RechargeSendSmsNoticeJobDispatchPublisher.class);

		when(depositTradeMapper.selectById("d1")).thenReturn(pendingTrade(), pendingTrade());
		when(depositTradeMapper.update(any(), any())).thenReturn(1);

		AdapayDepositTradeRechargeCallbackService svc =
				new AdapayDepositTradeRechargeCallbackService(depositTradeMapper, mutation, redis, publisher);

		svc.rechargeCallback("d1", "SUCCESS", successOptions());

		@SuppressWarnings("unchecked")
		org.mockito.ArgumentCaptor<Wrapper<DepositTrade>> captor =
				org.mockito.ArgumentCaptor.forClass(Wrapper.class);
		verify(depositTradeMapper).update(any(), captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("trade_status");
	}

	@Test
	void concurrentTakeoverLoss_updateReturnsZero_rejectsWithoutSideEffects() {
		DepositTradeMapper depositTradeMapper = mock(DepositTradeMapper.class);
		UserDepositBalanceMutationService mutation = mock(UserDepositBalanceMutationService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		RechargeSendSmsNoticeJobDispatchPublisher publisher =
				mock(RechargeSendSmsNoticeJobDispatchPublisher.class);

		when(depositTradeMapper.selectById("d1")).thenReturn(pendingTrade());
		// 并发方已抢先置 SUCCESS：带 ne(trade_status) 条件的 update 不再命中
		when(depositTradeMapper.update(any(), any())).thenReturn(0);

		AdapayDepositTradeRechargeCallbackService svc =
				new AdapayDepositTradeRechargeCallbackService(depositTradeMapper, mutation, redis, publisher);

		assertThatThrownBy(() -> svc.rechargeCallback("d1", "SUCCESS", successOptions()))
				.isInstanceOf(ResourceException.class)
				.hasMessage("更新已处理，不需要更新");

		verify(mutation, never()).addUserDepositTotal(anyLong(), anyLong(), anyLong());
		verifyNoInteractions(redis);
		verifyNoInteractions(publisher);
	}

	@Test
	void firstCallback_updateSucceeds_appliesDepositEffects() {
		DepositTradeMapper depositTradeMapper = mock(DepositTradeMapper.class);
		UserDepositBalanceMutationService mutation = mock(UserDepositBalanceMutationService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(any(), any(), any(Long.class))).thenReturn(1L);
		RechargeSendSmsNoticeJobDispatchPublisher publisher =
				mock(RechargeSendSmsNoticeJobDispatchPublisher.class);

		when(depositTradeMapper.selectById("d1")).thenReturn(pendingTrade(), pendingTrade());
		when(depositTradeMapper.update(any(), any())).thenReturn(1);

		AdapayDepositTradeRechargeCallbackService svc =
				new AdapayDepositTradeRechargeCallbackService(depositTradeMapper, mutation, redis, publisher);

		svc.rechargeCallback("d1", "SUCCESS", successOptions());

		verify(mutation).addUserDepositTotal(10L, 20L, 5000L);
	}
}
