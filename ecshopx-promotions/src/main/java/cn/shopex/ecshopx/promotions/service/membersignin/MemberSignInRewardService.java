package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivityTier;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardGrant;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardGrantStatus;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRewardRecordType;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninActivityTierMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberSignInRewardService {

	static final String COUPON_SOURCE = "会员签到活动";
	static final String DAILY_TITLE = "签到";

	private final MemberSigninActivityTierMapper tierMapper;
	private final MemberSigninRewardGrantMapper grantMapper;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final DiscountCardsMapper discountCardsMapper;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;

	public MemberSignInRewardService(
			MemberSigninActivityTierMapper tierMapper,
			MemberSigninRewardGrantMapper grantMapper,
			PointMemberAddPointService pointMemberAddPointService,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			DiscountCardsMapper discountCardsMapper,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper) {
		this.tierMapper = tierMapper;
		this.grantMapper = grantMapper;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.discountCardsMapper = discountCardsMapper;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
	}

	public void recordDailyGrant(
			long companyId, long userId, LocalDate signDate, int points, long signinRecordId) {
		if (points <= 0) {
			return;
		}
		Long existing =
				grantMapper.selectCount(
						new LambdaQueryWrapper<MemberSigninRewardGrant>()
								.eq(MemberSigninRewardGrant::getCompanyId, companyId)
								.eq(MemberSigninRewardGrant::getUserId, userId)
								.eq(MemberSigninRewardGrant::getRecordType, MemberSigninRewardRecordType.DAILY)
								.eq(MemberSigninRewardGrant::getSignDate, signDate));
		if (existing != null && existing > 0) {
			return;
		}
		int now = (int) Instant.now().getEpochSecond();
		MemberSigninRewardGrant grant = new MemberSigninRewardGrant();
		grant.setCompanyId(companyId);
		grant.setUserId(userId);
		grant.setRecordType(MemberSigninRewardRecordType.DAILY);
		grant.setSignDate(signDate);
		grant.setTitle(DAILY_TITLE);
		grant.setStatus(MemberSigninRewardGrantStatus.GRANTED);
		grant.setRewardSnapshot(MemberSignInRewardSnapshot.writeDailyPoints(objectMapper, points));
		grant.setSigninRecordId(signinRecordId);
		grant.setCreated(now);
		grant.setUpdated(now);
		try {
			grantMapper.insert(grant);
		} catch (DuplicateKeyException ignored) {
			// 幂等：同日日常流水已存在
		}
	}

	public GrantOutcome grantEligibleTiers(
			long companyId,
			long userId,
			MemberSigninActivity activity,
			int progressCount,
			long signinRecordId) {
		List<MemberSigninActivityTier> tiers =
				tierMapper.selectList(
						new LambdaQueryWrapper<MemberSigninActivityTier>()
								.eq(MemberSigninActivityTier::getActivityId, activity.getId())
								.orderByAsc(MemberSigninActivityTier::getTierCount));
		if (tiers == null || tiers.isEmpty()) {
			return GrantOutcome.empty();
		}
		Set<Integer> existing =
				grantMapper
						.selectList(
								new LambdaQueryWrapper<MemberSigninRewardGrant>()
										.eq(MemberSigninRewardGrant::getCompanyId, companyId)
										.eq(MemberSigninRewardGrant::getUserId, userId)
										.eq(MemberSigninRewardGrant::getActivityId, activity.getId())
										.eq(
												MemberSigninRewardGrant::getRecordType,
												MemberSigninRewardRecordType.TIER))
						.stream()
						.map(MemberSigninRewardGrant::getTierCount)
						.filter(count -> count != null)
						.collect(Collectors.toSet());

		List<Map<String, Object>> successGrants = new ArrayList<>();
		boolean anyFailure = false;
		int now = (int) Instant.now().getEpochSecond();

		for (MemberSigninActivityTier tier : tiers) {
			if (tier.getTierCount() == null || tier.getTierCount() > progressCount) {
				continue;
			}
			if (existing.contains(tier.getTierCount())) {
				continue;
			}
			if (!tierHasReward(tier)) {
				continue;
			}
			MemberSigninRewardGrant grant = new MemberSigninRewardGrant();
			grant.setCompanyId(companyId);
			grant.setUserId(userId);
			grant.setRecordType(MemberSigninRewardRecordType.TIER);
			grant.setActivityId(activity.getId());
			grant.setTierCount(tier.getTierCount());
			grant.setTitle(tierTitle(tier.getTierCount()));
			grant.setStatus(MemberSigninRewardGrantStatus.PENDING);
			grant.setSigninRecordId(signinRecordId);
			grant.setCreated(now);
			grant.setUpdated(now);
			grantMapper.insert(grant);

			ExecuteResult result = executeGrant(companyId, userId, tier, grant);
			if (result.granted()) {
				grant.setStatus(MemberSigninRewardGrantStatus.GRANTED);
				grant.setErrorMessage(null);
				grant.setUpdated((int) Instant.now().getEpochSecond());
				grantMapper.updateById(grant);
				successGrants.add(toGrantResponse(tier, result.rewardItems()));
			} else {
				anyFailure = true;
				grant.setStatus(MemberSigninRewardGrantStatus.FAILED);
				if (!StringUtils.hasText(grant.getErrorMessage())) {
					grant.setErrorMessage("发放失败");
				}
				grant.setUpdated((int) Instant.now().getEpochSecond());
				grantMapper.updateById(grant);
			}
		}
		return new GrantOutcome(anyFailure, successGrants);
	}

	private boolean tierHasReward(MemberSigninActivityTier tier) {
		Integer points = tier.getPointsReward();
		Long couponId = tier.getCouponId();
		return (points != null && points > 0) || (couponId != null && couponId > 0L);
	}

	private ExecuteResult executeGrant(
			long companyId, long userId, MemberSigninActivityTier tier, MemberSigninRewardGrant grant) {
		List<Map<String, Object>> items = new ArrayList<>();
		List<String> errors = new ArrayList<>();

		Integer points = tier.getPointsReward();
		if (points != null && points > 0) {
			try {
				pointMemberAddPointService.addPointForMemberSignInActivityTier(userId, companyId, points);
				items.add(MemberSignInRewardSnapshot.pointsItem(points));
			} catch (Exception ex) {
				errors.add(ex.getMessage());
			}
		}
		Long couponId = tier.getCouponId();
		if (couponId != null && couponId > 0L) {
			if (grantCoupon(companyId, userId, couponId, grant.getId())) {
				items.add(
						MemberSignInRewardSnapshot.couponItem(
								couponId, couponTitle(companyId, couponId)));
			} else {
				errors.add("优惠券发放失败");
			}
		}

		grant.setRewardSnapshot(MemberSignInRewardSnapshot.writeTierItems(objectMapper, items));
		if (!errors.isEmpty()) {
			grant.setErrorMessage(String.join("; ", errors));
		}
		return new ExecuteResult(!items.isEmpty(), items);
	}

	private boolean grantCoupon(long companyId, long userId, long couponId, long grantId) {
		try {
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				return false;
			}
			userDiscountReceiveCardService.receiveCard(
					companyId,
					userId,
					mobile,
					couponId,
					0L,
					"",
					COUPON_SOURCE,
					"member-signin-" + grantId);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	private Map<String, Object> toGrantResponse(
			MemberSigninActivityTier tier, List<Map<String, Object>> rewardItems) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("tier_count", tier.getTierCount());
		item.put("title", tierTitle(tier.getTierCount()));
		item.put("reward_items", rewardItems);
		return item;
	}

	static String tierTitle(int tierCount) {
		return String.format(Locale.ROOT, "累计签到满%d天", tierCount);
	}

	public String couponTitle(long companyId, Long couponId) {
		if (couponId == null || couponId <= 0L) {
			return null;
		}
		DiscountCards card =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardId, couponId)
								.last("LIMIT 1"));
		return card == null ? null : card.getTitle();
	}

	private record ExecuteResult(boolean granted, List<Map<String, Object>> rewardItems) {}

	public record GrantOutcome(boolean anyFailure, List<Map<String, Object>> successGrants) {
		static GrantOutcome empty() {
			return new GrantOutcome(false, List.of());
		}
	}
}
