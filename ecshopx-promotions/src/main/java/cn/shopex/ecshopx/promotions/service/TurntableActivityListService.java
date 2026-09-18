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

import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LuckyDrawActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TurntableActivityListService {

	private final LuckyDrawActivityMapper luckyDrawActivityMapper;
	private final LuckyDrawActivityOutsideMultiLangReadService luckyDrawActivityOutsideMultiLangReadService;

	public TurntableActivityListService(
			LuckyDrawActivityMapper luckyDrawActivityMapper,
			LuckyDrawActivityOutsideMultiLangReadService luckyDrawActivityOutsideMultiLangReadService) {
		this.luckyDrawActivityMapper = luckyDrawActivityMapper;
		this.luckyDrawActivityOutsideMultiLangReadService = luckyDrawActivityOutsideMultiLangReadService;
	}

	public Map<String, Object> getDrawActivityList(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String activityId,
			String activityName,
			String status,
			String requestLangTag) {
		long now = Instant.now().getEpochSecond();
		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);

		LambdaQueryWrapper<LuckyDrawActivity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LuckyDrawActivity::getCompanyId, companyId);

		if (activityId != null && !"".equals(activityId) && !"0".equals(activityId)) {
			try {
				long id = Long.parseLong(activityId.trim());
				wrapper.eq(LuckyDrawActivity::getId, id);
			} catch (NumberFormatException ignored) {
				// omit id filter
			}
		}

		if (activityName != null && !"".equals(activityName) && !"0".equals(activityName)) {
			String escaped = escapeSqlLike(activityName);
			wrapper.apply("activity_name LIKE CONCAT('%',{0},'%') ESCAPE '\\\\'", escaped);
		}

		if (Objects.equals(status, "notstart")) {
			wrapper.gt(LuckyDrawActivity::getBeginTime, now);
		}
		if (Objects.equals(status, "expire")) {
			wrapper.le(LuckyDrawActivity::getEndTime, now);
		}
		if (Objects.equals(status, "online")) {
			wrapper.le(LuckyDrawActivity::getBeginTime, now).gt(LuckyDrawActivity::getEndTime, now);
		}

		Long count = luckyDrawActivityMapper.selectCount(wrapper);
		Page<LuckyDrawActivity> pageReq = new Page<>(page, pageSize, false);
		Page<LuckyDrawActivity> pageResult = luckyDrawActivityMapper.selectPage(pageReq, wrapper);

		List<Map<String, Object>> rows = new ArrayList<>();
		for (LuckyDrawActivity row : pageResult.getRecords()) {
			LinkedHashMap<String, Object> map = toRowMap(row);
			long dataId = row.getId() == null ? 0L : row.getId();
			luckyDrawActivityOutsideMultiLangReadService.applyActivityNameIntro(
					companyId, dataId, map, requestLangTag);
			long beginTime = row.getBeginTime() == null ? 0L : row.getBeginTime();
			long endTime = row.getEndTime() == null ? 0L : row.getEndTime();
			if (beginTime > now) {
				map.put("status", "notstart");
			} else if (endTime <= now) {
				map.put("status", "expire");
			} else {
				map.put("status", "online");
			}
			rows.add(map);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", count);
		out.put("list", rows);
		return out;
	}

	private static LinkedHashMap<String, Object> toRowMap(LuckyDrawActivity row) {
		LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
		detail.put("id", row.getId());
		detail.put("company_id", row.getCompanyId());
		detail.put("begin_time", row.getBeginTime());
		detail.put("end_time", row.getEndTime());
		detail.put("cost_value", row.getCostValue());
		detail.put("activity_type", row.getActivityType());
		detail.put("activity_name", row.getActivityName());
		detail.put("activity_template_config", row.getActivityTemplateConfig());
		detail.put("prize_data", row.getPrizeData());
		detail.put("intro", row.getIntro());
		detail.put("config_version", row.getConfigVersion() == null ? 1L : row.getConfigVersion());
		detail.put("created", row.getCreated());
		detail.put("updated", row.getUpdated());
		detail.put("limit_total", row.getLimitTotal());
		detail.put("limit_day", row.getLimitDay());
		return detail;
	}

	private static int parsePage(String pageRaw) {
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			return 1;
		}
		try {
			int v = Integer.parseInt(pageRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			return 10;
		}
		try {
			int v = Integer.parseInt(pageSizeRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 10;
		}
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
