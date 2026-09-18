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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.UpdateCompanyInfoData;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompanysInfoUpdateService {

	private final CompanysMapper companysMapper;

	public CompanysInfoUpdateService(CompanysMapper companysMapper) {
		this.companysMapper = companysMapper;
	}

	/** 使用 DTO 承载结果以满足 {@code ApiResult} 与 Jackson 对嵌套对象 null 的序列化语义。 */
	public UpdateCompanyInfoData updateCompanyInfo(long companyId, Map<String, Object> mergedInput) {
		Companys row = companysMapper.selectById(companyId);
		if (row == null) {
			throw new ResourceException("企业账号为" + companyId + "不存在！");
		}
		UpdateCompanyInfoData out = new UpdateCompanyInfoData();
		out.setCompanyId(companyId);

		boolean shouldUpdateIndustry =
				mergedInput != null
						&& mergedInput.containsKey("industry")
						&& mergedInput.get("industry") != null;
		if (!shouldUpdateIndustry) {
			out.setIndustry(null);
			return out;
		}

		Object raw = mergedInput.get("industry");
		String industryStr = raw instanceof String ? (String) raw : String.valueOf(raw);
		row.setIndustry(industryStr);
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		companysMapper.updateById(row);
		out.setIndustry(industryStr);
		return out;
	}
}
