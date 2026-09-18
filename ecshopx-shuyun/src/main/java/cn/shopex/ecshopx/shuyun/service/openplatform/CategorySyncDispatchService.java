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

import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformCategorySyncPort;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D2：类目同步合并派发。 */
@Service
@Primary
public class CategorySyncDispatchService implements ShuyunOpenPlatformCategorySyncPort {

	private static final Logger log = LoggerFactory.getLogger(CategorySyncDispatchService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OpenPlatformDispatchDedupeService dedupeService;
	private final ShuyunOpenPlatformProperties properties;
	private final CategorySyncService categorySyncService;

	public CategorySyncDispatchService(
			OpenPlatformConfigService openPlatformConfigService,
			OpenPlatformDispatchDedupeService dedupeService,
			ShuyunOpenPlatformProperties properties,
			CategorySyncService categorySyncService) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.dedupeService = dedupeService;
		this.properties = properties;
		this.categorySyncService = categorySyncService;
	}

	@Override
	public void dispatchIfAuthAllows(long companyId, long categoryId) {
		if (companyId < 1 || categoryId < 1) {
			return;
		}
		var row = openPlatformConfigService.findByCompanyId(companyId);
		if (row != null && !StringUtils.hasText(row.getAuthValue())) {
			log.info(
					"Shuyun open platform category sync: skip dispatch, empty auth_value. companyId={} categoryId={}",
					companyId,
					categoryId);
			return;
		}
		String mergeKey = OrderSyncDispatchCacheKeys.categorySyncMergeKey(companyId, categoryId);
		int ttl = Math.max(0, properties.getMergeDispatchTtlSeconds());
		if (ttl > 0 && !dedupeService.tryAcquire(mergeKey, ttl)) {
			log.info(
					"Shuyun open platform category sync: skip dispatch, merged within TTL. companyId={} categoryId={}",
					companyId,
					categoryId);
			return;
		}
		log.info(
				"Shuyun open platform category sync: job dispatched. companyId={} categoryId={}",
				companyId,
				categoryId);
		categorySyncService.syncCategory(companyId, categoryId);
	}
}
