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

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.promotions.support.MemberTierDiscountPrice;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEffectivePriceCalculatorTest {

	private static final long NOW = 1_700_000_000L;

	private SearchEffectivePriceCalculator calculator;

	@BeforeEach
	void setUp() {
		calculator = new SearchEffectivePriceCalculator();
	}

	@Test
	void activityBeatsMemberAndSale() {
		GoodsSearchDocument doc = doc(
				2000,
				Map.of(10L, 1500L),
				Map.of(),
				List.of(new GoodsSearchDocument.Activity("seckill", 1L, 990, NOW - 100, NOW + 100, 1)));
		SearchMemberTier tier = new SearchMemberTier("normal", 10L, 90, "银卡");
		assertThat(calculator.effectivePriceFen(doc, tier, NOW)).isEqualTo(990);
	}

	@Test
	void customMemberBeatsDiscount() {
		GoodsSearchDocument doc = doc(2000, Map.of(10L, 1500L), Map.of(), List.of());
		SearchMemberTier tier = new SearchMemberTier("normal", 10L, 90, "银卡");
		assertThat(calculator.effectivePriceFen(doc, tier, NOW)).isEqualTo(1500);
	}

	@Test
	void tierDiscountWhenNoCustom() {
		GoodsSearchDocument doc = doc(100, Map.of(), Map.of(), List.of());
		SearchMemberTier tier = new SearchMemberTier("normal", 10L, 90, "银卡");
		// 与 MemberTierDiscountPriceTest：sale 100、discount 90 → 优惠 90 分、实付 10 分
		assertThat(calculator.effectivePriceFen(doc, tier, NOW))
				.isEqualTo(MemberTierDiscountPrice.memberPriceFromTierDiscount(100, 90));
	}

	@Test
	void saleWhenNoMember() {
		GoodsSearchDocument doc = doc(2000, Map.of(), Map.of(), List.of());
		SearchMemberTier tier = new SearchMemberTier("normal", 10L, 100, "银卡");
		assertThat(calculator.effectivePriceFen(doc, tier, NOW)).isEqualTo(2000);
	}

	@Test
	void tierDiscountAlignsMemberTierDiscountPriceBoundary() {
		assertThat(MemberTierDiscountPrice.memberPriceFromTierDiscount(1, 90)).isEqualTo(1);
		GoodsSearchDocument doc = doc(1, Map.of(), Map.of(), List.of());
		SearchMemberTier tier = new SearchMemberTier("normal", 1L, 90, "默认");
		assertThat(calculator.effectivePriceFen(doc, tier, NOW)).isEqualTo(1);
	}

	@Test
	void vipTierUsesVipGradeBucket() {
		GoodsSearchDocument doc = doc(2000, Map.of(5L, 999L), Map.of(20L, 1200L), List.of());
		SearchMemberTier tier = new SearchMemberTier("vip", 20L, 90, "VIP");
		assertThat(calculator.effectivePriceFen(doc, tier, NOW)).isEqualTo(1200);
	}

	private static GoodsSearchDocument doc(
			long sale,
			Map<Long, Long> grade,
			Map<Long, Long> vipGrade,
			List<GoodsSearchDocument.Activity> activities) {
		return new GoodsSearchDocument(
				"1_1_1",
				1L,
				1L,
				1L,
				"",
				"",
				"",
				"",
				List.of(),
				"onsale",
				"approved",
				List.of(),
				sale,
				0L,
				0L,
				0L,
				new GoodsSearchDocument.MemberPrices(grade, vipGrade),
				List.of(),
				activities);
	}
}
