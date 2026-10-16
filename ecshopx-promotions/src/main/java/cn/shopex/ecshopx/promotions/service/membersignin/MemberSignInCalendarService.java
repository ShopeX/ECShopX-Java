package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityDisplay;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninCalendarWeek;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDailyPointsPayload;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDisplayStatus;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninDailyPointsMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class MemberSignInCalendarService {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private final MemberSigninRecordMapper recordMapper;
	private final MemberSigninActivityMapper activityMapper;
	private final MemberSigninDailyPointsMapper dailyPointsMapper;
	private final MemberSignInActivitySupport activitySupport;
	private final MemberSigninZoneResolver zoneResolver;
	private final MessageSource messageSource;
	private final MemberSignInReminderPushService reminderPushService;

	public MemberSignInCalendarService(
			MemberSigninRecordMapper recordMapper,
			MemberSigninActivityMapper activityMapper,
			MemberSigninDailyPointsMapper dailyPointsMapper,
			MemberSignInActivitySupport activitySupport,
			MemberSigninZoneResolver zoneResolver,
			MessageSource messageSource,
			MemberSignInReminderPushService reminderPushService) {
		this.recordMapper = recordMapper;
		this.activityMapper = activityMapper;
		this.dailyPointsMapper = dailyPointsMapper;
		this.activitySupport = activitySupport;
		this.zoneResolver = zoneResolver;
		this.messageSource = messageSource;
		this.reminderPushService = reminderPushService;
	}

	public Map<String, Object> getCalendar(
			long companyId, long userId, String weekStartRaw, Locale locale) {
		// 惰性兜底：修复 Redis 延迟消息丢失导致的提醒断链（内部自行吞异常，不影响页面加载）
		reminderPushService.ensureArmed(companyId, userId);
		LocalDate today = LocalDate.now(zoneResolver.resolveZone(companyId));
		return getCalendar(companyId, userId, weekStartRaw, today, locale);
	}

	Map<String, Object> getCalendar(
			long companyId,
			long userId,
			String weekStartRaw,
			LocalDate today,
			Locale locale) {
		LocalDate weekStart =
				MemberSigninCalendarWeek.resolveWeekStart(weekStartRaw, today, messageSource, locale);
		LocalDate weekEnd = MemberSigninCalendarWeek.weekEnd(weekStart);

		Map<LocalDate, MemberSigninRecord> signedByDate = loadSignedDates(companyId, userId, weekStart, weekEnd);
		Long currentActivityId = findInProgressActivityId(companyId);

		List<Map<String, Object>> days = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			LocalDate date = weekStart.plusDays(i);
			int configured = activitySupport.resolveDailyPointsForDate(companyId, date);
			days.add(
					MemberSignInDayDisplay.buildDayMap(
							date, today, signedByDate.get(date), false, configured));
		}

		MemberSigninRecord todayRecord = signedByDate.get(today);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("week_start", ISO_DATE.format(weekStart));
		out.put("week_end", ISO_DATE.format(weekEnd));
		out.put("signed_today", todayRecord != null);
		if (currentActivityId != null) {
			out.put("current_activity_id", currentActivityId);
		}
		out.put(
				MemberSigninDailyPointsPayload.RULE_DESC,
				MemberSigninDailyPointsPayload.ruleDescOrEmpty(dailyPointsMapper.selectById(companyId)));
		out.put("days", days);
		return out;
	}

	private Map<LocalDate, MemberSigninRecord> loadSignedDates(
			long companyId, long userId, LocalDate weekStart, LocalDate weekEnd) {
		List<MemberSigninRecord> rows =
				recordMapper.selectList(
						new LambdaQueryWrapper<MemberSigninRecord>()
								.eq(MemberSigninRecord::getCompanyId, companyId)
								.eq(MemberSigninRecord::getUserId, userId)
								.ge(MemberSigninRecord::getSignDate, weekStart)
								.le(MemberSigninRecord::getSignDate, weekEnd));
		Map<LocalDate, MemberSigninRecord> map = new LinkedHashMap<>();
		if (rows == null) {
			return map;
		}
		for (MemberSigninRecord row : rows) {
			if (row.getSignDate() != null) {
				map.put(row.getSignDate(), row);
			}
		}
		return map;
	}

	private Long findInProgressActivityId(long companyId) {
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
				return activity.getId();
			}
		}
		return null;
	}
}
