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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物化搜索文档所需的只读领域数据聚合（item、门店价、库存、定制会员价、改价活动行）。
 */
public record SearchIndexSource(
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
		long hqPriceFen,
		Long distributorPriceFen,
		long marketPriceFen,
		Map<Long, Long> memberPriceGrade,
		Map<Long, Long> memberPriceVipGrade,
		List<Sku> skus,
		List<Activity> activities) {

	public record Sku(long skuId, long store, String approveStatus) {}

	public record Activity(
			String type, long activityId, long activityPrice, long startAt, long endAt, Integer priority) {}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private long companyId;
		private long distributorId;
		private long defaultItemId;
		private String itemName = "";
		private String brief = "";
		private String itemBn = "";
		private String barcode = "";
		private List<String> tags = List.of();
		private String approveStatus = "onsale";
		private String auditStatus = "approved";
		private List<Long> categoryIds = List.of();
		private long hqPriceFen;
		private Long distributorPriceFen;
		private long marketPriceFen;
		private Map<Long, Long> memberPriceGrade = new HashMap<>();
		private Map<Long, Long> memberPriceVipGrade = new HashMap<>();
		private List<Sku> skus = new ArrayList<>();
		private List<Activity> activities = new ArrayList<>();

		public Builder companyId(long companyId) {
			this.companyId = companyId;
			return this;
		}

		public Builder distributorId(long distributorId) {
			this.distributorId = distributorId;
			return this;
		}

		public Builder defaultItemId(long defaultItemId) {
			this.defaultItemId = defaultItemId;
			return this;
		}

		public Builder itemName(String itemName) {
			this.itemName = itemName != null ? itemName : "";
			return this;
		}

		public Builder brief(String brief) {
			this.brief = brief != null ? brief : "";
			return this;
		}

		public Builder itemBn(String itemBn) {
			this.itemBn = itemBn != null ? itemBn : "";
			return this;
		}

		public Builder barcode(String barcode) {
			this.barcode = barcode != null ? barcode : "";
			return this;
		}

		public Builder tags(List<String> tags) {
			this.tags = tags != null ? List.copyOf(tags) : List.of();
			return this;
		}

		public Builder approveStatus(String approveStatus) {
			this.approveStatus = approveStatus != null ? approveStatus : "onsale";
			return this;
		}

		public Builder auditStatus(String auditStatus) {
			this.auditStatus = auditStatus != null ? auditStatus : "approved";
			return this;
		}

		public Builder categoryIds(List<Long> categoryIds) {
			this.categoryIds = categoryIds != null ? List.copyOf(categoryIds) : List.of();
			return this;
		}

		public Builder hqPriceFen(long hqPriceFen) {
			this.hqPriceFen = hqPriceFen;
			return this;
		}

		public Builder distributorPriceFen(long distributorPriceFen) {
			this.distributorPriceFen = distributorPriceFen;
			return this;
		}

		public Builder marketPriceFen(long marketPriceFen) {
			this.marketPriceFen = marketPriceFen;
			return this;
		}

		public Builder memberPriceGrade(Map<Long, Long> memberPriceGrade) {
			this.memberPriceGrade = new HashMap<>(memberPriceGrade);
			return this;
		}

		public Builder memberPriceVipGrade(Map<Long, Long> memberPriceVipGrade) {
			this.memberPriceVipGrade = new HashMap<>(memberPriceVipGrade);
			return this;
		}

		public Builder skus(List<Sku> skus) {
			this.skus = new ArrayList<>(skus);
			return this;
		}

		public Builder activities(List<Activity> activities) {
			this.activities = new ArrayList<>(activities);
			return this;
		}

		public SearchIndexSource build() {
			return new SearchIndexSource(
					companyId,
					distributorId,
					defaultItemId,
					itemName,
					brief,
					itemBn,
					barcode,
					List.copyOf(tags),
					approveStatus,
					auditStatus,
					List.copyOf(categoryIds),
					hqPriceFen,
					distributorPriceFen,
					marketPriceFen,
					Collections.unmodifiableMap(new HashMap<>(memberPriceGrade)),
					Collections.unmodifiableMap(new HashMap<>(memberPriceVipGrade)),
					List.copyOf(skus),
					List.copyOf(activities));
		}
	}
}
