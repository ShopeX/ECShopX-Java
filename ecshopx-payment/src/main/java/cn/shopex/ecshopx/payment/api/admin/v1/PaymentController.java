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

package cn.shopex.ecshopx.payment.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.payment.AdminPaymentSettingListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.payment.dto.PaymentOpenStatusSnapshot;
import cn.shopex.ecshopx.payment.service.CompanyPaymentOpenStatusReadService;
import cn.shopex.ecshopx.payment.service.admin.HfpayVersionStatusService;
import cn.shopex.ecshopx.payment.service.admin.PaymentRsaKeyGenerateService;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingInputResolver;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingReadService;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingSaveService;
import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import jakarta.servlet.http.HttpServletRequest;
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

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("paymentAdminV1Payment")
@RequestMapping("/api/v1/trade/payment")
public class PaymentController {

	private final PaymentSettingInputResolver paymentSettingInputResolver;
	private final PaymentSettingReadService paymentSettingReadService;
	private final PaymentSettingSaveService paymentSettingSaveService;
	private final HfpayVersionStatusService hfpayVersionStatusService;
	private final AdminPaymentSettingListPort adminPaymentSettingListPort;
	private final CompanyPaymentOpenStatusReadService companyPaymentOpenStatusReadService;
	private final PaymentRsaKeyGenerateService paymentRsaKeyGenerateService;

	public PaymentController(
			PaymentSettingInputResolver paymentSettingInputResolver,
			PaymentSettingReadService paymentSettingReadService,
			PaymentSettingSaveService paymentSettingSaveService,
			HfpayVersionStatusService hfpayVersionStatusService,
			AdminPaymentSettingListPort adminPaymentSettingListPort,
			CompanyPaymentOpenStatusReadService companyPaymentOpenStatusReadService,
			PaymentRsaKeyGenerateService paymentRsaKeyGenerateService) {
		this.paymentSettingInputResolver = paymentSettingInputResolver;
		this.paymentSettingReadService = paymentSettingReadService;
		this.paymentSettingSaveService = paymentSettingSaveService;
		this.hfpayVersionStatusService = hfpayVersionStatusService;
		this.adminPaymentSettingListPort = adminPaymentSettingListPort;
		this.companyPaymentOpenStatusReadService = companyPaymentOpenStatusReadService;
		this.paymentRsaKeyGenerateService = paymentRsaKeyGenerateService;
	}

	@Activated(routeAlias = "trade.payment.setting.set")
	@PostMapping(value = "/setting", name = "支付配置保存")
	public ResponseEntity<ApiResult<Object>> setPaymentSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		PaymentSettingCommand cmd = paymentSettingInputResolver.resolve(request, body);
		Object result = paymentSettingSaveService.setPaymentSetting(cmd);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "trade.payment.setting.get")
	@GetMapping(value = "/setting", name = "支付配置")
	public ResponseEntity<ApiResult<Object>> getPaymentSetting(
			HttpServletRequest request,
			@RequestParam(value = "pay_type", required = false) String payType,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(value = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		String cc = countryCode == null ? "" : countryCode.trim();
		if (!StringUtils.hasText(cc)) {
			cc = "zh-CN";
		}
		if (payType == null || !StringUtils.hasText(payType.trim())) {
			throw new BadRequestException("暂时不支持");
		}
		long distributorId = paymentSettingInputResolver.parseDistributorIdForPaymentList(distributorIdParam);
		Object data = paymentSettingReadService.getPaymentSetting(request, payType.trim(), distributorId, cc);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "trade.payment.open.status.get")
	@GetMapping(value = "/open-status", name = "支付开关")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPaymentOpenStatus(HttpServletRequest request) {
		long companyId = paymentSettingInputResolver.requireCompanyId(request);
		PaymentOpenStatusSnapshot s = companyPaymentOpenStatusReadService.getOpenStatus(companyId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("wxpay", Map.of("is_open", s.isWxpayOpen()));
		body.put("alipay", Map.of("is_open", s.isAlipayOpen()));
		body.put("chinaumspay", Map.of("is_open", s.isChinaumspayOpen()));
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "trade.payment.setting.list")
	@GetMapping(value = "/list", name = "支付配置列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getPaymentSettingList(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam) {
		long companyId = paymentSettingInputResolver.requireCompanyId(request);
		long distributorId = paymentSettingInputResolver.parseDistributorIdForPaymentList(distributorIdParam);
		List<Map<String, Object>> data =
				adminPaymentSettingListPort.getPaymentSettingList(companyId, distributorId, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "trade.payment.hfpay.status")
	@GetMapping(value = "/hfpayversionstatus", name = "汇付版本状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> getHfpayVersionStatus() {
		return ResponseEntity.ok(ApiResult.ok(hfpayVersionStatusService.getHfpayVersionStatus()));
	}

	@Activated(routeAlias = "trade.payment.rsa.gen")
	@GetMapping(value = "/rsakey", name = "生成RSA密钥")
	public ResponseEntity<ApiResult<Map<String, String>>> genRsaKey() {
		return ResponseEntity.ok(ApiResult.ok(paymentRsaKeyGenerateService.genRsaKey()));
	}
}
