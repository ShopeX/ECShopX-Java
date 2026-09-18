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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.MerchantType;
import cn.shopex.ecshopx.merchant.repository.MerchantTypeRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantTypeNameQueryService {

	private final MerchantTypeRepository merchantTypeRepository;
	private final MerchantTypeOutsideLangReadService merchantTypeOutsideLangReadService;

	public MerchantTypeNameQueryService(
			MerchantTypeRepository merchantTypeRepository,
			MerchantTypeOutsideLangReadService merchantTypeOutsideLangReadService) {
		this.merchantTypeRepository = merchantTypeRepository;
		this.merchantTypeOutsideLangReadService = merchantTypeOutsideLangReadService;
	}

	public Map<String, Object> getTypeNameById(long companyId, long merchantTypeId, String acceptLanguageHeader) {
		MerchantType type = merchantTypeRepository.findByCompanyIdAndId(companyId, merchantTypeId);
		if (type == null) {
			throw new ResourceException("经营范围数据查询失败");
		}
		String name = merchantTypeOutsideLangReadService.resolveName(
				companyId, merchantTypeId, type.getName(), acceptLanguageHeader);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("merchant_type_name", name);
		if (Integer.valueOf(1).equals(type.getLevel())) {
			out.put("merchant_type_parent_id", "");
			out.put("merchant_type_parent_name", "");
			return out;
		}
		long parentId = type.getParentId();
		MerchantType parent = merchantTypeRepository.findByCompanyIdAndId(companyId, parentId);
		if (parent == null) {
			throw new ResourceException("商户类型数据查询失败");
		}
		out.put("merchant_type_parent_id", (int) parentId);
		String parentName = merchantTypeOutsideLangReadService.resolveName(
				companyId, parentId, parent.getName(), acceptLanguageHeader);
		out.put("merchant_type_parent_name", parentName);
		return out;
	}
}
