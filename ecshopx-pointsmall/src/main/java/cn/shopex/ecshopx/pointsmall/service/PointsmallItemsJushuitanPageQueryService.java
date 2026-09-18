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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsJushuitanPageQueryService {

	/** Must match {@code ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS}. */
	private static final String KEY_ITEM_ID_OR_DEFAULT_IDS = "item_id_or_default_ids";

	/** Must match {@code ItemsListQueryRepository.KEY_UPDATED_GTE}. */
	private static final String KEY_UPDATED_GTE = "updated_gte";

	private final PointsmallItemsMapper pointsmallItemsMapper;

	public PointsmallItemsJushuitanPageQueryService(PointsmallItemsMapper pointsmallItemsMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
	}

	public Map<String, Object> fetchPage(long companyId, Map<String, Object> filterBase, int page, int pageSize) {
		LambdaQueryWrapper<PointsmallItems> countW = Wrappers.lambdaQuery();
		applyJushuitanFilters(countW, companyId, filterBase);
		long total = pointsmallItemsMapper.selectCount(countW);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total <= 0) {
			out.put("list", List.of());
			return out;
		}
		int offset = Math.max(0, (page - 1) * pageSize);
		LambdaQueryWrapper<PointsmallItems> w = Wrappers.lambdaQuery();
		applyJushuitanFilters(w, companyId, filterBase);
		w.orderByDesc(PointsmallItems::getItemId).last("LIMIT " + offset + "," + pageSize);
		List<PointsmallItems> rows = pointsmallItemsMapper.selectList(w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (PointsmallItems it : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("item_id", it.getItemId());
			list.add(m);
		}
		out.put("list", list);
		return out;
	}

	private static void applyJushuitanFilters(LambdaQueryWrapper<PointsmallItems> w, long companyId, Map<String, Object> filterBase) {
		w.eq(PointsmallItems::getCompanyId, companyId);
		w.eq(PointsmallItems::getItemType, "normal");
		w.eq(PointsmallItems::getIsDefault, Boolean.TRUE);
		w.eq(PointsmallItems::getAuditStatus, "approved");
		@SuppressWarnings("unchecked")
		List<Long> idOrDef = (List<Long>) filterBase.get(KEY_ITEM_ID_OR_DEFAULT_IDS);
		if (idOrDef != null && !idOrDef.isEmpty()) {
			w.and(q -> q.in(PointsmallItems::getItemId, idOrDef).or().in(PointsmallItems::getDefaultItemId, idOrDef));
		} else if (filterBase.containsKey(KEY_UPDATED_GTE) && filterBase.get(KEY_UPDATED_GTE) != null) {
			Object ug = filterBase.get(KEY_UPDATED_GTE);
			int t = toIntBound(ug);
			if (t > 0) {
				w.ge(PointsmallItems::getUpdated, t);
			}
		}
	}

	private static int toIntBound(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null || !StringUtils.hasText(o.toString())) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
