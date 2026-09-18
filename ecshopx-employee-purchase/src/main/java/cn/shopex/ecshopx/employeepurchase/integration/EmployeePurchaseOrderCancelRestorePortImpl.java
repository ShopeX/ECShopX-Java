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

package cn.shopex.ecshopx.employeepurchase.integration;

import cn.shopex.ecshopx.common.port.order.EmployeePurchaseOrderCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseOrderRestoreItemStoreService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchasePrepaidRestoreService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EmployeePurchaseOrderCancelRestorePortImpl implements EmployeePurchaseOrderCancelRestorePort {

	/**
	 * pass_refund 会先走 {@link #restoreOnOrderCancel} 再走 {@link #restoreAfterRefundPassApproved}；
	 * 同事务内只还原一次，避免额度/SKU 限购/活动库存双还。
	 */
	private static final Object RESTORE_ONCE_TX_RESOURCE = new Object();

	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final OrdersRelActivityMapper ordersRelActivityMapper;
	private final EmployeePurchaseOrderRestoreItemStoreService employeePurchaseOrderRestoreItemStoreService;
	private final EmployeePurchasePrepaidRestoreService employeePurchasePrepaidRestoreService;

	public EmployeePurchaseOrderCancelRestorePortImpl(
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			OrdersRelActivityMapper ordersRelActivityMapper,
			EmployeePurchaseOrderRestoreItemStoreService employeePurchaseOrderRestoreItemStoreService,
			EmployeePurchasePrepaidRestoreService employeePurchasePrepaidRestoreService) {
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.ordersRelActivityMapper = ordersRelActivityMapper;
		this.employeePurchaseOrderRestoreItemStoreService = employeePurchaseOrderRestoreItemStoreService;
		this.employeePurchasePrepaidRestoreService = employeePurchasePrepaidRestoreService;
	}

	@Override
	public void restoreOnOrderCancel(long orderId, long companyId) {
		restoreInternal(companyId, orderId, null);
	}

	@Override
	public boolean isShareStore(long companyId, long orderId) {
		OrdersRelActivity rel = ordersRelActivityMapper.selectById(orderId);
		if (rel == null || !companyIdMatches(rel, companyId)) {
			return false;
		}
		return Boolean.TRUE.equals(rel.getIfShareStore());
	}

	@Override
	public void restoreAfterRefundPassApproved(long companyId, long orderId) {
		Optional<Map<String, Object>> headOpt = orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		if (headOpt.isEmpty()) {
			return;
		}
		String orderClass = str(headOpt.get().get("order_class"));
		if (!"employee_purchase".equalsIgnoreCase(orderClass)
				&& !"normal_employee_purchase".equalsIgnoreCase(orderClass)) {
			return;
		}
		restoreInternal(companyId, orderId, headOpt.get());
	}

	private void restoreInternal(long companyId, long orderId, Map<String, Object> header) {
		if (alreadyRestoredInCurrentTx(orderId)) {
			return;
		}
		OrdersRelActivity rel = ordersRelActivityMapper.selectById(orderId);
		if (rel == null || !companyIdMatches(rel, companyId)) {
			return;
		}
		List<Map<String, Object>> items = orderNormalOrderItemsReadPort.listItems(companyId, orderId);
		if (items == null || items.isEmpty()) {
			return;
		}

		if (PurchaseModeSupport.isPrepaidPoint(rel.getPurchaseMode())) {
			int payable = rel.getPrepaidPayableFee() == null ? 0 : rel.getPrepaidPayableFee();
			int restored = rel.getRestoredPrepaidFee() == null ? 0 : rel.getRestoredPrepaidFee();
			// 跨请求幂等：点数已还满则整单取消还原已完成（含 SKU 限购）
			if (payable > 0 && restored >= payable) {
				markRestoredInCurrentTx(orderId);
				return;
			}
			// 预充点：还剩余未还点数；库存/SKU 限购仍按整单行还（与现网取消一致）
			employeePurchasePrepaidRestoreService.restoreRemainingOrPartial(rel, 0);
			employeePurchaseOrderRestoreItemStoreService.restoreItemStoreAndAggregate(
					companyId, rel, 0, items);
			markRestoredInCurrentTx(orderId);
			return;
		}

		int itemFee = resolveItemFee(header, items);
		employeePurchaseOrderRestoreItemStoreService.restoreItemStoreAndAggregate(
				companyId, rel, itemFee, items);
		markRestoredInCurrentTx(orderId);
	}

	@SuppressWarnings("unchecked")
	private static boolean alreadyRestoredInCurrentTx(long orderId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return false;
		}
		Set<Long> done = (Set<Long>) TransactionSynchronizationManager.getResource(RESTORE_ONCE_TX_RESOURCE);
		return done != null && done.contains(orderId);
	}

	@SuppressWarnings("unchecked")
	private static void markRestoredInCurrentTx(long orderId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		Set<Long> done = (Set<Long>) TransactionSynchronizationManager.getResource(RESTORE_ONCE_TX_RESOURCE);
		if (done == null) {
			done = new HashSet<>();
			TransactionSynchronizationManager.bindResource(RESTORE_ONCE_TX_RESOURCE, done);
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							TransactionSynchronizationManager.unbindResourceIfPossible(RESTORE_ONCE_TX_RESOURCE);
						}
					});
		}
		done.add(orderId);
	}

	private static int resolveItemFee(Map<String, Object> header, List<Map<String, Object>> items) {
		if (header != null) {
			int fromHeader = intVal(header.get("item_fee"));
			if (fromHeader > 0) {
				return fromHeader;
			}
		}
		int sum = 0;
		for (Map<String, Object> line : items) {
			int lineFee = intVal(line.get("item_fee"));
			if (lineFee <= 0) {
				lineFee = intVal(line.get("total_fee"));
			}
			sum += lineFee;
		}
		return sum;
	}

	private static boolean companyIdMatches(OrdersRelActivity rel, long companyId) {
		return rel.getCompanyId() != null && rel.getCompanyId().longValue() == companyId;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
