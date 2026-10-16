package cn.shopex.ecshopx.promotions.domain.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.time.LocalDate;
import java.util.Locale;
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
class MemberSigninCalendarWeekTest {

	@Mock private MessageSource messageSource;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
	}

	@Test
	void resolveWeekStart_defaultsToMondayOfCurrentWeek() {
		LocalDate tuesday = LocalDate.of(2026, 9, 15);
		LocalDate weekStart =
				MemberSigninCalendarWeek.resolveWeekStart(null, tuesday, messageSource, Locale.CHINA);
		assertThat(weekStart).isEqualTo(LocalDate.of(2026, 9, 14));
		assertThat(MemberSigninCalendarWeek.weekEnd(weekStart)).isEqualTo(LocalDate.of(2026, 9, 20));
	}

	@Test
	void resolveWeekStart_acceptsExplicitMonday() {
		LocalDate weekStart =
				MemberSigninCalendarWeek.resolveWeekStart(
						"2026-09-07", LocalDate.of(2026, 9, 15), messageSource, Locale.CHINA);
		assertThat(weekStart).isEqualTo(LocalDate.of(2026, 9, 7));
	}

	@Test
	void resolveWeekStart_rejectsNonMonday() {
		assertThatThrownBy(
						() ->
								MemberSigninCalendarWeek.resolveWeekStart(
										"2026-09-15",
										LocalDate.of(2026, 9, 15),
										messageSource,
										Locale.CHINA))
				.isInstanceOf(BadRequestException.class);
	}
}
