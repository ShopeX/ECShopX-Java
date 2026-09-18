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

import cn.shopex.ecshopx.adapay.domain.AdapayBankCodes;
import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.adapay.domain.AdapayRegions;
import cn.shopex.ecshopx.adapay.mapper.AdapayBankCodesMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantEntryMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayRegionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayMerchantEntryInfoLoader {

	private final AdapayMerchantEntryMapper adapayMerchantEntryMapper;
	private final AdapayBankCodesMapper adapayBankCodesMapper;
	private final AdapayRegionsMapper adapayRegionsMapper;

	public AdapayMerchantEntryInfoLoader(
			AdapayMerchantEntryMapper adapayMerchantEntryMapper,
			AdapayBankCodesMapper adapayBankCodesMapper,
			AdapayRegionsMapper adapayRegionsMapper) {
		this.adapayMerchantEntryMapper = adapayMerchantEntryMapper;
		this.adapayBankCodesMapper = adapayBankCodesMapper;
		this.adapayRegionsMapper = adapayRegionsMapper;
	}

	public Optional<AdapayMerchantEntryInfoContext> load(long companyId) {
		AdapayMerchantEntry entry =
				adapayMerchantEntryMapper.selectOne(
						new LambdaQueryWrapper<AdapayMerchantEntry>()
								.eq(AdapayMerchantEntry::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (entry == null) {
			return Optional.empty();
		}

		String bankName = "";
		String provName = "";
		String areaName = "";

		if (StringUtils.hasText(entry.getBankCode())) {
			AdapayBankCodes bankRow =
					adapayBankCodesMapper.selectOne(
							new LambdaQueryWrapper<AdapayBankCodes>()
									.eq(AdapayBankCodes::getBankCode, entry.getBankCode())
									.last("LIMIT 1"));
			if (bankRow != null && StringUtils.hasText(bankRow.getBankName())) {
				bankName = bankRow.getBankName();
			}
		}
		if (StringUtils.hasText(entry.getProvCode())) {
			AdapayRegions provRow =
					adapayRegionsMapper.selectOne(
							new LambdaQueryWrapper<AdapayRegions>()
									.eq(AdapayRegions::getAreaCode, entry.getProvCode())
									.last("LIMIT 1"));
			if (provRow != null && StringUtils.hasText(provRow.getAreaName())) {
				provName = provRow.getAreaName();
			}
		}
		if (StringUtils.hasText(entry.getAreaCode())) {
			AdapayRegions areaRow =
					adapayRegionsMapper.selectOne(
							new LambdaQueryWrapper<AdapayRegions>()
									.eq(AdapayRegions::getAreaCode, entry.getAreaCode())
									.last("LIMIT 1"));
			if (areaRow != null && StringUtils.hasText(areaRow.getAreaName())) {
				areaName = areaRow.getAreaName();
			}
		}

		return Optional.of(new AdapayMerchantEntryInfoContext(entry, bankName, provName, areaName));
	}
}
