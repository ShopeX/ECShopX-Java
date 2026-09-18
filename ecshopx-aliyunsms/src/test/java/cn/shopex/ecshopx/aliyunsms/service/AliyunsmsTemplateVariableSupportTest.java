package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AliyunsmsTemplateVariableSupportTest {

	private static final String VARIABLES_JSON =
			"[{\"var_name\":\"code\",\"var_title\":\"验证码\",\"rule\":\"numberCaptcha\"}]";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("filterTemplateParam accepts Aliyun-sync placeholder ${code}")
	void filterTemplateParam_codePlaceholder() {
		String content = "验证码：${code}，30分钟内有效，如非本人操作请忽略！";
		Map<String, String> data = Map.of("code", "123456");

		Map<String, String> filtered =
				AliyunsmsTemplateVariableSupport.filterTemplateParam(
						content, data, VARIABLES_JSON, objectMapper);

		assertThat(filtered).containsEntry("code", "123456");
	}

	@Test
	@DisplayName("filterTemplateParam accepts admin placeholder ${验证码}")
	void filterTemplateParam_titlePlaceholder() {
		String content = "验证码：${验证码}，30分钟内有效，如非本人操作请忽略！";
		Map<String, String> data = Map.of("code", "654321");

		Map<String, String> filtered =
				AliyunsmsTemplateVariableSupport.filterTemplateParam(
						content, data, VARIABLES_JSON, objectMapper);

		assertThat(filtered).containsEntry("code", "654321");
	}

	@Test
	@DisplayName("allowedPlaceholderKeys includes var_title and var_name")
	void allowedPlaceholderKeys_bothKeys() {
		List<Map<String, Object>> varDefs =
				AliyunsmsTemplateVariableSupport.decodeVariables(VARIABLES_JSON, objectMapper);
		Set<String> allowed = AliyunsmsTemplateVariableSupport.allowedPlaceholderKeys(varDefs);

		assertThat(allowed).containsExactlyInAnyOrder("验证码", "code");
	}

	@Test
	@DisplayName("compileTemplateDisplay replaces ${code} with data value")
	void compileTemplateDisplay_codePlaceholder() {
		String content = "验证码：${code}，30分钟内有效";
		Map<String, String> data = new LinkedHashMap<>();
		data.put("code", "888888");

		String compiled =
				AliyunsmsTemplateVariableSupport.compileTemplateDisplay(
						content, data, VARIABLES_JSON, objectMapper);

		assertThat(compiled).isEqualTo("验证码：888888，30分钟内有效");
	}
}
