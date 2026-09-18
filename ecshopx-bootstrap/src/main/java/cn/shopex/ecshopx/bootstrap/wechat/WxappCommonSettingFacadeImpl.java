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

package cn.shopex.ecshopx.bootstrap.wechat;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WxappCommonSettingFacade;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.companys.service.setting.CategoryPageSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.NostoresSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.ShareParametersSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.WhitelistSettingRedisService;
import cn.shopex.ecshopx.im.service.EChatConfigService;
import cn.shopex.ecshopx.im.service.MeiqiaConfigService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import cn.shopex.ecshopx.espier.storage.StorageProperties;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappCommonSettingFacadeImpl implements WxappCommonSettingFacade {

	private final MeiqiaConfigService meiqiaConfigService;
	private final EChatConfigService eChatConfigService;
	private final NostoresSettingRedisService nostoresSettingRedisService;
	private final WhitelistSettingRedisService whitelistSettingRedisService;
	private final ShareParametersSettingRedisService shareParametersSettingRedisService;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final CategoryPageSettingRedisService categoryPageSettingRedisService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final StorageProperties storageProperties;
	private final String diskDriverRaw;

	public WxappCommonSettingFacadeImpl(
			MeiqiaConfigService meiqiaConfigService,
			EChatConfigService eChatConfigService,
			NostoresSettingRedisService nostoresSettingRedisService,
			WhitelistSettingRedisService whitelistSettingRedisService,
			ShareParametersSettingRedisService shareParametersSettingRedisService,
			PointMemberRuleReadService pointMemberRuleReadService,
			CategoryPageSettingRedisService categoryPageSettingRedisService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			DmCrmSettingReadPort dmCrmSettingReadPort,
			StorageProperties storageProperties,
			@Value("${DISK_DRIVER:}") String diskDriverRaw) {
		this.meiqiaConfigService = meiqiaConfigService;
		this.eChatConfigService = eChatConfigService;
		this.nostoresSettingRedisService = nostoresSettingRedisService;
		this.whitelistSettingRedisService = whitelistSettingRedisService;
		this.shareParametersSettingRedisService = shareParametersSettingRedisService;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.categoryPageSettingRedisService = categoryPageSettingRedisService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.storageProperties = storageProperties;
		this.diskDriverRaw = diskDriverRaw;
	}

	@Override
	public Map<String, Object> getCommonSetting(long companyId, String countryCode) {
		Map<String, Object> meiqia = meiqiaConfigService.getInfo(companyId);
		Map<String, Object> echat = eChatConfigService.getInfo(companyId);

		boolean nostoresStatus;
		try {
			Map<String, Object> nostoresMap = nostoresSettingRedisService.getNostoresStatus(companyId);
			nostoresStatus = Boolean.TRUE.equals(nostoresMap.get("nostores_status"));
		} catch (ResourceException e) {
			nostoresStatus = false;
		}

		Map<String, Object> whitelist =
				whitelistSettingRedisService.getMergedConfig(companyId, Collections.emptyMap());
		Object whitelistStatus = whitelist.get("whitelist_status");

		Map<String, Object> share = shareParametersSettingRedisService.read(companyId);
		boolean distributorParam;
		if (share == null) {
			distributorParam = false;
		} else {
			Object raw = share.get("distributor_param_status");
			distributorParam =
					Boolean.TRUE.equals(raw) || "true".equals(String.valueOf(raw).trim());
		}

		Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId, countryCode);
		String pointRuleName = rule.get("name") == null ? "" : String.valueOf(rule.get("name"));

		String categoryStyle;
		try {
			Object catRaw = categoryPageSettingRedisService.getCategoryPageSetting(companyId);
			if (catRaw == null || !(catRaw instanceof Map<?, ?> m)) {
				categoryStyle = "category";
			} else {
				Object s = m.get("style");
				if (s != null && StringUtils.hasText(String.valueOf(s).trim())) {
					categoryStyle = String.valueOf(s).trim();
				} else {
					categoryStyle = "category";
				}
			}
		} catch (ResourceException e) {
			categoryStyle = "category";
		}

		boolean dmcrmIsOpen = dmCrmSettingReadPort.isPointIntegrationOpen(companyId);

		String diskDriverOut;
		if (diskDriverRaw != null && StringUtils.hasText(diskDriverRaw.trim())) {
			diskDriverOut = diskDriverRaw.trim();
		} else {
			String configured = storageProperties.getDriver();
			diskDriverOut =
					configured != null && StringUtils.hasText(configured.trim())
							? configured.trim()
							: "local";
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("meiqia", meiqia);
		data.put("echat", echat);
		data.put("nostores_status", nostoresStatus);
		data.put("whitelist_status", whitelistStatus);
		data.put("distributor_param_status", distributorParam);
		data.put("disk_driver", diskDriverOut);
		data.put("point_rule_name", pointRuleName);
		data.put("category_style", categoryStyle);
		data.put("dmcrm_is_open", dmcrmIsOpen);
		CurrencyExchangeRate currencyRow = companyDefaultCurrencyService.getCur(companyId);
		data.put("currency", companyDefaultCurrencyService.toCurResponseMap(currencyRow));
		return data;
	}
}
