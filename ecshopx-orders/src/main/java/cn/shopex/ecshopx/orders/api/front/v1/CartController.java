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

package cn.shopex.ecshopx.orders.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.port.WxappCartAddPort;
import cn.shopex.ecshopx.orders.port.WxappCartListPort;
import cn.shopex.ecshopx.orders.port.WxappCheckPlusItemPort;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappBatchUpdateCartNumService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappCartParamMergeSupport;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappDeleteCartDataService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappUpdateCartCheckStatusService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappUpdateCartItemPromotionService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappUpdateCartNumService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@FrontAuth
@RestController("ordersFrontV1Cart")
@RequestMapping("/api/v1/h5app/wxapp")
public class CartController {

	private static final Logger log = LoggerFactory.getLogger(CartController.class);

	private final WxappCartAddPort wxappCartAddPort;
	private final WxappCartListPort wxappCartListPort;
	private final WxappCheckPlusItemPort wxappCheckPlusItemPort;
	private final WxappBatchUpdateCartNumService wxappBatchUpdateCartNumService;
	private final WxappDeleteCartDataService wxappDeleteCartDataService;
	private final WxappUpdateCartCheckStatusService wxappUpdateCartCheckStatusService;
	private final WxappUpdateCartItemPromotionService wxappUpdateCartItemPromotionService;
	private final WxappUpdateCartNumService wxappUpdateCartNumService;

	public CartController(
			WxappCartAddPort wxappCartAddPort,
			WxappCartListPort wxappCartListPort,
			WxappCheckPlusItemPort wxappCheckPlusItemPort,
			WxappBatchUpdateCartNumService wxappBatchUpdateCartNumService,
			WxappDeleteCartDataService wxappDeleteCartDataService,
			WxappUpdateCartCheckStatusService wxappUpdateCartCheckStatusService,
			WxappUpdateCartItemPromotionService wxappUpdateCartItemPromotionService,
			WxappUpdateCartNumService wxappUpdateCartNumService) {
		this.wxappCartAddPort = wxappCartAddPort;
		this.wxappCartListPort = wxappCartListPort;
		this.wxappCheckPlusItemPort = wxappCheckPlusItemPort;
		this.wxappBatchUpdateCartNumService = wxappBatchUpdateCartNumService;
		this.wxappDeleteCartDataService = wxappDeleteCartDataService;
		this.wxappUpdateCartCheckStatusService = wxappUpdateCartCheckStatusService;
		this.wxappUpdateCartItemPromotionService = wxappUpdateCartItemPromotionService;
		this.wxappUpdateCartNumService = wxappUpdateCartNumService;
	}

	@PostMapping(
			value = "/cart",
			name = "加购",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> addCart(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> params = WxappCartParamMergeSupport.applyAuthAndCartDefaults(merged, auth);
		Object shopTypeRaw = params.get("shop_type");
		if (shopTypeRaw == null || !StringUtils.hasText(shopTypeRaw.toString().trim())) {
			params.remove("shop_type");
		} else {
			params.put("shop_type", shopTypeRaw.toString().trim());
		}
		Object data = wxappCartAddPort.addCart(params);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/cart", name = "购物车列表")
	public ResponseEntity<ApiResult<Object>> getCartList(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (isUserIdFalsyForGetCartList(auth.get("user_id"))) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		Map<String, Object> query = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		long companyId = longVal(auth.get("company_id"));
		long shopId = resolveShopIdFilterForGetCart(query);
		Object st = query.get("shop_type");
		String shopType =
				(st == null || !StringUtils.hasText(st.toString().trim())) ? "distributor" : st.toString().trim();
		Map<String, Object> data =
				wxappCartListPort.getCartList(companyId, longVal(auth.get("user_id")), shopId, shopType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/cart/list", name = "分销商购物车")
	public ResponseEntity<ApiResult<Object>> getDistributorCartList(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> auth = resolveFrontNoAuthClaims(request);
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("未授权");
		}
		auth.put("company_id", companyId);

		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		normalizeDistributorCartListQuery(merged);
		normalizeDistributorCartItemsList(merged, log);

		String cartType = merged.get("cart_type").toString().trim();
		if (!"offline".equals(cartType) && isUserIdFalsyForGetCartList(auth.get("user_id"))) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}

		long authUserId = longVal(auth.get("user_id"));
		long effectiveUserId = authUserId;
		long promoterUserId = longVal(merged.get("promoter_user_id"));
		long buyUserId = longVal(merged.get("buy_user_id"));
		if (promoterUserId != 0L) {
			merged.put("promoter_user_id", promoterUserId);
		}
		if (buyUserId != 0L && promoterUserId == authUserId) {
			effectiveUserId = buyUserId;
		}

		Map<String, Object> data =
				wxappCartListPort.getDistributorCartList(companyId, authUserId, effectiveUserId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DeleteMapping(value = "/cartdel", name = "删购物车", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteCartData(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = longVal(auth.get("company_id"));
		long authUserId = longVal(auth.get("user_id"));
		Object shopRaw = merged.get("distributor_id");
		Long shopIdFilterOrNull =
				isUnsetShopIdPrimary(shopRaw) ? null : Long.valueOf(normalizeShopIdFilterForCart(shopRaw));
		wxappDeleteCartDataService.deleteCartData(companyId, authUserId, merged, shopIdFilterOrNull);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@DeleteMapping(value = "/cartdelbat", name = "批量删购物车", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteCartDataBat(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = longVal(auth.get("company_id"));
		wxappDeleteCartDataService.deleteCartDataBat(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@PutMapping(
			value = "/cartupdate/checkstatus",
			name = "选中状态",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateCartCheckStatus(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = longVal(auth.get("company_id"));
		long authUserId = longVal(auth.get("user_id"));
		long buyUserId = longVal(merged.get("buy_user_id"));
		long effectiveUserId = (buyUserId != 0L) ? buyUserId : authUserId;
		long cartId = parseCartIdStrictLong(merged.get("cart_id"));
		int rows =
				wxappUpdateCartCheckStatusService.updateCartCheckStatus(
						companyId,
						effectiveUserId,
						cartId,
						resolveIsCheckedLoose(merged.get("is_checked")));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", rows)));
	}

	@PutMapping(
			value = "/cartupdate/batchnum",
			name = "批量数量",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> batchUpdateCartNum(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		if (!merged.containsKey("cart") || merged.get("cart") == null) {
			throw new BadRequestException("参数非法");
		}
		Object cartRaw = merged.get("cart");
		if (!(cartRaw instanceof List<?> cartList)) {
			throw new BadRequestException("参数非法");
		}
		if (cartList.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		List<Map<String, Object>> cartItems = new ArrayList<>(cartList.size());
		for (Object el : cartList) {
			if (!(el instanceof Map<?, ?> rowRaw)) {
				throw new BadRequestException("参数非法");
			}
			Map<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rowRaw.entrySet()) {
				row.put(String.valueOf(e.getKey()), e.getValue());
			}
			cartItems.add(row);
		}
		List<Map<String, Object>> result =
				wxappBatchUpdateCartNumService.batchUpdateCartNum(companyId, userId, cartItems);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@PutMapping(
			value = "/cartupdate/num",
			name = "商品数量",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> updateCartNum(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = longVal(auth.get("company_id"));
		long authUserId = longVal(auth.get("user_id"));
		Object data = wxappUpdateCartNumService.updateCartNum(companyId, authUserId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PutMapping(
			value = "/cartupdate/promotion",
			name = "促销",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateCartItemPromotion(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		long cartId = parseCartIdStrictLong(merged.get("cart_id"));
		long marketingId = parseMarketingIdStrictLong(merged.get("activity_id"));
		Object shopTypeObj = merged.get("shop_type");
		String shopTypeStr = shopTypeObj == null ? "" : shopTypeObj.toString().trim();
		String wxappOpt = null;
		if (StringUtils.hasText(stringVal(auth.get("wxapp_appid")))) {
			wxappOpt = stringVal(auth.get("wxapp_appid"));
		}
		wxappUpdateCartItemPromotionService.updateCartItemPromotion(
				companyId, userId, wxappOpt, shopTypeStr, cartId, marketingId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@GetMapping(value = "/cartcount", name = "购物车数量")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCartItemCount(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> query = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		Object st = query.get("shop_type");
		if (st == null || !StringUtils.hasText(st.toString().trim())) {
			throw new BadRequestException("参数非法");
		}
		String shopType = st.toString().trim();
		String cartType = stringVal(query.get("cart_type"));
		if (!StringUtils.hasText(cartType)) {
			cartType = "cart";
		}
		long companyId = longVal(auth.get("company_id"));
		long shopId = resolveShopIdFilterForGetCart(query);
		int iscrossborder = intParamWithDefault(query.get("iscrossborder"), 0, 0);
		int isShopScreen = intParamWithDefault(query.get("isShopScreen"), 0, 0);
		long promoterUserId = longVal(query.get("promoter_user_id"));
		long buyUserId = longVal(query.get("buy_user_id"));
		Map<String, Object> data =
				wxappCartListPort.getCartItemCount(
						companyId,
						longVal(auth.get("user_id")),
						shopId,
						cartType,
						shopType,
						iscrossborder,
						isShopScreen,
						promoterUserId,
						buyUserId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(
			value = "/cart/check/plusitem",
			name = "加价购",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> checkPlusItem(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		wxappCheckPlusItemPort.checkPlusItem(companyId, userId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private Map<String, Object> resolveFrontNoAuthClaims(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth instanceof Map<?, ?> authRaw) {
			return toStringKeyMap(authRaw);
		}
		rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawAuth instanceof Map<?, ?> authRaw2) {
			return toStringKeyMap(authRaw2);
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		auth.put("user_id", 0L);
		return auth;
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> raw) {
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private static void normalizeDistributorCartListQuery(Map<String, Object> merged) {
		Object ct = merged.get("cart_type");
		merged.put(
				"cart_type",
				(ct == null || !StringUtils.hasText(ct.toString().trim())) ? "cart" : ct.toString().trim());
		Object st = merged.get("shop_type");
		if (st == null || !StringUtils.hasText(st.toString().trim())) {
			merged.put("shop_type", "distributor");
		} else {
			merged.put("shop_type", st.toString().trim());
		}
		merged.put("iscrossborder", intParamWithDefault(merged.get("iscrossborder"), 0, 0));
		merged.put("isShopScreen", intParamWithDefault(merged.get("isShopScreen"), 0, 0));
		merged.put("isNostores", intParamWithDefault(merged.get("isNostores"), 2, 0));
	}

	private static void normalizeDistributorCartItemsList(Map<String, Object> merged, Logger log) {
		Object raw = merged.get("items");
		if (!(raw instanceof List<?> list)) {
			merged.put("items", List.of());
			return;
		}
		List<Map<String, Object>> out = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			Object el = list.get(i);
			if (el instanceof Map<?, ?> m) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(row);
			} else {
				log.warn(
						"distributor cart list skipped non-Map items entry index={} type={}",
						i,
						el == null ? "null" : el.getClass().getName());
			}
		}
		merged.put("items", out);
	}

	private static int intParamWithDefault(Object v, int whenMissingOrNull, int whenBadParse) {
		if (v == null) {
			return whenMissingOrNull;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return whenBadParse;
		}
	}

	private static boolean isUserIdFalsyForGetCartList(Object uid) {
		if (uid == null) {
			return true;
		}
		if (uid instanceof Number n) {
			return n.longValue() <= 0L;
		}
		if (uid instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			try {
				return Long.parseLong(t) <= 0L;
			} catch (NumberFormatException e) {
				return true;
			}
		}
		return longVal(uid) <= 0L;
	}

	private static long resolveShopIdFilterForGetCart(Map<String, Object> query) {
		Object primary = query.get("shop_id");
		if (isUnsetShopIdPrimary(primary)) {
			primary = query.get("distributor_id");
		}
		return normalizeShopIdFilterForCart(primary);
	}

	private static boolean isUnsetShopIdPrimary(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Boolean b) {
			return !b;
		}
		if (o instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (o instanceof String s) {
			return s.trim().isEmpty();
		}
		return String.valueOf(o).trim().isEmpty();
	}

	private static long normalizeShopIdFilterForCart(Object raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.toString().trim();
		if (!StringUtils.hasText(t) || "undefined".equals(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static long parseMarketingIdStrictLong(Object raw) {
		if (raw == null) {
			throw new ResourceException("商品促销id错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t) || "0".equals(t)) {
				throw new ResourceException("商品促销id错误");
			}
			try {
				long v = Long.parseLong(t);
				if (v <= 0L) {
					throw new ResourceException("商品促销id错误");
				}
				return v;
			} catch (NumberFormatException e) {
				throw new ResourceException("商品促销id错误");
			}
		}
		if (raw instanceof Number n) {
			if (n.longValue() <= 0L) {
				throw new ResourceException("商品促销id错误");
			}
			return n.longValue();
		}
		String t = String.valueOf(raw).trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			throw new ResourceException("商品促销id错误");
		}
		try {
			long v = Long.parseLong(t);
			if (v <= 0L) {
				throw new ResourceException("商品促销id错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("商品促销id错误");
		}
	}

	private static long parseCartIdStrictLong(Object raw) {
		if (raw == null) {
			throw new ResourceException("购物车参数错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t) || "0".equals(t)) {
				throw new ResourceException("购物车参数错误");
			}
			try {
				long v = Long.parseLong(t);
				if (v <= 0L) {
					throw new ResourceException("购物车参数错误");
				}
				return v;
			} catch (NumberFormatException e) {
				throw new ResourceException("购物车参数错误");
			}
		}
		if (raw instanceof Number n) {
			if (n.longValue() <= 0L) {
				throw new ResourceException("购物车参数错误");
			}
			return n.longValue();
		}
		String t = String.valueOf(raw).trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			throw new ResourceException("购物车参数错误");
		}
		try {
			long v = Long.parseLong(t);
			if (v <= 0L) {
				throw new ResourceException("购物车参数错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("购物车参数错误");
		}
	}

	private static boolean resolveIsCheckedLoose(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "false".equals(t) || "0".equals(t)) {
				return false;
			}
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		String t = String.valueOf(raw).trim();
		if (t.isEmpty() || "false".equals(t) || "0".equals(t)) {
			return false;
		}
		return true;
	}
}
