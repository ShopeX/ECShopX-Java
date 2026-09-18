package cn.shopex.ecshopx.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcLoginPageSettingSaveRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PcLoginPageSettingServiceTest {

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private ObjectMapper objectMapper;
	private PcLoginPageSettingSaveService saveService;
	private PcLoginPageSettingGetService getService;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		saveService = new PcLoginPageSettingSaveService(companysRedisTemplate, objectMapper);
		getService = new PcLoginPageSettingGetService(companysRedisTemplate, objectMapper);
	}

	@Test
	void savePersistsLogoLightAndLogoDarkLikePhp() throws Exception {
		PcLoginPageSettingSaveRequest body = new PcLoginPageSettingSaveRequest();
		body.setLogoLight("https://example.com/light.jpg");
		body.setLogoDark("https://example.com/dark.png");
		body.setBackground("https://example.com/bg.png");

		saveService.saveLoginPageSetting(38L, body);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(eq("pc_login_page:38"), jsonCaptor.capture());
		Map<String, Object> stored = objectMapper.readValue(jsonCaptor.getValue(), Map.class);
		assertThat(stored.get("logo")).isEqualTo("https://example.com/light.jpg");
		assertThat(stored.get("logo_light")).isEqualTo("https://example.com/light.jpg");
		assertThat(stored.get("logo_dark")).isEqualTo("https://example.com/dark.png");
		assertThat(stored.get("background")).isEqualTo("https://example.com/bg.png");
	}

	@Test
	void getNormalizesLegacyLogoOnlyPayload() throws Exception {
		when(valueOperations.get("pc_login_page:38"))
				.thenReturn("{\"logo\":\"https://example.com/legacy.jpg\",\"background\":\"https://example.com/bg.png\"}");

		Map<String, Object> data = getService.getLoginPageSetting(38L);

		assertThat(data.get("logo")).isEqualTo("https://example.com/legacy.jpg");
		assertThat(data.get("logo_light")).isEqualTo("https://example.com/legacy.jpg");
		assertThat(data.get("logo_dark")).isEqualTo("https://example.com/legacy.jpg");
		assertThat(data.get("background")).isEqualTo("https://example.com/bg.png");
	}

	@Test
	void getReturnsAllFieldsWhenMissingRedisValue() {
		when(valueOperations.get("pc_login_page:38")).thenReturn(null);

		Map<String, Object> data = getService.getLoginPageSetting(38L);

		assertThat(data).containsEntry("logo", "");
		assertThat(data).containsEntry("logo_light", "");
		assertThat(data).containsEntry("logo_dark", "");
		assertThat(data).containsEntry("background", "");
	}
}
