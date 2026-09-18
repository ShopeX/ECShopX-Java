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

import cn.shopex.ecshopx.common.port.wdterp.WdtErpLogisticsPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 委托 {@link WdtErpOpenApiClient} 实现旺店通待同步物流拉取与回写；test-cron 下由 Noop 覆盖。
 */
@Profile("!test-cron")
@Service
@RequiredArgsConstructor
@Slf4j
public class WdtErpLogisticsPortImpl implements WdtErpLogisticsPort {

	private static final int PAGE_SIZE = 10;

	private final WdtErpOpenApiClient wdtErpOpenApiClient;

	@Value("${ecshopx.wdterp.methods.logistics-get-wait-sync:sales.LogisticsSync.getSyncListExt}")
	private String methodLogisticsGetWaitSync;

	@Value("${ecshopx.wdterp.methods.logistics-sync-success:sales.LogisticsSync.update}")
	private String methodLogisticsSyncSuccess;

	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getWaitSyncPage(
			long companyId, String shopNo, int pageNo, String sid, String appKey, String appSecret) {
		Map<String, Object> parMap = new LinkedHashMap<>();
		parMap.put("shop_no", shopNo);
		parMap.put("is_own_platform", true);
		try {
			Object raw = wdtErpOpenApiClient.pageCall(
					companyId, methodLogisticsGetWaitSync, PAGE_SIZE, pageNo, true, List.of(parMap), sid, appKey, appSecret);
			if (raw instanceof Map<?, ?> m && m.containsKey("fail_msg")) {
				log.debug("旺店通请求失败:{}", m.get("fail_msg"));
				return new ArrayList<>();
			}
			if (raw instanceof Map<?, ?> root) {
				Object d = root.get("data");
				if (d instanceof List<?> list) {
					List<Map<String, Object>> out = new ArrayList<>();
					for (Object o : list) {
						if (o instanceof Map<?, ?> row) {
							out.add((Map<String, Object>) (Map<?, ?>) row);
						}
					}
					return out;
				}
				return new ArrayList<>();
			}
		} catch (Exception e) {
			log.debug("旺店通请求失败:{}", e.getMessage());
		}
		return new ArrayList<>();
	}

	@Override
	public void acknowledgeSync(
			long companyId, List<Map<String, Object>> items, String sid, String appKey, String appSecret) {
		if (items == null || items.isEmpty()) {
			return;
		}
		try {
			Object out =
					wdtErpOpenApiClient.call(companyId, methodLogisticsSyncSuccess, List.of(items), sid, appKey, appSecret);
			if (out instanceof Map<?, ?> m && m.containsKey("fail_msg")) {
				log.debug("旺店通请求失败:{}", m.get("fail_msg"));
			}
		} catch (Exception e) {
			log.debug("旺店通请求失败:{}", e.getMessage());
		}
	}
}
