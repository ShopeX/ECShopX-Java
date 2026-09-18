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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionGroupsActivityItemMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsActivityListService {

	private static final int IN_CHUNK_SIZE = 500;

	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;
	private final PromotionGroupsActivityItemMultiLangReadService promotionGroupsActivityItemMultiLangReadService;
	private final JdbcTemplate jdbcTemplate;
	private final MessageSource messageSource;

	public PromotionGroupsActivityListService(
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler,
			PromotionGroupsActivityItemMultiLangReadService promotionGroupsActivityItemMultiLangReadService,
			JdbcTemplate jdbcTemplate,
			MessageSource messageSource) {
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsActivityAdminRowAssembler = promotionGroupsActivityAdminRowAssembler;
		this.promotionGroupsActivityItemMultiLangReadService = promotionGroupsActivityItemMultiLangReadService;
		this.jdbcTemplate = jdbcTemplate;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getPromotionGroupsActivityList(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String keywords,
			Integer viewForTimeFilter,
			String groupGoodsTypeFilter,
			String requestLangTag) {
		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);
		long now = Instant.now().getEpochSecond();

		LambdaQueryWrapper<PromotionGroupsActivity> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(PromotionGroupsActivity::getCompanyId, companyId)
				.eq(PromotionGroupsActivity::getDisabled, Boolean.FALSE);
		if (StringUtils.hasText(keywords)) {
			wrapper.eq(PromotionGroupsActivity::getActName, keywords.trim());
		}
		if (groupGoodsTypeFilter != null && StringUtils.hasText(groupGoodsTypeFilter)) {
			wrapper.eq(PromotionGroupsActivity::getGroupGoodsType, groupGoodsTypeFilter.trim());
		}
		if (viewForTimeFilter != null && viewForTimeFilter != 0) {
			switch (viewForTimeFilter) {
				case 1 -> wrapper
						.gt(PromotionGroupsActivity::getBeginTime, now)
						.gt(PromotionGroupsActivity::getEndTime, now);
				case 2 -> wrapper
						.le(PromotionGroupsActivity::getBeginTime, now)
						.ge(PromotionGroupsActivity::getEndTime, now);
				case 3 -> wrapper.lt(PromotionGroupsActivity::getEndTime, now);
				default -> {
					/* no extra time filter */
				}
			}
		}
		wrapper.orderByDesc(PromotionGroupsActivity::getCreated);

		Long total = promotionGroupsActivityMapper.selectCount(wrapper);
		Page<PromotionGroupsActivity> p = new Page<>(page, pageSize, false);
		promotionGroupsActivityMapper.selectPage(p, wrapper);
		List<PromotionGroupsActivity> records = p.getRecords();

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (records.isEmpty()) {
			out.put("list", List.of());
			return out;
		}

		List<Map<String, Object>> list = new ArrayList<>();
		Map<Long, Map<String, Object>> rowByActivityId = new LinkedHashMap<>();
		List<Long> activityIds = new ArrayList<>();
		for (PromotionGroupsActivity e : records) {
			Map<String, Object> row = promotionGroupsActivityAdminRowAssembler.toRow(e, (int) now);
			list.add(row);
			Long id = e.getGroupsActivityId();
			if (id != null) {
				rowByActivityId.put(id, row);
				activityIds.add(id);
			}
		}

		promotionGroupsActivityItemMultiLangReadService.applyBatch(
				companyId, activityIds, rowByActivityId, requestLangTag);

		List<Long> goodsIds =
				records.stream()
						.map(PromotionGroupsActivity::getGoodsId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		Map<Long, String> itemNameById = loadItemNamesByCompanyAndItemIds(companyId, goodsIds);

		for (int i = 0; i < records.size(); i++) {
			PromotionGroupsActivity e = records.get(i);
			Map<String, Object> row = list.get(i);
			Long goodsId = e.getGoodsId();
			String name = goodsId == null ? null : itemNameById.get(goodsId);
			if (StringUtils.hasText(name)) {
				row.put("goods_name", name);
			} else {
				row.put(
						"goods_name",
						messageSource.getMessage(
								"promotions.groups.invalid_product", null, LocaleContextHolder.getLocale()));
			}
			long begin = e.getBeginTime() == null ? 0L : e.getBeginTime();
			long end = e.getEndTime() == null ? 0L : e.getEndTime();
			int activityStatus;
			if (begin > now) {
				activityStatus = 1;
			} else if (end < now) {
				activityStatus = 3;
			} else {
				activityStatus = 2;
			}
			row.put("activity_status", activityStatus);
		}

		out.put("list", list);
		return out;
	}

	private Map<Long, String> loadItemNamesByCompanyAndItemIds(long companyId, List<Long> itemIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return out;
		}
		List<Long> distinct = itemIds.stream().distinct().toList();
		for (int i = 0; i < distinct.size(); i += IN_CHUNK_SIZE) {
			List<Long> chunk = distinct.subList(i, Math.min(i + IN_CHUNK_SIZE, distinct.size()));
			String placeholders = chunk.stream().map(id -> "?").collect(Collectors.joining(","));
			String sql =
					"SELECT item_id, item_name FROM items WHERE company_id = ? AND item_id IN ("
							+ placeholders
							+ ")";
			List<Object> args = new ArrayList<>();
			args.add(companyId);
			args.addAll(chunk);
			jdbcTemplate.query(
					sql,
					rs -> {
						long itemId = rs.getLong("item_id");
						String itemName = rs.getString("item_name");
						if (itemName != null) {
							out.put(itemId, itemName);
						}
					},
					args.toArray());
		}
		return out;
	}

	private static int parsePage(String pageRaw) {
		if (!StringUtils.hasText(pageRaw)) {
			return 1;
		}
		try {
			return Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSize(String pageSizeRaw) {
		if (!StringUtils.hasText(pageSizeRaw)) {
			return 20;
		}
		try {
			return Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			return 20;
		}
	}
}
