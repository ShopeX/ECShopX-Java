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

package cn.shopex.ecshopx.espier.service.subdistrict;

import cn.shopex.ecshopx.espier.domain.Subdistrict;
import cn.shopex.ecshopx.espier.mapper.SubdistrictMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SubdistrictFrontApplicationService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final SubdistrictMapper subdistrictMapper;

	public SubdistrictFrontApplicationService(SubdistrictMapper subdistrictMapper) {
		this.subdistrictMapper = subdistrictMapper;
	}

	public List<Map<String, Object>> get(
			long companyId,
			List<String> distributorIdParams,
			String receiverState,
			String receiverCity,
			String receiverDistrict) {
		boolean scalarDefaultZero;
		List<String> multi = null;
		String single = null;
		if (distributorIdParams == null || distributorIdParams.isEmpty()) {
			scalarDefaultZero = true;
		} else if (distributorIdParams.size() >= 2) {
			scalarDefaultZero = false;
			multi = distributorIdParams;
		} else {
			scalarDefaultZero = false;
			single = distributorIdParams.get(0);
		}

		Map<String, String> regions = new LinkedHashMap<>();
		if (receiverState != null && !receiverState.isEmpty()) {
			regions.put("province", receiverState);
		}
		if (receiverCity != null && !receiverCity.isEmpty()) {
			regions.put("city", receiverCity);
		}
		if (receiverDistrict != null && !receiverDistrict.isEmpty()) {
			regions.put("area", receiverDistrict);
		}

		LambdaQueryWrapper<Subdistrict> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Subdistrict::getCompanyId, companyId).eq(Subdistrict::getParentId, 0L);
		applyDistributorContains(wrapper, multi, scalarDefaultZero, single);
		if (!regions.isEmpty()) {
			wrapper
					.like(Subdistrict::getProvince, "%" + regions.getOrDefault("province", "") + "%")
					.like(Subdistrict::getCity, "%" + regions.getOrDefault("city", "") + "%");
			String areaFilter = regions.containsKey("area") ? regions.get("area").replace("区", "") : "";
			wrapper.like(Subdistrict::getArea, "%" + areaFilter + "%");
		}
		wrapper.orderByAsc(Subdistrict::getLabel);
		List<Subdistrict> topRows = subdistrictMapper.selectList(wrapper);

		List<Map<String, Object>> out = new ArrayList<>();
		for (Subdistrict row : topRows) {
			Map<String, Object> item = entityToStableOrderedRow(row);
			LambdaQueryWrapper<Subdistrict> cWrapper = new LambdaQueryWrapper<>();
			cWrapper.eq(Subdistrict::getCompanyId, companyId).eq(Subdistrict::getParentId, row.getId());
			applyDistributorContains(cWrapper, multi, scalarDefaultZero, single);
			cWrapper.orderByAsc(Subdistrict::getLabel);
			List<Subdistrict> childRows = subdistrictMapper.selectList(cWrapper);
			List<Map<String, Object>> children = new ArrayList<>();
			for (Subdistrict c : childRows) {
				children.add(entityToStableOrderedRow(c));
			}
			item.put("children", children);
			out.add(item);
		}
		return out;
	}

	private void applyDistributorContains(
			LambdaQueryWrapper<Subdistrict> w,
			List<String> multiOrNull,
			boolean scalarDefaultZero,
			String singleOrNull) {
		if (multiOrNull != null) {
			w.and(q -> {
				for (int i = 0; i < multiOrNull.size(); i++) {
					String did = multiOrNull.get(i);
					if (i == 0) {
						q.like(Subdistrict::getDistributorId, "," + did + ",");
					} else {
						q.or().like(Subdistrict::getDistributorId, "," + did + ",");
					}
				}
			});
			return;
		}
		if (scalarDefaultZero) {
			w.like(Subdistrict::getDistributorId, ",0,");
			return;
		}
		if (singleOrNull != null) {
			boolean geZero;
			try {
				geZero = new BigDecimal(singleOrNull.trim()).signum() >= 0;
			} catch (NumberFormatException e) {
				geZero = true;
			}
			if (geZero) {
				w.like(Subdistrict::getDistributorId, "," + singleOrNull + ",");
			}
		}
	}

	private Map<String, Object> entityToStableOrderedRow(Subdistrict e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("label", e.getLabel());
		m.put("parent_id", e.getParentId());
		m.put("distributor_id", splitDistributorIdColumnToList(e.getDistributorId()));
		m.put("province", e.getProvince());
		m.put("city", e.getCity());
		m.put("area", e.getArea());
		m.put("regions_id", decodeRegionsJson(e.getRegionsId()));
		return m;
	}

	private List<Object> splitDistributorIdColumnToList(String raw) {
		String s = (raw == null || raw.isEmpty()) ? "," : raw;
		String core = trimLeadingTrailingChars(s, ',');
		if (core.isEmpty()) {
			ArrayList<Object> singleEmpty = new ArrayList<>(1);
			singleEmpty.add("");
			return singleEmpty;
		}
		return new ArrayList<>(Arrays.asList(core.split(",", -1)));
	}

	private static String trimLeadingTrailingChars(String s, char ch) {
		int start = 0;
		int end = s.length();
		while (start < end && s.charAt(start) == ch) {
			start++;
		}
		while (end > start && s.charAt(end - 1) == ch) {
			end--;
		}
		return s.substring(start, end);
	}

	private Object decodeRegionsJson(String regionsJson) {
		if (regionsJson == null || regionsJson.isBlank()) {
			return null;
		}
		try {
			return JSON.readValue(regionsJson, new TypeReference<Object>() {});
		} catch (Exception e) {
			return null;
		}
	}
}
