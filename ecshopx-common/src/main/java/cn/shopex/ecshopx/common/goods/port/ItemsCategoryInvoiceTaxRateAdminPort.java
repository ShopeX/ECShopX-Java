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

package cn.shopex.ecshopx.common.goods.port;

import java.util.Collection;

/**
 * Admin-side updates to {@code items_category} invoice tax fields, implemented in {@code ecshopx-goods} to avoid a
 * Maven dependency cycle with {@code ecshopx-orders}.
 */
public interface ItemsCategoryInvoiceTaxRateAdminPort {

	/**
	 * When {@code encodedCategoryIdsJson} is null or empty, returns immediately. Otherwise checks rows where
	 * {@code category_id} equals the given literal string (encoded JSON form) and {@code invoice_tax_rate_id} is
	 * positive; throws if any match.
	 */
	void assertNoOccupiedCategories(long companyId, String encodedCategoryIdsJson);

	/**
	 * When {@code categoryIds} is null or empty, returns immediately. Otherwise ensures no row has
	 * {@code invoice_tax_rate_id} positive for the given categories, except rows already tied to
	 * {@code currentTaxRateId} when that id is positive.
	 */
	void assertCategoriesNotBoundToOtherTaxRate(long companyId, Collection<Long> categoryIds, long currentTaxRateId);

	/**
	 * Clears invoice tax fields for rows matching {@code companyId} and {@code invoiceTaxRateId}. Zero rows updated is
	 * allowed.
	 */
	void clearInvoiceTaxFieldsForInvoiceTaxRateId(long companyId, long invoiceTaxRateId);

	/**
	 * Clears invoice tax fields for rows matching {@code companyId} and {@code category_id IN (categoryIds)}. When
	 * {@code categoryIds} is null or empty, or no matching rows exist, throws {@code ResourceException("未查询到更新数据")}.
	 */
	void clearInvoiceTaxFieldsForCategoryIds(long companyId, Collection<Long> categoryIds);

	/**
	 * Applies invoice tax rate to each category id. When {@code categoryIds} is null or empty, throws
	 * {@code ResourceException("未查询到更新数据")} immediately. Otherwise patches each row.
	 */
	void applyInvoiceTaxRateToCategories(long companyId, Collection<Long> categoryIds, long invoiceTaxRateId,
			String invoiceTaxRate);
}
