package cn.shopex.ecshopx.promotions.domain.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberSigninDailyPointsRulesTest {

	@Mock private MessageSource messageSource;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
	}

	@Test
	void parse_defaultsMissingFieldsToZero() {
		Map<String, Integer> parsed =
				MemberSigninDailyPointsRules.parseNonNegativeWeekdayPoints(
						Map.of("points_mon", 3), messageSource, Locale.SIMPLIFIED_CHINESE);

		assertThat(parsed.get("points_mon")).isEqualTo(3);
		assertThat(parsed.get("points_tue")).isZero();
	}

	@Test
	void parse_rejectsNonNumericString() {
		assertThatThrownBy(
						() ->
								MemberSigninDailyPointsRules.parseNonNegativeWeekdayPoints(
										Map.of("points_mon", "abc"),
										messageSource,
										Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void parseRuleDesc_defaultsNullToEmpty() {
		assertThat(
						MemberSigninDailyPointsRules.parseRuleDesc(
								null, messageSource, Locale.SIMPLIFIED_CHINESE))
				.isEmpty();
	}

	@Test
	void parseRuleDesc_keepsHtml() {
		assertThat(
						MemberSigninDailyPointsRules.parseRuleDesc(
								"<p>规则</p>", messageSource, Locale.SIMPLIFIED_CHINESE))
				.isEqualTo("<p>规则</p>");
	}

	@Test
	void parseRuleDesc_rejectsNonString() {
		assertThatThrownBy(
						() ->
								MemberSigninDailyPointsRules.parseRuleDesc(
										Map.of("html", "x"), messageSource, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}
}
