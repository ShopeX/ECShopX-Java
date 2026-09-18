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

package cn.shopex.ecshopx.kaquan.service.discount;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardAppendDistributorIdForListService {

	private static final String SOURCE_TYPE_ADMIN = "admin";
	private static final String SOURCE_TYPE_DISTRIBUTOR = "distributor";

	public void apply(long companyId, List<Map<String, Object>> discountCardList) {
		if (companyId < 1L || discountCardList == null || discountCardList.isEmpty()) {
			return;
		}
		for (Map<String, Object> item : discountCardList) {
			String sourceType = item.get("source_type") == null ? null : String.valueOf(item.get("source_type"));
			if (SOURCE_TYPE_DISTRIBUTOR.equals(sourceType)) {
				Object sid = item.get("source_id");
				if (sid instanceof Number n) {
					item.put("distributor_id", n.longValue());
				} else if (sid != null) {
					try {
						item.put("distributor_id", Long.parseLong(sid.toString().trim()));
					} catch (NumberFormatException e) {
						item.put("distributor_id", null);
					}
				} else {
					item.put("distributor_id", null);
				}
			} else if (SOURCE_TYPE_ADMIN.equals(sourceType)) {
				item.put("distributor_id", 0L);
			} else {
				item.put("distributor_id", null);
			}
		}
	}
}
