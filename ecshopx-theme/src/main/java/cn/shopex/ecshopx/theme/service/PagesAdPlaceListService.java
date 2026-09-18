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
import cn.shopex.ecshopx.theme.service.dto.PagesAdPlaceListCriteria;
import cn.shopex.ecshopx.theme.service.dto.PagesAdPlaceWxappListRow;
import cn.shopex.ecshopx.theme.support.PagesAdPlaceColumnNamesDataMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PagesAdPlaceListService {

	private final PagesAdPlaceMapper pagesAdPlaceMapper;

	private final PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper;

	private final PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper;

	private final PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper;

	public PagesAdPlaceListService(
			PagesAdPlaceMapper pagesAdPlaceMapper,
			PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper,
			PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper,
			PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper) {
		this.pagesAdPlaceMapper = pagesAdPlaceMapper;
		this.pagesAdPlaceRelMemberTagsMapper = pagesAdPlaceRelMemberTagsMapper;
		this.pagesAdPlaceRelDistributorsMapper = pagesAdPlaceRelDistributorsMapper;
		this.pagesAdPlaceColumnNamesDataMapper = pagesAdPlaceColumnNamesDataMapper;
	}

	public List<Map<String, Object>> listUnpagedMapsWithRel(
			PagesAdPlaceListCriteria criteria, boolean includeDerivedStatus) {
		List<PagesAdPlace> rows = pagesAdPlaceMapper.selectAllByListCriteria(criteria);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		return toListMaps(rows, includeDerivedStatus);
	}

	public List<Map<String, Object>> listUnpagedMapsWithRelForWxapp(
			PagesAdPlaceListCriteria criteria, boolean includeDerivedStatus) {
		List<PagesAdPlaceWxappListRow> rows = pagesAdPlaceMapper.selectWxappAdListRowsByCriteria(criteria);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<Long> ids = rows.stream()
				.map(r -> r.getPlace() != null ? r.getPlace().getId() : null)
				.filter(Objects::nonNull)
				.toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<Long, List<Map<String, Object>>> tagsMap = loadRelTagsGrouped(ids);
		Map<Long, List<Map<String, Object>>> distMap = loadRelDistributorsGrouped(ids);
		Long nowEpoch = includeDerivedStatus ? Instant.now().getEpochSecond() : null;
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (PagesAdPlaceWxappListRow row : rows) {
			PagesAdPlace entity = row.getPlace();
			if (entity == null || entity.getId() == null) {
				continue;
			}
			Map<String, Object> rowMap =
					new LinkedHashMap<>(pagesAdPlaceColumnNamesDataMapper.toColumnNamesData(entity));
			if (includeDerivedStatus && nowEpoch != null) {
				long st = entity.getStartTime() == null ? 0L : entity.getStartTime();
				long et = entity.getEndTime() == null ? 0L : entity.getEndTime();
				int statusVal;
				if (nowEpoch < st) {
					statusVal = 0;
				} else if (nowEpoch > et) {
					statusVal = 2;
				} else {
					statusVal = 1;
				}
				rowMap.put("status", Integer.valueOf(statusVal));
			}
			rowMap.put("ad_place_id", row.getAdPlaceIdFromJoin());
			rowMap.put("distributor_id", row.getDistributorIdFromJoin());
			Long id = entity.getId();
			rowMap.put("rel_tags", tagsMap.getOrDefault(id, List.of()));
			rowMap.put("rel_distributors", distMap.getOrDefault(id, List.of()));
			listMaps.add(rowMap);
		}
		return listMaps;
	}

	public Map<String, Object> getList(PagesAdPlaceListCriteria criteria, int page, int pageSize) {
		long total = pagesAdPlaceMapper.countByListCriteria(criteria);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}
		long offset = (long) (page - 1) * pageSize;
		List<PagesAdPlace> rows = pagesAdPlaceMapper.selectPageByListCriteria(criteria, offset, pageSize);
		List<Long> ids = rows.stream().map(PagesAdPlace::getId).filter(Objects::nonNull).toList();
		if (rows.isEmpty() || ids.isEmpty()) {
			out.put("list", List.of());
			return out;
		}
		List<Map<String, Object>> listMaps = toListMaps(rows, true);
		out.put("list", listMaps);
		return out;
	}

	private Map<Long, List<Map<String, Object>>> loadRelTagsGrouped(List<Long> ids) {
		List<PagesAdPlaceTagJoinRow> tagRows = pagesAdPlaceRelMemberTagsMapper.selectRelTagsByAdPlaceIds(ids);
		Map<Long, List<Map<String, Object>>> tagsMap = new HashMap<>();
		for (PagesAdPlaceTagJoinRow r : tagRows) {
			Long adPlaceId = r.getAdPlaceId();
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("tag_id", r.getTagId());
			item.put("tag_name", r.getTagName());
			tagsMap.computeIfAbsent(adPlaceId, k -> new ArrayList<>()).add(item);
		}
		return tagsMap;
	}

	private Map<Long, List<Map<String, Object>>> loadRelDistributorsGrouped(List<Long> ids) {
		List<PagesAdPlaceDistributorJoinRow> distRows =
				pagesAdPlaceRelDistributorsMapper.selectRelDistributorsByAdPlaceIds(ids);
		Map<Long, List<Map<String, Object>>> distMap = new HashMap<>();
		for (PagesAdPlaceDistributorJoinRow r : distRows) {
			Long adPlaceId = r.getAdPlaceId();
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("distributor_id", r.getDistributorId());
			item.put("distributor_name", r.getDistributorName());
			distMap.computeIfAbsent(adPlaceId, k -> new ArrayList<>()).add(item);
		}
		return distMap;
	}

	private List<Map<String, Object>> toListMaps(List<PagesAdPlace> rows, boolean includeDerivedStatus) {
		List<Long> ids = rows.stream().map(PagesAdPlace::getId).filter(Objects::nonNull).toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<Long, List<Map<String, Object>>> tagsMap = loadRelTagsGrouped(ids);
		Map<Long, List<Map<String, Object>>> distMap = loadRelDistributorsGrouped(ids);
		Long nowEpoch = includeDerivedStatus ? Instant.now().getEpochSecond() : null;
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (PagesAdPlace entity : rows) {
			Map<String, Object> rowMap = pagesAdPlaceColumnNamesDataMapper.toColumnNamesData(entity);
			if (includeDerivedStatus && nowEpoch != null) {
				long st = entity.getStartTime() == null ? 0L : entity.getStartTime();
				long et = entity.getEndTime() == null ? 0L : entity.getEndTime();
				int statusVal;
				if (nowEpoch < st) {
					statusVal = 0;
				} else if (nowEpoch > et) {
					statusVal = 2;
				} else {
					statusVal = 1;
				}
				rowMap.put("status", Integer.valueOf(statusVal));
			}
			Long id = entity.getId();
			rowMap.put("rel_tags", tagsMap.getOrDefault(id, List.of()));
			rowMap.put("rel_distributors", distMap.getOrDefault(id, List.of()));
			listMaps.add(rowMap);
		}
		return listMaps;
	}
}
