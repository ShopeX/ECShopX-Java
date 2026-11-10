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

package cn.shopex.ecshopx.goods.search.price;

import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.promotions.support.MemberTierDiscountPrice;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 独占分支生效单价：活动价 &gt; 定制会员价 &gt; 等级折扣价 &gt; 销售价（见商品 ES 搜索设计 §6）。
 */
@Component
public class SearchEffectivePriceCalculator {

	/**
	 * @param nowEpochSec 查询时刻（秒），用于活动窗命中
	 */
	public int effectivePriceFen(GoodsSearchDocument doc, SearchMemberTier tier, long nowEpochSec) {
		Optional<GoodsSearchDocument.Activity> act = doc.activities().stream()
				.filter(a -> a.startAt() <= nowEpochSec && nowEpochSec <= a.endAt())
				.sorted(Comparator.comparingInt(GoodsSearchDocument.Activity::priority))
				.findFirst();
		if (act.isPresent()) {
			return Math.toIntExact(act.get().activityPrice());
		}
		long sale = doc.salePrice();
		Map<Long, Long> bucket =
				"normal".equals(tier.lvType()) ? doc.memberPrices().grade() : doc.memberPrices().vipGrade();
		Long custom = bucket.get(tier.gradeId());
		if (custom != null && custom > 0) {
			return Math.toIntExact(custom);
		}
		int d = tier.discount();
		if (d > 0 && d < 100) {
			return MemberTierDiscountPrice.memberPriceFromTierDiscount(sale, d);
		}
		return Math.toIntExact(sale);
	}
}
