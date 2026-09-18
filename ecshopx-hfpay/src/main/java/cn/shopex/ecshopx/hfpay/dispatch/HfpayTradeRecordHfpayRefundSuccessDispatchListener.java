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

package cn.shopex.ecshopx.hfpay.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.hfpay.service.HfpayTradeRecordService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HfpayTradeRecordHfpayRefundSuccessDispatchListener implements DispatchListener {

	private final HfpayTradeRecordService hfpayTradeRecordService;

	public HfpayTradeRecordHfpayRefundSuccessDispatchListener(HfpayTradeRecordService hfpayTradeRecordService) {
		this.hfpayTradeRecordService = hfpayTradeRecordService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Object oid = payload.get("order_id");
		if (oid == null) {
			return;
		}
		String orderId = String.valueOf(oid).trim();
		if (!StringUtils.hasText(orderId)) {
			return;
		}
		Object rbn = payload.get("refund_bn");
		if (rbn == null) {
			return;
		}
		long refundBn;
		if (rbn instanceof Number n) {
			refundBn = n.longValue();
		} else {
			String s = String.valueOf(rbn).trim();
			if (!StringUtils.hasText(s)) {
				return;
			}
			try {
				refundBn = Long.parseLong(s);
			} catch (NumberFormatException e) {
				return;
			}
		}
		hfpayTradeRecordService.refundSuccess(orderId, refundBn);
	}
}
