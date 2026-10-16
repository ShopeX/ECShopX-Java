package cn.shopex.ecshopx.common.companys.language;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LanguageCanonicalCodesTest {

	@Test
	@DisplayName("别名 en-US / ar 归一化为 en-CN / ar-SA")
	void normalizeAliases() {
		assertThat(LanguageCanonicalCodes.normalize("en-US")).isEqualTo("en-CN");
		assertThat(LanguageCanonicalCodes.normalize("ar")).isEqualTo("ar-SA");
		assertThat(LanguageCanonicalCodes.normalize("zh-cn")).isEqualTo("zh-CN");
	}

	@Test
	@DisplayName("固定六种顺序与展示名")
	void fixedOrderAndDisplayNames() {
		assertThat(LanguageCanonicalCodes.FIXED_ORDER)
				.containsExactly("zh-CN", "en-CN", "ar-SA", "zh-TW", "ja-JP", "ru-RU");
		assertThat(LanguageCanonicalCodes.displayName("en-CN")).isEqualTo("English");
		assertThat(LanguageCanonicalCodes.displayName("ja-JP")).isEqualTo("日本語");
		assertThat(LanguageCanonicalCodes.sortOf("zh-TW")).isEqualTo(4);
		assertThat(LanguageCanonicalCodes.sortOf("ru-RU")).isEqualTo(6);
	}
}
