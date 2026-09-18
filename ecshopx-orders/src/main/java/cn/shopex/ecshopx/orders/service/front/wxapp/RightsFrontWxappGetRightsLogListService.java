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
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.orders.domain.RightsLog;
import cn.shopex.ecshopx.orders.mapper.RightsLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RightsFrontWxappGetRightsLogListService {

	private final RightsLogMapper rightsLogMapper;
	private final WxShopsMapper wxShopsMapper;

	public RightsFrontWxappGetRightsLogListService(
			RightsLogMapper rightsLogMapper, WxShopsMapper wxShopsMapper) {
		this.rightsLogMapper = rightsLogMapper;
		this.wxShopsMapper = wxShopsMapper;
	}

	public Map<String, Object> getRightsLogList(HttpServletRequest request, Map<String, Object> authClaims) {
		int page = parseRequiredPositiveIntParam("page", request.getParameter("page"));
		int pageSize = parseRequiredPositiveIntParam("pageSize", request.getParameter("pageSize"));

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

		LambdaQueryWrapper<RightsLog> w = new LambdaQueryWrapper<RightsLog>()
				.eq(RightsLog::getCompanyId, companyId)
				.eq(RightsLog::getUserId, userId)
				.orderByDesc(RightsLog::getCreated);

		long total = rightsLogMapper.selectCount(w);
		Page<RightsLog> pageObj = new Page<>(page, pageSize, false);
		rightsLogMapper.selectPage(pageObj, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (RightsLog e : pageObj.getRecords()) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("rights_id", e.getRightsId());
			row.put("user_id", e.getUserId());
			row.put("company_id", e.getCompanyId());
			row.put("shop_id", e.getShopId());
			row.put("rights_name", e.getRightsName());
			row.put("consum_num", e.getConsumNum());
			row.put("rights_subname", e.getRightsSubname());
			row.put("attendant", e.getAttendant());
			row.put("consum_time", e.getEndTime());
			row.put("created", e.getCreated());
			row.put("salesperson_mobile", displaySalespersonMobile(e.getSalespersonMobile()));

			Long shopPk = parseShopIdToLong(e.getShopId());
			try {
				WxShops shop = shopPk == null ? null : wxShopsMapper.selectById(shopPk);
				row.put("store_name", shop != null && shop.getStoreName() != null ? shop.getStoreName() : "");
			} catch (Exception ex) {
				row.put("store_name", "");
			}

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

	private static int parseRequiredPositiveIntParam(String name, String raw) {
		if (raw == null || raw.isBlank()) {
			throw rightsLogListBadRequest(name, "validation.required");
		}
		String t = raw.trim();
		int v;
		try {
			v = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw rightsLogListBadRequest(name, "validation.integer");
		}
		if ("page".equals(name) && v < 1) {
			throw rightsLogListBadRequest(name, "validation.min.numeric");
		}
		if ("pageSize".equals(name)) {
			if (v < 1) {
				throw rightsLogListBadRequest(name, "validation.min.numeric");
			}
			if (v > 50) {
				throw rightsLogListBadRequest(name, "validation.max.numeric");
			}
		}
		return v;
	}

	private static BadRequestException rightsLogListBadRequest(String field, String validationCode) {
		return new BadRequestException("获取权益列表出错", Map.of(field, List.of(validationCode)), 422);
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

	private static String displaySalespersonMobile(String stored) {
		if (stored == null) {
			return "";
		}
		try {
			String d = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(stored);
			return d == null ? "" : d;
		} catch (Exception ex) {
			return stored;
		}
	}

	private static Long parseShopIdToLong(String shopId) {
		if (shopId == null) {
			return null;
		}
		try {
			return Long.parseLong(shopId.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
