package cn.shopex.ecshopx.thirdparty.service.saascert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import cn.shopex.ecshopx.thirdparty.domain.ShopBind;
import cn.shopex.ecshopx.thirdparty.mapper.ShopBindMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SaasCertBindShopNodeServiceTest {

	private ShopBindMapper shopBindMapper;
	private StringRedisTemplate prismRedis;
	private StringRedisTemplate companysRedis;
	private ValueOperations<String, String> prismOps;
	private ValueOperations<String, String> companysOps;
	private SaasCertBindShopNodeService service;

	@BeforeEach
	void setUp() {
		shopBindMapper = Mockito.mock(ShopBindMapper.class);
		prismRedis = Mockito.mock(StringRedisTemplate.class);
		companysRedis = Mockito.mock(StringRedisTemplate.class);
		prismOps = Mockito.mock(ValueOperations.class);
		companysOps = Mockito.mock(ValueOperations.class);
		when(prismRedis.opsForValue()).thenReturn(prismOps);
		when(companysRedis.opsForValue()).thenReturn(companysOps);
		CompanyPassportUidByCompanyIdPort passportPort = companyId -> Optional.of("passport-uid");
		service =
				new SaasCertBindShopNodeService(
						shopBindMapper,
						prismRedis,
						companysRedis,
						new ObjectMapper(),
						passportPort,
						"store-key-123");
	}

	@Test
	void bindShopNode_acceptsEmptyNodeTypeLikePhp() {
		when(prismOps.get(any())).thenReturn("{\"cert_id\":\"c1\",\"node_id\":\"n1\",\"token\":\"tok\"}");
		when(shopBindMapper.selectOne(any())).thenReturn(null);

		Map<String, String> params = new LinkedHashMap<>();
		params.put("node_id", "erp-node-empty-type");
		params.put("shop_name", "ERP");
		params.put("status", "bind");
		params.put("certi_ac", sign(params, "tok"));

		assertEquals("succ", service.bindShopNode(1L, params));

		ArgumentCaptor<ShopBind> captor = ArgumentCaptor.forClass(ShopBind.class);
		verify(shopBindMapper).insert(captor.capture());
		assertEquals("", captor.getValue().getNodeType());
	}

	@Test
	void bindShopNode_acceptsSpBcNodeType() {
		when(prismOps.get(any())).thenReturn("{\"cert_id\":\"c1\",\"node_id\":\"n1\",\"token\":\"tok\"}");
		when(shopBindMapper.selectOne(any())).thenReturn(null);

		Map<String, String> params = new LinkedHashMap<>();
		params.put("node_id", "erp-node-1");
		params.put("node_type", "sp.bc");
		params.put("shop_name", "BBC ERP");
		params.put("status", "bind");
		params.put("certi_ac", sign(params, "tok"));

		assertEquals("succ", service.bindShopNode(1L, params));

		ArgumentCaptor<ShopBind> captor = ArgumentCaptor.forClass(ShopBind.class);
		verify(shopBindMapper).insert(captor.capture());
		assertEquals("sp.bc", captor.getValue().getNodeType());
		verify(companysOps).set(SaasCertErpBindReadService.erpBindRedisKey(1L), "erp-node-1");
	}

	@Test
	void bindShopNode_rejectsDuplicateNodeType() {
		when(prismOps.get(any())).thenReturn("{\"cert_id\":\"c1\",\"node_id\":\"n1\",\"token\":\"tok\"}");
		ShopBind existing = new ShopBind();
		existing.setId(9L);
		when(shopBindMapper.selectOne(any())).thenReturn(existing);

		Map<String, String> params = new LinkedHashMap<>();
		params.put("node_id", "erp-node-2");
		params.put("node_type", "sp.bc");
		params.put("shop_name", "BBC ERP");
		params.put("status", "bind");
		params.put("certi_ac", sign(params, "tok"));

		assertEquals("node_type is exists", service.bindShopNode(1L, params));
		verify(shopBindMapper, never()).insert(any(ShopBind.class));
	}

	private static String sign(Map<String, String> params, String token) {
		java.util.TreeMap<String, String> sorted = new java.util.TreeMap<>(params);
		StringBuilder str = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			if ("certi_ac".equals(e.getKey())) {
				continue;
			}
			str.append(e.getValue() == null ? "" : e.getValue());
		}
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] d = md.digest((str + token).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
