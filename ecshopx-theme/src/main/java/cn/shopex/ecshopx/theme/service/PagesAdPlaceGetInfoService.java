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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.theme.domain.PagesAdPlace;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceMapper;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceRelDistributorsMapper;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceRelMemberTagsMapper;
import cn.shopex.ecshopx.theme.mapper.dto.PagesAdPlaceDistributorJoinRow;
import cn.shopex.ecshopx.theme.mapper.dto.PagesAdPlaceTagJoinRow;
import cn.shopex.ecshopx.theme.support.PagesAdPlaceColumnNamesDataMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PagesAdPlaceGetInfoService {

	private final PagesAdPlaceMapper pagesAdPlaceMapper;

	private final PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper;

	private final PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper;

	private final PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper;

	public PagesAdPlaceGetInfoService(
			PagesAdPlaceMapper pagesAdPlaceMapper,
			PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper,
			PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper,
			PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper) {
		this.pagesAdPlaceMapper = pagesAdPlaceMapper;
		this.pagesAdPlaceRelMemberTagsMapper = pagesAdPlaceRelMemberTagsMapper;
		this.pagesAdPlaceRelDistributorsMapper = pagesAdPlaceRelDistributorsMapper;
		this.pagesAdPlaceColumnNamesDataMapper = pagesAdPlaceColumnNamesDataMapper;
	}

	public Object getInfo(long companyId, long adPlaceId, Long sourceIdFilter) {
		PagesAdPlace entity = pagesAdPlaceMapper.selectOneForAdminGetInfo(companyId, adPlaceId, sourceIdFilter);
		if (entity == null) {
			return Collections.emptyList();
		}
		Map<String, Object> result =
				new LinkedHashMap<>(pagesAdPlaceColumnNamesDataMapper.toColumnNamesData(entity));
		Long id = entity.getId();
		List<Long> ids = List.of(id);
		List<PagesAdPlaceTagJoinRow> tagRows = pagesAdPlaceRelMemberTagsMapper.selectRelTagsByAdPlaceIds(ids);
		List<Map<String, Object>> tagsList = new ArrayList<>();
		for (PagesAdPlaceTagJoinRow r : tagRows) {
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("tag_id", r.getTagId());
			item.put("tag_name", r.getTagName());
			tagsList.add(item);
		}
		List<PagesAdPlaceDistributorJoinRow> distRows =
				pagesAdPlaceRelDistributorsMapper.selectRelDistributorsByAdPlaceIds(ids);
		List<Map<String, Object>> distList = new ArrayList<>();
		for (PagesAdPlaceDistributorJoinRow r : distRows) {
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("distributor_id", r.getDistributorId());
			item.put("distributor_name", r.getDistributorName());
			distList.add(item);
		}
		result.put("rel_tags", tagsList);
		result.put("rel_distributors", distList);
		return result;
	}
}
