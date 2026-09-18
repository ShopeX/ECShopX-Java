package cn.shopex.ecshopx.common.marketing.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutsideMultiLangTableSupportTest {

	@Test
	void legacyLocalesKeepCamelCaseSuffix() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("zh-CN")).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("en-CN")).isEqualTo("enCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("ar-SA")).isEqualTo("arSA");
	}

	@Test
	void zhTwBecomesLowercaseZhtw() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("zh-TW")).isEqualTo("zhtw");
	}

	@Test
	void futureLocaleUsesLowercaseRule() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("ja-JP")).isEqualTo("jajp");
	}

	@Test
	void blankOrJunkFallsBackToZhCn() {
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix(null)).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("")).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("undefined")).isEqualTo("zhCN");
		assertThat(OutsideMultiLangTableSupport.normalizeLangTableSuffix("bad tag!")).isEqualTo("zhCN");
	}
}
