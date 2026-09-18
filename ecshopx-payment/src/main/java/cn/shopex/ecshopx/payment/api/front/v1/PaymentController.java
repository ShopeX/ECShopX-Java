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

package cn.shopex.ecshopx.payment.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.payment.FrontWithdrawPayTypeListPort;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.payment.service.AlipayH5SyncReturnService;
import cn.shopex.ecshopx.payment.service.admin.HfpayVersionStatusService;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingInputResolver;
import cn.shopex.ecshopx.payment.service.front.FrontPaymentSettingReadService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
		notFound = true)
@FrontNoAuth
@RestController("paymentFrontV1Payment")
@RequestMapping("/api/v1/h5app/wxapp/trade")
public class PaymentController {

	private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

	private final AlipayH5SyncReturnService alipayH5SyncReturnService;
	private final FrontPaymentSettingReadService frontPaymentSettingReadService;
	private final HfpayVersionStatusService hfpayVersionStatusService;
	private final PaymentSettingInputResolver paymentSettingInputResolver;
	private final ObjectMapper objectMapper;
	private final FrontWithdrawPayTypeListPort frontWithdrawPayTypeListPort;

	public PaymentController(
			AlipayH5SyncReturnService alipayH5SyncReturnService,
			FrontPaymentSettingReadService frontPaymentSettingReadService,
			HfpayVersionStatusService hfpayVersionStatusService,
			PaymentSettingInputResolver paymentSettingInputResolver,
			ObjectMapper objectMapper,
			FrontWithdrawPayTypeListPort frontWithdrawPayTypeListPort) {
		this.alipayH5SyncReturnService = alipayH5SyncReturnService;
		this.frontPaymentSettingReadService = frontPaymentSettingReadService;
		this.hfpayVersionStatusService = hfpayVersionStatusService;
		this.paymentSettingInputResolver = paymentSettingInputResolver;
		this.objectMapper = objectMapper;
		this.frontWithdrawPayTypeListPort = frontWithdrawPayTypeListPort;
	}

	@GetMapping(value = "/payment/list", name = "支付列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getPaymentSettingList(
			HttpServletRequest request,
			@RequestParam(value = "platform", required = false, defaultValue = "") String platform,
			@RequestParam(value = "order_type", required = false, defaultValue = "") String orderType,
			@RequestParam(value = "country_code", required = false, defaultValue = "zh-CN") String countryCodeRaw) {
		try {
			String json = objectMapper.writeValueAsString(FlexibleHttpServletParameterMap.toObjectMap(request));
			log.info("payment::getPaymentSettingList::request====>{}", json);
		} catch (JsonProcessingException e) {
			log.warn("payment::getPaymentSettingList::request====>{}", e.toString());
		}

		Object rawCompany = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (rawCompany instanceof Number) {
			companyId = ((Number) rawCompany).longValue();
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		long distributorId = paymentSettingInputResolver.parseDistributorIdForPaymentList(request.getParameter("distributor_id"));
		String countryCodeNormalized = normalizeMagicLang(countryCodeRaw);

		List<Map<String, Object>> list = frontPaymentSettingReadService.getPaymentSettingList(
				request, companyId, distributorId, platform, orderType, countryCodeNormalized);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	private static String normalizeMagicLang(String countryCodeRaw) {
		String s = countryCodeRaw == null ? "" : countryCodeRaw.trim();
		if (s.isEmpty() || "0".equals(s)) {
			return "zh-CN";
		}
		return s;
	}

	@GetMapping(value = "/payment/listInfo", name = "支付列表详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPaymentSettingListInfo(
			HttpServletRequest request,
			@RequestParam(value = "platform", required = false, defaultValue = "") String platform,
			@RequestParam(value = "country_code", required = false, defaultValue = "zh-CN") String countryCodeRaw) {
		Object rawCompany = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (rawCompany instanceof Number) {
			companyId = ((Number) rawCompany).longValue();
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		String p = platform == null ? "" : platform;
		long distributorId = paymentSettingInputResolver.parseDistributorIdForPaymentList(request.getParameter("distributor_id"));
		String countryCodeNormalized = normalizeMagicLang(countryCodeRaw);

		List<Map<String, Object>> list = frontPaymentSettingReadService.getPaymentSettingList(
				request, companyId, distributorId, p, "", countryCodeNormalized);
		return ResponseEntity.ok(ApiResult.ok(buildListInfoPayload(list)));
	}

	private static Map<String, Object> buildListInfoPayload(List<Map<String, Object>> result) {
		List<Map<String, Object>> rows = result == null ? List.of() : result;
		Map<String, Map<String, Object>> byPayType = rows.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(
						m -> String.valueOf(m.get("pay_type_code")),
						m -> m,
						(a, b) -> b,
						LinkedHashMap::new));
		boolean isAdapay = byPayType.containsKey("adapay");
		boolean isWxpay = byPayType.containsKey("wxpay");
		boolean isPaypal = byPayType.containsKey("paypal");
		boolean isDoumenIntl = byPayType.containsKey("doumen_intl");
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("list", result);
		data.put("is_adapay", isAdapay);
		data.put("is_wxpay", isWxpay);
		data.put("is_paypal", isPaypal);
		data.put("is_doumen_intl", isDoumenIntl);
		return data;
	}

	@GetMapping(value = "/payment/get_setting", name = "支付配置")
	public ResponseEntity<ApiResult<Object>> getPaymentSetting(
			HttpServletRequest request,
			@RequestParam(value = "pay_type", required = false) String payType,
			@RequestParam(value = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		Object rawCompany = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (rawCompany instanceof Number) {
			companyId = ((Number) rawCompany).longValue();
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		String normalizedPayType = payType == null ? "" : payType;
		String cc = countryCode == null ? "" : countryCode.trim();
		if (!StringUtils.hasText(cc)) {
			cc = "zh-CN";
		}

		Object data = frontPaymentSettingReadService.getPaymentSetting(companyId, normalizedPayType, cc);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/withdraw/list", name = "提现方式")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getWithDrawList(
			HttpServletRequest request,
			@RequestParam(value = "country_code", required = false, defaultValue = "zh-CN") String countryCodeRaw) {
		Object rawCompany = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (rawCompany instanceof Number) {
			companyId = ((Number) rawCompany).longValue();
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		String countryCodeNormalized = normalizeMagicLang(countryCodeRaw);
		List<Map<String, Object>> list =
				frontWithdrawPayTypeListPort.getWithDrawList(companyId, request, countryCodeNormalized);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	@GetMapping(value = "/payment/hfpayversionstatus", name = "汇付状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> getHfpayVersionStatus() {
		return ResponseEntity.ok(ApiResult.ok(hfpayVersionStatusService.getHfpayVersionStatus()));
	}

	@GetMapping(
			value = "/payment/alipay/result",
			name = "支付宝结果页",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> alipayResult(HttpServletRequest request) {
		String status = alipayH5SyncReturnService.alipayResult(request);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", status);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
