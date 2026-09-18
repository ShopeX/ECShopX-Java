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

package cn.shopex.ecshopx.popularize.api.front.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.popularize.PromoterSalesmanStoreitemsQueryPort;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsBaseInfoService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import cn.shopex.ecshopx.popularize.service.PromoterChangePromoterService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontChildrenPromoterListService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontGetPromoterGoodsService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontGetPromoterInfoService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontGetPromoterQrcodeService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontNewQrcodePngService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontQrcodePngService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontIndexCountService;
import cn.shopex.ecshopx.popularize.service.PromoterFrontPromoterChildrenListService;
import cn.shopex.ecshopx.popularize.service.PromoterRelGoodsService;
import cn.shopex.ecshopx.popularize.service.PromoterSalesmanCountQueryService;
import cn.shopex.ecshopx.popularize.service.PromoterSalesmanStaticQueryService;
import cn.shopex.ecshopx.popularize.service.PromoterUpdateInfoService;
import cn.shopex.ecshopx.popularize.support.PopularizeQueryFlagTruthy;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("popularizeFrontV1Promoter")
@RequestMapping("/api/v1/h5app/wxapp")
public class PromoterController {

	private static final Logger log = LoggerFactory.getLogger(PromoterController.class);

	/**
	 * Tenant id for the second baseline read when the first read’s {@code banner_img} is empty or
	 * whitespace-only and the resolved {@code company_id} is not this id.
	 */
	private static final long DEFAULT_FALLBACK_PROMOTER_BANNER_COMPANY_ID = 1L;

	/**
	 * Records how {@code company_id} was resolved for a single request (request-scoped attribute).
	 */
	private static final class FrontNoAuthCompanyContext {

		private static final String REQUEST_ATTR_SOURCE =
				FrontNoAuthCompanyContext.class.getName() + ".SOURCE";

		enum Source {
			NONE,
			ATTRIBUTE,
			JWT_CLAIMS,
			QUERY_ONLY
		}

		static void setSource(HttpServletRequest request, Source source) {
			request.setAttribute(REQUEST_ATTR_SOURCE, source);
		}

		static Source getSource(HttpServletRequest request) {
			Object raw = request.getAttribute(REQUEST_ATTR_SOURCE);
			return raw instanceof Source s ? s : Source.NONE;
		}

		private FrontNoAuthCompanyContext() {}
	}

	private final PromoterChangePromoterService promoterChangePromoterService;
	private final PromoterRelGoodsService promoterRelGoodsService;
	private final PromoterUpdateInfoService promoterUpdateInfoService;
	private final PromoterFrontChildrenPromoterListService promoterFrontChildrenPromoterListService;
	private final PromoterFrontPromoterChildrenListService promoterFrontPromoterChildrenListService;
	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final MemberAccountService memberAccountService;
	private final PromoterSalesmanCountQueryService promoterSalesmanCountQueryService;
	private final PromoterSalesmanStaticQueryService promoterSalesmanStaticQueryService;
	private final PromoterSalesmanStoreitemsQueryPort promoterSalesmanStoreitemsQueryPort;
	private final PromoterFrontIndexCountService promoterFrontIndexCountService;
	private final PromoterFrontGetPromoterInfoService promoterFrontGetPromoterInfoService;
	private final PromoterFrontGetPromoterGoodsService promoterFrontGetPromoterGoodsService;
	private final PromoterFrontNewQrcodePngService promoterFrontNewQrcodePngService;
	private final PromoterFrontGetPromoterQrcodeService promoterFrontGetPromoterQrcodeService;
	private final PromoterFrontQrcodePngService promoterFrontQrcodePngService;
	private final LangueProperties langueProperties;
	private final boolean oemShuyun;

	public PromoterController(
			PromoterChangePromoterService promoterChangePromoterService,
			PromoterRelGoodsService promoterRelGoodsService,
			PromoterUpdateInfoService promoterUpdateInfoService,
			PromoterFrontChildrenPromoterListService promoterFrontChildrenPromoterListService,
			PromoterFrontPromoterChildrenListService promoterFrontPromoterChildrenListService,
			PopularizeSettingSaveService popularizeSettingSaveService,
			MemberAccountService memberAccountService,
			PromoterSalesmanCountQueryService promoterSalesmanCountQueryService,
			PromoterSalesmanStaticQueryService promoterSalesmanStaticQueryService,
			PromoterSalesmanStoreitemsQueryPort promoterSalesmanStoreitemsQueryPort,
			PromoterFrontIndexCountService promoterFrontIndexCountService,
			PromoterFrontGetPromoterInfoService promoterFrontGetPromoterInfoService,
			PromoterFrontGetPromoterGoodsService promoterFrontGetPromoterGoodsService,
			PromoterFrontNewQrcodePngService promoterFrontNewQrcodePngService,
			PromoterFrontGetPromoterQrcodeService promoterFrontGetPromoterQrcodeService,
			PromoterFrontQrcodePngService promoterFrontQrcodePngService,
			LangueProperties langueProperties,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterChangePromoterService = promoterChangePromoterService;
		this.promoterRelGoodsService = promoterRelGoodsService;
		this.promoterUpdateInfoService = promoterUpdateInfoService;
		this.promoterFrontChildrenPromoterListService = promoterFrontChildrenPromoterListService;
		this.promoterFrontPromoterChildrenListService = promoterFrontPromoterChildrenListService;
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.memberAccountService = memberAccountService;
		this.promoterSalesmanCountQueryService = promoterSalesmanCountQueryService;
		this.promoterSalesmanStaticQueryService = promoterSalesmanStaticQueryService;
		this.promoterSalesmanStoreitemsQueryPort = promoterSalesmanStoreitemsQueryPort;
		this.promoterFrontIndexCountService = promoterFrontIndexCountService;
		this.promoterFrontGetPromoterInfoService = promoterFrontGetPromoterInfoService;
		this.promoterFrontGetPromoterGoodsService = promoterFrontGetPromoterGoodsService;
		this.promoterFrontNewQrcodePngService = promoterFrontNewQrcodePngService;
		this.promoterFrontGetPromoterQrcodeService = promoterFrontGetPromoterQrcodeService;
		this.promoterFrontQrcodePngService = promoterFrontQrcodePngService;
		this.langueProperties = langueProperties;
		this.oemShuyun = oemShuyun;
	}

	@PostMapping(value = "/promoter", name = "成为推广员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, String>>> changePromoter(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		promoterChangePromoterService.changePromoter(companyId, userId, false, null);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", "true")));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long resolveCompanyId(HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		throw new UnauthorizedException("Unable to authenticate user.");
	}

	private static long resolveCompanyIdForFrontNoAuth(HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			FrontNoAuthCompanyContext.setSource(request, FrontNoAuthCompanyContext.Source.ATTRIBUTE);
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			FrontNoAuthCompanyContext.setSource(request, FrontNoAuthCompanyContext.Source.JWT_CLAIMS);
			return fromClaims;
		}
		long fromQuery = parsePositiveLongOrZero(
				FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
		if (fromQuery > 0L) {
			FrontNoAuthCompanyContext.setSource(request, FrontNoAuthCompanyContext.Source.QUERY_ONLY);
			return fromQuery;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	private static boolean hasNonEmptyBannerImg(Map<String, Object> data) {
		if (data == null) {
			return false;
		}
		Object raw = data.get("banner_img");
		if (raw == null) {
			return false;
		}
		return StringUtils.hasText(String.valueOf(raw));
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@PutMapping(value = "/promoter", name = "更新推广员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> updatePromoterInfo(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		Map<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			input.putAll(body);
		}
		Map<String, Object> payload = promoterUpdateInfoService.updatePromoterInfo(companyId, userId, input);
		if (payload.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@GetMapping(value = "/promoter/index", name = "推广员首页", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<LinkedHashMap<String, Object>>> indexCount(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		String isSalesmanPageRaw = request.getParameter("isSalesmanPage");
		String distributorIdRaw = request.getParameter("distributor_id");
		LinkedHashMap<String, Object> data =
				promoterFrontIndexCountService.indexCount(companyId, userId, isSalesmanPageRaw, distributorIdRaw);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	@SuppressWarnings("unused")
	@GetMapping(value = "/promoter/children", name = "下级列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromoterchildrenList(
			HttpServletRequest request,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "buy_type", required = false) String buyType,
			@RequestParam(value = "userName", required = false) String userName,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "shopName", required = false) String shopName) {
		// userName / mobile / shopName: accepted on the query string but not applied as list filters here.
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}

		int pageSize = parsePageOrPageSize(pageSizeRaw, 20);
		int buyPage;
		int notBuyPage;
		if ("buy".equals(buyType)) {
			buyPage = parsePageOrPageSize(pageRaw, 1);
			notBuyPage = 1;
		} else if ("not_buy".equals(buyType)) {
			notBuyPage = parsePageOrPageSize(pageRaw, 1);
			buyPage = 1;
		} else {
			buyPage = 1;
			notBuyPage = 1;
		}

		String isBuyRaw = request.getParameter("isBuy");
		boolean isBuyTruthy = isBuyTruthyDefaultOneWhenAbsent(isBuyRaw);

		LinkedHashMap<String, Object> filterBuy = new LinkedHashMap<>();
		filterBuy.put("company_id", companyId);
		filterBuy.put("user_id", userId);
		if (oemShuyun) {
			filterBuy.put("is_promoter", 0);
		}
		filterBuy.put("disabled", 0);
		filterBuy.put("is_buy", 1);
		Map<String, Object> buyData =
				promoterFrontPromoterChildrenListService.getPromoterchildrenList(filterBuy, buyPage, pageSize);

		LinkedHashMap<String, Object> filterNotBuy = new LinkedHashMap<>();
		filterNotBuy.put("company_id", companyId);
		filterNotBuy.put("user_id", userId);
		if (oemShuyun) {
			filterNotBuy.put("is_promoter", 0);
		}
		filterNotBuy.put("disabled", 0);
		filterNotBuy.put("is_buy", 0);
		Map<String, Object> notBuyData =
				promoterFrontPromoterChildrenListService.getPromoterchildrenList(
						filterNotBuy, notBuyPage, pageSize);

		applyChildrenPromoterListSensitiveMasking(buyData);
		applyChildrenPromoterListSensitiveMasking(notBuyData);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("buy", buyData);
		data.put("not_buy", notBuyData);

		if (isBuyTruthy) {
			@SuppressWarnings("unchecked")
			Map<String, Object> buyMap = (Map<String, Object>) data.get("buy");
			@SuppressWarnings("unchecked")
			Map<String, Object> notBuyMap = (Map<String, Object>) data.get("not_buy");
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>(data);
			LinkedHashMap<String, Object> all = new LinkedHashMap<>(buyMap);
			all.putAll(notBuyMap);
			payload.put("all", all);
			return ResponseEntity.ok(ApiResult.ok(payload));
		}
		if ("buy".equals(buyType)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> buyOnly = (Map<String, Object>) data.get("buy");
			return ResponseEntity.ok(ApiResult.ok(buyOnly));
		}
		if ("not_buy".equals(buyType)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> notBuyOnly = (Map<String, Object>) data.get("not_buy");
			return ResponseEntity.ok(ApiResult.ok(notBuyOnly));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Resolves whether the {@code isBuy} request parameter should be treated as “active” for response shaping.
	 * Values come from {@link HttpServletRequest#getParameter(String)} (no Spring {@code defaultValue} is applied).
	 * <ul>
	 *   <li>{@code null} when the parameter is omitted → {@code true} (same end result as if the value were
	 *       default {@code "1"})</li>
	 *   <li>After {@link String#trim()}, an empty string → {@code false}</li>
	 *   <li>After trim, exactly {@code "0"} → {@code false}</li>
	 *   <li>Any other non-empty string (including other digits or non-numeric text) → {@code true}</li>
	 * </ul>
	 */
	private static boolean isBuyTruthyDefaultOneWhenAbsent(String rawFromRequest) {
		if (rawFromRequest == null) {
			return true;
		}
		String t = rawFromRequest.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equals(t);
	}

	@GetMapping(value = "/childrenpromoter/list", name = "下级推广员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getChildrenpromoterList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "promoter_mobile", required = false) String promoterMobile,
			@RequestParam(value = "buy_type", required = false) String buyType) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}

		int page = parsePageOrPageSize(pageRaw, 1);
		int pageSize = parsePageOrPageSize(pageSizeRaw, 20);

		Optional<Long> promoterFilter;
		if (!StringUtils.hasText(promoterMobile)) {
			promoterFilter = Optional.empty();
		} else {
			Members m = memberAccountService.findMemberByCompanyAndMobile(companyId, promoterMobile.trim());
			if (m == null || m.getUserId() == null || m.getUserId() <= 0L) {
				promoterFilter = Optional.of(-1L);
			} else {
				promoterFilter = Optional.of(m.getUserId());
			}
		}

		Map<String, Object> data =
				promoterFrontChildrenPromoterListService.getChildrenpromoterList(
						companyId, userId, promoterFilter, page, pageSize);
		applyChildrenPromoterListSensitiveMasking(data);

		if ("buy".equals(buyType) || "not_buy".equals(buyType)) {
			return ResponseEntity.ok(ApiResult.ok(null));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePageOrPageSize(String raw, int defaultValue) {
		if (!StringUtils.hasText(raw)) {
			return defaultValue;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(t);
			if (v < 1) {
				throw new BadRequestException("参数错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
	}

	@SuppressWarnings("unchecked")
	private void applyChildrenPromoterListSensitiveMasking(Map<String, Object> data) {
		Object listObj = data.get("list");
		if (!(listObj instanceof List<?> rawList)) {
			return;
		}
		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> rawMap)) {
				continue;
			}
			Map<String, Object> row = (Map<String, Object>) rawMap;
			Object username = row.get("username");
			if (username != null && StringUtils.hasText(String.valueOf(username))) {
				row.put(
						"username",
						DataMasking.maskTruenameIfBlocked(String.valueOf(username), 1));
			}
			Object nickname = row.get("nickname");
			if (nickname != null && StringUtils.hasText(String.valueOf(nickname))) {
				row.put(
						"nickname",
						DataMasking.maskTruenameIfBlocked(String.valueOf(nickname), 1));
			}
			Object regionMobile = row.get("region_mobile");
			if (regionMobile != null && StringUtils.hasText(String.valueOf(regionMobile))) {
				row.put("region_mobile", DataMasking.maskMobile(String.valueOf(regionMobile)));
			}
			Object pmobile = row.get("pmobile");
			if (pmobile != null && StringUtils.hasText(String.valueOf(pmobile))) {
				row.put("pmobile", DataMasking.maskMobile(String.valueOf(pmobile)));
			}
			Object mobile = row.get("mobile");
			if (mobile != null && StringUtils.hasText(String.valueOf(mobile))) {
				row.put("mobile", DataMasking.maskMobile(String.valueOf(mobile)));
			}
		}
	}

	@GetMapping(value = "/promoter/qrcode", name = "推广员码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, String>>> getPromoterQrcode(
			HttpServletRequest request,
			@RequestParam(value = "path", required = false, defaultValue = "pages/index") String path) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		Map<String, String> data = promoterFrontGetPromoterQrcodeService.getPromoterQrcode(companyId, claims, path);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	@PostMapping(value = "/promoter/relgoods", name = "关联商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> relPromoterGoods(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		Map<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			input.putAll(body);
		}
		Object goodsIdRaw = input.get("goods_id");
		promoterRelGoodsService.relPromoterGoods(companyId, userId, goodsIdRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@DeleteMapping(value = "/promoter/relgoods", name = "删关联商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteRelPromoterGoods(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		Map<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			input.putAll(body);
		}
		Object goodsIdRaw = input.get("goods_id");
		promoterRelGoodsService.deleteRelPromoterGoods(companyId, userId, goodsIdRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@GetMapping(value = "/promoter/getSalesmanCount", name = "业务员统计", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSalesmanCount(
			HttpServletRequest request,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "datetype", required = false) String datetype,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		Map<String, Object> data =
				promoterSalesmanCountQueryService.getSalesmanCount(userId, date, datetype, distributorId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	@GetMapping(value = "/promoter/getSalesmanStatic", name = "业务员业绩", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<LinkedHashMap<String, Object>>>> getSalesmanStatic(
			HttpServletRequest request,
			@RequestParam(value = "tab", required = false) String tab,
			@RequestParam(value = "datetype", required = false) String datetype,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		List<LinkedHashMap<String, Object>> data =
				promoterSalesmanStaticQueryService.getSalesmanStatic(userId, tab, datetype, date, distributorId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	@GetMapping(
			value = "/promoter/getSalesmanStoreitems",
			name = "推广店铺商品",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getSalesmanStoreitems(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "is_default", required = false) String isDefaultRaw,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		int page = parsePositiveIntDefault(pageRaw, 1);
		int pageSize = parsePositiveIntDefault(pageSizeRaw, 50);
		Object data =
				promoterSalesmanStoreitemsQueryPort.getSalesmanStoreitems(
						companyId,
						userId,
						page,
						pageSize,
						isDefaultRaw,
						distributorId,
						RequestLangTag.current(langueProperties));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(data));
	}

	private static int parsePositiveIntDefault(String raw, int defaultValue) {
		if (!StringUtils.hasText(raw)) {
			return defaultValue;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(t);
			if (v <= 0) {
				return defaultValue;
			}
			return v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@FrontNoAuth
	@GetMapping(value = "/promoter/info", name = "推广员信息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getPromoterInfo(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		long selfId = parsePositiveLongOrZero(claims.get("user_id"));
		String userIdParam = request.getParameter("user_id");
		String country = WxShopsBaseInfoService.resolveCountryCodeForWxSetting(request);
		Object body =
				promoterFrontGetPromoterInfoService.getPromoterInfo(
						companyId, selfId, userIdParam, claims, country);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(body));
	}

	@FrontNoAuth
	@GetMapping(value = "/promoter/relgoods", name = "关联商品列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromoterGoods(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "user_id", required = false) String userId,
			@RequestParam(value = "goods_id", required = false) String goodsId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		long filterUserId;
		if (PopularizeQueryFlagTruthy.isTruthyForH5QueryFlag(userId)) {
			String t = userId == null ? "" : userId.strip();
			long parsedFromParam;
			try {
				parsedFromParam = Long.parseLong(t);
			} catch (NumberFormatException e) {
				parsedFromParam = 0L;
			}
			if (parsedFromParam > 0L) {
				filterUserId = parsedFromParam;
			} else {
				filterUserId = parsePositiveLongOrZero(claims.get("user_id"));
			}
		} else {
			filterUserId = parsePositiveLongOrZero(claims.get("user_id"));
		}
		Long goodsIdLongOrNull = parsePositiveGoodsIdOrNull(goodsId);
		int pageInt = parsePositiveIntDefault(page, 1);
		int pageSizeInt = parsePositiveIntDefault(pageSize, 20);
		Map<String, Object> cfg = popularizeSettingSaveService.getMergedPopularizeConfig(companyId);
		Object goodsRaw = cfg.get("goods");
		List<Long> ids =
				promoterFrontGetPromoterGoodsService.getPromoterGoods(
						companyId, filterUserId, goodsIdLongOrNull, pageInt, pageSizeInt, goodsRaw);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("goods_id", ids);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(body));
	}

	private static Long parsePositiveGoodsIdOrNull(String goodsId) {
		if (!StringUtils.hasText(goodsId)) {
			return null;
		}
		String t = goodsId.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			if (v > 0L) {
				return v;
			}
		} catch (NumberFormatException ignored) {
			return null;
		}
		return null;
	}

	@FrontNoAuth
	@GetMapping(value = "/promoter/banner", name = "店招封面", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getPromoterBanner(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		Map<String, Object> data = popularizeSettingSaveService.getPromoterBanner(companyId);
		if (!hasNonEmptyBannerImg(data)
				&& companyId != DEFAULT_FALLBACK_PROMOTER_BANNER_COMPANY_ID) {
			Map<String, Object> fallback =
					popularizeSettingSaveService.getPromoterBanner(DEFAULT_FALLBACK_PROMOTER_BANNER_COMPANY_ID);
			data = new LinkedHashMap<>(data);
			Object fb = fallback.get("banner_img");
			data.put("banner_img", fb == null ? "" : String.valueOf(fb));
		}
		return ApiResult.ok(data);
	}

	@FrontNoAuth
	@GetMapping(value = "/promoter/custompage", name = "虚拟店首页", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getPromoterCustompage(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		Map<String, Object> data = popularizeSettingSaveService.getPromoterCustompage(companyId);
		return ApiResult.ok(data);
	}

	@FrontNoAuth
	@PostMapping(value = "/promoter/qrcode/log", name = "推广码日志", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> logPromoterQrcode(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> inputData = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			inputData.putAll(body);
		}
		log.debug("推荐关系跟踪 inputData：{}", inputData);
		Map<String, Object> payload = Map.of("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@FrontNoAuth
	@GetMapping(value = "/promoter/qrcode.png", name = "推广码PNG", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getPromoterQrcodePng(HttpServletRequest request) {
		Object rawCompany = FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id");
		if (rawCompany != null
				&& StringUtils.hasText(String.valueOf(rawCompany).trim())
				&& parsePositiveLongOrZero(rawCompany) == 0L) {
			throw new BadRequestException("非法的 company_id");
		}
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		String appidQuery = request.getParameter("appid");
		String uidRaw = request.getParameter("uid");
		String userIdRaw = request.getParameter("user_id");
		boolean dtidPresent = request.getParameterMap().containsKey("dtid");
		String dtidRaw = request.getParameter("dtid");
		boolean qrPresent = request.getParameterMap().containsKey("qr");
		String qrRaw = request.getParameter("qr");
		String prescriptionOrderId = Objects.toString(request.getParameter("prescription_order_id"), "");
		String pathRaw = request.getParameter("path");
		boolean pageOverridePresent = request.getParameterMap().containsKey("page");
		String pageOverrideRaw = request.getParameter("page");
		boolean orderIdPresent = request.getParameterMap().containsKey("order_id");
		String orderIdRaw = request.getParameter("order_id");
		byte[] png =
				promoterFrontQrcodePngService.getPromoterQrcodePng(
						companyId,
						appidQuery,
						uidRaw,
						userIdRaw,
						dtidRaw,
						dtidPresent,
						qrRaw,
						qrPresent,
						prescriptionOrderId,
						pathRaw,
						pageOverrideRaw,
						pageOverridePresent,
						orderIdRaw,
						orderIdPresent);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.body(png);
	}

	@FrontNoAuth
	@GetMapping(
			value = "/promoter/new_qrcode.png",
			name = "邀请码PNG",
			produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getPromoterNewQrcodePng(HttpServletRequest request) {
		Object rawCompany = FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id");
		if (rawCompany != null
				&& StringUtils.hasText(String.valueOf(rawCompany).trim())
				&& parsePositiveLongOrZero(rawCompany) == 0L) {
			throw new BadRequestException("非法的 company_id");
		}
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		String appidQuery = request.getParameter("appid");
		String puidRaw = request.getParameter("puid");
		String pathRaw = request.getParameter("path");
		byte[] png = promoterFrontNewQrcodePngService.getPromoterNewQrcodePng(companyId, appidQuery, puidRaw, pathRaw);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.body(png);
	}
}
