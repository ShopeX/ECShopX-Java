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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagCodes;
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagPayloadIds;
import cn.shopex.ecshopx.members.service.systemtag.NewCustomerEffectivePaidOrderQuery;
import cn.shopex.ecshopx.members.service.systemtag.NewCustomerTagReconcileService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NormalOrderCancelNewCustomerDispatchListener implements DispatchListener {

	private final NewCustomerTagReconcileService newCustomerTagReconcileService;
	private final NewCustomerEffectivePaidOrderQuery effectivePaidOrderQuery;

	public NormalOrderCancelNewCustomerDispatchListener(
			NewCustomerTagReconcileService newCustomerTagReconcileService,
			NewCustomerEffectivePaidOrderQuery effectivePaidOrderQuery) {
		this.newCustomerTagReconcileService = newCustomerTagReconcileService;
		this.effectivePaidOrderQuery = effectivePaidOrderQuery;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		long companyId = MemberSystemTagPayloadIds.companyId(payload);
		long userId = MemberSystemTagPayloadIds.userId(payload);
		if (userId <= 0L) {
			long orderId = MemberSystemTagPayloadIds.orderId(payload);
			if (companyId > 0L && orderId > 0L) {
				Long fromOrder = effectivePaidOrderQuery.findUserIdByOrder(companyId, orderId);
				userId = fromOrder == null ? 0L : fromOrder;
			}
		}
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		newCustomerTagReconcileService.reconcile(companyId, userId, MemberSystemTagCodes.SOURCE_CANCEL);
	}
}
