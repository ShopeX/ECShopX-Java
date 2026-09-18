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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Writes {@code order_status} / {@code cancel_status} across {@code supplier_order},
 * {@code orders_associations} and {@code orders_normal_orders}, matching PHP
 * {@code AbstractNormalOrder::update} supplier branching.
 */
@Service
public class NormalOrderStatusUpdateService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;

	public NormalOrderStatusUpdateService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			SupplierOrderMapper supplierOrderMapper,
			OrderAssociationsMapper orderAssociationsMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
	}

	/**
	 * @param supplierScope {@code >0} updates that supplier row first; {@code 0} updates all supplier
	 *     rows for the order (when any exist)
	 * @param orderStatus optional; blank skips
	 * @param cancelStatus optional; blank skips
	 * @return {@code false} when supplier-scoped {@code order_status=CANCEL} and sibling supplier
	 *     orders are not all {@code CANCEL} yet (buyer main order left unchanged)
	 */
	public boolean applyStatusUpdate(
			long companyId, long orderId, long supplierScope, String orderStatus, String cancelStatus) {
		String os = blankToNull(orderStatus);
		String cs = blankToNull(cancelStatus);
		if (os == null && cs == null) {
			return true;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);

		if (supplierScope > 0L) {
			SupplierOrder supplierOrder =
					supplierOrderMapper.selectOne(
							new LambdaQueryWrapper<SupplierOrder>()
									.eq(SupplierOrder::getCompanyId, companyId)
									.eq(SupplierOrder::getOrderId, orderId)
									.eq(SupplierOrder::getSupplierId, supplierScope)
									.last("LIMIT 1"));
			if (supplierOrder == null) {
				throw new ResourceException("订单不存在");
			}
			LambdaUpdateWrapper<SupplierOrder> su = new LambdaUpdateWrapper<>();
			su.eq(SupplierOrder::getCompanyId, companyId)
					.eq(SupplierOrder::getOrderId, orderId)
					.eq(SupplierOrder::getSupplierId, supplierScope);
			applySupplierFields(su, os, cs, now);
			supplierOrderMapper.update(null, su);

			if ("CANCEL".equalsIgnoreCase(os)) {
				List<SupplierOrder> siblings =
						supplierOrderMapper.selectList(
								new LambdaQueryWrapper<SupplierOrder>()
										.eq(SupplierOrder::getCompanyId, companyId)
										.eq(SupplierOrder::getOrderId, orderId));
				for (SupplierOrder sibling : siblings) {
					if (!"CANCEL".equalsIgnoreCase(safe(sibling.getOrderStatus()))) {
						return false;
					}
				}
				if (hasRemainingActiveShipmentScope(companyId, orderId)) {
					return false;
				}
			}
		} else {
			NormalOrders normal =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderId)
									.last("LIMIT 1"));
			if (normal == null) {
				throw new ResourceException("订单不存在");
			}
			LambdaQueryWrapper<SupplierOrder> countQ =
					new LambdaQueryWrapper<SupplierOrder>()
							.eq(SupplierOrder::getCompanyId, companyId)
							.eq(SupplierOrder::getOrderId, orderId);
			if (supplierOrderMapper.selectCount(countQ) > 0) {
				LambdaUpdateWrapper<SupplierOrder> su = new LambdaUpdateWrapper<>();
				su.eq(SupplierOrder::getCompanyId, companyId).eq(SupplierOrder::getOrderId, orderId);
				applySupplierFields(su, os, cs, now);
				supplierOrderMapper.update(null, su);
			}
		}

		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getCompanyId, companyId).eq(OrderAssociations::getOrderId, orderId);
		applyAssociationFields(au, os, cs, now);
		orderAssociationsMapper.update(null, au);

		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getCompanyId, companyId).eq(NormalOrders::getOrderId, orderId);
		applyNormalFields(ou, os, cs, now);
		int n = normalOrdersMapper.update(null, ou);
		if (n <= 0) {
			throw new ResourceException("订单不存在");
		}
		return true;
	}

	/**
	 * 是否仍有待发货范围：活跃 supplier 子单，或未取消完的平台自营明细（supplier_id=0）。
	 */
	public boolean hasRemainingActiveShipmentScope(long companyId, long orderId) {
		long activeSupplierOrders =
				supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, orderId)
								.ne(SupplierOrder::getOrderStatus, "CANCEL"));
		if (activeSupplierOrders > 0L) {
			return true;
		}
		long uncancelledSelfItems =
				normalOrdersItemsMapper.selectCount(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getSupplierId, 0)
								.apply("(IFNULL(cancel_item_num, 0) < IFNULL(num, 0))"));
		return uncancelledSelfItems > 0L;
	}

	/** 平台自营子单取消申请：仅主单与 associations 进入待审，不改动 supplier_order。 */
	public void applyMainCancelWaitProcessOnly(long companyId, long orderId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderId, orderId)
				.set(OrderAssociations::getCancelStatus, "WAIT_PROCESS")
				.set(OrderAssociations::getUpdateTime, now);
		orderAssociationsMapper.update(null, au);

		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getCancelStatus, "WAIT_PROCESS")
				.set(NormalOrders::getUpdateTime, now);
		int n = normalOrdersMapper.update(null, ou);
		if (n <= 0) {
			throw new ResourceException("订单不存在");
		}
	}

	/** 仅回写主单与 associations，不改动 supplier_order（部分子单取消后恢复买家侧待发货）。 */
	public void revertMainOrderToPayedNoApplyCancel(long companyId, long orderId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderId, orderId)
				.set(OrderAssociations::getOrderStatus, "PAYED")
				.set(OrderAssociations::getCancelStatus, "NO_APPLY_CANCEL")
				.set(OrderAssociations::getUpdateTime, now);
		orderAssociationsMapper.update(null, au);

		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getOrderStatus, "PAYED")
				.set(NormalOrders::getCancelStatus, "NO_APPLY_CANCEL")
				.set(NormalOrders::getUpdateTime, now);
		int n = normalOrdersMapper.update(null, ou);
		if (n <= 0) {
			throw new ResourceException("订单不存在");
		}
	}

	private static void applySupplierFields(
			LambdaUpdateWrapper<SupplierOrder> su, String os, String cs, int now) {
		if (os != null) {
			su.set(SupplierOrder::getOrderStatus, os);
		}
		if (cs != null) {
			su.set(SupplierOrder::getCancelStatus, cs);
		}
		su.set(SupplierOrder::getUpdateTime, now);
	}

	private static void applyAssociationFields(
			LambdaUpdateWrapper<OrderAssociations> au, String os, String cs, int now) {
		if (os != null) {
			au.set(OrderAssociations::getOrderStatus, os);
		}
		if (cs != null) {
			au.set(OrderAssociations::getCancelStatus, cs);
		}
		au.set(OrderAssociations::getUpdateTime, now);
	}

	private static void applyNormalFields(
			LambdaUpdateWrapper<NormalOrders> ou, String os, String cs, int now) {
		if (os != null) {
			ou.set(NormalOrders::getOrderStatus, os);
		}
		if (cs != null) {
			ou.set(NormalOrders::getCancelStatus, cs);
		}
		ou.set(NormalOrders::getUpdateTime, now);
	}

	private static String blankToNull(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		return raw.trim();
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
