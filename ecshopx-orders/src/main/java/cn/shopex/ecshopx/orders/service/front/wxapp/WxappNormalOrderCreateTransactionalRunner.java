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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseExtendPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsExtendPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsInventoryPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateInventoryPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreatePersistencePort;
import cn.shopex.ecshopx.common.order.normal.OrderCreatePostCommitPort;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreatePrePersistEnrichmentService;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderMarkdownApplyService;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderPointDeductFormatter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxappNormalOrderCreateTransactionalRunner {

	private static final Pattern CN_ID_CARD =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[xX\\d]$");

	private final NormalOrderPointDeductFormatter normalOrderPointDeductFormatter;
	private final NormalOrderMarkdownApplyService normalOrderMarkdownApplyService;
	private final NormalOrderCreatePrePersistEnrichmentService normalOrderCreatePrePersistEnrichmentService;
	private final OrderCreatePersistencePort orderCreatePersistencePort;
	private final OrderCreateGroupsExtendPort orderCreateGroupsExtendPort;
	private final OrderCreateEmployeePurchaseExtendPort orderCreateEmployeePurchaseExtendPort;
	private final OrderCreateInventoryPort orderCreateInventoryPort;
	private final OrderCreateGroupsInventoryPort orderCreateGroupsInventoryPort;
	private final OrderCreatePostCommitPort orderCreatePostCommitPort;

	public WxappNormalOrderCreateTransactionalRunner(
			NormalOrderPointDeductFormatter normalOrderPointDeductFormatter,
			NormalOrderMarkdownApplyService normalOrderMarkdownApplyService,
			NormalOrderCreatePrePersistEnrichmentService normalOrderCreatePrePersistEnrichmentService,
			OrderCreatePersistencePort orderCreatePersistencePort,
			OrderCreateGroupsExtendPort orderCreateGroupsExtendPort,
			OrderCreateEmployeePurchaseExtendPort orderCreateEmployeePurchaseExtendPort,
			OrderCreateInventoryPort orderCreateInventoryPort,
			OrderCreateGroupsInventoryPort orderCreateGroupsInventoryPort,
			OrderCreatePostCommitPort orderCreatePostCommitPort) {
		this.normalOrderPointDeductFormatter = normalOrderPointDeductFormatter;
		this.normalOrderMarkdownApplyService = normalOrderMarkdownApplyService;
		this.normalOrderCreatePrePersistEnrichmentService = normalOrderCreatePrePersistEnrichmentService;
		this.orderCreatePersistencePort = orderCreatePersistencePort;
		this.orderCreateGroupsExtendPort = orderCreateGroupsExtendPort;
		this.orderCreateEmployeePurchaseExtendPort = orderCreateEmployeePurchaseExtendPort;
		this.orderCreateInventoryPort = orderCreateInventoryPort;
		this.orderCreateGroupsInventoryPort = orderCreateGroupsInventoryPort;
		this.orderCreatePostCommitPort = orderCreatePostCommitPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void runInTransaction(NormalOrderCreateParams p, HttpServletRequest request) {
		applyCrossBorderIdentityIfNeeded(p);
		normalOrderPointDeductFormatter.applyIfNeeded(p);
		normalOrderMarkdownApplyService.applyIfMarkdownPresent(p);
		normalOrderCreatePrePersistEnrichmentService.applyBeforePersist(p);
		orderCreatePersistencePort.persistOrder(p);
		orderCreateGroupsExtendPort.applyAfterOrderInsert(p);
		orderCreateEmployeePurchaseExtendPort.applyAfterOrderInsert(p);
		orderCreateInventoryPort.minusStores(p);
		orderCreateGroupsInventoryPort.minusGroupItemStoreIfNeeded(p);
		orderCreatePostCommitPort.runAfterOrderInsert(p);
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
			throw new BadRequestException("身份证格式错误");
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
}
