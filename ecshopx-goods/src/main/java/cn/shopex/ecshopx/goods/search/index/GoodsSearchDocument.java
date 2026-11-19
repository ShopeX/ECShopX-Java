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

package cn.shopex.ecshopx.goods.search.index;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 写入 ES 的商品搜索文档形状（与 {@link GoodsSearchMappingService} mapping 对齐）。
 */
public record GoodsSearchDocument(
		String id,
		long companyId,
		long distributorId,
		long defaultItemId,
		String itemName,
		String brief,
		String itemBn,
		String barcode,
		List<String> tags,
		String approveStatus,
		String auditStatus,
		List<Long> categoryIds,
		long salePrice,
		long marketPrice,
		long sort,
		long sales,
		long listedAt,
		MemberPrices memberPrices,
		List<Sku> skus,
		List<Activity> activities,
		ListAttrs listAttrs) {

	private static final Set<String> SELLABLE_SKU_STATUSES = Set.of("onsale", "only_show");

	public record MemberPrices(Map<Long, Long> grade, Map<Long, Long> vipGrade) {}

	public record Sku(long skuId, long store, String approveStatus) {}

	public record Activity(
			String type, long activityId, long activityPrice, long startAt, long endAt, int priority) {}

	/**
	 * 与微信小程序默认商品列表的数据库筛选列对齐，供 ES filter 使用。
	 * {@code storeListable}：店铺自有，或该默认商品在上架表 {@code goods_can_sale = 1}。
	 */
	public record ListAttrs(
			String itemType,
			boolean gift,
			boolean defaultItem,
			long brandId,
			int itemCrossType,
			String regionsId,
			int rebate,
			String rebateType,
			long goodsId,
			boolean storeListable,
			long itemDistributorId) {

		public static ListAttrs empty() {
			return new ListAttrs("", false, false, 0L, 0, "", 0, "", 0L, false, 0L);
		}
	}

	/** 任一 SKU {@code store > 0} 且处于前台可售状态则为 true。 */
	public boolean hasStock() {
		return skus.stream()
				.anyMatch(sku -> sku.store() > 0 && SELLABLE_SKU_STATUSES.contains(sku.approveStatus()));
	}
}
