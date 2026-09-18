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

package cn.shopex.ecshopx.members.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.setting.WhitelistSettingRedisService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.address.MemberAddressAreaService;
import cn.shopex.ecshopx.members.service.address.MemberAddressCreateService;
import cn.shopex.ecshopx.members.service.address.MemberAddressDeleteService;
import cn.shopex.ecshopx.members.service.address.MemberAddressGetService;
import cn.shopex.ecshopx.members.service.address.MemberAddressListService;
import cn.shopex.ecshopx.members.service.address.MemberAddressUpdateService;
import cn.shopex.ecshopx.members.service.invoice.MemberInvoiceCreateService;
import cn.shopex.ecshopx.members.service.invoice.MemberInvoiceDeleteService;
import cn.shopex.ecshopx.members.service.invoice.MemberInvoiceGetService;
import cn.shopex.ecshopx.members.service.invoice.MemberInvoiceListService;
import cn.shopex.ecshopx.members.service.invoice.MemberInvoiceUpdateService;
import cn.shopex.ecshopx.members.service.h5.H5LoginOrchestrator;
import cn.shopex.ecshopx.members.service.h5.H5LoginRequestAssembler;
import cn.shopex.ecshopx.members.service.h5.H5MemberTokenRefreshService;
import cn.shopex.ecshopx.members.service.h5.MemberNoAuthDecryptPhoneService;
import cn.shopex.ecshopx.members.service.h5.bind.WxappBindSalespersonService;
import cn.shopex.ecshopx.members.service.h5.bind.WxappMemberBindFacade;
import cn.shopex.ecshopx.members.service.h5.bind.WxappSalespersonUniqueVisitoService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberEditInfoService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberGetMemberInfoService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberRegAgreementService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberRegSettingService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberSmsCodeSendService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberMobileChangeService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberNotUseValidationUpdateService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberDeleteMemberService;
import cn.shopex.ecshopx.members.service.h5.WxappMemberUpdateMemberService;
import cn.shopex.ecshopx.members.service.h5.register.WxappMemberCreatMemberFacade;
import cn.shopex.ecshopx.members.service.admin.AdminMemberRegisterSettingService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.wxapp.MemberBarcodeGenerateService;
import cn.shopex.ecshopx.members.service.wxapp.WxappGoodsArrivalNoticeSubscribeService;
import cn.shopex.ecshopx.members.service.wxapp.WxappMemberStatisticalService;
import cn.shopex.ecshopx.members.service.h5.dto.H5LoginAttemptResult;
import cn.shopex.ecshopx.members.service.h5.dto.H5LoginResponseData;
import cn.shopex.ecshopx.members.service.articlefav.MemberArticleFavAddService;
import cn.shopex.ecshopx.members.service.articlefav.MemberArticleFavGetInfoService;
import cn.shopex.ecshopx.members.service.articlefav.MemberArticleFavGetNumService;
import cn.shopex.ecshopx.members.service.articlefav.MemberArticleFavListService;
import cn.shopex.ecshopx.members.service.articlefav.MemberArticleFavRemoveService;
import cn.shopex.ecshopx.members.service.distributionfav.MemberDistributionFavAddService;
import cn.shopex.ecshopx.members.service.distributionfav.MemberDistributionFavCheckService;
import cn.shopex.ecshopx.members.service.distributionfav.MemberDistributionFavGetNumService;
import cn.shopex.ecshopx.members.service.distributionfav.MemberDistributionFavListService;
import cn.shopex.ecshopx.members.service.distributionfav.MemberDistributionFavRemoveService;
import cn.shopex.ecshopx.members.service.itemsfav.MemberItemsFavAddService;
import cn.shopex.ecshopx.members.service.itemsfav.MemberItemsFavGetNumService;
import cn.shopex.ecshopx.members.service.itemsfav.MemberItemsFavListService;
import cn.shopex.ecshopx.members.service.itemsfav.MemberItemsFavRemoveService;
import cn.shopex.ecshopx.members.service.browse.MemberBrowseHistoryListService;
import cn.shopex.ecshopx.members.service.browse.MemberBrowseHistorySaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("membersFrontV1Members")
@RequiredArgsConstructor
@RequestMapping("/api/v1/h5app")
public class MembersController {

	private static final Pattern ZH_NAME = Pattern.compile("^[a-z0-9A-Z\\u4e00-\\u9fa5]+$");
	private static final Pattern MOBILE_CN = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final Pattern POSITIVE_ITEM_ID = Pattern.compile("^[1-9]\\d*$");
	private static final Pattern LEADING_ZH_MARKET_CHARS =
			Pattern.compile("^市+", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern TRAILING_ZH_MARKET_CHARS =
			Pattern.compile("市+$", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern DELETE_ITEMS_FAV_ITEM_IDS_INDEXED =
			Pattern.compile("^item_ids\\[(\\d+)\\]$");

	private static final Set<String> MEMBER_IMAGE_VCODE_TYPES =
			Set.of("sign", "forgot_password", "login", "update", "merchant_login");

	private final H5LoginRequestAssembler h5LoginRequestAssembler;

	private final H5LoginOrchestrator h5LoginOrchestrator;

	private final H5MemberTokenRefreshService h5MemberTokenRefreshService;

	private final MemberAccountService memberAccountService;

	private final MemberAddressCreateService memberAddressCreateService;

	private final MemberAddressUpdateService memberAddressUpdateService;

	private final MemberAddressGetService memberAddressGetService;

	private final MemberAddressDeleteService memberAddressDeleteService;

	private final MemberAddressListService memberAddressListService;

	private final MemberInvoiceCreateService memberInvoiceCreateService;

	private final MemberInvoiceUpdateService memberInvoiceUpdateService;

	private final MemberInvoiceGetService memberInvoiceGetService;

	private final MemberInvoiceListService memberInvoiceListService;

	private final MemberInvoiceDeleteService memberInvoiceDeleteService;

	private final MemberBrowseHistorySaveService memberBrowseHistorySaveService;

	private final MemberBrowseHistoryListService memberBrowseHistoryListService;

	private final WhitelistSettingRedisService whitelistSettingRedisService;

	private final WxappMemberBindFacade wxappMemberBindFacade;

	private final WxappBindSalespersonService wxappBindSalespersonService;

	private final WxappSalespersonUniqueVisitoService wxappSalespersonUniqueVisitoService;

	private final WxappGoodsArrivalNoticeSubscribeService wxappGoodsArrivalNoticeSubscribeService;

	private final MemberBarcodeGenerateService memberBarcodeGenerateService;

	private final MembersMapper membersMapper;

	private final WxappMemberCreatMemberFacade wxappMemberCreatMemberFacade;

	private final WxappMemberUpdateMemberService wxappMemberUpdateMemberService;

	private final WxappMemberDeleteMemberService wxappMemberDeleteMemberService;

	private final WxappMemberNotUseValidationUpdateService wxappMemberNotUseValidationUpdateService;

	private final WxappMemberMobileChangeService wxappMemberMobileChangeService;

	private final WxappMemberGetMemberInfoService wxappMemberGetMemberInfoService;

	private final WxappMemberEditInfoService wxappMemberEditInfoService;

	private final WxappMemberRegAgreementService wxappMemberRegAgreementService;

	private final WxappMemberRegSettingService wxappMemberRegSettingService;

	private final WxappMemberStatisticalService wxappMemberStatisticalService;

	private final MemberArticleFavAddService memberArticleFavAddService;

	private final MemberArticleFavListService memberArticleFavListService;

	private final MemberArticleFavGetInfoService memberArticleFavGetInfoService;

	private final MemberArticleFavGetNumService memberArticleFavGetNumService;

	private final MemberArticleFavRemoveService memberArticleFavRemoveService;

	private final MemberDistributionFavAddService memberDistributionFavAddService;

	private final MemberDistributionFavListService memberDistributionFavListService;

	private final MemberDistributionFavCheckService memberDistributionFavCheckService;

	private final MemberDistributionFavGetNumService memberDistributionFavGetNumService;

	private final MemberDistributionFavRemoveService memberDistributionFavRemoveService;

	private final MemberItemsFavAddService memberItemsFavAddService;

	private final MemberItemsFavListService memberItemsFavListService;

	private final MemberItemsFavGetNumService memberItemsFavGetNumService;

	private final MemberItemsFavRemoveService memberItemsFavRemoveService;

	private final MemberAddressAreaService memberAddressAreaService;

	private final MemberNoAuthDecryptPhoneService memberNoAuthDecryptPhoneService;

	private final AdminMemberRegisterSettingService adminMemberRegisterSettingService;

	private final WxappMemberSmsCodeSendService wxappMemberSmsCodeSendService;

	private final LangueProperties langueProperties;

	@DataPass
	@GetMapping(
			value = "/wxapp/member",
			name = "会员详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getMemberInfo(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", defaultValue = "0") String activityId,
			@RequestParam(value = "code", required = false) String code) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long authUserId = parseOptionalAuthUserId(claims);
		Map<String, Object> data =
				wxappMemberGetMemberInfoService.getMemberInfo(
						companyId, authUserId, claims, activityId, code, request);
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/memberinfo",
			name = "会员编辑信息",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getMemberEditInfo(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		String lang = RequestLangTag.current(langueProperties);
		return ApiResult.ok(wxappMemberEditInfoService.getMemberEditInfo(companyId, userId, lang));
	}

	@PutMapping(value = "/wxapp/member", name = "更新会员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateMember(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		String acceptLanguage = RequestLangTag.current(langueProperties);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> mergedBody = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
		String country = nullToEmptyQuery(request.getParameter("country"));
		String province = nullToEmptyQuery(request.getParameter("province"));
		String city = nullToEmptyQuery(request.getParameter("city"));
		String language = nullToEmptyQuery(request.getParameter("language"));
		boolean isGetWxInfo = resolveIsGetWxInfoFlag(mergedBody, request);
		Map<String, Object> result =
				wxappMemberUpdateMemberService.updateMember(
						companyId,
						userId,
						claims,
						mergedBody,
						country,
						province,
						city,
						language,
						isGetWxInfo,
						acceptLanguage);
		return ApiResult.ok(result);
	}

	@PutMapping(value = "/wxapp/memberinfo", name = "更新会员免校验", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateMemberNotUseValidationConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		LinkedHashMap<String, Object> requestData = new LinkedHashMap<>();
		if (merged.containsKey("username")) {
			requestData.put("username", merged.get("username"));
		}
		if (merged.containsKey("avatar")) {
			requestData.put("avatar", merged.get("avatar"));
		}
		Map<String, Object> result =
				wxappMemberNotUseValidationUpdateService.updateMemberNotUseValidationConfig(
						companyId, userId, claims, requestData);
		return ApiResult.ok(result);
	}

	@PutMapping(value = "/wxapp/member/mobile", name = "更新手机", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateMemberMobile(
			HttpServletRequest request,
			@RequestParam(value = "old_mobile", required = false) String oldMobile,
			@RequestParam(value = "old_region_mobile", required = false) String oldRegionMobile,
			@RequestParam(value = "old_country_code", required = false) String oldCountryCode,
			@RequestParam(value = "new_mobile", required = false) String newMobile,
			@RequestParam(value = "new_region_mobile", required = false) String newRegionMobile,
			@RequestParam(value = "new_country_code", required = false) String newCountryCode,
			@RequestParam(value = "smsCode", required = false) String smsCode) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> data =
				wxappMemberMobileChangeService.updateMemberMobile(
						companyId,
						userId,
						nullToEmptyQuery(oldMobile),
						nullToEmptyQuery(oldRegionMobile),
						nullToEmptyQuery(oldCountryCode),
						nullToEmptyQuery(newMobile),
						nullToEmptyQuery(newRegionMobile),
						nullToEmptyQuery(newCountryCode),
						smsCode);
		return ApiResult.ok(data);
	}

	@GetMapping(value = "/wxapp/barcode", name = "会员码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getBarcode(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		Object claimRaw = claims.get("user_card_code");
		String normalized = null;
		if (!isAbsentUserCardCode(claimRaw)) {
			normalized = normalizeUserCardCodeOrNull(claimRaw);
			if (normalized == null) {
				return absentUserCardCode411();
			}
		}
		if (normalized == null) {
			Members row =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getUserId, userId)
									.select(Members::getUserCardCode)
									.last("LIMIT 1"));
			Object dbRaw = row != null ? row.getUserCardCode() : null;
			if (isAbsentUserCardCode(dbRaw)) {
				return absentUserCardCode411();
			}
			normalized = normalizeUserCardCodeOrNull(dbRaw);
			if (normalized == null) {
				return absentUserCardCode411();
			}
		}
		Map<String, String> payload = memberBarcodeGenerateService.generateBarCode(normalized);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(payload));
	}

	@GetMapping(
			value = "/wxapp/member/statistical",
			name = "会员统计",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getMemberStatistical(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		int page = parseItemsFavListPage(pageRaw);
		int pageSize = parseItemsFavListPageSize(pageSizeRaw);
		Map<String, Object> data =
				wxappMemberStatisticalService.getMemberStatistical(
						companyId, userId, page, pageSize, RequestLangTag.current(langueProperties));
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/member/addresslist",
			name = "地址列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getAddressList(
			HttpServletRequest request,
			@RequestParam(value = "address_id", required = false) String addressIdParam,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam,
			@RequestParam(value = "receipt_type", required = false) String receiptTypeParam,
			@RequestParam(value = "city", required = false) String cityParam,
			@RequestParam(value = "promoter_user_id", required = false) String promoterUserIdParam,
			@RequestParam(value = "buy_user_id", required = false) String buyUserIdParam) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long authUserId = resolveUserId(claims);

		Long addressIdOrNull = resolveAddressIdFilterOrNull(addressIdParam);

		String cityContainsOrNull = null;
		String receiptTrim = receiptTypeParam == null ? "" : receiptTypeParam.trim();
		if ("dada".equals(receiptTrim)) {
			String c = cityParam == null ? "" : cityParam.trim();
			if (StringUtils.hasText(c)) {
				cityContainsOrNull = trimLeadingTrailingShi(c);
			}
		}

		Long filterUserId;
		if (request.getParameterMap().containsKey("promoter_user_id")
				&& promoterUserIdIsTruthyForAddressList(promoterUserIdParam)
				&& promoterLooselyEqualsAuth(promoterUserIdParam, authUserId)) {
			filterUserId = parseBuyUserIdForAddressListNullable(request, buyUserIdParam);
		} else {
			filterUserId = Long.valueOf(authUserId);
		}

		int page = parseAddressListPageParam(request);
		int pageSize = parseAddressListPageSizeParam(request);

		Map<String, Object> data =
				memberAddressListService.getAddressList(
						companyId, filterUserId, addressIdOrNull, cityContainsOrNull, page, pageSize);
		return ApiResult.ok(data);
	}

	@PostMapping(
			value = "/wxapp/member/address",
			name = "添加地址",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createAddress(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;

		String username = trimToNull(b.get("username"));
		if (username == null || !ZH_NAME.matcher(username).matches()) {
			throw new ResourceException("请填写正确的收货人姓名");
		}
		String telephone = trimToNull(b.get("telephone"));
		if (telephone == null || !MOBILE_CN.matcher(telephone).matches()) {
			throw new ResourceException("请填写正确的手机号");
		}
		String province = trimToNull(b.get("province"));
		if (province == null || !ZH_NAME.matcher(province).matches()) {
			throw new ResourceException("请填写正确的省份");
		}
		String city = trimToNull(b.get("city"));
		if (city == null || !ZH_NAME.matcher(city).matches()) {
			throw new ResourceException("请填写正确的城市");
		}
		String county = trimToNull(b.get("county"));
		if (county == null || !ZH_NAME.matcher(county).matches()) {
			throw new ResourceException("请填写正确的区/县");
		}
		String adrdetail = trimToNull(b.get("adrdetail"));
		if (adrdetail == null) {
			throw new ResourceException("请填写正确的详细地址");
		}

		int isDef = resolveIsDefNormalized01(b);

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long authUserId = resolveUserId(claims);

		long effectiveUserId = authUserId;
		if (b.containsKey("promoter_user_id")
				&& promoterLooselyEqualsAuth(b.get("promoter_user_id"), authUserId)
				&& buyUserIdIsTruthy(b.get("buy_user_id"))) {
			long buyId = parsePositiveLongOrZero(b.get("buy_user_id"));
			if (buyId > 0L) {
				effectiveUserId = buyId;
			}
		}

		String postalCodeOrNull = trimToNull(b.get("postalCode"));
		String thirdDataOrNull = b.containsKey("third_data") ? trimToNull(b.get("third_data")) : null;

		Map<String, Object> result =
				memberAddressCreateService.createAddress(
						companyId,
						effectiveUserId,
						username,
						telephone,
						province,
						city,
						county,
						adrdetail,
						postalCodeOrNull,
						isDef,
						thirdDataOrNull);
		return ApiResult.ok(result);
	}

	@PutMapping(
			value = "/wxapp/member/address/{address_id}",
			name = "修改地址",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateAddress(
			HttpServletRequest request,
			@PathVariable("address_id") String addressId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;

		String rawId = addressId == null ? "" : addressId.trim();
		if (rawId.isEmpty()) {
			throw new ResourceException("缺少地址id");
		}
		if (!POSITIVE_ITEM_ID.matcher(rawId).matches()) {
			throw new ResourceException("地址ID格式不正确");
		}
		long parsedAddressId = Long.parseLong(rawId);

		String username = trimToNull(b.get("username"));
		if (username == null || !ZH_NAME.matcher(username).matches()) {
			throw new ResourceException("请填写正确的收货人姓名");
		}
		String telephone = trimToNull(b.get("telephone"));
		if (telephone == null || !MOBILE_CN.matcher(telephone).matches()) {
			throw new ResourceException("请填写正确的手机号");
		}
		String province = trimToNull(b.get("province"));
		if (province == null || !ZH_NAME.matcher(province).matches()) {
			throw new ResourceException("请填写正确的省份");
		}
		String city = trimToNull(b.get("city"));
		if (city == null || !ZH_NAME.matcher(city).matches()) {
			throw new ResourceException("请填写正确的城市");
		}
		String county = trimToNull(b.get("county"));
		if (county == null || !ZH_NAME.matcher(county).matches()) {
			throw new ResourceException("请填写正确的区/县");
		}
		String adrdetail = trimToNull(b.get("adrdetail"));
		if (adrdetail == null) {
			throw new ResourceException("请填写正确的详细地址");
		}

		boolean isDefKeyPresent = b.containsKey("is_def");
		boolean normalizedIsDef = false;
		if (isDefKeyPresent) {
			Object rawIsDef = b.get("is_def");
			if (rawIsDef instanceof Boolean bool) {
				normalizedIsDef = bool;
			} else if (rawIsDef instanceof Number n) {
				normalizedIsDef = (n.intValue() == 1);
			} else if (rawIsDef != null) {
				String t = String.valueOf(rawIsDef).trim();
				normalizedIsDef = "true".equals(t) || "1".equals(t);
			}
		}

		boolean postalKeyPresent = b.containsKey("postalCode");
		String postalVal = trimToNull(b.get("postalCode"));

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		return ApiResult.ok(
				memberAddressUpdateService.updateAddress(
						companyId,
						userId,
						parsedAddressId,
						username,
						telephone,
						province,
						city,
						county,
						adrdetail,
						postalKeyPresent,
						postalVal,
						isDefKeyPresent,
						normalizedIsDef));
	}

	@GetMapping(
			value = "/wxapp/member/address/{address_id}",
			name = "地址详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getAddress(
			HttpServletRequest request, @PathVariable("address_id") String addressId) {
		String rawId = addressId == null ? "" : addressId.trim();
		if (rawId.isEmpty()) {
			throw new ResourceException(
					"查询地址详情出错.",
					Map.of("address_id", List.of("address id 不能为空。")));
		}

		long parsedAddressId;
		if (POSITIVE_ITEM_ID.matcher(rawId).matches()) {
			parsedAddressId = Long.parseLong(rawId);
		} else {
			try {
				long v = Long.parseLong(rawId);
				if (v < 1L) {
					throw new ResourceException(
							"查询地址详情出错.",
							Map.of("address_id", List.of("address id 必须大于等于 1。")));
				}
				throw new ResourceException(
						"查询地址详情出错.",
						Map.of("address_id", List.of("address id 必须是整数。")));
			} catch (NumberFormatException e) {
				throw new ResourceException(
						"查询地址详情出错.",
						Map.of("address_id", List.of("address id 必须是整数。")));
			}
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> row = memberAddressGetService.getAddress(companyId, userId, parsedAddressId);
		if (row == null) {
			return ApiResult.ok(Collections.emptyList());
		}
		return ApiResult.ok(row);
	}

	@DeleteMapping(
			value = "/wxapp/member/address/{address_id}",
			name = "删除地址",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteAddress(
			HttpServletRequest request, @PathVariable("address_id") String addressId) {
		String rawId = addressId == null ? "" : addressId.trim();
		if (rawId.isEmpty()) {
			throw new ResourceException(
					"删除地址出错.",
					Map.of("address_id", List.of("address id 不能为空。")));
		}

		long parsedAddressId;
		if (POSITIVE_ITEM_ID.matcher(rawId).matches()) {
			parsedAddressId = Long.parseLong(rawId);
		} else {
			try {
				long v = Long.parseLong(rawId);
				if (v < 1L) {
					throw new ResourceException(
							"删除地址出错.",
							Map.of("address_id", List.of("address id 必须大于等于 1。")));
				}
				throw new ResourceException(
						"删除地址出错.",
						Map.of("address_id", List.of("address id 必须是整数。")));
			} catch (NumberFormatException e) {
				throw new ResourceException(
						"删除地址出错.",
						Map.of("address_id", List.of("address id 必须是整数。")));
			}
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		boolean status = memberAddressDeleteService.deleteAddress(companyId, userId, parsedAddressId);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", Boolean.valueOf(status));
		body.put("address_id", rawId);
		return ApiResult.ok(body);
	}

	@PostMapping(
			value = "/wxapp/member/collect/item/",
			name = "收藏商品（路径尾斜杠）",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> addItemsFavTrailingSlash() {
		throw new ResourceException("405 Method Not Allowed", 405);
	}

	@PostMapping(
			value = "/wxapp/member/collect/item/{item_id}",
			name = "收藏商品",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> addItemsFav(
			HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@RequestParam(value = "item_type", required = false) String itemTypeQuery,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String raw = itemId == null ? "" : itemId.trim();
		if (raw.isEmpty()) {
			throw new BadRequestException("没有选择收藏的商品");
		}
		long parsed;
		try {
			parsed = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ResourceException("商品不存在");
		}

		String normalizedItemType;
		if (itemTypeQuery != null && !itemTypeQuery.isBlank()) {
			normalizedItemType = itemTypeQuery.trim();
		} else {
			String fromBody = "";
			if (body != null && body.get("item_type") != null) {
				fromBody = Objects.toString(body.get("item_type"), "").trim();
			}
			normalizedItemType = fromBody.isEmpty() ? "normal" : fromBody;
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		Map<String, Object> data =
				memberItemsFavAddService.addItemsFav(companyId, userId, parsed, normalizedItemType);
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/member/collect/item",
			name = "商品收藏列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getItemsFavList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		int page = parseItemsFavListPage(pageRaw);
		int pageSize = parseItemsFavListPageSize(pageSizeRaw);
		Long distributorIdOrNull = null;
		if (isEffectiveDistributorIdQueryParam(distributorIdRaw)) {
			try {
				distributorIdOrNull = Long.parseLong(distributorIdRaw.trim());
			} catch (NumberFormatException e) {
				distributorIdOrNull = null;
			}
		}
		Map<String, Object> data =
				memberItemsFavListService.getItemsFavList(
						companyId, userId, page, pageSize, distributorIdOrNull, RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(
			value = "/wxapp/member/collect/item/num",
			name = "收藏数量",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getItemsFavNum(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		long total = memberItemsFavGetNumService.getItemsFavNum(companyId, userId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("fav_total_count", Long.valueOf(total));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DeleteMapping(
			value = "/wxapp/member/collect/item",
			name = "删除收藏商品",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteItemsFav(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;
		Object isEmptyRaw = b.containsKey("is_empty") ? b.get("is_empty") : request.getParameter("is_empty");
		if (isEmptyRaw == null
				&& !b.containsKey("is_empty")
				&& request.getParameter("is_empty") == null) {
			throw new BadRequestException(
					"删除收藏商品出错.", Map.of("is_empty", List.of("validation.required")), 422);
		}
		if (b.containsKey("is_empty") && b.get("is_empty") == null) {
			throw new BadRequestException(
					"删除收藏商品出错.", Map.of("is_empty", List.of("validation.required")), 422);
		}
		if (isEmptyRaw instanceof Boolean) {
			throw new BadRequestException(
					"删除收藏商品出错.", Map.of("is_empty", List.of("validation.in")), 422);
		}
		if (!isDeleteItemsFavIsEmptyValueAllowed(isEmptyRaw)) {
			throw new BadRequestException(
					"删除收藏商品出错.", Map.of("is_empty", List.of("validation.in")), 422);
		}
		boolean isEmpty = "true".equalsIgnoreCase(String.valueOf(isEmptyRaw).trim());

		List<Object> boundIdsOrNull = null;
		if (!isEmpty) {
			Object itemIdsSource;
			String[] bracketVals = request.getParameterValues("item_ids[]");
			if (bracketVals != null && bracketVals.length > 0) {
				if (bracketVals.length == 1) {
					itemIdsSource = bracketVals[0] == null ? "" : bracketVals[0].trim();
				} else {
					itemIdsSource =
							List.copyOf(Arrays.stream(bracketVals).map(s -> s == null ? "" : s.trim()).toList());
				}
			} else {
				String single = request.getParameter("item_ids");
				if (single != null && !single.trim().isEmpty()) {
					itemIdsSource = single.trim();
				} else {
					itemIdsSource = b.get("item_ids");
					if (isArticleIdRequiredFailure(itemIdsSource)) {
						List<String> indexed = collectDeleteItemsFavIndexedItemIds(request, b);
						if (!indexed.isEmpty()) {
							itemIdsSource = indexed;
						}
					}
				}
			}
			if (isArticleIdRequiredFailure(itemIdsSource)) {
				throw new BadRequestException(
						"删除收藏商品出错.", Map.of("item_ids", List.of("validation.required_if")), 422);
			}
			boundIdsOrNull = buildItemIdsBoundListForDeleteItemsFav(itemIdsSource);
			if (boundIdsOrNull.isEmpty()) {
				throw new BadRequestException(
						"删除收藏商品出错.", Map.of("item_ids", List.of("validation.required_if")), 422);
			}
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		int rows = memberItemsFavRemoveService.removeItemsFav(companyId, userId, isEmpty, boundIdsOrNull);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Integer.valueOf(rows));
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/member/browse/history/list",
			name = "浏览记录",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getBrowseHistory(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveBrowseHistoryCompanyId(request, claims);
		long userId = resolveBrowseHistoryUserId(request, claims);
		int page = parseBrowseHistoryPage(pageRaw);
		int pageSize = parseBrowseHistoryPageSize(pageSizeRaw);
		String languageTag = RequestLangTag.current(langueProperties);
		return ApiResult.ok(
				memberBrowseHistoryListService.getBrowseHistory(companyId, userId, page, pageSize, languageTag));
	}

	@PostMapping(
			value = "/wxapp/member/browse/history/save",
			name = "保存浏览",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> saveBrowseHistory(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;
		Object fromBody = b.get("item_id");
		String fromQuery = request.getParameter("item_id");
		String raw = trimToNull(fromBody);
		if (raw == null) {
			raw = trimToNull(fromQuery);
		}
		if (raw == null) {
			throw new ResourceException("参数错误", Map.of("item_id", List.of("validation.required")));
		}
		long requestItemId = parseBrowseHistoryItemIdLenient(raw);

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		boolean ok = memberBrowseHistorySaveService.saveBrowseHistory(companyId, userId, requestItemId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", ok);
		return ApiResult.ok(data);
	}

	@PostMapping(
			value = "/wxapp/member/collect/article/{article_id}",
			name = "心愿单添加",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> addArticleFav(
			HttpServletRequest request, @PathVariable("article_id") String articleId) {
		String raw = articleId == null ? "" : articleId.trim();
		if (raw.isEmpty()) {
			throw new BadRequestException("没有选择收藏的心愿单");
		}
		long parsedArticleId;
		try {
			parsedArticleId = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("没有选择收藏的心愿单");
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		Map<String, Object> data =
				memberArticleFavAddService.addArticleFav(companyId, userId, parsedArticleId);
		return ApiResult.ok(data);
	}

	@DeleteMapping(
			value = "/wxapp/member/collect/article",
			name = "心愿单删除",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteArticleFav(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;
		Object v = b.containsKey("article_id") ? b.get("article_id") : request.getParameter("article_id");
		if (isArticleIdRequiredFailure(v)) {
			throw new BadRequestException(
					"删除收藏心愿单出错.",
					Map.of("article_id", List.of("validation.required")),
					422);
		}
		long parsedArticleId = parseArticleIdForDeleteArticleFav(v);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		memberArticleFavRemoveService.deleteArticleFav(companyId, userId, parsedArticleId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/member/collect/article",
			name = "心愿单列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getArticleFavList(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		int page = parseArticleFavListPage(request.getParameter("page"));
		int pageSize = parseArticleFavListPageSize(request.getParameter("pageSize"));
		String requestLang = RequestLangTag.current(langueProperties);
		Object body =
				memberArticleFavListService.getArticleFavList(
						companyId, userId, page, pageSize, requestLang);
		return ApiResult.ok(body);
	}

	@GetMapping(
			value = "/wxapp/member/collect/article/num",
			name = "心愿单总数",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getArticleFavNum(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		long total = memberArticleFavGetNumService.getArticleFavNum(companyId, userId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", Long.valueOf(total));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(
			value = "/wxapp/member/collect/article/info",
			name = "心愿单查询",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getArticleFavInfo(HttpServletRequest request) {
		if (request.getParameter("article_id") == null) {
			return ApiResult.ok(Collections.emptyList());
		}
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		Map<String, Object> merged = new LinkedHashMap<>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			merged.put(name, request.getParameter(name));
		}
		if (request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID) != null) {
			merged.put("company_id", request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		merged.putAll(claims);
		if (!merged.containsKey("user_id") || !merged.containsKey("company_id")) {
			throw new ResourceException("获取用户信息失败");
		}
		long userId = parseMergedAuthBusinessIdForArticleFav(merged, "user_id");
		long companyId = parseMergedAuthBusinessIdForArticleFav(merged, "company_id");
		String requestLang = RequestLangTag.current(langueProperties);
		Object body =
				memberArticleFavGetInfoService.getArticleFavInfo(
						companyId, userId, request.getParameter("article_id"), requestLang);
		return ApiResult.ok(body);
	}

	@PostMapping(
			value = "/wxapp/member/collect/distribution/{distributor_id}",
			name = "收藏店铺",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> addDistributionFav(
			HttpServletRequest request, @PathVariable("distributor_id") String distributorId) {
		String raw = distributorId == null ? "" : distributorId.trim();
		if (raw.isEmpty()) {
			throw new ResourceException("没有选择收藏的店铺");
		}
		long parsedDistributorId;
		try {
			parsedDistributorId = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ResourceException("店铺信息有误");
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		Map<String, Object> data =
				memberDistributionFavAddService.addDistributionFav(companyId, userId, parsedDistributorId);
		return ApiResult.ok(data);
	}

	@DeleteMapping(
			value = "/wxapp/member/collect/distribution",
			name = "取消收藏店铺",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteDistributionFav(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;
		Object v = b.containsKey("distributor_id") ? b.get("distributor_id") : request.getParameter("distributor_id");
		if (isDistributorIdRequiredFailure(v)) {
			throw new BadRequestException(
					"删除收藏店铺出错.",
					Map.of("distributor_id", List.of("validation.required")),
					422);
		}
		Object distributorIdForEq = resolveDistributorIdForDeleteDistributionFav(v);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		memberDistributionFavRemoveService.deleteDistributionFav(companyId, userId, distributorIdForEq);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/member/collect/distribution",
			name = "收藏店铺列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getDistributionFavList(HttpServletRequest request) {
		Map<String, Object> merged = new LinkedHashMap<>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			merged.put(name, request.getParameter(name));
		}
		if (request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID) != null) {
			merged.put("company_id", request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		merged.putAll(readH5AuthClaimsMap(request));
		if (!merged.containsKey("user_id") || !merged.containsKey("company_id")) {
			throw new ResourceException("获取用户信息失败");
		}
		if (merged.get("user_id") == null || merged.get("company_id") == null) {
			throw new ResourceException("获取用户信息失败");
		}
		long userId = parseMergedAuthBusinessIdForArticleFav(merged, "user_id");
		long companyId = parseMergedAuthBusinessIdForArticleFav(merged, "company_id");
		int page = parseDistributionFavListPage(request.getParameter("page"));
		int pageSize = parseDistributionFavListPageSize(request.getParameter("pageSize"));
		String requestLang = RequestLangTag.current(langueProperties);
		Object body =
				memberDistributionFavListService.getDistributionFavList(
						companyId, userId, page, pageSize, requestLang);
		return ApiResult.ok(body);
	}

	@GetMapping(
			value = "/wxapp/member/collect/distribution/num",
			name = "收藏店铺数",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getDistributionFavNum(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long total;
		if (isEffectiveDistributorIdQueryParam(distributorId)) {
			String t = distributorId.trim();
			try {
				long distId = Long.parseLong(t);
				total =
						memberDistributionFavGetNumService.getDistributionFavNum(
								companyId, 0L, Long.valueOf(distId));
			} catch (NumberFormatException e) {
				total = 0L;
			}
		} else {
			long userId = resolveUserId(claims);
			total = memberDistributionFavGetNumService.getDistributionFavNum(companyId, userId, null);
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", Long.valueOf(total));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(
			value = "/wxapp/member/collect/distribution/check",
			name = "是否收藏店铺",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> checkDistributionFav(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		String rawDist = distributorId == null ? "" : distributorId.trim();
		if (rawDist.isEmpty()) {
			return softFalseIsFav();
		}
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		if (isEmptyUserIdForDistributionFavCheck(claims.get("user_id"))) {
			return softFalseIsFav();
		}
		long distId;
		try {
			distId = Long.parseLong(rawDist);
		} catch (NumberFormatException e) {
			return softFalseIsFav();
		}
		long userId;
		Object rawUser = claims.get("user_id");
		if (rawUser instanceof Number n) {
			userId = n.longValue();
		} else if (rawUser instanceof String s) {
			try {
				userId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return softFalseIsFav();
			}
		} else {
			return softFalseIsFav();
		}
		long companyId = resolveCompanyId(request, claims);
		boolean fav =
				memberDistributionFavCheckService.checkDistributionFav(companyId, userId, distId);
		return ApiResult.ok(isFavResponseBody(fav));
	}

	@PostMapping(
			value = "/wxapp/member/subscribe/item/{item_id}",
			name = "缺货订阅",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> itemsSubscribe(
			HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@RequestParam(name = "distributor_id", defaultValue = "0") String distributorIdParam) {
		long parsedItemId = parseSubscribeItemId(itemId);
		int distributorId = parseDistributorIdParam(distributorIdParam);

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		String openId = Objects.toString(claims.get("open_id"), "").trim();
		Object wxaAppid = claims.get("wxapp_appid");
		String source = "wechat";
		if (StringUtils.hasText(String.valueOf(claims.getOrDefault("alipay_user_id", "")).trim())) {
			openId = String.valueOf(claims.get("alipay_user_id")).trim();
			source = "alipay";
		}

		String acceptLanguage = RequestLangTag.current(langueProperties);
		Object data =
				wxappGoodsArrivalNoticeSubscribeService.itemsSubscribe(
						companyId,
						userId,
						openId,
						wxaAppid,
						source,
						distributorId,
						parsedItemId,
						acceptLanguage);
		return ApiResult.ok(data);
	}

	@PostMapping(
			value = "/wxapp/member/bindSalesperson",
			name = "绑定导购",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> bindSalesperson(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		String workUserid = trimToNull(merged.get("work_userid"));
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		String unionid = Objects.toString(claims.get("unionid"), "").trim();
		String mobile = Objects.toString(claims.get("mobile"), "").trim();
		return ApiResult.ok(wxappBindSalespersonService.bindSalesperson(companyId, userId, unionid, mobile, workUserid));
	}

	@PostMapping(
			value = "/wxapp/member/salesperson/uniquevisito",
			name = "导购UV",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> salespersonUniqueVisito(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		String workUserid = trimToNull(merged.get("work_userid"));
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String unionid = Objects.toString(claims.get("unionid"), "").trim();
		if (workUserid == null) {
			throw new ResourceException("导购员工编号不能为空");
		}
		wxappSalespersonUniqueVisitoService.salespersonUniqueVisito(companyId, workUserid, unionid);
		return ApiResult.ok(Map.of("status", true));
	}

	@DeleteMapping(value = "/wxapp/member", name = "会员注销", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteMember(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		long companyId = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		if (companyId <= 0L) {
			companyId = parsePositiveLongOrZero(readH5AuthClaimsMap(request).get("company_id"));
		}
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		String mobile = Objects.toString(claims.get("mobile"), "").trim();
		if (companyId <= 0L || userId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		if (!StringUtils.hasText(mobile)) {
			mobile = memberAccountService.resolvePlainMobileForMember(companyId, userId).trim();
		}
		if (!StringUtils.hasText(mobile)) {
			throw new UnauthorizedException("未登录");
		}
		String protocolLang = RequestLangTag.current(langueProperties);
		boolean isDeleteConfirmed = ValuePresence.hasEffectiveValue(merged.get("is_delete"));
		Map<String, Object> data =
				wxappMemberDeleteMemberService.deleteMember(
						companyId, userId, mobile, isDeleteConfirmed, protocolLang);
		return ApiResult.ok(data);
	}

	@GetMapping(
			value = "/wxapp/member/invoicelist",
			name = "发票列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getInvoiceList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		int page = parseAddressListPageParam(request);
		int pageSize = parseAddressListPageSizeParam(request);
		return ApiResult.ok(memberInvoiceListService.getInvoiceList(companyId, userId, page, pageSize));
	}

	@PostMapping(
			value = "/wxapp/member/invoice",
			name = "添加发票",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createInvoice(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;

		String invoicesType = trimToNull(b.get("invoices_type"));
		if (!"personal".equals(invoicesType) && !"corporate".equals(invoicesType)) {
			throw new ResourceException("请选择抬头类型");
		}

		String name = trimToNull(b.get("name"));
		if (name == null) {
			throw new ResourceException("请填写发票抬头");
		}

		String telephoneOrNull = trimToNull(b.get("telephone"));
		String taxNumberOrNull = trimToNull(b.get("tax_number"));
		String businessAddressOrNull = trimToNull(b.get("business_address"));
		String bankOrNull = trimToNull(b.get("bank"));
		String bankAccountOrNull = trimToNull(b.get("bank_account"));

		if ("corporate".equals(invoicesType)) {
			if (telephoneOrNull == null) {
				throw new ResourceException("请填写正确的手机号");
			}
			if (taxNumberOrNull == null) {
				throw new ResourceException("请填写税号");
			}
			if (businessAddressOrNull == null) {
				throw new ResourceException("请填写详细地址");
			}
			if (bankOrNull == null) {
				throw new ResourceException("请填写银行");
			}
			if (bankAccountOrNull == null) {
				throw new ResourceException("请填写银行账号");
			}
		}

		int isDef = resolveIsDefNormalized01(b);

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		Map<String, Object> result =
				memberInvoiceCreateService.createInvoice(
						companyId,
						userId,
						invoicesType,
						name,
						telephoneOrNull,
						taxNumberOrNull,
						businessAddressOrNull,
						bankOrNull,
						bankAccountOrNull,
						isDef);
		return ApiResult.ok(result);
	}

	@PutMapping(
			value = "/wxapp/member/invoice/{invoice_id}",
			name = "修改发票",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateInvoice(
			HttpServletRequest request,
			@PathVariable("invoice_id") String invoiceId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long invoicesId = parseInvoiceIdForUpdate(invoiceId);
		Map<String, Object> b = body == null ? Collections.emptyMap() : body;

		String invoicesType = trimToNull(b.get("invoices_type"));
		if (!"personal".equals(invoicesType) && !"corporate".equals(invoicesType)) {
			throw new ResourceException("请选择抬头类型");
		}

		String name = trimToNull(b.get("name"));
		if (name == null) {
			throw new ResourceException("请填写发票抬头");
		}

		String telephoneOrNull = trimToNull(b.get("telephone"));
		String taxNumberOrNull = trimToNull(b.get("tax_number"));
		String businessAddressOrNull = trimToNull(b.get("business_address"));
		String bankOrNull = trimToNull(b.get("bank"));
		String bankAccountOrNull = trimToNull(b.get("bank_account"));

		if ("corporate".equals(invoicesType)) {
			if (telephoneOrNull == null) {
				throw new ResourceException("请填写正确的手机号");
			}
			if (taxNumberOrNull == null) {
				throw new ResourceException("请填写税号");
			}
			if (businessAddressOrNull == null) {
				throw new ResourceException("请填写详细地址");
			}
			if (bankOrNull == null) {
				throw new ResourceException("请填写银行");
			}
			if (bankAccountOrNull == null) {
				throw new ResourceException("请填写银行账号");
			}
		}

		LinkedHashMap<String, Object> patchBase = new LinkedHashMap<>();
		patchBase.put("invoices_type", invoicesType);
		patchBase.put("name", name);
		if ("corporate".equals(invoicesType)) {
			patchBase.put("telephone", telephoneOrNull);
			patchBase.put("tax_number", taxNumberOrNull);
			patchBase.put("business_address", businessAddressOrNull);
			patchBase.put("bank", bankOrNull);
			patchBase.put("bank_account", bankAccountOrNull);
		} else {
			if (b.containsKey("telephone")) {
				patchBase.put("telephone", trimToNull(b.get("telephone")));
			}
			if (b.containsKey("tax_number")) {
				patchBase.put("tax_number", trimToNull(b.get("tax_number")));
			}
			if (b.containsKey("business_address")) {
				patchBase.put("business_address", trimToNull(b.get("business_address")));
			}
			if (b.containsKey("bank")) {
				patchBase.put("bank", trimToNull(b.get("bank")));
			}
			if (b.containsKey("bank_account")) {
				patchBase.put("bank_account", trimToNull(b.get("bank_account")));
			}
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);

		Map<String, Object> result =
				memberInvoiceUpdateService.updateInvoice(companyId, userId, invoicesId, b, patchBase);
		return ApiResult.ok(result);
	}

	@GetMapping(
			value = "/wxapp/member/invoice/{invoice_id}",
			name = "发票详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getInvoice(
			HttpServletRequest request, @PathVariable("invoice_id") String invoiceId) {
		String rawId = invoiceId == null ? "" : invoiceId.trim();
		if (rawId.isEmpty()) {
			throw new ResourceException(
					"查询发票详情出错.",
					Map.of("invoices_id", List.of("invoices id 不能为空。")));
		}

		long parsedInvoicesId;
		if (POSITIVE_ITEM_ID.matcher(rawId).matches()) {
			parsedInvoicesId = Long.parseLong(rawId);
		} else {
			try {
				long v = Long.parseLong(rawId);
				if (v < 1L) {
					throw new ResourceException(
							"查询发票详情出错.",
							Map.of("invoices_id", List.of("invoices id 必须大于等于 1。")));
				}
				throw new ResourceException(
						"查询发票详情出错.",
						Map.of("invoices_id", List.of("invoices id 必须是整数。")));
			} catch (NumberFormatException e) {
				throw new ResourceException(
						"查询发票详情出错.",
						Map.of("invoices_id", List.of("invoices id 必须是整数。")));
			}
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		Map<String, Object> row =
				memberInvoiceGetService.getInvoice(companyId, userId, parsedInvoicesId);
		if (row == null) {
			return ApiResult.ok(Collections.emptyList());
		}
		return ApiResult.ok(row);
	}

	@DeleteMapping(
			value = "/wxapp/member/invoice/{invoice_id}",
			name = "删除发票",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteInvoice(
			HttpServletRequest request, @PathVariable("invoice_id") String invoiceId) {
		String rawId = invoiceId == null ? "" : invoiceId.trim();
		if (rawId.isEmpty()) {
			throw new ResourceException(
					"删除发票信息出错.",
					Map.of("invoices_id", List.of("invoices id 不能为空。")));
		}

		long parsedInvoicesId;
		if (POSITIVE_ITEM_ID.matcher(rawId).matches()) {
			parsedInvoicesId = Long.parseLong(rawId);
		} else {
			try {
				long v = Long.parseLong(rawId);
				if (v < 1L) {
					throw new ResourceException(
							"删除发票信息出错.",
							Map.of("invoices_id", List.of("invoices id 必须大于等于 1。")));
				}
				throw new ResourceException(
						"删除发票信息出错.",
						Map.of("invoices_id", List.of("invoices id 必须是整数。")));
			} catch (NumberFormatException e) {
				throw new ResourceException(
						"删除发票信息出错.",
						Map.of("invoices_id", List.of("invoices id 必须是整数。")));
			}
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = resolveUserId(claims);
		memberInvoiceDeleteService.deleteInvoice(companyId, userId, parsedInvoicesId);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", Boolean.TRUE);
		body.put("invoices_id", rawId);
		return ApiResult.ok(body);
	}

	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member",
			name = "注册会员",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> creatMember(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		long cid = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		merged.put("company_id", cid);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		mergeH5ClaimsIntoCreatMemberPayload(merged, claims);
		applyWxappIdentifiersFromH5ClaimsOnly(merged, claims);
		if (merged.containsKey("user_name") && !merged.containsKey("username")) {
			Object un = merged.get("user_name");
			if (un != null) {
				String t = String.valueOf(un).trim();
				if (StringUtils.hasText(t)) {
					merged.put("username", t);
				}
			}
		}
		return wxappMemberCreatMemberFacade.creatMember(merged);
	}

	@FrontNoAuth
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@GetMapping(
			value = "/wxapp/member/setting",
			name = "会员配置",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> getRegSetting(
			HttpServletRequest request,
			@RequestParam(value = "is_edite_page", required = false) String isEditePage) {
		boolean edited = parseH5IsEditePageTruthy(isEditePage);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForUnauthorizedBody(request, claims);
		String acceptLanguage = RequestLangTag.current(langueProperties);
		Object data = wxappMemberRegSettingService.getRegSetting(companyId, edited, acceptLanguage);
		return ApiResult.ok(data);
	}

	@FrontNoAuth
	@GetMapping(
			value = "/wxapp/member/agreement",
			name = "注册协议",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getRegAgreementSetting(
			HttpServletRequest request,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		Map<String, Object> data =
				wxappMemberRegAgreementService.getRegAgreementSetting(companyId, countryCode);
		return ApiResult.ok(data);
	}

	@FrontNoAuth
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@GetMapping(
			value = "/wxapp/member/sms/code",
			name = "短信码",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getSmsCode(
			HttpServletRequest request,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "token", required = false) String token,
			@RequestParam(value = "yzm", required = false) String yzm) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long authUserId = parsePositiveLongOrZero(claims.get("user_id"));
		wxappMemberSmsCodeSendService.getSmsCode(companyId, authUserId, claims, mobile, type, token, yzm);
		return ApiResult.ok(Map.of("message", "短信发送成功"));
	}

	@FrontNoAuth
	@GetMapping(
			value = "/wxapp/member/image/code",
			name = "图片码",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, String>> getImageVcode(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String typeRaw) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String type =
				(typeRaw != null && StringUtils.hasText(typeRaw.trim())) ? typeRaw.trim() : "sign";
		if (!MEMBER_IMAGE_VCODE_TYPES.contains(type)) {
			throw new BadRequestException("图片验证码类型错误");
		}
		Map<String, String> data = adminMemberRegisterSettingService.generateImageVcode(companyId, type);
		return ApiResult.ok(data);
	}

	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/reset/password",
			name = "重置密码",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> resetMemberPassword(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		merged.put("mobile", coerceH5MobileInput(merged.get("mobile")));
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long authUserId = parsePositiveLongOrZero(claims.get("user_id"));
		Map<String, Object> result =
				memberAccountService.resetMemberPassword(companyId, authUserId, merged);
		return ApiResult.ok(result);
	}

	@FrontNoAuth
	@GetMapping(
			value = "/wxapp/whitelist/status",
			name = "白名单状态",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getWhitelistStatus(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForUnauthorizedBody(request, claims);
		Object status = whitelistSettingRedisService.getWhitelistStatusFieldValue(companyId);
		return ApiResult.ok(Map.of("status", status));
	}

	@FrontNoAuth
	@GetMapping(
			value = "/wxapp/member/item/is_subscribe/{item_id}",
			name = "是否订阅商品",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Object> IsSubscribe(
			HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@RequestParam(name = "distributor_id", defaultValue = "0") String distributorIdParam) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		if (isEmptyUserIdForDistributionFavCheck(claims.get("user_id"))) {
			return ApiResult.ok(Collections.emptyList());
		}
		int distributorId = parseDistributorIdLenientForIsSubscribe(distributorIdParam);
		String source = "wechat";
		if (StringUtils.hasText(String.valueOf(claims.getOrDefault("alipay_user_id", "")).trim())) {
			source = "alipay";
		}
		long userId = resolveUserId(claims);
		Object data =
				wxappGoodsArrivalNoticeSubscribeService.isSubscribe(
						companyId,
						userId,
						source,
						distributorId,
						itemId,
						RequestLangTag.current(langueProperties));
		return ApiResult.ok(data);
	}

	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/is_new",
			name = "是否新会员",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> isNewMember(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		String mobileNorm = coerceH5MobileInput(merged.get("mobile"));
		long companyId = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		boolean mobileOk = StringUtils.hasText(mobileNorm) && !"0".equals(mobileNorm);
		int isNew;
		if (companyId > 0L && mobileOk) {
			Map<String, Object> userInfo = memberAccountService.getInfoByMobile(companyId, mobileNorm);
			isNew = (userInfo == null || userInfo.isEmpty()) ? 1 : 0;
		} else {
			isNew = 1;
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("is_new", isNew);
		return ApiResult.ok(data);
	}

	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/bind",
			name = "绑定会员",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<H5LoginResponseData>> bindMember(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		long cid = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		merged.put("company_id", cid);
		String token = wxappMemberBindFacade.bindMember(merged);
		return ResponseEntity.ok(ApiResult.ok(new H5LoginResponseData(token)));
	}

	@FrontNoAuth
	@GetMapping(
			value = "/wxapp/member/addressarea",
			name = "地区json",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getAddressArea() {
		return ResponseEntity.ok(ApiResult.ok(memberAddressAreaService.getAddressArea()));
	}

	@FrontNoAuth
	@GetMapping(
			value = "/wxapp/member/decryptPhone",
			name = "解密手机",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getNoAuthDecryptPhoneNumber(
			@RequestParam(value = "appid", required = false) String appid,
			@RequestParam(value = "code", required = false) String code,
			@RequestParam(value = "encryptedData", required = false) String encryptedData,
			@RequestParam(value = "iv", required = false) String iv) {
		return ResponseEntity.ok(
				ApiResult.ok(
						memberNoAuthDecryptPhoneService.getNoAuthDecryptPhoneNumber(
								appid, code, encryptedData, iv)));
	}

	@FrontNoAuth
	@PostMapping(value = "/wxapp/login", name = "C端登录")
	public ResponseEntity<ApiResult<H5LoginResponseData>> login(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> credentials = h5LoginRequestAssembler.merge(request, body);
		H5LoginAttemptResult r = h5LoginOrchestrator.attempt(credentials);
		if (r.success()) {
			return ResponseEntity.ok(ApiResult.ok(new H5LoginResponseData(r.jwtToken())));
		}
		if (r.credentialsRejected()) {
			return ResponseEntity.ok(ApiResult.ok(new H5LoginResponseData(Boolean.FALSE)));
		}
		return ResponseEntity.ok(ApiResult.ok(new H5LoginResponseData(Boolean.FALSE)));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/token/refresh", name = "C端刷新token")
	public ResponseEntity<ApiResult<Map<String, Boolean>>> tokenRefresh(HttpServletRequest request) {
		String newJwt = h5MemberTokenRefreshService.tokenRefresh(request);
		return ResponseEntity.ok()
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + newJwt)
				.body(ApiResult.ok(Map.of("result", Boolean.TRUE)));
	}

	private static void mergeH5ClaimsIntoCreatMemberPayload(
			Map<String, Object> merged, Map<String, Object> claims) {
		if (claims == null || claims.isEmpty()) {
			if (!merged.containsKey("api_from")
					|| !StringUtils.hasText(String.valueOf(merged.get("api_from")).trim())) {
				merged.put("api_from", "h5app");
			}
			return;
		}
		putIfAbsentNonBlank(merged, "user_id", claims.get("user_id"));
		putIfAbsentNonBlank(merged, "unionid", claims.get("unionid"));
		putIfAbsentNonBlank(merged, "open_id", claims.get("open_id"));
		if (!merged.containsKey("open_id") || !StringUtils.hasText(String.valueOf(merged.get("open_id")).trim())) {
			putIfAbsentNonBlank(merged, "open_id", claims.get("openid"));
		}
		putIfAbsentNonBlank(merged, "wxapp_appid", claims.get("wxapp_appid"));
		putIfAbsentNonBlank(merged, "wxa_appid", claims.get("wxa_appid"));
		putIfAbsentNonBlank(merged, "woa_appid", claims.get("woa_appid"));
		putIfAbsentNonBlank(merged, "authorizer_appid", claims.get("authorizer_appid"));
		if (!merged.containsKey("api_from")
				|| !StringUtils.hasText(String.valueOf(merged.get("api_from")).trim())) {
			Object af = claims.get("api_from");
			String apiFrom =
					af != null && StringUtils.hasText(String.valueOf(af).trim())
							? String.valueOf(af).trim()
							: "h5app";
			merged.put("api_from", apiFrom);
		}
		if (!merged.containsKey("auth_type")
				|| !StringUtils.hasText(String.valueOf(merged.get("auth_type")).trim())) {
			putIfAbsentNonBlank(merged, "auth_type", claims.get("auth_type"));
		}
		if (!merged.containsKey("sex")) {
			merged.put("sex", claims.get("sex") != null ? claims.get("sex") : 0);
		}
		if (!merged.containsKey("avatar")
				|| !StringUtils.hasText(String.valueOf(merged.get("avatar")).trim())) {
			putIfAbsentNonBlank(merged, "avatar", claims.get("headimgurl"));
		}
		if (!merged.containsKey("inviter_id")) {
			putIfAbsentNonBlank(merged, "inviter_id", claims.get("inviter_id"));
		}
		if (!merged.containsKey("source_from")) {
			putIfAbsentNonBlank(merged, "source_from", claims.get("source_from"));
		}
	}

	private static void applyWxappIdentifiersFromH5ClaimsOnly(
			Map<String, Object> merged, Map<String, Object> claims) {
		String authType = Objects.toString(merged.get("auth_type"), "").trim();
		if (!"wxapp".equals(authType)) {
			return;
		}
		Map<String, Object> c = claims != null ? claims : Collections.emptyMap();
		putOrRemoveFromClaimsTrimmed(merged, c, "unionid", "unionid");
		String openFromClaims = trimToNull(c.get("open_id"));
		if (openFromClaims == null) {
			openFromClaims = trimToNull(c.get("openid"));
		}
		if (openFromClaims != null) {
			merged.put("open_id", openFromClaims);
		} else {
			merged.remove("open_id");
			merged.remove("openid");
		}
		putOrRemoveFromClaimsTrimmed(merged, c, "wxapp_appid", "wxapp_appid");
		putOrRemoveFromClaimsTrimmed(merged, c, "wxa_appid", "wxa_appid");
		putOrRemoveFromClaimsTrimmed(merged, c, "woa_appid", "woa_appid");
		putOrRemoveFromClaimsTrimmed(merged, c, "authorizer_appid", "authorizer_appid");
	}

	private static void putOrRemoveFromClaimsTrimmed(
			Map<String, Object> merged, Map<String, Object> claims, String payloadKey, String claimsKey) {
		String v = trimToNull(claims.get(claimsKey));
		if (v != null) {
			merged.put(payloadKey, v);
		} else {
			merged.remove(payloadKey);
		}
	}

	private static void putIfAbsentNonBlank(Map<String, Object> m, String key, Object fromClaims) {
		Object cur = m.get(key);
		if (cur != null && StringUtils.hasText(String.valueOf(cur).trim())) {
			return;
		}
		if (fromClaims == null) {
			return;
		}
		String t = String.valueOf(fromClaims).trim();
		if (StringUtils.hasText(t)) {
			m.put(key, t);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	/**
	 * {@code distributor_id} 查询参数是否在「按店铺维度计数」分支下视为有效（非空、非零字符串）。
	 */
	private static boolean isEffectiveDistributorIdQueryParam(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equals(t);
	}

	/**
	 * JWT {@code user_id} 是否应按「未登录/无效用户」处理为未收藏（HTTP 200、{@code is_fav=false}）。
	 */
	private static boolean isEmptyUserIdForDistributionFavCheck(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return !Boolean.TRUE.equals(b);
		}
		if (raw instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (raw instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (raw instanceof Object[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof int[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof long[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof short[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof byte[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof char[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof float[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof double[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof boolean[] arr) {
			return arr.length == 0;
		}
		return false;
	}

	private static ApiResult<Map<String, Object>> softFalseIsFav() {
		return ApiResult.ok(isFavResponseBody(false));
	}

	private static Map<String, Object> isFavResponseBody(boolean fav) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("is_fav", Boolean.valueOf(fav));
		return m;
	}

	private static boolean isAbsentUserCardCode(Object raw) {
		if (raw instanceof Object[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof int[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof long[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof short[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof byte[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof char[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof float[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof double[] arr) {
			return arr.length == 0;
		}
		if (raw instanceof boolean[] arr) {
			return arr.length == 0;
		}
		return !ValuePresence.hasEffectiveValue(raw);
	}

	private static String normalizeUserCardCodeOrNull(Object raw) {
		if (raw instanceof String s) {
			return s.trim();
		}
		if (raw instanceof Number n) {
			return numberClaimToPlainDecimalString(n);
		}
		if (raw instanceof Boolean b) {
			return Boolean.TRUE.equals(b) ? "1" : null;
		}
		if (raw != null && raw.getClass().isArray()) {
			return null;
		}
		if (raw instanceof Map<?, ?> m && !m.isEmpty()) {
			return null;
		}
		if (raw instanceof Collection<?> c && !c.isEmpty()) {
			return null;
		}
		return null;
	}

	private static String numberClaimToPlainDecimalString(Number n) {
		if (n instanceof BigDecimal bd) {
			return bd.stripTrailingZeros().toPlainString();
		}
		if (n instanceof BigInteger bi) {
			return bi.toString();
		}
		if (n instanceof Double || n instanceof Float) {
			double d = n.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return null;
			}
			if (d == Math.rint(d)) {
				return Long.toString((long) d);
			}
			return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
		}
		return Long.toString(n.longValue());
	}

	private static ResponseEntity<Map<String, Object>> absentUserCardCode411() {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("message", "获取失败！");
		inner.put("status_code", 411);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", inner);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(body);
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

	private static boolean parseH5IsEditePageTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		return true;
	}

	private static long resolveCompanyIdForUnauthorizedBody(
			HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	/**
	 * 浏览足迹列表：company_id 合并语义（auth claims 键优先，其次请求属性，再 query），允许 0。
	 */
	private static long resolveBrowseHistoryCompanyId(HttpServletRequest request, Map<String, Object> claims) {
		Map<String, Object> c = claims != null ? claims : Collections.emptyMap();
		if (c.containsKey("company_id")) {
			Object v = c.get("company_id");
			if (v == null) {
				throw new UnauthorizedException("获取用户信息失败");
			}
			return parseBrowseHistoryLong(v);
		}
		if (request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID) != null) {
			return parseBrowseHistoryLong(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (request.getParameter("company_id") != null) {
			return parseBrowseHistoryLong(request.getParameter("company_id"));
		}
		throw new UnauthorizedException("获取用户信息失败");
	}

	/** 浏览足迹列表：user_id 合并语义（auth claims 键优先，再 query），允许 0。 */
	private static long resolveBrowseHistoryUserId(HttpServletRequest request, Map<String, Object> claims) {
		Map<String, Object> c = claims != null ? claims : Collections.emptyMap();
		if (c.containsKey("user_id")) {
			Object v = c.get("user_id");
			if (v == null) {
				throw new UnauthorizedException("获取用户信息失败");
			}
			return parseBrowseHistoryLong(v);
		}
		if (request.getParameter("user_id") != null) {
			return parseBrowseHistoryLong(request.getParameter("user_id"));
		}
		throw new UnauthorizedException("获取用户信息失败");
	}

	private static long parseBrowseHistoryLong(Object raw) {
		if (raw == null) {
			throw new UnauthorizedException("获取用户信息失败");
		}
		if (raw instanceof Boolean b) {
			return Boolean.TRUE.equals(b) ? 1L : 0L;
		}
		if (raw instanceof Number n) {
			if (n instanceof Double || n instanceof Float) {
				String s = numberClaimToPlainDecimalString(n);
				if (s == null) {
					throw new UnauthorizedException("获取用户信息失败");
				}
				try {
					return Long.parseLong(s);
				} catch (NumberFormatException e) {
					throw new UnauthorizedException("获取用户信息失败");
				}
			}
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("获取用户信息失败");
			}
		}
		throw new UnauthorizedException("获取用户信息失败");
	}

	private static int parseBrowseHistoryPage(String pageRaw) {
		if (pageRaw == null || pageRaw.isBlank()) {
			return 1;
		}
		try {
			int v = Integer.parseInt(pageRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseBrowseHistoryPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || pageSizeRaw.isBlank()) {
			return 10;
		}
		try {
			int v = Integer.parseInt(pageSizeRaw.trim());
			return v < 1 ? 10 : v;
		} catch (NumberFormatException e) {
			return 10;
		}
	}

	private static int parseArticleFavListPage(String pageRaw) {
		if (pageRaw == null || pageRaw.isBlank()) {
			return 1;
		}
		try {
			int v = Integer.parseInt(pageRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseArticleFavListPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || pageSizeRaw.isBlank()) {
			return 20;
		}
		try {
			int v = Integer.parseInt(pageSizeRaw.trim());
			return v < 1 ? 20 : v;
		} catch (NumberFormatException e) {
			return 20;
		}
	}

	private static final int ARTICLE_ID_UNWRAP_MAX = 8;

	private static final int DISTRIBUTOR_ID_UNWRAP_MAX = 8;

	private static boolean isArticleIdRequiredFailure(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.trim().isEmpty();
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length == 0;
		}
		if (v instanceof int[] arr) {
			return arr.length == 0;
		}
		if (v instanceof long[] arr) {
			return arr.length == 0;
		}
		if (v instanceof short[] arr) {
			return arr.length == 0;
		}
		if (v instanceof byte[] arr) {
			return arr.length == 0;
		}
		if (v instanceof char[] arr) {
			return arr.length == 0;
		}
		if (v instanceof float[] arr) {
			return arr.length == 0;
		}
		if (v instanceof double[] arr) {
			return arr.length == 0;
		}
		if (v instanceof boolean[] arr) {
			return arr.length == 0;
		}
		return false;
	}

	private static boolean isDeleteItemsFavIsEmptyValueAllowed(Object raw) {
		if (raw instanceof String s) {
			String t = s.trim();
			return "true".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t);
		}
		return false;
	}

	private static List<String> collectDeleteItemsFavIndexedItemIds(
			HttpServletRequest request, Map<String, Object> body) {
		Map<Integer, String> byIndex = new TreeMap<>();
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			Matcher m = DELETE_ITEMS_FAV_ITEM_IDS_INDEXED.matcher(e.getKey());
			if (!m.matches()) {
				continue;
			}
			int idx = Integer.parseInt(m.group(1));
			String[] vals = e.getValue();
			if (vals == null || vals.length == 0) {
				continue;
			}
			String v = vals[0] == null ? "" : vals[0].trim();
			if (!v.isEmpty()) {
				byIndex.putIfAbsent(idx, v);
			}
		}
		for (Map.Entry<String, Object> e : body.entrySet()) {
			Matcher m = DELETE_ITEMS_FAV_ITEM_IDS_INDEXED.matcher(e.getKey());
			if (!m.matches()) {
				continue;
			}
			int idx = Integer.parseInt(m.group(1));
			String v = deleteItemsFavIndexedParamScalar(e.getValue());
			if (v != null && !v.isEmpty()) {
				byIndex.putIfAbsent(idx, v);
			}
		}
		return new ArrayList<>(byIndex.values());
	}

	private static String deleteItemsFavIndexedParamScalar(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() ? null : t;
		}
		if (raw instanceof Collection<?> c && c.size() == 1) {
			return deleteItemsFavIndexedParamScalar(c.iterator().next());
		}
		String t = Objects.toString(raw, "").trim();
		return t.isEmpty() ? null : t;
	}

	private static List<Object> buildItemIdsBoundListForDeleteItemsFav(Object itemIdsSource) {
		List<Object> boundIds = new ArrayList<>();
		addItemIdLeavesFromDeleteItemsFavSource(itemIdsSource, boundIds);
		return boundIds;
	}

	private static void addItemIdLeavesFromDeleteItemsFavSource(Object itemIdsSource, List<Object> boundIds) {
		if (itemIdsSource instanceof String s) {
			addItemIdLeafForDeleteItemsFav(s, boundIds);
			return;
		}
		if (itemIdsSource instanceof Collection<?> c) {
			for (Object elem : c) {
				addItemIdLeafForDeleteItemsFav(elem, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof Object[] arr) {
			for (Object elem : arr) {
				addItemIdLeafForDeleteItemsFav(elem, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof int[] arr) {
			for (int v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof long[] arr) {
			for (long v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof short[] arr) {
			for (short v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof byte[] arr) {
			for (byte v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof char[] arr) {
			for (char v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof float[] arr) {
			for (float v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof double[] arr) {
			for (double v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		if (itemIdsSource instanceof boolean[] arr) {
			for (boolean v : arr) {
				addItemIdLeafForDeleteItemsFav(v, boundIds);
			}
			return;
		}
		addItemIdLeafForDeleteItemsFav(itemIdsSource, boundIds);
	}

	private static void addItemIdLeafForDeleteItemsFav(Object raw, List<Object> boundIds) {
		if (raw == null) {
			return;
		}
		if (raw instanceof Number n) {
			boundIds.add(Long.valueOf(n.longValue()));
			return;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return;
		}
		try {
			boundIds.add(Long.valueOf(Long.parseLong(s)));
		} catch (NumberFormatException e) {
			boundIds.add(s);
		}
	}

	private static long parseArticleIdForDeleteArticleFav(Object v) {
		Object cur = v;
		for (int i = 0; i < ARTICLE_ID_UNWRAP_MAX; i++) {
			if (cur instanceof Collection<?> c) {
				if (c.size() != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = c.iterator().next();
				continue;
			}
			if (cur instanceof Object[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof int[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof long[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof short[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof byte[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof char[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof float[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof double[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof boolean[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误");
				}
				cur = arr[0];
				continue;
			}
			break;
		}
		if (isArticleIdRequiredFailure(cur)) {
			throw new BadRequestException(
					"删除收藏心愿单出错.", Map.of("article_id", List.of("validation.required")), 422);
		}
		if (cur instanceof Map<?, ?>) {
			throw new BadRequestException("参数有误");
		}
		if (cur instanceof Number n) {
			return n.longValue();
		}
		if (cur instanceof String s) {
			String t = s.trim();
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数有误");
			}
		}
		throw new BadRequestException("参数有误");
	}

	private static boolean isDistributorIdRequiredFailure(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.trim().isEmpty();
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length == 0;
		}
		if (v instanceof int[] arr) {
			return arr.length == 0;
		}
		if (v instanceof long[] arr) {
			return arr.length == 0;
		}
		if (v instanceof short[] arr) {
			return arr.length == 0;
		}
		if (v instanceof byte[] arr) {
			return arr.length == 0;
		}
		if (v instanceof char[] arr) {
			return arr.length == 0;
		}
		if (v instanceof float[] arr) {
			return arr.length == 0;
		}
		if (v instanceof double[] arr) {
			return arr.length == 0;
		}
		if (v instanceof boolean[] arr) {
			return arr.length == 0;
		}
		return false;
	}

	private static Object resolveDistributorIdForDeleteDistributionFav(Object v) {
		Object cur = v;
		for (int i = 0; i < DISTRIBUTOR_ID_UNWRAP_MAX; i++) {
			if (cur instanceof Collection<?> c) {
				if (c.size() != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = c.iterator().next();
				continue;
			}
			if (cur instanceof Object[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof int[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof long[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof short[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof byte[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof char[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof float[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof double[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			if (cur instanceof boolean[] arr) {
				if (arr.length != 1) {
					throw new BadRequestException("参数有误", 422);
				}
				cur = arr[0];
				continue;
			}
			break;
		}
		if (isDistributorIdRequiredFailure(cur)) {
			throw new BadRequestException(
					"删除收藏店铺出错.", Map.of("distributor_id", List.of("validation.required")), 422);
		}
		if (cur instanceof Map<?, ?>) {
			throw new BadRequestException("参数有误", 422);
		}
		if (cur instanceof Number n) {
			if (n.longValue() == 0L) {
				throw new BadRequestException("参数有误", 422);
			}
			return Long.valueOf(n.longValue());
		}
		if (cur instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException(
						"删除收藏店铺出错.", Map.of("distributor_id", List.of("validation.required")), 422);
			}
			if ("0".equals(t)) {
				throw new BadRequestException("参数有误", 422);
			}
			try {
				long parsed = Long.parseLong(t);
				if (parsed == 0L) {
					throw new BadRequestException("参数有误", 422);
				}
				return Long.valueOf(parsed);
			} catch (NumberFormatException e) {
				return t;
			}
		}
		throw new BadRequestException("参数有误", 422);
	}

	private static int parseItemsFavListPage(String pageRaw) {
		if (pageRaw == null || pageRaw.isBlank()) {
			return 1;
		}
		try {
			int v = Integer.parseInt(pageRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseItemsFavListPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || pageSizeRaw.isBlank()) {
			return 100;
		}
		try {
			int v = Integer.parseInt(pageSizeRaw.trim());
			return v < 1 ? 100 : v;
		} catch (NumberFormatException e) {
			return 100;
		}
	}

	private static int parseDistributionFavListPage(String pageRaw) {
		if (pageRaw == null || pageRaw.isBlank()) {
			return 1;
		}
		try {
			int v = Integer.parseInt(pageRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseDistributionFavListPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || pageSizeRaw.isBlank()) {
			return 100;
		}
		try {
			int v = Integer.parseInt(pageSizeRaw.trim());
			return v < 1 ? 100 : v;
		} catch (NumberFormatException e) {
			return 100;
		}
	}

	private static long parseMergedAuthBusinessIdForArticleFav(Map<String, Object> merged, String key) {
		String raw = Objects.toString(merged.get(key), "").trim();
		if (raw.isEmpty()) {
			throw new ResourceException("获取用户信息失败");
		}
		long v;
		try {
			v = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ResourceException("获取用户信息失败");
		}
		if (v <= 0L) {
			throw new ResourceException("获取用户信息失败");
		}
		return v;
	}

	private static long parseOptionalAuthUserId(Map<String, Object> claims) {
		if (claims == null || claims.isEmpty()) {
			return 0L;
		}
		return parsePositiveLongOrZero(claims.get("user_id"));
	}

	private static long resolveUserId(Map<String, Object> claims) {
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		try {
			long userId = Long.parseLong(userIdStr);
			if (userId <= 0L) {
				throw new UnauthorizedException("还未授权，请授权手机号");
			}
			return userId;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
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

	private static String coerceH5MobileInput(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() ? null : t;
		}
		if (raw instanceof Number n) {
			if (raw instanceof Double || raw instanceof Float) {
				double d = n.doubleValue();
				if (d == Math.rint(d)) {
					return Long.toString((long) d);
				}
				return String.valueOf(raw);
			}
			return String.valueOf(n.longValue());
		}
		String t = String.valueOf(raw).trim();
		return t.isEmpty() ? null : t;
	}

	private static String trimToNull(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() ? null : s;
	}

	private static long parseInvoiceIdForUpdate(String invoiceId) {
		if (invoiceId == null) {
			throw new ResourceException("缺少发票信息id");
		}
		String t = invoiceId.trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException("缺少发票信息id");
		}
		try {
			BigDecimal bd = new BigDecimal(t);
			if (bd.scale() > 0) {
				throw new ResourceException("缺少发票信息id");
			}
			long v = bd.longValueExact();
			if (v < 1L) {
				throw new ResourceException("缺少发票信息id");
			}
			return v;
		} catch (NumberFormatException | ArithmeticException e) {
			throw new ResourceException("缺少发票信息id");
		}
	}

	private static int resolveIsDefNormalized01(Map<String, Object> b) {
		if (!b.containsKey("is_def")) {
			return 0;
		}
		Object raw = b.get("is_def");
		if (raw == null) {
			return 0;
		}
		return normalizeNonNullIsDef(raw);
	}

	private static int normalizeNonNullIsDef(Object raw) {
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			if (d == 0.0d) {
				return 0;
			}
			if (d == 1.0d) {
				return 1;
			}
			throw new ResourceException("默认地址开启错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if ("0".equals(t)) {
				return 0;
			}
			if ("1".equals(t)) {
				return 1;
			}
			throw new ResourceException("默认地址开启错误");
		}
		throw new ResourceException("默认地址开启错误");
	}

	private static boolean promoterLooselyEqualsAuth(Object promoter, long authUserId) {
		return authUserId > 0L && parsePositiveLongOrZero(promoter) == authUserId;
	}

	/** 无法解析为十进制正整数商品 ID 时视为无有效商品，由服务层返回 {@code status:false}。 */
	private static long parseBrowseHistoryItemIdLenient(String raw) {
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		if (!POSITIVE_ITEM_ID.matcher(t).matches()) {
			return 0L;
		}
		return Long.parseLong(t);
	}

	private static long parseSubscribeItemId(String raw) {
		if (raw == null) {
			throw new BadRequestException("商品ID不能为空");
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException("商品ID不能为空");
		}
		if (!POSITIVE_ITEM_ID.matcher(t).matches()) {
			throw new BadRequestException("商品ID格式不正确");
		}
		return Long.parseLong(t);
	}

	private static int parseDistributorIdParam(String raw) {
		if (raw == null) {
			return 0;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return 0;
		}
		try {
			int v = Integer.parseInt(t);
			if (v < 0) {
				return 0;
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("分销商ID格式不正确");
		}
	}

	private static int parseDistributorIdLenientForIsSubscribe(String raw) {
		if (raw == null) {
			return 0;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return 0;
		}
		try {
			int v = Integer.parseInt(t);
			if (v < 0) {
				return 0;
			}
			return v;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static boolean buyUserIdIsTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return false;
			}
			return !"0".equals(t);
		}
		return true;
	}

	private static String nullToEmptyQuery(String raw) {
		return raw == null ? "" : raw;
	}

	private static boolean resolveIsGetWxInfoFlag(Map<String, Object> body, HttpServletRequest request) {
		if (body != null && body.containsKey("isGetWxInfo")) {
			return looseTruthy(body.get("isGetWxInfo"));
		}
		return looseTruthy(request.getParameter("isGetWxInfo"));
	}

	private static boolean looseTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return false;
		}
		return !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static String trimLeadingTrailingShi(String city) {
		if (city == null || city.isEmpty()) {
			return city;
		}
		String s = city;
		for (;;) {
			String next = LEADING_ZH_MARKET_CHARS.matcher(s).replaceFirst("");
			next = TRAILING_ZH_MARKET_CHARS.matcher(next).replaceFirst("");
			if (next.equals(s)) {
				return s;
			}
			s = next;
		}
	}

	private static boolean promoterUserIdIsTruthyForAddressList(String promoterUserIdParam) {
		return buyUserIdIsTruthy(promoterUserIdParam);
	}

	private static Long parseBuyUserIdForAddressListNullable(
			HttpServletRequest request, String buyUserIdParam) {
		if (!request.getParameterMap().containsKey("buy_user_id")) {
			return null;
		}
		if (buyUserIdParam == null) {
			return null;
		}
		String t = buyUserIdParam.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long resolveAddressIdFilterOrNull(String addressIdParam) {
		String t = addressIdParam == null ? "" : addressIdParam.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		long parsed = parsePositiveLongOrZero(t);
		return parsed > 0L ? Long.valueOf(parsed) : null;
	}

	private static int parseAddressListPageParam(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("page")) {
			return 1;
		}
		String[] vals = request.getParameterMap().get("page");
		String raw = (vals == null || vals.length == 0 || vals[0] == null) ? "" : vals[0];
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(trimmed);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseAddressListPageSizeParam(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("pageSize")) {
			return 20;
		}
		String[] vals = request.getParameterMap().get("pageSize");
		String raw = (vals == null || vals.length == 0 || vals[0] == null) ? "" : vals[0];
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(trimmed);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
