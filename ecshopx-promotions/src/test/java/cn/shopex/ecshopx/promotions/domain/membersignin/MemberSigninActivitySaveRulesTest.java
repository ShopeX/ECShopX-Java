package cn.shopex.ecshopx.promotions.domain.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.time.ZoneId;
import java.util.List;
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
class MemberSigninActivitySaveRulesTest {

	@Mock private MessageSource messageSource;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
	}

	@Test
	void overlaps_detectsClosedIntervalConflict() {
		assertThat(MemberSigninActivitySaveRules.overlaps(1, 10, 5, 15)).isTrue();
		assertThat(MemberSigninActivitySaveRules.overlaps(1, 4, 5, 10)).isFalse();
	}

	@Test
	void validateNoTimeConflict_rejectsOverlap() {
		MemberSigninActivity other = new MemberSigninActivity();
		other.setId(2L);
		other.setCompanyId(1L);
		other.setBeginTime(100L);
		other.setEndTime(200L);

		assertThatThrownBy(
						() ->
								MemberSigninActivitySaveRules.validateNoTimeConflict(
										1L,
										150L,
										250L,
										null,
										List.of(other),
										messageSource,
										Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void parseAndValidateTiers_requiresReward() {
		assertThatThrownBy(
						() ->
								MemberSigninActivitySaveRules.parseAndValidateTiers(
										List.of(Map.of("tier_count", 1)),
										7,
										messageSource,
										Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void periodDays_countsInclusiveLocalDates() {
		ZoneId zone = ZoneId.of("Asia/Shanghai");
		long begin =
				java.time.LocalDate.of(2024, 1, 1).atStartOfDay(zone).toEpochSecond();
		long end = java.time.LocalDate.of(2024, 1, 2).atStartOfDay(zone).toEpochSecond();
		assertThat(MemberSigninActivitySaveRules.periodDays(begin, end, zone)).isEqualTo(2);
	}
}
