/**
 * Copyright 2019-2026 ShopeX
 */

package cn.shopex.ecshopx.goods.search;

import cn.shopex.ecshopx.goods.domain.Items;

public final class DefaultItemIdResolver {

	private DefaultItemIdResolver() {}

	public static long resolve(Items item) {
		if (item == null || item.getItemId() == null) {
			return 0L;
		}
		if (item.getDefaultItemId() != null && item.getDefaultItemId() > 0) {
			return item.getDefaultItemId();
		}
		return item.getItemId();
	}
}
