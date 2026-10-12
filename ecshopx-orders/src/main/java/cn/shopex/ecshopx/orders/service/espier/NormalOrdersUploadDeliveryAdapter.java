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

package cn.shopex.ecshopx.orders.service.espier;

import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadSepItem;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadShipCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NormalOrdersUploadDeliveryAdapter implements NormalOrdersUploadDeliveryPort {

	private final AdminNormalOrderDeliveryCoreService deliveryCore;
	private final NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver;
	private final ObjectMapper objectMapper;

	public NormalOrdersUploadDeliveryAdapter(
			AdminNormalOrderDeliveryCoreService deliveryCore,
			NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver,
			ObjectMapper objectMapper) {
		this.deliveryCore = deliveryCore;
		this.deliveryCorpResolver = deliveryCorpResolver;
		this.objectMapper = objectMapper;
	}

	@Override
	public void deliverWhole(UploadShipCommand command) {
		Map<String, Object> params = baseParams(command);
		deliveryCore.deliverUploadWholeRemaining(params, null);
	}

	@Override
	public void deliverSep(UploadShipCommand command, List<UploadSepItem> items) {
		Map<String, Object> params = baseParams(command);
		params.put("delivery_type", "sep");
		params.put("sepInfo", toSepInfoJson(items));
		deliveryCore.deliveryNormalPhysical(params, null, "normal");
	}

	private Map<String, Object> baseParams(UploadShipCommand command) {
		String deliveryCorp =
				deliveryCorpResolver.resolveDeliveryCorpCode(
						command.companyId(), command.deliveryCorpName(), command.supplierId());
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", command.companyId());
		params.put("order_id", command.orderId());
		params.put("supplier_id", (int) command.supplierId());
		params.put("delivery_corp", deliveryCorp);
		params.put("delivery_code", command.deliveryCode());
		params.put("operator_id", command.operatorId());
		params.put("operator_type", command.operatorType());
		return params;
	}

	private String toSepInfoJson(List<UploadSepItem> items) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (UploadSepItem item : items) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", item.orderItemId());
			row.put("delivery_num", item.deliveryNum());
			rows.add(row);
		}
		try {
			return objectMapper.writeValueAsString(rows);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
