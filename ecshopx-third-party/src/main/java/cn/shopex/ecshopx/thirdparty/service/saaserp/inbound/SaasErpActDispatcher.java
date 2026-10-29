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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SaasErpActDispatcher {

	private static final Map<String, String> PRODUCTION_TARGETS;

	static {
		Map<String, String> targets = new LinkedHashMap<>();
		targets.put("get_orders_info", "GetOrdersInfoSaasErpAct@handle");
		targets.put("ome_create_delivery", "CreateDeliverySaasErpAct@handle");
		targets.put("update_store", "UpdateStoreSaasErpAct@handle");
		targets.put("update_order_status", "UpdateOrderReviewStatusSaasErpAct@handle");
		targets.put("get_return_status", "UpdateAftersalesStatusSaasErpAct@handle");
		targets.put("ome_create_reimburse", "UpdateOrderRefundSaasErpAct@handle");
		targets.put("b2c.invoice.send", "InvoiceCallbackSaasErpAct@handle");
		PRODUCTION_TARGETS = Collections.unmodifiableMap(targets);
	}

	private final Map<String, String> targets;
	private final List<SaasErpActHandler> handlers;

	public SaasErpActDispatcher(List<SaasErpActHandler> handlers) {
		this(PRODUCTION_TARGETS, handlers);
	}

	SaasErpActDispatcher(Map<String, String> targets, List<SaasErpActHandler> handlers) {
		this.targets = targets;
		this.handlers = handlers == null ? List.of() : handlers;
	}

	public Optional<SaasErpActHandler> find(String act) {
		return handlers.stream().filter(handler -> handler.act().equals(act)).findFirst();
	}

	public String targetOf(String act) {
		return targets.get(act);
	}

	public boolean isBroken(String act) {
		String target = targetOf(act);
		if (target == null) {
			return false;
		}
		int at = target.indexOf('@');
		if (at < 0) {
			return true;
		}
		String beanSide = target.substring(0, at);
		String methodSide = target.substring(at + 1);
		return beanSide.isEmpty() || methodSide.isEmpty();
	}
}
