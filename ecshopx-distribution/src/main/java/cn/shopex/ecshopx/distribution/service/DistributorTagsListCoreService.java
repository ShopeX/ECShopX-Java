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

import cn.shopex.ecshopx.distribution.domain.DistributorTags;
import cn.shopex.ecshopx.distribution.mapper.DistributorTagsMapper;
import cn.shopex.ecshopx.distribution.support.DistributorTagsRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorTagsListCoreService {

	private final DistributorTagsMapper distributorTagsMapper;
	private final DistributorTagsListOutsideLangReadService distributorTagsListOutsideLangReadService;

	public DistributorTagsListCoreService(
			DistributorTagsMapper distributorTagsMapper,
			DistributorTagsListOutsideLangReadService distributorTagsListOutsideLangReadService) {
		this.distributorTagsMapper = distributorTagsMapper;
		this.distributorTagsListOutsideLangReadService = distributorTagsListOutsideLangReadService;
	}

	public Map<String, Object> buildList(Map<String, Object> user, Map<String, Object> merged, String requestLangTag) {
		long companyId = longOf(user.get("company_id"));
		LambdaQueryWrapper<DistributorTags> w = new LambdaQueryWrapper<>();
		w.eq(DistributorTags::getCompanyId, companyId);

		Object tagNameObj = merged.get("tag_name");
		if (tagNameObj instanceof String tagName && DistributeLogsListCoreService.isStringTruthy(tagName)) {
			String escaped = escapeSqlLike(tagName.trim());
			w.like(DistributorTags::getTagName, "%" + escaped + "%");
		}

		if (Boolean.TRUE.equals(merged.get("front_show_query_present"))) {
			Object rawObj = merged.get("front_show_raw");
			String raw = rawObj != null ? rawObj.toString() : "";
			int frontShow = 0;
			if (StringUtils.hasText(raw.trim())) {
				try {
					frontShow = Integer.parseInt(raw.trim());
				} catch (NumberFormatException e) {
					frontShow = 0;
				}
			}
			w.eq(DistributorTags::getFrontShow, frontShow);
		}

		int page = ((Number) merged.get("page")).intValue();
		int pageSize = ((Number) merged.get("pageSize")).intValue();
		Page<DistributorTags> p = new Page<>(page, pageSize);
		distributorTagsMapper.selectPage(p, w.orderByDesc(DistributorTags::getCreated));

		List<Map<String, Object>> list = new ArrayList<>();
		for (DistributorTags row : p.getRecords()) {
			list.add(DistributorTagsRowMapper.toRow(row));
		}
		distributorTagsListOutsideLangReadService.applyTagFields(companyId, requestLangTag, list);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (int) p.getTotal());
		data.put("list", list);
		return data;
	}

	private static String escapeSqlLike(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static long longOf(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
