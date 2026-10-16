package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountNewGiftCardUpdateService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityDisplay;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivitySaveRules;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivitySaveRules.ParsedTier;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityTier;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDisplayStatus;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityTierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberSignInActivityService {

	private final MemberSigninActivityMapper activityMapper;
	private final MemberSigninActivityTierMapper tierMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final MessageSource messageSource;
	private final ZoneId zoneId = ZoneId.systemDefault();

	public MemberSignInActivityService(
			MemberSigninActivityMapper activityMapper,
			MemberSigninActivityTierMapper tierMapper,
			DiscountCardsMapper discountCardsMapper,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			MessageSource messageSource) {
		this.activityMapper = activityMapper;
		this.tierMapper = tierMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.messageSource = messageSource;
	}

	public Map<String, Object> listActivities(
			long companyId, String name, String pageRaw, String pageSizeRaw) {
		long now = Instant.now().getEpochSecond();
		int page = MemberSigninActivitySaveRules.parsePage(pageRaw);
		int pageSize = MemberSigninActivitySaveRules.parsePageSize(pageSizeRaw);

		LambdaQueryWrapper<MemberSigninActivity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(MemberSigninActivity::getCompanyId, companyId);
		if (StringUtils.hasText(name)) {
			String escaped = name.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
			wrapper.apply("name LIKE CONCAT('%',{0},'%') ESCAPE '\\\\'", escaped);
		}
		wrapper.orderByDesc(MemberSigninActivity::getId);

		Page<MemberSigninActivity> pageReq = new Page<>(page, pageSize);
		Page<MemberSigninActivity> pageResult = activityMapper.selectPage(pageReq, wrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		for (MemberSigninActivity row : pageResult.getRecords()) {
			list.add(MemberSigninActivitySaveRules.toActivitySummary(row, now, null));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", pageResult.getTotal());
		out.put("page", page);
		out.put("page_size", pageSize);
		return out;
	}

	public Map<String, Object> getActivity(long companyId, long activityId) {
		MemberSigninActivity row = requireActivity(companyId, activityId, Locale.getDefault());
		long now = Instant.now().getEpochSecond();
		List<MemberSigninActivityTier> tiers = loadTiers(activityId);
		return MemberSigninActivitySaveRules.toActivitySummary(
				row, now, MemberSigninActivitySaveRules.toTierMaps(tiers));
	}

	@Transactional
	public Map<String, Object> createActivity(long companyId, Map<String, Object> input, Locale locale) {
		long now = Instant.now().getEpochSecond();
		String name = MemberSigninActivitySaveRules.parseAndValidateName(input.get("name"), messageSource, locale);
		long beginTime =
				MemberSigninActivitySaveRules.parseRequiredEpoch(input.get("begin_time"), messageSource, locale);
		long endTime =
				MemberSigninActivitySaveRules.parseRequiredEpoch(input.get("end_time"), messageSource, locale);
		MemberSigninActivitySaveRules.validateForCreate(beginTime, endTime, now, messageSource, locale);
		List<MemberSigninActivity> existing = loadCompanyActivities(companyId);
		MemberSigninActivitySaveRules.validateNoTimeConflict(
				companyId, beginTime, endTime, null, existing, messageSource, locale);
		int periodDays = MemberSigninActivitySaveRules.periodDays(beginTime, endTime, zoneId);
		List<ParsedTier> tiers =
				MemberSigninActivitySaveRules.parseAndValidateTiers(
						input.get("tiers"), periodDays, messageSource, locale);
		validateTierCoupons(companyId, tiers, locale);

		MemberSigninActivity row = new MemberSigninActivity();
		row.setCompanyId(companyId);
		row.setName(name);
		row.setBeginTime(beginTime);
		row.setEndTime(endTime);
		row.setConfigVersion(1L);
		row.setCreated((int) now);
		row.setUpdated((int) now);
		activityMapper.insert(row);
		saveTiers(companyId, row.getId(), tiers, now);
		return getActivity(companyId, row.getId());
	}

	@Transactional
	public Map<String, Object> updateActivity(
			long companyId, long activityId, Map<String, Object> input, Locale locale) {
		long now = Instant.now().getEpochSecond();
		MemberSigninActivity existing = requireActivity(companyId, activityId, locale);
		long currentBegin = existing.getBeginTime() == null ? 0L : existing.getBeginTime();
		long currentEnd = existing.getEndTime() == null ? 0L : existing.getEndTime();
		String currentStatus = MemberSigninActivityDisplay.resolve(currentBegin, currentEnd, now);

		long expectedVersion =
				MemberSigninActivitySaveRules.parseRequiredConfigVersion(
						input.get("config_version"), messageSource, locale);
		long actualVersion = existing.getConfigVersion() == null ? 1L : existing.getConfigVersion();
		if (expectedVersion != actualVersion) {
			throw new ConflictException(
					messageSource.getMessage(
							"promotions.member_signin.config_version_conflict",
							null,
							"活动配置已被他人修改，请刷新后重试",
							locale));
		}

		String name = MemberSigninActivitySaveRules.parseAndValidateName(input.get("name"), messageSource, locale);
		long beginTime =
				MemberSigninActivitySaveRules.parseRequiredEpoch(input.get("begin_time"), messageSource, locale);
		long endTime =
				MemberSigninActivitySaveRules.parseRequiredEpoch(input.get("end_time"), messageSource, locale);
		MemberSigninActivitySaveRules.validateForUpdate(
				currentStatus, beginTime, endTime, now, messageSource, locale);
		List<MemberSigninActivity> all = loadCompanyActivities(companyId);
		MemberSigninActivitySaveRules.validateNoTimeConflict(
				companyId, beginTime, endTime, activityId, all, messageSource, locale);
		int periodDays = MemberSigninActivitySaveRules.periodDays(beginTime, endTime, zoneId);
		List<ParsedTier> tiers =
				MemberSigninActivitySaveRules.parseAndValidateTiers(
						input.get("tiers"), periodDays, messageSource, locale);
		validateTierCoupons(companyId, tiers, locale);

		LambdaUpdateWrapper<MemberSigninActivity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper
				.eq(MemberSigninActivity::getId, activityId)
				.eq(MemberSigninActivity::getCompanyId, companyId)
				.eq(MemberSigninActivity::getConfigVersion, actualVersion)
				.set(MemberSigninActivity::getName, name)
				.set(MemberSigninActivity::getBeginTime, beginTime)
				.set(MemberSigninActivity::getEndTime, endTime)
				.set(MemberSigninActivity::getConfigVersion, actualVersion + 1L)
				.set(MemberSigninActivity::getUpdated, (int) now);
		int rows = activityMapper.update(null, updateWrapper);
		if (rows == 0) {
			throw new ConflictException(
					messageSource.getMessage(
							"promotions.member_signin.config_version_conflict",
							null,
							"活动配置已被他人修改，请刷新后重试",
							locale));
		}

		tierMapper.delete(
				new LambdaQueryWrapper<MemberSigninActivityTier>()
						.eq(MemberSigninActivityTier::getActivityId, activityId));
		saveTiers(companyId, activityId, tiers, now);
		return getActivity(companyId, activityId);
	}

	@Transactional
	public Map<String, Object> stopActivity(long companyId, long activityId, Locale locale) {
		long now = Instant.now().getEpochSecond();
		MemberSigninActivity existing = requireActivity(companyId, activityId, locale);
		long begin = existing.getBeginTime() == null ? 0L : existing.getBeginTime();
		long end = existing.getEndTime() == null ? 0L : existing.getEndTime();
		String status = MemberSigninActivityDisplay.resolve(begin, end, now);
		if (MemberSigninDisplayStatus.ENDED.equals(status)) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_signin.activity_ended_readonly",
							null,
							"活动已结束，不可编辑",
							locale));
		}

		com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<MemberSigninActivity> updateWrapper =
				new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
		updateWrapper
				.eq("id", activityId)
				.eq("company_id", companyId)
				.set("end_time", now)
				.set("updated", (int) now);
		int rows = activityMapper.update(null, updateWrapper);
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.member_signin.activity_not_found",
							null,
							"活动不存在",
							locale));
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", activityId);
		out.put("end_time", now);
		out.put("display_status", MemberSigninDisplayStatus.ENDED);
		return out;
	}

	private void validateTierCoupons(long companyId, List<ParsedTier> tiers, Locale locale) {
		for (ParsedTier tier : tiers) {
			if (tier.couponId() == null) {
				continue;
			}
			DiscountCards card =
					discountCardsMapper.selectOne(
							new LambdaQueryWrapper<DiscountCards>()
									.eq(DiscountCards::getCompanyId, companyId)
									.eq(DiscountCards::getCardId, tier.couponId())
									.last("LIMIT 1"));
			if (!isCouponValidForConfig(card)) {
				throw new BadRequestException(
						messageSource.getMessage(
								"promotions.member_signin.tier_coupon_invalid",
								null,
								"阶梯优惠券无效或库存不足",
								locale));
			}
		}
	}

	private boolean isCouponValidForConfig(DiscountCards card) {
		if (card == null) {
			return false;
		}
		int kq = card.getKqStatus() == null ? -1 : card.getKqStatus();
		if (kq != DiscountNewGiftCardUpdateService.STATUS_NORMAL) {
			return false;
		}
		Integer end = card.getEndDate();
		long nowSec = Instant.now().getEpochSecond();
		if (end != null && end > 0 && end <= nowSec) {
			return false;
		}
		int quantity = card.getQuantity() == null ? 0 : card.getQuantity();
		int issued =
				userDiscountReceiveCardService.countIssuedForCard(card.getCompanyId(), card.getCardId());
		return quantity > issued;
	}

	private void saveTiers(long companyId, long activityId, List<ParsedTier> tiers, long now) {
		int sort = 0;
		for (ParsedTier tier : tiers) {
			MemberSigninActivityTier row = new MemberSigninActivityTier();
			row.setActivityId(activityId);
			row.setCompanyId(companyId);
			row.setTierCount(tier.tierCount());
			row.setPointsReward(tier.pointsReward());
			row.setCouponId(tier.couponId());
			row.setSortOrder(sort++);
			tierMapper.insert(row);
		}
	}

	private List<MemberSigninActivityTier> loadTiers(long activityId) {
		return tierMapper.selectList(
				new LambdaQueryWrapper<MemberSigninActivityTier>()
						.eq(MemberSigninActivityTier::getActivityId, activityId)
						.orderByAsc(MemberSigninActivityTier::getTierCount));
	}

	private List<MemberSigninActivity> loadCompanyActivities(long companyId) {
		return activityMapper.selectList(
				new LambdaQueryWrapper<MemberSigninActivity>().eq(MemberSigninActivity::getCompanyId, companyId));
	}

	private MemberSigninActivity requireActivity(long companyId, long activityId, Locale locale) {
		MemberSigninActivity row =
				activityMapper.selectOne(
						new LambdaQueryWrapper<MemberSigninActivity>()
								.eq(MemberSigninActivity::getId, activityId)
								.eq(MemberSigninActivity::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.member_signin.activity_not_found",
							null,
							"活动不存在",
							locale));
		}
		return row;
	}
}
