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

package cn.shopex.ecshopx.goods.service.keywords;

import cn.shopex.ecshopx.goods.domain.Keywords;
import cn.shopex.ecshopx.goods.mapper.KeywordsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsKeywordsQueryService {

	private final KeywordsMapper keywordsMapper;

	public GoodsKeywordsQueryService(KeywordsMapper keywordsMapper) {
		this.keywordsMapper = keywordsMapper;
	}

	/**
	 * 微信小程序店铺热门关键词：先按 {@code distributorId} 查，无数据且 {@code distributorId != 0} 时再查全店默认（0），并附带 {@code content} 文案列表。
	 */
	public Map<String, Object> listByShopForWxapp(long companyId, long distributorId) {
		Map<String, Object> result = listAllForCompanyAndDistributor(companyId, distributorId);
		long total = ((Number) result.get("total_count")).longValue();
		if (total == 0L && distributorId != 0L) {
			result = listAllForCompanyAndDistributor(companyId, 0L);
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		List<String> contentCol = new ArrayList<>();
		if (list != null) {
			for (Map<String, Object> row : list) {
				Object c = row.get("content");
				contentCol.add(c == null ? "" : c.toString());
			}
		}
		Map<String, Object> out = new LinkedHashMap<>(result);
		out.put("content", contentCol);
		return out;
	}

	private Map<String, Object> listAllForCompanyAndDistributor(long companyId, long distributorId) {
		LambdaQueryWrapper<Keywords> wrapper = new LambdaQueryWrapper<Keywords>()
				.eq(Keywords::getCompanyId, companyId)
				.eq(Keywords::getDistributorId, distributorId)
				.orderByAsc(Keywords::getId);
		List<Keywords> records = keywordsMapper.selectList(wrapper);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Keywords k : records) {
			list.add(toRow(k));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", (long) records.size());
		out.put("list", list);
		return out;
	}

	public Map<String, Object> listKeywords(
			long companyId,
			Long distributorIdEqOrNull,
			String contentContainsOrNull,
			long page,
			long pageSize) {
		long p = page < 1L ? 1L : page;
		long ps = pageSize < 1L ? 10L : pageSize;

		LambdaQueryWrapper<Keywords> wrapper = new LambdaQueryWrapper<Keywords>()
				.eq(Keywords::getCompanyId, companyId);
		if (distributorIdEqOrNull != null) {
			wrapper.eq(Keywords::getDistributorId, distributorIdEqOrNull);
		}
		if (StringUtils.hasText(contentContainsOrNull)) {
			wrapper.like(Keywords::getContent, contentContainsOrNull.trim());
		}
		wrapper.orderByAsc(Keywords::getId);

		Page<Keywords> mpPage = new Page<>(p, ps);
		keywordsMapper.selectPage(mpPage, wrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Keywords k : mpPage.getRecords()) {
			list.add(toRow(k));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", mpPage.getTotal());
		out.put("list", list);
		return out;
	}

	/**
	 * 按主键查询关键词；未命中返回 {@code null}（由调用方转为空列表响应）。
	 */
	public Map<String, Object> getInfoById(long id) {
		Keywords row = keywordsMapper.selectById(id);
		if (row == null) {
			return null;
		}
		return toRow(row);
	}

	private static Map<String, Object> toRow(Keywords k) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", k.getId());
		m.put("company_id", k.getCompanyId());
		Long dist = k.getDistributorId();
		m.put("distributor_id", dist != null ? dist : 0L);
		m.put("content", k.getContent());
		return m;
	}
}
