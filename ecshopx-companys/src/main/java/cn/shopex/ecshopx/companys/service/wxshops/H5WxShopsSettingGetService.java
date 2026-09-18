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

package cn.shopex.ecshopx.companys.service.wxshops;

import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.service.domain.CompanyDomainLookupService;
import cn.shopex.ecshopx.companys.service.setting.OpenDistributorDividedSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingLangSliceHelper;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingRedisReadService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class H5WxShopsSettingGetService {

	private final CompanyDomainLookupService companyDomainLookupService;
	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;
	private final OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final String systemMainCompanysIdRaw;

	public H5WxShopsSettingGetService(
			CompanyDomainLookupService companyDomainLookupService,
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService,
			OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			@Value("${common.system-main-companys-id:}") String systemMainCompanysIdRaw) {
		this.companyDomainLookupService = companyDomainLookupService;
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
		this.openDistributorDividedSettingRedisService = openDistributorDividedSettingRedisService;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.systemMainCompanysIdRaw = systemMainCompanysIdRaw;
	}

	public Object getWxShopsSetting(String domain, String countryCodeRaw) {
		String lang =
				(countryCodeRaw == null || countryCodeRaw.isBlank()) ? "zh-CN" : countryCodeRaw.trim();
		long companyId = parseSystemMainCompanyId(systemMainCompanysIdRaw);
		if (StringUtils.hasText(domain)) {
			Optional<Map<String, Object>> opt = companyDomainLookupService.findCompanyInfoByDomain(domain);
			if (opt.isPresent()) {
				Map<String, Object> info = opt.get();
				Object cid = info.get("company_id");
				if (cid instanceof Number n && n.longValue() > 0) {
					companyId = n.longValue();
				} else if (cid != null) {
					String s = String.valueOf(cid).trim();
					if (!s.isEmpty()) {
						try {
							long v = Long.parseLong(s);
							if (v > 0) {
								companyId = v;
							}
						} catch (NumberFormatException ignored) {
						}
					}
				}
			}
		}
		if (companyId <= 0L) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		Map<String, Object> loaded = wxShopsSettingRedisReadService.load(companyId);
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
		if (data.isEmpty()) {
			return Collections.emptyList();
		}
		return data;
	}

	private static long parseSystemMainCompanyId(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0 ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
