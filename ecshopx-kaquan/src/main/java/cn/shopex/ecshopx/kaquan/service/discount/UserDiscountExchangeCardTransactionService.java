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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountExchangeCardTransactionService {

	private final UserDiscountMapper userDiscountMapper;
	private final DistributorMapper distributorMapper;
	private final DiscountCardTemplateForExchangeService discountCardTemplateForExchangeService;
	private final UserDiscountExchangeItemRedisLockService userDiscountExchangeItemRedisLockService;
	private final ExcardInventoryPort excardInventoryPort;

	public UserDiscountExchangeCardTransactionService(
			UserDiscountMapper userDiscountMapper,
			DistributorMapper distributorMapper,
			DiscountCardTemplateForExchangeService discountCardTemplateForExchangeService,
			UserDiscountExchangeItemRedisLockService userDiscountExchangeItemRedisLockService,
			ExcardInventoryPort excardInventoryPort) {
		this.userDiscountMapper = userDiscountMapper;
		this.distributorMapper = distributorMapper;
		this.discountCardTemplateForExchangeService = discountCardTemplateForExchangeService;
		this.userDiscountExchangeItemRedisLockService = userDiscountExchangeItemRedisLockService;
		this.excardInventoryPort = excardInventoryPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean exchangeCard(
			long companyId,
			long userId,
			long userCardRecordId,
			long itemId,
			long distributorId) {
		UserDiscount userCard = userDiscountMapper.selectOne(new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getId, userCardRecordId)
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getUserId, userId));
		if (userCard == null) {
			throw new ResourceException("兑换券不存在");
		}
		Integer st = userCard.getStatus();
		if (st == null || (st != 1 && st != 10)) {
			throw new ResourceException("兑换券使用失败，该兑换券已被使用或无效");
		}
		if (!"new_gift".equals(userCard.getCardType())) {
			throw new ResourceException("兑换券类型错误");
		}
		int nowUnix = (int) (System.currentTimeMillis() / 1000L);
		if (userCard.getBeginDate() != null && userCard.getBeginDate() > nowUnix) {
			throw new ResourceException("兑换券使用失败，该兑换券未到使用日期");
		}
		if (userCard.getEndDate() != null && userCard.getEndDate() <= nowUnix) {
			throw new ResourceException("兑换券使用失败，该兑换券已过期");
		}
		if (Objects.equals(String.valueOf(itemId), nullToEmpty(userCard.getRelItemIds()))
				&& Objects.equals(String.valueOf(distributorId), nullToEmpty(userCard.getRelDistributorIds()))) {
			return true;
		}
		if (distributorId > 0L) {
			Distributor d = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getDistributorId, distributorId)
					.eq(Distributor::getCompanyId, companyId)
					.last("LIMIT 1"));
			if (d == null) {
				throw new ResourceException("兑换券使用失败，店铺不存在");
			}
		}
		long templateCardId = userCard.getCardId() != null ? userCard.getCardId() : 0L;
		if (templateCardId <= 0L) {
			throw new ResourceException("兑换券模板不存在");
		}
		Map<String, Object> cardInfo = discountCardTemplateForExchangeService.loadForExchange(companyId, templateCardId, itemId);
		if (cardInfo == null || cardInfo.isEmpty()) {
			throw new ResourceException("兑换券模板不存在");
		}
		assertStoreWhitelist(cardInfo.get("distributor_id"), distributorId);
		int useBound = toInt(cardInfo.get("use_bound"));
		final int exchangeLimit;
		if (useBound != 0) {
			exchangeLimit = resolveUseLimitForItem(cardInfo, itemId);
		} else {
			exchangeLimit = 0;
		}
		final int lockHours = toInt(cardInfo.get("lock_time"));
		final int nowUnixFinal = nowUnix;
		final UserDiscount userCardRef = userCard;
		final long templateCardIdFinal = templateCardId;
		userDiscountExchangeItemRedisLockService.executeWithLockOrThrow(companyId, itemId, () -> {
			if (exchangeLimit > 0) {
				long exchangeCount = userDiscountMapper.selectCount(new LambdaQueryWrapper<UserDiscount>()
						.eq(UserDiscount::getCompanyId, companyId)
						.eq(UserDiscount::getCardId, templateCardIdFinal)
						.eq(UserDiscount::getRelItemIds, String.valueOf(itemId)));
				if (exchangeLimit <= exchangeCount) {
					throw new ResourceException("兑换券使用失败，该商品兑换已达到上限");
				}
			}
			boolean isTotal = excardInventoryPort.resolveIsTotalStore(companyId, itemId, distributorId);
			boolean decrOk = excardInventoryPort.minusItemStore(companyId, itemId, 1, distributorId, isTotal);
			if (!decrOk) {
				throw new ResourceException("兑换券使用失败，该商品库存已用尽");
			}
			if (userCardRef.getStatus() != null && userCardRef.getStatus() == 10) {
				String prevItemStr = userCardRef.getRelItemIds();
				if (!StringUtils.hasText(prevItemStr) || !StringUtils.hasText(prevItemStr.trim())) {
					throw new ResourceException("兑换券使用失败，不适用的商品");
				}
				long prevItemId;
				try {
					prevItemId = Long.parseLong(prevItemStr.trim());
				} catch (NumberFormatException e) {
					throw new ResourceException("兑换券使用失败，不适用的商品");
				}
				if (prevItemId <= 0L) {
					throw new ResourceException("兑换券使用失败，不适用的商品");
				}
				long prevDistributorId = 0L;
				String pds = userCardRef.getRelDistributorIds();
				if (StringUtils.hasText(pds)) {
					try {
						prevDistributorId = Long.parseLong(pds.trim());
					} catch (NumberFormatException e) {
						prevDistributorId = 0L;
					}
				}
				boolean prevTotal = excardInventoryPort.resolveIsTotalStore(companyId, prevItemId, prevDistributorId);
				excardInventoryPort.minusItemStore(companyId, prevItemId, -1, prevDistributorId, prevTotal);
			}
			int candidate = nowUnixFinal + lockHours * 3600;
			int expiredUnix;
			if (userCardRef.getEndDate() != null) {
				expiredUnix = Math.min(candidate, userCardRef.getEndDate());
			} else {
				expiredUnix = candidate;
			}
			userDiscountMapper.update(null, new LambdaUpdateWrapper<UserDiscount>()
					.eq(UserDiscount::getId, userCardRecordId)
					.eq(UserDiscount::getCompanyId, companyId)
					.eq(UserDiscount::getUserId, userId)
					.set(UserDiscount::getRelItemIds, String.valueOf(itemId))
					.set(UserDiscount::getRelDistributorIds, String.valueOf(distributorId))
					.set(UserDiscount::getStatus, 10)
					.set(UserDiscount::getUsedTime, nowUnixFinal)
					.set(UserDiscount::getExpiredTime, expiredUnix));
		});
		return true;
	}

	private static void assertStoreWhitelist(Object distributorField, long distributorId) {
		String raw = distributorField == null ? "" : String.valueOf(distributorField).trim();
		if (raw.startsWith(",")) {
			raw = raw.substring(1);
		}
		if (raw.endsWith(",")) {
			raw = raw.substring(0, raw.length() - 1);
		}
		if (!StringUtils.hasText(raw)) {
			return;
		}
		boolean allowed = false;
		for (String p : raw.split(",")) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				if (Long.parseLong(p.trim()) == distributorId) {
					allowed = true;
					break;
				}
			} catch (NumberFormatException ignored) {
				// skip token
			}
		}
		if (!allowed) {
			throw new ResourceException("兑换券使用失败，不适用的门店");
		}
	}

	private static int resolveUseLimitForItem(Map<String, Object> cardInfo, long itemId) {
		Object raw = cardInfo.get("rel_items");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			throw new ResourceException("兑换券使用失败，不适用的商品");
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> m)) {
				continue;
			}
			Object iid = m.get("item_id");
			long rowItemId = toLong(iid);
			if (rowItemId == itemId) {
				return toInt(m.get("use_limit"));
			}
		}
		throw new ResourceException("兑换券使用失败，不适用的商品");
	}

	private static int toInt(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long toLong(Object v) {
		if (v == null) {
			return Long.MIN_VALUE;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return Long.MIN_VALUE;
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
