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

package cn.shopex.ecshopx.thirdparty.service.dada;

import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReAddOrderPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("dadaLocalDeliveryReAddOrderNoOp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.dada",
		name = "http-enabled",
		havingValue = "false",
		matchIfMissing = true)
public class DadaLocalDeliveryReAddOrderNoOp implements DadaLocalDeliveryReAddOrderPort {

	@Override
	public void reAddOrder(long companyId, long orderId) {
		// 未启用达达 HTTP 时不调用远端，由业务侧仅更新本地 orders_rel_dada 等字段。
	}
}
