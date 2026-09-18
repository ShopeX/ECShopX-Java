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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartDataListFacade;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartScanBarcodeFacade;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartAddDataService;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartDeleteDataService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@RestController("companysAdminV1OperatorCart")
@RequestMapping("/api/v1/operator")
public class OperatorCartController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final OperatorCartAddDataService operatorCartAddDataService;
	private final OperatorCartScanBarcodeFacade scanBarcodeFacade;
	private final OperatorCartDataListFacade operatorCartDataListFacade;
	private final OperatorCartDeleteDataService operatorCartDeleteDataService;

	public OperatorCartController(OperatorCartAddDataService operatorCartAddDataService,
			OperatorCartScanBarcodeFacade scanBarcodeFacade, OperatorCartDataListFacade operatorCartDataListFacade,
			OperatorCartDeleteDataService operatorCartDeleteDataService) {
		this.operatorCartAddDataService = operatorCartAddDataService;
		this.scanBarcodeFacade = scanBarcodeFacade;
		this.operatorCartDataListFacade = operatorCartDataListFacade;
		this.operatorCartDeleteDataService = operatorCartDeleteDataService;
	}

	@Activated(routeAlias = "companys.operator.cartdata.scanadd")
	@PostMapping(value = "/scancodeAddcart", name = "扫条形码加入购物车")
	public ResponseEntity<ApiResult<Object>> scanCodeSales(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readLongClaimRelaxed(jwt, "company_id");
		long operatorId = readLongClaimRelaxed(jwt, "operator_id");
		long distributorId = longVal(input.get("distributor_id"), 0L);
		String barcodeStr = normalizeOperatorScanBarcode(input.get("barcode"));
		long itemId = scanBarcodeFacade.resolveItemIdForOperatorScan(companyId, distributorId, barcodeStr);
		Map<String, Object> columnMap = operatorCartAddDataService.addCartData(companyId, operatorId, distributorId,
				itemId, 1L, true, true, true, true);
		if (columnMap.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(columnMap));
	}

	@Activated(routeAlias = "companys.operator.cartdata.add")
	@PostMapping(value = "/cartdataadd", name = "管理员购物车新增")
	public ResponseEntity<ApiResult<Object>> cartDataAdd(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readLongClaimRelaxed(jwt, "company_id");
		long operatorId = readLongClaimRelaxed(jwt, "operator_id");
		boolean itemIdPresent = input.containsKey("item_id");
		boolean distributorIdPresent = input.containsKey("distributor_id");
		long itemId = longVal(input.get("item_id"), 0L);
		long num = longVal(input.get("num"), 0L);
		long distributorId = longVal(input.get("distributor_id"), 0L);
		boolean isChecked = parseIsChecked(input.get("is_checked"));
		boolean accumulate = parseIsAccumulate(input.get("is_accumulate"));
		Map<String, Object> columnMap =
				operatorCartAddDataService.addCartData(companyId, operatorId, distributorId, itemId, num, isChecked,
						accumulate, itemIdPresent, distributorIdPresent);
		if (columnMap.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(columnMap));
	}

	@Activated(routeAlias = "companys.operator.cartdata.update")
	@PostMapping(value = "/cartdataupdate", name = "管理员购物车更新")
	public ResponseEntity<ApiResult<Object>> updateCartData(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readLongClaimRelaxed(jwt, "company_id");
		long operatorId = readLongClaimRelaxed(jwt, "operator_id");
		boolean itemIdPresent = input.containsKey("item_id");
		long cartId = longVal(input.get("cart_id"), 0L);
		long itemId = longVal(input.get("item_id"), 0L);
		long num = longVal(input.get("num"), 0L);
		long distributorId = longVal(input.get("distributor_id"), 0L);
		boolean isChecked = parseIsChecked(input.get("is_checked"));
		Map<String, Object> columnMap = operatorCartAddDataService.updateCartData(companyId, operatorId, distributorId,
				cartId, itemId, num, isChecked, itemIdPresent, true);
		return ResponseEntity.ok(ApiResult.ok(columnMap));
	}

	@Activated(routeAlias = "companys.operator.cartdata.list")
	@GetMapping(value = "/cartdatalist", name = "获取管理员购物车")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCartDataList(HttpServletRequest request,
			@RequestParam(value = "user_id", required = false, defaultValue = "0") String userIdRaw,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readLongClaimRelaxed(jwt, "company_id");
		long operatorId = readLongClaimRelaxed(jwt, "operator_id");
		long distributorId = longVal(distributorIdRaw, 0L);
		long targetUserId = longVal(userIdRaw, 0L);
		Map<String, Object> data = operatorCartDataListFacade.getCartDataList(companyId, operatorId, distributorId,
				targetUserId, request, false);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "companys.operator.cartdata.del")
	@DeleteMapping(value = "/cartdatadel", name = "管理员购物车删除")
	public ResponseEntity<ApiResult<Map<String, Object>>> delCartData(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readLongClaimRelaxed(jwt, "company_id");
		long operatorId = readLongClaimRelaxed(jwt, "operator_id");
		Long cartF = nullableTruthyCartFilterLong(input.get("cart_id"));
		Long itemF = nullableTruthyCartFilterLong(input.get("item_id"));
		operatorCartDeleteDataService.delCartData(companyId, operatorId, cartF, itemF);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long readLongClaimRelaxed(Map<String, Object> jwt, String key) {
		Object v = jwt.get(key);
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/** 条码缺省为 {@code "0"}；数字转为 long 的十进制字符串。 */
	private static String normalizeOperatorScanBarcode(Object o) {
		if (o == null) {
			return "0";
		}
		if (o instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		return o.toString().trim();
	}

	private static long longVal(Object o, long defaultVal) {
		if (o == null) {
			return defaultVal;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	/**
	 * 将可选的长整型过滤参数规范化：入参缺失、空串、数值 0 或无法解析为长整数时返回 null，表示不在该维度追加条件；否则返回解析后的 ID。
	 */
	private static Long nullableTruthyCartFilterLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Boolean b) {
			if (!b) {
				return null;
			}
			long vb = longVal(raw, 0L);
			return vb == 0L ? null : Long.valueOf(vb);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
			long v;
			try {
				v = Long.parseLong(t);
			} catch (NumberFormatException e) {
				return null;
			}
			return v == 0L ? null : Long.valueOf(v);
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v == 0L ? null : Long.valueOf(v);
		}
		long v = longVal(raw, 0L);
		return v == 0L ? null : Long.valueOf(v);
	}

	/**
	 * 默认勾选；仅 false、不区分大小写的 {@code false}、{@code "0"} 为未勾选。
	 */
	private static boolean parseIsChecked(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return true;
		}
		if ("0".equals(s)) {
			return false;
		}
		return !"false".equalsIgnoreCase(s);
	}

	/**
	 * 默认累加；{@code false}、{@code "false"}、{@code "0"}、{@link Boolean#FALSE} 为覆盖数量。
	 */
	private static boolean parseIsAccumulate(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		String s = raw.toString().trim();
		if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
			return false;
		}
		return true;
	}
}
