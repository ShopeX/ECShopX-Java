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

package cn.shopex.ecshopx.goods.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.goods.service.cart.WxappScanCodeAddCartService;
import cn.shopex.ecshopx.goods.service.items.ItemsUserItemShareCheckService;
import cn.shopex.ecshopx.goods.service.items.WxappItemIntroService;
import cn.shopex.ecshopx.goods.service.items.WxappItemsFavStatusService;
import cn.shopex.ecshopx.goods.service.keywords.GoodsKeywordsQueryService;
import cn.shopex.ecshopx.goods.service.memberprice.MemberPriceListQueryService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsBatchService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsDetailOrchestratorService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsPriceAndStoreService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsFilterRequestParser;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemShareInfoService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsFilterService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappShopItemsListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("goodsFrontV1Items")
@RequestMapping("/api/v1/h5app/wxapp/goods")
public class ItemsController {

	private final WxappScanCodeAddCartService wxappScanCodeAddCartService;
	private final ItemsUserItemShareCheckService itemsUserItemShareCheckService;
	private final WxappItemIntroService wxappItemIntroService;
	private final WxappGoodsItemsListService wxappGoodsItemsListService;
	private final WxappShopItemsListService wxappShopItemsListService;
	private final WxappGoodsItemsFilterRequestParser wxappGoodsItemsFilterRequestParser;
	private final WxappGoodsItemsFilterService wxappGoodsItemsFilterService;
	private final GoodsKeywordsQueryService goodsKeywordsQueryService;
	private final WxappGoodsItemsBatchService wxappGoodsItemsBatchService;
	private final WxappGoodsItemsDetailOrchestratorService wxappGoodsItemsDetailOrchestratorService;
	private final WxappGoodsItemsPriceAndStoreService wxappGoodsItemsPriceAndStoreService;
	private final WxappItemsFavStatusService wxappItemsFavStatusService;
	private final MemberPriceListQueryService memberPriceListQueryService;
	private final WxappGoodsItemShareInfoService wxappGoodsItemShareInfoService;
	private final LangueProperties langueProperties;

	public ItemsController(
			WxappScanCodeAddCartService wxappScanCodeAddCartService,
			ItemsUserItemShareCheckService itemsUserItemShareCheckService,
			WxappItemIntroService wxappItemIntroService,
			WxappGoodsItemsListService wxappGoodsItemsListService,
			WxappShopItemsListService wxappShopItemsListService,
			WxappGoodsItemsFilterRequestParser wxappGoodsItemsFilterRequestParser,
			WxappGoodsItemsFilterService wxappGoodsItemsFilterService,
			GoodsKeywordsQueryService goodsKeywordsQueryService,
			WxappGoodsItemsBatchService wxappGoodsItemsBatchService,
			WxappGoodsItemsDetailOrchestratorService wxappGoodsItemsDetailOrchestratorService,
			WxappGoodsItemsPriceAndStoreService wxappGoodsItemsPriceAndStoreService,
			WxappItemsFavStatusService wxappItemsFavStatusService,
			MemberPriceListQueryService memberPriceListQueryService,
			WxappGoodsItemShareInfoService wxappGoodsItemShareInfoService,
			LangueProperties langueProperties) {
		this.wxappScanCodeAddCartService = wxappScanCodeAddCartService;
		this.itemsUserItemShareCheckService = itemsUserItemShareCheckService;
		this.wxappItemIntroService = wxappItemIntroService;
		this.wxappGoodsItemsListService = wxappGoodsItemsListService;
		this.wxappShopItemsListService = wxappShopItemsListService;
		this.wxappGoodsItemsFilterRequestParser = wxappGoodsItemsFilterRequestParser;
		this.wxappGoodsItemsFilterService = wxappGoodsItemsFilterService;
		this.goodsKeywordsQueryService = goodsKeywordsQueryService;
		this.wxappGoodsItemsBatchService = wxappGoodsItemsBatchService;
		this.wxappGoodsItemsDetailOrchestratorService = wxappGoodsItemsDetailOrchestratorService;
		this.wxappGoodsItemsPriceAndStoreService = wxappGoodsItemsPriceAndStoreService;
		this.wxappItemsFavStatusService = wxappItemsFavStatusService;
		this.memberPriceListQueryService = memberPriceListQueryService;
		this.wxappGoodsItemShareInfoService = wxappGoodsItemShareInfoService;
		this.langueProperties = langueProperties;
	}

	@GetMapping(value = "/items", name = "商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsList(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		Map<String, Object> data = wxappGoodsItemsListService.execute(companyId, userId, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/items/filter", name = "商品筛选条件")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsFilter(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> filterMap = wxappGoodsItemsFilterRequestParser.parseFilter(request);
		Map<String, Object> data = wxappGoodsItemsFilterService.buildFilterResult(companyId, filterMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/shopitems", name = "小店商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getShopItemsList(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		Map<String, Object> data = wxappShopItemsListService.execute(companyId, userId, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(
			value = "/itemintro/{item_id}",
			name = "商品文描",
			produces = "text/html;charset=UTF-8")
	public ResponseEntity<String> getItemsIntro(
			@PathVariable("item_id") String itemIdStr, HttpServletRequest request) {
		long itemId = parsePathItemIdForWxappIntro(itemIdStr);
		requireWxappIntroAccessContextOrUnauthorized(request);
		long companyId = parseCompanyIdFromRequest(request);
		String authorizerAppId = parseOptionalWoaAppidFromRequest(request);
		String body = wxappItemIntroService.buildIntroBody(companyId, itemId, authorizerAppId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
				.body(body);
	}

	@GetMapping(value = "/memberprice/{item_id}", name = "商品会员价")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMemberPriceList(
			@PathVariable("item_id") String itemIdStr,
			HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long itemId = parsePathItemIdForMemberPrice(itemIdStr);
		Map<String, Object> data =
				memberPriceListQueryService.getMemberPriceList(companyId, itemId, RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/keywords", name = "店铺热门关键词")
	public ResponseEntity<ApiResult<Map<String, Object>>> getKeywords(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long distributorId = parseOptionalIntegerDistributorIdForKeywords(request);
		Map<String, Object> data = goodsKeywordsQueryService.listByShopForWxapp(companyId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/items/{item_id}/fav", name = "商品收藏")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsFav(
			@PathVariable("item_id") String itemIdStr, HttpServletRequest request) {
		long userId = parseRequiredUserIdFromRequest(request);
		Long parsed = parsePathItemIdSoft(itemIdStr);
		int fav = parsed == null ? 0 : wxappItemsFavStatusService.isFavorited(userId, parsed.longValue());
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("fav", fav);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/checkshare/items", name = "检查是否可分享")
	public ResponseEntity<ApiResult<Map<String, Object>>> checkShare(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseRequiredUserIdFromRequest(request);
		Map<String, Object> data = itemsUserItemShareCheckService.checkUserItemShare(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/share/items/{item_id}", name = "分享商品信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getShareInfo(
			@PathVariable("item_id") String itemIdStr, HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		Long parsed = parsePathItemIdSoft(itemIdStr);
		if (parsed == null || parsed < 1L) {
			throw new ResourceException("商品不存在或者已下架");
		}
		Map<String, Object> data = wxappGoodsItemShareInfoService.buildSharePayload(companyId, parsed);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/items/batch", name = "批量获取商品基本信息")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getBatchItems(
			@RequestParam(value = "item_ids", required = false, defaultValue = "") String itemIdsStr,
			HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		String countryCode = RequestLangTag.current(langueProperties);
		List<Map<String, Object>> data = wxappGoodsItemsBatchService.execute(companyId, itemIdsStr, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/items/{item_id}", name = "商品详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsDetail(@PathVariable("item_id") String itemIdStr, HttpServletRequest request,
			@RequestParam(value = "view_mode", required = false) String viewMode, @RequestParam(value = "goods_id", required = false) Long goodsId,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "isShopScreen", required = false) String isShopScreenRaw,
			@RequestParam(value = "is_tdk", required = false) String isTdkRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		String authorizerAppId = parseOptionalWoaAppidFromRequest(request);
		Long pathItemParsed = parsePathItemIdSoft(itemIdStr);
		boolean goodsIdOverride = goodsId != null && goodsId > 0L;
		if (!goodsIdOverride && (pathItemParsed == null || pathItemParsed < 1L)) {
			throw new ResourceException("商品不存在或下架");
		}
		long pathItem = pathItemParsed != null ? pathItemParsed : 0L;
		long distributorId = parseDistributorIdRaw(distributorIdRaw);
		boolean needTdk = "1".equals(isTdkRaw == null ? "" : isTdkRaw.trim());
		Map<String, Object> data = wxappGoodsItemsDetailOrchestratorService.execute(request, pathItem, companyId, userId, authorizerAppId, viewMode, goodsId,
				distributorId, needTdk);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/newitems", name = "商品详情新")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsDetailNew(
			@RequestParam(value = "item_id", required = false) String itemIdStr,
			HttpServletRequest request,
			@RequestParam(value = "view_mode", required = false) String viewMode,
			@RequestParam(value = "goods_id", required = false) Long goodsId,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "isShopScreen", required = false) String isShopScreenRaw,
			@RequestParam(value = "is_tdk", required = false) String isTdkRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		String authorizerAppId = parseOptionalWoaAppidFromRequest(request);
		Long pathItemParsed = parsePathItemIdSoft(itemIdStr);
		boolean goodsIdOverride = goodsId != null && goodsId > 0L;
		if (!goodsIdOverride && (pathItemParsed == null || pathItemParsed < 1L)) {
			throw new ResourceException("商品不存在或下架");
		}
		long pathItem = pathItemParsed != null ? pathItemParsed : 0L;
		long distributorId = parseDistributorIdRaw(distributorIdRaw);
		boolean needTdk = "1".equals(isTdkRaw == null ? "" : isTdkRaw.trim());
		Map<String, Object> data = wxappGoodsItemsDetailOrchestratorService.execute(request, pathItem, companyId, userId, authorizerAppId, viewMode, goodsId,
				distributorId, needTdk);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/items_price_store/{item_id}", name = "商品价格库存")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsPriceAndStore(
			@PathVariable("item_id") String itemIdStr,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		String authorizerAppId = parseOptionalWoaAppidFromRequest(request);
		long distributorId = parseDistributorIdRaw(distributorIdRaw);
		Map<String, Object> body =
				wxappGoodsItemsPriceAndStoreService.execute(request, itemIdStr, companyId, userId, authorizerAppId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@PostMapping(value = "/scancodeAddcart", name = "扫码加购")
	public ResponseEntity<ApiResult<Map<String, Object>>> scanCodeSales(
			HttpServletRequest request,
			@RequestParam(required = false) String barcode,
			@RequestParam(required = false) String distributor_id,
			@RequestParam(required = false) String isShopScreen,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		Map<String, Object> merged = mergeScancodeParams(request, barcode, distributor_id, isShopScreen, body);
		Map<String, Object> data = wxappScanCodeAddCartService.execute(companyId, userId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> mergeScancodeParams(
			HttpServletRequest request,
			String barcodeParam,
			String distributorIdParam,
			String isShopScreenParam,
			Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		if (barcodeParam != null) {
			merged.put("barcode", barcodeParam);
		}
		if (distributorIdParam != null) {
			merged.put("distributor_id", distributorIdParam);
		}
		if (isShopScreenParam != null) {
			merged.put("isShopScreen", isShopScreenParam);
		}
		return merged;
	}

	/**
	 * 文描接口：匿名访问时须具备可识别的租户来源（登录态、显式 company_id、小程序 appid 或浏览器 Origin/Referer 之一）。
	 */
	private static void requireWxappIntroAccessContextOrUnauthorized(HttpServletRequest request) {
		Object claims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (claims instanceof Map<?, ?> m && !m.isEmpty()) {
			return;
		}
		if (StringUtils.hasText(request.getParameter("company_id"))) {
			return;
		}
		if (StringUtils.hasText(request.getParameter("appid"))) {
			return;
		}
		if (StringUtils.hasText(request.getHeader("authorizer-appid"))) {
			return;
		}
		if (StringUtils.hasText(request.getHeader("Origin")) || StringUtils.hasText(request.getHeader("Referer"))) {
			return;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	private static long parsePathItemIdForMemberPrice(String itemIdStr) {
		String t = itemIdStr == null ? "" : itemIdStr.trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException(
					"获取会员价详情出错.", Map.of("item_id", List.of("validation.required")), 422);
		}
		try {
			long itemId = Long.parseLong(t);
			if (itemId < 1L) {
				throw new BadRequestException(
						"获取会员价详情出错.", Map.of("item_id", List.of("validation.min.numeric")), 422);
			}
			return itemId;
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					"获取会员价详情出错.", Map.of("item_id", List.of("validation.integer")), 422);
		}
	}

	private static long parsePathItemIdForWxappIntro(String itemIdStr) {
		String t = itemIdStr == null ? "" : itemIdStr.trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException(
					"商品ID无效", Map.of("item_id", List.of("validation.required")), 422);
		}
		try {
			long itemId = Long.parseLong(t);
			if (itemId < 1L) {
				throw new BadRequestException(
						"商品ID无效", Map.of("item_id", List.of("validation.min.numeric")), 422);
			}
			return itemId;
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					"商品ID无效", Map.of("item_id", List.of("validation.integer")), 422);
		}
	}

	@SuppressWarnings("unchecked")
	private static String parseOptionalWoaAppidFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return "";
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object woa = claims.get("woa_appid");
		if (woa == null) {
			return "";
		}
		String s = woa.toString().trim();
		return s;
	}

	private static Long parsePathItemIdSoft(String itemIdStr) {
		if (itemIdStr == null) {
			return null;
		}
		String t = itemIdStr.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseDistributorIdRaw(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if ("undefined".equals(t) || "null".equals(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	@SuppressWarnings("unchecked")
	private static long parseOptionalUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return 0L;
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			return 0L;
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@SuppressWarnings("unchecked")
	private static long parseRequiredUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return v;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static long parseOptionalIntegerDistributorIdForKeywords(HttpServletRequest request) {
		String raw = request.getParameter("distributor_id");
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		String t = raw.trim();
		if (t.contains(".")) {
			throw new BadRequestException(
					"参数错误.",
					Map.of("distributor_id", List.of("validation.integer")),
					422);
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					"参数错误.",
					Map.of("distributor_id", List.of("validation.integer")),
					422);
		}
	}
}
