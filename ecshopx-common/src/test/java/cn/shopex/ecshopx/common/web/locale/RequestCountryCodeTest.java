package cn.shopex.ecshopx.common.web.locale;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.config.LangueProperties;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class RequestCountryCodeTest {

	@AfterEach
	void reset() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void prefersBodyOverQueryAndAcceptLanguage() {
		LangueProperties props = new LangueProperties();
		LocaleContextHolder.setLocale(Locale.forLanguageTag("zh-CN"), true);
		String tag = RequestCountryCode.resolve(props, "zh-CN", Map.of("country_code", "zh-TW"));
		assertThat(tag).isEqualTo("zh-TW");
	}

	@Test
	void prefersEarlierMapOverLater() {
		LangueProperties props = new LangueProperties();
		String tag = RequestCountryCode.resolve(
				props, null, Map.of("country_code", "en-CN"), Map.of("country_code", "zh-TW"));
		assertThat(tag).isEqualTo("en-CN");
	}

	@Test
	void usesQueryWhenBodyAbsent() {
		LangueProperties props = new LangueProperties();
		LocaleContextHolder.setLocale(Locale.forLanguageTag("zh-CN"), true);
		String tag = RequestCountryCode.resolve(props, "zh-TW", Map.of("item_name", "x"));
		assertThat(tag).isEqualTo("zh-TW");
	}

	@Test
	void fallsBackToRequestLangTag() {
		LangueProperties props = new LangueProperties();
		LocaleContextHolder.setLocale(Locale.forLanguageTag("en-CN"), true);
		assertThat(RequestCountryCode.resolve(props, Map.of())).isEqualTo("en-CN");
	}
}
