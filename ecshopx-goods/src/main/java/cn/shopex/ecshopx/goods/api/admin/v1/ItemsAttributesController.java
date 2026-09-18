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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.ItemsAttributesCreateService;
import cn.shopex.ecshopx.goods.service.ItemsAttributesDeleteService;
import cn.shopex.ecshopx.goods.service.ItemsAttributesQueryService;
import cn.shopex.ecshopx.goods.service.ItemsAttributesUpdateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("goodsAdminV1ItemsAttributes")
@RequestMapping("/api/v1/goods/attributes")
public class ItemsAttributesController {

	private static final String[] UPDATE_WHITELIST_KEYS = { "attribute_type", "attribute_name", "attribute_memo", "attribute_sort", "is_show",
			"image_url", "is_image" };

	private final ItemsAttributesCreateService itemsAttributesCreateService;
	private final ItemsAttributesUpdateService itemsAttributesUpdateService;
	private final ItemsAttributesDeleteService itemsAttributesDeleteService;
	private final ItemsAttributesQueryService itemsAttributesQueryService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public ItemsAttributesController(ItemsAttributesCreateService itemsAttributesCreateService,
			ItemsAttributesUpdateService itemsAttributesUpdateService, ItemsAttributesDeleteService itemsAttributesDeleteService,
			ItemsAttributesQueryService itemsAttributesQueryService, ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.itemsAttributesCreateService = itemsAttributesCreateService;
		this.itemsAttributesUpdateService = itemsAttributesUpdateService;
		this.itemsAttributesDeleteService = itemsAttributesDeleteService;
		this.itemsAttributesQueryService = itemsAttributesQueryService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "goods.attributes.add")
	@PostMapping(name = "新增属性")
	public ResponseEntity<ApiResult<Map<String, Object>>> addItemsAttributes(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long distributorIdFromJwt = readDistributorId(ud);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);

		String countryCode = RequestCountryCode.resolve(langueProperties, input);

		Object nameObj = input.get("attribute_name");
		if (nameObj == null || !StringUtils.hasText(nameObj.toString().trim())) {
			throw new BadRequestException("请填写名称");
		}

		if (Objects.equals(stringValue(input.get("attribute_type")), "brand")) {
			input.put("distributor_id", distributorIdFromJwt);
		}
		input.put("company_id", companyId);

		normalizeAttributeValues(input);

		itemsAttributesCreateService.createAttr(companyId, input, countryCode);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static String stringValue(Object o) {
		return o == null ? null : o.toString();
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private void normalizeAttributeValues(Map<String, Object> input) {
		if (!input.containsKey("attribute_values")) {
			return;
		}
		Object val = input.get("attribute_values");
		if (val == null) {
			return;
		}
		if (val instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return;
			}
			try {
				JsonNode node = objectMapper.readTree(s);
				if (!node.isArray()) {
					throw new BadRequestException("attribute_values 格式无效");
				}
				List<Map<String, Object>> out = new ArrayList<>();
				for (JsonNode el : node) {
					if (!el.isObject()) {
						throw new BadRequestException("attribute_values 格式无效");
					}
					out.add(objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {}));
				}
				input.put("attribute_values", out);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("attribute_values 格式无效");
			}
			return;
		}
		if (val instanceof List<?> rawList) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : rawList) {
				if (o instanceof Map<?, ?> m) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					out.add(row);
				} else if (o != null) {
					try {
						out.add(objectMapper.convertValue(o, new TypeReference<Map<String, Object>>() {}));
					} catch (IllegalArgumentException ex) {
						throw new BadRequestException("attribute_values 格式无效");
					}
				} else {
					throw new BadRequestException("attribute_values 格式无效");
				}
			}
			input.put("attribute_values", out);
			return;
		}
		throw new BadRequestException("attribute_values 格式无效");
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException(key + " 缺失或无效");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 缺失或无效");
		}
	}

	private static long readDistributorId(Map<?, ?> ud) {
		Object v = ud.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}

	@Activated(routeAlias = "goods.attributes.update")
	@PutMapping(value = "/{attribute_id}", name = "更新属性")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateItemsAttributes(HttpServletRequest request,
			@PathVariable("attribute_id") String attributeIdStr, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");

		long attributeId;
		try {
			attributeId = Long.parseLong(attributeIdStr.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("attribute_id 无效");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		if (merged.containsKey("attribute_values")) {
			normalizeAttributeValues(merged);
		}

		Map<String, Object> input = new LinkedHashMap<>();
		for (String k : UPDATE_WHITELIST_KEYS) {
			if (merged.containsKey(k)) {
				input.put(k, merged.get(k));
			}
		}
		if (merged.containsKey("attribute_values")) {
			input.put("attribute_values", merged.get("attribute_values"));
		}

		String countryCode = RequestCountryCode.resolve(langueProperties, merged);

		Object nameObj = input.get("attribute_name");
		if (nameObj == null || !StringUtils.hasText(nameObj.toString().trim())) {
			throw new BadRequestException("请填写名称");
		}

		itemsAttributesUpdateService.updateAttr(companyId, attributeId, input, countryCode);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.attributes.list")
	@GetMapping(name = "属性列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsAttrList(HttpServletRequest request,
			@RequestParam(name = "attribute_type", required = false) String attributeType,
			@RequestParam(name = "page") int page, @RequestParam(name = "pageSize") int pageSize,
			@RequestParam(name = "attribute_name", required = false) String attributeName,
			@RequestParam(name = "attribute_ids", required = false) String[] attributeIdsParam,
			@RequestParam(name = "distributor_id", required = false) String distributorIdQuery,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long jwtDistributorId = readDistributorId(ud);
		List<Long> attributeIdsParsed = parseAttributeIds(attributeIdsParam);
		Map<String, Object> data = itemsAttributesQueryService.getAttrList(companyId, jwtDistributorId, attributeType, attributeName,
				attributeIdsParsed, distributorIdQuery, page, pageSize, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static List<Long> parseAttributeIds(String[] attributeIdsParam) {
		if (attributeIdsParam == null || attributeIdsParam.length == 0) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		for (String raw : attributeIdsParam) {
			if (!StringUtils.hasText(raw)) {
				continue;
			}
			for (String part : raw.split(",")) {
				String t = part.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException e) {
					throw new BadRequestException("attribute_ids 无效");
				}
			}
		}
		return out.isEmpty() ? null : out;
	}

	@Activated(routeAlias = "goods.attributes.delete")
	@DeleteMapping(value = "/{attribute_id}", name = "删除属性")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteItemsAttributes(HttpServletRequest request,
			@PathVariable("attribute_id") String attributeIdStr) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long attributeId;
		try {
			attributeId = Long.parseLong(attributeIdStr.trim());
		} catch (NumberFormatException e) {
			attributeId = 0L;
		}
		itemsAttributesDeleteService.deleteAttr(companyId, attributeId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
