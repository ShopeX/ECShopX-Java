package cn.shopex.ecshopx.common.web.locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestMessageLocaleTest {

	private final LangueProperties langueProperties = new LangueProperties();

	@Test
	@DisplayName("传了 country_code 时用该语种，不看后管默认语言")
	void countryCodeWinsOverCompanyDefault() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("country_code")).thenReturn("zh-CN");
		when(request.getLocale()).thenReturn(Locale.US);

		String tag = RequestMessageLocale.langTag(langueProperties, request, null, "en-CN");

		assertThat(tag).isEqualTo("zh-CN");
		assertThat(RequestMessageLocale.toMessageLocale(tag).toLanguageTag()).isEqualTo("zh-CN");
	}

	@Test
	@DisplayName("body 里的 en-CN 走英文文案包")
	void enCnUsesEnglishMessageLocale() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("country_code")).thenReturn(null);

		Locale locale =
				RequestMessageLocale.messageLocale(langueProperties, request, Map.of("country_code", "en-CN"), "zh-CN");

		assertThat(locale.toLanguageTag()).isEqualTo("en-US");
	}

	@Test
	@DisplayName("未传 country_code 时用后管配置的公司默认语言")
	void blankCountryCodeUsesCompanyDefault() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("country_code")).thenReturn("  ");
		when(request.getLocale()).thenReturn(Locale.US);

		assertThat(RequestMessageLocale.langTag(langueProperties, request, null, "en-CN")).isEqualTo("en-CN");
	}
}
