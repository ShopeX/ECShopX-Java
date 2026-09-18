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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayOpenapiClientFactory;
import cn.shopex.ecshopx.payment.service.dto.AlipayTradeQueryParsedResponse;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayOpenapiTradeQueryService {

	private final AlipayOpenapiClientFactory alipayOpenapiClientFactory;

	public AlipayOpenapiTradeQueryService(AlipayOpenapiClientFactory alipayOpenapiClientFactory) {
		this.alipayOpenapiClientFactory = alipayOpenapiClientFactory;
	}

	public AlipayTradeQueryParsedResponse queryTrade(
			long companyId, long distributorIdForSetting, String outTradeNo) {
		if (!StringUtils.hasText(outTradeNo)) {
			throw new BadRequestException("支付宝查单失败");
		}
		AlipayClient client = alipayOpenapiClientFactory.alipayClient(companyId, distributorIdForSetting);
		AlipayTradeQueryModel model = new AlipayTradeQueryModel();
		model.setOutTradeNo(outTradeNo.trim());
		AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
		request.setBizModel(model);
		AlipayTradeQueryResponse response;
		try {
			response = client.execute(request);
		} catch (AlipayApiException e) {
			throw new BadRequestException("支付宝查单失败");
		}
		if (response == null) {
			throw new BadRequestException("支付宝查单失败");
		}
		if (!response.isSuccess() || !"10000".equals(response.getCode())) {
			String msg = StringUtils.hasText(response.getSubMsg()) ? response.getSubMsg() : "支付宝查单失败";
			throw new BadRequestException(msg);
		}
		String tradeStatus = response.getTradeStatus() == null ? "" : response.getTradeStatus().trim();
		String tradeNo = response.getTradeNo() == null ? "" : response.getTradeNo().trim();
		String passbackParams = response.getPassbackParams() == null ? "" : response.getPassbackParams().trim();
		String subMsg = response.getSubMsg() == null ? "" : response.getSubMsg().trim();
		return new AlipayTradeQueryParsedResponse(response.getCode(), tradeStatus, tradeNo, passbackParams, subMsg);
	}
}
