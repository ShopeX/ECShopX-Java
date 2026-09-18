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

package cn.shopex.ecshopx.youshu.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderPaySuccessSrDataSyncService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NormalOrderPaySuccessYoushuDispatchListener implements DispatchListener {

	private final YoushuNormalOrderPaySuccessSrDataSyncService youshuNormalOrderPaySuccessSrDataSyncService;

	public NormalOrderPaySuccessYoushuDispatchListener(
			YoushuNormalOrderPaySuccessSrDataSyncService youshuNormalOrderPaySuccessSrDataSyncService) {
		this.youshuNormalOrderPaySuccessSrDataSyncService = youshuNormalOrderPaySuccessSrDataSyncService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		Object rawCompany = payload.get("company_id");
		Object rawOrder = payload.get("order_id");
		if (rawCompany == null || rawOrder == null) {
			throw new BadRequestException("company_id and order_id are required");
		}
		long companyId = requireLong(rawCompany, "company_id");
		youshuNormalOrderPaySuccessSrDataSyncService.syncOrderAfterNormalPaySuccess(companyId, rawOrder);
	}

	private static long requireLong(Object raw, String fieldLabel) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(fieldLabel + " must be a number");
		}
	}
}
