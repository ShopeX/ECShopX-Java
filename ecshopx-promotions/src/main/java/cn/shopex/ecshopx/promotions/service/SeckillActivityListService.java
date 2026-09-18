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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.support.SeckillActivityPayloadSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SeckillActivityListService {

	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillActivityCreateService seckillActivityCreateService;
	private final SeckillActivityOutsideMultiLangReadService seckillActivityOutsideMultiLangReadService;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final ObjectMapper objectMapper;

	public SeckillActivityListService(
			SeckillActivityMapper seckillActivityMapper,
			SeckillActivityCreateService seckillActivityCreateService,
			SeckillActivityOutsideMultiLangReadService seckillActivityOutsideMultiLangReadService,
			DistributorListQueryService distributorListQueryService,
			DistributorListRowFormatService distributorListRowFormatService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			ObjectMapper objectMapper) {
		this.seckillActivityMapper = seckillActivityMapper;
		this.seckillActivityCreateService = seckillActivityCreateService;
		this.seckillActivityOutsideMultiLangReadService = seckillActivityOutsideMultiLangReadService;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSeckillActivityList(
			long companyId,
			long sourceIdFromQuery,
			String rawDistributorIdQueryParam,
			Map<String, String> input,
			String requestLangTag) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<SeckillActivity> w = new LambdaQueryWrapper<>();
		w.eq(SeckillActivity::getCompanyId, companyId);

		if (input.containsKey("item_title")) {
			String value = input.get("item_title");
			if (StringUtils.hasText(value != null ? value.trim() : "")) {
				w.apply(
						"EXISTS (SELECT 1 FROM promotions_seckill_rel_goods g WHERE g.company_id = {0} AND g.seckill_id = promotions_seckill_activity.seckill_id AND g.item_title LIKE CONCAT('%', {1}, '%') ESCAPE '\\\\')",
						companyId,
						escapeSqlLikeForSeckillList(value.trim()));
			}
		}

		if (queryStringLooksMeaningful(rawDistributorIdQueryParam)) {
			w.eq(SeckillActivity::getDistributorId, "," + rawDistributorIdQueryParam.trim() + ",");
		}

		if (input.containsKey("status")) {
			String status = input.get("status");
			if (StringUtils.hasText(status != null ? status.trim() : "")) {
				applyStatusFilter(w, status.trim(), now);
			}
		}

		if (input.containsKey("seckill_id")) {
			String sid = input.get("seckill_id");
			if (StringUtils.hasText(sid != null ? sid.trim() : "")) {
				try {
					long id = Long.parseLong(sid.trim());
					if (id > 0L) {
						w.eq(SeckillActivity::getSeckillId, id);
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}

		String activityNameCandidate = null;
		if (input.containsKey("keywords")) {
			String kw = input.get("keywords");
			if (StringUtils.hasText(kw != null ? kw.trim() : "")) {
				activityNameCandidate = kw.trim();
			}
		}
		if (input.containsKey("name")) {
			String nm = input.get("name");
			if (StringUtils.hasText(nm != null ? nm.trim() : "")) {
				activityNameCandidate = nm.trim();
			}
		}
		if (activityNameCandidate != null) {
			w.apply(
					"activity_name LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'",
					escapeSqlLikeForSeckillList(activityNameCandidate));
		}

		if (input.containsKey("is_free_shipping")) {
			String v = input.get("is_free_shipping");
			if (queryParamLooksMeaningful(v)) {
				w.eq(SeckillActivity::getIsFreeShipping, true);
			}
		}

		if (input.containsKey("start_time") && input.containsKey("end_time")) {
			String st = input.get("start_time");
			String et = input.get("end_time");
			if (StringUtils.hasText(st != null ? st.trim() : "")
					&& StringUtils.hasText(et != null ? et.trim() : "")) {
				try {
					int startParsed = Integer.parseInt(st.trim());
					int endParsed = Integer.parseInt(et.trim());
					w.ge(SeckillActivity::getActivityReleaseTime, startParsed)
							.le(SeckillActivity::getActivityEndTime, endParsed);
				} catch (NumberFormatException ignored) {
				}
			}
		}

		String stype = input.get("seckill_type");
		String st = StringUtils.hasText(stype != null ? stype.trim() : "") ? stype.trim() : "normal";
		w.eq(SeckillActivity::getSeckillType, st);

		int pageNo = parseLoosePositiveInt(input.get("page"), 1);
		int pageSize = parseLoosePositiveInt(input.get("pageSize"), 20);

		w.orderByDesc(SeckillActivity::getSeckillId);

		Page<SeckillActivity> page = new Page<>(pageNo, pageSize);
		seckillActivityMapper.selectPage(page, w);
		long totalCount = page.getTotal();
		if (totalCount == 0L) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		for (SeckillActivity e : page.getRecords()) {
			Map<String, Object> row =
					seckillActivityCreateService.assembleAdminSeckillReadPayload(e, List.of());
			row.remove("items");
			rows.add(row);
		}

		seckillActivityOutsideMultiLangReadService.applyBatch(companyId, rows, requestLangTag);

		for (Map<String, Object> row : rows) {
			applyAdminSeckillBooleanStringFields(row);
		}

		for (Map<String, Object> row : rows) {
			row.put("source_name", "");
		}

		LinkedHashSet<Long> sourceDistributorIds = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			if (!"distributor".equals(Objects.toString(row.get("source_type"), ""))) {
				continue;
			}
			long sid = rowSourceIdFromRow(row);
			if (sid > 0L) {
				sourceDistributorIds.add(sid);
			}
		}
		Map<Long, String> sourceNameById = new LinkedHashMap<>();
		if (!sourceDistributorIds.isEmpty()) {
			List<Distributor> sourceDists =
					distributorListQueryService.listByIdsAndCompany(companyId, new ArrayList<>(sourceDistributorIds));
			for (Distributor d : sourceDists) {
				if (d.getDistributorId() != null) {
					sourceNameById.put(d.getDistributorId(), d.getName() == null ? "" : d.getName());
				}
			}
		}
		for (Map<String, Object> row : rows) {
			if (!"distributor".equals(Objects.toString(row.get("source_type"), ""))) {
				continue;
			}
			long sid = rowSourceIdFromRow(row);
			String nm = sourceNameById.get(sid);
			row.put("source_name", nm == null ? "" : nm);
		}

		LinkedHashSet<Long> allDistIds = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			@SuppressWarnings("unchecked")
			List<String> distList = (List<String>) row.get("distributor_id");
			if (distList == null || distList.isEmpty()) {
				continue;
			}
			for (String s : distList) {
				if (!StringUtils.hasText(s)) {
					continue;
				}
				try {
					long id = Long.parseLong(s.trim());
					if (id > 0L) {
						allDistIds.add(id);
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}

		Map<Long, Map<String, Object>> distMap = new LinkedHashMap<>();
		if (!allDistIds.isEmpty()) {
			List<Distributor> distributors =
					distributorListQueryService.listByIdsAndCompany(companyId, new ArrayList<>(allDistIds));
			for (Distributor d : distributors) {
				long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
				if (did <= 0L) {
					continue;
				}
				int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
				Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
				distMap.put(did, distributorListRowFormatService.formatStoreRow(d, setting, objectMapper));
			}
		}

		for (Map<String, Object> row : rows) {
			long rowSourceId = rowSourceIdFromRow(row);
			String rowSourceType = Objects.toString(row.get("source_type"), "");
			String editBtn;
			if (rowSourceId != sourceIdFromQuery) {
				if ("staff".equals(rowSourceType) && sourceIdFromQuery == 0L) {
					editBtn = "Y";
				} else {
					editBtn = "N";
				}
			} else {
				editBtn = "Y";
			}
			row.put("edit_btn", editBtn);

			@SuppressWarnings("unchecked")
			List<String> distList = (List<String>) row.get("distributor_id");
			if (distList == null || distList.isEmpty()) {
				row.put("distributor_info", List.of());
				continue;
			}
			List<Map<String, Object>> distInfo = new ArrayList<>();
			for (String s : distList) {
				if (!StringUtils.hasText(s)) {
					continue;
				}
				try {
					long id = Long.parseLong(s.trim());
					Map<String, Object> formatted = distMap.get(id);
					if (formatted != null) {
						distInfo.add(formatted);
					}
				} catch (NumberFormatException ignored) {
				}
			}
			row.put("distributor_info", distInfo);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", rows);
		return out;
	}

	private static void applyStatusFilter(LambdaQueryWrapper<SeckillActivity> w, String status, int now) {
		switch (status) {
			case "waiting" -> w.ge(SeckillActivity::getActivityReleaseTime, now)
					.ge(SeckillActivity::getActivityEndTime, now)
					.eq(SeckillActivity::getDisabled, false);
			case "in_the_notice" -> w.le(SeckillActivity::getActivityReleaseTime, now)
					.gt(SeckillActivity::getActivityStartTime, now)
					.ge(SeckillActivity::getActivityEndTime, now)
					.eq(SeckillActivity::getDisabled, false);
			case "in_sale" -> w.le(SeckillActivity::getActivityStartTime, now)
					.gt(SeckillActivity::getActivityEndTime, now)
					.eq(SeckillActivity::getDisabled, false);
			case "it_has_ended" -> w.and(
					q -> q.le(SeckillActivity::getActivityEndTime, now)
							.or()
							.eq(SeckillActivity::getDisabled, true));
			case "valid" -> w.le(SeckillActivity::getActivityReleaseTime, now)
					.ge(SeckillActivity::getActivityEndTime, now)
					.eq(SeckillActivity::getDisabled, false);
			case "not_end" -> w.ge(SeckillActivity::getActivityEndTime, now)
					.eq(SeckillActivity::getDisabled, false);
			default -> {
			}
		}
	}

	private static int parseLoosePositiveInt(String raw, int defaultValue) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? defaultValue : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static boolean queryStringLooksMeaningful(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim()) || "0".equals(raw.trim())) {
			return false;
		}
		return true;
	}

	private static boolean queryParamLooksMeaningful(String v) {
		if (v == null || !StringUtils.hasText(v.trim())) {
			return false;
		}
		String trim = v.trim();
		if ("0".equals(trim) || "false".equalsIgnoreCase(trim)) {
			return false;
		}
		return true;
	}

	private static String escapeSqlLikeForSeckillList(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void applyAdminSeckillBooleanStringFields(Map<String, Object> row) {
		SeckillActivityPayloadSupport.applySeckillActivityBooleanStringFieldsForApi(row);
	}

	private static long rowSourceIdFromRow(Map<String, Object> row) {
		Object v = row.get("source_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
