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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WxShopsSettingGetService {

	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;
	private final OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final CompanysMapper companysMapper;

	public WxShopsSettingGetService(
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService,
			OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			CompanysMapper companysMapper) {
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
		this.openDistributorDividedSettingRedisService = openDistributorDividedSettingRedisService;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.companysMapper = companysMapper;
	}

	public Map<String, Object> getWxShopsSetting(long companyId, String countryCodeRaw) {
		String lang =
				(countryCodeRaw == null || countryCodeRaw.isBlank()) ? "zh-CN" : countryCodeRaw.trim();
		Map<String, Object> loaded = wxShopsSettingRedisReadService.load(companyId);
		Map<String, Object> data = new LinkedHashMap<>();
		if (loaded.isEmpty()) {
			openDistributorDividedSettingRedisService.getOpenDistributorDivided(companyId);
		} else {
			Map<String, Object> slice = WxShopsSettingLangSliceHelper.innerMapForLang(loaded, lang);
			if (!slice.isEmpty()) {
				data.putAll(slice);
			}
		}
		Map<String, Object> selfRow = distributionDistributorSelfReadMapper.selectSelfStoreRow(companyId);
		boolean hasSelf =
				selfRow != null && !selfRow.isEmpty() && selfRow.get("distributor_id") != null;
		if (hasSelf && !data.isEmpty()) {
			data.put("brand_name", Objects.toString(selfRow.get("name"), ""));
			data.put("logo", Objects.toString(selfRow.get("logo"), ""));
		}
		Companys row = companysMapper.selectById(companyId);
		data.put(
				"company_name",
				row != null && row.getCompanyName() != null ? row.getCompanyName() : "");
		return data;
	}
}
