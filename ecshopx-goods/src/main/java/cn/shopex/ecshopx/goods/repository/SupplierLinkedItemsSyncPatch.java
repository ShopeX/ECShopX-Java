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

package cn.shopex.ecshopx.goods.repository;

/**
 * 供应商编辑已关联 {@code items} 行时与 PHP {@code SupplierItemsService::createItems} 白名单对齐的字段集合。
 */
public record SupplierLinkedItemsSyncPatch(
		int store,
		String pics,
		String approveStatus,
		String itemName,
		Integer costPrice,
		Integer startNum,
		String auditStatus,
		String auditReason,
		Integer auditDate) {

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private int store;
		private String pics;
		private String approveStatus;
		private String itemName;
		private Integer costPrice;
		private Integer startNum;
		private String auditStatus;
		private String auditReason;
		private Integer auditDate;

		public Builder store(int store) {
			this.store = store;
			return this;
		}

		public Builder pics(String pics) {
			this.pics = pics;
			return this;
		}

		public Builder approveStatus(String approveStatus) {
			this.approveStatus = approveStatus;
			return this;
		}

		public Builder itemName(String itemName) {
			this.itemName = itemName;
			return this;
		}

		public Builder costPrice(Integer costPrice) {
			this.costPrice = costPrice;
			return this;
		}

		public Builder startNum(Integer startNum) {
			this.startNum = startNum;
			return this;
		}

		public Builder auditStatus(String auditStatus) {
			this.auditStatus = auditStatus;
			return this;
		}

		public Builder auditReason(String auditReason) {
			this.auditReason = auditReason;
			return this;
		}

		public Builder auditDate(Integer auditDate) {
			this.auditDate = auditDate;
			return this;
		}

		public SupplierLinkedItemsSyncPatch build() {
			return new SupplierLinkedItemsSyncPatch(
					store, pics, approveStatus, itemName, costPrice, startNum, auditStatus, auditReason, auditDate);
		}
	}
}
