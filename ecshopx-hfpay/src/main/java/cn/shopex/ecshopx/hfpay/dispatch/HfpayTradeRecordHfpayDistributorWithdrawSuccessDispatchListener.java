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
public class HfpayTradeRecordHfpayDistributorWithdrawSuccessDispatchListener implements DispatchListener {

	private final HfpayTradeRecordService hfpayTradeRecordService;

	public HfpayTradeRecordHfpayDistributorWithdrawSuccessDispatchListener(HfpayTradeRecordService hfpayTradeRecordService) {
		this.hfpayTradeRecordService = hfpayTradeRecordService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Object ent = payload.get("entities");
		if (!(ent instanceof Map<?, ?> rawMap)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) rawMap;

		Object cid = entities.get("company_id");
		if (cid == null) {
			return;
		}
		long companyId = toLong(cid);

		Object did = entities.get("distributor_id");
		long distributorId = did == null ? 0L : toLong(did);

		Object oid = entities.get("order_id");
		if (oid == null) {
			return;
		}
		String orderId = String.valueOf(oid).trim();
		if (!StringUtils.hasText(orderId)) {
			return;
		}

		Object tamt = entities.get("trans_amt");
		int transAmtFen = parseTransAmtFen(tamt);
		if (transAmtFen < 1) {
			return;
		}

		hfpayTradeRecordService.withdraw(companyId, distributorId, transAmtFen, orderId);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseTransAmtFen(Object tamt) {
		if (tamt == null) {
			return 0;
		}
		if (tamt instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(tamt).trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
