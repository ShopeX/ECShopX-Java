/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.promotions.service.usersign;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageReceivesPackageService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import cn.shopex.ecshopx.promotions.domain.UserSignIn;
import cn.shopex.ecshopx.promotions.domain.UserSignInLogs;
import cn.shopex.ecshopx.promotions.domain.UserSignInRules;
import cn.shopex.ecshopx.promotions.mapper.UserSignInLogsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserSignInMapper;
import cn.shopex.ecshopx.promotions.mapper.UserSignInRulesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserSignFrontSignInService {

	private static final ZoneId SIGN_ZONE = ZoneId.of("Asia/Shanghai");

	private static final String COUPON_SOURCE = "大转盘中奖领取";

	private final UserSignInMapper userSignInMapper;
	private final UserSignInRulesMapper userSignInRulesMapper;
	private final UserSignInLogsMapper userSignInLogsMapper;
	private final ObjectMapper objectMapper;
	private final MemberAccountService memberAccountService;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final CardPackageReceivesPackageService cardPackageReceivesPackageService;
	private final MessageSource messageSource;

	public UserSignFrontSignInService(
			UserSignInMapper userSignInMapper,
			UserSignInRulesMapper userSignInRulesMapper,
			UserSignInLogsMapper userSignInLogsMapper,
			ObjectMapper objectMapper,
			MemberAccountService memberAccountService,
			PointMemberAddPointService pointMemberAddPointService,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			CardPackageReceivesPackageService cardPackageReceivesPackageService,
			MessageSource messageSource) {
		this.userSignInMapper = userSignInMapper;
		this.userSignInRulesMapper = userSignInRulesMapper;
		this.userSignInLogsMapper = userSignInLogsMapper;
		this.objectMapper = objectMapper;
		this.memberAccountService = memberAccountService;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.cardPackageReceivesPackageService = cardPackageReceivesPackageService;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean signIn(long companyId, long userId, Locale locale) {
		LocalDate today = LocalDate.now(SIGN_ZONE);
		long dup =
				userSignInMapper.selectCount(
						new LambdaQueryWrapper<UserSignIn>()
								.eq(UserSignIn::getUserId, userId)
								.eq(UserSignIn::getSignDate, today));
		if (dup > 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.sign.error_already_signed_today", null, locale));
		}

		UserSignIn row = new UserSignIn();
		row.setUserId(userId);
		row.setCompanyId(companyId);
		row.setSignDate(today);
		int now = (int) Math.min(Instant.now().getEpochSecond(), Integer.MAX_VALUE);
		row.setCreated(now);
		row.setUpdated(now);
		userSignInMapper.insert(row);

		int consecutiveDays = countConsecutiveDays(userId, today);
		UserSignInRules rule =
				userSignInRulesMapper.selectOne(
						new LambdaQueryWrapper<UserSignInRules>()
								.eq(UserSignInRules::getCompanyId, companyId)
								.eq(UserSignInRules::getDaysRequired, (long) consecutiveDays)
								.last("LIMIT 1"));
		if (rule == null) {
			return false;
		}

		String rewardText = rule.getRewardText();
		if (!StringUtils.hasText(rewardText)) {
			throw new BadRequestException("签到规则 reward_text 配置无效");
		}
		List<java.util.LinkedHashMap<String, Object>> rewards;
		try {
			rewards =
					objectMapper.readValue(
							rewardText.trim(),
							new TypeReference<List<java.util.LinkedHashMap<String, Object>>>() {});
		} catch (JsonProcessingException e) {
			throw new BadRequestException("签到规则 reward_text 配置无效");
		}
		if (rewards == null) {
			throw new BadRequestException("签到规则 reward_text 配置无效");
		}
		for (Object o : rewards) {
			if (o == null || !(o instanceof Map)) {
				throw new BadRequestException("签到规则 reward_text 配置无效");
			}
		}

		grantRewards(rewards, companyId, userId);

		UserSignInLogs logRow = new UserSignInLogs();
		logRow.setCompanyId(companyId);
		logRow.setUserId(userId);
		logRow.setType(2);
		logRow.setRewardTitle(rule.getRuleName() != null ? rule.getRuleName() : "");
		logRow.setActivityId(0L);
		logRow.setRewardText(rule.getRewardText() != null ? rule.getRewardText() : "");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		userSignInLogsMapper.insert(logRow);

		return true;
	}

	private int countConsecutiveDays(long userId, LocalDate upTo) {
		int days = 0;
		LocalDate d = upTo;
		while (true) {
			long cnt =
					userSignInMapper.selectCount(
							new LambdaQueryWrapper<UserSignIn>()
									.eq(UserSignIn::getUserId, userId)
									.eq(UserSignIn::getSignDate, d));
			if (cnt > 0) {
				days++;
				d = d.minusDays(1);
			} else {
				break;
			}
		}
		return days;
	}

	private void grantRewards(
			List<java.util.LinkedHashMap<String, Object>> rewards, long companyId, long userId) {
		for (java.util.LinkedHashMap<String, Object> item : rewards) {
			Map<String, Object> map = item;
			String type = Objects.toString(map.getOrDefault("prize_type", ""), "").trim();
			if (Objects.equals("points", type)) {
				int points = parsePrizePositiveInt(map.get("prize_value"));
				pointMemberAddPointService.addPointForTurntableWin(userId, companyId, points);
			} else if (Objects.equals("coupon", type)) {
				long cardId = parsePrizePositiveLong(map.get("prize_value"), "签到优惠券奖励配置无效");
				Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
				String mobile =
						member.get("mobile") == null ? "" : String.valueOf(member.get("mobile")).trim();
				if (!StringUtils.hasText(mobile)) {
					throw new ResourceException("未查询到相关会员信息");
				}
				userDiscountReceiveCardService.receiveCard(
						companyId, userId, mobile, cardId, 0L, "", COUPON_SOURCE);
			} else if (Objects.equals("coupons", type)) {
				long packageId = parsePrizePositiveLong(map.get("prize_value"), "签到券包奖励配置无效");
				cardPackageReceivesPackageService.receivesPackage(
						companyId, packageId, userId, COUPON_SOURCE, 0L);
			}
		}
	}

	private static int parsePrizePositiveInt(Object prizeValue) {
		String s = Objects.toString(prizeValue, "").trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("签到积分奖励配置无效");
		}
		try {
			int v = Integer.parseInt(s);
			if (v <= 0) {
				throw new ResourceException("签到积分奖励配置无效");
			}
			return v;
		} catch (NumberFormatException ignored) {
			try {
				double d = Double.parseDouble(s);
				if (d <= 0 || d > Integer.MAX_VALUE || d != Math.rint(d)) {
					throw new ResourceException("签到积分奖励配置无效");
				}
				return (int) d;
			} catch (NumberFormatException e) {
				throw new ResourceException("签到积分奖励配置无效");
			}
		}
	}

	private static long parsePrizePositiveLong(Object prizeValue, String invalidMsg) {
		String s = Objects.toString(prizeValue, "").trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(invalidMsg);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException ignored) {
			try {
				double d = Double.parseDouble(s);
				if (d <= 0 || d != Math.rint(d) || d > (double) Long.MAX_VALUE) {
					throw new ResourceException(invalidMsg);
				}
				return (long) d;
			} catch (NumberFormatException e) {
				throw new ResourceException(invalidMsg);
			}
		}
	}
}
