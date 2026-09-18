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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.mapper.PromoterGoodsMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromoterFrontGetPromoterGoodsService {

	private final PromoterGoodsMapper promoterGoodsMapper;

	public PromoterFrontGetPromoterGoodsService(PromoterGoodsMapper promoterGoodsMapper) {
		this.promoterGoodsMapper = promoterGoodsMapper;
	}

	public List<Long> getPromoterGoods(
			long companyId,
			long filterUserId,
			Long goodsIdOrNull,
			int page,
			int pageSize,
			Object goodsRaw) {
		boolean isAllGoodsMode = computeIsAllGoodsModeForRebateSkip(goodsRaw);
		long totalBase = promoterGoodsMapper.countBasePromoterGoods(companyId, filterUserId, goodsIdOrNull);
		if (totalBase <= 0L) {
			return Collections.emptyList();
		}
		int offset = (page - 1) * pageSize;
		List<Long> rows =
				promoterGoodsMapper.listPagedGoodsIdsJoinedRebate(
						companyId, filterUserId, goodsIdOrNull, isAllGoodsMode, offset, pageSize);
		if (rows == null) {
			rows = Collections.emptyList();
		}
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (Long x : rows) {
			if (x != null) {
				set.add(x);
			}
		}
		return new ArrayList<>(set);
	}

	private static boolean computeIsAllGoodsModeForRebateSkip(Object goodsRaw) {
		if (goodsRaw == null) {
			return false;
		}
		if (goodsRaw instanceof Boolean b) {
			return b;
		}
		if (goodsRaw instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (goodsRaw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return false;
			}
			if ("0".equals(t)) {
				return false;
			}
			if ("all".equalsIgnoreCase(t)) {
				return true;
			}
			return true;
		}
		if (goodsRaw instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (goodsRaw instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		String v = String.valueOf(goodsRaw).trim();
		return !v.isEmpty();
	}
}
