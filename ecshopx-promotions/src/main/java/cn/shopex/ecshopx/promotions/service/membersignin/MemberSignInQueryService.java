package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityProgress;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivitySaveRules;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityTier;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardGrant;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardGrantStatus;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardRecordType;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninTierDisplayStatus;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityProgressMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityTierMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberSignInQueryService {

	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

	private final MemberSignInActivitySupport activitySupport;
	private final MemberSigninZoneResolver zoneResolver;
	private final MemberSigninActivityProgressMapper progressMapper;
	private final MemberSigninActivityTierMapper tierMapper;
	private final MemberSigninRewardGrantMapper grantMapper;
	private final MemberSigninRecordMapper recordMapper;
	private final MemberSigninActivityMapper activityMapper;
	private final MemberSignInRewardService rewardService;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public MemberSignInQueryService(
			MemberSignInActivitySupport activitySupport,
			MemberSigninZoneResolver zoneResolver,
			MemberSigninActivityProgressMapper progressMapper,
			MemberSigninActivityTierMapper tierMapper,
			MemberSigninRewardGrantMapper grantMapper,
			MemberSigninRecordMapper recordMapper,
			MemberSigninActivityMapper activityMapper,
			MemberSignInRewardService rewardService,
			MessageSource messageSource,
			ObjectMapper objectMapper) {
		this.activitySupport = activitySupport;
		this.zoneResolver = zoneResolver;
		this.progressMapper = progressMapper;
		this.tierMapper = tierMapper;
		this.grantMapper = grantMapper;
		this.recordMapper = recordMapper;
		this.activityMapper = activityMapper;
		this.rewardService = rewardService;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getActivityProgress(long companyId, long userId) {
		MemberSigninActivity activity = activitySupport.findInProgressActivity(companyId);
		Map<String, Object> out = new LinkedHashMap<>();
		if (activity == null) {
			out.put("progress_count", 0);
			out.put("activity", null);
			return out;
		}
		int progressCount = loadProgressCount(companyId, userId, activity.getId());
		long begin = activity.getBeginTime() == null ? 0L : activity.getBeginTime();
		long end = activity.getEndTime() == null ? 0L : activity.getEndTime();
		int periodDays =
				MemberSigninActivitySaveRules.periodDays(
						begin, end, zoneResolver.resolveZone(companyId));

		List<MemberSigninActivityTier> tiers =
				tierMapper.selectList(
						new LambdaQueryWrapper<MemberSigninActivityTier>()
								.eq(MemberSigninActivityTier::getActivityId, activity.getId())
								.orderByAsc(MemberSigninActivityTier::getTierCount));
		Map<Integer, MemberSigninRewardGrant> grantsByTier = loadTierGrants(companyId, userId, activity.getId());

		List<Map<String, Object>> tierItems = new ArrayList<>();
		if (tiers != null) {
			for (MemberSigninActivityTier tier : tiers) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("tier_count", tier.getTierCount());
				if (tier.getPointsReward() != null && tier.getPointsReward() > 0) {
					item.put("points_reward", tier.getPointsReward());
				}
				if (tier.getCouponId() != null && tier.getCouponId() > 0L) {
					Map<String, Object> couponSummary = new LinkedHashMap<>();
					couponSummary.put("id", tier.getCouponId());
					String title = rewardService.couponTitle(companyId, tier.getCouponId());
					if (StringUtils.hasText(title)) {
						couponSummary.put("title", title);
					}
					item.put("coupon_summary", couponSummary);
				}
				item.put("status", resolveTierStatus(progressCount, tier.getTierCount(), grantsByTier));
				tierItems.add(item);
			}
		}

		Map<String, Object> activityMap = new LinkedHashMap<>();
		activityMap.put("id", activity.getId());
		activityMap.put("name", activity.getName());
		activityMap.put("begin_time", begin);
		activityMap.put("end_time", end);
		activityMap.put("progress_count", progressCount);
		activityMap.put("period_days", periodDays);
		activityMap.put("tiers", tierItems);

		out.put("progress_count", progressCount);
		out.put("activity", activityMap);
		return out;
	}

	public Map<String, Object> getMonthlyRecords(long companyId, long userId, String monthRaw, Locale locale) {
		YearMonth month = parseMonth(monthRaw, locale);
		LocalDate monthStart = month.atDay(1);
		LocalDate monthEnd = month.atEndOfMonth();
		LocalDate today = LocalDate.now(zoneResolver.resolveZone(companyId));

		List<MemberSigninRecord> rows =
				recordMapper.selectList(
						new LambdaQueryWrapper<MemberSigninRecord>()
								.eq(MemberSigninRecord::getCompanyId, companyId)
								.eq(MemberSigninRecord::getUserId, userId)
								.ge(MemberSigninRecord::getSignDate, monthStart)
								.le(MemberSigninRecord::getSignDate, monthEnd));
		Map<LocalDate, MemberSigninRecord> signedByDate = new LinkedHashMap<>();
		if (rows != null) {
			for (MemberSigninRecord row : rows) {
				if (row.getSignDate() != null) {
					signedByDate.put(row.getSignDate(), row);
				}
			}
		}

		List<Map<String, Object>> days = new ArrayList<>();
		for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
			int configured = activitySupport.resolveDailyPointsForDate(companyId, date);
			days.add(
					MemberSignInDayDisplay.buildDayMap(
							date, today, signedByDate.get(date), true, configured));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("month", MONTH.format(month));
		out.put("days", days);
		return out;
	}

	public Map<String, Object> getRewards(
			long companyId, long userId, Long activityId, String pageRaw, String pageSizeRaw) {
		int page = MemberSigninActivitySaveRules.parsePage(pageRaw);
		int pageSize = MemberSigninActivitySaveRules.parsePageSize(pageSizeRaw);
		Page<MemberSigninRewardGrant> pageReq = new Page<>(page, pageSize);
		LambdaQueryWrapper<MemberSigninRewardGrant> query =
				new LambdaQueryWrapper<MemberSigninRewardGrant>()
						.eq(MemberSigninRewardGrant::getCompanyId, companyId)
						.eq(MemberSigninRewardGrant::getUserId, userId)
						.eq(MemberSigninRewardGrant::getStatus, MemberSigninRewardGrantStatus.GRANTED)
						.orderByDesc(MemberSigninRewardGrant::getId);
		if (activityId != null && activityId > 0L) {
			query.eq(MemberSigninRewardGrant::getActivityId, activityId);
		}
		Page<MemberSigninRewardGrant> result = grantMapper.selectPage(pageReq, query);

		List<Map<String, Object>> list = new ArrayList<>();
		for (MemberSigninRewardGrant grant : result.getRecords()) {
			list.add(toRewardItem(companyId, grant));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", result.getTotal());
		out.put("page", page);
		out.put("page_size", pageSize);
		return out;
	}

	private Map<String, Object> toRewardItem(long companyId, MemberSigninRewardGrant grant) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("id", grant.getId());
		String recordType =
				StringUtils.hasText(grant.getRecordType())
						? grant.getRecordType()
						: MemberSigninRewardRecordType.TIER;
		item.put("record_type", recordType);
		if (grant.getActivityId() != null) {
			item.put("activity_id", grant.getActivityId());
			MemberSigninActivity activity = activityMapper.selectById(grant.getActivityId());
			if (activity != null && StringUtils.hasText(activity.getName())) {
				item.put("activity_name", activity.getName());
			}
		}
		if (StringUtils.hasText(grant.getTitle())) {
			item.put("title", grant.getTitle());
		}
		if (grant.getTierCount() != null) {
			item.put("tier_count", grant.getTierCount());
		}
		List<Map<String, Object>> rewardItems =
				MemberSignInRewardSnapshot.parseItems(objectMapper, grant.getRewardSnapshot());
		enrichCouponTitles(companyId, rewardItems);
		item.put("reward_items", rewardItems);
		item.put("created", grant.getCreated());
		return item;
	}

	private void enrichCouponTitles(long companyId, List<Map<String, Object>> rewardItems) {
		for (Map<String, Object> rewardItem : rewardItems) {
			if (!"coupon".equals(rewardItem.get("type"))) {
				continue;
			}
			if (rewardItem.containsKey("title")) {
				continue;
			}
			Object couponId = rewardItem.get("coupon_id");
			if (couponId instanceof Number number) {
				String title = rewardService.couponTitle(companyId, number.longValue());
				if (StringUtils.hasText(title)) {
					rewardItem.put("title", title);
				}
			}
		}
	}

	private Map<Integer, MemberSigninRewardGrant> loadTierGrants(
			long companyId, long userId, long activityId) {
		List<MemberSigninRewardGrant> grants =
				grantMapper.selectList(
						new LambdaQueryWrapper<MemberSigninRewardGrant>()
								.eq(MemberSigninRewardGrant::getCompanyId, companyId)
								.eq(MemberSigninRewardGrant::getUserId, userId)
								.eq(MemberSigninRewardGrant::getActivityId, activityId)
								.eq(
										MemberSigninRewardGrant::getRecordType,
										MemberSigninRewardRecordType.TIER));
		Map<Integer, MemberSigninRewardGrant> map = new LinkedHashMap<>();
		if (grants != null) {
			for (MemberSigninRewardGrant grant : grants) {
				if (grant.getTierCount() != null) {
					map.put(grant.getTierCount(), grant);
				}
			}
		}
		return map;
	}

	private String resolveTierStatus(
			int progressCount, int tierCount, Map<Integer, MemberSigninRewardGrant> grantsByTier) {
		if (progressCount < tierCount) {
			return MemberSigninTierDisplayStatus.NOT_REACHED;
		}
		MemberSigninRewardGrant grant = grantsByTier.get(tierCount);
		if (grant == null) {
			return MemberSigninTierDisplayStatus.NOT_REACHED;
		}
		if (MemberSigninRewardGrantStatus.PENDING.equals(grant.getStatus())) {
			return MemberSigninTierDisplayStatus.PROCESSING;
		}
		if (MemberSigninRewardGrantStatus.FAILED.equals(grant.getStatus())) {
			return MemberSigninTierDisplayStatus.FAILED;
		}
		return MemberSigninTierDisplayStatus.GRANTED;
	}

	private int loadProgressCount(long companyId, long userId, long activityId) {
		MemberSigninActivityProgress row =
				progressMapper.selectOne(
						new LambdaQueryWrapper<MemberSigninActivityProgress>()
								.eq(MemberSigninActivityProgress::getCompanyId, companyId)
								.eq(MemberSigninActivityProgress::getUserId, userId)
								.eq(MemberSigninActivityProgress::getActivityId, activityId)
								.last("LIMIT 1"));
		return row == null || row.getProgressCount() == null ? 0 : row.getProgressCount();
	}

	private YearMonth parseMonth(String monthRaw, Locale locale) {
		if (!StringUtils.hasText(monthRaw)) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_signin.records_month_invalid",
							null,
							"月份参数无效",
							locale));
		}
		try {
			return YearMonth.parse(monthRaw.trim(), MONTH);
		} catch (DateTimeParseException ex) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_signin.records_month_invalid",
							null,
							"月份参数无效",
							locale));
		}
	}
}
