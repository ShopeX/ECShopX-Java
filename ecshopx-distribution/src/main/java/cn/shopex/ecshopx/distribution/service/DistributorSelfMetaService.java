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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorSelfMetaService {

	private static final String PLATFORM_SELF_OPERATED_LABEL = "平台自营";

	private final DistributorMapper distributorMapper;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public DistributorSelfMetaService(
			DistributorMapper distributorMapper,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public Object getDistributorSelf(long companyId) {
		Distributor row = selectSelfDistributor(companyId);
		if (row == null || row.getDistributorId() == null) {
			return 0L;
		}
		return row.getDistributorId();
	}

	/**
	 * 平台总店（{@code distributor_self = 1}）的 {@code distributor_id} 与展示名，供账号创建等场景写入 {@code distributor_ids}。
	 */
	public Map<String, Object> getDistributorSelfStoreInfo(long companyId) {
		Map<String, Object> out = new LinkedHashMap<>();
		Distributor self = selectSelfDistributor(companyId);
		if (self == null || self.getDistributorId() == null) {
			out.put("distributor_id", 0L);
			out.put("name", "");
			return out;
		}
		out.put("distributor_id", self.getDistributorId());
		out.put("name", self.getName() != null ? self.getName() : "");
		return out;
	}

	public Map<String, Object> getDistributorSelfSimpleInfo(long companyId) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("distributor_id", 0L);
		result.put("company_id", companyId);
		result.put("name", PLATFORM_SELF_OPERATED_LABEL);
		result.put("logo", "");
		result.put("shop_code", "");
		result.put("hour", "");
		result.put("mobile", "");
		result.put("contract_phone", "");
		result.put("contact", "");
		result.put("store_name", PLATFORM_SELF_OPERATED_LABEL);
		result.put("store_address", "");
		Map<String, Object> fake = DistributorRowMaps.toApiRow(new Distributor(), objectMapper);
		for (Map.Entry<String, Object> e : fake.entrySet()) {
			result.putIfAbsent(e.getKey(), e.getValue());
		}
		Distributor self = selectSelfDistributor(companyId);
		if (self != null) {
			Map<String, Object> formatted =
					distributorListRowFormatService.formatStoreInfoOnly(self, objectMapper);
			if (formatted.get("is_refund_freight") != null) {
				result.put("is_refund_freight", formatted.get("is_refund_freight"));
			}
			if (formatted.get("name") != null) {
				result.put("name", formatted.get("name"));
			}
			if (formatted.get("logo") != null) {
				result.put("logo", formatted.get("logo"));
			}
			if (formatted.get("shop_code") != null) {
				result.put("shop_code", formatted.get("shop_code"));
			}
			if (formatted.get("hour") != null) {
				result.put("hour", formatted.get("hour"));
			}
			if (formatted.get("mobile") != null) {
				result.put("mobile", formatted.get("mobile"));
			}
			if (formatted.get("contract_phone") != null) {
				result.put("contract_phone", formatted.get("contract_phone"));
			}
			if (formatted.get("contact") != null) {
				result.put("contact", formatted.get("contact"));
			}
			if (formatted.get("store_name") != null) {
				result.put("store_name", formatted.get("store_name"));
			}
			if (formatted.get("store_address") != null) {
				result.put("store_address", formatted.get("store_address"));
			}
		}
		// Synthetic head-office row (distributor_id=0): null out region/delivery fields and drop source_from.
		applyPlatformPlaceholderRowApiShape(result);
		result.remove("merchant_name");
		return result;
	}

	/**
	 * Shapes the platform self-operated placeholder row: region and third-party delivery fields are null;
	 * {@code source_from} is not included.
	 */
	private static void applyPlatformPlaceholderRowApiShape(Map<String, Object> result) {
		result.put("regions_id", null);
		result.put("regions", null);
		result.put("is_dada", null);
		result.put("dada_shop_create", null);
		result.put("shansong_shop_create", null);
		result.remove("source_from");
	}

	private Distributor selectSelfDistributor(long companyId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getDistributorSelf, 1).last("LIMIT 1");
		return distributorMapper.selectOne(w);
	}
}
