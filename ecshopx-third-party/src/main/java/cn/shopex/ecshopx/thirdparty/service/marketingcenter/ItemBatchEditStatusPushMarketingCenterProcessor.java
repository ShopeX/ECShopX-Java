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

import cn.shopex.ecshopx.common.port.goods.ItemBatchEditStatusMarketingSkuRowsPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ItemBatchEditStatusPushMarketingCenterProcessor {

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final ItemBatchEditStatusMarketingSkuRowsPort itemBatchEditStatusMarketingSkuRowsPort;

	public ItemBatchEditStatusPushMarketingCenterProcessor(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			ItemBatchEditStatusMarketingSkuRowsPort itemBatchEditStatusMarketingSkuRowsPort) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.itemBatchEditStatusMarketingSkuRowsPort = itemBatchEditStatusMarketingSkuRowsPort;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		long companyId = readLong(payload.get("company_id"));
		long goodsId = readLong(payload.get("goods_id"));
		if (companyId <= 0L || goodsId <= 0L) {
			return;
		}
		List<Map<String, Object>> rows = itemBatchEditStatusMarketingSkuRowsPort.listRows(companyId, goodsId);
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		int index = 0;
		for (Map<String, Object> row : rows) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>();
			Object bn = row.get("item_bn");
			Object st = row.get("approve_status");
			input.put("item_bn", bn == null ? "" : bn.toString());
			input.put("approve_status", st == null ? "" : st.toString());
			params.put(String.valueOf(index++), input);
		}
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
}
