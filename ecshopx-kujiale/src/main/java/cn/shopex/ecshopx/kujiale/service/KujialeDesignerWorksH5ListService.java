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

package cn.shopex.ecshopx.kujiale.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kujiale.api.front.v1.dto.KujialeH5DesignerWorkListRowDto;
import cn.shopex.ecshopx.kujiale.api.front.v1.request.KujialeDesginWorkListRequest;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksH5ListMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * H5 设计师方案列表；模糊条件使用 {@code CONCAT('%', 参数, '%')}，{@code _}、{@code %} 按 LIKE 通配符语义生效。
 */
@Service
public class KujialeDesignerWorksH5ListService {

	private static final String DESIGN_ID_OMIT = "omit";
	private static final String DESIGN_ID_EMPTY = "empty_string";
	private static final String DESIGN_ID_IN = "in_list";
	private static final String CITY_OMIT = "omit";
	private static final String CITY_SCALAR = "scalar";
	private static final String CITY_LIST = "list";

	private final KujialeDesignerWorksH5ListMapper h5ListMapper;

	public KujialeDesignerWorksH5ListService(KujialeDesignerWorksH5ListMapper h5ListMapper) {
		this.h5ListMapper = h5ListMapper;
	}

	public Map<String, Object> getDesignerWorksList(KujialeDesginWorkListRequest request) {
		if (request == null) {
			request = new KujialeDesginWorkListRequest();
		}

		assertTagsParamsShape(request.getTagsParams());

		int page = resolvePage(request.getPage());
		int pageSize = resolvePageSize(request.getPageSize());

		String keyword = blankToNull(request.getKeywords());
		String sortKey = resolveSortKey(request.getSort());

		String designIdMode = DESIGN_ID_OMIT;
		List<String> designIdsForIn = null;

		Object tagsRaw = request.getTagsParams();
		if (tagsRaw instanceof List<?> tagsList && !tagsList.isEmpty()) {
			List<String> intersected = intersectDesignIdsByTags(tagsList);
			if (intersected.isEmpty()) {
				designIdMode = DESIGN_ID_EMPTY;
			} else {
				designIdMode = DESIGN_ID_IN;
				designIdsForIn = intersected;
			}
		}

		CityParams city = resolveCityParams(request.getCityId());

		long total = h5ListMapper.countByFilter(
				keyword,
				designIdMode,
				designIdsForIn,
				city.mode(),
				city.scalar(),
				city.ids());

		int offset = (page - 1) * pageSize;
		List<Map<String, Object>> rawList =
				h5ListMapper.selectPageByFilter(
						keyword,
						designIdMode,
						designIdsForIn,
						city.mode(),
						city.scalar(),
						city.ids(),
						sortKey,
						offset,
						pageSize);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);

		if (rawList.isEmpty()) {
			out.put("list", List.of());
			return out;
		}

		List<String> pageDesignIds = new ArrayList<>();
		for (Map<String, Object> row : rawList) {
			Object did = row.get("design_id");
			if (did != null && StringUtils.hasText(did.toString())) {
				pageDesignIds.add(did.toString());
			}
		}

		Map<String, List<Map<String, Object>>> taginfoByDesign = new LinkedHashMap<>();
		if (!pageDesignIds.isEmpty()) {
			List<Map<String, Object>> relRows = h5ListMapper.selectRelTagsByDesignIds(pageDesignIds);
			for (Map<String, Object> rel : relRows) {
				Object did = rel.get("design_id");
				if (did == null) {
					continue;
				}
				String key = did.toString();
				taginfoByDesign.computeIfAbsent(key, k -> new ArrayList<>()).add(rel);
			}
		}

		List<KujialeH5DesignerWorkListRowDto> list = new ArrayList<>(rawList.size());
		for (Map<String, Object> row : rawList) {
			KujialeH5DesignerWorkListRowDto dto = toRowDto(row);
			dto.setLike(false);
			Object did = row.get("design_id");
			String dk = did != null ? did.toString() : "";
			dto.setTaginfo(taginfoByDesign.getOrDefault(dk, List.of()));
			list.add(dto);
		}

		out.put("list", list);
		return out;
	}

	private static void assertTagsParamsShape(Object raw) {
		if (raw == null) {
			return;
		}
		if (raw instanceof List<?>) {
			return;
		}
		// JSON `{}` 反序列化为 Map；空对象视为未传 tags，不做过滤
		if (raw instanceof Map<?, ?> map && map.isEmpty()) {
			return;
		}
		throw new ResourceException("参数无效");
	}

	private List<String> intersectDesignIdsByTags(List<?> tagsList) {
		List<String> intersection = null;
		for (Object tagObj : tagsList) {
			String catId = "";
			String tagId = "";
			if (tagObj instanceof Map<?, ?> m) {
				Object a = m.get("tag_category_id");
				Object b = m.get("tag_id");
				if (a != null) {
					catId = a.toString();
				}
				if (b != null) {
					tagId = b.toString();
				}
			}
			List<String> ids = h5ListMapper.selectDesignIdsByTag(catId, tagId);
			if (intersection == null) {
				intersection = new ArrayList<>(ids);
			} else {
				intersection.retainAll(ids);
			}
		}
		if (intersection == null) {
			return List.of();
		}
		return intersection;
	}

	private static int resolvePage(Integer raw) {
		if (raw == null) {
			return 1;
		}
		int v = raw;
		if (v < 1) {
			return 1;
		}
		return v;
	}

	private static int resolvePageSize(Integer raw) {
		if (raw == null) {
			return 50;
		}
		int v = raw;
		if (v < 1) {
			return 50;
		}
		return v;
	}

	private static String resolveSortKey(String sort) {
		if (!StringUtils.hasText(sort)) {
			return "";
		}
		String s = sort.trim();
		if ("hot".equalsIgnoreCase(s)) {
			return "hot";
		}
		if ("latest".equalsIgnoreCase(s)) {
			return "latest";
		}
		return "";
	}

	private static String blankToNull(String s) {
		if (s == null || !StringUtils.hasText(s.trim())) {
			return null;
		}
		return s.trim();
	}

	private static CityParams resolveCityParams(Object cityId) {
		if (cityId == null) {
			return new CityParams(CITY_OMIT, null, null);
		}
		if (cityId instanceof List<?> list) {
			List<String> ids = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String t = o.toString().trim();
				if (StringUtils.hasText(t)) {
					ids.add(t);
				}
			}
			if (ids.isEmpty()) {
				return new CityParams(CITY_OMIT, null, null);
			}
			return new CityParams(CITY_LIST, null, ids);
		}
		String scalar = cityId.toString().trim();
		if (!StringUtils.hasText(scalar)) {
			return new CityParams(CITY_OMIT, null, null);
		}
		return new CityParams(CITY_SCALAR, scalar, null);
	}

	private static KujialeH5DesignerWorkListRowDto toRowDto(Map<String, Object> row) {
		KujialeH5DesignerWorkListRowDto dto = new KujialeH5DesignerWorkListRowDto();
		dto.setId(row.get("id"));
		dto.setDesignName(row.get("design_name"));
		dto.setCoverPic(row.get("cover_pic"));
		dto.setIsOrigin(row.get("is_origin"));
		dto.setIsExcellent(row.get("is_excellent"));
		dto.setIsRealExcellent(row.get("is_real_excellent"));
		dto.setIsTop(row.get("is_top"));
		dto.setDesignId(row.get("design_id"));
		dto.setPlanId(row.get("plan_id"));
		dto.setCommName(row.get("comm_name"));
		dto.setCity(row.get("city"));
		dto.setName(row.get("name"));
		dto.setTagId(row.get("tag_id"));
		dto.setDesignPanoUrl(row.get("design_pano_url"));
		dto.setUserAvatar(row.get("user_avatar"));
		dto.setEmail(row.get("email"));
		dto.setUserName(row.get("user_name"));
		dto.setUserId(row.get("user_id"));
		dto.setOrganizationId(row.get("organization_id"));
		dto.setCreated(row.get("created"));
		dto.setUpdated(row.get("updated"));
		dto.setViewCount(row.get("view_count"));
		dto.setLikeCount(row.get("like_count"));
		dto.setKuCreated(row.get("ku_created"));
		return dto;
	}

	private record CityParams(String mode, String scalar, List<String> ids) {}
}
