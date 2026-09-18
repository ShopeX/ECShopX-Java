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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailSalespersonLookupPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailSalespromoterService {

	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final AdminOrderDetailSalespersonLookupPort adminOrderDetailSalespersonLookupPort;

	public WxappOrderDetailSalespromoterService(
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			AdminOrderDetailSalespersonLookupPort adminOrderDetailSalespersonLookupPort) {
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.adminOrderDetailSalespersonLookupPort = adminOrderDetailSalespersonLookupPort;
	}

	@SuppressWarnings("unchecked")
	public void fillPromoterAndSalesperson(long companyId, Map<String, Object> orderAssocRow, Map<String, Object> result) {
		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		if (orderInfo == null) {
			return;
		}
		long promoterUserId = longVal(orderAssocRow.get("promoter_user_id"));
		orderInfo.put("promoter_user_id", promoterUserId > 0L ? promoterUserId : 0L);

		long distributorId = longVal(orderInfo.get("distributor_id"));
		Map<String, Object> distributorInfo =
				adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(companyId, String.valueOf(distributorId));
		boolean openSalesman = truthy(distributorInfo.get("is_open_salesman"));

		Map<String, Object> salespersonInfo;
		long outPromoter = promoterUserId;
		if (promoterUserId > 0L && openSalesman) {
			salespersonInfo =
					new LinkedHashMap<>(
							adminOrderDetailSalespersonLookupPort.loadSalespersonForOrderDetail(companyId, promoterUserId));
			if (salespersonInfo.isEmpty() || longVal(salespersonInfo.get("user_id")) <= 0L) {
				salespersonInfo = emptySalesperson();
				outPromoter = 0L;
			}
		} else {
			salespersonInfo = emptySalesperson();
			outPromoter = 0L;
		}
		orderInfo.put("promoter_user_id", outPromoter);
		orderInfo.put("salespersonInfo", salespersonInfo);
	}

	private static Map<String, Object> emptySalesperson() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", "");
		m.put("name", "");
		m.put("mobile", "");
		return m;
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		return true;
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
