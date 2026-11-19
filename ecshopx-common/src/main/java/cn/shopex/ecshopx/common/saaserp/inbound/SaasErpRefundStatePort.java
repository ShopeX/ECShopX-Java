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

package cn.shopex.ecshopx.common.saaserp.inbound;

import java.util.Map;
import java.util.Optional;

/**
 * State-changing methods must not call {@code store.trade.refund.add} or {@code store.trade.update};
 * those calls stay in {@code UpdateOrderRefundSaasErpAct}.
 */
public interface SaasErpRefundStatePort {

	Optional<Map<String, Object>> findRefund(long companyId, String orderId, String refundId);

	Optional<String> findAftersalesBnByRefundBn(String refundBn);

	Optional<Map<String, Object>> findAftersales(long companyId, String aftersalesBn);

	Optional<Map<String, Object>> findOrder(long companyId, String orderId);

	Object refuseRefundWithoutAftersales(Map<String, Object> refund, Map<String, String> params);

	Object agreeRefundWithoutAftersales(Map<String, Object> refund, Map<String, String> params);

	Object confirmRefundGoods(Map<String, Object> aftersales, Map<String, String> params, boolean agree);

	Object reviewOnlyRefund(Map<String, Object> aftersales, Map<String, String> params, boolean agree);
}
