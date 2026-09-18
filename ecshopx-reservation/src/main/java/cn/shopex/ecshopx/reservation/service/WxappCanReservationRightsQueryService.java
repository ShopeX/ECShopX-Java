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
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationWxappCanRightsListPort;
import cn.shopex.ecshopx.reservation.port.ReservationWxappCanRightsListPort.WxappCanRightsListResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WxappCanReservationRightsQueryService {

	private static final List<String> RESERVATION_STATUSES =
			List.of("system", "success", "not_to_shop", "to_the_shop");

	private final ReservationWxappCanRightsListPort wxappCanRightsListPort;

	private final ReservationRecordMapper reservationRecordMapper;

	public WxappCanReservationRightsQueryService(
			ReservationWxappCanRightsListPort wxappCanRightsListPort,
			ReservationRecordMapper reservationRecordMapper) {
		this.wxappCanRightsListPort = wxappCanRightsListPort;
		this.reservationRecordMapper = reservationRecordMapper;
	}

	public Map<String, Object> query(HttpServletRequest request, int page, int pageSize) {
		int now = (int) (System.currentTimeMillis() / 1000L);

		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}

		long companyId = toLong(auth.get("company_id"));
		Long userId = resolveQueryUserId(auth.get("user_id"));

		WxappCanRightsListResult rightsPage =
				wxappCanRightsListPort.queryPage(companyId, userId, now, page, pageSize);
		List<Map<String, Object>> rawList = rightsPage.getList();
		if (rawList == null || rawList.isEmpty()) {
			return Map.of("list", List.of());
		}

		List<Map<String, Object>> working = new ArrayList<>();
		for (Map<String, Object> row : rawList) {
			working.add(new LinkedHashMap<>(row));
		}

		List<Long> rightsIds = new ArrayList<>();
		for (Map<String, Object> row : working) {
			Long rid = longFromMap(row.get("rights_id"));
			if (rid != null) {
				rightsIds.add(rid);
			}
		}

		Map<Long, Integer> reservationCountByRightsId = new HashMap<>();
		if (!rightsIds.isEmpty()) {
			LambdaQueryWrapper<ReservationRecord> w = Wrappers.lambdaQuery();
			w.eq(ReservationRecord::getCompanyId, companyId);
			if (userId != null) {
				w.eq(ReservationRecord::getUserId, userId);
			}
			w.in(ReservationRecord::getRightsId, rightsIds);
			w.in(ReservationRecord::getStatus, RESERVATION_STATUSES);
			Page<ReservationRecord> p = new Page<>(1, 100);
			reservationRecordMapper.selectPage(p, w);
			for (ReservationRecord rec : p.getRecords()) {
				if (rec.getRightsId() == null) {
					continue;
				}
				reservationCountByRightsId.merge(rec.getRightsId(), 1, Integer::sum);
			}
		}

		List<Map<String, Object>> filtered = new ArrayList<>();
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

		filtered.sort(Comparator.comparing((Map<String, Object> m) -> !Boolean.TRUE.equals(m.get("is_valid"))));

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", filtered);
		return out;
	}

	private static Long resolveQueryUserId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String us = raw.toString().trim();
		if (us.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(us);
		} catch (NumberFormatException e) {
			return null;
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
