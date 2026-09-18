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

package cn.shopex.ecshopx.common.inventory;

/**
 * 单条订单商品行的库存上下文（扣减/返还判定用）。
 *
 * @param companyId      公司 ID
 * @param itemId         平台/店铺商品 SKU（items.item_id）
 * @param supplierId     订单行 supplier_id，{@code >0} 表示供应商同步商品
 * @param distributorId  订单行 distributor_id
 * @param isTotalStore   是否总部库存（standard 模式下区分 items / distributor_items）
 * @param receiptType    订单 receipt_type，如 logistics / ziti
 * @param supplierItemId 由 items.supplier_item_id 解析，仅 SUPPLIER_ITEMS 目标需要
 */
public record ItemInventoryLineContext(
		long companyId,
		long itemId,
		long supplierId,
		long distributorId,
		boolean isTotalStore,
		String receiptType,
		long supplierItemId) {

	public static ItemInventoryLineContext of(
			long companyId,
			long itemId,
			long supplierId,
			long distributorId,
			boolean isTotalStore,
			String receiptType,
			long supplierItemId) {
		return new ItemInventoryLineContext(
				companyId, itemId, supplierId, distributorId, isTotalStore, receiptType, supplierItemId);
	}

	/** 从 orders_normal_orders_items 行 + 订单 receipt_type 构建（supplier_item_id 由扣减层按需解析）。 */
	public static ItemInventoryLineContext fromOrderLine(
			long companyId,
			long itemId,
			Integer supplierId,
			Long distributorId,
			Boolean isTotalStore,
			String receiptType) {
		long sid = supplierId != null ? supplierId.longValue() : 0L;
		long did = distributorId != null ? distributorId : 0L;
		boolean total = isTotalStore == null || isTotalStore;
		String rt = receiptType != null ? receiptType.trim() : "";
		return of(companyId, itemId, sid, did, total, rt, 0L);
	}
}
