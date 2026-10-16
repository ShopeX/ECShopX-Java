package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.util.StringUtils;

/** 自然周 Mon～Sun 解析（F-011） */
public final class MemberSigninCalendarWeek {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private MemberSigninCalendarWeek() {}

	public static LocalDate resolveWeekStart(
			String weekStartRaw, LocalDate today, MessageSource messageSource, Locale locale) {
		if (!StringUtils.hasText(weekStartRaw)) {
			return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		}
		LocalDate weekStart;
		try {
			weekStart = LocalDate.parse(weekStartRaw.trim(), ISO_DATE);
		} catch (DateTimeParseException ex) {
			throw invalidWeek(messageSource, locale);
		}
		if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
			throw invalidWeek(messageSource, locale);
		}
		return weekStart;
	}

	public static LocalDate weekEnd(LocalDate weekStart) {
		return weekStart.plusDays(6);
	}

	public static int weekdayMonOne(LocalDate date) {
		return date.getDayOfWeek().getValue();
	}

	private static BadRequestException invalidWeek(MessageSource messageSource, Locale locale) {
		String message =
				messageSource.getMessage(
						"promotions.member_signin.calendar_week_invalid",
						null,
						"周起始日期无效",
						locale);
		return new BadRequestException(message);
	}
}
