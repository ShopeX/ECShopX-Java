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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInvoiceCallbackPort;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(SaasErpInvoiceCallbackPort.class)
public class InvoiceCallbackSaasErpAct implements SaasErpActHandler {

	private final SaasErpInvoiceCallbackPort invoiceCallbackPort;

	public InvoiceCallbackSaasErpAct(SaasErpInvoiceCallbackPort invoiceCallbackPort) {
		this.invoiceCallbackPort = invoiceCallbackPort;
	}

	@Override
	public String act() {
		return "b2c.invoice.send";
	}

	@Override
	public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
		String invoiceMemo = params.get("invoice_memo");
		if (invoiceMemo == null || invoiceMemo.isBlank()) {
			return SaasErpInboundResult.fail("发票状态缺少！");
		}
		String blueInvoiceCode = params.get("blue_invoice_code");
		if (blueInvoiceCode == null || blueInvoiceCode.isBlank()) {
			return SaasErpInboundResult.fail("发票代码缺少！");
		}
		String blueInvoiceNo = params.get("blue_invoice_no");
		if (blueInvoiceNo == null || blueInvoiceNo.isBlank()) {
			return SaasErpInboundResult.fail("发票号缺少！");
		}
		String invoiceNo = params.get("invoice_no");
		if (invoiceNo == null || invoiceNo.isBlank()) {
			return SaasErpInboundResult.fail("发票申请单号缺少！");
		}
		String orderBn = params.get("order_bn");
		if (orderBn == null || orderBn.isBlank()) {
			return SaasErpInboundResult.fail("订单号缺少！");
		}
		if ("2".equals(invoiceMemo.trim())) {
			String pdfInfo = params.get("pdf_info");
			if (pdfInfo == null || pdfInfo.isBlank()) {
				return SaasErpInboundResult.fail("缺少发票文件！");
			}
		}

		Map<String, Object> callbackResult = invoiceCallbackPort.handle(params);
		Object success = callbackResult.get("success");
		boolean ok = Boolean.TRUE.equals(success) || "true".equals(success);

		Object message = callbackResult.get("message");
		String msg = message == null ? "" : String.valueOf(message);

		Object info =
				callbackResult.containsKey("data") ? callbackResult.get("data") : List.of();

		if (ok) {
			return SaasErpInboundResult.ok(msg, info);
		}
		return SaasErpInboundResult.fail(msg, info);
	}
}
