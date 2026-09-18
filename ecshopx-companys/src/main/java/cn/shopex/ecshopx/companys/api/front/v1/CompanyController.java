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

package cn.shopex.ecshopx.companys.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.PrivacySettingData;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.companylogistics.CompanyLogisticsEnableListService;
import cn.shopex.ecshopx.companys.service.companylogistics.CompanyLogisticsListService;
import cn.shopex.ecshopx.companys.service.privacy.CompanysPrivacySettingService;
import cn.shopex.ecshopx.companys.service.setting.H5CompanySettingService;
import cn.shopex.ecshopx.companys.service.setting.ItemPriceSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.LanguageSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.NostoresSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.TradeRateSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.WebUrlSettingRedisService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("companysFrontV1CompanyWxapp")
@RequestMapping("/api/v1/h5app")
public class CompanyController {

	private static final String MSG_H5_COMPANY_UNAUTHORIZED = "无权访问该API,非法访问！";

	private final CompanyLogisticsEnableListService companyLogisticsEnableListService;
	private final CompanyLogisticsListService companyLogisticsListService;
	private final TradeRateSettingRedisService tradeRateSettingRedisService;
	private final CompanysPrivacySettingService companysPrivacySettingService;
	private final H5CompanySettingService h5CompanySettingService;
	private final LangueProperties langueProperties;
	private final NostoresSettingRedisService nostoresSettingRedisService;
	private final ItemPriceSettingRedisService itemPriceSettingRedisService;
	private final WebUrlSettingRedisService webUrlSettingRedisService;
	private final LanguageSettingRedisService languageSettingRedisService;

	public CompanyController(
			CompanyLogisticsEnableListService companyLogisticsEnableListService,
			CompanyLogisticsListService companyLogisticsListService,
			TradeRateSettingRedisService tradeRateSettingRedisService,
			CompanysPrivacySettingService companysPrivacySettingService,
			H5CompanySettingService h5CompanySettingService,
			LangueProperties langueProperties,
			NostoresSettingRedisService nostoresSettingRedisService,
			ItemPriceSettingRedisService itemPriceSettingRedisService,
			WebUrlSettingRedisService webUrlSettingRedisService,
			LanguageSettingRedisService languageSettingRedisService) {
		this.companyLogisticsEnableListService = companyLogisticsEnableListService;
		this.companyLogisticsListService = companyLogisticsListService;
		this.tradeRateSettingRedisService = tradeRateSettingRedisService;
		this.companysPrivacySettingService = companysPrivacySettingService;
		this.h5CompanySettingService = h5CompanySettingService;
		this.langueProperties = langueProperties;
		this.nostoresSettingRedisService = nostoresSettingRedisService;
		this.itemPriceSettingRedisService = itemPriceSettingRedisService;
		this.webUrlSettingRedisService = webUrlSettingRedisService;
		this.languageSettingRedisService = languageSettingRedisService;
	}

	@GetMapping(value = "/wxapp/company/setting", name = "商城配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCompanySetting(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data = h5CompanySettingService.getCompanySetting(companyId, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/setting/weburl", name = "外部链接配置")
	public ResponseEntity<ApiResult<Object>> getWebUrlSetting(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> defaults = buildWebUrlDefaultQuerySubset(request);
		String mobile = resolveH5MobileForWebUrl(request);
		Object data = webUrlSettingRedisService.getWebUrlSettingForH5(companyId, defaults, mobile);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/company/logistics/list", name = "公司物流列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCompanyLogisticsList(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) Integer distributorId,
			@RequestParam(value = "supplier_id", required = false) Integer supplierId) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> data =
				companyLogisticsListService.getCompanyLogisticsList(
						companyId,
						distributorId == null ? 0 : distributorId,
						supplierId == null ? 0 : supplierId,
						null,
						null);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/traderate/getstatus", name = "评价状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRateSettingStatus(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> data = tradeRateSettingRedisService.getRateSettingStatus(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/nostores/getstatus", name = "无店铺状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> getNostoresStatus(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> data = nostoresSettingRedisService.getNostoresStatus(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/company/logistics/enableList", name = "启用物流列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getLogisticsEnableList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) {
		long companyId = requirePositiveH5CompanyId(request);
		int p = page != null ? page : 1;
		p = Math.max(1, p);
		int ps = pageSize != null ? pageSize : -1;
		List<Map<String, Object>> data = companyLogisticsEnableListService.getLogisticsEnableList(companyId, p, ps);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/setting/itemPrice", name = "商品价格显示设置")
	public ResponseEntity<ApiResult<Object>> getItemPriceSetting(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		Object data = itemPriceSettingRedisService.getItemPriceSettingForH5(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/setting/language", name = "已启用语言列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLanguageSwitchSetting(HttpServletRequest request) {
		long companyId = requirePositiveCompanyIdForLanguageSwitch(request);
		Map<String, Object> data = languageSettingRedisService.getSwitchSetting(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/company/privacy_setting_ck", name = "隐私设置")
	public ResponseEntity<ApiResult<PrivacySettingData>> getPrivacySetting(HttpServletRequest request) {
		String lang = RequestLangTag.current(langueProperties);
		long companyId = requirePositiveH5CompanyId(request);
		PrivacySettingData out = companysPrivacySettingService.getPrivacySetting(companyId, lang);
		return ResponseEntity.ok(ApiResult.ok(out));
	}

	private static Map<String, Object> buildWebUrlDefaultQuerySubset(HttpServletRequest request) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(2);
		var names = request.getParameterMap();
		if (names.containsKey("mycoach")) {
			out.put("mycoach", request.getParameter("mycoach"));
		}
		if (names.containsKey("aftersales")) {
			out.put("aftersales", request.getParameter("aftersales"));
		}
		return out;
	}

	private static String resolveH5MobileForWebUrl(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> m)) {
			return "";
		}
		Object mobileObj = m.get("mobile");
		if (mobileObj == null) {
			return "";
		}
		return String.valueOf(mobileObj).trim();
	}

	private static long requirePositiveCompanyIdForLanguageSwitch(HttpServletRequest request) {
		long fromH5 = tryPositiveCompanyId(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		if (fromH5 > 0L) {
			return fromH5;
		}
		long fromOperator =
				tryCompanyIdFromOperatorJwt(
						request.getAttribute(
								"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA"));
		if (fromOperator > 0L) {
			return fromOperator;
		}
		throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
	}

	private static long tryCompanyIdFromOperatorJwt(Object raw) {
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			return 0L;
		}
		return tryPositiveCompanyId(ud.get("company_id"));
	}

	private static long tryPositiveCompanyId(Object companyAttr) {
		if (companyAttr instanceof Number n) {
			long id = n.longValue();
			return id > 0L ? id : 0L;
		}
		if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				long id = Long.parseLong(s.trim());
				return id > 0L ? id : 0L;
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static long requirePositiveH5CompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
			}
		} else {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		return companyId;
	}

}
