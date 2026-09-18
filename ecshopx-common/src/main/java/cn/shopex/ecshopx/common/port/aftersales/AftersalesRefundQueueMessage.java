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

package cn.shopex.ecshopx.common.port.aftersales;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核成功待退款向慢队列投递时的载荷：至少含 {@code refund_bn}、{@code company_id}，与消费端 getInfo 过滤一致时附带
 * {@code order_id}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AftersalesRefundQueueMessage implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 退款单号 */
	private Long refundBn;

	private Long companyId;

	private Long orderId;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AftersalesRefundQueueMessage that = (AftersalesRefundQueueMessage) o;
		return Objects.equals(refundBn, that.refundBn)
				&& Objects.equals(companyId, that.companyId)
				&& Objects.equals(orderId, that.orderId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(refundBn, companyId, orderId);
	}
}
