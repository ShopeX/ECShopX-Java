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

package cn.shopex.ecshopx.common.dispatch;

public final class GoodsBundleDispatchJobNames {

	public static final String MEDICINE_ITEMS_SUBMIT_AUDIT =
			"job:72:GoodsBundle\\Jobs\\MedicineItemsSubmitAudit";

	public static final String ITEM_BATCH_EDIT_STATUS_EVENT_JOB =
			"job:74:GoodsBundle\\Jobs\\ItemBatchEditStatusEventJob";

	public static final String GET_ITEMS_FROM_OME =
			"job:75:SystemLinkBundle\\Jobs\\GetItemsFromOme";

	public static final String GET_ITEMS_CATEGORY_FROM_OME =
			"job:76:SystemLinkBundle\\Jobs\\GetItemsCategoryFromOme";

	public static final String GET_ITEMS_SPEC_FROM_OME =
			"job:77:SystemLinkBundle\\Jobs\\GetItemsSpecFromOme";

	public static final String GET_BRAND_FROM_OME =
			"job:78:SystemLinkBundle\\Jobs\\GetBrandFromOme";

	public static final String UPLOAD_ITEMS_TO_WDT_ERP =
			"job:79:SystemLinkBundle\\Jobs\\UploadItemsToWdtErpJob";

	public static final String UPLOAD_ITEMS_TO_JUSHUITAN =
			"job:82:SystemLinkBundle\\Jobs\\UploadItemsToJushuitanJob";

	public static final String INVENTORY_QUERY_FROM_JUSHUITAN =
			"job:84:SystemLinkBundle\\Jobs\\InventoryQueryFromJushuitanJob";

	private GoodsBundleDispatchJobNames() {
	}
}
