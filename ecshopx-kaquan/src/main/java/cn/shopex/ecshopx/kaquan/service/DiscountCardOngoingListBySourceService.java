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

package cn.shopex.ecshopx.kaquan.service;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardOngoingListBySourceService {

	public static final String SOURCE_TYPE_DISTRIBUTOR = "distributor";

	private final DiscountCardsMapper discountCardsMapper;

	public DiscountCardOngoingListBySourceService(DiscountCardsMapper discountCardsMapper) {
		this.discountCardsMapper = discountCardsMapper;
	}

	public Map<Long, List<Map<String, Object>>> mapOngoingByDistributorIds(long companyId, List<Long> distributorIds) {
		Map<Long, List<Map<String, Object>>> byShop = new LinkedHashMap<>();
		if (distributorIds == null || distributorIds.isEmpty()) {
			return byShop;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
		w.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getSourceType, SOURCE_TYPE_DISTRIBUTOR)
				.in(DiscountCards::getSourceId, distributorIds)
				.eq(DiscountCards::getKqStatus, 0)
				.and(q -> q.eq(DiscountCards::getDateType, "DATE_TYPE_FIX_TERM")
						.or(n -> n.eq(DiscountCards::getDateType, "DATE_TYPE_FIX_TIME_RANGE")
								.le(DiscountCards::getBeginDate, now)
								.ge(DiscountCards::getEndDate, now)));
		w.orderByDesc(DiscountCards::getCreated);
		List<DiscountCards> rows = discountCardsMapper.selectList(w);
		for (DiscountCards c : rows) {
			Long sid = c.getSourceId();
			if (sid == null) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("card_id", c.getCardId());
			m.put("card_type", c.getCardType());
			m.put("title", c.getTitle());
			m.put("color", c.getColor());
			m.put("date_type", c.getDateType());
			m.put("begin_date", c.getBeginDate());
			m.put("end_date", c.getEndDate());
			m.put("fixed_term", c.getFixedTerm());
			m.put("quantity", c.getQuantity());
			m.put("discount", c.getDiscount());
			m.put("least_cost", c.getLeastCost());
			m.put("most_cost", c.getMostCost());
			m.put("reduce_cost", c.getReduceCost());
			m.put("get_limit", c.getGetLimit());
			int receive = 0;
			if (c.getReceive() != null) {
				String r = c.getReceive().trim();
				receive = ("1".equals(r) || "true".equalsIgnoreCase(r)) ? 1 : 0;
			}
			m.put("receive", receive);
			byShop.computeIfAbsent(sid, k -> new ArrayList<>()).add(m);
		}
		return byShop;
	}
}
