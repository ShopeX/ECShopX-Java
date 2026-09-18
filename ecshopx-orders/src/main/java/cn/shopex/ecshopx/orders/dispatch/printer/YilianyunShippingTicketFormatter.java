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

package cn.shopex.ecshopx.orders.dispatch.printer;

import java.util.List;
import org.springframework.util.StringUtils;

public class YilianyunShippingTicketFormatter {

	private static final String NL = "\n";
	private static final String DASH = "--------------------------------";

	public String format(YilianyunShippingTicketModel model) {
		StringBuilder sb = new StringBuilder();
		sb.append("<FS2><center>发货小票</center></FS2>").append(NL);
		appendCenter(sb, blankToEmpty(model.storeName()));
		appendLine(sb, "单号:", blankToEmpty(model.orderId()));
		appendLine(sb, "时间:", blankToEmpty(model.orderTime()));
		appendLine(sb, "下单店铺:", blankToEmpty(model.orderStoreName()));
		if (StringUtils.hasText(model.printerOperator())) {
			appendLine(sb, "打印人员:", model.printerOperator().trim());
		}
		sb.append(DASH).append(NL);
		sb.append("品名").append(NL);
		sb.append("<table><tr><td>单价</td><td>数量</td><td>实付金额</td></tr></table>").append(NL);
		List<YilianyunShippingTicketModel.Item> items =
				model.items() == null ? List.of() : model.items();
		for (YilianyunShippingTicketModel.Item item : items) {
			sb.append(blankToEmpty(item.displayName())).append(NL);
			sb.append("<table><tr><td>")
					.append(blankToEmpty(item.unitPriceYuan()))
					.append("</td><td>")
					.append(blankToEmpty(item.quantityText()))
					.append("</td><td>")
					.append(blankToEmpty(item.linePaidYuan()))
					.append("</td></tr></table>")
					.append(NL);
		}
		sb.append(DASH).append(NL);
		appendLine(sb, "总数:", String.valueOf(items.size()));
		appendLine(sb, "已付:", blankToEmpty(model.paidYuan()));
		appendLine(sb, "支付方式:", blankToEmpty(model.payTypeLabel()));
		sb.append("<FS2>实付:").append(blankToEmpty(model.paidYuan())).append("</FS2>").append(NL);
		sb.append(DASH).append(NL);
		appendLine(sb, "收件人:", blankToEmpty(model.receiverName()));
		appendLine(sb, "收件电话:", blankToEmpty(model.receiverMobile()));
		appendLine(sb, "收货地址:", blankToEmpty(model.receiverAddress()));
		if (StringUtils.hasText(model.deliveryTime())) {
			appendLine(sb, "送达时间:", model.deliveryTime().trim());
		}
		if (StringUtils.hasText(model.buyerRemark())) {
			appendLine(sb, "买家备注:", model.buyerRemark().trim());
		}
		if (StringUtils.hasText(model.storePhone()) && !"0".equals(model.storePhone().trim())) {
			appendLine(sb, "店铺电话:", model.storePhone().trim());
		}
		if (StringUtils.hasText(model.storeAddress())) {
			appendLine(sb, "店铺地址:", model.storeAddress().trim());
		}
		sb.append(NL).append(NL).append(NL);
		return sb.toString();
	}

	private static void appendCenter(StringBuilder sb, String value) {
		if (!StringUtils.hasText(value)) {
			return;
		}
		sb.append("<center>").append(value).append("</center>").append(NL);
	}

	private static void appendLine(StringBuilder sb, String label, String value) {
		sb.append(label).append(value).append(NL);
	}

	private static String blankToEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}
