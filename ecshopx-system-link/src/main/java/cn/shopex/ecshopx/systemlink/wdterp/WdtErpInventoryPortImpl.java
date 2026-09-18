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

package cn.shopex.ecshopx.systemlink.wdterp;

import cn.shopex.ecshopx.common.port.wdterp.WdtErpInventoryPort;
import cn.shopex.ecshopx.common.port.wdterp.dto.WdtInventoryWaitSyncPage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
@Profile("!test-cron")
@Service
@RequiredArgsConstructor
@Slf4j
public class WdtErpInventoryPortImpl implements WdtErpInventoryPort {

	private static final int WAIT_SYNC_COUNT = 100;
	private static final String METHOD_STOCK_GET_WAIT_SYNC = "sales.StockSync.getSelfWaitSyncIdListOpen";
	private static final String METHOD_STORE_QUERY = "sales.StockSync.calcStock";
	private static final String METHOD_STORE_SYNC_SUCCESS = "sales.StockSync.syncSuccess";
	private static final String METHOD_STORE_SYNC_FAIL = "sales.StockSync.syncFail";

	private final WdtErpOpenApiClient wdtErpOpenApiClient;

	@Value("${ecshopx.wdterp.methods.stock-get-wait-sync:" + METHOD_STOCK_GET_WAIT_SYNC + "}")
	private String methodStockGetWaitSync;

	@Value("${ecshopx.wdterp.methods.store-query:" + METHOD_STORE_QUERY + "}")
	private String methodStoreQuery;

	@Value("${ecshopx.wdterp.methods.store-sync-success:" + METHOD_STORE_SYNC_SUCCESS + "}")
	private String methodStoreSyncSuccess;

	@Value("${ecshopx.wdterp.methods.store-sync-fail:" + METHOD_STORE_SYNC_FAIL + "}")
	private String methodStoreSyncFail;

	@Override
	@SuppressWarnings("unchecked")
	public WdtInventoryWaitSyncPage fetchWaitSyncPage(
			long companyId, int position, String sid, String appKey, String appSecret) {
		try {
			Object raw = wdtErpOpenApiClient.call(
					companyId, methodStockGetWaitSync, List.of(WAIT_SYNC_COUNT, position), sid, appKey, appSecret);
			if (raw instanceof Map<?, ?> m) {
				if (m.containsKey("fail_msg")) {
					return new WdtInventoryWaitSyncPage(List.of(), position);
				}
				return parseWaitPage(m, position);
			}
		} catch (Exception e) {
			log.debug("旺店通请求失败:{}", e.getMessage());
		}
		return new WdtInventoryWaitSyncPage(List.of(), position);
	}

	private static WdtInventoryWaitSyncPage parseWaitPage(Map<?, ?> body, int fallbackPos) {
		Object idList = body.get("id_list");
		int next = body.get("position") != null ? toIntOrAny(body.get("position"), fallbackPos) : fallbackPos;
		List<String> recIds = new ArrayList<>();
		if (idList instanceof List<?> list) {
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				recIds.add(String.valueOf(o).trim());
			}
		}
		return new WdtInventoryWaitSyncPage(recIds, next);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> queryStore(
			long companyId, String recId, String sid, String appKey, String appSecret) {
		try {
			Object raw = wdtErpOpenApiClient.call(companyId, methodStoreQuery, List.of(recId, false), sid, appKey, appSecret);
			if (raw instanceof Map<?, ?> m && m.containsKey("fail_msg")) {
				return Map.of();
			}
			if (raw instanceof Map<?, ?> m) {
				if (m.containsKey("data") && m.get("data") instanceof Map<?, ?> inner) {
					return (Map<String, Object>) (Map<?, ?>) inner;
				}
				return (Map<String, Object>) (Map<?, ?>) m;
			}
		} catch (Exception e) {
			log.debug("旺店通请求失败:{}", e.getMessage());
		}
		return Map.of();
	}

	@Override
	public void acknowledgeSuccess(
			long companyId, String recId, Map<String, Object> stockInfo, String sid, String appKey, String appSecret) {
		invokeAck(companyId, recId, stockInfo, true, methodStoreSyncSuccess, sid, appKey, appSecret);
	}

	@Override
	public void acknowledgeFail(
			long companyId, String recId, Map<String, Object> stockInfo, String sid, String appKey, String appSecret) {
		invokeAck(companyId, recId, stockInfo, false, methodStoreSyncFail, sid, appKey, appSecret);
	}

	@SuppressWarnings("unchecked")
	private void invokeAck(
			long companyId,
			String recId,
			Map<String, Object> stockInfo,
			boolean result,
			String method,
			String sid,
			String appKey,
			String appSecret) {
		try {
			Map<String, Object> info = buildAckInfo(stockInfo, result);
			Object out = wdtErpOpenApiClient.call(companyId, method, List.of(recId, info), sid, appKey, appSecret);
			if (out instanceof Map<?, ?> m && m.containsKey("fail_msg")) {
				log.debug("旺店通请求失败:{}", m.get("fail_msg"));
			}
		} catch (Exception e) {
			log.debug("旺店通请求失败:{}", e.getMessage());
		}
	}

	private static Map<String, Object> buildAckInfo(Map<String, Object> stockInfo, boolean result) {
		Map<String, Object> info = new LinkedHashMap<>();
		copyKey(stockInfo, info, "syn_stock");
		copyKey(stockInfo, info, "stock_change_count");
		copyKey(stockInfo, info, "stock_syn_rule_id");
		copyKey(stockInfo, info, "stock_syn_rule_no");
		info.put("stock_syn_other", "");
		copyKey(stockInfo, info, "stock_syn_warehouses");
		copyKey(stockInfo, info, "stock_syn_mask");
		copyKey(stockInfo, info, "stock_syn_percent");
		copyKey(stockInfo, info, "stock_syn_plus");
		copyKey(stockInfo, info, "stock_syn_min");
		copyKey(stockInfo, info, "stock_syn_max");
		copyKey(stockInfo, info, "is_auto_listing");
		copyKey(stockInfo, info, "is_auto_delisting");
		info.put("is_syn_success", result ? 1 : 0);
		info.put("is_manual", 1);
		info.put("syn_result", result ? "库存同步成功" : "库存同步失败");
		return info;
	}

	private static void copyKey(Map<String, Object> from, Map<String, Object> to, String key) {
		if (from != null && from.containsKey(key)) {
			to.put(key, from.get(key));
		}
	}

	private static int toIntOrAny(Object v, int dflt) {
		if (v == null) {
			return dflt;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}
}
