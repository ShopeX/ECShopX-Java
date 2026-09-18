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

import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.domain.ResourceLevel;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReservationListQueryService {

	private static final List<String> RECORD_STATUSES =
			List.of("system", "success", "not_to_shop", "to_the_shop");

	private final ReservationSettingQueryService reservationSettingQueryService;
	private final ResourceLevelMapper resourceLevelMapper;
	private final ReservationRecordMapper reservationRecordMapper;

	public ReservationListQueryService(
			ReservationSettingQueryService reservationSettingQueryService,
			ResourceLevelMapper resourceLevelMapper,
			ReservationRecordMapper reservationRecordMapper) {
		this.reservationSettingQueryService = reservationSettingQueryService;
		this.resourceLevelMapper = resourceLevelMapper;
		this.reservationRecordMapper = reservationRecordMapper;
	}

	/**
	 * @param shopIdOrNull {@code null} 表示查询条件为 {@code shop_id} IS NULL；非 null 时按该门店 id 等值过滤。
	 */
	public Map<String, Object> getReservationList(
			long companyId, Long shopIdOrNull, int agreementDateEpoch, int page, int pageSize) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("list", new ArrayList<>());
		result.put("total_count", 0);

		ReservationSetting setting =
				reservationSettingQueryService.findByCompanyId(companyId).orElse(null);
		if (setting == null) {
			return result;
		}

		List<Map<String, Object>> listMaps = null;
		Long count = null;

		if (Integer.valueOf(1).equals(setting.getReservationMode())) {
			LambdaQueryWrapper<ResourceLevel> rlw = resourceLevelWrapper(companyId, shopIdOrNull);
			Page<ResourceLevel> pg = new Page<>(page, pageSize, false);
			pg.setOptimizeCountSql(true);
			Page<ResourceLevel> pageResult = resourceLevelMapper.selectPage(pg, rlw);
			listMaps = new ArrayList<>();
			for (ResourceLevel rl : pageResult.getRecords()) {
				listMaps.add(resourceLevelToMap(rl));
			}
			count = resourceLevelMapper.selectCount(rlw);
		}

		LambdaQueryWrapper<ReservationRecord> recW = recordWrapper(companyId, shopIdOrNull, agreementDateEpoch);
		Page<ReservationRecord> page100 = new Page<>(1, 100, false);
		page100.setOptimizeCountSql(true);
		Page<ReservationRecord> recordPage = reservationRecordMapper.selectPage(page100, recW);
		List<Map<String, Object>> recordMaps = new ArrayList<>();
		for (ReservationRecord rec : recordPage.getRecords()) {
			recordMaps.add(reservationRecordToMap(rec));
		}
		long recordCount = reservationRecordMapper.selectCount(recW);

		boolean listEmpty = listMaps == null || listMaps.isEmpty();
		if (recordMaps.isEmpty() && listEmpty) {
			return result;
		}
		if (listEmpty) {
			result.put("list", recordMaps);
			result.put("total_count", recordCount);
			return result;
		}
		if (recordMaps.isEmpty()) {
			result.put("list", listMaps);
			result.put("total_count", count);
			return result;
		}

		for (Map<String, Object> resource : listMaps) {
			for (Map<String, Object> record : recordMaps) {
				if (looseEqualsResourceLevelId(record.get("resourceLevelId"), resource.get("resourceLevelId"))) {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> nested =
							(List<Map<String, Object>>)
									resource.computeIfAbsent("record", k -> new ArrayList<Map<String, Object>>());
					nested.add(record);
				}
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> nested = (List<Map<String, Object>>) resource.get("record");
			if (nested != null && !nested.isEmpty()) {
				List<String> timedata = new ArrayList<>();
				for (Map<String, Object> r : nested) {
					Object bt = r.get("beginTime");
					timedata.add(bt == null ? null : bt.toString());
				}
				resource.put("timedata", timedata);
			}
		}
		result.put("list", listMaps);
		result.put("total_count", count);
		return result;
	}

	private static LambdaQueryWrapper<ResourceLevel> resourceLevelWrapper(long companyId, Long shopIdOrNull) {
		LambdaQueryWrapper<ResourceLevel> w = new LambdaQueryWrapper<>();
		w.eq(ResourceLevel::getCompanyId, String.valueOf(companyId));
		w.eq(ResourceLevel::getStatus, "active");
		if (shopIdOrNull == null) {
			w.isNull(ResourceLevel::getShopId);
		} else {
			w.eq(ResourceLevel::getShopId, String.valueOf(shopIdOrNull));
		}
		w.orderByDesc(ResourceLevel::getResourceLevelId);
		return w;
	}

	private static LambdaQueryWrapper<ReservationRecord> recordWrapper(
			long companyId, Long shopIdOrNull, int agreementDateEpoch) {
		LambdaQueryWrapper<ReservationRecord> w = new LambdaQueryWrapper<>();
		w.eq(ReservationRecord::getCompanyId, companyId);
		if (shopIdOrNull == null) {
			w.isNull(ReservationRecord::getShopId);
		} else {
			w.eq(ReservationRecord::getShopId, shopIdOrNull);
		}
		w.eq(ReservationRecord::getAgreementDate, agreementDateEpoch);
		w.in(ReservationRecord::getStatus, RECORD_STATUSES);
		w.orderByDesc(ReservationRecord::getAgreementDate).orderByAsc(ReservationRecord::getToShopTime);
		return w;
	}

	private static Map<String, Object> resourceLevelToMap(ResourceLevel rl) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("companyId", rl.getCompanyId());
		m.put("resourceLevelId", rl.getResourceLevelId());
		m.put("shopId", rl.getShopId());
		m.put("shopName", rl.getShopName());
		m.put("name", rl.getName());
		m.put("description", rl.getDescription());
		m.put("status", rl.getStatus());
		m.put("imageUrl", rl.getImageUrl());
		m.put("quantity", rl.getQuantity());
		m.put("created", rl.getCreated());
		m.put("updated", rl.getUpdated());
		return m;
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

	private static boolean looseEqualsResourceLevelId(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a instanceof Number na && b instanceof Number nb) {
			return na.longValue() == nb.longValue();
		}
		if (a instanceof Number na && b instanceof CharSequence csb) {
			return numberEqualsParsedLong(na, csb);
		}
		if (b instanceof Number nb && a instanceof CharSequence csa) {
			return numberEqualsParsedLong(nb, csa);
		}
		if (a instanceof CharSequence csa && b instanceof CharSequence csb) {
			Long la = tryParseLong(csa);
			Long lb = tryParseLong(csb);
			if (la == null || lb == null) {
				return false;
			}
			return la.equals(lb);
		}
		return false;
	}

	private static boolean numberEqualsParsedLong(Number n, CharSequence cs) {
		Long parsed = tryParseLong(cs);
		if (parsed == null) {
			return false;
		}
		return n.longValue() == parsed;
	}

	private static Long tryParseLong(CharSequence cs) {
		String t = cs.toString().trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
