package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityProgress;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityProgressMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInRewardService.GrantOutcome;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSignInSignService {

	private final MemberSigninRecordMapper recordMapper;
	private final MemberSigninActivityProgressMapper progressMapper;
	private final MemberSignInActivitySupport activitySupport;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final MemberSignInRewardService rewardService;
	private final MemberSigninZoneResolver zoneResolver;
	private final MessageSource messageSource;

	public MemberSignInSignService(
			MemberSigninRecordMapper recordMapper,
			MemberSigninActivityProgressMapper progressMapper,
			MemberSignInActivitySupport activitySupport,
			PointMemberAddPointService pointMemberAddPointService,
			MemberSignInRewardService rewardService,
			MemberSigninZoneResolver zoneResolver,
			MessageSource messageSource) {
		this.recordMapper = recordMapper;
		this.progressMapper = progressMapper;
		this.activitySupport = activitySupport;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.rewardService = rewardService;
		this.zoneResolver = zoneResolver;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> sign(long companyId, long userId, Locale locale) {
		LocalDate today = LocalDate.now(zoneResolver.resolveZone(companyId));
		assertNotSignedToday(companyId, userId, today, locale);

		int weekday = today.getDayOfWeek().getValue();
		int dailyPoints = activitySupport.resolveDailyPointsForDate(companyId, today);
		MemberSigninActivity activity = activitySupport.findInProgressActivity(companyId);
		int now = (int) Instant.now().getEpochSecond();

		MemberSigninRecord record = new MemberSigninRecord();
		record.setCompanyId(companyId);
		record.setUserId(userId);
		record.setSignDate(today);
		record.setWeekday(weekday);
		record.setDailyPoints(0);
		record.setCreated(now);
		try {
			recordMapper.insert(record);
		} catch (DuplicateKeyException ex) {
			throw alreadySigned(locale);
		}

		if (dailyPoints > 0) {
			pointMemberAddPointService.addPointForMemberSignInDaily(userId, companyId, dailyPoints);
			record.setDailyPoints(dailyPoints);
			recordMapper.updateById(record);
			rewardService.recordDailyGrant(companyId, userId, today, dailyPoints, record.getId());
		}

		Map<String, Object> activitySummary = null;
		int progressCount = 0;
		if (activity != null) {
			progressCount = incrementProgress(companyId, userId, activity.getId(), now);
			record.setActivityId(activity.getId());
			record.setActivitySeq(progressCount);
			recordMapper.updateById(record);
			activitySummary = new LinkedHashMap<>();
			activitySummary.put("id", activity.getId());
			activitySummary.put("name", activity.getName());
			activitySummary.put("progress_count", progressCount);
		}

		GrantOutcome grantOutcome = GrantOutcome.empty();
		if (activity != null) {
			grantOutcome =
					rewardService.grantEligibleTiers(
							companyId, userId, activity, progressCount, record.getId());
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("signed_today", true);
		out.put("daily_points", MemberSignInDayDisplay.nullableDailyPoints(record));
		out.put("activity", activitySummary);
		if (!grantOutcome.anyFailure() && !grantOutcome.successGrants().isEmpty()) {
			out.put("new_grants", grantOutcome.successGrants());
		}
		return out;
	}

	private void assertNotSignedToday(long companyId, long userId, LocalDate today, Locale locale) {
		Long count =
				recordMapper.selectCount(
						new LambdaQueryWrapper<MemberSigninRecord>()
								.eq(MemberSigninRecord::getCompanyId, companyId)
								.eq(MemberSigninRecord::getUserId, userId)
								.eq(MemberSigninRecord::getSignDate, today));
		if (count != null && count > 0) {
			throw alreadySigned(locale);
		}
	}

	private ConflictException alreadySigned(Locale locale) {
		return new ConflictException(
				messageSource.getMessage(
						"promotions.member_signin.already_signed_today",
						null,
						"今日已签到",
						locale));
	}

	private int incrementProgress(long companyId, long userId, long activityId, int now) {
		MemberSigninActivityProgress row =
				progressMapper.selectOne(
						new LambdaQueryWrapper<MemberSigninActivityProgress>()
								.eq(MemberSigninActivityProgress::getCompanyId, companyId)
								.eq(MemberSigninActivityProgress::getUserId, userId)
								.eq(MemberSigninActivityProgress::getActivityId, activityId)
								.last("LIMIT 1"));
		if (row == null) {
			row = new MemberSigninActivityProgress();
			row.setCompanyId(companyId);
			row.setUserId(userId);
			row.setActivityId(activityId);
			row.setProgressCount(1);
			row.setUpdated(now);
			progressMapper.insert(row);
			return 1;
		}
		int next = (row.getProgressCount() == null ? 0 : row.getProgressCount()) + 1;
		row.setProgressCount(next);
		row.setUpdated(now);
		progressMapper.updateById(row);
		return next;
	}
}
