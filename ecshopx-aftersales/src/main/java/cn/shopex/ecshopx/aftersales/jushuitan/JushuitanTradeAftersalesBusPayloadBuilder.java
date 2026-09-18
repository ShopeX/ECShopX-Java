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

package cn.shopex.ecshopx.aftersales.jushuitan;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JushuitanTradeAftersalesBusPayloadBuilder {

	public Map<String, Object> build(Aftersales main, Map<String, Object> context) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", main.getAftersalesBn());
		m.put("company_id", main.getCompanyId());
		m.put("order_id", main.getOrderId());
		m.put("distributor_id", main.getDistributorId() == null ? 0L : main.getDistributorId());
		m.put("shop_id", main.getShopId() == null ? 0L : main.getShopId());
		m.put("supplier_id", main.getSupplierId() == null ? 0 : main.getSupplierId());
		m.put("user_id", main.getUserId() == null ? 0L : main.getUserId());
		m.put("aftersales_type", main.getAftersalesType());
		m.put("aftersales_status", main.getAftersalesStatus() == null ? 0 : main.getAftersalesStatus());
		m.put("progress", main.getProgress() == null ? 0 : main.getProgress());
		m.put("reason", main.getReason() == null ? "" : main.getReason());
		m.put("description", main.getDescription() == null ? "" : main.getDescription());
		m.put("evidence_pic", main.getEvidencePic() == null ? "" : main.getEvidencePic());
		long salesmanId = longVal(context.get("salesman_id"));
		if (salesmanId == 0L && main.getSalesmanId() != null) {
			salesmanId = main.getSalesmanId();
		}
		m.put("salesman_id", salesmanId);
		m.put("contact", main.getContact() == null ? "" : main.getContact());
		m.put("mobile", main.getMobile() == null ? "" : main.getMobile());
		m.put("merchant_id", main.getMerchantId() == null ? 0L : main.getMerchantId());
		m.put(
				"self_delivery_operator_id",
				main.getSelfDeliveryOperatorId() == null ? 0L : main.getSelfDeliveryOperatorId());
		m.put("is_partial_cancel", Boolean.TRUE.equals(main.getIsPartialCancel()));
		m.put("return_type", main.getReturnType() == null ? "logistics" : main.getReturnType());
		m.put("freight", main.getFreight() == null ? 0 : main.getFreight());
		m.put("freight_type", main.getFreightType() == null ? "cash" : main.getFreightType());
		m.put(
				"return_distributor_id",
				main.getReturnDistributorId() == null ? 0L : main.getReturnDistributorId());
		String addr = main.getAftersalesAddress();
		m.put("aftersales_address", addr == null ? "" : addr);
		return m;
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
