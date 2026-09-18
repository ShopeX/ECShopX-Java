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

package cn.shopex.ecshopx.orders.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import cn.shopex.ecshopx.orders.service.admin.RightsAdminCreateRightsService;
import cn.shopex.ecshopx.orders.service.admin.RightsAdminDelayRightsService;
import cn.shopex.ecshopx.orders.service.admin.RightsAdminGetRightsInfoService;
import cn.shopex.ecshopx.orders.service.admin.RightsAdminGetRightsListDataService;
import cn.shopex.ecshopx.orders.service.admin.RightsAdminGetRightsListService;
import cn.shopex.ecshopx.orders.service.admin.RightsAdminTransferRightsListService;
import cn.shopex.ecshopx.orders.service.admin.RightsAdminTransferRightsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1Rights")
@RequestMapping("/api/v1")
public class RightsController {

	private final RightsAdminCreateRightsService rightsAdminCreateRightsService;
	private final RightsAdminDelayRightsService rightsAdminDelayRightsService;
	private final RightsAdminTransferRightsService rightsAdminTransferRightsService;
	private final RightsAdminGetRightsListDataService rightsAdminGetRightsListDataService;
	private final RightsAdminGetRightsListService rightsAdminGetRightsListService;
	private final RightsAdminTransferRightsListService rightsAdminTransferRightsListService;
	private final RightsAdminGetRightsInfoService rightsAdminGetRightsInfoService;
	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;
	private final ObjectMapper objectMapper;

	public RightsController(
			RightsAdminCreateRightsService rightsAdminCreateRightsService,
			RightsAdminDelayRightsService rightsAdminDelayRightsService,
			RightsAdminTransferRightsService rightsAdminTransferRightsService,
			RightsAdminGetRightsListDataService rightsAdminGetRightsListDataService,
			RightsAdminGetRightsListService rightsAdminGetRightsListService,
			RightsAdminTransferRightsListService rightsAdminTransferRightsListService,
			RightsAdminGetRightsInfoService rightsAdminGetRightsInfoService,
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService,
			ObjectMapper objectMapper) {
		this.rightsAdminCreateRightsService = rightsAdminCreateRightsService;
		this.rightsAdminDelayRightsService = rightsAdminDelayRightsService;
		this.rightsAdminTransferRightsService = rightsAdminTransferRightsService;
		this.rightsAdminGetRightsListDataService = rightsAdminGetRightsListDataService;
		this.rightsAdminGetRightsListService = rightsAdminGetRightsListService;
		this.rightsAdminTransferRightsListService = rightsAdminTransferRightsListService;
		this.rightsAdminGetRightsInfoService = rightsAdminGetRightsInfoService;
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "order.rights.list")
	@GetMapping(value = "/rights/getdata", name = "用户权益数据")
	public ResponseEntity<?> getRightsListData(HttpServletRequest request) {
		Map<String, Object> merged = FlexibleHttpServletParameterMap.toObjectMap(request);
		long companyId = readCompanyIdFromJwt(request);

		LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();

		Long userId = null;
		Object userRaw = merged.get("user_id");
		if (userRaw == null || !StringUtils.hasText(String.valueOf(userRaw).trim())) {
			fieldErrors.put("user_id", List.of("validation.required"));
		} else {
			try {
				userId = Long.parseLong(String.valueOf(userRaw).trim());
			} catch (NumberFormatException e) {
				fieldErrors.put("user_id", List.of("validation.integer"));
			}
		}

		Integer page = null;
		Object pageRaw = merged.get("page");
		if (pageRaw == null || !StringUtils.hasText(String.valueOf(pageRaw).trim())) {
			fieldErrors.put("page", List.of("validation.required"));
		} else {
			try {
				page = Integer.parseInt(String.valueOf(pageRaw).trim());
			} catch (NumberFormatException e) {
				fieldErrors.put("page", List.of("validation.integer"));
			}
		}
		if (page != null && page < 1) {
			fieldErrors.put("page", List.of("validation.min.numeric"));
		}

		Integer pageSize = null;
		Object pageSizeRaw = merged.get("pageSize");
		if (pageSizeRaw == null || !StringUtils.hasText(String.valueOf(pageSizeRaw).trim())) {
			fieldErrors.put("pageSize", List.of("validation.required"));
		} else {
			try {
				pageSize = Integer.parseInt(String.valueOf(pageSizeRaw).trim());
			} catch (NumberFormatException e) {
				fieldErrors.put("pageSize", List.of("validation.integer"));
			}
		}
		if (pageSize != null && pageSize < 1) {
			fieldErrors.put("pageSize", List.of("validation.min.numeric"));
		}
		if (pageSize != null && pageSize > 100) {
			fieldErrors.put("pageSize", List.of("validation.max.numeric"));
		}

		if (!fieldErrors.isEmpty()) {
			throw new BadRequestException("获取权益列表出错.", fieldErrors);
		}

		Long optionalRightsId = parseOptionalRightsId(merged.get("rights_id"));

		Integer optionalEndTimeEpoch = null;
		Object endTimeRaw = merged.get("end_time");
		if (isTruthyOptionalFilterValue(endTimeRaw)) {
			Long sec = DateExpressionParser.parseToEpochSecond(endTimeRaw, ZoneId.systemDefault());
			if (sec == null) {
				throw new BadRequestException(
						"获取权益列表出错.", Map.of("end_time", List.of("validation.date")), 422);
			}
			optionalEndTimeEpoch = sec.intValue();
		}

		String resourceLevelIdRaw =
				merged.get("resource_level_id") == null
						? ""
						: String.valueOf(merged.get("resource_level_id")).trim();

		Map<String, Object> data =
				rightsAdminGetRightsListDataService.getRightsListData(
						companyId,
						userId,
						page,
						pageSize,
						optionalRightsId,
						optionalEndTimeEpoch,
						resourceLevelIdRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Long parseOptionalRightsId(Object rRaw) {
		if (rRaw == null) {
			return null;
		}
		if (rRaw instanceof Number n) {
			if (n.longValue() == 0L) {
				return null;
			}
			return n.longValue();
		}
		String rs = String.valueOf(rRaw).trim();
		if (!StringUtils.hasText(rs) || "0".equals(rs)) {
			return null;
		}
		try {
			return Long.parseLong(rs);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Whether an optional list filter should take effect: {@code true} only for a present,
	 * non-false boolean, a non-zero number, or a non-empty string that is not {@code "0"}.
	 */
	private static boolean isTruthyOptionalFilterValue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(v).trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	@DataPass
	@Activated(routeAlias = "order.rights.list.get")
	@GetMapping(value = "/rights/list", name = "权益列表")
	public ApiResult<Map<String, Object>> getRightsList(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(rightsAdminGetRightsListService.getRightsList(companyId, request));
	}

	@Activated(routeAlias = "order.rights.add")
	@PostMapping(value = "/rights", name = "新增权益", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createRights(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);

		Object m0 = merged.get("mobile");
		String mobile = m0 == null ? null : String.valueOf(m0).trim();
		if (!StringUtils.hasText(mobile) || "0".equals(mobile)) {
			return dingoEmbeddedError(412, "请填写手机号");
		}

		List<Long> itemIds = parseItemIds(merged.get("itemids"));
		if (itemIds.isEmpty()) {
			return dingoEmbeddedError(412, "请选择新增权益的商品");
		}

		Long userId = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, mobile);
		if (userId == null) {
			return dingoEmbeddedError(412, "当前手机号不是会员");
		}

		rightsAdminCreateRightsService.createRights(companyId, itemIds, mobile);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "order.rights.transfer")
	@PutMapping(value = "/transfer/rights", name = "转赠权益", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> transferRights(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);

		Object rightsRaw = merged.get("rights_id");
		if (rightsRaw == null) {
			return dingoEmbeddedError(412, "权益id必填");
		}
		if (rightsRaw instanceof Number && ((Number) rightsRaw).longValue() == 0L) {
			return dingoEmbeddedError(412, "权益id必填");
		}
		String rightsStr = String.valueOf(rightsRaw).trim();
		if (!StringUtils.hasText(rightsStr) || "0".equals(rightsStr)) {
			return dingoEmbeddedError(412, "权益id必填");
		}
		long rightsId;
		try {
			rightsId = Long.parseLong(rightsStr);
		} catch (NumberFormatException e) {
			throw new ResourceException("rights_id=" + rightsStr + "的权益不存在");
		}

		String mobile = merged.get("mobile") == null ? null : String.valueOf(merged.get("mobile")).trim();
		String transferMobile =
				merged.get("transfer_mobile") == null
						? null
						: String.valueOf(merged.get("transfer_mobile")).trim();
		String remark = merged.get("remark") == null ? null : String.valueOf(merged.get("remark"));

		RightsAdminTransferRightsService.TransferRightsResult r =
				rightsAdminTransferRightsService.transferRights(
						companyId, rightsId, mobile, transferMobile, remark);
		if (r instanceof RightsAdminTransferRightsService.Embedded412 e) {
			return dingoEmbeddedError(412, e.message());
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@DataPass
	@Activated(routeAlias = "order.rights.transfer.list")
	@GetMapping(value = "/transfer/rights/list", name = "转赠权益列表")
	public ApiResult<Map<String, Object>> transferRightsList(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(rightsAdminTransferRightsListService.transferRightsList(companyId, request));
	}

	@Activated(routeAlias = "rights.info")
	@GetMapping(value = "/rights/info", name = "权益核销详情")
	public ResponseEntity<?> getRightsInfo(HttpServletRequest request) {
		Object rightsRaw = FlexibleHttpServletParameterMap.toObjectMap(request).get("rights_id");
		Map<String, Object> logsPayload = rightsAdminGetRightsInfoService.getRightsInfo(rightsRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("logs", logsPayload)));
	}

	@Activated(routeAlias = "rights.delay")
	@PostMapping(value = "/rights/delay", name = "权益延期", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> delayRights(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);

		Object rightsRaw = merged.get("rights_id");
		if (rightsRaw == null) {
			return dingoEmbeddedError(412, "权益id必填");
		}
		if (rightsRaw instanceof Number && ((Number) rightsRaw).longValue() == 0L) {
			return dingoEmbeddedError(412, "权益id必填");
		}
		String rightsStr = String.valueOf(rightsRaw).trim();
		if (!StringUtils.hasText(rightsStr) || "0".equals(rightsStr)) {
			return dingoEmbeddedError(412, "权益id必填");
		}
		long rightsId;
		try {
			rightsId = Long.parseLong(rightsStr);
		} catch (NumberFormatException e) {
			return dingoEmbeddedError(412, "权益id必填");
		}

		Object delayRaw = merged.get("delay_date");
		if (delayRaw == null) {
			return dingoEmbeddedError(412, "请填写延期日期");
		}
		String delayStr = String.valueOf(delayRaw).trim();
		if (!StringUtils.hasText(delayStr)) {
			return dingoEmbeddedError(412, "请填写延期日期");
		}

		String remark =
				merged.get("remark") == null ? "" : String.valueOf(merged.get("remark")).trim();
		if (!StringUtils.hasText(remark)) {
			return dingoEmbeddedError(412, "请填写此次操作备注");
		}

		Map<String, Object> data =
				rightsAdminDelayRightsService.delayRights(
						companyId,
						rightsId,
						delayStr,
						remark,
						readOptionalOperatorIdFromJwt(request));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static ResponseEntity<Map<String, Object>> dingoEmbeddedError(int statusCode, String message) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		return ResponseEntity.ok(Map.of("data", data));
	}

	private List<Long> parseItemIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof List<?> list) {
			return parseItemIdListElements(list);
		}
		if (raw instanceof CharSequence) {
			String t = raw.toString().trim();
			if (t.isEmpty()) {
				return List.of();
			}
			if (t.contains(",") && !t.startsWith("[")) {
				return List.of();
			}
			if (t.startsWith("[")) {
				try {
					List<?> arr = objectMapper.readValue(t, new TypeReference<List<?>>() {});
					return parseItemIdListElements(arr);
				} catch (JsonProcessingException e) {
					throw new BadRequestException("商品ID格式错误");
				}
			}
			try {
				return List.of(Long.parseLong(t));
			} catch (NumberFormatException e) {
				throw new BadRequestException("商品ID格式错误");
			}
		}
		throw new BadRequestException("商品ID格式错误");
	}

	private List<Long> parseItemIdListElements(List<?> list) {
		List<Long> out = new ArrayList<>(list.size());
		for (Object el : list) {
			out.add(parseOneItemId(el));
		}
		return out;
	}

	private long parseOneItemId(Object el) {
		if (el == null) {
			throw new BadRequestException("商品ID格式错误");
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(el).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("商品ID格式错误");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品ID格式错误");
		}
	}

	private static Long readOptionalOperatorIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			return null;
		}
		if (!jwt.containsKey("operator_id")) {
			return null;
		}
		Object op = jwt.get("operator_id");
		if (op == null) {
			return null;
		}
		try {
			return Long.parseLong(String.valueOf(op).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
