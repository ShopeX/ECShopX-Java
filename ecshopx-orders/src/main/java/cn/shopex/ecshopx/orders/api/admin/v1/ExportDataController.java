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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.service.financial.export.FinancialSalesreportExportService;
import cn.shopex.ecshopx.orders.service.invoice.export.InvoiceExportService;
import cn.shopex.ecshopx.orders.service.orderexport.OrderExportService;
import cn.shopex.ecshopx.orders.service.orderexport.OrderNormalDataExportService;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportEmptyShop;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportQueued;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportResult;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportService;
import cn.shopex.ecshopx.orders.service.rights.export.consume.RightsConsumeLogExportService;
import cn.shopex.ecshopx.orders.service.tradeexport.TradeExportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("ordersAdminV1ExportData")
@RequestMapping("/api/v1")
public class ExportDataController {

	private final FinancialSalesreportExportService financialSalesreportExportService;
	private final InvoiceExportService invoiceExportService;
	private final OrderExportService orderExportService;
	private final OrderNormalDataExportService orderNormalDataExportService;
	private final RightsExportService rightsExportService;
	private final RightsConsumeLogExportService rightsConsumeLogExportService;
	private final TradeExportService tradeExportService;
	private final ObjectMapper objectMapper;

	public ExportDataController(
			FinancialSalesreportExportService financialSalesreportExportService,
			InvoiceExportService invoiceExportService,
			OrderExportService orderExportService,
			OrderNormalDataExportService orderNormalDataExportService,
			RightsExportService rightsExportService,
			RightsConsumeLogExportService rightsConsumeLogExportService,
			TradeExportService tradeExportService,
			ObjectMapper objectMapper) {
		this.financialSalesreportExportService = financialSalesreportExportService;
		this.invoiceExportService = invoiceExportService;
		this.orderExportService = orderExportService;
		this.orderNormalDataExportService = orderNormalDataExportService;
		this.rightsExportService = rightsExportService;
		this.rightsConsumeLogExportService = rightsConsumeLogExportService;
		this.tradeExportService = tradeExportService;
		this.objectMapper = objectMapper;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "order.list.export")
	@GetMapping(value = "/orders/exportdata", name = "导出订单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportOrderData(
			HttpServletRequest request,
			@RequestParam(name = "order_type", required = false) String orderType,
			@RequestParam(name = "type", required = false) String type,
			@RequestParam(name = "shop_id", required = false) String shopId,
			@RequestParam(name = "mobile", required = false) String mobile,
			@RequestParam(name = "order_class", required = false) String orderClass,
			@RequestParam(name = "order_class_exclude", required = false) String orderClassExclude,
			@RequestParam(name = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(name = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(name = "delivery_time_begin", required = false) String deliveryTimeBegin,
			@RequestParam(name = "delivery_time_end", required = false) String deliveryTimeEnd,
			@RequestParam(name = "order_status", required = false) String orderStatus,
			@RequestParam(name = "act_id", required = false) String actId,
			@RequestParam(name = "pay_type", required = false) String payType,
			@RequestParam(name = "order_id", required = false) String orderId,
			@RequestParam(name = "receipt_type", required = false) String receiptType,
			@RequestParam(name = "user_id", required = false) String userId,
			@RequestParam(name = "source_id", required = false) String sourceId,
			@RequestParam(name = "distributor_id", required = false) String distributorId,
			@RequestParam(name = "salesman_mobile", required = false) String salesmanMobile,
			@RequestParam(name = "activity_name", required = false) String activityName,
			@RequestParam(name = "activity_status", required = false) String activityStatus,
			@RequestParam(name = "subdistrict_parent_id", required = false) String subdistrictParentId,
			@RequestParam(name = "subdistrict_id", required = false) String subdistrictId,
			@RequestParam(name = "promoter_identity", required = false) String promoterIdentity,
			@RequestParam(name = "promoter_mobile", required = false) String promoterMobile) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		String operatorType = readOperatorTypeFromJwt(request);
		Long merchantIdOrNull = readMerchantIdFromJwtOrNull(request);
		List<Long> distributorIds = extractOperatorDistributorIds(readJwtMap(request).get("distributor_ids"));
		List<Long> shopIds = extractOperatorShopIds(readJwtMap(request).get("shop_ids"));
		orderExportService.exportOrderData(
				companyId, operatorId, operatorType, merchantIdOrNull, distributorIds, shopIds, request);
		try {
			String json = objectMapper.writeValueAsString(Map.of("data", Map.of("status", true)));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "invoice.list.export")
	@GetMapping(value = "/invoice/exportdata", name = "导出发票", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportInvoiceData(
			HttpServletRequest request,
			@RequestParam(name = "order_type", required = false) String orderType,
			@RequestParam(name = "order_class_exclude", required = false) String orderClassExclude,
			@RequestParam(name = "order_class", required = false) String orderClass,
			@RequestParam(name = "distributor_id", required = false) String distributorId,
			@RequestParam(name = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(name = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(name = "order_status", required = false) String orderStatus,
			@RequestParam(name = "pay_type", required = false) String payType,
			@RequestParam(name = "order_id", required = false) String orderId,
			@RequestParam(name = "mobile", required = false) String mobile,
			@RequestParam(name = "user_id", required = false) String userId,
			@RequestParam(name = "source_id", required = false) String sourceId,
			@RequestParam(name = "salesman_mobile", required = false) String salesmanMobile) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		String operatorType = readOperatorTypeFromJwt(request);
		Long merchantIdOrNull = readMerchantIdFromJwtOrNull(request);
		List<Long> distributorIds = extractOperatorDistributorIds(readJwtMap(request).get("distributor_ids"));
		invoiceExportService.exportInvoiceData(
				companyId, operatorId, operatorType, merchantIdOrNull, distributorIds, request);
		try {
			String json = objectMapper.writeValueAsString(Map.of("data", Map.of("status", true)));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "rights.list.export")
	@GetMapping(value = "/rights/exportdata", name = "导出权益", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportRightData(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobile,
			@RequestParam(name = "user_id", required = false) String userId,
			@RequestParam(name = "valid", required = false) String valid,
			@RequestParam(name = "date_begin", required = false) String dateBegin,
			@RequestParam(name = "date_end", required = false) String dateEnd,
			@RequestParam(name = "rights_from", required = false) String rightsFrom,
			@RequestParam(name = "order_id", required = false) String orderId,
			@RequestParam(name = "shop_id", required = false) String shopId,
			@RequestHeader(value = "x-datapass-block", required = false) String datapassBlockHeader) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		RightsExportResult r =
				rightsExportService.exportRightData(
						companyId,
						operatorId,
						request,
						mobile,
						userId,
						valid,
						dateBegin,
						dateEnd,
						rightsFrom,
						orderId,
						shopId,
						datapassBlockHeader);
		try {
			if (r instanceof RightsExportEmptyShop) {
				String body =
						objectMapper.writeValueAsString(
								ApiResult.ok(Map.of("list", List.of(), "total_count", 0)));
				return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
			}
			if (r instanceof RightsExportQueued) {
				String body = objectMapper.writeValueAsString(Map.of("status", true));
				return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
			}
			throw new IllegalStateException("unexpected export result");
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "normal.list.export")
	@GetMapping(
			value = "/orders/exportnormaldata",
			name = "导出实体订单",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportOrderNormalData(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobile,
			@RequestParam(name = "shop_id", required = false) String shopId,
			@RequestParam(name = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(name = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(name = "order_id", required = false) String orderId,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "exportStart", required = false) String exportStart,
			@RequestParam(name = "exportLimit", required = false) String exportLimit) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, String> body = orderNormalDataExportService.exportOrderNormalData(companyId, request);
		try {
			String json = objectMapper.writeValueAsString(Map.of("data", body));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "trades.list.export")
	@DataPass
	@GetMapping(value = "/trades/exportdata", name = "导出交易单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportTradeData(
			HttpServletRequest request,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "orderId", required = false) String orderId,
			@RequestParam(name = "mobile", required = false) String mobile,
			@RequestParam(name = "date_begin", required = false) String dateBegin,
			@RequestParam(name = "date_end", required = false) String dateEnd,
			@RequestParam(name = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(name = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(name = "shop_id", required = false) String shopId,
			@RequestParam(name = "distributor_id", required = false) String distributorId,
			@RequestParam(name = "order_type", required = false) String orderType) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		String operatorType = readOperatorTypeFromJwt(request);
		Long merchantIdOrNull = readMerchantIdFromJwtOrNull(request);
		List<Long> distributorIds = extractOperatorDistributorIds(readJwtMap(request).get("distributor_ids"));
		List<Long> shopIds = extractOperatorShopIds(readJwtMap(request).get("shop_ids"));
		tradeExportService.exportTradeData(
				companyId, operatorId, operatorType, merchantIdOrNull, distributorIds, shopIds, request);
		try {
			String json = objectMapper.writeValueAsString(Map.of("data", Map.of("status", true)));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "rights.log.list.export")
	@GetMapping(
			value = "/rights/logExport",
			name = "导出权益核销",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportRightConsumeData(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		rightsConsumeLogExportService.exportRightConsumeData(companyId, operatorId, request);
		try {
			String json = objectMapper.writeValueAsString(Map.of("status", true));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "financial.salesreport.export")
	@GetMapping(value = "/financial/salesreport", name = "财务销售报表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> exportSalesreportData(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderId,
			@RequestParam(name = "brand", required = false) String brand,
			@RequestParam(name = "main_category", required = false) String mainCategory,
			@RequestParam(name = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(name = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(name = "delivery_time_start_begin", required = false) String deliveryTimeStartBegin,
			@RequestParam(name = "delivery_time_start_end", required = false) String deliveryTimeStartEnd) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		LinkedHashMap<String, String> query = new LinkedHashMap<>();
		if (orderId != null && !orderId.isBlank()) {
			query.put("order_id", orderId);
		}
		if (brand != null && !brand.isBlank()) {
			query.put("brand", brand);
		}
		if (mainCategory != null && !mainCategory.isBlank()) {
			query.put("main_category", mainCategory);
		}
		if (timeStartBegin != null && !timeStartBegin.isBlank()) {
			query.put("time_start_begin", timeStartBegin);
		}
		if (timeStartEnd != null && !timeStartEnd.isBlank()) {
			query.put("time_start_end", timeStartEnd);
		}
		if (deliveryTimeStartBegin != null && !deliveryTimeStartBegin.isBlank()) {
			query.put("delivery_time_start_begin", deliveryTimeStartBegin);
		}
		if (deliveryTimeStartEnd != null && !deliveryTimeStartEnd.isBlank()) {
			query.put("delivery_time_start_end", deliveryTimeStartEnd);
		}
		financialSalesreportExportService.exportSalesreportData(companyId, operatorId, query);
		try {
			String json = objectMapper.writeValueAsString(Map.of("status", true));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Map<?, ?> readJwtMap(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (attr instanceof Map<?, ?> jwt) {
			return jwt;
		}
		throw new UnauthorizedException("未登录");
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Map<?, ?> jwt = readJwtMap(request);
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new ResourceException("登录验证错误");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("登录验证错误");
		}
	}

	private static long readOperatorIdFromJwt(HttpServletRequest request) {
		Map<?, ?> jwt = readJwtMap(request);
		Object operatorIdObj = jwt.get("operator_id");
		if (operatorIdObj == null) {
			throw new ResourceException("登录验证错误");
		}
		try {
			return Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("登录验证错误");
		}
	}

	private static String readOperatorTypeFromJwt(HttpServletRequest request) {
		Object v = readJwtMap(request).get("operator_type");
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static Long readMerchantIdFromJwtOrNull(HttpServletRequest request) {
		Object v = readJwtMap(request).get("merchant_id");
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static List<Long> extractOperatorShopIds(Object raw) {
		List<Long> out = new ArrayList<>();
		if (!(raw instanceof List<?> list)) {
			return out;
		}
		for (Object row : list) {
			if (row instanceof Map<?, ?> m) {
				Object id = m.get("shop_id");
				if (id == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(id).trim()));
				} catch (NumberFormatException ignored) {
				}
			} else if (row instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out;
	}

	private static List<Long> extractOperatorDistributorIds(Object raw) {
		List<Long> out = new ArrayList<>();
		if (!(raw instanceof List<?> list)) {
			return out;
		}
		for (Object row : list) {
			if (row instanceof Map<?, ?> m) {
				Object id = m.get("distributor_id");
				if (id == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(id).trim()));
				} catch (NumberFormatException ignored) {
				}
			} else if (row instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out;
	}
}
