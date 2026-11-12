package cn.shopex.ecshopx.companys.service.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.ProtocolUpdateLog;
import cn.shopex.ecshopx.companys.mapper.ProtocolUpdateLogMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ShopProtocolSetServiceGetFallbackTest {

	private static final long COMPANY_ID = 38L;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@Mock
	private ProtocolUpdateLogMapper protocolUpdateLogMapper;

	private ShopProtocolSetService service;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		when(companysRedisTemplate.opsForHash()).thenReturn(hashOperations);
		service =
				new ShopProtocolSetService(companysRedisTemplate, objectMapper, protocolUpdateLogMapper);
	}

	@Test
	@DisplayName("Redis 空且 DB 有最新协议时回源并回写 Redis")
	void getFallsBackToDbAndRepopulatesRedisWhenHashEmpty() throws Exception {
		String hashKey = resolveHashKey(COMPANY_ID, "zh-CN");
		when(hashOperations.entries(hashKey)).thenReturn(Collections.emptyMap());

		ProtocolUpdateLog privacy = new ProtocolUpdateLog();
		privacy.setCompanyId(COMPANY_ID);
		privacy.setType("privacy");
		privacy.setContent("<p>隐私正文</p>");
		privacy.setDigest("digest-privacy");
		privacy.setCreated(1700000001);
		privacy.setUpdated(1700000002);

		when(protocolUpdateLogMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(privacy));
		when(hashOperations.get(eq(hashKey), eq("privacy_draft"))).thenReturn(null);

		Map<String, Object> data = service.get(COMPANY_ID, null, "zh-CN");

		assertThat(data).containsKey("privacy");
		@SuppressWarnings("unchecked")
		Map<String, Object> privacyInfo = (Map<String, Object>) data.get("privacy");
		assertThat(privacyInfo.get("content")).isEqualTo("<p>隐私正文</p>");
		assertThat(privacyInfo.get("digest")).isEqualTo("digest-privacy");
		assertThat(privacyInfo.get("title")).isEqualTo("隐私政策");
		assertThat(privacyInfo.get("update_time")).isEqualTo(1700000002);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(hashOperations).put(eq(hashKey), eq("privacy"), jsonCaptor.capture());
		assertThat(jsonCaptor.getValue()).contains("隐私正文");
		assertThat(jsonCaptor.getValue()).contains("digest-privacy");
	}

	@Test
	@DisplayName("Redis 有数据时不查 DB")
	void getUsesRedisWithoutDbWhenHashPresent() throws Exception {
		String hashKey = resolveHashKey(COMPANY_ID, "zh-CN");
		String redisJson =
				objectMapper.writeValueAsString(
						Map.of(
								"type",
								"privacy",
								"title",
								"隐私政策",
								"content",
								"<p>来自Redis</p>",
								"digest",
								"from-redis",
								"update_date",
								"",
								"take_effect_date",
								"",
								"update_time",
								1700000100));
		when(hashOperations.entries(hashKey)).thenReturn(Map.of("privacy", redisJson));
		when(hashOperations.get(eq(hashKey), eq("privacy_draft"))).thenReturn(null);

		Map<String, Object> data = service.get(COMPANY_ID, null, "zh-CN");

		@SuppressWarnings("unchecked")
		Map<String, Object> privacyInfo = (Map<String, Object>) data.get("privacy");
		assertThat(privacyInfo.get("content")).isEqualTo("<p>来自Redis</p>");
		verify(protocolUpdateLogMapper, never()).selectList(any());
		verify(protocolUpdateLogMapper, never()).selectOne(any());
		verify(hashOperations, never()).put(any(), any(), any());
	}

	@Test
	@DisplayName("Redis 空且 DB 无数据时不回写 Redis")
	void getDoesNotWriteRedisWhenDbAlsoEmpty() {
		String hashKey = resolveHashKey(COMPANY_ID, "zh-CN");
		when(hashOperations.entries(hashKey)).thenReturn(Collections.emptyMap());
		when(protocolUpdateLogMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

		Map<String, Object> data = service.get(COMPANY_ID, null, "zh-CN");

		assertThat(data).isEmpty();
		verify(hashOperations, never()).put(any(), any(), any());
	}

	@Test
	@DisplayName("单类型 Redis 空时按 type 回源 DB 并回写")
	void getSingleTypeFallsBackToDbWhenFieldMissing() throws Exception {
		String hashKey = resolveHashKey(COMPANY_ID, "zh-CN");
		when(hashOperations.get(eq(hashKey), eq("member_register"))).thenReturn(null);

		ProtocolUpdateLog log = new ProtocolUpdateLog();
		log.setCompanyId(COMPANY_ID);
		log.setType("member_register");
		log.setContent("<p>注册协议</p>");
		log.setDigest("digest-reg");
		log.setCreated(1700000003);
		log.setUpdated(1700000004);
		when(protocolUpdateLogMapper.selectOne(any(Wrapper.class))).thenReturn(log);
		when(hashOperations.get(eq(hashKey), eq("member_register_draft"))).thenReturn(null);

		Map<String, Object> data = service.get(COMPANY_ID, "member_register", "zh-CN");

		@SuppressWarnings("unchecked")
		Map<String, Object> info = (Map<String, Object>) data.get("member_register");
		assertThat(info.get("content")).isEqualTo("<p>注册协议</p>");
		assertThat(info.get("title")).isEqualTo("注册协议");

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(hashOperations).put(eq(hashKey), eq("member_register"), jsonCaptor.capture());
		assertThat(jsonCaptor.getValue()).contains("注册协议");
	}

	private static String resolveHashKey(long companyId, String countryCode) {
		String lang = countryCode.toLowerCase().replace("-", "");
		String cacheName = "zhcn".equals(lang) ? "companyProtocol" : ("companyProtocol_" + lang);
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(String.valueOf(companyId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return "hash:" + cacheName + ":" + java.util.HexFormat.of().formatHex(digest);
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
