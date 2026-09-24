package cn.shopex.ecshopx.selfservice.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

class RegistrationActivityFrontRequestLocaleTest {

	private static final String MOBILE_KEY = "selfservice.front.registration_submit.please_enter_correct_mobile";

	private static ReloadableResourceBundleMessageSource messages;

	private final LangueProperties langueProperties = new LangueProperties();

	@BeforeAll
	static void loadRegistrationMessages() {
		Path basename = Path.of("..", "ecshopx-distribution", "src", "main", "resources", "messages");
		if (!Files.exists(basename.getParent().resolve("messages_zh_CN.properties"))) {
			basename = Path.of("ecshopx-distribution", "src", "main", "resources", "messages");
		}
		messages = new ReloadableResourceBundleMessageSource();
		messages.setBasename(basename.toAbsolutePath().normalize().toUri().toString());
		messages.setDefaultEncoding("UTF-8");
		messages.setFallbackToSystemLocale(false);
	}

	@Test
	@DisplayName("country_code=zh-CN 时手机号错误文案为中文，不跟 Servlet 默认英文")
	void zhCnCountryCode_usesChineseMobileMessage() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("country_code")).thenReturn("zh-CN");
		when(request.getLocale()).thenReturn(Locale.US);

		Locale locale = RegistrationActivityFrontRequestLocale.messageLocale(langueProperties, request, null, "en-CN");

		assertThat(locale.toLanguageTag()).isEqualTo("zh-CN");
		assertThat(messages.getMessage(MOBILE_KEY, null, locale)).isEqualTo("请填写正确的手机号");
	}

	@Test
	@DisplayName("country_code=en-CN 时手机号错误文案为英文，不回落到默认中文")
	void enCnCountryCode_usesEnglishMobileMessage() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("country_code")).thenReturn(null);
		Map<String, Object> body = Map.of("country_code", "en-CN");

		Locale locale = RegistrationActivityFrontRequestLocale.messageLocale(langueProperties, request, body, "zh-CN");

		assertThat(messages.getMessage(MOBILE_KEY, null, locale)).isEqualTo("Please enter a valid mobile number");
	}

	@Test
	@DisplayName("未传 country_code 时用后管配置的公司默认语言")
	void blankCountryCode_usesCompanyDefault() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("country_code")).thenReturn("  ");
		when(request.getLocale()).thenReturn(Locale.US);

		assertThat(RegistrationActivityFrontRequestLocale.langTag(langueProperties, request, null, "en-CN"))
				.isEqualTo("en-CN");
	}
}
