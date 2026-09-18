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

package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderPointDeductFormatter;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderMarkdownApplyService;

@Service
public class NormalOrderAdminCheckoutService {

	private static final Pattern CN_ID_CARD =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[xX\\d]$");

	private final NormalOrderAdminCreateUserOrderParamBuilder paramBuilder;
	private final OrderCreateNeedParamsPort orderCreateNeedParamsPort;
	private final OrderCheckoutCartPort orderCheckoutCartPort;
	private final OrderCreateItemCheckPort orderCreateItemCheckPort;
	private final OrderCreateDistributorCheckPort orderCreateDistributorCheckPort;
	private final OrderCreateFormatDataPort orderCreateFormatDataPort;
	private final NormalOrderPointDeductFormatter normalOrderPointDeductFormatter;
	private final NormalOrderMarkdownApplyService normalOrderMarkdownApplyService;
	private final ShopadminNormalOrderTempInfoEnrichmentService shopadminNormalOrderTempInfoEnrichmentService;

	public NormalOrderAdminCheckoutService(
			NormalOrderAdminCreateUserOrderParamBuilder paramBuilder,
			OrderCreateNeedParamsPort orderCreateNeedParamsPort,
			OrderCheckoutCartPort orderCheckoutCartPort,
			OrderCreateItemCheckPort orderCreateItemCheckPort,
			OrderCreateDistributorCheckPort orderCreateDistributorCheckPort,
			OrderCreateFormatDataPort orderCreateFormatDataPort,
			NormalOrderPointDeductFormatter normalOrderPointDeductFormatter,
			NormalOrderMarkdownApplyService normalOrderMarkdownApplyService,
			ShopadminNormalOrderTempInfoEnrichmentService shopadminNormalOrderTempInfoEnrichmentService) {
		this.paramBuilder = paramBuilder;
		this.orderCreateNeedParamsPort = orderCreateNeedParamsPort;
		this.orderCheckoutCartPort = orderCheckoutCartPort;
		this.orderCreateItemCheckPort = orderCreateItemCheckPort;
		this.orderCreateDistributorCheckPort = orderCreateDistributorCheckPort;
		this.orderCreateFormatDataPort = orderCreateFormatDataPort;
		this.normalOrderPointDeductFormatter = normalOrderPointDeductFormatter;
		this.normalOrderMarkdownApplyService = normalOrderMarkdownApplyService;
		this.shopadminNormalOrderTempInfoEnrichmentService = shopadminNormalOrderTempInfoEnrichmentService;
	}

	public Map<String, Object> checkout(
			long companyId, long operatorId, HttpServletRequest request, Map<String, Object> mergedInput) {
		NormalOrderCreateState state = paramBuilder.build(companyId, operatorId, mergedInput, request);
		assertKnownOrderType(state);
		orderCreateNeedParamsPort.validate(state);
		orderCheckoutCartPort.fillItemsFromOperatorCart(state, request);
		orderCreateItemCheckPort.check(state);
		orderCreateDistributorCheckPort.check(state);
		orderCreateFormatDataPort.format(state);
		applyCrossBorderIdentityIfNeeded(state);
		normalOrderPointDeductFormatter.applyIfNeeded(state);
		normalOrderMarkdownApplyService.applyIfMarkdownPresent(state);
		return shopadminNormalOrderTempInfoEnrichmentService.apply(state);
	}

	private static void assertKnownOrderType(NormalOrderCreateState state) {
		String ot = String.valueOf(state.getParams().getOrDefault("order_type", ""));
		if (!"normal_shopadmin".equals(ot)) {
			throw new ResourceException("无此类型订单！");
		}
	}

	private static void applyCrossBorderIdentityIfNeeded(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if (!pr.containsKey("iscrossborder")) {
			return;
		}
		if (intFrom(pr.get("iscrossborder")) != 1) {
			return;
		}
		String identityId = String.valueOf(pr.getOrDefault("identity_id", "")).trim();
		if (!CN_ID_CARD.matcher(identityId).matches()) {
			throw new ResourceException("身份证格式错误");
		}
		Map<String, Object> od = p.getOrderData();
		od.put("identity_id", identityId);
		od.put("identity_name", String.valueOf(pr.getOrDefault("identity_name", "")));
		od.put("type", 1);
	}

	private static int intFrom(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
