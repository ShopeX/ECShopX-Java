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

package cn.shopex.ecshopx.wsugc.service.badge;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wsugc.domain.Badge;
import cn.shopex.ecshopx.wsugc.mapper.BadgeMapper;
import cn.shopex.ecshopx.wsugc.service.post.UgcPostUserIdResolveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BadgeListService {

	private static final Logger log = LoggerFactory.getLogger(BadgeListService.class);

	private final BadgeMapper badgeMapper;
	private final UgcPostUserIdResolveService ugcPostUserIdResolveService;
	private final BadgeDetailService badgeDetailService;
	private final BadgeOutsideLangReadService badgeOutsideLangReadService;

	public BadgeListService(
			BadgeMapper badgeMapper,
			UgcPostUserIdResolveService ugcPostUserIdResolveService,
			BadgeDetailService badgeDetailService,
			BadgeOutsideLangReadService badgeOutsideLangReadService) {
		this.badgeMapper = badgeMapper;
		this.ugcPostUserIdResolveService = ugcPostUserIdResolveService;
		this.badgeDetailService = badgeDetailService;
		this.badgeOutsideLangReadService = badgeOutsideLangReadService;
	}

	public Object buildList(
			long companyId,
			String langTag,
			int page,
			int pageSize,
			String badgeName,
			String badgeMemo,
			String isTopRaw,
			String nickname,
			String mobile,
			String sort) {
		LambdaQueryWrapper<Badge> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Badge::getCompanyId, companyId);
		if (StringUtils.hasText(badgeName)) {
			String trimmedName = badgeName.trim();
			List<Long> langBadgeIds =
					badgeOutsideLangReadService.findDataIdsByFieldContains(
							companyId, langTag, "badge_name", trimmedName);
			if (!langBadgeIds.isEmpty()) {
				// 多语言拦截器逻辑：当语言表有匹配时，仅按 data_id IN 过滤（不走主表 LIKE OR）
				wrapper.in(Badge::getBadgeId, langBadgeIds);
			} else {
				wrapper.like(Badge::getBadgeName, "%" + escapeLike(trimmedName) + "%");
			}
		}
		if (StringUtils.hasText(badgeMemo)) {
			wrapper.like(Badge::getBadgeMemo, "%" + escapeLike(badgeMemo.trim()) + "%");
		}
		if (isTopRaw != null && !isTopRaw.isEmpty()) {
			Integer isTopVal = parseIsTop(isTopRaw);
			wrapper.eq(Badge::getIsTop, isTopVal);
		}
		if (StringUtils.hasText(mobile)) {
			wrapper.in(Badge::getUserId, ugcPostUserIdResolveService.userIdsByMobile(mobile.trim()));
		} else if (StringUtils.hasText(nickname)) {
			wrapper.in(Badge::getUserId, ugcPostUserIdResolveService.userIdsByNicknameContains(nickname.trim()));
		}
		applySort(wrapper, sort);

		Page<Badge> p = new Page<>(page, pageSize);
		badgeMapper.selectPage(p, wrapper);

		if (p.getTotal() == 0L || p.getRecords() == null || p.getRecords().isEmpty()) {
			try {
				log.debug("badge list empty companyId={} page={} pageSize={}", companyId, page, pageSize);
			} catch (Exception ignored) {
				// debug only
			}
			return Collections.emptyList();
		}

		boolean nameSearchActive = StringUtils.hasText(badgeName);
		String badgeNameQuery = badgeName != null ? badgeName : "";

		List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
		for (Badge e : p.getRecords()) {
			LinkedHashMap<String, Object> rowMap = BadgeDetailService.badgeToListRowMap(e);
			badgeDetailService.applyOutsideLangAndFormatForListRow(rowMap, langTag);
			if (nameSearchActive) {
				long bid = e.getBadgeId() != null ? e.getBadgeId() : 0L;
				long rank =
						Objects.equals(String.valueOf(rowMap.get("badge_name")), badgeNameQuery) ? -1L : bid;
				rowMap.put("rank", rank);
			}
			rows.add(BadgeDetailService.ksortRowMap(rowMap));
		}

		if (nameSearchActive) {
			rows.sort(
					Comparator.comparingLong((LinkedHashMap<String, Object> m) -> longFromMap(m, "rank"))
							.thenComparingLong(m -> longFromMap(m, "badge_id")));
		}

		try {
			log.debug("badge list size={} total={}", rows.size(), p.getTotal());
		} catch (Exception ignored) {
			// debug only
		}

		Map<String, Object> body = new TreeMap<>();
		body.put("list", rows);
		body.put("total_count", p.getTotal());
		return body;
	}

	private static long longFromMap(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer parseIsTop(String raw) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void applySort(LambdaQueryWrapper<Badge> wrapper, String sort) {
		if (sort == null || sort.trim().isEmpty()) {
			wrapper.orderByAsc(Badge::getPOrder).orderByDesc(Badge::getCreated);
			return;
		}
		String trimmed = sort.trim();
		int sp = trimmed.indexOf(' ');
		if (sp <= 0 || sp >= trimmed.length() - 1) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String field = trimmed.substring(0, sp).trim();
		String direction = trimmed.substring(sp + 1).trim();
		if (!StringUtils.hasText(field) || !StringUtils.hasText(direction)) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String dirLower = direction.toLowerCase();
		boolean asc;
		if ("asc".equals(dirLower)) {
			asc = true;
		} else if ("desc".equals(dirLower)) {
			asc = false;
		} else {
			throw new BadRequestException("sort 参数格式无效");
		}
		switch (field) {
			case "p_order" -> {
				if (asc) {
					wrapper.orderByAsc(Badge::getPOrder);
				} else {
					wrapper.orderByDesc(Badge::getPOrder);
				}
			}
			case "created" -> {
				if (asc) {
					wrapper.orderByAsc(Badge::getCreated);
				} else {
					wrapper.orderByDesc(Badge::getCreated);
				}
			}
			case "updated" -> {
				if (asc) {
					wrapper.orderByAsc(Badge::getUpdated);
				} else {
					wrapper.orderByDesc(Badge::getUpdated);
				}
			}
			case "badge_id" -> {
				if (asc) {
					wrapper.orderByAsc(Badge::getBadgeId);
				} else {
					wrapper.orderByDesc(Badge::getBadgeId);
				}
			}
			case "user_id" -> {
				if (asc) {
					wrapper.orderByAsc(Badge::getUserId);
				} else {
					wrapper.orderByDesc(Badge::getUserId);
				}
			}
			case "is_top" -> {
				if (asc) {
					wrapper.orderByAsc(Badge::getIsTop);
				} else {
					wrapper.orderByDesc(Badge::getIsTop);
				}
			}
			case "status" -> {
				if (asc) {
					wrapper.orderByAsc(Badge::getStatus);
				} else {
					wrapper.orderByDesc(Badge::getStatus);
				}
			}
			default -> throw new BadRequestException("sort 参数格式无效");
		}
		if (!"p_order".equals(field)) {
			wrapper.orderByAsc(Badge::getPOrder);
		}
	}
}
