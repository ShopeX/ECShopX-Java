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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.config.AmapTrackProperties;
import cn.shopex.ecshopx.thirdparty.domain.MapConfig;
import cn.shopex.ecshopx.thirdparty.mapper.MapConfigMapper;
import cn.shopex.ecshopx.thirdparty.service.cache.ThirdPartyRedisPreventionCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class MapConfigAdminService {

	public static final int DEFAULT_YES = 1;
	public static final int DEFAULT_NO = 0;

	private static final String TYPE_AMAP = "amap";
	private static final String TYPE_TENCENT = "tencent";

	private static final Logger log = LoggerFactory.getLogger(MapConfigAdminService.class);

	private final MapConfigMapper mapConfigMapper;
	private final TransactionTemplate transactionTemplate;
	private final AmapTrackProperties amapTrackProperties;
	private final ThirdPartyRedisPreventionCache thirdPartyRedisPreventionCache;

	public MapConfigAdminService(
			MapConfigMapper mapConfigMapper,
			PlatformTransactionManager transactionManager,
			AmapTrackProperties amapTrackProperties,
			ThirdPartyRedisPreventionCache thirdPartyRedisPreventionCache) {
		this.mapConfigMapper = mapConfigMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.amapTrackProperties = amapTrackProperties;
		this.thirdPartyRedisPreventionCache = thirdPartyRedisPreventionCache;
	}

	public Map<String, Object> get(long companyId) {
		long total = mapConfigMapper.selectCount(
				new LambdaQueryWrapper<MapConfig>().eq(MapConfig::getCompanyId, companyId));
		if (total > 0) {
			List<MapConfig> rows = mapConfigMapper.selectList(
					new LambdaQueryWrapper<MapConfig>()
							.eq(MapConfig::getCompanyId, companyId)
							.orderByDesc(MapConfig::getId));
			List<Object> list = new ArrayList<>();
			for (MapConfig e : rows) {
				list.add(toColumnMap(e));
			}
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", total);
			out.put("list", list);
			return out;
		}
		Object defaultItem = loadDefaultConfigWithPreventionCache(companyId);
		List<Object> list = new ArrayList<>(1);
		list.add(defaultItem);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 1L);
		out.put("list", list);
		return out;
	}

	private Object loadDefaultConfigPayload(long companyId) {
		return thirdPartyRedisPreventionCache.getByPrevention(
				companyId, "map_default_info", 60, () -> computeDefaultConfigRowOrEmptyList(companyId));
	}

	private Object loadDefaultConfigWithPreventionCache(long companyId) {
		return loadDefaultConfigPayload(companyId);
	}

	public Optional<MapConfig> resolveDefaultMapConfigForThirdParty(long companyId) {
		Object raw = loadDefaultConfigPayload(companyId);
		if (raw == null) {
			return Optional.empty();
		}
		if (raw instanceof List<?>) {
			return Optional.empty();
		}
		if (raw instanceof Map<?, ?> m) {
			long id = parseConfigId(m.get("id"));
			if (id <= 0L) {
				return Optional.empty();
			}
			MapConfig entity = mapConfigMapper.selectById(id);
			return Optional.ofNullable(entity);
		}
		return Optional.empty();
	}

	private static long parseConfigId(Object idObj) {
		if (idObj == null) {
			return 0L;
		}
		if (idObj instanceof Number n) {
			return n.longValue();
		}
		if (idObj instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	public Map<String, Object> defaultAmapRowForFront(long companyId) {
		Object raw = loadDefaultConfigPayload(companyId);
		if (raw == null) {
			return Collections.emptyMap();
		}
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return Collections.emptyMap();
			}
			throw new IllegalStateException("unexpected default map cache payload: " + raw.getClass());
		}
		if (raw instanceof Map<?, ?> m) {
			return copyToLinkedStringKeyMap(m);
		}
		throw new IllegalStateException("unexpected default map cache payload: " + raw.getClass());
	}

	private LinkedHashMap<String, Object> copyToLinkedStringKeyMap(Map<?, ?> source) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(source.size());
		for (Map.Entry<?, ?> e : source.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private Object computeDefaultConfigRowOrEmptyList(long companyId) {
		MapConfig row = mapConfigMapper.selectOne(
				new LambdaQueryWrapper<MapConfig>()
						.eq(MapConfig::getCompanyId, companyId)
						.eq(MapConfig::getIsDefault, true)
						.last("LIMIT 1"));
		if (row != null) {
			return toColumnMap(row);
		}
		String key = amapTrackProperties.getAppKey() == null ? "" : amapTrackProperties.getAppKey().trim();
		String secret =
				amapTrackProperties.getAppSecret() == null ? "" : amapTrackProperties.getAppSecret().trim();
		if (!StringUtils.hasText(key) || !StringUtils.hasText(secret)) {
			return Collections.emptyList();
		}
		MapConfig n = new MapConfig();
		n.setCompanyId(companyId);
		n.setType(TYPE_AMAP);
		n.setAppKey(key);
		n.setAppSecret(secret);
		n.setIsDefault(true);
		int now = epochSeconds();
		n.setCreated(now);
		n.setUpdated(now);
		mapConfigMapper.insert(n);
		MapConfig inserted = mapConfigMapper.selectById(n.getId());
		return toColumnMap(inserted);
	}

	public Optional<Map<String, Object>> set(
			long companyId,
			String mapType,
			String appKey,
			String appSecret,
			int isDefault) {
		return transactionTemplate.execute(status -> {
			try {
				return Optional.of(applyMapConfigChange(companyId, mapType, appKey, appSecret, isDefault));
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				status.setRollbackOnly();
				StackTraceElement[] stack = e.getStackTrace();
				String file = stack.length > 0 ? stack[0].getFileName() : "";
				int line = stack.length > 0 ? stack[0].getLineNumber() : 0;
				log.info("message={}, file={}, line={}", e.getMessage(), file, line, e);
				return Optional.empty();
			}
		});
	}

	private Map<String, Object> applyMapConfigChange(
			long companyId,
			String mapType,
			String appKey,
			String appSecret,
			int isDefault) {
		if (isDefault == DEFAULT_YES) {
			mapConfigMapper.update(
					null,
					new LambdaUpdateWrapper<MapConfig>()
							.eq(MapConfig::getCompanyId, companyId)
							.set(MapConfig::getIsDefault, false));
		} else {
			String otherType = TYPE_TENCENT.equalsIgnoreCase(mapType) ? TYPE_AMAP : TYPE_TENCENT;
			mapConfigMapper.update(
					null,
					new LambdaUpdateWrapper<MapConfig>()
							.eq(MapConfig::getCompanyId, companyId)
							.eq(MapConfig::getType, otherType)
							.set(MapConfig::getIsDefault, true));
		}

		long cnt = mapConfigMapper.selectCount(
				new LambdaQueryWrapper<MapConfig>()
						.eq(MapConfig::getCompanyId, companyId)
						.eq(MapConfig::getType, mapType));

		if (cnt > 0) {
			MapConfig entity = mapConfigMapper.selectOne(
					new LambdaQueryWrapper<MapConfig>()
							.eq(MapConfig::getCompanyId, companyId)
							.eq(MapConfig::getType, mapType)
							.last("LIMIT 1"));
			if (entity == null) {
				throw new ResourceException("未查询到更新数据");
			}
			entity.setAppKey(appKey);
			entity.setAppSecret(appSecret == null ? "" : appSecret);
			entity.setIsDefault(isDefault == DEFAULT_YES);
			entity.setUpdated(epochSeconds());
			mapConfigMapper.updateById(entity);
			MapConfig refreshed = mapConfigMapper.selectById(entity.getId());
			return toColumnMap(refreshed);
		}

		MapConfig n = new MapConfig();
		n.setCompanyId(companyId);
		n.setType(mapType);
		n.setAppKey(appKey);
		n.setAppSecret(appSecret == null ? "" : appSecret);
		n.setIsDefault(isDefault == DEFAULT_YES);
		int now = epochSeconds();
		n.setCreated(now);
		n.setUpdated(now);
		mapConfigMapper.insert(n);
		MapConfig inserted = mapConfigMapper.selectById(n.getId());
		return toColumnMap(inserted);
	}

	private static Map<String, Object> toColumnMap(MapConfig e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("type", e.getType() == null ? "" : e.getType());
		m.put("app_key", e.getAppKey() == null ? "" : e.getAppKey());
		m.put("app_secret", e.getAppSecret() == null ? "" : e.getAppSecret());
		int def = e.getIsDefault() != null && e.getIsDefault() ? 1 : 0;
		m.put("is_default", def);
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}

	private static int epochSeconds() {
		return (int) Instant.now().getEpochSecond();
	}
}
