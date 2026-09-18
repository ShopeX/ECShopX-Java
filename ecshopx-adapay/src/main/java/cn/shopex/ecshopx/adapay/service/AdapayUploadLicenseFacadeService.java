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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayUploadLicenseFacadeService {

	private final AdapayMerchantEntryInfoLoader adapayMerchantEntryInfoLoader;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;

	public AdapayUploadLicenseFacadeService(
			AdapayMerchantEntryInfoLoader adapayMerchantEntryInfoLoader,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader) {
		this.adapayMerchantEntryInfoLoader = adapayMerchantEntryInfoLoader;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
	}

	public void uploadLicense(long companyId, String fileUrl, String fileType, String fileDir) {
		Optional<AdapayMerchantEntryInfoContext> loaded = adapayMerchantEntryInfoLoader.load(companyId);
		if (loaded.isEmpty()) {
			throw new ResourceException("请先完成开户进件");
		}
		AdapayMerchantEntryInfoContext ctx = loaded.get();
		AdapayMerchantEntry entry = ctx.entry();

		@SuppressWarnings("unused")
		Map<String, Object> displayInfo = new LinkedHashMap<>();
		if (StringUtils.hasText(ctx.bankName())) {
			displayInfo.put("bank_name", ctx.bankName());
		}
		if (StringUtils.hasText(ctx.provName())) {
			displayInfo.put("prov_name", ctx.provName());
		}
		if (StringUtils.hasText(ctx.areaName())) {
			displayInfo.put("area_name", ctx.areaName());
		}

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("subApiKey", entry.getLiveApiKey() == null ? "" : entry.getLiveApiKey());
		params.put("file", fileUrl);
		params.put("fileType", fileType);
		params.put("file_dir", fileDir);
		params.put("api_method", "MerchantProfile.merProfilePicture");
		params.put("merchant_info", adapayPaymentSettingRedisReader.getPaymentSetting(companyId));

		throw new ResourceException("暂不支持开户流程");
	}
}
