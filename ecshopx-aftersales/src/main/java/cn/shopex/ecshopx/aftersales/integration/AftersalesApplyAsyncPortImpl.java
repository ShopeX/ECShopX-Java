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

package cn.shopex.ecshopx.aftersales.integration;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyPostCommitCommand;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.common.dispatch.OrderRefundCompleteJobDispatchPublisher;
import org.springframework.stereotype.Component;

@Component("aftersalesApplyAsyncPortImpl")
public class AftersalesApplyAsyncPortImpl implements AftersalesApplyAsyncPort {

	private final OrderRefundCompleteJobDispatchPublisher orderRefundCompleteJobDispatchPublisher;

	public AftersalesApplyAsyncPortImpl(OrderRefundCompleteJobDispatchPublisher orderRefundCompleteJobDispatchPublisher) {
		this.orderRefundCompleteJobDispatchPublisher = orderRefundCompleteJobDispatchPublisher;
	}

	@Override
	public void dispatchPostCommitSideEffects(AftersalesApplyPostCommitCommand cmd) {
		if ("ONLY_REFUND".equals(cmd.aftersalesType()) || cmd.goodsReturned()) {
			orderRefundCompleteJobDispatchPublisher.publish(cmd.companyId(), cmd.orderId());
		}
	}
}
