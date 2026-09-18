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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayOpenapiClientFactory;
import cn.shopex.ecshopx.payment.service.dto.AlipayTradePayShallowResult;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePayResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Alipay OpenAPI calls used by orders-layer pay param building (app pay order string, barcode pay).
 */
@Service
public class AlipayExternalPaySdkService {

	private final AlipayOpenapiClientFactory alipayOpenapiClientFactory;

	public AlipayExternalPaySdkService(AlipayOpenapiClientFactory alipayOpenapiClientFactory) {
		this.alipayOpenapiClientFactory = alipayOpenapiClientFactory;
	}

	public String pageWapPayFormHtml(
			long companyId,
			long distributorIdForSetting,
			String bizContentJson,
			String returnUrl,
			String notifyUrl) {
		if (!StringUtils.hasText(bizContentJson)) {
			throw new ResourceException("支付失败");
		}
		AlipayClient client = alipayOpenapiClientFactory.alipayClient(companyId, distributorIdForSetting);
		AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
		request.setBizContent(bizContentJson.trim());
		if (StringUtils.hasText(returnUrl)) {
			request.setReturnUrl(returnUrl.trim());
		}
		if (StringUtils.hasText(notifyUrl)) {
			request.setNotifyUrl(notifyUrl.trim());
		}
		try {
			AlipayTradeWapPayResponse response = client.pageExecute(request);
			if (response == null || !StringUtils.hasText(response.getBody())) {
				throw new ResourceException("支付失败");
			}
			return normalizeWapPayFormHtml(response.getBody());
		} catch (AlipayApiException e) {
			throw new ResourceException("支付失败");
		}
	}

	private static String normalizeWapPayFormHtml(String html) {
		if (!StringUtils.hasText(html)) {
			return html;
		}
		if (html.contains("id=\"alipay_submit\"") || html.contains("id='alipay_submit'")) {
			return html;
		}
		return html.replaceFirst(
				"<form\\s+name=\"punchout_form\"",
				"<form id=\"alipay_submit\" name=\"alipay_submit\"");
	}

	public String sdkAppPayOrderString(long companyId, long distributorIdForSetting, String bizContentJson) {
		if (!StringUtils.hasText(bizContentJson)) {
			throw new ResourceException("支付失败");
		}
		AlipayClient client = alipayOpenapiClientFactory.alipayClient(companyId, distributorIdForSetting);
		AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
		request.setBizContent(bizContentJson.trim());
		try {
			AlipayTradeAppPayResponse response = client.sdkExecute(request);
			if (response == null) {
				throw new ResourceException("支付失败");
			}
			String body = response.getBody();
			if (StringUtils.hasText(body)) {
				return body;
			}
			if (StringUtils.hasText(response.getOrderStr())) {
				return response.getOrderStr();
			}
			throw new ResourceException("支付失败");
		} catch (AlipayApiException e) {
			throw new ResourceException("支付失败");
		}
	}

	public AlipayTradePayShallowResult tradePayBarcodeFaceToFace(
			long companyId,
			long distributorIdForSetting,
			String tradeId,
			String totalYuan,
			String subject,
			String authCode) {
		AlipayTradePayModel model = new AlipayTradePayModel();
		model.setOutTradeNo(tradeId);
		model.setTotalAmount(totalYuan);
		model.setSubject(subject);
		model.setScene("bar_code");
		model.setAuthCode(authCode);
		model.setProductCode("FACE_TO_FACE_PAYMENT");
		AlipayClient client = alipayOpenapiClientFactory.alipayClient(companyId, distributorIdForSetting);
		AlipayTradePayRequest request = new AlipayTradePayRequest();
		request.setBizModel(model);
		try {
			AlipayTradePayResponse response = client.execute(request);
			return AlipayTradePayShallowResult.from(response);
		} catch (AlipayApiException e) {
			String msg = StringUtils.hasText(e.getErrMsg()) ? e.getErrMsg() : "支付失败";
			throw new ResourceException(msg);
		}
	}
}
