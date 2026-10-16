package cn.shopex.ecshopx.promotions.domain.membersignin;

import java.util.LinkedHashMap;
import java.util.Map;

/** Admin 日常积分配置 Api 字段（points_mon … points_sun、rule_desc） */
public final class MemberSigninDailyPointsPayload {

	public static final String POINTS_MON = "points_mon";
	public static final String POINTS_TUE = "points_tue";
	public static final String POINTS_WED = "points_wed";
	public static final String POINTS_THU = "points_thu";
	public static final String POINTS_FRI = "points_fri";
	public static final String POINTS_SAT = "points_sat";
	public static final String POINTS_SUN = "points_sun";
	public static final String RULE_DESC = "rule_desc";
	public static final int RULE_DESC_MAX_LENGTH = 65535;

	private static final String[] FIELD_KEYS = {
		POINTS_MON, POINTS_TUE, POINTS_WED, POINTS_THU, POINTS_FRI, POINTS_SAT, POINTS_SUN
	};

	private MemberSigninDailyPointsPayload() {}

	public static String[] fieldKeys() {
		return FIELD_KEYS.clone();
	}

	public static Map<String, Object> toMap(MemberSigninDailyPoints row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (row == null) {
			for (String key : FIELD_KEYS) {
				out.put(key, 0);
			}
			out.put(RULE_DESC, "");
			return out;
		}
		out.put(POINTS_MON, nullToZero(row.getPointsMon()));
		out.put(POINTS_TUE, nullToZero(row.getPointsTue()));
		out.put(POINTS_WED, nullToZero(row.getPointsWed()));
		out.put(POINTS_THU, nullToZero(row.getPointsThu()));
		out.put(POINTS_FRI, nullToZero(row.getPointsFri()));
		out.put(POINTS_SAT, nullToZero(row.getPointsSat()));
		out.put(POINTS_SUN, nullToZero(row.getPointsSun()));
		out.put(RULE_DESC, ruleDescOrEmpty(row));
		return out;
	}

	public static void applyToEntity(
			MemberSigninDailyPoints row, Map<String, Integer> values, String ruleDesc) {
		row.setPointsMon(values.get(POINTS_MON));
		row.setPointsTue(values.get(POINTS_TUE));
		row.setPointsWed(values.get(POINTS_WED));
		row.setPointsThu(values.get(POINTS_THU));
		row.setPointsFri(values.get(POINTS_FRI));
		row.setPointsSat(values.get(POINTS_SAT));
		row.setPointsSun(values.get(POINTS_SUN));
		row.setRuleDesc(ruleDesc == null ? "" : ruleDesc);
	}

	public static String ruleDescOrEmpty(MemberSigninDailyPoints row) {
		if (row == null || row.getRuleDesc() == null) {
			return "";
		}
		return row.getRuleDesc();
	}

	private static int nullToZero(Integer value) {
		return value == null ? 0 : value;
	}
}
