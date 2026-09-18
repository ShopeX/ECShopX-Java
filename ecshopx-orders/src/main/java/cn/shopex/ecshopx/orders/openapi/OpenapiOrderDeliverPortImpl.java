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

package cn.shopex.ecshopx.orders.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverPort;
import cn.shopex.ecshopx.openapi.thirdapi.v1.orders.OpenapiThirdApiV1OrderDeliverService;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2OrderDeliverListService;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2OrderDeliverService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiOrderDeliverPortImpl implements OpenapiOrderDeliverPort {

	private final OpenapiThirdApiV1OrderDeliverService v1DeliverService;
	private final OpenapiThirdApiV2OrderDeliverService v2DeliverService;
	private final OpenapiThirdApiV2OrderDeliverListService v2DeliverListService;

	public OpenapiOrderDeliverPortImpl(
			OpenapiThirdApiV1OrderDeliverService v1DeliverService,
			OpenapiThirdApiV2OrderDeliverService v2DeliverService,
			OpenapiThirdApiV2OrderDeliverListService v2DeliverListService) {
		this.v1DeliverService = v1DeliverService;
		this.v2DeliverService = v2DeliverService;
		this.v2DeliverListService = v2DeliverListService;
	}

	@Override
	public Map<String, Object> createDelivery(
			long companyId, String orderIdRaw, String itemInfoRaw, String lcCode, String lCode) {
		return v1DeliverService.executeOpenapiDeliver(companyId, orderIdRaw, itemInfoRaw, lcCode, lCode);
	}

	@Override
	public Map<String, Object> createDeliveryV2(
			long companyId, String orderIdRaw, String itemInfoRaw, String lcCode, String lCode) {
		return v2DeliverService.executeOpenapiDeliverV2(companyId, orderIdRaw, itemInfoRaw, lcCode, lCode);
	}

	@Override
	public Map<String, Object> getOrderDeliveryListV2(long companyId, String orderIdRaw) {
		return v2DeliverListService.executeGetOrderDeliveryListV2(companyId, orderIdRaw);
	}
}
