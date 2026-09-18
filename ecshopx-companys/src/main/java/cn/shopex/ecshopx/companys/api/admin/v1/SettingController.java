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

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.crypto.MailSettingPasswordCodec;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.MailSettingResponse;
import cn.shopex.ecshopx.companys.service.setting.CategoryPageSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.DianwuSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.GiftSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.CompanyBaseSettingService;
import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.ItemPriceSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.ItemSalesSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.ItemStartNumSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.SupplierItemStartNumSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.KujialeConfigRedisService;
import cn.shopex.ecshopx.companys.service.setting.ItemShareSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.MailSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.NostoresSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.OpenDistributorDividedSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.RechargeSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.SelfdeliveryAddressRedisService;
import cn.shopex.ecshopx.companys.service.setting.SendOmsSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.CompanysPharmaIndustrySettingSaveService;
import cn.shopex.ecshopx.companys.service.setting.ItemStoreSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.ShareParametersSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.ShareSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.AllSettingsAggregateService;
import cn.shopex.ecshopx.companys.service.setting.TradeRateSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.WebUrlSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.WhitelistSettingRedisService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("companysAdminV1Setting")
@RequestMapping("/api/v1")
public class SettingController {

	/** Request attribute name set by the admin JWT filter (string literal avoids ecshopx-companys depending on ecshopx-espier). */
	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final String ITEM_SHARE_IS_OPEN_REQUIRED = "商品分享是否限制不能为空";
	private static final String ITEM_SHARE_IS_OPEN_INVALID = "商品分享是否限制取值无效";
	private static final String ITEM_SHARE_VALID_GRADE_REQUIRED = "可分享会员等级至少选择一个";
	private static final String ITEM_SHARE_MSG_REQUIRED = "分享限制提示语不能为空";
	private static final String ITEM_SHARE_MSG_TOO_LONG = "分享限制提示语不能超过20个字";
	private static final String ITEM_SHARE_PAGE_REQUIRED = "分享跳转页面不能为空";
	private static final String ITEM_SHARE_SAVE_FAILED = "保存商品分享设置失败";

	private static final String MAIL_SETTING_SMTP_PORT_REQUIRED = "SMTP端口不能为空";
	private static final String MAIL_SETTING_RELAY_HOST_REQUIRED = "SMTP服务器地址不能为空";
	private static final String MAIL_SETTING_SENDER_REQUIRED_OR_INVALID = "发件人邮箱不能为空且格式正确";
	private static final String MAIL_SETTING_USER_REQUIRED = "SMTP用户名不能为空";
	private static final String MAIL_SETTING_PASSWORD_REQUIRED = "SMTP密码不能为空";

	private static final String NOSTORES_STATUS_REQUIRED = "设置状态不能为空";

	private static final String[] SHARE_SETTING_SCENES = {
		"index", "planting", "itemlist", "group", "seckill", "coupon"
	};
	private static final String[] SHARE_SETTING_FIELDS = {"title", "desc", "imageUrl"};

	private final ItemShareSettingRedisService itemShareSettingRedisService;
	private final ItemStoreSettingRedisService itemStoreSettingRedisService;
	private final ItemSalesSettingRedisService itemSalesSettingRedisService;
	private final ShareSettingRedisService shareSettingRedisService;
	private final CategoryPageSettingRedisService categoryPageSettingRedisService;
	private final DianwuSettingRedisService dianwuSettingRedisService;
	private final GiftSettingRedisService giftSettingRedisService;
	private final CompanyBaseSettingService companyBaseSettingService;
	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final ItemPriceSettingRedisService itemPriceSettingRedisService;
	private final ItemStartNumSettingRedisService itemStartNumSettingRedisService;
	private final SupplierItemStartNumSettingRedisService supplierItemStartNumSettingRedisService;
	private final KujialeConfigRedisService kujialeConfigRedisService;
	private final MailSettingRedisService mailSettingRedisService;
	private final MailSettingPasswordCodec mailSettingPasswordCodec;
	private final WhitelistSettingRedisService whitelistSettingRedisService;
	private final NostoresSettingRedisService nostoresSettingRedisService;
	private final OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService;
	private final PickupcodeSettingRedisService pickupcodeSettingRedisService;
	private final RechargeSettingRedisService rechargeSettingRedisService;
	private final SendOmsSettingRedisService sendOmsSettingRedisService;
	private final SelfdeliveryAddressRedisService selfdeliveryAddressRedisService;
	private final CompanysPharmaIndustrySettingSaveService companysPharmaIndustrySettingSaveService;
	private final WebUrlSettingRedisService webUrlSettingRedisService;
	private final ShareParametersSettingRedisService shareParametersSettingRedisService;
	private final TradeRateSettingRedisService tradeRateSettingRedisService;
	private final AllSettingsAggregateService allSettingsAggregateService;

	public SettingController(
			ItemShareSettingRedisService itemShareSettingRedisService,
			ItemStoreSettingRedisService itemStoreSettingRedisService,
			ItemSalesSettingRedisService itemSalesSettingRedisService,
			ShareSettingRedisService shareSettingRedisService,
			CategoryPageSettingRedisService categoryPageSettingRedisService,
			DianwuSettingRedisService dianwuSettingRedisService,
			GiftSettingRedisService giftSettingRedisService,
			CompanyBaseSettingService companyBaseSettingService,
			InvoiceSettingRedisService invoiceSettingRedisService,
			ItemPriceSettingRedisService itemPriceSettingRedisService,
			ItemStartNumSettingRedisService itemStartNumSettingRedisService,
			SupplierItemStartNumSettingRedisService supplierItemStartNumSettingRedisService,
			KujialeConfigRedisService kujialeConfigRedisService,
			MailSettingRedisService mailSettingRedisService,
			MailSettingPasswordCodec mailSettingPasswordCodec,
			WhitelistSettingRedisService whitelistSettingRedisService,
			NostoresSettingRedisService nostoresSettingRedisService,
			OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService,
			PickupcodeSettingRedisService pickupcodeSettingRedisService,
			RechargeSettingRedisService rechargeSettingRedisService,
			SendOmsSettingRedisService sendOmsSettingRedisService,
			SelfdeliveryAddressRedisService selfdeliveryAddressRedisService,
			CompanysPharmaIndustrySettingSaveService companysPharmaIndustrySettingSaveService,
			WebUrlSettingRedisService webUrlSettingRedisService,
			ShareParametersSettingRedisService shareParametersSettingRedisService,
			TradeRateSettingRedisService tradeRateSettingRedisService,
			AllSettingsAggregateService allSettingsAggregateService) {
		this.itemShareSettingRedisService = itemShareSettingRedisService;
		this.itemStoreSettingRedisService = itemStoreSettingRedisService;
		this.itemSalesSettingRedisService = itemSalesSettingRedisService;
		this.shareSettingRedisService = shareSettingRedisService;
		this.categoryPageSettingRedisService = categoryPageSettingRedisService;
		this.dianwuSettingRedisService = dianwuSettingRedisService;
		this.giftSettingRedisService = giftSettingRedisService;
		this.companyBaseSettingService = companyBaseSettingService;
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.itemPriceSettingRedisService = itemPriceSettingRedisService;
		this.itemStartNumSettingRedisService = itemStartNumSettingRedisService;
		this.supplierItemStartNumSettingRedisService = supplierItemStartNumSettingRedisService;
		this.kujialeConfigRedisService = kujialeConfigRedisService;
		this.mailSettingRedisService = mailSettingRedisService;
		this.mailSettingPasswordCodec = mailSettingPasswordCodec;
		this.whitelistSettingRedisService = whitelistSettingRedisService;
		this.nostoresSettingRedisService = nostoresSettingRedisService;
		this.openDistributorDividedSettingRedisService = openDistributorDividedSettingRedisService;
		this.pickupcodeSettingRedisService = pickupcodeSettingRedisService;
		this.rechargeSettingRedisService = rechargeSettingRedisService;
		this.sendOmsSettingRedisService = sendOmsSettingRedisService;
		this.selfdeliveryAddressRedisService = selfdeliveryAddressRedisService;
		this.companysPharmaIndustrySettingSaveService = companysPharmaIndustrySettingSaveService;
		this.webUrlSettingRedisService = webUrlSettingRedisService;
		this.shareParametersSettingRedisService = shareParametersSettingRedisService;
		this.tradeRateSettingRedisService = tradeRateSettingRedisService;
		this.allSettingsAggregateService = allSettingsAggregateService;
	}

	@Activated(routeAlias = "company.setting.set")
	@PostMapping(value = "/company/setting", name = "设置当前企业的基础设置")
	public ApiResult<Map<String, Object>> setSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		LinkedHashMap<String, Object> present = new LinkedHashMap<>();
		for (String key :
				new String[] {"community_config", "withdraw_bank", "consumer_hotline", "customer_switch"}) {
			if (merged.containsKey(key)) {
				present.put(key, merged.get(key));
			}
		}
		Map<String, Object> result = companyBaseSettingService.upsertCompanySetting(companyId, present);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "company.setting.get")
	@GetMapping(value = "/company/setting", name = "获取当前企业的基础设置")
	public ApiResult<Map<String, Object>> getSetting(
			HttpServletRequest request,
			@RequestParam(name = "community_config", required = false) String communityConfig,
			@RequestParam(name = "withdraw_bank", required = false) String withdrawBank,
			@RequestParam(name = "consumer_hotline", required = false) String consumerHotline,
			@RequestParam(name = "customer_switch", required = false) String customerSwitch) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data =
				companyBaseSettingService.getSetting(
						companyId, communityConfig, withdrawBank, consumerHotline, customerSwitch);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "share.setting.set")
	@PostMapping(value = "/share/setting", name = "设置分享设置")
	public ApiResult<Map<String, Object>> setShareSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> nested = buildShareSettingNestedWhitelist(merged);
		String countryCodeRaw =
				merged == null
						? null
						: (merged.get("country_code") instanceof String s
								? s
								: merged.get("country_code") == null
										? null
										: String.valueOf(merged.get("country_code")));
		shareSettingRedisService.saveSingleLangWholeKeyReplace(companyId, nested, countryCodeRaw);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "share.setting.get")
	@GetMapping(value = "/share/setting", name = "获取分享设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getShareSetting(
			HttpServletRequest request,
			@RequestParam(name = "country_code", required = false) String countryCode) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = shareSettingRedisService.getEffective(companyId, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "company.selfdelivery.set.address")
	@PostMapping(value = "/setting/selfdelivery", name = "配置固定的自提地址")
	public ApiResult<Map<String, Object>> setSelfdeliveryAddress(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		List<Map<String, Object>> list = normalizeSelfdeliveryAddreeList(merged);
		if (list.isEmpty()) {
			throw new BadRequestException("地址参数有误");
		}
		validateSelfdeliveryRows(list);
		selfdeliveryAddressRedisService.saveWholeList(companyId, list);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "company.selfdelivery.get.address")
	@GetMapping(value = "/setting/selfdelivery", name = "获取自提地址配置")
	public ApiResult<List<Map<String, Object>>> getSelfdeliveryAddress(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		List<Map<String, Object>> data = selfdeliveryAddressRedisService.readWholeList(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "company.setting.weburl.set")
	@PostMapping(value = "/setting/weburl", name = "配置外部链接")
	public ApiResult<Map<String, Object>> saveWebUrlSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> fourKeys = webUrlSettingRedisService.buildFourKeysFromMerged(merged);
		webUrlSettingRedisService.saveFourKeys(companyId, fourKeys);
		return ApiResult.ok(fourKeys);
	}

	@Activated(routeAlias = "company.setting.weburl.get")
	@GetMapping(value = "/setting/weburl", name = "获取外部链接配置")
	public ApiResult<Object> getWebUrlSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		return ApiResult.ok(webUrlSettingRedisService.readPayload(companyId));
	}

	@Activated(routeAlias = "trade.rate.setting.get")
	@RequestMapping(
			value = "/traderate/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "评价状态读写")
	public ApiResult<Object> rateSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		overlayRateStatusFromParameterMap(request, merged);
		Map<String, Object> data = tradeRateSettingRedisService.rateSetting(companyId, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "member.whitelist.setting.get")
	@GetMapping(value = "/member/whitelist/setting", name = "获取白名单设置状态")
	public ApiResult<Map<String, Object>> getWhitelistSetting(HttpServletRequest request) {
		return handleWhitelistSetting(request, null);
	}

	@Activated(routeAlias = "member.whitelist.setting.set")
	@PostMapping(value = "/member/whitelist/setting", name = "设置白名单状态")
	public ApiResult<Map<String, Object>> saveWhitelistSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		return handleWhitelistSetting(request, body);
	}

	private static void overlayWhitelistKeysFromParameterMap(
			HttpServletRequest request, Map<String, Object> merged) {
		Map<String, String[]> parameterMap = request.getParameterMap();
		for (String key : new String[] {"whitelist_status", "whitelist_tips"}) {
			if (!parameterMap.containsKey(key)) {
				continue;
			}
			if (merged.containsKey(key)) {
				continue;
			}
			String[] arr = parameterMap.get(key);
			String raw = (arr != null && arr.length > 0 && arr[0] != null) ? arr[0] : "";
			merged.put(key, raw);
		}
	}

	private static void overlayPickupcodeStatusFromParameterMap(
			HttpServletRequest request, Map<String, Object> merged) {
		Map<String, String[]> parameterMap = request.getParameterMap();
		for (String key : new String[] {"pickupcode_status"}) {
			if (!parameterMap.containsKey(key)) {
				continue;
			}
			if (merged.containsKey(key)) {
				continue;
			}
			String[] arr = parameterMap.get(key);
			String raw = (arr != null && arr.length > 0 && arr[0] != null) ? arr[0] : "";
			merged.put(key, raw);
		}
	}

	private static void overlayRechargeStatusFromParameterMap(
			HttpServletRequest request, Map<String, Object> merged) {
		Map<String, String[]> parameterMap = request.getParameterMap();
		for (String key : new String[] {"recharge_status"}) {
			if (!parameterMap.containsKey(key)) {
				continue;
			}
			if (merged.containsKey(key)) {
				continue;
			}
			String[] arr = parameterMap.get(key);
			String raw = (arr != null && arr.length > 0 && arr[0] != null) ? arr[0] : "";
			merged.put(key, raw);
		}
	}

	private static void overlayRateStatusFromParameterMap(
			HttpServletRequest request, Map<String, Object> merged) {
		Map<String, String[]> parameterMap = request.getParameterMap();
		for (String key : new String[] {"rate_status"}) {
			if (!parameterMap.containsKey(key)) {
				continue;
			}
			if (merged.containsKey(key)) {
				continue;
			}
			String[] arr = parameterMap.get(key);
			String raw = (arr != null && arr.length > 0 && arr[0] != null) ? arr[0] : "";
			merged.put(key, raw);
		}
	}

	private ApiResult<Map<String, Object>> handleWhitelistSetting(
			HttpServletRequest request, Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		overlayWhitelistKeysFromParameterMap(request, merged);
		return ApiResult.ok(whitelistSettingRedisService.getMergedConfig(companyId, merged));
	}

	@Activated(routeAlias = "presale.pickupcode.setting.get")
	@RequestMapping(
			value = "/pickupcode/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "预售提货码状态读写")
	public ApiResult<Map<String, Object>> pickupcodeSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		overlayPickupcodeStatusFromParameterMap(request, merged);
		return ApiResult.ok(pickupcodeSettingRedisService.handle(companyId, merged));
	}

	@Activated(routeAlias = "trade.gift.setting.get")
	@GetMapping(value = "/gift/setting", name = "赠品相关设置查询")
	public ApiResult<Map<String, Object>> getGiftSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		return ApiResult.ok(giftSettingRedisService.getGiftSetting(companyId));
	}

	@Activated(routeAlias = "trade.gift.setting.set")
	@PostMapping(value = "/gift/setting", name = "赠品相关设置保存")
	public ApiResult<Map<String, Object>> setGiftSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> result = giftSettingRedisService.saveGiftSetting(companyId, merged);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "trade.sendoms.setting.get")
	@GetMapping(value = "/sendoms/setting", name = "获取推 OMS 设置")
	public ApiResult<Map<String, Object>> getSendOmsSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		return ApiResult.ok(sendOmsSettingRedisService.readZitiSendOms(companyId));
	}

	@Activated(routeAlias = "trade.sendoms.setting.set")
	@PostMapping(value = "/sendoms/setting", name = "设置推 OMS 设置")
	public ApiResult<Map<String, Object>> setSendOmsSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> result = sendOmsSettingRedisService.saveZitiSendOms(companyId, merged);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "nostores.setting.get")
	@GetMapping(value = "/nostores/setting", name = "获取前端店铺展示开关")
	public ApiResult<Map<String, Object>> getNostoresSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		return ApiResult.ok(nostoresSettingRedisService.readSetting(companyId));
	}

	@Activated(routeAlias = "nostores.setting.set")
	@PostMapping(value = "/nostores/setting", name = "设置前端店铺展示开关")
	public ApiResult<Map<String, Object>> setNostoresSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		validateNostoresStatusRequired(merged);
		nostoresSettingRedisService.writeWholePayload(companyId, merged);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	private static void validateNostoresStatusRequired(Map<String, Object> merged) {
		if (merged == null
				|| !merged.containsKey("nostores_status")
				|| merged.get("nostores_status") == null) {
			throw new BadRequestException(NOSTORES_STATUS_REQUIRED);
		}
		Object v = merged.get("nostores_status");
		if (v instanceof String s && s.trim().isEmpty()) {
			throw new BadRequestException(NOSTORES_STATUS_REQUIRED);
		}
	}

	private static List<Map<String, Object>> normalizeSelfdeliveryAddreeList(Map<String, Object> merged) {
		if (merged == null || !merged.containsKey("addreeList")) {
			return new ArrayList<>();
		}
		Object raw = merged.get("addreeList");
		if (raw == null) {
			return new ArrayList<>();
		}
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("地址参数有误");
		}
		List<Map<String, Object>> out = new ArrayList<>(list.size());
		for (Object elem : list) {
			if (!(elem instanceof Map<?, ?> m)) {
				throw new BadRequestException("地址参数有误");
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(m.size());
			for (Map.Entry<?, ?> e : m.entrySet()) {
				row.put(String.valueOf(e.getKey()), e.getValue());
			}
			out.add(row);
		}
		return out;
	}

	private static void validateSelfdeliveryRows(List<Map<String, Object>> list) {
		for (Map<String, Object> row : list) {
			requireSelfdeliveryField(row, "username", "收货人姓名不能为空");
			requireSelfdeliveryField(row, "telephone", "手机号不能为空");
			requireSelfdeliveryField(row, "regions_id", "地区不能为空");
			requireSelfdeliveryField(row, "adrdetail", "详细地址不能为空");
			requireSelfdeliveryField(row, "regions", "地区不能为空");
		}
	}

	private static void requireSelfdeliveryField(Map<String, Object> row, String field, String message) {
		Object v = row.get(field);
		if (v == null) {
			throw new BadRequestException(message);
		}
		if (String.valueOf(v).trim().isEmpty()) {
			throw new BadRequestException(message);
		}
	}

	@Activated(routeAlias = "presale.recharge.setting.get")
	@RequestMapping(
			value = "/recharge/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "储值功能状态读写")
	public ApiResult<Map<String, Object>> rechargeSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		overlayRechargeStatusFromParameterMap(request, merged);
		return ApiResult.ok(rechargeSettingRedisService.handle(companyId, merged));
	}

	@Activated(routeAlias = "item.store.setting.get")
	@RequestMapping(
			value = "/itemStore/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "商品库存显示状态读写")
	public ApiResult<Map<String, Object>> itemStoreSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		boolean writeBranch =
				input.containsKey("item_store_status") && input.get("item_store_status") != null;
		if (writeBranch) {
			Map<String, Object> data = itemStoreSettingRedisService.setItemStoreSetting(companyId, input);
			return ApiResult.ok(data);
		}
		Map<String, Object> data = itemStoreSettingRedisService.getItemStoreSetting(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "item.store.setting.get")
	@RequestMapping(
			value = "/itemSales/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "商品销量显示状态读写")
	public ApiResult<Map<String, Object>> itemSalesSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		boolean writeBranch =
				input.containsKey("item_sales_status") && input.get("item_sales_status") != null;
		if (writeBranch) {
			Map<String, Object> data = itemSalesSettingRedisService.setItemSalesSetting(companyId, input);
			return ApiResult.ok(data);
		}
		Map<String, Object> data = itemSalesSettingRedisService.getItemSalesSetting(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "item.store.setting.get")
	@GetMapping(value = "/invoice/setting", name = "发票选项显示状态查询")
	public ApiResult<Object> getInvoiceSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		return ApiResult.ok(invoiceSettingRedisService.getInvoiceSetting(companyId));
	}

	@Activated(routeAlias = "item.store.setting.set")
	@PostMapping(value = "/invoice/setting", name = "发票选项显示状态设置")
	public ApiResult<Object> postInvoiceSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		boolean writeBranch =
				input != null
						&& input.containsKey("invoice_status")
						&& input.get("invoice_status") != null;
		if (writeBranch) {
			invoiceSettingRedisService.setInvoiceSetting(companyId, input);
			LinkedHashMap<String, Object> successPayload = new LinkedHashMap<>();
			successPayload.put("status", Boolean.TRUE);
			return ApiResult.ok(successPayload);
		}
		Object data = invoiceSettingRedisService.getInvoiceSetting(companyId);
		return ApiResult.ok(data);
	}

	@RequestMapping(
			value = "/itemshare/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "商品分享设置读写")
	public ApiResult<Object> itemShareSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			return ApiResult.ok(itemShareSettingRedisService.getEffective(companyId));
		}
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		validateItemShareSaveBody(merged);
		try {
			itemShareSettingRedisService.save(companyId, merged);
		} catch (Exception e) {
			throw new ResourceException(ITEM_SHARE_SAVE_FAILED);
		}
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	private static String mailSettingTrimmed(Map<String, Object> merged, String key) {
		if (merged == null || !merged.containsKey(key) || merged.get(key) == null) {
			return "";
		}
		return merged.get(key).toString().trim();
	}

	private MailSettingResponse buildMailSettingResponse(Map<String, Object> data) {
		MailSettingResponse resp = new MailSettingResponse();
		resp.setEmailSmtpPort(mailSettingTrimmed(data, MailSettingRedisService.EMAIL_SMTP_PORT));
		resp.setEmailRelayHost(mailSettingTrimmed(data, MailSettingRedisService.EMAIL_RELAY_HOST));
		resp.setEmailSender(mailSettingTrimmed(data, MailSettingRedisService.EMAIL_SENDER));
		resp.setEmailUser(mailSettingTrimmed(data, MailSettingRedisService.EMAIL_USER));
		String plainPassword = mailSettingTrimmed(data, MailSettingRedisService.EMAIL_PASSWORD);
		resp.setEmailPassword(
				plainPassword.isEmpty() ? "" : mailSettingPasswordCodec.encryptMailPasswordForApiResponse(plainPassword));
		resp.setEmailActivationH5Domain(
				mailSettingTrimmed(data, MailSettingRedisService.EMAIL_ACTIVATION_H5_DOMAIN));
		resp.setEmailActivationPcDomain(
				mailSettingTrimmed(data, MailSettingRedisService.EMAIL_ACTIVATION_PC_DOMAIN));
		return resp;
	}

	private static String sanitizeStoredDomain(String value) {
		String s = value == null ? "" : value.trim();
		s = s.replace("\r", "").replace("\n", "");
		if (s.length() > 512) {
			s = s.substring(0, 512);
		}
		return s;
	}

	private static String mailSettingDomainValue(
			Map<String, Object> merged, Map<String, Object> existing, String key) {
		if (merged != null && merged.containsKey(key)) {
			Object v = merged.get(key);
			return v == null ? "" : String.valueOf(v);
		}
		return mailSettingTrimmed(existing, key);
	}

	private LinkedHashMap<String, String> validateAndBuildMailConfig(
			Map<String, Object> merged, Map<String, Object> existing) {
		String port = mailSettingTrimmed(merged, MailSettingRedisService.EMAIL_SMTP_PORT);
		if (port.isEmpty()) {
			throw new BadRequestException(MAIL_SETTING_SMTP_PORT_REQUIRED);
		}
		String relay = mailSettingTrimmed(merged, MailSettingRedisService.EMAIL_RELAY_HOST);
		if (relay.isEmpty()) {
			throw new BadRequestException(MAIL_SETTING_RELAY_HOST_REQUIRED);
		}
		String sender = mailSettingTrimmed(merged, MailSettingRedisService.EMAIL_SENDER);
		if (sender.isEmpty()) {
			throw new BadRequestException(MAIL_SETTING_SENDER_REQUIRED_OR_INVALID);
		}
		try {
			new InternetAddress(sender).validate();
		} catch (AddressException e) {
			throw new BadRequestException(MAIL_SETTING_SENDER_REQUIRED_OR_INVALID);
		}
		String user = mailSettingTrimmed(merged, MailSettingRedisService.EMAIL_USER);
		if (user.isEmpty()) {
			throw new BadRequestException(MAIL_SETTING_USER_REQUIRED);
		}
		String existingPassword = mailSettingTrimmed(existing, MailSettingRedisService.EMAIL_PASSWORD);
		String passwordInput = mailSettingTrimmed(merged, MailSettingRedisService.EMAIL_PASSWORD);
		String password =
				mailSettingPasswordCodec.resolveMailPasswordFromSaveInput(passwordInput, existingPassword);
		if (password.isEmpty()) {
			throw new BadRequestException(MAIL_SETTING_PASSWORD_REQUIRED);
		}
		String activationH5Domain =
				sanitizeStoredDomain(
						mailSettingDomainValue(
								merged, existing, MailSettingRedisService.EMAIL_ACTIVATION_H5_DOMAIN));
		String activationPcDomain =
				sanitizeStoredDomain(
						mailSettingDomainValue(
								merged, existing, MailSettingRedisService.EMAIL_ACTIVATION_PC_DOMAIN));
		LinkedHashMap<String, String> out = new LinkedHashMap<>(7);
		out.put(MailSettingRedisService.EMAIL_SMTP_PORT, port);
		out.put(MailSettingRedisService.EMAIL_RELAY_HOST, relay);
		out.put(MailSettingRedisService.EMAIL_SENDER, sender);
		out.put(MailSettingRedisService.EMAIL_USER, user);
		out.put(MailSettingRedisService.EMAIL_PASSWORD, password);
		out.put(MailSettingRedisService.EMAIL_ACTIVATION_H5_DOMAIN, activationH5Domain);
		out.put(MailSettingRedisService.EMAIL_ACTIVATION_PC_DOMAIN, activationPcDomain);
		return out;
	}

	private static Map<String, Object> buildShareSettingNestedWhitelist(Map<String, Object> merged) {
		LinkedHashMap<String, Object> output = new LinkedHashMap<>();
		if (merged == null) {
			return output;
		}
		for (String scene : SHARE_SETTING_SCENES) {
			LinkedHashMap<String, Object> outInner = new LinkedHashMap<>();
			Object sceneVal = merged.get(scene);
			if (sceneVal instanceof Map<?, ?> inner) {
				for (String field : SHARE_SETTING_FIELDS) {
					if (inner.containsKey(field)) {
						outInner.put(field, inner.get(field));
					}
				}
			} else {
				for (String field : SHARE_SETTING_FIELDS) {
					String flatKey = scene + "." + field;
					if (merged.containsKey(flatKey)) {
						outInner.put(field, merged.get(flatKey));
					}
				}
			}
			if (!outInner.isEmpty()) {
				output.put(scene, outInner);
			}
		}
		return output;
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static void validateItemShareSaveBody(Map<String, Object> body) {
		if (!body.containsKey("is_open") || body.get("is_open") == null) {
			throw new ResourceException(ITEM_SHARE_IS_OPEN_REQUIRED);
		}
		Object rawOpen = body.get("is_open");
		final String openStr;
		if (rawOpen instanceof Boolean b) {
			openStr = b.booleanValue() ? "true" : "false";
		} else {
			String s = rawOpen.toString().trim();
			if (!"true".equals(s) && !"false".equals(s)) {
				throw new ResourceException(ITEM_SHARE_IS_OPEN_INVALID);
			}
			openStr = s;
		}
		if (!"true".equals(openStr)) {
			return;
		}
		Object vg = body.get("valid_grade");
		if (!isNonEmptyItemShareArrayLike(vg)) {
			throw new ResourceException(ITEM_SHARE_VALID_GRADE_REQUIRED);
		}
		Object msgObj = body.get("msg");
		if (msgObj == null) {
			throw new ResourceException(ITEM_SHARE_MSG_REQUIRED);
		}
		String text =
				(msgObj instanceof String) ? ((String) msgObj).trim() : String.valueOf(msgObj).trim();
		if (text.isEmpty()) {
			throw new ResourceException(ITEM_SHARE_MSG_REQUIRED);
		}
		if (text.length() > 20) {
			throw new ResourceException(ITEM_SHARE_MSG_TOO_LONG);
		}
		validateItemSharePageWhenOpen(body);
	}

	private static int lengthOfJavaArray(Object value) {
		if (value instanceof Object[] a) {
			return a.length;
		}
		if (value instanceof boolean[] a) {
			return a.length;
		}
		if (value instanceof byte[] a) {
			return a.length;
		}
		if (value instanceof short[] a) {
			return a.length;
		}
		if (value instanceof char[] a) {
			return a.length;
		}
		if (value instanceof int[] a) {
			return a.length;
		}
		if (value instanceof long[] a) {
			return a.length;
		}
		if (value instanceof float[] a) {
			return a.length;
		}
		if (value instanceof double[] a) {
			return a.length;
		}
		return 0;
	}

	private static boolean isNonEmptyItemShareArrayLike(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof List<?> list) {
			return !list.isEmpty();
		}
		if (value instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (value.getClass().isArray()) {
			return lengthOfJavaArray(value) > 0;
		}
		return false;
	}

	private static void validateItemSharePageWhenOpen(Map<String, Object> body) {
		if (!body.containsKey("page")) {
			throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
		}
		Object value = body.get("page");
		if (value == null) {
			throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
		}
		if (value instanceof String s) {
			if (s.trim().isEmpty()) {
				throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
			}
			return;
		}
		if (value instanceof Number) {
			return;
		}
		if (value instanceof Map<?, ?>) {
			throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
		}
		if (value instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
			}
			return;
		}
		if (value.getClass().isArray()) {
			if (lengthOfJavaArray(value) == 0) {
				throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
			}
			return;
		}
		if (value instanceof Boolean) {
			throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
		}
		throw new ResourceException(ITEM_SHARE_PAGE_REQUIRED);
	}

	@Activated(routeAlias = "share.parameters.setting.get")
	@GetMapping(value = "/shareParameters/setting", name = "获取小程序分享参数设置")
	public ResponseEntity<?> getShareParametersSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> payload = shareParametersSettingRedisService.read(companyId);
		if (payload == null) {
			Map<String, Object> envelope = new LinkedHashMap<>();
			envelope.put("code", 200);
			envelope.put("msg", "success");
			envelope.put("data", null);
			return ResponseEntity.ok(envelope);
		}
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "share.parameters.setting.save")
	@PostMapping(value = "/shareParameters/setting", name = "保存小程序分享参数设置")
	public ApiResult<Map<String, Object>> saveShareParametersSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		return ApiResult.ok(shareParametersSettingRedisService.save(companyId, merged));
	}

	@Activated(routeAlias = "item.dianwu.setting.get")
	@GetMapping(value = "/dianwu/setting", name = "获取店务端设置")
	public ApiResult<Map<String, Object>> getDianwuSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = dianwuSettingRedisService.getDianwuSetting(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "item.dianwu.setting.save")
	@PostMapping(value = "/dianwu/setting", name = "保存店务端设置")
	public ApiResult<Map<String, Object>> saveDianwuSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> result = dianwuSettingRedisService.saveDianwuSetting(companyId, merged);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "item.price.setting.get")
	@GetMapping(value = "/itemPrice/setting", name = "获取商品价格显示配置")
	public ApiResult<Map<String, Object>> getItemPriceSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = itemPriceSettingRedisService.getItemPriceSetting(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "item.price.setting.set")
	@PostMapping(value = "/itemPrice/setting", name = "保存商品价格显示配置")
	public ApiResult<Map<String, Object>> saveItemPriceSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> result = itemPriceSettingRedisService.saveItemPriceSetting(companyId, merged);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "category.page.setting.get")
	@GetMapping(value = "/categoryPage/setting", name = "获取分类页页面风格设置")
	public ApiResult<Object> getCategoryPageSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Object data = categoryPageSettingRedisService.getCategoryPageSetting(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "category.page.setting.set")
	@PostMapping(value = "/categoryPage/setting", name = "保存分类页页面风格设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveCategoryPageSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		String style = validateCategoryPageStyle(input);
		Map<String, Object> result = categoryPageSettingRedisService.save(companyId, style);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static String validateCategoryPageStyle(Map<String, Object> input) {
		Object raw = input == null ? null : input.get("style");
		if (raw == null) {
			throw new BadRequestException("页面风格必填");
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException("页面风格必填");
		}
		if (!"category".equals(s) && !"items".equals(s)) {
			throw new BadRequestException("页面风格必填");
		}
		return s;
	}

	@Activated(routeAlias = "pharma.industry.setting.set")
	@PostMapping(value = "/pharmaIndustry/setting", name = "保存医药行业设置")
	public ApiResult<Map<String, Object>> saveMedicineSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = companysPharmaIndustrySettingSaveService.save(companyId, merged);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "all.setting.get")
	@GetMapping(value = "/settings", name = "获取全部配置")
	public ApiResult<Map<String, Object>> getAllSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = allSettingsAggregateService.getAllSetting(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "distributor.white.set")
	@PostMapping(value = "/openDivided/setting", name = "保存店铺隔离白名单")
	public ApiResult<Map<String, Object>> openDistributorDividedSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		boolean writeBranch =
				merged.containsKey("open_distributor_divided")
						&& merged.get("open_distributor_divided") != null;
		if (writeBranch) {
			return ApiResult.ok(
					openDistributorDividedSettingRedisService.setOpenDistributorDivided(companyId, merged));
		}
		return ApiResult.ok(openDistributorDividedSettingRedisService.getOpenDistributorDivided(companyId));
	}

	@Activated(routeAlias = "item.startnum.setting.get")
	@RequestMapping(
			value = "/items/startNum/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "自营商品起订量读写")
	public ApiResult<Map<String, Object>> itemStartNumSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		if (request.getParameterMap().containsKey("item_start_num") && !input.containsKey("item_start_num")) {
			input.put(
					"item_start_num",
					request.getParameter("item_start_num") != null ? request.getParameter("item_start_num") : "");
		}
		boolean writeBranch = input.containsKey("item_start_num") && input.get("item_start_num") != null;
		if (writeBranch) {
			return ApiResult.ok(itemStartNumSettingRedisService.setItemStartNumSetting(companyId, input));
		}
		return ApiResult.ok(itemStartNumSettingRedisService.getItemStartNumSetting(companyId));
	}

	@Activated(routeAlias = "supplieritem.startnum.setting.get")
	@RequestMapping(
			value = "/supplierItems/startNum/setting",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "供应商商品起订量读写")
	public ApiResult<Map<String, Object>> supplirItemStartNumSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		if (request.getParameterMap().containsKey("supplier_item_start_num")
				&& !input.containsKey("supplier_item_start_num")) {
			input.put(
					"supplier_item_start_num",
					request.getParameter("supplier_item_start_num") != null
							? request.getParameter("supplier_item_start_num")
							: "");
		}
		boolean writeBranch =
				input.containsKey("supplier_item_start_num") && input.get("supplier_item_start_num") != null;
		if (writeBranch) {
			return ApiResult.ok(
					supplierItemStartNumSettingRedisService.setSupplierItemStartNumSetting(companyId, input));
		}
		return ApiResult.ok(supplierItemStartNumSettingRedisService.getSupplierItemStartNumSetting(companyId));
	}

	@Activated(routeAlias = "mail.setting.get")
	@GetMapping(value = "/mail/setting", name = "获取邮件配置")
	public ApiResult<MailSettingResponse> getMailSetting(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = mailSettingRedisService.readMailSetting(companyId);
		return ApiResult.ok(buildMailSettingResponse(data));
	}

	@Activated(routeAlias = "mail.setting.set")
	@PostMapping(value = "/mail/setting", name = "保存邮件配置")
	public ApiResult<Map<String, Object>> saveMailSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> existing = mailSettingRedisService.readMailSetting(companyId);
		LinkedHashMap<String, String> config = validateAndBuildMailConfig(merged, existing);
		mailSettingRedisService.saveMailSetting(companyId, config);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "kujiale.config.get")
	@GetMapping(value = "/kujiale/config", name = "酷家乐路由配置查询")
	public ResponseEntity<Map<String, Object>> getKujialeConfig(HttpServletRequest request) {
		long companyId = readCompanyIdForKujiale(request);
		Map<String, Object> data = kujialeConfigRedisService.readConfig(companyId);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("success", Boolean.TRUE);
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@Activated(routeAlias = "kujiale.config.set")
	@PostMapping(value = "/kujiale/config", name = "酷家乐路由配置保存")
	public ApiResult<Map<String, Object>> saveKujialeConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdForKujiale(request);
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		validateKujialeSaveBody(input);
		Object ak = input.get("appKey");
		Object sk = input.get("appSecret");
		String appKey = ak instanceof String s ? s.trim() : ((CharSequence) ak).toString().trim();
		String appSecret = sk instanceof String s ? s.trim() : ((CharSequence) sk).toString().trim();
		kujialeConfigRedisService.writeConfig(companyId, appKey, appSecret);
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("success", Boolean.TRUE);
		payload.put("message", "保存成功");
		return ApiResult.ok(payload);
	}

	private static long readCompanyIdForKujiale(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new ResourceException("无法获取公司ID");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new ResourceException("无法获取公司ID");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("无法获取公司ID");
		}
	}

	private static void validateKujialeSaveBody(Map<String, Object> input) {
		if (input == null) {
			throw new BadRequestException("酷家乐保存参数体无效");
		}
		if (!input.containsKey("appKey") || input.get("appKey") == null) {
			throw new BadRequestException("酷家乐 appKey 不能为空");
		}
		Object rawKey = input.get("appKey");
		if (!(rawKey instanceof CharSequence)) {
			throw new BadRequestException("酷家乐 appKey 须为字符串");
		}
		String appKey = rawKey.toString().trim();
		if (appKey.isEmpty()) {
			throw new BadRequestException("酷家乐 appKey 不能为空");
		}
		if (!input.containsKey("appSecret") || input.get("appSecret") == null) {
			throw new BadRequestException("酷家乐 appSecret 不能为空");
		}
		Object rawSecret = input.get("appSecret");
		if (!(rawSecret instanceof CharSequence)) {
			throw new BadRequestException("酷家乐 appSecret 须为字符串");
		}
		String appSecret = rawSecret.toString().trim();
		if (appSecret.isEmpty()) {
			throw new BadRequestException("酷家乐 appSecret 不能为空");
		}
	}
}
