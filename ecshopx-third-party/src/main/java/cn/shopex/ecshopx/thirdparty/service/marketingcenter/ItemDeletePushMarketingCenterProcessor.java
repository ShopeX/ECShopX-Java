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

package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ItemDeletePushMarketingCenterProcessor {

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	public ItemDeletePushMarketingCenterProcessor(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		long companyId = readLong(payload.get("company_id"));
		if (companyId <= 0L) {
			return;
		}
		List<Long> delIds = readLongList(payload.get("del_ids"));
		if (CollectionUtils.isEmpty(delIds)) {
			return;
		}
		String itemBn = readItemBn(payload.get("item_info"));
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("del_ids", new ArrayList<>(delIds));
		params.put("item_bn", itemBn);
		marketingCenterOpenApiSignedFormClient.basicsItemProccess(companyId, params);
	}

	private static long readLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw != null && StringUtils.hasText(raw.toString())) {
			try {
				return Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static List<Long> readLongList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>(list.size());
		for (Object element : list) {
			if (element instanceof Number n) {
				out.add(n.longValue());
			} else if (element != null && StringUtils.hasText(element.toString())) {
				try {
					out.add(Long.parseLong(element.toString().trim()));
				} catch (NumberFormatException e) {
					// skip invalid element
				}
			}
		}
		return out;
	}

	private static String readItemBn(Object itemInfoRaw) {
		if (!(itemInfoRaw instanceof Map<?, ?> row)) {
			return "";
		}
		Object bn = row.get("item_bn");
		return bn == null ? "" : bn.toString();
	}
}
