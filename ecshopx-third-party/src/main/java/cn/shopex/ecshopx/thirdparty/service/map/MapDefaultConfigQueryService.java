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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MapDefaultConfigQueryService {

	private final MapConfigMapper mapConfigMapper;

	public MapDefaultConfigQueryService(MapConfigMapper mapConfigMapper) {
		this.mapConfigMapper = mapConfigMapper;
	}

	public Map<String, Object> loadConfigInfoMap(long companyId) {
		MapConfig row = mapConfigMapper.selectOne(new LambdaQueryWrapper<MapConfig>()
				.eq(MapConfig::getCompanyId, companyId)
				.eq(MapConfig::getIsDefault, true)
				.orderByAsc(MapConfig::getId)
				.last("LIMIT 1"));
		if (row == null) {
			List<MapConfig> any = mapConfigMapper.selectList(new LambdaQueryWrapper<MapConfig>()
					.eq(MapConfig::getCompanyId, companyId)
					.in(MapConfig::getType, "amap", "tencent")
					.orderByAsc(MapConfig::getId));
			row = firstWithAppKey(any);
		}
		if (row == null) {
			return Map.of();
		}
		return toConfigInfoMap(row);
	}

	private static MapConfig firstWithAppKey(List<MapConfig> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.stream().filter(c -> StringUtils.hasText(c.getAppKey())).findFirst().orElse(null);
	}

	private static Map<String, Object> toConfigInfoMap(MapConfig row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", row.getCompanyId());
		m.put("id", row.getId());
		m.put("app_key", row.getAppKey() == null ? "" : row.getAppKey());
		m.put("app_secret", row.getAppSecret() == null ? "" : row.getAppSecret());
		m.put("type", row.getType() == null ? "" : row.getType());
		return m;
	}
}
