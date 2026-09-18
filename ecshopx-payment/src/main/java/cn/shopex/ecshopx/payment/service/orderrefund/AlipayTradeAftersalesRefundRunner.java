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

package cn.shopex.ecshopx.payment.service.orderrefund;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayOpenapiClientFactory;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 支付宝交易退款（对位 {@code alipay.trade.refund}）。
 */
@Component
public class AlipayTradeAftersalesRefundRunner {

	private final AlipayOpenapiClientFactory alipayOpenapiClientFactory;

	public AlipayTradeAftersalesRefundRunner(AlipayOpenapiClientFactory alipayOpenapiClientFactory) {
		this.alipayOpenapiClientFactory = alipayOpenapiClientFactory;
	}

	public Map<String, Object> refund(
			long companyId,
			long distributorIdForSetting,
			String tradeId,
			long refundBn,
			int refundFeeFen,
			int totalPayFeeFen) {
		if (!StringUtils.hasText(tradeId)) {
			return fail("缺少交易单号");
		}
		final AlipayClient client;
		try {
			client = alipayOpenapiClientFactory.alipayClient(companyId, distributorIdForSetting);
		} catch (BadRequestException e) {
			return fail("请检查支付宝支付相关配置是否完成");
		}
		int useRefund = refundFeeFen > 0 ? refundFeeFen : totalPayFeeFen;
		String refundYuan =
				BigDecimal.valueOf(useRefund).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
		AlipayTradeRefundModel model = new AlipayTradeRefundModel();
		model.setOutTradeNo(tradeId.trim());
		model.setRefundAmount(refundYuan);
		model.setOutRequestNo(String.valueOf(refundBn));
		AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
		request.setBizModel(model);
		AlipayTradeRefundResponse refundResp;
		try {
			refundResp = client.execute(request);
		} catch (AlipayApiException e) {
			return fail("支付宝退款请求失败");
		}
		if (refundResp == null) {
			return fail("支付宝退款失败");
		}
		String code = refundResp.getCode() == null ? "" : refundResp.getCode().trim();
		String subCode = refundResp.getSubCode() == null ? "" : refundResp.getSubCode().trim();
		String subMsg = refundResp.getSubMsg() == null ? "" : refundResp.getSubMsg().trim();
		String msg = refundResp.getMsg() == null ? "" : refundResp.getMsg().trim();
		if ("10000".equals(code)) {
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("status", "SUCCESS");
			String fundChange = refundResp.getFundChange() == null ? "" : refundResp.getFundChange().trim();
			if ("N".equalsIgnoreCase(fundChange)) {
				ok.put("status", "PROCESSING");
			}
			String tradeNo = refundResp.getTradeNo() == null ? "" : refundResp.getTradeNo().trim();
			ok.put("refund_id", StringUtils.hasText(tradeNo) ? tradeNo : String.valueOf(refundBn));
			return ok;
		}
		String err = StringUtils.hasText(subMsg) ? subMsg : (StringUtils.hasText(msg) ? msg : "支付宝退款失败");
		Map<String, Object> f = new LinkedHashMap<>();
		f.put("status", "FAIL");
		f.put("error_code", subCode);
		f.put("error_desc", err);
		return f;
	}

	private static Map<String, Object> fail(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", "FAIL");
		m.put("error_desc", msg);
		return m;
	}
}
