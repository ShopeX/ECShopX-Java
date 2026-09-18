package cn.shopex.ecshopx.common.web.locale;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.config.LangueProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CountryCodeLocaleFilterTest {

	@AfterEach
	void reset() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void setsLocaleFromCountryCodeAndResetsAfterChain() throws ServletException, IOException {
		LangueProperties props = new LangueProperties();
		CountryCodeLocaleFilter filter = new CountryCodeLocaleFilter(props);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("country_code", "en-CN");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Locale> during = new AtomicReference<>();
		FilterChain chain = (req, res) -> during.set(LocaleContextHolder.getLocale());
		filter.doFilter(request, response, chain);
		assertThat(during.get()).isEqualTo(Locale.forLanguageTag("en-CN"));
		assertThat(LocaleContextHolder.getLocaleContext()).isNull();
	}

	@Test
	void invalidCountryCodeFallsBackToDefaultLocale() throws ServletException, IOException {
		LangueProperties props = new LangueProperties();
		CountryCodeLocaleFilter filter = new CountryCodeLocaleFilter(props);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("country_code", "invalid-lang");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Locale> during = new AtomicReference<>();
		FilterChain chain = (req, res) -> during.set(LocaleContextHolder.getLocale());
		filter.doFilter(request, response, chain);
		assertThat(during.get()).isEqualTo(Locale.forLanguageTag("zh-CN"));
	}

	@Test
	void fallsBackToAcceptLanguageWhenCountryCodeAbsent() throws ServletException, IOException {
		LangueProperties props = new LangueProperties();
		CountryCodeLocaleFilter filter = new CountryCodeLocaleFilter(props);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Language", "en-CN,en;q=0.9");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Locale> during = new AtomicReference<>();
		FilterChain chain = (req, res) -> during.set(LocaleContextHolder.getLocale());
		filter.doFilter(request, response, chain);
		assertThat(during.get()).isEqualTo(Locale.forLanguageTag("en-CN"));
	}

	@Test
	void countryCodeTakesPrecedenceOverAcceptLanguage() throws ServletException, IOException {
		LangueProperties props = new LangueProperties();
		CountryCodeLocaleFilter filter = new CountryCodeLocaleFilter(props);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("country_code", "zh-CN");
		request.addHeader("Accept-Language", "en-CN");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Locale> during = new AtomicReference<>();
		FilterChain chain = (req, res) -> during.set(LocaleContextHolder.getLocale());
		filter.doFilter(request, response, chain);
		assertThat(during.get()).isEqualTo(Locale.forLanguageTag("zh-CN"));
	}

	@Test
	void firstAcceptLanguageTagParsesQualityValues() {
		assertThat(CountryCodeLocaleFilter.firstAcceptLanguageTag("zh-CN,zh;q=0.9"))
				.isEqualTo("zh-CN");
		assertThat(CountryCodeLocaleFilter.firstAcceptLanguageTag("en-CN;q=0.8")).isEqualTo("en-CN");
		assertThat(CountryCodeLocaleFilter.firstAcceptLanguageTag(null)).isNull();
		assertThat(CountryCodeLocaleFilter.firstAcceptLanguageTag("")).isNull();
	}
}
