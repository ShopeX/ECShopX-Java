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

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.port.order.EmployeePurchaseOrderCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.LimitedTimeSaleQuotaFen;
import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.common.port.order.PartialCancelOrderItemRow;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 整单取消成功（CANCEL + SUCCESS）时返还商品库存与限购额度，对齐 PHP
 * {@code AbstractNormalOrder::update} 在 {@code order_status=CANCEL && cancel_status=SUCCESS} 时的回补。
 * 内购订单额外对齐 PHP {@code EmployeePurchaseBundle\NormalOrderService::cancelOrder} 在
 * {@code refund_status=SUCCESS} 时的 {@code restoreItemStoreAndAggregate}。
 */
@Service
public class NormalOrderFullCancelItemStoreRestoreService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderCancelItemStoreRestorePort orderCancelItemStoreRestorePort;
	private final PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;
	private final PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;
	private final EmployeePurchaseOrderCancelRestorePort employeePurchaseOrderCancelRestorePort;

	public NormalOrderFullCancelItemStoreRestoreService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderCancelItemStoreRestorePort orderCancelItemStoreRestorePort,
			PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort,
			PartialCancelPromotionRestorePort partialCancelPromotionRestorePort,
			EmployeePurchaseOrderCancelRestorePort employeePurchaseOrderCancelRestorePort) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderCancelItemStoreRestorePort = orderCancelItemStoreRestorePort;
		this.pointsmallPartialCancelItemStorePort = pointsmallPartialCancelItemStorePort;
		this.partialCancelPromotionRestorePort = partialCancelPromotionRestorePort;
		this.employeePurchaseOrderCancelRestorePort = employeePurchaseOrderCancelRestorePort;
	}

	/**
	 * @param supplierScope {@code >0} 时仅回补该供应商订单行；{@code 0} 且非平台自营子单时回补整单。
	 */
	public void restoreOnCancelSuccess(long companyId, long orderId, long supplierScope) {
		restoreOnCancelSuccess(companyId, orderId, supplierScope, false);
	}

	/** 部分发货订单上取消剩余未发商品成功后，按剩余数量回补库存。 */
	public void restoreRemainingUnshippedItemsOnCancelSuccess(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			return;
		}
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.apply("IFNULL(delivery_item_num, 0) + IFNULL(cancel_item_num, 0) < IFNULL(num, 0)"));
		if (items.isEmpty()) {
			return;
		}
		String orderClass = order.getOrderClass() == null ? "" : order.getOrderClass().trim();
		String receiptType = order.getReceiptType() != null ? order.getReceiptType() : "";
		boolean seckill = "seckill".equals(orderClass);
		boolean pointsmall = "pointsmall".equals(orderClass);
		List<PartialCancelOrderItemRow> limitedTimeSaleRows = new ArrayList<>();
		for (NormalOrdersItems item : items) {
			int num = item.getNum() == null ? 0 : item.getNum();
			int delivered = item.getDeliveryItemNum() == null ? 0 : item.getDeliveryItemNum();
			int cancelled = item.getCancelItemNum() == null ? 0 : item.getCancelItemNum();
			int pending = Math.max(0, num - delivered - cancelled);
			if (item.getItemId() == null || pending <= 0) {
				continue;
			}
			if (!seckill) {
				if (pointsmall) {
					pointsmallPartialCancelItemStorePort.minusItemStore(companyId, item.getItemId(), -pending, true);
				} else {
					orderCancelItemStoreRestorePort.restoreStore(
							ItemInventoryLineContext.fromOrderLine(
									companyId,
									item.getItemId(),
									item.getSupplierId(),
									item.getDistributorId(),
									item.getIsTotalStore(),
									receiptType),
							pending);
				}
			}
			long rowCompany = item.getCompanyId() == null ? companyId : item.getCompanyId();
			long rowUserId = item.getUserId() == null ? 0L : item.getUserId();
			partialCancelPromotionRestorePort.reduceLimitPerson(rowCompany, rowUserId, item.getItemId(), pending);
			if (item.getItemId() > 0L) {
				int price = item.getPrice() == null ? 0 : item.getPrice();
				int discountFee = item.getDiscountFee() == null ? 0 : item.getDiscountFee();
				int unitFen = LimitedTimeSaleQuotaFen.unitFen(price, pending, discountFee * pending / Math.max(1, num));
				limitedTimeSaleRows.add(new PartialCancelOrderItemRow(item.getItemId(), pending, unitFen));
			}
		}
		partialCancelPromotionRestorePort.restoreLimitedTimeSaleUserBuys(companyId, orderId, limitedTimeSaleRows);
	}

	/** 平台自营子单取消成功时，仅回补 {@code supplier_id=0} 的商品行。 */
	public void restorePlatformSelfItemsOnCancelSuccess(long companyId, long orderId) {
		restoreOnCancelSuccess(companyId, orderId, 0L, true);
	}

	private void restoreOnCancelSuccess(
			long companyId, long orderId, long supplierScope, boolean platformSelfOnly) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			return;
		}
		String orderClass = order.getOrderClass() == null ? "" : order.getOrderClass().trim();
		boolean employeePurchase = "employee_purchase".equals(orderClass);
		boolean shareStore =
				employeePurchase
						&& employeePurchaseOrderCancelRestorePort.isShareStore(companyId, orderId);

		LambdaQueryWrapper<NormalOrdersItems> iw = new LambdaQueryWrapper<>();
		iw.eq(NormalOrdersItems::getOrderId, orderId).eq(NormalOrdersItems::getCompanyId, companyId);
		if (platformSelfOnly) {
			iw.eq(NormalOrdersItems::getSupplierId, 0);
		} else if (supplierScope > 0L) {
			iw.eq(NormalOrdersItems::getSupplierId, (int) Math.min(supplierScope, Integer.MAX_VALUE));
		}
		List<NormalOrdersItems> items = normalOrdersItemsMapper.selectList(iw);
		if (items == null || items.isEmpty()) {
			if (employeePurchase) {
				employeePurchaseOrderCancelRestorePort.restoreOnOrderCancel(orderId, companyId);
			}
			return;
		}
		String receiptType = order.getReceiptType() != null ? order.getReceiptType() : "";
		boolean seckill = "seckill".equals(orderClass);
		boolean pointsmall = "pointsmall".equals(orderClass);
		for (NormalOrdersItems item : items) {
			if (item.getItemId() == null || item.getNum() == null || item.getNum() <= 0) {
				continue;
			}
			int num = item.getNum();
			// 对齐 PHP AbstractNormalOrder::update：
			// employee_purchase 仅当 if_share_store 时还通用库存；非共享扣的是 activity_store。
			if (!seckill) {
				if (pointsmall) {
					pointsmallPartialCancelItemStorePort.minusItemStore(companyId, item.getItemId(), -num, true);
				} else if (!employeePurchase || shareStore) {
					orderCancelItemStoreRestorePort.restoreStore(
							ItemInventoryLineContext.fromOrderLine(
									companyId,
									item.getItemId(),
									item.getSupplierId(),
									item.getDistributorId(),
									item.getIsTotalStore(),
									receiptType),
							num);
				}
			}
			long rowCompany = item.getCompanyId() == null ? companyId : item.getCompanyId();
			long rowUserId = item.getUserId() == null ? 0L : item.getUserId();
			partialCancelPromotionRestorePort.reduceLimitPerson(rowCompany, rowUserId, item.getItemId(), num);
		}
		if (employeePurchase) {
			// 对齐 PHP EmployeePurchase NormalOrderService::cancelOrder(refund_status=SUCCESS)
			employeePurchaseOrderCancelRestorePort.restoreOnOrderCancel(orderId, companyId);
		}
		List<PartialCancelOrderItemRow> limitedTimeSaleRows = new ArrayList<>();
		for (NormalOrdersItems item : items) {
			if (item.getItemId() == null || item.getItemId() <= 0L || item.getNum() == null || item.getNum() <= 0) {
				continue;
			}
			int price = item.getPrice() == null ? 0 : item.getPrice();
			int discountFee = item.getDiscountFee() == null ? 0 : item.getDiscountFee();
			int unitFen = LimitedTimeSaleQuotaFen.unitFen(price, item.getNum(), discountFee);
			limitedTimeSaleRows.add(new PartialCancelOrderItemRow(item.getItemId(), item.getNum(), unitFen));
		}
		partialCancelPromotionRestorePort.restoreLimitedTimeSaleUserBuys(companyId, orderId, limitedTimeSaleRows);
	}
}
