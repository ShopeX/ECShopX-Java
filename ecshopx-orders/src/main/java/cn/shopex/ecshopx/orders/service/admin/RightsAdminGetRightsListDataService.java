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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.service.reservation.RightsTimesCardRowFactory;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.service.ResourceLevelGetService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RightsAdminGetRightsListDataService {

	private static final List<String> RESERVATION_STATUSES =
			List.of("system", "success", "not_to_shop", "to_the_shop");

	private final RightsMapper rightsMapper;
	private final ReservationRecordMapper reservationRecordMapper;
	private final ResourceLevelGetService resourceLevelGetService;
	private final RightsTimesCardRowFactory rightsTimesCardRowFactory;

	public RightsAdminGetRightsListDataService(
			RightsMapper rightsMapper,
			ReservationRecordMapper reservationRecordMapper,
			ResourceLevelGetService resourceLevelGetService,
			RightsTimesCardRowFactory rightsTimesCardRowFactory) {
		this.rightsMapper = rightsMapper;
		this.reservationRecordMapper = reservationRecordMapper;
		this.resourceLevelGetService = resourceLevelGetService;
		this.rightsTimesCardRowFactory = rightsTimesCardRowFactory;
	}

	public Map<String, Object> getRightsListData(
			long companyId,
			long userId,
			int page,
			int pageSize,
			Long optionalRightsId,
			Integer optionalEndTimeEpochSec,
			String optionalResourceLevelIdRaw) {
		int nowEpochSec = (int) (System.currentTimeMillis() / 1000L);

		int p = page < 1 ? 1 : page;
		int ps = pageSize;
		if (ps > 100) {
			ps = 100;
		}
		if (ps < 1) {
			ps = 1;
		}

		LambdaQueryWrapper<Rights> w = Wrappers.lambdaQuery();
		w.eq(Rights::getCompanyId, companyId);
		w.eq(Rights::getUserId, userId);
		if (optionalRightsId != null) {
			w.eq(Rights::getRightsId, optionalRightsId);
		}
		if (optionalEndTimeEpochSec != null) {
			w.gt(Rights::getEndTime, optionalEndTimeEpochSec);
		}
		w.orderByAsc(Rights::getEndTime);

		Page<Rights> mpPage = new Page<>(p, ps);
		Page<Rights> rightsPage = rightsMapper.selectPage(mpPage, w);

		List<Map<String, Object>> working = new ArrayList<>();
		for (Rights r : rightsPage.getRecords()) {
			working.add(rightsTimesCardRowFactory.toTimesCardRow(r, nowEpochSec));
		}

		List<Map<String, Object>> filtered;
		if (working.isEmpty()) {
			filtered = new ArrayList<>();
		} else {
			List<Long> rightsIds = new ArrayList<>();
			for (Map<String, Object> row : working) {
				Long rid = longFromMap(row.get("rights_id"));
				if (rid != null) {
					rightsIds.add(rid);
				}
			}

			Map<Long, Integer> reservationCountByRightsId = new HashMap<>();
			if (!rightsIds.isEmpty()) {
				LambdaQueryWrapper<ReservationRecord> rw = Wrappers.lambdaQuery();
				rw.eq(ReservationRecord::getCompanyId, companyId);
				rw.eq(ReservationRecord::getUserId, userId);
				rw.in(ReservationRecord::getRightsId, rightsIds);
				rw.in(ReservationRecord::getStatus, RESERVATION_STATUSES);
				Page<ReservationRecord> pr = new Page<>(1, 100);
				reservationRecordMapper.selectPage(pr, rw);
				for (ReservationRecord rec : pr.getRecords()) {
					if (rec.getRightsId() == null) {
						continue;
					}
					reservationCountByRightsId.merge(rec.getRightsId(), 1, Integer::sum);
				}
			}

			filtered = new ArrayList<>();
			for (Map<String, Object> val : working) {
				Long rightsId = longFromMap(val.get("rights_id"));
				int count = rightsId != null ? reservationCountByRightsId.getOrDefault(rightsId, 0) : 0;
				boolean isValid = Boolean.TRUE.equals(val.get("is_valid"));
				Integer isNotLimitNum = intObject(val.get("is_not_limit_num"));

				if (rightsId != null
						&& reservationCountByRightsId.containsKey(rightsId)
						&& isValid
						&& isNotLimitNum != null
						&& isNotLimitNum == 2) {
					long totalNum = longFromObject(val.get("total_num"));
					long totalConsumNum = longFromObject(val.get("total_consum_num"));
					long surplus = totalNum - totalConsumNum;
					if (surplus == 0 || totalNum <= count) {
						val.put("is_valid", Boolean.FALSE);
						isValid = false;
					}
				}

				if (!isValid || !isCanReservationTrue(val.get("can_reservation"))) {
					continue;
				}

				Object labelInfos = val.get("label_infos");
				if (labelInfos instanceof List<?> list && !list.isEmpty()) {
					Object first = list.get(0);
					if (first instanceof Map<?, ?> lm) {
						Object lid = lm.get("label_id");
						Object lname = lm.get("label_name");
						if (lid != null) {
							val.put("label_id", lid instanceof Number n ? n.longValue() : lid);
						}
						if (lname != null) {
							val.put("label_name", Objects.toString(lname, ""));
						}
					}
				}
				filtered.add(val);
			}
		}

		filtered.sort(Comparator.comparing((Map<String, Object> m) -> !Boolean.TRUE.equals(m.get("is_valid"))));

		String levelRaw = optionalResourceLevelIdRaw == null ? "" : optionalResourceLevelIdRaw.trim();
		Object rlObj = resourceLevelGetService.getResourceLevel(companyId, levelRaw);
		List<Long> materialIds;
		if (rlObj instanceof Map<?, ?> rlMap) {
			Object mid = rlMap.get("materialIds");
			if (mid instanceof List<?>) {
				materialIds = new ArrayList<>();
				for (Object el : (List<?>) mid) {
					Long parsed = longFromMap(el);
					if (parsed != null) {
						materialIds.add(parsed);
					}
				}
			} else {
				materialIds = List.of();
			}
		} else {
			materialIds = List.of();
		}

		Set<Long> materialIdSet = new HashSet<>(materialIds);
		List<Map<String, Object>> afterResource = new ArrayList<>();
		for (Map<String, Object> row : filtered) {
			Object labelInfos = row.get("label_infos");
			if (!(labelInfos instanceof List<?> list) || list.isEmpty()) {
				afterResource.add(row);
				continue;
			}
			boolean keep = true;
			for (Object el : list) {
				if (!(el instanceof Map<?, ?> lm)) {
					keep = false;
					break;
				}
				Long lid = longFromMap(lm.get("label_id"));
				if (lid == null || !materialIdSet.contains(lid)) {
					keep = false;
					break;
				}
			}
			if (keep) {
				afterResource.add(row);
			}
		}
		filtered = afterResource;

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", filtered);
		return out;
	}

	private static Long longFromMap(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longFromObject(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer intObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isCanReservationTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}
}
