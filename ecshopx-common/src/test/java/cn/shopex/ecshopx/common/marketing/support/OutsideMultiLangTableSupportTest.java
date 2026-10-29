package cn.shopex.ecshopx.common.marketing.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutsideMultiLangTableSupportTest {

	@Test
	void supportedLocalesUseCamelCaseSuffix() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("zh-CN")).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("en-CN")).isEqualTo("enCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("ar-SA")).isEqualTo("arSA");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("zh-TW")).isEqualTo("zhTW");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("ja-JP")).isEqualTo("jaJP");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("ru-RU")).isEqualTo("ruRU");
	}

	@Test
	void caseInsensitiveSupportedTags() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("zh-tw")).isEqualTo("zhTW");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("JA-jp")).isEqualTo("jaJP");
	}

	@Test
	void compactSuffixNormalizedToCamelCase() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("zhtw")).isEqualTo("zhTW");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("jajp")).isEqualTo("jaJP");
	}

	@Test
	void futureLocaleUsesCamelCaseRule() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("ko-KR")).isEqualTo("koKR");
	}

	@Test
	void blankOrJunkFallsBackToZhCn() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix(null)).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("")).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("undefined")).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("bad tag!")).isEqualTo("zhCN");
	}
}
