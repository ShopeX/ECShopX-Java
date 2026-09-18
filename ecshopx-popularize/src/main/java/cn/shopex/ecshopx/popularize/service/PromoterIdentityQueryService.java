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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromoterIdentityQueryService {

	private final PromoterIdentityMapper promoterIdentityMapper;

	public PromoterIdentityQueryService(PromoterIdentityMapper promoterIdentityMapper) {
		this.promoterIdentityMapper = promoterIdentityMapper;
	}

	public Map<String, Object> getPromoteridentityInfo(long companyId, long id) {
		PromoterIdentity row = promoterIdentityMapper.selectOne(new LambdaQueryWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getCompanyId, companyId)
				.eq(PromoterIdentity::getId, id)
				.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("company_id", row.getCompanyId());
		out.put("name", row.getName());
		out.put("is_subordinates", row.getIsSubordinates());
		out.put("is_default", row.getIsDefault());
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}

	public Map<String, Object> getPromoteridentityList(long companyId, String pageQuery, String pageSizeQuery) {
		long pageSizeNum = pageSizeQuery == null ? 20L : LeadingNumberParser.parseAsLong(pageSizeQuery.trim());
		long pageNum = pageQuery == null ? 1L : LeadingNumberParser.parseAsLong(pageQuery.trim());
		boolean paginate = pageSizeNum > 0L;

		LambdaQueryWrapper<PromoterIdentity> base = new LambdaQueryWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getCompanyId, companyId);
		long total = promoterIdentityMapper.selectCount(base);
		if (total == 0L) {
			return Map.of("total_count", 0L, "list", List.of());
		}
		LambdaQueryWrapper<PromoterIdentity> listWrapper = new LambdaQueryWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getCompanyId, companyId);
		if (paginate) {
			long limit = pageSizeNum;
			long offset = pageNum - 1L;
			offset = offset * pageSizeNum;
			listWrapper.last("LIMIT " + limit + " OFFSET " + offset);
		}
		List<PromoterIdentity> rows = promoterIdentityMapper.selectList(listWrapper);
		if (rows == null) {
			rows = List.of();
		}
		List<Map<String, Object>> listMaps = new ArrayList<>(rows.size());
		for (PromoterIdentity row : rows) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", row.getId());
			item.put("company_id", row.getCompanyId());
			item.put("name", row.getName());
			item.put("is_subordinates", row.getIsSubordinates());
			item.put("is_default", row.getIsDefault());
			item.put("created", row.getCreated());
			item.put("updated", row.getUpdated());
			listMaps.add(item);
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", listMaps);
		return out;
	}
}
