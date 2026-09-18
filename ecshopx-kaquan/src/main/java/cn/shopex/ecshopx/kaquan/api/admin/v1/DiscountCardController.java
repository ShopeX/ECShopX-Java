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

package cn.shopex.ecshopx.kaquan.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.discount.DiscountCardKaquanDetailLoadService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.kaquan.service.discount.ConsumeExCardCodeParser;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardConsumeExCardFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardConsumeExCardRequestMergeService;
import cn.shopex.ecshopx.kaquan.service.discount.CouponCardGrantSettingWriteService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardCreateFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardCreateRequestMergeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardDeleteFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardAdminListService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardEasyListService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardEffectiveListService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardDetailListService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardParamNormalize;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardUploadWechatFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardStoreUpdateService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardUpdateFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("kaquanDiscountCardAdminV1")
@RequestMapping("/api/v1")
public class DiscountCardController {

	private final DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService;
	private final DiscountCardCreateFacadeService discountCardCreateFacadeService;
	private final DiscountCardUpdateFacadeService discountCardUpdateFacadeService;
	private final DiscountCardConsumeExCardRequestMergeService discountCardConsumeExCardRequestMergeService;
	private final ConsumeExCardCodeParser consumeExCardCodeParser;
	private final DiscountCardConsumeExCardFacadeService discountCardConsumeExCardFacadeService;
	private final CouponCardGrantSettingWriteService couponCardGrantSettingWriteService;
	private final DiscountCardStoreUpdateService discountCardStoreUpdateService;
	private final DiscountCardUploadWechatFacadeService discountCardUploadWechatFacadeService;
	private final DiscountCardKaquanDetailLoadService discountCardKaquanDetailLoadService;
	private final DiscountCardDetailListService discountCardDetailListService;
	private final DiscountCardAdminListService discountCardAdminListService;
	private final DiscountCardEasyListService discountCardEasyListService;
	private final DiscountCardEffectiveListService discountCardEffectiveListService;
	private final DiscountCardDeleteFacadeService discountCardDeleteFacadeService;

	public DiscountCardController(DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService,
			DiscountCardCreateFacadeService discountCardCreateFacadeService,
			DiscountCardUpdateFacadeService discountCardUpdateFacadeService,
			DiscountCardConsumeExCardRequestMergeService discountCardConsumeExCardRequestMergeService,
			ConsumeExCardCodeParser consumeExCardCodeParser,
			DiscountCardConsumeExCardFacadeService discountCardConsumeExCardFacadeService,
			CouponCardGrantSettingWriteService couponCardGrantSettingWriteService,
			DiscountCardStoreUpdateService discountCardStoreUpdateService,
			DiscountCardUploadWechatFacadeService discountCardUploadWechatFacadeService,
			DiscountCardKaquanDetailLoadService discountCardKaquanDetailLoadService,
			DiscountCardDetailListService discountCardDetailListService,
			DiscountCardAdminListService discountCardAdminListService,
			DiscountCardEasyListService discountCardEasyListService,
			DiscountCardEffectiveListService discountCardEffectiveListService,
			DiscountCardDeleteFacadeService discountCardDeleteFacadeService) {
		this.discountCardCreateRequestMergeService = discountCardCreateRequestMergeService;
		this.discountCardCreateFacadeService = discountCardCreateFacadeService;
		this.discountCardUpdateFacadeService = discountCardUpdateFacadeService;
		this.discountCardConsumeExCardRequestMergeService = discountCardConsumeExCardRequestMergeService;
		this.consumeExCardCodeParser = consumeExCardCodeParser;
		this.discountCardConsumeExCardFacadeService = discountCardConsumeExCardFacadeService;
		this.couponCardGrantSettingWriteService = couponCardGrantSettingWriteService;
		this.discountCardStoreUpdateService = discountCardStoreUpdateService;
		this.discountCardUploadWechatFacadeService = discountCardUploadWechatFacadeService;
		this.discountCardKaquanDetailLoadService = discountCardKaquanDetailLoadService;
		this.discountCardDetailListService = discountCardDetailListService;
		this.discountCardAdminListService = discountCardAdminListService;
		this.discountCardEasyListService = discountCardEasyListService;
		this.discountCardEffectiveListService = discountCardEffectiveListService;
		this.discountCardDeleteFacadeService = discountCardDeleteFacadeService;
	}

	@Activated(routeAlias = "card.create")
	@PostMapping(value = "/discountcard", name = "添加优惠券")
	public ResponseEntity<ApiResult<Map<String, Object>>> createDiscountCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> allParams = discountCardCreateRequestMergeService.merge(request, body);
		Map<String, Object> row = discountCardCreateFacadeService.create(allParams, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", row)));
	}

	@Activated(routeAlias = "card.delete")
	@DeleteMapping(value = "/discountcard", name = "删除卡券")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteDiscountCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		String authorizerAppid = DiscountCardParamNormalize.stringVal(operatorJwt.get("authorizer_appid"));
		discountCardDeleteFacadeService.deleteDiscountCard(merged, companyId, authorizerAppid);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "card.update")
	@PatchMapping(value = "/discountcard", name = "修改卡券内容")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateDiscountCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> allParams = discountCardCreateRequestMergeService.merge(request, body);
		Map<String, Object> row = discountCardUpdateFacadeService.update(allParams, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", row)));
	}

	@Activated(routeAlias = "card.get")
	@GetMapping(value = "/discountcard/get", name = "获取卡券明细")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDiscountCardDetail(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, null);
		Object cardIdObj = merged.get("card_id");
		String cardIdRaw = cardIdObj == null ? "" : String.valueOf(cardIdObj);
		discountCardDetailListService.validateCardIdForAdminDetail(cardIdRaw);
		String cardIdTrimmed = cardIdRaw.trim();
		String distributorIdOpt = DiscountCardParamNormalize.stringVal(merged.get("distributor_id"));
		Map<String, Object> result = discountCardKaquanDetailLoadService.loadDetailForAdmin(companyId, cardIdTrimmed, distributorIdOpt);
		if (result == null || result.isEmpty()) {
			Map<String, Object> statusOnly = new LinkedHashMap<>();
			statusOnly.put("status", result != null ? result : Map.of());
			return ResponseEntity.ok(ApiResult.ok(statusOnly));
		}
		applyDiscountCardDetailActionPresentation(result);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@SuppressWarnings("unchecked")
	private static void applyDiscountCardDetailActionPresentation(Map<String, Object> result) {
		Object tlObj = result.remove("time_limit");
		if (tlObj instanceof List<?> timeLimit) {
			if (timeLimit.isEmpty()) {
				result.put("time_limit", null);
			} else {
			String begin = "";
			String end = "";
			List<String> typeSequence = new ArrayList<>();
			Map<String, Object> timeLimitDate = new LinkedHashMap<>();
			for (Object el : timeLimit) {
				if (!(el instanceof Map<?, ?> vm)) {
					continue;
				}
				Map<String, Object> value = (Map<String, Object>) vm;
				Object typeObj = value.get("type");
				if (typeObj != null) {
					typeSequence.add(String.valueOf(typeObj));
				}
				Object bh = value.get("begin_hour");
				Object bm = value.get("begin_minute");
				Object eh = value.get("end_hour");
				Object em = value.get("end_minute");
				if (bh != null && bm != null && eh != null && em != null) {
					String bStr = bh + ":" + bm;
					String eStr = eh + ":" + em;
					if (begin.isEmpty() && end.isEmpty()) {
						begin = bStr;
						end = eStr;
						Map<String, Object> slot = new LinkedHashMap<>();
						slot.put("begin_time", begin);
						slot.put("end_time", end);
						timeLimitDate.put("1", slot);
					} else if (!begin.equals(bStr)) {
						Map<String, Object> slot2 = new LinkedHashMap<>();
						slot2.put("begin_time", bStr);
						slot2.put("end_time", eStr);
						timeLimitDate.put("2", slot2);
					}
				}
			}
			if (!typeSequence.isEmpty()) {
				Set<String> seen = new LinkedHashSet<>();
				List<String> uniqueTypes = new ArrayList<>();
				for (String t : typeSequence) {
					if (seen.add(t)) {
						uniqueTypes.add(t);
					}
				}
				result.put("time_limit_type", uniqueTypes);
			}
			if (!timeLimitDate.isEmpty()) {
				result.put("time_limit_date", timeLimitDate);
			}
			}
		} else {
			result.put("time_limit", null);
		}
		result.put("begin_time", intValueFlexible(result.get("begin_date")));
		Object fixedTerm = result.get("fixed_term");
		result.put("days", fixedTerm != null ? intValueFlexible(fixedTerm) : 30);
		Object endDate = result.get("end_date");
		result.put("end_time", endDate != null ? intValueFlexible(endDate) : 0);
		Object disc = result.get("discount");
		if (disc instanceof Number n && n.intValue() > 0) {
			result.put("discount", (100.0 - n.doubleValue()) / 10.0);
		}
		if (ValuePresence.hasEffectiveValue(result.get("least_cost"))) {
			result.put("least_cost", divideBy100AsDouble(result.get("least_cost")));
		}
		if (ValuePresence.hasEffectiveValue(result.get("reduce_cost"))) {
			result.put("reduce_cost", divideBy100AsDouble(result.get("reduce_cost")));
		}
		if (ValuePresence.hasEffectiveValue(result.get("most_cost"))) {
			result.put("most_cost", divideBy100AsDouble(result.get("most_cost")));
		}
	}

	private static int intValueFlexible(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static double divideBy100AsDouble(Object o) {
		if (o instanceof Number n) {
			return n.doubleValue() / 100.0;
		}
		if (o == null) {
			return 0.0;
		}
		try {
			return Double.parseDouble(o.toString().trim()) / 100.0;
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	@Activated(routeAlias = "card.list")
	@GetMapping(value = "/discountcard/list", name = "获取卡券列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDiscountCardList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, null);
		String[] statusValues = request.getParameterValues("status");
		if (statusValues != null && statusValues.length > 0) {
			if (statusValues.length == 1) {
				merged.put("status", statusValues[0]);
			} else {
				merged.put("status", Arrays.asList(statusValues));
			}
		}
		Map<String, Object> data = discountCardAdminListService.query(companyId, merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "effectiveCard.list")
	@GetMapping(value = "/effectiveDiscountcard/list", name = "获取有效卡券列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getEffectiveDiscountCardList(HttpServletRequest request,
			@RequestParam(value = "page_no", required = false) String pageNoRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw,
			@RequestParam(value = "card_type", required = false) String cardType) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long sourceId = DiscountCardParamNormalize.longFromObject(operatorJwt.get("distributor_id"), 0L);
		int pageNo = DiscountCardParamNormalize.parseIntFlexible(pageNoRaw, 1);
		int pageSize = DiscountCardParamNormalize.parseIntFlexible(pageSizeRaw, 20);
		if (pageSize < 1) {
			pageSize = 20;
		}
		Map<String, Object> data = discountCardEffectiveListService.query(companyId, sourceId, cardType, pageNo, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "card.store")
	@PostMapping(value = "/discountcard/updatestore", name = "修改卡券库存")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateCardStore(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		assertQuantityNotNegativeForStoreUpdate(merged.get("quantity"));
		discountCardStoreUpdateService.updateStore(merged, companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	/**
	 * 修改库存时对数量做前置校验：仅当能解析为整数且小于 0 时拒绝；空值不拦截；无法解析为整数的输入交由服务层按业务规则处理；数量上界由服务层结合剩余库存等规则判定。
	 */
	private static void assertQuantityNotNegativeForStoreUpdate(Object raw) {
		if (raw == null) {
			return;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return;
			}
			try {
				long v = Long.parseLong(t);
				if (v < 0L) {
					throw new BadRequestException("修改库存出错,请输入大于0的整数");
				}
			} catch (NumberFormatException e) {
				// 无法解析则交给 Service 按 0/宽松逻辑处理
			}
			return;
		}
		if (raw instanceof Number n) {
			if (n.longValue() < 0L) {
				throw new BadRequestException("修改库存出错,请输入大于0的整数");
			}
			return;
		}
		String t = raw.toString().trim();
		if (t.isEmpty()) {
			return;
		}
		try {
			long v = Long.parseLong(t);
			if (v < 0L) {
				throw new BadRequestException("修改库存出错,请输入大于0的整数");
			}
		} catch (NumberFormatException e) {
			// ignore
		}
	}

	@Activated(routeAlias = "card.upload")
	@PostMapping(value = "/discountcard/uploadToWechat", name = "卡券推送至微信")
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadToWechatCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		List<Long> cardIds = DiscountCardUploadWechatFacadeService.parseCardIdsFromMerged(merged.get("card_ids"));
		if (cardIds.isEmpty()) {
			throw new BadRequestException("请选择要同步至微信的卡券");
		}
		// 先校验所选卡券，再读取公众号授权 appid；无授权时异步任务仍会入队，接口立即返回成功。
		String authorizerAppid = DiscountCardParamNormalize.stringVal(operatorJwt.get("authorizer_appid"));
		discountCardUploadWechatFacadeService.submitUploadToWechat(authorizerAppid, companyId, cardIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "card.easy.list")
	@GetMapping(value = "/discountcard/listdata", name = "获取优惠券列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getEasyDiscountList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, null);
		Object cardTypeObj = merged.get("card_type");
		String cardTypeRaw = cardTypeObj == null ? null : String.valueOf(cardTypeObj);
		int page = Math.max(1, DiscountCardParamNormalize.parseIntFlexible(merged.get("page"), 1));
		int pageSize = DiscountCardParamNormalize.parseIntFlexible(merged.get("pageSize"), 30);
		if (pageSize <= 0) {
			pageSize = 30;
		}
		if (pageSize > 500) {
			pageSize = 500;
		}
		List<Map<String, Object>> data = discountCardEasyListService.list(companyId, cardTypeRaw, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "couponGrantSetting.list")
	@GetMapping(value = "/discountcard/couponGrantSetting", name = "获取优惠券发放管理配置信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCouponCardGrantSetting(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, String> rawFields = couponCardGrantSettingWriteService.loadAll(companyId);
		Map<String, Object> dataMap = new LinkedHashMap<>(rawFields.size());
		rawFields.forEach(dataMap::put);
		return ResponseEntity.ok(ApiResult.ok(dataMap));
	}

	@Activated(routeAlias = "couponGrantSetting.set")
	@PostMapping(value = "/discountcard/couponGrantSetting", name = "保存优惠券发放管理配置信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> setCouponCardGrantSetting(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		boolean ok = couponCardGrantSettingWriteService.save(companyId, merged);
		String result = ok ? "ok" : "fail";
		return ResponseEntity.ok(ApiResult.ok(Map.of("result", result)));
	}

	@Activated(routeAlias = "card.consume")
	@PostMapping(value = "/discountcard/consume", name = "兑换券核销")
	public ResponseEntity<ApiResult<Map<String, Object>>> consumeExCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardConsumeExCardRequestMergeService.merge(request, body);
		Object codeObj = merged.get("code");
		String rawCode = codeObj == null ? "" : codeObj.toString().trim();
		if (rawCode.isEmpty()) {
			throw new BadRequestException("code 错误");
		}
		ConsumeExCardCodeParser.Parsed parsed = consumeExCardCodeParser.parse(rawCode);
		Object distributorRaw = merged.get("distributor_id");
		try {
			Map<String, Object> orderData = discountCardConsumeExCardFacadeService.consume(
					request, companyId, parsed.userCardId(), parsed.verifySegment(), distributorRaw, operatorJwt);
			return ResponseEntity.ok(ApiResult.ok(Map.of("status", true, "order_info", orderData)));
		} catch (ResourceException e) {
			Map<String, Object> distributors = discountCardConsumeExCardFacadeService.buildSoftFailDistributors(
					request, companyId, parsed.userCardId(), operatorJwt, e);
			return ResponseEntity.ok(ApiResult.ok(Map.of(
					"status", false,
					"message", e.getMessage(),
					"distributors", distributors)));
		}
	}
}
