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

package cn.shopex.ecshopx.salesperson.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.QywxSalespersonAuth;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.service.salesperson.SalespersonCartDataListFacade;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.QywxSalespersonAuthAttributes;
import cn.shopex.ecshopx.salesperson.service.SalespersonCartAddDataService;
import cn.shopex.ecshopx.salesperson.service.SalespersonCartCheckStatusService;
import cn.shopex.ecshopx.salesperson.service.SalespersonCartCountService;
import cn.shopex.ecshopx.salesperson.service.SalesPromotionsCreateService;
import cn.shopex.ecshopx.salesperson.service.SalespersonScanCodeAddCartService;
import cn.shopex.ecshopx.salesperson.web.ShopSalespersonFrontRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@QywxSalespersonAuth
@RestController("salespersonCartFrontV1")
@RequestMapping("/api/v1/h5app")
public class SalespersonCartController {

	private final SalespersonScanCodeAddCartService salespersonScanCodeAddCartService;
	private final SalespersonCartCheckStatusService salespersonCartCheckStatusService;
	private final SalespersonCartCountService salespersonCartCountService;
	private final SalespersonCartAddDataService salespersonCartAddDataService;
	private final SalespersonCartDataListFacade salespersonCartDataListFacade;
	private final SalesPromotionsCreateService salesPromotionsCreateService;

	public SalespersonCartController(
			SalespersonScanCodeAddCartService salespersonScanCodeAddCartService,
			SalespersonCartCheckStatusService salespersonCartCheckStatusService,
			SalespersonCartCountService salespersonCartCountService,
			SalespersonCartAddDataService salespersonCartAddDataService,
			SalespersonCartDataListFacade salespersonCartDataListFacade,
			SalesPromotionsCreateService salesPromotionsCreateService) {
		this.salespersonScanCodeAddCartService = salespersonScanCodeAddCartService;
		this.salespersonCartCheckStatusService = salespersonCartCheckStatusService;
		this.salespersonCartCountService = salespersonCartCountService;
		this.salespersonCartAddDataService = salespersonCartAddDataService;
		this.salespersonCartDataListFacade = salespersonCartDataListFacade;
		this.salesPromotionsCreateService = salesPromotionsCreateService;
	}

	@GetMapping(value = "/wxapp/salesperson/cartdataadd", name = "导购员购物车新增")
	public ResponseEntity<Map<String, Object>> cartdataAdd(HttpServletRequest request) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.parameterMapToMapLikeResolver(request);
		Object rawAuth = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			throw new BadRequestException("导购身份无效");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> authMap = (Map<String, Object>) rawAuth;

		long sessionSpId = longFromAuth(authMap.get("session_salesperson_id"));
		if (sessionSpId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long spId = longFromAuth(authMap.get("salesperson_id"));
		if (spId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long companyId = longFromAuth(authMap.get("company_id"));
		if (companyId <= 0) {
			throw new BadRequestException("企业信息有误");
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("item_id", merged.get("item_id"));
		filter.put("salesperson_id", authMap.get("salesperson_id"));
		filter.put("distributor_id", authMap.get("distributor_id"));
		filter.put("company_id", authMap.get("company_id"));

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("num", resolveNumForCartAdd(merged));
		params.put("is_checked", parseIsCheckedForCartAdd(merged));

		boolean accumulate = parseIsAccumulateFlag(merged);
		Map<String, Object> result = salespersonCartAddDataService.addCartdata(filter, params, accumulate);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@PostMapping(value = "/wxapp/salesperson/scancodeAddcart", name = "扫条形码加入购物车")
	public ResponseEntity<?> scanCodeSales(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object rawAuth = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			throw new BadRequestException("导购身份无效");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> authMap = (Map<String, Object>) rawAuth;
		Map<String, Object> result = salespersonScanCodeAddCartService.scanCodeSales(request, merged, authMap);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@GetMapping(value = "/wxapp/salesperson/cartdatalist", name = "获取导购员购物车")
	public ResponseEntity<Map<String, Object>> getCartdataList(HttpServletRequest request) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.parameterMapToMapLikeResolver(request);
		Object rawAuth = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			throw new BadRequestException("导购身份无效");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> authMap = (Map<String, Object>) rawAuth;

		long sessionSpId = longFromAuth(authMap.get("session_salesperson_id"));
		if (sessionSpId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long spId = longFromAuth(authMap.get("salesperson_id"));
		if (spId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long companyId = longFromAuth(authMap.get("company_id"));
		if (companyId <= 0) {
			throw new BadRequestException("企业信息有误");
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("salesperson_id", authMap.get("salesperson_id"));
		filter.put("distributor_id", authMap.get("distributor_id"));
		filter.put("company_id", authMap.get("company_id"));

		long userId = userIdFromQuery(merged.get("user_id"));
		Map<String, Object> result = salespersonCartDataListFacade.getCartdataList(userId, filter, request);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@PutMapping(value = "/wxapp/salesperson/cartupdate/checkstatus", name = "修改购物车选中状态")
	public ResponseEntity<Map<String, Object>> updateCartCheckStatus(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object rawAuth = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			throw new BadRequestException("导购身份无效");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> authMap = (Map<String, Object>) rawAuth;
		salespersonCartCheckStatusService.updateCartCheckStatus(merged, authMap);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@GetMapping(value = "/wxapp/salesperson/cartcount", name = "获取购物车数量")
	public ResponseEntity<Map<String, Object>> getCartItemCount(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			throw new BadRequestException("导购身份无效");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> authMap = (Map<String, Object>) rawAuth;
		Map<String, Object> result = salespersonCartCountService.getCartItemCount(authMap);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@GetMapping(value = "/wxapp/salesperson/salesPromotion", name = "获取导购员促销单")
	public ResponseEntity<Map<String, Object>> createSalesPromotion(HttpServletRequest request) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.parameterMapToMapLikeResolver(request);
		Object rawAuth = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			throw new BadRequestException("导购身份无效");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> authMap = (Map<String, Object>) rawAuth;

		long sessionSpId = longFromAuth(authMap.get("session_salesperson_id"));
		if (sessionSpId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long spId = longFromAuth(authMap.get("salesperson_id"));
		if (spId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long companyId = longFromAuth(authMap.get("company_id"));
		if (companyId <= 0) {
			throw new BadRequestException("企业信息有误");
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("salesperson_id", authMap.get("salesperson_id"));
		filter.put("distributor_id", authMap.get("distributor_id"));
		filter.put("company_id", authMap.get("company_id"));

		long salespersonId = longFromAuth(authMap.get("salesperson_id"));
		long distributorId = longFromAuth(authMap.get("distributor_id"));

		Map<String, Object> cartData =
				salespersonCartDataListFacade.getCartdataListForCheckout(0L, filter, request);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validCart = (List<Map<String, Object>>) cartData.get("valid_cart");
		if (validCart != null && !validCart.isEmpty()) {
			Map<String, Object> bucket0 = validCart.get(0);
			Object listObj = bucket0.get("list");
			if (listObj instanceof List<?> rawList && !rawList.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> cartlist = (List<Map<String, Object>>) listObj;
				Map<String, Object> salePromotion =
						salesPromotionsCreateService.createSalesPromotions(
								companyId, salespersonId, distributorId, cartlist);
				bucket0.put("sales_promotion_id", salePromotion.get("sales_promotion_id"));
			}
		}

		return ResponseEntity.ok(Map.of("data", cartData));
	}

	private static long userIdFromQuery(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long longFromAuth(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean parseIsCheckedForCartAdd(Map<String, Object> merged) {
		if (!merged.containsKey("is_checked")) {
			return true;
		}
		return checkedFlagToBoolean(merged.get("is_checked"));
	}

	private static boolean checkedFlagToBoolean(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		if (raw instanceof String s) {
			s = s.trim();
			if (s.isEmpty()) {
				return false;
			}
			if ("false".equals(s)) {
				return false;
			}
			return true;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("false".equals(s)) {
			return false;
		}
		return true;
	}

	private static boolean parseIsAccumulateFlag(Map<String, Object> merged) {
		if (!merged.containsKey("is_accumulate")) {
			return true;
		}
		Object raw = merged.get("is_accumulate");
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (raw instanceof String s) {
			s = s.trim();
			if (s.equalsIgnoreCase("false") || "0".equals(s)) {
				return false;
			}
			return true;
		}
		String s = raw.toString().trim();
		if (s.equalsIgnoreCase("false") || "0".equals(s)) {
			return false;
		}
		return true;
	}

	private static long resolveNumForCartAdd(Map<String, Object> merged) {
		Object n = merged.get("num");
		if (n == null) {
			return 0L;
		}
		if (n instanceof Number num) {
			return num.longValue();
		}
		String s = n.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
