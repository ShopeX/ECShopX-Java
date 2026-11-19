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
import cn.shopex.ecshopx.common.web.QywxSalespersonAuthAttributes;
import cn.shopex.ecshopx.salesperson.service.SalespersonRelShopListService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonCheckDistributorIsValidService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("salespersonShopFrontV1")
@RequestMapping("/api/v1/h5app")
public class ShopSalespersonController {

	private final ShopSalespersonCheckDistributorIsValidService shopSalespersonCheckDistributorIsValidService;
	private final SalespersonRelShopListService salespersonRelShopListService;

	public ShopSalespersonController(
			ShopSalespersonCheckDistributorIsValidService shopSalespersonCheckDistributorIsValidService,
			SalespersonRelShopListService salespersonRelShopListService) {
		this.shopSalespersonCheckDistributorIsValidService = shopSalespersonCheckDistributorIsValidService;
		this.salespersonRelShopListService = salespersonRelShopListService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@QywxSalespersonAuth
	@GetMapping(value = "/wxapp/salesperson/distributorlist", name = "获取导购店铺列表")
	public ResponseEntity<Map<String, Object>> getDistributorDataList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "500") int pageSize,
			@RequestParam(value = "store_type", required = false) String storeType,
			@RequestParam(value = "store_name", required = false) String storeName) {
		long companyId = parseQywxCompanyIdFromRequest(request);
		long salespersonId = parseQywxSalespersonIdFromRequest(request);
		String salespersonIdStr = String.valueOf(salespersonId);
		String raw = storeType;
		String t = (raw == null) ? "" : raw.trim();
		String storeTypeParam = t.isEmpty() ? "distributor" : t;
		Map<String, Object> body = salespersonRelShopListService.getDistributorDataList(companyId, salespersonIdStr,
				storeTypeParam, storeName, page, pageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@QywxSalespersonAuth
	@GetMapping(value = "/wxapp/salesperson/distributor/is_valid", name = "校验导购员的店铺信息")
	public ResponseEntity<Map<String, Object>> checkDistributorIsValid(
			HttpServletRequest request,
			@RequestParam(value = "salesperson_id", required = false) String salespersonIdRaw,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		if (salespersonIdRaw == null || !StringUtils.hasText(salespersonIdRaw.trim())) {
			throw new BadRequestException("导购员id不能为空");
		}
		if (distributorIdRaw == null || !StringUtils.hasText(distributorIdRaw.trim())) {
			throw new BadRequestException("店铺id不能为空");
		}
		long companyId = parseQywxCompanyIdFromRequest(request);
		boolean status = shopSalespersonCheckDistributorIsValidService.checkDistributorIsValid(companyId,
				salespersonIdRaw.trim(), distributorIdRaw.trim());
		return ResponseEntity.ok(Map.of("data", Map.of("status", status)));
	}

	@SuppressWarnings("unchecked")
	private static long parseQywxSalespersonIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购身份无效");
		}
		Map<String, Object> auth = (Map<String, Object>) rawMap;
		Object sid = auth.get("salesperson_id");
		if (sid == null) {
			throw new BadRequestException("导购身份无效");
		}
		if (sid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("导购身份无效");
			}
			return v;
		}
		String s = sid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("导购身份无效");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new BadRequestException("导购身份无效");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购身份无效");
		}
	}

	@SuppressWarnings("unchecked")
	private static long parseQywxCompanyIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购身份无效");
		}
		Map<String, Object> auth = (Map<String, Object>) rawMap;
		Object cid = auth.get("company_id");
		if (cid == null) {
			throw new BadRequestException("企业信息有误");
		}
		if (cid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("企业信息有误");
			}
			return v;
		}
		String s = cid.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("企业信息有误");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new BadRequestException("企业信息有误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业信息有误");
		}
	}
}