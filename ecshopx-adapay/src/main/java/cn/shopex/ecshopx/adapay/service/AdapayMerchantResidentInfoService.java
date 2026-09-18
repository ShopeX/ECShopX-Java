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

import cn.shopex.ecshopx.adapay.domain.AdapayMerchantResident;
import cn.shopex.ecshopx.adapay.domain.AdapayRegionsThird;
import cn.shopex.ecshopx.adapay.domain.AdapayWxBusinessCategory;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantResidentMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayRegionsThirdMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayWxBusinessCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayMerchantResidentInfoService {

	private final AdapayMerchantResidentMapper adapayMerchantResidentMapper;
	private final AdapayRegionsThirdMapper adapayRegionsThirdMapper;
	private final AdapayWxBusinessCategoryMapper adapayWxBusinessCategoryMapper;
	private final ObjectMapper objectMapper;

	public AdapayMerchantResidentInfoService(
			AdapayMerchantResidentMapper adapayMerchantResidentMapper,
			AdapayRegionsThirdMapper adapayRegionsThirdMapper,
			AdapayWxBusinessCategoryMapper adapayWxBusinessCategoryMapper,
			ObjectMapper objectMapper) {
		this.adapayMerchantResidentMapper = adapayMerchantResidentMapper;
		this.adapayRegionsThirdMapper = adapayRegionsThirdMapper;
		this.adapayWxBusinessCategoryMapper = adapayWxBusinessCategoryMapper;
		this.objectMapper = objectMapper;
	}

	public Object merchantResidentInfo(long companyId) {
		AdapayMerchantResident resident =
				adapayMerchantResidentMapper.selectOne(
						new LambdaQueryWrapper<AdapayMerchantResident>()
								.eq(AdapayMerchantResident::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (resident == null) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", resident.getId());
		row.put("company_id", resident.getCompanyId());
		row.put("request_id", nz(resident.getRequestId()));
		row.put("sub_api_key", nz(resident.getSubApiKey()));
		row.put("fee_type", nz(resident.getFeeType()));
		row.put("app_id", nz(resident.getAppId()));
		row.put("wx_category", nz(resident.getWxCategory()));
		row.put("alipay_category", nz(resident.getAlipayCategory()));
		row.put("cls_id", nz(resident.getClsId()));
		row.put("model_type", nz(resident.getModelType()));
		row.put("mer_type", nz(resident.getMerType()));
		row.put("province_code", nz(resident.getProvinceCode()));
		row.put("city_code", nz(resident.getCityCode()));
		row.put("district_code", nz(resident.getDistrictCode()));
		row.put("add_value_list", nz(resident.getAddValueList()));
		row.put("status", nz(resident.getStatus()));
		row.put("adapay_fee_mode", nz(resident.getAdapayFeeMode()));
		row.put("alipay_stat", nz(resident.getAlipayStat()));
		row.put("alipay_stat_msg", nz(resident.getAlipayStatMsg()));
		row.put("wx_stat", nz(resident.getWxStat()));
		row.put("wx_stat_msg", nz(resident.getWxStatMsg()));
		row.put("create_time", resident.getCreateTime());
		row.put("update_time", resident.getUpdateTime());

		if (StringUtils.hasText(resident.getAddValueList())) {
			try {
				JsonNode node = objectMapper.readTree(resident.getAddValueList());
				if (node != null && node.isObject()) {
					JsonNode appidNode = node.path("wx_lite").path("appid");
					if (appidNode != null
							&& !appidNode.isNull()
							&& appidNode.isTextual()
							&& StringUtils.hasText(appidNode.asText())) {
						row.put("authorizer_appid", appidNode.asText());
					}
				}
			} catch (JsonProcessingException ignored) {
				// omit authorizer_appid on invalid JSON
			}
		}

		putRegionName(row, "province_name", resident.getProvinceCode());
		putRegionName(row, "city_name", resident.getCityCode());
		putRegionName(row, "district_name", resident.getDistrictCode());

		if (StringUtils.hasText(resident.getWxCategory())) {
			AdapayWxBusinessCategory cat =
					adapayWxBusinessCategoryMapper.selectOne(
							new LambdaQueryWrapper<AdapayWxBusinessCategory>()
									.eq(
											AdapayWxBusinessCategory::getBusinessCategoryId,
											resident.getWxCategory())
									.last("LIMIT 1"));
			row.put(
					"wx_category_name",
					cat == null || cat.getMerchantTypeName() == null
							? ""
							: cat.getMerchantTypeName());
		}

		return row;
	}

	private void putRegionName(LinkedHashMap<String, Object> row, String key, String code) {
		if (!StringUtils.hasText(code)) {
			row.put(key, "");
			return;
		}
		AdapayRegionsThird region =
				adapayRegionsThirdMapper.selectOne(
						new LambdaQueryWrapper<AdapayRegionsThird>()
								.eq(AdapayRegionsThird::getAreaCode, code)
								.last("LIMIT 1"));
		row.put(key, region == null || region.getAreaName() == null ? "" : region.getAreaName());
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
