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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 由 {@code reward_config} 生成 C 端/Admin 列表 {@code rewards[]} 与 CSV 多行文案。 */
@Service
public class MemberTaskRewardsDisplayService {

	private final DiscountCardsMapper discountCardsMapper;
	private final ObjectMapper objectMapper;

	public MemberTaskRewardsDisplayService(
			DiscountCardsMapper discountCardsMapper, ObjectMapper objectMapper) {
		this.discountCardsMapper = discountCardsMapper;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> buildRewards(long companyId, String rewardConfigJson) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (!StringUtils.hasText(rewardConfigJson)) {
			return out;
		}
		try {
			JsonNode node = objectMapper.readTree(rewardConfigJson);
			JsonNode points = node.get("points");
			if (points != null && !points.isNull() && points.isNumber() && points.asDouble() > 0) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("kind", "points");
				item.put("points", points.asLong());
				out.add(item);
			}
			JsonNode coupon = node.get("coupon_id");
			if (coupon != null && !coupon.isNull() && coupon.isNumber() && coupon.asLong() > 0) {
				long couponId = coupon.asLong();
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("kind", "coupon");
				item.put("coupon_id", couponId);
				String title = couponTitle(companyId, couponId);
				if (StringUtils.hasText(title)) {
					item.put("title", title);
				}
				out.add(item);
			}
		} catch (Exception ignored) {
			// malformed reward_config → empty rewards
		}
		return out;
	}

	public String formatRewardsMultiline(List<Map<String, Object>> rewards) {
		if (rewards == null || rewards.isEmpty()) {
			return "";
		}
		List<String> lines = new ArrayList<>();
		for (Map<String, Object> reward : rewards) {
			if (reward == null) {
				continue;
			}
			Object kind = reward.get("kind");
			if ("points".equals(kind)) {
				Object points = reward.get("points");
				if (points instanceof Number n && n.longValue() > 0) {
					lines.add(n.longValue() + "积分");
				}
			} else if ("coupon".equals(kind)) {
				Object title = reward.get("title");
				if (title != null && StringUtils.hasText(title.toString())) {
					lines.add(title.toString().trim());
				} else {
					lines.add("优惠券");
				}
			}
		}
		return String.join("\n", lines);
	}

	private String couponTitle(long companyId, long couponId) {
		DiscountCards card =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardId, couponId)
								.last("LIMIT 1"));
		return card == null ? null : card.getTitle();
	}
}
