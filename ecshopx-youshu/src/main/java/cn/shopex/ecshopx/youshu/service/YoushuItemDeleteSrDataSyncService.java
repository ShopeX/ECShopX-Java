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

import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.integration.YoushuItemSkuDeletePort;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class YoushuItemDeleteSrDataSyncService {

	private static final int DATA_SOURCE_TYPE_SKU = 3;

	private final YoushuSettingMapper youshuSettingMapper;
	private final YoushuDataSourceApiPort youshuDataSourceApiPort;
	private final YoushuItemSkuDeletePort youshuItemSkuDeletePort;

	public YoushuItemDeleteSrDataSyncService(
			YoushuSettingMapper youshuSettingMapper,
			YoushuDataSourceApiPort youshuDataSourceApiPort,
			YoushuItemSkuDeletePort youshuItemSkuDeletePort) {
		this.youshuSettingMapper = youshuSettingMapper;
		this.youshuDataSourceApiPort = youshuDataSourceApiPort;
		this.youshuItemSkuDeletePort = youshuItemSkuDeletePort;
	}

	public void syncItemsDelete(long companyId, long itemId) {
		YoushuSetting setting =
				youshuSettingMapper.selectOne(
						new LambdaQueryWrapper<YoushuSetting>()
								.eq(YoushuSetting::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (setting == null) {
			return;
		}
		String merchantId = setting.getMerchantId();
		if (!StringUtils.hasText(merchantId)) {
			return;
		}

		YoushuOpenApiCredentials credentials = toCredentials(setting);
		String dataSourceId =
				youshuDataSourceApiPort.getOrCreateDataSourceId(merchantId.trim(), DATA_SOURCE_TYPE_SKU, credentials);
		youshuItemSkuDeletePort.deleteSkuByExternalId(dataSourceId, itemId, credentials);
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
