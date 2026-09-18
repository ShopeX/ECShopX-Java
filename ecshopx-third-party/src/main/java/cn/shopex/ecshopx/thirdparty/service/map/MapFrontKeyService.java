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

package cn.shopex.ecshopx.thirdparty.service.map;

import cn.shopex.ecshopx.thirdparty.domain.MapConfig;
import cn.shopex.ecshopx.thirdparty.mapper.MapConfigMapper;
import cn.shopex.ecshopx.thirdparty.service.cache.ThirdPartyRedisPreventionCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MapFrontKeyService {

	private static final String TYPE_AMAP = "amap";

	private final ThirdPartyRedisPreventionCache thirdPartyRedisPreventionCache;
	private final MapConfigMapper mapConfigMapper;
	private final MapConfigAdminService mapConfigAdminService;

	public MapFrontKeyService(
			ThirdPartyRedisPreventionCache thirdPartyRedisPreventionCache,
			MapConfigMapper mapConfigMapper,
			MapConfigAdminService mapConfigAdminService) {
		this.thirdPartyRedisPreventionCache = thirdPartyRedisPreventionCache;
		this.mapConfigMapper = mapConfigMapper;
		this.mapConfigAdminService = mapConfigAdminService;
	}

	public Map<String, Object> get(long companyId, String typeForCacheKey) {
		String cacheName =
				"third_party_map_"
						+ (typeForCacheKey == null || typeForCacheKey.isBlank()
								? TYPE_AMAP
								: typeForCacheKey.trim());
		Object raw = thirdPartyRedisPreventionCache.getByPrevention(
				companyId, cacheName, 60, () -> loadFirstAmapRowOrEmptyMap(companyId));

		Map<String, Object> row;
		if (raw == null) {
			row = Collections.emptyMap();
		} else if (raw instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>(m.size());
			for (Map.Entry<?, ?> e : m.entrySet()) {
				copy.put(String.valueOf(e.getKey()), e.getValue());
			}
			row = copy;
		} else {
			row = Collections.emptyMap();
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>(3);
		out.put("type", row.get("type"));
		out.put("key", row.get("app_key"));
		out.put("is_default", row.get("is_default"));
		return out;
	}

	private Map<String, Object> loadFirstAmapRowOrEmptyMap(long companyId) {
		long cnt = mapConfigMapper.selectCount(
				new LambdaQueryWrapper<MapConfig>()
						.eq(MapConfig::getCompanyId, companyId)
						.eq(MapConfig::getType, TYPE_AMAP));
		if (cnt > 0) {
			List<MapConfig> rows = mapConfigMapper.selectList(
					new LambdaQueryWrapper<MapConfig>()
							.eq(MapConfig::getCompanyId, companyId)
							.eq(MapConfig::getType, TYPE_AMAP)
							.orderByDesc(MapConfig::getId));
			if (rows == null || rows.isEmpty()) {
				return Collections.emptyMap();
			}
			MapConfig first = rows.get(0);
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("type", first.getType());
			m.put("app_key", first.getAppKey());
			m.put("is_default", first.getIsDefault() != null && first.getIsDefault() ? 1 : 0);
			return m;
		}
		return mapConfigAdminService.defaultAmapRowForFront(companyId);
	}
}
