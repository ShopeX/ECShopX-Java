package cn.shopex.ecshopx.common.operatorcart.dto;

import java.util.Set;

/** 购物车 SKU 的券适用范围快照（类目 / 品牌 / 标签）。 */
public record CouponCartItemScope(
		long itemId, long defaultItemId, Long categoryId, Integer brandId, Set<Long> tagIds) {

	public CouponCartItemScope {
		tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
	}
}
