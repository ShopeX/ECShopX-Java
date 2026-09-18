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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShopSyncDispatchService {

	private static final Logger log = LoggerFactory.getLogger(ShopSyncDispatchService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OpenPlatformDispatchDedupeService dedupeService;
	private final ShuyunOpenPlatformProperties properties;
	private final ShopSyncService shopSyncService;

	public ShopSyncDispatchService(
			OpenPlatformConfigService openPlatformConfigService,
			OpenPlatformDispatchDedupeService dedupeService,
			ShuyunOpenPlatformProperties properties,
			ShopSyncService shopSyncService) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.dedupeService = dedupeService;
		this.properties = properties;
		this.shopSyncService = shopSyncService;
	}

	public void dispatchIfAuthAllows(long companyId, long distributorId) {
		if (companyId < 1 || distributorId < 1) {
			return;
		}
		var row = openPlatformConfigService.findByCompanyId(companyId);
		if (row != null && !org.springframework.util.StringUtils.hasText(row.getAuthValue())) {
			log.info(
					"Shuyun open platform shop sync: skip dispatch, empty auth_value. companyId={} distributorId={}",
					companyId,
					distributorId);
			return;
		}
		String mergeKey = OrderSyncDispatchCacheKeys.shopSyncMergeKey(companyId, distributorId);
		int ttl = Math.max(0, properties.getMergeDispatchTtlSeconds());
		if (ttl > 0 && !dedupeService.tryAcquire(mergeKey, ttl)) {
			log.info(
					"Shuyun open platform shop sync: skip dispatch, merged within TTL. companyId={} distributorId={}",
					companyId,
					distributorId);
			return;
		}
		log.info(
				"Shuyun open platform shop sync: job dispatched. companyId={} distributorId={}",
				companyId,
				distributorId);
		shopSyncService.syncShop(companyId, distributorId);
	}
}
