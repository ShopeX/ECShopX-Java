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

package cn.shopex.ecshopx.onecode.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.onecode.service.BatchsAddService;
import cn.shopex.ecshopx.onecode.service.BatchsDeleteService;
import cn.shopex.ecshopx.onecode.service.BatchsDetailService;
import cn.shopex.ecshopx.onecode.service.BatchsListService;
import cn.shopex.ecshopx.onecode.service.BatchsUpdateService;
import cn.shopex.ecshopx.onecode.service.OneCodeBatchsWxaQrcodeService;
import cn.shopex.ecshopx.onecode.service.OneCodeShopOperatorContextGate;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422
)
@RestController("onecodeBatchsAdminV1")
@RequestMapping("/api/v1")
public class BatchsController {

	private static final String BATCHS_LIST_VALIDATION_MESSAGE = "获取批次列表出错.";

	private static final String BATCHS_DETAIL_VALIDATION_MESSAGE = "获取物品详情出错.";

	private static final String BATCHS_DELETE_VALIDATION_MESSAGE = "删除物品批次出错.";

	private static final String WXA_STREAM_VALIDATION_MESSAGE = "获取小程序码参数出错，请检查.";

	private static final Pattern SIGNED_INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private final BatchsAddService batchsAddService;
	private final BatchsDeleteService batchsDeleteService;
	private final BatchsUpdateService batchsUpdateService;
	private final BatchsListService batchsListService;
	private final BatchsDetailService batchsDetailService;
	private final OneCodeShopOperatorContextGate oneCodeShopOperatorContextGate;
	private final OneCodeBatchsWxaQrcodeService oneCodeBatchsWxaQrcodeService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public BatchsController(
			BatchsAddService batchsAddService,
			BatchsDeleteService batchsDeleteService,
			BatchsUpdateService batchsUpdateService,
			BatchsListService batchsListService,
			BatchsDetailService batchsDetailService,
			OneCodeShopOperatorContextGate oneCodeShopOperatorContextGate,
			OneCodeBatchsWxaQrcodeService oneCodeBatchsWxaQrcodeService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.batchsAddService = batchsAddService;
		this.batchsDeleteService = batchsDeleteService;
		this.batchsUpdateService = batchsUpdateService;
		this.batchsListService = batchsListService;
		this.batchsDetailService = batchsDetailService;
		this.oneCodeShopOperatorContextGate = oneCodeShopOperatorContextGate;
		this.oneCodeBatchsWxaQrcodeService = oneCodeBatchsWxaQrcodeService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "onecode.batchs.create")
	@PostMapping(value = "/onecode/batchs", name = "添加物品批次")
	public ResponseEntity<ApiResult<Map<String, Object>>> createBatchs(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		oneCodeShopOperatorContextGate.assertCreateBatchs(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));

		long thingId = parseRequiredPositiveLong(merged.get("thing_id"), "物品id必填");
		String batchNumber = requiredNonEmptyString(merged.get("batch_number"), "请填写批次编号");
		String batchName = requiredNonEmptyString(merged.get("batch_name"), "请填写批次名称");
		int batchQuantity = parseRequiredPositiveInt(merged.get("batch_quantity"), "请填写批次件数");
		validateShowTracePresent(merged);

		boolean showTraceForData = ValuePresence.hasEffectiveValue(merged.get("show_trace"));
		Object traceInfoRaw = merged.get("trace_info");
		boolean setTraceInfo = merged.containsKey("trace_info") && ValuePresence.hasEffectiveValue(traceInfoRaw);

		Map<String, Object> data =
				batchsAddService.addBatchs(
						companyId, thingId, batchNumber, batchName, batchQuantity, showTraceForData, traceInfoRaw, setTraceInfo);

		Map<String, Object> logParams = new LinkedHashMap<>(merged);
		logParams.put("company_id", companyId);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/onecode/batchs");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "添加物品批次");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void validateShowTracePresent(Map<String, Object> merged) {
		if (!merged.containsKey("show_trace") || merged.get("show_trace") == null) {
			throw new BadRequestException("是否显示流通信息");
		}
		Object v = merged.get("show_trace");
		if (v instanceof String s && !StringUtils.hasText(s)) {
			throw new BadRequestException("是否显示流通信息");
		}
	}

	private static long parseRequiredPositiveLong(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		long v;
		try {
			if (raw instanceof Number n) {
				v = n.longValue();
			} else {
				String t = raw.toString().trim();
				if (!StringUtils.hasText(t)) {
					throw new BadRequestException(message);
				}
				v = Long.parseLong(t);
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
		if (v < 1) {
			throw new BadRequestException(message);
		}
		return v;
	}

	private static int parseRequiredPositiveInt(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		int v;
		try {
			if (raw instanceof Number n) {
				v = n.intValue();
			} else {
				String t = raw.toString().trim();
				if (!StringUtils.hasText(t)) {
					throw new BadRequestException(message);
				}
				v = Integer.parseInt(t);
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
		if (v < 1) {
			throw new BadRequestException(message);
		}
		return v;
	}

	private static String requiredNonEmptyString(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		String s = raw instanceof String str ? str : raw.toString();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(message);
		}
		return s;
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

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.PLAIN,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422
	)
	@Activated(routeAlias = "onecode.batchs.lists")
	@GetMapping(value = "/onecode/batchs", name = "获取物品批次列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBatchsList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam,
			@RequestParam(name = "thing_id", required = false) String thingIdParam) {
		Map<String, Object> merged = RequestParamToMapResolver.toMergedMap(request);

		oneCodeShopOperatorContextGate.assertBatchsList(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		assertBatchsListPagination(pageParam, pageSizeParam);

		long companyId = toLong(ud.get("company_id"));
		long pageVal = Long.parseLong(pageParam.trim());
		int page = pageVal > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pageVal;
		int pageSize = Integer.parseInt(pageSizeParam.trim());

		Long thingId = resolveThingIdForBatchsListFilter(request, thingIdParam);
		Map<String, Object> data = batchsListService.list(companyId, thingId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void assertBatchsListPagination(String pageParam, String pageSizeParam) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendBatchsListPageErrors("page", pageParam, errors);
		appendBatchsListPageSizeErrors("pageSize", pageSizeParam, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(BATCHS_LIST_VALIDATION_MESSAGE);
		}
	}

	private static void appendBatchsListPageErrors(String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
		long v = Long.parseLong(p);
		if (v < 1) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须大于或等于1。");
		}
	}

	private static void appendBatchsListPageSizeErrors(String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
		long v = Long.parseLong(p);
		if (v < 1) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须大于或等于1。");
		}
		if (v > 100) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能大于100。");
		}
	}

	private static Long resolveThingIdForBatchsListFilter(HttpServletRequest request, String thingIdParam) {
		if (!request.getParameterMap().containsKey("thing_id")) {
			return null;
		}
		if (thingIdParam == null || thingIdParam.trim().isEmpty()) {
			return 0L;
		}
		String t = thingIdParam.trim();
		if (!SIGNED_INTEGER_STRING.matcher(t).matches()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422
	)
	@Activated(routeAlias = "onecode.batchs.detail")
	@GetMapping(value = "/onecode/batchs/{batch_id}", name = "获取物品批次详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBatchsDetail(
			HttpServletRequest request, @PathVariable("batch_id") String batchId) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("batch_id", batchId);

		oneCodeShopOperatorContextGate.assertBatchsDetail(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));
		assertBatchsDetailPathBatchId(batchId);
		long id = Long.parseLong(batchId.trim());
		Map<String, Object> data = batchsDetailService.getDetail(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void assertBatchsDetailPathBatchId(String batchId) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendBatchsPathBatchIdErrors("batch_id", batchId, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(BATCHS_DETAIL_VALIDATION_MESSAGE);
		}
	}

	private static void assertDeleteBatchsPathBatchId(String batchId) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendBatchsPathBatchIdErrors("batch_id", batchId, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(BATCHS_DELETE_VALIDATION_MESSAGE);
		}
	}

	private static void appendBatchsPathBatchIdErrors(
			String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
		long v = Long.parseLong(p);
		if (v < 1) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须大于或等于1。");
		}
	}

	/**
	 * Query params for wxaOneCodeStream: require present non-blank integer string only.
	 * 校验层不拒绝 {@code batch_id}/{@code num} 为 {@code 0} 的情况（查询 + {@code min:1} 历史行为）;
	 * those values must reach {@link OneCodeBatchsWxaQrcodeService} for parity.
	 */
	private static void appendWxaOneCodeStreamQueryIntErrors(
			String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422
	)
	@Activated(routeAlias = "onecode.batchs.delete")
	@DeleteMapping(value = "/onecode/batchs/{batch_id}", name = "删除物品批次")
	public ResponseEntity<Void> deleteBatchs(HttpServletRequest request, @PathVariable("batch_id") String batchId) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("batch_id", batchId);
		oneCodeShopOperatorContextGate.assertDeleteBatchs(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));
		assertDeleteBatchsPathBatchId(batchId);
		batchsDeleteService.deleteBatchs(companyId, Long.parseLong(batchId.trim()));

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("batch_id", batchId);
		logParams.put("company_id", companyId);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/onecode/batchs/" + batchId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "删除物品批次");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "onecode.batchs.update")
	@PutMapping(value = "/onecode/batchs/{batch_id}", name = "更新物品批次")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateBatchs(
			HttpServletRequest request,
			@PathVariable("batch_id") String batchId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("batch_id", batchId);
		oneCodeShopOperatorContextGate.assertUpdateBatchs(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));
		long pathBatchIdLong =
				parseRequiredPositiveLong(batchId, "请确认您所编辑的物品批次是否存在");

		long thingId = parseRequiredPositiveLong(merged.get("thing_id"), "物品id必填");
		String batchNumber = requiredNonEmptyString(merged.get("batch_number"), "请填写批次编号");
		String batchName = requiredNonEmptyString(merged.get("batch_name"), "请填写批次名称");
		int batchQuantity = parseRequiredPositiveInt(merged.get("batch_quantity"), "请填写批次件数");
		validateShowTracePresent(merged);

		boolean persistShowTraceColumn = ValuePresence.hasEffectiveValue(merged.get("show_trace"));
		Object traceInfoRaw = merged.get("trace_info");
		boolean traceInfoKeyPresent = merged.containsKey("trace_info");

		Map<String, Object> data =
				batchsUpdateService.updateBatchs(
						companyId,
						pathBatchIdLong,
						thingId,
						batchNumber,
						batchName,
						batchQuantity,
						persistShowTraceColumn,
						traceInfoRaw,
						traceInfoKeyPresent);

		Map<String, Object> logParams = new LinkedHashMap<>(merged);
		logParams.put("company_id", companyId);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/onecode/batchs/" + batchId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "更新物品批次");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422
	)
	@Activated(routeAlias = "onecode.batchs.wxacode")
	@GetMapping(value = "/onecode/wxaOneCodeStream", name = "获取物品批次小程序码")
	public ResponseEntity<byte[]> getWxaOneCodeStream(
			HttpServletRequest request,
			@RequestParam(value = "batch_id", required = false) String batchIdRaw,
			@RequestParam(value = "num", required = false) String numRaw) {
		Map<String, Object> merged = RequestParamToMapResolver.toMergedMap(request);
		oneCodeShopOperatorContextGate.assertWxaOneCodeStream(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendWxaOneCodeStreamQueryIntErrors("batch_id", batchIdRaw, errors);
		appendWxaOneCodeStreamQueryIntErrors("num", numRaw, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(WXA_STREAM_VALIDATION_MESSAGE);
		}

		long companyId = toLong(ud.get("company_id"));
		String batchIdForScene = Long.toString(Long.parseLong(batchIdRaw.trim()));
		String numForScene = Long.toString(Long.parseLong(numRaw.trim()));
		byte[] body = oneCodeBatchsWxaQrcodeService.buildJpegBytes(companyId, batchIdForScene, numForScene);
		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(body);
	}
}
