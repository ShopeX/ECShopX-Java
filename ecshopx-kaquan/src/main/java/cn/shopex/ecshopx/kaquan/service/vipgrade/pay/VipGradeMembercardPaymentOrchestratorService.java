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

package cn.shopex.ecshopx.kaquan.service.vipgrade.pay;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class VipGradeMembercardPaymentOrchestratorService {

	private final VipGradeMembercardWxpayPaymentService wxpayPaymentService;
	private final VipGradeMembercardAdapayPaymentService adapayPaymentService;
	private final VipGradeMembercardBsPayPaymentService bsPayPaymentService;
	private final VipGradeMembercardAlipayMiniPaymentService alipayMiniPaymentService;

	public VipGradeMembercardPaymentOrchestratorService(
			VipGradeMembercardWxpayPaymentService wxpayPaymentService,
			VipGradeMembercardAdapayPaymentService adapayPaymentService,
			VipGradeMembercardBsPayPaymentService bsPayPaymentService,
			VipGradeMembercardAlipayMiniPaymentService alipayMiniPaymentService) {
		this.wxpayPaymentService = wxpayPaymentService;
		this.adapayPaymentService = adapayPaymentService;
		this.bsPayPaymentService = bsPayPaymentService;
		this.alipayMiniPaymentService = alipayMiniPaymentService;
	}

	public Map<String, Object> pay(String authorizerAppId, String wxaAppId, Map<String, Object> data, boolean secondArgFalse) {
		Object rawPt = data.get("pay_type");
		String payType = rawPt == null ? "" : rawPt.toString().trim().toLowerCase(Locale.ROOT);
		if (payType.isEmpty()) {
			throw new BadRequestException("不支持该支付方式");
		}
		return switch (payType) {
			case "wxpay" -> wxpayPaymentService.pay(authorizerAppId, wxaAppId, data, secondArgFalse);
			case "adapay" -> adapayPaymentService.pay(authorizerAppId, wxaAppId, data, secondArgFalse);
			case "bspay" -> bsPayPaymentService.pay(authorizerAppId, wxaAppId, data, secondArgFalse);
			case "alipaymini" -> alipayMiniPaymentService.pay(authorizerAppId, wxaAppId, data, secondArgFalse);
			case "wxpaypc" -> throw new BadRequestException("不支持该支付方式：wxpaypc");
			case "alipay" -> throw new BadRequestException("不支持该支付方式：alipay");
			case "point" -> throw new BadRequestException("不支持该支付方式：point");
			case "deposit" -> throw new BadRequestException("不支持该支付方式：deposit");
			case "alipayh5" -> throw new BadRequestException("不支持该支付方式：alipayh5");
			case "alipayapp" -> throw new BadRequestException("不支持该支付方式：alipayapp");
			case "wxpayh5" -> throw new BadRequestException("不支持该支付方式：wxpayh5");
			case "wxpayjs" -> throw new BadRequestException("不支持该支付方式：wxpayjs");
			case "wxpayapp" -> throw new BadRequestException("不支持该支付方式：wxpayapp");
			case "alipaypos" -> throw new BadRequestException("不支持该支付方式：alipaypos");
			case "wxpaypos" -> throw new BadRequestException("不支持该支付方式：wxpaypos");
			case "hfpay" -> throw new BadRequestException("不支持该支付方式：hfpay");
			case "chinaums" -> throw new BadRequestException("不支持该支付方式：chinaums");
			case "pos" -> throw new BadRequestException("不支持该支付方式：pos");
			case "offline_pay" -> throw new BadRequestException("不支持该支付方式：offline_pay");
			case "paypal" -> throw new BadRequestException("不支持该支付方式：paypal");
			default -> throw new BadRequestException("不支持该支付方式");
		};
	}
}
