package cn.shopex.ecshopx.deposit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class DepositTradeConsumeServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), DepositTrade.class);
	}

	@Test
	void consume_withoutCurPayFee_defaultsCurPayFeeToMoneyFen() {
		DepositTradeMapper depositTradeMapper = mock(DepositTradeMapper.class);
		DepositTradeIdGenerator depositTradeIdGenerator = mock(DepositTradeIdGenerator.class);
		SensitiveFieldEncryptor sensitiveFieldEncryptor = mock(SensitiveFieldEncryptor.class);
		UserDepositBalanceReadService balanceRead = mock(UserDepositBalanceReadService.class);
		UserDepositBalanceMutationService balanceMutation = mock(UserDepositBalanceMutationService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

		when(depositTradeIdGenerator.nextDepositTradeId(2L)).thenReturn("CZtest001");
		when(sensitiveFieldEncryptor.encrypt("15901872216")).thenReturn("15901872216");
		when(balanceRead.getUserDepositTotal(1L, 2L)).thenReturn(10_000L);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(any(), any(), any(Long.class))).thenReturn(1L);

		DepositTradeConsumeService service =
				new DepositTradeConsumeService(
						depositTradeMapper,
						depositTradeIdGenerator,
						sensitiveFieldEncryptor,
						balanceRead,
						balanceMutation,
						redis);

		Map<String, Object> consumeData = new LinkedHashMap<>();
		consumeData.put("company_id", 1L);
		consumeData.put("user_id", 2L);
		consumeData.put("money", 102L);
		consumeData.put("member_card_code", "M17803858093052554");
		consumeData.put("mobile", "15901872216");
		consumeData.put("detail", "测试商品");

		service.consume(consumeData);

		ArgumentCaptor<DepositTrade> captor = ArgumentCaptor.forClass(DepositTrade.class);
		verify(depositTradeMapper).insert(captor.capture());
		assertThat(captor.getValue().getCurPayFee()).isEqualTo("102");
		verify(balanceMutation).applyUserDepositTotalDelta(eq(1L), eq(2L), eq(-102L));
	}

	@Test
	void consume_withExplicitCurPayFee_preservesProvidedValue() {
		DepositTradeMapper depositTradeMapper = mock(DepositTradeMapper.class);
		DepositTradeIdGenerator depositTradeIdGenerator = mock(DepositTradeIdGenerator.class);
		SensitiveFieldEncryptor sensitiveFieldEncryptor = mock(SensitiveFieldEncryptor.class);
		UserDepositBalanceReadService balanceRead = mock(UserDepositBalanceReadService.class);
		UserDepositBalanceMutationService balanceMutation = mock(UserDepositBalanceMutationService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

		when(depositTradeIdGenerator.nextDepositTradeId(2L)).thenReturn("CZtest002");
		when(sensitiveFieldEncryptor.encrypt("")).thenReturn("");
		when(balanceRead.getUserDepositTotal(1L, 2L)).thenReturn(10_000L);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(any(), any(), any(Long.class))).thenReturn(1L);

		DepositTradeConsumeService service =
				new DepositTradeConsumeService(
						depositTradeMapper,
						depositTradeIdGenerator,
						sensitiveFieldEncryptor,
						balanceRead,
						balanceMutation,
						redis);

		Map<String, Object> consumeData = new LinkedHashMap<>();
		consumeData.put("company_id", 1L);
		consumeData.put("user_id", 2L);
		consumeData.put("money", 500L);
		consumeData.put("cur_pay_fee", "480");

		service.consume(consumeData);

		ArgumentCaptor<DepositTrade> captor = ArgumentCaptor.forClass(DepositTrade.class);
		verify(depositTradeMapper).insert(captor.capture());
		assertThat(captor.getValue().getCurPayFee()).isEqualTo("480");
	}
}
