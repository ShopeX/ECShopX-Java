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

import cn.shopex.ecshopx.merchant.domain.MerchantType;
import cn.shopex.ecshopx.merchant.repository.MerchantTypeRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantTypeListQueryService {

	private final MerchantTypeRepository merchantTypeRepository;
	private final MerchantTypeOutsideLangReadService merchantTypeOutsideLangReadService;

	public MerchantTypeListQueryService(
			MerchantTypeRepository merchantTypeRepository,
			MerchantTypeOutsideLangReadService merchantTypeOutsideLangReadService) {
		this.merchantTypeRepository = merchantTypeRepository;
		this.merchantTypeOutsideLangReadService = merchantTypeOutsideLangReadService;
	}

	public List<Map<String, Object>> queryTypeTree(
			long companyId,
			String sortOrderBy,
			boolean isShowChildren,
			String nameFilter,
			String langTag) {
		LinkedHashMap<String, String> orderBy = new LinkedHashMap<>();
		if (sortOrderBy != null
				&& !sortOrderBy.isEmpty()
				&& ("asc".equals(sortOrderBy) || "desc".equals(sortOrderBy))) {
			orderBy.put("sort", sortOrderBy);
		}
		orderBy.put("updated", "DESC");

		boolean byName = nameFilter != null && !nameFilter.trim().isEmpty();
		String nameContains = byName ? nameFilter.trim() : null;

		List<MerchantType> rows;
		if (byName) {
			rows = merchantTypeRepository.listMerchantTypesForTypeList(companyId, nameContains, null, orderBy);
			if (rows.isEmpty()) {
				return Collections.emptyList();
			}
			LinkedHashSet<Long> typeIds = new LinkedHashSet<>();
			for (MerchantType value : rows) {
				typeIds.add(value.getId());
			}
			for (MerchantType value : rows) {
				List<MerchantType> extra;
				if (value.getParentId() == 0L) {
					extra = merchantTypeRepository.listMerchantTypesByRelatedAnchor(companyId, value.getId(), true, orderBy);
				} else {
					extra =
							merchantTypeRepository.listMerchantTypesByRelatedAnchor(companyId, value.getParentId(), false, orderBy);
				}
				for (MerchantType e : extra) {
					typeIds.add(e.getId());
				}
			}
			rows = merchantTypeRepository.listMerchantTypesForTypeList(companyId, null, new ArrayList<>(typeIds), orderBy);
		} else {
			rows = merchantTypeRepository.listMerchantTypesForTypeList(companyId, null, null, orderBy);
		}

		List<Map<String, Object>> flat = new ArrayList<>();
		for (MerchantType e : rows) {
			Map<String, Object> m = toRowMap(e);
			m.put("children", new ArrayList<Map<String, Object>>());
			long rowId = e.getId();
			String resolvedName =
					merchantTypeOutsideLangReadService.resolveName(companyId, rowId, e.getName(), langTag);
			m.put("name", resolvedName);
			flat.add(m);
		}
		return getTree(flat, 0L, 0, isShowChildren);
	}

	private static Map<String, Object> toRowMap(MerchantType e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("name", e.getName());
		m.put("parent_id", e.getParentId());
		m.put("path", e.getPath() != null ? e.getPath() : "0");
		m.put("sort", e.getSort() != null ? e.getSort() : 0L);
		m.put("level", e.getLevel() != null ? e.getLevel() : 1);
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated() != null ? e.getUpdated() : 0);
		m.put("is_show", e.getIsShow() ? Integer.valueOf(1) : Integer.valueOf(0));
		return m;
	}

	private List<Map<String, Object>> getTree(
			List<Map<String, Object>> flat, long pid, int level, boolean isShowChildren) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> row : flat) {
			if (longValue(row.get("parent_id")) != pid) {
				continue;
			}
			row.put("cur_level", level);
			long currentId = longValue(row.get("id"));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) row.get("children");
			List<Map<String, Object>> nested = getTree(flat, currentId, level + 1, isShowChildren);
			children.clear();
			children.addAll(nested);

			Object sortObj = row.get("sort");
			int sortInt = 0;
			if (sortObj instanceof Number n) {
				sortInt = n.intValue();
			} else if (sortObj != null) {
				try {
					sortInt = Integer.parseInt(sortObj.toString());
				} catch (NumberFormatException ignored) {
					sortInt = 0;
				}
			}
			row.put("sort", sortInt);

			int dbLevel = 0;
			Object levelObj = row.get("level");
			if (levelObj instanceof Number n) {
				dbLevel = n.intValue();
			}
			if (dbLevel == 2) {
				row.remove("children");
			} else if (!isShowChildren && children.isEmpty()) {
				row.remove("children");
			}

			result.add(row);
		}
		return result;
	}

	private static long longValue(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
