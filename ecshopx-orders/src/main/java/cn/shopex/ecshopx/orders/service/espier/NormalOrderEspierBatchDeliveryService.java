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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.OrdersDeliveryItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.service.admin.PartialDeliveryFulfillmentReconcileService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Batch logistics delivery for a single normal order (creates {@code orders_delivery} + line updates),
 * aligned with legacy batch shipment semantics.
 */
@Service
public class NormalOrderEspierBatchDeliveryService {

	private static final String KUAIDI_REDIS_PREFIX = "kuaidiTypeOpenConfig:";

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final OrdersDeliveryItemsMapper ordersDeliveryItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver;
	private final StringRedisTemplate stringRedisTemplate;

	public NormalOrderEspierBatchDeliveryService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrdersDeliveryMapper ordersDeliveryMapper,
			OrdersDeliveryItemsMapper ordersDeliveryItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver,
			StringRedisTemplate stringRedisTemplate) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.ordersDeliveryItemsMapper = ordersDeliveryItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.deliveryCorpResolver = deliveryCorpResolver;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deliverBatchForUpload(
			long companyId,
			long orderId,
			long supplierId,
			String deliveryCorp,
			String deliveryCode) {
		if (orderId <= 0L) {
			throw new ResourceException("订单号无效");
		}
		if (!StringUtils.hasText(deliveryCorp)) {
			throw new ResourceException("缺少快递公司");
		}
		if (!StringUtils.hasText(deliveryCode)) {
			throw new ResourceException("缺少物流单号");
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId));
		if (order == null) {
			throw new ResourceException("订单不存在");
		}

		SupplierOrder supplierOrder = null;
		if (supplierId > 0L) {
			supplierOrder =
					supplierOrderMapper.selectOne(
							new LambdaQueryWrapper<SupplierOrder>()
									.eq(SupplierOrder::getCompanyId, companyId)
									.eq(SupplierOrder::getOrderId, orderId)
									.eq(SupplierOrder::getSupplierId, (int) supplierId));
			if (supplierOrder == null) {
				throw new ResourceException("订单不存在");
			}
			assertShipable(
					safe(supplierOrder.getOrderStatus()),
					safe(supplierOrder.getCancelStatus()),
					safe(supplierOrder.getDeliveryStatus()),
					safe(supplierOrder.getReceiptType()));
		} else {
			assertShipable(
					safe(order.getOrderStatus()),
					safe(order.getCancelStatus()),
					safe(order.getDeliveryStatus()),
					safe(order.getReceiptType()));
		}

		LambdaQueryWrapper<NormalOrdersItems> itemQw =
				new LambdaQueryWrapper<NormalOrdersItems>()
						.eq(NormalOrdersItems::getCompanyId, companyId)
						.eq(NormalOrdersItems::getOrderId, orderId);
		if (supplierId > 0L) {
			itemQw.eq(NormalOrdersItems::getSupplierId, (int) supplierId);
		} else {
			// 平台导入与后台手动发货一致：只能发自营商品，供应商商品由供应商发货
			itemQw.and(
					w -> w.isNull(NormalOrdersItems::getSupplierId).or().eq(NormalOrdersItems::getSupplierId, 0));
		}
		List<NormalOrdersItems> items = normalOrdersItemsMapper.selectList(itemQw);
		if (items.isEmpty()) {
			if (supplierId <= 0L && orderHasAnyItems(companyId, orderId)) {
				throw new ResourceException("供应商商品请由供应商发货");
			}
			throw new ResourceException("订单商品明细不存在");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		String corpSource = readKuaidiOpenType(companyId);
		String corpName =
				deliveryCorpResolver.resolveDeliveryCorpName(companyId, deliveryCorp, supplierId);
		String receiverMobile = firstNonBlank(order.getReceiverMobile(), order.getMobile());

		OrdersDelivery delivery = new OrdersDelivery();
		delivery.setCompanyId(companyId);
		delivery.setSupplierId(supplierId > 0L ? (int) supplierId : 0);
		delivery.setOrderId(orderId);
		delivery.setUserId(order.getUserId() != null ? order.getUserId() : 0L);
		delivery.setDeliveryCorp(deliveryCorp.trim());
		delivery.setDeliveryCorpName(corpName);
		delivery.setDeliveryCode(deliveryCode.trim());
		delivery.setDeliveryCorpSource(corpSource);
		delivery.setReceiverMobile(receiverMobile);
		delivery.setPackageType("batch");
		delivery.setDeliveryTime(now);
		delivery.setCreated(now);
		delivery.setUpdated(now);
		delivery.setSelfDeliveryOperatorId(0L);
		delivery.setDeliveryRemark("");
		delivery.setDeliveryPics("");
		ordersDeliveryMapper.insert(delivery);
		Long deliveryId = delivery.getOrdersDeliveryId();
		if (deliveryId == null || deliveryId <= 0L) {
			throw new ResourceException("创建发货单失败");
		}

		int canAftersales = 0;
		for (NormalOrdersItems it : items) {
			OrdersDeliveryItems di = new OrdersDeliveryItems();
			di.setCompanyId(companyId);
			di.setOrderId(orderId);
			di.setOrdersDeliveryId(deliveryId);
			di.setOrderItemsId(it.getId());
			di.setGoodsId(it.getGoodsId() != null ? it.getGoodsId() : 0L);
			di.setItemId(it.getItemId() != null ? it.getItemId() : 0L);
			int num = it.getNum() != null ? it.getNum() : 0;
			di.setNum(num);
			di.setItemName(it.getItemName());
			di.setPic(it.getPic());
			di.setCreated(now);
			di.setUpdated(now);
			ordersDeliveryItemsMapper.insert(di);
			canAftersales += Math.max(num, 0);

			LambdaUpdateWrapper<NormalOrdersItems> iu = new LambdaUpdateWrapper<>();
			iu.eq(NormalOrdersItems::getId, it.getId())
					.set(NormalOrdersItems::getDeliveryStatus, "DONE")
					.set(NormalOrdersItems::getDeliveryTime, now)
					.set(NormalOrdersItems::getDeliveryItemNum, num)
					.set(NormalOrdersItems::getUpdateTime, now);
			normalOrdersItemsMapper.update(null, iu);
		}

		boolean hasPending = hasPendingItems(companyId, orderId);
		int finishDays = 15;
		long autoFinishEpoch = now + finishDays * 24L * 3600L;

		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getOrderId, orderId).eq(NormalOrders::getCompanyId, companyId);
		int leftBase = order.getLeftAftersalesNum() != null ? order.getLeftAftersalesNum() : 0;
		ou.set(NormalOrders::getLeftAftersalesNum, leftBase + canAftersales);
		ou.set(NormalOrders::getDeliveryCorp, deliveryCorp.trim())
				.set(NormalOrders::getDeliveryCode, deliveryCode.trim())
				.set(NormalOrders::getDeliveryCorpSource, corpSource);
		if (hasPending) {
			ou.set(NormalOrders::getDeliveryStatus, "PARTAIL");
		} else {
			ou.set(NormalOrders::getDeliveryStatus, "DONE")
					.set(NormalOrders::getDeliveryTime, now)
					.set(NormalOrders::getAutoFinishTime, String.valueOf(autoFinishEpoch))
					.set(NormalOrders::getOrderStatus, "WAIT_BUYER_CONFIRM");
		}
		normalOrdersMapper.update(null, ou);

		if (supplierId > 0L) {
			updateSupplierShipStatus(orderId, (int) supplierId);
		}

		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getOrderId, orderId).eq(OrderAssociations::getCompanyId, companyId);
		if (hasPending) {
			au.set(OrderAssociations::getDeliveryStatus, "PARTAIL");
		} else {
			au.set(OrderAssociations::getDeliveryStatus, "DONE")
					.set(OrderAssociations::getDeliveryTime, now)
					.set(OrderAssociations::getOrderStatus, "WAIT_BUYER_CONFIRM");
		}
		au.set(OrderAssociations::getUpdateTime, now);
		orderAssociationsMapper.update(null, au);
	}

	private boolean hasPendingItems(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getCompanyId, companyId));
		return PartialDeliveryFulfillmentReconcileService.hasPendingShippableItems(items);
	}

	private boolean orderHasAnyItems(long companyId, long orderId) {
		LambdaQueryWrapper<NormalOrdersItems> qw = new LambdaQueryWrapper<>();
		qw.eq(NormalOrdersItems::getOrderId, orderId).eq(NormalOrdersItems::getCompanyId, companyId);
		return normalOrdersItemsMapper.selectCount(qw) > 0;
	}

	private void updateSupplierShipStatus(long orderId, int supplierId) {
		List<NormalOrdersItems> lines =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getSupplierId, supplierId));
		String deliveryStatus = "DONE";
		for (NormalOrdersItems v : lines) {
			int num = v.getNum() == null ? 0 : v.getNum();
			int sent = v.getDeliveryItemNum() == null ? 0 : v.getDeliveryItemNum();
			int cancelled = v.getCancelItemNum() == null ? 0 : v.getCancelItemNum();
			if (sent + cancelled < num) {
				deliveryStatus = "PARTAIL";
				break;
			}
		}
		LambdaUpdateWrapper<SupplierOrder> su = new LambdaUpdateWrapper<>();
		su.eq(SupplierOrder::getOrderId, orderId).eq(SupplierOrder::getSupplierId, supplierId);
		su.set(SupplierOrder::getDeliveryStatus, deliveryStatus);
		if ("DONE".equals(deliveryStatus)) {
			su.set(SupplierOrder::getOrderStatus, "WAIT_BUYER_CONFIRM");
		}
		supplierOrderMapper.update(null, su);
	}

	private static void assertShipable(
			String orderStatus, String cancelStatus, String deliveryStatus, String receiptType) {
		if ("NOTPAY".equals(orderStatus)) {
			throw new ResourceException("未支付订单不能发货");
		}
		if ("CANCEL".equals(orderStatus)) {
			throw new ResourceException("已取消订单不能发货");
		}
		if ("WAIT_PROCESS".equals(cancelStatus) || "REFUND_PROCESS".equals(cancelStatus)) {
			throw new ResourceException("退款处理中的订单不能发货");
		}
		if ("DONE".equals(deliveryStatus)) {
			throw new ResourceException("订单已发货");
		}
		if ("merchant".equalsIgnoreCase(receiptType)) {
			throw new ResourceException("自配送订单发货必须选择配送状态");
		}
	}

	private String readKuaidiOpenType(long companyId) {
		String key = KUAIDI_REDIS_PREFIX + sha1Hex(String.valueOf(companyId));
		try {
			String v = stringRedisTemplate.opsForValue().get(key);
			return StringUtils.hasText(v) ? v.trim() : "";
		} catch (DataAccessException e) {
			return "";
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a.trim();
		}
		if (StringUtils.hasText(b)) {
			return b.trim();
		}
		return "";
	}
}
