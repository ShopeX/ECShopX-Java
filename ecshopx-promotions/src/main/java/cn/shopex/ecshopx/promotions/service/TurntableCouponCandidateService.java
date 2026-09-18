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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.kaquan.domain.CardPackageReceive;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveMapper;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageDetailQueryService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountNewGiftCardUpdateService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableGrantQueryResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 抽奖券/券包候选可发预检与发奖查询。 */
@Service
public class TurntableCouponCandidateService {

	private static final String TURNTABLE_FROM = "大转盘中奖领取";
	private static final int PKG_SUCCESS = 2;
	private static final int PKG_FAIL = 3;

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final CardPackageDetailQueryService cardPackageDetailQueryService;
	private final CardPackageReceiveMapper cardPackageReceiveMapper;
	private final MemberAccountService memberAccountService;

	public TurntableCouponCandidateService(
			DiscountCardsMapper discountCardsMapper,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			CardPackageDetailQueryService cardPackageDetailQueryService,
			CardPackageReceiveMapper cardPackageReceiveMapper,
			MemberAccountService memberAccountService) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.cardPackageDetailQueryService = cardPackageDetailQueryService;
		this.cardPackageReceiveMapper = cardPackageReceiveMapper;
		this.memberAccountService = memberAccountService;
	}

	public boolean hasMobile(long userId, long companyId) {
		try {
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
			Object mobile = memberInfo == null ? null : memberInfo.get("mobile");
			return mobile != null && StringUtils.hasText(String.valueOf(mobile));
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isCouponIssuable(long companyId, long userId, long cardId) {
		if (cardId <= 0L || !hasMobile(userId, companyId)) {
			return false;
		}
		DiscountCards card =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardId, cardId)
								.last("LIMIT 1"));
		return isCardIssuable(companyId, userId, card);
	}

	/** 券包配置顺序上是否存在至少一张业务侧仍可发的券。 */
	public boolean isPackageIssuable(long companyId, long userId, long packageId) {
		if (packageId <= 0L || !hasMobile(userId, companyId)) {
			return false;
		}
		List<Map<String, Object>> cards;
		try {
			Map<String, Object> details = cardPackageDetailQueryService.getDetails(companyId, packageId, null);
			Object raw = details.get("discount_cards");
			if (!(raw instanceof List<?> list)) {
				return false;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> typed = (List<Map<String, Object>>) list;
			cards = typed;
		} catch (Exception e) {
			return false;
		}
		for (Map<String, Object> row : cards) {
			long cardId = longOf(row.get("card_id"));
			if (cardId > 0L && isCouponIssuable(companyId, userId, cardId)) {
				return true;
			}
		}
		return false;
	}

	public TurntableGrantQueryResult queryCouponGrant(long companyId, long userId, String requestId) {
		if (userDiscountReceiveCardService.existsTurntableGrant(companyId, userId, requestId)) {
			return TurntableGrantQueryResult.SUCCESS;
		}
		return TurntableGrantQueryResult.UNKNOWN;
	}

	public TurntableGrantQueryResult queryPackageGrant(
			long companyId, long userId, long packageId, int createdEpoch) {
		List<CardPackageReceive> rows =
				cardPackageReceiveMapper.selectList(
						new LambdaQueryWrapper<CardPackageReceive>()
								.eq(CardPackageReceive::getCompanyId, companyId)
								.eq(CardPackageReceive::getUserId, userId)
								.eq(CardPackageReceive::getPackageId, packageId)
								.eq(CardPackageReceive::getReceiveType, TURNTABLE_FROM)
								.ge(CardPackageReceive::getCreated, createdEpoch)
								.orderByDesc(CardPackageReceive::getReceiveId)
								.last("LIMIT 5"));
		if (rows == null || rows.isEmpty()) {
			return TurntableGrantQueryResult.UNKNOWN;
		}
		for (CardPackageReceive row : rows) {
			Integer st = row.getReceiveStatus();
			if (st != null && st == PKG_SUCCESS) {
				return TurntableGrantQueryResult.SUCCESS;
			}
			if (st != null && st == PKG_FAIL) {
				return TurntableGrantQueryResult.FAILED;
			}
		}
		return TurntableGrantQueryResult.UNKNOWN;
	}

	private boolean isCardIssuable(long companyId, long userId, DiscountCards card) {
		if (card == null) {
			return false;
		}
		int kq = card.getKqStatus() == null ? -1 : card.getKqStatus();
		if (kq != DiscountNewGiftCardUpdateService.STATUS_NORMAL) {
			return false;
		}
		Integer end = card.getEndDate();
		long nowSec = System.currentTimeMillis() / 1000L;
		if (end != null && end > 0 && end <= nowSec) {
			return false;
		}
		int quantity = card.getQuantity() == null ? 0 : card.getQuantity();
		int issued = userDiscountReceiveCardService.countIssuedForCard(companyId, card.getCardId());
		if (quantity <= issued) {
			return false;
		}
		int getLimit = card.getGetLimit() == null ? 0 : card.getGetLimit();
		if (getLimit > 0) {
			int userIssued =
					userDiscountReceiveCardService.countIssuedForCardAndUser(companyId, userId, card.getCardId());
			if (userIssued >= getLimit) {
				return false;
			}
		}
		return true;
	}

	private static long longOf(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
