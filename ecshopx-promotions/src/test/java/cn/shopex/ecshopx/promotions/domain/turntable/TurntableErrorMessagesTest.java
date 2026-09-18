package cn.shopex.ecshopx.promotions.domain.turntable;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class TurntableErrorMessagesTest {

	@Test
	void resolvesZhCnFallbackWhenKeyMissingFromBundle() {
		StaticMessageSource source = new StaticMessageSource();
		String text =
				TurntableErrorMessages.message(
						source, TurntableErrorCodes.ACTIVITY_NOT_FOUND, java.util.Locale.SIMPLIFIED_CHINESE);
		assertThat(text).isEqualTo("错误，活动不存在或id错误");
	}

	@Test
	void mapsAllStableCodesToI18nKeys() {
		StaticMessageSource source = new StaticMessageSource();
		source.addMessage(
				"promotions.turntable.grant_failed",
				java.util.Locale.US,
				"Prize grant failed, please try again later");
		String text =
				TurntableErrorMessages.message(source, TurntableErrorCodes.GRANT_FAILED, java.util.Locale.US);
		assertThat(text).isEqualTo("Prize grant failed, please try again later");
	}
}
