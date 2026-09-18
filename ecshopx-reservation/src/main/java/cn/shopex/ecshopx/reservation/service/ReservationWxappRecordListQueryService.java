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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ResourceLevel;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReservationWxappRecordListQueryService {

	private final ReservationRecordMapper reservationRecordMapper;

	private final ResourceLevelMapper resourceLevelMapper;

	public ReservationWxappRecordListQueryService(
			ReservationRecordMapper reservationRecordMapper, ResourceLevelMapper resourceLevelMapper) {
		this.reservationRecordMapper = reservationRecordMapper;
		this.resourceLevelMapper = resourceLevelMapper;
	}

	public Map<String, Object> query(Map<String, Object> auth, String pageParam, String pageSizeParam) {
		long companyId = toLong(auth.get("company_id"));
		Long userId = ReservationWxappGetRecordCountService.resolveQueryUserId(auth.get("user_id"));
		if (userId == null) {
			throw new UnauthorizedException("未登录");
		}

		int page = parsePositiveIntOrDefault(pageParam, 1);
		int pageSize = parsePositiveIntOrDefault(pageSizeParam, 1);

		LambdaQueryWrapper<ReservationRecord> w = Wrappers.lambdaQuery();
		w.eq(ReservationRecord::getCompanyId, companyId);
		w.eq(ReservationRecord::getUserId, userId);

		Long totalLong = reservationRecordMapper.selectCount(w);
		int totalCount = totalLong == null ? 0 : totalLong.intValue();

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);

		if (totalCount == 0) {
			out.put("list", List.of());
			return out;
		}

		w.orderByDesc(ReservationRecord::getAgreementDate).orderByAsc(ReservationRecord::getToShopTime);
		Page<ReservationRecord> pg = new Page<>(page, pageSize, false);
		reservationRecordMapper.selectPage(pg, w);

		List<Map<String, Object>> rows = new ArrayList<>();
		for (ReservationRecord rec : pg.getRecords()) {
			rows.add(reservationRecordToMap(rec));
		}

		Map<Long, String> levelIdToImageUrl = new HashMap<>();
		Set<Long> levelIds = new LinkedHashSet<>();
		for (ReservationRecord rec : pg.getRecords()) {
			Long rid = rec.getResourceLevelId();
			if (rid != null) {
				levelIds.add(rid);
			}
		}
		if (!levelIds.isEmpty()) {
			LambdaQueryWrapper<ResourceLevel> lw = Wrappers.lambdaQuery();
			lw.in(ResourceLevel::getResourceLevelId, levelIds);
			Page<ResourceLevel> levelPage = new Page<>(1, 100, false);
			resourceLevelMapper.selectPage(levelPage, lw);
			for (ResourceLevel rl : levelPage.getRecords()) {
				levelIdToImageUrl.put(rl.getResourceLevelId(), rl.getImageUrl());
			}
		}

		for (Map<String, Object> row : rows) {
			Object ridObj = row.get("resourceLevelId");
			if (ridObj == null) {
				continue;
			}
			Long rid = ridObj instanceof Number n ? n.longValue() : null;
			if (rid == null) {
				try {
					rid = Long.parseLong(ridObj.toString().trim());
				} catch (NumberFormatException e) {
					continue;
				}
			}
			if (levelIdToImageUrl.containsKey(rid)) {
				row.put("imageUrl", levelIdToImageUrl.get(rid));
			}
		}

		out.put("list", rows);
		return out;
	}

	private static Map<String, Object> reservationRecordToMap(ReservationRecord rec) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("recordId", rec.getRecordId());
		m.put("companyId", rec.getCompanyId());
		m.put("shopId", rec.getShopId());
		m.put("shopName", rec.getShopName());
		m.put("agreementDate", rec.getAgreementDate());
		m.put("toShopTime", rec.getToShopTime());
		m.put("beginTime", rec.getBeginTime());
		m.put("endTime", rec.getEndTime());
		m.put("status", rec.getStatus());
		m.put("num", rec.getNum());
		m.put("userId", rec.getUserId());
		m.put("userName", rec.getUserName());
		m.put("sex", rec.getSex());
		m.put("mobile", rec.getMobile());
		m.put("resourceLevelId", rec.getResourceLevelId());
		m.put("resourceLevelName", rec.getResourceLevelName());
		m.put("rightsId", rec.getRightsId());
		m.put("rightsName", rec.getRightsName());
		m.put("labelId", rec.getLabelId());
		m.put("labelName", rec.getLabelName());
		m.put("created", rec.getCreated());
		m.put("updated", rec.getUpdated());
		return m;
	}

	private static int parsePositiveIntOrDefault(String raw, int defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return defaultVal;
		}
		try {
			int v = Integer.parseInt(t);
			return v < 1 ? defaultVal : v;
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
