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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.service.reservation.RightsTimesCardRowFactory;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RightsFrontWxappGetRightsListService {

	private final RightsMapper rightsMapper;
	private final RightsTimesCardRowFactory rightsTimesCardRowFactory;

	public RightsFrontWxappGetRightsListService(
			RightsMapper rightsMapper, RightsTimesCardRowFactory rightsTimesCardRowFactory) {
		this.rightsMapper = rightsMapper;
		this.rightsTimesCardRowFactory = rightsTimesCardRowFactory;
	}

	public Map<String, Object> getRightsList(HttpServletRequest request, Map<String, Object> authClaims) {
		int page = normalizePage(parseRequiredPositiveIntParam("page", request.getParameter("page")));
		int pageSize = normalizePageSize(parseRequiredPositiveIntParam("pageSize", request.getParameter("pageSize")));
		Integer validFilter = resolveValidFilter(request);

		if (falsyUserId(authClaims.get("user_id"))) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("list", List.of());
			data.put("total_count", List.of());
			return data;
		}

		long companyId = longVal(authClaims.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}

		long userId = longVal(authClaims.get("user_id"));
		if (userId <= 0L) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("list", List.of());
			data.put("total_count", List.of());
			return data;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		long total =
				rightsMapper.selectCount(RightsWxappListQuerySupport.forCount(companyId, userId, now, validFilter));

		Page<Rights> pageObj = new Page<>(page, pageSize, false);
		rightsMapper.selectPage(
				pageObj, RightsWxappListQuerySupport.forPage(companyId, userId, now, validFilter));

		List<Map<String, Object>> list = new ArrayList<>();
		for (Rights r : pageObj.getRecords()) {
			Map<String, Object> row = rightsTimesCardRowFactory.toTimesCardRow(r, now);
			row.put("operator_desc", r.getOperatorDesc());
			row.put("created", r.getCreated());
			row.put("updated", r.getUpdated());
			list.add(row);
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", list);
		return data;
	}

	private static boolean falsyUserId(Object userIdRaw) {
		if (userIdRaw == null) {
			return true;
		}
		if (userIdRaw instanceof Boolean b) {
			return !b;
		}
		if (userIdRaw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (userIdRaw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		String t = String.valueOf(userIdRaw).trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static int normalizePage(int page) {
		return page < 1 ? 1 : page;
	}

	private static int normalizePageSize(int pageSize) {
		if (pageSize > 1000) {
			return 1000;
		}
		if (pageSize <= 0) {
			return 20;
		}
		return pageSize;
	}

	private static int parseRequiredPositiveIntParam(String name, String raw) {
		if (raw == null || raw.isBlank()) {
			throw new BadRequestException("获取权益列表出错");
		}
		String t = raw.trim();
		int v;
		try {
			v = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("获取权益列表出错");
		}
		if ("page".equals(name) && v < 1) {
			throw new BadRequestException("获取权益列表出错");
		}
		if ("pageSize".equals(name) && (v < 1 || v > 50)) {
			throw new BadRequestException("获取权益列表出错");
		}
		return v;
	}

	private static Integer resolveValidFilter(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("valid")) {
			return null;
		}
		String raw = request.getParameter("valid");
		if (raw == null) {
			throw new BadRequestException("获取权益列表出错");
		}
		String t = raw.trim();
		if ("1".equals(t)) {
			return 1;
		}
		if ("0".equals(t)) {
			return 0;
		}
		throw new BadRequestException("获取权益列表出错");
	}

	private static long longVal(Object v) {
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
