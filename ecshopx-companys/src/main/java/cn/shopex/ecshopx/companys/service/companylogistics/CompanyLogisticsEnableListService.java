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

package cn.shopex.ecshopx.companys.service.companylogistics;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.companys.mapper.CompanyRelLogisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompanyLogisticsEnableListService {

	private final CompanyRelLogisticsMapper companyRelLogisticsMapper;

	public CompanyLogisticsEnableListService(CompanyRelLogisticsMapper companyRelLogisticsMapper) {
		this.companyRelLogisticsMapper = companyRelLogisticsMapper;
	}

	public List<Map<String, Object>> getLogisticsEnableList(long companyId, int page, int pageSize) {
		if (companyId > Integer.MAX_VALUE) {
			throw new BadRequestException("参数错误");
		}
		int cid = (int) companyId;

		LambdaQueryWrapper<CompanyRelLogistics> base =
				Wrappers.lambdaQuery(CompanyRelLogistics.class).eq(CompanyRelLogistics::getCompanyId, cid);
		long total = companyRelLogisticsMapper.selectCount(base);

		List<CompanyRelLogistics> dbRows;
		if (total == 0) {
			dbRows = new ArrayList<>();
		} else {
			LambdaQueryWrapper<CompanyRelLogistics> selectWrapper =
					base.clone().select(CompanyRelLogistics::getCorpCode, CompanyRelLogistics::getCorpName);
			if (pageSize > 0) {
				int p = Math.max(1, page);
				Page<CompanyRelLogistics> pg = new Page<>(p, pageSize, false);
				dbRows = companyRelLogisticsMapper.selectPage(pg, selectWrapper).getRecords();
			} else {
				dbRows = companyRelLogisticsMapper.selectList(selectWrapper);
			}
		}

		List<Map<String, Object>> out = new ArrayList<>(dbRows.size() + 1);
		for (CompanyRelLogistics row : dbRows) {
			Map<String, Object> m = new LinkedHashMap<>(2);
			m.put("corp_code", row.getCorpCode());
			m.put("corp_name", row.getCorpName());
			out.add(m);
		}
		Map<String, Object> tail = new LinkedHashMap<>(2);
		tail.put("corp_code", "OTHER");
		tail.put("corp_name", "其他");
		out.add(tail);
		return out;
	}
}
