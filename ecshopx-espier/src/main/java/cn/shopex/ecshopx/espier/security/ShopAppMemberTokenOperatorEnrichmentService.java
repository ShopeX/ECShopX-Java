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

package cn.shopex.ecshopx.espier.security;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves shop-app ({@code _espier_}) member JWTs to bound operator accounts, mirroring PHP
 * {@code CompanysActivationEgo::userCheck}.
 */
@Service
public class ShopAppMemberTokenOperatorEnrichmentService {

	private static final Map<Integer, String> MENU_TYPE_TO_PRODUCT_MODEL =
			Map.of(1, "all", 2, "b2c", 3, "platform", 4, "standard", 5, "in_purchase");

	private final MembersMapper membersMapper;
	private final CompanysMapper companysMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final ObjectMapper objectMapper;

	@Value("${common.product-model:platform}")
	private String defaultProductModel;

	public ShopAppMemberTokenOperatorEnrichmentService(
			MembersMapper membersMapper,
			CompanysMapper companysMapper,
			OperatorsQueryService operatorsQueryService,
			ObjectMapper objectMapper) {
		this.membersMapper = membersMapper;
		this.companysMapper = companysMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.objectMapper = objectMapper;
	}

	public void enrichIfApplicable(Map<String, Object> userData) {
		if (userData == null || userData.isEmpty()) {
			return;
		}
		String identifier = resolveEspierIdentifier(userData);
		if (!StringUtils.hasText(identifier) || !identifier.contains("_espier_")) {
			return;
		}
		EspierSubject subject = parseEspierSubject(identifier);
		if (subject == null || subject.userId() <= 0L) {
			throw new ResourceException("登录验证错误");
		}
		long companyId = longOrZero(userData.get("company_id"));
		if (companyId <= 0L) {
			throw new ResourceException("获取用户信息出错");
		}
		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getUserId, subject.userId())
								.eq(Members::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (member == null || !StringUtils.hasText(member.getMobile())) {
			throw new ResourceException("获取用户信息出错");
		}
		String productModel = resolveProductModel(companyId);
		String operatorType = "platform".equals(productModel) ? "distributor" : "staff";
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("mobile", member.getMobile());
		filter.put("operator_type", operatorType);
		Map<String, Object> operator = normalizeOperatorRow(operatorsQueryService.getInfo(filter));
		if (operator == null
				|| operator.isEmpty()
				|| "admin".equals(stringVal(operator.get("operator_type")))) {
			throw new ResourceException("登录验证错误");
		}
		if (isDisabled(operator.get("is_disable"))) {
			throw new ResourceException("登录验证错误");
		}
		Object operatorId = operator.get("operator_id");
		if (operatorId != null) {
			userData.put("operator_id", operatorId);
		}
		copyIfPresent(userData, operator, "operator_type");
		Object distributorIds = decodeDistributorIdsClaim(operator.get("distributor_ids"));
		if (distributorIds != null) {
			userData.put("distributor_ids", distributorIds);
		}
		copyIfPresent(userData, operator, "shop_ids");
		copyIfPresent(userData, operator, "merchant_id");
		copyIfPresent(userData, operator, "regionauth_id");
		copyIfPresent(userData, operator, "username");
		copyIfPresent(userData, operator, "head_portrait");
		copyIfPresent(userData, operator, "mobile");
		userData.put("source", "user");
		userData.put("logintype", "user");
		userData.put("id", identifier);
	}

	private static String resolveEspierIdentifier(Map<String, Object> userData) {
		String id = stringVal(userData.get("id"));
		if (StringUtils.hasText(id)) {
			return id;
		}
		return stringVal(userData.get("sub"));
	}

	private Map<String, Object> normalizeOperatorRow(Map<String, Object> operator) {
		if (operator == null || operator.isEmpty()) {
			return operator;
		}
		LinkedHashMap<String, Object> normalized = new LinkedHashMap<>(operator);
		coalesceKey(normalized, "operator_id", "operatorId");
		coalesceKey(normalized, "company_id", "companyId");
		coalesceKey(normalized, "operator_type", "operatorType");
		coalesceKey(normalized, "merchant_id", "merchantId");
		coalesceKey(normalized, "regionauth_id", "regionauthId");
		coalesceKey(normalized, "head_portrait", "headPortrait");
		coalesceKey(normalized, "distributor_ids", "distributorIds");
		coalesceKey(normalized, "shop_ids", "shopIds");
		coalesceKey(normalized, "is_disable", "isDisable");
		return normalized;
	}

	private static void coalesceKey(LinkedHashMap<String, Object> m, String snake, String camel) {
		if (m.containsKey(snake)) {
			m.remove(camel);
			return;
		}
		if (m.containsKey(camel)) {
			m.put(snake, m.remove(camel));
		}
	}

	private Object decodeDistributorIdsClaim(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> || raw instanceof Map<?, ?>) {
			return raw;
		}
		if (!(raw instanceof String s) || !StringUtils.hasText(s)) {
			return raw;
		}
		String trimmed = s.trim();
		if (!trimmed.startsWith("[")) {
			return raw;
		}
		try {
			List<Object> parsed = objectMapper.readValue(trimmed, new TypeReference<List<Object>>() {});
			if (parsed == null) {
				return List.of();
			}
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object row : parsed) {
				if (row instanceof Map<?, ?> mapRow) {
					LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : mapRow.entrySet()) {
						normalized.put(String.valueOf(e.getKey()), e.getValue());
					}
					out.add(normalized);
					continue;
				}
				if (row instanceof Number n) {
					out.add(Map.of("distributor_id", n.longValue()));
				}
			}
			return out.isEmpty() ? parsed : out;
		} catch (Exception e) {
			return raw;
		}
	}

	private String resolveProductModel(long companyId) {
		Companys company = companysMapper.selectById(companyId);
		if (company == null || company.getMenuType() == null || company.getMenuType() == 0) {
			return StringUtils.hasText(defaultProductModel) ? defaultProductModel : "platform";
		}
		return MENU_TYPE_TO_PRODUCT_MODEL.getOrDefault(
				company.getMenuType(),
				StringUtils.hasText(defaultProductModel) ? defaultProductModel : "platform");
	}

	private static void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
		if (source.containsKey(key) && source.get(key) != null) {
			target.put(key, source.get(key));
		}
	}

	private static boolean isDisabled(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String text = raw.toString().trim();
		return "1".equals(text) || "true".equalsIgnoreCase(text);
	}

	private static EspierSubject parseEspierSubject(String identifier) {
		if (!StringUtils.hasText(identifier) || !identifier.contains("_espier_")) {
			return null;
		}
		String[] parts = identifier.split("_espier_", 3);
		if (parts.length < 3) {
			return null;
		}
		try {
			return new EspierSubject(Long.parseLong(parts[0].trim()), parts[1], parts[2]);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longOrZero(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString();
	}

	private record EspierSubject(long userId, String openIdPart, String unionidPart) {}
}
