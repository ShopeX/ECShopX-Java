package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;

/** 日常积分配置校验（F-002） */
public final class MemberSigninDailyPointsRules {

	private MemberSigninDailyPointsRules() {}

	public static Map<String, Integer> parseNonNegativeWeekdayPoints(
			Map<String, Object> input, MessageSource messageSource, Locale locale) {
		LinkedHashMap<String, Integer> parsed = new LinkedHashMap<>();
		for (String key : MemberSigninDailyPointsPayload.fieldKeys()) {
			Object raw = input == null ? null : input.get(key);
			if (raw == null || (raw instanceof String s && s.isBlank())) {
				parsed.put(key, 0);
				continue;
			}
			Integer value = parseNonNegativeInteger(raw, messageSource, locale);
			parsed.put(key, value);
		}
		return parsed;
	}

	public static String parseRuleDesc(Object raw, MessageSource messageSource, Locale locale) {
		if (raw == null) {
			return "";
		}
		if (!(raw instanceof String text)) {
			throw invalidRuleDesc(messageSource, locale);
		}
		if (text.length() > MemberSigninDailyPointsPayload.RULE_DESC_MAX_LENGTH) {
			throw invalidRuleDesc(messageSource, locale);
		}
		return text;
	}

	private static Integer parseNonNegativeInteger(
			Object raw, MessageSource messageSource, Locale locale) {
		if (raw instanceof Number number) {
			double asDouble = number.doubleValue();
			if (asDouble != Math.floor(asDouble) || asDouble < 0) {
				throw invalid(messageSource, locale);
			}
			if (asDouble > Integer.MAX_VALUE) {
				throw invalid(messageSource, locale);
			}
			return (int) asDouble;
		}
		String text = Objects.toString(raw, "").trim();
		if (text.isEmpty()) {
			return 0;
		}
		try {
			if (text.contains(".") || text.contains("e") || text.contains("E")) {
				throw invalid(messageSource, locale);
			}
			long value = Long.parseLong(text);
			if (value < 0 || value > Integer.MAX_VALUE) {
				throw invalid(messageSource, locale);
			}
			return (int) value;
		} catch (NumberFormatException ex) {
			throw invalid(messageSource, locale);
		}
	}

	private static BadRequestException invalid(MessageSource messageSource, Locale locale) {
		String message =
				messageSource.getMessage(
						"promotions.member_signin.daily_points_invalid",
						null,
						"日常积分配置无效",
						locale);
		return new BadRequestException(message);
	}

	private static BadRequestException invalidRuleDesc(MessageSource messageSource, Locale locale) {
		String message =
				messageSource.getMessage(
						"promotions.member_signin.rule_desc_invalid",
						null,
						"签到规则说明无效",
						locale);
		return new BadRequestException(message);
	}
}
