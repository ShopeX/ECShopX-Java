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

package cn.shopex.ecshopx.espier.service.upload.headerinfo;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderColumnInfo;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对齐 PHP {@code ActivityItemsUploadService}：按是否共享库存动态插入「活动库存」列。
 */
public final class EmployeePurchaseActivityItemsHeaderInfo {
	private EmployeePurchaseActivityItemsHeaderInfo() {}

	public static UploadHeaderTitle title(boolean ifShareStore) {
		Map<String, String> all = new LinkedHashMap<>();
		Map<String, String> need = new LinkedHashMap<>();
		Map<String, UploadHeaderColumnInfo> info = new LinkedHashMap<>();

		all.put("SKU编码", "item_bn");
		need.put("SKU编码", "item_bn");
		info.put("SKU编码", new UploadHeaderColumnInfo(20, "SKU编码", true));

		all.put("活动价格", "activity_price");
		need.put("活动价格", "activity_price");
		info.put("活动价格", new UploadHeaderColumnInfo(20, "活动价格", true));

		if (!ifShareStore) {
			all.put("活动库存", "activity_store");
			need.put("活动库存", "activity_store");
			info.put("活动库存", new UploadHeaderColumnInfo(20, "活动库存", true));
		}

		all.put("限购数量", "limit_num");
		info.put("限购数量", new UploadHeaderColumnInfo(20, "限购数量", false));

		all.put("限购金额", "limit_fee");
		info.put("限购金额", new UploadHeaderColumnInfo(20, "限购金额", false));

		all.put("状态", "shelf_status");
		info.put("状态", new UploadHeaderColumnInfo(5, "1-上架，0-下架，留空默认上架", false));

		return new UploadHeaderTitle(
				Collections.unmodifiableMap(all),
				Collections.unmodifiableMap(need),
				Collections.unmodifiableMap(info));
	}

	/** @deprecated 请用 {@link #title(boolean)} */
	public static Map<String, UploadHeaderColumnInfo> map() {
		return title(false).headerInfo();
	}
}
