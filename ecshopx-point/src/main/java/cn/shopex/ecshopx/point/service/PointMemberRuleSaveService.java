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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointMemberRuleSaveService {

	private static final Pattern DEDUCT_PROPORTION_POSITIVE_INT = Pattern.compile("^[1-9][0-9]*$");

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final PopularizeConfigPointCommissionCloseService popularizeConfigPointCommissionCloseService;

	public PointMemberRuleSaveService(
			PointMemberRuleReadService pointMemberRuleReadService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper,
			PopularizeConfigPointCommissionCloseService popularizeConfigPointCommissionCloseService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.popularizeConfigPointCommissionCloseService = popularizeConfigPointCommissionCloseService;
	}

	public Map<String, Object> saveFromAdminRequest(HttpServletRequest request, Map<String, Object> data) {
		long companyId = requireCompanyId(request);

		String countryCode =
				data.containsKey("country_code") && data.get("country_code") != null
						? String.valueOf(data.get("country_code")).trim()
						: "";
		if (!StringUtils.hasText(countryCode)) {
			countryCode = "zh-CN";
		}

		validate(data);

		Map<String, Object> oldRule = new LinkedHashMap<>(pointMemberRuleReadService.getPointRule(companyId, countryCode));
		Map<String, Object> newRule = new LinkedHashMap<>(oldRule);
		newRule.putAll(data);

		Object nameVal = data.get("name");
		if (nameVal != null && StringUtils.hasText(String.valueOf(nameVal).trim())) {
			String langSuffix = countryCode.replace("-", "");
			stringRedisTemplate
					.opsForValue()
					.set(
							"memeberpoint:rule:lang:" + companyId + "_" + langSuffix,
							String.valueOf(nameVal).trim());
		}

		normalizeToggleStringsForRedis(newRule);
		try {
			stringRedisTemplate
					.opsForValue()
					.set("memeberpoint:rule:" + companyId, objectMapper.writeValueAsString(newRule));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}

		Map<String, Object> refreshed = pointMemberRuleReadService.getPointRule(companyId, countryCode);
		if (!isBothPointSwitchesTrue(refreshed)) {
			popularizeConfigPointCommissionCloseService.closeIfCommissionTypeIsPoint(companyId);
		}

		return pointMemberRuleReadService.getPointRule(companyId, countryCode);
	}

	public Map<String, Object> savePointPayFirstOnly(long companyId, int pointPayFirst) {
		Map<String, Object> merged =
				new LinkedHashMap<>(pointMemberRuleReadService.getPointRule(companyId, "zh-CN"));
		merged.put("point_pay_first", pointPayFirst);
		normalizeToggleStringsForRedis(merged);
		try {
			stringRedisTemplate
					.opsForValue()
					.set("memeberpoint:rule:" + companyId, objectMapper.writeValueAsString(merged));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}

		Map<String, Object> refreshed = pointMemberRuleReadService.getPointRule(companyId, "zh-CN");
		if (!isBothPointSwitchesTrue(refreshed)) {
			popularizeConfigPointCommissionCloseService.closeIfCommissionTypeIsPoint(companyId);
		}

		return pointMemberRuleReadService.getPointRule(companyId, "zh-CN");
	}

	private static long requireCompanyId(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return parseLongLoose(cid);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static long parseLongLoose(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private void validate(Map<String, Object> data) {
		if (data.containsKey("name") && data.get("name") != null) {
			String n = String.valueOf(data.get("name")).trim();
			if (!n.isEmpty() && n.length() > 8) {
				throw new BadRequestException("积分名最长为8个字符");
			}
		}

		if (data.containsKey("isOpenMemberPoint")) {
			String v = stringifyToggle(data.get("isOpenMemberPoint"));
			if (!"true".equals(v) && !"false".equals(v)) {
				throw new BadRequestException("是否开启积分错误");
			}
		}
		if (data.containsKey("isOpenDeductPoint")) {
			String v = stringifyToggle(data.get("isOpenDeductPoint"));
			if (!"true".equals(v) && !"false".equals(v)) {
				throw new BadRequestException("是否开启积分抵扣错误");
			}
		}

		if ("true".equals(stringifyToggle(data.get("isOpenMemberPoint")))) {
			requireNumericMin(data, "gain_point", "获取积分比例必填", BigDecimal.ZERO);
			requireNumericMin(data, "gain_limit", "获取积分限制必填", BigDecimal.ONE);
			requireNumericMin(data, "gain_time", "获取积分时间点必填", BigDecimal.ZERO);
		}

		if ("true".equals(stringifyToggle(data.get("isOpenDeductPoint")))) {
			requireNumericMin(data, "deduct_proportion_limit", "每单积分抵扣金额上限最小为1", BigDecimal.ONE);
			requireNumericMin(data, "deduct_point", "抵扣积分比例必填", BigDecimal.ZERO);
		}

		Object accessVal = data.get("access");
		if (accessVal != null && "order".equals(String.valueOf(accessVal).trim())) {
			if (!data.containsKey("include_freight") || data.get("include_freight") == null) {
				throw new BadRequestException("是否包含运费错误");
			}
			String ifv = String.valueOf(data.get("include_freight")).trim();
			if (!StringUtils.hasText(ifv)) {
				throw new BadRequestException("是否包含运费错误");
			}
		}

		Object rawDeductOpen = data.get("isOpenDeductPoint");
		if (deductProportionExtraCheckApplies(rawDeductOpen)) {
			Object rawLimit = data.get("deduct_proportion_limit");
			if (rawLimit == null) {
				throw new BadRequestException("每单积分抵扣金额上限为1-100的整数");
			}
			String limitStr = String.valueOf(rawLimit).trim();
			if (!DEDUCT_PROPORTION_POSITIVE_INT.matcher(limitStr).matches()) {
				throw new BadRequestException("每单积分抵扣金额上限为1-100的整数");
			}
			try {
				int iv = Integer.parseInt(limitStr);
				if (iv > 100) {
					throw new BadRequestException("每单积分抵扣金额上限为1-100的整数");
				}
			} catch (NumberFormatException e) {
				throw new BadRequestException("每单积分抵扣金额上限为1-100的整数");
			}
		}
	}

	private static String stringifyToggle(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	/**
	 * 是否对抵扣比例上限做 1–100 整数格式校验：布尔 false、空串、{@code "0"} 不触发；非空字符串（含 {@code "false"}）触发。
	 */
	private static boolean deductProportionExtraCheckApplies(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s)) {
			return false;
		}
		return true;
	}

	private static void requireNumericMin(Map<String, Object> data, String key, String missingMsg, BigDecimal minInclusive) {
		if (!data.containsKey(key) || data.get(key) == null) {
			throw new BadRequestException(missingMsg);
		}
		BigDecimal bd = parseBigDecimal(data.get(key));
		if (bd == null) {
			throw new BadRequestException(missingMsg);
		}
		if (bd.compareTo(minInclusive) < 0) {
			throw new BadRequestException(missingMsg);
		}
	}

	private static void normalizeToggleStringsForRedis(Map<String, Object> rule) {
		for (String k : new String[] {"isOpenMemberPoint", "isOpenDeductPoint", "include_freight"}) {
			Object v = rule.get(k);
			if (v instanceof Boolean b) {
				rule.put(k, b ? "true" : "false");
			}
		}
	}

	private static BigDecimal parseBigDecimal(Object o) {
		if (o instanceof BigDecimal b) {
			return b;
		}
		if (o instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isBothPointSwitchesTrue(Map<String, Object> rule) {
		return "true".equals(String.valueOf(rule.get("isOpenMemberPoint")))
				&& "true".equals(String.valueOf(rule.get("isOpenDeductPoint")));
	}
}
