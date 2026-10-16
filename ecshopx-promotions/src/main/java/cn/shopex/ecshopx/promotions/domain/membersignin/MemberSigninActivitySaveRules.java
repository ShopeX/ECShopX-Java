package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.util.StringUtils;

/** Admin 累计活动保存校验（§5.0 · F-004/F-005/F-015） */
public final class MemberSigninActivitySaveRules {

	private MemberSigninActivitySaveRules() {}

	public static String parseAndValidateName(Object raw, MessageSource messageSource, Locale locale) {
		String name = Objects.toString(raw, "").trim();
		if (!StringUtils.hasText(name) || name.length() > 50) {
			throw badRequest(messageSource, locale, "promotions.member_signin.activity_name_invalid", "活动名称不合法");
		}
		return name;
	}

	public static long parseRequiredEpoch(Object raw, MessageSource messageSource, Locale locale) {
		Long value = parseOptionalEpoch(raw);
		if (value == null) {
			throw badRequest(messageSource, locale, "promotions.member_signin.activity_time_invalid", "活动时间不合法");
		}
		return value;
	}

	public static Long parseOptionalEpoch(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number number) {
			return number.longValue();
		}
		String text = raw.toString().trim();
		if (!StringUtils.hasText(text)) {
			return null;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public static long parseRequiredConfigVersion(Object raw, MessageSource messageSource, Locale locale) {
		Long value = parseOptionalEpoch(raw);
		if (value == null || value < 1L) {
			throw badRequest(
					messageSource,
					locale,
					"promotions.member_signin.config_version_conflict",
					"活动配置已被他人修改，请刷新后重试");
		}
		return value;
	}

	public static void validateBeginNotAfterEnd(
			long beginTime, long endTime, MessageSource messageSource, Locale locale) {
		if (beginTime > endTime) {
			throw badRequest(messageSource, locale, "promotions.member_signin.activity_time_invalid", "活动时间不合法");
		}
	}

	public static void validateForCreate(
			long beginTime, long endTime, long nowEpochSec, MessageSource messageSource, Locale locale) {
		validateBeginNotAfterEnd(beginTime, endTime, messageSource, locale);
		if (beginTime < nowEpochSec) {
			throw badRequest(messageSource, locale, "promotions.member_signin.activity_time_invalid", "活动时间不合法");
		}
	}

	public static void validateForUpdate(
			String currentDisplayStatus,
			long beginTime,
			long endTime,
			long nowEpochSec,
			MessageSource messageSource,
			Locale locale) {
		if (MemberSigninDisplayStatus.ENDED.equals(currentDisplayStatus)) {
			throw badRequest(
					messageSource,
					locale,
					"promotions.member_signin.activity_ended_readonly",
					"活动已结束，不可编辑");
		}
		validateBeginNotAfterEnd(beginTime, endTime, messageSource, locale);
		if (MemberSigninDisplayStatus.IN_PROGRESS.equals(currentDisplayStatus)) {
			if (beginTime < nowEpochSec || endTime < nowEpochSec) {
				throw badRequest(messageSource, locale, "promotions.member_signin.activity_time_invalid", "活动时间不合法");
			}
		}
		if (MemberSigninDisplayStatus.NOT_STARTED.equals(currentDisplayStatus)) {
			if (beginTime < nowEpochSec) {
				throw badRequest(messageSource, locale, "promotions.member_signin.activity_time_invalid", "活动时间不合法");
			}
		}
	}

	public static boolean overlaps(long aBegin, long aEnd, long bBegin, long bEnd) {
		return aBegin <= bEnd && aEnd >= bBegin;
	}

	public static void validateNoTimeConflict(
			long companyId,
			long beginTime,
			long endTime,
			Long excludeActivityId,
			List<MemberSigninActivity> existing,
			MessageSource messageSource,
			Locale locale) {
		for (MemberSigninActivity row : existing) {
			if (row == null || row.getId() == null) {
				continue;
			}
			if (excludeActivityId != null && excludeActivityId.equals(row.getId())) {
				continue;
			}
			if (!Objects.equals(row.getCompanyId(), companyId)) {
				continue;
			}
			long existBegin = row.getBeginTime() == null ? 0L : row.getBeginTime();
			long existEnd = row.getEndTime() == null ? 0L : row.getEndTime();
			if (overlaps(beginTime, endTime, existBegin, existEnd)) {
				throw badRequest(
						messageSource,
						locale,
						"promotions.member_signin.activity_time_conflict",
						"活动时间与已有活动冲突");
			}
		}
	}

	public static int periodDays(long beginTime, long endTime, ZoneId zoneId) {
		LocalDate start = Instant.ofEpochSecond(beginTime).atZone(zoneId).toLocalDate();
		LocalDate end = Instant.ofEpochSecond(endTime).atZone(zoneId).toLocalDate();
		return (int) (end.toEpochDay() - start.toEpochDay() + 1);
	}

	public static List<ParsedTier> parseAndValidateTiers(
			Object rawTiers,
			int periodDays,
			MessageSource messageSource,
			Locale locale) {
		if (!(rawTiers instanceof List<?> list) || list.isEmpty()) {
			throw badRequest(messageSource, locale, "promotions.member_signin.tier_required", "请至少配置一个阶梯奖励");
		}
		Set<Integer> seen = new HashSet<>();
		List<ParsedTier> parsed = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof Map<?, ?> map)) {
				throw badRequest(messageSource, locale, "promotions.member_signin.tier_invalid", "阶梯配置不合法");
			}
			Integer tierCount = parseNonNegativeInt(map.get("tier_count"));
			if (tierCount == null || tierCount < 1 || tierCount > periodDays) {
				throw badRequest(messageSource, locale, "promotions.member_signin.tier_invalid", "阶梯配置不合法");
			}
			if (!seen.add(tierCount)) {
				throw badRequest(messageSource, locale, "promotions.member_signin.tier_duplicate", "阶梯累计次数不可重复");
			}
			Integer pointsReward = parseNullableNonNegativeInt(map.get("points_reward"));
			Long couponId = parseNullablePositiveLong(map.get("coupon_id"));
			boolean hasPoints = pointsReward != null && pointsReward > 0;
			boolean hasCoupon = couponId != null && couponId > 0L;
			if (!hasPoints && !hasCoupon) {
				throw badRequest(messageSource, locale, "promotions.member_signin.tier_reward_required", "每个阶梯至少配置积分或优惠券");
			}
			parsed.add(new ParsedTier(tierCount, hasPoints ? pointsReward : null, hasCoupon ? couponId : null));
		}
		parsed.sort((a, b) -> Integer.compare(a.tierCount(), b.tierCount()));
		return parsed;
	}

	public static int parsePage(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 1;
		}
		try {
			return Math.max(1, Integer.parseInt(raw.trim()));
		} catch (NumberFormatException ex) {
			return 1;
		}
	}

	public static int parsePageSize(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 20;
		}
		try {
			return Math.min(100, Math.max(1, Integer.parseInt(raw.trim())));
		} catch (NumberFormatException ex) {
			return 20;
		}
	}

	public static Map<String, Object> toActivitySummary(
			MemberSigninActivity row, long nowEpochSec, List<Map<String, Object>> tiers) {
		long begin = row.getBeginTime() == null ? 0L : row.getBeginTime();
		long end = row.getEndTime() == null ? 0L : row.getEndTime();
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("name", row.getName());
		out.put("begin_time", begin);
		out.put("end_time", end);
		out.put("display_status", MemberSigninActivityDisplay.resolve(begin, end, nowEpochSec));
		out.put("config_version", row.getConfigVersion() == null ? 1L : row.getConfigVersion());
		out.put("created", row.getCreated());
		if (tiers != null) {
			out.put("tiers", tiers);
		}
		return out;
	}

	public static List<Map<String, Object>> toTierMaps(List<MemberSigninActivityTier> tiers) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (tiers == null) {
			return out;
		}
		for (MemberSigninActivityTier tier : tiers) {
			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			map.put("tier_count", tier.getTierCount());
			map.put("points_reward", tier.getPointsReward());
			map.put("coupon_id", tier.getCouponId());
			out.add(map);
		}
		return out;
	}

	private static Integer parseNonNegativeInt(Object raw) {
		Long value = parseOptionalEpoch(raw);
		if (value == null || value < 0L || value > Integer.MAX_VALUE) {
			return null;
		}
		return value.intValue();
	}

	private static Integer parseNullableNonNegativeInt(Object raw) {
		if (raw == null) {
			return null;
		}
		String text = raw.toString().trim();
		if (!StringUtils.hasText(text)) {
			return null;
		}
		return parseNonNegativeInt(raw);
	}

	private static Long parseNullablePositiveLong(Object raw) {
		Long value = parseOptionalEpoch(raw);
		if (value == null || value <= 0L) {
			return null;
		}
		return value;
	}

	private static BadRequestException badRequest(
			MessageSource messageSource, Locale locale, String key, String fallback) {
		String message = messageSource.getMessage(key, null, fallback, locale);
		return new BadRequestException(message);
	}

	public record ParsedTier(int tierCount, Integer pointsReward, Long couponId) {}
}
