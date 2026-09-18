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

package cn.shopex.ecshopx.goods.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.goods.integration.wdterp.WdtErpUploadItemsBatchHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UploadItemsToWdtErpJobHandler implements DispatchHandler {

	private final WdtErpUploadItemsBatchHandler batchHandler;

	public UploadItemsToWdtErpJobHandler(WdtErpUploadItemsBatchHandler batchHandler) {
		this.batchHandler = batchHandler;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = toLong(payload.get("company_id"));
		long distributorId = toLong(payload.get("distributor_id"));
		List<Long> itemIds = parseItemIds(payload.get("item_ids"));
		if (itemIds.isEmpty()) {
			return;
		}
		batchHandler.handleBatch(companyId, itemIds, distributorId);
	}

	private static List<Long> parseItemIds(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null) {
				try {
					out.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
					// skip bad element
				}
			}
		}
		return out;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
