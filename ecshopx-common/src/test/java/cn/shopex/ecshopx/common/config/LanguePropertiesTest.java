package cn.shopex.ecshopx.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class LanguePropertiesTest {

	@Configuration
	@EnableConfigurationProperties(LangueProperties.class)
	static class LanguePropertiesOnlyConfiguration {}

	@Test
	void resolveToSupportedTag_zhTwIsSupported() {
		LangueProperties p = new LangueProperties();
		assertThat(p.resolveToSupportedTag("zh-TW")).isEqualTo("zh-TW");
		assertThat(p.resolveToSupportedTag("zh-tw")).isEqualTo("zh-TW");
	}

	@Test
	void resolveToSupportedTag_blankFallsBackToDefault() {
		LangueProperties p = new LangueProperties();
		assertThat(p.resolveToSupportedTag(null)).isEqualTo("zh-CN");
		assertThat(p.resolveToSupportedTag("")).isEqualTo("zh-CN");
		assertThat(p.resolveToSupportedTag("   ")).isEqualTo("zh-CN");
	}

	@Test
	void resolveToSupportedTag_caseInsensitiveMatchReturnsCanonical() {
		LangueProperties p = new LangueProperties();
		assertThat(p.resolveToSupportedTag("zh-cn")).isEqualTo("zh-CN");
		assertThat(p.resolveToSupportedTag("EN-cn")).isEqualTo("en-CN");
	}

	@Test
	void resolveToSupportedTag_unknownFallsBackToDefault() {
		LangueProperties p = new LangueProperties();
		assertThat(p.resolveToSupportedTag("fr-FR")).isEqualTo("zh-CN");
		assertThat(p.resolveToSupportedTag("xx")).isEqualTo("zh-CN");
	}

	@Test
	void defaultMustBeInListBeanCreationFails() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withUserConfiguration(LanguePropertiesOnlyConfiguration.class)
				.withPropertyValues(
						"langue.default-lang=not-in-list",
						"langue.list[0]=zh-CN",
						"langue.list[1]=en-CN",
						"langue.list[2]=ar-SA");

		runner.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void defaultInListBeanCreationSucceeds() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withUserConfiguration(LanguePropertiesOnlyConfiguration.class)
				.withPropertyValues(
						"langue.default-lang=zh-CN",
						"langue.list[0]=zh-CN",
						"langue.list[1]=en-CN",
						"langue.list[2]=ar-SA");

		runner.run(context -> assertThat(context).hasNotFailed());
	}
}
