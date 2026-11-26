package cn.shopex.ecshopx.deposit.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.deposit.domain.UserDeposit;
import cn.shopex.ecshopx.deposit.mapper.UserDepositMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class UserDepositBalanceMutationServiceAtomicTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, UserDeposit.class);
	}

	@Mock
	private UserDepositMapper userDepositMapper;
	@Mock
	private StringRedisTemplate redis;
	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	private UserDepositBalanceMutationService service;

	@BeforeEach
	void setUp() {
		lenient().when(redis.opsForHash()).thenReturn(hashOperations);
		service = new UserDepositBalanceMutationService(userDepositMapper, redis);
	}

	@Test
	@DisplayName("扣减影响行数为 0 时抛余额不足且不写 Redis")
	void debit_whenNoRowAffected_throwsInsufficient() {
		when(userDepositMapper.addMoneyDelta(eq(1L), eq(2L), eq(-50L), eq(50L), any(Integer.class))).thenReturn(0);

		assertThatThrownBy(() -> service.applyUserDepositTotalDelta(1L, 2L, -50L))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("余额不足");

		verify(userDepositMapper, never()).updateById(any(UserDeposit.class));
		verify(hashOperations, never()).put(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("扣减成功走原子 delta 更新并刷新 Redis")
	void debit_whenAffected_usesAtomicDeltaAndRefreshesRedis() {
		when(userDepositMapper.addMoneyDelta(eq(1L), eq(2L), eq(-50L), eq(50L), any(Integer.class))).thenReturn(1);
		UserDeposit after = new UserDeposit();
		after.setMoney(150L);
		when(userDepositMapper.selectOne(any())).thenReturn(after);

		service.applyUserDepositTotalDelta(1L, 2L, -50L);

		verify(userDepositMapper).addMoneyDelta(eq(1L), eq(2L), eq(-50L), eq(50L), any(Integer.class));
		verify(userDepositMapper, never()).updateById(any(UserDeposit.class));
		verify(hashOperations).put("userDepositTotal_1", "2", "150");
	}

	@Test
	@DisplayName("加款无行时插入新行")
	void credit_whenNoExisting_insertsRow() {
		when(userDepositMapper.addMoneyDelta(eq(1L), eq(2L), eq(80L), eq(0L), any(Integer.class))).thenReturn(0);
		when(userDepositMapper.insert(any(UserDeposit.class))).thenReturn(1);

		service.applyUserDepositTotalDelta(1L, 2L, 80L);

		verify(userDepositMapper).insert(any(UserDeposit.class));
		verify(hashOperations).put("userDepositTotal_1", "2", "80");
	}
}
