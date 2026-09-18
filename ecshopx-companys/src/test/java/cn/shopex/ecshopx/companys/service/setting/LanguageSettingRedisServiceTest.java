package cn.shopex.ecshopx.companys.service.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class LanguageSettingRedisServiceTest {

	private static final long COMPANY_ID = 1001L;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private MessageSource messageSource;

	private LanguageSettingRedisService service;

	@BeforeEach
	void setUp() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		lenient()
				.when(messageSource.getMessage(anyString(), eq(null), anyString(), any()))
				.thenAnswer(inv -> inv.getArgument(2));
		LangueProperties langueProperties = new LangueProperties();
		langueProperties.setList(List.of("zh-CN", "en-CN", "ar-SA", "zh-TW"));
		langueProperties.setDefaultLang("zh-CN");
		service =
				new LanguageSettingRedisService(
						companysRedisTemplate, new ObjectMapper(), langueProperties, messageSource);
	}

	@Test
	@DisplayName("无 Redis 键时 GET 缺省仅中文启用 version=0")
	void defaultAdminSettingWhenMissingKey() {
		when(valueOperations.get("languageSetting:1001")).thenReturn(null);

		Map<String, Object> data = service.getAdminSetting(COMPANY_ID);

		assertThat(data.get("version")).isEqualTo(0);
		assertThat(data.get("defaultLanguage")).isEqualTo("zh-CN");
		assertThat(data.get("enabledCount")).isEqualTo(1);
		assertThat(data.get("configValid")).isEqualTo(true);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> languages = (List<Map<String, Object>>) data.get("languages");
		assertThat(languages).hasSize(4);
		assertThat(languages.get(0).get("enabled")).isEqualTo(true);
		assertThat(languages.get(1).get("enabled")).isEqualTo(false);
	}

	@Test
	@DisplayName("首次保存写入 version=1")
	void firstSaveWritesVersionOne() {
		when(valueOperations.get("languageSetting:1001")).thenReturn(null);

		Map<String, Object> body = validSaveBody(null, "zh-CN", true, true, false, false);
		Map<String, Object> out = service.saveSetting(COMPANY_ID, body);

		assertThat(out.get("version")).isEqualTo(1);
		assertThat(out.get("enabledCount")).isEqualTo(2L);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(eq("languageSetting:1001"), jsonCaptor.capture());
		assertThat(jsonCaptor.getValue()).contains("\"version\":1");
	}

	@Test
	@DisplayName("全关语言保存拒绝")
	void rejectAllDisabled() {
		when(valueOperations.get("languageSetting:1001")).thenReturn(null);

		Map<String, Object> body = validSaveBody(null, "zh-CN", false, false, false, false);

		assertThatThrownBy(() -> service.saveSetting(COMPANY_ID, body))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("至少开启一种语言");
	}

	@Test
	@DisplayName("version 冲突拒绝保存")
	void rejectVersionConflict() throws Exception {
		Map<String, Object> stored = new LinkedHashMap<>();
		stored.put("version", 2);
		stored.put("defaultLanguage", "zh-CN");
		stored.put(
				"languages",
				List.of(
						Map.of("code", "zh-CN", "enabled", true, "sort", 1),
						Map.of("code", "en-CN", "enabled", false, "sort", 2),
						Map.of("code", "ar-SA", "enabled", false, "sort", 3),
						Map.of("code", "zh-TW", "enabled", false, "sort", 4)));
		when(valueOperations.get("languageSetting:1001"))
				.thenReturn(new ObjectMapper().writeValueAsString(stored));

		Map<String, Object> body = validSaveBody(1, "zh-CN", true, false, false, false);

		assertThatThrownBy(() -> service.saveSetting(COMPANY_ID, body))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("配置已被他人修改");
	}

	@Test
	@DisplayName("resolveEffectiveTag 对已关闭语言 clamp 到默认")
	void clampDisabledLanguage() throws Exception {
		Map<String, Object> stored = new LinkedHashMap<>();
		stored.put("version", 1);
		stored.put("defaultLanguage", "zh-CN");
		stored.put(
				"languages",
				List.of(
						Map.of("code", "zh-CN", "enabled", true, "sort", 1),
						Map.of("code", "en-CN", "enabled", false, "sort", 2),
						Map.of("code", "ar-SA", "enabled", false, "sort", 3),
						Map.of("code", "zh-TW", "enabled", false, "sort", 4)));
		when(valueOperations.get("languageSetting:1001"))
				.thenReturn(new ObjectMapper().writeValueAsString(stored));

		assertThat(service.resolveEffectiveTag(COMPANY_ID, "en-CN")).isEqualTo("zh-CN");
		assertThat(service.resolveEffectiveTag(COMPANY_ID, "en-US")).isEqualTo("zh-CN");
	}

	@Test
	@DisplayName("切换 GET 仅返回 enabled 语言")
	void switchSettingOnlyEnabled() throws Exception {
		Map<String, Object> stored = new LinkedHashMap<>();
		stored.put("version", 1);
		stored.put("defaultLanguage", "zh-CN");
		stored.put("updatedAt", 1725123456789L);
		stored.put(
				"languages",
				List.of(
						Map.of("code", "zh-CN", "enabled", true, "sort", 1),
						Map.of("code", "en-CN", "enabled", true, "sort", 2),
						Map.of("code", "ar-SA", "enabled", false, "sort", 3),
					 Map.of("code", "zh-TW", "enabled", false, "sort", 4)));
		when(valueOperations.get("languageSetting:1001"))
				.thenReturn(new ObjectMapper().writeValueAsString(stored));

		Map<String, Object> data = service.getSwitchSetting(COMPANY_ID);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> enabled = (List<Map<String, Object>>) data.get("enabledLanguages");
		assertThat(enabled).hasSize(2);
		assertThat(enabled.get(0).get("code")).isEqualTo("zh-CN");
		assertThat(enabled.get(1).get("code")).isEqualTo("en-CN");
	}

	private static Map<String, Object> validSaveBody(
			Integer version,
			String defaultLanguage,
			boolean zhEnabled,
			boolean enEnabled,
			boolean arEnabled,
			boolean twEnabled) {
		Map<String, Object> body = new LinkedHashMap<>();
		if (version != null) {
			body.put("version", version);
		}
		body.put("defaultLanguage", defaultLanguage);
		List<Map<String, Object>> languages = new ArrayList<>();
		languages.add(langRow("zh-CN", zhEnabled));
		languages.add(langRow("en-CN", enEnabled));
		languages.add(langRow("ar-SA", arEnabled));
		languages.add(langRow("zh-TW", twEnabled));
		body.put("languages", languages);
		return body;
	}

	private static Map<String, Object> langRow(String code, boolean enabled) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("code", code);
		row.put("enabled", enabled);
		return row;
	}
}
