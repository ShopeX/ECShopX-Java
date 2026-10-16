package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityDisplay;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDailyPoints;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDisplayStatus;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninDailyPointsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MemberSignInActivitySupport {

	private final MemberSigninActivityMapper activityMapper;
	private final MemberSigninDailyPointsMapper dailyPointsMapper;

	public MemberSignInActivitySupport(
			MemberSigninActivityMapper activityMapper, MemberSigninDailyPointsMapper dailyPointsMapper) {
		this.activityMapper = activityMapper;
		this.dailyPointsMapper = dailyPointsMapper;
	}

	public MemberSigninActivity findInProgressActivity(long companyId) {
		long now = Instant.now().getEpochSecond();
		List<MemberSigninActivity> activities =
				activityMapper.selectList(
						new LambdaQueryWrapper<MemberSigninActivity>()
								.eq(MemberSigninActivity::getCompanyId, companyId));
		if (activities == null) {
			return null;
		}
		for (MemberSigninActivity activity : activities) {
			long begin = activity.getBeginTime() == null ? 0L : activity.getBeginTime();
			long end = activity.getEndTime() == null ? 0L : activity.getEndTime();
			if (MemberSigninDisplayStatus.IN_PROGRESS.equals(
					MemberSigninActivityDisplay.resolve(begin, end, now))) {
				return activity;
			}
		}
		return null;
	}

	public int resolveDailyPointsForDate(long companyId, LocalDate date) {
		MemberSigninDailyPoints config = dailyPointsMapper.selectById(companyId);
		if (config == null) {
			return 0;
		}
		DayOfWeek dow = date.getDayOfWeek();
		return switch (dow) {
			case MONDAY -> zeroIfNull(config.getPointsMon());
			case TUESDAY -> zeroIfNull(config.getPointsTue());
			case WEDNESDAY -> zeroIfNull(config.getPointsWed());
			case THURSDAY -> zeroIfNull(config.getPointsThu());
			case FRIDAY -> zeroIfNull(config.getPointsFri());
			case SATURDAY -> zeroIfNull(config.getPointsSat());
			case SUNDAY -> zeroIfNull(config.getPointsSun());
		};
	}

	private static int zeroIfNull(Integer value) {
		return value == null ? 0 : Math.max(0, value);
	}
}
