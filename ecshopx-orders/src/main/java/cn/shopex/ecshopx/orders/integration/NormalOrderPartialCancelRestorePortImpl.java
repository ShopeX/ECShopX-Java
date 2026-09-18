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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
import cn.shopex.ecshopx.common.port.order.LimitedTimeSaleQuotaFen;
import cn.shopex.ecshopx.common.port.order.NormalOrderPartialCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.PartialCancelOrderItemRow;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderPartialCancelRestorePortImpl implements NormalOrderPartialCancelRestorePort {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final ExcardInventoryPort excardInventoryPort;
	private final PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;
	private final PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;

	public NormalOrderPartialCancelRestorePortImpl(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			ExcardInventoryPort excardInventoryPort,
			PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort,
			PartialCancelPromotionRestorePort partialCancelPromotionRestorePort) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.excardInventoryPort = excardInventoryPort;
		this.pointsmallPartialCancelItemStorePort = pointsmallPartialCancelItemStorePort;
		this.partialCancelPromotionRestorePort = partialCancelPromotionRestorePort;
	}

	@Override
	public void partialCancelRestore(long companyId, long orderId, boolean onApprove) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId));
		if (order == null) {
			throw new ResourceException("订单数据异常");
		}
		if (!onApprove) {
			normalOrdersMapper.update(
					null,
					new LambdaUpdateWrapper<NormalOrders>()
							.eq(NormalOrders::getCompanyId, companyId)
							.eq(NormalOrders::getOrderId, orderId)
							.set(NormalOrders::getOrderStatus, "PAYED"));
			normalOrdersItemsMapper.update(
					null,
					new LambdaUpdateWrapper<NormalOrdersItems>()
							.eq(NormalOrdersItems::getOrderId, orderId)
							.set(NormalOrdersItems::getCancelItemNum, 0));
			return;
		}
		List<NormalOrdersItems> orderItems =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderId)
								.gt(NormalOrdersItems::getCancelItemNum, 0));
		if (orderItems == null || orderItems.isEmpty()) {
			return;
		}
		String oc = orderClass(order);
		long orderUserId = order.getUserId() == null ? 0L : order.getUserId();
		for (NormalOrdersItems row : orderItems) {
			int cancelNum = row.getCancelItemNum() == null ? 0 : row.getCancelItemNum();
			if (cancelNum <= 0) {
				continue;
			}
			long itemId = row.getItemId() == null ? 0L : row.getItemId();
			long distributorId = row.getDistributorId() == null ? 0L : row.getDistributorId();
			boolean isTotal = Boolean.TRUE.equals(row.getIsTotalStore());
			String receiptType = order.getReceiptType() != null ? order.getReceiptType() : "";
			if (!inIgnoreCase(oc, "seckill", "pointsmall")) {
				excardInventoryPort.minusItemStore(
						ItemInventoryLineContext.fromOrderLine(
								companyId, itemId, row.getSupplierId(), distributorId, isTotal, receiptType),
						-cancelNum);
			}
			if ("pointsmall".equalsIgnoreCase(oc)) {
				boolean ok =
						pointsmallPartialCancelItemStorePort.minusItemStore(
								companyId, itemId, -cancelNum, true);
				if (!ok) {
					throw new ResourceException("积分商城订单部分取消恢复失败：库存数据异常");
				}
			}
			if ("groups".equalsIgnoreCase(oc)) {
				partialCancelPromotionRestorePort.restoreGroupItemStore(
						companyId, orderId, orderUserId, itemId, cancelNum);
			}
			long rowUserId = row.getUserId() == null ? 0L : row.getUserId();
			long rowCompany = row.getCompanyId() == null ? companyId : row.getCompanyId();
			partialCancelPromotionRestorePort.reduceLimitPerson(rowCompany, rowUserId, itemId, cancelNum);
		}
		List<PartialCancelOrderItemRow> ltd = new ArrayList<>();
		for (NormalOrdersItems row : orderItems) {
			int cn = row.getCancelItemNum() == null ? 0 : row.getCancelItemNum();
			int price = row.getPrice() == null ? 0 : row.getPrice();
			int origNum = row.getNum() == null ? 0 : row.getNum();
			int discountFee = row.getDiscountFee() == null ? 0 : row.getDiscountFee();
			int unitFen = LimitedTimeSaleQuotaFen.unitFen(price, origNum, discountFee);
			long iid = row.getItemId() == null ? 0L : row.getItemId();
			if (cn > 0 && iid > 0L) {
				ltd.add(new PartialCancelOrderItemRow(iid, cn, unitFen));
			}
		}
		partialCancelPromotionRestorePort.restoreLimitedTimeSaleUserBuys(companyId, orderId, ltd);
	}

	private static String orderClass(NormalOrders order) {
		String c = order.getOrderClass();
		if (c == null || c.isBlank()) {
			return "normal";
		}
		return c;
	}

	private static boolean inIgnoreCase(String v, String a, String b) {
		return a.equalsIgnoreCase(v) || b.equalsIgnoreCase(v);
	}
}
