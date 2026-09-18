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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.service.DistributorAftersalesAddressReadService;
import cn.shopex.ecshopx.distribution.service.DistributorAftersalesAddressWriteService;
import cn.shopex.ecshopx.distribution.service.dto.DistributorAftersalesAddressPutUpdateInput;
import cn.shopex.ecshopx.distribution.service.dto.DistributorAftersalesAddressSetLogisticsInput;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
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

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributorAftersalesAddress")
@RequestMapping("/api/v1/distributors/aftersalesaddress")
public class DistributorAftersalesAddressController {

	private static final Pattern ADDRESS_ID_LEADING_NUMBER = Pattern.compile("^[+-]?\\d+");

	private static final String MSG_SELECT_PROVINCE = "请选择省份";
	private static final String MSG_SELECT_CITY = "请选择城市";
	private static final String MSG_SELECT_AREA = "请选择区县";
	private static final String MSG_ENTER_ADDRESS = "请输入地址";
	private static final String MSG_ENTER_MOBILE = "请输入手机号";
	private static final String MSG_ENTER_CONTACT = "请输入联系人";

	private final DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService;
	private final DistributorAftersalesAddressReadService distributorAftersalesAddressReadService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public DistributorAftersalesAddressController(
			DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService,
			DistributorAftersalesAddressReadService distributorAftersalesAddressReadService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.distributorAftersalesAddressWriteService = distributorAftersalesAddressWriteService;
		this.distributorAftersalesAddressReadService = distributorAftersalesAddressReadService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@PostMapping(
			value = "",
			name = "添加店铺售后地址",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setDistributorAfterSalesAddressPost(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		String operatorType = ud.get("operator_type") == null ? "" : ud.get("operator_type").toString();
		long merchantId = "merchant".equals(operatorType) ? parseLongDefault(ud.get("merchant_id"), 0L) : 0L;
		int supplierId = "supplier".equals(operatorType) ? (int) parseLongDefault(ud.get("operator_id"), 0L) : 0;

		Object rawSkip = merge(request, body, "set_default");
		boolean skipValidation = isTruthySkipValidation(rawSkip);

		List<Long> distributorIds = null;
		Object rawDist = merge(request, body, "distributor_id");
		String distributorIdRaw;
		if (rawDist == null) {
			distributorIdRaw = "";
		} else if (rawDist instanceof String s) {
			distributorIdRaw = s.trim();
		} else {
			try {
				distributorIdRaw = objectMapper.writeValueAsString(rawDist);
			} catch (Exception e) {
				distributorIdRaw = rawDist.toString().trim();
			}
		}

		if (!skipValidation) {
			if (rawDist instanceof List<?> list) {
				distributorIds = parseDistributorIdListFromObjects(list);
			} else {
				JsonNode node;
				try {
					node = objectMapper.readTree(distributorIdRaw);
				} catch (Exception e) {
					throw new BadRequestException("distributor_id 格式错误");
				}
				if (!node.isArray()) {
					throw new BadRequestException("distributor_id 格式错误");
				}
				distributorIds = new ArrayList<>();
				for (JsonNode n : node) {
					long id;
					if (n.isNumber()) {
						id = n.longValue();
					} else if (n.isTextual()) {
						try {
							id = Long.parseLong(n.asText().trim());
						} catch (NumberFormatException e) {
							throw new BadRequestException("distributor_id 格式错误");
						}
					} else {
						throw new BadRequestException("distributor_id 格式错误");
					}
					distributorIds.add(id);
				}
			}
		}

		if (!skipValidation) {
			requireText(merge(request, body, "province"), MSG_SELECT_PROVINCE);
			requireText(merge(request, body, "city"), MSG_SELECT_CITY);
			requireText(merge(request, body, "area"), MSG_SELECT_AREA);
			requireText(merge(request, body, "address"), MSG_ENTER_ADDRESS);
			requireText(merge(request, body, "mobile"), MSG_ENTER_MOBILE);
			requireText(merge(request, body, "contact"), MSG_ENTER_CONTACT);
		}

		Object rawAddressId = merge(request, body, "address_id");
		boolean invokeService =
				rawAddressId == null
						|| (rawAddressId instanceof Number n && n.longValue() == 0L);
		if (!invokeService) {
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(ApiResult.ok(Map.of("status", false)));
		}

		String province = stringFromMerge(request, body, "province");
		String city = stringFromMerge(request, body, "city");
		String area = stringFromMerge(request, body, "area");
		String regionsId = stringFromMerge(request, body, "regions_id");
		String regions = stringFromMerge(request, body, "regions");
		String address = stringFromMerge(request, body, "address");
		String mobile = stringFromMerge(request, body, "mobile");
		String contact = stringFromMerge(request, body, "contact");

		var input = new DistributorAftersalesAddressSetLogisticsInput(
				companyId,
				distributorIds,
				distributorIdRaw,
				province,
				city,
				area,
				regionsId,
				regions,
				address,
				mobile,
				contact,
				merchantId,
				supplierId);
		String lang = RequestCountryCode.resolve(langueProperties, request.getParameter("country_code"), body);
		Map<String, Object> svc = distributorAftersalesAddressWriteService.setDistributorAfterSalesAddress(input, lang);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(svc));
	}

	@PutMapping(
			value = "",
			name = "修改店铺售后地址",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setDistributorAfterSalesAddressPut(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Objects.requireNonNull(request, "request");
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		String operatorType = ud.get("operator_type") == null ? "" : ud.get("operator_type").toString();
		long merchantId = "merchant".equals(operatorType) ? parseLongDefault(ud.get("merchant_id"), 0L) : 0L;
		int supplierId = "supplier".equals(operatorType) ? (int) parseLongDefault(ud.get("operator_id"), 0L) : 0;

		boolean skip = isTruthySkipValidation(merge(request, body, "set_default"));

		long distributorIdForData = 0L;
		if (!skip) {
			distributorIdForData = parseDistributorIdIntvalEquivalent(merge(request, body, "distributor_id"));
			requireText(merge(request, body, "province"), MSG_SELECT_PROVINCE);
			requireText(merge(request, body, "city"), MSG_SELECT_CITY);
			requireText(merge(request, body, "area"), MSG_SELECT_AREA);
			requireText(merge(request, body, "address"), MSG_ENTER_ADDRESS);
			requireText(merge(request, body, "mobile"), MSG_ENTER_MOBILE);
			requireText(merge(request, body, "contact"), MSG_ENTER_CONTACT);
		}

		Object rawAddrId = merge(request, body, "address_id");
		if (isAddressIdStrictZeroUnset(request, body, rawAddrId)) {
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(ApiResult.ok(Map.of("status", false)));
		}
		long addressId = parsePutBusinessAddressId(rawAddrId);
		if (skip) {
			Map<String, Object> r =
					distributorAftersalesAddressWriteService.setDefaultAddress(addressId, companyId);
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(ApiResult.ok(r));
		}
		String province = stringFromMerge(request, body, "province");
		String city = stringFromMerge(request, body, "city");
		String area = stringFromMerge(request, body, "area");
		String regionsId = stringFromMerge(request, body, "regions_id");
		String regions = stringFromMerge(request, body, "regions");
		String address = stringFromMerge(request, body, "address");
		String mobile = stringFromMerge(request, body, "mobile");
		String contact = stringFromMerge(request, body, "contact");
		var input = new DistributorAftersalesAddressPutUpdateInput(
				distributorIdForData,
				province,
				city,
				area,
				regionsId,
				regions,
				address,
				mobile,
				contact,
				merchantId,
				supplierId,
				"logistics");
		Map<String, Object> r =
				distributorAftersalesAddressWriteService.updateDistributorAfterSalesAddress(
						companyId, addressId, input, RequestCountryCode.resolve(langueProperties, request.getParameter("country_code"), body));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(r));
	}

	@DeleteMapping(
			value = "/{address_id}",
			name = "删除店铺售后地址",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteDistributorAfterSalesAddress(
			HttpServletRequest request, @PathVariable("address_id") String addressId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		String digits = LeadingNumberParser.parseAsString(addressId == null ? "" : addressId.trim());
		if (!StringUtils.hasText(digits)) {
			throw new BadRequestException("路径参数格式错误");
		}
		long aid;
		try {
			aid = Long.parseLong(digits);
		} catch (NumberFormatException e) {
			throw new BadRequestException("路径参数格式错误");
		}
		Map<String, Object> body =
				distributorAftersalesAddressWriteService.deleteDistributorAfterSalesAddress(companyId, aid);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ApiResult.ok(body));
	}

	@DataPass
	@GetMapping(name = "获取店铺售后地址列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorAfterSalesAddress(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "page_size", required = false) String pageSizeRaw,
			@RequestParam(name = "province", required = false) String province,
			@RequestParam(name = "city", required = false) String city,
			@RequestParam(name = "area", required = false) String area,
			@RequestParam(name = "distributor_id", required = false) String distributorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		String operatorType = ud.get("operator_type") == null ? "" : ud.get("operator_type").toString();
		long merchantIdJwt =
				"merchant".equalsIgnoreCase(operatorType) ? parseLongDefault(ud.get("merchant_id"), 0L) : 0L;
		long operatorId = parseLongDefault(ud.get("operator_id"), 0L);

		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 10);

		Map<String, Object> inner =
				distributorAftersalesAddressReadService.getDistributorAfterSalesAddress(
						companyId,
						merchantIdJwt,
						operatorType,
						operatorId,
						page,
						pageSize,
						province,
						city,
						area,
						distributorId,
						RequestLangTag.current(langueProperties));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>(inner);
		int block = resolveDatapassBlock(request);
		payload.put("datapass_block", block);
		if (block == 1) {
			Object listObj = payload.get("list");
			if (listObj instanceof List<?> rows && !rows.isEmpty()) {
				for (Object o : rows) {
					if (!(o instanceof Map<?, ?> rawRow)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> row = (Map<String, Object>) rawRow;
					String mobileStr = String.valueOf(row.getOrDefault("mobile", ""));
					String contactStr = String.valueOf(row.getOrDefault("contact", ""));
					String addressStr = String.valueOf(row.getOrDefault("address", ""));
					boolean anyText =
							StringUtils.hasText(mobileStr.trim())
									|| StringUtils.hasText(contactStr.trim())
									|| StringUtils.hasText(addressStr.trim());
					if (!anyText) {
						continue;
					}
					if (StringUtils.hasText(mobileStr.trim())) {
						row.put("mobile", DataMasking.maskMobile(mobileStr));
					}
					if (StringUtils.hasText(contactStr.trim())) {
						row.put("contact", DataMasking.maskTruename(contactStr));
					}
					if (StringUtils.hasText(addressStr.trim())) {
						row.put("address", DataMasking.maskAddress(addressStr));
					}
				}
			}
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(payload));
	}

	@GetMapping(value = "/{address_id}", name = "获取店铺售后地址详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorAfterSalesAddressDetail(
			HttpServletRequest request, @PathVariable("address_id") String addressId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		String digits = LeadingNumberParser.parseAsString(addressId == null ? "" : addressId.trim());
		if (!StringUtils.hasText(digits)) {
			throw new BadRequestException("路径参数格式错误");
		}
		long aid;
		try {
			aid = Long.parseLong(digits);
		} catch (NumberFormatException e) {
			throw new BadRequestException("路径参数格式错误");
		}
		Map<String, Object> data =
				distributorAftersalesAddressReadService.getDistributorAfterSalesAddressDetail(
						companyId, aid, RequestLangTag.current(langueProperties));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	private static long parseDistributorIdIntvalEquivalent(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Boolean b) {
			return b ? 1L : 0L;
		}
		if (raw instanceof Number n) {
			if (raw instanceof Double d) {
				return d.longValue();
			}
			if (raw instanceof Float f) {
				return f.longValue();
			}
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			int i = 0;
			if (t.charAt(0) == '+' || t.charAt(0) == '-') {
				i = 1;
			}
			int startDigits = i;
			while (i < t.length() && Character.isDigit(t.charAt(i))) {
				i++;
			}
			if (i == startDigits) {
				return 0L;
			}
			String num = t.substring(0, i);
			try {
				return Long.parseLong(num);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
			throw new BadRequestException("distributor_id 格式错误");
		}
		return parseDistributorIdIntvalEquivalent(raw.toString());
	}

	/**
	 * When {@code true}, respond {@code status:false} without parsing an id or calling the write service (unset key or JSON
	 * whole-number zero).
	 */
	private static boolean isAddressIdStrictZeroUnset(
			HttpServletRequest request, Map<String, Object> body, Object rawAddrId) {
		if (!(body != null && body.containsKey("address_id"))
				&& request.getParameter("address_id") == null) {
			return true;
		}
		if (rawAddrId instanceof Boolean) {
			return false;
		}
		if (rawAddrId instanceof Number n) {
			if (n instanceof Float || n instanceof Double || n instanceof BigDecimal) {
				return false;
			}
			return n.longValue() == 0L;
		}
		if (rawAddrId instanceof String) {
			return false;
		}
		if (rawAddrId instanceof CharSequence && !(rawAddrId instanceof String)) {
			return false;
		}
		return false;
	}

	/**
	 * Leading-number id for persistence after the strict-zero gate: blank string → {@code 0L}; non-blank without a
	 * leading signed digit run → parameter error.
	 */
	private static long parsePutBusinessAddressId(Object rawAddrId) {
		if (rawAddrId == null) {
			return 0L;
		}
		String s;
		if (rawAddrId instanceof String str) {
			s = str.trim();
			if (!StringUtils.hasText(s)) {
				return 0L;
			}
		} else if (rawAddrId instanceof BigInteger) {
			return ((BigInteger) rawAddrId).longValue();
		} else if (rawAddrId instanceof Number n) {
			if (n instanceof Float || n instanceof Double || n instanceof BigDecimal) {
				s = n.toString().trim();
			} else {
				return n.longValue();
			}
		} else {
			s = rawAddrId.toString().trim();
		}
		String digits = LeadingNumberParser.parseAsString(s);
		Matcher m = ADDRESS_ID_LEADING_NUMBER.matcher(s);
		if (!m.find()) {
			if (StringUtils.hasText(s)) {
				throw new BadRequestException("address_id 格式错误");
			}
			return 0L;
		}
		try {
			return Long.parseLong(digits);
		} catch (NumberFormatException e) {
			throw new BadRequestException("address_id 格式错误");
		}
	}

	private static void requireText(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		String s = raw instanceof String ? ((String) raw).trim() : raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(message);
		}
	}

	private static boolean isTruthySkipValidation(Object rawSkip) {
		if (Boolean.TRUE.equals(rawSkip)) {
			return true;
		}
		if (rawSkip instanceof Number n && n.longValue() != 0) {
			return true;
		}
		if (rawSkip instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return false;
			}
			if ("0".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t)) {
				return false;
			}
			return true;
		}
		return false;
	}

	private static Object merge(HttpServletRequest request, Map<String, Object> body, String key) {
		if (body != null && body.containsKey(key)) {
			return body.get(key);
		}
		return request.getParameter(key);
	}

	private static String stringFromMerge(HttpServletRequest request, Map<String, Object> body, String key) {
		Object o = merge(request, body, key);
		if (o == null) {
			return "";
		}
		return o.toString().trim();
	}

	private static List<Long> parseDistributorIdListFromObjects(List<?> list) {
		List<Long> out = new ArrayList<>();
		for (Object el : list) {
			if (el instanceof Number n) {
				out.add(n.longValue());
			} else if (el != null && StringUtils.hasText(el.toString())) {
				try {
					out.add(Long.parseLong(el.toString().trim()));
				} catch (NumberFormatException e) {
					throw new BadRequestException("distributor_id 格式错误");
				}
			} else {
				throw new BadRequestException("distributor_id 格式错误");
			}
		}
		return out;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static long parseLongDefault(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			String s = o.toString().trim();
			if (!StringUtils.hasText(s)) {
				return def;
			}
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int parsePositivePagingInt(String raw, int defaultVal) {
		try {
			String s = raw == null ? "" : raw.trim();
			String digits = LeadingNumberParser.parseAsString(s);
			int v = Integer.parseInt(digits);
			return Math.max(1, v);
		} catch (Exception e) {
			return defaultVal;
		}
	}

	private static int resolveDatapassBlock(HttpServletRequest request) {
		if (truthyDatapassToken(request.getHeader("X-Datapass-Block"))) {
			return 1;
		}
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Boolean b && b) {
			return 1;
		}
		if (attr != null && truthyDatapassToken(attr.toString())) {
			return 1;
		}
		return truthyDatapassToken(request.getParameter("x-datapass-block")) ? 1 : 0;
	}

	private static boolean truthyDatapassToken(String v) {
		if (v == null) {
			return false;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return !"false".equalsIgnoreCase(t);
	}
}
