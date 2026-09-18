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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardParamNormalize;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiDiscountCardListFilterBuilder {

	private static final List<String> DEFAULT_CARD_TYPES = List.of("cash", "discount", "gift");

	public LambdaQueryWrapper<DiscountCards> build(long companyId, Map<String, Object> merged,
			long nowEpochSeconds) {
		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
		w.eq(DiscountCards::getCompanyId, companyId);
		w.in(DiscountCards::getCouponType, "guide");

		String isValid = merged.get("is_valid") != null ? String.valueOf(merged.get("is_valid")) : null;
		if (isOpenapiValidFilter(isValid)) {
			int threshold = (int) Math.min(nowEpochSeconds, Integer.MAX_VALUE);
			w.and(q -> q.gt(DiscountCards::getEndDate, threshold)
					.or()
					.eq(DiscountCards::getEndDate, 0)
					.or()
					.isNull(DiscountCards::getEndDate));
		}

		Object statusRaw = merged.get("status");
		if (statusRaw != null && isPhpTruthy(String.valueOf(statusRaw).trim())) {
			int parsedStatus = DiscountCardParamNormalize.parseIntFlexible(statusRaw, -1);
			w.eq(DiscountCards::getKqStatus, parsedStatus);
		}

		Object cardTypeObj = merged.get("card_type");
		if (cardTypeObj != null && StringUtils.hasText(String.valueOf(cardTypeObj).trim())) {
			w.eq(DiscountCards::getCardType, String.valueOf(cardTypeObj).trim());
		} else {
			w.in(DiscountCards::getCardType, DEFAULT_CARD_TYPES);
		}

		Object titleObj = merged.get("title");
		if (titleObj != null && StringUtils.hasText(String.valueOf(titleObj).trim())) {
			w.like(DiscountCards::getTitle, String.valueOf(titleObj).trim());
		}

		Object cardIdsObj = merged.get("card_ids");
		if (cardIdsObj != null && StringUtils.hasText(String.valueOf(cardIdsObj))) {
			List<Long> parsedIds = parseCardIds(String.valueOf(cardIdsObj));
			if (!parsedIds.isEmpty()) {
				w.in(DiscountCards::getCardId, parsedIds);
			}
		}

		return w;
	}

	private static boolean isOpenapiValidFilter(String isValid) {
		return "true".equals(isValid) || "true".equals(String.valueOf(isValid));
	}

	private static boolean isPhpTruthy(String value) {
		return value != null && !value.isEmpty() && !"0".equals(value);
	}

	private static List<Long> parseCardIds(String cardIdsRaw) {
		String[] parts = cardIdsRaw.split(",", -1);
		List<Long> out = new ArrayList<>(parts.length);
		for (String p : parts) {
			try {
				out.add(Long.parseLong(p));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}
}
