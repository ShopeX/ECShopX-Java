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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.distribution.domain.DistributorSalesmanRole;
import cn.shopex.ecshopx.distribution.mapper.DistributorSalesmanRoleMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappSalespersonSessionAuthService {

	private static final String SESSION_KEY_PREFIX = "frontSession3rd:";

	private static final String HEADER_WXAPP_SESSION = "x-wxapp-session";

	private static final String HEADER_SALESPERSON_TYPE = "salesperson-type";

	private final StringRedisTemplate wechatRedis;

	private final ObjectMapper objectMapper;

	private final ShopSalespersonMapper shopSalespersonMapper;

	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;

	private final DistributorSalesmanRoleMapper distributorSalesmanRoleMapper;

	public WxappSalespersonSessionAuthService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate wechatRedis,
			ObjectMapper objectMapper,
			ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			DistributorSalesmanRoleMapper distributorSalesmanRoleMapper) {
		this.wechatRedis = wechatRedis;
		this.objectMapper = objectMapper;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.distributorSalesmanRoleMapper = distributorSalesmanRoleMapper;
	}

	/**
	 * 校验企业微信导购小程序会话并返回与前端对齐的上下文字段（至少含 salesperson_id）。
	 */
	public Map<String, Object> authenticate(HttpServletRequest request) {
		String sessionHeader = request.getHeader(HEADER_WXAPP_SESSION);
		if (!StringUtils.hasText(sessionHeader)) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String raw = wechatRedis.opsForValue().get(SESSION_KEY_PREFIX + sessionHeader.trim());
		if (!StringUtils.hasText(raw)) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		Map<String, Object> sessionUser;
		try {
			sessionUser = objectMapper.readValue(raw, new TypeReference<>() {});
		} catch (Exception e) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		if (sessionUser == null || sessionUser.isEmpty()) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}

		long companyId = parseRequiredPositiveLong(sessionUser.get("company_id"));
		String workUserid = stringVal(sessionUser.get("work_userid"));
		if (!StringUtils.hasText(workUserid)) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		Object sessionTypeObj = sessionUser.get("salesperson_type");
		if (sessionTypeObj == null || !StringUtils.hasText(sessionTypeObj.toString())) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		String sessionSalespersonType = sessionTypeObj.toString().trim();

		String headerSalespersonType = request.getHeader(HEADER_SALESPERSON_TYPE);
		List<String> allowedTypes;
		if (StringUtils.hasText(headerSalespersonType)) {
			allowedTypes = List.of(headerSalespersonType.trim());
		} else {
			allowedTypes = List.of("admin", "verification_clerk");
		}

		ShopSalesperson dbRow = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getWorkUserid, workUserid)
				.in(ShopSalesperson::getSalespersonType, allowedTypes)
				.eq(ShopSalesperson::getIsValid, "true")
				.last("LIMIT 1"));
		if (dbRow == null) {
			throw new UnauthorizedException("当前手机号无权限");
		}

		assertRouteRoleAllowed(dbRow, companyId, null);

		if (!Objects.equals(dbRow.getSalespersonType(), sessionSalespersonType)) {
			throw new UnauthorizedException("会话与库不一致");
		}

		Map<String, Object> out = new LinkedHashMap<>(sessionUser);
		// Redis JSON 中的导购 ID（写入 DB 覆盖前的值）；供 cartcount 等接口与库表交叉校验
		out.put("session_salesperson_id", parseSessionSalespersonIdLenient(sessionUser.get("salesperson_id")));
		out.put("salesperson_id", dbRow.getSalespersonId());
		// 库表中的 distributor 覆盖会话中的 distributor_id
		long distributorId = resolveFirstDistributorId(companyId, dbRow.getSalespersonId());
		out.put("distributor_id", distributorId);
		return out;
	}

	/**
	 * 取 shop_rel_salesperson 中 store_type=distributor 的第一条
	 * shop_id（按 shop_id 升序）；无则 0。
	 */
	private long resolveFirstDistributorId(long companyId, Long salespersonId) {
		if (salespersonId == null) {
			return 0L;
		}
		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, salespersonId)
						.orderByAsc(ShopsRelSalesperson::getShopId));
		for (ShopsRelSalesperson rel : relRows) {
			if (rel == null || !"distributor".equals(rel.getStoreType())) {
				continue;
			}
			Long sid = rel.getShopId();
			if (sid != null) {
				return sid;
			}
		}
		return 0L;
	}

	private void assertRouteRoleAllowed(ShopSalesperson row, long companyId, Integer routeRoleRuleId) {
		if (routeRoleRuleId == null) {
			return;
		}
		String roleStr = row.getRole();
		if (!StringUtils.hasText(roleStr)) {
			throw new ForbiddenException("无权限");
		}
		long roleId;
		try {
			roleId = Long.parseLong(roleStr.trim());
		} catch (NumberFormatException e) {
			throw new ForbiddenException("无权限");
		}
		DistributorSalesmanRole roleRow = distributorSalesmanRoleMapper.selectOne(
				new LambdaQueryWrapper<DistributorSalesmanRole>()
						.eq(DistributorSalesmanRole::getSalesmanRoleId, roleId)
						.eq(DistributorSalesmanRole::getCompanyId, (int) companyId)
						.last("LIMIT 1"));
		if (roleRow == null) {
			throw new ForbiddenException("无权限");
		}
		List<Integer> ruleIds = parseRuleIds(roleRow.getRuleIds());
		if (!ruleIds.contains(routeRoleRuleId)) {
			throw new ForbiddenException("无权限");
		}
	}

	private List<Integer> parseRuleIds(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith("[")) {
			try {
				List<Integer> parsed = objectMapper.readValue(t, new TypeReference<>() {});
				return parsed != null ? parsed : List.of();
			} catch (Exception e) {
				return List.of();
			}
		}
		String[] parts = t.split(",");
		List<Integer> out = new ArrayList<>();
		for (String p : parts) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				out.add(Integer.parseInt(p.trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	/**
	 * 会话 JSON 中的 salesperson_id；缺失、非数字或 ≤0 时返回 0（不抛错，由业务层报 422）。
	 */
	private static long parseSessionSalespersonIdLenient(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseRequiredPositiveLong(Object raw) {
		if (raw == null) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		long v;
		if (raw instanceof Number n) {
			v = n.longValue();
		} else {
			try {
				v = Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("Unable to authenticate wxapp user.");
			}
		}
		if (v <= 0L) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		return v;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : Objects.toString(o, "");
	}
}
