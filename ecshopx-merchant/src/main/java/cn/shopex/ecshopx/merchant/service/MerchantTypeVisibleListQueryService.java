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
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantTypeVisibleListQueryService {

	private final MerchantTypeRepository merchantTypeRepository;
	private final MerchantTypeOutsideLangReadService merchantTypeOutsideLangReadService;

	public MerchantTypeVisibleListQueryService(
			MerchantTypeRepository merchantTypeRepository,
			MerchantTypeOutsideLangReadService merchantTypeOutsideLangReadService) {
		this.merchantTypeRepository = merchantTypeRepository;
		this.merchantTypeOutsideLangReadService = merchantTypeOutsideLangReadService;
	}

	public List<Map<String, Object>> queryVisibleTypeList(
			long companyId, long parentId, String name, String langTag) {
		String nameContains =
				(name == null || name.isBlank()) ? null : name.trim();

		long total =
				merchantTypeRepository.countVisibleTypes(companyId, parentId, null, null, nameContains);
		List<MerchantType> orderedFirst =
				merchantTypeRepository.listVisibleTypes(
						companyId, parentId, null, null, nameContains, true);

		if (parentId > 0 || total <= 0) {
			return mapRowsWithNames(companyId, orderedFirst, langTag);
		}

		List<MerchantType> firstList =
				merchantTypeRepository.listVisibleTypes(companyId, 0L, null, null, nameContains, false);
		if (firstList.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> parentIds = new ArrayList<>(firstList.size());
		for (MerchantType t : firstList) {
			parentIds.add(t.getId());
		}

		long childrenTotal =
				merchantTypeRepository.countVisibleTypes(companyId, 0L, parentIds, null, null);
		if (childrenTotal <= 0) {
			return Collections.emptyList();
		}

		List<MerchantType> childrenRows =
				merchantTypeRepository.listVisibleTypes(companyId, 0L, parentIds, null, null, false);
		List<Long> filterParentIds =
				childrenRows.stream().map(MerchantType::getParentId).distinct().toList();
		if (filterParentIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<MerchantType> finalRows =
				merchantTypeRepository.listVisibleTypes(
						companyId, 0L, null, filterParentIds, nameContains, true);
		return mapRowsWithNames(companyId, finalRows, langTag);
	}

	public Map<String, Object> queryVisibleTypeListPaged(
			long companyId,
			long parentId,
			String nameContains,
			String langTag,
			int page,
			int pageSize) {
		long totalFirst =
				merchantTypeRepository.countVisibleTypes(companyId, parentId, null, null, nameContains);

		if (parentId > 0) {
			List<MerchantType> listPage =
					merchantTypeRepository.listVisibleTypes(
							companyId,
							parentId,
							null,
							null,
							nameContains,
							true,
							pageSize,
							(page - 1) * pageSize);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalFirst);
			out.put("list", mapRowsWithNames(companyId, listPage, langTag));
			return out;
		}

		if (totalFirst <= 0) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalFirst);
			out.put("list", Collections.emptyList());
			return out;
		}

		List<MerchantType> firstList =
				merchantTypeRepository.listVisibleTypes(companyId, 0L, null, null, nameContains, false, null, null);
		List<Long> parentIds = new ArrayList<>(firstList.size());
		for (MerchantType t : firstList) {
			parentIds.add(t.getId());
		}

		long childrenTotal =
				merchantTypeRepository.countVisibleTypes(companyId, 0L, parentIds, null, null);
		if (childrenTotal <= 0) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", 0L);
			out.put("list", Collections.emptyList());
			return out;
		}

		List<MerchantType> childrenRows =
				merchantTypeRepository.listVisibleTypes(companyId, 0L, parentIds, null, null, false, null, null);
		List<Long> filterParentIds = childrenRows.stream().map(MerchantType::getParentId).distinct().toList();
		if (filterParentIds.isEmpty()) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", 0L);
			out.put("list", Collections.emptyList());
			return out;
		}

		long totalFinal =
				merchantTypeRepository.countVisibleTypes(companyId, 0L, null, filterParentIds, nameContains);
		List<MerchantType> finalRows =
				merchantTypeRepository.listVisibleTypes(
						companyId,
						0L,
						null,
						filterParentIds,
						nameContains,
						true,
						pageSize,
						(page - 1) * pageSize);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalFinal);
		out.put("list", mapRowsWithNames(companyId, finalRows, langTag));
		return out;
	}

	private List<Map<String, Object>> mapRowsWithNames(
			long companyId, List<MerchantType> rows, String langTag) {
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (MerchantType e : rows) {
			Map<String, Object> m = toApiRow(e);
			long rowId = e.getId();
			String resolved =
					merchantTypeOutsideLangReadService.resolveName(companyId, rowId, e.getName(), langTag);
			m.put("name", resolved);
			out.add(m);
		}
		return out;
	}

	/**
	 * 与 {@link MerchantTypeListQueryService} 中 {@code toRowMap} 字段一致（扁平列表不含 {@code children} /
	 * {@code cur_level}）。
	 */
	private static Map<String, Object> toApiRow(MerchantType e) {
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
}
