package cn.shopex.ecshopx.companys.service.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class McpSettingRedisServiceTest {

	private StringRedisTemplate redis;

	private ValueOperations<String, String> values;

	private McpSettingRedisService service;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		redis = mock(StringRedisTemplate.class);
		values = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(values);
		service = new McpSettingRedisService(redis, new ObjectMapper(), "https://admin.example.com", "/api/mcp");
	}

	@Test
	void getSettingDefaultsDisabledAndBuildsUrl() {
		when(values.get("mcpSetting:7")).thenReturn(null);
		Map<String, Object> data = service.getSetting(7L, "http://ignored");
		assertFalse((Boolean) data.get("enabled"));
		assertEquals("https://admin.example.com/api/mcp", data.get("mcp_url"));
		assertEquals("ecshopx-admin", data.get("server_name"));
	}

	@Test
	void saveSettingPersistsEnabled() {
		when(values.get("mcpSetting:7")).thenReturn("{\"enabled\":true}");
		Map<String, Object> data = service.saveSetting(7L, Map.of("enabled", true), "http://ignored");
		assertTrue((Boolean) data.get("enabled"));
		verify(values).set(eq("mcpSetting:7"), anyString());
	}
}
