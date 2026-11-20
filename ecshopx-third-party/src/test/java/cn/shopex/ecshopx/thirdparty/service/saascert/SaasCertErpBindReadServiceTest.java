package cn.shopex.ecshopx.thirdparty.service.saascert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.thirdparty.domain.ShopBind;
import cn.shopex.ecshopx.thirdparty.mapper.ShopBindMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SaasCertErpBindReadServiceTest {

	private StringRedisTemplate companysRedis;
	private ValueOperations<String, String> companysOps;
	private ShopBindMapper shopBindMapper;
	private SaasCertErpBindReadService service;

	@BeforeEach
	void setUp() {
		companysRedis = Mockito.mock(StringRedisTemplate.class);
		companysOps = Mockito.mock(ValueOperations.class);
		shopBindMapper = Mockito.mock(ShopBindMapper.class);
		when(companysRedis.opsForValue()).thenReturn(companysOps);
		service = new SaasCertErpBindReadService(companysRedis, shopBindMapper);
	}

	@Test
	void resolve_usesRedisWhenPresent() {
		String key = SaasCertErpBindReadService.erpBindRedisKey(38L);
		when(companysOps.get(key)).thenReturn("1880110232");

		Optional<String> node = service.resolveErpBindNodeId(38L);

		assertEquals(Optional.of("1880110232"), node);
		assertTrue(service.isErpBound(38L));
		verify(shopBindMapper, never()).selectOne(any());
		verify(companysOps, never()).set(any(), any());
	}

	@Test
	void resolve_backfillsFromDbWhenRedisMissing() {
		String key = SaasCertErpBindReadService.erpBindRedisKey(38L);
		when(companysOps.get(key)).thenReturn(null);
		ShopBind row = new ShopBind();
		row.setNodeId("1880110232");
		row.setNodeType("sp.bc");
		row.setStatus(1);
		when(shopBindMapper.selectOne(any())).thenReturn(row);

		Optional<String> node = service.resolveErpBindNodeId(38L);

		assertEquals(Optional.of("1880110232"), node);
		verify(companysOps).set(eq(key), eq("1880110232"));
		assertTrue(service.isErpBound(38L));
	}

	@Test
	void resolve_emptyWhenRedisAndDbMissing() {
		String key = SaasCertErpBindReadService.erpBindRedisKey(38L);
		when(companysOps.get(key)).thenReturn("");
		when(shopBindMapper.selectOne(any())).thenReturn(null);

		assertEquals(Optional.empty(), service.resolveErpBindNodeId(38L));
		assertFalse(service.isErpBound(38L));
		verify(companysOps, never()).set(any(), any());
	}
}
