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

package cn.shopex.ecshopx.orders.service.espier;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrdersUploadSplitValidatorImpl implements NormalOrdersUploadSplitValidator {

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	public NormalOrdersUploadSplitValidatorImpl(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
	}

	@Override
	public List<String> validate(long companyId, long supplierId, List<Map<String, Object>> segment) {
		if (segment == null || segment.isEmpty()) {
			return List.of();
		}
		String orderLevelMessage = resolveOrderLevelMessage(companyId, supplierId, segment);
		if (orderLevelMessage != null) {
			List<String> blocked = new ArrayList<>(segment.size());
			for (int i = 0; i < segment.size(); i++) {
				blocked.add(orderLevelMessage);
			}
			return blocked;
		}

		Map<Long, Integer> itemIdCounts = countOrderItemIds(segment);
		List<String> rowMessages = new ArrayList<>(segment.size());
		boolean hasRowError = false;
		for (Map<String, Object> row : segment) {
			String message = validateRow(companyId, supplierId, row, itemIdCounts);
			if (message != null) {
				hasRowError = true;
			}
			rowMessages.add(message);
		}
		if (!hasRowError) {
			return rowMessages;
		}
		List<String> result = new ArrayList<>(segment.size());
		for (String message : rowMessages) {
			result.add(message != null ? message : "同一包裹存在错误行，本次未发货");
		}
		return result;
	}

	private String resolveOrderLevelMessage(
			long companyId, long supplierId, List<Map<String, Object>> segment) {
		Long orderId = parseOrderId(segment.get(0).get("order_id"));
		if (orderId == null) {
			return "订单不存在";
		}
		NormalOrders master = loadMasterOrder(companyId, orderId);
		if (supplierId > 0L) {
			SupplierOrder supplierOrder = loadSupplierOrder(companyId, orderId, supplierId);
			if (supplierOrder == null) {
				return "订单不存在";
			}
			if (master == null) {
				return "订单不存在";
			}
			return orderLevelBlockMessage(
					supplierOrder.getOrderStatus(),
					supplierOrder.getCancelStatus(),
					supplierOrder.getDeliveryStatus(),
					supplierOrder.getReceiptType());
		}
		if (master == null) {
			return "订单不存在";
		}
		return orderLevelBlockMessage(
				master.getOrderStatus(),
				master.getCancelStatus(),
				master.getDeliveryStatus(),
				master.getReceiptType());
	}

	private String validateRow(
			long companyId,
			long supplierId,
			Map<String, Object> row,
			Map<Long, Integer> itemIdCounts) {
		Long rowOrderId = parseOrderId(row.get("order_id"));
		Long itemId = parsePositiveLong(row.get("order_item_id"));
		if (rowOrderId == null || itemId == null) {
			return "子订单号不属于该订单";
		}
		NormalOrdersItems item = normalOrdersItemsMapper.selectById(itemId);
		if (item == null
				|| !companyIdEquals(item.getCompanyId(), companyId)
				|| !orderIdEquals(item.getOrderId(), rowOrderId)) {
			return "子订单号不属于该订单";
		}
		if (supplierId <= 0L) {
			int lineSupplierId = item.getSupplierId() == null ? 0 : item.getSupplierId();
			if (lineSupplierId > 0) {
				return "供应商商品请由供应商发货";
			}
		} else {
			int lineSupplierId = item.getSupplierId() == null ? 0 : item.getSupplierId();
			if (lineSupplierId != supplierId) {
				return "发货商品不正确";
			}
		}
		String cellBn = trimBn(row.get("item_bn"));
		String itemBn = trimBn(item.getItemBn());
		if (!cellBn.equals(itemBn)) {
			return "商品编码与子订单号不匹配";
		}
		if (itemIdCounts.getOrDefault(itemId, 0) > 1) {
			return "同一包裹中子订单号重复";
		}
		int remain = remainingDeliverable(item);
		if (remain <= 0) {
			return "发货商品已发货";
		}
		int shipQty = parseShipQuantity(row.get("delivery_num"));
		if (shipQty > remain) {
			return "发货数量超过剩余可发数量";
		}
		return null;
	}

	private static String orderLevelBlockMessage(
			String orderStatus, String cancelStatus, String deliveryStatus, String receiptType) {
		if ("NOTPAY".equals(orderStatus)) {
			return "订单未支付，不能发货";
		}
		if ("CANCEL".equalsIgnoreCase(orderStatus)) {
			return "订单已取消，不能发货";
		}
		if ("WAIT_PROCESS".equals(cancelStatus) || "REFUND_PROCESS".equals(cancelStatus)) {
			return "订单有退款待处理，不能发货";
		}
		if ("DONE".equals(deliveryStatus)) {
			return "订单已发货，不能重复发货";
		}
		if (receiptType != null && "merchant".equalsIgnoreCase(receiptType.trim())) {
			return "自配送订单发货必须选择配送状态";
		}
		return null;
	}

	private static int remainingDeliverable(NormalOrdersItems item) {
		int num = item.getNum() == null ? 0 : item.getNum();
		int sent = item.getDeliveryItemNum() == null ? 0 : item.getDeliveryItemNum();
		int cancelled = item.getCancelItemNum() == null ? 0 : item.getCancelItemNum();
		return Math.max(0, num - sent - cancelled);
	}

	private static Map<Long, Integer> countOrderItemIds(List<Map<String, Object>> segment) {
		Map<Long, Integer> counts = new HashMap<>();
		for (Map<String, Object> row : segment) {
			Long itemId = parsePositiveLong(row.get("order_item_id"));
			if (itemId != null) {
				counts.merge(itemId, 1, Integer::sum);
			}
		}
		return counts;
	}

	private NormalOrders loadMasterOrder(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId));
	}

	private SupplierOrder loadSupplierOrder(long companyId, long orderId, long supplierId) {
		return supplierOrderMapper.selectOne(
				new LambdaQueryWrapper<SupplierOrder>()
						.eq(SupplierOrder::getCompanyId, companyId)
						.eq(SupplierOrder::getOrderId, orderId)
						.eq(SupplierOrder::getSupplierId, supplierId));
	}

	private static Long parseOrderId(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = NormalOrdersUploadImportOrderIdSanitizer.sanitize(String.valueOf(raw).trim());
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long parsePositiveLong(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parseShipQuantity(Object raw) {
		if (raw == null) {
			return 0;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String trimBn(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
	}

	private static boolean companyIdEquals(Long entityCompanyId, long companyId) {
		return entityCompanyId != null && entityCompanyId == companyId;
	}

	private static boolean orderIdEquals(Long entityOrderId, long orderId) {
		return entityOrderId != null && entityOrderId == orderId;
	}
}
