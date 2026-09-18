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

package cn.shopex.ecshopx.kaquan.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageReceivesPackageService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.PackageReceivesCurrentGradeCardPackageService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.PackageReceivesShowCardPackageService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountBindCardListQueryService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountConsumCardService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountGetCardDetailService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountMyUserCardListService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountNewGetCardListFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountWxappUserCardListService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountExchangeCardInfoService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountExchangeCardTransactionService;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountUserRemoveCardService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountNewGetCardListRequest;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("kaquanUserDiscountFrontV1")
@RequestMapping("/api/v1/h5app")
public class UserDiscountController {

	private static final String MSG_REMOVE_AUTH_INVALID = "删除卡券失败，信息有误";
	private static final String MSG_CONSUM_CARD_AUTH_INVALID = "核销卡券失败，信息有误";
	private static final String MSG_GET_CARD_DETAIL_AUTH_INVALID = "获取优惠券失败，信息有误";

	private final UserDiscountUserRemoveCardService userDiscountUserRemoveCardService;
	private final PackageReceivesCurrentGradeCardPackageService packageReceivesCurrentGradeCardPackageService;
	private final UserDiscountExchangeCardTransactionService userDiscountExchangeCardTransactionService;
	private final CardPackageReceivesPackageService cardPackageReceivesPackageService;
	private final UserDiscountConsumCardService userDiscountConsumCardService;
	private final UserDiscountExchangeCardInfoService userDiscountExchangeCardInfoService;
	private final UserDiscountBindCardListQueryService userDiscountBindCardListQueryService;
	private final UserDiscountGetCardDetailService userDiscountGetCardDetailService;
	private final UserDiscountWxappUserCardListService userDiscountWxappUserCardListService;
	private final UserDiscountMyUserCardListService userDiscountMyUserCardListService;
	private final UserDiscountNewGetCardListFacadeService userDiscountNewGetCardListFacadeService;
	private final MemberAccountService memberAccountService;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final PackageReceivesShowCardPackageService packageReceivesShowCardPackageService;

	public UserDiscountController(UserDiscountUserRemoveCardService userDiscountUserRemoveCardService,
			PackageReceivesCurrentGradeCardPackageService packageReceivesCurrentGradeCardPackageService,
			UserDiscountExchangeCardTransactionService userDiscountExchangeCardTransactionService,
			CardPackageReceivesPackageService cardPackageReceivesPackageService,
			UserDiscountConsumCardService userDiscountConsumCardService,
			UserDiscountExchangeCardInfoService userDiscountExchangeCardInfoService,
			UserDiscountBindCardListQueryService userDiscountBindCardListQueryService,
			UserDiscountGetCardDetailService userDiscountGetCardDetailService,
			UserDiscountWxappUserCardListService userDiscountWxappUserCardListService,
			UserDiscountMyUserCardListService userDiscountMyUserCardListService,
			UserDiscountNewGetCardListFacadeService userDiscountNewGetCardListFacadeService,
			MemberAccountService memberAccountService,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			PackageReceivesShowCardPackageService packageReceivesShowCardPackageService) {
		this.userDiscountUserRemoveCardService = userDiscountUserRemoveCardService;
		this.packageReceivesCurrentGradeCardPackageService = packageReceivesCurrentGradeCardPackageService;
		this.userDiscountExchangeCardTransactionService = userDiscountExchangeCardTransactionService;
		this.cardPackageReceivesPackageService = cardPackageReceivesPackageService;
		this.userDiscountConsumCardService = userDiscountConsumCardService;
		this.userDiscountExchangeCardInfoService = userDiscountExchangeCardInfoService;
		this.userDiscountBindCardListQueryService = userDiscountBindCardListQueryService;
		this.userDiscountGetCardDetailService = userDiscountGetCardDetailService;
		this.userDiscountWxappUserCardListService = userDiscountWxappUserCardListService;
		this.userDiscountMyUserCardListService = userDiscountMyUserCardListService;
		this.userDiscountNewGetCardListFacadeService = userDiscountNewGetCardListFacadeService;
		this.memberAccountService = memberAccountService;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.packageReceivesShowCardPackageService = packageReceivesShowCardPackageService;
	}

	@GetMapping("/wxapp/user/receiveCard")
	public ResponseEntity<ApiResult<Map<String, Object>>> receiveCard(
			HttpServletRequest request,
			@RequestParam(value = "card_id", required = false) String cardIdParam,
			@RequestParam(value = "salesperson_id", required = false, defaultValue = "0") String salespersonIdParam,
			@RequestParam(value = "work_userid", required = false, defaultValue = "") String workUserid,
			@RequestParam(value = "salesperson_code", required = false, defaultValue = "") String salespersonCodeParam) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdForReceiveCardOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		String mobile = resolveMobileForUserDiscountContextOrThrow(companyId, userId, claims);

		Long cardId = null;
		if (StringUtils.hasText(cardIdParam)) {
			try {
				long v = Long.parseLong(cardIdParam.trim());
				if (v > 0L) {
					cardId = v;
				}
			} catch (NumberFormatException ignored) {
				// treat as absent
			}
		}
		long salespersonId = parseLongFlexible(salespersonIdParam, 0L);
		String salespersonCode =
				StringUtils.hasText(workUserid)
						? workUserid.trim()
						: (salespersonCodeParam != null ? salespersonCodeParam.trim() : "");

		Map<String, Object> status =
				userDiscountReceiveCardService.receiveCard(companyId, userId, mobile, cardId, salespersonId, salespersonCode);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@GetMapping("/wxapp/user/consumCard")
	public ResponseEntity<ApiResult<Map<String, Object>>> ConsumCard(
			HttpServletRequest request,
			@RequestParam(value = "code", required = false) String codeParam) {
		if (isBlankOrZeroQueryParam(codeParam)) {
			throw new ResourceException("核销卡券失败,code码必填.");
		}
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdForConsumCardOrThrow(claims);
		String mobile = parseMobileFromClaimsForConsumCardOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		Map<String, Object> result = userDiscountConsumCardService.consumePayBillCard(companyId, userId, mobile, codeParam.trim());
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", result)));
	}

	@GetMapping("/wxapp/user/removeCard")
	public ResponseEntity<ApiResult<Map<String, Object>>> DeleteUserCard(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idParam,
			@RequestParam(value = "code", required = false) String codeParam) {
		if (isBlankOrZeroQueryParam(idParam) && isBlankOrZeroQueryParam(codeParam)) {
			throw new ResourceException("核销卡券失败,code码 和 卡券id 二选一必填.");
		}
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdFromClaimsOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);

		Long recordId = null;
		if (!isBlankOrZeroQueryParam(idParam)) {
			String trimmedId = idParam.trim();
			try {
				long parsed = Long.parseLong(trimmedId);
				if (parsed > 0L) {
					recordId = parsed;
				}
			} catch (NumberFormatException e) {
				return ResponseEntity.ok(ApiResult.ok(Map.of("status", false)));
			}
		}
		String code = null;
		if (!isBlankOrZeroQueryParam(codeParam)) {
			code = codeParam.trim();
		}

		boolean ok = userDiscountUserRemoveCardService.removeUserReceivedCard(companyId, userId, recordId, code);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", ok)));
	}

	@PostMapping("/wxapp/user/exchangeCard")
	public ResponseEntity<ApiResult<Map<String, Object>>> exchangeCard(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdFromClaimsOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		Map<String, Object> merged = mergeExchangeCardInput(request, body);
		long userCardRecordId = parseRequiredPositiveLongParam(merged, "user_card_id");
		long itemId = parseRequiredPositiveLongParam(merged, "item_id");
		if (!merged.containsKey("distributor_id") || merged.get("distributor_id") == null) {
			throw new ResourceException("参数错误");
		}
		long distributorId = parseLongFlexible(merged.get("distributor_id"), -1L);
		if (distributorId < 0L) {
			throw new ResourceException("参数错误");
		}
		userDiscountExchangeCardTransactionService.exchangeCard(companyId, userId, userCardRecordId, itemId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@GetMapping("/wxapp/user/exchangeCardInfo")
	public ResponseEntity<ApiResult<Map<String, Object>>> exchangeCardInfo(
			HttpServletRequest request,
			@RequestParam(value = "user_card_id", required = false) String userCardIdParam) {
		if (isBlankOrZeroQueryParam(userCardIdParam)) {
			throw new ResourceException("user_card_id 必填");
		}
		long userCardId;
		try {
			userCardId = Long.parseLong(userCardIdParam.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("兑换券不存在");
		}
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdFromClaimsOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		Map<String, Object> data = userDiscountExchangeCardInfoService.buildExchangeCardInfo(companyId, userCardId, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/user/getCardList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserCardList(
			HttpServletRequest request,
			@RequestParam(value = "page_no", defaultValue = "1") int pageNo,
			@RequestParam(value = "page_size", defaultValue = "20") int pageSize,
			@RequestParam(value = "shop_id", required = false, defaultValue = "0") String shopIdParam,
			@RequestParam(value = "amount", required = false) String amountParam,
			@RequestParam(value = "code", required = false) String codeParam,
			@RequestParam(value = "card_id", required = false) String cardIdParam,
			@RequestParam(value = "use_scenes", required = false) String useScenesParam,
			@RequestParam(value = "use_platform", required = false) String usePlatformParam,
			@RequestParam(value = "status", required = false) String statusParam) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
		}
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		String mobile = resolveMobileForUserDiscountContextOrThrow(companyId, userId, claims);
		Map<String, Object> data = userDiscountWxappUserCardListService.build(
				companyId,
				userId,
				mobile,
				pageNo,
				pageSize,
				shopIdParam,
				amountParam,
				codeParam,
				cardIdParam,
				useScenesParam,
				usePlatformParam,
				statusParam);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/user/newGetCardList")
	public ResponseEntity<ApiResult<Map<String, Object>>> newGetCardList(
			HttpServletRequest request,
			@RequestParam(value = "amount", required = false) String amount,
			@RequestParam(value = "items", required = false) String items,
			@RequestParam(value = "code", required = false) String code,
			@RequestParam(value = "card_id", required = false) String cardId,
			@RequestParam(value = "shop_id", required = false) String shopId,
			@RequestParam(value = "item_id", required = false) String itemId,
			@RequestParam(value = "page_no", required = false, defaultValue = "1") int pageNo,
			@RequestParam(value = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(value = "use_platform", required = false) String usePlatform,
			@RequestParam(value = "page_type", required = false) String pageType,
			@RequestParam(value = "use_scenes", required = false) String useScenes,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "is_checkout", required = false) String isCheckout,
			@RequestParam(value = "cart_type", required = false) String cartType,
			@RequestParam(value = "cxdid", required = false) String cxdid,
			@RequestParam(value = "point_use", required = false) String pointUse,
			@RequestParam(value = "iscrossborder", required = false) String iscrossborder,
			@RequestParam(value = "isShopScreen", required = false) String isShopScreen,
			@RequestParam(value = "valid", required = false, defaultValue = "true") String valid) {
		Map<String, Object> claims = readH5ClaimsForNewGetCardListOrThrow(request);
		long companyId = parsePositiveCompanyIdFromRequestForNewGetCardListOrThrow(request);
		long userId = parsePositiveUserIdForNewGetCardListOrThrow(claims);
		UserDiscountNewGetCardListRequest req = new UserDiscountNewGetCardListRequest(
				amount,
				items,
				code,
				cardId,
				shopId,
				itemId,
				pageNo,
				pageSize,
				usePlatform,
				pageType,
				useScenes,
				distributorId,
				isCheckout,
				cartType,
				cxdid,
				pointUse,
				iscrossborder,
				isShopScreen,
				valid);
		Map<String, Object> data = userDiscountNewGetCardListFacadeService.build(companyId, userId, req);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/user/getUserCardList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMyUserCardList(
			HttpServletRequest request,
			@RequestParam(value = "status", required = false, defaultValue = "1") String status,
			@RequestParam(value = "card_type", required = false) String cardType,
			@RequestParam(value = "scope_type", required = false) String scopeType,
			@RequestParam(value = "source_type", required = false) String sourceType,
			@RequestParam(value = "source_id", required = false) String sourceIdParam,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
		}
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		String mobile = resolveMobileForUserDiscountContextOrThrow(companyId, userId, claims);
		Map<String, Object> data = userDiscountMyUserCardListService.build(
				companyId,
				userId,
				mobile,
				status,
				cardType,
				scopeType,
				sourceType,
				sourceIdParam,
				page,
				pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/user/getCardDetail")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserDiscountDetail(
			HttpServletRequest request,
			@RequestParam(value = "code", required = false) String code,
			@RequestParam(value = "card_id", required = false) String cardIdParam) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
		}
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		resolveMobileForUserDiscountContextOrThrow(companyId, userId, claims);
		Long parsedCardId = null;
		if (StringUtils.hasText(cardIdParam)) {
			try {
				long v = Long.parseLong(cardIdParam.trim());
				if (v > 0L) {
					parsedCardId = v;
				}
			} catch (NumberFormatException ignored) {
				// omit card_id filter (invalid string treated as absent)
			}
		}
		String wxappAppid = null;
		if (claims.containsKey("wxapp_appid") && claims.get("wxapp_appid") != null) {
			wxappAppid = String.valueOf(claims.get("wxapp_appid")).trim();
		}
		Map<String, Object> data =
				userDiscountGetCardDetailService.build(companyId, userId, code, parsedCardId, wxappAppid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/user/newGetCardDetail")
	public ResponseEntity<ApiResult<Map<String, Object>>> newGetCardDetail(
			HttpServletRequest request,
			@RequestParam(value = "code", required = false) String code,
			@RequestParam(value = "card_id", required = false) String cardIdParam) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		long userId = parseLongFlexible(claims.get("user_id"), 0L);
		Long parsedCardId = null;
		if (StringUtils.hasText(cardIdParam)) {
			try {
				long v = Long.parseLong(cardIdParam.trim());
				if (v > 0L) {
					parsedCardId = v;
				}
			} catch (NumberFormatException ignored) {
				// omit card_id filter (invalid string treated as absent)
			}
		}
		String wxappAppid = null;
		if (claims.containsKey("wxapp_appid") && claims.get("wxapp_appid") != null) {
			wxappAppid = String.valueOf(claims.get("wxapp_appid")).trim();
		}
		Map<String, Object> data = userDiscountGetCardDetailService.buildWithCur(
				companyId, userId, code, parsedCardId, wxappAppid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/user/usedCard")
	public ResponseEntity<ApiResult<Map<String, Object>>> userUsedCard(
			HttpServletRequest request,
			@RequestParam(value = "code", required = false) String codeParam,
			@RequestParam(value = "shop_id", required = false) String shopIdParam,
			@RequestParam(value = "verify_code", required = false) String verifyCodeParam,
			@RequestParam(value = "remark_amount", required = false) String remarkAmountParam,
			@RequestParam(value = "consume_outer_str", required = false) String consumeOuterStrParam) {
		if (isBlankOrZeroQueryParam(codeParam) || isBlankOrZeroQueryParam(shopIdParam)) {
			throw new ResourceException("核销卡券失败,提交卡券信息有误.");
		}
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException("使用失败，信息有误");
		}
		long userId = parseLongFlexible(claims.get("user_id"), 0L);
		if (userId <= 0L) {
			throw new ResourceException("使用失败，信息有误");
		}
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		Map<String, Object> result =
				userDiscountConsumCardService.userUsedCardSelfConsume(
						companyId,
						userId,
						codeParam.trim(),
						shopIdParam.trim(),
						verifyCodeParam,
						remarkAmountParam,
						consumeOuterStrParam);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", result)));
	}

	@PostMapping("/wxapp/user/currentGardCardPackage")
	public ResponseEntity<ApiResult<Map<String, Object>>> currentGardCardPackage(HttpServletRequest request) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdFromClaimsOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		String type = packageReceivesCurrentGradeCardPackageService.currentGardCardPackage(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("type", type)));
	}

	@PostMapping("/wxapp/user/receiveCardPackage")
	public ResponseEntity<ApiResult<Map<String, Object>>> receivesPackage(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5ClaimsOrThrow(request);
		long userId = parsePositiveUserIdFromClaimsOrThrow(claims);
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		Map<String, Object> merged = mergeReceiveCardPackageInput(request, body);
		if (!merged.containsKey("package_id")) {
			throw new ResourceException(" 卡券包ID必填");
		}
		Object packageIdRaw = merged.get("package_id");
		if (packageIdRaw == null) {
			throw new ResourceException(" 卡券包ID必填");
		}
		if (packageIdRaw instanceof String s && s.trim().isEmpty()) {
			throw new ResourceException(" 卡券包ID必填");
		}
		long packageId = parseLongFlexible(packageIdRaw, 0L);
		if (packageId <= 0L) {
			throw new BadRequestException("卡券包ID必须为大于0的整数");
		}
		Object salespersonRaw =
				merged.containsKey("sales_person_id") && merged.get("sales_person_id") != null
						? merged.get("sales_person_id")
						: merged.get("salesperson_id");
		long salespersonId = parseLongFlexible(salespersonRaw, 0L);
		cardPackageReceivesPackageService.receivesPackage(companyId, packageId, userId, "template", salespersonId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@GetMapping("/wxapp/user/showCardPackage")
	public ResponseEntity<ApiResult<Map<String, Object>>> showCardPackage(
			HttpServletRequest request,
			@RequestParam(value = "receive_type", required = false) String receiveTypeParam) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?>)) {
			throw new UnauthorizedException(MSG_REMOVE_AUTH_INVALID);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = (Map<String, Object>) rawClaims;
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new UnauthorizedException(MSG_REMOVE_AUTH_INVALID);
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new UnauthorizedException(MSG_REMOVE_AUTH_INVALID);
		}
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_REMOVE_AUTH_INVALID);
			}
		} else {
			throw new UnauthorizedException(MSG_REMOVE_AUTH_INVALID);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_REMOVE_AUTH_INVALID);
		}
		if (receiveTypeParam == null || receiveTypeParam.isBlank()) {
			throw new ResourceException("显示类型必传");
		}
		String trimmedType = receiveTypeParam.trim();
		if (!"template".equals(trimmedType) && !"grade".equals(trimmedType) && !"vip_grade".equals(trimmedType)) {
			throw new ResourceException("显示类型必传");
		}
		Map<String, Object> data =
				packageReceivesShowCardPackageService.showCardPackage(companyId, userId, trimmedType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping("/wxapp/user/confirmPackageShow")
	public ResponseEntity<Void> confirmPackageReceivesShow() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/wxapp/user/getBindCardList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCardListByBindType(
			HttpServletRequest request,
			@RequestParam(value = "grade_id", required = false) String gradeIdParam,
			@RequestParam(value = "type", required = false) String typeParam) {
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		long gradeId = parseGradeIdForBindCardListOrThrow(gradeIdParam);
		String type = parseBindCardListTypeParamOrThrow(typeParam);
		Map<String, Object> result = userDiscountBindCardListQueryService.buildBindCardList(companyId, gradeId, type);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private String resolveMobileForUserDiscountContextOrThrow(long companyId, long userId, Map<String, Object> claims) {
		Object mobileRaw = claims.get("mobile");
		if (mobileRaw != null) {
			String fromClaim = mobileRaw instanceof Number n ? n.toString() : mobileRaw.toString().trim();
			if (StringUtils.hasText(fromClaim)) {
				return fromClaim;
			}
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		if (info == null || info.isEmpty()) {
			throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
		}
		Object region = info.get("region_mobile");
		if (region != null) {
			String s = region instanceof Number n ? n.toString() : region.toString().trim();
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		Object mob = info.get("mobile");
		if (mob != null) {
			String s = mob instanceof Number n ? n.toString() : mob.toString().trim();
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		throw new ResourceException(MSG_GET_CARD_DETAIL_AUTH_INVALID);
	}

	private static boolean isBlankOrZeroQueryParam(String v) {
		return v == null || v.isBlank() || "0".equals(v.trim());
	}

	private static long parseGradeIdForBindCardListOrThrow(String gradeIdParam) {
		if (gradeIdParam == null || gradeIdParam.isBlank()) {
			throw new ResourceException("等级设置ID必传");
		}
		try {
			long v = Long.parseLong(gradeIdParam.trim());
			if (v < 1L) {
				throw new ResourceException("等级设置ID必传");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("等级设置ID必传");
		}
	}

	private static String parseBindCardListTypeParamOrThrow(String typeParam) {
		if (typeParam == null || typeParam.isBlank()) {
			throw new ResourceException("类型必传");
		}
		String t = typeParam.trim();
		if (!"vip_grade".equals(t) && !"grade".equals(t)) {
			throw new BadRequestException("The selected type is invalid.");
		}
		return t;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5ClaimsOrThrow(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?>)) {
			throw new ResourceException(MSG_REMOVE_AUTH_INVALID);
		}
		return (Map<String, Object>) rawClaims;
	}

	private static long parsePositiveUserIdForReceiveCardOrThrow(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException(KaquanDiscountCardMessages.NOT_MEMBER_CANNOT_RECEIVE);
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException(KaquanDiscountCardMessages.NOT_MEMBER_CANNOT_RECEIVE);
		}
		return userId;
	}

	private static long parsePositiveUserIdFromClaimsOrThrow(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException(MSG_REMOVE_AUTH_INVALID);
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException(MSG_REMOVE_AUTH_INVALID);
		}
		return userId;
	}

	private static long parsePositiveUserIdForConsumCardOrThrow(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !h5UserIdTruthy(claimUid)) {
			throw new ResourceException(MSG_CONSUM_CARD_AUTH_INVALID);
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new ResourceException(MSG_CONSUM_CARD_AUTH_INVALID);
		}
		return userId;
	}

	private static String parseMobileFromClaimsForConsumCardOrThrow(Map<String, Object> claims) {
		Object raw = claims.get("mobile");
		if (raw == null) {
			throw new ResourceException(MSG_CONSUM_CARD_AUTH_INVALID);
		}
		String s = raw instanceof Number n ? n.toString() : raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(MSG_CONSUM_CARD_AUTH_INVALID);
		}
		return s.trim();
	}

	private static long parsePositiveCompanyIdFromRequestOrThrow(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException(MSG_REMOVE_AUTH_INVALID);
			}
		} else {
			throw new ResourceException(MSG_REMOVE_AUTH_INVALID);
		}
		if (companyId <= 0L) {
			throw new ResourceException(MSG_REMOVE_AUTH_INVALID);
		}
		return companyId;
	}

	private static boolean h5UserIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5ClaimsForNewGetCardListOrThrow(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> claims)) {
			throw new UnauthorizedException("未授权或登录已过期");
		}
		return (Map<String, Object>) claims;
	}

	private static long parsePositiveCompanyIdFromRequestForNewGetCardListOrThrow(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("未授权或登录已过期");
			}
		} else {
			throw new UnauthorizedException("未授权或登录已过期");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("未授权或登录已过期");
		}
		return companyId;
	}

	private static long parsePositiveUserIdForNewGetCardListOrThrow(Map<String, Object> claims) {
		Object raw = claims.get("user_id");
		long userId = parseLongFlexible(raw, 0L);
		if (userId <= 0L) {
			throw new UnauthorizedException("未授权或登录已过期");
		}
		return userId;
	}

	private static Map<String, Object> mergeExchangeCardInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> mergeReceiveCardPackageInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static long parseRequiredPositiveLongParam(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key) || merged.get(key) == null) {
			throw new ResourceException("参数错误");
		}
		long v = parseLongFlexible(merged.get(key), 0L);
		if (v <= 0L) {
			throw new ResourceException("参数错误");
		}
		return v;
	}
}
