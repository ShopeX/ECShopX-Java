package cn.shopex.ecshopx.promotions.domain.membersignin;

/** 累计签到活动展示态（G24，动态计算） */
public final class MemberSigninActivityDisplay {

	private MemberSigninActivityDisplay() {}

	public static String resolve(long beginTime, long endTime, long nowEpochSec) {
		if (endTime < beginTime) {
			return MemberSigninDisplayStatus.ENDED;
		}
		if (nowEpochSec < beginTime) {
			return MemberSigninDisplayStatus.NOT_STARTED;
		}
		if (nowEpochSec <= endTime) {
			return MemberSigninDisplayStatus.IN_PROGRESS;
		}
		return MemberSigninDisplayStatus.ENDED;
	}
}
