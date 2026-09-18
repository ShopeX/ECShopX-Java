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

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 多供应商订单中平台自营明细（{@code normal_orders_items.supplier_id=0}）的未发货子单取消，
 * 语义对齐供应商子单取消，但退款单仍使用 {@code supplier_id=0}，需与整单取消按金额区分。
 */
@Service
public class PlatformSelfSubCancelSupport {

	public static final String PLATFORM_SELF_SUB_CANCEL_PARAM = "platform_self_sub_cancel";

	/** 店铺/平台在部分发货订单上取消剩余未发商品，走整单取消建单与状态流转。 */
	public static final String SHOP_REMAINING_FULL_CANCEL_PARAM = "shop_remaining_full_cancel";

	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final AftersalesRefundService aftersalesRefundService;

	public PlatformSelfSubCancelSupport(
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersMapper normalOrdersMapper,
			CancelOrdersMapper cancelOrdersMapper,
			AftersalesRefundService aftersalesRefundService) {
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.aftersalesRefundService = aftersalesRefundService;
	}

	public boolean isScope(long companyId, long orderId, String mainDeliveryStatus) {
		return isShopRemainingFullCancelScope(companyId, orderId, mainDeliveryStatus)
				&& hasUndeliveredPlatformSelfItems(companyId, orderId);
	}

	/** 部分发货主单上是否仍有任意未发完、未退完的商品（含供应商商品）。 */
	public boolean isShopRemainingFullCancelScope(long companyId, long orderId, String mainDeliveryStatus) {
		if (!"PARTAIL".equalsIgnoreCase(safe(mainDeliveryStatus))) {
			return false;
		}
		return hasRemainingUnshippedItems(companyId, orderId);
	}

	public boolean hasRemainingUnshippedItems(long companyId, long orderId) {
		return normalOrdersItemsMapper.selectCount(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.apply("IFNULL(delivery_item_num, 0) + IFNULL(cancel_item_num, 0) < IFNULL(num, 0)"))
				> 0L;
	}

	public boolean hasSupplierOrders(long companyId, long orderId) {
		return supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<cn.shopex.ecshopx.supplier.domain.SupplierOrder>()
								.eq(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getCompanyId, companyId)
								.eq(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getOrderId, orderId))
				> 0L;
	}

	public boolean hasUndeliveredPlatformSelfItems(long companyId, long orderId) {
		return normalOrdersItemsMapper.selectCount(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getSupplierId, 0)
								.apply("IFNULL(cancel_item_num, 0) < IFNULL(num, 0)")
								.apply("IFNULL(delivery_item_num, 0) = 0"))
				> 0L;
	}

	public PlatformSelfCancelAmounts computeCancelAmounts(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getSupplierId, 0)
								.apply("IFNULL(cancel_item_num, 0) < IFNULL(num, 0)")
								.apply("IFNULL(delivery_item_num, 0) = 0"));
		return sumRemainingItemAmounts(items);
	}

	/** 剩余未发商品金额（全供应商 + 平台自营）。 */
	public PlatformSelfCancelAmounts computeRemainingItemsCancelAmounts(long companyId, long orderId) {
		List<NormalOrdersItems> items = listRemainingUnshippedItems(companyId, orderId);
		return sumRemainingItemAmounts(items);
	}

	public boolean isShopRemainingFullCancelRefund(long companyId, long orderId, AftersalesRefund refund) {
		return isPartialDeliveryScopedRefund(companyId, orderId, refund);
	}

	/** {@code supplier_id=0} 且非整单全额取消的售前退款（部分发货剩余取消）。 */
	public boolean isPartialDeliveryScopedRefund(long companyId, long orderId, AftersalesRefund refund) {
		if (refund == null || refund.getSupplierId() == null || refund.getSupplierId() != 0L) {
			return false;
		}
		return !isFullOrderCancelRefund(companyId, orderId, refund);
	}

	public void markRemainingUnshippedItemsCancelled(long companyId, long orderId) {
		for (NormalOrdersItems item : listRemainingUnshippedItems(companyId, orderId)) {
			if (item.getId() == null || item.getNum() == null) {
				continue;
			}
			int delivered = nz(item.getDeliveryItemNum());
			int cancelled = nz(item.getCancelItemNum());
			int pending = Math.max(0, item.getNum() - delivered - cancelled);
			if (pending <= 0) {
				continue;
			}
			LambdaUpdateWrapper<NormalOrdersItems> uw = new LambdaUpdateWrapper<>();
			uw.eq(NormalOrdersItems::getId, item.getId()).set(NormalOrdersItems::getCancelItemNum, cancelled + pending);
			normalOrdersItemsMapper.update(null, uw);
		}
	}

	public void finalizeSupplierOrdersAfterRemainingCancelPass(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		List<cn.shopex.ecshopx.supplier.domain.SupplierOrder> supplierOrders =
				supplierOrderMapper.selectList(
						new LambdaQueryWrapper<cn.shopex.ecshopx.supplier.domain.SupplierOrder>()
								.eq(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getCompanyId, companyId)
								.eq(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getOrderId, orderId));
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (cn.shopex.ecshopx.supplier.domain.SupplierOrder supplierOrder : supplierOrders) {
			if (supplierOrder.getSupplierId() == null) {
				continue;
			}
			int supplierId = supplierOrder.getSupplierId();
			boolean anyDelivered = false;
			for (NormalOrdersItems item : items) {
				if (!Integer.valueOf(supplierId).equals(item.getSupplierId())) {
					continue;
				}
				if (nz(item.getDeliveryItemNum()) > 0) {
					anyDelivered = true;
					break;
				}
			}
			LambdaUpdateWrapper<cn.shopex.ecshopx.supplier.domain.SupplierOrder> uw =
					new LambdaUpdateWrapper<>();
			uw.eq(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getCompanyId, companyId)
					.eq(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getOrderId, orderId)
					.eq(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getSupplierId, supplierId)
					.set(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getUpdateTime, now);
			if (anyDelivered) {
				uw.set(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getCancelStatus, "NO_APPLY_CANCEL");
				if (!"WAIT_BUYER_CONFIRM".equalsIgnoreCase(safe(supplierOrder.getOrderStatus()))) {
					uw.set(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getOrderStatus, "WAIT_BUYER_CONFIRM");
				}
			} else {
				uw.set(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getOrderStatus, "CANCEL")
						.set(cn.shopex.ecshopx.supplier.domain.SupplierOrder::getCancelStatus, "SUCCESS");
			}
			supplierOrderMapper.update(null, uw);
		}
	}

	private List<NormalOrdersItems> listRemainingUnshippedItems(long companyId, long orderId) {
		return normalOrdersItemsMapper.selectList(
				new LambdaQueryWrapper<NormalOrdersItems>()
						.eq(NormalOrdersItems::getCompanyId, companyId)
						.eq(NormalOrdersItems::getOrderId, orderId)
						.apply("IFNULL(delivery_item_num, 0) + IFNULL(cancel_item_num, 0) < IFNULL(num, 0)"));
	}

	private PlatformSelfCancelAmounts sumRemainingItemAmounts(List<NormalOrdersItems> items) {
		int totalFee = 0;
		int refundPoint = 0;
		for (NormalOrdersItems item : items) {
			int num = nz(item.getNum());
			if (num <= 0) {
				continue;
			}
			int delivered = nz(item.getDeliveryItemNum());
			int cancelled = nz(item.getCancelItemNum());
			int pending = Math.max(0, num - delivered - cancelled);
			if (pending <= 0) {
				continue;
			}
			int lineFee = item.getTotalFee() == null ? 0 : item.getTotalFee();
			int linePoint = item.getPoint() == null ? 0 : item.getPoint();
			totalFee += (int) Math.round((double) lineFee * pending / num);
			refundPoint += (int) Math.round((double) linePoint * pending / num);
		}
		int freightFee = 0;
		int refundFee = Math.max(0, totalFee - freightFee);
		return new PlatformSelfCancelAmounts(totalFee, freightFee, refundFee, refundPoint);
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}

	public long countReadyPlatformSelfSubCancelRefunds(long companyId, long orderId) {
		List<AftersalesRefund> ready =
				aftersalesRefundService.listReadyCancelRefundsForSupplier(companyId, orderId, 0L);
		return ready.stream().filter(r -> isPartialDeliveryScopedRefund(companyId, orderId, r)).count();
	}

	public boolean hasApprovedFullOrderCancelRefund(long companyId, long orderId) {
		List<AftersalesRefund> approved =
				aftersalesRefundService.listApprovedCancelRefundsForSupplier(companyId, orderId, 0L);
		for (AftersalesRefund refund : approved) {
			if (isFullOrderCancelRefund(companyId, orderId, refund)) {
				return true;
			}
		}
		return false;
	}

	public boolean isPlatformSelfSubCancelRefund(long companyId, long orderId, AftersalesRefund refund) {
		if (!isPartialDeliveryScopedRefund(companyId, orderId, refund)) {
			return false;
		}
		return hasUndeliveredPlatformSelfItems(companyId, orderId)
				|| hasRemainingUnshippedPlatformSelfItemsMarked(companyId, orderId);
	}

	private boolean hasRemainingUnshippedPlatformSelfItemsMarked(long companyId, long orderId) {
		return normalOrdersItemsMapper.selectCount(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getSupplierId, 0)
								.apply("IFNULL(cancel_item_num, 0) > 0")
								.apply("IFNULL(delivery_item_num, 0) = 0"))
				> 0L;
	}

	public boolean isFullOrderCancelRefund(long companyId, long orderId, AftersalesRefund refund) {
		if (refund == null || refund.getSupplierId() == null || refund.getSupplierId() != 0L) {
			return false;
		}
		NormalOrders main = loadMain(companyId, orderId);
		if (main == null) {
			return false;
		}
		CancelOrders cancelRow =
				cancelOrdersMapper.selectOne(
						new LambdaQueryWrapper<CancelOrders>()
								.eq(CancelOrders::getCompanyId, companyId)
								.eq(CancelOrders::getOrderId, orderId)
								.eq(CancelOrders::getSupplierId, 0L)
								.orderByDesc(CancelOrders::getCreateTime)
								.last("LIMIT 1"));
		int mainTotal = parseMoneyInt(main.getTotalFee());
		// 纯积分/零元单：按积分判定整单取消，避免 reconcile 误回滚为 PAYED
		if (mainTotal <= 0) {
			int mainPoint = main.getPoint() == null ? 0 : main.getPoint();
			if (mainPoint <= 0) {
				return false;
			}
			if (cancelRow != null && cancelRow.getPoint() != null) {
				return cancelRow.getPoint() >= mainPoint;
			}
			return refund.getRefundPoint() != null && refund.getRefundPoint() >= mainPoint;
		}
		if (cancelRow != null && cancelRow.getTotalFee() != null) {
			return cancelRow.getTotalFee() >= mainTotal;
		}
		int mainFreight = main.getFreightFee() == null ? 0 : main.getFreightFee();
		int expectedRefundFee = Math.max(0, mainTotal - mainFreight);
		return refund.getRefundFee() != null && refund.getRefundFee() >= expectedRefundFee;
	}

	public void markPlatformSelfItemsCancelled(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getSupplierId, 0)
								.apply("IFNULL(cancel_item_num, 0) < IFNULL(num, 0)")
								.apply("IFNULL(delivery_item_num, 0) = 0"));
		for (NormalOrdersItems item : items) {
			if (item.getId() == null || item.getNum() == null) {
				continue;
			}
			int deliveryNum = item.getDeliveryItemNum() == null ? 0 : item.getDeliveryItemNum();
			int cancelNum = Math.max(0, item.getNum() - deliveryNum);
			LambdaUpdateWrapper<NormalOrdersItems> uw = new LambdaUpdateWrapper<>();
			uw.eq(NormalOrdersItems::getId, item.getId()).set(NormalOrdersItems::getCancelItemNum, cancelNum);
			normalOrdersItemsMapper.update(null, uw);
		}
	}

	/**
	 * 供应商子单取消成功：将该供应商未发完明细的 {@code cancel_item_num} 补齐，
	 * 以便后续发货收尾把「已发 + 已取消」推进为主单待收货。
	 */
	public void markSupplierItemsCancelled(long companyId, long orderId, long supplierId) {
		if (companyId <= 0L || orderId <= 0L || supplierId <= 0L) {
			return;
		}
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getSupplierId, (int) supplierId)
								.apply("IFNULL(delivery_item_num, 0) + IFNULL(cancel_item_num, 0) < IFNULL(num, 0)"));
		for (NormalOrdersItems item : items) {
			if (item.getId() == null || item.getNum() == null) {
				continue;
			}
			int delivered = nz(item.getDeliveryItemNum());
			int cancelled = nz(item.getCancelItemNum());
			int pending = Math.max(0, item.getNum() - delivered - cancelled);
			if (pending <= 0) {
				continue;
			}
			LambdaUpdateWrapper<NormalOrdersItems> uw = new LambdaUpdateWrapper<>();
			uw.eq(NormalOrdersItems::getId, item.getId())
					.set(NormalOrdersItems::getCancelItemNum, cancelled + pending);
			normalOrdersItemsMapper.update(null, uw);
		}
	}

	private NormalOrders loadMain(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.last("LIMIT 1"));
	}

	private static int parseMoneyInt(Object totalFee) {
		if (totalFee == null) {
			return 0;
		}
		if (totalFee instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(totalFee).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	public record PlatformSelfCancelAmounts(int totalFee, int freightFee, int refundFee, int refundPoint) {}
}
