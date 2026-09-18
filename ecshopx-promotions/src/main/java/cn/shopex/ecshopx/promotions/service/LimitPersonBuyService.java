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

import cn.shopex.ecshopx.promotions.domain.LimitPersonPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitPersonPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.springframework.stereotype.Service;

/**
 * 限购活动用户累计购买量（对齐 PHP LimitService::createLimitPerson / getLimitPersonInfo）。
 */
@Service
public class LimitPersonBuyService {

	private final LimitPersonPromotionsMapper limitPersonPromotionsMapper;
	private final LimitPromotionsMapper limitPromotionsMapper;

	public LimitPersonBuyService(
			LimitPersonPromotionsMapper limitPersonPromotionsMapper, LimitPromotionsMapper limitPromotionsMapper) {
		this.limitPersonPromotionsMapper = limitPersonPromotionsMapper;
		this.limitPromotionsMapper = limitPromotionsMapper;
	}

	/**
	 * 下单成功后写入/累加用户限购购买数量。
	 *
	 * @param params limit_id, user_id, item_id, company_id, number, day, distributor_id
	 */
	public void createLimitPerson(Map<String, Object> params) {
		long limitId = longVal(params.get("limit_id"));
		long userId = longVal(params.get("user_id"));
		long itemId = longVal(params.get("item_id"));
		long companyId = longVal(params.get("company_id"));
		long number = longVal(params.get("number"));
		long distributorId = longVal(params.get("distributor_id"));
		int day = intVal(params.get("day"));
		if (limitId <= 0L || userId <= 0L || itemId <= 0L || companyId <= 0L || number <= 0L) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LimitPersonPromotions existing =
				limitPersonPromotionsMapper.selectOne(
						new LambdaQueryWrapper<LimitPersonPromotions>()
								.eq(LimitPersonPromotions::getCompanyId, companyId)
								.eq(LimitPersonPromotions::getUserId, userId)
								.eq(LimitPersonPromotions::getItemId, itemId)
								.eq(LimitPersonPromotions::getLimitId, limitId)
								.eq(LimitPersonPromotions::getDistributorId, distributorId)
								.lt(LimitPersonPromotions::getStartTime, now)
								.gt(LimitPersonPromotions::getEndTime, now)
								.orderByDesc(LimitPersonPromotions::getId)
								.last("LIMIT 1"));
		if (existing == null) {
			LimitPromotions limit = limitPromotionsMapper.selectById(limitId);
			if (limit == null) {
				return;
			}
			LimitPersonPromotions row = new LimitPersonPromotions();
			row.setLimitId(limitId);
			row.setUserId(userId);
			row.setItemId(itemId);
			row.setCompanyId(companyId);
			row.setNumber(number);
			row.setDistributorId(distributorId);
			if (day == 0) {
				row.setStartTime(limit.getStartTime() != null ? limit.getStartTime() : now);
				row.setEndTime(limit.getEndTime() != null ? limit.getEndTime() : now);
			} else {
				int dayStart = startOfTodayEpochSeconds();
				row.setStartTime(dayStart);
				row.setEndTime(dayStart + day * 24 * 3600);
			}
			row.setCreated(now);
			row.setUpdated(now);
			limitPersonPromotionsMapper.insert(row);
			return;
		}
		long cur = existing.getNumber() != null ? existing.getNumber() : 0L;
		existing.setNumber(cur + number);
		existing.setUpdated(now);
		limitPersonPromotionsMapper.updateById(existing);
	}

	/** 查询用户在有效限购活动窗口内的累计已购数量（跨店铺累加，对齐 PHP getLimitPersonInfo）。 */
	public long getLimitPersonBuyNumber(long companyId, long userId, long itemId) {
		return getLimitPersonBuyNumber(companyId, userId, itemId, null);
	}

	/**
	 * @param distributorId null 表示不按店铺过滤；非 null 时按店铺过滤（店铺限购）
	 */
	public long getLimitPersonBuyNumber(long companyId, long userId, long itemId, Long distributorId) {
		if (companyId <= 0L || userId <= 0L || itemId <= 0L) {
			return 0L;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<LimitPersonPromotions> q =
				new LambdaQueryWrapper<LimitPersonPromotions>()
						.eq(LimitPersonPromotions::getCompanyId, companyId)
						.eq(LimitPersonPromotions::getUserId, userId)
						.eq(LimitPersonPromotions::getItemId, itemId)
						.lt(LimitPersonPromotions::getStartTime, now)
						.gt(LimitPersonPromotions::getEndTime, now)
						.orderByDesc(LimitPersonPromotions::getCreated)
						.last("LIMIT 300");
		if (distributorId != null) {
			q.eq(LimitPersonPromotions::getDistributorId, distributorId);
		}
		List<LimitPersonPromotions> rows = limitPersonPromotionsMapper.selectList(q);
		if (rows == null || rows.isEmpty()) {
			return 0L;
		}
		long number = 0L;
		for (LimitPersonPromotions row : rows) {
			Long limitId = row.getLimitId();
			if (limitId == null || limitId <= 0L) {
				continue;
			}
			LimitPromotions limit = limitPromotionsMapper.selectById(limitId);
			if (limit == null) {
				continue;
			}
			int start = limit.getStartTime() != null ? limit.getStartTime() : 0;
			int end = limit.getEndTime() != null ? limit.getEndTime() : 0;
			if (start < now && end > now) {
				number += row.getNumber() != null ? row.getNumber() : 0L;
			}
		}
		return number;
	}

	private static int startOfTodayEpochSeconds() {
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return (int) (cal.getTimeInMillis() / 1000L);
	}

	private static long longVal(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
