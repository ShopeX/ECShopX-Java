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

package cn.shopex.ecshopx.wechat.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.promotions.port.WxaNewTmplListPort;
import cn.shopex.ecshopx.common.wechat.WxappCommonSettingFacade;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.wechat.service.WxappOAuthGetOpenIdService;
import cn.shopex.ecshopx.wechat.service.WxappOAuthRedirectUrlService;
import cn.shopex.ecshopx.wechat.service.WxappOauthLoginAuthorizeService;
import cn.shopex.ecshopx.wechat.service.WxappOpenIdUnionidService;
import cn.shopex.ecshopx.wechat.service.WxappGetByShareIdService;
import cn.shopex.ecshopx.wechat.service.WxappShareSettingService;
import cn.shopex.ecshopx.wechat.service.WxappWxaUrlLinkService;
import cn.shopex.ecshopx.wechat.service.WxappWxaUrlSchemeService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaCartremindSettingService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappMemberCenterSettingService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappPageParamsSettingService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappPagestemplateBaseinfoService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappPromotionArticlesService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("wechatFrontV1Wxapp")
@RequestMapping("/api/v1/h5app")
public class WxappController {

	private final WxappOauthLoginAuthorizeService wxappOauthLoginAuthorizeService;
	private final WxappOAuthGetOpenIdService wxappOAuthGetOpenIdService;
	private final WxappOAuthRedirectUrlService wxappOAuthRedirectUrlService;
	private final WxappOpenIdUnionidService wxappOpenIdUnionidService;
	private final WxappShareSettingService wxappShareSettingService;
	private final WxappWxaUrlLinkService wxappWxaUrlLinkService;
	private final WxappWxaUrlSchemeService wxappWxaUrlSchemeService;
	private final WxappPromotionArticlesService wxappPromotionArticlesService;
	private final WxaCartremindSettingService wxaCartremindSettingService;
	private final WxappCommonSettingFacade wxappCommonSettingFacade;
	private final WxappGetByShareIdService wxappGetByShareIdService;
	private final WxappMemberCenterSettingService wxappMemberCenterSettingService;
	private final WxaNewTmplListPort wxaNewTmplListPort;
	private final WxappPageParamsSettingService wxappPageParamsSettingService;
	private final WxappPagestemplateBaseinfoService wxappPagestemplateBaseinfoService;

	public WxappController(
			WxappOauthLoginAuthorizeService wxappOauthLoginAuthorizeService,
			WxappOAuthGetOpenIdService wxappOAuthGetOpenIdService,
			WxappOAuthRedirectUrlService wxappOAuthRedirectUrlService,
			WxappOpenIdUnionidService wxappOpenIdUnionidService,
			WxappShareSettingService wxappShareSettingService,
			WxappWxaUrlLinkService wxappWxaUrlLinkService,
			WxappWxaUrlSchemeService wxappWxaUrlSchemeService,
			WxappPromotionArticlesService wxappPromotionArticlesService,
			WxaCartremindSettingService wxaCartremindSettingService,
			WxappCommonSettingFacade wxappCommonSettingFacade,
			WxappGetByShareIdService wxappGetByShareIdService,
			WxappMemberCenterSettingService wxappMemberCenterSettingService,
			WxaNewTmplListPort wxaNewTmplListPort,
			WxappPageParamsSettingService wxappPageParamsSettingService,
			WxappPagestemplateBaseinfoService wxappPagestemplateBaseinfoService) {
		this.wxappOauthLoginAuthorizeService = wxappOauthLoginAuthorizeService;
		this.wxappOAuthGetOpenIdService = wxappOAuthGetOpenIdService;
		this.wxappOAuthRedirectUrlService = wxappOAuthRedirectUrlService;
		this.wxappOpenIdUnionidService = wxappOpenIdUnionidService;
		this.wxappShareSettingService = wxappShareSettingService;
		this.wxappWxaUrlLinkService = wxappWxaUrlLinkService;
		this.wxappWxaUrlSchemeService = wxappWxaUrlSchemeService;
		this.wxappPromotionArticlesService = wxappPromotionArticlesService;
		this.wxaCartremindSettingService = wxaCartremindSettingService;
		this.wxappCommonSettingFacade = wxappCommonSettingFacade;
		this.wxappGetByShareIdService = wxappGetByShareIdService;
		this.wxappMemberCenterSettingService = wxappMemberCenterSettingService;
		this.wxaNewTmplListPort = wxaNewTmplListPort;
		this.wxappPageParamsSettingService = wxappPageParamsSettingService;
		this.wxappPagestemplateBaseinfoService = wxappPagestemplateBaseinfoService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/oauthlogin", name = "oauth登录校验")
	public ResponseEntity<Map<String, Object>> checkOauthLogin(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		boolean status = wxappOauthLoginAuthorizeService.checkOauthLogin(
				request, body == null ? Collections.emptyMap() : body);
		return ResponseEntity.ok(wrapFrontDataEnvelope(Map.of("status", status)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/getopenid", name = "openid与unionid")
	public ResponseEntity<Map<String, Object>> getUserOpentIdAndUnionid(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> data = wxappOpenIdUnionidService.getUserOpentIdAndUnionid(request, body);
		return ResponseEntity.ok(wrapFrontDataEnvelope(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/oauth/getredirecturl", name = "微信登录跳转")
	public ResponseEntity<Map<String, Object>> oauthRedirectUrl(
			HttpServletRequest request,
			@RequestParam(value = "url", required = false) String url) {
		long companyId = requireCompanyId(request);
		String redirectUrl = wxappOAuthRedirectUrlService.oauthRedirectUrl(companyId, url);
		return ResponseEntity.ok(wrapFrontDataEnvelope(Map.of("redirect_url", redirectUrl)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/oauth/getopenid", name = "获取openid")
	public ResponseEntity<Map<String, Object>> getOpenId(
			HttpServletRequest request,
			@RequestParam(value = "code", required = false) String code) {
		long companyId = requireCompanyId(request);
		String openId = wxappOAuthGetOpenIdService.getOpenId(companyId, code);
		if (!StringUtils.hasText(openId)) {
			throw new ResourceException("该账号不在店务应用可见范围内");
		}
		return ResponseEntity.ok(wrapFrontDataEnvelope(Map.of("open_id", openId)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/oauth/login/authorize", name = "oauth授权")
	public ResponseEntity<Map<String, Object>> authorizeOauthLogin(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		wxappOauthLoginAuthorizeService.authorizeOauthLogin(request, body == null ? Collections.emptyMap() : body);
		return ResponseEntity.ok(wrapFrontDataEnvelope(Map.of("status", Boolean.TRUE)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/oauth/login/valid", name = "oauth校验")
	public ResponseEntity<Map<String, Object>> validOauthLogin(
			@RequestParam(value = "token", required = false) String token) {
		return ResponseEntity.ok(
				wrapFrontDataEnvelope(wxappOauthLoginAuthorizeService.validOauthLogin(token)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/urllink", name = "小程序url链接")
	public ResponseEntity<Map<String, Object>> wxaUrlLink(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> full =
				WxappOpenIdUnionidService.mergeJsonBodyWithRequestParams(
						body == null ? Collections.emptyMap() : body, request);
		Map<String, Object> mergedParams = new LinkedHashMap<>();
		for (String k : List.of("path", "query", "env_version")) {
			mergedParams.put(k, full.get(k));
		}
		Map<String, Object> wechatData = wxappWxaUrlLinkService.wxaUrlLink(companyId, mergedParams);
		return ResponseEntity.ok(wrapFrontDataEnvelope(wechatData));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/urlschema", name = "小程序schema链接")
	public ResponseEntity<Map<String, Object>> wxaUrlSchema(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> full =
				WxappOpenIdUnionidService.mergeJsonBodyWithRequestParams(
						body == null ? Collections.emptyMap() : body, request);
		Map<String, Object> mergedParams = new LinkedHashMap<>();
		for (String k : List.of("path", "query", "env_version")) {
			mergedParams.put(k, full.get(k));
		}
		Map<String, Object> wechatData = wxappWxaUrlSchemeService.wxaUrlSchema(companyId, mergedParams);
		return ResponseEntity.ok(wrapFrontDataEnvelope(wechatData));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxa/promotion/articles", name = "推广文章")
	public ResponseEntity<Map<String, Object>> getPromotionArticles(
			HttpServletRequest request,
			@RequestParam(name = "template_name", required = false) String templateName,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "page_name", defaultValue = "index") String pageName,
			@RequestParam(name = "country_code", required = false) String countryCodeRaw) {
		long companyId = requireCompanyId(request);
		List<Map<String, Object>> list =
				wxappPromotionArticlesService.getPromotionArticles(
						companyId, templateName, name, pageName, countryCodeRaw);
		stripViewContentAndContentFromFirstSettingsParams(list);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", list);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxa/promotion/articles/info", name = "推广文章详情")
	public ResponseEntity<Map<String, Object>> getPromotionArticlesInfo(
			HttpServletRequest request,
			@RequestParam(name = "template_name", required = false) String templateName,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "page_name", defaultValue = "index") String pageName,
			@RequestParam(name = "index", required = false) String indexRaw,
			@RequestParam(name = "country_code", required = false) String countryCodeRaw) {
		long companyId = requireCompanyId(request);
		Object data =
				wxappPromotionArticlesService.getPromotionArticlesInfo(
						companyId, templateName, name, pageName, countryCodeRaw, indexRaw);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/pageparams/setting", name = "页面参数配置")
	public ResponseEntity<Map<String, Object>> getParamByTempName(
			HttpServletRequest request,
			@RequestParam(value = "template_name", required = false) String templateName,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "page_name", required = false) String pageName,
			@RequestParam(value = "version", required = false) String version,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "regionauth_id", required = false) String regionauthIdRaw,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = requireCompanyId(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		long distributorId = parsePositiveLongOrZero(distributorIdRaw);
		long regionauthId = parsePositiveLongOrZero(regionauthIdRaw);
		long jwtDistributorId = parsePositiveLongOrZero(claims.get("distributor_id"));
		String locale = (countryCode == null || countryCode.isBlank()) ? "zh-CN" : countryCode.trim();
		Object payload =
				wxappPageParamsSettingService.getParamByTempName(
						companyId,
						userId,
						templateName,
						name,
						pageName,
						version,
						distributorId,
						regionauthId,
						locale,
						jwtDistributorId);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", payload);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/share/setting", name = "分享设置")
	public ResponseEntity<Map<String, Object>> getShareSetting(
			HttpServletRequest request,
			@org.springframework.web.bind.annotation.RequestParam(name = "country_code", required = false) String countryCode,
			@org.springframework.web.bind.annotation.RequestParam(name = "shareindex", defaultValue = "index") String shareIndex) {
		long companyId = requireCompanyId(request);
		Map<String, Object> data = wxappShareSettingService.getFrontShareScene(companyId, countryCode, shareIndex);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	private static void stripViewContentAndContentFromFirstSettingsParams(List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		Map<String, Object> first = list.get(0);
		if (first == null) {
			return;
		}
		Object paramsObj = first.get("params");
		if (!(paramsObj instanceof List<?> rawList)) {
			return;
		}
		List<Object> newList = new ArrayList<>();
		for (Object el : rawList) {
			if (el instanceof Map<?, ?> m) {
				LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					copy.put(String.valueOf(e.getKey()), e.getValue());
				}
				copy.remove("viewcontent");
				copy.remove("content");
				newList.add(copy);
			} else {
				newList.add(el);
			}
		}
		first.put("params", newList);
	}

	private static LinkedHashMap<String, Object> wrapFrontDataEnvelope(Map<String, Object> data) {
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return body;
	}

	/**
	 * Query 字符串判定：null、空串或完全等于 "0" 时返回 true（短路返回空列表）；不对入参 trim。
	 */
	private static boolean isAbsentOrZeroLikeString(String s) {
		if (s == null) {
			return true;
		}
		if (s.isEmpty()) {
			return true;
		}
		return "0".equals(s);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/newtemplate", name = "订阅消息模板")
	public ResponseEntity<Map<String, Object>> getWxaNewTmpl(
			HttpServletRequest request,
			@RequestParam(name = "source_type", required = false) String sourceType,
			@RequestParam(name = "temp_name", required = false) String tempName) {
		if (isAbsentOrZeroLikeString(sourceType) || isAbsentOrZeroLikeString(tempName)) {
			return ResponseEntity.ok(wrapFrontDataEnvelope(Map.of("template_id", List.of())));
		}
		long companyId = requireCompanyId(request);
		List<String> ids = wxaNewTmplListPort.listTemplateIds(companyId, sourceType, tempName);
		return ResponseEntity.ok(wrapFrontDataEnvelope(Map.of("template_id", ids)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/membercenter/setting", name = "会员中心参数")
	public ResponseEntity<Map<String, Object>> getMemberCenterParamByTempName(
			HttpServletRequest request,
			@RequestParam(name = "template_name", required = false) String templateName,
			@RequestParam(name = "version", defaultValue = "v1.0.0") String version,
			@RequestParam(name = "country_code", required = false) String countryCode) {
		long companyId = requireCompanyId(request);
		long userId = parsePositiveLongOrZero(readH5AuthClaimsMap(request).get("user_id"));
		Map<String, Object> result =
				wxappMemberCenterSettingService.getMemberCenterParamByTempName(
						companyId, userId, templateName, version, countryCode);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", result);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/common/setting", name = "首页配置")
	public ResponseEntity<Map<String, Object>> getCommonSetting(
			HttpServletRequest request,
			@RequestParam(name = "country_code", required = false) String countryCode) {
		long companyId = requireCompanyId(request);
		Map<String, Object> data = wxappCommonSettingFacade.getCommonSetting(companyId, countryCode);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/cartremind/setting", name = "购物车提醒")
	public ResponseEntity<Map<String, Object>> getCartremindSetting(HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		Map<String, Object> data = wxaCartremindSettingService.getCartremindSetting(companyId);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/getbyshareid", name = "share_id参数")
	public ResponseEntity<Map<String, Object>> getByShareId(
			HttpServletRequest request,
			@RequestParam(value = "share_id", required = false) String shareId) {
		if (shareId == null
				|| !StringUtils.hasText(shareId.trim())
				|| "0".equals(shareId.trim())) {
			throw new BadRequestException("参数错误");
		}
		long companyId = requireCompanyId(request);
		Object payload = wxappGetByShareIdService.getByShareId(companyId, shareId.trim());
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", payload);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/pagestemplate/baseinfo", name = "模板基础信息")
	public ResponseEntity<Map<String, Object>> getPagestemplateBaseinfo(
			HttpServletRequest request,
			@RequestParam(value = "template_name", required = false) String templateName,
			@RequestParam(value = "version", required = false) String version,
			@RequestParam(value = "regionauth_id", required = false) String regionauthIdRaw,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = requireCompanyId(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		long jwtDistributorId = parsePositiveLongOrZero(claims.get("distributor_id"));
		Map<String, Object> data =
				wxappPagestemplateBaseinfoService.getPagestemplateBaseinfo(
						companyId,
						userId,
						jwtDistributorId,
						templateName,
						version,
						regionauthIdRaw,
						countryCode);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@GetMapping(value = "/wxapp/pagestemplate/membercenter", name = "会员中心模板")
	public ResponseEntity<Map<String, Object>> getPagestemplateMembercenter(
			HttpServletRequest request,
			@RequestParam(value = "template_name", required = false) String templateName,
			@RequestParam(value = "version", required = false) String version,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = requireCompanyId(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		Map<String, Object> flat = wxappMemberCenterSettingService.getPagestemplateMembercenter(
				companyId, userId, templateName, version, countryCode);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", flat);
		return ResponseEntity.ok(body);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long requireCompanyId(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long fromAttr = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		long fromQuery = parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
		if (fromQuery > 0L) {
			return fromQuery;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
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
}
