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

package cn.shopex.ecshopx.dispatch.integration;

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.common.port.hfpay.HfpayDistributorWithdrawSuccessEventPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HfpayDistributorWithdrawSuccessEventPublishPortImpl implements HfpayDistributorWithdrawSuccessEventPublishPort {

	private final DispatchFacade dispatchFacade;

	public HfpayDistributorWithdrawSuccessEventPublishPortImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publishSyncAfterScheduleWithdrawSuccess(
			long hfpayCashRecordId, long companyId, long distributorId, int transAmtFen, String orderId) {
		LinkedHashMap<String, Object> entities = new LinkedHashMap<>();
		entities.put("hfpay_cash_record_id", hfpayCashRecordId);
		entities.put("company_id", companyId);
		entities.put("distributor_id", distributorId);
		entities.put("trans_amt", transAmtFen);
		entities.put("order_id", orderId == null ? "" : orderId);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);
		dispatchFacade.publishEvent(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW_SUCCESS,
				payload,
				DispatchOptions.eventDefaults());
	}
}
