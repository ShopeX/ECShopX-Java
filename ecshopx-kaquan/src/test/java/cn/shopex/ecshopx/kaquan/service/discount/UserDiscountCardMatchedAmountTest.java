package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDiscountCardMatchedAmountTest {

	private static final long TAGGED_ITEM = 2614L;
	private static final long UNTAGGED_ITEM = 2446L;
	private static final long TAG_ID = 1001L;

	@Test
	@DisplayName("指定标签券只累计活动商品金额，不含非标签商品")
	void tagBoundCountsOnlyTaggedItemFee() {
		Map<Long, Long> fees = new LinkedHashMap<>();
		fees.put(TAGGED_ITEM, 1390L);
		fees.put(UNTAGGED_ITEM, 12000L);
		Map<Long, CouponCartItemScope> scopes = Map.of(
				TAGGED_ITEM, new CouponCartItemScope(TAGGED_ITEM, TAGGED_ITEM, null, null, Set.of(TAG_ID)),
				UNTAGGED_ITEM, new CouponCartItemScope(UNTAGGED_ITEM, UNTAGGED_ITEM, null, null, Set.of()));

		long matched = UserDiscountCardMatchedAmount.feeFen(3, "," + TAG_ID + ",", fees, scopes);

		assertThat(matched).isEqualTo(1390L);
	}

	@Test
	@DisplayName("指定标签券活动商品金额低于门槛时匹配金额仍只含活动商品")
	void tagBoundBelowLeastCostDoesNotUseFullCart() {
		Map<Long, Long> fees = new LinkedHashMap<>();
		fees.put(TAGGED_ITEM, 1390L);
		fees.put(UNTAGGED_ITEM, 12000L);
		Map<Long, CouponCartItemScope> scopes = Map.of(
				TAGGED_ITEM, new CouponCartItemScope(TAGGED_ITEM, TAGGED_ITEM, null, null, Set.of(TAG_ID)),
				UNTAGGED_ITEM, new CouponCartItemScope(UNTAGGED_ITEM, UNTAGGED_ITEM, null, null, Set.of()));

		long matched = UserDiscountCardMatchedAmount.feeFen(3, "," + TAG_ID + ",", fees, scopes);

		assertThat(matched).isLessThan(2000L);
		assertThat(matched).isNotEqualTo(13390L);
	}

	@Test
	@DisplayName("全场券累计全部购物车金额")
	void allBoundCountsFullCart() {
		Map<Long, Long> fees = Map.of(TAGGED_ITEM, 1390L, UNTAGGED_ITEM, 12000L);

		assertThat(UserDiscountCardMatchedAmount.feeFen(0, "all", fees, Map.of())).isEqualTo(13390L);
	}

	@Test
	@DisplayName("指定商品券只累计 rel_item_ids 中的商品")
	void assignItemsBoundCountsListedItems() {
		Map<Long, Long> fees = Map.of(TAGGED_ITEM, 1390L, UNTAGGED_ITEM, 12000L);

		assertThat(UserDiscountCardMatchedAmount.feeFen(1, "," + TAGGED_ITEM + ",", fees, Map.of()))
				.isEqualTo(1390L);
	}
}
