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

package cn.shopex.ecshopx.youshu.service;

import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.integration.YoushuItemsCategoryPushPort;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class YoushuItemCategoryAddSyncService {

	private static final int BATCH_SIZE = 50;

	private final YoushuSettingMapper youshuSettingMapper;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final YoushuItemsCategoryPushPort youshuItemsCategoryPushPort;

	public YoushuItemCategoryAddSyncService(
			YoushuSettingMapper youshuSettingMapper,
			ItemsCategoryRepository itemsCategoryRepository,
			YoushuItemsCategoryPushPort youshuItemsCategoryPushPort) {
		this.youshuSettingMapper = youshuSettingMapper;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.youshuItemsCategoryPushPort = youshuItemsCategoryPushPort;
	}

	public void syncAfterCategoryAdd(long companyId) {
		YoushuSetting setting = youshuSettingMapper.selectOne(
				new LambdaQueryWrapper<YoushuSetting>().eq(YoushuSetting::getCompanyId, companyId).last("LIMIT 1"));
		if (setting == null) {
			return;
		}
		String merchantId = setting.getMerchantId();
		if (merchantId == null || merchantId.isBlank()) {
			return;
		}
		YoushuOpenApiCredentials credentials = toCredentials(setting);
		List<ItemsCategory> entities = itemsCategoryRepository.listAllEntitiesByCompanyId(companyId);
		if (entities.isEmpty()) {
			return;
		}
		List<Map<String, Object>> rows = new ArrayList<>(entities.size());
		for (ItemsCategory e : entities) {
			rows.add(toRow(e));
		}
		for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
			int end = Math.min(i + BATCH_SIZE, rows.size());
			youshuItemsCategoryPushPort.pushCategoryRows(merchantId, rows.subList(i, end), credentials);
		}
	}

	private static Map<String, Object> toRow(ItemsCategory e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("category_id", e.getCategoryId());
		m.put("company_id", e.getCompanyId());
		m.put("category_name", e.getCategoryName());
		Long parentId = e.getParentId();
		m.put("parent_id", parentId == null ? 0L : parentId);
		m.put("category_level", e.getCategoryLevel() != null ? e.getCategoryLevel() : 1);
		m.put("is_main_category", Boolean.TRUE.equals(e.getIsMainCategory()) ? 1 : 0);
		m.put("sort", e.getSort() != null ? e.getSort() : 0L);
		m.put("path", e.getPath() != null ? e.getPath() : "");
		m.put("distributor_id", e.getDistributorId() != null ? e.getDistributorId() : 0L);
		return m;
	}

	private static YoushuOpenApiCredentials toCredentials(YoushuSetting v) {
		String base = firstNonBlank(v.getApiUrl(), v.getSandboxApiUrl());
		String appId = firstNonBlank(v.getAppId(), v.getSandboxAppId());
		String appSecret = firstNonBlank(v.getAppSecret(), v.getSandboxAppSecret());
		return new YoushuOpenApiCredentials(base, appId, appSecret);
	}

	private static String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		return "";
	}
}
