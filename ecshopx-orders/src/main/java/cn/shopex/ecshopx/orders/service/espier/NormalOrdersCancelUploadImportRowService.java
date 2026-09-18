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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderPartialCancelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NormalOrdersCancelUploadImportRowService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	public NormalOrdersCancelUploadImportRowService(
			NormalOrdersMapper normalOrdersMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			CancelOrdersMapper cancelOrdersMapper,
			AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.adminNormalOrderPartialCancelService = adminNormalOrderPartialCancelService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			String operatorType) {
		String orderIdRaw = stripExcelQuotes(trim(row.get("order_id")));
		if (!StringUtils.hasText(orderIdRaw)) {
			throw new BadRequestException("订单号错误");
		}
		long orderId;
		try {
			orderId = Long.parseLong(orderIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("订单号格式错误");
		}
		String cancelReason = trim(row.get("cancel_reason"));
		if (!StringUtils.hasText(cancelReason)) {
			throw new BadRequestException("取消原因必填");
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId));
		if (order == null) {
			throw new BadRequestException("订单不存在");
		}
		if (!"normal".equalsIgnoreCase(safe(order.getOrderType()))) {
			throw new BadRequestException("仅支持普通实体订单取消");
		}
		String deliveryStatus = safe(order.getDeliveryStatus());
		if ("PARTAIL".equalsIgnoreCase(deliveryStatus)) {
			long userId = order.getUserId() == null ? 0L : order.getUserId();
			long mappedSupplierId = "supplier".equalsIgnoreCase(safe(operatorType)) ? operatorId : 0L;
			adminNormalOrderPartialCancelService.execute(
					companyId,
					safe(operatorType),
					operatorId,
					mappedSupplierId,
					userId,
					orderId,
					cancelReason.trim());
			return;
		}
		if (!"PENDING".equalsIgnoreCase(deliveryStatus)) {
			throw new BadRequestException("没有可取消的发货明细");
		}
		String cancelStatus = safe(order.getCancelStatus());
		if (!"NO_APPLY_CANCEL".equalsIgnoreCase(cancelStatus) && !"FAILS".equalsIgnoreCase(cancelStatus)) {
			throw new ResourceException("订单已申请取消或不可重复取消");
		}
		String os = safe(order.getOrderStatus());
		if ("NOTPAY".equalsIgnoreCase(os)) {
			insertCancelRecord(order, cancelReason, 3, "SUCCESS");
			closeNotPayOrder(companyId, orderId);
			return;
		}
		if (!"PAYED".equalsIgnoreCase(os) && !"REVIEW_PASS".equalsIgnoreCase(os)) {
			throw new ResourceException("订单状态不允许取消");
		}
		if ("dada".equalsIgnoreCase(safe(order.getReceiptType()))) {
			throw new ResourceException("同城配订单请使用订单详情中的取消流程");
		}
		insertCancelRecord(order, cancelReason, 0, "WAIT_CHECK");
		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getCancelStatus, "WAIT_PROCESS");
		normalOrdersMapper.update(null, ou);
		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderId, orderId)
				.set(OrderAssociations::getCancelStatus, "WAIT_PROCESS");
		orderAssociationsMapper.update(null, au);
	}

	private void closeNotPayOrder(long companyId, long orderId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getOrderStatus, "CANCEL")
				.set(NormalOrders::getCancelStatus, "SUCCESS")
				.set(NormalOrders::getUpdateTime, now);
		normalOrdersMapper.update(null, ou);
		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderId, orderId)
				.set(OrderAssociations::getOrderStatus, "CANCEL")
				.set(OrderAssociations::getCancelStatus, "SUCCESS")
				.set(OrderAssociations::getUpdateTime, now);
		orderAssociationsMapper.update(null, au);
	}

	private void insertCancelRecord(NormalOrders order, String cancelReason, int progress, String refundStatus) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		CancelOrders c = new CancelOrders();
		c.setOrderId(order.getOrderId());
		c.setCompanyId(order.getCompanyId());
		c.setSupplierId(order.getSupplierId() != null ? order.getSupplierId().longValue() : 0L);
		c.setShopId(order.getShopId() != null ? order.getShopId() : 0L);
		c.setUserId(order.getUserId() != null ? order.getUserId() : 0L);
		c.setDistributorId(order.getDistributorId() != null ? order.getDistributorId() : 0L);
		c.setOrderType(safe(order.getOrderType()));
		c.setTotalFee(parseMoneyLong(order.getTotalFee()));
		c.setProgress(progress);
		c.setCancelFrom("shop");
		c.setCancelReason(cancelReason.trim());
		c.setRefundStatus(refundStatus);
		c.setCreateTime(now);
		c.setUpdateTime(now);
		c.setPoint(order.getPoint() != null ? order.getPoint() : 0);
		c.setPayType(safe(order.getPayType()));
		cancelOrdersMapper.insert(c);
	}

	private static long parseMoneyLong(Object totalFee) {
		if (totalFee == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(totalFee).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stripExcelQuotes(String s) {
		String t = s.trim().replace("'", "").replace("\"", "");
		return t.trim();
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
