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

import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformProductSyncPort;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D3：商品同步合并派发。 */
@Service
@Primary
public class ProductSyncDispatchService implements ShuyunOpenPlatformProductSyncPort {

	private static final Logger log = LoggerFactory.getLogger(ProductSyncDispatchService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OpenPlatformDispatchDedupeService dedupeService;
	private final ShuyunOpenPlatformProperties properties;
	private final ProductSyncService productSyncService;

	public ProductSyncDispatchService(
			OpenPlatformConfigService openPlatformConfigService,
			OpenPlatformDispatchDedupeService dedupeService,
			ShuyunOpenPlatformProperties properties,
			ProductSyncService productSyncService) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.dedupeService = dedupeService;
		this.properties = properties;
		this.productSyncService = productSyncService;
	}

	@Override
	public void dispatchIfAuthAllows(long companyId, long distributorId, long defaultItemId) {
		if (companyId < 1 || distributorId < 1 || defaultItemId < 1) {
			return;
		}
		var row = openPlatformConfigService.findByCompanyId(companyId);
		if (row != null && !StringUtils.hasText(row.getAuthValue())) {
			log.info(
					"Shuyun open platform product sync: skip dispatch, empty auth_value. companyId={} distributorId={} defaultItemId={}",
					companyId,
					distributorId,
					defaultItemId);
			return;
		}
		String mergeKey =
				OrderSyncDispatchCacheKeys.productSyncMergeKey(companyId, distributorId, defaultItemId);
		int ttl = Math.max(0, properties.getMergeDispatchTtlSeconds());
		if (ttl > 0 && !dedupeService.tryAcquire(mergeKey, ttl)) {
			log.info(
					"Shuyun open platform product sync: skip dispatch, merged within TTL. companyId={} distributorId={} defaultItemId={}",
					companyId,
					distributorId,
					defaultItemId);
			return;
		}
		log.info(
				"Shuyun open platform product sync: job dispatched. companyId={} distributorId={} defaultItemId={}",
				companyId,
				distributorId,
				defaultItemId);
		productSyncService.syncProductByDefaultItem(companyId, distributorId, defaultItemId);
	}
}
