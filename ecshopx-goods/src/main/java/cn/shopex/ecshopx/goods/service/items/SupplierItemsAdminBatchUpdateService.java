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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.supplier.domain.SupplierItemsUpdatePatch;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SupplierItemsAdminBatchUpdateService {

	private final SupplierItemsRepository supplierItemsRepository;

	public SupplierItemsAdminBatchUpdateService(SupplierItemsRepository supplierItemsRepository) {
		this.supplierItemsRepository = supplierItemsRepository;
	}

	public void batchUpdateItems(long companyId, SupplierItemsFilter filter, SupplierItemsUpdatePatch patch) {
		if (patch.isMarketColumnPresent()) {
			if (effectiveTruthy(patch.getIsMarketValueRaw())) {
				filter.setIsMarketEqOrNull(0);
				patch.setApproveStatus("onsale");
				patch.setAuditStatus("processing");
			} else {
				filter.setIsMarketEqOrNull(1);
				patch.setApproveStatus("instock");
				patch.setAuditStatus("processing");
			}
		}
		long n = supplierItemsRepository.countByCompanyGoodsOrItem(
				companyId,
				filter.getGoodsIdsOrNull(),
				filter.getItemIdsOrNull(),
				filter.getIsMarketEqOrNull(),
				filter.getAuditStatusInOrNull());
		if (n == 0) {
			return;
		}
		supplierItemsRepository.updateAllByCompanyGoodsOrItem(
				companyId,
				filter.getGoodsIdsOrNull(),
				filter.getItemIdsOrNull(),
				filter.getIsMarketEqOrNull(),
				filter.getAuditStatusInOrNull(),
				patch);
	}

	/** Loose truthiness for raw is_market values (scalar cast semantics for filter branch). */
	private static boolean effectiveTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			if ("false".equalsIgnoreCase(t)) {
				return false;
			}
			return true;
		}
		return false;
	}

	public static final class SupplierItemsFilter {
		private List<Long> goodsIdsOrNull;
		private List<Long> itemIdsOrNull;
		private Integer isMarketEqOrNull;
		private List<String> auditStatusInOrNull;

		public List<Long> getGoodsIdsOrNull() {
			return goodsIdsOrNull;
		}

		public void setGoodsIdsOrNull(List<Long> goodsIdsOrNull) {
			this.goodsIdsOrNull = goodsIdsOrNull;
		}

		public List<Long> getItemIdsOrNull() {
			return itemIdsOrNull;
		}

		public void setItemIdsOrNull(List<Long> itemIdsOrNull) {
			this.itemIdsOrNull = itemIdsOrNull;
		}

		public Integer getIsMarketEqOrNull() {
			return isMarketEqOrNull;
		}

		public void setIsMarketEqOrNull(Integer isMarketEqOrNull) {
			this.isMarketEqOrNull = isMarketEqOrNull;
		}

		public List<String> getAuditStatusInOrNull() {
			return auditStatusInOrNull;
		}

		public void setAuditStatusInOrNull(List<String> auditStatusInOrNull) {
			this.auditStatusInOrNull = auditStatusInOrNull;
		}
	}
}
