package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDayStatus;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** 签到日历/月历 C 端展示字段（daily_points、display、day_status） */
public final class MemberSignInDayDisplay {

	private MemberSignInDayDisplay() {}

	public static Integer nullableDailyPoints(MemberSigninRecord record) {
		if (record == null || record.getDailyPoints() == null || record.getDailyPoints() <= 0) {
			return null;
		}
		return record.getDailyPoints();
	}

	public static String resolveDisplay(boolean signed, Integer dailyPoints) {
		if (dailyPoints != null && dailyPoints > 0) {
			return "+" + dailyPoints;
		}
		if (signed) {
			return "已签";
		}
		return null;
	}

	public static Integer nullableConfiguredDailyPoints(int configuredDailyPoints) {
		return configuredDailyPoints > 0 ? configuredDailyPoints : null;
	}

	public static String resolveDayStatus(LocalDate date, LocalDate today, boolean signed) {
		if (date.isAfter(today)) {
			return MemberSigninDayStatus.FUTURE;
		}
		if (signed) {
			return MemberSigninDayStatus.SIGNED;
		}
		if (date.equals(today)) {
			return MemberSigninDayStatus.TODAY;
		}
		return MemberSigninDayStatus.MISSED;
	}

	public static Map<String, Object> buildDayMap(
			LocalDate date, LocalDate today, MemberSigninRecord record, boolean includeDayStatus) {
		return buildDayMap(date, today, record, includeDayStatus, 0);
	}

	public static Map<String, Object> buildDayMap(
			LocalDate date,
			LocalDate today,
			MemberSigninRecord record,
			boolean includeDayStatus,
			int configuredDailyPoints) {
		boolean future = date.isAfter(today);
		boolean signed = !future && record != null;
		Integer dailyPoints =
				signed
						? nullableDailyPoints(record)
						: nullableConfiguredDailyPoints(configuredDailyPoints);

		Map<String, Object> day = new LinkedHashMap<>();
		day.put("date", date.toString());
		day.put("signed", signed);
		day.put("daily_points", dailyPoints);
		day.put("display", resolveDisplay(signed, dailyPoints));
		if (signed && record.getActivitySeq() != null) {
			day.put("activity_seq", record.getActivitySeq());
		}
		if (includeDayStatus) {
			day.put("day_status", resolveDayStatus(date, today, signed));
		}
		return day;
	}
}
