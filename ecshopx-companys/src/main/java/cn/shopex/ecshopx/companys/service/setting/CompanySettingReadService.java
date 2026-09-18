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

import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CompanySettingReadService {

	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final CompanysShopexErpSettingSharedRedisReader companysShopexErpSettingSharedRedisReader;

	public CompanySettingReadService(
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			CompanysShopexErpSettingSharedRedisReader companysShopexErpSettingSharedRedisReader) {
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.companysShopexErpSettingSharedRedisReader = companysShopexErpSettingSharedRedisReader;
	}

	public Map<String, Object> getCompanySetting(long companyId, String lang) {
		Map<String, Object> loaded = wxShopsSettingRedisReadService.load(companyId);
		Map<String, Object> wx = resolveWxFlatMap(loaded, lang);
		Map<String, Object> result = new LinkedHashMap<>();
		if (!wx.isEmpty()) {
			result.putAll(wx);
		}
		Map<String, Object> selfRow = distributionDistributorSelfReadMapper.selectSelfStoreRow(companyId);
		boolean resultTruthy = !result.isEmpty();
		boolean hasSelf =
				selfRow != null && !selfRow.isEmpty() && selfRow.get("distributor_id") != null;
		if (resultTruthy && hasSelf) {
			result.put("brand_name", Objects.toString(selfRow.get("name"), ""));
			result.put("logo", Objects.toString(selfRow.get("logo"), ""));
		}
		Optional<Map<String, Object>> erpOpt = companysShopexErpSettingSharedRedisReader.readParsed(companyId);
		boolean isOpen = false;
		if (erpOpt.isPresent()) {
			Object v = erpOpt.get().get("is_open");
			if (v instanceof Boolean b) {
				isOpen = Boolean.TRUE.equals(b);
			} else if (v instanceof Number n) {
				isOpen = n.intValue() != 0;
			}
		}
		result.put("is_open_erp", isOpen);
		return result;
	}

	/**
	 * Resolves WeChat shop display settings from Redis: either the inner map for the given
	 * language bucket key, or the full map when the payload is already flat (no language wrapper).
	 *
	 * @param loaded result of {@link WxShopsSettingRedisReadService#load(long)}; must not be
	 *     mutated by callers
	 * @param lang non-blank language key as produced by the controller (e.g. {@code zh-CN})
	 */
	private Map<String, Object> resolveWxFlatMap(Map<String, Object> loaded, String lang) {
		return WxShopsSettingLangSliceHelper.innerMapForLang(loaded, lang);
	}
}
