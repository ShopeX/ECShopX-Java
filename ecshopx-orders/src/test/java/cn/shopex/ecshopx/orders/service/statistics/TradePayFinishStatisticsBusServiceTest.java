package cn.shopex.ecshopx.orders.service.statistics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class TradePayFinishStatisticsBusServiceTest {

	@Mock
	private StringRedisTemplate companysRedis;

	@Mock
	private HashOperations<String, Object, Object> hashOps;

	@Mock
	private SetOperations<String, String> setOps;

	private TradePayFinishStatisticsBusService service;

	@BeforeEach
	void setUp() {
		when(companysRedis.opsForHash()).thenReturn(hashOps);
		when(companysRedis.opsForSet()).thenReturn(setOps);
		when(hashOps.increment(anyString(), anyString(), anyLong())).thenReturn(1L);
		when(setOps.add(anyString(), any())).thenReturn(1L);
		service = new TradePayFinishStatisticsBusService(companysRedis);
	}

	@Test
	void recordPayFinishStatistics_whenSuccess_incrementsOrderPayStatisticsHashAndCompanyIdsSet() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 9L);
		row.put("order_id", 501L);
		row.put("trade_source_type", "normal");
		row.put("trade_state", "SUCCESS");
		row.put("total_fee", 200L);
		row.put("user_id", "7");

		service.recordPayFinishStatistics(row);

		ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
		verify(setOps).add(keys.capture(), eq("9"));
		String companyIdsKey = keys.getValue();
		Assertions.assertTrue(companyIdsKey.startsWith("companyIds:"));

		verify(hashOps).increment(anyString(), eq("orderPayFee"), eq(200L));
		verify(hashOps).increment(anyString(), eq("orderPayNum"), eq(1L));
		verify(setOps).add(anyString(), eq("7"));
	}
}
