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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.MemberTagsCheckAndProcessService;
import cn.shopex.ecshopx.theme.service.dto.PagesAdPlaceListCriteria;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PagesAdPlaceWxappAdListService {

	private final PagesAdPlaceListService pagesAdPlaceListService;

	private final MemberTagsCheckAndProcessService memberTagsCheckAndProcessService;

	public PagesAdPlaceWxappAdListService(
			PagesAdPlaceListService pagesAdPlaceListService,
			MemberTagsCheckAndProcessService memberTagsCheckAndProcessService) {
		this.pagesAdPlaceListService = pagesAdPlaceListService;
		this.memberTagsCheckAndProcessService = memberTagsCheckAndProcessService;
	}

	public List<Map<String, Object>> getAdList(
			long companyId,
			long userId,
			String adTypeRaw,
			String pageTypeRaw,
			long regionauthId,
			Long distributorIdOrNull) {
		if (adTypeRaw == null || !StringUtils.hasText(adTypeRaw.trim())) {
			throw new ResourceException("validation.required");
		}
		String trimAd = adTypeRaw.trim();
		if (!"popup".equals(trimAd) && !"carousel".equals(trimAd)) {
			throw new ResourceException("validation.in");
		}
		if (pageTypeRaw == null || !StringUtils.hasText(pageTypeRaw.trim())) {
			throw new ResourceException("validation.required");
		}
		String pageType = pageTypeRaw.trim();
		long now = Instant.now().getEpochSecond();
		PagesAdPlaceListCriteria criteriaBase =
				PagesAdPlaceListCriteria.builder()
						.companyId(companyId)
						.sourceId(null)
						.regionauthId(regionauthId)
						.useBound(0)
						.adType(trimAd)
						.pagesExact(pageType)
						.auditStatus("approved")
						.startTimeLte(now)
						.endTimeGte(now)
						.distributorIdForJoin(null)
						.build();
		List<Map<String, Object>> list1 =
				pagesAdPlaceListService.listUnpagedMapsWithRelForWxapp(criteriaBase, false);
		List<Map<String, Object>> resultList;
		if (distributorIdOrNull != null && distributorIdOrNull > 0L) {
			PagesAdPlaceListCriteria criteriaBound =
					PagesAdPlaceListCriteria.builder()
							.companyId(companyId)
							.sourceId(null)
							.regionauthId(regionauthId)
							.useBound(1)
							.adType(trimAd)
							.pagesExact(pageType)
							.auditStatus("approved")
							.startTimeLte(now)
							.endTimeGte(now)
							.distributorIdForJoin(distributorIdOrNull)
							.build();
			List<Map<String, Object>> list2 =
					pagesAdPlaceListService.listUnpagedMapsWithRelForWxapp(criteriaBound, false);
			List<Map<String, Object>> merged = new ArrayList<>(list1);
			merged.addAll(list2);
			merged.sort(
					Comparator.comparingInt((Map<String, Object> m) -> intValue(m.get("sort")))
							.thenComparingLong((Map<String, Object> m) -> longValue(m.get("id")))
							.reversed());
			resultList = merged;
		} else {
			resultList = list1;
		}
		Map<Long, List<Long>> tagRelAdPlaceIds = new LinkedHashMap<>();
		if (userId > 0L) {
			for (Map<String, Object> val : resultList) {
				Object rel = val.get("rel_tags");
				if (rel instanceof List<?> list && !list.isEmpty()) {
					long idVal = longValue(val.get("id"));
					for (Object el : list) {
						if (el instanceof Map<?, ?> tagMap) {
							Object tidObj = tagMap.get("tag_id");
							long tagId = longValue(tidObj);
							if (tagId == 0L) {
								continue;
							}
							List<Long> bucket = tagRelAdPlaceIds.computeIfAbsent(tagId, k -> new ArrayList<>());
							if (!bucket.contains(idVal)) {
								bucket.add(idVal);
							}
						}
					}
				}
			}
		} else {
			for (Iterator<Map<String, Object>> it = resultList.iterator(); it.hasNext(); ) {
				Map<String, Object> val = it.next();
				Object rel = val.get("rel_tags");
				if (rel instanceof List<?> list && !list.isEmpty()) {
					it.remove();
				}
			}
		}
		if (!tagRelAdPlaceIds.isEmpty()) {
			List<Map<String, Object>> bindTags =
					memberTagsCheckAndProcessService.checkAndProcessTag(
							companyId, userId, new ArrayList<>(tagRelAdPlaceIds.keySet()));
			Set<Long> filterAdPlaceIds = new LinkedHashSet<>();
			for (Map<String, Object> tag : bindTags) {
				Object raw = tag.get("tag_id");
				if (!(raw instanceof Number)) {
					throw new BadRequestException("The tag id must be an integer.");
				}
				long tagId = ((Number) raw).longValue();
				if (Boolean.TRUE.equals(tag.get("related")) && tagRelAdPlaceIds.containsKey(tagId)) {
					filterAdPlaceIds.addAll(tagRelAdPlaceIds.get(tagId));
				}
			}
			resultList.removeIf(
					val -> {
						Object rel = val.get("rel_tags");
						if (!(rel instanceof List<?> list) || list.isEmpty()) {
							return false;
						}
						return !filterAdPlaceIds.contains(longValue(val.get("id")));
					});
		}
		return resultList;
	}

	private static int intValue(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o instanceof String s && StringUtils.hasText(s)) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
		return 0;
	}

	private static long longValue(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				return 0L;
			}
		}
		return 0L;
	}
}
